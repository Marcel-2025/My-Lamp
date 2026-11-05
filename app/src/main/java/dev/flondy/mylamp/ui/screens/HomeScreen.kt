package dev.flondy.mylamp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.flondy.mylamp.ui.components.TileSize
import dev.flondy.mylamp.R
import dev.flondy.mylamp.ads.AdaptiveAdBanner
import dev.flondy.mylamp.data.LampThemeId
import dev.flondy.mylamp.torch.LightMode
import dev.flondy.mylamp.ui.MainUiState
import dev.flondy.mylamp.ui.components.GlassIconButton
import dev.flondy.mylamp.ui.components.ModeTile
import dev.flondy.mylamp.ui.components.PowerButton
import dev.flondy.mylamp.ui.components.PremiumHint
import dev.flondy.mylamp.ui.components.StrobeControl
import dev.flondy.mylamp.ui.components.TorchStrengthControl
import dev.flondy.mylamp.ui.components.glass
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LampThemes
import dev.flondy.mylamp.ui.theme.LampVisual
import dev.flondy.mylamp.ui.theme.LocalMotion
import kotlin.math.roundToInt

data class HomeActions(
    val onPower: () -> Unit,
    val onSos: () -> Unit,
    val onStrobe: () -> Unit,
    val onScreenLight: () -> Unit,
    val onSettings: () -> Unit,
    val onPremium: () -> Unit,
    val onStrobeHz: (Float) -> Unit,
    val onStrobeHzDone: () -> Unit,
    val onStrength: (Float) -> Unit,
    val onStrengthDone: () -> Unit,
    val onCloseTopHint: () -> Unit,
    val onCloseBottomHint: () -> Unit,
)

fun accentFor(mode: LightMode): Color = when (mode) {
    LightMode.SOS -> LampColors.Sos
    LightMode.STROBE -> LampColors.Strobe
    else -> LampColors.Gold
}

@Composable
fun HomeScreen(state: MainUiState, adUnitId: String, actions: HomeActions) {
    // Premium-Design nur mit Premium (z. B. nach Rückerstattung zurück auf „Klassisch“)
    val themeId = state.settings.theme.takeIf { !it.isPremium || state.isPremium } ?: LampThemeId.CLASSIC
    val visual = LampThemes.of(themeId)
    Column(Modifier.fillMaxSize().background(LampColors.Night)) {
        val hasBottomArea = (state.showAds && state.ads.canRequestAds) ||
            (state.isPremium && state.settings.showPremiumBottomHint)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            ThemedBackground(visual, state.isLightOn, accentFor(state.mode))
            HomeContent(state, actions, padBottom = !hasBottomArea)
        }
        if (hasBottomArea) BottomArea(state, adUnitId, actions)
    }
}

@Composable
private fun ThemedBackground(visual: LampVisual, isOn: Boolean, accent: Color) {
    val motion = LocalMotion.current
    val scrim by animateFloatAsState(
        if (isOn) visual.onScrim else visual.offScrim,
        motion.spec(tween(900)), label = "scrim",
    )
    val glow by animateFloatAsState(if (isOn) 1f else 0f, motion.spec(tween(900)), label = "bgGlow")

    Box(Modifier.fillMaxSize()) {
        if (visual.offImage != null) {
            val image = if (isOn && visual.onImage != null) visual.onImage else visual.offImage
            Crossfade(targetState = image, animationSpec = motion.spec(tween(900)), label = "bgImage") { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Box(Modifier.fillMaxSize().background(LampColors.NightGradient))
        }
        // Lesbarkeit + Lichtstimmung (in der Draw-Phase animiert)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = scrim))
            drawRect(
                Brush.verticalGradient(
                    0f to LampColors.Night.copy(alpha = 0.55f),
                    0.25f to Color.Transparent,
                    0.6f to Color.Transparent,
                    1f to LampColors.Night.copy(alpha = 0.9f),
                )
            )
            if (glow > 0.01f) {
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.22f * glow), Color.Transparent),
                        // Im Querformat sitzt der Power-Button in der linken Hälfte
                        center = if (size.width > size.height) Offset(size.width * 0.27f, size.height * 0.55f)
                        else Offset(size.width / 2, size.height * 0.42f),
                        radius = size.maxDimension * (if (size.width > size.height) 0.45f else 0.6f),
                    )
                )
            }
        }
    }
}

/** Layout-Klassen nach verfügbarer Fläche (nicht nach Gerätetyp). */
enum class HomeLayout { PHONE_PORTRAIT, PHONE_LANDSCAPE, TABLET_PORTRAIT, TABLET_LANDSCAPE }

fun homeLayoutFor(width: Dp, height: Dp): HomeLayout {
    val landscape = width > height
    val tablet = minOf(width.value, height.value) >= 600f || (landscape && width >= 840.dp && height >= 480.dp)
    return when {
        tablet && landscape -> HomeLayout.TABLET_LANDSCAPE
        tablet -> HomeLayout.TABLET_PORTRAIT
        landscape -> HomeLayout.PHONE_LANDSCAPE
        else -> HomeLayout.PHONE_PORTRAIT
    }
}

