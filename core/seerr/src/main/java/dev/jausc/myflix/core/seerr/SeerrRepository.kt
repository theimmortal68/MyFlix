package dev.jausc.myflix.core.seerr

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository layer for Seerr operations.
 *
 * Provides:
 * - Caching of frequently accessed data (genres, servers, sliders)
 * - Clean interface for ViewModels (delegates to SeerrClient)
 * - State management via StateFlow
 * - Error handling
 *
 * Usage:
 * ```kotlin
 * val repository = SeerrRepository(seerrClient)
 * repository.initialize() // Load cached data if authenticated
 *
 * // Observe state
 * repository.movieGenres.collect { genres -> ... }
 * repository.isAuthenticated.collect { authenticated -> ... }
 *
 * // Make requests
 * repository.requestMovie(tmdbId)
 * ```
 */
@Suppress("TooManyFunctions")
class SeerrRepository(
    private val client: SeerrClient,
) {
    // ========================================================================
    // Cached State
    // ========================================================================

    private val _movieGenres = MutableStateFlow<List<SeerrGenre>>(emptyList())
    val movieGenres: StateFlow<List<SeerrGenre>> = _movieGenres.asStateFlow()

    private val _tvGenres = MutableStateFlow<List<SeerrGenre>>(emptyList())
    val tvGenres: StateFlow<List<SeerrGenre>> = _tvGenres.asStateFlow()

    private val _radarrServers = MutableStateFlow<List<SeerrRadarrServer>>(emptyList())
    val radarrServers: StateFlow<List<SeerrRadarrServer>> = _radarrServers.asStateFlow()

    private val _sonarrServers = MutableStateFlow<List<SeerrSonarrServer>>(emptyList())
    val sonarrServers: StateFlow<List<SeerrSonarrServer>> = _sonarrServers.asStateFlow()

    private val _discoverSliders = MutableStateFlow<List<SeerrDiscoverSlider>>(emptyList())
    val discoverSliders: StateFlow<List<SeerrDiscoverSlider>> = _discoverSliders.asStateFlow()

    private val _currentUser = MutableStateFlow<SeerrUser?>(null)
    val currentUser: StateFlow<SeerrUser?> = _currentUser.asStateFlow()

    // ========================================================================
    // Delegated State from Client
    // ========================================================================

    /** Authentication state from client */
    val isAuthenticated: StateFlow<Boolean> = client.isAuthenticated

    /** Base URL of connected server */
    val baseUrl: String?
        get() = client.baseUrl

    /** Session cookie for persistent auth */
    val sessionCookie: String?
        get() = client.sessionCookie

    /** API key if using API key auth */
    val apiKey: String?
        get() = client.apiKey

    // ========================================================================
    // Initialization
    // ========================================================================

    /**
     * Initialize repository by testing connection and loading cached data.
     * Call this after connecting to a server or restoring auth.
     */
    suspend fun initialize(): Result<Unit> = runCatching {
        if (client.isAuthenticated.value) {
            loadCachedData()
        }
    }

    /**
     * Load all cached data from server in parallel.
     * Called automatically after successful login.
     */
    private suspend fun loadCachedData() = coroutineScope {
        // Load all data in parallel for faster initialization
        val userDeferred = async { client.getCurrentUser() }
        val movieGenresDeferred = async { client.getMovieGenres() }
        val tvGenresDeferred = async { client.getTVGenres() }
        val discoverDeferred = async { client.getDiscoverSettings() }
        val radarrDeferred = async { client.getRadarrServers() }
        val sonarrDeferred = async { client.getSonarrServers() }

        // Await all results and update state
        userDeferred.await().onSuccess { _currentUser.value = it }
        movieGenresDeferred.await().onSuccess { _movieGenres.value = it }
        tvGenresDeferred.await().onSuccess { _tvGenres.value = it }
        discoverDeferred.await().onSuccess { _discoverSliders.value = it }
        radarrDeferred.await().onSuccess { _radarrServers.value = it }
        sonarrDeferred.await().onSuccess { _sonarrServers.value = it }
    }

    /**
     * Clear all cached data.
     * Called automatically on logout.
     */
    private fun clearCachedData() {
        _currentUser.value = null
        _movieGenres.value = emptyList()
        _tvGenres.value = emptyList()
        _discoverSliders.value = emptyList()
        _radarrServers.value = emptyList()
        _sonarrServers.value = emptyList()
    }

    /**
     * Refresh all cached data.
     */
    suspend fun refreshCachedData() {
        if (client.isAuthenticated.value) {
            loadCachedData()
        }
    }

    // ========================================================================
    // Connection & Authentication
    // ========================================================================

    /**
     * Attempt to detect Seerr server based on Jellyfin URL.
     */
    suspend fun detectServer(jellyfinHost: String): Result<String> =
        client.detectServer(jellyfinHost)

    /**
     * Connect to a specific Seerr server URL.
     */
    suspend fun connectToServer(serverUrl: String): Result<SeerrStatus> =
        client.connectToServer(serverUrl)

    /**
     * Get server status.
     */
    suspend fun getStatus(): Result<SeerrStatus> = client.getStatus()

    /**
     * Authenticate using Jellyfin credentials.
     * Loads cached data on successful login.
     */
    suspend fun loginWithJellyfin(
        username: String,
        password: String,
        jellyfinHost: String? = null,
    ): Result<SeerrUser> {
        val result = client.loginWithJellyfin(username, password, jellyfinHost)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
            loadCachedData()
        }
        return result
    }

    /**
     * Authenticate using local account credentials.
     * Loads cached data on successful login.
     */
    suspend fun loginWithLocal(email: String, password: String): Result<SeerrUser> {
        val result = client.loginWithLocal(email, password)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
            loadCachedData()
        }
        return result
    }

    /**
     * Authenticate using API key.
     * Loads cached data on successful login.
     */
    suspend fun loginWithApiKey(key: String): Result<SeerrUser> {
        val result = client.loginWithApiKey(key)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
            loadCachedData()
        }
        return result
    }

    /**
     * Authenticate using stored session cookie.
     * Loads cached data on successful login.
     */
    suspend fun loginWithSessionCookie(cookie: String): Result<SeerrUser> {
        val result = client.loginWithSessionCookie(cookie)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
            loadCachedData()
        }
        return result
    }

    /**
     * Logout and clear all cached data.
     */
    suspend fun logout(): Result<Unit> {
        val result = client.logout()
        clearCachedData()
        return result
    }

    // ========================================================================
    // Quick Connect Authentication
    // ========================================================================

    /**
     * Check if Quick Connect is available on the Seerr server.
     */
    suspend fun isQuickConnectAvailable(): Boolean = client.isQuickConnectAvailable()

    /**
     * Initiate Quick Connect flow.
     */
    suspend fun initiateQuickConnect(): Result<SeerrQuickConnectState> =
        client.initiateQuickConnect()

    /**
     * Check Quick Connect approval status.
     */
    suspend fun checkQuickConnectStatus(secret: String): Result<SeerrQuickConnectState> =
        client.checkQuickConnectStatus(secret)

    /**
     * Quick Connect authentication flow as a Kotlin Flow.
     * Note: Must call loadCachedData() after successful auth from this flow.
     */
    fun quickConnectFlow(): Flow<SeerrQuickConnectFlowState> = client.quickConnectFlow()

    /**
     * Called after Quick Connect completes to load cached data.
     */
    suspend fun onQuickConnectComplete() {
        _currentUser.value = client.currentUser
        loadCachedData()
    }

    // ========================================================================
    // User
    // ========================================================================

    /**
     * Get current authenticated user.
     */
    suspend fun getCurrentUser(): Result<SeerrUser> {
        val result = client.getCurrentUser()
        result.onSuccess { _currentUser.value = it }
        return result
    }

    /**
     * Get user quota information.
     */
    suspend fun getUserQuota(): Result<SeerrUserQuota> = client.getUserQuota()

    // ========================================================================
    // Discovery
    // ========================================================================

    /**
     * Get discover slider configuration.
     * Returns cached value if available.
     */
    suspend fun getDiscoverSettings(): Result<List<SeerrDiscoverSlider>> {
        // Return cached if available
        if (_discoverSliders.value.isNotEmpty()) {
            return Result.success(_discoverSliders.value)
        }
        // Otherwise fetch and cache
        val result = client.getDiscoverSettings()
        result.onSuccess { _discoverSliders.value = it }
        return result
    }

    /**
     * Get trending movies and TV shows.
     */
    suspend fun getTrending(page: Int = 1): Result<SeerrDiscoverResult> =
        client.getTrending(page)

    /**
     * Get popular movies.
     */
    suspend fun getPopularMovies(page: Int = 1): Result<SeerrDiscoverResult> =
        client.getPopularMovies(page)

    /**
     * Get popular TV shows.
     */
    suspend fun getPopularTV(page: Int = 1): Result<SeerrDiscoverResult> =
        client.getPopularTV(page)

    /**
     * Get upcoming movies.
     */
    suspend fun getUpcomingMovies(page: Int = 1): Result<SeerrDiscoverResult> =
        client.getUpcomingMovies(page)

    /**
     * Get upcoming TV shows.
     */
    suspend fun getUpcomingTV(page: Int = 1): Result<SeerrDiscoverResult> =
        client.getUpcomingTV(page)

    /**
     * Get movie genres.
     * Returns cached value if available.
     */
    suspend fun getMovieGenres(): Result<List<SeerrGenre>> {
        if (_movieGenres.value.isNotEmpty()) {
            return Result.success(_movieGenres.value)
        }
        val result = client.getMovieGenres()
        result.onSuccess { _movieGenres.value = it }
        return result
    }

    /**
     * Get TV genres.
     * Returns cached value if available.
     */
    suspend fun getTVGenres(): Result<List<SeerrGenre>> {
        if (_tvGenres.value.isNotEmpty()) {
            return Result.success(_tvGenres.value)
        }
        val result = client.getTVGenres()
        result.onSuccess { _tvGenres.value = it }
        return result
    }

    /**
     * Discover movies with optional filters.
     */
    suspend fun discoverMovies(
        page: Int = 1,
        genreId: Int? = null,
        sortBy: String = "popularity.desc",
        year: Int? = null,
    ): Result<SeerrDiscoverResult> = client.discoverMovies(page, genreId, sortBy, year)

    /**
     * Discover movies with raw query parameters.
     */
    suspend fun discoverMoviesWithParams(
        params: Map<String, String>,
        page: Int = 1,
    ): Result<SeerrDiscoverResult> = client.discoverMoviesWithParams(params, page)

    /**
     * Discover TV shows with optional filters.
     */
    suspend fun discoverTV(
        page: Int = 1,
        genreId: Int? = null,
        sortBy: String = "popularity.desc",
        year: Int? = null,
    ): Result<SeerrDiscoverResult> = client.discoverTV(page, genreId, sortBy, year)

    /**
     * Discover TV shows with raw query parameters.
     */
    suspend fun discoverTVWithParams(
        params: Map<String, String>,
        page: Int = 1,
    ): Result<SeerrDiscoverResult> = client.discoverTVWithParams(params, page)

    // ========================================================================
    // Search
    // ========================================================================

    /**
     * Search for movies and TV shows.
     */
    suspend fun search(query: String, page: Int = 1): Result<SeerrDiscoverResult> =
        client.search(query, page)

    /**
     * Search for movies only.
     */
    suspend fun searchMovies(query: String, page: Int = 1): Result<SeerrDiscoverResult> =
        client.searchMovies(query, page)

    /**
     * Search for TV shows only.
     */
    suspend fun searchTV(query: String, page: Int = 1): Result<SeerrDiscoverResult> =
        client.searchTV(query, page)

    // ========================================================================
    // Media Details
    // ========================================================================

    /**
     * Get movie details by TMDB ID.
     */
    suspend fun getMovie(tmdbId: Int): Result<SeerrMedia> = client.getMovie(tmdbId)

    /**
     * Get TV show details by TMDB ID.
     */
    suspend fun getTVShow(tmdbId: Int): Result<SeerrMedia> = client.getTVShow(tmdbId)

    /**
     * Get collection details by TMDB collection ID.
     */
    suspend fun getCollection(collectionId: Int): Result<SeerrCollection> =
        client.getCollection(collectionId)

    /**
     * Get TV show season details.
     */
    suspend fun getTVSeason(tmdbId: Int, seasonNumber: Int): Result<SeerrSeason> =
        client.getTVSeason(tmdbId, seasonNumber)

    /**
     * Get similar movies.
     */
    suspend fun getSimilarMovies(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> =
        client.getSimilarMovies(tmdbId, page)

    /**
     * Get similar TV shows.
     */
    suspend fun getSimilarTV(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> =
        client.getSimilarTV(tmdbId, page)

    /**
     * Get movie recommendations.
     */
    suspend fun getMovieRecommendations(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> =
        client.getMovieRecommendations(tmdbId, page)

    /**
     * Get TV recommendations.
     */
    suspend fun getTVRecommendations(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> =
        client.getTVRecommendations(tmdbId, page)

    /**
     * Get combined ratings for a movie.
     */
    suspend fun getMovieRatings(tmdbId: Int): Result<SeerrRatingResponse> =
        client.getMovieRatings(tmdbId)

    /**
     * Get Rotten Tomatoes ratings for a TV show.
     */
    suspend fun getTVRatings(tmdbId: Int): Result<SeerrRottenTomatoesRating> =
        client.getTVRatings(tmdbId)

    // ========================================================================
    // Person/Actor
    // ========================================================================

    /**
     * Get person details by TMDB person ID.
     */
    suspend fun getPerson(personId: Int): Result<SeerrPerson> = client.getPerson(personId)

    /**
     * Get person's combined credits.
     */
    suspend fun getPersonCombinedCredits(personId: Int): Result<SeerrPersonCredits> =
        client.getPersonCombinedCredits(personId)

    // ========================================================================
    // Requests
    // ========================================================================

    /**
     * Get current user's requests.
     */
    suspend fun getMyRequests(
        page: Int = 1,
        pageSize: Int = 20,
        filter: String = "all",
        sort: String = "added",
        sortDirection: String = "desc",
    ): Result<SeerrRequestResult> =
        client.getMyRequests(page, pageSize, filter, sort, sortDirection)

    /**
     * Get all requests (admin).
     */
    suspend fun getAllRequests(
        page: Int = 1,
        pageSize: Int = 20,
        filter: String = "all",
        sort: String = "added",
        sortDirection: String = "desc",
    ): Result<SeerrRequestResult> =
        client.getAllRequests(page, pageSize, filter, sort, sortDirection)

    /**
     * Get a specific request by ID.
     */
    suspend fun getRequest(requestId: Int): Result<SeerrRequest> = client.getRequest(requestId)

    /**
     * Create a new media request.
     */
    suspend fun createRequest(request: CreateMediaRequest): Result<SeerrRequest> =
        client.createRequest(request)

    /**
     * Request a movie by TMDB ID.
     */
    suspend fun requestMovie(tmdbId: Int, is4k: Boolean = false): Result<SeerrRequest> =
        client.requestMovie(tmdbId, is4k)

    /**
     * Request a TV show by TMDB ID.
     */
    suspend fun requestTVShow(
        tmdbId: Int,
        seasons: List<Int>? = null,
        is4k: Boolean = false,
    ): Result<SeerrRequest> = client.requestTVShow(tmdbId, seasons, is4k)

    /**
     * Cancel/delete a request.
     */
    suspend fun cancelRequest(requestId: Int): Result<Unit> = client.cancelRequest(requestId)

    /**
     * Delete media from Jellyseerr/Overseerr.
     */
    suspend fun deleteMedia(mediaId: Int): Result<Unit> = client.deleteMedia(mediaId)

    /**
     * Approve a request (admin).
     */
    suspend fun approveRequest(requestId: Int): Result<SeerrRequest> =
        client.approveRequest(requestId)

    /**
     * Decline a request (admin).
     */
    suspend fun declineRequest(requestId: Int): Result<Unit> = client.declineRequest(requestId)

    // ========================================================================
    // Blacklist
    // ========================================================================

    /**
     * Add media to blacklist.
     */
    suspend fun addToBlacklist(tmdbId: Int, mediaType: String): Result<Unit> =
        client.addToBlacklist(tmdbId, mediaType)

    /**
     * Remove media from blacklist.
     */
    suspend fun removeFromBlacklist(tmdbId: Int): Result<Unit> =
        client.removeFromBlacklist(tmdbId)

    // ========================================================================
    // Issues
    // ========================================================================

    /**
     * Get list of issues.
     */
    suspend fun getIssues(
        filter: String? = null,
        sort: String? = null,
        take: Int = 20,
        skip: Int = 0,
    ): Result<SeerrIssueListResponse> = client.getIssues(filter, sort, take, skip)

    /**
     * Create a new issue.
     */
    suspend fun createIssue(
        mediaId: Int,
        issueType: Int,
        message: String,
        problemSeason: Int? = null,
        problemEpisode: Int? = null,
    ): Result<SeerrIssue> =
        client.createIssue(mediaId, issueType, message, problemSeason, problemEpisode)

    /**
     * Get an issue by ID.
     */
    suspend fun getIssue(issueId: Int): Result<SeerrIssue> = client.getIssue(issueId)

    /**
     * Add a comment to an issue.
     */
    suspend fun addIssueComment(issueId: Int, message: String): Result<SeerrIssueComment> =
        client.addIssueComment(issueId, message)

    /**
     * Resolve an issue.
     */
    suspend fun resolveIssue(issueId: Int): Result<SeerrIssue> = client.resolveIssue(issueId)

    /**
     * Delete an issue.
     */
    suspend fun deleteIssue(issueId: Int): Result<Unit> = client.deleteIssue(issueId)

    // ========================================================================
    // Settings & Service Configuration
    // ========================================================================

    /**
     * Get public server settings.
     */
    suspend fun getPublicSettings(): Result<SeerrPublicSettings> = client.getPublicSettings()

    /**
     * Get Radarr server configurations.
     * Returns cached value if available.
     */
    suspend fun getRadarrServers(): Result<List<SeerrRadarrServer>> {
        if (_radarrServers.value.isNotEmpty()) {
            return Result.success(_radarrServers.value)
        }
        val result = client.getRadarrServers()
        result.onSuccess { _radarrServers.value = it }
        return result
    }

    /**
     * Get Sonarr server configurations.
     * Returns cached value if available.
     */
    suspend fun getSonarrServers(): Result<List<SeerrSonarrServer>> {
        if (_sonarrServers.value.isNotEmpty()) {
            return Result.success(_sonarrServers.value)
        }
        val result = client.getSonarrServers()
        result.onSuccess { _sonarrServers.value = it }
        return result
    }

    // ========================================================================
    // Image URLs
    // ========================================================================

    /**
     * Build TMDB poster URL.
     */
    fun getPosterUrl(posterPath: String?, size: String = "w500"): String? =
        client.getPosterUrl(posterPath, size)

    /**
     * Build TMDB backdrop URL.
     */
    fun getBackdropUrl(backdropPath: String?, size: String = "w1280"): String? =
        client.getBackdropUrl(backdropPath, size)

    /**
     * Build TMDB profile URL.
     */
    fun getProfileUrl(profilePath: String?, size: String = "w185"): String? =
        client.getProfileUrl(profilePath, size)

    /**
     * Build TMDB still URL.
     */
    fun getStillUrl(stillPath: String?, size: String = "w300"): String? =
        client.getStillUrl(stillPath, size)

    // ========================================================================
    // Utilities
    // ========================================================================

    /**
     * Reset repository and client state.
     */
    fun reset() {
        clearCachedData()
        client.reset()
    }
}
