package dev.jausc.myflix.core.common.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val TAG = "TrailerStreamService"

/**
 * Response from the ExtrasDownloader plugin's StreamUrl endpoint.
 */
@Serializable
data class StreamUrlResponse(
    @SerialName("streamUrl") val streamUrl: String? = null,
    @SerialName("extractedAt") val extractedAt: String? = null,
    @SerialName("error") val error: String? = null,
)

/**
 * Response from the ExtrasDownloader plugin's Videos endpoint.
 */
@Serializable
data class VideosResponse(
    @SerialName("tmdbId") val tmdbId: Int,
    @SerialName("videos") val videos: List<VideoInfo> = emptyList(),
)

/**
 * Video information from TMDB via ExtrasDownloader plugin.
 */
@Serializable
data class VideoInfo(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("official") val official: Boolean = false,
    @SerialName("publishedAt") val publishedAt: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("size") val size: Int = 0,
)

/**
 * Cached stream URL with expiry tracking.
 */
data class CachedStreamUrl(
    val streamUrl: String,
    val fetchedAt: Long,
    val expiresAt: Long,
)

/**
 * Service for fetching and caching YouTube trailer stream URLs via the Jellyfin ExtrasDownloader plugin.
 *
 * Uses yt-dlp on the server side for reliable extraction with POT (Proof of Origin Token) support,
 * which handles YouTube's bot detection better than client-side solutions.
 *
 * Features:
 * - In-memory cache with 30-minute TTL (conservative; actual URLs last 4-6 hours)
 * - Prefetch support for instant playback when user clicks play
 * - Graceful fallback when server is unreachable
 */
object TrailerStreamService {
    // Cache TTL: 30 minutes (conservative estimate; YouTube URLs typically last 4-6 hours)
    private const val CACHE_TTL_MS = 30 * 60 * 1000L

    private val cache = mutableMapOf<String, CachedStreamUrl>()
    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // yt-dlp can take time
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Server configuration - set by the app when connecting to Jellyfin
    private var serverUrl: String? = null

    /**
     * Configure the service with the Jellyfin server URL.
     * Call this when the user logs in.
     */
    fun configure(serverUrl: String) {
        this.serverUrl = serverUrl.trimEnd('/')
        Log.d(TAG, "Configured with server: ${this.serverUrl}")
    }

    /**
     * Prefetch a trailer URL in the background.
     * Call this when a detail screen loads to have the URL ready when user clicks play.
     *
     * @param videoKey The YouTube video key (e.g., "dQw4w9WgXcQ")
     */
    suspend fun prefetch(videoKey: String) {
        Log.d(TAG, "Prefetching stream URL for videoKey=$videoKey")

        // Check cache first
        mutex.withLock {
            val cached = cache[videoKey]
            if (cached != null && !isExpired(cached)) {
                Log.d(TAG, "Prefetch skipped - already cached for videoKey=$videoKey")
                return
            }
        }

        // Fetch in background
        try {
            val url = fetchStreamUrl(videoKey)
            if (url != null) {
                Log.d(TAG, "Prefetch successful for videoKey=$videoKey")
            } else {
                Log.w(TAG, "Prefetch failed for videoKey=$videoKey - no URL returned")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Prefetch failed for videoKey=$videoKey: ${e.message}")
        }
    }

    /**
     * Get the stream URL for a YouTube video.
     * Returns cached URL if valid, otherwise fetches fresh from the server.
     *
     * @param videoKey The YouTube video key (e.g., "dQw4w9WgXcQ")
     * @return The direct stream URL, or null if extraction failed
     */
    suspend fun getStreamUrl(videoKey: String): String? {
        Log.d(TAG, "Getting stream URL for videoKey=$videoKey")

        // Check cache first
        val cached = getCachedUrl(videoKey)
        if (cached != null) {
            Log.d(TAG, "Cache hit for videoKey=$videoKey")
            return cached
        }

        // Fetch fresh
        Log.d(TAG, "Cache miss for videoKey=$videoKey, fetching from server")
        return fetchStreamUrl(videoKey)
    }

    /**
     * Get cached URL if still valid, without fetching.
     *
     * @param videoKey The YouTube video key
     * @return The cached stream URL, or null if not cached or expired
     */
    fun getCachedUrl(videoKey: String): String? {
        val cached = cache[videoKey]
        return if (cached != null && !isExpired(cached)) {
            cached.streamUrl
        } else {
            null
        }
    }

    /**
     * Clear all cached URLs.
     */
    fun clearCache() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Get videos for a movie or TV show from TMDB via ExtrasDownloader.
     *
     * @param tmdbId The TMDB ID of the movie or TV show
     * @param type The item type: "movie" or "tv"
     * @param extraType Optional filter by video type: "Trailer", "Teaser", "Featurette", etc.
     * @param officialOnly If true, only return official videos
     * @return List of videos, or empty list if fetch failed
     */
    suspend fun getVideos(
        tmdbId: Int,
        type: String = "movie",
        extraType: String? = null,
        officialOnly: Boolean = false,
    ): List<VideoInfo> {
        val server = serverUrl
        if (server == null) {
            Log.e(TAG, "Server URL not configured")
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder("$server/ExtrasDownloader/Videos/$tmdbId?type=$type")
                if (!extraType.isNullOrEmpty()) {
                    urlBuilder.append("&extraType=$extraType")
                }
                if (officialOnly) {
                    urlBuilder.append("&officialOnly=true")
                }

                val url = urlBuilder.toString()
                Log.d(TAG, "Fetching videos from: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Server returned error: ${response.code}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    Log.e(TAG, "Empty response body")
                    return@withContext emptyList()
                }

                val body = json.decodeFromString<VideosResponse>(bodyString)
                Log.d(TAG, "Fetched ${body.videos.size} videos for TMDB $tmdbId")
                body.videos
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch videos for TMDB $tmdbId", e)
                emptyList()
            }
        }
    }

