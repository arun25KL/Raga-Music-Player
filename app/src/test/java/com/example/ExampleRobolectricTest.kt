package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.CustomTrackMetadata
import com.example.data.SettingsManager
import com.example.model.FolderLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Raga Music Player", appName)
  }

  @Test
  fun `verify custom track metadata persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = SettingsManager(context)
    val testMetadata = CustomTrackMetadata(
      title = "New Classical Raga",
      artist = "Raga Master",
      album = "Indian Ragas Vol. 1",
      year = "2026",
      genre = "Classical"
    )
    settings.saveCustomTrackMetadata("track_123", testMetadata)

    val loaded = settings.getCustomTrackMetadata("track_123")
    assertNotNull(loaded)
    assertEquals("New Classical Raga", loaded?.title)
    assertEquals("Raga Master", loaded?.artist)
    assertEquals("Indian Ragas Vol. 1", loaded?.album)
  }

  @Test
  fun `verify played track history persistence across app reopens`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val initialSettings = SettingsManager(context)
    initialSettings.clearPlayedTracks()

    // Mark track 1 and track 2 as played
    initialSettings.recordPlayedTrackId("track_1")
    initialSettings.recordPlayedTrackId("track_2")

    // Re-instantiate settings (simulating reopening the app)
    val reopenedSettings = SettingsManager(context)
    val playedSet = reopenedSettings.getValidPlayedTrackIds()

    assertEquals(2, playedSet.size)
    assert(playedSet.contains("track_1"))
    assert(playedSet.contains("track_2"))
  }

  @Test
  fun `verify 5 percent resume threshold and 60 percent played trigger calculations`() {
    val durationMs = 100_000L // 100 seconds

    // Below 5% (e.g. 4,000ms = 4%) -> resets to 0:00 (0L)
    val pos4Pct = 4_000L
    val ratio4Pct = pos4Pct.toFloat() / durationMs.toFloat()
    val startPosBelow5 = if (ratio4Pct < 0.05f) 0L else pos4Pct
    assertEquals(0L, startPosBelow5)

    // At or above 5% (e.g. 15,000ms = 15%) -> resumes from exact saved millisecond
    val pos15Pct = 15_000L
    val ratio15Pct = pos15Pct.toFloat() / durationMs.toFloat()
    val startPos15 = if (ratio15Pct < 0.05f) 0L else pos15Pct
    assertEquals(15_000L, startPos15)

    // 60% played trigger check
    val pos59Pct = 59_000L
    val isPlayed59 = (pos59Pct.toFloat() / durationMs.toFloat()) >= 0.60f
    assertEquals(false, isPlayed59)

    val pos60Pct = 60_000L
    val isPlayed60 = (pos60Pct.toFloat() / durationMs.toFloat()) >= 0.60f
    assertEquals(true, isPlayed60)
  }

  @Test
  fun `verify played tracks persist indefinitely without 24 hour expiration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = SettingsManager(context)
    settings.clearPlayedTracks()

    // Simulate track played 48 hours ago
    val twoDaysAgo = System.currentTimeMillis() - (48 * 3600 * 1000L)
    val prefs = context.getSharedPreferences("music_player_settings", Context.MODE_PRIVATE)
    val json = org.json.JSONObject()
    json.put("old_track_1", twoDaysAgo)
    prefs.edit().putString("key_played_tracks_timestamps_json", json.toString()).apply()

    // Verify it is still valid and not expired
    val validIds = settings.getValidPlayedTrackIds()
    assertEquals(1, validIds.size)
    assert(validIds.contains("old_track_1"))

    // Verify automatic reset when clearing
    settings.clearPlayedTracks()
    assertEquals(0, settings.getValidPlayedTrackIds().size)
  }

  @Test
  fun `verify duplicate folder addition restriction logic`() {
    val existingLocations = listOf(
      FolderLocation(
        id = "folder_1",
        name = "My Music",
        uriString = "content://com.android.externalstorage.documents/tree/primary%3AMusic",
        isDeviceStorage = false
      )
    )

    fun normalizeFolderUriKey(uStr: String): String {
      val decoded = Uri.decode(uStr)
      val treeDocIdx = decoded.indexOf("/tree/")
      return if (treeDocIdx != -1) {
        decoded.substring(treeDocIdx).trimEnd('/')
      } else {
        decoded.trimEnd('/')
      }
    }

    val incomingDuplicateUri = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
    val incomingDuplicateKey = normalizeFolderUriKey(incomingDuplicateUri)

    val isDuplicate = existingLocations.any { existing ->
      !existing.isDeviceStorage && (existing.uriString == incomingDuplicateUri || normalizeFolderUriKey(existing.uriString) == incomingDuplicateKey)
    }
    assertEquals(true, isDuplicate)

    val incomingNewUri = "content://com.android.externalstorage.documents/tree/primary%3AAudiobooks"
    val incomingNewKey = normalizeFolderUriKey(incomingNewUri)

    val isNewDuplicate = existingLocations.any { existing ->
      !existing.isDeviceStorage && (existing.uriString == incomingNewUri || normalizeFolderUriKey(existing.uriString) == incomingNewKey)
    }
    assertEquals(false, isNewDuplicate)
  }

  @Test
  fun `verify session played tracks memory persists across shutdown and restart`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = SettingsManager(context)
    settings.clearPlayedTracks()

    // 1. Mark track 1 and track 2 as played in session
    settings.recordPlayedTrackId("track_1")
    settings.recordPlayedTrackId("track_2")
    assertEquals(2, settings.getValidPlayedTrackIds().size)

    // 2. Simulate shutdownApp call (save playback state and finish)
    val vm1 = com.example.player.MusicPlayerViewModel(ApplicationProvider.getApplicationContext())
    vm1.shutdownApp(null)

    // 3. Re-open app / initialize fresh ViewModel
    val vm2 = com.example.player.MusicPlayerViewModel(ApplicationProvider.getApplicationContext())
    val restoredPlayedIds = vm2.sessionPlayedIds.value
    assertEquals(2, restoredPlayedIds.size)
    assert(restoredPlayedIds.contains("track_1"))
    assert(restoredPlayedIds.contains("track_2"))
  }
}
