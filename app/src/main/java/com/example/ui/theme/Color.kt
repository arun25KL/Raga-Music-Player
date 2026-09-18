package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Universal Button & Surface Letter Contrast Helper:
 * If any portion of the button/surface is in full black or black shade colors (luminance <= 0.5f),
 * use letters color to be crisp pure white.
 * Vice versa, if in white or light shade colors (luminance > 0.5f), use letters color to be dark/black.
 */
fun getContrastingTextColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) Color(0xFF111827) else Color.White
}

fun getContrastingSecondaryTextColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) Color(0xFF374151) else Color(0xFFE5E7EB)
}

// Artistic Orange & Yellow Design Tokens
val ArtisticSunsetOrange = Color(0xFFFF9800)
val ArtisticVibrantOrange = Color(0xFFFF6D00)
val ArtisticGoldenYellow = Color(0xFFFFD54F)
val ArtisticLuminousYellow = Color(0xFFFFEE58)
val ArtisticWarmAmber = Color(0xFFFFB300)
val ArtisticWarmGold = Color(0xFFFFE082)
val ArtisticPeachGold = Color(0xFFFFCC80)
val ArtisticDeepOrange = Color(0xFFE65100)

// Vibrant Fluorescent & Crisp White Letter Tokens
val FluorescentWhite = Color(0xFFFFFFFF)
val FluorescentCyan = Color(0xFF00F5FF)
val FluorescentNeonGold = Color(0xFFFFE600)
val FluorescentElectricGreen = Color(0xFF00FF88)
val FluorescentIceBlue = Color(0xFF80D8FF)
val FluorescentLavender = Color(0xFFE0C3FC)

val ArtisticDarkBg = Color(0xFF121110)
val ArtisticDarkSurface = Color(0xFF1C1816)
val ArtisticDarkContainer = Color(0xFF28221D)
val ArtisticDarkBorder = Color(0xFF45362B)
val ArtisticTextPrimary = Color(0xFFFFFFFF) // Crisp Pure White
val ArtisticTextSecondary = Color(0xFFD4E0EB) // High clarity fluorescent off-white/ice

// Google Brand Color Accents
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

// Backward compatible aliases
val ElegantDarkBg = ArtisticDarkBg
val ElegantDarkSurface = ArtisticDarkSurface
val ElegantDarkContainer = ArtisticDarkContainer
val ElegantDarkBorder = ArtisticDarkBorder
val ElegantLavenderAccent = ArtisticWarmAmber
val ElegantOnLavender = Color(0xFF2B1700)
val ElegantTextPrimary = ArtisticTextPrimary
val ElegantTextSecondary = ArtisticTextSecondary

val DeepIndigo = ArtisticDarkBg
val SurfaceDark = ArtisticDarkSurface
val SurfaceContainerDark = ArtisticDarkContainer
val CyanAccent = ArtisticWarmAmber
val CyanGlow = ArtisticGoldenYellow
val PurpleAccent = ArtisticSunsetOrange
val PinkAccent = ArtisticPeachGold
val EmeraldGreen = GoogleGreen
val TextPrimaryDark = ArtisticTextPrimary
val TextSecondaryDark = ArtisticTextSecondary
val TrackCardPlayed = ArtisticDarkSurface
val TrackCardUnplayed = ArtisticDarkSurface
val VinylCenter = ArtisticWarmAmber
