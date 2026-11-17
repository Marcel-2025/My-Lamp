package dev.flondy.mylamp.shop

import dev.flondy.mylamp.data.ButtonSkin
import dev.flondy.mylamp.data.LampThemeId
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.data.ScreenLightColor
import dev.flondy.mylamp.torch.LightEffect

/** Kategorien im „Studio & Shop“. */
enum class ShopCategory { THEMES, SKINS, LIGHT_EFFECTS, SCREEN }

/**
 * Ein Inhalt im Shop. Alle Premium-Inhalte werden gemeinsam mit dem Einmalkauf `remove_ads`
 * freigeschaltet – es gibt keine Einzelkäufe und keine Zufallsinhalte.
 */
sealed interface ShopItem {
    val category: ShopCategory
    val isPremium: Boolean
    val key: String

    data class Theme(val id: LampThemeId) : ShopItem {
        override val category get() = ShopCategory.THEMES
        override val isPremium get() = id.isPremium
        override val key get() = "theme:${id.name}"
    }

    data class Skin(val skin: ButtonSkin) : ShopItem {
        override val category get() = ShopCategory.SKINS
        override val isPremium get() = skin.isPremium
        override val key get() = "skin:${skin.name}"
    }

    data class Effect(val effect: LightEffect) : ShopItem {
        override val category get() = ShopCategory.LIGHT_EFFECTS
        override val isPremium get() = effect.isPremium
        override val key get() = "effect:${effect.name}"
    }

    data class ScreenColor(val color: ScreenLightColor) : ShopItem {
        override val category get() = ShopCategory.SCREEN
        override val isPremium get() = color.isPremium
        override val key get() = "color:${color.name}"
    }

    data class ScreenFx(val effect: ScreenEffect) : ShopItem {
        override val category get() = ShopCategory.SCREEN
        override val isPremium get() = effect.isPremium
        override val key get() = "screenfx:${effect.name}"
    }
}

object ShopCatalog {

    val items: List<ShopItem> = buildList {
        LampThemeId.entries.forEach { add(ShopItem.Theme(it)) }
        ButtonSkin.entries.forEach { add(ShopItem.Skin(it)) }
        LightEffect.entries.forEach { add(ShopItem.Effect(it)) }
        ScreenLightColor.entries.filter { it.isPremium }.forEach { add(ShopItem.ScreenColor(it)) }
        ScreenEffect.entries.filter { it != ScreenEffect.NONE }.forEach { add(ShopItem.ScreenFx(it)) }
    }

    fun inCategory(category: ShopCategory): List<ShopItem> = items.filter { it.category == category }

    fun premiumCount(category: ShopCategory): Int = inCategory(category).count { it.isPremium }

    val premiumTotal: Int get() = items.count { it.isPremium }

    fun isUnlocked(item: ShopItem, isPremium: Boolean): Boolean = !item.isPremium || isPremium
}
