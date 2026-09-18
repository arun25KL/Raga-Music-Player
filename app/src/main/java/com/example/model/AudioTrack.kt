package com.example.model

import android.net.Uri

enum class SupportedAudioFormat(
    val extension: String,
    val displayName: String,
    val description: String,
    val mimeTypes: List<String>,
    val fileExtensions: List<String>
) {
    MP3("mp3", "MP3", "MPEG Layer 3", listOf("audio/mpeg", "audio/mp3"), listOf("mp3")),
    M4A("m4a", "M4A", "MPEG-4 Audio", listOf("audio/mp4", "audio/m4a", "audio/x-m4a"), listOf("m4a")),
    FLAC("flac", "FLAC", "Free Lossless Audio", listOf("audio/flac", "audio/x-flac"), listOf("flac")),
    ALAC("alac", "ALAC", "Apple Lossless", listOf("audio/alac", "audio/x-alac", "audio/caf"), listOf("alac", "caf")),
    WAV("wav", "WAV", "Waveform Audio", listOf("audio/wav", "audio/x-wav", "audio/wave"), listOf("wav")),
    AIFF("aiff", "AIFF", "Audio Interchange", listOf("audio/aiff", "audio/x-aiff"), listOf("aiff", "aif")),
    AAC("aac", "AAC", "Advanced Audio", listOf("audio/aac", "audio/aacp"), listOf("aac")),
    OGG("ogg", "OGG", "Ogg Vorbis", listOf("audio/ogg", "application/ogg"), listOf("ogg", "oga")),
    OPUS("opus", "OPUS", "Opus Audio", listOf("audio/opus", "audio/ogg"), listOf("opus")),
    WMA("wma", "WMA", "Windows Media", listOf("audio/x-ms-wma"), listOf("wma")),
    MIDI("mid", "MIDI", "MIDI Musical", listOf("audio/midi", "audio/mid", "audio/x-midi"), listOf("mid", "midi"));

    companion object {
        val ALL_FORMAT_NAMES: Set<String> = entries.map { it.name }.toSet()

        fun fromFileNameOrMime(fileName: String, mimeType: String = ""): SupportedAudioFormat? {
            val lowerName = fileName.lowercase()
            val ext = lowerName.substringAfterLast('.', "")
            if (ext.isNotEmpty()) {
                val match = entries.firstOrNull { it.fileExtensions.contains(ext) }
                if (match != null) return match
            }

            val lowerMime = mimeType.lowercase()
            if (lowerMime.isNotEmpty()) {
                val match = entries.firstOrNull { format ->
                    format.mimeTypes.any { lowerMime.contains(it) }
                }
                if (match != null) return match
            }

            return null
        }
    }
}

enum class TrackSortOrder(val displayName: String, val shortLabel: String) {
    ALPHABETICAL("A → Z", "A-Z"),
    NEWEST_FIRST("New → Old", "Newest"),
    OLDEST_FIRST("Old → New", "Oldest"),
    FOLDER_WISE("Folder wise", "Folder")
}

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Local Audio",
    val durationMs: Long = 0L,
    val uri: Uri,
    val fileExtension: String = "mp3",
    val format: SupportedAudioFormat = SupportedAudioFormat.MP3,
    val sizeBytes: Long = 0L,
    val sizeFormatted: String = "",
    val dateModifiedMs: Long = 0L,
    val fileName: String = "",
    val folderLocationId: String = "",
    val subfolderId: String = "root",
    val subfolderName: String = "Root Folder",
    val relativePath: String = "",
    val bitrateKbps: Int = 0,
    val albumArtist: String = "",
    val composer: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val sampleRateHz: Int = 0
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val calculatedBitrateKbps: Int
        get() {
            if (bitrateKbps > 0) return bitrateKbps
            if (durationMs <= 0 || sizeBytes <= 0) return 0
            val kbps = ((sizeBytes * 8) / durationMs).toInt()
            return kbps
        }

    val bitrateFormatted: String
        get() {
            val kbps = calculatedBitrateKbps
            return if (kbps > 0) "$kbps kbps" else "Standard"
        }

    val sampleRateFormatted: String
        get() {
            return when {
                sampleRateHz >= 96000 -> "${sampleRateHz / 1000} kHz Hi-Res"
                sampleRateHz >= 44100 -> "${String.format("%.1f", sampleRateHz / 1000f)} kHz"
                sampleRateHz > 0 -> "${sampleRateHz} Hz"
                format == SupportedAudioFormat.FLAC || format == SupportedAudioFormat.WAV -> "Lossless 48kHz"
                else -> "44.1 kHz"
            }
        }

    val qualityBadgeFormatted: String
        get() {
            val fmt = format.displayName
            val br = if (calculatedBitrateKbps > 0) "${calculatedBitrateKbps}k" else ""
            val sr = if (sampleRateHz > 0) "${sampleRateHz / 1000}kHz" else if (format == SupportedAudioFormat.FLAC || format == SupportedAudioFormat.WAV) "Lossless" else "HD"
            return if (br.isNotEmpty()) "$fmt • $br • $sr" else "$fmt • $sr"
        }
}

data class FolderLocation(
    val id: String,
    val name: String,
    val uriString: String,
    val isDeviceStorage: Boolean = false,
    val trackCount: Int = 0,
    val dateAddedMs: Long = System.currentTimeMillis()
)

data class Subfolder(
    val id: String,
    val name: String,
    val relativePath: String,
    val trackCount: Int,
    val isRoot: Boolean = false,
    val folderLocationId: String = ""
)

data class FolderScanResult(
    val folderLocationId: String,
    val rootName: String,
    val rootDocId: String,
    val subfolders: List<Subfolder>,
    val allTracks: List<AudioTrack>
)

enum class RepeatMode {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE
}

enum class AudioSourceType {
    FOLDER,
    DEVICE
}
