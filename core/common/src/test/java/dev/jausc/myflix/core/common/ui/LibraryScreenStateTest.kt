package dev.jausc.myflix.core.common.ui

import dev.jausc.myflix.core.common.model.JellyfinItem
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryScreenStateTest {

    // Test data
    private val item1 = JellyfinItem(
        id = "item-1",
        name = "Test Item 1",
        type = "Movie",
    )

    private val item2 = JellyfinItem(
        id = "item-2",
        name = "Test Item 2",
        type = "Movie",
    )

    // region Initial State Tests

    @Test
    fun `initial state has correct library info`() {
        val state = LibraryScreenState("lib-1", "Movies")

        assertEquals("lib-1", state.libraryId)
        assertEquals("Movies", state.libraryName)
    }

    @Test
    fun `initial state has empty items and isLoading true`() {
        val state = LibraryScreenState("lib-1", "Movies")

        assertTrue(state.items.isEmpty())
        assertTrue(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `initial isEmpty is false when loading`() {
        val state = LibraryScreenState("lib-1", "Movies")

        // isEmpty should be false because isLoading is true
        assertFalse(state.isEmpty)
    }

    // endregion

    // region isEmpty Tests

    @Test
    fun `isEmpty returns false when loading`() {
        val state = LibraryScreenState("lib-1", "Movies")
        state.isLoading = true
        state.items = emptyList()
        state.error = null

        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty returns false when has items`() {
        val state = LibraryScreenState("lib-1", "Movies")
        state.isLoading = false
        state.items = listOf(item1, item2)
        state.error = null

        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty returns false when has error`() {
        val state = LibraryScreenState("lib-1", "Movies")
        state.isLoading = false
        state.items = emptyList()
        state.error = "Network error"

        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty returns true when not loading, no items, and no error`() {
        val state = LibraryScreenState("lib-1", "Movies")
        state.isLoading = false
        state.items = emptyList()
        state.error = null

        assertTrue(state.isEmpty)
    }

    // endregion

    // region State Modification Tests

    @Test
    fun `items can be set`() {
        val state = LibraryScreenState("lib-1", "Movies")

        state.items = listOf(item1, item2)

        assertEquals(listOf(item1, item2), state.items)
    }

    @Test
    fun `isLoading can be set`() {
        val state = LibraryScreenState("lib-1", "Movies")
        assertTrue(state.isLoading)

        state.isLoading = false

        assertFalse(state.isLoading)
    }

    @Test
    fun `error can be set`() {
        val state = LibraryScreenState("lib-1", "Movies")
        assertNull(state.error)

        state.error = "Failed to load"

        assertEquals("Failed to load", state.error)
    }

    @Test
    fun `error can be cleared`() {
        val state = LibraryScreenState("lib-1", "Movies")
        state.error = "Failed to load"

        state.error = null

        assertNull(state.error)
    }

    // endregion
}
