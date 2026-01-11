package dev.jausc.myflix.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isSeries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for DetailScreen.
 * Manages item details, seasons, episodes, and loading state across TV and mobile platforms.
 */
@Stable
class DetailScreenState(
    val itemId: String,
    private val loader: DetailLoader,
    private val scope: CoroutineScope,
) {
    var item by mutableStateOf<JellyfinItem?>(null)
        private set

    var seasons by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var selectedSeason by mutableStateOf<JellyfinItem?>(null)
        private set

    var episodes by mutableStateOf<List<JellyfinItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /**
     * Whether this item is a series with seasons.
     */
    val isSeries: Boolean
        get() = item?.isSeries == true

    /**
     * Whether there are seasons to display.
     */
    val hasSeasons: Boolean
        get() = isSeries && seasons.isNotEmpty()

    /**
     * Whether there are episodes to display.
     */
    val hasEpisodes: Boolean
        get() = episodes.isNotEmpty()

    /**
     * Load the initial item and its seasons/episodes if applicable.
     */
    internal fun loadItem() {
        scope.launch {
            isLoading = true
            error = null

            loader.loadItem(itemId)
                .onSuccess { loadedItem ->
                    item = loadedItem

                    if (loadedItem.isSeries) {
                        loader.loadSeasons(itemId)
                            .onSuccess { seasonList ->
                                seasons = seasonList
                                selectedSeason = seasonList.firstOrNull()
                                selectedSeason?.let { season ->
                                    loader.loadEpisodes(itemId, season.id)
                                        .onSuccess { eps -> episodes = eps }
                                        .onFailure { error = it.message }
                                }
                            }
                            .onFailure { error = it.message }
                    }
                }
                .onFailure { error = it.message }

            isLoading = false
        }
    }

    /**
     * Select a season and load its episodes.
     */
    fun selectSeason(season: JellyfinItem) {
        if (selectedSeason?.id == season.id) return

        selectedSeason = season
        loadEpisodesForSelectedSeason()
    }

    /**
     * Reload episodes for the currently selected season.
     * Useful after marking episodes as watched/unwatched.
     */
    fun refreshEpisodes() {
        loadEpisodesForSelectedSeason()
    }

    private fun loadEpisodesForSelectedSeason() {
        val currentItem = item ?: return
        val season = selectedSeason ?: return

        if (!currentItem.isSeries) return

        scope.launch {
            loader.loadEpisodes(currentItem.id, season.id)
                .onSuccess { eps -> episodes = eps }
                .onFailure { error = it.message }
        }
    }
}

/**
 * Detail loader interface for dependency injection.
 * Abstracts the API client to allow for testing and flexibility.
 */
interface DetailLoader {
    suspend fun loadItem(itemId: String): Result<JellyfinItem>
    suspend fun loadSeasons(seriesId: String): Result<List<JellyfinItem>>
    suspend fun loadEpisodes(seriesId: String, seasonId: String): Result<List<JellyfinItem>>
}

/**
 * Creates and remembers a [DetailScreenState].
 *
 * @param itemId The ID of the item to display
 * @param loader Loader to fetch item details, seasons, and episodes
 * @return A [DetailScreenState] for managing detail screen UI state
 */
@Composable
fun rememberDetailScreenState(
    itemId: String,
    loader: DetailLoader,
): DetailScreenState {
    val scope = rememberCoroutineScope()
    val state = remember(itemId) {
        DetailScreenState(itemId, loader, scope)
    }

    LaunchedEffect(itemId) {
        state.loadItem()
    }

    return state
}
