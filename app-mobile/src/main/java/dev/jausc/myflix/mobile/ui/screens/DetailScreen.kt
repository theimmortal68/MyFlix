package dev.jausc.myflix.mobile.ui.screens

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
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.*
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentItem = item
        if (isLoading || currentItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = jellyfinClient.getPrimaryImageUrl(currentItem.id, currentItem.imageTags?.primary, 300),
                            contentDescription = currentItem.name,
                            modifier = Modifier
                                .width(150.dp)
                                .aspectRatio(2f / 3f)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentItem.name,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                currentItem.productionYear?.let { year ->
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                currentItem.runtimeMinutes?.let { runtime ->
                                    Text(
                                        text = "${runtime} min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            currentItem.communityRating?.let { rating ->
                                Text(
                                    text = "★ %.1f".format(rating),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier.fillMaxWidth()
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (currentItem.isSeries && seasons.isNotEmpty()) {
                    item {
                        Text(
                            text = "Seasons",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(seasons, key = { it.id }) { season ->
                                val isSelected = selectedSeason?.id == season.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSeason = season },
                                    label = { Text(season.name) }
                                )
                            }
                        }
                    }

                    if (episodes.isNotEmpty()) {
                        item {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleMedium
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

@Composable
private fun EpisodeCard(
    episode: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = episode.name,
                modifier = Modifier
                    .width(120.dp)
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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                episode.runtimeMinutes?.let { runtime ->
                    Text(
                        text = "${runtime} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
