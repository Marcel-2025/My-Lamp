package dev.flondy.mylamp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion

/**
 * Großer, animierter Power-Button.
 * - Weicher Farbwechsel, Glow und Druck-Skalierung
 * - Dezenter Puls NUR bei eingeschaltetem Licht und aktivierten Animationen
 * - Alle animierten Werte werden erst in der Draw-Phase gelesen → keine unnötigen Recompositions
 */
@Composable
fun PowerButton(
    isOn: Boolean,
    accent: Color,
    isPremium: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    stateText: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale = animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "powerScale",
    )
    val glow = animateFloatAsState(
        targetValue = if (isOn) 1f else if (isPremium) 0.22f else 0f,
        animationSpec = motion.spec(tween(700, easing = FastOutSlowInEasing)),
        label = "powerGlow",
    )
    val accentColor by animateColorAsState(accent, motion.spec(tween(500)), label = "accent")
    val discTop by animateColorAsState(
        if (isOn) Color(0xFFFFFBD6) else Color(0xFF1B2036),
        motion.spec(tween(450)), label = "discTop",
    )
    val discBottom by animateColorAsState(
        if (isOn) accent else LampColors.NightRaised,
        motion.spec(tween(450)), label = "discBottom",
    )
    val iconTint by animateColorAsState(
        when {
            isOn -> LampColors.Night
            isPremium -> LampColors.Gold.copy(alpha = 0.85f)
            else -> LampColors.TextMuted
        },
        motion.spec(tween(350)), label = "iconTint",
    )

    val pulse: State<Float> = if (isOn && motion.enabled) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
            label = "pulseValue",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }
    val pulseActive by rememberUpdatedState(isOn && motion.enabled)

    Box(
        modifier = modifier.size(size * 1.45f),
        contentAlignment = Alignment.Center,
    ) {
        // Glow + Puls-Ring (Draw-Phase)
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = size.toPx() / 2
            val g = glow.value
            if (g > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.55f * g), accentColor.copy(alpha = 0.12f * g), Color.Transparent),
                        center = center,
                        radius = baseRadius * 1.45f,
                    ),
                    radius = baseRadius * 1.45f,
                    center = center,
                )
            }
            if (pulseActive) {
                val p = pulse.value
                drawCircle(
                    color = accentColor.copy(alpha = (1f - p) * 0.45f),
                    radius = baseRadius * (1.02f + 0.35f * p),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            // Feiner Außenring
            drawCircle(
                color = if (g > 0.5f) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.10f),
                radius = baseRadius + 6.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(CircleShape)
                .semantics { stateDescription = stateText }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Switch,
                    onClickLabel = contentDescription,
                ) {
                    haptics.toggle(!isOn)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(discTop, discBottom),
                        center = Offset(this.size.width * 0.4f, this.size.height * 0.32f),
                        radius = this.size.minDimension * 0.85f,
                    )
                )
                // Lichtreflexion oben
                drawCircle(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (isOn) 0.35f else 0.07f), Color.Transparent),
                        startY = 0f,
                        endY = this.size.height * 0.55f,
                    ),
                    radius = this.size.minDimension / 2 - 3.dp.toPx(),
                )
                drawCircle(
                    color = Color.White.copy(alpha = if (isOn) 0.45f else 0.08f),
                    style = Stroke(width = 1.dp.toPx()),
                    radius = this.size.minDimension / 2 - 0.5.dp.toPx(),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_power),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.34f),
            )
        }
    }
}
