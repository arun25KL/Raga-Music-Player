package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.util.Size
import java.io.File
import java.io.InputStream

object AlbumArtExtractor {
    private const val TAG = "AlbumArtExtractor"
    private const val DEFAULT_COVER_SIZE = 512
    private const val THUMB_SIZE = 128

    // In-memory LRU cache for decoded album cover art (up to ~8MB)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 16).coerceIn(1024, 8192)

    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Extracts high quality album artwork for the Now Playing screen.
     */
    fun extractCoverArt(context: Context, uri: Uri, trackId: String): Bitmap? {
        val cacheKey = "full_$trackId"
        memoryCache.get(cacheKey)?.let { return it }

        // Check custom disk artwork first
        val customDiskArt = getCustomCoverArt(context, trackId)
        if (customDiskArt != null) {
            val scaled = if (customDiskArt.width > DEFAULT_COVER_SIZE || customDiskArt.height > DEFAULT_COVER_SIZE) {
                Bitmap.createScaledBitmap(customDiskArt, DEFAULT_COVER_SIZE, DEFAULT_COVER_SIZE, true)
            } else {
                customDiskArt
            }
            memoryCache.put(cacheKey, scaled)
            return scaled
        }

        val bitmap = extractBitmapInternal(context, uri, DEFAULT_COVER_SIZE)
        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    /**
     * Extracts thumbnail album artwork for the track list items.
     */
    fun extractThumbnail(context: Context, uri: Uri, trackId: String): Bitmap? {
        val cacheKey = "thumb_$trackId"
        memoryCache.get(cacheKey)?.let { return it }

        // Check custom disk artwork first
        val customDiskArt = getCustomCoverArt(context, trackId)
        if (customDiskArt != null) {
            val thumb = Bitmap.createScaledBitmap(customDiskArt, THUMB_SIZE, THUMB_SIZE, true)
            memoryCache.put(cacheKey, thumb)
            return thumb
        }

        val bitmap = extractBitmapInternal(context, uri, THUMB_SIZE)
        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    /**
     * Persistently saves a custom user-selected/downloaded artwork for a track.
     */
    fun saveCustomCoverArt(context: Context, trackId: String, bitmap: Bitmap): Boolean {
        return try {
            val dir = File(context.filesDir, "custom_artwork")
            if (!dir.exists()) dir.mkdirs()
            val safeId = trackId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val file = File(dir, "art_$safeId.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Invalidate and update in-memory caches
            val fullKey = "full_$trackId"
            val thumbKey = "thumb_$trackId"
            memoryCache.put(fullKey, bitmap)
            val thumb = Bitmap.createScaledBitmap(bitmap, THUMB_SIZE, THUMB_SIZE, true)
            memoryCache.put(thumbKey, thumb)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom artwork for track $trackId", e)
            false
        }
    }

    /**
     * Retrieves custom cover art file from app storage if present.
     */
    fun getCustomCoverArt(context: Context, trackId: String): Bitmap? {
        return try {
            val safeId = trackId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val file = File(File(context.filesDir, "custom_artwork"), "art_$safeId.png")
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasCustomCoverArt(context: Context, trackId: String): Boolean {
        val safeId = trackId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(File(context.filesDir, "custom_artwork"), "art_$safeId.png")
        return file.exists() && file.length() > 0
    }

    fun deleteCustomCoverArt(context: Context, trackId: String): Boolean {
        return try {
            val safeId = trackId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val file = File(File(context.filesDir, "custom_artwork"), "art_$safeId.png")
            if (file.exists()) file.delete()
            memoryCache.remove("full_$trackId")
            memoryCache.remove("thumb_$trackId")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun extractBitmapInternal(context: Context, uri: Uri, targetSize: Int): Bitmap? {
        // Strategy 1: Modern ContentResolver loadThumbnail (fast for MediaStore on Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val thumb = context.contentResolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
                if (thumb != null) {
                    return thumb
                }
            } catch (e: Throwable) {
                // Fall through to MediaMetadataRetriever
            }
        }

        // Strategy 2: MediaMetadataRetriever for embedded ID3/MP4/FLAC/OGG artwork
        try {
            val retriever = MediaMetadataRetriever()
            try {
                // Try setDataSource via FileDescriptor first for SAF / content Uris
                val pfd = try {
                    context.contentResolver.openFileDescriptor(uri, "r")
                } catch (e: Throwable) {
                    null
                }

                if (pfd != null) {
                    pfd.use { descriptor ->
                        retriever.setDataSource(descriptor.fileDescriptor)
                        val picture = retriever.embeddedPicture
                        if (picture != null) {
                            return decodeSampledBitmapFromByteArray(picture, targetSize)
                        }
                    }
                } else {
                    retriever.setDataSource(context, uri)
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        return decodeSampledBitmapFromByteArray(picture, targetSize)
                    }
                }
            } finally {
                try {
                    retriever.release()
                } catch (ignored: Throwable) {}
            }
        } catch (e: Throwable) {
            Log.d(TAG, "MediaMetadataRetriever failed for $uri: ${e.message}")
        }

        // Strategy 3: MediaStore Audio Album Art URI fallback
        try {
            val path = uri.toString()
            if (path.contains("audio/media/")) {
                val mediaId = uri.lastPathSegment?.toLongOrNull()
                if (mediaId != null) {
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        mediaId
                    )
                    context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                        return BitmapFactory.decodeStream(stream)
                    }
                }
            }
        } catch (e: Throwable) {
            // Album art uri not found
        }

        return null
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, targetSize: Int): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            var inSampleSize = 1
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= targetSize && (halfWidth / inSampleSize) >= targetSize) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory-safe and fast
            }
            return BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
        } catch (e: Throwable) {
            Log.e(TAG, "Error decoding artwork byte array", e)
            return null
        }
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
