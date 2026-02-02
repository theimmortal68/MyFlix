package dev.jausc.myflix.tv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinGenre
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the universe collections screen.
 */
data class UniverseCollectionsUiState(
    val items: List<JellyfinItem> = emptyList(),
    val totalRecordCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Filter state (sort, view mode, filters) */
    val filterState: LibraryFilterState = LibraryFilterState.DEFAULT,
    /** Available genres for filter menu */
    val availableGenres: List<JellyfinGenre> = emptyList(),
    /** Available parental ratings for filter menu */
    val availableParentalRatings: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty() && error == null
}

/**
 * ViewModel for the Universe Collections screen.
 * Manages universe collections (BoxSets tagged with "universe-collection") with sorting and filtering.
 */
class UniverseCollectionsViewModel(
    private val jellyfinClient: JellyfinClient,
) : ViewModel() {
    /**
     * Factory for creating UniverseCollectionsViewModel with manual dependency injection.
     */
    class Factory(
        private val jellyfinClient: JellyfinClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UniverseCollectionsViewModel(jellyfinClient) as T
    }

    private val _uiState = MutableStateFlow(UniverseCollectionsUiState())
    val uiState: StateFlow<UniverseCollectionsUiState> = _uiState.asStateFlow()

    init {
        loadCollections()
        loadGenres()
    }

    /**
     * Load available genres for collections.
     */
    private fun loadGenres() {
        viewModelScope.launch {
            jellyfinClient.getCollectionGenres()
                .onSuccess { genres ->
                    _uiState.update { state ->
                        state.copy(availableGenres = genres)
                    }
                }
        }
    }

    /**
     * Load universe collections with current filter/sort state.
     */
    private fun loadCollections() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filterState = _uiState.value.filterState

            // Build filter parameters
            val isPlayed = when (filterState.watchedFilter) {
                WatchedFilter.ALL -> null
                WatchedFilter.WATCHED -> true
                WatchedFilter.UNWATCHED -> false
            }

            jellyfinClient.getUniverseCollectionsFiltered(
                limit = 200,
                sortBy = filterState.sortBy.jellyfinValue,
                sortOrder = filterState.sortOrder.jellyfinValue,
                genres = filterState.selectedGenres.toList().takeIf { it.isNotEmpty() },
                isPlayed = isPlayed,
                isFavorite = if (filterState.favoritesOnly) true else null,
                minCommunityRating = filterState.ratingFilter,
                years = filterState.yearRange.toJellyfinParam(),
                officialRatings = filterState.selectedParentalRatings.toList().takeIf { it.isNotEmpty() },
            )
                .onSuccess { items ->
                    // Extract parental ratings from results for filter menu
                    val parentalRatings = items
                        .mapNotNull { it.officialRating }
                        .distinct()
                        .sorted()

                    _uiState.update { state ->
                        state.copy(
                            items = items,
                            totalRecordCount = items.size,
                            isLoading = false,
                            availableParentalRatings = parentalRatings,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            error = e.message ?: "Failed to load universe collections",
                            isLoading = false,
                        )
                    }
                }
        }
    }

    /**
     * Refresh collections.
     */
    fun refresh() {
        loadCollections()
    }

    // ========== Filter/Sort Methods ==========

    /**
     * Set the view mode (poster or thumbnail).
     */
    fun setViewMode(mode: LibraryViewMode) {
        _uiState.update { state ->
            state.copy(filterState = state.filterState.copy(viewMode = mode))
        }
    }

    /**
     * Update sort options.
     */
    fun updateSort(sortBy: LibrarySortOption, sortOrder: SortOrder) {
        _uiState.update { state ->
            state.copy(
                filterState = state.filterState.copy(sortBy = sortBy, sortOrder = sortOrder),
                isLoading = true,
            )
        }
        loadCollections()
    }

    /**
     * Apply filters.
     */
    fun applyFilters(
        watchedFilter: WatchedFilter,
        ratingFilter: Float?,
        yearRange: YearRange,
        favoritesOnly: Boolean,
    ) {
        _uiState.update { state ->
            state.copy(
                filterState = state.filterState.copy(
                    watchedFilter = watchedFilter,
                    ratingFilter = ratingFilter,
                    yearRange = yearRange,
                    favoritesOnly = favoritesOnly,
                ),
                isLoading = true,
            )
        }
        loadCollections()
    }

    /**
     * Toggle a genre in the filter.
     */
    fun toggleGenre(genre: String) {
        _uiState.update { state ->
            val newGenres = if (state.filterState.selectedGenres.contains(genre)) {
                state.filterState.selectedGenres - genre
            } else {
                state.filterState.selectedGenres + genre
            }
            state.copy(
                filterState = state.filterState.copy(selectedGenres = newGenres),
                isLoading = true,
            )
        }
        loadCollections()
    }

    /**
     * Clear all selected genres.
     */
    fun clearGenres() {
        _uiState.update { state ->
            state.copy(
                filterState = state.filterState.copy(selectedGenres = emptySet()),
                isLoading = true,
            )
        }
        loadCollections()
    }

    /**
     * Toggle a parental rating in the filter.
     */
    fun toggleParentalRating(rating: String) {
        _uiState.update { state ->
            val newRatings = if (state.filterState.selectedParentalRatings.contains(rating)) {
                state.filterState.selectedParentalRatings - rating
            } else {
                state.filterState.selectedParentalRatings + rating
            }
            state.copy(
                filterState = state.filterState.copy(selectedParentalRatings = newRatings),
                isLoading = true,
            )
        }
        loadCollections()
    }

    /**
     * Clear all selected parental ratings.
     */
    fun clearParentalRatings() {
        _uiState.update { state ->
            state.copy(
                filterState = state.filterState.copy(selectedParentalRatings = emptySet()),
                isLoading = true,
            )
        }
        loadCollections()
    }

    /**
     * Mark a collection as played/unplayed.
     */
    fun setPlayed(itemId: String, played: Boolean) {
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    items = state.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(userData = item.userData?.copy(played = played))
                        } else {
                            item
                        }
                    },
                )
            }

            // API call
            jellyfinClient.setPlayed(itemId, played).onFailure {
                // Revert on failure
                _uiState.update { state ->
                    state.copy(
                        items = state.items.map { item ->
                            if (item.id == itemId) {
                                item.copy(userData = item.userData?.copy(played = !played))
                            } else {
                                item
                            }
                        },
                    )
                }
            }
        }
    }

    /**
     * Toggle favorite status for a collection.
     */
    fun setFavorite(itemId: String, favorite: Boolean) {
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    items = state.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(userData = item.userData?.copy(isFavorite = favorite))
                        } else {
                            item
                        }
                    },
                )
            }

            // API call
            jellyfinClient.setFavorite(itemId, favorite).onFailure {
                // Revert on failure
                _uiState.update { state ->
                    state.copy(
                        items = state.items.map { item ->
                            if (item.id == itemId) {
                                item.copy(userData = item.userData?.copy(isFavorite = !favorite))
                            } else {
                                item
                            }
                        },
                    )
                }
            }
        }
    }

    /**
     * Get a random collection ID for shuffle.
     */
    fun getShuffleItemId(): String? {
        val items = _uiState.value.items
        return if (items.isNotEmpty()) items.random().id else null
    }
}
