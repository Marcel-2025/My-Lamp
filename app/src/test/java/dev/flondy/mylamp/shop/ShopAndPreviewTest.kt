package dev.flondy.mylamp.shop

import dev.flondy.mylamp.data.ButtonSkin
import dev.flondy.mylamp.data.LampThemeId
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.data.ScreenLightColor
import dev.flondy.mylamp.torch.LightEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopAndPreviewTest {

    @Test
    fun catalog_keysAreUnique() {
        val keys = ShopCatalog.items.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun catalog_countsMatchEnums() {
        assertEquals(LampThemeId.entries.count { it.isPremium }, ShopCatalog.premiumCount(ShopCategory.THEMES))
        assertEquals(ButtonSkin.entries.count { it.isPremium }, ShopCatalog.premiumCount(ShopCategory.SKINS))
        assertEquals(LightEffect.entries.count { it.isPremium }, ShopCatalog.premiumCount(ShopCategory.LIGHT_EFFECTS))
        assertEquals(8, ShopCatalog.premiumCount(ShopCategory.THEMES))
        assertEquals(6, ShopCatalog.premiumCount(ShopCategory.SKINS))
        assertEquals(3, ShopCatalog.premiumCount(ShopCategory.LIGHT_EFFECTS))
        // 5 Premium-Farben (inkl. eigener Farbe) + 4 Effekte
        assertEquals(9, ShopCatalog.premiumCount(ShopCategory.SCREEN))
    }

    @Test
    fun catalog_freeItemsAlwaysUnlocked() {
        ShopCatalog.items.filter { !it.isPremium }.forEach { assertTrue(ShopCatalog.isUnlocked(it, isPremium = false)) }
        ShopCatalog.items.filter { it.isPremium }.forEach { assertFalse(ShopCatalog.isUnlocked(it, isPremium = false)) }
        ShopCatalog.items.forEach { assertTrue(ShopCatalog.isUnlocked(it, isPremium = true)) }
    }

    @Test
    fun preview_themeShownOnlyWhileActive() {
        val p = PremiumPreview(ShopItem.Theme(LampThemeId.AURORA), endsAtMs = 1_000)
        assertEquals(LampThemeId.AURORA, EffectiveLook.theme(LampThemeId.CLASSIC, false, p, nowMs = 999))
        assertEquals(LampThemeId.CLASSIC, EffectiveLook.theme(LampThemeId.CLASSIC, false, p, nowMs = 1_000))
    }

    @Test
    fun premiumSelectionFallsBackWithoutPremium() {
        // z. B. nach einer Rückerstattung
        assertEquals(LampThemeId.CLASSIC, EffectiveLook.theme(LampThemeId.EMBER, false, null, 0))
        assertEquals(LampThemeId.EMBER, EffectiveLook.theme(LampThemeId.EMBER, true, null, 0))
        assertEquals(ButtonSkin.GOLD, EffectiveLook.skin(ButtonSkin.NEON, false, null, 0))
        assertEquals(ScreenLightColor.NEUTRAL, EffectiveLook.screenColor(ScreenLightColor.VIOLET, false, null, 0))
        assertEquals(ScreenEffect.NONE, EffectiveLook.screenEffect(ScreenEffect.PARTY, false, null, 0))
    }

    @Test
    fun preview_skinDoesNotChangeTheme() {
        val p = PremiumPreview(ShopItem.Skin(ButtonSkin.LAVA), endsAtMs = 5_000)
        assertEquals(ButtonSkin.LAVA, EffectiveLook.skin(ButtonSkin.GOLD, false, p, 0))
        assertEquals(LampThemeId.MIDNIGHT, EffectiveLook.theme(LampThemeId.MIDNIGHT, false, p, 0))
    }

    @Test
    fun preview_colorShowsWithoutEffect() {
        val p = PremiumPreview(ShopItem.ScreenColor(ScreenLightColor.CYAN), endsAtMs = 5_000)
        assertEquals(ScreenLightColor.CYAN, EffectiveLook.screenColor(ScreenLightColor.WARM, true, p, 0))
        assertEquals(ScreenEffect.NONE, EffectiveLook.screenEffect(ScreenEffect.RAINBOW, true, p, 0))
    }

    @Test
    fun preview_durations() {
        assertEquals(10_000L, PremiumPreview.durationFor(ShopItem.Effect(LightEffect.BEACON)))
        assertEquals(30_000L, PremiumPreview.durationFor(ShopItem.Theme(LampThemeId.OCEAN)))
        assertEquals(4_000L, PremiumPreview(ShopItem.Skin(ButtonSkin.ROSE), 10_000).remainingMs(6_000))
        assertEquals(0L, PremiumPreview(ShopItem.Skin(ButtonSkin.ROSE), 10_000).remainingMs(12_000))
    }
}
