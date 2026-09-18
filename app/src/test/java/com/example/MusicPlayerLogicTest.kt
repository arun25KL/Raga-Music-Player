package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import com.example.model.AudioSourceType
import com.example.model.EqualizerPreset
import com.example.model.RepeatMode
import com.example.model.SleepTimerOption
import com.example.model.TrackSortOrder
import com.example.player.MusicPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicPlayerLogicTest {

    private lateinit var app: Application
    private lateinit var settingsManager: SettingsManager
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(app)
        settingsManager.clearAll()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingsPersistence() {
        settingsManager.includeSubfolders = false
        assertFalse(settingsManager.includeSubfolders)

        settingsManager.includeSubfolders = true
        assertTrue(settingsManager.includeSubfolders)

        settingsManager.repeatMode = RepeatMode.REPEAT_ONE
        assertEquals(RepeatMode.REPEAT_ONE, settingsManager.repeatMode)

        settingsManager.playbackSpeed = 1.5f
        assertEquals(1.5f, settingsManager.playbackSpeed, 0.01f)

        settingsManager.equalizerPreset = EqualizerPreset.ROCK
        assertEquals(EqualizerPreset.ROCK, settingsManager.equalizerPreset)
    }

    @Test
    fun testViewModelInitializationAndSamplePack() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unplayedCount.collect() }

        viewModel.loadSampleTracks().join()
        testScheduler.advanceUntilIdle()

        // Master tracks should be populated with sample pack
        val master = viewModel.masterPlaylist.value
        assertTrue(master.isNotEmpty())
        assertEquals(5, master.size)

        // Verify all 5 tracks are mp3 or m4a
        master.forEach { track ->
            assertTrue(track.fileExtension == "mp3" || track.fileExtension == "m4a")
        }
    }

    @Test
    fun testNeverRepeatedTrackSelectionAnd85PercentRule() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unplayedCount.collect() }

        viewModel.loadSampleTracks().join()
        testScheduler.advanceUntilIdle()

        val activeList = viewModel.activePlaylist.value
        assertEquals(5, activeList.size)

        // Play random new track
        viewModel.playNewTrack()
        testScheduler.advanceUntilIdle()
        val current = viewModel.currentTrack.value
        assertNotNull(current)

        // Mark track as played when reaching 85%
        settingsManager.recordPlayedTrackId(current!!.id)
        viewModel.playNewTrack()
        testScheduler.advanceUntilIdle()

        // Session played IDs should now contain previous track
        assertTrue(viewModel.sessionPlayedIds.value.contains(current.id))
        assertEquals(4, viewModel.unplayedCount.value)
    }

    @Test
    fun testResumeRulesFor5PercentThreshold() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }

        viewModel.loadSampleTracks().join()
        testScheduler.advanceUntilIdle()

        val track = viewModel.activePlaylist.value.first()
        settingsManager.lastTrackId = track.id

        // Case 1: Below 5% progress (e.g. 5,000ms out of 185,000ms = 2.7%)
        settingsManager.lastPositionMs = 5000L
        settingsManager.setTrackPosition(track.id, 5000L)
        viewModel.playTrack(track, forceRestart = true, isUserAction = true)
        testScheduler.advanceUntilIdle()
        // Should resume from last position
        assertEquals(5000L, viewModel.currentPositionMs.value)

        // Case 2: 5% to 100% progress (e.g. 20,000ms out of 185,000ms = 10.8%)
        settingsManager.lastPositionMs = 20000L
        settingsManager.setTrackPosition(track.id, 20000L)
        viewModel.playTrack(track, forceRestart = true, isUserAction = true)
        testScheduler.advanceUntilIdle()
        // Should resume from last position
        assertEquals(20000L, viewModel.currentPositionMs.value)
    }

    @Test
    fun testNextAndPreviousNavigation() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unplayedCount.collect() }

        viewModel.loadSampleTracks().join()
        testScheduler.advanceUntilIdle()
        val playlist = viewModel.activePlaylist.value
        assertTrue(playlist.size >= 2)

        // Start playing track 0 in playlist
        viewModel.playTrack(playlist[0])
        testScheduler.advanceUntilIdle()
        assertEquals(playlist[0].id, viewModel.currentTrack.value?.id)

        // Next should move to track 1
        viewModel.skipNext()
        testScheduler.advanceUntilIdle()
        assertEquals(playlist[1].id, viewModel.currentTrack.value?.id)

        // Previous should move back to track 0
        viewModel.skipPrevious()
        testScheduler.advanceUntilIdle()
        assertEquals(playlist[0].id, viewModel.currentTrack.value?.id)
    }

    @Test
    fun testSleepTimerOption() = runTest {
        val viewModel = MusicPlayerViewModel(app)
        viewModel.setSleepTimer(SleepTimerOption.MIN_30)
        
        val timerState = viewModel.sleepTimerState.value
        assertTrue(timerState.isRunning)
        assertEquals(SleepTimerOption.MIN_30, timerState.selectedOption)
        assertEquals(1800, timerState.remainingSeconds)

        viewModel.setSleepTimer(SleepTimerOption.OFF)
        assertFalse(viewModel.sleepTimerState.value.isRunning)
    }

    @Test
    fun testSubfolderExclusionAndPlayNewTrackRespectsExclusion() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.unplayedCount.collect() }

        viewModel.loadSampleTracks().join()
        testScheduler.advanceUntilIdle()

        // Sample pack has:
        // root: electric_dawn.mp3
        // sub_lofi: midnight_groove.mp3
        // sub_synth: cyber_pulse.m4a
        // sub_acoustic: acoustic_breeze.mp3
        // sub_ambient: deep_space_drift.m4a
        val allSubs = viewModel.subfolders.value
        assertTrue(allSubs.isNotEmpty())

        // Exclude sub_synth and sub_lofi
        viewModel.toggleSubfolderExclusion("sub_synth")
        viewModel.toggleSubfolderExclusion("sub_lofi")
        testScheduler.advanceUntilIdle()

        val activeList = viewModel.activePlaylist.value
        assertEquals(3, activeList.size)
        assertFalse(activeList.any { it.subfolderId == "sub_synth" })
        assertFalse(activeList.any { it.subfolderId == "sub_lofi" })

        // Repeatedly call playNewTrack and ensure it NEVER plays excluded subfolder songs
        for (i in 1..10) {
            viewModel.playNewTrack()
            testScheduler.advanceUntilIdle()
            val current = viewModel.currentTrack.value
            assertNotNull(current)
            assertFalse("Track from excluded subfolder played!", current!!.subfolderId == "sub_synth" || current.subfolderId == "sub_lofi")
        }

        // When all subfolders are excluded, only root track should remain in active playlist
        viewModel.excludeAllSubfolders()
        testScheduler.advanceUntilIdle()
        val rootOnlyList = viewModel.activePlaylist.value
        assertEquals(1, rootOnlyList.size)
        assertEquals("root", rootOnlyList[0].subfolderId)

        // Toggling includeSubfolders off should also leave only root track
        viewModel.includeAllSubfolders()
        viewModel.setIncludeSubfolders(false)
        testScheduler.advanceUntilIdle()
        val includeOffList = viewModel.activePlaylist.value
        assertEquals(1, includeOffList.size)
        assertEquals("root", includeOffList[0].subfolderId)
    }

    @Test
    fun testFolderWiseSorting() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }

        viewModel.loadSampleTracks()
        testScheduler.advanceUntilIdle()

        viewModel.setTrackSortOrder(TrackSortOrder.FOLDER_WISE)
        testScheduler.advanceUntilIdle()

        val list = viewModel.activePlaylist.value
        assertTrue(list.isNotEmpty())
        assertEquals(TrackSortOrder.FOLDER_WISE, viewModel.trackSortOrder.value)

        // Ensure tracks in the same subfolder are grouped together sequentially
        val subfolderIds = list.map { it.subfolderId }
        val distinctSubfolderCount = subfolderIds.distinct().size
        var transitions = 1
        for (i in 0 until subfolderIds.size - 1) {
            if (subfolderIds[i] != subfolderIds[i + 1]) {
                transitions++
            }
        }
        assertEquals(distinctSubfolderCount, transitions)
    }

    @Test
    fun testFolderWiseDisablesShuffle() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }

        viewModel.toggleShuffle()
        assertTrue(viewModel.isShuffleEnabled.value)

        viewModel.setTrackSortOrder(TrackSortOrder.FOLDER_WISE)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.isShuffleEnabled.value)

        viewModel.toggleShuffle()
        assertFalse(viewModel.isShuffleEnabled.value)
    }

    @Test
    fun testFolderWisePlaybackQueueScopedToFolder() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }

        viewModel.loadSampleTracks()
        testScheduler.advanceUntilIdle()

        viewModel.setTrackSortOrder(TrackSortOrder.FOLDER_WISE)
        testScheduler.advanceUntilIdle()

        val allTracks = viewModel.activePlaylist.value
        val track1 = allTracks.first()

        // Play track1 in first folder
        viewModel.playTrack(track1, forceRestart = true, isUserAction = true)
        testScheduler.advanceUntilIdle()

        val queue1 = viewModel.getEffectivePlaybackQueue()
        val firstFolderKey = track1.subfolderId.ifBlank { track1.relativePath }
        assertTrue(queue1.all { (it.subfolderId.ifBlank { it.relativePath }) == firstFolderKey })

        // Find a track in a different folder
        val trackOtherFolder = allTracks.firstOrNull { (it.subfolderId.ifBlank { it.relativePath }) != firstFolderKey }
        if (trackOtherFolder != null) {
            viewModel.playTrack(trackOtherFolder, forceRestart = true, isUserAction = true)
            testScheduler.advanceUntilIdle()

            val queue2 = viewModel.getEffectivePlaybackQueue()
            val secondFolderKey = trackOtherFolder.subfolderId.ifBlank { trackOtherFolder.relativePath }
            assertTrue(queue2.all { (it.subfolderId.ifBlank { it.relativePath }) == secondFolderKey })
        }
    }

    @Test
    fun testNonFolderWiseRetainsOriginalPlaybackRules() = runTest {
        settingsManager.includeSubfolders = true
        val viewModel = MusicPlayerViewModel(app)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activePlaylist.collect() }

        viewModel.loadSampleTracks()
        testScheduler.advanceUntilIdle()

        viewModel.setTrackSortOrder(TrackSortOrder.ALPHABETICAL)
        testScheduler.advanceUntilIdle()

        // Shuffle can be enabled in Alphabetical mode
        viewModel.toggleShuffle()
        assertTrue(viewModel.isShuffleEnabled.value)

        val allTracks = viewModel.activePlaylist.value
        viewModel.playTrack(allTracks.first(), forceRestart = true, isUserAction = true)
        testScheduler.advanceUntilIdle()

        // Effective queue returns full active playlist for non-Folder Wise modes
        val queue = viewModel.getEffectivePlaybackQueue()
        assertEquals(allTracks.size, queue.size)
    }
}
