package dev.jausc.myflix.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.jausc.myflix.core.common.model.JellyfinItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for SearchScreen.
 * Manages search query, results, and loading state across TV and mobile platforms.
 */
@Stable
class SearchScreenState(
    private val searcher: SearchExecutor,
    private val scope: CoroutineScope,
) {
    var query by mutableStateOf("")
        private set

    var results by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var isSearching by mutableStateOf(false)
        private set

    var hasSearched by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /**
     * Whether the search returned no results after a search was performed.
     */
    val isEmpty: Boolean
        get() = hasSearched && results.isEmpty() && !isSearching && error == null

    /**
     * Whether the search button should be enabled.
     */
    val canSearch: Boolean
        get() = query.isNotBlank() && !isSearching

    /**
     * Update the search query.
     */
    fun updateQuery(newQuery: String) {
        query = newQuery
    }

    /**
     * Execute a search with the current query.
     */
    fun performSearch() {
        if (query.isBlank()) return

        scope.launch {
            isSearching = true
            error = null

            searcher.search(query)
                .onSuccess { items ->
                    results = items
                }
                .onFailure { throwable ->
                    error = throwable.message ?: "Search failed"
                }

            hasSearched = true
            isSearching = false
        }
    }

    /**
     * Clear the search query and results.
     */
    fun clear() {
        query = ""
        results = emptyList()
        hasSearched = false
        error = null
    }
}

/**
 * Search executor interface for dependency injection.
 * Abstracts the API client to allow for testing and flexibility.
 */
fun interface SearchExecutor {
    suspend fun search(query: String): Result<List<JellyfinItem>>
}

/**
 * Creates and remembers a [SearchScreenState].
 *
 * @param searcher Function to execute search queries
 * @return A [SearchScreenState] for managing search UI state
 */
@Composable
fun rememberSearchScreenState(
    searcher: SearchExecutor,
): SearchScreenState {
    val scope = rememberCoroutineScope()
    return remember {
        SearchScreenState(searcher, scope)
    }
}
