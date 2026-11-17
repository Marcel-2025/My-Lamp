package dev.flondy.mylamp.data

import dev.flondy.mylamp.sensor.ShakeSensitivity
import dev.flondy.mylamp.torch.LightPatterns

/** Was beim App-Start passieren soll. */
enum class StartBehavior { OFF, TORCH_ON, SCREEN_LIGHT }

/** Welche Lichtquelle der große Power-Button schaltet. */
enum class DefaultLightSource { LED, SCREEN }

/** Farbvoreinstellungen für das Bildschirmlicht. [CUSTOM] nutzt den gespeicherten Farbton. */
enum class ScreenLightColor(val argb: Long, val isPremium: Boolean = false) {
    NEUTRAL(0xFFFFFFFF),
    WARM(0xFFFFE3BF),
    COOL(0xFFE3EEFF),
    AMBER(0xFFFFB547),
    RED(0xFFFF3B30),
    GREEN(0xFF39E27D, isPremium = true),
    VIOLET(0xFFA77BFF, isPremium = true),
    CYAN(0xFF4FE3F0, isPremium = true),
    PINK(0xFFFF7EB6, isPremium = true),
    CUSTOM(0xFFFFFFFF, isPremium = true),
}

/** Bewegte Bildschirmlicht-Effekte – alle langsam, ohne schnelles Flackern. */
enum class ScreenEffect(val isPremium: Boolean) {
    NONE(false),
    RAINBOW(true),
    CANDLE(true),
    PARTY(true),
    WARNING(true),
}

/** Skins für den großen Power-Button. */
enum class ButtonSkin(val isPremium: Boolean) {
    GOLD(false),
    CHROME(true),
    NEON(true),
    ROSE(true),
    EMERALD(true),
    LAVA(true),
    AURORA(true),
}

/** Auto-Aus nach … Minuten (0 = aus). */
val AUTO_OFF_CHOICES = listOf(0, 1, 5, 15, 30, 60)

/** Visuelle Designs. Premium-Designs sind für Käufer von `remove_ads` freigeschaltet. */
enum class LampThemeId(val isPremium: Boolean) {
    CLASSIC(false),
    MIDNIGHT(false),
    MISTY_FOREST(true),
    FOREST_ROAD(true),
    GLADE(true),
    GOLDEN_HOUR(true),
    AURORA(true),
    STARLIGHT(true),
    EMBER(true),
    OCEAN(true),
}

data class AppSettings(
    // Allgemein
    val fullscreen: Boolean = false,
    val showNavigationBar: Boolean = true,
    val animationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val theme: LampThemeId = LampThemeId.CLASSIC,
    val buttonSkin: ButtonSkin = ButtonSkin.GOLD,
    // Taschenlampe
    val defaultLightSource: DefaultLightSource = DefaultLightSource.LED,
    val startBehavior: StartBehavior = StartBehavior.OFF,
    val strobeHz: Float = LightPatterns.STROBE_DEFAULT_HZ,
    val torchStrength: Float = 1f,
    val shakeEnabled: Boolean = false,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
    val volumeKeysToggle: Boolean = false,
    val autoOffMinutes: Int = 0,
    val morseText: String = "SOS",
    // Bildschirmlicht
    val screenLightColor: ScreenLightColor = ScreenLightColor.NEUTRAL,
    val screenLightBrightness: Float = 1f,
    val screenEffect: ScreenEffect = ScreenEffect.NONE,
    /** Farbton 0..360 für [ScreenLightColor.CUSTOM]. */
    val screenCustomHue: Float = 200f,
    // Premium-Hinweise
    val showPremiumTopHint: Boolean = true,
    val showPremiumBottomHint: Boolean = true,
    val premiumCelebrationShown: Boolean = false,
    // Sicherheit
    val strobeWarningAccepted: Boolean = false,
)
