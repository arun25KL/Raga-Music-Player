package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AlbumArtExtractor
import com.example.model.AudioTrack
import com.example.model.TrackSortOrder
import com.example.ui.theme.ArtisticDarkBorder
import com.example.ui.theme.ArtisticDarkContainer
import com.example.ui.theme.ArtisticDarkSurface
import com.example.ui.theme.ArtisticGoldenYellow
import com.example.ui.theme.ArtisticPeachGold
import com.example.ui.theme.ArtisticSunsetOrange
import com.example.ui.theme.getContrastingTextColor
import com.example.ui.theme.getContrastingSecondaryTextColor
import com.example.ui.theme.ArtisticTextPrimary
import com.example.ui.theme.ArtisticTextSecondary
import com.example.ui.theme.ArtisticWarmAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlaylistSection(
    tracks: List<AudioTrack>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    sessionPlayedIds: Set<String>,
    trackSortOrder: TrackSortOrder,
    onSortOrderChanged: (TrackSortOrder) -> Unit,
    onTrackSelected: (AudioTrack) -> Unit,
    onResetSession: () -> Unit,
    onPlayNew: (() -> Unit)? = null,
    onShowTrackDetails: ((AudioTrack) -> Unit)? = null,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    headerContent: (@Composable () -> Unit)? = null
) {
    val totalTracks = tracks.size
    val playedCount = tracks.count { it.id in sessionPlayedIds }
    val unplayedCount = (totalTracks - playedCount).coerceAtLeast(0)
    val sessionProgress = if (totalTracks > 0) playedCount.toFloat() / totalTracks.toFloat() else 0f

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }

        // Session Tracker Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SESSION PLAYED TRACKS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )

                        val counterBg = MaterialTheme.colorScheme.primary
                        val counterTextColor = getContrastingTextColor(counterBg)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = counterBg,
                            modifier = Modifier.testTag("session_counter_badge")
                        ) {
                            Text(
                                text = "$playedCount / $totalTracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = counterTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { sessionProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$unplayedCount unplayed remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )

                        // Reset Session Action
                        val resetBg = MaterialTheme.colorScheme.surfaceVariant
                        val resetTextColor = getContrastingTextColor(resetBg)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = resetBg,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.clickable(onClick = onResetSession).testTag("btn_reset_session")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = resetTextColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = resetTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (onPlayNew != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Play New Track Action Button inside Session Tracker
                        val playNewBg = MaterialTheme.colorScheme.primary
                        val playNewContentColor = getContrastingTextColor(playNewBg)
                        Button(
                            onClick = onPlayNew,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_play_new_session"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = playNewBg,
                                contentColor = playNewContentColor
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = playNewContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play New Track",
                                    fontWeight = FontWeight.Bold,
                                    color = playNewContentColor,
                                    fontSize = 13.sp
                                )
                                if (unplayedCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = playNewContentColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, playNewContentColor.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "$unplayedCount left",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = playNewContentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gap Icon Indicator Divider above Tracklist
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Track list gap divider",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                }
            }
        }

        // Section Title & Sorting Toggle Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TRACKLIST",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$totalTracks tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Sorting Option Button (Alphabetical A-Z, New to Old, Old to New, Folder wise)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable {
                            val next = when (trackSortOrder) {
                                TrackSortOrder.ALPHABETICAL -> TrackSortOrder.NEWEST_FIRST
                                TrackSortOrder.NEWEST_FIRST -> TrackSortOrder.OLDEST_FIRST
                                TrackSortOrder.OLDEST_FIRST -> TrackSortOrder.FOLDER_WISE
                                TrackSortOrder.FOLDER_WISE -> TrackSortOrder.ALPHABETICAL
                            }
                            onSortOrderChanged(next)
                        }
                        .testTag("btn_cycle_sort_order")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (trackSortOrder) {
                                TrackSortOrder.ALPHABETICAL -> Icons.Default.SortByAlpha
                                TrackSortOrder.NEWEST_FIRST, TrackSortOrder.OLDEST_FIRST -> Icons.Default.SwapVert
                                TrackSortOrder.FOLDER_WISE -> Icons.Default.Folder
                            },
                            contentDescription = "Sort order",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trackSortOrder.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Track List items
        if (tracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No audio tracks found in this folder",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add folders or scan device storage in Settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id }
            ) { index, track ->
                val isActive = currentTrack?.id == track.id
                val isPlayedInSession = track.id in sessionPlayedIds

                TrackItemCard(
                    track = track,
                    index = index + 1,
                    isActive = isActive,
                    isPlaying = isActive && isPlaying,
                    isPlayedInSession = isPlayedInSession,
                    onClick = {
                        if (!isActive || !isPlaying) {
                            onTrackSelected(track)
                        }
                    },
                    onInfoClick = onShowTrackDetails?.let { { it(track) } }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TrackThumbnailArt(
    track: AudioTrack,
    index: Int,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnail by remember(track.id) { mutableStateOf<Bitmap?>(null) }
    val isSurfaceLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val activeOverlayTint = if (MaterialTheme.colorScheme.primary.luminance() < 0.25f) Color.White else MaterialTheme.colorScheme.primary
    val fallbackIconTint = if (isSurfaceLight && MaterialTheme.colorScheme.primary.luminance() > 0.65f) {
        Color(0xFF1D4ED8)
    } else if (isSurfaceLight && MaterialTheme.colorScheme.primary == Color.Black) {
        Color(0xFF111827)
    } else {
        MaterialTheme.colorScheme.primary
    }

    LaunchedEffect(track.id) {
        withContext(Dispatchers.IO) {
            thumbnail = AlbumArtExtractor.extractThumbnail(context, track.uri, track.id)
        }
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSurfaceLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                if (isActive) fallbackIconTint else (if (isSurfaceLight) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.outline),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = thumbnail
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Track Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Active overlay
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        EqualizerAnimation(color = activeOverlayTint)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = activeOverlayTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // Track number or Equalizer animation fallback
            if (isActive && isPlaying) {
                EqualizerAnimation(color = fallbackIconTint)
            } else if (isActive) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = fallbackIconTint,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSurfaceLight) Color(0xFF4B5563) else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackItemCard(
    track: AudioTrack,
    index: Int,
    isActive: Boolean,
    isPlaying: Boolean,
    isPlayedInSession: Boolean,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    val isSurfaceLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val effectiveActivePrimary = if (isSurfaceLight) {
        if (MaterialTheme.colorScheme.primary.luminance() > 0.65f) Color(0xFF1D4ED8)
        else if (MaterialTheme.colorScheme.primary == Color.Black || MaterialTheme.colorScheme.primary.luminance() < 0.15f) Color(0xFF111827)
        else MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (!isActive || !isPlaying) {
                        onClick()
                    }
                },
                onLongClick = { onInfoClick?.invoke() }
            )
            .testTag("track_item_${track.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                if (isSurfaceLight) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                if (isSurfaceLight) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isActive) {
            BorderStroke(1.5.dp, if (isSurfaceLight && effectiveActivePrimary == Color(0xFF111827)) Color(0xFF374151) else effectiveActivePrimary)
        } else {
            BorderStroke(1.dp, if (isSurfaceLight) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Album Art Thumbnail / Number container
            TrackThumbnailArt(
                track = track,
                index = index,
                isActive = isActive,
                isPlaying = isPlaying
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Artist, Format badge and File Size
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) {
                        effectiveActivePrimary
                    } else {
                        if (isSurfaceLight) {
                            if (isPlayedInSession) Color(0xFF6B7280) else Color(0xFF111827)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isPlayedInSession) 0.65f else 1f)
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("track_item_audio_portion_${track.id}")
                ) {
                    // Audio Format Badge in Theme colors
                    val formatBadgeBg = if (isSurfaceLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant
                    val formatBadgeTextColor = getContrastingTextColor(formatBadgeBg)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = formatBadgeBg,
                        border = BorderStroke(1.dp, if (isSurfaceLight) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = track.format.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = formatBadgeTextColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // File size badge
                    if (track.sizeFormatted.isNotBlank()) {
                        Text(
                            text = track.sizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isSurfaceLight) Color(0xFF4B5563) else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isSurfaceLight) Color(0xFF9CA3AF) else MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSurfaceLight) Color(0xFF4B5563) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Session Status Badge & Duration (Playing when active, Played if >= 60%, Fresh otherwise)
            Column(horizontalAlignment = Alignment.End) {
                if (track.durationMs > 0) {
                    Text(
                        text = track.durationFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) effectiveActivePrimary else (if (isSurfaceLight) Color(0xFF4B5563) else MaterialTheme.colorScheme.secondary),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val isTrackPlaying = isActive && isPlaying
                val statusText = when {
                    isTrackPlaying -> "Playing"
                    isPlayedInSession -> "Played"
                    else -> "Fresh"
                }
                val statusBgColor = when {
                    isTrackPlaying -> MaterialTheme.colorScheme.primary
                    isPlayedInSession -> if (isSurfaceLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant
                    else -> if (isSurfaceLight) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                }
                val statusTextColor = getContrastingTextColor(statusBgColor)

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBgColor,
                    border = BorderStroke(
                        1.dp,
                        when {
                            isTrackPlaying -> Color.Transparent
                            isPlayedInSession -> if (isSurfaceLight) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.outline
                            else -> if (isSurfaceLight) Color(0xFFBFDBFE) else MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor,
                        fontSize = 10.sp,
                        fontWeight = if (isTrackPlaying || !isPlayedInSession) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerAnimation(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerAnim")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar3"
    )

    Row(
        modifier = modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height((16 * h1).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height((16 * h2).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height((16 * h3).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
    }
}
