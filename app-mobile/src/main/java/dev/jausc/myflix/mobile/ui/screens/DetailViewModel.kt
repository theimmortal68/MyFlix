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
 * UI state for the detail screen.
 */
data class DetailUiState(
    val item: JellyfinItem? = null,
    val seasons: List<JellyfinItem> = emptyList(),
    val episodes: List<JellyfinItem> = emptyList(),
    val selectedSeason: JellyfinItem? = null,
    val nextUpEpisode: JellyfinItem? = null,
    val isLoading: Boolean = true,
    val isLoadingEpisodes: Boolean = false,
    val isLoadingNextUp: Boolean = false,
    val error: String? = null,
    val recommendations: List<JellyfinItem> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val similarItems: List<JellyfinItem> = emptyList(),
    val isLoadingSimilar: Boolean = false,
    val specialFeatures: List<JellyfinItem> = emptyList(),
    val isLoadingSpecialFeatures: Boolean = false,
    val collections: List<JellyfinItem> = emptyList(),
    val collectionItems: Map<String, List<JellyfinItem>> = emptyMap(),
    val isLoadingCollections: Boolean = false,
) {
    val hasSeasons: Boolean get() = seasons.isNotEmpty()
    val hasEpisodes: Boolean get() = episodes.isNotEmpty()
    val isSeries: Boolean get() = item?.type == "Series"
    val isMovie: Boolean get() = item?.type == "Movie"
    val isSeason: Boolean get() = item?.type == "Season"
    val isEpisode: Boolean get() = item?.type == "Episode"
}

/**
 * ViewModel for the mobile detail screen.
 * Manages item details, seasons, and episodes loading.
 */
