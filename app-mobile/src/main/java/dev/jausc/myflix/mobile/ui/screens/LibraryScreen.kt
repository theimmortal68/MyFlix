@file:Suppress(
    "MagicNumber",
    "LongMethod",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.components.library.LibraryFilterSheet
import dev.jausc.myflix.mobile.ui.components.library.LibrarySortSheet
import dev.jausc.myflix.mobile.ui.components.library.MobileLibraryFilterBar
import dev.jausc.myflix.mobile.ui.components.library.MobileLibraryListItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryId: String,
    libraryName: String,
    jellyfinClient: JellyfinClient,
    preferences: AppPreferences,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    // ViewModel with manual DI
    val viewModel: LibraryViewModel = viewModel(
        key = libraryId,
        factory = LibraryViewModel.Factory(libraryId, jellyfinClient, preferences),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Sheet state
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Scroll state for pagination
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // Pagination - load more when near end (grid)
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex >= state.items.lastIndex - 4 && state.canLoadMore) {
                    viewModel.loadMore()
                }
            }
    }

    // Pagination - load more when near end (list)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex >= state.items.lastIndex - 4 && state.canLoadMore) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(libraryName)
                        if (!state.isLoading && state.items.isNotEmpty()) {
                            Text(
                                text = "${state.items.size} of ${state.totalRecordCount} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Filter bar
            MobileLibraryFilterBar(
                filterState = state.filterState,
                availableGenres = state.availableGenres,
                onViewModeChange = { viewModel.setViewMode(it) },
                onGenreToggle = { viewModel.toggleGenre(it) },
                onSortClick = { showSortSheet = true },
                onFiltersClick = { showFilterSheet = true },
                onShuffleClick = {
                    viewModel.getShuffleItemId()?.let { itemId ->
                        onPlayClick(itemId)
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )

            // Content
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Error loading library",
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.isEmpty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (state.filterState.hasActiveFilters) {
                                "No items match your filters"
                            } else {
                                "This library is empty"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                else -> {
                    when (state.filterState.viewMode) {
                        LibraryViewMode.POSTER -> {
                            LibraryGridContent(
                                state = state,
                                gridState = gridState,
                                jellyfinClient = jellyfinClient,
                                onItemClick = onItemClick,
                            )
                        }
                        LibraryViewMode.THUMBNAIL -> {
                            LibraryListContent(
                                state = state,
                                listState = listState,
                                jellyfinClient = jellyfinClient,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                }
            }
        }

        // Sort sheet
        if (showSortSheet) {
            LibrarySortSheet(
                currentSortBy = state.filterState.sortBy,
                currentSortOrder = state.filterState.sortOrder,
                onSortSelected = { sortBy, sortOrder ->
                    viewModel.updateSort(sortBy, sortOrder)
                    showSortSheet = false
                },
                onDismiss = { showSortSheet = false },
            )
        }

        // Filter sheet
        if (showFilterSheet) {
            LibraryFilterSheet(
                currentWatchedFilter = state.filterState.watchedFilter,
                currentRatingFilter = state.filterState.ratingFilter,
                currentYearRange = state.filterState.yearRange,
                onApply = { watchedFilter, ratingFilter, yearRange ->
                    viewModel.applyFilters(watchedFilter, ratingFilter, yearRange)
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false },
            )
        }
    }
}

@Composable
private fun LibraryGridContent(
    state: LibraryUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 130.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { _, item ->
            LibraryGridCard(
                item = item,
                imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                onClick = { onItemClick(item.id) },
            )
        }

        // Loading more indicator
        if (state.isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryListContent(
    state: LibraryUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { _, item ->
            MobileLibraryListItem(
                item = item,
                imageUrl = jellyfinClient.getBackdropUrl(item.id, item.backdropImageTags?.firstOrNull())
                    .takeIf { item.backdropImageTags?.isNotEmpty() == true }
                    ?: jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                onClick = { onItemClick(item.id) },
            )
        }

        // Loading more indicator
        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

/**
 * Grid card for library items with progress and watched indicators.
 */
@Composable
private fun LibraryGridCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column {
            // Image with overlays
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )

                // Progress bar
                val progress = item.progressPercent
                if (progress > 0f && progress < 1f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomStart),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    )
                }

                // Watched indicator
                if (item.userData?.played == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Watched",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            // Info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Metadata row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Year
                    item.productionYear?.let { year ->
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Rating
                    item.communityRating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Content rating
                    item.officialRating?.let { rating ->
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}
