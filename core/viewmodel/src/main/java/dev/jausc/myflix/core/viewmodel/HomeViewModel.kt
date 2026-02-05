package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.HeroContentBuilder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrDiscoverHelper
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val POLL_INTERVAL_MS = 30_000L

/**
 * UI state for the home screen.
 */
data class HomeUiState(
    val libraries: List<JellyfinItem> = emptyList(),
    val continueWatching: List<JellyfinItem> = emptyList(),
    val nextUp: List<JellyfinItem> = emptyList(),
    val recentMovies: List<JellyfinItem> = emptyList(),
    val recentShows: List<JellyfinItem> = emptyList(),
    val recentEpisodes: List<JellyfinItem> = emptyList(),
    val seasonPremieres: List<JellyfinItem> = emptyList(),
    val collections: List<JellyfinItem> = emptyList(),
    val suggestions: List<JellyfinItem> = emptyList(),
    val recentRequests: List<SeerrRequest> = emptyList(),
    val pinnedCollectionsData: Map<String, Pair<String, List<JellyfinItem>>> = emptyMap(),
    val genreRowsData: Map<String, List<JellyfinItem>> = emptyMap(),
    val availableGenres: List<String> = emptyList(),
    val featuredItems: List<JellyfinItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    /**
     * Filtered Next Up - excludes items already in Continue Watching.
     */
    val filteredNextUp: List<JellyfinItem>
        get() {
            val continueWatchingIds = continueWatching.map { it.id }.toSet()
            return nextUp.filter { it.id !in continueWatchingIds }
        }

    val contentReady: Boolean
        get() = !isLoading && featuredItems.isNotEmpty()
}

/**
 * Shared ViewModel for home screens.
 * Manages content loading, caching, and refresh with proper lifecycle handling.
 */
