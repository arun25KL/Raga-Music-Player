package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ArtDisplayMode(val displayName: String, val icon: String) {
    VINYL("Vinyl Record", "📻"),
    HOLO_CD("Holographic CD", "💿"),
    GLASS_CARD("Glassmorphic Card", "🖼️"),
    AUDIO_RING("Pulse Equalizer Ring", "💫")
}

@Composable
fun VinylDiscArt(
    isPlaying: Boolean,
    coverArt: Bitmap? = null,
    size: Dp = 160.dp,
    displayMode: ArtDisplayMode = ArtDisplayMode.AUDIO_RING,
    modifier: Modifier = Modifier
) {
    when (displayMode) {
        ArtDisplayMode.VINYL -> VinylRecordView(isPlaying = isPlaying, coverArt = coverArt, size = size, modifier = modifier)
        ArtDisplayMode.HOLO_CD -> HolographicCdView(isPlaying = isPlaying, coverArt = coverArt, size = size, modifier = modifier)
        ArtDisplayMode.GLASS_CARD -> GlassmorphicCardView(isPlaying = isPlaying, coverArt = coverArt, size = size, modifier = modifier)
        ArtDisplayMode.AUDIO_RING -> PulseEqualizerRingView(isPlaying = isPlaying, coverArt = coverArt, size = size, modifier = modifier)
    }
}

@Composable
private fun VinylRecordView(
    isPlaying: Boolean,
    coverArt: Bitmap?,
    size: Dp,
    modifier: Modifier
) {
    // 33 1/3 RPM continuous vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "VinylDiscRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscAngle"
    )

    val effectiveRotation = if (isPlaying) rotation else 0f
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor = MaterialTheme.colorScheme.background

    // Spinning Vinyl LP Record with Embedded Album Artwork (95% Album Art, 5% Black Vinyl Rim)
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isPlaying) 18.dp else 10.dp,
                shape = CircleShape,
                spotColor = primaryColor.copy(alpha = if (isPlaying) 0.5f else 0.25f)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF202226),
                        Color(0xFF141518),
                        Color(0xFF070809)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        primaryColor.copy(alpha = 0.85f),
                        borderColor.copy(alpha = 0.5f),
                        secondaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.85f)
                    )
                ),
                shape = CircleShape
            )
            .rotate(effectiveRotation),
        contentAlignment = Alignment.Center
    ) {
        // High-precision vinyl audio micro-grooves along the 5% outer black rim
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f - 1.5.dp.toPx()
            val innerRadius = (this.size.width * 0.95f) / 2f

            val totalGrooves = 3
            val grooveSpacing = (outerRadius - innerRadius) / (totalGrooves + 1)

            for (i in 1..totalGrooves) {
                val grooveR = innerRadius + i * grooveSpacing
                drawCircle(
                    color = Color.White.copy(alpha = 0.20f),
                    radius = grooveR,
                    center = center,
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }
        }

        // Realistic double-wedge specular light sheen (reflections across spinning record)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.sweepGradient(
                        0.0f to Color.White.copy(alpha = 0.08f),
                        0.12f to Color.Transparent,
                        0.25f to Color.White.copy(alpha = 0.14f),
                        0.37f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.08f),
                        0.62f to Color.Transparent,
                        0.75f to Color.White.copy(alpha = 0.14f),
                        0.87f to Color.Transparent,
                        1.0f to Color.White.copy(alpha = 0.08f)
                    )
                )
        )

        // Center Album Art / Label (95% of total vinyl diameter)
        val centerLabelSize = size * 0.95f
        Box(
            modifier = Modifier
                .size(centerLabelSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(containerColor, Color(0xFF1A1C20))
                    )
                )
                .border(1.dp, primaryColor.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (coverArt != null) {
                // High-resolution rotating Album Artwork covering 95% of vinyl
                Image(
                    bitmap = coverArt.asImageBitmap(),
                    contentDescription = "Rotating Album Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Subtle circular sheen overlay on cover art
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f)
                                )
                            )
                        )
                )
            } else {
                // Default artistic label when no album art is embedded
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = secondaryColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(centerLabelSize * 0.35f)
                )
            }

            // Center Spindle Bushing and Hole
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE2E8F0),
                                Color(0xFF64748B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(0.75.dp, Color(0xFF334155), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Center Spindle Hole Cutout
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                )
            }
        }
    }
}

