package dev.jausc.myflix.tv.dream

import android.graphics.Bitmap
import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * Represents the different states of content that can be displayed in the dream service.
 */
sealed interface DreamContent {
    /**
     * Display the MyFlix logo (shown during loading or when not signed in).
     */
    data class Logo(val message: String? = null) : DreamContent

    /**
     * Display a library item with backdrop and metadata.
     *
     * @param backdrop The backdrop image bitmap
     * @param logo Optional logo image for the item (movie/series logo)
     * @param title The item title (used if no logo available)
     * @param year Release year (optional)
     * @param rating Community rating (optional, 0-10 scale)
     * @param genres List of genre names
     * @param itemType Type of item (Movie, Series, Episode, etc.)
     */
    data class LibraryShowcase(
        val backdrop: Bitmap,
        val logo: Bitmap? = null,
        val title: String,
        val year: Int? = null,
        val rating: Float? = null,
        val genres: List<String> = emptyList(),
        val itemType: String? = null,
    ) : DreamContent

    /**
     * Display "Now Playing" info when media is actively playing.
     *
     * @param item The media item being played
     * @param positionMs Current playback position in milliseconds
     * @param durationMs Total duration in milliseconds
     * @param isPaused Whether playback is paused
     * @param posterBitmap Poster/thumbnail image for the item
     * @param backdropBitmap Optional backdrop image for background
     */
    data class NowPlaying(
        val item: JellyfinItem,
        val positionMs: Long,
        val durationMs: Long,
        val isPaused: Boolean,
        val posterBitmap: Bitmap?,
        val backdropBitmap: Bitmap?,
    ) : DreamContent {
        val progress: Float
            get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

        val remainingMs: Long
            get() = (durationMs - positionMs).coerceAtLeast(0L)

        /** Formatted title - for episodes shows "S01E05 - Episode Name" */
        val displayTitle: String
            get() = when (item.type) {
                "Episode" -> {
                    val season = item.parentIndexNumber?.let { "S${it.toString().padStart(2, '0')}" } ?: ""
                    val episode = item.indexNumber?.let { "E${it.toString().padStart(2, '0')}" } ?: ""
                    if (season.isNotEmpty() || episode.isNotEmpty()) {
                        "$season$episode - ${item.name}"
                    } else {
                        item.name
                    }
                }
                else -> item.name
            }

        /** Subtitle - series name for episodes, year for movies */
        val displaySubtitle: String?
            get() = when (item.type) {
                "Episode" -> item.seriesName
                "Movie" -> item.productionYear?.toString()
                else -> item.productionYear?.toString()
            }
    }

    /**
     * Display an error state.
     */
    data class Error(val message: String) : DreamContent
}
