@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.*
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    // ViewModel with manual DI
    val viewModel: DetailViewModel = viewModel(
        key = itemId, // Use itemId as key so ViewModel is recreated for different items
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
            onGoToSeries = null, // Already on series page
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.item?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val currentItem = state.item
        if (state.isLoading || currentItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AsyncImage(
                            model = jellyfinClient.getPrimaryImageUrl(
                                currentItem.id,
                                currentItem.imageTags?.primary,
                                300,
                            ),
                            contentDescription = currentItem.name,
                            modifier = Modifier
                                .width(150.dp)
                                .aspectRatio(2f / 3f)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop,
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = currentItem.name,
                                style = MaterialTheme.typography.headlineSmall,
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                currentItem.productionYear?.let { year ->
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                currentItem.runtimeMinutes?.let { runtime ->
                                    Text(
                                        text = "$runtime min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            currentItem.communityRating?.let { rating ->
                                Text(
                                    text = "★ %.1f".format(rating),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play")
                            }
                        }
                    }
                }

                currentItem.overview?.let { overview ->
                    item {
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (state.hasSeasons) {
                    item {
                        Text(
                            text = "Seasons",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.seasons, key = { it.id }) { season ->
                                val isSelected = state.selectedSeason?.id == season.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectSeason(season) },
                                    label = { Text(season.name) },
                                )
                            }
                        }
                    }

                    if (state.hasEpisodes) {
                        item {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        items(state.episodes, key = { it.id }) { episode ->
                            EpisodeCard(
                                episode = episode,
                                imageUrl = jellyfinClient.getPrimaryImageUrl(episode.id, episode.imageTags?.primary),
                                onClick = { onEpisodeClick(episode.id) },
                                onLongClick = {
                                    popupMenuParams = BottomSheetParams(
                                        title = episode.name,
                                        subtitle = "Episode ${episode.indexNumber ?: ""}",
                                        items = buildDetailMenuItems(episode, menuActions),
                                    )
                                },
                            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeCard(
    episode: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = episode.name,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Episode ${episode.indexNumber ?: ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                episode.runtimeMinutes?.let { runtime ->
                    Text(
                        text = "$runtime min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
