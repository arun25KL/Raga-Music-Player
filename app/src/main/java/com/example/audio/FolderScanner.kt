package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.example.model.AudioTrack
import com.example.model.FolderScanResult
import com.example.model.Subfolder
import com.example.model.SupportedAudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.ArrayDeque

sealed class ScanProgressUpdate {
    data class TracksDiscovered(
        val folderLocationId: String,
        val newTracks: List<AudioTrack>,
        val currentSubfolders: List<Subfolder>,
        val rootName: String
    ) : ScanProgressUpdate()

    data class ScanComplete(
        val result: FolderScanResult
    ) : ScanProgressUpdate()
}

object FolderScanner {

    private const val TAG = "FolderScanner"
    // Minimum audio file size: strictly greater than 5KB (5120 bytes). Filters 0B & below/equal 5KB files.
    const val MIN_FILE_SIZE_BYTES = 5120L

    /**
     * Scans a specific SAF Document Tree and emits discovered audio tracks immediately
     * in the background so the tracklist updates silently.
     */
    fun scanFolderTreeFlow(
        context: Context,
        treeUri: Uri,
        folderLocationId: String
    ): Flow<ScanProgressUpdate> = flow {
        val tracks = mutableListOf<AudioTrack>()
        val subfolderMap = mutableMapOf<String, SubfolderBuilder>()
        var rootFolderName = "Music Folder"
        var rootDocId = ""

        try {
            val contentResolver = context.contentResolver
            rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

            // Query root folder display name
            try {
                contentResolver.query(
                    parentUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        rootFolderName = cursor.getString(0) ?: "Music Folder"
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading root folder name: ${e.message}")
            }

            subfolderMap[rootDocId] = SubfolderBuilder(
                id = "${folderLocationId}_$rootDocId",
                name = rootFolderName,
                relativePath = rootFolderName,
                isRoot = true,
                folderLocationId = folderLocationId
            )

            data class FolderNode(val docId: String, val folderName: String, val relativePath: String, val isRoot: Boolean)
            val folderQueue = ArrayDeque<FolderNode>()
            val visitedDocIds = mutableSetOf<String>()

            folderQueue.add(FolderNode(rootDocId, rootFolderName, rootFolderName, true))
            visitedDocIds.add(rootDocId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            val batchToEmit = mutableListOf<AudioTrack>()

            while (folderQueue.isNotEmpty()) {
                val currentFolder = folderQueue.removeFirst()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentFolder.docId)

                try {
                    contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                        val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                        while (cursor.moveToNext()) {
                            val childDocId = cursor.getString(idCol) ?: continue
                            val displayName = cursor.getString(nameCol) ?: continue
                            val mimeType = cursor.getString(mimeCol) ?: ""
                            val sizeBytes = cursor.getLong(sizeCol)
                            val lastModified = if (modCol >= 0) cursor.getLong(modCol) else 0L

                            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                                if (!visitedDocIds.contains(childDocId)) {
                                    visitedDocIds.add(childDocId)
                                    val childRelativePath = "${currentFolder.relativePath}/$displayName"
                                    val subId = "${folderLocationId}_$childDocId"

                                    subfolderMap[childDocId] = SubfolderBuilder(
                                        id = subId,
                                        name = displayName,
                                        relativePath = childRelativePath,
                                        isRoot = false,
                                        folderLocationId = folderLocationId
                                    )

                                    folderQueue.add(FolderNode(childDocId, displayName, childRelativePath, false))
                                }
                            } else {
                                // Exclude all 0B and <= 5KB files (junk, corrupted, or placeholder files)
                                if (sizeBytes <= MIN_FILE_SIZE_BYTES) {
                                    continue
                                }

                                val detectedFormat = SupportedAudioFormat.fromFileNameOrMime(displayName, mimeType)

                                if (detectedFormat != null) {
                                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                                    val sizeFormatted = formatFileSize(sizeBytes)
                                    val titleArtist = parseTitleAndArtist(displayName)

                                    val folderBuilder = subfolderMap[currentFolder.docId]
                                    if (folderBuilder != null) {
                                        folderBuilder.trackCount++
                                    }

                                    val subId = "${folderLocationId}_${currentFolder.docId}"

                                    val track = AudioTrack(
                                        id = "${folderLocationId}_$childDocId",
                                        title = titleArtist.first,
                                        artist = titleArtist.second,
                                        album = currentFolder.folderName,
                                        durationMs = 0L,
                                        uri = fileUri,
                                        fileExtension = detectedFormat.extension,
                                        format = detectedFormat,
                                        sizeBytes = sizeBytes,
                                        sizeFormatted = sizeFormatted,
                                        dateModifiedMs = if (lastModified > 0) lastModified else System.currentTimeMillis(),
                                        fileName = displayName,
                                        folderLocationId = folderLocationId,
                                        subfolderId = subId,
                                        subfolderName = if (currentFolder.isRoot) "$rootFolderName (Root)" else currentFolder.folderName,
                                        relativePath = "${currentFolder.relativePath}/$displayName"
                                    )

                                    tracks.add(track)
                                    batchToEmit.add(track)

                                    if (batchToEmit.size >= 4) {
                                        val currentSubfolders = subfolderMap.values.map {
                                            Subfolder(it.id, it.name, it.relativePath, it.trackCount, it.isRoot, it.folderLocationId)
                                        }.sortedWith(compareBy({ !it.isRoot }, { it.name }))

                                        emit(
                                            ScanProgressUpdate.TracksDiscovered(
                                                folderLocationId = folderLocationId,
                                                newTracks = batchToEmit.toList(),
                                                currentSubfolders = currentSubfolders,
                                                rootName = rootFolderName
                                            )
                                        )
                                        batchToEmit.clear()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading children: ${e.message}")
                }
            }

            if (batchToEmit.isNotEmpty()) {
                val currentSubfolders = subfolderMap.values.map {
                    Subfolder(it.id, it.name, it.relativePath, it.trackCount, it.isRoot, it.folderLocationId)
                }.sortedWith(compareBy({ !it.isRoot }, { it.name }))

                emit(
                    ScanProgressUpdate.TracksDiscovered(
                        folderLocationId = folderLocationId,
                        newTracks = batchToEmit.toList(),
                        currentSubfolders = currentSubfolders,
                        rootName = rootFolderName
                    )
                )
                batchToEmit.clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanFolderTreeFlow", e)
        }

        val subfolders = subfolderMap.values.map {
            Subfolder(it.id, it.name, it.relativePath, it.trackCount, it.isRoot, it.folderLocationId)
        }.sortedWith(compareBy({ !it.isRoot }, { it.name }))

        emit(
            ScanProgressUpdate.ScanComplete(
                FolderScanResult(
                    folderLocationId = folderLocationId,
                    rootName = rootFolderName,
                    rootDocId = rootDocId,
                    subfolders = subfolders,
                    allTracks = tracks
                )
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Fast MediaStore scan that streams discovered tracks in background.
     */
    fun scanDeviceAudioFlow(
        context: Context,
        folderLocationId: String = "device_music_storage"
    ): Flow<ScanProgressUpdate> = flow {
        val tracks = mutableListOf<AudioTrack>()
        val subfolderMap = mutableMapOf<String, SubfolderBuilder>()
        val contentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val batchToEmit = mutableListOf<AudioTrack>()

        try {
            contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val modCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val addedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol) ?: "Track $id"
                    val title = cursor.getString(titleCol) ?: displayName
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Device Audio"
                    val durationMs = cursor.getLong(durationCol)
                    val dataPath = cursor.getString(dataCol) ?: ""
                    val sizeBytes = cursor.getLong(sizeCol)
                    val mimeType = cursor.getString(mimeCol) ?: ""
                    val dateModifiedSec = if (modCol >= 0) cursor.getLong(modCol) else 0L
                    val dateAddedSec = if (addedCol >= 0) cursor.getLong(addedCol) else 0L

                    // Exclude all 0B and <= 5KB files (junk, corrupt, empty placeholders)
                    if (sizeBytes <= MIN_FILE_SIZE_BYTES) {
                        continue
                    }

                    val detectedFormat = SupportedAudioFormat.fromFileNameOrMime(displayName, mimeType)

                    if (detectedFormat != null || (!displayName.contains(".") && durationMs > 0)) {
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val finalFormat = detectedFormat ?: SupportedAudioFormat.MP3

                        val parentDirName = if (dataPath.isNotBlank()) {
                            try {
                                val parentFile = File(dataPath).parentFile
                                parentFile?.name ?: "Music"
                            } catch (e: Exception) {
                                "Music"
                            }
                        } else {
                            album.ifBlank { "Music" }
                        }

                        val folderId = "${folderLocationId}_${parentDirName.hashCode()}"
                        val folderBuilder = subfolderMap.getOrPut(folderId) {
                            SubfolderBuilder(
                                id = folderId,
                                name = parentDirName,
                                relativePath = parentDirName,
                                isRoot = false,
                                folderLocationId = folderLocationId
                            )
                        }
                        folderBuilder.trackCount++

                        val finalTitle = if (title.isNotBlank() && title != "<unknown>") {
                            title
                        } else {
                            parseTitleAndArtist(displayName).first
                        }

                        val finalArtist = if (artist.isNotBlank() && artist != "<unknown>") {
                            artist
                        } else {
                            parseTitleAndArtist(displayName).second
                        }

                        val finalDateModifiedMs = when {
                            dateModifiedSec > 0 -> dateModifiedSec * 1000L
                            dateAddedSec > 0 -> dateAddedSec * 1000L
                            else -> System.currentTimeMillis()
                        }

                        val track = AudioTrack(
                            id = "${folderLocationId}_$id",
                            title = finalTitle,
                            artist = finalArtist,
                            album = if (album.isNotBlank() && album != "<unknown>") album else parentDirName,
                            durationMs = if (durationMs > 0) durationMs else 0L,
                            uri = contentUri,
                            fileExtension = finalFormat.extension,
                            format = finalFormat,
                            sizeBytes = sizeBytes,
                            sizeFormatted = formatFileSize(sizeBytes),
                            dateModifiedMs = finalDateModifiedMs,
                            fileName = displayName,
                            folderLocationId = folderLocationId,
                            subfolderId = folderId,
                            subfolderName = parentDirName,
                            relativePath = dataPath.ifBlank { "$parentDirName/$displayName" }
                        )

                        tracks.add(track)
                        batchToEmit.add(track)

                        if (batchToEmit.size >= 4) {
                            val currentSubs = subfolderMap.values.map {
                                Subfolder(it.id, it.name, it.relativePath, it.trackCount, false, it.folderLocationId)
                            }.sortedBy { it.name }

                            emit(
                                ScanProgressUpdate.TracksDiscovered(
                                    folderLocationId = folderLocationId,
                                    newTracks = batchToEmit.toList(),
                                    currentSubfolders = currentSubs,
                                    rootName = "Device Music Storage"
                                )
                            )
                            batchToEmit.clear()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanDeviceAudioFlow", e)
        }

        if (batchToEmit.isNotEmpty()) {
            val currentSubs = subfolderMap.values.map {
                Subfolder(it.id, it.name, it.relativePath, it.trackCount, false, it.folderLocationId)
            }.sortedBy { it.name }

            emit(
                ScanProgressUpdate.TracksDiscovered(
                    folderLocationId = folderLocationId,
                    newTracks = batchToEmit.toList(),
                    currentSubfolders = currentSubs,
                    rootName = "Device Music Storage"
                )
            )
            batchToEmit.clear()
        }

        val subfolders = subfolderMap.values.map {
            Subfolder(it.id, it.name, it.relativePath, it.trackCount, false, it.folderLocationId)
        }.sortedBy { it.name }

        emit(
            ScanProgressUpdate.ScanComplete(
                FolderScanResult(
                    folderLocationId = folderLocationId,
                    rootName = "Device Music Storage",
                    rootDocId = "device_root",
                    subfolders = subfolders,
                    allTracks = tracks
                )
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun parseTitleAndArtist(fileName: String): Pair<String, String> {
        val baseName = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        if (baseName.contains(" - ")) {
            val parts = baseName.split(" - ", limit = 2)
            val first = parts[0].trim()
            val second = parts[1].trim()
            if (first.isNotEmpty() && second.isNotEmpty()) {
                return Pair(second, first)
            }
        }
        val cleanName = baseName.replace('_', ' ').trim()
        val finalTitle = if (cleanName.isNotBlank()) cleanName else "Audio Track"
        return Pair(finalTitle, "Local Artist")
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format("%.1f MB", mb)
        } else {
            String.format("%.0f KB", kb)
        }
    }

    private class SubfolderBuilder(
        val id: String,
        val name: String,
        val relativePath: String,
        var trackCount: Int = 0,
        val isRoot: Boolean = false,
        val folderLocationId: String = ""
    )
}
