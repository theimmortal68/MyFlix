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

    private fun createMockRepository(
        trending: List<SeerrMedia> = emptyList(),
        popularMovies: List<SeerrMedia> = emptyList(),
        popularTV: List<SeerrMedia> = emptyList(),
        upcomingMovies: List<SeerrMedia> = emptyList(),
        upcomingTV: List<SeerrMedia> = emptyList(),
    ): SeerrRepository {
        val mockRepository = mockk<SeerrRepository>()
        coEvery { mockRepository.getTrending(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = trending.size, results = trending)
        )
        coEvery { mockRepository.getPopularMovies(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = popularMovies.size, results = popularMovies)
        )
        coEvery { mockRepository.getPopularTV(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = popularTV.size, results = popularTV)
        )
        coEvery { mockRepository.getUpcomingMovies(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = upcomingMovies.size, results = upcomingMovies)
        )
        coEvery { mockRepository.getUpcomingTV(any()) } returns Result.success(
            SeerrDiscoverResult(page = 1, totalPages = 1, totalResults = upcomingTV.size, results = upcomingTV)
        )
        return mockRepository
    }

    // ==================== loadFallbackRows Tests ====================

    @Test
    fun `loadFallbackRows returns trending row when trending has items`() = runTest {
        val trendingItems = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Trending Movie")
        )
        val repository = createMockRepository(trending = trendingItems)

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        assertTrue(rows.any { it.title == "Trending" })
        val trendingRow = rows.find { it.title == "Trending" }
        assertEquals(1, trendingRow?.items?.size)
    }

    @Test
    fun `loadFallbackRows returns popular movies row when movies exist`() = runTest {
        val movies = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Popular Movie")
        )
        val repository = createMockRepository(popularMovies = movies)

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        assertTrue(rows.any { it.title == "Popular Movies" })
    }

    @Test
    fun `loadFallbackRows returns popular TV row when shows exist`() = runTest {
        val shows = listOf(
            SeerrMedia(id = 1, mediaType = "tv", name = "Popular Show")
        )
        val repository = createMockRepository(popularTV = shows)

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        assertTrue(rows.any { it.title == "Popular TV Shows" })
    }

    @Test
    fun `loadFallbackRows returns upcoming movies row when upcoming exists`() = runTest {
        val upcomingMovies = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Upcoming Movie")
        )
        val repository = createMockRepository(upcomingMovies = upcomingMovies)

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        assertTrue(rows.any { it.title == "Upcoming Movies" })
    }

    @Test
    fun `loadFallbackRows returns upcoming TV row when upcoming TV exists`() = runTest {
        val upcomingTV = listOf(
            SeerrMedia(id = 1, mediaType = "tv", name = "Upcoming Show")
        )
        val repository = createMockRepository(upcomingTV = upcomingTV)

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        assertTrue(rows.any { it.title == "Upcoming TV" })
    }

    @Test
    fun `loadFallbackRows filters out available and requested items`() = runTest {
        val items = listOf(
            SeerrMedia(id = 1, mediaType = "movie", title = "Available", mediaInfo = SeerrMediaInfo(status = 5)),
            SeerrMedia(id = 2, mediaType = "movie", title = "Requested", mediaInfo = SeerrMediaInfo(status = 2)),
            SeerrMedia(id = 3, mediaType = "movie", title = "Discoverable")
        )
        val repository = createMockRepository(trending = items)

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        val trendingRow = rows.find { it.title == "Trending" }
        assertEquals(1, trendingRow?.items?.size)
        assertEquals("Discoverable", trendingRow?.items?.first()?.displayTitle)
    }

    @Test
    fun `loadFallbackRows returns empty list when all content is empty`() = runTest {
        val repository = createMockRepository()

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `loadFallbackRows limits items to 12 per row`() = runTest {
        val manyItems = (1..20).map {
            SeerrMedia(id = it, mediaType = "movie", title = "Movie $it")
        }
        val repository = createMockRepository(trending = manyItems)

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        val trendingRow = rows.find { it.title == "Trending" }
        assertEquals(12, trendingRow?.items?.size)
    }

    @Test
    fun `loadFallbackRows assigns correct colors to rows`() = runTest {
        val repository = createMockRepository(
            trending = listOf(SeerrMedia(id = 1, mediaType = "movie", title = "T")),
            popularMovies = listOf(SeerrMedia(id = 2, mediaType = "movie", title = "M")),
            popularTV = listOf(SeerrMedia(id = 3, mediaType = "tv", name = "TV")),
            upcomingMovies = listOf(SeerrMedia(id = 4, mediaType = "movie", title = "UM")),
            upcomingTV = listOf(SeerrMedia(id = 5, mediaType = "tv", name = "UT")),
        )

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

        val trendingRow = rows.find { it.title == "Trending" }
        val moviesRow = rows.find { it.title == "Popular Movies" }
        val tvRow = rows.find { it.title == "Popular TV Shows" }
        val upcomingMoviesRow = rows.find { it.title == "Upcoming Movies" }
        val upcomingTvRow = rows.find { it.title == "Upcoming TV" }

        assertEquals(SeerrColors.PURPLE, trendingRow?.accentColorValue)
        assertEquals(SeerrColors.YELLOW, moviesRow?.accentColorValue)
        assertEquals(SeerrColors.TEAL, tvRow?.accentColorValue)
        assertEquals(SeerrColors.BLUE, upcomingMoviesRow?.accentColorValue)
        assertEquals(SeerrColors.BLUE, upcomingTvRow?.accentColorValue)
    }

    @Test
    fun `loadFallbackRows generates unique keys for each row`() = runTest {
        val repository = createMockRepository(
            trending = listOf(SeerrMedia(id = 1, mediaType = "movie", title = "T")),
            popularMovies = listOf(SeerrMedia(id = 2, mediaType = "movie", title = "M")),
        )

        val rows = SeerrDiscoverHelper.loadFallbackRows(repository)

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
