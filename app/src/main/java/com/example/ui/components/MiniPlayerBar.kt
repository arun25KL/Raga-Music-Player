package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.ExtractedAlbumPalette
import com.example.model.AudioTrack
import com.example.ui.theme.getContrastingTextColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerBar(
    visible: Boolean,
    track: AudioTrack?,
    coverArt: Bitmap?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    albumPalette: ExtractedAlbumPalette,
    isDynamicThemeEnabled: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dynamic Mood Pulse in Mini-Player
    val infiniteTransition = rememberInfiniteTransition(label = "MiniPlayerMoodPulse")
    val moodPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MoodPulseAlpha"
    )
    val moodGlowRadius by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MoodGlowRadius"
    )

    AnimatedVisibility(
        visible = visible && track != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (track == null) return@AnimatedVisibility

        val defaultAccent = MaterialTheme.colorScheme.primary
        val defaultSecondary = MaterialTheme.colorScheme.secondary
        val defaultBgStart = MaterialTheme.colorScheme.surfaceVariant
        val defaultBgEnd = MaterialTheme.colorScheme.surface
        val defaultBorder = MaterialTheme.colorScheme.outline

        val isThemeLight = defaultBgEnd.luminance() > 0.5f

        val isCustomAlbumArtPresent = coverArt != null
        val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        val rawAccentColor = if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.vibrantAccent else defaultAccent
        val accentColor = if (isThemeLight && rawAccentColor.luminance() > 0.65f) {
            if (defaultAccent.luminance() <= 0.65f) defaultAccent else Color(0xFF1D4ED8)
        } else {
            rawAccentColor
        }
        val barBgStart = if (isThemeLight) Color(0xFFFFFFFF) else (if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.surfaceGradientStart else defaultBgStart)
        val barBgEnd = if (isThemeLight) Color(0xFFF9FAFB) else (if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.surfaceGradientEnd else defaultBgEnd)
        val effectiveMoodAlpha = if (isPlaying) moodPulseAlpha else 0.2f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .shadow(
                    elevation = if (isPlaying) 18.dp else 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = accentColor.copy(alpha = if (isThemeLight) 0.12f else (if (isPlaying) effectiveMoodAlpha else 0.25f))
                )
                .combinedClickable(
                    onClick = { onClick() },
                    onDoubleClick = { onDoubleClick() }
                )
                .testTag("mini_player_bar"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(
                width = if (isPlaying) 1.5.dp else 1.dp,
                color = if (isThemeLight) Color(0xFFE5E7EB) else accentColor.copy(alpha = if (isPlaying) effectiveMoodAlpha.coerceAtLeast(0.45f) else 0.35f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(barBgStart, barBgEnd)
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini progress indicator across top
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = accentColor,
                        trackColor = defaultBorder.copy(alpha = 0.5f),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art thumbnail with Dynamic Mood Pulse aura
                        Box(
                            modifier = Modifier.size(46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(moodGlowRadius)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accentColor.copy(alpha = effectiveMoodAlpha * 0.45f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(defaultBgStart)
                                    .border(
                                        width = if (isPlaying) 1.5.dp else 1.dp,
                                        color = if (isPlaying) accentColor.copy(alpha = effectiveMoodAlpha) else defaultBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (coverArt != null) {
                                    Image(
                                        bitmap = coverArt.asImageBitmap(),
                                        contentDescription = "Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Artist with White & Theme secondary letters
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isThemeLight) Color(0xFF111827) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isThemeLight) {
                                    if (accentColor.luminance() <= 0.60f && accentColor != Color.Black) accentColor else Color(0xFF374151)
                                } else {
                                    if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.vibrantAccent else defaultSecondary
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Playback quick controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier.size(36.dp).testTag("mini_btn_prev")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = if (isThemeLight) Color(0xFF1F2937) else defaultSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Play/Pause button in Theme Accent
                            val miniPlayBg = accentColor
                            val miniPlayIconTint = getContrastingTextColor(miniPlayBg)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(miniPlayBg)
                                    .clickable { onTogglePlayPause() }
                                    .testTag("mini_btn_play_pause"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = miniPlayIconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier.size(36.dp).testTag("mini_btn_next")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = if (isThemeLight) Color(0xFF1F2937) else defaultSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
