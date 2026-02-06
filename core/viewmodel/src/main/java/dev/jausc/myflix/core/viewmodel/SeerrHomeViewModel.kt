package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.seerr.SeerrDiscoverHelper
import dev.jausc.myflix.core.seerr.SeerrDiscoverRow
import dev.jausc.myflix.core.seerr.SeerrGenreRow
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrNetworkRow
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrRowType
import dev.jausc.myflix.core.seerr.SeerrStudioRow
import dev.jausc.myflix.core.seerr.filterDiscoverable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Seerr home/discover screen.
 */
data class SeerrHomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val rows: List<SeerrDiscoverRow> = emptyList(),
    val genreRows: List<SeerrGenreRow> = emptyList(),
    val studiosRow: SeerrStudioRow? = null,
    val networksRow: SeerrNetworkRow? = null,
    val featuredItem: SeerrMedia? = null,
) {
    /** Trending row from discover rows */
    val trendingRow: SeerrDiscoverRow?
        get() = rows.find { it.rowType == SeerrRowType.TRENDING }

    /** Popular Movies row from discover rows */
    val popularMoviesRow: SeerrDiscoverRow?
        get() = rows.find { it.rowType == SeerrRowType.POPULAR_MOVIES }

    /** Popular TV row from discover rows */
    val popularTvRow: SeerrDiscoverRow?
        get() = rows.find { it.rowType == SeerrRowType.POPULAR_TV }

    /** Upcoming Movies row from discover rows */
    val upcomingMoviesRow: SeerrDiscoverRow?
        get() = rows.find { it.rowType == SeerrRowType.UPCOMING_MOVIES }

    /** Upcoming TV row from discover rows */
    val upcomingTvRow: SeerrDiscoverRow?
        get() = rows.find { it.rowType == SeerrRowType.UPCOMING_TV }

    /** Movie genres row */
    val movieGenresRow: SeerrGenreRow?
        get() = genreRows.find { it.mediaType == "movie" }

    /** TV genres row */
    val tvGenresRow: SeerrGenreRow?
        get() = genreRows.find { it.mediaType == "tv" }

    /** Other/custom rows not in the predefined types */
    val otherRows: List<SeerrDiscoverRow>
        get() = rows.filter { it.rowType == SeerrRowType.OTHER }
}

/**
 * Shared ViewModel for Seerr home/discover screens.
 * Manages content loading and state with proper lifecycle handling.
 */
