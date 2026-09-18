package com.example.audio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AlbumArtSearchResult(
    val id: String,
    val trackTitle: String,
    val artistName: String,
    val albumName: String,
    val previewArtworkUrl: String,
    val highResArtworkUrl: String,
    val source: String = "iTunes", // JioSaavn, Gaana, YouTube, Google, iTunes, Deezer
    val year: String? = null,
    val genre: String? = null
)

enum class ArtSearchProvider(val displayName: String, val badgeColorHex: Long) {
    ALL("All Sources", 0xFFE040FB),
    JIOSAAVN("JioSaavn", 0xFF00D26A),
    GAANA("Gaana", 0xFFE72C30),
    YOUTUBE("YouTube Music", 0xFFFF0000),
    GOOGLE("Google / Web", 0xFF4285F4),
    ITUNES("iTunes / Apple", 0xFFFC3C44),
    DEEZER("Deezer", 0xFFFF0055)
}

object AlbumArtSearchService {
    private const val TAG = "AlbumArtSearchService"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Searches for track album artwork across Indian and Global music services:
     * JioSaavn, Gaana, YouTube / YouTube Music, Google / Web, iTunes (India & Global), and Deezer.
     */
    suspend fun searchArtwork(
        query: String,
        provider: ArtSearchProvider = ArtSearchProvider.ALL
    ): List<AlbumArtSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val results = mutableListOf<AlbumArtSearchResult>()

