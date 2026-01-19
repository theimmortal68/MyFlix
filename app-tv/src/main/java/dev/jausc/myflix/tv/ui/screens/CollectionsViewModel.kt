package dev.jausc.myflix.tv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 100
private const val PREFETCH_DELAY_MS = 50L
private const val PREFETCH_PARALLEL_PAGES = 3

/**
 * UI state for the collections library screen.
 */
data class CollectionsUiState(
    val items: List<JellyfinItem> = emptyList(),
    val totalRecordCount: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    /** Current letter filter for alphabet navigation (null = show all) */
    val currentLetter: Char? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty() && error == null

    val canLoadMore: Boolean
        get() = items.size < totalRecordCount && !isLoading && !isLoadingMore
}

/**
 * ViewModel for the Collections library screen.
 * Manages collections loading with pagination, sorting, and alphabet navigation.
 */
class CollectionsViewModel(
    private val jellyfinClient: JellyfinClient,
    private val excludeUniverseCollections: Boolean,
) : ViewModel() {

    /**
     * Factory for creating CollectionsViewModel with manual dependency injection.
     */
    class Factory(
        private val jellyfinClient: JellyfinClient,
        private val excludeUniverseCollections: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CollectionsViewModel(jellyfinClient, excludeUniverseCollections) as T
    }

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    init {
        loadCollections(resetList = true)
        startBackgroundPrefetch()
    }

    /**
     * Pre-fetch collection items in background for faster navigation.
     */
    private fun startBackgroundPrefetch() {
        viewModelScope.launch {
            // Wait for initial load to complete
            while (_uiState.value.isLoading) {
                delay(100)
            }

            val initialState = _uiState.value
            if (initialState.items.isEmpty() || initialState.error != null) return@launch

            val totalItems = initialState.totalRecordCount
            var apiStartIndex = PAGE_SIZE

            // Phase 1: Load remaining pages
            while (apiStartIndex < totalItems) {
                if (_uiState.value.currentLetter != null || _uiState.value.isLoading) {
                    delay(500)
                    continue
                }

                val pagesToFetch = (0 until PREFETCH_PARALLEL_PAGES)
                    .map { apiStartIndex + (it * PAGE_SIZE) }
                    .filter { it < totalItems }

                pagesToFetch.map { startIndex ->
                    async { prefetchPage(startIndex, null) }
                }.awaitAll()

                apiStartIndex += PREFETCH_PARALLEL_PAGES * PAGE_SIZE
                delay(PREFETCH_DELAY_MS)
            }

            // Phase 2: Pre-fetch first page of each letter
            val alphabet = listOf('#') + ('A'..'Z').toList()
            alphabet.chunked(PREFETCH_PARALLEL_PAGES).forEach { letterBatch ->
                if (_uiState.value.currentLetter != null || _uiState.value.isLoading) {
                    delay(500)
                    return@forEach
                }

                letterBatch.map { letter ->
                    async {
                        prefetchPage(0, if (letter == '#') "#" else letter.toString())
                    }
                }.awaitAll()

                delay(PREFETCH_DELAY_MS)
            }
        }
    }

    /**
     * Prefetch a page of collections into cache without updating UI.
     */
    private suspend fun prefetchPage(startIndex: Int, nameStartsWith: String?) {
        jellyfinClient.getCollectionsFiltered(
            limit = PAGE_SIZE,
            startIndex = startIndex,
            sortBy = null,
            sortOrder = null,
            nameStartsWith = nameStartsWith,
            excludeUniverseCollections = excludeUniverseCollections,
        )
    }

    /**
     * Load collections with pagination support.
     */
    private fun loadCollections(resetList: Boolean = false, startIndex: Int = 0) {
        viewModelScope.launch {
            if (resetList) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val currentState = _uiState.value
            val nameStartsWith = currentState.currentLetter?.let { letter ->
                if (letter == '#') "#" else letter.toString()
            }

            jellyfinClient.getCollectionsFiltered(
                limit = PAGE_SIZE,
                startIndex = startIndex,
                sortBy = null,
                sortOrder = null,
                nameStartsWith = nameStartsWith,
                excludeUniverseCollections = excludeUniverseCollections,
            )
                .onSuccess { result ->
                    _uiState.update { state ->
                        val newItems = if (resetList) {
                            result.items
                        } else {
                            (state.items + result.items).distinctBy { it.id }
                        }
                        state.copy(
                            items = newItems,
                            totalRecordCount = result.totalRecordCount,
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            error = e.message ?: "Failed to load collections",
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
        }
    }

    /**
     * Load more collections (pagination).
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.canLoadMore) {
            loadCollections(resetList = false, startIndex = currentState.items.size)
        }
    }

    /**
     * Jump to collections starting with a specific letter.
     */
    fun jumpToLetter(letter: Char) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), currentLetter = letter) }
            loadCollections(resetList = true)
        }
    }

    /**
     * Clear the letter filter and show all collections.
     */
    fun clearLetterFilter() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), currentLetter = null) }
            loadCollections(resetList = true)
        }
    }

    /**
     * Refresh collections.
     */
    fun refresh() {
        loadCollections(resetList = true)
    }
}
