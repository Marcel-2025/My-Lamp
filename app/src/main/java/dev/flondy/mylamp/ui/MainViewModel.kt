package dev.flondy.mylamp.ui

import android.app.Activity
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.flondy.mylamp.MyLampApp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.ads.AdsState
import dev.flondy.mylamp.billing.BillingEvent
import dev.flondy.mylamp.billing.BillingState
import dev.flondy.mylamp.billing.Entitlement
import dev.flondy.mylamp.core.AppContainer
import dev.flondy.mylamp.core.DebugOverrides
import dev.flondy.mylamp.core.PremiumSimulation
import dev.flondy.mylamp.core.AutoOff
import dev.flondy.mylamp.data.AppSettings
import dev.flondy.mylamp.data.ButtonSkin
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.shop.EffectiveLook
import dev.flondy.mylamp.shop.PremiumPreview
import dev.flondy.mylamp.shop.ShopCatalog
import dev.flondy.mylamp.shop.ShopItem
import dev.flondy.mylamp.torch.LightEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import dev.flondy.mylamp.data.DefaultLightSource
import dev.flondy.mylamp.data.LampThemeId
import dev.flondy.mylamp.data.ScreenLightColor
import dev.flondy.mylamp.data.StartBehavior
import dev.flondy.mylamp.sensor.ShakeSensitivity
import dev.flondy.mylamp.torch.LightMode
import dev.flondy.mylamp.torch.TorchError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Route { HOME, SETTINGS, SHOP, PAYWALL, SCREEN_LIGHT }

/** Laufender Auto-Aus-Timer (Zeiten in [SystemClock.elapsedRealtime]). */
data class AutoOffTimer(val endsAtMs: Long, val totalMs: Long)

data class UiMessage(@param:StringRes val text: Int, val isError: Boolean = false, val id: Long = System.nanoTime())

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val settingsLoaded: Boolean = false,
    val mode: LightMode = LightMode.OFF,
    val strobeHz: Float = AppSettings().strobeHz,
    val torchStrength: Float = 1f,
    val hasFlash: Boolean = true,
    val supportsStrength: Boolean = false,
    val billing: BillingState = BillingState(),
    val simulation: PremiumSimulation = PremiumSimulation.REAL,
    val ads: AdsState = AdsState(configured = false),
    val effect: LightEffect? = null,
    val morseText: String = "",
    val preview: PremiumPreview? = null,
    val autoOff: AutoOffTimer? = null,
    /** Tatsächlich angezeigtes Design/Skin/Bildschirmlicht (eigene Auswahl oder Vorschau). */
    val theme: LampThemeId = LampThemeId.CLASSIC,
    val skin: ButtonSkin = ButtonSkin.GOLD,
    val screenColor: ScreenLightColor = ScreenLightColor.NEUTRAL,
    val screenEffect: ScreenEffect = ScreenEffect.NONE,
) {
    /** Premium für die Darstellung (inkl. Debug-Simulation, die nur in Debug-Builds wirkt). */
    val isPremium: Boolean get() = DebugOverrides.effectivePremium(billing.isPremium, simulation)

    /** Werbung nur, wenn sicher feststeht, dass der Nutzer KEIN Premium besitzt. */
    val showAds: Boolean
        get() = ads.configured && !isPremium &&
            (billing.entitlement == Entitlement.NONE || billing.entitlement == Entitlement.PENDING ||
                simulation == PremiumSimulation.SIMULATE_FREE)

    val isLightOn: Boolean get() = mode != LightMode.OFF
}