        try {
            when (provider) {
                ArtSearchProvider.ALL -> {
                    // Search in parallel across Indian and Global providers
                    val jioSaavnDeferred = async { searchJioSaavn(trimmed) }
                    val gaanaDeferred = async { searchGaana(trimmed) }
                    val youtubeDeferred = async { searchYouTube(trimmed) }
                    val itunesInDeferred = async { searchITunes(trimmed, country = "IN") }
                    val googleDeferred = async { searchGoogleImages(trimmed) }
                    val deezerDeferred = async { searchDeezer(trimmed) }

                    val allLists = awaitAll(
                        jioSaavnDeferred,
                        gaanaDeferred,
                        youtubeDeferred,
                        itunesInDeferred,
                        googleDeferred,
                        deezerDeferred
                    )

                    // Interleave results so the user gets a rich mix from JioSaavn, Gaana, YouTube, iTunes, etc.
                    val maxLen = allLists.maxOfOrNull { it.size } ?: 0
                    val seenUrls = mutableSetOf<String>()

                    for (i in 0 until maxLen) {
                        for (list in allLists) {
                            if (i < list.size) {
                                val item = list[i]
                                if (item.previewArtworkUrl.isNotBlank() && seenUrls.add(item.highResArtworkUrl)) {
                                    results.add(item)
                                }
                            }
                        }
                    }
                }
                ArtSearchProvider.JIOSAAVN -> {
                    results.addAll(searchJioSaavn(trimmed))
                }
                ArtSearchProvider.GAANA -> {
                    results.addAll(searchGaana(trimmed))
                }
                ArtSearchProvider.YOUTUBE -> {
                    results.addAll(searchYouTube(trimmed))
                }
                ArtSearchProvider.GOOGLE -> {
                    results.addAll(searchGoogleImages(trimmed))
                }
                ArtSearchProvider.ITUNES -> {
                    val inResults = searchITunes(trimmed, country = "IN")
                    results.addAll(inResults)
                    if (results.size < 8) {
                        val globalResults = searchITunes(trimmed, country = "US")
                        for (r in globalResults) {
                            if (results.none { it.highResArtworkUrl == r.highResArtworkUrl }) {
                                results.add(r)
                            }
                        }
                    }
                }
                ArtSearchProvider.DEEZER -> {
                    results.addAll(searchDeezer(trimmed))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for '$query'", e)
        }

        results
    }

    // =========================================================================
    // 1. JIOSAAVN API (Indian Hindi, Tamil, Telugu, Malayalam, Punjabi, etc.)
    // =========================================================================
    private suspend fun searchJioSaavn(query: String): List<AlbumArtSearchResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlbumArtSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")

            // 1A. Saavn Autocomplete API
            val autoUrl = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&cc=in&includeMetaTags=1&query=$encoded"
            val req1 = Request.Builder()
                .url(autoUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Referer", "https://www.jiosaavn.com/")
                .build()

            client.newCall(req1).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        // Songs
                        val songsObj = json.optJSONObject("songs")
                        val songsData = songsObj?.optJSONArray("data")
                        if (songsData != null) {
                            for (i in 0 until songsData.length()) {
                                val item = songsData.optJSONObject(i) ?: continue
                                val id = item.optString("id", "$i")
                                val title = decodeHtmlEntities(item.optString("title", ""))
                                val subtitle = decodeHtmlEntities(item.optString("subtitle", ""))
                                val album = decodeHtmlEntities(item.optString("album", subtitle))
                                val rawImg = item.optString("image", "")

                                if (rawImg.isNotBlank()) {
                                    val highRes = upgradeJioSaavnImageUrl(rawImg)
                                    list.add(
                                        AlbumArtSearchResult(
                                            id = "jiosaavn_s_$id",
                                            trackTitle = title,
                                            artistName = subtitle,
                                            albumName = album,
                                            previewArtworkUrl = rawImg,
                                            highResArtworkUrl = highRes,
                                            source = "JioSaavn"
                                        )
                                    )
                                }
                            }
                        }

                        // Albums
                        val albumsObj = json.optJSONObject("albums")
                        val albumsData = albumsObj?.optJSONArray("data")
                        if (albumsData != null) {
                            for (i in 0 until albumsData.length()) {
                                val item = albumsData.optJSONObject(i) ?: continue
                                val id = item.optString("id", "$i")
                                val title = decodeHtmlEntities(item.optString("title", ""))
                                val music = decodeHtmlEntities(item.optString("music", item.optString("subtitle", "")))
                                val rawImg = item.optString("image", "")

                                if (rawImg.isNotBlank()) {
                                    val highRes = upgradeJioSaavnImageUrl(rawImg)
                                    list.add(
                                        AlbumArtSearchResult(
                                            id = "jiosaavn_a_$id",
                                            trackTitle = title,
                                            artistName = music.ifBlank { "Indian Music" },
                                            albumName = title,
                                            previewArtworkUrl = rawImg,
                                            highResArtworkUrl = highRes,
                                            source = "JioSaavn"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1B. Saavn Search Results API if needed
            if (list.size < 6) {
                val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&p=1&n=12&q=$encoded"
                val req2 = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Referer", "https://www.jiosaavn.com/")
                    .build()

                client.newCall(req2).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val results = json.optJSONArray("results")
                            if (results != null) {
                                for (i in 0 until results.length()) {
                                    val item = results.optJSONObject(i) ?: continue
                                    val id = item.optString("id", "$i")
                                    val song = decodeHtmlEntities(item.optString("song", item.optString("title", "")))
                                    val singers = decodeHtmlEntities(item.optString("singers", item.optString("primary_artists", "")))
                                    val album = decodeHtmlEntities(item.optString("album", ""))
                                    val year = item.optString("year", null)
                                    val rawImg = item.optString("image", "")

                                    if (rawImg.isNotBlank() && list.none { it.trackTitle.equals(song, ignoreCase = true) }) {
                                        val highRes = upgradeJioSaavnImageUrl(rawImg)
                                        list.add(
                                            AlbumArtSearchResult(
                                                id = "jiosaavn_res_$id",
                                                trackTitle = song,
                                                artistName = singers.ifBlank { "Indian Artists" },
                                                albumName = album.ifBlank { song },
                                                previewArtworkUrl = rawImg,
                                                highResArtworkUrl = highRes,
                                                source = "JioSaavn",
                                                year = year
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "JioSaavn search exception: ${e.message}")
        }
        list
    }

    private fun upgradeJioSaavnImageUrl(url: String): String {
        return url.replace("50x50.jpg", "500x500.jpg")
            .replace("150x150.jpg", "500x500.jpg")
            .replace("50x50.png", "500x500.png")
            .replace("150x150.png", "500x500.png")
            .replace(Regex("-\\d+x\\d+\\."), "-500x500.")
            .replace("http://", "https://")
    }

    // =========================================================================
    // 2. GAANA API (Indian Bollywood, Punjabi, Regional & Indipop)
    // =========================================================================
    private suspend fun searchGaana(query: String): List<AlbumArtSearchResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlbumArtSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.gaana.com/index.php?type=search&subtype=search_song&key=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Referer", "https://gaana.com/")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val tracks = json.optJSONArray("tracks")
                        if (tracks != null) {
                            for (i in 0 until tracks.length()) {
                                val item = tracks.optJSONObject(i) ?: continue
                                val trackId = item.optString("track_id", "$i")
                                val title = decodeHtmlEntities(item.optString("track_title", ""))
                                val album = decodeHtmlEntities(item.optString("album_title", ""))
                                
                                val artistArr = item.optJSONArray("artist")
                                val artists = if (artistArr != null && artistArr.length() > 0) {
                                    val names = mutableListOf<String>()
                                    for (a in 0 until artistArr.length()) {
                                        val aObj = artistArr.optJSONObject(a)
                                        val name = aObj?.optString("name", "")
                                        if (!name.isNullOrBlank()) names.add(name)
                                    }
                                    names.joinToString(", ")
                                } else {
                                    item.optString("primary_artist", "Gaana Music")
                                }

                                val artwork = item.optString("artwork", "")
                                val artworkLarge = item.optString("artwork_large", artwork)

                                val finalArt = artworkLarge.ifBlank { artwork }
                                if (finalArt.isNotBlank()) {
                                    val highRes = finalArt
                                        .replace("50x50", "480x480")
                                        .replace("80x80", "480x480")
                                        .replace("175x175", "480x480")
                                        .replace("http://", "https://")

                                    list.add(
                                        AlbumArtSearchResult(
                                            id = "gaana_$trackId",
                                            trackTitle = title,
                                            artistName = artists,
                                            albumName = album.ifBlank { title },
                                            previewArtworkUrl = finalArt,
                                            highResArtworkUrl = highRes,
                                            source = "Gaana"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gaana search exception: ${e.message}")
        }
        list
    }

    // =========================================================================
    // 3. YOUTUBE & YOUTUBE MUSIC (Official Music Videos & Tracks)
    // =========================================================================
    private suspend fun searchYouTube(query: String): List<AlbumArtSearchResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlbumArtSearchResult>()
        try {
            val encoded = URLEncoder.encode("$query official music", "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    
                    // Regex patterns to extract videoId and title from YouTube search HTML payload
                    val videoPattern = Pattern.compile("(?s)\"videoRenderer\":\\{\"videoId\":\"([a-zA-Z0-9_-]{11})\".*?\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"\\}\\]")
                    val matcher = videoPattern.matcher(html)
                    val seenVideos = mutableSetOf<String>()

                    while (matcher.find() && list.size < 12) {
                        val videoId = matcher.group(1) ?: continue
                        val rawTitle = matcher.group(2) ?: ""
                        val title = decodeHtmlEntities(rawTitle.replace("\\u0026", "&").replace("\\\"", "\""))

                        if (seenVideos.add(videoId) && !title.contains("shorts", ignoreCase = true)) {
                            val preview = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
                            val highRes = "https://i.ytimg.com/vi/$videoId/hq720.jpg"

                            // Clean artist & title heuristic
                            val parts = title.split("-", "|", "•", "–")
                            val cleanTitle = if (parts.size >= 2) parts[1].trim() else title
                            val cleanArtist = if (parts.size >= 2) parts[0].trim() else "YouTube Music"

                            list.add(
                                AlbumArtSearchResult(
                                    id = "yt_$videoId",
                                    trackTitle = cleanTitle,
                                    artistName = cleanArtist,
                                    albumName = "YouTube Music Video",
                                    previewArtworkUrl = preview,
                                    highResArtworkUrl = highRes,
                                    source = "YouTube Music"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube search exception: ${e.message}")
        }
        list
    }

    // =========================================================================
    // 4. GOOGLE & DUCKDUCKGO WEB IMAGES (Web-wide High-Res Cover Art)
    // =========================================================================
    private suspend fun searchGoogleImages(query: String): List<AlbumArtSearchResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlbumArtSearchResult>()
        try {
            // Use DuckDuckGo Image Tokenless Query for high resolution album artwork
            val encoded = URLEncoder.encode("$query album art cover", "UTF-8")
            val tokenUrl = "https://duckduckgo.com/?q=$encoded&iax=images&ia=images"
            val tokenReq = Request.Builder()
                .url(tokenUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()

            var vqd = ""
            client.newCall(tokenReq).execute().use { res ->
                val body = res.body?.string() ?: ""
                val vqdMatcher = Pattern.compile("vqd=([0-9-]+)&|vqd=([0-9-]+)\"|vqd=\"([0-9-]+)\"").matcher(body)
                if (vqdMatcher.find()) {
                    vqd = vqdMatcher.group(1) ?: vqdMatcher.group(2) ?: vqdMatcher.group(3) ?: ""
                }
            }

            if (vqd.isNotBlank()) {
                val imgApiUrl = "https://duckduckgo.com/i.js?q=$encoded&o=json&vqd=$vqd&p=1"
                val imgReq = Request.Builder()
                    .url(imgApiUrl)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Referer", "https://duckduckgo.com/")
                    .build()

                client.newCall(imgReq).execute().use { res ->
                    if (res.isSuccessful) {
                        val jsonStr = res.body?.string()
                        if (!jsonStr.isNullOrBlank()) {
                            val json = JSONObject(jsonStr)
                            val results = json.optJSONArray("results")
                            if (results != null) {
                                for (i in 0 until minOf(results.length(), 10)) {
                                    val item = results.optJSONObject(i) ?: continue
                                    val imgUrl = item.optString("image", "")
                                    val thumbUrl = item.optString("thumbnail", imgUrl)
                                    val title = decodeHtmlEntities(item.optString("title", query))
                                    val sourceSite = item.optString("source", "Web")

                                    if (imgUrl.isNotBlank()) {
                                        list.add(
                                            AlbumArtSearchResult(
                                                id = "web_img_$i",
                                                trackTitle = title,
                                                artistName = sourceSite,
                                                albumName = "$query (Web Art)",
                                                previewArtworkUrl = thumbUrl,
                                                highResArtworkUrl = imgUrl,
                                                source = "Google / Web"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google/Web image search exception: ${e.message}")
        }
        list
    }

    // =========================================================================
    // 5. ITUNES & APPLE MUSIC API (Supports Indian Store "IN" & Global)
    // =========================================================================
    private suspend fun searchITunes(query: String, country: String = "IN"): List<AlbumArtSearchResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlbumArtSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&country=$country&entity=song&limit=15"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val items = json.optJSONArray("results")
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i) ?: continue
                                val trackName = item.optString("trackName", "").ifBlank { item.optString("collectionName", "") }
                                val artistName = item.optString("artistName", "")
                                val albumName = item.optString("collectionName", "")
                                val art100 = item.optString("artworkUrl100", "")

                                if (art100.isNotBlank()) {
                                    val highRes = art100
                                        .replace("100x100bb", "600x600bb")
                                        .replace("100x100", "600x600")

                                    val releaseDate = item.optString("releaseDate", "")
                                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else null
                                    val genre = item.optString("primaryGenreName", null)
                                    val trackId = item.optString("trackId", "$i")

                                    list.add(
                                        AlbumArtSearchResult(
                                            id = "itunes_${country}_$trackId",
                                            trackTitle = trackName,
                                            artistName = artistName,
                                            albumName = albumName,
                                            previewArtworkUrl = art100,
                                            highResArtworkUrl = highRes,
                                            source = if (country.equals("IN", ignoreCase = true)) "iTunes (India)" else "iTunes",
                                            year = year,
                                            genre = genre
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "iTunes ($country) search failed: ${e.message}")
        }
        list
    }

    // =========================================================================
    // 6. DEEZER API (High-Res 1000x1000 Covers)
    // =========================================================================
    private suspend fun searchDeezer(query: String): List<AlbumArtSearchResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlbumArtSearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.deezer.com/search?q=$encoded&limit=15"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val items = json.optJSONArray("data")
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i) ?: continue
                                val trackName = item.optString("title", "")
                                val artistObj = item.optJSONObject("artist")
                                val artistName = artistObj?.optString("name", "") ?: ""
                                val albumObj = item.optJSONObject("album")
                                val albumName = albumObj?.optString("title", "") ?: ""

                                val coverMedium = albumObj?.optString("cover_medium", "") ?: ""
                                val coverBig = albumObj?.optString("cover_xl", "")
                                    ?: albumObj?.optString("cover_big", "")
                                    ?: coverMedium

                                val deezerId = item.optString("id", "$i")

                                if (coverBig.isNotBlank()) {
                                    list.add(
                                        AlbumArtSearchResult(
                                            id = "deezer_$deezerId",
                                            trackTitle = trackName,
                                            artistName = artistName,
                                            albumName = albumName,
                                            previewArtworkUrl = coverMedium.ifBlank { coverBig },
                                            highResArtworkUrl = coverBig,
                                            source = "Deezer"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Deezer search failed: ${e.message}")
        }
        list
    }

    /**
     * Downloads full resolution artwork bitmap from the web with fallback for HD YouTube / CDN thumbnails.
     */
    suspend fun downloadArtwork(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null

        // Try primary URL first
        val primaryBitmap = fetchBitmapFromUrl(imageUrl)
        if (primaryBitmap != null) return@withContext primaryBitmap

        // YouTube fallback from hq720 / maxresdefault to hqdefault
        if (imageUrl.contains("i.ytimg.com")) {
            val fallbackUrl = imageUrl
                .replace("hq720.jpg", "hqdefault.jpg")
                .replace("maxresdefault.jpg", "hqdefault.jpg")
            if (fallbackUrl != imageUrl) {
                val fallbackBitmap = fetchBitmapFromUrl(fallbackUrl)
                if (fallbackBitmap != null) return@withContext fallbackBitmap
            }
        }

        null
    }

    private fun fetchBitmapFromUrl(url: String): Bitmap? {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val stream = response.body?.byteStream()
                    if (stream != null) {
                        return BitmapFactory.decodeStream(stream)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed downloading artwork from $url: ${e.message}")
        }
        return null
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
    }
}
