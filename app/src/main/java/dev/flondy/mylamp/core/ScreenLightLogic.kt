package dev.flondy.mylamp.core

import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.data.ScreenLightColor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Reine Farblogik für das Bildschirmlicht (ohne Android-Abhängigkeiten, daher testbar).
 * Alle Effekte sind langsam: Regenbogen 12 s pro Umlauf, Party 1,5 s pro Farbe mit Überblendung,
 * Kerze flackert nur weich in der Helligkeit, Warnlicht blinkt mit 1 Hz.
 */
object ScreenLightLogic {

    const val RAINBOW_PERIOD_MS = 12_000L
    const val PARTY_SLOT_MS = 1_500L
    const val PARTY_FADE_MS = 500L
    const val WARNING_PERIOD_MS = 1_000L

    private const val CANDLE_ARGB = 0xFFFFB070.toInt()
    private const val WARNING_ARGB = 0xFFFFA000.toInt()
    private const val WARNING_DARK_ARGB = 0xFF1A0E00.toInt()

    val PARTY_COLORS = intArrayOf(
        0xFFFF7EB6.toInt(), 0xFFA77BFF.toInt(), 0xFF4FE3F0.toInt(), 0xFF39E27D.toInt(), 0xFFFFB547.toInt(),
    )

    /** Grundfarbe ohne Effekt. */
    fun baseColor(color: ScreenLightColor, customHue: Float): Int =
        if (color == ScreenLightColor.CUSTOM) hsvToArgb(customHue, 0.6f, 1f) else color.argb.toInt()

    /** Farbe des Bildschirmlichts zum Zeitpunkt [tMs] (ab Start des Effekts). */
    fun colorAt(effect: ScreenEffect, base: Int, tMs: Long): Int = when (effect) {
        ScreenEffect.NONE -> base
        ScreenEffect.RAINBOW -> hsvToArgb((tMs % RAINBOW_PERIOD_MS) * 360f / RAINBOW_PERIOD_MS, 0.55f, 1f)
        ScreenEffect.CANDLE -> scale(CANDLE_ARGB, candleLevel(tMs))
        ScreenEffect.PARTY -> {
            val slot = (tMs / PARTY_SLOT_MS).toInt()
            val within = tMs % PARTY_SLOT_MS
            val from = PARTY_COLORS[slot % PARTY_COLORS.size]
            val to = PARTY_COLORS[(slot + 1) % PARTY_COLORS.size]
            val fadeStart = PARTY_SLOT_MS - PARTY_FADE_MS
            if (within < fadeStart) from else lerp(from, to, (within - fadeStart).toFloat() / PARTY_FADE_MS)
        }
        ScreenEffect.WARNING -> if (tMs % WARNING_PERIOD_MS < WARNING_PERIOD_MS / 2) WARNING_ARGB else WARNING_DARK_ARGB
    }

    /** Weiches Kerzenflackern: Helligkeit 0.78..1, Summe dreier langsamer Sinuswellen. */
    fun candleLevel(tMs: Long): Float {
        val t = tMs.toDouble()
        val n = sin(t * 0.0021) + 0.6 * sin(t * 0.0057 + 1.3) + 0.3 * sin(t * 0.0131 + 2.1)
        val normalized = ((n / 1.9) + 1.0) / 2.0 // 0..1
        return (0.78 + 0.22 * normalized).toFloat().coerceIn(0.78f, 1f)
    }

    fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val c = value * saturation
        val x = c * (1 - abs((h / 60f) % 2f - 1))
        val m = value - c
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(255, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    fun lerp(a: Int, b: Int, f: Float): Int {
        val t = f.coerceIn(0f, 1f)
        fun ch(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * t).roundToInt()
        return argb(ch(24), ch(16), ch(8), ch(0))
    }

    private fun scale(c: Int, f: Float): Int =
        argb((c shr 24) and 0xFF, (((c shr 16) and 0xFF) * f).roundToInt(), (((c shr 8) and 0xFF) * f).roundToInt(), ((c and 0xFF) * f).roundToInt())

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
}
