package dev.jausc.myflix.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
 * UI state for Seerr search screen.
 */
data class SeerrSearchUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val results: List<SeerrMedia> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = false,
    val currentPage: Int = 1,
    val totalResults: Int = 0,
)

/**
 * ViewModel for Seerr search functionality.
 * Provides debounced search with pagination support.
 */
class SeerrSearchViewModel(
    private val seerrRepository: SeerrRepository,
) : ViewModel() {

    /**
     * Factory for creating SeerrSearchViewModel with manual dependency injection.
     */
    class Factory(
        private val seerrRepository: SeerrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeerrSearchViewModel(seerrRepository) as T
    }

    private val _uiState = MutableStateFlow(SeerrSearchUiState())
    val uiState: StateFlow<SeerrSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    companion object {
        private const val DEBOUNCE_DELAY_MS = 300L
        private const val TAG = "SeerrSearchViewModel"
    }

    /**
     * Perform a debounced search.
     * Resets pagination to page 1.
     *
     * @param query Search query string
     */
    fun search(query: String) {
        _uiState.update { it.copy(query = query) }

        // Cancel previous search job
        searchJob?.cancel()

        if (query.isBlank()) {
            clearSearch()
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce the search
            delay(DEBOUNCE_DELAY_MS)

            _uiState.update { it.copy(isLoading = true, error = null, currentPage = 1) }

            seerrRepository.search(query, page = 1)
                .onSuccess { result ->
                    val hasMore = result.page < result.totalPages
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = result.results,
                            hasMore = hasMore,
                            currentPage = 1,
                            totalResults = result.totalResults,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Search failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Search failed",
                        )
                    }
                }
        }
    }

    /**
     * Load more search results (pagination).
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMore || state.query.isBlank()) {
            return
        }

        val nextPage = state.currentPage + 1

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            seerrRepository.search(state.query, page = nextPage)
                .onSuccess { result ->
                    val hasMore = result.page < result.totalPages
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = it.results + result.results,
                            hasMore = hasMore,
                            currentPage = nextPage,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Load more failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load more results",
                        )
                    }
                }
        }
    }

    /**
     * Clear search results and reset state.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            SeerrSearchUiState(query = "")
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
