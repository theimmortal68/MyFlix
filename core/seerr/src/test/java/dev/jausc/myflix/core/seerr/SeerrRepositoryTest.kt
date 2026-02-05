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
 * Unit tests for SeerrRepository.
 * Tests caching behavior, delegation to SeerrClient, and state management.
 */
class SeerrRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun createMockRepository(handler: MockRequestHandler): SeerrRepository {
        val mockEngine = MockEngine(handler)
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        val client = SeerrClient(httpClient = httpClient)
        return SeerrRepository(client)
    }

    private suspend fun setupAuthenticated(repository: SeerrRepository, client: SeerrClient) {
        client.connectToServer("https://seerr.local")
        client.loginWithApiKey("test-api-key")
    }

    // ==================== Authentication State Tests ====================

    @Test
    fun `isAuthenticated reflects client state`() = runTest {
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1, "email": "test@example.com"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        assertFalse(repository.isAuthenticated.value)

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        assertTrue(repository.isAuthenticated.value)
    }

    @Test
    fun `sessionCookie exposed from client`() = runTest {
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/jellyfin") -> respond(
                    content = """{"id": 1, "email": "test@example.com"}""",
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/json"),
                        HttpHeaders.SetCookie to listOf("connect.sid=test-session-cookie; Path=/; HttpOnly")
                    )
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithJellyfin("user", "pass")

        // Session cookie should be captured from login response
        assertNotNull(repository.sessionCookie)
    }

    // ==================== Caching Tests ====================

    @Test
    fun `getMovieGenres returns cached value on second call`() = runTest {
        var callCount = 0
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.contains("/genreslider/movie") -> {
                    callCount++
                    respond(
                        content = """[{"id": 28, "name": "Action"}, {"id": 35, "name": "Comedy"}]""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        // First call - should hit API
        val result1 = repository.getMovieGenres()
        assertTrue(result1.isSuccess)
        assertEquals(2, result1.getOrNull()?.size)
        assertEquals(1, callCount)

        // Second call - should return cached
        val result2 = repository.getMovieGenres()
        assertTrue(result2.isSuccess)
        assertEquals(2, result2.getOrNull()?.size)
        assertEquals(1, callCount) // Still 1, no additional API call
    }

    @Test
    fun `getTVGenres returns cached value on second call`() = runTest {
        var callCount = 0
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.contains("/genreslider/tv") -> {
                    callCount++
                    respond(
                        content = """[{"id": 18, "name": "Drama"}, {"id": 10765, "name": "Sci-Fi"}]""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        // First call - should hit API
        val result1 = repository.getTVGenres()
        assertTrue(result1.isSuccess)
        assertEquals(2, result1.getOrNull()?.size)
        assertEquals(1, callCount)

        // Second call - should return cached
        val result2 = repository.getTVGenres()
        assertTrue(result2.isSuccess)
        assertEquals(1, callCount) // Still 1
    }

    @Test
    fun `reset clears cached data`() = runTest {
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1, "displayName": "Test User"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.contains("/genreslider/movie") -> respond(
                    content = """[{"id": 28, "name": "Action"}]""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        // Load some cached data
        repository.getMovieGenres()
        assertEquals(1, repository.movieGenres.value.size)

        // Reset should clear everything
        repository.reset()

        assertEquals(0, repository.movieGenres.value.size)
        assertNull(repository.currentUser.value)
    }

    // ==================== Delegation Tests ====================

    @Test
    fun `search delegates to client`() = runTest {
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.contains("/search") -> respond(
                    content = """{
                        "page": 1,
                        "totalPages": 1,
                        "totalResults": 1,
                        "results": [{"id": 123, "mediaType": "movie", "title": "Test"}]
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        val result = repository.search("test")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.results?.size)
    }

    @Test
    fun `getMovie delegates to client`() = runTest {
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.contains("/movie/123") -> respond(
                    content = """{"id": 123, "mediaType": "movie", "title": "Test Movie"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        val result = repository.getMovie(123)

        assertTrue(result.isSuccess)
        assertEquals("Test Movie", result.getOrNull()?.displayTitle)
    }

    @Test
    fun `getTVShow delegates to client`() = runTest {
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.contains("/tv/456") -> respond(
                    content = """{"id": 456, "mediaType": "tv", "name": "Test Show"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        val result = repository.getTVShow(456)

        assertTrue(result.isSuccess)
        assertEquals("Test Show", result.getOrNull()?.displayTitle)
    }

    @Test
    fun `requestMovie delegates to client`() = runTest {
        val repository = createMockRepository { request ->
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
                    content = """{"id": 1, "status": 1, "media": {"id": 123, "mediaType": "movie"}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        val result = repository.requestMovie(123)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.id)
    }

    // ==================== URL Builder Tests ====================

    @Test
    fun `getPosterUrl delegates to client`() = runTest {
        val repository = createMockRepository { _ -> respondError(HttpStatusCode.NotFound) }

        val url = repository.getPosterUrl("/path/to/poster.jpg")

        assertNotNull(url)
        assertTrue(url.contains("image.tmdb.org"))
    }

    @Test
    fun `getBackdropUrl delegates to client`() = runTest {
        val repository = createMockRepository { _ -> respondError(HttpStatusCode.NotFound) }

        val url = repository.getBackdropUrl("/path/to/backdrop.jpg")

        assertNotNull(url)
        assertTrue(url.contains("image.tmdb.org"))
    }

    @Test
    fun `getProfileUrl delegates to client`() = runTest {
        val repository = createMockRepository { _ -> respondError(HttpStatusCode.NotFound) }

        val url = repository.getProfileUrl("/path/to/profile.jpg")

        assertNotNull(url)
        assertTrue(url.contains("image.tmdb.org"))
    }

    // ==================== Current User Tests ====================

    @Test
    fun `currentUser updated after login`() = runTest {
        val repository = createMockRepository { request ->
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

        assertNull(repository.currentUser.value)

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")

        assertNotNull(repository.currentUser.value)
        assertEquals("Test User", repository.currentUser.value?.displayName)
    }

    @Test
    fun `logout clears currentUser`() = runTest {
        val repository = createMockRepository { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> respond(
                    content = """{"version": "1.0.0", "commitTag": "abc123"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/me") -> respond(
                    content = """{"id": 1, "displayName": "Test User"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath.endsWith("/auth/logout") -> respond(
                    content = """{}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        repository.connectToServer("https://seerr.local")
        repository.loginWithApiKey("test-key")
        assertNotNull(repository.currentUser.value)

        repository.logout()

        assertNull(repository.currentUser.value)
    }
}
