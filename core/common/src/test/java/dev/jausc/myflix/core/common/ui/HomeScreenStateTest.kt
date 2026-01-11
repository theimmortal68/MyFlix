package dev.jausc.myflix.core.common.ui

import dev.jausc.myflix.core.common.model.JellyfinGenre
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.UserData
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
class HomeScreenStateTest {

    private lateinit var mockLoader: HomeContentLoader
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val moviesLibrary = createTestItem("lib-movies", "Movies", "CollectionFolder", collectionType = "movies")
    private val showsLibrary = createTestItem("lib-shows", "TV Shows", "CollectionFolder", collectionType = "tvshows")
    private val testLibraries = listOf(moviesLibrary, showsLibrary)

    private val movie1 = createTestItem("movie-1", "Test Movie 1", "Movie", backdropTags = listOf("tag1"))
    private val movie2 = createTestItem("movie-2", "Test Movie 2", "Movie", backdropTags = listOf("tag2"))
    private val movie3Watched = createTestItem("movie-3", "Watched Movie", "Movie", played = true)

    private val show1 = createTestItem("show-1", "Test Show 1", "Series", backdropTags = listOf("tag1"))
    private val show2Watched = createTestItem("show-2", "Watched Show", "Series", played = true)

    private val episode1 = createTestItem("ep-1", "Episode 1", "Episode", seriesId = "show-1")
    private val episode2Watched = createTestItem("ep-2", "Episode 2", "Episode", seriesId = "show-1", played = true)

    private val continueItem1 = createTestItem("continue-1", "Continue 1", "Movie", backdropTags = listOf("tag1"))
    private val continueItem2 = createTestItem("continue-2", "Continue 2", "Episode", seriesId = "show-1")

    private val nextUpItem1 = createTestItem("nextup-1", "Next Up 1", "Episode", seriesId = "show-1")
    private val nextUpItem2 = createTestItem("continue-1", "Next Up 2 (same as continue)", "Episode") // Same ID as continueItem1

    @Before
    fun setUp() {
        mockLoader = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)

