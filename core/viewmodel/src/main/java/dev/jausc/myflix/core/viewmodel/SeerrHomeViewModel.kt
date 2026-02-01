package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrDiscoverHelper
import dev.jausc.myflix.core.seerr.SeerrDiscoverRow
import dev.jausc.myflix.core.seerr.SeerrGenreRow
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrNetworkRow
import dev.jausc.myflix.core.seerr.SeerrRowType
import dev.jausc.myflix.core.seerr.SeerrStudioRow
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
    private val seerrClient: SeerrClient,
) : ViewModel() {

    /**
     * Factory for creating SeerrHomeViewModel with manual dependency injection.
     */
    class Factory(
        private val seerrClient: SeerrClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeerrHomeViewModel(seerrClient) as T
    }

    private val _uiState = MutableStateFlow(SeerrHomeUiState())
    val uiState: StateFlow<SeerrHomeUiState> = _uiState.asStateFlow()

    /** Exposed auth state for observing in Composables */
    val isAuthenticated = seerrClient.isAuthenticated

    init {
        loadContent()
    }

    /**
     * Load all Seerr discover content.
     */
    fun loadContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (!seerrClient.isAuthenticated.value) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Not connected to Seerr",
                    )
                }
                return@launch
            }

            // Load discover rows using shared helper
            val sliders = seerrClient.getDiscoverSettings().getOrNull()
            val discoverRows = if (!sliders.isNullOrEmpty()) {
                SeerrDiscoverHelper.loadDiscoverRows(seerrClient, sliders)
            } else {
                SeerrDiscoverHelper.loadFallbackRows(seerrClient)
            }

            val featuredItem = discoverRows.firstOrNull()?.items?.firstOrNull()

            // Load genre rows for browsing
            val genreRows = SeerrDiscoverHelper.loadGenreRows(seerrClient)

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
     * Refresh content without showing loading indicator.
     */
    fun refresh() {
        viewModelScope.launch {
            if (!seerrClient.isAuthenticated.value) {
                _uiState.update {
                    it.copy(errorMessage = "Not connected to Seerr")
                }
                return@launch
            }

            val sliders = seerrClient.getDiscoverSettings().getOrNull()
            val discoverRows = if (!sliders.isNullOrEmpty()) {
                SeerrDiscoverHelper.loadDiscoverRows(seerrClient, sliders)
            } else {
                SeerrDiscoverHelper.loadFallbackRows(seerrClient)
            }

            val featuredItem = discoverRows.firstOrNull()?.items?.firstOrNull()
            val genreRows = SeerrDiscoverHelper.loadGenreRows(seerrClient)
            val studiosRow = SeerrDiscoverHelper.getStudiosRow()
            val networksRow = SeerrDiscoverHelper.getNetworksRow()

            _uiState.update {
                it.copy(
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
     * Request a movie from Seerr.
     */
    fun requestMovie(tmdbId: Int) {
        viewModelScope.launch {
            seerrClient.requestMovie(tmdbId)
        }
    }

    /**
     * Request a TV show from Seerr.
     */
    fun requestTVShow(tmdbId: Int) {
        viewModelScope.launch {
            seerrClient.requestTVShow(tmdbId)
        }
    }

    /**
     * Request media (movie or TV show) based on type.
     */
    fun requestMedia(media: SeerrMedia) {
        viewModelScope.launch {
            val tmdbId = media.tmdbId ?: media.id
            if (media.isMovie) {
                seerrClient.requestMovie(tmdbId)
            } else {
                seerrClient.requestTVShow(tmdbId)
            }
        }
    }

    /**
     * Add media to blacklist.
     */
    fun addToBlacklist(media: SeerrMedia) {
        viewModelScope.launch {
            seerrClient.addToBlacklist(media.tmdbId ?: media.id, media.mediaType)
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
