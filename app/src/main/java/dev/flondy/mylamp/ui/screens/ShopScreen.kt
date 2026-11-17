package dev.flondy.mylamp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.core.ScreenLightLogic
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.data.ScreenLightColor
import dev.flondy.mylamp.shop.ShopCatalog
import dev.flondy.mylamp.shop.ShopCategory
import dev.flondy.mylamp.shop.ShopItem
import dev.flondy.mylamp.torch.LightMode
import dev.flondy.mylamp.ui.MainUiState
import dev.flondy.mylamp.ui.components.GlassIconButton
import dev.flondy.mylamp.ui.components.RhythmDot
import dev.flondy.mylamp.ui.components.ScreenLightPreview
import dev.flondy.mylamp.ui.components.SkinPreview
import dev.flondy.mylamp.ui.components.ThemePreview
import dev.flondy.mylamp.ui.components.glass
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LampThemes
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion
import dev.flondy.mylamp.ui.theme.Skins
import kotlinx.coroutines.launch

/**
 * „Studio & Shop“: alle Designs, Button-Skins, Lichteffekte und Bildschirmlicht-Inhalte.
 * - Kostenlose und freigeschaltete Inhalte werden per Tipp ausgewählt.
 * - Gesperrte Premium-Inhalte starten eine Live-Vorschau (nichts wird gespeichert).
 * - Ein Einmalkauf (`remove_ads`) schaltet alles frei – keine Einzelkäufe, keine Zufallsinhalte.
 */
@Composable
fun ShopScreen(
    state: MainUiState,
    onItem: (ShopItem) -> Unit,
    onUnlockAll: () -> Unit,
    onBack: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val categories = ShopCategory.entries
    // Kopfzeile + je Kategorie eine Überschrift → Index der Überschrift für die Sprungmarken
    val headerIndex = remember {
        var index = 1
        categories.associateWith { cat -> index.also { index += 1 + ShopCatalog.inCategory(cat).size } }
    }

    Box(Modifier.fillMaxSize().background(LampColors.NightGradient)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.shop_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = LampColors.Text,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        if (state.isPremium) stringResource(R.string.shop_subtitle_premium) else stringResource(R.string.shop_subtitle_free),
                        style = MaterialTheme.typography.bodySmall,
                        color = LampColors.TextMuted,
                        maxLines = 2,
                    )
                }
            }
            // Sprungmarken zu den Kategorien
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { cat ->
                    Text(
                        stringResource(categoryName(cat)),
                        style = MaterialTheme.typography.labelMedium,
                        color = LampColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .glass(RoundedCornerShape(50))
                            .clickable(role = Role.Tab) { scope.launch { gridState.animateScrollToItem(headerIndex.getValue(cat)) } }
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "intro") {
                    ShopIntro(state)
                }
                categories.forEach { cat ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h_${cat.name}") {
                        CategoryHeader(cat)
                    }
                    itemsIndexed(ShopCatalog.inCategory(cat), key = { _, it -> it.key }) { index, item ->
                        ShopCard(item, state, index, onClick = { onItem(item) })
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "legal") {
                    Text(
                        stringResource(R.string.shop_legal),
                        style = MaterialTheme.typography.bodySmall,
                        color = LampColors.TextFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                }
            }
            if (!state.isPremium) UnlockBar(state, onUnlockAll)
            else Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun ShopIntro(state: MainUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), color = LampColors.Card, borderColor = LampColors.Gold.copy(alpha = if (state.isPremium) 0.5f else 0.25f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(LampColors.GoldGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_crown), null, tint = LampColors.Night, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (state.isPremium) stringResource(R.string.premium_status_active) else stringResource(R.string.shop_premium_count, ShopCatalog.premiumTotal),
                style = MaterialTheme.typography.titleSmall,
                color = if (state.isPremium) LampColors.Gold else LampColors.Text,
            )
            Text(
                stringResource(R.string.shop_intro_text),
                style = MaterialTheme.typography.bodySmall,
                color = LampColors.TextMuted,
            )
        }
    }
}

@Composable
private fun CategoryHeader(cat: ShopCategory) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(categoryName(cat)).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = LampColors.Gold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            stringResource(R.string.shop_premium_count, ShopCatalog.premiumCount(cat)),
            style = MaterialTheme.typography.labelSmall,
            color = LampColors.TextFaint,
        )
    }
}

