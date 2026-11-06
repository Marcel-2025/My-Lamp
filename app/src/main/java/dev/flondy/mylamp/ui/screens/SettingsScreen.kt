package dev.flondy.mylamp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Check
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.BuildConfig
import dev.flondy.mylamp.R
import dev.flondy.mylamp.billing.Entitlement
import dev.flondy.mylamp.core.AppLanguage
import dev.flondy.mylamp.core.PremiumSimulation
import dev.flondy.mylamp.data.DefaultLightSource
import dev.flondy.mylamp.data.LampThemeId
import dev.flondy.mylamp.data.StartBehavior
import dev.flondy.mylamp.sensor.ShakeSensitivity
import dev.flondy.mylamp.torch.LightPatterns
import dev.flondy.mylamp.ui.MainUiState
import dev.flondy.mylamp.ui.components.GlassIconButton
import dev.flondy.mylamp.ui.components.LampCard
import dev.flondy.mylamp.ui.components.glass
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LampThemes
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion
import kotlin.math.roundToInt

data class SettingsActions(
    val onBack: () -> Unit,
    val setFullscreen: (Boolean) -> Unit,
    val setShowNavigationBar: (Boolean) -> Unit,
    val setAnimations: (Boolean) -> Unit,
    val setHaptics: (Boolean) -> Unit,
    val selectTheme: (LampThemeId) -> Unit,
    val setDefaultSource: (DefaultLightSource) -> Unit,
    val setStartBehavior: (StartBehavior) -> Unit,
    val setStrobeHz: (Float) -> Unit,
    val setShakeEnabled: (Boolean) -> Unit,
    val setShakeSensitivity: (ShakeSensitivity) -> Unit,
    val openPaywall: () -> Unit,
    val restorePurchases: () -> Unit,
    val setTopHint: (Boolean) -> Unit,
    val setBottomHint: (Boolean) -> Unit,
    val openPrivacyOptions: () -> Unit,
    val openPrivacyPolicy: (() -> Unit)?,
    val contactSupport: () -> Unit,
    val openPlayStore: () -> Unit,
    // Debug
    val setSimulation: (PremiumSimulation) -> Unit,
    val debugCelebration: () -> Unit,
    val debugResetHints: () -> Unit,
    val debugResetConsent: () -> Unit,
)

@Composable
fun SettingsScreen(
    state: MainUiState,
    shakeAvailable: Boolean,
    actions: SettingsActions,
    languageTag: String = "",
    onSelectLanguage: (String) -> Unit = {},
) {
    var showLanguages by remember { mutableStateOf(false) }
    val openLanguages = { showLanguages = true }
    Box(Modifier.fillMaxSize().background(LampColors.NightGradient)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Breite Bildschirme (Tablet quer, große Tablets hoch): zwei Spalten statt einer langen Liste
            val twoColumns = maxWidth >= 840.dp
            val sidePadding = if (maxWidth >= 600.dp) 32.dp else 20.dp
            if (twoColumns) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = sidePadding, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 1280.dp).fillMaxWidth()) {
                        SettingsHeader(actions)
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                PremiumStatusCard(state, actions)
                                GeneralSection(state, actions, languageTag, openLanguages)
                            }
                            Column(Modifier.weight(1f)) {
                                FlashlightSection(state, shakeAvailable, actions)
                                InfoSection(state, actions)
                                SafetyNotice()
                                if (BuildConfig.DEBUG) DebugSection(state, actions)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = sidePadding, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val m = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                    item { Box(m) { SettingsHeader(actions) } }
                    item { Box(m) { PremiumStatusCard(state, actions) } }
                    item { Column(m) { GeneralSection(state, actions, languageTag, openLanguages) } }
                    item { Column(m) { FlashlightSection(state, shakeAvailable, actions) } }
                    item { Column(m) { InfoSection(state, actions) } }
                    item { Box(m) { SafetyNotice() } }
                    if (BuildConfig.DEBUG) item { Column(m) { DebugSection(state, actions) } }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        LanguagePicker(
            visible = showLanguages,
            selected = languageTag,
            onSelect = { tag ->
                showLanguages = false
                if (tag != languageTag) onSelectLanguage(tag)
            },
            onDismiss = { showLanguages = false },
        )
    }
}

