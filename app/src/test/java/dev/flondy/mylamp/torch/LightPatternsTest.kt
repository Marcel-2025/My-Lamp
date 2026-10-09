package dev.flondy.mylamp.torch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightPatternsTest {

    private val u = LightPatterns.MORSE_UNIT_MS

    @Test
    fun sos_hasNineFlashes_threeShortThreeLongThreeShort() {
        val flashes = LightPatterns.SOS.filter { it.on }.map { it.durationMs }
        assertEquals(listOf(u, u, u, 3 * u, 3 * u, 3 * u, u, u, u), flashes)
    }

    @Test
    fun sos_gapsFollowItuTiming() {
        val gaps = LightPatterns.SOS.filter { !it.on }.map { it.durationMs / u }
        // innerhalb eines Buchstabens 1, zwischen Buchstaben 3, Wortende 7 Einheiten
        assertEquals(listOf(1L, 1L, 3L, 1L, 1L, 3L, 1L, 1L, 7L), gaps)
    }

    @Test
    fun sos_alternatesOnAndOff_startingWithOn() {
        LightPatterns.SOS.forEachIndexed { i, step -> assertEquals(i % 2 == 0, step.on) }
    }

    @Test
    fun sos_totalDurationIs34Units() {
        // 9 Symbole: 6×1 + 3×3 = 15, Pausen: 6×1 + 2×3 + 7 = 19 → 34 Einheiten
        assertEquals(34 * u, LightPatterns.SOS.sumOf { it.durationMs })
    }

    @Test
    fun strobe_isClampedToSafeHardwareRange() {
        assertEquals(LightPatterns.STROBE_MAX_HZ, LightPatterns.clampStrobeHz(100f))
        assertEquals(LightPatterns.STROBE_MIN_HZ, LightPatterns.clampStrobeHz(0f))
        assertTrue(LightPatterns.strobeHalfPeriodMs(1000f) >= 40L)
    }

    @Test
    fun strobe_halfPeriodMatchesFrequency() {
        assertEquals(500L, LightPatterns.strobeHalfPeriodMs(1f))
        assertEquals(100L, LightPatterns.strobeHalfPeriodMs(5f))
    }
}
