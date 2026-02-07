package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A suggested result shown while typing.
 */
data class SearchSuggestion(
    val id: String,
    val name: String,
    val type: String,
)

/**
 * UI state for the unified search screen.
 */
data class SearchUiState(
    val query: String = "",
    val movies: List<JellyfinItem> = emptyList(),
    val series: List<JellyfinItem> = emptyList(),
    val episodes: List<JellyfinItem> = emptyList(),
    val discoverResults: List<SeerrMedia> = emptyList(),
    val suggestions: List<SearchSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
) {
    /**
     * Whether the search returned no results across all categories.
     */
    val isEmpty: Boolean
        get() = hasSearched && movies.isEmpty() && series.isEmpty() &&
            episodes.isEmpty() && discoverResults.isEmpty() && !isSearching && error == null

    /**
     * Whether any results exist.
     */
    val hasResults: Boolean
        get() = movies.isNotEmpty() || series.isNotEmpty() ||
            episodes.isNotEmpty() || discoverResults.isNotEmpty()

    /**
     * Whether the search button should be enabled.
     */
    val canSearch: Boolean
        get() = query.isNotBlank() && !isSearching
}

private const val SUGGESTION_DEBOUNCE_MS = 300L
private const val MIN_SUGGESTION_CHARS = 3
private const val SUGGESTION_LIMIT = 8

/**
 * Shared ViewModel for the unified search screen.
 * Searches Jellyfin (single call, split client-side) and Seerr discover in parallel.
 */
class SearchViewModel(
    private val jellyfinClient: JellyfinClient,
    private val seerrRepository: SeerrRepository?,
) : ViewModel() {

    class Factory(
        private val jellyfinClient: JellyfinClient,
        private val seerrRepository: SeerrRepository?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(jellyfinClient, seerrRepository) as T
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null

    /**
     * Update the search query and trigger debounced suggestions.
     */
    fun updateQuery(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        suggestionJob?.cancel()
        if (newQuery.length < MIN_SUGGESTION_CHARS) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        suggestionJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            jellyfinClient.search(newQuery, limit = SUGGESTION_LIMIT, includeItemTypes = "Movie,Series")
                .onSuccess { items ->
                    val suggestions = items.map { item ->
                        SearchSuggestion(
                            id = item.id,
                            name = item.name,
                            type = item.type ?: "",
                        )
                    }
                    _uiState.update { it.copy(suggestions = suggestions) }
                }
        }
    }

    /**
     * Execute search across Jellyfin (single call, split client-side) and Seerr.
     */
    fun performSearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        // Clear suggestions when performing full search
        suggestionJob?.cancel()
        _uiState.update { it.copy(suggestions = emptyList()) }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            val jellyfinResult = jellyfinClient.search(query)
            val discoverResults = seerrRepository?.search(query)
                ?.getOrNull()?.results ?: emptyList()

            jellyfinResult
                .onSuccess { items ->
                    // Collect TMDB IDs from Jellyfin results to filter duplicates from Discover
                    val libraryTmdbIds = items.mapNotNull { item ->
                        item.providerIds?.get("Tmdb")?.toIntOrNull()
                    }.toSet()
                    val filteredDiscover = discoverResults.filter { media ->
                        val effectiveTmdbId = media.tmdbId ?: media.id
                        effectiveTmdbId !in libraryTmdbIds
                    }

                    _uiState.update {
                        it.copy(
                            movies = items.filter { item -> item.type == "Movie" },
                            series = items.filter { item -> item.type == "Series" },
                            episodes = items.filter { item -> item.type == "Episode" },
                            discoverResults = filteredDiscover,
                            hasSearched = true,
                            isSearching = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            movies = emptyList(),
                            series = emptyList(),
                            episodes = emptyList(),
                            discoverResults = discoverResults,
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
        suggestionJob?.cancel()
        _uiState.update { SearchUiState() }
    }
}
