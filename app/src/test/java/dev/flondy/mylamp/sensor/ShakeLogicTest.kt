package dev.flondy.mylamp.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeLogicTest {
    private val g = 9.80665f

    private fun ShakeLogic.peak(t: Long) = onSample(t, 3.5f * g, 0f, 0f)
    private fun ShakeLogic.rest(t: Long) = onSample(t, 0f, 0f, g)

    @Test
    fun resting_neverTriggers() {
        val logic = ShakeLogic()
        repeat(100) { assertFalse(logic.rest(it * 20L)) }
    }

    @Test
    fun singleBump_doesNotTrigger() {
        val logic = ShakeLogic()
        assertFalse(logic.peak(0))
        assertFalse(logic.peak(1000))
        assertFalse(logic.peak(2000))
    }

    @Test
    fun threeQuickPeaks_trigger() {
        val logic = ShakeLogic()
        assertFalse(logic.peak(0))
        assertFalse(logic.peak(150))
        assertTrue(logic.peak(300))
    }

    @Test
    fun cooldown_preventsDoubleToggle() {
        val logic = ShakeLogic(cooldownMs = 1500)
        logic.peak(0); logic.peak(150); assertTrue(logic.peak(300))
        var triggers = 0
        for (t in 400L..1700L step 150) if (logic.peak(t)) triggers++
        assertEquals(0, triggers)
        logic.peak(2000); logic.peak(2150)
        assertTrue(logic.peak(2300))
    }

    @Test
    fun sensitivityThresholdsAreOrdered() {
        assertTrue(ShakeSensitivity.HIGH.thresholdG < ShakeSensitivity.MEDIUM.thresholdG)
        assertTrue(ShakeSensitivity.MEDIUM.thresholdG < ShakeSensitivity.LOW.thresholdG)
    }
}
