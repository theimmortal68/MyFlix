@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jausc.myflix.core.network.JellyfinClient

/**
 * Detail screen router that shows MovieDetailScreen or SeriesDetailScreen
 * based on the item type. Wholphin-style text-based detail screens.
 */
@Composable
fun DetailScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    onPlayClick: (String, Long?) -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToGenre: (String, String) -> Unit = { _, _ -> },
    onNavigateToPerson: (String) -> Unit = {},
) {
    // ViewModel with manual DI
    val viewModel: DetailViewModel = viewModel(
        key = itemId,
        factory = DetailViewModel.Factory(itemId, jellyfinClient),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading || state.item == null -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.isMovie -> {
                // Movie detail screen
                MovieDetailScreen(
                    state = state,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = { startPositionMs ->
                        onPlayClick(itemId, startPositionMs)
                    },
                    onPlayItemClick = onPlayItemClick,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToPerson = onNavigateToPerson,
                    onWatchedClick = {
                        val played = state.item?.userData?.played == true
                        viewModel.setItemPlayed(!played)
                    },
                    onFavoriteClick = { viewModel.toggleItemFavorite() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            state.isSeries -> {
                // Series detail screen
                SeriesDetailScreen(
                    state = state,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = {
                        // Play next episode
                        val nextUpEpisode = state.nextUpEpisode ?: state.episodes.firstOrNull()
                        if (nextUpEpisode != null) {
                            onEpisodeClick(nextUpEpisode.id)
                        }
                    },
                    onShuffleClick = {
                        // Shuffle play - pick random episode
                        val episodes = state.episodes
                        if (episodes.isNotEmpty()) {
                            val randomEpisode = episodes.random()
                            onEpisodeClick(randomEpisode.id)
                        }
                    },
                    onSeasonClick = { season ->
                        onNavigateToDetail(season.id)
                    },
                    onPlayItemClick = onPlayItemClick,
                    onTrailerClick = onTrailerClick,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToPerson = onNavigateToPerson,
                    onWatchedClick = {
                        val played = state.item?.userData?.played == true
                        viewModel.setItemPlayed(!played)
                    },
                    onFavoriteClick = { viewModel.toggleItemFavorite() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            state.isSeason -> {
                SeasonDetailScreen(
                    state = state,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = {
                        val firstEpisode = state.episodes.firstOrNull()
                        if (firstEpisode != null) {
                            onEpisodeClick(firstEpisode.id)
                        }
                    },
                    onShuffleClick = {
                        val episodes = state.episodes
                        if (episodes.isNotEmpty()) {
                            val randomEpisode = episodes.random()
                            onEpisodeClick(randomEpisode.id)
                        }
                    },
                    onSeasonClick = { season ->
                        onNavigateToDetail(season.id)
                    },
                    onPlayItemClick = onPlayItemClick,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToPerson = onNavigateToPerson,
                    onWatchedClick = {
                        val played = state.item?.userData?.played == true
                        viewModel.setItemPlayed(!played)
                    },
                    onFavoriteClick = { viewModel.toggleItemFavorite() },
                    onEpisodeWatchedToggle = { episodeId, played ->
                        viewModel.setPlayed(episodeId, played)
                    },
                    onEpisodeFavoriteToggle = { episodeId, favorite ->
                        viewModel.setFavorite(episodeId, favorite)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            state.isEpisode -> {
                EpisodeDetailScreen(
                    state = state,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = { startPositionMs ->
                        onPlayClick(itemId, startPositionMs)
                    },
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToPerson = onNavigateToPerson,
                    onWatchedClick = {
                        val played = state.item?.userData?.played == true
                        viewModel.setItemPlayed(!played)
                    },
                    onFavoriteClick = { viewModel.toggleItemFavorite() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                // Unknown item type - show generic loading or error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Unsupported content type")
                }
            }
        }
    }
}
