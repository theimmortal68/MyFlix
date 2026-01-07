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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    jellyfinClient: JellyfinClient,
    onLibraryClick: (String, String) -> Unit,
    onItemClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var nextUp by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var latestByLibrary by remember { mutableStateOf<Map<String, List<JellyfinItem>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            jellyfinClient.getLibraries().onSuccess { libs ->
                libraries = libs
                val latestMap = mutableMapOf<String, List<JellyfinItem>>()
                libs.forEach { lib ->
                    jellyfinClient.getLatestItems(lib.id).onSuccess { items ->
                        latestMap[lib.id] = items
                    }
                }
                latestByLibrary = latestMap
            }

            jellyfinClient.getContinueWatching().onSuccess { items ->
                continueWatching = items
            }

            jellyfinClient.getNextUp().onSuccess { items ->
                nextUp = items
            }

            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        if (isLoading) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                if (continueWatching.isNotEmpty()) {
                    item {
                        MediaRow(
                            title = "Continue Watching",
                            items = continueWatching,
                            jellyfinClient = jellyfinClient,
                            onItemClick = onItemClick
                        )
                    }
                }

                if (nextUp.isNotEmpty()) {
                    item {
                        MediaRow(
                            title = "Next Up",
                            items = nextUp,
                            jellyfinClient = jellyfinClient,
                            onItemClick = onItemClick
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            text = "Libraries",
                            style = MaterialTheme.typography.titleLarge,
                            color = TvColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(libraries, key = { it.id }) { library ->
                                Surface(
                                    onClick = { onLibraryClick(library.id, library.name) },
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(100.dp),
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
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = library.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TvColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                libraries.forEach { library ->
                    val latest = latestByLibrary[library.id] ?: emptyList()
                    if (latest.isNotEmpty()) {
                        item {
                            MediaRow(
                                title = "Latest in ${library.name}",
                                items = latest,
                                jellyfinClient = jellyfinClient,
                                onItemClick = onItemClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(
    title: String,
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}
