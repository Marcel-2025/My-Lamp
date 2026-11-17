package dev.flondy.mylamp.torch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseAndEffectsTest {

    private val u = LightPatterns.MORSE_UNIT_MS

    @Test
    fun alphabet_coversLettersAndDigits() {
        ('A'..'Z').forEach { assertTrue("$it fehlt", it in LightPatterns.MORSE_TABLE) }
        ('0'..'9').forEach { assertTrue("$it fehlt", it in LightPatterns.MORSE_TABLE) }
    }

    @Test
    fun alphabet_codesAreUnique() {
        val codes = LightPatterns.MORSE_TABLE.values
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun sanitize_uppercasesTransliteratesAndDropsUnknown() {
        assertEquals("HALLO WELT", LightPatterns.sanitizeMorse("  hallo   welt  "))
        assertEquals("GRUESSE", LightPatterns.sanitizeMorse("Grüße"))
        assertEquals("AB", LightPatterns.sanitizeMorse("a#b"))
        assertEquals("", LightPatterns.sanitizeMorse("###"))
    }

    @Test
    fun sanitize_limitsLength() {
        val long = "A".repeat(100)
        assertEquals(LightPatterns.MORSE_MAX_LENGTH, LightPatterns.sanitizeMorse(long).length)
    }

    @Test
    fun morseCode_readableWithWordSeparator() {
        assertEquals("... --- ...", LightPatterns.morseCode("sos"))
        assertEquals(".... .. / .- .-..", LightPatterns.morseCode("hi al"))
    }

    @Test
    fun morse_usesSevenUnitGapBetweenWords() {
        // "E E": Punkt, 7 Einheiten Wortpause, Punkt, 7 Einheiten
        val steps = LightPatterns.morse("E E")
        assertEquals(
            listOf(LightStep(true, u), LightStep(false, 7 * u), LightStep(true, u), LightStep(false, 7 * u)),
            steps,
        )
    }

    @Test
    fun morse_emptyTextGivesNoSteps() {
        assertTrue(LightPatterns.morse("§$%").isEmpty())
    }

    @Test
    fun sos_isUnchangedByAlphabetExtension() {
        assertEquals(34 * u, LightPatterns.SOS.sumOf { it.durationMs })
    }

    @Test
    fun effects_stayBelowThreeFlashesPerSecond() {
        LightEffect.entries.forEach { e ->
            val fps = LightPatterns.flashesPerSecond(e.steps)
            assertTrue("${e.name}: $fps Blitze/s", fps < 3f)
        }
    }

    @Test
    fun effects_alternateStartingWithOn() {
        LightEffect.entries.forEach { e ->
            e.steps.forEachIndexed { i, step -> assertEquals("${e.name}[$i]", i % 2 == 0, step.on) }
        }
    }

    @Test
    fun effects_blinkIsFreeOthersPremium() {
        assertFalse(LightEffect.BLINK.isPremium)
        assertTrue(LightEffect.entries.filter { it != LightEffect.BLINK }.all { it.isPremium })
    }

    @Test
    fun blink_isOneHertz() {
        assertEquals(1f, LightPatterns.flashesPerSecond(LightEffect.BLINK.steps), 0.001f)
    }
}
