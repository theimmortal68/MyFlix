package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    jellyfinClient: JellyfinClient,
    onLibraryClick: (String, String) -> Unit,
    onItemClick: (String) -> Unit,
    onSearchClick: () -> Unit
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyFlix") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
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
                contentPadding = PaddingValues(vertical = 16.dp)
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
                    Text(
                        text = "Libraries",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(libraries, key = { it.id }) { library ->
                            ElevatedCard(
                                onClick = { onLibraryClick(library.id, library.name) },
                                modifier = Modifier.size(width = 150.dp, height = 80.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = library.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
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
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
private fun MediaCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(130.dp)
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