/** Sprachauswahl als Overlay im Premium-Design (Sprachnamen in eigener Schreibweise). */
@Composable
private fun LanguagePicker(visible: Boolean, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val motion = LocalMotion.current
    if (visible) BackHandler(onBack = onDismiss)
    AnimatedVisibility(visible = visible, enter = fadeIn(motion.spec(tween(200))), exit = fadeOut(motion.spec(tween(150)))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xB3000000))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(28.dp), color = LampColors.NightRaised, borderColor = LampColors.Gold.copy(alpha = 0.35f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { }
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleLarge,
                    color = LampColors.Text,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp).semantics { heading() },
                )
                Text(
                    stringResource(R.string.settings_language_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = LampColors.TextMuted,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 8.dp),
                )
                val entries = listOf("" to stringResource(R.string.language_system)) +
                    AppLanguage.options.map { it.tag to it.nativeName }
                LazyColumn(Modifier.heightIn(max = 520.dp)) {
                    items(entries, key = { it.first }) { (tag, name) ->
                        val isSelected = tag == selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(tag) }
                                .padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                name,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) LampColors.Gold else LampColors.Text,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) Icon(Icons.Filled.Check, null, tint = LampColors.Gold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(actions: SettingsActions) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
            contentDescription = stringResource(R.string.action_back),
            onClick = actions.onBack,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = LampColors.Text,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun GeneralSection(state: MainUiState, actions: SettingsActions, languageTag: String, onOpenLanguages: () -> Unit) {
    val s = state.settings
    SectionHeader(stringResource(R.string.settings_section_general))
    LampCard(Modifier.fillMaxWidth()) {
        NavRow(
            stringResource(R.string.settings_language),
            AppLanguage.displayName(languageTag) ?: stringResource(R.string.language_system),
            onOpenLanguages,
        )
        RowDivider()
        SwitchRow(stringResource(R.string.settings_fullscreen), stringResource(R.string.settings_fullscreen_desc), s.fullscreen, actions.setFullscreen)
        RowDivider()
        SwitchRow(
            stringResource(R.string.settings_nav_bar), stringResource(R.string.settings_nav_bar_desc),
            checked = s.showNavigationBar && !s.fullscreen,
            onChange = actions.setShowNavigationBar,
            enabled = !s.fullscreen,
        )
        RowDivider()
        SwitchRow(stringResource(R.string.settings_animations), stringResource(R.string.settings_animations_desc), s.animationsEnabled, actions.setAnimations)
        RowDivider()
        SwitchRow(stringResource(R.string.settings_haptics), stringResource(R.string.settings_haptics_desc), s.hapticsEnabled, actions.setHaptics)
        RowDivider()
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall, color = LampColors.Text)
            Text(stringResource(R.string.settings_theme_desc), style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
        }
        ThemePicker(selected = s.theme, isPremium = state.isPremium, onSelect = actions.selectTheme)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun FlashlightSection(state: MainUiState, shakeAvailable: Boolean, actions: SettingsActions) {
    val s = state.settings
    SectionHeader(stringResource(R.string.settings_section_flashlight))
    LampCard(Modifier.fillMaxWidth()) {
        ChoiceRow(
            title = stringResource(R.string.settings_default_source),
            options = listOf(
                DefaultLightSource.LED to stringResource(R.string.source_led),
                DefaultLightSource.SCREEN to stringResource(R.string.source_screen),
            ),
            selected = s.defaultLightSource,
            onSelect = actions.setDefaultSource,
        )
        RowDivider()
        ChoiceRow(
            title = stringResource(R.string.settings_start_behavior),
            options = listOf(
                StartBehavior.OFF to stringResource(R.string.start_off),
                StartBehavior.TORCH_ON to stringResource(R.string.start_torch),
                StartBehavior.SCREEN_LIGHT to stringResource(R.string.start_screen),
            ),
            selected = s.startBehavior,
            onSelect = actions.setStartBehavior,
        )
        RowDivider()
        StrobeSliderRow(s.strobeHz, actions.setStrobeHz)
        RowDivider()
        SwitchRow(
            stringResource(R.string.settings_shake),
            if (shakeAvailable) stringResource(R.string.settings_shake_desc) else stringResource(R.string.settings_shake_unavailable),
            checked = s.shakeEnabled && shakeAvailable,
            onChange = actions.setShakeEnabled,
            enabled = shakeAvailable,
        )
        if (s.shakeEnabled && shakeAvailable) {
            ChoiceRow(
                title = stringResource(R.string.settings_shake_sensitivity),
                options = listOf(
                    ShakeSensitivity.LOW to stringResource(R.string.sensitivity_low),
                    ShakeSensitivity.MEDIUM to stringResource(R.string.sensitivity_medium),
                    ShakeSensitivity.HIGH to stringResource(R.string.sensitivity_high),
                ),
                selected = s.shakeSensitivity,
                onSelect = actions.setShakeSensitivity,
            )
        }
    }
}

@Composable
private fun InfoSection(state: MainUiState, actions: SettingsActions) {
    SectionHeader(stringResource(R.string.settings_section_info))
    LampCard(Modifier.fillMaxWidth()) {
        InfoRow(stringResource(R.string.settings_version), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        RowDivider()
        InfoRow(stringResource(R.string.settings_developer), "Flondy")
        RowDivider()
        if (state.ads.privacyOptionsRequired) {
            NavRow(stringResource(R.string.settings_privacy_options), stringResource(R.string.settings_privacy_options_desc), actions.openPrivacyOptions)
            RowDivider()
        }
        actions.openPrivacyPolicy?.let {
            NavRow(stringResource(R.string.settings_privacy_policy), null, it)
            RowDivider()
        }
        NavRow(stringResource(R.string.settings_support), BuildConfig.SUPPORT_EMAIL, actions.contactSupport)
        RowDivider()
        NavRow(stringResource(R.string.settings_play_store), stringResource(R.string.settings_play_store_desc), actions.openPlayStore)
    }
}

@Composable
private fun SafetyNotice() {
    Text(
        stringResource(R.string.settings_safety_notice),
        style = MaterialTheme.typography.bodySmall,
        color = LampColors.TextFaint,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
    )
}

@Composable
private fun DebugSection(state: MainUiState, actions: SettingsActions) {
    SectionHeader(stringResource(R.string.debug_section))
    DebugCard(state, actions)
}

@Composable
private fun PremiumStatusCard(state: MainUiState, actions: SettingsActions) {
    val s = state.settings
    LampCard(
        Modifier.fillMaxWidth(),
        borderColor = if (state.isPremium) LampColors.Gold.copy(alpha = 0.45f) else LampColors.Outline,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (state.isPremium) LampColors.GoldGradient else Brush.linearGradient(listOf(LampColors.CardHigh, LampColors.CardHigh))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_crown), null,
                    tint = if (state.isPremium) LampColors.Night else LampColors.Gold,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        state.isPremium -> stringResource(R.string.premium_status_active)
                        state.billing.entitlement == Entitlement.PENDING -> stringResource(R.string.premium_status_pending)
                        else -> stringResource(R.string.premium_status_free)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isPremium) LampColors.Gold else LampColors.Text,
                )
                Text(
                    if (state.isPremium) stringResource(R.string.premium_status_active_desc) else stringResource(R.string.premium_status_free_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = LampColors.TextMuted,
                )
            }
        }
        if (!state.isPremium) {
            RowDivider()
            NavRow(stringResource(R.string.action_go_premium), state.billing.formattedPrice?.let { stringResource(R.string.paywall_price_once, it) }, actions.openPaywall, accent = true)
        } else {
            RowDivider()
            SwitchRow(stringResource(R.string.settings_premium_top_hint), stringResource(R.string.settings_premium_top_hint_desc), s.showPremiumTopHint, actions.setTopHint)
            RowDivider()
            SwitchRow(stringResource(R.string.settings_premium_bottom_hint), stringResource(R.string.settings_premium_bottom_hint_desc), s.showPremiumBottomHint, actions.setBottomHint)
        }
        RowDivider()
        NavRow(stringResource(R.string.action_restore_purchases), stringResource(R.string.settings_restore_desc), actions.restorePurchases)
    }
}

