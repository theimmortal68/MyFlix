@file:Suppress("TooManyFunctions")

package dev.jausc.myflix.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 100

/**
 * UI state for the mobile library screen with filter support.
 */
data class LibraryUiState(
    val items: List<JellyfinItem> = emptyList(),
    val totalRecordCount: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val filterState: LibraryFilterState = LibraryFilterState.DEFAULT,
    val availableGenres: List<String> = emptyList(),
    val availableParentalRatings: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty() && error == null

    val canLoadMore: Boolean
        get() = items.size < totalRecordCount && !isLoading && !isLoadingMore
}

/**
 * ViewModel for the mobile library screen with Plex-style filtering.
 * Manages library items loading, filtering, sorting, and pagination.
 *
 * @param collectionType The library type ("movies", "tvshows", etc.) used for filtering
 */
class LibraryViewModel(
    private val libraryId: String,
    private val collectionType: String?,
    private val jellyfinClient: JellyfinClient,
    private val preferences: AppPreferences,
) : ViewModel() {

    /**
     * Factory for creating LibraryViewModel with manual dependency injection.
     */
    class Factory(
        private val libraryId: String,
        private val collectionType: String?,
        private val jellyfinClient: JellyfinClient,
        private val preferences: AppPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(libraryId, collectionType, jellyfinClient, preferences) as T
    }

    /**
     * Determine include item types based on collection type.
     * For TV shows: only include "Series" to filter out extras
     * For movies: only include "Movie" to filter out non-movie content
     */
    private val includeItemTypes: List<String>? = when (collectionType) {
        "tvshows" -> listOf("Series")
        "movies" -> listOf("Movie")
        else -> null
    }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // Load saved filter state from preferences
        val savedFilterState = preferences.getLibraryFilterState(libraryId)
        _uiState.update { it.copy(filterState = savedFilterState) }

        // Load genres and items
        loadGenres()
        loadLibraryItems(resetList = true)
    }

    /**
     * Load available genres for the library.
     */
    private fun loadGenres() {
        viewModelScope.launch {
            jellyfinClient.getGenres(libraryId)
                .onSuccess { genres ->
                    _uiState.update { state ->
                        state.copy(availableGenres = genres.map { it.name })
                    }
                }
        }
    }

    /**
     * Load library items with current filter state.
     */
    private fun loadLibraryItems(resetList: Boolean = false, startIndex: Int = 0) {
        viewModelScope.launch {
            if (resetList) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val filterState = _uiState.value.filterState

            // Build filter parameters
            val genres = filterState.selectedGenres.toList().takeIf { it.isNotEmpty() }
            val isPlayed = when (filterState.watchedFilter) {
                WatchedFilter.WATCHED -> true
                WatchedFilter.UNWATCHED -> false
                WatchedFilter.ALL -> null
            }
            val years = filterState.yearRange.toJellyfinParam()

            jellyfinClient.getLibraryItemsFiltered(
                libraryId = libraryId,
                limit = PAGE_SIZE,
                startIndex = startIndex,
                sortBy = filterState.sortBy.jellyfinValue,
                sortOrder = filterState.sortOrder.jellyfinValue,
                genres = genres,
                isPlayed = isPlayed,
                minCommunityRating = filterState.ratingFilter,
                years = years,
                officialRatings = filterState.selectedParentalRatings.toList().takeIf { it.isNotEmpty() },
                includeItemTypes = includeItemTypes,
            )
                .onSuccess { result ->
                    _uiState.update { state ->
                        val newItems = if (resetList) {
                            result.items
                        } else {
                            (state.items + result.items).distinctBy { it.id }
                        }
                        val parentalRatings = newItems
                            .mapNotNull { item ->
                                item.officialRating?.takeIf { it.isNotBlank() && it != "0" }
                            }
                            .distinct()
                            .sorted()
                        state.copy(
                            items = newItems,
                            totalRecordCount = result.totalRecordCount,
                            isLoading = false,
                            isLoadingMore = false,
                            availableParentalRatings = parentalRatings,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            error = e.message ?: "Failed to load library",
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
        }
    }

    /**
     * Load more items (pagination).
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.canLoadMore) {
            loadLibraryItems(resetList = false, startIndex = currentState.items.size)
        }
    }

    /**
     * Update sort option and order.
     */
    fun updateSort(sortBy: LibrarySortOption, sortOrder: SortOrder) {
        updateFilterState { it.copy(sortBy = sortBy, sortOrder = sortOrder) }
    }

    /**
     * Toggle view mode between poster and thumbnail.
     */
    fun toggleViewMode() {
        updateFilterState { state ->
            val newMode = when (state.viewMode) {
                LibraryViewMode.POSTER -> LibraryViewMode.THUMBNAIL
                LibraryViewMode.THUMBNAIL -> LibraryViewMode.POSTER
            }
            state.copy(viewMode = newMode)
        }
    }

    /**
     * Set view mode directly.
     */
    fun setViewMode(mode: LibraryViewMode) {
        updateFilterState { it.copy(viewMode = mode) }
    }

    /**
     * Toggle a genre selection.
     */
    fun toggleGenre(genre: String) {
        updateFilterState { state ->
            val newGenres = if (state.selectedGenres.contains(genre)) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(selectedGenres = newGenres)
        }
    }

    /**
     * Update watched filter.
     */
    fun updateWatchedFilter(filter: WatchedFilter) {
        updateFilterState { it.copy(watchedFilter = filter) }
    }

    /**
     * Update rating filter.
     */
    fun updateRatingFilter(rating: Float?) {
        updateFilterState { it.copy(ratingFilter = rating) }
    }

    /**
     * Update year range filter.
     */
    fun updateYearRange(range: YearRange) {
        updateFilterState { it.copy(yearRange = range) }
    }

    /**
     * Apply multiple filter changes at once (from filter sheet).
     */
    fun applyFilters(watchedFilter: WatchedFilter, ratingFilter: Float?, yearRange: YearRange) {
        updateFilterState { state ->
            state.copy(
                watchedFilter = watchedFilter,
                ratingFilter = ratingFilter,
                yearRange = yearRange,
            )
        }
    }

    /**
     * Clear all filters to defaults.
     */
    fun clearFilters() {
        updateFilterState { LibraryFilterState.DEFAULT.copy(viewMode = it.viewMode) }
    }

    /**
     * Get a random item ID for shuffle play.
     * Returns null if no items are available.
     */
    fun getShuffleItemId(): String? {
        val items = _uiState.value.items
        return if (items.isNotEmpty()) {
            items.random().id
        } else {
            null
        }
    }

    /**
     * Refresh library items.
     */
    fun refresh() {
        loadLibraryItems(resetList = true)
    }

    /**
     * Update filter state and reload items.
     */
    private fun updateFilterState(update: (LibraryFilterState) -> LibraryFilterState) {
        val currentState = _uiState.value.filterState
        val newState = update(currentState)

        // Only reload if filter actually changed (not just view mode)
        val filtersChanged = newState.copy(viewMode = currentState.viewMode) !=
            currentState.copy(viewMode = currentState.viewMode)

        _uiState.update { it.copy(filterState = newState) }

        // Save to preferences
        preferences.setLibraryFilterState(libraryId, newState)

        // Reload items if filters changed
        if (filtersChanged) {
            loadLibraryItems(resetList = true)
        }
    }
}
