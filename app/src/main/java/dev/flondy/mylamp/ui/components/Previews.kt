package dev.flondy.mylamp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.core.ScreenLightLogic
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.torch.LightStep
import dev.flondy.mylamp.ui.theme.LampColors
import dev.flondy.mylamp.ui.theme.LampVisual
import dev.flondy.mylamp.ui.theme.ProceduralBackground
import dev.flondy.mylamp.ui.theme.SkinStyle
import kotlinx.coroutines.delay

/** Leuchtpunkt, der ein LED-Muster im echten Takt zeigt (nur Anzeige, schaltet keine LED). */
@Composable
fun RhythmDot(steps: List<LightStep>, color: Color, animate: Boolean, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    var on by remember(steps) { mutableStateOf(true) }
    LaunchedEffect(steps, animate) {
        if (!animate) {
            on = true
            return@LaunchedEffect
        }
        while (true) {
            for (step in steps) {
                on = step.on
                delay(step.durationMs)
            }
        }
    }
    Box(modifier.size(size * 2), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (on) {
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent)),
                    radius = this.size.minDimension / 2,
                )
            }
            drawCircle(if (on) color else color.copy(alpha = 0.18f), radius = size.toPx() / 2)
        }
    }
}

/** Kleine Design-Vorschau: Foto-Thumbnail oder gezeichnetes, laufendes Design. */
@Composable
fun ThemePreview(visual: LampVisual, animate: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        when {
            visual.procedural != null -> ProceduralBackground(visual.procedural, Modifier.fillMaxSize(), animate)
            visual.thumb != null -> Image(painterResource(visual.thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> Box(Modifier.fillMaxSize().background(LampColors.NightGradient))
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .size(26.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(LampColors.Gold, LampColors.GoldWarm)))
        )
    }
}

/** Mini-Power-Button im jeweiligen Skin (leuchtend). */
@Composable
fun SkinPreview(skin: SkinStyle, animate: Boolean, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val rotation = if (skin.ring.isNotEmpty() && animate) {
        rememberInfiniteTransition(label = "skinRing").animateFloat(
            0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "skinRot",
        )
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val glow = if (animate) {
        rememberInfiniteTransition(label = "skinGlow").animateFloat(
            0.55f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "skinGlowValue",
        )
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0.8f) }
    Box(modifier.size(size * 1.6f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val r = size.toPx() / 2
            drawCircle(
                Brush.radialGradient(listOf(skin.accent.copy(alpha = 0.5f * glow.value), Color.Transparent), c, r * 1.6f),
                radius = r * 1.6f, center = c,
            )
            drawCircle(
                Brush.radialGradient(listOf(skin.discTop, skin.accent), Offset(c.x - r * 0.2f, c.y - r * 0.35f), r * 1.7f),
                radius = r, center = c,
            )
            if (skin.ring.isNotEmpty()) {
                rotate(rotation.value, c) {
                    drawCircle(Brush.sweepGradient(skin.ring, c), radius = r + 4.dp.toPx(), center = c, style = Stroke(2.5.dp.toPx()))
                }
            } else {
                drawCircle(skin.accent.copy(alpha = 0.7f), radius = r + 4.dp.toPx(), center = c, style = Stroke(1.dp.toPx()))
            }
        }
        Icon(painterResource(R.drawable.ic_power), null, tint = LampColors.Night, modifier = Modifier.size(size * 0.36f))
    }
}

/** Bildschirmlicht-Vorschau mit laufendem Effekt (gleiche Logik wie das echte Bildschirmlicht). */
@Composable
fun ScreenLightPreview(base: Int, effect: ScreenEffect, animate: Boolean, modifier: Modifier = Modifier) {
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(effect, animate) {
        if (effect == ScreenEffect.NONE || !animate) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { t = (it - start).toInt() }
        }
    }
    val color = Color(ScreenLightLogic.colorAt(effect, base, t.toLong()))
    Box(modifier.background(color))
}