@Composable
private fun HomeContent(state: MainUiState, actions: HomeActions, padBottom: Boolean) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                if (padBottom) WindowInsets.safeDrawing
                else WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        val layout = homeLayoutFor(maxWidth, maxHeight)
        val tablet = layout == HomeLayout.TABLET_PORTRAIT || layout == HomeLayout.TABLET_LANDSCAPE
        val sidePadding = when {
            tablet -> 40.dp
            layout == HomeLayout.PHONE_LANDSCAPE -> 24.dp
            else -> 20.dp
        }
        val availableHeight = maxHeight
        val availableWidth = maxWidth - sidePadding * 2

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = sidePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val landscape = layout == HomeLayout.PHONE_LANDSCAPE || layout == HomeLayout.TABLET_LANDSCAPE
            // Im Querformat sitzt der Premium-Hinweis in der Kopfzeile und kostet so keine Höhe.
            TopBar(state, actions, inlineHint = landscape)
            if (!landscape) {
                PremiumHint(
                    visible = state.isPremium && state.settings.showPremiumTopHint,
                    title = stringResource(R.string.premium_hint_top_title),
                    subtitle = stringResource(R.string.premium_hint_top_subtitle),
                    onClose = actions.onCloseTopHint,
                    modifier = Modifier.padding(top = 8.dp).widthIn(max = 560.dp),
                )
            }

            when (layout) {
                HomeLayout.PHONE_PORTRAIT, HomeLayout.TABLET_PORTRAIT -> {
                    val maxButton = if (tablet) 320.dp else 230.dp
                    val buttonSize = min(min(availableWidth * 0.55f, availableHeight * 0.28f), maxButton).coerceAtLeast(120.dp)
                    val contentWidth = if (tablet) 640.dp else 520.dp
                    Spacer(Modifier.weight(1f))
                    PowerSection(state, actions, buttonSize, strengthWidth = if (tablet) 480.dp else 420.dp)
                    Spacer(Modifier.weight(1f))
                    StrobeSection(state, actions, Modifier.padding(bottom = 12.dp).widthIn(max = contentWidth).fillMaxWidth())
                    ModeTilesRow(
                        state, actions,
                        size = if (tablet) TileSize.Large else TileSize.Regular,
                        modifier = Modifier.widthIn(max = contentWidth).fillMaxWidth(),
                        spacing = if (tablet) 16.dp else 12.dp,
                    )
                    Spacer(Modifier.height(if (tablet) 32.dp else if (availableHeight < 560.dp) 12.dp else 20.dp))
                }

                HomeLayout.PHONE_LANDSCAPE, HomeLayout.TABLET_LANDSCAPE -> {
                    // Zwei Spalten: links das Licht, rechts die Modi – nutzt die Breite statt zu scrollen.
                    val paneWidth = availableWidth / 2
                    val maxButton = if (tablet) 320.dp else 200.dp
                    val buttonSize = min(min(availableHeight * 0.42f, paneWidth * 0.55f), maxButton).coerceAtLeast(96.dp)
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (tablet) 40.dp else 20.dp),
                    ) {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            PowerSection(state, actions, buttonSize, strengthWidth = if (tablet) 420.dp else 300.dp)
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val paneContent = Modifier.widthIn(max = if (tablet) 520.dp else 420.dp).fillMaxWidth()
                            StrobeSection(state, actions, paneContent.padding(bottom = if (tablet) 16.dp else 10.dp), compact = !tablet)
                            ModeTilesRow(
                                state, actions,
                                size = if (tablet) TileSize.Large else TileSize.Compact,
                                modifier = paneContent,
                                spacing = if (tablet) 16.dp else 10.dp,
                            )
                        }
                    }
                    Spacer(Modifier.height(if (tablet) 24.dp else 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PowerSection(state: MainUiState, actions: HomeActions, buttonSize: Dp, strengthWidth: Dp) {
    val motion = LocalMotion.current
    val accent = accentFor(state.mode)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val statusText = when (state.mode) {
            LightMode.OFF -> stringResource(R.string.status_off)
            LightMode.TORCH -> stringResource(R.string.status_on)
            LightMode.SOS -> stringResource(R.string.status_sos)
            LightMode.STROBE -> stringResource(R.string.status_strobe, state.strobeHz.roundToInt())
        }
        PowerButton(
            isOn = state.isLightOn,
            accent = accent,
            isPremium = state.isPremium,
            onClick = actions.onPower,
            contentDescription = stringResource(R.string.cd_power),
            stateText = statusText,
            size = buttonSize,
        )
        AnimatedContent(
            targetState = statusText,
            transitionSpec = { fadeIn(motion.spec(tween(250))) togetherWith fadeOut(motion.spec(tween(200))) },
            label = "status",
        ) { text ->
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isLightOn) accent else LampColors.TextMuted,
            )
        }
        if (!state.hasFlash) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.no_flash_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LampColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = strengthWidth),
            )
        }
        AnimatedVisibility(
            visible = state.mode == LightMode.TORCH && state.supportsStrength,
            enter = fadeIn(motion.spec(tween(300))) + expandVertically(motion.spec(tween(300))),
            exit = fadeOut(motion.spec(tween(200))) + shrinkVertically(motion.spec(tween(200))),
        ) {
            TorchStrengthControl(
                value = state.torchStrength,
                onChange = actions.onStrength,
                onChangeFinished = actions.onStrengthDone,
                modifier = Modifier.padding(top = 16.dp).widthIn(max = strengthWidth).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StrobeSection(state: MainUiState, actions: HomeActions, modifier: Modifier, compact: Boolean = false) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = state.mode == LightMode.STROBE,
        enter = fadeIn(motion.spec(tween(300))) + expandVertically(motion.spec(tween(300))),
        exit = fadeOut(motion.spec(tween(200))) + shrinkVertically(motion.spec(tween(200))),
    ) {
        StrobeControl(
            hz = state.strobeHz,
            onChange = actions.onStrobeHz,
            onChangeFinished = actions.onStrobeHzDone,
            modifier = modifier,
            compact = compact,
        )
    }
}

