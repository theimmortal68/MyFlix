package dev.jausc.myflix.core.viewmodel

import dev.jausc.myflix.core.common.HeroContentBuilder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lightweight prefetcher that runs parallel API calls during splash
 * to warm the home screen data before HomeViewModel is created.
 *
 * Usage:
 * 1. Create in MyFlixTvApp composable
 * 2. Call [prefetch] when user is logged in
 * 3. Observe [isHeroReady] to dismiss splash
 * 4. Pass [consumeState] result to HomeViewModel.Factory
 */
class HomeDataPrefetcher(
    private val jellyfinClient: JellyfinClient,
    private val preferences: AppPreferences,
) {
    private val _prefetchedState = MutableStateFlow<HomeUiState?>(null)
    private val _isHeroReady = MutableStateFlow(false)
    val isHeroReady: StateFlow<Boolean> = _isHeroReady.asStateFlow()

    /** Libraries exposed for MainActivity NavRail (replaces separate getLibraries call). */
    private val _libraries = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val libraries: StateFlow<List<JellyfinItem>> = _libraries.asStateFlow()

    private var consumed = false
    private val stateMutex = Mutex()

    /**
     * Run parallel API calls to prefetch home screen data.
     * Call this from a coroutine when the user is logged in.
     */
    suspend fun prefetch() {
        coroutineScope {
            // Phase 1: All independent calls in parallel
            val librariesDef = async { jellyfinClient.getLibraries() }
            val nextUpDef = async { jellyfinClient.getNextUp(limit = 12) }
            val continueDef = async { jellyfinClient.getContinueWatching(limit = 12) }

            val libs = librariesDef.await().getOrNull() ?: emptyList()
            _libraries.value = libs

            val moviesLibId = libs.find { it.collectionType == "movies" }?.id
            val showsLibId = libs.find { it.collectionType == "tvshows" }?.id

            // Phase 2: Library-dependent calls in parallel alongside awaiting phase 1 results
            val moviesDef = moviesLibId?.let { async { jellyfinClient.getLatestMovies(it, limit = 12) } }
            val seriesDef = showsLibId?.let { async { jellyfinClient.getLatestSeries(it, limit = 12) } }
            val episodesDef = showsLibId?.let { async { jellyfinClient.getLatestEpisodes(it, limit = 12) } }

            val nextUp = nextUpDef.await().getOrNull() ?: emptyList()
            val continueWatching = continueDef.await().getOrNull() ?: emptyList()
            val recentMovies = moviesDef?.await()?.getOrNull() ?: emptyList()
            val recentShows = seriesDef?.await()?.getOrNull() ?: emptyList()
            val recentEpisodes = episodesDef?.await()?.getOrNull() ?: emptyList()

            val featured = HeroContentBuilder.buildFeaturedItems(
                continueWatching = continueWatching,
                nextUp = nextUp,
                recentMovies = recentMovies,
                recentShows = recentShows,
            )

            _prefetchedState.value = HomeUiState(
                libraries = libs,
                continueWatching = continueWatching,
                nextUp = nextUp,
                recentMovies = recentMovies,
                recentShows = recentShows,
                recentEpisodes = recentEpisodes,
                featuredItems = featured,
                isLoading = false,
            )
            _isHeroReady.value = featured.isNotEmpty()

            // Phase 3: Below-fold optional data (non-blocking for splash dismiss)
            loadBelowFoldData(libs, showsLibId)
        }
    }

    private suspend fun loadBelowFoldData(libs: List<JellyfinItem>, showsLibId: String?) {
        coroutineScope {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<*>>()

            if (preferences.showSeasonPremieres.value) {
                jobs += async {
                    jellyfinClient.getUpcomingEpisodes(limit = 12).onSuccess { items ->
                        updateState { it.copy(seasonPremieres = items) }
                    }
                }
            }
            if (preferences.showCollections.value) {
                jobs += async {
                    jellyfinClient.getCollections(limit = 50).onSuccess { items ->
                        updateState { it.copy(collections = items) }
                        loadPinnedCollections()
                    }
                }
            }
            if (preferences.showSuggestions.value) {
                jobs += async {
                    jellyfinClient.getSuggestions(limit = 12).onSuccess { items ->
                        updateState { it.copy(suggestions = items) }
                    }
                }
            }
            if (preferences.showGenreRows.value) {
                jobs += async { loadGenreRows() }
            }
            jobs.awaitAll()
        }
    }

    private suspend fun loadPinnedCollections() {
        val pinned = preferences.pinnedCollections.value
        if (pinned.isEmpty()) {
            updateState { it.copy(pinnedCollectionsData = emptyMap()) }
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
        updateState { it.copy(pinnedCollectionsData = pinnedData) }
    }

    private suspend fun loadGenreRows() {
        val current = _prefetchedState.value ?: return

        var availableGenres = current.availableGenres
        if (availableGenres.isEmpty()) {
            jellyfinClient.getGenres().onSuccess { genres ->
                availableGenres = genres.map { it.name }
                updateState { it.copy(availableGenres = availableGenres) }
            }
        }

        val enabled = preferences.enabledGenres.value
        val genresToLoad = if (enabled.isNotEmpty()) {
            enabled
        } else {
            availableGenres.take(3)
        }

        val genreData = mutableMapOf<String, List<JellyfinItem>>()
        genresToLoad.forEach { genreName ->
            jellyfinClient.getItemsByGenre(genreName, limit = 12).onSuccess { items ->
                if (items.isNotEmpty()) {
                    genreData[genreName] = items
                }
            }
        }
        updateState { it.copy(genreRowsData = genreData) }
    }

    private suspend fun updateState(transform: (HomeUiState) -> HomeUiState) {
        stateMutex.withLock {
            _prefetchedState.value?.let { current ->
                _prefetchedState.value = transform(current)
            }
        }
    }

    /**
     * Consume the prefetched state (one-shot).
     * After consumption, HomeViewModel owns the data and this returns null.
     */
    fun consumeState(): HomeUiState? {
        if (consumed) return null
        consumed = true
        return _prefetchedState.value
    }
}
