package dev.jausc.myflix.core.common.ui

import dev.jausc.myflix.core.common.model.JellyfinItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DetailScreenStateTest {

    private lateinit var mockLoader: DetailLoader
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val movieItem = JellyfinItem(
        id = "movie-1",
        name = "Test Movie",
        type = "Movie",
    )

    private val seriesItem = JellyfinItem(
        id = "series-1",
        name = "Test Series",
        type = "Series",
    )

    private val season1 = JellyfinItem(
        id = "season-1",
        name = "Season 1",
        type = "Season",
        indexNumber = 1,
    )

    private val season2 = JellyfinItem(
        id = "season-2",
        name = "Season 2",
        type = "Season",
        indexNumber = 2,
    )

    private val episode1 = JellyfinItem(
        id = "episode-1",
        name = "Episode 1",
        type = "Episode",
        indexNumber = 1,
    )

    private val episode2 = JellyfinItem(
        id = "episode-2",
        name = "Episode 2",
        type = "Episode",
        indexNumber = 2,
    )

    @Before
    fun setUp() {
        mockLoader = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)
    }

    // region Initial State Tests

    @Test
    fun `initial state has null item and isLoading true`() {
        val state = createState("item-1")

        assertTrue(state.isLoading)
        assertNull(state.item)
        assertTrue(state.seasons.isEmpty())
        assertNull(state.selectedSeason)
        assertTrue(state.episodes.isEmpty())
        assertNull(state.error)
        assertFalse(state.isSeries)
        assertFalse(state.hasSeasons)
        assertFalse(state.hasEpisodes)
    }

    // endregion

    // region loadItem Tests - Movie

    @Test
    fun `loadItem loads movie successfully`() = testScope.runTest {
        coEvery { mockLoader.loadItem("movie-1") } returns Result.success(movieItem)

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals(movieItem, state.item)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadItem for movie does not load seasons`() = testScope.runTest {
        coEvery { mockLoader.loadItem("movie-1") } returns Result.success(movieItem)

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockLoader.loadSeasons(any()) }
        assertTrue(state.seasons.isEmpty())
        assertFalse(state.isSeries)
        assertFalse(state.hasSeasons)
    }

    @Test
    fun `loadItem sets error on failure`() = testScope.runTest {
        coEvery { mockLoader.loadItem("item-1") } returns Result.failure(Exception("Network error"))

        val state = createState("item-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals("Network error", state.error)
        assertFalse(state.isLoading)
        assertNull(state.item)
    }

    // endregion

    // region loadItem Tests - Series

    @Test
    fun `loadItem loads series with seasons and episodes`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1, season2))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.success(listOf(episode1, episode2))

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals(seriesItem, state.item)
        assertEquals(listOf(season1, season2), state.seasons)
        assertEquals(season1, state.selectedSeason)
        assertEquals(listOf(episode1, episode2), state.episodes)
        assertTrue(state.isSeries)
        assertTrue(state.hasSeasons)
        assertTrue(state.hasEpisodes)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadItem selects first season by default`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1, season2))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.success(emptyList())

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals(season1, state.selectedSeason)
    }

    @Test
    fun `loadItem sets error on seasons failure`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.failure(Exception("Seasons error"))

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals("Seasons error", state.error)
        assertTrue(state.seasons.isEmpty())
    }

    @Test
    fun `loadItem sets error on episodes failure`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.failure(Exception("Episodes error"))

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals("Episodes error", state.error)
        assertTrue(state.episodes.isEmpty())
    }

    // endregion

    // region selectSeason Tests

    @Test
    fun `selectSeason changes selected season and loads episodes`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1, season2))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.success(listOf(episode1))
        coEvery { mockLoader.loadEpisodes("series-1", "season-2") } returns Result.success(listOf(episode2))

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertEquals(season1, state.selectedSeason)

        state.selectSeason(season2)
        advanceUntilIdle()

        assertEquals(season2, state.selectedSeason)
        assertEquals(listOf(episode2), state.episodes)
    }

    @Test
    fun `selectSeason does nothing if same season selected`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.success(listOf(episode1))

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        // loadEpisodes called once during loadItem
        coVerify(exactly = 1) { mockLoader.loadEpisodes("series-1", "season-1") }

        state.selectSeason(season1)
        advanceUntilIdle()

        // Should not call again since same season
        coVerify(exactly = 1) { mockLoader.loadEpisodes("series-1", "season-1") }
    }

    // endregion

    // region refreshEpisodes Tests

    @Test
    fun `refreshEpisodes reloads episodes for selected season`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.success(listOf(episode1))

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockLoader.loadEpisodes("series-1", "season-1") }

        state.refreshEpisodes()
        advanceUntilIdle()

        coVerify(exactly = 2) { mockLoader.loadEpisodes("series-1", "season-1") }
    }

    @Test
    fun `refreshEpisodes does nothing without item`() = testScope.runTest {
        val state = createState("series-1")

        state.refreshEpisodes()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockLoader.loadEpisodes(any(), any()) }
    }

    @Test
    fun `refreshEpisodes does nothing without selected season`() = testScope.runTest {
        coEvery { mockLoader.loadItem("movie-1") } returns Result.success(movieItem)

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        state.refreshEpisodes()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockLoader.loadEpisodes(any(), any()) }
    }

    // endregion

    // region Property Tests

    @Test
    fun `isSeries returns true for series item`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(emptyList())

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertTrue(state.isSeries)
    }

    @Test
    fun `isSeries returns false for movie item`() = testScope.runTest {
        coEvery { mockLoader.loadItem("movie-1") } returns Result.success(movieItem)

        val state = createState("movie-1")
        state.loadItem()
        advanceUntilIdle()

        assertFalse(state.isSeries)
    }

    @Test
    fun `hasSeasons returns true when series has seasons`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1))
        coEvery { mockLoader.loadEpisodes(any(), any()) } returns Result.success(emptyList())

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertTrue(state.hasSeasons)
    }

    @Test
    fun `hasSeasons returns false when series has no seasons`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(emptyList())

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertFalse(state.hasSeasons)
    }

    @Test
    fun `hasEpisodes returns true when episodes loaded`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.success(listOf(episode1))

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertTrue(state.hasEpisodes)
    }

    @Test
    fun `hasEpisodes returns false when no episodes`() = testScope.runTest {
        coEvery { mockLoader.loadItem("series-1") } returns Result.success(seriesItem)
        coEvery { mockLoader.loadSeasons("series-1") } returns Result.success(listOf(season1))
        coEvery { mockLoader.loadEpisodes("series-1", "season-1") } returns Result.success(emptyList())

        val state = createState("series-1")
        state.loadItem()
        advanceUntilIdle()

        assertFalse(state.hasEpisodes)
    }

    // endregion

    // Helper methods

    private fun createState(itemId: String): DetailScreenState {
        return DetailScreenState(itemId, mockLoader, testScope)
    }
}
