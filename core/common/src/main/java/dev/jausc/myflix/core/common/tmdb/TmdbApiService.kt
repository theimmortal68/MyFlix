package dev.jausc.myflix.core.common.tmdb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "TmdbApiService"
private const val BASE_URL = "https://api.themoviedb.org/3"

/**
 * Service for fetching trailer information from TMDB API.
 * Used as fallback when Jellyfin metadata lacks trailer information.
 */
object TmdbApiService {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Fetch videos for a movie or TV show from TMDB.
     *
     * @param tmdbId TMDB ID of the content
     * @param mediaType "movie" or "tv"
     * @param apiKey TMDB API key
     * @param language Optional ISO 639-1 language code for localized trailers
     */
    suspend fun getVideos(
        tmdbId: Int,
        mediaType: String,
        apiKey: String,
        language: String? = null,
    ): Result<List<TmdbVideo>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = buildString {
                append("$BASE_URL/$mediaType/$tmdbId/videos?api_key=$apiKey")
                if (!language.isNullOrBlank()) {
                    append("&language=$language")
                    append("&include_video_language=$language,en,null")
                } else {
                    append("&include_video_language=en,null")
                }
            }

            Log.d(TAG, "Fetching TMDB videos for $mediaType/$tmdbId")

            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val response = json.decodeFromString<TmdbVideoResponse>(responseText)
                Log.d(TAG, "Found ${response.results.size} videos for $mediaType/$tmdbId")
                Result.success(response.results)
            } else {
                Log.e(TAG, "TMDB error: ${connection.responseCode}")
                Result.failure(Exception("TMDB error: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch TMDB videos", e)
            Result.failure(e)
        }
    }

    /**
     * Select the best trailer from TMDB results.
     * Priority: Official trailer in preferred language > Official trailer > Any trailer > Teaser > Any video
     */
    fun selectBestTrailer(videos: List<TmdbVideo>, preferredLanguage: String? = null): TmdbVideo? {
        val youtubeVideos = videos.filter { it.site.equals("YouTube", ignoreCase = true) }

        return youtubeVideos.firstOrNull {
            it.type == "Trailer" && it.official && it.languageCode == preferredLanguage
        } ?: youtubeVideos.firstOrNull {
            it.type == "Trailer" && it.official
        } ?: youtubeVideos.firstOrNull {
            it.type == "Trailer"
        } ?: youtubeVideos.firstOrNull {
            it.type == "Teaser"
        } ?: youtubeVideos.firstOrNull()
    }

    /**
     * Verify a TMDB API key is valid.
     */
    suspend fun verifyApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/configuration?api_key=${apiKey.trim()}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000

            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Result.success(Unit)
                HttpURLConnection.HTTP_UNAUTHORIZED -> Result.failure(Exception("Invalid API key"))
                else -> Result.failure(Exception("Error: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
