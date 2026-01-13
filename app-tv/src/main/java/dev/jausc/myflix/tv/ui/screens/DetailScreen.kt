@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.common.ui.PlayAllData
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.PlayQueueManager
import dev.jausc.myflix.core.player.QueueItem
import dev.jausc.myflix.core.player.QueueSource
import dev.jausc.myflix.tv.ui.components.DetailDialogActions
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.buildDetailDialogItems
import dev.jausc.myflix.tv.ui.components.detail.CastCrewRow
import dev.jausc.myflix.tv.ui.components.detail.DetailHeroSection
import dev.jausc.myflix.tv.ui.components.detail.DetailsSection
import dev.jausc.myflix.tv.ui.components.detail.EpisodeRow
import dev.jausc.myflix.tv.ui.components.detail.RelatedRow
import dev.jausc.myflix.tv.ui.components.detail.SeasonBar
import dev.jausc.myflix.tv.ui.theme.TvColors
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

    // Focus requesters for D-pad navigation
    val actionsFocusRequester = remember { FocusRequester() }
    val seasonBarFocusRequester = remember { FocusRequester() }

    // Long-press dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Dialog actions for episodes
    val dialogActions = remember(viewModel, scope) {
        DetailDialogActions(
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

    // Request initial focus on actions when content loads
    LaunchedEffect(state.item) {
        if (state.item != null) {
            try {
                actionsFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus requester may not be ready yet
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        if (state.isLoading || state.item == null) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading...",
                    color = TvColors.TextPrimary,
                )
            }
        } else {
            val currentItem = state.item!!

            // Single scrolling page
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Hero Section
                item(key = "hero") {
                    DetailHeroSection(
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
                        actionsFocusRequester = actionsFocusRequester,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Season Bar (TV shows only)
                if (state.isSeries && state.hasSeasons) {
                    item(key = "seasonBar") {
                        Spacer(modifier = Modifier.height(16.dp))
                        SeasonBar(
                            seasons = state.seasons,
                            selectedSeason = state.selectedSeason,
                            onSeasonSelected = { viewModel.selectSeason(it) },
                            focusRequester = seasonBarFocusRequester,
                        )
                    }

                    // Episodes Row
                    item(key = "episodes") {
                        Spacer(modifier = Modifier.height(16.dp))
                        EpisodeRow(
                            episodes = state.episodes,
                            jellyfinClient = jellyfinClient,
                            onEpisodeClick = onEpisodeClick,
                            onEpisodeLongClick = { episode ->
                                dialogParams = DialogParams(
                                    title = episode.name,
                                    items = buildDetailDialogItems(episode, dialogActions),
                                    fromLongClick = true,
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
                        CastCrewRow(
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
                    RelatedRow(
                        items = state.similarItems,
                        jellyfinClient = jellyfinClient,
                        onItemClick = onNavigateToDetail,
                        onItemLongClick = { item ->
                            dialogParams = DialogParams(
                                title = item.name,
                                items = buildDetailDialogItems(item, dialogActions),
                                fromLongClick = true,
                            )
                        },
                        isLoading = state.isLoadingSimilar,
                    )
                }

                // Details Section
                item(key = "details") {
                    Spacer(modifier = Modifier.height(24.dp))
                    DetailsSection(item = currentItem)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    // Long-press context menu dialog
    dialogParams?.let { params ->
        DialogPopup(
            params = params,
            onDismissRequest = { dialogParams = null },
        )
    }

    // Media Information dialog
    mediaInfoItem?.let { episode ->
        MediaInfoDialog(
            item = episode,
            onDismiss = { mediaInfoItem = null },
        )
    }
}