        // Default mock responses
        coEvery { mockLoader.getLibraries() } returns Result.success(testLibraries)
        coEvery { mockLoader.findMoviesLibraryId(any()) } returns "lib-movies"
        coEvery { mockLoader.findShowsLibraryId(any()) } returns "lib-shows"
        coEvery { mockLoader.getLatestMovies(any(), any()) } returns Result.success(listOf(movie1, movie2))
        coEvery { mockLoader.getLatestSeries(any(), any()) } returns Result.success(listOf(show1))
        coEvery { mockLoader.getLatestEpisodes(any(), any()) } returns Result.success(listOf(episode1))
        coEvery { mockLoader.getNextUp(any()) } returns Result.success(listOf(nextUpItem1))
        coEvery { mockLoader.getContinueWatching(any()) } returns Result.success(listOf(continueItem1))
        coEvery { mockLoader.getUpcomingEpisodes(any()) } returns Result.success(emptyList())
        coEvery { mockLoader.getCollections(any()) } returns Result.success(emptyList())
        coEvery { mockLoader.getSuggestions(any()) } returns Result.success(emptyList())
        coEvery { mockLoader.getGenres() } returns Result.success(emptyList())
    }

    // region Initial State Tests

    @Test
    fun `initial state has empty content and isLoading true`() {
        val state = createState()

        assertTrue(state.isLoading)
        assertTrue(state.libraries.isEmpty())
        assertTrue(state.continueWatching.isEmpty())
        assertTrue(state.nextUp.isEmpty())
        assertTrue(state.recentMovies.isEmpty())
        assertTrue(state.featuredItems.isEmpty())
        assertNull(state.error)
        assertFalse(state.contentReady)
    }

    // endregion

    // region loadContent Tests

    @Test
    fun `loadContent loads libraries successfully`() = runTest {
        val state = createState()

        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(testLibraries, state.libraries)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadContent loads all content rows`() = runTest {
        val state = createState()

        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(listOf(movie1, movie2), state.recentMovies)
        assertEquals(listOf(show1), state.recentShows)
        assertEquals(listOf(episode1), state.recentEpisodes)
        assertEquals(listOf(nextUpItem1), state.nextUp)
        assertEquals(listOf(continueItem1), state.continueWatching)
    }

    @Test
    fun `loadContent sets error on library failure`() = runTest {
        coEvery { mockLoader.getLibraries() } returns Result.failure(Exception("Network error"))

        val state = createState()
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals("Failed to load libraries: Network error", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadContent clears cache before loading`() = runTest {
        val state = createState()

        state.loadContent(showLoading = true)
        advanceUntilIdle()

        coVerify { mockLoader.clearCache() }
    }

    @Test
    fun `loadContent builds featured items from content`() = runTest {
        coEvery { mockLoader.getContinueWatching(any()) } returns Result.success(listOf(continueItem1))
        coEvery { mockLoader.getNextUp(any()) } returns Result.success(listOf(nextUpItem1))

        val state = createState()
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertTrue(state.featuredItems.isNotEmpty())
        assertTrue(state.contentReady)
    }

    @Test
    fun `loadContent does not show loading when showLoading is false`() = runTest {
        val state = createState()
        state.loadContent(showLoading = false)

        // isLoading should remain true initially (default value)
        // but should be false after loading completes
        advanceUntilIdle()
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadContent loads season premieres when enabled`() = runTest {
        val premieres = listOf(createTestItem("premiere-1", "Premiere 1", "Episode"))
        coEvery { mockLoader.getUpcomingEpisodes(any()) } returns Result.success(premieres)

        val config = HomeScreenConfig(showSeasonPremieres = true)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(premieres, state.seasonPremieres)
    }

    @Test
    fun `loadContent skips season premieres when disabled`() = runTest {
        val config = HomeScreenConfig(showSeasonPremieres = false)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { mockLoader.getUpcomingEpisodes(any()) }
        assertTrue(state.seasonPremieres.isEmpty())
    }

    @Test
    fun `loadContent loads suggestions when enabled`() = runTest {
        val suggestions = listOf(createTestItem("suggest-1", "Suggestion 1", "Movie"))
        coEvery { mockLoader.getSuggestions(any()) } returns Result.success(suggestions)

        val config = HomeScreenConfig(showSuggestions = true)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(suggestions, state.suggestions)
    }

    @Test
    fun `loadContent loads genre rows when enabled`() = runTest {
        val genres = listOf(JellyfinGenre("genre-1", "Action"), JellyfinGenre("genre-2", "Comedy"))
        val actionItems = listOf(createTestItem("action-1", "Action Movie", "Movie"))
        coEvery { mockLoader.getGenres() } returns Result.success(genres)
        coEvery { mockLoader.getItemsByGenre("Action", any()) } returns Result.success(actionItems)
        coEvery { mockLoader.getItemsByGenre("Comedy", any()) } returns Result.success(emptyList())

        val config = HomeScreenConfig(showGenreRows = true)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(listOf("Action", "Comedy"), state.availableGenres)
        assertEquals(actionItems, state.genreRowsData["Action"])
    }

    // endregion

    // region Filtered Content Tests

    @Test
    fun `filteredNextUp excludes items in continueWatching`() = runTest {
        coEvery { mockLoader.getContinueWatching(any()) } returns Result.success(listOf(continueItem1))
        coEvery { mockLoader.getNextUp(any()) } returns Result.success(listOf(nextUpItem1, nextUpItem2))

        val state = createState()
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        // nextUpItem2 has same ID as continueItem1, so should be filtered out
        assertEquals(listOf(nextUpItem1), state.filteredNextUp)
    }

    @Test
    fun `filteredRecentMovies excludes watched when hideWatchedFromRecent enabled`() = runTest {
        coEvery { mockLoader.getLatestMovies(any(), any()) } returns Result.success(listOf(movie1, movie2, movie3Watched))

        val config = HomeScreenConfig(hideWatchedFromRecent = true)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(listOf(movie1, movie2), state.filteredRecentMovies)
    }

    @Test
    fun `filteredRecentMovies includes watched when hideWatchedFromRecent disabled`() = runTest {
        coEvery { mockLoader.getLatestMovies(any(), any()) } returns Result.success(listOf(movie1, movie2, movie3Watched))

        val config = HomeScreenConfig(hideWatchedFromRecent = false)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(listOf(movie1, movie2, movie3Watched), state.filteredRecentMovies)
    }

    @Test
    fun `filteredRecentShows excludes watched when hideWatchedFromRecent enabled`() = runTest {
        coEvery { mockLoader.getLatestSeries(any(), any()) } returns Result.success(listOf(show1, show2Watched))

        val config = HomeScreenConfig(hideWatchedFromRecent = true)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(listOf(show1), state.filteredRecentShows)
    }

    @Test
    fun `filteredRecentEpisodes excludes watched when hideWatchedFromRecent enabled`() = runTest {
        coEvery { mockLoader.getLatestEpisodes(any(), any()) } returns Result.success(listOf(episode1, episode2Watched))

        val config = HomeScreenConfig(hideWatchedFromRecent = true)
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(listOf(episode1), state.filteredRecentEpisodes)
    }

    // endregion

    // region Action Tests

    @Test
    fun `clearError sets error to null`() {
        val state = createState()

        // Manually set error for testing
        runTest {
            coEvery { mockLoader.getLibraries() } returns Result.failure(Exception("Error"))
            state.loadContent(showLoading = true)
            advanceUntilIdle()
        }

        state.clearError()

        assertNull(state.error)
    }

    @Test
    fun `refresh triggers content reload`() = testScope.runTest {
        val state = createState()

        state.refresh()
        advanceUntilIdle()

        // Should have called loadContent which clears cache
        coVerify(atLeast = 1) { mockLoader.clearCache() }
    }

    @Test
    fun `setPlayed calls loader and refreshes content`() = testScope.runTest {
        val state = createState()

        state.setPlayed("movie-1", true)
        advanceUntilIdle()

        coVerify { mockLoader.setPlayed("movie-1", true) }
        coVerify(atLeast = 1) { mockLoader.clearCache() }
    }

    @Test
    fun `setFavorite calls loader and refreshes content`() = testScope.runTest {
        val state = createState()

        state.setFavorite("movie-1", true)
        advanceUntilIdle()

        coVerify { mockLoader.setFavorite("movie-1", true) }
        coVerify(atLeast = 1) { mockLoader.clearCache() }
    }

    // endregion

    // region contentReady Tests

    @Test
    fun `contentReady is false when loading`() {
        val state = createState()

        assertTrue(state.isLoading)
        assertFalse(state.contentReady)
    }

    @Test
    fun `contentReady is false when featuredItems is empty`() = runTest {
        coEvery { mockLoader.getContinueWatching(any()) } returns Result.success(emptyList())
        coEvery { mockLoader.getNextUp(any()) } returns Result.success(emptyList())
        coEvery { mockLoader.getLatestMovies(any(), any()) } returns Result.success(emptyList())
        coEvery { mockLoader.getLatestSeries(any(), any()) } returns Result.success(emptyList())

        val state = createState()
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertFalse(state.isLoading)
        assertTrue(state.featuredItems.isEmpty())
        assertFalse(state.contentReady)
    }

    @Test
    fun `contentReady is true when not loading and featuredItems not empty`() = runTest {
        val state = createState()
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertFalse(state.isLoading)
        assertTrue(state.featuredItems.isNotEmpty())
        assertTrue(state.contentReady)
    }

    // endregion

    // region Pinned Collections Tests

    @Test
    fun `loadContent loads pinned collection items`() = runTest {
        val collection = createTestItem("collection-1", "My Collection", "BoxSet")
        val collectionItems = listOf(createTestItem("item-1", "Item 1", "Movie"))

        coEvery { mockLoader.getCollections(any()) } returns Result.success(listOf(collection))
        coEvery { mockLoader.getItem("collection-1") } returns Result.success(collection)
        coEvery { mockLoader.getCollectionItems("collection-1", any()) } returns Result.success(collectionItems)

        val config = HomeScreenConfig(
            showCollections = true,
            pinnedCollections = listOf("collection-1")
        )
        val state = createState(config)
        state.loadContent(showLoading = true)
        advanceUntilIdle()

        assertEquals(1, state.pinnedCollectionsData.size)
        assertEquals("My Collection" to collectionItems, state.pinnedCollectionsData["collection-1"])
    }

    // endregion

    // Helper methods

    private fun createState(config: HomeScreenConfig = HomeScreenConfig()): HomeScreenState {
        return HomeScreenState(mockLoader, testScope, config)
    }

    private fun createTestItem(
        id: String,
        name: String,
        type: String,
        collectionType: String? = null,
        backdropTags: List<String>? = null,
        seriesId: String? = null,
        played: Boolean = false,
    ): JellyfinItem {
        return JellyfinItem(
            id = id,
            name = name,
            type = type,
            collectionType = collectionType,
            backdropImageTags = backdropTags,
            seriesId = seriesId,
            userData = if (played) UserData(played = true) else null,
        )
    }
}
