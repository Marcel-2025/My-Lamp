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

    /** Höchstlänge einer Morse-Nachricht (Zeichen). */
    const val MORSE_MAX_LENGTH = 40

    /** Internationales Morsealphabet (ITU-R M.1677-1) – Buchstaben, Ziffern und gängige Satzzeichen. */
    val MORSE_TABLE: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '!' to "-.-.--", '/' to "-..-.",
        '-' to "-....-", '@' to ".--.-.", '=' to "-...-", '+' to ".-.-.", ':' to "---...",
        '\'' to ".----.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...",
    )

    /** Umlaute und ß werden wie im Funkverkehr üblich umschrieben. */
    private val TRANSLITERATION = mapOf('Ä' to "AE", 'Ö' to "OE", 'Ü' to "UE", 'ß' to "SS", 'ẞ' to "SS")

    /**
     * Bereitet einen Text für das Morsen vor: Großbuchstaben, Umlaute umschreiben, nicht morsbare
     * Zeichen entfernen, mehrfache Leerzeichen zusammenfassen, auf [MORSE_MAX_LENGTH] kürzen.
     */
    fun sanitizeMorse(text: String): String {
        val sb = StringBuilder()
        for (raw in text.uppercase()) {
            val mapped = TRANSLITERATION[raw]
            when {
                mapped != null -> sb.append(mapped)
                raw in MORSE_TABLE -> sb.append(raw)
                raw.isWhitespace() -> if (sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
                else -> Unit
            }
        }
        return sb.toString().trim().take(MORSE_MAX_LENGTH).trim()
    }

    /** Lesbare Morse-Darstellung, z. B. „... --- ...“ (Wörter mit „ / “ getrennt). */
    fun morseCode(text: String): String =
        sanitizeMorse(text).split(' ').filter { it.isNotEmpty() }.joinToString(" / ") { word ->
            word.map { MORSE_TABLE.getValue(it) }.joinToString(" ")
        }

    /**
     * Erzeugt ein Morse-Muster nach ITU-Konvention:
     * Punkt = 1 Einheit, Strich = 3 Einheiten, Pause innerhalb eines Zeichens = 1,
     * Pause zwischen Buchstaben = 3, zwischen Wörtern und nach der Nachricht = 7 Einheiten.
     */
    fun morse(text: String, unitMs: Long = MORSE_UNIT_MS): List<LightStep> {
        val words = sanitizeMorse(text).split(' ').filter { it.isNotEmpty() }
        val steps = mutableListOf<LightStep>()
        words.forEach { word ->
            word.forEachIndexed { letterIndex, letter ->
                val code = MORSE_TABLE.getValue(letter)
                code.forEachIndexed { symbolIndex, symbol ->
                    steps += LightStep(true, if (symbol == '.') unitMs else unitMs * 3)
                    val gapUnits = when {
                        symbolIndex != code.lastIndex -> 1
                        letterIndex != word.lastIndex -> 3
                        else -> 7 // Wortende (auch zwischen Wörtern und vor der Wiederholung)
                    }
                    steps += LightStep(false, unitMs * gapUnits)
                }
            }
        }
        return steps
    }

    /** Das standardisierte SOS-Signal ( ... --- ... ) inklusive Wortpause, endlos wiederholbar. */
    val SOS: List<LightStep> = morse("SOS")

    fun clampStrobeHz(hz: Float): Float = hz.coerceIn(STROBE_MIN_HZ, STROBE_MAX_HZ)

    /** Halbe Periodendauer (An- bzw. Aus-Phase) für eine Stroboskop-Frequenz. */
    fun strobeHalfPeriodMs(hz: Float): Long = (1000f / (2f * clampStrobeHz(hz))).toLong().coerceAtLeast(1L)

    /** Blitze pro Sekunde eines Musters (für die Sicherheitsprüfung: Effekte bleiben unter 3). */
    fun flashesPerSecond(steps: List<LightStep>): Float {
        val total = steps.sumOf { it.durationMs }
        if (total <= 0L) return 0f
        return steps.count { it.on } * 1000f / total
    }
}

/**
 * Feste LED-Lichteffekte. Alle bleiben deutlich unter 3 Blitzen pro Sekunde
 * (Grenzwert der WCAG für Lichtblitze) – deshalb ist kein Warnhinweis wie beim Stroboskop nötig.
 */
enum class LightEffect(val isPremium: Boolean, val steps: List<LightStep>) {
    /** Warnblinker wie am Auto: 1 Hz, gleich lang an und aus. */
    BLINK(false, listOf(LightStep(true, 500), LightStep(false, 500))),

    /** Zwei kurze Schläge, dann Pause – wie ein Herzschlag (ca. 60 Schläge pro Minute). */
    HEARTBEAT(true, listOf(LightStep(true, 120), LightStep(false, 160), LightStep(true, 120), LightStep(false, 600))),

    /** Leuchtfeuer: ein kurzer Blitz alle zwei Sekunden – gut aus der Ferne sichtbar, sehr sparsam. */
    BEACON(true, listOf(LightStep(true, 180), LightStep(false, 1820))),

    /** Signal: Doppelblitz alle 1,5 Sekunden, angelehnt an Positionslichter. */
    SIGNAL(true, listOf(LightStep(true, 90), LightStep(false, 140), LightStep(true, 90), LightStep(false, 1180))),
}
