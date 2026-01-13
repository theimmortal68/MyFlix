@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import dev.jausc.myflix.mobile.ui.components.detail.MobileDetailsTabContent
import dev.jausc.myflix.mobile.ui.components.detail.MobileDetailHeroSection
import dev.jausc.myflix.mobile.ui.components.detail.MobileEpisodesTabContent
import dev.jausc.myflix.mobile.ui.components.detail.MobileOverviewTabContent
import dev.jausc.myflix.mobile.ui.components.detail.MobileRelatedTabContent
import kotlinx.coroutines.launch

/**
 * Netflix-style detail screen with fixed hero section, swipeable tab bar, and tab content.
 * Layout:
 * - Hero section with backdrop, title, metadata, and buttons
 * - Tab bar (swipeable)
 * - Tab content (pager)
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

    // Pager state for swipeable tabs
    val pagerState = rememberPagerState(
        initialPage = state.selectedTabIndex,
        pageCount = { state.availableTabs.size },
    )

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.selectedTabIndex) {
            viewModel.selectTab(pagerState.currentPage)
        }
    }

    // Sync ViewModel state with pager
    LaunchedEffect(state.selectedTabIndex) {
        if (state.selectedTabIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(state.selectedTabIndex)
        }
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
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Fixed Hero Section
                MobileDetailHeroSection(
                    item = currentItem,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = onPlayClick,
                    onBackClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Tab Row
                if (state.availableTabs.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = state.selectedTabIndex,
                        edgePadding = 16.dp,
                    ) {
                        state.availableTabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == state.selectedTabIndex,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = { Text(tab.title) },
                            )
                        }
                    }

                    // Tab Content (Pager)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) { page ->
                        val tab = state.availableTabs.getOrNull(page) ?: return@HorizontalPager

                        when (tab) {
                            DetailTab.OVERVIEW -> {
                                MobileOverviewTabContent(
                                    item = currentItem,
                                    jellyfinClient = jellyfinClient,
                                )
                            }
                            DetailTab.EPISODES -> {
                                MobileEpisodesTabContent(
                                    state = state,
                                    jellyfinClient = jellyfinClient,
                                    onSeasonSelected = { viewModel.selectSeason(it) },
                                    onEpisodeClick = onEpisodeClick,
                                    onEpisodeLongClick = { episode ->
                                        popupMenuParams = BottomSheetParams(
                                            title = episode.name,
                                            subtitle = "Episode ${episode.indexNumber ?: ""}",
                                            items = buildDetailMenuItems(episode, menuActions),
                                        )
                                    },
                                )
                            }
                            DetailTab.RELATED -> {
                                MobileRelatedTabContent(
                                    similarItems = state.similarItems,
                                    isLoading = state.isLoadingSimilar,
                                    jellyfinClient = jellyfinClient,
                                    onItemClick = onNavigateToDetail,
                                )
                            }
                            DetailTab.DETAILS -> {
                                MobileDetailsTabContent(
                                    item = currentItem,
                                )
                            }
                        }
                    }
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
