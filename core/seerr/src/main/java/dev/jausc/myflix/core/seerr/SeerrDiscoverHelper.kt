package dev.jausc.myflix.core.seerr

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Helper for loading Seerr discover content.
 * Shared between TV and Mobile home screens.
 */
object SeerrDiscoverHelper {

    /**
     * Load discover rows based on Seerr slider settings.
     *
     * @param seerrClient The Seerr API client
     * @param sliders List of discover sliders from settings
     * @param filterDiscoverable Filter function to exclude already available/requested items
     * @return List of discover rows with content
     */
    suspend fun loadDiscoverRows(
        seerrClient: SeerrClient,
        sliders: List<SeerrDiscoverSlider>,
        filterDiscoverable: List<SeerrMedia>.() -> List<SeerrMedia>,
    ): List<SeerrDiscoverRow> {
        val rows = mutableListOf<SeerrDiscoverRow>()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        for (slider in sliders) {
            val (title, colorValue) = discoverTitleAndColor(slider)
            val items = when (slider.type) {
                SeerrDiscoverSliderType.TRENDING ->
                    seerrClient.getTrending().map { it.results }.getOrDefault(emptyList())
                SeerrDiscoverSliderType.POPULAR_MOVIES ->
                    seerrClient.getPopularMovies().map { it.results }.getOrDefault(emptyList())
                SeerrDiscoverSliderType.POPULAR_TV ->
                    seerrClient.getPopularTV().map { it.results }.getOrDefault(emptyList())
                SeerrDiscoverSliderType.UPCOMING_MOVIES ->
                    seerrClient.getUpcomingMovies().map { it.results }.getOrDefault(emptyList())
                SeerrDiscoverSliderType.UPCOMING_TV ->
                    seerrClient.discoverTVWithParams(mapOf("firstAirDateGte" to today))
                        .map { it.results }.getOrDefault(emptyList())
                SeerrDiscoverSliderType.PLEX_WATCHLIST ->
                    emptyList() // Watchlist removed - use blacklist instead
                SeerrDiscoverSliderType.TMDB_MOVIE_KEYWORD ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("keywords" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_TV_KEYWORD ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("keywords" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_MOVIE_GENRE ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("genre" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_TV_GENRE ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("genre" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_STUDIO ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("studio" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_NETWORK ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("network" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_SEARCH ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.search(data)
                            .map { it.results.filter { media ->
                                media.mediaType == "movie" || media.mediaType == "tv"
                            } }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_MOVIE_STREAMING_SERVICES ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("watchProviders" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_TV_STREAMING_SERVICES ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("watchProviders" to data))
                            .map { it.results }.getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.RECENTLY_ADDED,
                SeerrDiscoverSliderType.RECENT_REQUESTS,
                SeerrDiscoverSliderType.MOVIE_GENRES,
                SeerrDiscoverSliderType.TV_GENRES,
                SeerrDiscoverSliderType.STUDIOS,
                SeerrDiscoverSliderType.NETWORKS -> emptyList()
            }

            val filtered = items.filterDiscoverable().take(MAX_ITEMS_PER_ROW)
            if (filtered.isNotEmpty()) {
                val rowType = sliderTypeToRowType(slider.type)
                rows.add(
                    SeerrDiscoverRow(
                        key = "discover_${slider.type.name.lowercase(Locale.US)}_${slider.id}",
                        title = title,
                        items = filtered,
                        accentColorValue = colorValue,
                        rowType = rowType,
                    ),
                )
            }
        }

        return rows
    }

    /**
     * Load fallback discover rows when no slider settings are available.
     *
     * @param seerrClient The Seerr API client
     * @param filterDiscoverable Filter function to exclude already available/requested items
     * @return List of default discover rows
     */
    suspend fun loadFallbackRows(
        seerrClient: SeerrClient,
        filterDiscoverable: List<SeerrMedia>.() -> List<SeerrMedia>,
    ): List<SeerrDiscoverRow> {
        val rows = mutableListOf<SeerrDiscoverRow>()
        val trending = seerrClient.getTrending().map { it.results }.getOrDefault(emptyList())
        val popularMovies = seerrClient.getPopularMovies().map { it.results }.getOrDefault(emptyList())
        val popularTv = seerrClient.getPopularTV().map { it.results }.getOrDefault(emptyList())
        val upcoming = seerrClient.getUpcomingMovies().map { it.results }.getOrDefault(emptyList())

        data class FallbackRowData(
            val title: String,
            val colorValue: Long,
            val items: List<SeerrMedia>,
            val rowType: SeerrRowType,
        )

        listOf(
            FallbackRowData("Trending", SeerrColors.PURPLE, trending, SeerrRowType.TRENDING),
            FallbackRowData("Popular Movies", SeerrColors.YELLOW, popularMovies, SeerrRowType.POPULAR_MOVIES),
            FallbackRowData("Popular TV Shows", SeerrColors.TEAL, popularTv, SeerrRowType.POPULAR_TV),
            FallbackRowData("Coming Soon", SeerrColors.BLUE, upcoming, SeerrRowType.OTHER),
        ).forEach { rowData ->
            val filtered = rowData.items.filterDiscoverable().take(MAX_ITEMS_PER_ROW)
            if (filtered.isNotEmpty()) {
                rows.add(
                    SeerrDiscoverRow(
                        key = "fallback_${rowData.title.lowercase(Locale.US).replace(" ", "_")}",
                        title = rowData.title,
                        items = filtered,
                        accentColorValue = rowData.colorValue,
                        rowType = rowData.rowType,
                    ),
                )
            }
        }

        return rows
    }

    /**
     * Get the display title and accent color for a discover slider.
     */
    private fun discoverTitleAndColor(slider: SeerrDiscoverSlider): Pair<String, Long> {
        val defaultTitle = when (slider.type) {
            SeerrDiscoverSliderType.RECENTLY_ADDED -> "Recently Added"
            SeerrDiscoverSliderType.RECENT_REQUESTS -> "Recent Requests"
            SeerrDiscoverSliderType.PLEX_WATCHLIST -> "Watchlist"
            SeerrDiscoverSliderType.TRENDING -> "Trending"
            SeerrDiscoverSliderType.POPULAR_MOVIES -> "Popular Movies"
            SeerrDiscoverSliderType.MOVIE_GENRES -> "Movie Genres"
            SeerrDiscoverSliderType.UPCOMING_MOVIES -> "Upcoming Movies"
            SeerrDiscoverSliderType.STUDIOS -> "Studios"
            SeerrDiscoverSliderType.POPULAR_TV -> "Popular TV"
            SeerrDiscoverSliderType.TV_GENRES -> "TV Genres"
            SeerrDiscoverSliderType.UPCOMING_TV -> "Upcoming TV"
            SeerrDiscoverSliderType.NETWORKS -> "Networks"
            SeerrDiscoverSliderType.TMDB_MOVIE_KEYWORD -> slider.title ?: "Movie Keyword"
            SeerrDiscoverSliderType.TMDB_TV_KEYWORD -> slider.title ?: "TV Keyword"
            SeerrDiscoverSliderType.TMDB_MOVIE_GENRE -> slider.title ?: "Movie Genre"
            SeerrDiscoverSliderType.TMDB_TV_GENRE -> slider.title ?: "TV Genre"
            SeerrDiscoverSliderType.TMDB_STUDIO -> slider.title ?: "Studio"
            SeerrDiscoverSliderType.TMDB_NETWORK -> slider.title ?: "Network"
            SeerrDiscoverSliderType.TMDB_SEARCH -> slider.title ?: "Search"
            SeerrDiscoverSliderType.TMDB_MOVIE_STREAMING_SERVICES -> slider.title ?: "Streaming Movies"
            SeerrDiscoverSliderType.TMDB_TV_STREAMING_SERVICES -> slider.title ?: "Streaming TV"
        }

        val accentColor = when (slider.type) {
            SeerrDiscoverSliderType.TRENDING -> SeerrColors.PURPLE
            SeerrDiscoverSliderType.POPULAR_MOVIES -> SeerrColors.YELLOW
            SeerrDiscoverSliderType.POPULAR_TV -> SeerrColors.TEAL
            SeerrDiscoverSliderType.UPCOMING_MOVIES -> SeerrColors.BLUE
            SeerrDiscoverSliderType.UPCOMING_TV -> SeerrColors.BLUE
            SeerrDiscoverSliderType.PLEX_WATCHLIST -> SeerrColors.GREEN
            else -> SeerrColors.PURPLE
        }

        return defaultTitle to accentColor
    }

    /**
     * Map slider type to row type for navigation purposes.
     */
    private fun sliderTypeToRowType(sliderType: SeerrDiscoverSliderType): SeerrRowType {
        return when (sliderType) {
            SeerrDiscoverSliderType.TRENDING -> SeerrRowType.TRENDING
            SeerrDiscoverSliderType.POPULAR_MOVIES -> SeerrRowType.POPULAR_MOVIES
            SeerrDiscoverSliderType.POPULAR_TV -> SeerrRowType.POPULAR_TV
            else -> SeerrRowType.OTHER
        }
    }

    private const val MAX_ITEMS_PER_ROW = 12
}

/**
 * Represents a row of discover content.
 */
data class SeerrDiscoverRow(
    val key: String,
    val title: String,
    val items: List<SeerrMedia>,
    val accentColorValue: Long,
    val rowType: SeerrRowType = SeerrRowType.OTHER,
)

/**
 * Type of discover row - used to determine "View All" navigation target.
 */
enum class SeerrRowType {
    TRENDING,
    POPULAR_MOVIES,
    POPULAR_TV,
    OTHER,
}

/**
 * Seerr color constants as Long values for platform-independent storage.
 * Convert to Compose Color using: Color(SeerrColors.PURPLE)
 */
object SeerrColors {
    const val PURPLE = 0xFF8B5CF6L
    const val YELLOW = 0xFFFBBF24L
    const val GREEN = 0xFF22C55EL
    const val BLUE = 0xFF60A5FAL
    const val TEAL = 0xFF34D399L
}