@Composable
private fun ShopCard(item: ShopItem, state: MainUiState, index: Int, onClick: () -> Unit) {
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val unlocked = ShopCatalog.isUnlocked(item, state.isPremium)
    val active = isActive(item, state)
    // Karten erscheinen nacheinander
    val appear = remember { Animatable(if (motion.enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (motion.enabled) appear.animateTo(1f, tween(420, delayMillis = (index % 8) * 50, easing = FastOutSlowInEasing))
    }
    val border by animateColorAsState(
        when {
            active -> LampColors.Gold
            !unlocked -> LampColors.Gold.copy(alpha = 0.25f)
            else -> LampColors.Outline
        },
        motion.spec(tween(250)), label = "cardBorder",
    )
    Column(
        Modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 24.dp.toPx()
            }
            .clip(RoundedCornerShape(22.dp))
            .background(LampColors.Card)
            .border(if (active) 2.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button) {
                haptics.click()
                onClick()
            },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ItemPreview(item, state, motion.enabled)
            if (!unlocked) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC050711))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Lock, stringResource(R.string.cd_locked), tint = LampColors.Gold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.shop_state_locked), style = MaterialTheme.typography.labelSmall, color = LampColors.Gold)
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                stringResource(shopItemName(item)),
                style = MaterialTheme.typography.titleSmall,
                color = LampColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    active -> {
                        Icon(Icons.Filled.Check, null, tint = LampColors.Gold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.shop_state_active), style = MaterialTheme.typography.labelMedium, color = LampColors.Gold)
                    }
                    !unlocked -> Text(stringResource(R.string.action_preview), style = MaterialTheme.typography.labelMedium, color = LampColors.Gold)
                    item is ShopItem.Effect -> Text(stringResource(R.string.action_start), style = MaterialTheme.typography.labelMedium, color = LampColors.TextMuted)
                    else -> Text(
                        if (item.isPremium) stringResource(R.string.action_select) else stringResource(R.string.shop_state_free),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isPremium) LampColors.TextMuted else LampColors.Success,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemPreview(item: ShopItem, state: MainUiState, animate: Boolean) {
    when (item) {
        is ShopItem.Theme -> ThemePreview(LampThemes.of(item.id), animate, Modifier.fillMaxSize())
        is ShopItem.Skin -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LampColors.NightRaised, LampColors.Night))), contentAlignment = Alignment.Center) {
            SkinPreview(Skins.of(item.skin), animate, size = 56.dp)
        }
        is ShopItem.Effect -> Box(Modifier.fillMaxSize().background(LampColors.Night), contentAlignment = Alignment.Center) {
            RhythmDot(item.effect.steps, Skins.of(state.skin).accent, animate, size = 22.dp)
        }
        is ShopItem.ScreenColor -> {
            if (item.color == ScreenLightColor.CUSTOM) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient((0..6).map { Color(ScreenLightLogic.hsvToArgb(it * 60f, 0.6f, 1f)) })
                    )
                )
            } else {
                ScreenLightPreview(item.color.argb.toInt(), ScreenEffect.NONE, animate, Modifier.fillMaxSize())
            }
        }
        is ShopItem.ScreenFx -> ScreenLightPreview(ScreenLightColor.WARM.argb.toInt(), item.effect, animate, Modifier.fillMaxSize())
    }
}

private fun isActive(item: ShopItem, state: MainUiState): Boolean = when (item) {
    is ShopItem.Theme -> state.theme == item.id
    is ShopItem.Skin -> state.skin == item.skin
    is ShopItem.Effect -> state.mode == LightMode.EFFECT && state.effect == item.effect
    is ShopItem.ScreenColor -> state.screenColor == item.color
    is ShopItem.ScreenFx -> state.screenEffect == item.effect
}

@Composable
private fun UnlockBar(state: MainUiState, onUnlockAll: () -> Unit) {
    val price = state.billing.formattedPrice
    Box(
        Modifier
            .fillMaxWidth()
            .background(LampColors.Night)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        PillButton(
            text = if (price != null) stringResource(R.string.shop_unlock_all, price) else stringResource(R.string.shop_unlock_all_noprice),
            filled = true,
            onClick = onUnlockAll,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        )
    }
}

private fun categoryName(cat: ShopCategory): Int = when (cat) {
    ShopCategory.THEMES -> R.string.shop_cat_themes
    ShopCategory.SKINS -> R.string.shop_cat_skins
    ShopCategory.LIGHT_EFFECTS -> R.string.shop_cat_effects
    ShopCategory.SCREEN -> R.string.shop_cat_screen
}
