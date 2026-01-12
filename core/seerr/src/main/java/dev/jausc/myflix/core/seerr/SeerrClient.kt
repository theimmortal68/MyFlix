package dev.jausc.myflix.core.seerr

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.net.URL

/**
 * Seerr/Jellyseerr API client.
 *
 * Features:
 * - Auto-detection of Seerr on same host as Jellyfin server
 * - Authentication via Jellyfin credentials or API key
 * - Full discovery, search, and request management
 * - TMDB image URL building
 */
@Suppress(
    "TooManyFunctions",
    "LargeClass",
    "StringLiteralDuplication",
    "MagicNumber",
    "LabeledExpression",
    "CognitiveComplexMethod",
)
class SeerrClient(
    httpClient: HttpClient? = null, // Allow injection for testing
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false // Don't send null values in request bodies
    }

    private val httpClient = httpClient ?: HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
                followSslRedirects(true)
            }
        }
        install(ContentNegotiation) { json(this@SeerrClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        install(HttpCookies) // Cookie storage for session auth
        defaultRequest {
            contentType(ContentType.Application.Json)
            // Include session cookie in all requests if set (for restored sessions)
            this@SeerrClient.sessionCookie?.let { cookie ->
                header(HttpHeaders.Cookie, cookie)
            }
        }
    }

    var baseUrl: String? = null
        private set
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    var currentUser: SeerrUser? = null
        private set
    var apiKey: String? = null
        private set
    var sessionCookie: String? = null
        private set

    companion object {
        /** Common Seerr ports to try during auto-detection */
        val COMMON_PORTS = listOf(5055, 5056)

        /** Common Seerr subdomain replacements for FQDN detection */
        val SEERR_SUBDOMAINS = listOf("seerr", "jellyseerr", "overseerr")

        /** TMDB image base URLs */
        private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p"
    }

    // ========================================================================
    // Connection & Authentication
    // ========================================================================

    /**
     * Attempt to detect Seerr server based on Jellyfin URL.
     *
     * Detection strategy:
     * 1. For FQDNs (e.g., jellyfin.myflix.media or jellyfin.local): Try subdomain substitution
     *    - seerr.myflix.media, jellyseerr.myflix.media, overseerr.myflix.media
     *    - seerr.local, jellyseerr.local, overseerr.local
     * 2. For IP addresses: Try common ports (5055, 5056)
     *
     * @param jellyfinHost The Jellyfin server URL
     * @return Result containing the detected Seerr URL, or failure if not found
     */
    suspend fun detectServer(jellyfinHost: String): Result<String> = runCatching {
        val host = extractHost(jellyfinHost)
        val scheme = if (jellyfinHost.startsWith("https")) "https" else "http"
        val isIp = isIpAddress(host)

        // For FQDNs (any hostname with dots that isn't an IP), try subdomain substitution
        // This works for both jellyfin.domain.tld and jellyfin.local
        if (!isIp && host.contains(".")) {
            // Extract base domain (e.g., "myflix.media" from "jellyfin.myflix.media"
            // or "local" from "jellyfin.local")
            val parts = host.split(".")
            val baseDomain = parts.drop(1).joinToString(".")

            for (subdomain in SEERR_SUBDOMAINS) {
                val testUrl = "$scheme://$subdomain.$baseDomain"
                if (tryConnectToSeerr(testUrl)) {
                    return@runCatching testUrl
                }
            }
        }

        // For IP addresses: Try common Seerr ports
        if (isIp) {
            // Strip any existing port from IP-based host
            val baseHost = host.substringBefore(":")
            for (port in COMMON_PORTS) {
                val testUrl = "$scheme://$baseHost:$port"
                if (tryConnectToSeerr(testUrl)) {
                    return@runCatching testUrl
                }
            }
        }

        throw Exception("Seerr server not found")
    }

    /**
     * Try to connect to a Seerr server at the given URL.
     * Returns true if successful and sets baseUrl.
     */
    private suspend fun tryConnectToSeerr(testUrl: String): Boolean {
        return try {
            val response = httpClient.get("$testUrl/api/v1/status") {
                timeout { requestTimeoutMillis = 5_000 }
            }
            if (response.status.isSuccess()) {
                @Suppress("UNUSED_VARIABLE")
                val status: SeerrStatus = response.body()
                baseUrl = testUrl
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if a string is an IP address (IPv4 or IPv6).
     */
    private fun isIpAddress(host: String): Boolean {
        // IPv4 pattern
        val ipv4Regex = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
        if (ipv4Regex.matches(host)) return true

        // IPv6 pattern (simplified - contains colons)
        if (host.contains(":")) return true

        return false
    }

    /**
     * Connect to a specific Seerr server URL.
     */
    suspend fun connectToServer(serverUrl: String): Result<SeerrStatus> = runCatching {
        val response = httpClient.get("$serverUrl/api/v1/status")
        if (!response.status.isSuccess()) {
            throw Exception("Failed to connect: ${response.status}")
        }
        val status: SeerrStatus = response.body()
        baseUrl = serverUrl
        status
    }

    /**
     * Get server status.
     */
    suspend fun getStatus(): Result<SeerrStatus> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/status")
        response.body()
    }

    /**
     * Authenticate using Jellyfin credentials.
     * This is the preferred method when using Jellyseerr with a Jellyfin backend.
     *
     * Note: hostname is not sent because Jellyseerr already knows which Jellyfin
     * server to authenticate against once it's configured.
     *
     * After successful login, stores the API key for persistent authentication.
     */
    suspend fun loginWithJellyfin(
        username: String,
        password: String,
        @Suppress("UNUSED_PARAMETER") jellyfinHost: String? = null, // Kept for API compatibility
    ): Result<SeerrUser> = runCatching {
        requireBaseUrl()

        val response = httpClient.post("$baseUrl/api/v1/auth/jellyfin") {
            setBody(
                SeerrJellyfinAuthRequest(
                    username = username,
                    password = password,
                    // Don't pass hostname - Seerr already knows the Jellyfin server
                ),
            )
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("Login failed: ${response.status} - $errorBody")
        }

        // Extract session cookie for persistent auth
        val setCookieHeaders = response.headers.getAll(HttpHeaders.SetCookie)
        setCookieHeaders?.forEach { cookie ->
            if (cookie.startsWith("connect.sid=")) {
                sessionCookie = cookie.substringBefore(";")
            }
        }

        val user: SeerrUser = response.body()
        _isAuthenticated.value = true
        currentUser = user
        apiKey = user.apiKey // Store API key for persistent auth (if provided)
        user
    }

    /**
     * Authenticate using a stored session cookie.
     * This validates the cookie by fetching the current user.
     */
    suspend fun loginWithSessionCookie(cookie: String): Result<SeerrUser> = runCatching {
        requireBaseUrl()

        val response = httpClient.get("$baseUrl/api/v1/auth/me") {
            header(HttpHeaders.Cookie, cookie)
        }

        if (!response.status.isSuccess()) {
            throw Exception("Session cookie authentication failed: ${response.status}")
        }

        val user: SeerrUser = response.body()
        _isAuthenticated.value = true
        currentUser = user
        sessionCookie = cookie
        user
    }

    /**
     * Authenticate using API key (for persistent login across sessions).
     * This validates the API key by fetching the current user.
     */
    suspend fun loginWithApiKey(key: String): Result<SeerrUser> = runCatching {
        requireBaseUrl()

        val response = httpClient.get("$baseUrl/api/v1/auth/me") {
            header("X-Api-Key", key)
        }

        if (!response.status.isSuccess()) {
            throw Exception("API key authentication failed: ${response.status}")
        }

        val user: SeerrUser = response.body()
        _isAuthenticated.value = true
        currentUser = user
        apiKey = key
        user
    }

    /**
     * Authenticate using local account credentials.
     */
    suspend fun loginWithLocal(email: String, password: String): Result<SeerrUser> = runCatching {
        requireBaseUrl()

        val response = httpClient.post("$baseUrl/api/v1/auth/local") {
            setBody(SeerrLocalAuthRequest(email, password))
        }

        if (!response.status.isSuccess()) {
            throw Exception("Login failed: ${response.status}")
        }

        val user: SeerrUser = response.body()
        _isAuthenticated.value = true
        currentUser = user
        user
    }

    // ========================================================================
    // Quick Connect Authentication (requires Seerr with PR #2212)
    // ========================================================================

    /**
     * Check if Quick Connect is available on the Seerr server.
     * This feature requires Seerr with PR #2212 merged.
     */
    suspend fun isQuickConnectAvailable(): Boolean {
        requireBaseUrl()
        return try {
            // Check if endpoint exists by making a test request
            // If server returns 404, feature is not available
            val response = httpClient.post("$baseUrl/api/v1/auth/jellyfin/quickconnect/initiate") {
                timeout { requestTimeoutMillis = 5_000 }
            }
            // 404 = not available, other statuses (including errors) = available
            response.status.value != 404
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Initiate Quick Connect flow.
     * Returns a code that the user must approve on their Jellyfin server.
     */
    suspend fun initiateQuickConnect(): Result<SeerrQuickConnectState> = runCatching {
        requireBaseUrl()
        val response = httpClient.post("$baseUrl/api/v1/auth/jellyfin/quickconnect/initiate")
        if (!response.status.isSuccess()) {
            throw Exception("Failed to initiate Quick Connect: ${response.status}")
        }
        response.body()
    }

    /**
     * Check Quick Connect approval status.
     * Called in a polling loop until authenticated becomes true.
     */
    suspend fun checkQuickConnectStatus(secret: String): Result<SeerrQuickConnectState> = runCatching {
        requireBaseUrl()
        val response = httpClient.post("$baseUrl/api/v1/auth/jellyfin/quickconnect/authorize") {
            setBody(mapOf("secret" to secret))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Quick Connect status check failed: ${response.status}")
        }
        response.body()
    }

    /**
     * Quick Connect authentication flow as a Kotlin Flow.
     * Polls every 3 seconds until authenticated or error.
     *
     * Usage:
     * ```
     * seerrClient.quickConnectFlow().collect { state ->
     *     when (state) {
     *         is SeerrQuickConnectFlowState.WaitingForApproval -> showCode(state.code)
     *         is SeerrQuickConnectFlowState.Authenticated -> onSuccess(state.user)
     *         is SeerrQuickConnectFlowState.Error -> showError(state.message)
     *         // ...
     *     }
     * }
     * ```
     */
    fun quickConnectFlow(): Flow<SeerrQuickConnectFlowState> = flow {
        emit(SeerrQuickConnectFlowState.Initializing)

        // Check if Quick Connect is available
        if (!isQuickConnectAvailable()) {
            emit(SeerrQuickConnectFlowState.NotAvailable)
            return@flow
        }

        // Initiate Quick Connect
        val initResult = initiateQuickConnect()
        if (initResult.isFailure) {
            emit(
                SeerrQuickConnectFlowState.Error(
                    initResult.exceptionOrNull()?.message ?: "Failed to initiate Quick Connect",
                ),
            )
            return@flow
        }

        var state = initResult.getOrThrow()
        emit(SeerrQuickConnectFlowState.WaitingForApproval(state.code, state.secret))

        // Poll for approval (3-second interval, matches Jellyfin implementation)
        while (!state.authenticated) {
            delay(3000)

            val statusResult = checkQuickConnectStatus(state.secret)
            if (statusResult.isFailure) {
                emit(
                    SeerrQuickConnectFlowState.Error(
                        statusResult.exceptionOrNull()?.message ?: "Quick Connect check failed",
                    ),
                )
                return@flow
            }

            state = statusResult.getOrThrow()

            if (!state.authenticated) {
                // Re-emit waiting state (in case UI needs to update)
                emit(SeerrQuickConnectFlowState.WaitingForApproval(state.code, state.secret))
            }
        }

        emit(SeerrQuickConnectFlowState.Authenticating)

        // Fetch authenticated user to get API key
        val userResult = getCurrentUser()
        if (userResult.isFailure) {
            emit(
                SeerrQuickConnectFlowState.Error(
                    "Quick Connect succeeded but failed to get user: ${userResult.exceptionOrNull()?.message}",
                ),
            )
            return@flow
        }

        val user = userResult.getOrThrow()
        _isAuthenticated.value = true
        currentUser = user
        apiKey = user.apiKey

        emit(SeerrQuickConnectFlowState.Authenticated(user))
    }

    /**
     * Logout and clear session.
     */
    suspend fun logout(): Result<Unit> = runCatching {
        requireBaseUrl()
        httpClient.post("$baseUrl/api/v1/auth/logout")
        _isAuthenticated.value = false
        currentUser = null
    }

    // ========================================================================
    // User
    // ========================================================================

    /**
     * Get current authenticated user.
     */
    suspend fun getCurrentUser(): Result<SeerrUser> = runCatching {
        requireAuth()
        val response = httpClient.get("$baseUrl/api/v1/auth/me")
        val user: SeerrUser = response.body()
        currentUser = user
        user
    }

    /**
     * Get user quota information.
     */
    suspend fun getUserQuota(): Result<SeerrUserQuota> = runCatching {
        requireAuth()
        val response = httpClient.get("$baseUrl/api/v1/auth/me")
        // Quota is returned as part of user object, extract it
        val user: SeerrUser = response.body()
        SeerrUserQuota(
            movie = SeerrQuotaDetails(
                limit = user.movieQuotaLimit,
                days = user.movieQuotaDays,
                used = user.requestCount ?: 0,
                remaining = user.movieQuotaRemaining,
            ),
            tv = SeerrQuotaDetails(
                limit = user.tvQuotaLimit,
                days = user.tvQuotaDays,
                used = user.requestCount ?: 0,
                remaining = user.tvQuotaRemaining,
            ),
        )
    }

    /**
     * Get user's watchlist.
     */
    suspend fun getWatchlist(page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireAuth()
        val response = httpClient.get("$baseUrl/api/v1/discover/watchlist") {
            parameter("page", page)
        }
        response.body()
    }

    // ========================================================================
    // Discovery
    // ========================================================================

    /**
     * Get discover slider configuration from server settings.
     */
    suspend fun getDiscoverSettings(): Result<List<SeerrDiscoverSlider>> = runCatching {
        requireAuth()
        val response = httpClient.get("$baseUrl/api/v1/settings/discover")
        response.body()
    }

    /**
     * Get trending movies and TV shows.
     */
    suspend fun getTrending(page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/trending") {
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Get popular movies.
     */
    suspend fun getPopularMovies(page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/movies") {
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Get popular TV shows.
     */
    suspend fun getPopularTV(page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/tv") {
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Get upcoming movies.
     */
    suspend fun getUpcomingMovies(page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/movies/upcoming") {
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Get movie genres.
     */
    suspend fun getMovieGenres(): Result<List<SeerrGenre>> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/genreslider/movie")
        response.body()
    }

    /**
     * Get TV genres.
     */
    suspend fun getTVGenres(): Result<List<SeerrGenre>> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/genreslider/tv")
        response.body()
    }

    /**
     * Discover movies with optional filters.
     */
    suspend fun discoverMovies(
        page: Int = 1,
        genreId: Int? = null,
        sortBy: String = "popularity.desc",
        year: Int? = null,
    ): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/movies") {
            parameter("page", page)
            parameter("sortBy", sortBy)
            genreId?.let { parameter("genre", it) }
            year?.let { parameter("primaryReleaseDateGte", "$it-01-01") }
            year?.let { parameter("primaryReleaseDateLte", "$it-12-31") }
        }
        response.body()
    }

    /**
     * Discover movies with raw query parameters.
     */
    suspend fun discoverMoviesWithParams(
        params: Map<String, String>,
        page: Int = 1,
    ): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/movies") {
            parameter("page", page)
            params.forEach { (key, value) -> parameter(key, value) }
        }
        response.body()
    }

    /**
     * Discover TV shows with optional filters.
     */
    suspend fun discoverTV(
        page: Int = 1,
        genreId: Int? = null,
        sortBy: String = "popularity.desc",
        year: Int? = null,
    ): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/tv") {
            parameter("page", page)
            parameter("sortBy", sortBy)
            genreId?.let { parameter("genre", it) }
            year?.let { parameter("firstAirDateGte", "$it-01-01") }
            year?.let { parameter("firstAirDateLte", "$it-12-31") }
        }
        response.body()
    }

    /**
     * Discover TV shows with raw query parameters.
     */
    suspend fun discoverTVWithParams(
        params: Map<String, String>,
        page: Int = 1,
    ): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/discover/tv") {
            parameter("page", page)
            params.forEach { (key, value) -> parameter(key, value) }
        }
        response.body()
    }

    // ========================================================================
    // Search
    // ========================================================================

    /**
     * Search for movies and TV shows.
     */
    suspend fun search(query: String, page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/search") {
            parameter("query", query)
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Search for movies only.
     */
    suspend fun searchMovies(query: String, page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        // Use combined search and filter client-side, or use discover with keyword
        val response = httpClient.get("$baseUrl/api/v1/search") {
            parameter("query", query)
            parameter("page", page)
        }
        val result: SeerrDiscoverResult = response.body()
        result.copy(results = result.results.filter { it.mediaType == "movie" })
    }

    /**
     * Search for TV shows only.
     */
    suspend fun searchTV(query: String, page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/search") {
            parameter("query", query)
            parameter("page", page)
        }
        val result: SeerrDiscoverResult = response.body()
        result.copy(results = result.results.filter { it.mediaType == "tv" })
    }

    // ========================================================================
    // Media Details
    // ========================================================================

    /**
     * Get movie details by TMDB ID.
     */
    suspend fun getMovie(tmdbId: Int): Result<SeerrMedia> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/movie/$tmdbId")
        response.body()
    }

    /**
     * Get TV show details by TMDB ID.
     */
    suspend fun getTVShow(tmdbId: Int): Result<SeerrMedia> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/tv/$tmdbId")
        response.body()
    }

    /**
     * Get collection details by TMDB collection ID.
     */
    suspend fun getCollection(collectionId: Int): Result<SeerrCollection> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/collection/$collectionId")
        response.body()
    }

    /**
     * Get TV show season details.
     */
    suspend fun getTVSeason(tmdbId: Int, seasonNumber: Int): Result<SeerrSeason> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/tv/$tmdbId/season/$seasonNumber")
        response.body()
    }

    /**
     * Get similar movies.
     */
    suspend fun getSimilarMovies(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/movie/$tmdbId/similar") {
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Get similar TV shows.
     */
    suspend fun getSimilarTV(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/tv/$tmdbId/similar") {
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Get movie recommendations.
     */
    suspend fun getMovieRecommendations(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/movie/$tmdbId/recommendations") {
            parameter("page", page)
        }
        response.body()
    }

    /**
     * Get TV recommendations.
     */
    suspend fun getTVRecommendations(tmdbId: Int, page: Int = 1): Result<SeerrDiscoverResult> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/tv/$tmdbId/recommendations") {
            parameter("page", page)
        }
        response.body()
    }

    // ========================================================================
    // Person/Actor
    // ========================================================================

    /**
     * Get person (actor/crew) details.
     *
     * @param personId TMDB person ID
     */
    suspend fun getPerson(personId: Int): Result<SeerrPerson> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/person/$personId")
        response.body()
    }

    /**
     * Get person's combined credits (movies and TV shows).
     *
     * @param personId TMDB person ID
     */
    suspend fun getPersonCombinedCredits(personId: Int): Result<SeerrPersonCredits> = runCatching {
        requireBaseUrl()
        val response = httpClient.get("$baseUrl/api/v1/person/$personId/combined_credits")
        response.body()
    }

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
    ): Result<SeerrRequestResult> = runCatching {
        requireAuth()
        val response = httpClient.get("$baseUrl/api/v1/request") {
            parameter("take", pageSize)
            parameter("skip", (page - 1) * pageSize)
            parameter("filter", filter)
            parameter("sort", sort)
            parameter("sortDirection", sortDirection)
            currentUser?.id?.let { parameter("requestedBy", it) }
        }
        response.body()
    }

    /**
     * Get all requests (admin).
     */
    suspend fun getAllRequests(
        page: Int = 1,
        pageSize: Int = 20,
        filter: String = "all",
        sort: String = "added",
        sortDirection: String = "desc",
    ): Result<SeerrRequestResult> = runCatching {
        requireAuth()
        val response = httpClient.get("$baseUrl/api/v1/request") {
            parameter("take", pageSize)
            parameter("skip", (page - 1) * pageSize)
            parameter("filter", filter)
            parameter("sort", sort)
            parameter("sortDirection", sortDirection)
        }
        response.body()
    }

    /**
     * Get a specific request by ID.
     */
    suspend fun getRequest(requestId: Int): Result<SeerrRequest> = runCatching {
        requireAuth()
        val response = httpClient.get("$baseUrl/api/v1/request/$requestId")
        response.body()
    }

    /**
     * Create a new media request.
     */
    suspend fun createRequest(request: CreateMediaRequest): Result<SeerrRequest> = runCatching {
        requireAuth()
        val response = httpClient.post("$baseUrl/api/v1/request") {
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            throw Exception("Failed to create request: $error")
        }
        response.body()
    }

    /**
     * Request a movie by TMDB ID.
     */
    suspend fun requestMovie(tmdbId: Int, is4k: Boolean = false): Result<SeerrRequest> {
        return createRequest(
            CreateMediaRequest(
                mediaType = "movie",
                mediaId = tmdbId,
                is4k = is4k,
            ),
        )
    }

    /**
     * Request a TV show by TMDB ID.
     *
     * @param tmdbId TMDB ID of the TV show
     * @param seasons List of season numbers to request (null = all seasons)
     * @param is4k Whether to request 4K version
     */
    suspend fun requestTVShow(tmdbId: Int, seasons: List<Int>? = null, is4k: Boolean = false,): Result<SeerrRequest> {
        return createRequest(
            CreateMediaRequest(
                mediaType = "tv",
                mediaId = tmdbId,
                seasons = seasons,
                is4k = is4k,
            ),
        )
    }

    /**
     * Cancel/delete a request.
     */
    suspend fun cancelRequest(requestId: Int): Result<Unit> = runCatching {
        requireAuth()
        val response = httpClient.delete("$baseUrl/api/v1/request/$requestId")
        if (!response.status.isSuccess()) {
            throw Exception("Failed to cancel request: ${response.status}")
        }
    }

    /**
     * Approve a request (admin).
     */
    suspend fun approveRequest(requestId: Int): Result<SeerrRequest> = runCatching {
        requireAuth()
        val response = httpClient.post("$baseUrl/api/v1/request/$requestId/approve")
        response.body()
    }

    /**
     * Decline a request (admin).
     */
    suspend fun declineRequest(requestId: Int): Result<Unit> = runCatching {
        requireAuth()
        val response = httpClient.post("$baseUrl/api/v1/request/$requestId/decline")
        if (!response.status.isSuccess()) {
            throw Exception("Failed to decline request: ${response.status}")
        }
    }

    // ========================================================================
    // Watchlist
    // ========================================================================

    /**
     * Add media to watchlist.
     */
    suspend fun addToWatchlist(tmdbId: Int, mediaType: String): Result<Unit> = runCatching {
        requireAuth()
        val response = httpClient.post("$baseUrl/api/v1/discover/watchlist") {
            setBody(
                mapOf(
                    "tmdbId" to tmdbId,
                    "mediaType" to mediaType,
                ),
            )
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to add to watchlist: ${response.status}")
        }
    }

    /**
     * Remove media from watchlist.
     */
    suspend fun removeFromWatchlist(tmdbId: Int, mediaType: String): Result<Unit> = runCatching {
        requireAuth()
        val response = httpClient.delete("$baseUrl/api/v1/discover/watchlist/$tmdbId") {
            parameter("mediaType", mediaType)
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to remove from watchlist: ${response.status}")
        }
    }

    // ========================================================================
    // Image URLs
    // ========================================================================

    /**
     * Build TMDB poster URL.
     *
     * @param posterPath Path from SeerrMedia.posterPath
     * @param size Image size (w92, w154, w185, w342, w500, w780, original)
     */
    fun getPosterUrl(posterPath: String?, size: String = "w500"): String? =
        posterPath?.let { "$TMDB_IMAGE_BASE/$size$it" }

    /**
     * Build TMDB backdrop URL.
     *
     * @param backdropPath Path from SeerrMedia.backdropPath
     * @param size Image size (w300, w780, w1280, original)
     */
    fun getBackdropUrl(backdropPath: String?, size: String = "w1280"): String? =
        backdropPath?.let { "$TMDB_IMAGE_BASE/$size$it" }

    /**
     * Build TMDB profile URL (for cast/crew photos).
     *
     * @param profilePath Path from SeerrCastMember.profilePath
     * @param size Image size (w45, w185, h632, original)
     */
    fun getProfileUrl(profilePath: String?, size: String = "w185"): String? =
        profilePath?.let { "$TMDB_IMAGE_BASE/$size$it" }

    /**
     * Build TMDB still URL (for episode thumbnails).
     *
     * @param stillPath Path from SeerrEpisode.stillPath
     * @param size Image size (w92, w185, w300, original)
     */
    fun getStillUrl(stillPath: String?, size: String = "w300"): String? = stillPath?.let {
        "$TMDB_IMAGE_BASE/$size$it"
    }

    // ========================================================================
    // Utilities
    // ========================================================================

    /**
     * Reset client state (for logout/disconnect).
     */
    fun reset() {
        baseUrl = null
        _isAuthenticated.value = false
        currentUser = null
    }

    private fun requireBaseUrl() {
        requireNotNull(baseUrl) { "Not connected to Seerr server. Call connectToServer() first." }
    }

    private fun requireAuth() {
        requireBaseUrl()
        require(isAuthenticated.value) { "Not authenticated. Call login*() first." }
    }

    /**
     * Extract hostname from a URL.
     */
    private fun extractHost(urlString: String): String {
        return try {
            URL(urlString).host
        } catch (_: Exception) {
            // Try parsing without protocol
            urlString
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore(":")
                .substringBefore("/")
        }
    }
}
