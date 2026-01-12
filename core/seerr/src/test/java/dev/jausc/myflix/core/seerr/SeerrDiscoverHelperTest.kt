package dev.jausc.myflix.core.seerr

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for SeerrDiscoverHelper.
 */
class SeerrDiscoverHelperTest {

    private fun createMockClient(
        trending: List<SeerrMedia> = emptyList(),
        popularMovies: List<SeerrMedia> = emptyList(),
        popularTV: List<SeerrMedia> = emptyList(),
        upcoming: List<SeerrMedia> = emptyList(),
    ): SeerrClient {
        val mockClient = mockk<SeerrClient>()
        coEvery { mockClient.getTrending(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = trending.size, results = trending)
        )
        coEvery { mockClient.getPopularMovies(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = popularMovies.size, results = popularMovies)
        )
        coEvery { mockClient.getPopularTV(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = popularTV.size, results = popularTV)
        )
        coEvery { mockClient.getUpcomingMovies(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = upcoming.size, results = upcoming)
        )
        return mockClient
    }

    // ==================== loadFallbackRows Tests ====================

    @Test
    fun `loadFallbackRows returns trending row when trending has items`() = runTest {
        val trendingItems = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Trending Movie")
        )
        val client = createMockClient(trending = trendingItems)

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        assertTrue(rows.any { it.title == "Trending" })
        val trendingRow = rows.find { it.title == "Trending" }
        assertEquals(1, trendingRow?.items?.size)
    }

    @Test
    fun `loadFallbackRows returns popular movies row when movies exist`() = runTest {
        val movies = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Popular Movie")
        )
        val client = createMockClient(popularMovies = movies)

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        assertTrue(rows.any { it.title == "Popular Movies" })
    }

    @Test
    fun `loadFallbackRows returns popular TV row when shows exist`() = runTest {
        val shows = listOf(
            SeerrMedia(id = 1, mediaType = "tv", name = "Popular Show")
        )
        val client = createMockClient(popularTV = shows)

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        assertTrue(rows.any { it.title == "Popular TV Shows" })
    }

    @Test
    fun `loadFallbackRows returns coming soon row when upcoming exists`() = runTest {
        val upcoming = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Upcoming Movie")
        )
        val client = createMockClient(upcoming = upcoming)

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        assertTrue(rows.any { it.title == "Coming Soon" })
    }

    @Test
    fun `loadFallbackRows filters out items using filterDiscoverable`() = runTest {
        val items = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Available", mediaInfo = SeerrMediaInfo(status = 5)),
            SeerrMedia(id = 2, mediaType = "movie", title = "Not Available")
        )
        val client = createMockClient(trending = items)

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) {
            filter { it.mediaInfo?.status != 5 } // Filter out available
        }

        val trendingRow = rows.find { it.title == "Trending" }
        assertEquals(1, trendingRow?.items?.size)
        assertEquals("Not Available", trendingRow?.items?.first()?.displayTitle)
    }

    @Test
    fun `loadFallbackRows returns empty list when all content is empty`() = runTest {
        val client = createMockClient()

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `loadFallbackRows limits items to 12 per row`() = runTest {
        val manyItems = (1..20).map {
            SeerrMedia(id = it, mediaType = "movie", title = "Movie $it")
        }
        val client = createMockClient(trending = manyItems)

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        val trendingRow = rows.find { it.title == "Trending" }
        assertEquals(12, trendingRow?.items?.size)
    }

    @Test
    fun `loadFallbackRows assigns correct colors to rows`() = runTest {
        val client = createMockClient(
            trending = listOf(SeerrMedia(id = 1, mediaType = "movie", title = "T")),
            popularMovies = listOf(SeerrMedia(id = 2, mediaType = "movie", title = "M")),
            popularTV = listOf(SeerrMedia(id = 3, mediaType = "tv", name = "TV")),
            upcoming = listOf(SeerrMedia(id = 4, mediaType = "movie", title = "U")),
        )

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        val trendingRow = rows.find { it.title == "Trending" }
        val moviesRow = rows.find { it.title == "Popular Movies" }
        val tvRow = rows.find { it.title == "Popular TV Shows" }
        val upcomingRow = rows.find { it.title == "Coming Soon" }

        assertEquals(SeerrColors.PURPLE, trendingRow?.accentColorValue)
        assertEquals(SeerrColors.YELLOW, moviesRow?.accentColorValue)
        assertEquals(SeerrColors.TEAL, tvRow?.accentColorValue)
        assertEquals(SeerrColors.BLUE, upcomingRow?.accentColorValue)
    }

    @Test
    fun `loadFallbackRows generates unique keys for each row`() = runTest {
        val client = createMockClient(
            trending = listOf(SeerrMedia(id = 1, mediaType = "movie", title = "T")),
            popularMovies = listOf(SeerrMedia(id = 2, mediaType = "movie", title = "M")),
        )

        val rows = SeerrDiscoverHelper.loadFallbackRows(client) { this }

        val keys = rows.map { it.key }
        assertEquals(keys.size, keys.distinct().size) // All keys should be unique
    }

    // ==================== SeerrColors Tests ====================

    @Test
    fun `SeerrColors constants are correct`() {
        assertEquals(0xFF8B5CF6L, SeerrColors.PURPLE)
        assertEquals(0xFFFBBF24L, SeerrColors.YELLOW)
        assertEquals(0xFF22C55EL, SeerrColors.GREEN)
        assertEquals(0xFF60A5FAL, SeerrColors.BLUE)
        assertEquals(0xFF34D399L, SeerrColors.TEAL)
    }

    // ==================== SeerrDiscoverRow Tests ====================

    @Test
    fun `SeerrDiscoverRow holds correct data`() {
        val items = listOf(SeerrMedia(id = 1, mediaType = "movie", title = "Test"))
        val row = SeerrDiscoverRow(
            key = "test_key",
            title = "Test Row",
            items = items,
            accentColorValue = SeerrColors.PURPLE
        )

        assertEquals("test_key", row.key)
        assertEquals("Test Row", row.title)
        assertEquals(1, row.items.size)
        assertEquals(SeerrColors.PURPLE, row.accentColorValue)
    }
}
