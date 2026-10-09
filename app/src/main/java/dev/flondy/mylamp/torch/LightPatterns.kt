package dev.flondy.mylamp.torch

/**
 * Ein einzelner Schritt eines Lichtmusters: Licht an oder aus für [durationMs] Millisekunden.
 */
data class LightStep(val on: Boolean, val durationMs: Long)

/**
 * Reine, testbare Logik für Lichtmuster (ohne Android-Abhängigkeiten).
 */
object LightPatterns {

    /** Länge einer Morse-Einheit (Punkt). 200 ms ist gut erkennbar und entspricht gängigen SOS-Lampen. */
    const val MORSE_UNIT_MS = 200L

    const val STROBE_MIN_HZ = 1f
    const val STROBE_MAX_HZ = 12f
    const val STROBE_DEFAULT_HZ = 5f

    private val MORSE = mapOf(
        'S' to "...",
        'O' to "---",
    )

    /**
     * Erzeugt ein Morse-Muster nach ITU-Konvention:
     * Punkt = 1 Einheit, Strich = 3 Einheiten, Pause innerhalb eines Zeichens = 1,
     * Pause zwischen Buchstaben = 3, Pause nach dem Wort = 7 Einheiten.
     */
    fun morse(word: String, unitMs: Long = MORSE_UNIT_MS): List<LightStep> {
        val steps = mutableListOf<LightStep>()
        val letters = word.uppercase().filter { it in MORSE }
        letters.forEachIndexed { letterIndex, letter ->
            val code = MORSE.getValue(letter)
            code.forEachIndexed { symbolIndex, symbol ->
                steps += LightStep(true, if (symbol == '.') unitMs else unitMs * 3)
                val isLastSymbol = symbolIndex == code.lastIndex
                val gapUnits = when {
                    !isLastSymbol -> 1
                    letterIndex != letters.lastIndex -> 3
                    else -> 7
                }
                steps += LightStep(false, unitMs * gapUnits)
            }
        }
        return steps
    }

    /** Das standardisierte SOS-Signal ( ... --- ... ) inklusive Wortpause, endlos wiederholbar. */
    val SOS: List<LightStep> = morse("SOS")

    fun clampStrobeHz(hz: Float): Float = hz.coerceIn(STROBE_MIN_HZ, STROBE_MAX_HZ)

    /** Halbe Periodendauer (An- bzw. Aus-Phase) für eine Stroboskop-Frequenz. */
    fun strobeHalfPeriodMs(hz: Float): Long = (1000f / (2f * clampStrobeHz(hz))).toLong().coerceAtLeast(1L)
}
