package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppThemePreset
import com.example.model.EqualizerPreset
import com.example.model.FolderLocation
import com.example.model.RepeatMode
import com.example.model.SupportedAudioFormat
import com.example.model.TrackSortOrder
import org.json.JSONArray
import org.json.JSONObject

data class CustomTrackMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val composer: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val discNumber: String = ""
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "music_player_settings"
        private const val KEY_FOLDER_LOCATIONS_JSON = "key_folder_locations_json"
        private const val KEY_ENABLED_FORMATS = "key_enabled_formats"
        private const val KEY_INCLUDE_SUBFOLDERS = "key_include_subfolders"
        private const val KEY_EXCLUDED_SUBFOLDER_IDS = "key_excluded_subfolder_ids"
        private const val KEY_REPEAT_MODE = "key_repeat_mode"
        private const val KEY_SHUFFLE_ENABLED = "key_shuffle_enabled"
        private const val KEY_PLAYBACK_SPEED = "key_playback_speed"
        private const val KEY_EQ_ENABLED = "key_eq_enabled"
        private const val KEY_EQ_PRESET = "key_eq_preset"
        private const val KEY_EQ_BASS_BOOST = "key_eq_bass_boost"
        private const val KEY_EQ_VIRTUALIZER = "key_eq_virtualizer"
        private const val KEY_LAST_TRACK_ID = "key_last_track_id"
        private const val KEY_LAST_POSITION_MS = "key_last_position_ms"
        private const val KEY_TRACK_SORT_ORDER = "key_track_sort_order"
        private const val KEY_CROSSFADE_SECONDS = "key_crossfade_seconds"
        private const val KEY_GAPLESS_ENABLED = "key_gapless_enabled"
        private const val KEY_REPLAYGAIN_ENABLED = "key_replaygain_enabled"
        private const val KEY_DYNAMIC_THEME_ENABLED = "key_dynamic_theme_enabled"
        private const val KEY_OLED_PURE_BLACK_ENABLED = "key_oled_pure_black_enabled"
        private const val KEY_DYNAMIC_ARTWORK_COLOR_ENABLED = "key_dynamic_artwork_color_enabled"
        private const val KEY_APP_THEME_PRESET = "key_app_theme_preset"
        private const val KEY_SUPER_BOOST_PERCENT = "key_super_boost_percent"
        private const val KEY_PLAYED_TRACKS_TIMESTAMPS_JSON = "key_played_tracks_timestamps_json"
        private const val KEY_TRACK_POSITIONS_JSON = "key_track_positions_json"
        private const val KEY_CUSTOM_TRACK_METADATA_JSON = "key_custom_track_metadata_json"
        private const val KEY_ART_DISPLAY_MODE = "key_art_display_mode"
        private const val KEY_ONLY_CALLS_INTERRUPT = "key_only_calls_interrupt"
        private const val KEY_WAS_PLAYING = "key_was_playing"
        private const val KEY_COMPLETED_TRACKS_SET = "key_completed_tracks_set"
    }

    var wasPlaying: Boolean
        get() = prefs.getBoolean(KEY_WAS_PLAYING, false)
        set(value) = prefs.edit().putBoolean(KEY_WAS_PLAYING, value).apply()

    fun isTrackCompleted(trackId: String): Boolean {
        val completedSet = prefs.getStringSet(KEY_COMPLETED_TRACKS_SET, emptySet()) ?: emptySet()
        return trackId in completedSet
    }

    fun markTrackCompleted(trackId: String, completed: Boolean) {
        val currentSet = prefs.getStringSet(KEY_COMPLETED_TRACKS_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (completed) {
            currentSet.add(trackId)
        } else {
            currentSet.remove(trackId)
        }
        prefs.edit().putStringSet(KEY_COMPLETED_TRACKS_SET, currentSet).commit()
    }

    var isOnlyCallsInterruptEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONLY_CALLS_INTERRUPT, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLY_CALLS_INTERRUPT, value).apply()

    var artDisplayMode: String
        get() = prefs.getString(KEY_ART_DISPLAY_MODE, "AUDIO_RING") ?: "AUDIO_RING"
        set(value) = prefs.edit().putString(KEY_ART_DISPLAY_MODE, value).apply()

    fun getCustomTrackMetadata(trackId: String): CustomTrackMetadata? {
        val jsonStr = prefs.getString(KEY_CUSTOM_TRACK_METADATA_JSON, null) ?: return null
        return try {
            val root = JSONObject(jsonStr)
            if (!root.has(trackId)) return null
            val obj = root.getJSONObject(trackId)
            CustomTrackMetadata(
                title = obj.optString("title", ""),
                artist = obj.optString("artist", ""),
                album = obj.optString("album", ""),
                albumArtist = obj.optString("albumArtist", ""),
                composer = obj.optString("composer", ""),
                genre = obj.optString("genre", ""),
                year = obj.optString("year", ""),
                trackNumber = obj.optString("trackNumber", ""),
                discNumber = obj.optString("discNumber", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveCustomTrackMetadata(trackId: String, metadata: CustomTrackMetadata) {
        val jsonStr = prefs.getString(KEY_CUSTOM_TRACK_METADATA_JSON, null) ?: "{}"
        try {
            val root = JSONObject(jsonStr)
            val obj = JSONObject().apply {
                put("title", metadata.title)
                put("artist", metadata.artist)
                put("album", metadata.album)
                put("albumArtist", metadata.albumArtist)
                put("composer", metadata.composer)
                put("genre", metadata.genre)
                put("year", metadata.year)
                put("trackNumber", metadata.trackNumber)
                put("discNumber", metadata.discNumber)
            }
            root.put(trackId, obj)
            prefs.edit().putString(KEY_CUSTOM_TRACK_METADATA_JSON, root.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllCustomTrackMetadata(): Map<String, CustomTrackMetadata> {
        val jsonStr = prefs.getString(KEY_CUSTOM_TRACK_METADATA_JSON, null) ?: return emptyMap()
        val result = mutableMapOf<String, CustomTrackMetadata>()
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val obj = root.getJSONObject(id)
                result[id] = CustomTrackMetadata(
                    title = obj.optString("title", ""),
                    artist = obj.optString("artist", ""),
                    album = obj.optString("album", ""),
                    albumArtist = obj.optString("albumArtist", ""),
                    composer = obj.optString("composer", ""),
                    genre = obj.optString("genre", ""),
                    year = obj.optString("year", ""),
                    trackNumber = obj.optString("trackNumber", ""),
                    discNumber = obj.optString("discNumber", "")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    var folderLocations: List<FolderLocation>
        get() {
            val jsonStr = prefs.getString(KEY_FOLDER_LOCATIONS_JSON, null) ?: return emptyList()
            val list = mutableListOf<FolderLocation>()
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        FolderLocation(
                            id = obj.optString("id", ""),
                            name = obj.optString("name", "Music Folder"),
                            uriString = obj.optString("uriString", ""),
                            isDeviceStorage = obj.optBoolean("isDeviceStorage", false),
                            trackCount = obj.optInt("trackCount", 0),
                            dateAddedMs = obj.optLong("dateAddedMs", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
        set(value) {
            try {
                val jsonArray = JSONArray()
                for (loc in value) {
                    val obj = JSONObject().apply {
                        put("id", loc.id)
                        put("name", loc.name)
                        put("uriString", loc.uriString)
                        put("isDeviceStorage", loc.isDeviceStorage)
                        put("trackCount", loc.trackCount)
                        put("dateAddedMs", loc.dateAddedMs)
                    }
                    jsonArray.put(obj)
                }
                prefs.edit().putString(KEY_FOLDER_LOCATIONS_JSON, jsonArray.toString()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    fun addFolderLocation(location: FolderLocation) {
        val current = folderLocations.toMutableList()
        // If already exists with same uri or id, update it
        val index = current.indexOfFirst { it.uriString == location.uriString || it.id == location.id }
        if (index >= 0) {
            current[index] = location
        } else {
            current.add(location)
        }
        folderLocations = current
    }

    fun removeFolderLocation(locationId: String) {
        val current = folderLocations.filter { it.id != locationId }
        folderLocations = current
    }

    fun updateFolderTrackCount(locationId: String, count: Int) {
        val current = folderLocations.map {
            if (it.id == locationId) it.copy(trackCount = count) else it
        }
        folderLocations = current
    }

    var enabledFormats: Set<String>
        get() {
            return prefs.getStringSet(KEY_ENABLED_FORMATS, null)
                ?: SupportedAudioFormat.ALL_FORMAT_NAMES
        }
        set(value) = prefs.edit().putStringSet(KEY_ENABLED_FORMATS, value).apply()

    fun setFormatEnabled(format: SupportedAudioFormat, enabled: Boolean) {
        val current = enabledFormats.toMutableSet()
        if (enabled) {
            current.add(format.name)
        } else {
            current.remove(format.name)
        }
        enabledFormats = current
    }

    fun isFormatEnabled(format: SupportedAudioFormat): Boolean {
        return enabledFormats.contains(format.name)
    }

    var includeSubfolders: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_SUBFOLDERS, false)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_SUBFOLDERS, value).apply()

    var excludedSubfolderIds: Set<String>
        get() = prefs.getStringSet(KEY_EXCLUDED_SUBFOLDER_IDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_EXCLUDED_SUBFOLDER_IDS, value).apply()

    var repeatMode: RepeatMode
        get() {
            val name = prefs.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name)
            return try {
                RepeatMode.valueOf(name ?: RepeatMode.OFF.name)
            } catch (e: Exception) {
                RepeatMode.OFF
            }
        }
        set(value) = prefs.edit().putString(KEY_REPEAT_MODE, value.name).apply()

    var isShuffleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHUFFLE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SHUFFLE_ENABLED, value).apply()

    var playbackSpeed: Float
        get() = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PLAYBACK_SPEED, value).apply()

    var isEqualizerEnabled: Boolean
        get() = prefs.getBoolean(KEY_EQ_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_EQ_ENABLED, value).apply()

    var equalizerPreset: EqualizerPreset
        get() {
            val name = prefs.getString(KEY_EQ_PRESET, EqualizerPreset.FLAT.name)
            return try {
                EqualizerPreset.valueOf(name ?: EqualizerPreset.FLAT.name)
            } catch (e: Exception) {
                EqualizerPreset.FLAT
            }
        }
        set(value) = prefs.edit().putString(KEY_EQ_PRESET, value.name).apply()

    var bassBoostStrength: Int
        get() = prefs.getInt(KEY_EQ_BASS_BOOST, 0)
        set(value) = prefs.edit().putInt(KEY_EQ_BASS_BOOST, value).apply()

    var virtualizerStrength: Int
        get() = prefs.getInt(KEY_EQ_VIRTUALIZER, 0)
        set(value) = prefs.edit().putInt(KEY_EQ_VIRTUALIZER, value).apply()

    var lastTrackId: String?
        get() = prefs.getString(KEY_LAST_TRACK_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_TRACK_ID, value).apply()

    var lastPositionMs: Long
        get() = prefs.getLong(KEY_LAST_POSITION_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_POSITION_MS, value).apply()

    var trackSortOrder: TrackSortOrder
        get() {
            val name = prefs.getString(KEY_TRACK_SORT_ORDER, TrackSortOrder.ALPHABETICAL.name)
            return try {
                TrackSortOrder.valueOf(name ?: TrackSortOrder.ALPHABETICAL.name)
            } catch (e: Exception) {
                TrackSortOrder.ALPHABETICAL
            }
        }
        set(value) = prefs.edit().putString(KEY_TRACK_SORT_ORDER, value.name).apply()

    var crossfadeSeconds: Int
        get() = prefs.getInt(KEY_CROSSFADE_SECONDS, 2)
        set(value) = prefs.edit().putInt(KEY_CROSSFADE_SECONDS, value.coerceIn(0, 10)).apply()

    var isGaplessEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAPLESS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GAPLESS_ENABLED, value).apply()

    var isReplayGainEnabled: Boolean
        get() = prefs.getBoolean(KEY_REPLAYGAIN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REPLAYGAIN_ENABLED, value).apply()

    var isDynamicThemeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_THEME_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_THEME_ENABLED, value).apply()

    var isOledPureBlackEnabled: Boolean
        get() = prefs.getBoolean(KEY_OLED_PURE_BLACK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OLED_PURE_BLACK_ENABLED, value).apply()

    var isDynamicArtworkColorEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_ARTWORK_COLOR_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_ARTWORK_COLOR_ENABLED, value).apply()

    var appThemePreset: AppThemePreset
        get() {
            val name = prefs.getString(KEY_APP_THEME_PRESET, AppThemePreset.SYSTEM_DEFAULT.name)
            return try {
                AppThemePreset.valueOf(name ?: AppThemePreset.SYSTEM_DEFAULT.name)
            } catch (e: Exception) {
                AppThemePreset.SYSTEM_DEFAULT
            }
        }
        set(value) = prefs.edit().putString(KEY_APP_THEME_PRESET, value.name).apply()

    var superBoostPercent: Int
        get() = prefs.getInt(KEY_SUPER_BOOST_PERCENT, 100)
        set(value) = prefs.edit().putInt(KEY_SUPER_BOOST_PERCENT, value.coerceIn(100, 200)).apply()

    fun getValidPlayedTrackIds(): Set<String> {
        val jsonStr = prefs.getString(KEY_PLAYED_TRACKS_TIMESTAMPS_JSON, null) ?: return emptySet()
        val validSet = mutableSetOf<String>()
        try {
            val jsonObj = JSONObject(jsonStr)
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val trackId = keys.next()
                validSet.add(trackId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return validSet
    }

    fun recordPlayedTrackId(trackId: String) {
        val jsonStr = prefs.getString(KEY_PLAYED_TRACKS_TIMESTAMPS_JSON, null) ?: "{}"
        val now = System.currentTimeMillis()
        try {
            val jsonObj = JSONObject(jsonStr)
            jsonObj.put(trackId, now)
            prefs.edit().putString(KEY_PLAYED_TRACKS_TIMESTAMPS_JSON, jsonObj.toString()).commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearPlayedTracks() {
        prefs.edit()
            .remove(KEY_PLAYED_TRACKS_TIMESTAMPS_JSON)
            .remove(KEY_COMPLETED_TRACKS_SET)
            .commit()
    }

    fun setTrackPosition(trackId: String, positionMs: Long) {
        val jsonStr = prefs.getString(KEY_TRACK_POSITIONS_JSON, null) ?: "{}"
        try {
            val jsonObj = JSONObject(jsonStr)
            jsonObj.put(trackId, positionMs)
            prefs.edit().putString(KEY_TRACK_POSITIONS_JSON, jsonObj.toString()).commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTrackPosition(trackId: String): Long {
        val jsonStr = prefs.getString(KEY_TRACK_POSITIONS_JSON, null) ?: return if (trackId == lastTrackId) lastPositionMs else 0L
        return try {
            val jsonObj = JSONObject(jsonStr)
            if (jsonObj.has(trackId)) {
                jsonObj.getLong(trackId)
            } else if (trackId == lastTrackId) {
                lastPositionMs
            } else {
                0L
            }
        } catch (e: Exception) {
            if (trackId == lastTrackId) lastPositionMs else 0L
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
