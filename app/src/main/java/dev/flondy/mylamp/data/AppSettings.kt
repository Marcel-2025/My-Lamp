package dev.flondy.mylamp.data

import dev.flondy.mylamp.sensor.ShakeSensitivity
import dev.flondy.mylamp.torch.LightPatterns

/** Was beim App-Start passieren soll. */
enum class StartBehavior { OFF, TORCH_ON, SCREEN_LIGHT }

/** Welche Lichtquelle der große Power-Button schaltet. */
enum class DefaultLightSource { LED, SCREEN }

/** Farbvoreinstellungen für das Bildschirmlicht. */
enum class ScreenLightColor(val argb: Long) {
    NEUTRAL(0xFFFFFFFF),
    WARM(0xFFFFE3BF),
    COOL(0xFFE3EEFF),
    AMBER(0xFFFFB547),
    RED(0xFFFF3B30),
}

/** Visuelle Designs. Premium-Designs sind für Käufer von `remove_ads` freigeschaltet. */
enum class LampThemeId(val isPremium: Boolean) {
    CLASSIC(false),
    MIDNIGHT(false),
    MISTY_FOREST(true),
    FOREST_ROAD(true),
    GLADE(true),
    GOLDEN_HOUR(true),
}

data class AppSettings(
    // Allgemein
    val fullscreen: Boolean = false,
    val showNavigationBar: Boolean = true,
    val animationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val theme: LampThemeId = LampThemeId.CLASSIC,
    // Taschenlampe
    val defaultLightSource: DefaultLightSource = DefaultLightSource.LED,
    val startBehavior: StartBehavior = StartBehavior.OFF,
    val strobeHz: Float = LightPatterns.STROBE_DEFAULT_HZ,
    val torchStrength: Float = 1f,
    val shakeEnabled: Boolean = false,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
    // Bildschirmlicht
    val screenLightColor: ScreenLightColor = ScreenLightColor.NEUTRAL,
    val screenLightBrightness: Float = 1f,
    // Premium-Hinweise
    val showPremiumTopHint: Boolean = true,
    val showPremiumBottomHint: Boolean = true,
    val premiumCelebrationShown: Boolean = false,
    // Sicherheit
    val strobeWarningAccepted: Boolean = false,
)
