package dev.jausc.myflix.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the search screen.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<JellyfinItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
) {
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
}

/**
 * ViewModel for the mobile search screen.
 * Manages search query, results, and loading state.
 */
class SearchViewModel(
    private val jellyfinClient: JellyfinClient,
) : ViewModel() {

    /**
     * Factory for creating SearchViewModel with manual dependency injection.
     */
    class Factory(
        private val jellyfinClient: JellyfinClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(jellyfinClient) as T
        }
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * Update the search query.
     */
    fun updateQuery(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
    }

    /**
     * Execute a search with the current query.
     */
    fun performSearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            jellyfinClient.search(query)
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            results = items,
                            hasSearched = true,
                            isSearching = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Search failed",
                            hasSearched = true,
                            isSearching = false,
                        )
                    }
                }
        }
    }

    /**
     * Clear the search query and results.
     */
    fun clear() {
        _uiState.update {
            SearchUiState()
        }
    }
}
