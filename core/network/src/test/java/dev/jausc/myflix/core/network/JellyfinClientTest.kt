package dev.jausc.myflix.core.network

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for JellyfinClient using Ktor MockEngine.
 *
 * Note: Tests that invoke methods using android.util.Log are excluded
 * since Log is not mocked in unit tests. Use instrumented tests for those.
 */
class JellyfinClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun createMockClient(handler: MockRequestHandler): JellyfinClient {
        val mockEngine = MockEngine(handler)
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        return JellyfinClient(httpClient = httpClient)
    }

    // ==================== getServerInfo Tests ====================

    @Test
    fun `getServerInfo returns ServerInfo on success`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "ServerName": "My Jellyfin",
                    "Version": "10.8.13",
                    "Id": "abc123def456",
                    "LocalAddress": "http://192.168.1.100:8096"
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = client.getServerInfo("https://jellyfin.local")

        assertTrue(result.isSuccess)
        val serverInfo = result.getOrNull()
        assertNotNull(serverInfo)
        assertEquals("My Jellyfin", serverInfo.serverName)
        assertEquals("10.8.13", serverInfo.version)
        assertEquals("abc123def456", serverInfo.id)
    }

    @Test
    fun `getServerInfo returns failure on network error`() = runTest {
        val client = createMockClient { _ ->
            respondError(HttpStatusCode.InternalServerError)
        }

        val result = client.getServerInfo("https://jellyfin.local")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getServerInfo returns failure on invalid JSON`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = "not valid json",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = client.getServerInfo("https://jellyfin.local")

        assertTrue(result.isFailure)
    }

    // ==================== login Tests ====================

    @Test
    fun `login returns AuthResponse on success`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "AccessToken": "test-token-123",
                    "User": {
                        "Id": "user-id-456",
                        "Name": "TestUser",
                        "ServerId": "server-123"
                    }
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = client.login("https://jellyfin.local", "testuser", "password123")

        assertTrue(result.isSuccess)
        val authResponse = result.getOrNull()
        assertNotNull(authResponse)
        assertEquals("test-token-123", authResponse.accessToken)
        assertEquals("user-id-456", authResponse.user.id)
        assertEquals("TestUser", authResponse.user.name)
    }

    @Test
    fun `login returns failure on 401 unauthorized`() = runTest {
        val client = createMockClient { _ ->
            respondError(HttpStatusCode.Unauthorized)
        }

        val result = client.login("https://jellyfin.local", "testuser", "wrongpassword")

        assertTrue(result.isFailure)
    }

    // ==================== getLibraries Tests ====================

    @Test
    fun `getLibraries returns list of libraries`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Items": [
                        {"Id": "lib1", "Name": "Movies", "CollectionType": "movies", "Type": "CollectionFolder"},
                        {"Id": "lib2", "Name": "TV Shows", "CollectionType": "tvshows", "Type": "CollectionFolder"}
                    ],
                    "TotalRecordCount": 2
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getLibraries()

        assertTrue(result.isSuccess)
        val libraries = result.getOrNull()
        assertNotNull(libraries)
        assertEquals(2, libraries.size)
        assertEquals("Movies", libraries[0].name)
        assertEquals("TV Shows", libraries[1].name)
    }

    @Test
    fun `getLibraries returns cached result on second call`() = runTest {
        var callCount = 0
        val client = createMockClient { _ ->
            callCount++
            respond(
                content = """{
                    "Items": [{"Id": "lib1", "Name": "Movies", "Type": "CollectionFolder"}],
                    "TotalRecordCount": 1
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        client.getLibraries()
        client.getLibraries()

        assertEquals(1, callCount, "Second call should use cache")
    }

    @Test
    fun `getLibraries makes new request after cache cleared`() = runTest {
        var callCount = 0
        val client = createMockClient { _ ->
            callCount++
            respond(
                content = """{
                    "Items": [{"Id": "lib1", "Name": "Movies", "Type": "CollectionFolder"}],
                    "TotalRecordCount": 1
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        client.getLibraries()
        client.clearCache()
        client.getLibraries()

        assertEquals(2, callCount, "Should make new request after cache cleared")
    }

    // ==================== getItem Tests ====================

    @Test
    fun `getItem returns item details`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Id": "item123",
                    "Name": "Test Movie",
                    "Type": "Movie",
                    "Overview": "A great test movie",
                    "ProductionYear": 2024,
                    "RunTimeTicks": 72000000000,
                    "CommunityRating": 8.5
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getItem("item123")

        assertTrue(result.isSuccess)
        val item = result.getOrNull()
        assertNotNull(item)
        assertEquals("item123", item.id)
        assertEquals("Test Movie", item.name)
        assertEquals("Movie", item.type)
        assertEquals(2024, item.productionYear)
    }

    @Test
    fun `getItem returns failure on 404`() = runTest {
        val client = createMockClient { _ ->
            respondError(HttpStatusCode.NotFound)
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getItem("nonexistent")

        assertTrue(result.isFailure)
    }

    // ==================== isAuthenticated Tests ====================

    @Test
    fun `isAuthenticated returns false when not configured`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }

        assertFalse(client.isAuthenticated)
    }

    @Test
    fun `isAuthenticated returns true when configured`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        assertTrue(client.isAuthenticated)
    }

    @Test
    fun `logout clears authentication state`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")
        assertTrue(client.isAuthenticated)

        client.logout()

        assertFalse(client.isAuthenticated)
        assertNull(client.serverUrl)
        assertNull(client.accessToken)
        assertNull(client.userId)
    }

    // ==================== Content API Tests ====================

    @Test
    fun `getContinueWatching returns list of items`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Items": [
                        {"Id": "movie1", "Name": "In Progress Movie", "Type": "Movie"},
                        {"Id": "ep1", "Name": "Episode 5", "Type": "Episode"}
                    ],
                    "TotalRecordCount": 2
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getContinueWatching()

        assertTrue(result.isSuccess)
        val items = result.getOrNull()
        assertNotNull(items)
        assertEquals(2, items.size)
        assertEquals("In Progress Movie", items[0].name)
    }

    @Test
    fun `getContinueWatching returns empty list when nothing in progress`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{"Items": [], "TotalRecordCount": 0}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getContinueWatching()

        assertTrue(result.isSuccess)
        val items = result.getOrNull()
        assertNotNull(items)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `getNextUp returns next episodes to watch`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Items": [
                        {"Id": "ep2", "Name": "Episode 2", "Type": "Episode", "SeriesName": "Test Series", "IndexNumber": 2}
                    ],
                    "TotalRecordCount": 1
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getNextUp()

        assertTrue(result.isSuccess)
        val items = result.getOrNull()
        assertNotNull(items)
        assertEquals(1, items.size)
        assertEquals("Episode 2", items[0].name)
    }

    @Test
    fun `getLatestMovies returns latest movies`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Items": [
                        {"Id": "movie1", "Name": "New Movie 1", "Type": "Movie", "ProductionYear": 2024},
                        {"Id": "movie2", "Name": "New Movie 2", "Type": "Movie", "ProductionYear": 2024}
                    ],
                    "TotalRecordCount": 2
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getLatestMovies("lib123")

        assertTrue(result.isSuccess)
        val movies = result.getOrNull()
        assertNotNull(movies)
        assertEquals(2, movies.size)
    }

    @Test
    fun `search returns matching items`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Items": [
                        {"Id": "item1", "Name": "Test Movie", "Type": "Movie"},
                        {"Id": "item2", "Name": "Test Series", "Type": "Series"}
                    ],
                    "TotalRecordCount": 2
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.search("test query")

        assertTrue(result.isSuccess)
        val items = result.getOrNull()
        assertNotNull(items)
        assertEquals(2, items.size)
    }

    @Test
    fun `search returns empty list when no matches`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{"Items": [], "TotalRecordCount": 0}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.search("nonexistent")

        assertTrue(result.isSuccess)
        val items = result.getOrNull()
        assertNotNull(items)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `getSeasons returns seasons for series`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Items": [
                        {"Id": "season1", "Name": "Season 1", "Type": "Season", "IndexNumber": 1},
                        {"Id": "season2", "Name": "Season 2", "Type": "Season", "IndexNumber": 2}
                    ],
                    "TotalRecordCount": 2
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getSeasons("series123")

        assertTrue(result.isSuccess)
        val seasons = result.getOrNull()
        assertNotNull(seasons)
        assertEquals(2, seasons.size)
        assertEquals("Season 1", seasons[0].name)
    }

    @Test
    fun `getEpisodes returns episodes for season`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{
                    "Items": [
                        {"Id": "ep1", "Name": "Pilot", "Type": "Episode", "IndexNumber": 1},
                        {"Id": "ep2", "Name": "Second Episode", "Type": "Episode", "IndexNumber": 2}
                    ],
                    "TotalRecordCount": 2
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.getEpisodes("series123", "season1")

        assertTrue(result.isSuccess)
        val episodes = result.getOrNull()
        assertNotNull(episodes)
        assertEquals(2, episodes.size)
        assertEquals("Pilot", episodes[0].name)
    }

    // ==================== Playback Reporting Tests ====================
    // Note: reportPlaybackStart/Progress/Stopped tests are skipped because they use
    // Map<String, Any> which kotlinx.serialization can't handle with mixed types.
    // These methods are tested via instrumented tests instead.

    @Test
    fun `setFavorite succeeds for favorite`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{"IsFavorite": true}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.setFavorite("item123", isFavorite = true)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `setFavorite succeeds for unfavorite`() = runTest {
        val client = createMockClient { _ ->
            respond(
                content = """{"IsFavorite": false}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val result = client.setFavorite("item123", isFavorite = false)

        assertTrue(result.isSuccess)
    }

    // ==================== URL Builder Tests ====================

    @Test
    fun `getStreamUrl returns correct format`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        client.configure("https://jellyfin.local", "token123", "userId", "deviceId")

        val url = client.getStreamUrl("item123")

        assertTrue(url.startsWith("https://jellyfin.local/"))
        assertTrue(url.contains("item123"))
        assertTrue(url.contains("Static=true"))  // Capital S
    }

    @Test
    fun `getPrimaryImageUrl returns correct format with tag`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val url = client.getPrimaryImageUrl("item123", "tag456", maxWidth = 300)

        assertTrue(url.contains("item123"))
        assertTrue(url.contains("Primary"))
        assertTrue(url.contains("tag=tag456"))
        assertTrue(url.contains("maxWidth=300"))
    }

    @Test
    fun `getPrimaryImageUrl returns correct format without tag`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val url = client.getPrimaryImageUrl("item123", null)

        assertTrue(url.contains("item123"))
        assertTrue(url.contains("Primary"))
        assertFalse(url.contains("tag="))
    }

    @Test
    fun `getBackdropUrl returns correct format`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val url = client.getBackdropUrl("item123", "backdrop-tag", maxWidth = 1920)

        assertTrue(url.contains("item123"))
        assertTrue(url.contains("Backdrop"))
        assertTrue(url.contains("tag=backdrop-tag"))
        assertTrue(url.contains("maxWidth=1920"))
    }

    @Test
    fun `getThumbUrl returns correct format`() {
        val client = createMockClient { _ -> respondError(HttpStatusCode.NotFound) }
        client.configure("https://jellyfin.local", "token", "userId", "deviceId")

        val url = client.getThumbUrl("item123", "thumb-tag", maxWidth = 600)

        assertTrue(url.contains("item123"))
        assertTrue(url.contains("Thumb"))
        assertTrue(url.contains("tag=thumb-tag"))
    }
}
