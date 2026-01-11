package dev.jausc.myflix.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.jausc.myflix.core.common.HeroContentBuilder
import dev.jausc.myflix.core.common.model.JellyfinGenre
import dev.jausc.myflix.core.common.model.JellyfinItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Background polling interval in milliseconds */
private const val POLL_INTERVAL_MS = 30_000L

/**
 * Configuration for home screen content loading.
 */
data class HomeScreenConfig(
    val showSeasonPremieres: Boolean = true,
    val showGenreRows: Boolean = false,
    val enabledGenres: List<String> = emptyList(),
    val showCollections: Boolean = true,
    val pinnedCollections: List<String> = emptyList(),
    val showSuggestions: Boolean = true,
    val hideWatchedFromRecent: Boolean = false,
    val heroConfig: HeroContentBuilder.Config = HeroContentBuilder.defaultConfig,
)

/**
 * State holder for HomeScreen.
 * Manages content loading, caching, and refresh across TV and mobile platforms.
 */
@Stable
class HomeScreenState(
    private val loader: HomeContentLoader,
    private val scope: CoroutineScope,
    private val config: HomeScreenConfig,
) {
    // Library state
    var libraries by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    // Content rows
    var continueWatching by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var nextUp by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var recentMovies by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var recentShows by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var recentEpisodes by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var seasonPremieres by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var collections by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var suggestions by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    // Pinned collections: collectionId -> (collectionName, items)
    var pinnedCollectionsData by mutableStateOf<Map<String, Pair<String, List<JellyfinItem>>>>(emptyMap())
        private set

    // Genre rows: genreName -> items
    var genreRowsData by mutableStateOf<Map<String, List<JellyfinItem>>>(emptyMap())
        private set

    var availableGenres by mutableStateOf<List<String>>(emptyList())
        private set

    // Hero section
    var featuredItems by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    // Loading/error state
    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Refresh trigger
    private var refreshTrigger = 0

    /**
     * Filtered Next Up - excludes items already in Continue Watching.
     */
    val filteredNextUp: List<JellyfinItem>
        get() {
            val continueWatchingIds = continueWatching.map { it.id }.toSet()
            return nextUp.filter { it.id !in continueWatchingIds }
        }

    /**
     * Filtered recent episodes (if hideWatchedFromRecent is enabled).
     */
    val filteredRecentEpisodes: List<JellyfinItem>
        get() = if (config.hideWatchedFromRecent) {
            recentEpisodes.filter { it.userData?.played != true }
        } else {
            recentEpisodes
        }

    /**
     * Filtered recent shows (if hideWatchedFromRecent is enabled).
     */
    val filteredRecentShows: List<JellyfinItem>
        get() = if (config.hideWatchedFromRecent) {
            recentShows.filter { it.userData?.played != true }
        } else {
            recentShows
        }

    /**
     * Filtered recent movies (if hideWatchedFromRecent is enabled).
     */
    val filteredRecentMovies: List<JellyfinItem>
        get() = if (config.hideWatchedFromRecent) {
            recentMovies.filter { it.userData?.played != true }
        } else {
            recentMovies
        }

    /**
     * Filtered featured items (if hideWatchedFromRecent is enabled).
     */
    val filteredFeaturedItems: List<JellyfinItem>
        get() = if (config.hideWatchedFromRecent) {
            featuredItems.filter { it.userData?.played != true }
        } else {
            featuredItems
        }

    /**
     * Whether content is ready to display.
     */
    val contentReady: Boolean
        get() = !isLoading && featuredItems.isNotEmpty()

    /**
     * Clear error message.
     */
    fun clearError() {
        error = null
    }

    /**
     * Trigger a refresh of all content.
     */
    fun refresh() {
        refreshTrigger++
        scope.launch {
            loadContent(showLoading = false)
        }
    }

    /**
     * Mark an item as watched/unwatched and refresh content.
     */
    fun setPlayed(itemId: String, played: Boolean) {
        scope.launch {
            loader.setPlayed(itemId, played)
            loadContent(showLoading = false)
        }
    }

    /**
     * Toggle an item's favorite status and refresh content.
     */
    fun setFavorite(itemId: String, favorite: Boolean) {
        scope.launch {
            loader.setFavorite(itemId, favorite)
            loadContent(showLoading = false)
        }
    }

    /**
     * Load all home screen content.
     */
    internal suspend fun loadContent(showLoading: Boolean = false) {
        if (showLoading) isLoading = true
        error = null

        // Clear cache to get fresh data
        loader.clearCache()

        // Get libraries first
        loader.getLibraries()
            .onSuccess { libs ->
                libraries = libs

                // Find Movies and Shows libraries
                val moviesLibraryId = loader.findMoviesLibraryId(libs)
                val showsLibraryId = loader.findShowsLibraryId(libs)

                // Get latest movies
                if (moviesLibraryId != null) {
                    loader.getLatestMovies(moviesLibraryId, limit = 12).onSuccess { items ->
                        recentMovies = items
                    }
                }

                // Get latest series
                if (showsLibraryId != null) {
                    loader.getLatestSeries(showsLibraryId, limit = 12).onSuccess { items ->
                        recentShows = items
                    }
                }

                // Get latest episodes
                if (showsLibraryId != null) {
                    loader.getLatestEpisodes(showsLibraryId, limit = 12).onSuccess { items ->
                        recentEpisodes = items
                    }
                }
            }
            .onFailure { e ->
                error = "Failed to load libraries: ${e.message ?: "Unknown error"}"
            }

        // Get Next Up
        loader.getNextUp(limit = 12)
            .onSuccess { items -> nextUp = items }
            .onFailure { /* Non-critical */ }

        // Get Continue Watching
        loader.getContinueWatching(limit = 12)
            .onSuccess { items -> continueWatching = items }
            .onFailure { /* Non-critical */ }

        // Get Season Premieres
        if (config.showSeasonPremieres) {
            loader.getUpcomingEpisodes(limit = 12)
                .onSuccess { items -> seasonPremieres = items }
                .onFailure { /* Non-critical */ }
        }

        // Get Collections
        if (config.showCollections) {
            loader.getCollections(limit = 50)
                .onSuccess { items ->
                    collections = items

                    // Load pinned collection items
                    if (config.pinnedCollections.isNotEmpty()) {
                        val pinnedData = linkedMapOf<String, Pair<String, List<JellyfinItem>>>()
                        config.pinnedCollections.forEach { collectionId ->
                            loader.getItem(collectionId).onSuccess { collection ->
                                loader.getCollectionItems(collectionId, limit = 12).onSuccess { collectionItems ->
                                    if (collectionItems.isNotEmpty()) {
                                        pinnedData[collectionId] = Pair(collection.name, collectionItems)
                                    }
                                }
                            }
                        }
                        pinnedCollectionsData = pinnedData
                    }
                }
                .onFailure { /* Non-critical */ }
        }

        // Get Suggestions
        if (config.showSuggestions) {
            loader.getSuggestions(limit = 12)
                .onSuccess { items -> suggestions = items }
                .onFailure { /* Non-critical */ }
        }

        // Get Genre Rows
        if (config.showGenreRows) {
            // Get available genres if not loaded
            if (availableGenres.isEmpty()) {
                loader.getGenres()
                    .onSuccess { genres ->
                        availableGenres = genres.map { it.name }
                    }
            }

            // Load items for enabled genres
            val genresToLoad = if (config.enabledGenres.isNotEmpty()) {
                config.enabledGenres
            } else {
                availableGenres.take(3)
            }

            val newGenreData = mutableMapOf<String, List<JellyfinItem>>()
            genresToLoad.forEach { genreName ->
                loader.getItemsByGenre(genreName, limit = 12)
                    .onSuccess { items ->
                        if (items.isNotEmpty()) {
                            newGenreData[genreName] = items
                        }
                    }
            }
            genreRowsData = newGenreData
        }

        // Build featured items for hero section
        featuredItems = HeroContentBuilder.buildFeaturedItems(
            continueWatching = continueWatching,
            nextUp = nextUp,
            recentMovies = recentMovies,
            recentShows = recentShows,
            config = config.heroConfig,
        )

        isLoading = false
    }

    /**
     * Start background polling for updates.
     */
    internal fun startBackgroundPolling() {
        scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                loadContent(showLoading = false)
            }
        }
    }
}