class SeerrHomeViewModel(
    private val seerrRepository: SeerrRepository,
) : ViewModel() {

    /**
     * Factory for creating SeerrHomeViewModel with manual dependency injection.
     */
    class Factory(
        private val seerrRepository: SeerrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeerrHomeViewModel(seerrRepository) as T
    }

    private val _uiState = MutableStateFlow(SeerrHomeUiState())
    val uiState: StateFlow<SeerrHomeUiState> = _uiState.asStateFlow()

    /** Focused media ratings (RT/IMDb) fetched on demand */
    private val _focusedRatings = MutableStateFlow<DiscoverFocusedRatings?>(null)
    val focusedRatings: StateFlow<DiscoverFocusedRatings?> = _focusedRatings.asStateFlow()

    private val ratingsCache = mutableMapOf<String, DiscoverFocusedRatings>()
    private var ratingsFetchJob: Job? = null

    /** Exposed auth state for observing in Composables */
    val isAuthenticated = seerrRepository.isAuthenticated

    /** Current authenticated user */
    val currentUser = seerrRepository.currentUser

    /** Cached movie genres */
    val movieGenres = seerrRepository.movieGenres

    /** Cached TV genres */
    val tvGenres = seerrRepository.tvGenres

    init {
        loadContent()
    }

    /**
     * Load all Seerr discover content.
     */
    fun loadContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (!seerrRepository.isAuthenticated.value) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Not connected to Seerr",
                    )
                }
                return@launch
            }

            // Load discover rows using shared helper
            val sliders = seerrRepository.getDiscoverSettings().getOrNull()
            val discoverRows = if (!sliders.isNullOrEmpty()) {
                SeerrDiscoverHelper.loadDiscoverRows(seerrRepository, sliders)
            } else {
                SeerrDiscoverHelper.loadFallbackRows(seerrRepository)
            }

            val featuredItem = discoverRows.firstOrNull()?.items?.firstOrNull()

            // Load genre rows for browsing
            val genreRows = SeerrDiscoverHelper.loadGenreRows(seerrRepository)

            // Load studios and networks rows
            val studiosRow = SeerrDiscoverHelper.getStudiosRow()
            val networksRow = SeerrDiscoverHelper.getNetworksRow()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    rows = discoverRows,
                    genreRows = genreRows,
                    studiosRow = studiosRow,
                    networksRow = networksRow,
                    featuredItem = featuredItem,
                )
            }
        }
    }

    /**
     * Refresh content with optional refresh indicator.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            if (!seerrRepository.isAuthenticated.value) {
                _uiState.update {
                    it.copy(isRefreshing = false, errorMessage = "Not connected to Seerr")
                }
                return@launch
            }

            val sliders = seerrRepository.getDiscoverSettings().getOrNull()
            val discoverRows = if (!sliders.isNullOrEmpty()) {
                SeerrDiscoverHelper.loadDiscoverRows(seerrRepository, sliders)
            } else {
                SeerrDiscoverHelper.loadFallbackRows(seerrRepository)
            }

            val featuredItem = discoverRows.firstOrNull()?.items?.firstOrNull()
            val genreRows = SeerrDiscoverHelper.loadGenreRows(seerrRepository)
            val studiosRow = SeerrDiscoverHelper.getStudiosRow()
            val networksRow = SeerrDiscoverHelper.getNetworksRow()

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    rows = discoverRows,
                    genreRows = genreRows,
                    studiosRow = studiosRow,
                    networksRow = networksRow,
                    featuredItem = featuredItem,
                )
            }
        }
    }

    /**
     * Load more items for a specific row (pagination).
     *
     * @param rowId The unique key of the row to load more items for
     * @param page The page number to load (1-indexed)
     */
    fun loadMoreForRow(rowId: String, page: Int) {
        viewModelScope.launch {
            val currentRows = _uiState.value.rows.toMutableList()
            val rowIndex = currentRows.indexOfFirst { it.key == rowId }
            if (rowIndex == -1) return@launch

            val row = currentRows[rowIndex]
            // Determine which API call to make based on row type
            val result = when (row.rowType) {
                SeerrRowType.TRENDING -> seerrRepository.getTrending(page)
                SeerrRowType.POPULAR_MOVIES -> seerrRepository.getPopularMovies(page)
                SeerrRowType.POPULAR_TV -> seerrRepository.getPopularTV(page)
                SeerrRowType.UPCOMING_MOVIES -> seerrRepository.getUpcomingMovies(page)
                SeerrRowType.UPCOMING_TV -> seerrRepository.getUpcomingTV(page)
                SeerrRowType.OTHER -> return@launch // Custom rows don't paginate via this method
            }

            result
                .onSuccess { response ->
                    val existingItems = row.items.toMutableList()
                    val newItems = response.results.filterDiscoverable()
                    // Avoid duplicates by checking IDs
                    val uniqueNewItems = newItems.filter { newItem ->
                        existingItems.none { existing -> existing.id == newItem.id }
                    }
                    existingItems.addAll(uniqueNewItems)

                    currentRows[rowIndex] = row.copy(items = existingItems)
                    _uiState.update { it.copy(rows = currentRows) }
                }
                .onFailure { error ->
                    // Log error but don't show to user - pagination failures are not critical
                    android.util.Log.w("SeerrHomeViewModel", "Failed to load more items: ${error.message}")
                }
        }
    }

    /**
     * Request a movie from Seerr.
     */
    fun requestMovie(tmdbId: Int) {
        viewModelScope.launch {
            seerrRepository.requestMovie(tmdbId)
        }
    }

    /**
     * Request a TV show from Seerr.
     */
    fun requestTVShow(tmdbId: Int) {
        viewModelScope.launch {
            seerrRepository.requestTVShow(tmdbId)
        }
    }

    /**
     * Request media (movie or TV show) based on type.
     */
    fun requestMedia(media: SeerrMedia) {
        viewModelScope.launch {
            val tmdbId = media.tmdbId ?: media.id
            if (media.isMovie) {
                seerrRepository.requestMovie(tmdbId)
            } else {
                seerrRepository.requestTVShow(tmdbId)
            }
        }
    }

    /**
     * Add media to blacklist.
     */
    fun addToBlacklist(media: SeerrMedia) {
        viewModelScope.launch {
            seerrRepository.addToBlacklist(media.tmdbId ?: media.id, media.mediaType)
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Fetch RT/IMDb ratings for a focused media item (debounced).
     * Results are cached to avoid redundant API calls.
     */
    @Suppress("MagicNumber")
    fun fetchRatingsForMedia(tmdbId: Int, isMovie: Boolean) {
        val cacheKey = "${if (isMovie) "movie" else "tv"}_$tmdbId"
        ratingsCache[cacheKey]?.let { cached ->
            _focusedRatings.value = cached
            return
        }

        ratingsFetchJob?.cancel()
        ratingsFetchJob = viewModelScope.launch {
            delay(300) // Debounce rapid focus changes
            if (isMovie) {
                seerrRepository.getMovieRatings(tmdbId)
                    .onSuccess { response ->
                        val ratings = DiscoverFocusedRatings(
                            tmdbId = tmdbId,
                            rtScore = response.rt?.criticsScore,
                            rtFresh = response.rt?.isCriticsFresh == true,
                            imdbScore = response.imdb?.criticsScore,
                        )
                        ratingsCache[cacheKey] = ratings
                        _focusedRatings.value = ratings
                    }
            } else {
                seerrRepository.getTVRatings(tmdbId)
                    .onSuccess { response ->
                        val ratings = DiscoverFocusedRatings(
                            tmdbId = tmdbId,
                            rtScore = response.criticsScore,
                            rtFresh = response.isCriticsFresh,
                            imdbScore = null, // TV endpoint doesn't return IMDb
                        )
                        ratingsCache[cacheKey] = ratings
                        _focusedRatings.value = ratings
                    }
            }
        }
    }
}

/**
 * Ratings data for a focused discover media item.
 */
data class DiscoverFocusedRatings(
    val tmdbId: Int,
    val rtScore: Int? = null,
    val rtFresh: Boolean = false,
    val imdbScore: Double? = null,
)
