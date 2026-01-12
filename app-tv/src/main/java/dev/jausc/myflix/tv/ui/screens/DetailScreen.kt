@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.*
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DetailDialogActions
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.buildDetailDialogItems
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

@Suppress("UnusedParameter")
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

    // Long-press dialog state (TV-specific)
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Dialog actions (TV-specific)
    val dialogActions = remember(viewModel) {
        DetailDialogActions(
            onPlay = { episodeId -> onEpisodeClick(episodeId) },
            onMarkWatched = { episodeId, watched ->
                viewModel.setPlayed(episodeId, watched)
            },
            onToggleFavorite = { episodeId, favorite ->
                viewModel.setFavorite(episodeId, favorite)
            },
            onShowMediaInfo = { episode -> mediaInfoItem = episode },
            onGoToSeries = null, // Already on series page
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        if (state.isLoading || state.item == null) {
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

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AsyncImage(
                        model = jellyfinClient.getPrimaryImageUrl(currentItem.id, currentItem.imageTags?.primary, 400),
                        contentDescription = currentItem.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                    )

                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("▶ Play")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(end = 48.dp, top = 48.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Text(
                            text = currentItem.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = TvColors.TextPrimary,
                        )
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            currentItem.productionYear?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TvColors.TextSecondary,
                                )
                            }
                            currentItem.runtimeMinutes?.let { runtime ->
                                Text(
                                    text = "$runtime min",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TvColors.TextSecondary,
                                )
                            }
                            currentItem.communityRating?.let { rating ->
                                Text(
                                    text = "★ %.1f".format(rating),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TvColors.BluePrimary,
                                )
                            }
                        }
                    }

                    currentItem.overview?.let { overview ->
                        item {
                            Text(
                                text = overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary,
                            )
                        }
                    }

                    if (state.hasSeasons) {
                        item {
                            Text(
                                text = "Seasons",
                                style = MaterialTheme.typography.titleMedium,
                                color = TvColors.TextPrimary,
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(state.seasons, key = { it.id }) { season ->
                                    val isSelected = state.selectedSeason?.id == season.id
                                    Surface(
                                        onClick = { viewModel.selectSeason(season) },
                                        shape = ClickableSurfaceDefaults.shape(
                                            shape = MaterialTheme.shapes.small,
                                        ),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = if (isSelected) TvColors.BluePrimary else TvColors.Surface,
                                            focusedContainerColor = TvColors.FocusedSurface,
                                        ),
                                    ) {
                                        Text(
                                            text = season.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TvColors.TextPrimary,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }

                        if (state.hasEpisodes) {
                            item {
                                Text(
                                    text = "Episodes",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TvColors.TextPrimary,
                                )
                            }

                            items(state.episodes, key = { it.id }) { episode ->
                                EpisodeCard(
                                    episode = episode,
                                    imageUrl = jellyfinClient.getPrimaryImageUrl(
                                        episode.id,
                                        episode.imageTags?.primary,
                                    ),
                                    onClick = { onEpisodeClick(episode.id) },
                                    onLongClick = {
                                        dialogParams = DialogParams(
                                            title = episode.name,
                                            items = buildDetailDialogItems(episode, dialogActions),
                                            fromLongClick = true,
                                        )
                                    },
                                )
                            }
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
            jellyfinClient = jellyfinClient,
            onDismiss = { mediaInfoItem = null },
        )
    }
}

@Suppress("UnusedParameter")
@Composable
private fun MediaInfoDialog(item: JellyfinItem, jellyfinClient: JellyfinClient, onDismiss: () -> Unit) {
    // Get media source info
    val mediaSource = item.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams ?: emptyList()
    val container = mediaSource?.container

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 400.dp, max = 600.dp)
                    .padding(32.dp),
                shape = MaterialTheme.shapes.large,
                colors = SurfaceDefaults.colors(
                    containerColor = TvColors.Surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Media Information",
                        style = MaterialTheme.typography.titleLarge,
                        color = TvColors.TextPrimary,
                    )

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TvColors.BluePrimary,
                    )

                    // Video stream info
                    mediaStreams.filter { it.type == "Video" }.firstOrNull()?.let { video ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "VIDEO",
                                style = MaterialTheme.typography.labelMedium,
                                color = TvColors.TextSecondary,
                            )
                            Text(
                                text = buildString {
                                    append(video.codec?.uppercase() ?: "Unknown")
                                    video.width?.let { w -> video.height?.let { h -> append(" • ${w}x$h") } }
                                    video.videoRangeType?.let { append(" • $it") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextPrimary,
                            )
                        }
                    }

                    // Audio streams
                    val audioStreams = mediaStreams.filter { it.type == "Audio" }
                    if (audioStreams.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "AUDIO (${audioStreams.size} tracks)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TvColors.TextSecondary,
                            )
                            audioStreams.take(3).forEach { audio ->
                                Text(
                                    text = buildString {
                                        append(audio.codec?.uppercase() ?: "Unknown")
                                        audio.channels?.let { append(" • ${it}ch") }
                                        audio.language?.let { append(" • $it") }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TvColors.TextPrimary,
                                )
                            }
                            if (audioStreams.size > 3) {
                                Text(
                                    text = "... and ${audioStreams.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TvColors.TextSecondary,
                                )
                            }
                        }
                    }

                    // Subtitle streams
                    val subtitleStreams = mediaStreams.filter { it.type == "Subtitle" }
                    if (subtitleStreams.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SUBTITLES (${subtitleStreams.size} tracks)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TvColors.TextSecondary,
                            )
                            Text(
                                text = subtitleStreams.take(5).mapNotNull { it.language }.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextPrimary,
                            )
                        }
                    }

                    // File info
                    container?.let { cont ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CONTAINER",
                                style = MaterialTheme.typography.labelMedium,
                                color = TvColors.TextSecondary,
                            )
                            Text(
                                text = cont.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextPrimary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium,
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = episode.name,
                modifier = Modifier
                    .width(160.dp)
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
                    color = TvColors.BluePrimary,
                )
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextPrimary,
                )
                episode.runtimeMinutes?.let { runtime ->
                    Text(
                        text = "$runtime min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                    )
                }
            }
        }
    }
}
