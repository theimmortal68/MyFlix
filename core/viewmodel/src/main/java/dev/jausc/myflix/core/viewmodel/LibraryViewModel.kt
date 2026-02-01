@file:Suppress("TooManyFunctions")

package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.model.SeriesStatusFilter
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange
import dev.jausc.myflix.core.common.model.isMovie
import dev.jausc.myflix.core.common.model.isSeries
import dev.jausc.myflix.core.common.preferences.AppPreferences
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
 * UI state for the library screen with filter support.
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
    /** Current letter filter for alphabet navigation (null = show all) */
    val currentLetter: Char? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty() && error == null

    val canLoadMore: Boolean
        get() = items.size < totalRecordCount && !isLoading && !isLoadingMore
}

/**
 * Shared ViewModel for the library screen with Plex-style filtering.
 * Manages library items loading, filtering, sorting, and pagination.
 *
 * @param libraryId The Jellyfin library ID
 * @param collectionType The library type ("movies", "tvshows", etc.) used for filtering
 * @param jellyfinClient The Jellyfin API client
 * @param preferences App preferences for saving filter state
 * @param enableBackgroundPrefetch Whether to enable background prefetching (recommended for TV)
 */
class LibraryViewModel(
    private val libraryId: String,
    private val collectionType: String?,
    private val jellyfinClient: JellyfinClient,
    private val preferences: AppPreferences,
    private val enableBackgroundPrefetch: Boolean = false,
) : ViewModel() {

    /**
     * Factory for creating LibraryViewModel with manual dependency injection.
     */
    class Factory(
        private val libraryId: String,
        private val collectionType: String?,
        private val jellyfinClient: JellyfinClient,
        private val preferences: AppPreferences,
        private val enableBackgroundPrefetch: Boolean = false,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(libraryId, collectionType, jellyfinClient, preferences, enableBackgroundPrefetch) as T
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

        // Start background pre-fetching after initial load (if enabled)
        if (enableBackgroundPrefetch) {
            startBackgroundPrefetch()
        }
    }

    /**
     * Pre-fetch library items in background for faster navigation.
     * Loads remaining pages of the main library in parallel batches,
     * then pre-fetches each letter for instant alphabet navigation.
     */
    private fun startBackgroundPrefetch() {
        viewModelScope.launch {
            // Wait for initial load to complete
            while (_uiState.value.isLoading) {
                delay(100)
            }

            // Only prefetch if we have items and no error
            val initialState = _uiState.value
            if (initialState.items.isEmpty() || initialState.error != null) return@launch

            val filterState = initialState.filterState
            val totalItems = initialState.totalRecordCount

            // Phase 1: Load remaining pages of main library in parallel batches
            // Start from PAGE_SIZE (first page already loaded), not items.size
            // because client-side filtering may have reduced visible items
            var apiStartIndex = PAGE_SIZE

            while (apiStartIndex < totalItems) {
                // Pause if user is actively filtering
                if (_uiState.value.currentLetter != null || _uiState.value.isLoading) {
                    delay(500)
                    continue
                }

                // Prefetch multiple pages in parallel
                val pagesToFetch = (0 until PREFETCH_PARALLEL_PAGES)
                    .map { apiStartIndex + (it * PAGE_SIZE) }
                    .filter { it < totalItems }

                pagesToFetch.map { startIndex ->
                    async {
                        prefetchPage(
                            startIndex = startIndex,
                            sortBy = filterState.sortBy.jellyfinValue,
                            sortOrder = filterState.sortOrder.jellyfinValue,
                            nameStartsWith = null,
                        )
                    }
                }.awaitAll()

                apiStartIndex += PREFETCH_PARALLEL_PAGES * PAGE_SIZE
                delay(PREFETCH_DELAY_MS)
            }

            // Phase 2: Pre-fetch first page of each letter in parallel batches
            val alphabet = listOf('#') + ('A'..'Z').toList()
            alphabet.chunked(PREFETCH_PARALLEL_PAGES).forEach { letterBatch ->
                // Pause if user is actively using the library
                if (_uiState.value.currentLetter != null || _uiState.value.isLoading) {
                    delay(500)
                    return@forEach
                }

                letterBatch.map { letter ->
                    async {
                        prefetchPage(
                            startIndex = 0,
                            sortBy = filterState.sortBy.jellyfinValue,
                            sortOrder = filterState.sortOrder.jellyfinValue,
                            nameStartsWith = if (letter == '#') "#" else letter.toString(),
                        )
                    }
                }.awaitAll()

                delay(PREFETCH_DELAY_MS)
            }
        }
    }

    /**
     * Prefetch a page of items into cache without updating UI.
     */
    private suspend fun prefetchPage(
        startIndex: Int,
        sortBy: String,
        sortOrder: String,
        nameStartsWith: String?,
    ) {
        // Make the API call - result gets cached by JellyfinClient
        jellyfinClient.getLibraryItemsFiltered(
            libraryId = libraryId,
            limit = PAGE_SIZE,
            startIndex = startIndex,
            sortBy = sortBy,
            sortOrder = sortOrder,
            genres = null,
            isPlayed = null,
            minCommunityRating = null,
            years = null,
            officialRatings = null,
            nameStartsWith = nameStartsWith,
            includeItemTypes = includeItemTypes,
        )
        // Result is cached - we don't need to do anything with it
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

            val currentState = _uiState.value
            val filterState = currentState.filterState

            // Build filter parameters
            val genres = filterState.selectedGenres.toList().takeIf { it.isNotEmpty() }
            val isPlayed = when (filterState.watchedFilter) {
                WatchedFilter.WATCHED -> true
                WatchedFilter.UNWATCHED -> false
                WatchedFilter.ALL -> null
            }
            val isFavorite = if (filterState.favoritesOnly) true else null
            val years = filterState.yearRange.toJellyfinParam()

            // Convert current letter to nameStartsWith parameter
            // '#' represents non-letter characters (numbers, symbols)
            val nameStartsWith = currentState.currentLetter?.let { letter ->
                if (letter == '#') "#" else letter.toString()
            }

            jellyfinClient.getLibraryItemsFiltered(
                libraryId = libraryId,
                limit = PAGE_SIZE,
                startIndex = startIndex,
                sortBy = filterState.sortBy.jellyfinValue,
                sortOrder = filterState.sortOrder.jellyfinValue,
                genres = genres,
                isPlayed = isPlayed,
                isFavorite = isFavorite,
                minCommunityRating = filterState.ratingFilter,
                years = years,
                officialRatings = filterState.selectedParentalRatings.toList().takeIf { it.isNotEmpty() },
                nameStartsWith = nameStartsWith,
                includeItemTypes = includeItemTypes,
                seriesStatus = filterState.seriesStatus.jellyfinValue,
            )
                .onSuccess { result ->
                    _uiState.update { state ->
                        val filteredItems = result.items.filterNot { item ->
                            // Filter out folders
                            item.type == "Folder" ||
                                // Filter out collections (BoxSet) from movie/show libraries
                                item.type == "BoxSet" ||
                                // Filter out movies without media sources/images
                                (item.isMovie &&
                                    item.mediaSources.isNullOrEmpty() &&
                                    item.imageTags?.primary == null &&
                                    item.backdropImageTags.isNullOrEmpty()) ||
                                // Filter out series without episodes (like upcoming shows)
                                (item.isSeries && (item.recursiveItemCount ?: 0) == 0)
                        }
                        val newItems = if (resetList) {
                            filteredItems
                        } else {
                            (state.items + filteredItems).distinctBy { it.id }
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
     * Toggle parental rating selection.
     */
    fun toggleParentalRating(rating: String) {
        updateFilterState { state ->
            val newRatings = if (state.selectedParentalRatings.contains(rating)) {
                state.selectedParentalRatings - rating
            } else {
                state.selectedParentalRatings + rating
            }
            state.copy(selectedParentalRatings = newRatings)
        }
    }

    /**
     * Clear all selected genres.
     */
    fun clearGenres() {
        updateFilterState { it.copy(selectedGenres = emptySet()) }
    }

    /**
     * Clear all selected parental ratings.
     */
    fun clearParentalRatings() {
        updateFilterState { it.copy(selectedParentalRatings = emptySet()) }
    }

    /**
     * Update year range filter.
     */
    fun updateYearRange(range: YearRange) {
        updateFilterState { it.copy(yearRange = range) }
    }

    /**
     * Update series status filter (Continuing/Ended).
     */
    fun updateSeriesStatus(status: SeriesStatusFilter) {
        updateFilterState { it.copy(seriesStatus = status) }
    }

    /**
     * Toggle favorites-only filter.
     */
    fun toggleFavoritesOnly() {
        updateFilterState { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    /**
     * Apply multiple filter changes at once (from filter dialog/sheet).
     */
    fun applyFilters(
        watchedFilter: WatchedFilter,
        ratingFilter: Float?,
        yearRange: YearRange,
        seriesStatus: SeriesStatusFilter = SeriesStatusFilter.ALL,
        favoritesOnly: Boolean = false,
    ) {
        updateFilterState { state ->
            state.copy(
                watchedFilter = watchedFilter,
                ratingFilter = ratingFilter,
                yearRange = yearRange,
                seriesStatus = seriesStatus,
                favoritesOnly = favoritesOnly,
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
     * Jump to items starting with a specific letter.
     * Uses the Jellyfin API's nameStartsWith filter for server-side filtering.
     */
    fun jumpToLetter(letter: Char) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), currentLetter = letter) }
            loadLibraryItems(resetList = true)
        }
    }

    /**
     * Clear the letter filter and show all items.
     */
    fun clearLetterFilter() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), currentLetter = null) }
            loadLibraryItems(resetList = true)
        }
    }

    /**
     * Mark an item as watched/unwatched.
     * Updates both the server and local state for immediate UI feedback.
     */
    fun setPlayed(itemId: String, played: Boolean) {
        viewModelScope.launch {
            jellyfinClient.setPlayed(itemId, played)
            // Update local state for immediate feedback
            _uiState.update { state ->
                state.copy(
                    items = state.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(userData = item.userData?.copy(played = played))
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }

    /**
     * Toggle an item's favorite status.
     * Updates both the server and local state for immediate UI feedback.
     */
    fun setFavorite(itemId: String, favorite: Boolean) {
        viewModelScope.launch {
            jellyfinClient.setFavorite(itemId, favorite)
            // Update local state for immediate feedback
            _uiState.update { state ->
                state.copy(
                    items = state.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(userData = item.userData?.copy(isFavorite = favorite))
                        } else {
                            item
                        }
                    }
                )
            }
        }
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

        // Clear letter filter when filters change (user is exploring differently)
        _uiState.update { it.copy(filterState = newState, currentLetter = null) }

        // Save to preferences
        preferences.setLibraryFilterState(libraryId, newState)

        // Reload items if filters changed
        if (filtersChanged) {
            loadLibraryItems(resetList = true)
        }
    }
}
