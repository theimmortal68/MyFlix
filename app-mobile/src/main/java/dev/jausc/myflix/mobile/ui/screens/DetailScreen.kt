@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.ui.DetailActions
import dev.jausc.myflix.core.common.ui.PlayAllData
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.PlayQueueManager
import dev.jausc.myflix.core.player.QueueItem
import dev.jausc.myflix.core.player.QueueSource
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MediaInfoBottomSheet
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import dev.jausc.myflix.mobile.ui.components.buildDetailMenuItems
import dev.jausc.myflix.mobile.ui.components.detail.MobileCastCrewRow
import dev.jausc.myflix.mobile.ui.components.detail.MobileDetailHeroSection
import dev.jausc.myflix.mobile.ui.components.detail.MobileDetailsSection
import dev.jausc.myflix.mobile.ui.components.detail.MobileEpisodeRow
import dev.jausc.myflix.mobile.ui.components.detail.MobileRelatedRow
import dev.jausc.myflix.mobile.ui.components.detail.MobileSeasonBar
import kotlinx.coroutines.launch

/**
 * Plex/VoidTV-style detail screen with single scrolling page layout.
 * Shows hero section, season bar (TV shows), episodes, cast, related items, and details.
 */
@Composable
fun DetailScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToGenre: (String, String) -> Unit = { _, _ -> },
) {
    // ViewModel with manual DI
    val viewModel: DetailViewModel = viewModel(
        key = itemId,
        factory = DetailViewModel.Factory(itemId, jellyfinClient),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Long-press menu state
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Menu actions for long-press on episodes
    val menuActions = remember(viewModel, scope) {
        DetailActions(
            onPlay = { episodeId -> onEpisodeClick(episodeId) },
            onMarkWatched = { episodeId, watched ->
                viewModel.setPlayed(episodeId, watched)
            },
            onToggleFavorite = { episodeId, favorite ->
                viewModel.setFavorite(episodeId, favorite)
            },
            onShowMediaInfo = { episode -> mediaInfoItem = episode },
            onGoToSeries = null,
            onPlayAllFromEpisode = { data: PlayAllData ->
                scope.launch {
                    jellyfinClient.getEpisodes(data.seriesId, data.seasonId)
                        .onSuccess { episodes ->
                            val sortedEpisodes = episodes.sortedBy { it.indexNumber ?: 0 }
                            val startIndex = sortedEpisodes.indexOfFirst { it.id == data.itemId }
                                .coerceAtLeast(0)
                            val episodesToPlay = sortedEpisodes.drop(startIndex)

                            if (episodesToPlay.isNotEmpty()) {
                                val queueItems = episodesToPlay.map { episode ->
                                    QueueItem(
                                        itemId = episode.id,
                                        title = episode.name,
                                        episodeInfo = buildString {
                                            episode.parentIndexNumber?.let { append("S$it ") }
                                            episode.indexNumber?.let { append("E$it") }
                                        }.takeIf { it.isNotBlank() },
                                        thumbnailItemId = episode.seriesId,
                                    )
                                }

                                PlayQueueManager.setQueue(
                                    items = queueItems,
                                    source = QueueSource.EPISODE_PLAY_ALL,
                                    startIndex = 0,
                                )
                                onEpisodeClick(episodesToPlay.first().id)
                            }
                        }
                }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        val currentItem = state.item
        if (state.isLoading || currentItem == null) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Single scrolling page
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Hero Section
                item(key = "hero") {
                    MobileDetailHeroSection(
                        item = currentItem,
                        jellyfinClient = jellyfinClient,
                        onPlayClick = onPlayClick,
                        onBackClick = onBack,
                        onFavoriteClick = { viewModel.toggleItemFavorite() },
                        onMarkWatchedClick = { viewModel.setItemPlayed(true) },
                        onMarkUnwatchedClick = { viewModel.setItemPlayed(false) },
                        onGenreSelected = { genre ->
                            // Navigate to library filtered by genre
                            val libraryType = if (currentItem.type == "Movie") "Movies" else "Shows"
                            onNavigateToGenre(genre, libraryType)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Season Bar (TV shows only)
                if (state.isSeries && state.hasSeasons) {
                    item(key = "seasonBar") {
                        Spacer(modifier = Modifier.height(16.dp))
                        MobileSeasonBar(
                            seasons = state.seasons,
                            selectedSeason = state.selectedSeason,
                            onSeasonSelected = { viewModel.selectSeason(it) },
                        )
                    }

                    // Episodes Row
                    item(key = "episodes") {
                        Spacer(modifier = Modifier.height(16.dp))
                        MobileEpisodeRow(
                            episodes = state.episodes,
                            jellyfinClient = jellyfinClient,
                            onEpisodeClick = onEpisodeClick,
                            onEpisodeLongClick = { episode ->
                                popupMenuParams = BottomSheetParams(
                                    title = episode.name,
                                    subtitle = "Episode ${episode.indexNumber ?: ""}",
                                    items = buildDetailMenuItems(episode, menuActions),
                                )
                            },
                            isLoading = state.isLoadingEpisodes,
                        )
                    }
                }

                // Cast & Crew Row
                val castAndCrew = currentItem.people?.filter {
                    it.type in listOf("Actor", "Director", "Writer", "Producer")
                } ?: emptyList()
                if (castAndCrew.isNotEmpty()) {
                    item(key = "castCrew") {
                        Spacer(modifier = Modifier.height(24.dp))
                        MobileCastCrewRow(
                            people = castAndCrew,
                            jellyfinClient = jellyfinClient,
                            onPersonClick = { _ ->
                                // TODO: Navigate to person detail or search
                            },
                        )
                    }
                }

                // Related/Similar Items Row
                item(key = "related") {
                    Spacer(modifier = Modifier.height(24.dp))
                    MobileRelatedRow(
                        items = state.similarItems,
                        jellyfinClient = jellyfinClient,
                        onItemClick = onNavigateToDetail,
                        onItemLongClick = { item ->
                            popupMenuParams = BottomSheetParams(
                                title = item.name,
                                subtitle = item.productionYear?.toString() ?: "",
                                items = buildDetailMenuItems(item, menuActions),
                            )
                        },
                        isLoading = state.isLoadingSimilar,
                    )
                }

                // Details Section
                item(key = "details") {
                    Spacer(modifier = Modifier.height(24.dp))
                    MobileDetailsSection(item = currentItem)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        // Popup menu for long-press
        popupMenuParams?.let { params ->
            PopupMenu(
                params = params,
                onDismiss = { popupMenuParams = null },
            )
        }

        // Media Information bottom sheet
        mediaInfoItem?.let { item ->
            MediaInfoBottomSheet(
                item = item,
                onDismiss = { mediaInfoItem = null },
            )
        }
    }
}