@Composable
private fun HolographicCdView(
    isPlaying: Boolean,
    coverArt: Bitmap?,
    size: Dp,
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CdRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CdAngle"
    )
    val effectiveRotation = if (isPlaying) rotation else 0f
    val primaryColor = MaterialTheme.colorScheme.primary

    // Holographic Silver CD Compact Disc with Iridescent Rainbow Sheen
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isPlaying) 16.dp else 8.dp,
                shape = CircleShape,
                spotColor = Color(0xFF38BDF8)
            )
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFFE2E8F0),
                        Color(0xFFBAE6FD),
                        Color(0xFFFBCFE8),
                        Color(0xFFFED7AA),
                        Color(0xFFDDD6FE),
                        Color(0xFFC7D2FE),
                        Color(0xFFE2E8F0)
                    )
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            .rotate(effectiveRotation),
        contentAlignment = Alignment.Center
    ) {
        // High-density CD audio tracks data ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = this.size.width / 2f - 2.dp.toPx()
            for (i in 1..8) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = maxR * (0.45f + i * 0.06f),
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }

        // Concentric Iridescent Prism Sheen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x3338BDF8),
                            Color(0x33F472B6),
                            Color.Transparent
                        )
                    )
                )
        )

        // Center Artwork Core (78% of CD surface)
        val artCoreSize = size * 0.78f
        Box(
            modifier = Modifier
                .size(artCoreSize)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (coverArt != null) {
                Image(
                    bitmap = coverArt.asImageBitmap(),
                    contentDescription = "CD Album Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(artCoreSize * 0.4f)
                )
            }

            // Transparent Silver Center CD Hub & Center Hole
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.12f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, Color(0xFF94A3B8), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun GlassmorphicCardView(
    isPlaying: Boolean,
    coverArt: Bitmap?,
    size: Dp,
    modifier: Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Glassmorphic Card Frame with Dynamic Breathing Aura
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isPlaying) 20.dp else 10.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = primaryColor
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (coverArt != null) {
            Image(
                bitmap = coverArt.asImageBitmap(),
                contentDescription = "Glassmorphic Artwork Card",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(size * 0.4f)
            )
        }
    }
}

@Composable
private fun PulseEqualizerRingView(
    isPlaying: Boolean,
    coverArt: Bitmap?,
    size: Dp,
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseRing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingAngle"
    )

    val effectiveRotation = if (isPlaying) ringRotation else 0f
    val primaryColor = MaterialTheme.colorScheme.primary

    // Circular Artwork surrounded by a reactive pulse visualizer ring
    Box(
        modifier = modifier
            .size(size)
            .shadow(16.dp, CircleShape, spotColor = primaryColor),
        contentAlignment = Alignment.Center
    ) {
        // Active Pulsing Waveform Ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(effectiveRotation)
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val ringRadius = (this.size.width / 2f) - 6.dp.toPx()
            val totalBars = 32

            for (i in 0 until totalBars) {
                val angle = (i * 360f / totalBars) * (Math.PI / 180f)
                val barLength = if (isPlaying) {
                    ((i % 4 + 1) * 4.dp.toPx() * pulseScale)
                } else 3.dp.toPx()

                val startX = center.x + ringRadius * Math.cos(angle).toFloat()
                val startY = center.y + ringRadius * Math.sin(angle).toFloat()

                val endX = center.x + (ringRadius + barLength) * Math.cos(angle).toFloat()
                val endY = center.y + (ringRadius + barLength) * Math.sin(angle).toFloat()

                drawLine(
                    color = primaryColor.copy(alpha = if (isPlaying) 0.85f else 0.40f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Center Album Artwork Circle
        val centerArtSize = size * 0.80f
        Box(
            modifier = Modifier
                .size(centerArtSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.5.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (coverArt != null) {
                Image(
                    bitmap = coverArt.asImageBitmap(),
                    contentDescription = "Pulse Ring Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(centerArtSize * 0.4f)
                )
            }
        }
    }
}

