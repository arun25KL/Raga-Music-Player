package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.ExtractedAlbumPalette
import com.example.audio.PaletteExtractor
import com.example.model.AudioTrack
import com.example.model.RepeatMode
import com.example.model.SleepTimerState
import com.example.ui.theme.getContrastingTextColor
import com.example.ui.theme.getContrastingSecondaryTextColor

@Composable
fun NowPlayingSection(
    track: AudioTrack?,
    coverArt: Bitmap?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    sessionPlayedIds: Set<String>,
    unplayedCount: Int,
    repeatMode: RepeatMode,
    isShuffleEnabled: Boolean,
    playbackSpeed: Float,
    sleepTimerState: SleepTimerState,
    albumPalette: ExtractedAlbumPalette = PaletteExtractor.DefaultPalette,
    isDynamicThemeEnabled: Boolean = true,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayNew: (() -> Unit)? = null,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenAudioFx: () -> Unit,
    onOpenTrackDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }
    var currentArtDisplayMode by remember { mutableStateOf(ArtDisplayMode.AUDIO_RING) }

    val effectivePosition = if (isUserScrubbing) scrubPositionMs.toLong() else currentPositionMs
    val safeDuration = if (durationMs > 0) durationMs else 1L
    val progressFraction = (effectivePosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val isPlayedInSession = track != null && track.id in sessionPlayedIds

    val defaultAccent = MaterialTheme.colorScheme.primary
    val defaultSecondary = MaterialTheme.colorScheme.secondary
    val defaultSurface = MaterialTheme.colorScheme.surface
    val defaultBorder = MaterialTheme.colorScheme.outline

    val isThemeLight = defaultSurface.luminance() > 0.5f

    val isCustomAlbumArtPresent = track != null && coverArt != null
    val rawCardAccent = if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.vibrantAccent else defaultAccent
    val cardAccent = if (isThemeLight && rawCardAccent.luminance() > 0.65f) {
        if (defaultAccent.luminance() <= 0.65f) defaultAccent else Color(0xFF1D4ED8)
    } else {
        rawCardAccent
    }

    val cardBg = if (isThemeLight) {
        if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.dominantColor.copy(alpha = 0.08f) else defaultSurface
    } else {
        if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.dominantColor.copy(alpha = 0.4f) else defaultSurface
    }

    val titleColor = if (isThemeLight) {
        Color(0xFF111827)
    } else {
        if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.onDominantColor else MaterialTheme.colorScheme.onSurface
    }

    val artistColor = if (isThemeLight) {
        if (cardAccent.luminance() <= 0.60f && cardAccent != Color.Black) cardAccent else Color(0xFF374151)
    } else {
        if (isDynamicThemeEnabled && isCustomAlbumArtPresent) albumPalette.vibrantAccent else defaultAccent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = cardAccent.copy(alpha = if (isThemeLight) 0.15f else 0.35f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, if (isThemeLight) Color(0xFFE5E7EB) else cardAccent.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            cardAccent.copy(alpha = if (isThemeLight) 0.05f else 0.16f),
                            cardBg.copy(alpha = 0.8f),
                            cardBg
                        ),
                        radius = 600f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badge Row: Format Tag + Session Status + Audio FX Quick Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio Codec & Bitrate / Sample Rate Quick Badge (Click opens Audio Track Details)
                    val audioBadgeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    val audioBadgeTextColor = getContrastingTextColor(audioBadgeBg)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = audioBadgeBg,
                        border = BorderStroke(1.dp, defaultAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable(enabled = onOpenTrackDetails != null) { onOpenTrackDetails?.invoke() }
                            .testTag("btn_audio_file_portion")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio Track Details",
                                tint = if (audioBadgeBg.luminance() > 0.5f) Color(0xFF111827) else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = track?.qualityBadgeFormatted ?: ((track?.format?.displayName ?: "AUDIO") + " FILE"),
                                style = MaterialTheme.typography.labelSmall,
                                color = audioBadgeTextColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Audio FX / Speed / Sleep indicator button
                    val fxBtnBg = if (isThemeLight) Color(0xFFF3F4F6) else defaultAccent.copy(alpha = 0.18f)
                    val fxBtnTextColor = getContrastingTextColor(fxBtnBg)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = fxBtnBg,
                        border = BorderStroke(1.dp, if (isThemeLight) Color(0xFFE5E7EB) else defaultAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onOpenAudioFx() }.testTag("btn_quick_fx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "FX",
                                tint = fxBtnTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (playbackSpeed != 1.0f) "${playbackSpeed}x" else if (sleepTimerState.isRunning) "⏳ ${sleepTimerState.remainingFormatted}" else "EQ & FX",
                                style = MaterialTheme.typography.labelSmall,
                                color = fxBtnTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Session Status Tag (Playing when active, Played if >= 60%, Fresh otherwise)
                    val statusText = when {
                        isPlaying -> "Playing"
                        isPlayedInSession -> "Played"
                        else -> "Fresh"
                    }
                    val statusTagBg = when {
                        isPlaying -> cardAccent
                        isPlayedInSession -> if (isThemeLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant
                        else -> if (isThemeLight) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                    val statusTagTextColor = getContrastingTextColor(statusTagBg)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusTagBg,
                        border = BorderStroke(
                            1.dp,
                            when {
                                isPlaying -> Color.Transparent
                                isPlayedInSession -> if (isThemeLight) Color(0xFFE5E7EB) else defaultBorder
                                else -> if (isThemeLight) Color(0xFFBFDBFE) else cardAccent.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusTagTextColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusTagTextColor,
                                fontWeight = if (isPlaying || !isPlayedInSession) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hero Artwork Container with Ambient Aura Backlight + Swipe Right or Left to Change Visual Styles
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .pointerInput(Unit) {
                            var totalDragX = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDragX = 0f },
                                onDragEnd = {
                                    val modes = ArtDisplayMode.values()
                                    val currentIdx = modes.indexOf(currentArtDisplayMode)
                                    if (totalDragX < -30f) { // Swipe Left -> Next Style
                                        val nextIdx = (currentIdx + 1) % modes.size
                                        currentArtDisplayMode = modes[nextIdx]
                                    } else if (totalDragX > 30f) { // Swipe Right -> Previous Style
                                        val prevIdx = if (currentIdx - 1 < 0) modes.size - 1 else currentIdx - 1
                                        currentArtDisplayMode = modes[prevIdx]
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount
                                }
                            )
                        }
                        .clickable {
                            val modes = ArtDisplayMode.values()
                            val nextIdx = (modes.indexOf(currentArtDisplayMode) + 1) % modes.size
                            currentArtDisplayMode = modes[nextIdx]
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient backlight halo
                    Box(
                        modifier = Modifier
                            .size(175.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        cardAccent.copy(alpha = if (isPlaying) 0.45f else 0.20f),
                                        cardAccent.copy(alpha = 0.10f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    VinylDiscArt(
                        isPlaying = isPlaying,
                        coverArt = coverArt,
                        size = 158.dp,
                        displayMode = currentArtDisplayMode
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

            // Track Title
            Text(
                text = track?.title ?: "Select a song to play",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("now_playing_title")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Artist & Album
            Text(
                text = buildString {
                    append(track?.artist ?: "Unknown Artist")
                    if (!track?.album.isNullOrBlank() && track?.album != "Local Audio" && track?.album != "Local Folder") {
                        append(" • ")
                        append(track?.album)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = artistColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slider / Progress Scrubber
            Slider(
                value = progressFraction,
                onValueChange = { frac ->
                    isUserScrubbing = true
                    scrubPositionMs = frac * safeDuration
                },
                onValueChangeFinished = {
                    onSeekTo(scrubPositionMs.toLong())
                    isUserScrubbing = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("playback_seekbar"),
                colors = SliderDefaults.colors(
                    thumbColor = if (isThemeLight) (if (cardAccent.luminance() <= 0.65f && cardAccent != Color.Black) cardAccent else Color(0xFF1D4ED8)) else defaultSecondary,
                    activeTrackColor = if (isThemeLight && cardAccent == Color.Black) Color(0xFF111827) else cardAccent,
                    inactiveTrackColor = if (isThemeLight) Color(0xFFE5E7EB) else defaultBorder
                )
            )

            // Time Labels (Elapsed & Remaining)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(effectivePosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isThemeLight) Color(0xFF374151) else defaultSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTime(safeDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isThemeLight) Color(0xFF6B7280) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(44.dp).testTag("btn_shuffle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Toggle Shuffle",
                        tint = if (isShuffleEnabled) cardAccent else (if (isThemeLight) Color(0xFF6B7280) else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                val prevBtnBg = if (isThemeLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant
                val prevBtnColor = getContrastingTextColor(prevBtnBg)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(prevBtnBg)
                        .border(1.dp, if (isThemeLight) Color(0xFFD1D5DB) else defaultBorder, CircleShape)
                        .clickable(onClick = onSkipPrevious)
                        .testTag("btn_skip_prev"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Skip Previous",
                        tint = prevBtnColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Large Play/Pause Button in Theme Accent
                val playBtnBg = cardAccent
                val playBtnColor = getContrastingTextColor(playBtnBg)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = cardAccent.copy(alpha = if (isThemeLight) 0.18f else 0.35f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(playBtnBg)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("btn_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = playBtnColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Next Button
                val nextBtnBg = if (isThemeLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant
                val nextBtnColor = getContrastingTextColor(nextBtnBg)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(nextBtnBg)
                        .border(1.dp, if (isThemeLight) Color(0xFFD1D5DB) else defaultBorder, CircleShape)
                        .clickable(onClick = onSkipNext)
                        .testTag("btn_skip_next"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Next",
                        tint = nextBtnColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(44.dp).testTag("btn_repeat")
                ) {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Toggle Repeat Mode",
                        tint = if (repeatMode != RepeatMode.OFF) cardAccent else (if (isThemeLight) Color(0xFF6B7280) else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Up & Down Space-Bar Position (Positioned below play & pause controls)
            SpaceBarVolumeControl(
                accentColor = if (isThemeLight && cardAccent == Color.Black) Color(0xFF111827) else cardAccent,
                borderColor = if (isThemeLight) Color(0xFFE5E7EB) else defaultBorder,
                textColor = if (isThemeLight) Color(0xFF374151) else defaultSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}

@Composable
fun SpaceBarVolumeControl(
    accentColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVol = remember(audioManager) { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var currentVol by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2))
    }

    val volumeFraction = (currentVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)

    val isSurfaceLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val effectiveButtonIconTint = if (isSurfaceLight) {
        if (accentColor.luminance() <= 0.60f && accentColor != Color.Black) accentColor else Color(0xFF111827)
    } else {
        accentColor
    }

    Card(
        modifier = modifier.testTag("volume_space_bar_container"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSurfaceLight) Color(0xFFF9FAFB) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, if (isSurfaceLight) Color(0xFFE5E7EB) else borderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Volume Down "-" Button
            val volDownBg = if (isSurfaceLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant
            val volDownColor = getContrastingTextColor(volDownBg)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(volDownBg)
                    .border(1.dp, if (isSurfaceLight) Color(0xFFD1D5DB) else borderColor.copy(alpha = 0.6f), CircleShape)
                    .clickable {
                        val newVol = (currentVol - 1).coerceAtLeast(0)
                        currentVol = newVol
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                    }
                    .testTag("btn_volume_down"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Volume Down (-)",
                    tint = volDownColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Space-Bar Volume Slider & Visual Track
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = volumeFraction,
                    onValueChange = { frac ->
                        val targetVol = (frac * maxVol).toInt().coerceIn(0, maxVol)
                        currentVol = targetVol
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .testTag("slider_volume_space_bar"),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = if (isSurfaceLight) Color(0xFFE5E7EB) else borderColor.copy(alpha = 0.5f)
                    )
                )
            }

            // Volume Up "+" Button
            val volUpBg = if (isSurfaceLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant
            val volUpColor = getContrastingTextColor(volUpBg)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(volUpBg)
                    .border(1.dp, if (isSurfaceLight) Color(0xFFD1D5DB) else borderColor.copy(alpha = 0.6f), CircleShape)
                    .clickable {
                        val newVol = (currentVol + 1).coerceAtMost(maxVol)
                        currentVol = newVol
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                    }
                    .testTag("btn_volume_up"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Volume Up (+)",
                    tint = volUpColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
