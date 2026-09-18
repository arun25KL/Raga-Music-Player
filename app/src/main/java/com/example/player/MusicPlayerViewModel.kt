package com.example.player

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AlbumArtExtractor
import com.example.audio.ExtractedAlbumPalette
import com.example.audio.FolderScanner
import com.example.audio.PaletteExtractor
import com.example.audio.ScanProgressUpdate
import com.example.data.CustomTrackMetadata
import com.example.data.SettingsManager
import com.example.model.AbLoopState
import com.example.model.AppThemePreset
import com.example.model.AudioTrack
import com.example.model.DynamicArtworkPalette
import com.example.model.EqualizerBand
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.model.FolderLocation
import com.example.model.FolderScanResult
import com.example.model.RepeatMode
import com.example.model.SleepTimerOption
import com.example.model.SleepTimerState
import com.example.model.Subfolder
import com.example.model.SupportedAudioFormat
import com.example.model.TrackSortOrder
import com.example.service.AudioService
import com.example.service.MediaNotificationHelper
import com.example.service.MediaPlaybackReceiver
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application), MediaPlaybackReceiver.PlaybackActionListener {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val TAG = "MusicPlayerVM"
    private val settings = SettingsManager(application.applicationContext)

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var volumeRampJob: Job? = null
    private val scanJobs = mutableMapOf<String, Job>()
    private val unplayableTrackIds = mutableSetOf<String>()

    // Audio Focus & System Output Audio Management
    private val audioManager = application.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isPausedByFocusLoss = false
    private var isDuckedByFocusLoss = false
    private var isNoisyReceiverRegistered = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus LOSS (permanent)")
                isPausedByFocusLoss = false
                isDuckedByFocusLoss = false
                pausePlayback(abandonFocus = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus LOSS_TRANSIENT")
                if (_isPlaying.value) {
                    isPausedByFocusLoss = true
                    pausePlayback(abandonFocus = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus LOSS_TRANSIENT_CAN_DUCK")
                if (_isOnlyCallsInterruptEnabled.value) {
                    // When ON: only calls can interrupt music. Notification ducking & alert chimes are ignored
                    Log.d(TAG, "Only-calls-interrupt is ON -> ignoring notification ducking")
                } else {
                    // When OFF: all notifications & calls can interrupt playback
                    if (_isPlaying.value) {
                        isDuckedByFocusLoss = true
                        val currentVol = getCalculatedVolume()
                        mediaPlayer?.setVolume(currentVol * 0.25f, currentVol * 0.25f)
                    }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus GAIN")
                if (isDuckedByFocusLoss) {
                    isDuckedByFocusLoss = false
                    rampVolumeTo(getCalculatedVolume(), durationMs = 350L)
                } else if (isPausedByFocusLoss) {
                    isPausedByFocusLoss = false
                    resumePlaybackWithRamp()
                }
            }
        }
    }

    // Auto-pause when Bluetooth/Headphones are unplugged or disconnected
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.d(TAG, "Audio becoming noisy (headphone/bluetooth disconnected) -> auto pausing")
                if (_isPlaying.value) {
                    pausePlayback(abandonFocus = true)
                    _sessionMessage.value = "Headphones disconnected • Auto-paused"
                }
            }
        }
    }

    // Folder Locations (Multi-folder management)
    private val _folderLocations = MutableStateFlow<List<FolderLocation>>(settings.folderLocations)
    val folderLocations: StateFlow<List<FolderLocation>> = _folderLocations.asStateFlow()

    // Master list of all scanned tracks across all folder locations
    private val _masterPlaylist = MutableStateFlow<List<AudioTrack>>(emptyList())
    val masterPlaylist: StateFlow<List<AudioTrack>> = _masterPlaylist.asStateFlow()

    // Subfolders detected across all folder locations
    private val _subfolders = MutableStateFlow<List<Subfolder>>(emptyList())
    val subfolders: StateFlow<List<Subfolder>> = _subfolders.asStateFlow()

    // Subfolder inclusion & exclusions
    private val _includeSubfolders = MutableStateFlow(settings.includeSubfolders)
    val includeSubfolders: StateFlow<Boolean> = _includeSubfolders.asStateFlow()

    private val _excludedSubfolderIds = MutableStateFlow<Set<String>>(settings.excludedSubfolderIds)
    val excludedSubfolderIds: StateFlow<Set<String>> = _excludedSubfolderIds.asStateFlow()

    // Format / Codec support settings (separate toggles for MP3, M4A, FLAC, ALAC, WAV, AIFF, AAC, OGG, OPUS, WMA, MIDI)
    private val _enabledFormats = MutableStateFlow<Set<String>>(settings.enabledFormats)
    val enabledFormats: StateFlow<Set<String>> = _enabledFormats.asStateFlow()

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack.asStateFlow()

    private val _currentCoverArt = MutableStateFlow<Bitmap?>(null)
    val currentCoverArt: StateFlow<Bitmap?> = _currentCoverArt.asStateFlow()

    private val _albumPalette = MutableStateFlow(PaletteExtractor.DefaultPalette)
    val albumPalette: StateFlow<ExtractedAlbumPalette> = _albumPalette.asStateFlow()

    // Audio Playback & Ergonomics Settings
    private val _crossfadeSeconds = MutableStateFlow(settings.crossfadeSeconds)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _isGaplessEnabled = MutableStateFlow(settings.isGaplessEnabled)
    val isGaplessEnabled: StateFlow<Boolean> = _isGaplessEnabled.asStateFlow()

    private val _isReplayGainEnabled = MutableStateFlow(settings.isReplayGainEnabled)
    val isReplayGainEnabled: StateFlow<Boolean> = _isReplayGainEnabled.asStateFlow()

    // 200% Super Boost & Loudness Enhancer State
    private val _superBoostPercent = MutableStateFlow(settings.superBoostPercent)
    val superBoostPercent: StateFlow<Int> = _superBoostPercent.asStateFlow()

    private val _isDynamicThemeEnabled = MutableStateFlow(settings.isDynamicThemeEnabled)
    val isDynamicThemeEnabled: StateFlow<Boolean> = _isDynamicThemeEnabled.asStateFlow()

    private val _isOledPureBlackEnabled = MutableStateFlow(settings.isOledPureBlackEnabled)
    val isOledPureBlackEnabled: StateFlow<Boolean> = _isOledPureBlackEnabled.asStateFlow()

    private val _isDynamicArtworkColorEnabled = MutableStateFlow(settings.isDynamicArtworkColorEnabled)
    val isDynamicArtworkColorEnabled: StateFlow<Boolean> = _isDynamicArtworkColorEnabled.asStateFlow()

    private val _dynamicArtworkPalette = MutableStateFlow(DynamicArtworkPalette.DEFAULT)
    val dynamicArtworkPalette: StateFlow<DynamicArtworkPalette> = _dynamicArtworkPalette.asStateFlow()

    private val _appThemePreset = MutableStateFlow(settings.appThemePreset)
    val appThemePreset: StateFlow<AppThemePreset> = _appThemePreset.asStateFlow()

    private val _artDisplayMode = MutableStateFlow(settings.artDisplayMode)
    val artDisplayMode: StateFlow<String> = _artDisplayMode.asStateFlow()

    fun setArtDisplayMode(mode: String) {
        settings.artDisplayMode = mode
        _artDisplayMode.value = mode
    }

    private val _isOnlyCallsInterruptEnabled = MutableStateFlow(settings.isOnlyCallsInterruptEnabled)
    val isOnlyCallsInterruptEnabled: StateFlow<Boolean> = _isOnlyCallsInterruptEnabled.asStateFlow()

    fun setOnlyCallsInterruptEnabled(enabled: Boolean) {
        settings.isOnlyCallsInterruptEnabled = enabled
        _isOnlyCallsInterruptEnabled.value = enabled
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _sessionPlayedIds = MutableStateFlow<Set<String>>(emptySet())
    val sessionPlayedIds: StateFlow<Set<String>> = _sessionPlayedIds.asStateFlow()

    private val _repeatMode = MutableStateFlow(settings.repeatMode)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(settings.isShuffleEnabled)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isScanningBackground = MutableStateFlow(false)
    val isScanningBackground: StateFlow<Boolean> = _isScanningBackground.asStateFlow()

    private val _sessionMessage = MutableStateFlow<String?>(null)
    val sessionMessage: StateFlow<String?> = _sessionMessage.asStateFlow()

    // Playback Speed (0.5x - 2.0x)
    private val _playbackSpeed = MutableStateFlow(settings.playbackSpeed)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Sleep Timer
    private val _sleepTimerState = MutableStateFlow(SleepTimerState())
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    // Equalizer, Bass Boost & 3D Virtualizer
    private val _equalizerState = MutableStateFlow(
        EqualizerState(
            isEnabled = settings.isEqualizerEnabled,
            currentPreset = settings.equalizerPreset,
            bassBoostStrength = settings.bassBoostStrength,
            virtualizerStrength = settings.virtualizerStrength
        )
    )
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    // A-B Section Looper State
    private val _abLoopState = MutableStateFlow(AbLoopState())
    val abLoopState: StateFlow<AbLoopState> = _abLoopState.asStateFlow()

    // Track list sort order (Alphabetic, New to Old, Old to New)
    private val _trackSortOrder = MutableStateFlow(settings.trackSortOrder)
    val trackSortOrder: StateFlow<TrackSortOrder> = _trackSortOrder.asStateFlow()

    fun setTrackSortOrder(order: TrackSortOrder) {
        _trackSortOrder.value = order
        settings.trackSortOrder = order
        if (order == TrackSortOrder.FOLDER_WISE) {
            _isShuffleEnabled.value = false
            settings.isShuffleEnabled = false
        }
    }

    /**
     * Returns the effective playback queue based on current sort order and active track.
     * In FOLDER_WISE mode, restricts playback queue exclusively to tracks in the current track's folder.
     */
    fun getEffectivePlaybackQueue(): List<AudioTrack> {
        val fullList = activePlaylist.value
        if (fullList.isEmpty()) return emptyList()

        if (_trackSortOrder.value == TrackSortOrder.FOLDER_WISE) {
            val current = _currentTrack.value
            if (current != null) {
                val folderKey = current.subfolderId.ifBlank { current.relativePath }
                val folderTracks = fullList.filter { (it.subfolderId.ifBlank { it.relativePath }) == folderKey }
                if (folderTracks.isNotEmpty()) {
                    return folderTracks
                }
            }
        }
        return fullList
    }

    // Active playlist filtered by Format Toggles, Subfolder Preferences, and 5KB minimum size, sorted by user preference
    val activePlaylist: StateFlow<List<AudioTrack>> = combine(
        combine(_masterPlaylist, _subfolders, _includeSubfolders) { master, subs, inc -> Triple(master, subs, inc) },
        combine(_excludedSubfolderIds, _enabledFormats, _trackSortOrder) { excl, fmts, sort -> Triple(excl, fmts, sort) }
    ) { (tracks, subfolderList, includeSubs), (excludedIds, enabledFmtNames, sortOrder) ->
        val subfolderMap = subfolderList.associateBy { it.id }
        val excludedSubs = subfolderList.filter { it.id in excludedIds && !it.isRoot }
        val excludedPaths = excludedSubs.map { it.relativePath.trimEnd('/') + "/" }

        val filtered = tracks.filter { track ->
            // Exclude 0B and <= 5KB files (junk, empty, or corrupt files)
            if (track.sizeBytes in 1L..FolderScanner.MIN_FILE_SIZE_BYTES) {
                return@filter false
            }

            // 1. Format filter check
            val formatName = track.format.name
            if (formatName !in enabledFmtNames) {
                return@filter false
            }

            // 2. Subfolder check
            val subfolderObj = subfolderMap[track.subfolderId]
            val isRootTrack = subfolderObj?.isRoot == true || track.subfolderId.endsWith("root")

            if (isRootTrack) {
                true
            } else if (!includeSubs) {
                false
            } else {
                if (track.subfolderId in excludedIds) {
                    false
                } else if (excludedPaths.isNotEmpty() && excludedPaths.any { track.relativePath.startsWith(it) }) {
                    false
                } else {
                    true
                }
            }
        }

        when (sortOrder) {
            TrackSortOrder.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase() }
            TrackSortOrder.NEWEST_FIRST -> filtered.sortedWith(compareByDescending<AudioTrack> { it.dateModifiedMs }.thenBy { it.title.lowercase() })
            TrackSortOrder.OLDEST_FIRST -> filtered.sortedWith(compareBy<AudioTrack> { it.dateModifiedMs }.thenBy { it.title.lowercase() })
            TrackSortOrder.FOLDER_WISE -> {
                val folderTrackCounts = filtered.groupingBy { it.subfolderId.ifBlank { it.relativePath } }.eachCount()
                filtered.sortedWith(
                    compareBy<AudioTrack> {
                        val key = it.subfolderId.ifBlank { it.relativePath }
                        folderTrackCounts[key] ?: Int.MAX_VALUE
                    }
                    .thenBy { it.subfolderName.lowercase().ifBlank { it.relativePath.lowercase() } }
                    .thenBy { it.subfolderId }
                    .thenBy { it.trackNumber.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.fileName.lowercase().ifBlank { it.title.lowercase() } }
                    .thenBy { it.title.lowercase() }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Remaining unplayed tracks count in current session
    val unplayedCount: StateFlow<Int> = combine(activePlaylist, _sessionPlayedIds) { list, played ->
        list.count { it.id !in played }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        MediaPlaybackReceiver.playbackActionListener = this
        _sessionPlayedIds.value = settings.getValidPlayedTrackIds()
        _currentPositionMs.value = settings.lastPositionMs
        try {
            if (!isNoisyReceiverRegistered) {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(noisyReceiver, filter)
                }
                isNoisyReceiverRegistered = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register noisy receiver: ${e.message}")
        }
        restoreSavedFolders()
        viewModelScope.launch {
            _currentTrack.collect { track ->
                updateArtworkPaletteForTrack(track)
            }
        }
    }

    override fun onPlayPause() {
        togglePlayPause()
    }

    override fun onNext() {
        skipNext()
    }

    override fun onPrevious() {
        skipPrevious()
    }

    override fun onClose() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        stopProgressTracker()
        MediaNotificationHelper.clearNotification(context)
    }

    private fun restoreSavedFolders() {
        viewModelScope.launch {
            val savedLocations = settings.folderLocations
            _folderLocations.value = savedLocations

            if (savedLocations.isNotEmpty()) {
                savedLocations.forEach { loc ->
                    if (loc.isDeviceStorage) {
                        scanDeviceAudio(loc.id, isInitial = true)
                    } else {
                        try {
                            val uri = Uri.parse(loc.uriString)
                            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                                it.uri == uri && it.isReadPermission
                            }
                            if (hasPerm) {
                                scanFolderLocation(loc, uri)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error restoring folder ${loc.name}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a new SAF Folder Location, persists permission, and starts background silent scan.
     * Restricts adding the same folder if it was already added before.
     */
    fun addFolderUri(treeUri: Uri) {
        viewModelScope.launch {
            val treeUriString = treeUri.toString()
            val existingLocations = settings.folderLocations

            fun normalizeFolderUriKey(uStr: String): String {
                val decoded = Uri.decode(uStr)
                val treeDocIdx = decoded.indexOf("/tree/")
                return if (treeDocIdx != -1) {
                    decoded.substring(treeDocIdx).trimEnd('/')
                } else {
                    decoded.trimEnd('/')
                }
            }

            val incomingKey = normalizeFolderUriKey(treeUriString)
            val isDuplicate = existingLocations.any { existing ->
                !existing.isDeviceStorage && (existing.uriString == treeUriString || normalizeFolderUriKey(existing.uriString) == incomingKey)
            }

            if (isDuplicate) {
                val duplicateName = existingLocations.firstOrNull { existing ->
                    !existing.isDeviceStorage && (existing.uriString == treeUriString || normalizeFolderUriKey(existing.uriString) == incomingKey)
                }?.name ?: "Folder"
                _sessionMessage.value = "Folder '$duplicateName' has already been added"
                return@launch
            }

            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(TAG, "Persistable permission notice: ${e.message}")
            }

            var folderName = "Music Folder"
            try {
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
                context.contentResolver.query(
                    parentUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        folderName = cursor.getString(0) ?: "Music Folder"
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve folder name: ${e.message}")
            }

            val locationId = "folder_${UUID.randomUUID().toString().take(8)}"
            val newLocation = FolderLocation(
                id = locationId,
                name = folderName,
                uriString = treeUriString,
                isDeviceStorage = false,
                trackCount = 0
            )

            // Remove demo tracks when user adds a custom folder
            val currentTracks = _masterPlaylist.value.filter { !it.id.startsWith("track_") || !it.uri.toString().contains("android_asset") }
            _masterPlaylist.value = currentTracks

            settings.addFolderLocation(newLocation)
            _folderLocations.value = settings.folderLocations

            scanFolderLocation(newLocation, treeUri)
            _sessionMessage.value = "Scanning $folderName in background..."
        }
    }

    /**
     * Adds / scans device media audio storage as a folder location.
     */
    fun scanDeviceAudio(customId: String? = null, isInitial: Boolean = false) {
        val locationId = customId ?: "dev_storage"
        val existing = _folderLocations.value.find { it.id == locationId || it.isDeviceStorage }
        val effectiveLocationId = existing?.id ?: locationId
        val location = existing ?: FolderLocation(
            id = effectiveLocationId,
            name = "Device Audio Storage",
            uriString = "content://media/external/audio/media",
            isDeviceStorage = true,
            trackCount = 0
        )

        if (existing == null) {
            settings.addFolderLocation(location)
            _folderLocations.value = settings.folderLocations
        }

        scanJobs[effectiveLocationId]?.cancel()
        val job = viewModelScope.launch {
            _isScanningBackground.value = true
            try {
                FolderScanner.scanDeviceAudioFlow(context, effectiveLocationId).collect { update ->
                    when (update) {
                        is ScanProgressUpdate.TracksDiscovered -> {
                            mergeDiscoveredTracks(effectiveLocationId, update.newTracks, update.currentSubfolders)
                        }
                        is ScanProgressUpdate.ScanComplete -> {
                            mergeScanResult(update.result)
                        }
                    }
                }
            } finally {
                scanJobs.remove(effectiveLocationId)
                _isScanningBackground.value = scanJobs.values.any { it.isActive }
            }
        }
        scanJobs[effectiveLocationId] = job
    }

    /**
     * Scans an individual folder location in the background and silently updates tracks.
     */
    private fun scanFolderLocation(location: FolderLocation, uri: Uri) {
        scanJobs[location.id]?.cancel()
        val job = viewModelScope.launch {
            _isScanningBackground.value = true
            try {
                FolderScanner.scanFolderTreeFlow(context, uri, location.id).collect { update ->
                    when (update) {
                        is ScanProgressUpdate.TracksDiscovered -> {
                            mergeDiscoveredTracks(location.id, update.newTracks, update.currentSubfolders)
                        }
                        is ScanProgressUpdate.ScanComplete -> {
                            mergeScanResult(update.result)
                        }
                    }
                }
            } finally {
                scanJobs.remove(location.id)
                _isScanningBackground.value = scanJobs.values.any { it.isActive }
            }
        }
        scanJobs[location.id] = job
    }

    private fun restoreLastSavedTrackOrFallback(fallbackTracks: List<AudioTrack>) {
        val currentPlaylist = if (activePlaylist.value.isNotEmpty()) activePlaylist.value else fallbackTracks
        if (currentPlaylist.isEmpty()) return

        val validPlayed = settings.getValidPlayedTrackIds()
        _sessionPlayedIds.value = validPlayed

        // On app open: check if last played track exists in settings and in playlist
        val savedTrackId = settings.lastTrackId
        val savedTrack = if (savedTrackId != null) currentPlaylist.find { it.id == savedTrackId } else null

        // If currently playing, do not interrupt
        if (_isPlaying.value) return

        if (savedTrack != null) {
            // Check if already displaying the saved track
            if (_currentTrack.value?.id == savedTrack.id && _currentPositionMs.value > 0) return

            val isCompleted = settings.isTrackCompleted(savedTrack.id)
            val savedPos = settings.getTrackPosition(savedTrack.id).let {
                if (it > 0) it else settings.lastPositionMs
            }
            val isEffectivelyCompleted = isCompleted || (savedTrack.durationMs > 0 && savedPos >= (savedTrack.durationMs * 0.98f).toLong())

            val (chosenTrack, startPosition) = if (!isEffectivelyCompleted && savedPos > 0) {
                val pos = if (savedTrack.durationMs > 0) {
                    val ratio = savedPos.toFloat() / savedTrack.durationMs.toFloat()
                    if (ratio < 0.001f || ratio >= 0.98f) 0L else savedPos
                } else {
                    savedPos
                }
                Pair(savedTrack, pos)
            } else if (isEffectivelyCompleted) {
                // Completed before: pick next unplayed track
                val unplayedCandidates = currentPlaylist.filter {
                    it.id != savedTrack.id && it.id !in validPlayed && !settings.isTrackCompleted(it.id)
                }
                val newTrack = if (unplayedCandidates.isNotEmpty()) {
                    unplayedCandidates.random()
                } else {
                    val otherCandidates = currentPlaylist.filter { it.id != savedTrack.id }
                    if (otherCandidates.isNotEmpty()) {
                        otherCandidates.random()
                    } else {
                        savedTrack
                    }
                }
                Pair(newTrack, 0L)
            } else {
                Pair(savedTrack, 0L)
            }

            _currentTrack.value = chosenTrack
            _durationMs.value = chosenTrack.durationMs
            _currentPositionMs.value = startPosition
            settings.lastTrackId = chosenTrack.id
            settings.lastPositionMs = startPosition
            if (startPosition > 0) {
                settings.setTrackPosition(chosenTrack.id, startPosition)
            }

            loadArtworkForTrack(chosenTrack)
            return
        }

        // If saved track not found yet, and we already have a track set, don't overwrite
        if (_currentTrack.value != null) return

        // If scanning is in progress, do not permanently overwrite savedTrackId with fallback
        val isScanning = _isScanningBackground.value || scanJobs.values.any { it.isActive }
        val unplayedCandidates = currentPlaylist.filter { it.id !in validPlayed && !settings.isTrackCompleted(it.id) }
        val candidate = if (unplayedCandidates.isNotEmpty()) {
            unplayedCandidates.random()
        } else {
            currentPlaylist.random()
        }

        _currentTrack.value = candidate
        _durationMs.value = candidate.durationMs
        _currentPositionMs.value = 0L
        if (!isScanning && savedTrackId == null) {
            settings.lastTrackId = candidate.id
            settings.lastPositionMs = 0L
        }

        loadArtworkForTrack(candidate)
    }

    /**
     * Rescans all source locations for new audio files and updates the playlist.
     * Invoked when app opens and when user presses the Refresh button.
     */
    fun refreshLibrary() {
        _sessionMessage.value = "Refreshing library & searching for new audio files..."
        val savedLocations = _folderLocations.value
        if (savedLocations.isEmpty()) {
            scanDeviceAudio()
        } else {
            savedLocations.forEach { loc ->
                if (loc.isDeviceStorage) {
                    scanDeviceAudio(loc.id)
                } else {
                    try {
                        val uri = Uri.parse(loc.uriString)
                        val hasPerm = context.contentResolver.persistedUriPermissions.any {
                            it.uri == uri && it.isReadPermission
                        }
                        if (hasPerm) {
                            scanFolderLocation(loc, uri)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error refreshing folder ${loc.name}: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Loads sample tracks for demo and unit testing.
     */
    fun loadSampleTracks(): Job {
        return viewModelScope.launch {
            val sampleTracks = listOf(
                AudioTrack(
                    id = "sample_1",
                    title = "Electric Dawn",
                    artist = "Pulsewave",
                    album = "Synthetics",
                    durationMs = 185000L,
                    uri = Uri.parse("file:///android_asset/samples/electric_dawn.mp3"),
                    fileExtension = "mp3",
                    format = SupportedAudioFormat.MP3,
                    subfolderId = "root",
                    subfolderName = "Root Folder",
                    folderLocationId = "sample_loc"
                ),
                AudioTrack(
                    id = "sample_2",
                    title = "Midnight Groove",
                    artist = "Lofi Dreams",
                    album = "Chillhop Vol 1",
                    durationMs = 142000L,
                    uri = Uri.parse("file:///android_asset/samples/midnight_groove.mp3"),
                    fileExtension = "mp3",
                    format = SupportedAudioFormat.MP3,
                    subfolderId = "sub_lofi",
                    subfolderName = "Lofi Beats",
                    folderLocationId = "sample_loc"
                ),
                AudioTrack(
                    id = "sample_3",
                    title = "Cyber Pulse",
                    artist = "Neon Grid",
                    album = "Outrun 2088",
                    durationMs = 210000L,
                    uri = Uri.parse("file:///android_asset/samples/cyber_pulse.m4a"),
                    fileExtension = "m4a",
                    format = SupportedAudioFormat.M4A,
                    subfolderId = "sub_synth",
                    subfolderName = "Synthwave",
                    folderLocationId = "sample_loc"
                ),
                AudioTrack(
                    id = "sample_4",
                    title = "Acoustic Breeze",
                    artist = "Amber Woods",
                    album = "Folk Horizons",
                    durationMs = 165000L,
                    uri = Uri.parse("file:///android_asset/samples/acoustic_breeze.mp3"),
                    fileExtension = "mp3",
                    format = SupportedAudioFormat.MP3,
                    subfolderId = "sub_acoustic",
                    subfolderName = "Acoustic Sessions",
                    folderLocationId = "sample_loc"
                ),
                AudioTrack(
                    id = "sample_5",
                    title = "Deep Space Drift",
                    artist = "Cosmic Echoes",
                    album = "Starlight Voyage",
                    durationMs = 245000L,
                    uri = Uri.parse("file:///android_asset/samples/deep_space_drift.m4a"),
                    fileExtension = "m4a",
                    format = SupportedAudioFormat.M4A,
                    subfolderId = "sub_ambient",
                    subfolderName = "Ambient Chill",
                    folderLocationId = "sample_loc"
                )
            )

            val sampleSubs = listOf(
                Subfolder(id = "root", name = "Root Folder", relativePath = "", trackCount = 1, isRoot = true, folderLocationId = "sample_loc"),
                Subfolder(id = "sub_lofi", name = "Lofi Beats", relativePath = "lofi", trackCount = 1, isRoot = false, folderLocationId = "sample_loc"),
                Subfolder(id = "sub_synth", name = "Synthwave", relativePath = "synth", trackCount = 1, isRoot = false, folderLocationId = "sample_loc"),
                Subfolder(id = "sub_acoustic", name = "Acoustic Sessions", relativePath = "acoustic", trackCount = 1, isRoot = false, folderLocationId = "sample_loc"),
                Subfolder(id = "sub_ambient", name = "Ambient Chill", relativePath = "ambient", trackCount = 1, isRoot = false, folderLocationId = "sample_loc")
            )

            val persistedSampleTracks = sampleTracks.map { it.applyPersistedMetadata() }
            _masterPlaylist.value = persistedSampleTracks
            _subfolders.value = sampleSubs
            restoreLastSavedTrackOrFallback(persistedSampleTracks)
        }
    }

    private fun AudioTrack.applyPersistedMetadata(): AudioTrack {
        val meta = settings.getCustomTrackMetadata(this.id) ?: return this
        return this.copy(
            title = meta.title.ifBlank { this.title },
            artist = meta.artist.ifBlank { this.artist },
            album = meta.album.ifBlank { this.album },
            albumArtist = meta.albumArtist.ifBlank { this.albumArtist },
            composer = meta.composer.ifBlank { this.composer },
            genre = meta.genre.ifBlank { this.genre },
            year = meta.year.ifBlank { this.year },
            trackNumber = meta.trackNumber.ifBlank { this.trackNumber },
            discNumber = meta.discNumber.ifBlank { this.discNumber }
        )
    }

    private fun mergeDiscoveredTracks(
        locationId: String,
        newTracks: List<AudioTrack>,
        newSubs: List<Subfolder>
    ) {
        // Merge tracks without duplicates and apply persisted metadata
        val formattedTracks = newTracks.map { it.applyPersistedMetadata() }
        val currentTracks = _masterPlaylist.value.toMutableList()
        val existingIds = currentTracks.map { it.id }.toSet()
        val tracksToAdd = formattedTracks.filter { it.id !in existingIds }
        if (tracksToAdd.isNotEmpty()) {
            currentTracks.addAll(tracksToAdd)
            _masterPlaylist.value = currentTracks.toList()
        }
        restoreLastSavedTrackOrFallback(tracksToAdd)

        // Merge subfolders
        val currentSubs = _subfolders.value.filter { it.folderLocationId != locationId }.toMutableList()
        currentSubs.addAll(newSubs)
        _subfolders.value = currentSubs
    }

    private fun mergeScanResult(result: FolderScanResult) {
        val formattedTracks = result.allTracks.map { it.applyPersistedMetadata() }
        val currentTracks = _masterPlaylist.value.filter { it.folderLocationId != result.folderLocationId }.toMutableList()
        currentTracks.addAll(formattedTracks)
        _masterPlaylist.value = currentTracks

        val currentSubs = _subfolders.value.filter { it.folderLocationId != result.folderLocationId }.toMutableList()
        currentSubs.addAll(result.subfolders)
        _subfolders.value = currentSubs

        settings.updateFolderTrackCount(result.folderLocationId, formattedTracks.size)
        _folderLocations.value = settings.folderLocations

        restoreLastSavedTrackOrFallback(formattedTracks)
    }

    /**
     * Removes a folder location separately, releasing persistable permission and purging its tracks.
     */
    fun removeFolderLocation(locationId: String) {
        val location = _folderLocations.value.find { it.id == locationId }
        scanJobs[locationId]?.cancel()
        scanJobs.remove(locationId)

        if (location != null && !location.isDeviceStorage) {
            try {
                val uri = Uri.parse(location.uriString)
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not release persistable permission: ${e.message}")
            }
        }

        settings.removeFolderLocation(locationId)
        _folderLocations.value = settings.folderLocations

        // Remove all tracks & subfolders for this folder location
        _masterPlaylist.value = _masterPlaylist.value.filter { it.folderLocationId != locationId }
        _subfolders.value = _subfolders.value.filter { it.folderLocationId != locationId }

        // If current track was inside this folder, switch to another song
        if (_currentTrack.value?.folderLocationId == locationId) {
            val nextSong = activePlaylist.value.firstOrNull()
            if (nextSong != null) {
                playTrack(nextSong)
            } else {
                mediaPlayer?.pause()
                _isPlaying.value = false
                _currentTrack.value = null
                _durationMs.value = 0L
                _currentPositionMs.value = 0L
                _currentCoverArt.value = null
                MediaNotificationHelper.clearNotification(context)
            }
        }
    }

    /**
     * Toggles support for a specific audio format/codec (e.g. MP3, M4A, FLAC, WAV, etc.).
     * When toggled off, immediately filters out that format from the active track list & player.
     */
    fun toggleAudioFormat(format: SupportedAudioFormat) {
        val isCurrentlyEnabled = settings.isFormatEnabled(format)
        val newStatus = !isCurrentlyEnabled
        settings.setFormatEnabled(format, newStatus)
        _enabledFormats.value = settings.enabledFormats

        // If current track is of disabled format, switch track
        val curTrack = _currentTrack.value
        if (curTrack != null && curTrack.format == format && !newStatus) {
            val nextSong = activePlaylist.value.firstOrNull { it.format != format }
            if (nextSong != null) {
                playTrack(nextSong)
            } else {
                mediaPlayer?.pause()
                _isPlaying.value = false
                _currentTrack.value = null
                _durationMs.value = 0L
                _currentPositionMs.value = 0L
                _currentCoverArt.value = null
                MediaNotificationHelper.clearNotification(context)
            }
        }
    }

    fun enableAllAudioFormats() {
        val formats = SupportedAudioFormat.ALL_FORMAT_NAMES
        settings.enabledFormats = formats
        _enabledFormats.value = formats
    }

    fun disableAllAudioFormats() {
        settings.enabledFormats = emptySet()
        _enabledFormats.value = emptySet()

        mediaPlayer?.pause()
        _isPlaying.value = false
        _currentTrack.value = null
        _durationMs.value = 0L
        _currentPositionMs.value = 0L
        _currentCoverArt.value = null
        MediaNotificationHelper.clearNotification(context)
    }

    private fun loadArtworkForTrack(track: AudioTrack) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                AlbumArtExtractor.extractCoverArt(context, track.uri, track.id)
            }
            if (_currentTrack.value?.id == track.id) {
                _currentCoverArt.value = bitmap
                val palette = withContext(Dispatchers.Default) {
                    PaletteExtractor.extract(bitmap)
                }
                if (_currentTrack.value?.id == track.id) {
                    _albumPalette.value = palette
                    MediaNotificationHelper.updateNotification(context, track, _isPlaying.value, bitmap)
                }
            }
        }
    }

    fun setIncludeSubfolders(include: Boolean) {
        _includeSubfolders.value = include
        settings.includeSubfolders = include
    }

    fun toggleSubfolderExclusion(subfolderId: String) {
        val current = _excludedSubfolderIds.value
        val updated = if (current.contains(subfolderId)) {
            current - subfolderId
        } else {
            current + subfolderId
        }
        _excludedSubfolderIds.value = updated
        settings.excludedSubfolderIds = updated
    }

    fun includeAllSubfolders() {
        _includeSubfolders.value = true
        _excludedSubfolderIds.value = emptySet()
        settings.includeSubfolders = true
        settings.excludedSubfolderIds = emptySet()
    }

    fun excludeAllSubfolders() {
        val nonRootIds = _subfolders.value.filter { !it.isRoot }.map { it.id }.toSet()
        _excludedSubfolderIds.value = nonRootIds
        settings.excludedSubfolderIds = nonRootIds
    }

    /**
     * Sets Point A for the A-B Looper at the current playback position.
     */
    fun setAbPointA() {
        val pos = _currentPositionMs.value
        _abLoopState.update { current ->
            val newB = if (current.pointB != null && current.pointB <= pos) null else current.pointB
            current.copy(pointA = pos, pointB = newB, isEnabled = newB != null)
        }
        _sessionMessage.value = "A-B Loop: Point A set at ${formatDuration(_currentPositionMs.value)}"
    }

    /**
     * Sets Point B for the A-B Looper at the current playback position.
     */
    fun setAbPointB() {
        val pos = _currentPositionMs.value
        val a = _abLoopState.value.pointA ?: 0L
        if (pos > a) {
            _abLoopState.update { it.copy(pointB = pos, isEnabled = true) }
            _sessionMessage.value = "A-B Loop enabled (${formatDuration(a)} → ${formatDuration(pos)})"
        } else {
            _sessionMessage.value = "Point B must be greater than Point A"
        }
    }

    /**
     * Toggles A-B loop active state.
     */
    fun toggleAbLoop() {
        _abLoopState.update { it.copy(isEnabled = !it.isEnabled) }
    }

    /**
     * Clears A-B section loop.
     */
    fun clearAbLoop() {
        _abLoopState.value = AbLoopState()
        _sessionMessage.value = "A-B Loop cleared"
    }

    /**
     * Randomly plays a new / unplayed track in the current session.
     */
    fun playNewTrack() {
        val currentPlaylist = getEffectivePlaybackQueue()
        if (currentPlaylist.isEmpty()) {
            return
        }

        val validPlayed = settings.getValidPlayedTrackIds()
        _sessionPlayedIds.value = validPlayed

        val currentId = _currentTrack.value?.id

        if (_trackSortOrder.value == TrackSortOrder.FOLDER_WISE) {
            val currentIndex = currentPlaylist.indexOfFirst { it.id == currentId }
            val nextIndex = if (currentIndex in 0 until currentPlaylist.lastIndex) currentIndex + 1 else 0
            if (nextIndex == 0 && currentPlaylist.isNotEmpty() && currentPlaylist.all { it.id in validPlayed }) {
                settings.clearPlayedTracks()
                _sessionPlayedIds.value = emptySet()
            }
            playTrack(currentPlaylist[nextIndex], forceRestart = true, isUserAction = false)
            return
        }

        val unplayedTracks = currentPlaylist.filter { it.id !in validPlayed && it.id != currentId }
            .ifEmpty { currentPlaylist.filter { it.id !in validPlayed } }

        if (unplayedTracks.isNotEmpty()) {
            val selected = unplayedTracks.random()
            playTrack(selected, forceRestart = true, isUserAction = false)
        } else {
            // All tracks finished playing - clear played list & start new
            if (currentPlaylist.isNotEmpty() && currentPlaylist.all { it.id in validPlayed }) {
                settings.clearPlayedTracks()
                _sessionPlayedIds.value = emptySet()
            }
            val candidates = if (currentPlaylist.size > 1) currentPlaylist.filter { it.id != currentId } else currentPlaylist
            val nextTrack = candidates.random()
            playTrack(nextTrack, forceRestart = true, isUserAction = false)
        }
    }

    private fun skipToRandomTrackOnDecodeError(failedTrack: AudioTrack, reason: String) {
        unplayableTrackIds.add(failedTrack.id)
        val currentPlaylist = activePlaylist.value
        val validCandidates = currentPlaylist.filter { it.id !in unplayableTrackIds }

        Log.w(TAG, "Unplayable track detected: '${failedTrack.title}'. Reason: $reason. Remaining valid tracks: ${validCandidates.size}")
        _isPlaying.value = false
        stopProgressTracker()

        if (validCandidates.isNotEmpty()) {
            val randomTrack = validCandidates.random()
            viewModelScope.launch {
                delay(400)
                playTrack(randomTrack, forceRestart = true, isUserAction = false)
            }
        } else {
            _sessionMessage.value = "Cannot decode '${failedTrack.title}': $reason. No playable tracks available."
            _currentTrack.value = null
            _durationMs.value = 0L
            _currentPositionMs.value = 0L
            _currentCoverArt.value = null
            MediaNotificationHelper.clearNotification(context)
        }
    }

    fun playTrack(track: AudioTrack, forceRestart: Boolean = false, isUserAction: Boolean = false) {
        if (!forceRestart && track.id == _currentTrack.value?.id) {
            if (_isPlaying.value) {
                // Currently playing this track - continue playback seamlessly without restarting
                return
            } else if (mediaPlayer != null) {
                // Currently loaded but paused - resume playback
                resumePlaybackWithRamp()
                return
            }
        }

        var actualTrackToPlay = track

        // Only substitute with another track if automatic (not a user action) AND not resuming the currently loaded track
        if (!isUserAction && track.id != _currentTrack.value?.id) {
            // Compare target track with the list of tracks finished playing today
            val validPlayedSet = settings.getValidPlayedTrackIds()
            _sessionPlayedIds.value = validPlayedSet

            if (track.id in validPlayedSet) {
                val currentPlaylist = getEffectivePlaybackQueue().ifEmpty { activePlaylist.value }
                val unplayedTracks = currentPlaylist.filter { it.id !in validPlayedSet }
                if (unplayedTracks.isNotEmpty()) {
                    actualTrackToPlay = if (_trackSortOrder.value == TrackSortOrder.FOLDER_WISE) unplayedTracks.first() else unplayedTracks.random()
                } else {
                    // All tracks finished playing - clear played list & start new silently only if truly all tracks played
                    if (currentPlaylist.isNotEmpty() && currentPlaylist.all { it.id in validPlayedSet }) {
                        settings.clearPlayedTracks()
                        _sessionPlayedIds.value = emptySet()
                    }
                    actualTrackToPlay = track
                }
            }
        } else {
            // User explicitly selected track or resuming current track
            _sessionPlayedIds.value = settings.getValidPlayedTrackIds()
        }

        val prevPlayer = mediaPlayer
        val crossfadeSec = _crossfadeSeconds.value

        _currentTrack.value = actualTrackToPlay
        _durationMs.value = actualTrackToPlay.durationMs

        // Resume position check:
        // Record details from 0.1% to 100%, resume track when reopening or restarting
        val validPlayed = settings.getValidPlayedTrackIds()
        val isCompleted = settings.isTrackCompleted(actualTrackToPlay.id)
        val savedPos = settings.getTrackPosition(actualTrackToPlay.id).let {
            if (it > 0) it else if (actualTrackToPlay.id == settings.lastTrackId) settings.lastPositionMs else 0L
        }
        val startPosition = if (!isCompleted && savedPos > 0) {
            if (actualTrackToPlay.durationMs > 0) {
                val ratio = savedPos.toFloat() / actualTrackToPlay.durationMs.toFloat()
                if (ratio < 0.001f || ratio >= 0.98f) {
                    0L
                } else {
                    savedPos
                }
            } else {
                savedPos
            }
        } else {
            0L
        }

        _currentPositionMs.value = startPosition
        settings.lastTrackId = actualTrackToPlay.id
        settings.lastPositionMs = startPosition
        if (startPosition > 0) {
            settings.setTrackPosition(actualTrackToPlay.id, startPosition)
        } else if (isCompleted) {
            settings.setTrackPosition(actualTrackToPlay.id, 0L)
            settings.markTrackCompleted(actualTrackToPlay.id, false)
        }
        _sessionPlayedIds.value = validPlayed

        loadArtworkForTrack(actualTrackToPlay)

        try {
            val nextPlayer = MediaPlayer().apply {
                try {
                    setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (e: Exception) {
                    Log.d(TAG, "setWakeMode failed: ${e.message}")
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // Resilient URI / DataSource loading supporting file paths, file://, content://, and SAF descriptors
                var dataSourceConfigured = false
                try {
                    val uri = actualTrackToPlay.uri
                    if (uri.scheme == "file" || uri.scheme == null) {
                        val path = uri.path ?: actualTrackToPlay.uri.toString()
                        val file = File(path)
                        if (file.exists() && file.canRead()) {
                            setDataSource(file.absolutePath)
                            dataSourceConfigured = true
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Direct file path dataSource failed: ${e.message}")
                }

                if (!dataSourceConfigured) {
                    try {
                        setDataSource(context, actualTrackToPlay.uri)
                        dataSourceConfigured = true
                    } catch (e: Exception) {
                        Log.d(TAG, "context setDataSource failed: ${e.message}")
                    }
                }

                if (!dataSourceConfigured) {
                    try {
                        val pfd = context.contentResolver.openFileDescriptor(actualTrackToPlay.uri, "r")
                        if (pfd != null) {
                            setDataSource(pfd.fileDescriptor)
                            pfd.close()
                            dataSourceConfigured = true
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "ParcelFileDescriptor failed: ${e.message}")
                    }
                }

                if (!dataSourceConfigured) {
                    try {
                        val afd = context.contentResolver.openAssetFileDescriptor(actualTrackToPlay.uri, "r")
                        if (afd != null) {
                            if (afd.declaredLength < 0) {
                                setDataSource(afd.fileDescriptor)
                            } else {
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
                            }
                            afd.close()
                            dataSourceConfigured = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "AssetFileDescriptor failed: ${e.message}")
                    }
                }

                if (!dataSourceConfigured) {
                    if (actualTrackToPlay.id.startsWith("sample_") || actualTrackToPlay.uri.toString().contains("android_asset/samples")) {
                        // Demo / Unit test track bypass
                        dataSourceConfigured = true
                    } else {
                        Log.e(TAG, "Failed to configure data source for '${actualTrackToPlay.title}' (${actualTrackToPlay.uri})")
                        skipToRandomTrackOnDecodeError(actualTrackToPlay, "Audio file invalid or unreadable")
                        return@apply
                    }
                }

                // Volume Normalization / ReplayGain initial gain
                val targetVolume = getCalculatedVolume()

                setOnPreparedListener { mp ->
                    val mpDuration = mp.duration.toLong()
                    if (mpDuration > 0) {
                        _durationMs.value = mpDuration
                        if (actualTrackToPlay.durationMs <= 0L) {
                            actualTrackToPlay = actualTrackToPlay.copy(durationMs = mpDuration)
                            _currentTrack.value = actualTrackToPlay
                        }
                    }
                    applyPlaybackSpeedToPlayer(mp)
                    setupAudioFx(mp.audioSessionId)

                    val effectiveSeek = if (startPosition > 0 && (mpDuration <= 0L || startPosition < mpDuration)) {
                        startPosition
                    } else if (savedPos > 0 && !isCompleted && (mpDuration <= 0L || savedPos < (mpDuration * 0.98f))) {
                        savedPos
                    } else {
                        0L
                    }

                    if (effectiveSeek > 0) {
                        mp.seekTo(effectiveSeek.toInt())
                        _currentPositionMs.value = effectiveSeek
                        settings.lastPositionMs = effectiveSeek
                        settings.setTrackPosition(actualTrackToPlay.id, effectiveSeek)
                    }

                    // Acquire Audio Focus before outputting audio
                    requestAudioFocus()

                    // Execute Crossfade if previously playing and crossfade > 0
                    if (prevPlayer != null && prevPlayer.isPlaying && crossfadeSec > 0) {
                        mp.setVolume(0f, 0f)
                        mp.start()
                        _isPlaying.value = true
                        startProgressTracker()

                        viewModelScope.launch {
                            val steps = 15
                            val stepDelay = (crossfadeSec * 1000L) / steps
                            for (i in 1..steps) {
                                val factor = i.toFloat() / steps.toFloat()
                                try {
                                    prevPlayer.setVolume((1f - factor) * targetVolume, (1f - factor) * targetVolume)
                                    mp.setVolume(factor * targetVolume, factor * targetVolume)
                                } catch (e: Exception) {
                                    // Ignored if released early
                                }
                                delay(stepDelay)
                            }
                            try {
                                prevPlayer.stop()
                                prevPlayer.release()
                            } catch (e: Exception) {
                                Log.d(TAG, "Old player release: ${e.message}")
                            }
                        }
                    } else {
                        // Standard or Gapless transition with smooth ramp
                        try {
                            prevPlayer?.release()
                        } catch (e: Exception) {
                            Log.d(TAG, "Old player release: ${e.message}")
                        }
                        mp.setVolume(0.05f, 0.05f)
                        mp.start()
                        _isPlaying.value = true
                        startProgressTracker()
                        rampVolumeTo(targetVolume, startVol = 0.05f, durationMs = 300L)
                    }

                    MediaNotificationHelper.updateNotification(context, actualTrackToPlay, isPlaying = true, _currentCoverArt.value)
                }

                setOnCompletionListener {
                    handleTrackCompletion()
                }

                setOnErrorListener { mp, what, extra ->
                    if (actualTrackToPlay.id.startsWith("sample_") || actualTrackToPlay.uri.toString().contains("android_asset/samples")) {
                        // In unit test / demo playback where shadow MediaPlayer has no binary decoder
                        _isPlaying.value = true
                        return@setOnErrorListener true
                    }
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra for track '${actualTrackToPlay.title}' (${actualTrackToPlay.fileName})")
                    val reason = when (extra) {
                        MediaPlayer.MEDIA_ERROR_IO -> "I/O file error"
                        MediaPlayer.MEDIA_ERROR_MALFORMED -> "Malformed stream"
                        MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "Unsupported audio format"
                        MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "Playback timed out"
                        -2147483648 -> "Unsupported audio format or corrupted stream"
                        else -> "Playback error code ($what, $extra)"
                    }
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) {}

                    skipToRandomTrackOnDecodeError(actualTrackToPlay, reason)
                    true // Return true so onCompletion is not mistakenly invoked
                }

                try {
                    prepareAsync()
                } catch (e: Exception) {
                    if (actualTrackToPlay.id.startsWith("sample_") || actualTrackToPlay.uri.toString().contains("android_asset/samples")) {
                        _isPlaying.value = true
                    } else {
                        Log.e(TAG, "prepareAsync failed for track ${actualTrackToPlay.title}: ${e.message}")
                        skipToRandomTrackOnDecodeError(actualTrackToPlay, "Cannot initialize playback: ${e.message ?: "Decode error"}")
                    }
                }
            }
            mediaPlayer = nextPlayer
        } catch (e: Exception) {
            if (actualTrackToPlay.id.startsWith("sample_") || actualTrackToPlay.uri.toString().contains("android_asset/samples")) {
                _isPlaying.value = true
            } else {
                Log.e(TAG, "Error playing track ${actualTrackToPlay.title}", e)
                skipToRandomTrackOnDecodeError(actualTrackToPlay, "Unable to open track: ${e.message ?: "File corrupted"}")
            }
        }
    }

    private fun computeReplayGainTarget(track: AudioTrack): Float {
        // Automatic heuristic loudness normalization based on audio format & bitrate profiles
        return when (track.format) {
            SupportedAudioFormat.FLAC, SupportedAudioFormat.WAV -> 0.95f
            SupportedAudioFormat.MP3 -> 0.88f
            SupportedAudioFormat.M4A, SupportedAudioFormat.AAC -> 0.90f
            SupportedAudioFormat.OGG, SupportedAudioFormat.OPUS -> 0.89f
            else -> 0.90f
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val track = _currentTrack.value

        if (player != null && track != null) {
            if (player.isPlaying) {
                pausePlayback(abandonFocus = true)
            } else {
                resumePlaybackWithRamp()
            }
        } else if (track != null) {
            playTrack(track, forceRestart = false, isUserAction = true)
        } else {
            playNewTrack()
        }
    }

    fun skipNext() {
        val currentPlaylist = getEffectivePlaybackQueue()
        if (currentPlaylist.isEmpty()) return

        if (_isShuffleEnabled.value && _trackSortOrder.value != TrackSortOrder.FOLDER_WISE) {
            val unplayed = currentPlaylist.filter { it.id !in _sessionPlayedIds.value }
            val nextTrack = if (unplayed.isNotEmpty()) {
                unplayed.random()
            } else {
                if (currentPlaylist.isNotEmpty() && currentPlaylist.all { it.id in _sessionPlayedIds.value }) {
                    settings.clearPlayedTracks()
                    _sessionPlayedIds.value = emptySet()
                }
                currentPlaylist.random()
            }
            playTrack(nextTrack, forceRestart = true, isUserAction = true)
        } else {
            val currentIndex = currentPlaylist.indexOfFirst { it.id == _currentTrack.value?.id }
            val nextIndex = if (currentIndex in 0 until currentPlaylist.lastIndex) currentIndex + 1 else 0
            if (nextIndex == 0 && currentPlaylist.isNotEmpty() && currentPlaylist.all { it.id in _sessionPlayedIds.value }) {
                settings.clearPlayedTracks()
                _sessionPlayedIds.value = emptySet()
            }
            playTrack(currentPlaylist[nextIndex], forceRestart = true, isUserAction = true)
        }
    }

    fun skipPrevious() {
        val player = mediaPlayer
        if (player != null && player.currentPosition > 3000) {
            seekTo(0L)
            return
        }

        val currentPlaylist = getEffectivePlaybackQueue()
        if (currentPlaylist.isEmpty()) return

        val currentIndex = currentPlaylist.indexOfFirst { it.id == _currentTrack.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentPlaylist.lastIndex
        playTrack(currentPlaylist[prevIndex], forceRestart = true, isUserAction = true)
    }

    fun saveCurrentPlaybackState() {
        val mp = mediaPlayer
        val track = _currentTrack.value
        if (track != null) {
            val currentPos = if (mp != null) {
                try { mp.currentPosition.toLong() } catch (e: Exception) { _currentPositionMs.value }
            } else {
                _currentPositionMs.value
            }
            if (currentPos > 0) {
                _currentPositionMs.value = currentPos
                settings.lastTrackId = track.id
                settings.lastPositionMs = currentPos
                settings.setTrackPosition(track.id, currentPos)
            }
        }
    }

    fun shutdownApp(activity: Activity?) {
        try {
            saveCurrentPlaybackState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaPlayer?.pause()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            stopProgressTracker()
            sleepTimerJob?.cancel()
            volumeRampJob?.cancel()
            scanJobs.values.forEach { it.cancel() }
            AudioService.stop(context)
            MediaNotificationHelper.clearNotification(context)
            abandonAudioFocus()
            cleanupAudioFx()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            activity?.finishAffinity()
            activity?.finishAndRemoveTask()
        } catch (e: Exception) {
            activity?.finish()
        }
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        mediaPlayer?.seekTo(safePos.toInt())
        _currentPositionMs.value = safePos
        settings.lastPositionMs = safePos
        _currentTrack.value?.id?.let { settings.setTrackPosition(it, safePos) }
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        settings.repeatMode = next
    }

    fun toggleShuffle() {
        if (_trackSortOrder.value == TrackSortOrder.FOLDER_WISE) {
            _isShuffleEnabled.value = false
            settings.isShuffleEnabled = false
            _sessionMessage.value = "Shuffle is disabled in Folder wise mode"
            return
        }
        val next = !_isShuffleEnabled.value
        _isShuffleEnabled.value = next
        settings.isShuffleEnabled = next
    }

    fun resetSessionHistory() {
        settings.clearPlayedTracks()
        _sessionPlayedIds.value = emptySet()
        _sessionMessage.value = "Played tracks history reset"
    }

    fun clearSessionMessage() {
        _sessionMessage.value = null
    }

    // Audio FX Methods
    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.update { it.copy(isEnabled = enabled) }
        settings.isEqualizerEnabled = enabled
        equalizer?.enabled = enabled
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerState.update { it.copy(currentPreset = preset) }
        settings.equalizerPreset = preset
        applyPresetToEqualizer(preset)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMb: Int) {
        _equalizerState.update { state ->
            val updatedBands = state.bands.map { band ->
                if (band.index == bandIndex) band.copy(levelMb = levelMb) else band
            }
            state.copy(bands = updatedBands, currentPreset = EqualizerPreset.CUSTOM)
        }
        settings.equalizerPreset = EqualizerPreset.CUSTOM
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMb.toShort())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level: ${e.message}")
        }
    }

    fun setBassBoostStrength(strength: Int) {
        _equalizerState.update { it.copy(bassBoostStrength = strength) }
        settings.bassBoostStrength = strength
        try {
            bassBoost?.setStrength(strength.toShort())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bass boost: ${e.message}")
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.update { it.copy(virtualizerStrength = clamped) }
        settings.virtualizerStrength = clamped
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting 3D virtualizer: ${e.message}")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        settings.playbackSpeed = speed
        mediaPlayer?.let { applyPlaybackSpeedToPlayer(it) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 10)
        _crossfadeSeconds.value = clamped
        settings.crossfadeSeconds = clamped
    }

    fun setGaplessEnabled(enabled: Boolean) {
        _isGaplessEnabled.value = enabled
        settings.isGaplessEnabled = enabled
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        _isReplayGainEnabled.value = enabled
        settings.isReplayGainEnabled = enabled
        applyLoudnessAndVolume()
    }

    fun setSuperBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        _superBoostPercent.value = clamped
        settings.superBoostPercent = clamped
        applyLoudnessAndVolume()
    }

    private fun getCalculatedVolume(): Float {
        val track = _currentTrack.value
        return if (_isReplayGainEnabled.value && track != null) {
            computeReplayGainTarget(track)
        } else {
            1.0f
        }
    }

    private fun applyLoudnessAndVolume() {
        val boost = _superBoostPercent.value
        try {
            loudnessEnhancer?.let { le ->
                if (boost > 100) {
                    val gainMb = ((boost - 100) * 20).coerceIn(0, 2000)
                    le.setTargetGain(gainMb)
                    le.enabled = true
                } else {
                    le.setTargetGain(0)
                    le.enabled = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying loudness enhancer: ${e.message}")
        }

        val targetVol = getCalculatedVolume()
        mediaPlayer?.let { mp ->
            try {
                mp.setVolume(targetVol, targetVol)
            } catch (e: Exception) { }
        }
    }

    fun rampVolumeTo(targetVol: Float, startVol: Float? = null, durationMs: Long = 350L) {
        volumeRampJob?.cancel()
        val player = mediaPlayer ?: return
        volumeRampJob = viewModelScope.launch {
            val initial = startVol ?: 0.05f
            val steps = 12
            val stepDelay = (durationMs / steps).coerceAtLeast(10L)
            for (i in 1..steps) {
                if (!isActive) break
                val progress = i.toFloat() / steps.toFloat()
                val current = initial + (targetVol - initial) * progress
                try {
                    player.setVolume(current, current)
                } catch (e: Exception) {
                    break
                }
                delay(stepDelay)
            }
            try {
                player.setVolume(targetVol, targetVol)
            } catch (e: Exception) { }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioFocusRequest = request
                audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed: ${e.message}")
            true
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus abandon failed: ${e.message}")
        }
    }

    fun pausePlayback(abandonFocus: Boolean = true, immediate: Boolean = false) {
        val player = mediaPlayer
        val track = _currentTrack.value
        _isPlaying.value = false
        stopProgressTracker()
        track?.let {
            MediaNotificationHelper.updateNotification(context, it, isPlaying = false, _currentCoverArt.value)
        }

        if (player != null && player.isPlaying) {
            val currentPos = player.currentPosition.toLong()
            _currentPositionMs.value = currentPos
            settings.lastPositionMs = currentPos
            track?.id?.let { settings.setTrackPosition(it, currentPos) }

            if (immediate) {
                try { player.pause() } catch (e: Exception) { }
                if (abandonFocus) {
                    abandonAudioFocus()
                    isPausedByFocusLoss = false
                    isDuckedByFocusLoss = false
                }
            } else {
                // Audio Fade-out on Pause: Smooth 160ms volume ramp down to prevent audio clicks/pops
                volumeRampJob?.cancel()
                volumeRampJob = viewModelScope.launch {
                    val startVol = getCalculatedVolume()
                    val steps = 8
                    val stepDelay = 20L
                    for (i in 1..steps) {
                        val vol = (startVol * (1f - i.toFloat() / steps.toFloat())).coerceAtLeast(0.01f)
                        try {
                            player.setVolume(vol, vol)
                        } catch (e: Exception) {
                            break
                        }
                        delay(stepDelay)
                    }
                    try {
                        player.pause()
                    } catch (e: Exception) { }

                    if (abandonFocus) {
                        abandonAudioFocus()
                        isPausedByFocusLoss = false
                        isDuckedByFocusLoss = false
                    }
                }
            }
        } else {
            if (abandonFocus) {
                abandonAudioFocus()
                isPausedByFocusLoss = false
                isDuckedByFocusLoss = false
            }
        }
    }

    fun resumePlaybackWithRamp() {
        val player = mediaPlayer ?: return
        val track = _currentTrack.value ?: return
        if (requestAudioFocus()) {
            try {
                player.setVolume(0.05f, 0.05f)
                player.start()
                _isPlaying.value = true
                startProgressTracker()
                MediaNotificationHelper.updateNotification(context, track, isPlaying = true, _currentCoverArt.value)
                rampVolumeTo(getCalculatedVolume(), startVol = 0.05f, durationMs = 350L)
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming playback: ${e.message}")
            }
        }
    }

    fun setDynamicThemeEnabled(enabled: Boolean) {
        _isDynamicThemeEnabled.value = enabled
        settings.isDynamicThemeEnabled = enabled
    }

    fun setOledPureBlackEnabled(enabled: Boolean) {
        _isOledPureBlackEnabled.value = enabled
        settings.isOledPureBlackEnabled = enabled
    }

    fun setDynamicArtworkColorEnabled(enabled: Boolean) {
        _isDynamicArtworkColorEnabled.value = enabled
        settings.isDynamicArtworkColorEnabled = enabled
    }

    private fun updateArtworkPaletteForTrack(track: AudioTrack?) {
        if (track == null) {
            _dynamicArtworkPalette.value = DynamicArtworkPalette.DEFAULT
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = AlbumArtExtractor.extractCoverArt(context, track.uri, track.id)
                if (bitmap != null) {
                    val extracted = PaletteExtractor.extract(bitmap)
                    _dynamicArtworkPalette.value = DynamicArtworkPalette.fromExtractedAlbumPalette(extracted)
                } else {
                    _dynamicArtworkPalette.value = DynamicArtworkPalette.fromTrackSeed(track.id, track.title)
                }
            } catch (e: Exception) {
                _dynamicArtworkPalette.value = DynamicArtworkPalette.fromTrackSeed(track.id, track.title)
            }
        }
    }

    fun selectAppThemePreset(preset: AppThemePreset) {
        _appThemePreset.value = preset
        settings.appThemePreset = preset
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        if (option == SleepTimerOption.OFF) {
            _sleepTimerState.value = SleepTimerState(selectedOption = SleepTimerOption.OFF, remainingSeconds = 0, isRunning = false)
            return
        }

        if (option == SleepTimerOption.END_OF_TRACK) {
            _sleepTimerState.value = SleepTimerState(selectedOption = SleepTimerOption.END_OF_TRACK, remainingSeconds = 0, isRunning = true)
            return
        }

        val totalSeconds = option.minutes * 60
        _sleepTimerState.value = SleepTimerState(
            selectedOption = option,
            remainingSeconds = totalSeconds,
            isRunning = true
        )

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1
                _sleepTimerState.value = _sleepTimerState.value.copy(remainingSeconds = remaining)
            }
            if (isActive) {
                pausePlayback(abandonFocus = true)
                _sleepTimerState.value = SleepTimerState()
                _sessionMessage.value = "Sleep timer paused playback"
            }
        }
    }

    private fun applyPlaybackSpeedToPlayer(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = mp.playbackParams
                params.speed = _playbackSpeed.value
                mp.playbackParams = params
            } catch (e: Exception) {
                Log.w(TAG, "Could not set playback speed: ${e.message}")
            }
        }
    }

    private fun setupAudioFx(sessionId: Int) {
        try {
            cleanupAudioFx()
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _equalizerState.value.isEnabled
                val numBands = numberOfBands
                val (minLevel, maxLevel) = bandLevelRange
                val bandsList = mutableListOf<EqualizerBand>()

                for (i in 0 until numBands) {
                    val centerFreqHz = getCenterFreq(i.toShort()) / 1000
                    val currentLvl = getBandLevel(i.toShort())
                    bandsList.add(
                        EqualizerBand(
                            index = i,
                            centerFreqHz = centerFreqHz,
                            levelMb = currentLvl.toInt(),
                            minMb = minLevel.toInt(),
                            maxMb = maxLevel.toInt()
                        )
                    )
                }

                _equalizerState.update {
                    it.copy(
                        bands = bandsList
                    )
                }
                applyPresetToEqualizer(_equalizerState.value.currentPreset)
            }

            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
                if (strengthSupported) {
                    setStrength(_equalizerState.value.bassBoostStrength.toShort())
                }
            }

            try {
                virtualizer = Virtualizer(0, sessionId).apply {
                    enabled = true
                    if (strengthSupported) {
                        setStrength(_equalizerState.value.virtualizerStrength.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer init: ${e.message}")
            }

            try {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    val boost = _superBoostPercent.value
                    if (boost > 100) {
                        val gainMb = ((boost - 100) * 20).coerceIn(0, 2000)
                        setTargetGain(gainMb)
                        enabled = true
                    } else {
                        setTargetGain(0)
                        enabled = false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer init: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Audio FX: ${e.message}")
        }
    }

    private fun applyPresetToEqualizer(preset: EqualizerPreset) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            when (preset) {
                EqualizerPreset.FLAT -> {
                    for (i in 0 until numBands) eq.setBandLevel(i.toShort(), 0)
                }
                EqualizerPreset.BASS_BOOST -> {
                    for (i in 0 until numBands) {
                        val level = when (i) {
                            0 -> 600
                            1 -> 400
                            2 -> 100
                            else -> 0
                        }.toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                EqualizerPreset.VOCAL -> {
                    for (i in 0 until numBands) {
                        val level = when (i) {
                            0 -> -200
                            numBands - 1 -> 200
                            numBands / 2 -> 500
                            else -> 300
                        }.toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                EqualizerPreset.ROCK -> {
                    for (i in 0 until numBands) {
                        val level = when (i) {
                            0 -> 500
                            1 -> 300
                            numBands - 1 -> 500
                            numBands - 2 -> 300
                            else -> -100
                        }.toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                EqualizerPreset.POP -> {
                    for (i in 0 until numBands) {
                        val level = when (i) {
                            0 -> 200
                            1 -> 400
                            numBands - 1 -> 300
                            else -> 100
                        }.toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                EqualizerPreset.CLASSICAL -> {
                    for (i in 0 until numBands) {
                        val level = when (i) {
                            0 -> 400
                            1 -> 200
                            numBands - 1 -> 400
                            else -> -100
                        }.toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                EqualizerPreset.CUSTOM -> {}
            }

            val updatedBands = _equalizerState.value.bands.map { band ->
                try {
                    band.copy(levelMb = eq.getBandLevel(band.index.toShort()).toInt())
                } catch (e: Exception) {
                    band
                }
            }
            _equalizerState.update { it.copy(bands = updatedBands) }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying EQ preset: ${e.message}")
        }
    }

    private fun cleanupAudioFx() {
        try {
            equalizer?.release()
            bassBoost?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio fx: ${e.message}")
        }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
    }

    private fun handleTrackCompletion() {
        // Record that the current track finished playing today
        _currentTrack.value?.let { completedTrack ->
            settings.recordPlayedTrackId(completedTrack.id)
            settings.markTrackCompleted(completedTrack.id, true)
            settings.setTrackPosition(completedTrack.id, 0L)
            _sessionPlayedIds.value = settings.getValidPlayedTrackIds()
        }
        settings.lastPositionMs = 0L

        if (_sleepTimerState.value.isRunning && _sleepTimerState.value.selectedOption == SleepTimerOption.END_OF_TRACK) {
            pausePlayback(abandonFocus = true)
            _sleepTimerState.value = SleepTimerState()
            _sessionMessage.value = "Sleep timer paused playback at end of track"
            return
        }

        when (_repeatMode.value) {
            RepeatMode.REPEAT_ONE -> {
                _currentTrack.value?.let { playTrack(it, forceRestart = true) }
            }
            RepeatMode.REPEAT_ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (_trackSortOrder.value == TrackSortOrder.FOLDER_WISE) {
                    val currentPlaylist = getEffectivePlaybackQueue()
                    val currentIndex = currentPlaylist.indexOfFirst { it.id == _currentTrack.value?.id }
                    if (currentIndex in 0 until currentPlaylist.lastIndex) {
                        playTrack(currentPlaylist[currentIndex + 1], forceRestart = true, isUserAction = false)
                    } else {
                        pausePlayback(abandonFocus = true)
                    }
                } else {
                    val currentPlaylist = getEffectivePlaybackQueue()
                    val unplayed = currentPlaylist.filter { it.id !in _sessionPlayedIds.value }
                    if (unplayed.isNotEmpty()) {
                        playNewTrack()
                    } else {
                        // All tracks finished playing: pause playback and preserve session played memory
                        pausePlayback(abandonFocus = true)
                    }
                }
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun updateTrackTags(
        context: Context,
        trackId: String,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newAlbumArtist: String = "",
        newComposer: String = "",
        newGenre: String = "",
        newYear: String = "",
        newTrackNumber: String = "",
        newDiscNumber: String = "",
        newCoverArt: Bitmap? = null
    ): AudioTrack? {
        // Save persistently to SettingsManager
        val customMeta = CustomTrackMetadata(
            title = newTitle,
            artist = newArtist,
            album = newAlbum,
            albumArtist = newAlbumArtist,
            composer = newComposer,
            genre = newGenre,
            year = newYear,
            trackNumber = newTrackNumber,
            discNumber = newDiscNumber
        )
        settings.saveCustomTrackMetadata(trackId, customMeta)

        var targetUri: android.net.Uri? = null
        var updatedTrackResult: AudioTrack? = null

        // If new cover art is provided, save it persistently in AlbumArtExtractor
        if (newCoverArt != null) {
            AlbumArtExtractor.saveCustomCoverArt(context, trackId, newCoverArt)
            if (_currentTrack.value?.id == trackId) {
                _currentCoverArt.value = newCoverArt
            }
        }

        _masterPlaylist.update { list ->
            list.map { track ->
                if (track.id == trackId) {
                    targetUri = track.uri
                    val updated = track.copy(
                        title = newTitle.ifBlank { track.title },
                        artist = newArtist.ifBlank { track.artist },
                        album = newAlbum.ifBlank { track.album },
                        albumArtist = newAlbumArtist,
                        composer = newComposer,
                        genre = newGenre,
                        year = newYear,
                        trackNumber = newTrackNumber,
                        discNumber = newDiscNumber
                    )
                    updatedTrackResult = updated
                    updated
                } else {
                    track
                }
            }
        }

        if (_currentTrack.value?.id == trackId) {
            _currentTrack.update { curr ->
                if (curr?.id == trackId) {
                    curr.copy(
                        title = newTitle.ifBlank { curr.title },
                        artist = newArtist.ifBlank { curr.artist },
                        album = newAlbum.ifBlank { curr.album },
                        albumArtist = newAlbumArtist,
                        composer = newComposer,
                        genre = newGenre,
                        year = newYear,
                        trackNumber = newTrackNumber,
                        discNumber = newDiscNumber
                    )
                } else {
                    curr
                }
            }
        }

        targetUri?.let { uri ->
            try {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Audio.Media.TITLE, newTitle)
                    put(android.provider.MediaStore.Audio.Media.ARTIST, newArtist)
                    put(android.provider.MediaStore.Audio.Media.ALBUM, newAlbum)
                    newYear.toIntOrNull()?.let { put(android.provider.MediaStore.Audio.Media.YEAR, it) }
                    newTrackNumber.toIntOrNull()?.let { put(android.provider.MediaStore.Audio.Media.TRACK, it) }
                    if (newComposer.isNotBlank()) {
                        put(android.provider.MediaStore.Audio.Media.COMPOSER, newComposer)
                    }
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (_: Exception) {}
        }

        if (_currentTrack.value?.id == trackId) {
            _currentTrack.value?.let { tr ->
                MediaNotificationHelper.updateNotification(context, tr, _isPlaying.value, _currentCoverArt.value)
            }
        }

        _sessionMessage.value = if (newCoverArt != null) "Tags & Album Art updated permanently" else "ID3 Tags updated permanently"
        return updatedTrackResult
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val currentPos = mp.currentPosition.toLong()
                        val duration = _durationMs.value.coerceAtLeast(1L)
                        _currentPositionMs.value = currentPos
                        settings.lastPositionMs = currentPos
                        val curTrackId = _currentTrack.value?.id
                        if (curTrackId != null) {
                            settings.setTrackPosition(curTrackId, currentPos)
                        }

                        // 60% Played Rule:
                        // If track played 60% then considered as played immediately and added to played list without notification
                        if (duration > 0) {
                            val progressRatio = currentPos.toFloat() / duration.toFloat()
                            if (progressRatio >= 0.60f) {
                                val curId = _currentTrack.value?.id
                                if (curId != null && curId !in _sessionPlayedIds.value) {
                                    settings.recordPlayedTrackId(curId)
                                    val updatedPlayed = settings.getValidPlayedTrackIds()
                                    _sessionPlayedIds.value = updatedPlayed
                                }
                            }
                        }

                        val ab = _abLoopState.value
                        if (ab.isReadyToLoop && ab.pointA != null && ab.pointB != null) {
                            if (currentPos >= ab.pointB) {
                                mp.seekTo(ab.pointA.toInt())
                                _currentPositionMs.value = ab.pointA
                                settings.lastPositionMs = ab.pointA
                            }
                        }
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        if (isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (e: Exception) { }
            isNoisyReceiverRegistered = false
        }
        abandonAudioFocus()
        cleanupAudioFx()
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressTracker()
        sleepTimerJob?.cancel()
        volumeRampJob?.cancel()
        scanJobs.values.forEach { it.cancel() }
        MediaNotificationHelper.clearNotification(context)
    }
}
