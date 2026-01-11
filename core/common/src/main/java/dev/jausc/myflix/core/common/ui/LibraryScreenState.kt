package dev.jausc.myflix.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * State holder for LibraryScreen.
 * Manages library items loading and state across TV and mobile platforms.
 */
@Stable
class LibraryScreenState(
    val libraryId: String,
    val libraryName: String,
) {
    var items by mutableStateOf<List<JellyfinItem>>(emptyList())
        internal set

    var isLoading by mutableStateOf(true)
        internal set

    var error by mutableStateOf<String?>(null)
        internal set

    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty() && error == null
}

/**
 * Data loader interface for LibraryScreen.
 * Abstracts the API client to allow for testing and flexibility.
 */
fun interface LibraryLoader {
    suspend fun loadLibraryItems(libraryId: String): Result<List<JellyfinItem>>
}

/**
 * Creates and remembers a [LibraryScreenState] that automatically loads library items.
 *
 * @param libraryId The ID of the library to load
 * @param libraryName Display name of the library
 * @param loader Function to load library items from the API
 * @return A [LibraryScreenState] that will be populated with items
 */
@Composable
fun rememberLibraryScreenState(
    libraryId: String,
    libraryName: String,
    loader: LibraryLoader,
): LibraryScreenState {
    val state = remember(libraryId) {
        LibraryScreenState(libraryId, libraryName)
    }

    LaunchedEffect(libraryId) {
        state.isLoading = true
        state.error = null

        loader.loadLibraryItems(libraryId)
            .onSuccess { items ->
                state.items = items
            }
            .onFailure { throwable ->
                state.error = throwable.message ?: "Failed to load library"
            }

        state.isLoading = false
    }

    return state
}
