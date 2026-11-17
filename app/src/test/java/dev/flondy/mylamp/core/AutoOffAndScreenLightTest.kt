package dev.flondy.mylamp.core

import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.data.ScreenLightColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoOffAndScreenLightTest {

    @Test
    fun autoOff_cyclesThroughChoices() {
        assertEquals(1, AutoOff.next(0))
        assertEquals(5, AutoOff.next(1))
        assertEquals(60, AutoOff.next(30))
        assertEquals(0, AutoOff.next(60))
        assertEquals(0, AutoOff.next(7)) // unbekannter Wert → sicher auf „aus“
    }

    @Test
    fun autoOff_formatsRemainingTime() {
        assertEquals("5:00", AutoOff.format(300_000))
        assertEquals("0:01", AutoOff.format(1)) // aufgerundet
        assertEquals("0:00", AutoOff.format(0))
        assertEquals("1:00:00", AutoOff.format(3_600_000))
        assertEquals("4:32", AutoOff.format(272_000))
    }

    @Test
    fun autoOff_progress() {
        assertEquals(0f, AutoOff.progress(60_000, 60_000), 0.0001f)
        assertEquals(0.5f, AutoOff.progress(30_000, 60_000), 0.0001f)
        assertEquals(1f, AutoOff.progress(0, 60_000), 0.0001f)
        assertEquals(0f, AutoOff.progress(10, 0), 0.0001f)
    }

    @Test
    fun hsv_primaryColors() {
        assertEquals(0xFFFF0000.toInt(), ScreenLightLogic.hsvToArgb(0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), ScreenLightLogic.hsvToArgb(120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), ScreenLightLogic.hsvToArgb(240f, 1f, 1f))
        assertEquals(0xFFFFFFFF.toInt(), ScreenLightLogic.hsvToArgb(77f, 0f, 1f))
        assertEquals(0xFFFF0000.toInt(), ScreenLightLogic.hsvToArgb(360f, 1f, 1f))
    }

    @Test
    fun noneEffect_keepsBaseColor() {
        val base = ScreenLightLogic.baseColor(ScreenLightColor.WARM, 0f)
        assertEquals(ScreenLightColor.WARM.argb.toInt(), base)
        assertEquals(base, ScreenLightLogic.colorAt(ScreenEffect.NONE, base, 12345))
    }

    @Test
    fun customColor_followsHue() {
        assertNotEquals(
            ScreenLightLogic.baseColor(ScreenLightColor.CUSTOM, 0f),
            ScreenLightLogic.baseColor(ScreenLightColor.CUSTOM, 180f),
        )
    }

    @Test
    fun rainbow_loopsAfterPeriod() {
        val p = ScreenLightLogic.RAINBOW_PERIOD_MS
        assertEquals(
            ScreenLightLogic.colorAt(ScreenEffect.RAINBOW, 0, 1000),
            ScreenLightLogic.colorAt(ScreenEffect.RAINBOW, 0, 1000 + p),
        )
    }

    @Test
    fun warning_blinksAtOneHertz() {
        val on = ScreenLightLogic.colorAt(ScreenEffect.WARNING, 0, 100)
        val off = ScreenLightLogic.colorAt(ScreenEffect.WARNING, 0, 600)
        assertNotEquals(on, off)
        assertEquals(on, ScreenLightLogic.colorAt(ScreenEffect.WARNING, 0, 1100))
    }

    @Test
    fun candle_staysInSoftRange() {
        for (t in 0L..20_000L step 37L) {
            val l = ScreenLightLogic.candleLevel(t)
            assertTrue(l in 0.78f..1f)
        }
    }

    @Test
    fun candle_changesSlowly() {
        // Kein hartes Flackern: zwischen zwei Bildern (16 ms) ändert sich die Helligkeit kaum
        for (t in 0L..20_000L step 16L) {
            val d = kotlin.math.abs(ScreenLightLogic.candleLevel(t + 16) - ScreenLightLogic.candleLevel(t))
            assertTrue("Sprung $d bei $t", d < 0.02f)
        }
    }

    @Test
    fun party_staysOnPaletteOutsideFade() {
        val c = ScreenLightLogic.colorAt(ScreenEffect.PARTY, 0, 200)
        assertEquals(ScreenLightLogic.PARTY_COLORS[0], c)
        val c2 = ScreenLightLogic.colorAt(ScreenEffect.PARTY, 0, ScreenLightLogic.PARTY_SLOT_MS + 200)
        assertEquals(ScreenLightLogic.PARTY_COLORS[1], c2)
    }
}
