@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
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
import dev.jausc.myflix.tv.ui.components.detail.DetailHeroSection
import dev.jausc.myflix.tv.ui.components.detail.DetailTabRow
import dev.jausc.myflix.tv.ui.components.detail.DetailsTabContent
import dev.jausc.myflix.tv.ui.components.detail.EpisodesTabContent
import dev.jausc.myflix.tv.ui.components.detail.OverviewTabContent
import dev.jausc.myflix.tv.ui.components.detail.RelatedTabContent
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

/**
 * Netflix-style detail screen with fixed hero section, tab bar, and scrollable tab content.
 * Layout:
 * - Hero section (40% - fixed)
 * - Tab bar (fixed)
 * - Tab content (60% - scrollable)
 */
@Composable
fun DetailScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
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
    val playButtonFocusRequester = remember { FocusRequester() }
    val tabRowFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    // Long-press dialog state (TV-specific)
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Dialog actions (TV-specific)
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

    // Request initial focus on play button when content loads
    LaunchedEffect(state.item) {
        if (state.item != null) {
            try {
                playButtonFocusRequester.requestFocus()
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

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Fixed Hero Section (~40%)
                DetailHeroSection(
                    item = currentItem,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = onPlayClick,
                    onBackClick = onBack,
                    playButtonFocusRequester = playButtonFocusRequester,
                    tabRowFocusRequester = tabRowFocusRequester,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Fixed Tab Bar
                DetailTabRow(
                    tabs = state.availableTabs,
                    selectedIndex = state.selectedTabIndex,
                    onTabSelected = { viewModel.selectTab(it) },
                    tabRowFocusRequester = tabRowFocusRequester,
                    heroFocusRequester = playButtonFocusRequester,
                    contentFocusRequester = contentFocusRequester,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Tab Content (~60% - scrollable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when (state.selectedTab) {
                        DetailTab.OVERVIEW -> {
                            OverviewTabContent(
                                item = currentItem,
                                jellyfinClient = jellyfinClient,
                                contentFocusRequester = contentFocusRequester,
                            )
                        }
                        DetailTab.EPISODES -> {
                            EpisodesTabContent(
                                state = state,
                                jellyfinClient = jellyfinClient,
                                onSeasonSelected = { viewModel.selectSeason(it) },
                                onEpisodeClick = onEpisodeClick,
                                onEpisodeLongClick = { episode ->
                                    dialogParams = DialogParams(
                                        title = episode.name,
                                        items = buildDetailDialogItems(episode, dialogActions),
                                        fromLongClick = true,
                                    )
                                },
                                contentFocusRequester = contentFocusRequester,
                            )
                        }
                        DetailTab.RELATED -> {
                            RelatedTabContent(
                                similarItems = state.similarItems,
                                isLoading = state.isLoadingSimilar,
                                jellyfinClient = jellyfinClient,
                                onItemClick = onNavigateToDetail,
                                contentFocusRequester = contentFocusRequester,
                            )
                        }
                        DetailTab.DETAILS -> {
                            DetailsTabContent(
                                item = currentItem,
                                contentFocusRequester = contentFocusRequester,
                            )
                        }
                    }
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
