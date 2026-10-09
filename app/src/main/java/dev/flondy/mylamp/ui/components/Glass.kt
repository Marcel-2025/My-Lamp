package dev.flondy.mylamp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion

/** Halbtransparente „Glas“-Fläche mit feiner Kontur. */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    color: Color = LampColors.Glass,
    borderColor: Color = LampColors.Outline,
): Modifier = this
    .clip(shape)
    .background(color)
    .border(1.dp, borderColor, shape)

/** Dezente Skalierung beim Drücken (Microinteraction). */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.95f): Modifier {
    val motion = LocalMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && motion.enabled) pressed else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun GlassIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LampColors.Text,
    highlighted: Boolean = false,
    size: Dp = 46.dp,
) {
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val border by animateColorAsState(
        if (highlighted) LampColors.Gold.copy(alpha = 0.55f) else LampColors.Outline,
        animationSpec = LocalMotion.current.spec(tween(300)),
        label = "iconBorder",
    )
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction, 0.9f)
            .glass(CircleShape, borderColor = border)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(bounded = true),
                role = Role.Button,
                onClickLabel = contentDescription,
            ) {
                haptics.click()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painter, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Karte für Einstellungen / Paywall. */
@Composable
fun LampCard(
    modifier: Modifier = Modifier,
    borderColor: Color = LampColors.Outline,
    background: Color = LampColors.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .glass(RoundedCornerShape(24.dp), color = background, borderColor = borderColor)
            .padding(vertical = 6.dp),
        content = content,
    )
}
