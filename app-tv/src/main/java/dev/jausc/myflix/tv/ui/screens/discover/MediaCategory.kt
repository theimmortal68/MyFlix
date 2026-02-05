package dev.jausc.myflix.tv.ui.screens.discover

/**
 * Categories for Discover home screen carousel rows.
 * Each category maps to a specific Seerr API endpoint or filter.
 */
enum class MediaCategory(
    val title: String,
    val apiEndpoint: String,
) {
    RECENT_REQUESTS("Recent Requests", "request"),
    TRENDING("Trending", "trending"),
    POPULAR_MOVIES("Popular Movies", "discover/movies"),
    POPULAR_TV("Popular TV Shows", "discover/tv"),
    UPCOMING_MOVIES("Upcoming Movies", "discover/movies/upcoming"),
    UPCOMING_TV("Upcoming TV Shows", "discover/tv/upcoming"),
    MOVIE_GENRES("Movie Genres", "genres/movie"),
    TV_GENRES("TV Genres", "genres/tv"),
    STUDIOS("Studios", "studio"),
    NETWORKS("Networks", "network"),
}
