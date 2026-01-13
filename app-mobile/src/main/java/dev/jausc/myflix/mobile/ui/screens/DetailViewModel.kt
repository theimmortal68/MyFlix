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
 * Detail screen tabs.
 */
enum class DetailTab(val title: String) {
    OVERVIEW("Overview"),
    EPISODES("Episodes"),
    RELATED("More Like This"),
    DETAILS("Details"),
}

/**
 * UI state for the detail screen.
 */
data class DetailUiState(
    val item: JellyfinItem? = null,
    val seasons: List<JellyfinItem> = emptyList(),
    val episodes: List<JellyfinItem> = emptyList(),
    val selectedSeason: JellyfinItem? = null,
    val isLoading: Boolean = true,
    val isLoadingEpisodes: Boolean = false,
    val error: String? = null,
    val selectedTabIndex: Int = 0,
    val similarItems: List<JellyfinItem> = emptyList(),
    val isLoadingSimilar: Boolean = false,
) {
    val hasSeasons: Boolean get() = seasons.isNotEmpty()
    val hasEpisodes: Boolean get() = episodes.isNotEmpty()
    val isSeries: Boolean get() = item?.type == "Series"

    /**
     * Get available tabs based on item type.
     * Movies: Overview, Related, Details
     * Series: Overview, Episodes, Related, Details
     */
    val availableTabs: List<DetailTab>
        get() = buildList {
            add(DetailTab.OVERVIEW)
            if (isSeries) add(DetailTab.EPISODES)
            add(DetailTab.RELATED)
            add(DetailTab.DETAILS)
        }

    val selectedTab: DetailTab
        get() = availableTabs.getOrNull(selectedTabIndex) ?: DetailTab.OVERVIEW
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
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
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
     * Load seasons for a series.
     */
    private fun loadSeasons(seriesId: String) {
        viewModelScope.launch {
            jellyfinClient.getSeasons(seriesId)
                .onSuccess { seasons ->
                    _uiState.update { it.copy(seasons = seasons) }

                    // Auto-select first season
                    if (seasons.isNotEmpty()) {
                        selectSeason(seasons.first())
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

        _uiState.update {
            it.copy(
                selectedSeason = season,
                isLoadingEpisodes = true,
            )
        }

        viewModelScope.launch {
            jellyfinClient.getEpisodes(currentItem.id, season.id)
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

        viewModelScope.launch {
            jellyfinClient.getEpisodes(currentItem.id, selectedSeason.id)
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
     * Reload the item details.
     */
    fun refresh() {
        loadItem()
    }

    /**
     * Select a tab by index.
     */
    fun selectTab(index: Int) {
        val tabs = _uiState.value.availableTabs
        if (index in tabs.indices) {
            _uiState.update { it.copy(selectedTabIndex = index) }

            // Load similar items when Related tab is selected (lazy load)
            if (tabs[index] == DetailTab.RELATED && _uiState.value.similarItems.isEmpty()) {
                loadSimilarItems()
            }
        }
    }

    /**
     * Load similar items for the Related tab.
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
}
