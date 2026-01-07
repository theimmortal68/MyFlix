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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.*
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var item by remember { mutableStateOf<JellyfinItem?>(null) }
    var seasons by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<JellyfinItem?>(null) }
    var episodes by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(itemId) {
        scope.launch {
            jellyfinClient.getItem(itemId).onSuccess { loadedItem ->
                item = loadedItem

                if (loadedItem.isSeries) {
                    jellyfinClient.getSeasons(itemId).onSuccess { seasonList ->
                        seasons = seasonList
                        selectedSeason = seasonList.firstOrNull()
                        selectedSeason?.let { season ->
                            jellyfinClient.getEpisodes(itemId, season.id).onSuccess { eps ->
                                episodes = eps
                            }
                        }
                    }
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedSeason) {
        selectedSeason?.let { season ->
            item?.let { currentItem ->
                if (currentItem.isSeries) {
                    jellyfinClient.getEpisodes(currentItem.id, season.id).onSuccess { eps ->
                        episodes = eps
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        if (isLoading || item == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    color = TvColors.TextPrimary
                )
            }
        } else {
            val currentItem = item!!

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = jellyfinClient.getPrimaryImageUrl(currentItem.id, currentItem.imageTags?.primary, 400),
                        contentDescription = currentItem.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )

                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶ Play")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(end = 48.dp, top = 48.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = currentItem.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = TvColors.TextPrimary
                        )
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentItem.productionYear?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TvColors.TextSecondary
                                )
                            }
                            currentItem.runtimeMinutes?.let { runtime ->
                                Text(
                                    text = "${runtime} min",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TvColors.TextSecondary
                                )
                            }
                            currentItem.communityRating?.let { rating ->
                                Text(
                                    text = "★ %.1f".format(rating),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TvColors.BluePrimary
                                )
                            }
                        }
                    }

                    currentItem.overview?.let { overview ->
                        item {
                            Text(
                                text = overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary
                            )
                        }
                    }

                    if (currentItem.isSeries && seasons.isNotEmpty()) {
                        item {
                            Text(
                                text = "Seasons",
                                style = MaterialTheme.typography.titleMedium,
                                color = TvColors.TextPrimary
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(seasons, key = { it.id }) { season ->
                                    val isSelected = selectedSeason?.id == season.id
                                    Surface(
                                        onClick = { selectedSeason = season },
                                        shape = ClickableSurfaceDefaults.shape(
                                            shape = MaterialTheme.shapes.small
                                        ),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = if (isSelected) TvColors.BluePrimary else TvColors.Surface,
                                            focusedContainerColor = TvColors.FocusedSurface
                                        )
                                    ) {
                                        Text(
                                            text = season.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TvColors.TextPrimary,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (episodes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Episodes",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TvColors.TextPrimary
                                )
                            }

                            items(episodes, key = { it.id }) { episode ->
                                EpisodeCard(
                                    episode = episode,
                                    imageUrl = jellyfinClient.getPrimaryImageUrl(episode.id, episode.imageTags?.primary),
                                    onClick = { onEpisodeClick(episode.id) }
                                )
                            }
                        }
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = episode.name,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Episode ${episode.indexNumber ?: ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TvColors.BluePrimary
                )
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextPrimary
                )
                episode.runtimeMinutes?.let { runtime ->
                    Text(
                        text = "${runtime} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary
                    )
                }
            }
        }
    }
}
