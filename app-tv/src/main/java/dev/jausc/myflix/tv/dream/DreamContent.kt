package dev.jausc.myflix.tv.dream

import android.graphics.Bitmap

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
     * Display an error state.
     */
    data class Error(val message: String) : DreamContent
}