@Composable
private fun ModeTilesRow(state: MainUiState, actions: HomeActions, size: TileSize, modifier: Modifier, spacing: Dp) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        ModeTile(
            label = stringResource(R.string.mode_sos),
            painter = painterResource(R.drawable.ic_sos),
            active = state.mode == LightMode.SOS,
            accent = LampColors.Sos,
            onClick = actions.onSos,
            modifier = Modifier.weight(1f),
            size = size,
        )
        ModeTile(
            label = stringResource(R.string.mode_strobe),
            painter = painterResource(R.drawable.ic_strobe),
            active = state.mode == LightMode.STROBE,
            accent = LampColors.Strobe,
            onClick = actions.onStrobe,
            modifier = Modifier.weight(1f),
            size = size,
        )
        ModeTile(
            label = stringResource(R.string.mode_screen),
            painter = painterResource(R.drawable.ic_phone_android),
            active = false,
            accent = LampColors.Gold,
            onClick = actions.onScreenLight,
            modifier = Modifier.weight(1f),
            size = size,
        )
    }
}

@Composable
private fun TopBar(state: MainUiState, actions: HomeActions, inlineHint: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(if (inlineHint) 0.6f else 1f)) {
            Text(
                stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = LampColors.Text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                if (state.isPremium) stringResource(R.string.badge_premium) else stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.isPremium) LampColors.Gold else LampColors.TextFaint,
            )
        }
        if (inlineHint) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PremiumHint(
                    visible = state.isPremium && state.settings.showPremiumTopHint,
                    title = stringResource(R.string.premium_hint_top_title),
                    subtitle = stringResource(R.string.premium_hint_top_subtitle),
                    onClose = actions.onCloseTopHint,
                    modifier = Modifier.widthIn(max = 460.dp),
                )
            }
            Box(Modifier.weight(0.6f), contentAlignment = Alignment.CenterEnd) {
                TopBarActions(state, actions)
            }
        } else {
            TopBarActions(state, actions)
        }
    }
}

@Composable
private fun TopBarActions(state: MainUiState, actions: HomeActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!state.isPremium) {
            Row(
                modifier = Modifier
                    .glass(RoundedCornerShape(50), borderColor = LampColors.Gold.copy(alpha = 0.45f))
                    .clickable(onClick = actions.onPremium)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_crown), null, tint = LampColors.Gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.action_go_premium), style = MaterialTheme.typography.labelMedium, color = LampColors.Gold)
            }
            Spacer(Modifier.size(10.dp))
        }
        GlassIconButton(
            painter = rememberVectorPainter(Icons.Filled.Settings),
            contentDescription = stringResource(R.string.cd_settings),
            onClick = actions.onSettings,
        )
    }
}

@Composable
private fun BottomArea(state: MainUiState, adUnitId: String, actions: HomeActions) {
    val navPadding = Modifier.windowInsetsPadding(
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    )
    when {
        state.showAds && state.ads.canRequestAds -> Box(
            Modifier
                .fillMaxWidth()
                .background(LampColors.Night)
                .then(navPadding)
                .padding(top = 6.dp),
        ) {
            AdaptiveAdBanner(adUnitId = adUnitId)
        }
        state.isPremium && state.settings.showPremiumBottomHint -> Box(
            Modifier
                .fillMaxWidth()
                .background(LampColors.Night)
                .then(navPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            PremiumHint(
                visible = true,
                title = stringResource(R.string.premium_hint_bottom),
                subtitle = null,
                onClose = actions.onCloseBottomHint,
                modifier = Modifier.widthIn(max = 520.dp),
            )
        }
        else -> Unit
    }
}
