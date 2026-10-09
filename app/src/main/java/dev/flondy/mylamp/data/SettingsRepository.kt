package dev.flondy.mylamp.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.flondy.mylamp.sensor.ShakeSensitivity
import dev.flondy.mylamp.torch.LightPatterns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persistente Einstellungen auf Basis von Jetpack DataStore.
 */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val FULLSCREEN = booleanPreferencesKey("fullscreen")
        val SHOW_NAV_BAR = booleanPreferencesKey("show_navigation_bar")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_SOURCE = stringPreferencesKey("default_light_source")
        val START_BEHAVIOR = stringPreferencesKey("start_behavior")
        val STROBE_HZ = floatPreferencesKey("strobe_hz")
        val TORCH_STRENGTH = floatPreferencesKey("torch_strength")
        val SHAKE_ENABLED = booleanPreferencesKey("shake_enabled")
        val SHAKE_SENSITIVITY = stringPreferencesKey("shake_sensitivity")
        val SCREEN_COLOR = stringPreferencesKey("screen_light_color")
        val SCREEN_BRIGHTNESS = floatPreferencesKey("screen_light_brightness")
        val PREMIUM_TOP_HINT = booleanPreferencesKey("show_premium_top_hint")
        val PREMIUM_BOTTOM_HINT = booleanPreferencesKey("show_premium_bottom_hint")
        val CELEBRATION_SHOWN = booleanPreferencesKey("premium_celebration_shown")
        val STROBE_WARNING = booleanPreferencesKey("strobe_warning_accepted")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { e ->
            if (e is IOException) {
                Log.w("SettingsRepository", "Reading settings failed, using defaults", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { it.toSettings() }
        .distinctUntilChanged()

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            fullscreen = this[Keys.FULLSCREEN] ?: d.fullscreen,
            showNavigationBar = this[Keys.SHOW_NAV_BAR] ?: d.showNavigationBar,
            animationsEnabled = this[Keys.ANIMATIONS] ?: d.animationsEnabled,
            hapticsEnabled = this[Keys.HAPTICS] ?: d.hapticsEnabled,
            theme = enumOrDefault(this[Keys.THEME], d.theme),
            defaultLightSource = enumOrDefault(this[Keys.DEFAULT_SOURCE], d.defaultLightSource),
            startBehavior = enumOrDefault(this[Keys.START_BEHAVIOR], d.startBehavior),
            strobeHz = LightPatterns.clampStrobeHz(this[Keys.STROBE_HZ] ?: d.strobeHz),
            torchStrength = (this[Keys.TORCH_STRENGTH] ?: d.torchStrength).coerceIn(0.05f, 1f),
            shakeEnabled = this[Keys.SHAKE_ENABLED] ?: d.shakeEnabled,
            shakeSensitivity = enumOrDefault(this[Keys.SHAKE_SENSITIVITY], d.shakeSensitivity),
            screenLightColor = enumOrDefault(this[Keys.SCREEN_COLOR], d.screenLightColor),
            screenLightBrightness = (this[Keys.SCREEN_BRIGHTNESS] ?: d.screenLightBrightness).coerceIn(0.05f, 1f),
            showPremiumTopHint = this[Keys.PREMIUM_TOP_HINT] ?: d.showPremiumTopHint,
            showPremiumBottomHint = this[Keys.PREMIUM_BOTTOM_HINT] ?: d.showPremiumBottomHint,
            premiumCelebrationShown = this[Keys.CELEBRATION_SHOWN] ?: d.premiumCelebrationShown,
            strobeWarningAccepted = this[Keys.STROBE_WARNING] ?: d.strobeWarningAccepted,
        )
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        try {
            store.edit { block(it) }
        } catch (e: IOException) {
            Log.w("SettingsRepository", "Writing settings failed", e)
        }
    }

    suspend fun setFullscreen(value: Boolean) = edit { it[Keys.FULLSCREEN] = value }
    suspend fun setShowNavigationBar(value: Boolean) = edit { it[Keys.SHOW_NAV_BAR] = value }
    suspend fun setAnimationsEnabled(value: Boolean) = edit { it[Keys.ANIMATIONS] = value }
    suspend fun setHapticsEnabled(value: Boolean) = edit { it[Keys.HAPTICS] = value }
    suspend fun setTheme(value: LampThemeId) = edit { it[Keys.THEME] = value.name }
    suspend fun setDefaultLightSource(value: DefaultLightSource) = edit { it[Keys.DEFAULT_SOURCE] = value.name }
    suspend fun setStartBehavior(value: StartBehavior) = edit { it[Keys.START_BEHAVIOR] = value.name }
    suspend fun setStrobeHz(value: Float) = edit { it[Keys.STROBE_HZ] = LightPatterns.clampStrobeHz(value) }
    suspend fun setTorchStrength(value: Float) = edit { it[Keys.TORCH_STRENGTH] = value.coerceIn(0.05f, 1f) }
    suspend fun setShakeEnabled(value: Boolean) = edit { it[Keys.SHAKE_ENABLED] = value }
    suspend fun setShakeSensitivity(value: ShakeSensitivity) = edit { it[Keys.SHAKE_SENSITIVITY] = value.name }
    suspend fun setScreenLightColor(value: ScreenLightColor) = edit { it[Keys.SCREEN_COLOR] = value.name }
    suspend fun setScreenLightBrightness(value: Float) = edit { it[Keys.SCREEN_BRIGHTNESS] = value.coerceIn(0.05f, 1f) }
    suspend fun setShowPremiumTopHint(value: Boolean) = edit { it[Keys.PREMIUM_TOP_HINT] = value }
    suspend fun setShowPremiumBottomHint(value: Boolean) = edit { it[Keys.PREMIUM_BOTTOM_HINT] = value }
    suspend fun setPremiumCelebrationShown(value: Boolean) = edit { it[Keys.CELEBRATION_SHOWN] = value }
    suspend fun setStrobeWarningAccepted(value: Boolean) = edit { it[Keys.STROBE_WARNING] = value }

    /** Nur für Debug: alle Hinweise und Einmal-Dialoge zurücksetzen. */
    suspend fun resetHints() = edit {
        it.remove(Keys.PREMIUM_TOP_HINT)
        it.remove(Keys.PREMIUM_BOTTOM_HINT)
        it.remove(Keys.CELEBRATION_SHOWN)
        it.remove(Keys.STROBE_WARNING)
    }
}

internal inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
