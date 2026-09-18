package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.audio.AlbumArtExtractor
import com.example.audio.AlbumArtSearchResult
import com.example.audio.AlbumArtSearchService
import com.example.audio.ArtSearchProvider
import com.example.model.AudioTrack
import com.example.ui.theme.getContrastingTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ComprehensiveAudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val composer: String,
    val genre: String,
    val year: String,
    val trackNumber: String,
    val discNumber: String,
    val formatDisplay: String,
    val mimeType: String,
    val sampleRateHzDisplay: String,
    val bitrateKbpsDisplay: String,
    val channelsDisplay: String,
    val gaplessDisplay: String,
    val fileSizeMbKbDisplay: String,
    val durationDisplay: String,
    val subfolderLocation: String,
    val relativePath: String,
    val dateModifiedDisplay: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailBottomSheet(
    track: AudioTrack,
    onDismiss: () -> Unit,
    onSaveTrackTags: (
        trackId: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        composer: String,
        genre: String,
        year: String,
        trackNumber: String,
        discNumber: String,
        newCoverArt: Bitmap?
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor = MaterialTheme.colorScheme.background
    val primaryContrastingTextColor = getContrastingTextColor(primaryColor)

    var isEditingTags by remember { mutableStateOf(false) }
    var showArtSearchDialog by remember { mutableStateOf(false) }

    var currentTrackState by remember(track) { mutableStateOf(track) }

    // Tag Editor States
    var editTitle by remember(currentTrackState.id, currentTrackState.title) { mutableStateOf(currentTrackState.title) }
    var editArtist by remember(currentTrackState.id, currentTrackState.artist) { mutableStateOf(currentTrackState.artist) }
    var editAlbum by remember(currentTrackState.id, currentTrackState.album) { mutableStateOf(currentTrackState.album) }
    var editAlbumArtist by remember(currentTrackState.id, currentTrackState.albumArtist) { mutableStateOf(if (currentTrackState.albumArtist.isNotBlank()) currentTrackState.albumArtist else currentTrackState.artist) }
    var editComposer by remember(currentTrackState.id, currentTrackState.composer) { mutableStateOf(if (currentTrackState.composer.isNotBlank()) currentTrackState.composer else "Unknown Composer") }
    var editGenre by remember(currentTrackState.id, currentTrackState.genre) { mutableStateOf(if (currentTrackState.genre.isNotBlank()) currentTrackState.genre else "Audio / Music") }
    var editYear by remember(currentTrackState.id, currentTrackState.year) { mutableStateOf(if (currentTrackState.year.isNotBlank()) currentTrackState.year else "Unknown Year") }
    var editTrackNumber by remember(currentTrackState.id, currentTrackState.trackNumber) { mutableStateOf(if (currentTrackState.trackNumber.isNotBlank()) currentTrackState.trackNumber else "1") }
    var editDiscNumber by remember(currentTrackState.id, currentTrackState.discNumber) { mutableStateOf(if (currentTrackState.discNumber.isNotBlank()) currentTrackState.discNumber else "1") }

    // Artwork States
    var currentArtBitmap by remember(currentTrackState.id) { mutableStateOf<Bitmap?>(null) }
    var pendingCoverArt by remember(currentTrackState.id) { mutableStateOf<Bitmap?>(null) }
    var isNewArtStaged by remember(currentTrackState.id) { mutableStateOf(false) }

    LaunchedEffect(currentTrackState.id) {
        withContext(Dispatchers.IO) {
            val art = AlbumArtExtractor.extractCoverArt(context, currentTrackState.uri, currentTrackState.id)
            withContext(Dispatchers.Main) {
                currentArtBitmap = art
            }
        }
    }

    // Gallery Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val pickedBitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (pickedBitmap != null) {
                        withContext(Dispatchers.Main) {
                            pendingCoverArt = pickedBitmap
                            isNewArtStaged = true
                            Toast.makeText(context, "Gallery artwork selected! Click Save Tags to persist.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val meta = remember(currentTrackState.id, currentTrackState.title, currentTrackState.artist, currentTrackState.album, currentTrackState.albumArtist, currentTrackState.composer, currentTrackState.genre, currentTrackState.year, currentTrackState.trackNumber, currentTrackState.discNumber, editTitle, editArtist, editAlbum, editAlbumArtist, editComposer, editGenre, editYear, editTrackNumber, editDiscNumber) {
        var mimeVal = "audio/${currentTrackState.fileExtension}"
        var sampleRateVal = "44,100 Hz (44.1 kHz)"
        var bitrateKbps = currentTrackState.calculatedBitrateKbps
        var numChannels = 2

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, currentTrackState.uri)

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let { mimeVal = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.let {
                val hz = it.toIntOrNull() ?: 44100
                sampleRateVal = "%,d Hz (%.1f kHz)".format(Locale.US, hz, hz / 1000f)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let {
                val br = it.toIntOrNull()
                if (br != null && br > 0) {
                    bitrateKbps = br / 1000
                }
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.let {
                val ch = it.toIntOrNull()
                if (ch != null && ch > 0) numChannels = ch
            }
            retriever.release()
        } catch (_: Exception) {
            // MediaMetadataRetriever fallback
        }

        val sizeFormatted = if (currentTrackState.sizeBytes > 0) {
            val mb = currentTrackState.sizeBytes.toDouble() / (1024.0 * 1024.0)
            val kb = currentTrackState.sizeBytes.toDouble() / 1024.0
            "%.2f MB (%,d KB)".format(Locale.US, mb, kb.toLong())
        } else {
            "Unknown Size"
        }

        val channelsFormatted = when (numChannels) {
            1 -> "1 Channel (Mono)"
            2 -> "2 Channels (Stereo)"
            else -> "$numChannels Channels (Multichannel Surround)"
        }

        val bitrateFormatted = if (bitrateKbps > 0) "$bitrateKbps kbps" else "Unknown / Variable Bitrate"

        val dateFormatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val formattedDate = if (currentTrackState.dateModifiedMs > 0) dateFormatter.format(Date(currentTrackState.dateModifiedMs)) else "Unknown Date"

        ComprehensiveAudioMetadata(
            title = currentTrackState.title,
            artist = currentTrackState.artist,
            album = currentTrackState.album,
            albumArtist = if (currentTrackState.albumArtist.isNotBlank()) currentTrackState.albumArtist else currentTrackState.artist,
            composer = if (currentTrackState.composer.isNotBlank()) currentTrackState.composer else "Unknown Composer",
            genre = if (currentTrackState.genre.isNotBlank()) currentTrackState.genre else "Audio / Music",
            year = if (currentTrackState.year.isNotBlank()) currentTrackState.year else "Unknown Year",
            trackNumber = if (currentTrackState.trackNumber.isNotBlank()) currentTrackState.trackNumber else "1",
            discNumber = if (currentTrackState.discNumber.isNotBlank()) currentTrackState.discNumber else "1",
            formatDisplay = currentTrackState.fileExtension.uppercase(Locale.getDefault()),
            mimeType = mimeVal,
            sampleRateHzDisplay = sampleRateVal,
            bitrateKbpsDisplay = bitrateFormatted,
            channelsDisplay = channelsFormatted,
            gaplessDisplay = "Supported (Sample Accurate)",
            fileSizeMbKbDisplay = sizeFormatted,
            durationDisplay = currentTrackState.durationFormatted,
            subfolderLocation = currentTrackState.subfolderName.ifBlank { "Root Storage" },
            relativePath = currentTrackState.relativePath.ifBlank { currentTrackState.uri.lastPathSegment ?: "Local Audio File" },
            dateModifiedDisplay = formattedDate
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag Indicator
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.6f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header with Close and Mode Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditingTags) "Edit ID3 Tags & Artwork" else "Audio Track Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEditingTags) "Search & replace cover art, modify song metadata" else "High-fidelity audio specifications & ID3 tags",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).testTag("btn_close_track_details")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isEditingTags) {
                // VIEW DETAILS MODE
                // Hero Banner Card: Track Title, Artist, Format Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art or Placeholder
                        Surface(
                            modifier = Modifier.size(68.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = containerColor,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            val activeArt = pendingCoverArt ?: currentArtBitmap
                            if (activeArt != null) {
                                Image(
                                    bitmap = activeArt.asImageBitmap(),
                                    contentDescription = "Album Artwork",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meta.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = meta.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpecBadge(meta.formatDisplay, primaryColor, containerColor)
                                SpecBadge(meta.durationDisplay, secondaryColor, containerColor)
                                SpecBadge(meta.bitrateKbpsDisplay, MaterialTheme.colorScheme.onSurfaceVariant, containerColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: ID3 Tag Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(title = "METADATA & ID3 TAGS", color = primaryColor)

                        DetailRow(Icons.Default.MusicNote, "Title", meta.title, primaryColor)
                        DetailRow(Icons.Default.Person, "Artist", meta.artist, primaryColor)
                        DetailRow(Icons.Default.Album, "Album", meta.album, primaryColor)
                        DetailRow(Icons.Default.Person, "Album Artist", meta.albumArtist, primaryColor)
                        DetailRow(Icons.Default.LibraryMusic, "Composer", meta.composer, primaryColor)
                        DetailRow(Icons.Default.Category, "Genre", meta.genre, primaryColor)
                        DetailRow(Icons.Default.DateRange, "Release Year", meta.year, primaryColor)
                        DetailRow(Icons.Default.Info, "Track / Disc", "Track ${meta.trackNumber} • Disc ${meta.discNumber}", primaryColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Technical Specifications
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(title = "TECHNICAL SPECIFICATIONS", color = secondaryColor)

                        DetailRow(Icons.Default.GraphicEq, "Sample Rate", meta.sampleRateHzDisplay, secondaryColor)
                        DetailRow(Icons.Default.Speed, "Bitrate", meta.bitrateKbpsDisplay, secondaryColor)
                        DetailRow(Icons.Default.Headphones, "Channels", meta.channelsDisplay, secondaryColor)
                        DetailRow(Icons.Default.Schedule, "Gapless Audio", meta.gaplessDisplay, secondaryColor)
                        DetailRow(Icons.Default.AudioFile, "MIME Type", meta.mimeType, secondaryColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Storage & File Location
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(title = "FILE & STORAGE DETAILS", color = MaterialTheme.colorScheme.onSurfaceVariant)

                        DetailRow(Icons.Default.Storage, "File Size", meta.fileSizeMbKbDisplay, MaterialTheme.colorScheme.onSurfaceVariant)
                        DetailRow(Icons.Default.Folder, "Subfolder", meta.subfolderLocation, MaterialTheme.colorScheme.onSurfaceVariant)
                        DetailRow(Icons.Default.Info, "Path / Location", meta.relativePath, MaterialTheme.colorScheme.onSurfaceVariant)
                        DetailRow(Icons.Default.DateRange, "Date Modified", meta.dateModifiedDisplay, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Edit ID3 Tags Action Button
                Button(
                    onClick = { isEditingTags = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_start_edit_id3"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = primaryContrastingTextColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = primaryContrastingTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit ID3 Tags & Album Art",
                            fontWeight = FontWeight.Bold,
                            color = primaryContrastingTextColor,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // ==================== EDIT TAGS FORM MODE ====================

                // Section: ALBUM ARTWORK SEARCH, REPLACE & PREVIEW
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, if (isNewArtStaged) primaryColor else borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ALBUM ARTWORK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 0.8.sp
                            )

                            if (isNewArtStaged) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primaryColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, primaryColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "New Art Staged",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = primaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Artwork Preview Box
                            Surface(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                color = containerColor,
                                border = BorderStroke(2.dp, if (isNewArtStaged) primaryColor else borderColor)
                            ) {
                                val previewBitmap = pendingCoverArt ?: currentArtBitmap
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap.asImageBitmap(),
                                        contentDescription = "Cover Art Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Album,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }

                            // Artwork Action Buttons Column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Search Online Album Art Button
                                Button(
                                    onClick = { showArtSearchDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("btn_search_online_art"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ImageSearch,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Search Online Art",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // 2. Pick from Device Gallery Button
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("btn_pick_gallery_art"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, borderColor),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pick from Gallery",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // 3. Reset Art Button if staged
                                if (isNewArtStaged) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .clickable {
                                                pendingCoverArt = null
                                                isNewArtStaged = false
                                            }
                                            .padding(top = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Revert to Original Art",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: ID3 TAG TEXT FIELDS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "EDIT ID3 TAG VALUES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            letterSpacing = 0.8.sp
                        )

                        TagInputField(
                            label = "Title",
                            value = editTitle,
                            onValueChange = { editTitle = it }
                        )

                        TagInputField(
                            label = "Artist",
                            value = editArtist,
                            onValueChange = { editArtist = it }
                        )

                        TagInputField(
                            label = "Album",
                            value = editAlbum,
                            onValueChange = { editAlbum = it }
                        )

                        TagInputField(
                            label = "Album Artist",
                            value = editAlbumArtist,
                            onValueChange = { editAlbumArtist = it }
                        )

                        TagInputField(
                            label = "Composer",
                            value = editComposer,
                            onValueChange = { editComposer = it }
                        )

                        TagInputField(
                            label = "Genre",
                            value = editGenre,
                            onValueChange = { editGenre = it }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TagInputField(
                                    label = "Year",
                                    value = editYear,
                                    onValueChange = { editYear = it }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TagInputField(
                                    label = "Track #",
                                    value = editTrackNumber,
                                    onValueChange = { editTrackNumber = it }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TagInputField(
                                    label = "Disc #",
                                    value = editDiscNumber,
                                    onValueChange = { editDiscNumber = it }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons for Form Mode (Cancel / Save Tags)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isEditingTags = false
                            pendingCoverArt = null
                            isNewArtStaged = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val finalCoverArt = pendingCoverArt
                            val updated = currentTrackState.copy(
                                title = editTitle.ifBlank { currentTrackState.title },
                                artist = editArtist.ifBlank { currentTrackState.artist },
                                album = editAlbum.ifBlank { currentTrackState.album },
                                albumArtist = editAlbumArtist,
                                composer = editComposer,
                                genre = editGenre,
                                year = editYear,
                                trackNumber = editTrackNumber,
                                discNumber = editDiscNumber
                            )
                            currentTrackState = updated
                            if (finalCoverArt != null) {
                                currentArtBitmap = finalCoverArt
                                pendingCoverArt = null
                                isNewArtStaged = false
                            }

                            onSaveTrackTags(
                                currentTrackState.id,
                                editTitle,
                                editArtist,
                                editAlbum,
                                editAlbumArtist,
                                editComposer,
                                editGenre,
                                editYear,
                                editTrackNumber,
                                editDiscNumber,
                                finalCoverArt
                            )
                            Toast.makeText(
                                context,
                                if (finalCoverArt != null) "Tags & Album Art saved permanently!" else "ID3 Tags updated permanently!",
                                Toast.LENGTH_SHORT
                            ).show()
                            isEditingTags = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("btn_save_id3_tags"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = primaryContrastingTextColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = primaryContrastingTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isNewArtStaged) "Save & Replace" else "Save Tags",
                            fontWeight = FontWeight.Bold,
                            color = primaryContrastingTextColor
                        )
                    }
                }
            }
        }
    }

    // ONLINE ALBUM ART FINDER & DOWNLOADER DIALOG
    if (showArtSearchDialog) {
        AlbumArtSearchDialog(
            initialQuery = "${editArtist.ifBlank { track.artist }} ${editTitle.ifBlank { track.title }}".trim(),
            defaultArtist = editArtist.ifBlank { track.artist },
            defaultAlbum = editAlbum.ifBlank { track.album },
            defaultTitle = editTitle.ifBlank { track.title },
            onDismiss = { showArtSearchDialog = false },
            onArtworkSelected = { downloadedBitmap, result ->
                pendingCoverArt = downloadedBitmap
                isNewArtStaged = true

                // Auto-fill any blank fields from the selected result
                if (editAlbum.isBlank() || editAlbum.equals("Unknown Album", ignoreCase = true) || editAlbum.equals("Local Audio", ignoreCase = true)) {
                    editAlbum = result.albumName
                }
                if (result.year != null && (editYear.isBlank() || editYear.equals("Unknown Year", ignoreCase = true))) {
                    editYear = result.year
                }
                if (result.genre != null && (editGenre.isBlank() || editGenre.equals("Audio / Music", ignoreCase = true))) {
                    editGenre = result.genre
                }

                Toast.makeText(context, "Artwork downloaded! Ready to save.", Toast.LENGTH_SHORT).show()
                showArtSearchDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlbumArtSearchDialog(
    initialQuery: String,
    defaultArtist: String,
    defaultAlbum: String,
    defaultTitle: String,
    onDismiss: () -> Unit,
    onArtworkSelected: (Bitmap, AlbumArtSearchResult) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedProvider by remember { mutableStateOf(ArtSearchProvider.ALL) }
    var isLoading by remember { mutableStateOf(false) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<AlbumArtSearchResult>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun performSearch(query: String, provider: ArtSearchProvider = selectedProvider) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        isLoading = true
        errorMessage = null
        hasSearched = true

        coroutineScope.launch {
            try {
                val results = AlbumArtSearchService.searchArtwork(trimmed, provider)
                searchResults = results
                if (results.isEmpty()) {
                    errorMessage = "No matching artwork found on ${provider.displayName}. Try another provider or query."
                }
            } catch (e: Exception) {
                errorMessage = "Search failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Auto search on initial open
    LaunchedEffect(Unit) {
        if (searchQuery.isNotBlank()) {
            performSearch(searchQuery, selectedProvider)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ImageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Find & Download Album Art",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "JioSaavn • Gaana • YouTube • Google • iTunes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search song, movie, artist or album...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_album_art"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = { performSearch(searchQuery, selectedProvider) },
                            enabled = searchQuery.isNotBlank() && !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Provider Switcher Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ArtSearchProvider.values().forEach { providerOption ->
                        val isSelected = selectedProvider == providerOption
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (selectedProvider != providerOption) {
                                        selectedProvider = providerOption
                                        if (searchQuery.isNotBlank()) {
                                            performSearch(searchQuery, providerOption)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                Color(providerOption.badgeColorHex).copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(providerOption.badgeColorHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(providerOption.badgeColorHex))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = providerOption.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(providerOption.badgeColorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Search Suggestion Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (defaultArtist.isNotBlank() && defaultTitle.isNotBlank()) {
                        val q1 = "$defaultArtist - $defaultTitle"
                        FilterChip(
                            selected = searchQuery == q1,
                            onClick = {
                                searchQuery = q1
                                performSearch(q1, selectedProvider)
                            },
                            label = { Text(q1, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (defaultAlbum.isNotBlank() && !defaultAlbum.equals("Unknown Album", ignoreCase = true) && !defaultAlbum.equals("Local Audio", ignoreCase = true)) {
                        val q2 = "$defaultAlbum $defaultArtist".trim()
                        FilterChip(
                            selected = searchQuery == q2,
                            onClick = {
                                searchQuery = q2
                                performSearch(q2, selectedProvider)
                            },
                            label = { Text(q2, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (defaultArtist.isNotBlank() && !defaultArtist.equals("Unknown Artist", ignoreCase = true)) {
                        FilterChip(
                            selected = searchQuery == defaultArtist,
                            onClick = {
                                searchQuery = defaultArtist
                                performSearch(defaultArtist, selectedProvider)
                            },
                            label = { Text(defaultArtist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Results Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Searching ${selectedProvider.displayName}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (errorMessage != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "No results",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (searchResults.isEmpty() && hasSearched) {
                        Text(
                            text = "No album art found. Try another search query.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults, key = { it.id }) { result ->
                                val isDownloading = downloadingId == result.id
                                val providerBadgeColor = when (result.source) {
                                    "JioSaavn" -> Color(0xFF00D26A)
                                    "Gaana" -> Color(0xFFE72C30)
                                    "YouTube Music" -> Color(0xFFFF2B2B)
                                    "Google / Web" -> Color(0xFF4285F4)
                                    "Deezer" -> Color(0xFFFF0055)
                                    else -> Color(0xFFFC3C44) // iTunes / Apple
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Async Image Preview
                                        Surface(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            AsyncImage(
                                                model = result.previewArtworkUrl,
                                                contentDescription = result.albumName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            // Source badge tag
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = providerBadgeColor.copy(alpha = 0.15f),
                                                border = BorderStroke(0.5.dp, providerBadgeColor.copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = result.source,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = providerBadgeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = result.trackTitle.ifBlank { result.albumName },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = result.artistName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (result.albumName.isNotBlank()) {
                                                Text(
                                                    text = result.albumName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Download & Apply Button
                                        val selectBtnBg = MaterialTheme.colorScheme.primary
                                        val selectBtnTextColor = getContrastingTextColor(selectBtnBg)
                                        Button(
                                            onClick = {
                                                downloadingId = result.id
                                                coroutineScope.launch {
                                                    val bitmap = AlbumArtSearchService.downloadArtwork(result.highResArtworkUrl)
                                                        ?: AlbumArtSearchService.downloadArtwork(result.previewArtworkUrl)
                                                    downloadingId = null
                                                    if (bitmap != null) {
                                                        onArtworkSelected(bitmap, result)
                                                    }
                                                }
                                            },
                                            enabled = !isDownloading,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = selectBtnBg,
                                                contentColor = selectBtnTextColor
                                            ),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            if (isDownloading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = selectBtnTextColor,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudDownload,
                                                        contentDescription = null,
                                                        tint = selectBtnTextColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Select",
                                                        fontSize = 11.sp,
                                                        color = selectBtnTextColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun SpecBadge(text: String, color: Color, containerColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