class DetailViewModel(
    private val itemId: String,
    private val jellyfinClient: JellyfinClient,
) : ViewModel() {

    /**
     * Factory for creating DetailViewModel with manual dependency injection.
     */
    class Factory(
        private val itemId: String,
        private val jellyfinClient: JellyfinClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(itemId, jellyfinClient) as T
    }

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadItem()
    }

    /**
     * Load item details and seasons if applicable.
     */
    private fun loadItem() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            jellyfinClient.getItem(itemId)
                .onSuccess { item ->
                    _uiState.update { it.copy(item = item) }

                    // Load seasons if this is a series
                    if (item.type == "Series") {
                        loadSeasons(item.id)
                        loadNextUp(item.id)
                    } else if (item.type == "Season") {
                        loadSeasons(item.seriesId ?: item.id, preferredSeasonId = item.id)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }

                    // Load similar items in parallel
                    loadSimilarItems()
                    loadRecommendations()
                    loadSpecialFeatures()
                    loadCollections()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load item: ${e.message ?: "Unknown error"}",
                        )
                    }
                }
        }
    }

    /**
     * Load the Next Up episode for a series.
     */
    private fun loadNextUp(seriesId: String) {
        _uiState.update { it.copy(isLoadingNextUp = true) }

        viewModelScope.launch {
            jellyfinClient.getNextUpForSeries(seriesId, limit = 1)
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            nextUpEpisode = items.firstOrNull(),
                            isLoadingNextUp = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingNextUp = false) }
                }
        }
    }

    /**
     * Load seasons for a series.
     */
    private fun loadSeasons(seriesId: String, preferredSeasonId: String? = null) {
        viewModelScope.launch {
            jellyfinClient.getSeasons(seriesId)
                .onSuccess { seasons ->
                    _uiState.update { it.copy(seasons = seasons) }

                    // Auto-select first season
                    if (seasons.isNotEmpty()) {
                        val preferredSeason = preferredSeasonId?.let { id ->
                            seasons.firstOrNull { it.id == id }
                        }
                        selectSeason(preferredSeason ?: seasons.first())
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    /**
     * Select a season and load its episodes.
     */
    fun selectSeason(season: JellyfinItem) {
        val currentItem = _uiState.value.item ?: return
        val seriesId = currentItem.seriesId ?: currentItem.id

        _uiState.update {
            it.copy(
                selectedSeason = season,
                isLoadingEpisodes = true,
            )
        }

        viewModelScope.launch {
            jellyfinClient.getEpisodes(seriesId, season.id)
                .onSuccess { episodes ->
                    _uiState.update {
                        it.copy(
                            episodes = episodes,
                            isLoading = false,
                            isLoadingEpisodes = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingEpisodes = false,
                        )
                    }
                }
        }
    }

    /**
     * Refresh episodes for the currently selected season.
     */
    fun refreshEpisodes() {
        val currentItem = _uiState.value.item ?: return
        val selectedSeason = _uiState.value.selectedSeason ?: return
        val seriesId = currentItem.seriesId ?: currentItem.id

        viewModelScope.launch {
            jellyfinClient.getEpisodes(seriesId, selectedSeason.id)
                .onSuccess { episodes ->
                    _uiState.update { it.copy(episodes = episodes) }
                }
        }
    }

    /**
     * Mark an episode as watched/unwatched.
     */
    fun setPlayed(episodeId: String, played: Boolean) {
        viewModelScope.launch {
            jellyfinClient.setPlayed(episodeId, played)
            refreshEpisodes()
        }
    }

    /**
     * Toggle an episode's favorite status.
     */
    fun setFavorite(episodeId: String, favorite: Boolean) {
        viewModelScope.launch {
            jellyfinClient.setFavorite(episodeId, favorite)
            refreshEpisodes()
        }
    }

    /**
     * Toggle the main item's favorite status.
     */
    fun toggleItemFavorite() {
        val currentItem = _uiState.value.item ?: return
        val isFavorite = currentItem.userData?.isFavorite == true

        viewModelScope.launch {
            jellyfinClient.setFavorite(currentItem.id, !isFavorite)
            refreshItem()
        }
    }

    /**
     * Mark the main item as watched/unwatched.
     */
    fun setItemPlayed(played: Boolean) {
        val currentItem = _uiState.value.item ?: return

        viewModelScope.launch {
            jellyfinClient.setPlayed(currentItem.id, played)
            refreshItem()
        }
    }

    /**
     * Refresh the main item's data.
     */
    private fun refreshItem() {
        viewModelScope.launch {
            jellyfinClient.getItem(itemId)
                .onSuccess { item ->
                    _uiState.update { it.copy(item = item) }
                }
        }
    }

    /**
     * Reload the item details.
     */
    fun refresh() {
        loadItem()
    }

    /**
     * Load similar items for the Related section.
     */
    private fun loadSimilarItems() {
        val currentItem = _uiState.value.item ?: return

        _uiState.update { it.copy(isLoadingSimilar = true) }

        viewModelScope.launch {
            jellyfinClient.getSimilarItems(currentItem.id, limit = 18)
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            similarItems = items,
                            isLoadingSimilar = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingSimilar = false) }
                }
        }
    }

    /**
     * Load recommended items (general suggestions).
     */
    private fun loadRecommendations() {
        val currentItem = _uiState.value.item ?: return

        _uiState.update { it.copy(isLoadingRecommendations = true) }

        viewModelScope.launch {
            jellyfinClient.getSuggestions(limit = 24)
                .onSuccess { items ->
                    val filtered = items.filter { it.id != currentItem.id }
                    _uiState.update {
                        it.copy(
                            recommendations = filtered,
                            isLoadingRecommendations = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingRecommendations = false) }
                }
        }
    }

    /**
     * Load special features (extras, trailers) for the item.
     */
    private fun loadSpecialFeatures() {
        val currentItem = _uiState.value.item ?: return

        _uiState.update { it.copy(isLoadingSpecialFeatures = true) }

        viewModelScope.launch {
            jellyfinClient.getSpecialFeatures(currentItem.id, limit = 12)
                .onSuccess { features ->
                    _uiState.update {
                        it.copy(
                            specialFeatures = features,
                            isLoadingSpecialFeatures = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingSpecialFeatures = false) }
                }
        }
    }

    /**
     * Load collections that contain this item and their items.
     */
    private fun loadCollections() {
        val currentItem = _uiState.value.item ?: return

        _uiState.update { it.copy(isLoadingCollections = true) }

        viewModelScope.launch {
            val collections = currentItem.collectionIds
                ?.mapNotNull { id -> jellyfinClient.getItem(id).getOrNull() }
                ?.filter { it.type == "BoxSet" }
                ?.takeIf { it.isNotEmpty() }
                ?: jellyfinClient.getItemAncestors(currentItem.id)
                    .getOrNull()
                    ?.filter { it.type == "BoxSet" }
                    .orEmpty()

            val collectionItems = mutableMapOf<String, List<JellyfinItem>>()
            collections.forEach { collection ->
                jellyfinClient.getCollectionItems(collection.id, limit = 12)
                    .onSuccess { items ->
                        collectionItems[collection.id] = items.filter { it.id != currentItem.id }
                    }
            }

            _uiState.update {
                it.copy(
                    collections = collections,
                    collectionItems = collectionItems,
                    isLoadingCollections = false,
                )
            }
        }
    }
}
