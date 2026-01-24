package dev.jausc.myflix.tv.ui.theme

import androidx.compose.ui.graphics.Color
import dev.jausc.myflix.core.common.ui.IconColors as CoreIconColors

/**
 * TV-specific icon colors extending the shared core colors.
 */
object IconColors {
    // Re-export shared colors from core
    val Play = CoreIconColors.Play
    val Resume = CoreIconColors.Resume
    val Restart = CoreIconColors.Restart
    val Shuffle = CoreIconColors.Shuffle
    val Watched = CoreIconColors.Watched
    val Favorite = CoreIconColors.Favorite
    val FavoriteFilled = CoreIconColors.FavoriteFilled
    val More = CoreIconColors.More
    val Trailer = CoreIconColors.Trailer
    val MediaInfo = CoreIconColors.MediaInfo
    val Navigation = CoreIconColors.Navigation
    val Playlist = CoreIconColors.Playlist
    val GoToSeries = CoreIconColors.GoToSeries

    // TV-specific colors
    val Info = Color(0xFF60A5FA) // Light Blue (for general info buttons)

    // Navigation rail colors
    val NavHome = Color(0xFF60A5FA) // Blue
    val NavSearch = Color(0xFFA78BFA) // Purple
    val NavShows = Color(0xFF34D399) // Green
    val NavMovies = Color(0xFFFBBF24) // Yellow/Gold
    val NavDiscover = Color(0xFF8B5CF6) // Violet
    val NavCollections = Color(0xFFFF7043) // Orange
    val NavUniverses = Color(0xFF9575CD) // Deep Purple
    val NavSettings = Color(0xFFF472B6) // Pink
}
