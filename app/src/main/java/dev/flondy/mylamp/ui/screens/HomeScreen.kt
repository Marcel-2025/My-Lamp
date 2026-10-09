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
                        center = Offset(size.width / 2, size.height * 0.42f),
                        radius = size.maxDimension * 0.6f,
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeContent(state: MainUiState, actions: HomeActions, padBottom: Boolean) {
    val motion = LocalMotion.current
    val accent = accentFor(state.mode)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                if (padBottom) WindowInsets.safeDrawing
                else WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        val buttonSize = min(min(maxWidth * 0.5f, maxHeight * 0.28f), 230.dp).coerceAtLeast(120.dp)
        val compact = maxHeight < 560.dp

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(state, actions)
            PremiumHint(
                visible = state.isPremium && state.settings.showPremiumTopHint,
                title = stringResource(R.string.premium_hint_top_title),
                subtitle = stringResource(R.string.premium_hint_top_subtitle),
                onClose = actions.onCloseTopHint,
                modifier = Modifier.padding(top = 8.dp).widthIn(max = 520.dp),
            )

            Spacer(Modifier.weight(1f))

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
                    modifier = Modifier.padding(top = 16.dp).widthIn(max = 420.dp).fillMaxWidth(),
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = state.mode == LightMode.STROBE,
                enter = fadeIn(motion.spec(tween(300))) + expandVertically(motion.spec(tween(300))),
                exit = fadeOut(motion.spec(tween(200))) + shrinkVertically(motion.spec(tween(200))),
            ) {
                StrobeControl(
                    hz = state.strobeHz,
                    onChange = actions.onStrobeHz,
                    onChangeFinished = actions.onStrobeHzDone,
                    modifier = Modifier.padding(bottom = 12.dp).widthIn(max = 520.dp).fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ModeTile(
                    label = stringResource(R.string.mode_sos),
                    painter = painterResource(R.drawable.ic_sos),
                    active = state.mode == LightMode.SOS,
                    accent = LampColors.Sos,
                    onClick = actions.onSos,
                    modifier = Modifier.weight(1f),
                )
                ModeTile(
                    label = stringResource(R.string.mode_strobe),
                    painter = painterResource(R.drawable.ic_strobe),
                    active = state.mode == LightMode.STROBE,
                    accent = LampColors.Strobe,
                    onClick = actions.onStrobe,
                    modifier = Modifier.weight(1f),
                )
                ModeTile(
                    label = stringResource(R.string.mode_screen),
                    painter = painterResource(R.drawable.ic_phone_android),
                    active = false,
                    accent = LampColors.Gold,
                    onClick = actions.onScreenLight,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
        }
    }
}

@Composable
private fun TopBar(state: MainUiState, actions: HomeActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
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
