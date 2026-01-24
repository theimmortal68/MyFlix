@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun SeerrSearchScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onPersonClick: (personId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SeerrSearchFilter.ALL) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }

    suspend fun loadSearch(pageToLoad: Int, append: Boolean) {
        if (query.length < 2) return
        if (append) {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        errorMessage = null

        seerrClient.search(query, pageToLoad)
            .onSuccess { response ->
                val filtered = response.results.filter { media ->
                    media.mediaType in allowedMediaTypes && selectedFilter.matches(media)
                }
                results = if (append) {
                    (results + filtered).distinctBy { "${it.mediaType}-${it.id}" }
                } else {
                    filtered
                }
                page = response.page
                totalPages = response.totalPages
            }
            .onFailure { error ->
                if (!append) {
                    errorMessage = error.message ?: "Search failed"
                }
            }

        if (append) {
            isLoadingMore = false
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(query, selectedFilter) {
        if (query.length < 2) {
            results = emptyList()
            errorMessage = null
            page = 1
            totalPages = 1
            isLoading = false
            isLoadingMore = false
            return@LaunchedEffect
        }

        results = emptyList()
        page = 1
        totalPages = 1
        loadSearch(pageToLoad = 1, append = false)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                val shouldLoadMore = lastVisibleIndex >= results.lastIndex - 3
                val hasMore = page < totalPages
                if (shouldLoadMore && hasMore && !isLoading && !isLoadingMore && query.length >= 2) {
                    scope.launch { loadSearch(pageToLoad = page + 1, append = true) }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Seerr Search",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search movies, TV, people") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SeerrSearchFilter.entries) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Search failed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            results.isEmpty() && query.length >= 2 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(results, key = { _, media -> "${media.mediaType}-${media.id}" }) { _, media ->
                        SeerrSearchMediaCard(
                            media = media,
                            seerrClient = seerrClient,
                            onClick = {
                                if (media.mediaType == "person") {
                                    onPersonClick(media.tmdbId ?: media.id)
                                } else {
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                }
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrSearchMediaCard(media: SeerrMedia, seerrClient: SeerrClient, onClick: () -> Unit,) {
    val imageUrl = if (media.mediaType == "person") {
        seerrClient.getProfileUrl(media.profilePath)
    } else {
        seerrClient.getPosterUrl(media.posterPath)
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = media.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mediaSecondaryLabel(media),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class SeerrSearchFilter(val label: String, val mediaType: String?) {
    ALL("All", null),
    MOVIES("Movies", "movie"),
    TV("TV", "tv"),
    PEOPLE("People", "person"),
    ;

    fun matches(media: SeerrMedia): Boolean = mediaType == null || media.mediaType == mediaType
}

private val allowedMediaTypes = setOf("movie", "tv", "person")

private fun mediaSecondaryLabel(media: SeerrMedia): String = when (media.mediaType) {
    "movie" -> media.year?.toString() ?: "Movie"
    "tv" -> media.year?.toString() ?: "TV"
    "person" -> "Person"
    else -> ""
}
