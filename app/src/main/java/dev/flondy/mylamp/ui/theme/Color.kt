package dev.flondy.mylamp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** My Lamp Premium-Palette (Dark / Futuristic / Luxury). */
object LampColors {
    val Night = Color(0xFF050711)          // Primärer Hintergrund
    val NightRaised = Color(0xFF0C0F1C)    // Sekundärer Hintergrund
    val Card = Color(0xFF111526)
    val CardHigh = Color(0xFF171C31)
    val Glass = Color(0x99101425)
    val Outline = Color(0x1FFFFFFF)
    val OutlineStrong = Color(0x33FFFFFF)

    val Gold = Color(0xFFFDEE51)           // Premium-Gold
    val GoldWarm = Color(0xFFF5C84B)
    val GoldDeep = Color(0xFFB8921F)
    val GoldGlow = Color(0x55FDEE51)
    val GoldSubtle = Color(0x1FFDEE51)

    val Text = Color(0xFFF5F5F5)
    val TextMuted = Color(0xB3F5F5F5)
    val TextFaint = Color(0x80F5F5F5)
    val Neutral600 = Color(0xFF3A3F55)
    val Neutral400 = Color(0xFF8A90A6)

    val Sos = Color(0xFFFF5A5F)
    val SosGlow = Color(0x55FF5A5F)
    val Strobe = Color(0xFF7FD7FF)
    val StrobeGlow = Color(0x447FD7FF)
    val Success = Color(0xFF6EE7A8)

    val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFF7A8), Gold, GoldWarm))
    val NightGradient = Brush.verticalGradient(listOf(NightRaised, Night))
}
