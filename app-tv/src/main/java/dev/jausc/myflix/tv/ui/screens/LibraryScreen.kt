@file:Suppress(
    "MagicNumber",
    "LongMethod",
)

package dev.jausc.myflix.tv.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterBar
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterDialog
import dev.jausc.myflix.tv.ui.components.library.LibraryListItem
import dev.jausc.myflix.tv.ui.components.library.LibrarySortDialog
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

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

    // Dialog state
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // Pagination - load more when near end
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex >= state.items.lastIndex - 4 && state.canLoadMore) {
                    viewModel.loadMore()
                }
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex >= state.items.lastIndex - 4 && state.canLoadMore) {
                    viewModel.loadMore()
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header row with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    scale = ButtonDefaults.scale(focusedScale = 1f),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = libraryName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TvColors.TextPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                // Item count
                if (!state.isLoading && state.items.isNotEmpty()) {
                    Text(
                        text = "${state.items.size} of ${state.totalRecordCount} items",
                        style = MaterialTheme.typography.labelMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }

            // Filter bar
            LibraryFilterBar(
                filterState = state.filterState,
                availableGenres = state.availableGenres,
                onViewModeChange = { viewModel.setViewMode(it) },
                onGenreToggle = { viewModel.toggleGenre(it) },
                onSortClick = { showSortDialog = true },
                onFiltersClick = { showFilterDialog = true },
                onShuffleClick = {
                    viewModel.getShuffleItemId()?.let { itemId ->
                        onPlayClick(itemId)
                    }
                },
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TvLoadingIndicator()
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
                                color = TvColors.Error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.colors(
                                    containerColor = TvColors.BluePrimary,
                                    contentColor = Color.White,
                                ),
                            ) {
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
                            color = TvColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                else -> {
                    when (state.filterState.viewMode) {
                        LibraryViewMode.GRID -> {
                            LibraryGridContent(
                                state = state,
                                gridState = gridState,
                                jellyfinClient = jellyfinClient,
                                firstItemFocusRequester = firstItemFocusRequester,
                                onItemClick = onItemClick,
                            )
                        }
                        LibraryViewMode.LIST -> {
                            LibraryListContent(
                                state = state,
                                listState = listState,
                                jellyfinClient = jellyfinClient,
                                firstItemFocusRequester = firstItemFocusRequester,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                }
            }
        }

        // Sort dialog
        if (showSortDialog) {
            LibrarySortDialog(
                currentSortBy = state.filterState.sortBy,
                currentSortOrder = state.filterState.sortOrder,
                onSortSelected = { sortBy, sortOrder ->
                    viewModel.updateSort(sortBy, sortOrder)
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false },
            )
        }

        // Filter dialog
        if (showFilterDialog) {
            LibraryFilterDialog(
                currentWatchedFilter = state.filterState.watchedFilter,
                currentRatingFilter = state.filterState.ratingFilter,
                currentYearRange = state.filterState.yearRange,
                onApply = { watchedFilter, ratingFilter, yearRange ->
                    viewModel.applyFilters(watchedFilter, ratingFilter, yearRange)
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false },
            )
        }
    }
}

@Composable
private fun LibraryGridContent(
    state: LibraryUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    jellyfinClient: JellyfinClient,
    firstItemFocusRequester: FocusRequester,
    onItemClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
            MediaCard(
                item = item,
                imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                onClick = { onItemClick(item.id) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                },
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
                    TvLoadingIndicator()
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
    firstItemFocusRequester: FocusRequester,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
            LibraryListItem(
                item = item,
                imageUrl = jellyfinClient.getBackdropUrl(item.id, item.backdropImageTags?.firstOrNull())
                    .takeIf { item.backdropImageTags?.isNotEmpty() == true }
                    ?: jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                onClick = { onItemClick(item.id) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                },
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
                    TvLoadingIndicator()
                }
            }
        }
    }
}
