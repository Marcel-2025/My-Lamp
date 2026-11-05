package dev.flondy.mylamp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.torch.LightPatterns
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion
import kotlin.math.roundToInt

/** Kachel für einen Lichtmodus (SOS, Strobe, Bildschirm). */
@Composable
fun ModeTile(
    label: String,
    painter: Painter,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.Regular,
) {
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        if (active) accent.copy(alpha = 0.22f).compositeOver(LampColors.Night.copy(alpha = 0.55f)) else LampColors.Glass,
        motion.spec(tween(350)), label = "tileBg",
    )
    val border by animateColorAsState(
        if (active) accent.copy(alpha = 0.75f) else LampColors.Outline,
        motion.spec(tween(350)), label = "tileBorder",
    )
    val iconBg by animateColorAsState(
        if (active) accent else Color.White.copy(alpha = 0.07f),
        motion.spec(tween(350)), label = "iconBg",
    )
    val iconTint by animateColorAsState(
        if (active) LampColors.Night else LampColors.Text,
        motion.spec(tween(350)), label = "iconTint",
    )

    Column(
        modifier = modifier
            .pressScale(interaction, 0.95f)
            .glass(RoundedCornerShape(22.dp), color = bg, borderColor = border)
            .semantics { selected = active }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptics.click()
                onClick()
            }
            .padding(vertical = size.verticalPadding, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(size.circle)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter, contentDescription = null, tint = iconTint, modifier = Modifier.size(size.icon))
            }
            if (active) ActiveDot(accent, Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(if (size == TileSize.Compact) 4.dp else 8.dp))
        Text(
            text = label,
            style = if (size == TileSize.Large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = if (active) LampColors.Text else LampColors.TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Größenstufen der Moduskacheln: kompakt (Handy quer), normal (Handy), groß (Tablet). */
enum class TileSize(val circle: androidx.compose.ui.unit.Dp, val icon: androidx.compose.ui.unit.Dp, val verticalPadding: androidx.compose.ui.unit.Dp) {
    Compact(38.dp, 20.dp, 8.dp),
    Regular(46.dp, 24.dp, 14.dp),
    Large(58.dp, 28.dp, 18.dp),
}

/** Kleiner pulsierender Statuspunkt (nur solange aktiv). */
@Composable
private fun ActiveDot(color: Color, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val alpha = if (motion.enabled) {
        rememberInfiniteTransition(label = "dot").animateFloat(
            initialValue = 1f, targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "dotAlpha",
        )
    } else remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    Box(
        modifier
            .size(10.dp)
            .graphicsLayer { this.alpha = alpha.value }
            .clip(CircleShape)
            .background(color)
    )
}

/** Stroboskop-Frequenzsteuerung mit direkter Rückmeldung und Sicherheitshinweis. */
@Composable
fun StrobeControl(
    hz: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    /** Kompakt (Handy quer): ohne Balkenanzeige, damit alles ohne Scrollen passt. */
    compact: Boolean = false,
) {
    val haptics = LocalHaptics.current
    val motion = LocalMotion.current
    val rounded = hz.roundToInt()
    val speedLabel = when {
        hz < 4f -> stringResource(R.string.strobe_slow)
        hz < 8f -> stringResource(R.string.strobe_medium)
        else -> stringResource(R.string.strobe_fast)
    }
    Column(
        modifier = modifier
            .glass(RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = if (compact) 8.dp else 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.strobe_frequency),
                style = MaterialTheme.typography.labelLarge,
                color = LampColors.TextMuted,
                modifier = Modifier.weight(1f),
            )
            AnimatedContent(
                targetState = rounded,
                transitionSpec = {
                    if (!motion.enabled) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else if (targetState > initialState) {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    } else {
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    }
                },
                label = "hz",
            ) { value ->
                Text(
                    stringResource(R.string.strobe_hz_value, value),
                    style = MaterialTheme.typography.titleMedium,
                    color = LampColors.Strobe,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(speedLabel, style = MaterialTheme.typography.bodySmall, color = LampColors.TextFaint)
        }
        if (!compact) {
            SpeedMeter(fraction = (hz - LightPatterns.STROBE_MIN_HZ) / (LightPatterns.STROBE_MAX_HZ - LightPatterns.STROBE_MIN_HZ))
        }
        Slider(
            value = hz,
            onValueChange = {
                if (it.roundToInt() != rounded) haptics.tick()
                onChange(it)
            },
            onValueChangeFinished = onChangeFinished,
            valueRange = LightPatterns.STROBE_MIN_HZ..LightPatterns.STROBE_MAX_HZ,
            colors = SliderDefaults.colors(
                thumbColor = LampColors.Strobe,
                activeTrackColor = LampColors.Strobe,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
            ),
        )
        Text(
            stringResource(R.string.strobe_epilepsy_caption),
            style = MaterialTheme.typography.bodySmall,
            color = LampColors.TextFaint,
        )
    }
}

/** Balkenanzeige, die die Geschwindigkeit visualisiert (ohne selbst zu blinken). */
@Composable
private fun SpeedMeter(fraction: Float) {
    val bars = 16
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp)
            .height(14.dp)
    ) {
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val active = i <= (fraction.coerceIn(0f, 1f) * (bars - 1))
            val h = size.height * (0.35f + 0.65f * (i + 1) / bars)
            drawRoundRect(
                color = if (active) LampColors.Strobe.copy(alpha = 0.35f + 0.65f * (i + 1) / bars) else Color.White.copy(alpha = 0.08f),
                topLeft = androidx.compose.ui.geometry.Offset(i * (w + gap), size.height - h),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2, w / 2),
            )
        }
    }
}

/** LED-Helligkeit (Android 13+, nur auf unterstützten Geräten). */
@Composable
fun TorchStrengthControl(
    value: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .glass(RoundedCornerShape(50))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.torch_brightness), style = MaterialTheme.typography.labelMedium, color = LampColors.TextMuted)
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = 0.05f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = LampColors.Gold,
                activeTrackColor = LampColors.Gold,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
            ),
        )
        Text("${(value * 100).roundToInt()} %", style = MaterialTheme.typography.labelMedium, color = LampColors.Gold)
    }
}