class HomeViewModel(
    private val jellyfinClient: JellyfinClient,
    private val preferences: AppPreferences,
    private val seerrRepository: SeerrRepository? = null,
    private val heroConfig: HeroContentBuilder.Config = HeroContentBuilder.defaultConfig,
) : ViewModel() {

    /**
     * Factory for creating HomeViewModel with manual dependency injection.
     */
    class Factory(
        private val jellyfinClient: JellyfinClient,
        private val preferences: AppPreferences,
        private val seerrRepository: SeerrRepository? = null,
        private val heroConfig: HeroContentBuilder.Config = HeroContentBuilder.defaultConfig,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(jellyfinClient, preferences, seerrRepository, heroConfig) as T
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Mutex to prevent overlapping loadContent calls during background polling */
    private val loadContentMutex = Mutex()

    // Observe preference changes
    val hideWatchedFromRecent = preferences.hideWatchedFromRecent
    val showSeasonPremieres = preferences.showSeasonPremieres
    val showGenreRows = preferences.showGenreRows
    val enabledGenres = preferences.enabledGenres
    val showCollections = preferences.showCollections
    val pinnedCollections = preferences.pinnedCollections
    val showSuggestions = preferences.showSuggestions
    val showSeerrRecentRequests = preferences.showSeerrRecentRequests
    val hasSeenNavBarTip = preferences.hasSeenNavBarTip

    /**
     * Mark the nav bar tip as seen (first-run tip dismissed).
     */
    fun setHasSeenNavBarTip() {
        preferences.setHasSeenNavBarTip(true)
    }

    init {
        loadContent()
        startBackgroundPolling()
    }

    /**
     * Load all home screen content.
     */
    fun loadContent(showLoading: Boolean = true) {
        viewModelScope.launch {
            // Use mutex to prevent overlapping loads during background polling
            // tryLock returns false if already loading, skipping this refresh
            if (!loadContentMutex.tryLock()) return@launch

            try {
                if (showLoading) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }

                jellyfinClient.clearCache()

                // Get libraries first
                jellyfinClient.getLibraries()
                    .onSuccess { libs ->
                        _uiState.update { it.copy(libraries = libs) }

                        val moviesLibraryId = findMoviesLibraryId(libs)
                        val showsLibraryId = findShowsLibraryId(libs)

                        // Load latest movies
                        if (moviesLibraryId != null) {
                            jellyfinClient.getLatestMovies(moviesLibraryId, limit = 12)
                                .onSuccess { items ->
                                    _uiState.update { it.copy(recentMovies = items) }
                                }
                        }

                        // Load latest series
                        if (showsLibraryId != null) {
                            jellyfinClient.getLatestSeries(showsLibraryId, limit = 12)
                                .onSuccess { items ->
                                    _uiState.update { it.copy(recentShows = items) }
                                }
                        }

                        // Load latest episodes
                        if (showsLibraryId != null) {
                            jellyfinClient.getLatestEpisodes(showsLibraryId, limit = 12)
                                .onSuccess { items ->
                                    _uiState.update { it.copy(recentEpisodes = items) }
                                }
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(error = "Failed to load libraries: ${e.message ?: "Unknown error"}")
                        }
                    }

                // Load Next Up
                jellyfinClient.getNextUp(limit = 12)
                    .onSuccess { items ->
                        _uiState.update { it.copy(nextUp = items) }
                    }

                // Load Continue Watching
                jellyfinClient.getContinueWatching(limit = 12)
                    .onSuccess { items ->
                        _uiState.update { it.copy(continueWatching = items) }
                    }

                // Load Season Premieres
                if (showSeasonPremieres.value) {
                    jellyfinClient.getUpcomingEpisodes(limit = 12)
                        .onSuccess { items ->
                            _uiState.update { it.copy(seasonPremieres = items) }
                        }
                }

                // Load Collections
                if (showCollections.value) {
                    jellyfinClient.getCollections(limit = 50)
                        .onSuccess { items ->
                            _uiState.update { it.copy(collections = items) }
                            loadPinnedCollections()
                        }
                }

                // Load Suggestions
                if (showSuggestions.value) {
                    jellyfinClient.getSuggestions(limit = 12)
                        .onSuccess { items ->
                            _uiState.update { it.copy(suggestions = items) }
                        }
                }

                // Load Genre Rows
                if (showGenreRows.value) {
                    loadGenreRows()
                }

                // Load Recent Requests from Seerr
                if (showSeerrRecentRequests.value && seerrRepository != null) {
                    loadRecentRequests()
                }

                // Build featured items
                buildFeaturedItems()

                _uiState.update { it.copy(isLoading = false) }
            } finally {
                loadContentMutex.unlock()
            }
        }
    }

    /**
     * Refresh content without showing loading indicator.
     */
    fun refresh() {
        loadContent(showLoading = false)
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Mark an item as watched/unwatched.
     */
    fun setPlayed(itemId: String, played: Boolean) {
        viewModelScope.launch {
            jellyfinClient.setPlayed(itemId, played)
            loadContent(showLoading = false)
        }
    }

    /**
     * Toggle an item's favorite status.
     */
    fun setFavorite(itemId: String, favorite: Boolean) {
        viewModelScope.launch {
            jellyfinClient.setFavorite(itemId, favorite)
            loadContent(showLoading = false)
        }
    }

    /**
     * Hide an item from Continue Watching by clearing its playback position.
     */
    fun hideFromResume(itemId: String) {
        viewModelScope.launch {
            jellyfinClient.hideFromResume(itemId)
            loadContent(showLoading = false)
        }
    }

    private suspend fun loadPinnedCollections() {
        val pinned = pinnedCollections.value
        if (pinned.isEmpty()) {
            _uiState.update { it.copy(pinnedCollectionsData = emptyMap()) }
            return
        }

        val pinnedData = linkedMapOf<String, Pair<String, List<JellyfinItem>>>()
        pinned.forEach { collectionId ->
            jellyfinClient.getItem(collectionId).onSuccess { collection ->
                jellyfinClient.getCollectionItems(collectionId, limit = 12).onSuccess { items ->
                    if (items.isNotEmpty()) {
                        pinnedData[collectionId] = Pair(collection.name, items)
                    }
                }
            }
        }
        _uiState.update { it.copy(pinnedCollectionsData = pinnedData) }
    }

    private suspend fun loadGenreRows() {
        val state = _uiState.value

        // Get available genres if not loaded
        if (state.availableGenres.isEmpty()) {
            jellyfinClient.getGenres()
                .onSuccess { genres ->
                    _uiState.update { it.copy(availableGenres = genres.map { g -> g.name }) }
                }
        }

        val enabled = enabledGenres.value
        val genresToLoad = if (enabled.isNotEmpty()) {
            enabled
        } else {
            _uiState.value.availableGenres.take(3)
        }

        val genreData = mutableMapOf<String, List<JellyfinItem>>()
        genresToLoad.forEach { genreName ->
            jellyfinClient.getItemsByGenre(genreName, limit = 12)
                .onSuccess { items ->
                    if (items.isNotEmpty()) {
                        genreData[genreName] = items
                    }
                }
        }
        _uiState.update { it.copy(genreRowsData = genreData) }
    }

    private fun buildFeaturedItems() {
        val state = _uiState.value
        val featured = HeroContentBuilder.buildFeaturedItems(
            continueWatching = state.continueWatching,
            nextUp = state.nextUp,
            recentMovies = state.recentMovies,
            recentShows = state.recentShows,
            config = heroConfig,
        )
        _uiState.update { it.copy(featuredItems = featured) }
    }

    private fun startBackgroundPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                loadContent(showLoading = false)
            }
        }
    }

    private fun findMoviesLibraryId(libraries: List<JellyfinItem>): String? =
        libraries.find { it.collectionType == "movies" }?.id

    private fun findShowsLibraryId(libraries: List<JellyfinItem>): String? =
        libraries.find { it.collectionType == "tvshows" }?.id

    /**
     * Filter items based on hideWatchedFromRecent preference.
     */
    fun filterWatched(items: List<JellyfinItem>): List<JellyfinItem> {
        return if (hideWatchedFromRecent.value) {
            items.filter { it.userData?.played != true }
        } else {
            items
        }
    }

    private suspend fun loadRecentRequests() {
        val repo = seerrRepository ?: return
        SeerrDiscoverHelper.loadAllRequestsRow(repo)?.let { row ->
            _uiState.update { it.copy(recentRequests = row.items) }
        }
    }
}