    /**
     * Get the best trailer for a movie or TV show.
     * Prioritizes: Official Trailer > Trailer > Teaser
     *
     * @param tmdbId The TMDB ID of the movie or TV show
     * @param type The item type: "movie" or "tv"
     * @return The best trailer video, or null if none found
     */
    suspend fun getBestTrailer(tmdbId: Int, type: String = "movie"): VideoInfo? {
        val videos = getVideos(tmdbId, type)
        if (videos.isEmpty()) {
            Log.d(TAG, "No videos found for TMDB $tmdbId")
            return null
        }

        // Find best trailer: official Trailer > non-official Trailer > Teaser
        val trailer = videos
            .filter { it.type.equals("Trailer", ignoreCase = true) }
            .maxByOrNull { if (it.official) 1 else 0 }

        if (trailer != null) {
            Log.d(TAG, "Found trailer for TMDB $tmdbId: ${trailer.name} (official=${trailer.official})")
            return trailer
        }

        // Fallback to teaser
        val teaser = videos
            .filter { it.type.equals("Teaser", ignoreCase = true) }
            .maxByOrNull { if (it.official) 1 else 0 }

        if (teaser != null) {
            Log.d(TAG, "Found teaser for TMDB $tmdbId: ${teaser.name} (official=${teaser.official})")
            return teaser
        }

        // No trailer or teaser found
        Log.d(TAG, "No trailer or teaser found for TMDB $tmdbId")
        return null
    }

    /**
     * Check if a cache entry has expired.
     */
    private fun isExpired(cached: CachedStreamUrl): Boolean {
        return System.currentTimeMillis() > cached.expiresAt
    }

    /**
     * Fetch stream URL from the ExtrasDownloader plugin.
     */
    private suspend fun fetchStreamUrl(videoKey: String): String? {
        val server = serverUrl
        if (server == null) {
            Log.e(TAG, "Server URL not configured")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "$server/ExtrasDownloader/StreamUrl?videoKey=$videoKey"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Server returned error: ${response.code}")
                    return@withContext null
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    Log.e(TAG, "Empty response body")
                    return@withContext null
                }

                val body = json.decodeFromString<StreamUrlResponse>(bodyString)

                if (body.error != null) {
                    Log.e(TAG, "Extraction error: ${body.error}")
                    return@withContext null
                }

                val streamUrl = body.streamUrl
                if (streamUrl.isNullOrBlank()) {
                    Log.e(TAG, "Empty stream URL returned")
                    return@withContext null
                }

                // Cache the result
                val now = System.currentTimeMillis()
                mutex.withLock {
                    cache[videoKey] = CachedStreamUrl(
                        streamUrl = streamUrl,
                        fetchedAt = now,
                        expiresAt = now + CACHE_TTL_MS,
                    )
                }

                Log.d(TAG, "Successfully fetched stream URL for videoKey=$videoKey")
                streamUrl
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch stream URL for videoKey=$videoKey", e)
                null
            }
        }
    }
}
