package com.example

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AudioTrack
import com.example.player.MusicPlayerViewModel
import com.example.ui.components.AudioFxBottomSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSection
import com.example.ui.components.PlaylistSection
import com.example.ui.components.SettingsAndSourceDialog
import com.example.ui.components.TrackDetailBottomSheet
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerApp(viewModel = viewModel)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveCurrentPlaybackState()
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveCurrentPlaybackState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp(
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val appThemePreset by viewModel.appThemePreset.collectAsStateWithLifecycle()
    val isOledPureBlackEnabled by viewModel.isOledPureBlackEnabled.collectAsStateWithLifecycle()
    val isDynamicArtworkColorEnabled by viewModel.isDynamicArtworkColorEnabled.collectAsStateWithLifecycle()
    val dynamicArtworkPalette by viewModel.dynamicArtworkPalette.collectAsStateWithLifecycle()

    MyApplicationTheme(
        themePreset = appThemePreset,
        isOledPureBlack = isOledPureBlackEnabled,
        isDynamicArtworkColorEnabled = isDynamicArtworkColorEnabled,
        dynamicArtworkPalette = dynamicArtworkPalette
    ) {
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val currentCoverArt by viewModel.currentCoverArt.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPos by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val sessionPlayedIds by viewModel.sessionPlayedIds.collectAsStateWithLifecycle()
    val unplayedCount by viewModel.unplayedCount.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val activeTracks by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val trackSortOrder by viewModel.trackSortOrder.collectAsStateWithLifecycle()
    val sessionMessage by viewModel.sessionMessage.collectAsStateWithLifecycle()

    // Multi-folder locations and format support
    val folderLocations by viewModel.folderLocations.collectAsStateWithLifecycle()
    val enabledFormats by viewModel.enabledFormats.collectAsStateWithLifecycle()

    // FX & Settings States
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsStateWithLifecycle()
    val isGaplessEnabled by viewModel.isGaplessEnabled.collectAsStateWithLifecycle()
    val isReplayGainEnabled by viewModel.isReplayGainEnabled.collectAsStateWithLifecycle()
    val superBoostPercent by viewModel.superBoostPercent.collectAsStateWithLifecycle()
    val isDynamicThemeEnabled by viewModel.isDynamicThemeEnabled.collectAsStateWithLifecycle()
    val isOnlyCallsInterruptEnabled by viewModel.isOnlyCallsInterruptEnabled.collectAsStateWithLifecycle()
    val albumPalette by viewModel.albumPalette.collectAsStateWithLifecycle()

    // Subfolder states
    val subfolders by viewModel.subfolders.collectAsStateWithLifecycle()
    val includeSubfolders by viewModel.includeSubfolders.collectAsStateWithLifecycle()
    val excludedSubfolderIds by viewModel.excludedSubfolderIds.collectAsStateWithLifecycle()

    val isScanningBackground by viewModel.isScanningBackground.collectAsStateWithLifecycle()
    val infiniteTransition = rememberInfiniteTransition(label = "spin_refresh")
    val refreshAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "refresh_angle"
    )

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAudioFxSheet by remember { mutableStateOf(false) }
    var selectedTrackForDetail by remember { mutableStateOf<AudioTrack?>(null) }

    val playlistListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Folder Picker Launcher (SAF OpenDocumentTree) for adding a folder location
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.addFolderUri(treeUri)
        }
    }

    // Permission launcher for device audio scanning
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanDeviceAudio()
        } else {
            Toast.makeText(context, "Storage permission required to scan device audio", Toast.LENGTH_SHORT).show()
        }
    }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            if (viewModel.folderLocations.value.isEmpty()) {
                viewModel.scanDeviceAudio(isInitial = true)
            }
        }
    }

    // Observe session messages
    LaunchedEffect(sessionMessage) {
        sessionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSessionMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
        },
        bottomBar = {
            MiniPlayerBar(
                visible = currentTrack != null,
                track = currentTrack,
                coverArt = currentCoverArt,
                isPlaying = isPlaying,
                currentPositionMs = currentPos,
                durationMs = duration,
                albumPalette = albumPalette,
                isDynamicThemeEnabled = isDynamicThemeEnabled,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onClick = {
                    // Single click on miniplayer: goes to mainscreen (Now Playing section at top)
                    coroutineScope.launch {
                        playlistListState.animateScrollToItem(0)
                    }
                },
                onDoubleClick = {
                    // Double click on miniplayer: goes to track (scrolls to the playing track in playlist)
                    val trackIndex = activeTracks.indexOfFirst { it.id == currentTrack?.id }
                    if (trackIndex >= 0) {
                        coroutineScope.launch {
                            val headerOffset = 2
                            playlistListState.animateScrollToItem((trackIndex + headerOffset).coerceAtLeast(0))
                        }
                    }
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.refreshLibrary() },
                        modifier = Modifier.testTag("top_bar_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Search for new audio files",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = if (isScanningBackground) Modifier.rotate(refreshAngle) else Modifier
                        )
                    }

                    // Audio FX / Equalizer button
                    IconButton(
                        onClick = { showAudioFxSheet = true },
                        modifier = Modifier.testTag("top_bar_fx_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio FX & Equalizer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Settings & Multi-Folder / Format Manager button
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("top_bar_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings and Source Locations",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Shutdown and Close App button
                    val activity = LocalContext.current as? Activity
                    IconButton(
                        onClick = {
                            viewModel.shutdownApp(activity)
                        },
                        modifier = Modifier.testTag("top_bar_shutdown_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Close and Shutdown App",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 700.dp

            if (isExpanded) {
                // Tablet / Landscape 2-pane layout
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        NowPlayingSection(
                            track = currentTrack,
                            coverArt = currentCoverArt,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPos,
                            durationMs = duration,
                            sessionPlayedIds = sessionPlayedIds,
                            unplayedCount = unplayedCount,
                            repeatMode = repeatMode,
                            isShuffleEnabled = isShuffleEnabled,
                            playbackSpeed = playbackSpeed,
                            sleepTimerState = sleepTimerState,
                            albumPalette = albumPalette,
                            isDynamicThemeEnabled = isDynamicThemeEnabled,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onSkipPrevious = { viewModel.skipPrevious() },
                            onSeekTo = { pos -> viewModel.seekTo(pos) },
                            onPlayNew = { viewModel.playNewTrack() },
                            onToggleRepeat = { viewModel.toggleRepeatMode() },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onOpenAudioFx = { showAudioFxSheet = true },
                            onOpenTrackDetails = { currentTrack?.let { selectedTrackForDetail = it } }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    ) {
                        PlaylistSection(
                            tracks = activeTracks,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            sessionPlayedIds = sessionPlayedIds,
                            trackSortOrder = trackSortOrder,
                            onSortOrderChanged = { viewModel.setTrackSortOrder(it) },
                            onTrackSelected = { track -> viewModel.playTrack(track, forceRestart = false, isUserAction = true) },
                            onResetSession = { viewModel.resetSessionHistory() },
                            onPlayNew = { viewModel.playNewTrack() },
                            onShowTrackDetails = { track -> selectedTrackForDetail = track },
                            listState = playlistListState
                        )
                    }
                }
            } else {
                // Compact Screen Single-view layout
                PlaylistSection(
                    tracks = activeTracks,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    sessionPlayedIds = sessionPlayedIds,
                    trackSortOrder = trackSortOrder,
                    onSortOrderChanged = { viewModel.setTrackSortOrder(it) },
                    onTrackSelected = { track -> viewModel.playTrack(track, forceRestart = false, isUserAction = true) },
                    onResetSession = { viewModel.resetSessionHistory() },
                    onPlayNew = { viewModel.playNewTrack() },
                    onShowTrackDetails = { track -> selectedTrackForDetail = track },
                    listState = playlistListState,
                    headerContent = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            NowPlayingSection(
                                track = currentTrack,
                                coverArt = currentCoverArt,
                                isPlaying = isPlaying,
                                currentPositionMs = currentPos,
                                durationMs = duration,
                                sessionPlayedIds = sessionPlayedIds,
                                unplayedCount = unplayedCount,
                                repeatMode = repeatMode,
                                isShuffleEnabled = isShuffleEnabled,
                                playbackSpeed = playbackSpeed,
                                sleepTimerState = sleepTimerState,
                                albumPalette = albumPalette,
                                isDynamicThemeEnabled = isDynamicThemeEnabled,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onSeekTo = { pos -> viewModel.seekTo(pos) },
                                onToggleRepeat = { viewModel.toggleRepeatMode() },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onOpenAudioFx = { showAudioFxSheet = true },
                                onOpenTrackDetails = { currentTrack?.let { selectedTrackForDetail = it } }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Track Details Modal Bottom Sheet
    selectedTrackForDetail?.let { track ->
        TrackDetailBottomSheet(
            track = track,
            onDismiss = { selectedTrackForDetail = null },
            onSaveTrackTags = { trId, title, artist, album, albumArtist, composer, genre, year, trackNum, discNum, newCoverArt ->
                val updatedTrack = viewModel.updateTrackTags(
                    context, trId, title, artist, album, albumArtist, composer, genre, year, trackNum, discNum, newCoverArt
                )
                if (updatedTrack != null) {
                    selectedTrackForDetail = updatedTrack
                }
            }
        )
    }

    // Audio FX Modal Bottom Sheet (Equalizer, Speed, Sleep Timer, Crossfade, Normalization, Dynamic Theming)
    if (showAudioFxSheet) {
        AudioFxBottomSheet(
            onDismiss = { showAudioFxSheet = false },
            equalizerState = equalizerState,
            playbackSpeed = playbackSpeed,
            sleepTimerState = sleepTimerState,
            crossfadeSeconds = crossfadeSeconds,
            isGaplessEnabled = isGaplessEnabled,
            isReplayGainEnabled = isReplayGainEnabled,
            superBoostPercent = superBoostPercent,
            onToggleEqualizer = { enabled -> viewModel.setEqualizerEnabled(enabled) },
            onSelectPreset = { preset -> viewModel.setEqualizerPreset(preset) },
            onBandLevelChanged = { bandIdx, level -> viewModel.setEqualizerBandLevel(bandIdx, level) },
            onBassBoostChanged = { strength -> viewModel.setBassBoostStrength(strength) },
            onVirtualizerChanged = { strength -> viewModel.setVirtualizerStrength(strength) },
            onPlaybackSpeedChanged = { speed -> viewModel.setPlaybackSpeed(speed) },
            onSelectSleepTimer = { option -> viewModel.setSleepTimer(option) },
            onCrossfadeChanged = { sec -> viewModel.setCrossfadeSeconds(sec) },
            onToggleGapless = { enabled -> viewModel.setGaplessEnabled(enabled) },
            onToggleReplayGain = { enabled -> viewModel.setReplayGainEnabled(enabled) },
            onSuperBoostChanged = { percent -> viewModel.setSuperBoostPercent(percent) }
        )
    }

    // Unified Settings & Multi-Source Locations Sheet
    if (showSettingsSheet) {
        SettingsAndSourceDialog(
            onDismiss = { showSettingsSheet = false },
            folderLocations = folderLocations,
            enabledFormats = enabledFormats,
            subfolders = subfolders,
            includeSubfolders = includeSubfolders,
            excludedSubfolderIds = excludedSubfolderIds,
            appThemePreset = appThemePreset,
            isDynamicThemeEnabled = isDynamicThemeEnabled,
            isOledPureBlackEnabled = isOledPureBlackEnabled,
            isDynamicArtworkColorEnabled = isDynamicArtworkColorEnabled,
            isOnlyCallsInterruptEnabled = isOnlyCallsInterruptEnabled,
            onAddFolderClicked = {
                folderPickerLauncher.launch(null)
            },
            onScanDeviceClicked = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.scanDeviceAudio()
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.scanDeviceAudio()
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            },
            onRemoveFolderLocation = { locId ->
                viewModel.removeFolderLocation(locId)
            },
            onToggleAudioFormat = { format ->
                viewModel.toggleAudioFormat(format)
            },
            onEnableAllFormats = {
                viewModel.enableAllAudioFormats()
            },
            onDisableAllFormats = {
                viewModel.disableAllAudioFormats()
            },
            onToggleIncludeSubfolders = { include ->
                viewModel.setIncludeSubfolders(include)
            },
            onToggleSubfolder = { subId ->
                viewModel.toggleSubfolderExclusion(subId)
            },
            onIncludeAllSubfolders = {
                viewModel.includeAllSubfolders()
            },
            onExcludeAllSubfolders = {
                viewModel.excludeAllSubfolders()
            },
            onSelectAppThemePreset = { preset ->
                viewModel.selectAppThemePreset(preset)
            },
            onToggleDynamicTheme = { enabled ->
                viewModel.setDynamicThemeEnabled(enabled)
            },
            onToggleOledPureBlack = { enabled ->
                viewModel.setOledPureBlackEnabled(enabled)
            },
            onToggleDynamicArtworkColor = { enabled ->
                viewModel.setDynamicArtworkColorEnabled(enabled)
            },
            onToggleOnlyCallsInterrupt = { enabled ->
                viewModel.setOnlyCallsInterruptEnabled(enabled)
            }
        )
    }
    }
}
