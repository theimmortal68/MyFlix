package dev.jausc.myflix.core.common.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TMDB Video model for trailers and other video content.
 * Maps to TMDB API v3 /movie/{id}/videos and /tv/{id}/videos endpoints.
 */
@Serializable
data class TmdbVideo(
    val id: String,
    @SerialName("iso_639_1")
    val languageCode: String? = null,
    @SerialName("iso_3166_1")
    val countryCode: String? = null,
    val key: String, // YouTube video ID
    val name: String,
    val site: String, // "YouTube", "Vimeo", etc.
    val size: Int, // Quality hint: 360, 480, 720, 1080
    val type: String, // "Trailer", "Teaser", "Clip", "Featurette"
    val official: Boolean = false,
    @SerialName("published_at")
    val publishedAt: String? = null,
)

@Serializable
data class TmdbVideoResponse(
    val id: Int,
    val results: List<TmdbVideo> = emptyList(),
)
