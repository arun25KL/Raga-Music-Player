package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.audio.ExtractedAlbumPalette
import androidx.palette.graphics.Palette

data class DynamicArtworkPalette(
    val dominantColor: Color? = null,
    val vibrantColor: Color? = null,
    val mutedColor: Color? = null,
    val darkVibrantColor: Color? = null,
    val lightVibrantColor: Color? = null,
    val darkMutedColor: Color? = null,
    val surfaceGradientStart: Color? = null,
    val surfaceGradientEnd: Color? = null,
    val isExtracted: Boolean = false
) {
    fun getPrimaryAccent(fallback: Color): Color {
        return vibrantColor ?: dominantColor ?: lightVibrantColor ?: fallback
    }

    fun getSecondaryAccent(fallback: Color): Color {
        return darkVibrantColor ?: mutedColor ?: darkMutedColor ?: fallback
    }

    fun getSurfaceBackground(fallback: Color): Color {
        return surfaceGradientStart ?: darkMutedColor ?: darkVibrantColor ?: Color(0xFF121212)
    }

    companion object {
        val DEFAULT = DynamicArtworkPalette()

        fun fromExtractedAlbumPalette(extracted: ExtractedAlbumPalette): DynamicArtworkPalette {
            return DynamicArtworkPalette(
                dominantColor = extracted.dominantColor,
                vibrantColor = extracted.vibrantAccent,
                surfaceGradientStart = extracted.surfaceGradientStart,
                surfaceGradientEnd = extracted.surfaceGradientEnd,
                isExtracted = true
            )
        }

        fun fromPalette(palette: Palette): DynamicArtworkPalette {
            val dom = palette.dominantSwatch?.rgb?.let { Color(it) }
            val vib = palette.vibrantSwatch?.rgb?.let { Color(it) }
            val mut = palette.mutedSwatch?.rgb?.let { Color(it) }
            val darkVib = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
            val lightVib = palette.lightVibrantSwatch?.rgb?.let { Color(it) }
            val darkMut = palette.darkMutedSwatch?.rgb?.let { Color(it) }

            return DynamicArtworkPalette(
                dominantColor = dom,
                vibrantColor = vib,
                mutedColor = mut,
                darkVibrantColor = darkVib,
                lightVibrantColor = lightVib,
                darkMutedColor = darkMut,
                isExtracted = dom != null || vib != null
            )
        }

        fun fromTrackSeed(trackId: String, title: String): DynamicArtworkPalette {
            val hash = (trackId + title).hashCode()
            val hue = (hash and 0x7FFFFFFF) % 360f
            val vibColor = Color.hsv(hue, 0.78f, 0.92f)
            val domColor = Color.hsv((hue + 25f) % 360f, 0.65f, 0.40f)
            val darkVibColor = Color.hsv(hue, 0.85f, 0.28f)
            val lightVibColor = Color.hsv(hue, 0.40f, 0.95f)

            return DynamicArtworkPalette(
                dominantColor = domColor,
                vibrantColor = vibColor,
                mutedColor = domColor.copy(alpha = 0.6f),
                darkVibrantColor = darkVibColor,
                lightVibrantColor = lightVibColor,
                darkMutedColor = darkVibColor,
                surfaceGradientStart = Color.hsv(hue, 0.50f, 0.15f),
                surfaceGradientEnd = Color(0xFF101010),
                isExtracted = true
            )
        }
    }
}
