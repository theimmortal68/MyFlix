package dev.jausc.myflix.core.seerr

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SeerrClient using Ktor MockEngine.
 */
class SeerrClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun createMockClient(handler: MockRequestHandler): SeerrClient {
        val mockEngine = MockEngine(handler)
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        return SeerrClient(httpClient = httpClient)
    }

    /**
     * Helper to set up a client with mock server and authentication.
     * Calls connectToServer first to set baseUrl, then loginWithApiKey.
     */
    private suspend fun setupAuthenticatedClient(client: SeerrClient) {
        // First connect to server to set baseUrl
        client.connectToServer("https://seerr.local")
        // Then login with API key
        client.loginWithApiKey("test-api-key")
    }

    // ==================== Configuration Tests ====================

    @Test
    fun `isAuthenticated returns false when not configured`() = runTest {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        assertFalse(client.isAuthenticated.value)
    }

    @Test
    fun `isAuthenticated returns true after loginWithApiKey`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1, "email": "test@example.com", "displayName": "Test User"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        setupAuthenticatedClient(client)

        assertTrue(client.isAuthenticated.value)
    }

    @Test
    fun `logout clears authentication state`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1, "email": "test@example.com"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/logout") -> respond(
                    content = """{}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        setupAuthenticatedClient(client)
        assertTrue(client.isAuthenticated.value)

        val result = client.logout()

        assertTrue(result.isSuccess)
        assertFalse(client.isAuthenticated.value)
        // Note: logout does NOT clear baseUrl, only authentication state
        assertNotNull(client.baseUrl)
    }

    // ==================== Search Tests ====================

    @Test
    fun `search returns parsed results`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/search") -> respond(
                    content = """{
                        "page": 1,
                        "totalPages": 5,
                        "totalResults": 100,
                        "results": [
                            {"id": 123, "mediaType": "movie", "title": "Test Movie"},
                            {"id": 456, "mediaType": "tv", "name": "Test Show"}
                        ]
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.search("test query")

        assertTrue(result.isSuccess)
        val searchResult = result.getOrNull()
        assertNotNull(searchResult)
        assertEquals(2, searchResult.results.size)
        assertEquals("Test Movie", searchResult.results[0].displayTitle)
        assertEquals("Test Show", searchResult.results[1].displayTitle)
    }

    @Test
    fun `search returns empty results when no matches`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/search") -> respond(
                    content = """{"page": 1, "totalPages": 0, "totalResults": 0, "results": []}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.search("nonexistent")

        assertTrue(result.isSuccess)
        val searchResult = result.getOrNull()
        assertNotNull(searchResult)
        assertTrue(searchResult.results.isEmpty())
    }

    @Test
    fun `search returns failure on network error`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/search") -> respondError(HttpStatusCode.InternalServerError)
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.search("test")

        assertTrue(result.isFailure)
    }

    // ==================== Discover Tests ====================

    @Test
    fun `getTrending returns trending media`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/discover/trending") -> respond(
                    content = """{
                        "page": 1,
                        "totalPages": 10,
                        "totalResults": 200,
                        "results": [
                            {"id": 1, "mediaType": "movie", "title": "Trending Movie"}
                        ]
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.getTrending()

        assertTrue(result.isSuccess)
        val trending = result.getOrNull()
        assertNotNull(trending)
        assertEquals(1, trending.results.size)
        assertEquals("Trending Movie", trending.results[0].displayTitle)
    }

    @Test
    fun `getPopularMovies returns popular movies`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/discover/movies") -> respond(
                    content = """{
                        "page": 1,
                        "totalPages": 5,
                        "totalResults": 100,
                        "results": [
                            {"id": 100, "mediaType": "movie", "title": "Popular Movie"}
                        ]
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.getPopularMovies()

        assertTrue(result.isSuccess)
        val movies = result.getOrNull()
        assertNotNull(movies)
        assertEquals(1, movies.results.size)
    }

    @Test
    fun `getPopularTV returns popular TV shows`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/discover/tv") -> respond(
                    content = """{
                        "page": 1,
                        "totalPages": 5,
                        "totalResults": 100,
                        "results": [
                            {"id": 200, "mediaType": "tv", "name": "Popular Show"}
                        ]
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.getPopularTV()

        assertTrue(result.isSuccess)
        val shows = result.getOrNull()
        assertNotNull(shows)
        assertEquals(1, shows.results.size)
    }

    // ==================== Media Detail Tests ====================

    @Test
    fun `getMovie returns movie details`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/movie/123") -> respond(
                    content = """{
                        "id": 123,
                        "mediaType": "movie",
                        "title": "Test Movie",
                        "overview": "A test movie description",
                        "releaseDate": "2024-01-15",
                        "voteAverage": 8.5
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.getMovie(123)

        assertTrue(result.isSuccess)
        val movie = result.getOrNull()
        assertNotNull(movie)
        assertEquals("Test Movie", movie.displayTitle)
        assertEquals(2024, movie.year)
        assertEquals(8.5, movie.voteAverage)
    }

    @Test
    fun `getTVShow returns TV show details`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.contains("/tv/456") -> respond(
                    content = """{
                        "id": 456,
                        "mediaType": "tv",
                        "name": "Test Show",
                        "overview": "A test show description",
                        "firstAirDate": "2023-06-01",
                        "number_of_seasons": 3
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.getTVShow(456)

        assertTrue(result.isSuccess)
        val show = result.getOrNull()
        assertNotNull(show)
        assertEquals("Test Show", show.displayTitle)
        assertEquals(2023, show.year)
        assertEquals(3, show.numberOfSeasons)
    }

    // ==================== Request Tests ====================

    @Test
    fun `requestMovie creates movie request`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.endsWith("/request") && request.method == HttpMethod.Post -> respond(
                    content = """{"id": 1, "status": 1, "media": {"id": 123, "mediaType": "movie"}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.requestMovie(123)

        assertTrue(result.isSuccess)
        val requestResult = result.getOrNull()
        assertNotNull(requestResult)
        assertEquals(1, requestResult.id)
    }

    @Test
    fun `requestTVShow creates TV request`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.endsWith("/request") && request.method == HttpMethod.Post -> respond(
                    content = """{"id": 2, "status": 1, "media": {"id": 456, "mediaType": "tv"}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.requestTVShow(456, listOf(1, 2))

        assertTrue(result.isSuccess)
        val requestResult = result.getOrNull()
        assertNotNull(requestResult)
        assertEquals(2, requestResult.id)
    }

    @Test
    fun `getMyRequests returns user requests`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/request") -> respond(
                    content = """{
                        "pageInfo": {"page": 1, "pages": 1, "pageSize": 20, "results": 2},
                        "results": [
                            {"id": 1, "status": 1, "media": {"id": 123, "mediaType": "movie"}},
                            {"id": 2, "status": 2, "media": {"id": 456, "mediaType": "tv"}}
                        ]
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        setupAuthenticatedClient(client)

        val result = client.getMyRequests()

        assertTrue(result.isSuccess, "Expected success but got: ${result.exceptionOrNull()?.message}")
        val requests = result.getOrNull()
        assertNotNull(requests)
        assertEquals(2, requests.results.size)
    }

    // ==================== URL Builder Tests ====================

    @Test
    fun `getPosterUrl returns correct format`() = runTest {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }

        val url = client.getPosterUrl("/path/to/poster.jpg")

        assertNotNull(url)
        assertTrue(url.contains("image.tmdb.org"))
        assertTrue(url.contains("poster.jpg"))
    }

    @Test
    fun `getPosterUrl returns null for null path`() = runTest {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }

        val url = client.getPosterUrl(null)

        assertNull(url)
    }

    @Test
    fun `getBackdropUrl returns correct format`() = runTest {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }

        val url = client.getBackdropUrl("/path/to/backdrop.jpg")

        assertNotNull(url)
        assertTrue(url.contains("image.tmdb.org"))
        assertTrue(url.contains("backdrop.jpg"))
    }

    @Test
    fun `getProfileUrl returns correct format`() = runTest {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }

        val url = client.getProfileUrl("/path/to/profile.jpg")

        assertNotNull(url)
        assertTrue(url.contains("image.tmdb.org"))
        assertTrue(url.contains("profile.jpg"))
    }

    // ==================== Server Detection Tests ====================

    @Test
    fun `detectServer tries subdomain substitution for FQDN with multiple dots`() = runTest {
        var requestedUrls = mutableListOf<String>()
        val client = createMockClient { request ->
            requestedUrls.add(request.url.toString())
            when {
                request.url.host == "seerr.myflix.media" -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("https://jellyfin.myflix.media")

        assertTrue(result.isSuccess)
        assertEquals("https://seerr.myflix.media", result.getOrNull())
        // Verify it tried subdomain substitution
        assertTrue(requestedUrls.any { it.contains("seerr.myflix.media") })
    }

    @Test
    fun `detectServer tries subdomain substitution for simple FQDN`() = runTest {
        var requestedUrls = mutableListOf<String>()
        val client = createMockClient { request ->
            requestedUrls.add(request.url.toString())
            when {
                request.url.host == "jellyseerr.local" -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("http://jellyfin.local")

        assertTrue(result.isSuccess)
        assertEquals("http://jellyseerr.local", result.getOrNull())
        // Verify it tried seerr.local first, then jellyseerr.local
        assertTrue(requestedUrls.any { it.contains("seerr.local") })
        assertTrue(requestedUrls.any { it.contains("jellyseerr.local") })
    }

    @Test
    fun `detectServer tries overseerr subdomain`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.host == "overseerr.domain.com" -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("https://jellyfin.domain.com")

        assertTrue(result.isSuccess)
        assertEquals("https://overseerr.domain.com", result.getOrNull())
    }

    @Test
    fun `detectServer uses port detection for IP address`() = runTest {
        var requestedUrls = mutableListOf<String>()
        val client = createMockClient { request ->
            requestedUrls.add(request.url.toString())
            when {
                request.url.host == "192.168.1.100" && request.url.port == 5055 -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("http://192.168.1.100:8096")

        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.100:5055", result.getOrNull())
        // Should NOT try subdomain substitution for IP addresses
        assertFalse(requestedUrls.any { it.contains("seerr.") || it.contains("jellyseerr.") })
    }

    @Test
    fun `detectServer tries port 5056 for IP address`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.host == "10.0.0.50" && request.url.port == 5056 -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("http://10.0.0.50:8096")

        assertTrue(result.isSuccess)
        assertEquals("http://10.0.0.50:5056", result.getOrNull())
    }

    @Test
    fun `detectServer fails when no server found`() = runTest {
        val client = createMockClient { _ ->
            respondError(HttpStatusCode.NotFound)
        }

        val result = client.detectServer("https://jellyfin.example.com")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `detectServer preserves https scheme`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.protocol.name == "https" && request.url.host == "seerr.secure.com" -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("https://jellyfin.secure.com")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.startsWith("https://") == true)
    }

    @Test
    fun `detectServer preserves http scheme`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.protocol.name == "http" && request.url.host == "seerr.local" -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("http://jellyfin.local")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.startsWith("http://") == true)
    }

    @Test
    fun `detectServer handles URL with port in FQDN`() = runTest {
        val client = createMockClient { request ->
            when {
                request.url.host == "seerr.myflix.media" -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val result = client.detectServer("https://jellyfin.myflix.media:8096")

        assertTrue(result.isSuccess)
        assertEquals("https://seerr.myflix.media", result.getOrNull())
    }
}
