@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.ThemeMusicPlayer
import dev.jausc.myflix.core.viewmodel.DetailViewModel
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Detail screen router that shows MovieDetailScreen or UnifiedSeriesScreen
 * based on the item type. Episodes and Seasons redirect to EpisodesScreen.
 */
@Suppress("UnusedParameter")
@Composable
fun DetailScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    themeMusicPlayer: ThemeMusicPlayer?,
    onPlayClick: (String, Long?) -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToGenre: (String, String) -> Unit = { _, _ -> },
    onNavigateToPerson: (String) -> Unit = {},
    onNavigateToEpisodes: (seriesId: String, seasonNumber: Int, episodeId: String?) -> Unit = { _, _, _ -> },
    leftEdgeFocusRequester: FocusRequester? = null,
) {
    // ViewModel with manual DI
    val viewModel: DetailViewModel = viewModel(
        key = itemId,
        factory = DetailViewModel.Factory(itemId, jellyfinClient),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Play theme music when available
    LaunchedEffect(state.themeSongUrl) {
        val themeUrl = state.themeSongUrl
        if (themeUrl != null && themeMusicPlayer != null) {
            themeMusicPlayer.play(themeUrl)
        }
    }

    // Stop theme music when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            themeMusicPlayer?.stop()
        }
    }

    // Focus requester for loading state - captures focus to prevent NavRail auto-expand
    val loadingFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        when {
            state.isLoading || state.item == null -> {
                // Loading state - focusable to capture focus during load
                LaunchedEffect(Unit) {
                    loadingFocusRequester.requestFocus()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(loadingFocusRequester)
                        .focusable(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading...",
                        color = TvColors.TextPrimary,
                    )
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
                    onTrailerClick = onTrailerClick,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToPerson = onNavigateToPerson,
                    onWatchedClick = {
                        val played = state.item?.userData?.played == true
                        viewModel.setItemPlayed(!played)
                    },
                    onFavoriteClick = { viewModel.toggleItemFavorite() },
                    modifier = Modifier.fillMaxSize(),
                    leftEdgeFocusRequester = leftEdgeFocusRequester,
                )
            }

            state.isSeries -> {
                // Series detail screen
                UnifiedSeriesScreen(
                    state = state,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = {
                        // Play next up episode or first episode
                        val nextUp = state.nextUpEpisode
                        val firstEpisode = state.episodes.firstOrNull()
                        val episodeToPlay = nextUp ?: firstEpisode
                        if (episodeToPlay != null) {
                            val startPosition = episodeToPlay.userData?.playbackPositionTicks
                                ?.let { it / 10_000 } ?: 0L
                            onPlayClick(episodeToPlay.id, startPosition)
                        }
                    },
                    onShuffleClick = {
                        // Play random episode from all episodes
                        val episodes = state.episodes
                        if (episodes.isNotEmpty()) {
                            val randomEpisode = episodes.random()
                            onPlayClick(randomEpisode.id, 0L)
                        }
                    },
                    onWatchedClick = {
                        val played = state.item?.userData?.played == true
                        viewModel.setItemPlayed(!played)
                    },
                    onFavoriteClick = { viewModel.toggleItemFavorite() },
                    onSeasonClick = { season ->
                        // Navigate to episodes screen with the selected season number
                        val seasonNumber = season.indexNumber ?: 1
                        onNavigateToEpisodes(state.item?.id ?: itemId, seasonNumber, null)
                    },
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToPerson = onNavigateToPerson,
                    modifier = Modifier.fillMaxSize(),
                    leftEdgeFocusRequester = leftEdgeFocusRequester,
                )
            }

            state.isSeason -> {
                // Redirect seasons to EpisodesScreen
                val season = state.item
                val seriesId = season?.seriesId
                val seasonNumber = season?.indexNumber ?: 1

                LaunchedEffect(seriesId) {
                    if (seriesId != null) {
                        onNavigateToEpisodes(seriesId, seasonNumber, null)
                    }
                }

                // Show loading while redirecting - focusable to capture focus
                LaunchedEffect(Unit) {
                    loadingFocusRequester.requestFocus()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(loadingFocusRequester)
                        .focusable(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading...",
                        color = TvColors.TextPrimary,
                    )
                }
            }

            state.isEpisode -> {
                // Redirect episodes to EpisodesScreen instead of showing inline
                val episode = state.item
                val seriesId = episode?.seriesId
                val seasonNumber = episode?.parentIndexNumber ?: 1

                LaunchedEffect(seriesId) {
                    if (seriesId != null) {
                        onNavigateToEpisodes(seriesId, seasonNumber, episode?.id)
                    }
                }

                // Show loading while redirecting - focusable to capture focus
                LaunchedEffect(Unit) {
                    loadingFocusRequester.requestFocus()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(loadingFocusRequester)
                        .focusable(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading...",
                        color = TvColors.TextPrimary,
                    )
                }
            }

            else -> {
                // Unknown item type - show generic error, focusable to capture focus
                LaunchedEffect(Unit) {
                    loadingFocusRequester.requestFocus()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(loadingFocusRequester)
                        .focusable(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Unsupported content type",
                        color = TvColors.TextPrimary,
                    )
                }
            }
        }
    }
}
