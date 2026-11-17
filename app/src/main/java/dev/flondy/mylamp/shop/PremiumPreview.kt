package dev.flondy.mylamp.shop

import dev.flondy.mylamp.data.ButtonSkin
import dev.flondy.mylamp.data.LampThemeId
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.data.ScreenLightColor

/**
 * Zeitlich begrenzte Vorschau eines Premium-Inhalts. Es wird nichts gespeichert:
 * Nach Ablauf zeigt die App automatisch wieder die eigene Auswahl.
 */
data class PremiumPreview(val item: ShopItem, val endsAtMs: Long) {
    fun remainingMs(nowMs: Long): Long = (endsAtMs - nowMs).coerceAtLeast(0L)
    fun isActive(nowMs: Long): Boolean = nowMs < endsAtMs

    companion object {
        const val VISUAL_PREVIEW_MS = 30_000L
        const val LED_PREVIEW_MS = 10_000L

        fun durationFor(item: ShopItem): Long =
            if (item is ShopItem.Effect) LED_PREVIEW_MS else VISUAL_PREVIEW_MS
    }
}

/** Ermittelt, was gerade wirklich angezeigt wird (eigene Auswahl, Vorschau, Rückfall nach Rückerstattung). */
object EffectiveLook {

    fun theme(selected: LampThemeId, isPremium: Boolean, preview: PremiumPreview?, nowMs: Long): LampThemeId {
        val p = preview?.takeIf { it.isActive(nowMs) }?.item
        if (p is ShopItem.Theme) return p.id
        return if (!selected.isPremium || isPremium) selected else LampThemeId.CLASSIC
    }

    fun skin(selected: ButtonSkin, isPremium: Boolean, preview: PremiumPreview?, nowMs: Long): ButtonSkin {
        val p = preview?.takeIf { it.isActive(nowMs) }?.item
        if (p is ShopItem.Skin) return p.skin
        return if (!selected.isPremium || isPremium) selected else ButtonSkin.GOLD
    }

    fun screenColor(selected: ScreenLightColor, isPremium: Boolean, preview: PremiumPreview?, nowMs: Long): ScreenLightColor {
        val p = preview?.takeIf { it.isActive(nowMs) }?.item
        if (p is ShopItem.ScreenColor) return p.color
        return if (!selected.isPremium || isPremium) selected else ScreenLightColor.NEUTRAL
    }

    fun screenEffect(selected: ScreenEffect, isPremium: Boolean, preview: PremiumPreview?, nowMs: Long): ScreenEffect {
        val p = preview?.takeIf { it.isActive(nowMs) }?.item
        if (p is ShopItem.ScreenFx) return p.effect
        // Eine Farb-Vorschau zeigt die Farbe ruhig, ohne Effekt darüber
        if (p is ShopItem.ScreenColor) return ScreenEffect.NONE
        return if (!selected.isPremium || isPremium) selected else ScreenEffect.NONE
    }
}