@Composable
private fun ThemePicker(selected: LampThemeId, isPremium: Boolean, onSelect: (LampThemeId) -> Unit) {
    val haptics = LocalHaptics.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(LampThemes.all, key = { it.id }) { visual ->
            val isSelected = visual.id == selected
            val locked = visual.id.isPremium && !isPremium
            val border by animateColorAsState(
                if (isSelected) LampColors.Gold else LampColors.Outline,
                LocalMotion.current.spec(tween(250)), label = "themeBorder",
            )
            val name = stringResource(visual.nameRes)
            Column(
                Modifier
                    .width(88.dp)
                    .selectable(selected = isSelected, role = Role.RadioButton) {
                        haptics.click()
                        onSelect(visual.id)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(width = 88.dp, height = 120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(if (isSelected) 2.dp else 1.dp, border, RoundedCornerShape(18.dp)),
                ) {
                    val img = visual.thumb
                    if (img != null) {
                        Image(painterResource(img), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(LampColors.NightGradient))
                    }
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(LampColors.Gold, LampColors.GoldWarm)))
                    )
                    if (locked) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0x99050711)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Lock, stringResource(R.string.cd_locked_premium), tint = LampColors.Gold, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) LampColors.Gold else LampColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StrobeSliderRow(hz: Float, onCommit: (Float) -> Unit) {
    var local by remember(hz) { mutableFloatStateOf(hz) }
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_strobe_speed), style = MaterialTheme.typography.titleSmall, color = LampColors.Text, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.strobe_hz_value, local.roundToInt()), style = MaterialTheme.typography.labelLarge, color = LampColors.Strobe)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local) },
            valueRange = LightPatterns.STROBE_MIN_HZ..LightPatterns.STROBE_MAX_HZ,
            colors = SliderDefaults.colors(
                thumbColor = LampColors.Strobe,
                activeTrackColor = LampColors.Strobe,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
            ),
        )
    }
}

