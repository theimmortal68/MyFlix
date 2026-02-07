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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

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
    prefetchedState: HomeUiState? = null,
) : ViewModel() {

    /**
     * Factory for creating HomeViewModel with manual dependency injection.
     */
    class Factory(
        private val jellyfinClient: JellyfinClient,
        private val preferences: AppPreferences,
        private val seerrRepository: SeerrRepository? = null,
        private val heroConfig: HeroContentBuilder.Config = HeroContentBuilder.defaultConfig,
        private val prefetchedState: HomeUiState? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(jellyfinClient, preferences, seerrRepository, heroConfig, prefetchedState) as T
    }

    private val _uiState = MutableStateFlow(prefetchedState ?: HomeUiState())
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
        if (prefetchedState?.featuredItems?.isNotEmpty() == true) {
            // Prefetched data available — skip initial load, just start polling
            startBackgroundPolling()
        } else {
            loadContent()
            startBackgroundPolling()
        }
    }

    /**
     * Load all home screen content with parallel API calls.
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

                // Phase 1: All independent calls in parallel
                coroutineScope {
                    val librariesDef = async { jellyfinClient.getLibraries() }
                    val nextUpDef = async { jellyfinClient.getNextUp(limit = 12) }
                    val continueDef = async { jellyfinClient.getContinueWatching(limit = 12) }

                    val libsResult = librariesDef.await()
                    val libs = libsResult.getOrNull()

                    if (libs != null) {
                        _uiState.update { it.copy(libraries = libs) }

                        val moviesLibraryId = findMoviesLibraryId(libs)
                        val showsLibraryId = findShowsLibraryId(libs)

                        // Phase 2: Library-dependent calls in parallel
                        val moviesDef = moviesLibraryId?.let { async { jellyfinClient.getLatestMovies(it, limit = 12) } }
                        val seriesDef = showsLibraryId?.let { async { jellyfinClient.getLatestSeries(it, limit = 12) } }
                        val episodesDef = showsLibraryId?.let { async { jellyfinClient.getLatestEpisodes(it, limit = 12) } }

                        moviesDef?.await()?.onSuccess { items -> _uiState.update { s -> s.copy(recentMovies = items) } }
                        seriesDef?.await()?.onSuccess { items -> _uiState.update { s -> s.copy(recentShows = items) } }
                        episodesDef?.await()?.onSuccess { items -> _uiState.update { s -> s.copy(recentEpisodes = items) } }
                    } else {
                        _uiState.update {
                            it.copy(error = "Failed to load libraries: ${libsResult.exceptionOrNull()?.message ?: "Unknown error"}")
                        }
                    }

                    nextUpDef.await().onSuccess { items -> _uiState.update { s -> s.copy(nextUp = items) } }
                    continueDef.await().onSuccess { items -> _uiState.update { s -> s.copy(continueWatching = items) } }
                }

                // Build featured items from phase 1+2 results
                buildFeaturedItems()

                // Phase 3: Below-fold optional data in parallel
                coroutineScope {
                    val jobs = mutableListOf<kotlinx.coroutines.Deferred<*>>()

                    if (showSeasonPremieres.value) {
                        jobs += async {
                            jellyfinClient.getUpcomingEpisodes(limit = 12).onSuccess { items ->
                                _uiState.update { it.copy(seasonPremieres = items) }
                            }
                        }
                    }
                    if (showCollections.value) {
                        jobs += async {
                            jellyfinClient.getCollections(limit = 50).onSuccess { items ->
                                _uiState.update { it.copy(collections = items) }
                                loadPinnedCollections()
                            }
                        }
                    }
                    if (showSuggestions.value) {
                        jobs += async {
                            jellyfinClient.getSuggestions(limit = 12).onSuccess { items ->
                                _uiState.update { it.copy(suggestions = items) }
                            }
                        }
                    }
                    if (showGenreRows.value) {
                        jobs += async { loadGenreRows() }
                    }
                    if (showSeerrRecentRequests.value && seerrRepository != null) {
                        jobs += async { loadRecentRequests() }
                    }
                    jobs.awaitAll()
                }

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
