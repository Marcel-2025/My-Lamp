package dev.flondy.mylamp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LocalHaptics
import dev.flondy.mylamp.ui.theme.LocalMotion
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Spark(val angle: Float, val distance: Float, val size: Float, val delay: Float)

/**
 * Einmalige, hochwertige Freischaltanimation nach einem BESTÄTIGTEN Kauf.
 * Läuft einmalig ca. 1,6 s – keine Daueranimation.
 */
@Composable
fun CelebrationOverlay(visible: Boolean, onDismiss: () -> Unit) {
    val motion = LocalMotion.current
    if (visible) BackHandler(onBack = onDismiss)
    AnimatedVisibility(visible = visible, enter = fadeIn(motion.spec(tween(300))), exit = fadeOut(motion.spec(tween(300)))) {
        val haptics = LocalHaptics.current
        val progress = remember { Animatable(if (motion.enabled) 0f else 1f) }
        val crownScale = remember { Animatable(if (motion.enabled) 0.3f else 1f) }
        val sparks = remember {
            val rnd = Random(7)
            List(36) { Spark(rnd.nextFloat() * 6.283f, 0.45f + rnd.nextFloat() * 0.55f, 2f + rnd.nextFloat() * 4f, rnd.nextFloat() * 0.25f) }
        }
        LaunchedEffect(Unit) {
            haptics.success()
            if (motion.enabled) {
                launch { crownScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
                progress.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6050711))
                .clickable(remember { MutableInteractionSource() }, indication = null) { }
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val p = progress.value
                val c = Offset(size.width / 2, size.height * 0.42f)
                val maxR = size.minDimension * 0.75f
                drawCircle(
                    Brush.radialGradient(
                        listOf(LampColors.Gold.copy(alpha = 0.45f * (1f - p * 0.5f)), Color.Transparent),
                        center = c, radius = maxR * (0.4f + 0.6f * p),
                    ),
                    radius = maxR * (0.4f + 0.6f * p), center = c,
                )
                if (p < 1f || !motion.enabled) {
                    sparks.forEach { s ->
                        val local = ((p - s.delay) / (1f - s.delay)).coerceIn(0f, 1f)
                        if (local > 0f) {
                            val d = maxR * s.distance * local
                            drawCircle(
                                color = LampColors.Gold.copy(alpha = (1f - local).coerceIn(0f, 1f)),
                                radius = s.size.dp.toPx() * (1f - local * 0.5f),
                                center = Offset(c.x + cos(s.angle) * d, c.y + sin(s.angle) * d),
                            )
                        }
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()) // Handy quer: wenig Höhe
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            ) {
                Box(
                    Modifier
                        .size(108.dp)
                        .graphicsLayer { scaleX = crownScale.value; scaleY = crownScale.value }
                        .clip(CircleShape)
                        .background(LampColors.GoldGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_crown), null, tint = LampColors.Night, modifier = Modifier.size(54.dp))
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.celebration_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = LampColors.Gold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.celebration_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LampColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = LampColors.Gold, contentColor = LampColors.Night),
                    modifier = Modifier.height(52.dp).widthIn(min = 200.dp),
                ) { Text(stringResource(R.string.celebration_continue), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