private data class LocalUi(
    val strobeHz: Float? = null,
    val torchStrength: Float? = null,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val torch = container.torchController
    private val settingsRepo = container.settingsRepository
    private val billing = container.billingRepository
    private val ads = container.adsManager

    private val localUi = MutableStateFlow(LocalUi())

    private val settingsFlow = settingsRepo.settings
    private val loadedSettings = settingsFlow.map { it to true }

    private val _preview = MutableStateFlow<PremiumPreview?>(null)
    private val _autoOff = MutableStateFlow<AutoOffTimer?>(null)

    private data class Extras(val effect: LightEffect?, val morse: String, val preview: PremiumPreview?, val autoOff: AutoOffTimer?)

    private val extras = combine(torch.effect, torch.morseText, _preview, _autoOff) { e, m, p, a -> Extras(e, m, p, a) }

    val uiState: StateFlow<MainUiState> = combine(
        loadedSettings,
        combine(torch.mode, torch.strobeHz, localUi) { m, hz, l -> Triple(m, hz, l) },
        combine(billing.state, DebugOverrides.simulation) { b, sim -> b to sim },
        ads.state,
        extras,
    ) { (settings, loaded), (mode, hz, local), (billingState, simulation), adsState, x ->
        val premium = DebugOverrides.effectivePremium(billingState.isPremium, simulation)
        val now = SystemClock.elapsedRealtime()
        MainUiState(
            effect = x.effect,
            morseText = x.morse,
            preview = x.preview,
            autoOff = x.autoOff,
            theme = EffectiveLook.theme(settings.theme, premium, x.preview, now),
            skin = EffectiveLook.skin(settings.buttonSkin, premium, x.preview, now),
            screenColor = EffectiveLook.screenColor(settings.screenLightColor, premium, x.preview, now),
            screenEffect = EffectiveLook.screenEffect(settings.screenEffect, premium, x.preview, now),
            settings = settings,
            settingsLoaded = loaded,
            mode = mode,
            strobeHz = local.strobeHz ?: hz,
            torchStrength = local.torchStrength ?: settings.torchStrength,
            hasFlash = torch.hasFlash,
            supportsStrength = torch.supportsStrength,
            billing = billingState,
            simulation = simulation,
            ads = adsState,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState(hasFlash = torch.hasFlash))

    private val _backStack = MutableStateFlow(listOf(Route.HOME))
    val route: StateFlow<Route> = _backStack.map { it.last() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Route.HOME)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration.asStateFlow()

    private val _showStrobeWarning = MutableStateFlow(false)
    val showStrobeWarning: StateFlow<Boolean> = _showStrobeWarning.asStateFlow()

    private val _showEffects = MutableStateFlow(false)
    val showEffects: StateFlow<Boolean> = _showEffects.asStateFlow()

    private var startBehaviorApplied = false
    private var previewJob: Job? = null
    private var autoOffJob: Job? = null

    init {
        // Gespeicherte Werte in die Hardware-Steuerung übernehmen.
        viewModelScope.launch {
            settingsFlow.map { it.strobeHz }.distinctUntilChanged().collect { torch.setStrobeHz(it) }
        }
        viewModelScope.launch {
            settingsFlow.map { it.torchStrength }.distinctUntilChanged().collect { torch.setStrength(it) }
        }
        viewModelScope.launch {
            torch.errors.collect { error ->
                _messages.tryEmit(
                    when (error) {
                        TorchError.NO_FLASH -> UiMessage(R.string.error_no_flash, isError = true)
                        TorchError.CAMERA_IN_USE -> UiMessage(R.string.error_camera_in_use, isError = true)
                        TorchError.UNKNOWN -> UiMessage(R.string.error_torch_unknown, isError = true)
                    }
                )
            }
        }
        // Auto-Aus: startet, sobald ein Licht leuchtet (LED-Modus oder Bildschirmlicht)
        viewModelScope.launch {
            combine(torch.mode, route, settingsFlow.map { it.autoOffMinutes }) { mode, r, minutes ->
                (mode != LightMode.OFF || r == Route.SCREEN_LIGHT) to minutes
            }.distinctUntilChanged().collect { (active, minutes) ->
                if (active && minutes > 0) startAutoOff(minutes) else cancelAutoOff()
            }
        }
        viewModelScope.launch {
            billing.events.collect { event ->
                when (event) {
                    BillingEvent.PurchaseCompleted -> {
                        // Was gerade in der Vorschau lief, wird direkt übernommen.
                        _preview.value?.let { applyItem(it.item, fromPurchase = true) }
                        endPreview(silent = true)
                        val s = settingsFlow.first()
                        if (!s.premiumCelebrationShown) _showCelebration.value = true
                        else _messages.tryEmit(UiMessage(R.string.billing_purchase_success))
                        if (route.value == Route.PAYWALL) navigateBack()
                    }
                    BillingEvent.PurchasePending -> _messages.tryEmit(UiMessage(R.string.billing_pending))
                    BillingEvent.Restored -> _messages.tryEmit(UiMessage(R.string.billing_restored))
                    BillingEvent.NothingToRestore -> _messages.tryEmit(UiMessage(R.string.billing_nothing_to_restore))
                    BillingEvent.Unavailable -> _messages.tryEmit(UiMessage(R.string.billing_unavailable, isError = true))
                    is BillingEvent.Error -> _messages.tryEmit(UiMessage(R.string.billing_error, isError = true))
                }
            }
        }
    }

    // ------------------------------------------------------------------ Navigation

    fun navigate(route: Route) {
        _backStack.update { if (it.last() == route) it else it + route }
    }

    /** Zurück bis zum Startbildschirm (z. B. für eine Design-Vorschau). */
    fun popToHome() {
        _backStack.value = listOf(Route.HOME)
    }

    /** @return false, wenn bereits auf dem Startbildschirm (System soll die App schließen). */
    fun navigateBack(): Boolean {
        val stack = _backStack.value
        if (stack.size <= 1) return false
        _backStack.value = stack.dropLast(1)
        return true
    }

    // ------------------------------------------------------------------ Licht

    fun applyStartBehaviorOnce() {
        if (startBehaviorApplied) return
        startBehaviorApplied = true
        viewModelScope.launch {
            when (settingsFlow.first().startBehavior) {
                StartBehavior.OFF -> Unit
                StartBehavior.TORCH_ON -> if (torch.hasFlash && torch.mode.value == LightMode.OFF) torch.startTorch()
                StartBehavior.SCREEN_LIGHT -> navigate(Route.SCREEN_LIGHT)
            }
        }
    }

    fun onPowerClick() {
        val state = uiState.value
        when {
            state.mode != LightMode.OFF -> torch.stop()
            !state.hasFlash || state.settings.defaultLightSource == DefaultLightSource.SCREEN -> navigate(Route.SCREEN_LIGHT)
            else -> torch.startTorch()
        }
    }

    fun onShake() {
        if (!torch.hasFlash) return
        if (torch.mode.value == LightMode.OFF) torch.startTorch() else torch.stop()
    }

    fun onSosClick() = torch.toggleSos()

    /** Lautstärketaste (falls in den Einstellungen aktiviert): Licht an/aus. */
    fun onVolumeKey(): Boolean {
        if (!uiState.value.settings.volumeKeysToggle) return false
        if (route.value == Route.SCREEN_LIGHT) {
            navigateBack()
            return true
        }
        onPowerClick()
        return true
    }

    fun openEffects() {
        _showEffects.value = true
    }

    fun closeEffects() {
        _showEffects.value = false
    }

    fun startMorse(text: String) {
        val clean = dev.flondy.mylamp.torch.LightPatterns.sanitizeMorse(text)
        if (clean.isEmpty()) {
            _messages.tryEmit(UiMessage(R.string.morse_preview_empty, isError = true))
            return
        }
        save { settingsRepo.setMorseText(text) }
        _showEffects.value = false
        torch.startMorse(clean)
    }

    /** Lichteffekt starten bzw. beenden. Premium-Effekte ohne Premium starten eine 10-Sekunden-Vorschau. */
    fun onEffectClick(effect: LightEffect) {
        val item = ShopItem.Effect(effect)
        if (!ShopCatalog.isUnlocked(item, uiState.value.isPremium)) {
            startPreview(item)
            return
        }
        _showEffects.value = false
        torch.toggleEffect(effect)
    }

    fun cycleAutoOff() {
        val next = AutoOff.next(uiState.value.settings.autoOffMinutes)
        save { settingsRepo.setAutoOffMinutes(next) }
    }

    fun setAutoOffMinutes(v: Int) = save { settingsRepo.setAutoOffMinutes(v) }

    private fun startAutoOff(minutes: Int) {
        autoOffJob?.cancel()
        val total = AutoOff.durationMs(minutes)
        _autoOff.value = AutoOffTimer(SystemClock.elapsedRealtime() + total, total)
        autoOffJob = viewModelScope.launch {
            delay(total)
            _autoOff.value = null
            torch.stop()
            if (route.value == Route.SCREEN_LIGHT) navigateBack()
            _messages.tryEmit(UiMessage(R.string.auto_off_done))
        }
    }

    private fun cancelAutoOff() {
        autoOffJob?.cancel()
        autoOffJob = null
        _autoOff.value = null
    }

    // ------------------------------------------------------------------ Studio & Shop

    /**
     * Freigeschaltete Inhalte werden übernommen, gesperrte Premium-Inhalte starten eine Vorschau:
     * Designs und Skins 30 s auf dem Startbildschirm, Bildschirmlicht 30 s, LED-Effekte 10 s.
     */
    fun onShopItem(item: ShopItem) {
        if (ShopCatalog.isUnlocked(item, uiState.value.isPremium)) applyItem(item, fromPurchase = false)
        else startPreview(item)
    }

    fun startPreview(item: ShopItem) {
        previewJob?.cancel()
        endPreview(silent = true)
        val duration = PremiumPreview.durationFor(item)
        _preview.value = PremiumPreview(item, SystemClock.elapsedRealtime() + duration)
        when (item) {
            is ShopItem.Theme, is ShopItem.Skin -> popToHome()
            is ShopItem.Effect -> {
                _showEffects.value = false
                if (torch.hasFlash) {
                    popToHome()
                    torch.startEffect(item.effect)
                } else {
                    _messages.tryEmit(UiMessage(R.string.error_no_flash, isError = true))
                }
            }
            is ShopItem.ScreenColor, is ShopItem.ScreenFx -> {
                if (torch.mode.value != LightMode.OFF) torch.stop()
                navigate(Route.SCREEN_LIGHT)
            }
        }
        previewJob = viewModelScope.launch {
            delay(duration)
            endPreview(silent = false)
        }
    }

    fun endPreview(silent: Boolean = false) {
        val p = _preview.value ?: return
        previewJob?.cancel()
        previewJob = null
        _preview.value = null
        val item = p.item
        if (item is ShopItem.Effect && torch.mode.value == LightMode.EFFECT && torch.effect.value == item.effect) torch.stop()
        if (!silent) _messages.tryEmit(UiMessage(R.string.preview_ended))
    }

    private fun applyItem(item: ShopItem, fromPurchase: Boolean) {
        when (item) {
            is ShopItem.Theme -> save { settingsRepo.setTheme(item.id) }
            is ShopItem.Skin -> save { settingsRepo.setButtonSkin(item.skin) }
            is ShopItem.ScreenColor -> save { settingsRepo.setScreenLightColor(item.color) }
            is ShopItem.ScreenFx -> save { settingsRepo.setScreenEffect(item.effect) }
            is ShopItem.Effect -> if (!fromPurchase) {
                if (torch.hasFlash) {
                    popToHome()
                    torch.startEffect(item.effect)
                } else {
                    _messages.tryEmit(UiMessage(R.string.error_no_flash, isError = true))
                }
                return
            }
        }
        if (!fromPurchase) _messages.tryEmit(UiMessage(R.string.shop_selected))
    }

    fun onStrobeClick() {
        if (torch.mode.value == LightMode.STROBE) {
            torch.stop()
            return
        }
        if (!torch.hasFlash) {
            _messages.tryEmit(UiMessage(R.string.error_no_flash, isError = true))
            return
        }
        viewModelScope.launch {
            if (settingsFlow.first().strobeWarningAccepted) torch.startStrobe()
            else _showStrobeWarning.value = true
        }
    }

    fun onStrobeWarningResult(accepted: Boolean) {
        _showStrobeWarning.value = false
        if (accepted) {
            viewModelScope.launch { settingsRepo.setStrobeWarningAccepted(true) }
            torch.startStrobe()
        }
    }

    fun onScreenLightClick() {
        if (torch.mode.value != LightMode.OFF) torch.stop()
        navigate(Route.SCREEN_LIGHT)
    }

    fun onStrobeHzChange(hz: Float) {
        localUi.update { it.copy(strobeHz = hz) }
        torch.setStrobeHz(hz)
    }

    fun onStrobeHzCommit() {
        val hz = localUi.value.strobeHz ?: return
        viewModelScope.launch {
            settingsRepo.setStrobeHz(hz)
            localUi.update { it.copy(strobeHz = null) }
        }
    }

    fun onTorchStrengthChange(value: Float) {
        localUi.update { it.copy(torchStrength = value) }
        torch.setStrength(value)
    }

    fun onTorchStrengthCommit() {
        val v = localUi.value.torchStrength ?: return
        viewModelScope.launch {
            settingsRepo.setTorchStrength(v)
            localUi.update { it.copy(torchStrength = null) }
        }
    }

    /** Beim endgültigen Verlassen der App: alle Muster und die LED aus. */
    fun onAppExit() = torch.stop()

    /** Stroboskop nicht unsichtbar im Hintergrund weiterlaufen lassen. */
    fun onAppBackgrounded() {
        if (torch.mode.value == LightMode.STROBE) torch.stop()
    }

    fun onAppForegrounded() {
        billing.refreshPurchases()
    }

    // ------------------------------------------------------------------ Premium

    fun purchase(activity: Activity) = billing.launchPurchase(activity)
    fun restorePurchases() = billing.restorePurchases()
    fun reloadProduct() = billing.queryProductDetails()

    fun dismissCelebration() {
        _showCelebration.value = false
        viewModelScope.launch { settingsRepo.setPremiumCelebrationShown(true) }
    }

    fun gatherConsent(activity: Activity) = ads.gatherConsentAndInit(activity)
    fun showPrivacyOptions(activity: Activity) = ads.showPrivacyOptions(activity)

    // ------------------------------------------------------------------ Einstellungen

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun setFullscreen(v: Boolean) = save { settingsRepo.setFullscreen(v) }
    fun setShowNavigationBar(v: Boolean) = save { settingsRepo.setShowNavigationBar(v) }
    fun setAnimationsEnabled(v: Boolean) = save { settingsRepo.setAnimationsEnabled(v) }
    fun setHapticsEnabled(v: Boolean) = save { settingsRepo.setHapticsEnabled(v) }
    fun setDefaultLightSource(v: DefaultLightSource) = save { settingsRepo.setDefaultLightSource(v) }
    fun setStartBehavior(v: StartBehavior) = save { settingsRepo.setStartBehavior(v) }
    fun setShakeEnabled(v: Boolean) = save { settingsRepo.setShakeEnabled(v) }
    fun setShakeSensitivity(v: ShakeSensitivity) = save { settingsRepo.setShakeSensitivity(v) }
    fun setScreenLightColor(v: ScreenLightColor) = save { settingsRepo.setScreenLightColor(v) }
    fun setScreenLightBrightness(v: Float) = save { settingsRepo.setScreenLightBrightness(v) }
    fun setScreenCustomHue(v: Float) = save { settingsRepo.setScreenCustomHue(v) }
    fun setVolumeKeysToggle(v: Boolean) = save { settingsRepo.setVolumeKeysToggle(v) }

    /** Farbe oder Effekt im Bildschirmlicht: gesperrte Premium-Inhalte starten eine Vorschau. */
    fun onScreenColor(c: ScreenLightColor) {
        if (c.isPremium && !uiState.value.isPremium) startPreview(ShopItem.ScreenColor(c))
        else {
            endPreview(silent = true)
            save { settingsRepo.setScreenLightColor(c) }
        }
    }

    fun onScreenEffect(e: ScreenEffect) {
        if (e.isPremium && !uiState.value.isPremium) startPreview(ShopItem.ScreenFx(e))
        else {
            endPreview(silent = true)
            save { settingsRepo.setScreenEffect(e) }
        }
    }
    fun setShowPremiumTopHint(v: Boolean) = save { settingsRepo.setShowPremiumTopHint(v) }
    fun setShowPremiumBottomHint(v: Boolean) = save { settingsRepo.setShowPremiumBottomHint(v) }

    /** Premium-Designs sind nur mit Premium wählbar – ohne Premium startet eine 30-Sekunden-Vorschau. */
    fun selectTheme(id: LampThemeId) {
        if (id.isPremium && !uiState.value.isPremium) {
            startPreview(ShopItem.Theme(id))
            return
        }
        endPreview(silent = true)
        save { settingsRepo.setTheme(id) }
    }

    // ------------------------------------------------------------------ Debug (nur Debug-Builds)

    fun setSimulation(v: PremiumSimulation) = DebugOverrides.setSimulation(v)
    fun debugShowCelebration() { _showCelebration.value = true }
    fun debugResetHints() = save { settingsRepo.resetHints() }
    fun debugResetConsent() = ads.resetConsentForDebug()

    fun postMessage(@StringRes text: Int, isError: Boolean = false) {
        _messages.tryEmit(UiMessage(text, isError))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyLampApp
                MainViewModel(app.container)
            }
        }
    }
}
