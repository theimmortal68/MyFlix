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
     * @return List of discover rows with content
     */
    suspend fun loadDiscoverRows(
        seerrClient: SeerrClient,
        sliders: List<SeerrDiscoverSlider>,
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
                        .map { it.results }
                        .getOrDefault(emptyList())
                SeerrDiscoverSliderType.PLEX_WATCHLIST ->
                    emptyList() // Watchlist removed - use blacklist instead
                SeerrDiscoverSliderType.TMDB_MOVIE_KEYWORD ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("keywords" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_TV_KEYWORD ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("keywords" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_MOVIE_GENRE ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("genre" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_TV_GENRE ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("genre" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_STUDIO ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("studio" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_NETWORK ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("network" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_SEARCH ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.search(data)
                            .map {
                                it.results.filter { media ->
                                media.mediaType == "movie" || media.mediaType == "tv"
                            }
                            }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_MOVIE_STREAMING_SERVICES ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverMoviesWithParams(mapOf("watchProviders" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.TMDB_TV_STREAMING_SERVICES ->
                    slider.data?.takeIf { it.isNotBlank() }?.let { data ->
                        seerrClient.discoverTVWithParams(mapOf("watchProviders" to data))
                            .map { it.results }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                SeerrDiscoverSliderType.RECENTLY_ADDED,
                SeerrDiscoverSliderType.RECENT_REQUESTS,
                SeerrDiscoverSliderType.MOVIE_GENRES,
                SeerrDiscoverSliderType.TV_GENRES,
                SeerrDiscoverSliderType.STUDIOS,
                SeerrDiscoverSliderType.NETWORKS,
                SeerrDiscoverSliderType.UNKNOWN -> emptyList()
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
     * @return List of default discover rows
     */
    suspend fun loadFallbackRows(seerrClient: SeerrClient,): List<SeerrDiscoverRow> {
        val rows = mutableListOf<SeerrDiscoverRow>()
        val trending = seerrClient.getTrending().map { it.results }.getOrDefault(emptyList())
        val popularMovies = seerrClient.getPopularMovies().map { it.results }.getOrDefault(emptyList())
        val popularTv = seerrClient.getPopularTV().map { it.results }.getOrDefault(emptyList())
        val upcomingMovies = seerrClient.getUpcomingMovies().map { it.results }.getOrDefault(emptyList())
        val upcomingTv = seerrClient.getUpcomingTV().map { it.results }.getOrDefault(emptyList())

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
            FallbackRowData("Upcoming Movies", SeerrColors.BLUE, upcomingMovies, SeerrRowType.UPCOMING_MOVIES),
            FallbackRowData("Upcoming TV", SeerrColors.BLUE, upcomingTv, SeerrRowType.UPCOMING_TV),
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
            SeerrDiscoverSliderType.UNKNOWN -> slider.title ?: "Unknown"
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
            SeerrDiscoverSliderType.UPCOMING_MOVIES -> SeerrRowType.UPCOMING_MOVIES
            SeerrDiscoverSliderType.UPCOMING_TV -> SeerrRowType.UPCOMING_TV
            else -> SeerrRowType.OTHER
        }
    }

    private const val MAX_ITEMS_PER_ROW = 12

    /**
     * Load genre rows for browsing.
     *
     * @param seerrClient The Seerr API client
     * @return List of genre rows (movie genres and TV genres)
     */
    suspend fun loadGenreRows(seerrClient: SeerrClient,): List<SeerrGenreRow> {
        val rows = mutableListOf<SeerrGenreRow>()

        // Load movie genres
        seerrClient.getMovieGenres().onSuccess { genres ->
            if (genres.isNotEmpty()) {
                rows.add(
                    SeerrGenreRow(
                        key = "genre_movies",
                        title = "Movie Genres",
                        mediaType = "movie",
                        genres = genres,
                    ),
                )
            }
        }

        // Load TV genres
        seerrClient.getTVGenres().onSuccess { genres ->
            if (genres.isNotEmpty()) {
                rows.add(
                    SeerrGenreRow(
                        key = "genre_tv",
                        title = "TV Genres",
                        mediaType = "tv",
                        genres = genres,
                    ),
                )
            }
        }

        return rows
    }

    /**
     * Get studios row for browsing.
     *
     * @return A row of popular movie studios
     */
    fun getStudiosRow(): SeerrStudioRow {
        return SeerrStudioRow(
            key = "studios",
            title = "Studios",
            studios = PopularStudios.studios,
        )
    }

    /**
     * Get networks row for browsing.
     *
     * @return A row of popular TV networks
     */
    fun getNetworksRow(): SeerrNetworkRow {
        return SeerrNetworkRow(
            key = "networks",
            title = "Networks",
            networks = PopularNetworks.networks,
        )
    }

    /**
     * Load recent requests row for the current user.
     *
     * @param seerrClient The Seerr API client
     * @return A row of recent requests, or null if no requests exist
     */
    suspend fun loadRecentRequestsRow(seerrClient: SeerrClient): SeerrRequestRow? {
        return seerrClient.getMyRequests(page = 1, pageSize = MAX_ITEMS_PER_ROW)
            .getOrNull()
            ?.results
            ?.takeIf { it.isNotEmpty() }
            ?.let { requests ->
                SeerrRequestRow(
                    key = "recent_requests",
                    title = "My Requests",
                    items = requests,
                    accentColorValue = SeerrColors.GREEN,
                )
            }
    }

    /**
     * Load all recent requests row (not filtered by user).
     * Shows all requests from all users, sorted by most recent.
     *
     * @param seerrClient The Seerr API client
     * @return A row of all recent requests, or null if no requests exist
     */
    suspend fun loadAllRequestsRow(seerrClient: SeerrClient): SeerrRequestRow? {
        return seerrClient.getAllRequests(page = 1, pageSize = MAX_ITEMS_PER_ROW)
            .getOrNull()
            ?.results
            ?.takeIf { it.isNotEmpty() }
            ?.let { requests ->
                SeerrRequestRow(
                    key = "all_requests",
                    title = "Recent Requests",
                    items = requests,
                    accentColorValue = SeerrColors.GREEN,
                )
            }
    }
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
 * Represents a row of genre cards for browsing.
 */
data class SeerrGenreRow(
    val key: String,
    val title: String,
    val mediaType: String, // "movie" or "tv"
    val genres: List<SeerrGenre>,
)

/**
 * Represents a row of studio cards for browsing.
 */
data class SeerrStudioRow(
    val key: String,
    val title: String,
    val studios: List<SeerrStudio>,
)

/**
 * Represents a row of network cards for browsing.
 */
data class SeerrNetworkRow(
    val key: String,
    val title: String,
    val networks: List<SeerrNetwork>,
)

/**
 * Represents a row of user request cards for browsing.
 */
data class SeerrRequestRow(
    val key: String,
    val title: String,
    val items: List<SeerrRequest>,
    val accentColorValue: Long = SeerrColors.GREEN,
)

/**
 * Type of discover row - used to determine "View All" navigation target.
 */
enum class SeerrRowType {
    TRENDING,
    POPULAR_MOVIES,
    POPULAR_TV,
    UPCOMING_MOVIES,
    UPCOMING_TV,
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
