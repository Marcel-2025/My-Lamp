package dev.flondy.mylamp.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Gezeichnete (nicht fotografische) Designs. Sie brauchen keine Bilddateien und sind animiert. */
enum class ProceduralKind { AURORA, STARLIGHT, EMBER, OCEAN }

private const val LOOP_MS = 24_000

/**
 * Zeichnet ein animiertes Design. Ist [animate] aus (Einstellung oder System), wird ein ruhiges Standbild gezeigt.
 * Alle Werte werden erst in der Draw-Phase gelesen – keine Recomposition pro Bild.
 */
@Composable
fun ProceduralBackground(kind: ProceduralKind, modifier: Modifier = Modifier, animate: Boolean = true) {
    val time: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "procedural").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "proceduralTime",
        )
    } else {
        remember { mutableFloatStateOf(0.3f) }
    }
    val stars = remember { seededPoints(90, 7) }
    val embers = remember { seededPoints(42, 23) }
    Canvas(modifier) {
        val t = time.value
        when (kind) {
            ProceduralKind.AURORA -> drawAurora(t)
            ProceduralKind.STARLIGHT -> drawStars(t, stars)
            ProceduralKind.EMBER -> drawEmbers(t, embers)
            ProceduralKind.OCEAN -> drawOcean(t)
        }
    }
}

/** Gleichmäßig verteilte, aber feste Punkte (x, y, Größe, Phase) – kein Zufall zur Laufzeit. */
private fun seededPoints(count: Int, seed: Int): List<FloatArray> {
    var s = seed.toLong()
    fun next(): Float {
        s = (s * 1103515245L + 12345L) and 0x7fffffffL
        return (s % 10_000L) / 10_000f
    }
    return List(count) { floatArrayOf(next(), next(), next(), next()) }
}

private const val TAU = (2 * PI).toFloat()

private fun DrawScope.drawAurora(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF07142A), Color(0xFF050711))))
    val bands = listOf(
        Triple(Color(0xFF5EF2C2), 0.30f, 0f),
        Triple(Color(0xFF7B8CFF), 0.40f, 0.33f),
        Triple(Color(0xFFE77BFF), 0.22f, 0.66f),
    )
    bands.forEachIndexed { i, (color, alpha, phase) ->
        val path = Path()
        val baseY = size.height * (0.22f + 0.1f * i)
        val amp = size.height * 0.06f
        val steps = 24
        path.moveTo(0f, baseY)
        for (k in 0..steps) {
            val x = size.width * k / steps
            val y = baseY + amp * sin(TAU * (k / steps.toFloat() * 1.3f + t + phase)) +
                amp * 0.4f * sin(TAU * (k / steps.toFloat() * 2.7f - t * 2f))
            path.lineTo(x, y)
        }
        path.lineTo(size.width, baseY + size.height * 0.35f)
        path.lineTo(0f, baseY + size.height * 0.35f)
        path.close()
        val pulse = 0.75f + 0.25f * sin(TAU * (t * 2f + phase))
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(color.copy(alpha = alpha * pulse), color.copy(alpha = 0f)),
                startY = baseY - amp, endY = baseY + size.height * 0.35f,
            ),
        )
    }
}

private fun DrawScope.drawStars(t: Float, points: List<FloatArray>) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0C1236), Color(0xFF050711))))
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x337B8CFF), Color.Transparent),
            center = Offset(size.width * 0.7f, size.height * 0.2f),
            radius = size.maxDimension * 0.5f,
        )
    )
    val unit = size.minDimension / 360f
    points.forEach { p ->
        // Ganzzahlige Faktoren → nahtlose Schleife
        val twinkle = 0.35f + 0.65f * abs(sin(TAU * (t * (1 + (p[3] * 3).toInt()) + p[3])))
        drawCircle(
            Color.White.copy(alpha = twinkle * (0.5f + 0.5f * p[2])),
            radius = unit * (0.6f + 1.4f * p[2]),
            center = Offset(p[0] * size.width, p[1] * size.height * 0.85f),
        )
    }
}

private fun DrawScope.drawEmbers(t: Float, points: List<FloatArray>) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF050711), Color(0xFF1D0805))))
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x66FF6A3D), Color.Transparent),
            center = Offset(size.width / 2, size.height * 1.05f),
            radius = size.maxDimension * 0.6f,
        )
    )
    val unit = size.minDimension / 360f
    points.forEach { p ->
        val cycles = 2 + (p[2] * 3).toInt() // ganzzahlig → nahtlose Schleife
        val progress = (p[1] + t * cycles) % 1f // 0 = unten, 1 = oben
        val y = size.height * (1f - progress)
        val x = size.width * p[0] + unit * 12f * sin(TAU * (t * 3f + p[3]))
        val fade = sin(PI.toFloat() * progress)
        drawCircle(
            Color(0xFFFFA24D).copy(alpha = 0.75f * fade),
            radius = unit * (1f + 2f * p[2]),
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawOcean(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF04223D), Color(0xFF020814))))
    // Lichtstrahlen von oben
    for (i in 0..2) {
        val x = size.width * (0.25f + 0.25f * i) + size.width * 0.05f * sin(TAU * (t + i * 0.3f))
        drawRect(
            Brush.radialGradient(
                listOf(Color(0x224FE3F0), Color.Transparent),
                center = Offset(x, 0f),
                radius = size.height * 0.7f,
            )
        )
    }
    val layers = listOf(0.55f to 0x334FE3F0, 0.68f to 0x2639A0FF, 0.8f to 0x40062A4A)
    layers.forEachIndexed { i, (level, argb) ->
        val path = Path()
        val baseY = size.height * level
        val amp = size.height * 0.02f
        val steps = 28
        path.moveTo(0f, baseY)
        for (k in 0..steps) {
            val x = size.width * k / steps
            val y = baseY + amp * sin(TAU * (k / steps.toFloat() * (1.5f + i * 0.5f) + t * (if (i % 2 == 0) 1f else -1f) * 2f))
            path.lineTo(x, y)
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        drawPath(path, Color(argb))
    }
}
