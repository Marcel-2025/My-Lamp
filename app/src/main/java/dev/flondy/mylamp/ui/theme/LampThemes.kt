package dev.flondy.mylamp.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.flondy.mylamp.R
import dev.flondy.mylamp.data.LampThemeId

/**
 * Visuelle Darstellung eines Designs.
 * @param offImage Bild bei ausgeschaltetem Licht (null = reiner Verlauf)
 * @param onImage Bild bei eingeschaltetem Licht (null = [offImage] heller dargestellt)
 */
data class LampVisual(
    val id: LampThemeId,
    @param:StringRes val nameRes: Int,
    @param:DrawableRes val offImage: Int?,
    @param:DrawableRes val onImage: Int?,
    /** Abdunklung bei ausgeschaltetem Licht (0..1). */
    val offScrim: Float,
    /** Abdunklung bei eingeschaltetem Licht (0..1). */
    val onScrim: Float,
    /** Kleine Vorschau für die Design-Auswahl (spart Speicher). */
    @param:DrawableRes val thumb: Int?,
)

object LampThemes {
    val all: List<LampVisual> = listOf(
        LampVisual(LampThemeId.CLASSIC, R.string.theme_classic, R.drawable.bg_classic_off, R.drawable.bg_classic_on, 0.35f, 0.15f, R.drawable.thumb_classic),
        LampVisual(LampThemeId.MIDNIGHT, R.string.theme_midnight, null, null, 0f, 0f, null),
        LampVisual(LampThemeId.MISTY_FOREST, R.string.theme_misty_forest, R.drawable.bg_misty_forest, null, 0.6f, 0.2f, R.drawable.thumb_misty_forest),
        LampVisual(LampThemeId.FOREST_ROAD, R.string.theme_forest_road, R.drawable.bg_forest_road, null, 0.6f, 0.2f, R.drawable.thumb_forest_road),
        LampVisual(LampThemeId.GLADE, R.string.theme_glade, R.drawable.bg_glade, null, 0.6f, 0.2f, R.drawable.thumb_glade),
        LampVisual(LampThemeId.GOLDEN_HOUR, R.string.theme_golden_hour, R.drawable.bg_golden_hour, null, 0.62f, 0.18f, R.drawable.thumb_golden_hour),
    )

    fun of(id: LampThemeId): LampVisual = all.first { it.id == id }
}
