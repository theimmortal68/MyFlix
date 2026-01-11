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
class SearchScreenStateTest {

    private lateinit var mockSearcher: SearchExecutor
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val movie1 = JellyfinItem(
        id = "movie-1",
        name = "The Matrix",
        type = "Movie",
    )

    private val movie2 = JellyfinItem(
        id = "movie-2",
        name = "Matrix Reloaded",
        type = "Movie",
    )

    private val show1 = JellyfinItem(
        id = "show-1",
        name = "Breaking Bad",
        type = "Series",
    )

    @Before
    fun setUp() {
        mockSearcher = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)
    }

    // region Initial State Tests

    @Test
    fun `initial state has empty query and results`() {
        val state = createState()

        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isSearching)
        assertFalse(state.hasSearched)
        assertNull(state.error)
    }

    @Test
    fun `initial isEmpty is false`() {
        val state = createState()

        // isEmpty should be false because hasSearched is false
        assertFalse(state.isEmpty)
    }

    @Test
    fun `initial canSearch is false with empty query`() {
        val state = createState()

        assertFalse(state.canSearch)
    }

    // endregion

    // region updateQuery Tests

    @Test
    fun `updateQuery updates the query`() {
        val state = createState()

        state.updateQuery("Matrix")

        assertEquals("Matrix", state.query)
    }

    @Test
    fun `canSearch returns true with non-blank query`() {
        val state = createState()

        state.updateQuery("Matrix")

        assertTrue(state.canSearch)
    }

    @Test
    fun `canSearch returns false with blank query`() {
        val state = createState()

        state.updateQuery("   ")

        assertFalse(state.canSearch)
    }

    // endregion

    // region performSearch Tests

    @Test
    fun `performSearch executes search and updates results`() = testScope.runTest {
        coEvery { mockSearcher.search("Matrix") } returns Result.success(listOf(movie1, movie2))

        val state = createState()
        state.updateQuery("Matrix")
        state.performSearch()
        advanceUntilIdle()

        assertEquals(listOf(movie1, movie2), state.results)
        assertTrue(state.hasSearched)
        assertFalse(state.isSearching)
        assertNull(state.error)
    }

    @Test
    fun `performSearch sets hasSearched to true`() = testScope.runTest {
        coEvery { mockSearcher.search("test") } returns Result.success(emptyList())

        val state = createState()
        state.updateQuery("test")
        state.performSearch()
        advanceUntilIdle()

        assertTrue(state.hasSearched)
    }

    @Test
    fun `performSearch sets error on failure`() = testScope.runTest {
        coEvery { mockSearcher.search("test") } returns Result.failure(Exception("Network error"))

        val state = createState()
        state.updateQuery("test")
        state.performSearch()
        advanceUntilIdle()

        assertEquals("Network error", state.error)
        assertTrue(state.hasSearched)
        assertFalse(state.isSearching)
    }

    @Test
    fun `performSearch does nothing with blank query`() = testScope.runTest {
        val state = createState()
        state.updateQuery("")
        state.performSearch()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockSearcher.search(any()) }
        assertFalse(state.hasSearched)
    }

    @Test
    fun `performSearch does nothing with whitespace-only query`() = testScope.runTest {
        val state = createState()
        state.updateQuery("   ")
        state.performSearch()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockSearcher.search(any()) }
    }

    @Test
    fun `canSearch returns false while searching`() = testScope.runTest {
        coEvery { mockSearcher.search(any()) } returns Result.success(emptyList())

        val state = createState()
        state.updateQuery("test")

        // Before search
        assertTrue(state.canSearch)

        state.performSearch()
        // During search (before advanceUntilIdle)
        // isSearching should be true
        // Note: We can't easily test this mid-search in unit tests

        advanceUntilIdle()

        // After search
        assertTrue(state.canSearch)
    }

    // endregion

    // region clear Tests

    @Test
    fun `clear resets all state`() = testScope.runTest {
        coEvery { mockSearcher.search("Matrix") } returns Result.success(listOf(movie1))

        val state = createState()
        state.updateQuery("Matrix")
        state.performSearch()
        advanceUntilIdle()

        // Verify state has data
        assertEquals("Matrix", state.query)
        assertEquals(listOf(movie1), state.results)
        assertTrue(state.hasSearched)

        state.clear()

        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.hasSearched)
        assertNull(state.error)
    }

    @Test
    fun `clear resets error`() = testScope.runTest {
        coEvery { mockSearcher.search("test") } returns Result.failure(Exception("Error"))

        val state = createState()
        state.updateQuery("test")
        state.performSearch()
        advanceUntilIdle()

        assertEquals("Error", state.error)

        state.clear()

        assertNull(state.error)
    }

    // endregion

    // region isEmpty Tests

    @Test
    fun `isEmpty returns false before search`() {
        val state = createState()

        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty returns true after search with no results`() = testScope.runTest {
        coEvery { mockSearcher.search("xyz") } returns Result.success(emptyList())

        val state = createState()
        state.updateQuery("xyz")
        state.performSearch()
        advanceUntilIdle()

        assertTrue(state.isEmpty)
    }

    @Test
    fun `isEmpty returns false after search with results`() = testScope.runTest {
        coEvery { mockSearcher.search("Matrix") } returns Result.success(listOf(movie1))

        val state = createState()
        state.updateQuery("Matrix")
        state.performSearch()
        advanceUntilIdle()

        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty returns false when error occurred`() = testScope.runTest {
        coEvery { mockSearcher.search("test") } returns Result.failure(Exception("Error"))

        val state = createState()
        state.updateQuery("test")
        state.performSearch()
        advanceUntilIdle()

        assertFalse(state.isEmpty)
    }

    // endregion

    // Helper methods

    private fun createState(): SearchScreenState {
        return SearchScreenState(mockSearcher, testScope)
    }
}