@Composable
private fun DebugCard(state: MainUiState, actions: SettingsActions) {
    LampCard(Modifier.fillMaxWidth(), borderColor = LampColors.Strobe.copy(alpha = 0.4f)) {
        ChoiceRow(
            title = stringResource(R.string.debug_simulation),
            options = listOf(
                PremiumSimulation.REAL to "Real",
                PremiumSimulation.SIMULATE_PREMIUM to "Premium",
                PremiumSimulation.SIMULATE_FREE to "Free",
            ),
            selected = state.simulation,
            onSelect = actions.setSimulation,
        )
        RowDivider()
        val b = state.billing
        InfoRow("Billing", "entitlement=${b.entitlement} play=${b.verifiedWithPlay} connected=${b.connected}")
        InfoRow("Produkt", "available=${b.productAvailable} price=${b.formattedPrice ?: "-"} sigCheck=${b.signatureCheckEnabled} code=${b.lastResponseCode ?: "-"}")
        val a = state.ads
        InfoRow("Ads", "configured=${a.configured} consent=${a.consentResolved} canRequest=${a.canRequestAds} sdk=${a.sdkInitialized} show=${state.showAds}")
        a.lastError?.let { InfoRow("Ads-Fehler", it) }
        InfoRow("Licht", "mode=${state.mode} flash=${state.hasFlash} strength=${state.supportsStrength}")
        RowDivider()
        NavRow(stringResource(R.string.debug_celebration), null, actions.debugCelebration)
        NavRow(stringResource(R.string.debug_reset_hints), null, actions.debugResetHints)
        NavRow(stringResource(R.string.debug_reset_consent), null, actions.debugResetConsent)
        SwitchRow("Fullscreen-Test", null, state.settings.fullscreen, actions.setFullscreen)
    }
}

// ---------------------------------------------------------------- Bausteine

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LampColors.Gold.copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 24.dp, bottom = 10.dp)
            .semantics { heading() },
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 18.dp), thickness = 1.dp, color = LampColors.Outline)
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val haptics = LocalHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) {
                haptics.toggle(it)
                onChange(it)
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) LampColors.Text else LampColors.TextFaint)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LampColors.Night,
                checkedTrackColor = LampColors.Gold,
                checkedBorderColor = LampColors.Gold,
                uncheckedThumbColor = LampColors.Neutral400,
                uncheckedTrackColor = LampColors.CardHigh,
                uncheckedBorderColor = LampColors.OutlineStrong,
            ),
        )
    }
}

@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val haptics = LocalHaptics.current
    val motion = LocalMotion.current
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LampColors.Text)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(50), color = LampColors.Night.copy(alpha = 0.6f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                val bg by animateColorAsState(if (isSelected) LampColors.Gold else Color.Transparent, motion.spec(tween(250)), label = "chipBg")
                val fg by animateColorAsState(if (isSelected) LampColors.Night else LampColors.TextMuted, motion.spec(tween(250)), label = "chipFg")
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .selectable(selected = isSelected, role = Role.RadioButton) {
                            haptics.click()
                            onSelect(value)
                        }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun NavRow(title: String, subtitle: String?, onClick: () -> Unit, accent: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (accent) LampColors.Gold else LampColors.Text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = if (accent) LampColors.Gold else LampColors.TextFaint)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LampColors.Text, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = LampColors.TextMuted, modifier = Modifier.weight(0.6f))
    }
}
