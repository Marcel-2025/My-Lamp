package dev.flondy.mylamp.core

import dev.flondy.mylamp.data.AUTO_OFF_CHOICES

/** Reine Logik für den Auto-Aus-Timer. */
object AutoOff {

    /** Nächste Stufe beim Antippen: aus → 1 → 5 → 15 → 30 → 60 → aus. */
    fun next(current: Int): Int {
        val i = AUTO_OFF_CHOICES.indexOf(current)
        return AUTO_OFF_CHOICES[(if (i < 0) 0 else i + 1) % AUTO_OFF_CHOICES.size]
    }

    fun durationMs(minutes: Int): Long = minutes.coerceAtLeast(0) * 60_000L

    /** Restzeit als „m:ss“ bzw. „h:mm:ss“ (aufgerundet auf volle Sekunden). */
    fun format(remainingMs: Long): String {
        val totalSeconds = (remainingMs.coerceAtLeast(0L) + 999L) / 1000L
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Anteil der abgelaufenen Zeit 0..1 (für den Countdown-Ring). */
    fun progress(remainingMs: Long, totalMs: Long): Float =
        if (totalMs <= 0L) 0f else (1f - remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}
