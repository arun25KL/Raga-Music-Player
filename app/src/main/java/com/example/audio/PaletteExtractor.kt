package com.example.audio

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

/**
 * Palette colors extracted from track album art or fallback artistic palette.
 */
data class ExtractedAlbumPalette(
    val dominantColor: Color,
    val vibrantAccent: Color,
    val surfaceGradientStart: Color,
    val surfaceGradientEnd: Color,
    val onDominantColor: Color
)

object PaletteExtractor {

    private val FallbackDominant = Color(0xFF201B18)
    private val FallbackVibrant = Color(0xFFFF9800)
    private val FallbackStart = Color(0xFF2C2018)
    private val FallbackEnd = Color(0xFF141211)
    private val FallbackOnDominant = Color(0xFFFFF8F0)

    val DefaultPalette = ExtractedAlbumPalette(
        dominantColor = FallbackDominant,
        vibrantAccent = FallbackVibrant,
        surfaceGradientStart = FallbackStart,
        surfaceGradientEnd = FallbackEnd,
        onDominantColor = FallbackOnDominant
    )

    /**
     * Fast, lightweight color extractor from Bitmap pixels that works on any Android API level
     * without needing external heavy dependencies.
     */
    fun extract(bitmap: Bitmap?): ExtractedAlbumPalette {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            return DefaultPalette
        }

        try {
            // Sample down to a small resolution for instant extraction
            val sampleSize = 24
            val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)
            val pixels = IntArray(sampleSize * sampleSize)
            scaled.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)

            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0

            var maxSat = -1f
            var vibrantColorInt = FallbackVibrant.toArgb()

            val hsv = FloatArray(3)
            for (pixel in pixels) {
                val a = (pixel ushr 24) and 0xFF
                if (a < 128) continue

                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                rSum += r
                gSum += g
                bSum += b
                count++

                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val value = hsv[2]

                // Look for vibrant, rich colors (high saturation, good brightness)
                if (sat > 0.35f && value in 0.25f..0.95f) {
                    val score = sat * 0.7f + value * 0.3f
                    if (score > maxSat) {
                        maxSat = score
                        vibrantColorInt = pixel
                    }
                }
            }

            if (scaled != bitmap) {
                scaled.recycle()
            }

            if (count == 0) return DefaultPalette

            val avgR = (rSum / count).toInt()
            val avgG = (gSum / count).toInt()
            val avgB = (bSum / count).toInt()

            val dominant = Color(
                red = (avgR / 255f).coerceIn(0f, 1f),
                green = (avgG / 255f).coerceIn(0f, 1f),
                blue = (avgB / 255f).coerceIn(0f, 1f)
            )

            val vibrant = if (maxSat > 0) {
                val vr = ((vibrantColorInt ushr 16) and 0xFF) / 255f
                val vg = ((vibrantColorInt ushr 8) and 0xFF) / 255f
                val vb = (vibrantColorInt and 0xFF) / 255f
                Color(vr, vg, vb)
            } else {
                Color(0xFFFF9800)
            }

            // Darken for surface background gradient
            val gradientStart = Color(
                red = (avgR / 255f * 0.35f).coerceIn(0.08f, 0.35f),
                green = (avgG / 255f * 0.35f).coerceIn(0.08f, 0.35f),
                blue = (avgB / 255f * 0.35f).coerceIn(0.08f, 0.35f)
            )
            val gradientEnd = Color(0xFF12100E)

            // Determine text contrast
            val luminance = (0.299 * avgR + 0.587 * avgG + 0.114 * avgB) / 255.0
            val onDominant = if (luminance > 0.5) Color(0xFF1E1A16) else Color(0xFFFFF8F0)

            return ExtractedAlbumPalette(
                dominantColor = dominant,
                vibrantAccent = vibrant,
                surfaceGradientStart = gradientStart,
                surfaceGradientEnd = gradientEnd,
                onDominantColor = onDominant
            )
        } catch (e: Exception) {
            return DefaultPalette
        }
    }
}
