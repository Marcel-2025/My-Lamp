package dev.flondy.mylamp.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.flondy.mylamp.R
import dev.flondy.mylamp.data.ButtonSkin

/**
 * Farbwelt eines Power-Button-Skins.
 * @param accent Leucht- und Akzentfarbe bei eingeschaltetem Licht
 * @param discTop helle Mitte der Scheibe bei eingeschaltetem Licht
 * @param ring Farben eines drehenden Rings (nur Polarlicht), sonst leer
 */
@Immutable
data class SkinStyle(
    val skin: ButtonSkin,
    @param:StringRes val nameRes: Int,
    val accent: Color,
    val discTop: Color,
    val ring: List<Color> = emptyList(),
)

object Skins {
    val all: List<SkinStyle> = listOf(
        SkinStyle(ButtonSkin.GOLD, R.string.skin_gold, LampColors.Gold, Color(0xFFFFFBD6)),
        SkinStyle(ButtonSkin.CHROME, R.string.skin_chrome, Color(0xFFD9E2EC), Color(0xFFFFFFFF)),
        SkinStyle(ButtonSkin.NEON, R.string.skin_neon, Color(0xFF39F3FF), Color(0xFFE0FFFF)),
        SkinStyle(ButtonSkin.ROSE, R.string.skin_rose, Color(0xFFFFB3C7), Color(0xFFFFF0F4)),
        SkinStyle(ButtonSkin.EMERALD, R.string.skin_emerald, Color(0xFF3DDC97), Color(0xFFE6FFF3)),
        SkinStyle(ButtonSkin.LAVA, R.string.skin_lava, Color(0xFFFF6A3D), Color(0xFFFFE3C2)),
        SkinStyle(
            ButtonSkin.AURORA, R.string.skin_aurora, Color(0xFF5EF2C2), Color(0xFFE9FFF8),
            ring = listOf(Color(0xFF5EF2C2), Color(0xFF7B8CFF), Color(0xFFE77BFF), Color(0xFF5EF2C2)),
        ),
    )

    fun of(skin: ButtonSkin): SkinStyle = all.first { it.skin == skin }
}