/**
 * Loader interface for home screen content.
 * Abstracts the JellyfinClient for dependency injection.
 */
interface HomeContentLoader {
    suspend fun clearCache()
    suspend fun getLibraries(): Result<List<JellyfinItem>>
    fun findMoviesLibraryId(libraries: List<JellyfinItem>): String?
    fun findShowsLibraryId(libraries: List<JellyfinItem>): String?
    suspend fun getLatestMovies(libraryId: String, limit: Int): Result<List<JellyfinItem>>
    suspend fun getLatestSeries(libraryId: String, limit: Int): Result<List<JellyfinItem>>
    suspend fun getLatestEpisodes(libraryId: String, limit: Int): Result<List<JellyfinItem>>
    suspend fun getNextUp(limit: Int): Result<List<JellyfinItem>>
    suspend fun getContinueWatching(limit: Int): Result<List<JellyfinItem>>
    suspend fun getUpcomingEpisodes(limit: Int): Result<List<JellyfinItem>>
    suspend fun getCollections(limit: Int): Result<List<JellyfinItem>>
    suspend fun getItem(itemId: String): Result<JellyfinItem>
    suspend fun getCollectionItems(collectionId: String, limit: Int): Result<List<JellyfinItem>>
    suspend fun getSuggestions(limit: Int): Result<List<JellyfinItem>>
    suspend fun getGenres(): Result<List<JellyfinGenre>>
    suspend fun getItemsByGenre(genreName: String, limit: Int): Result<List<JellyfinItem>>
    suspend fun setPlayed(itemId: String, played: Boolean)
    suspend fun setFavorite(itemId: String, favorite: Boolean)
}

/**
 * Creates and remembers a [HomeScreenState].
 *
 * @param loader Loader for home content
 * @param config Configuration for content display
 * @return A [HomeScreenState] for managing home screen UI state
 */
@Composable
fun rememberHomeScreenState(
    loader: HomeContentLoader,
    config: HomeScreenConfig = HomeScreenConfig(),
): HomeScreenState {
    val scope = rememberCoroutineScope()
    val state = remember(config) {
        HomeScreenState(loader, scope, config)
    }

    // Initial load
    LaunchedEffect(Unit) {
        state.loadContent(showLoading = true)
    }

    // Background polling
    LaunchedEffect(Unit) {
        state.startBackgroundPolling()
    }

    return state
}
