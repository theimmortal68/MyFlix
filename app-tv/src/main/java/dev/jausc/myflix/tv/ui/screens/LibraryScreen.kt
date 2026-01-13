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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBar
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.library.AlphabetScrollBar
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterBar
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterDialog
import dev.jausc.myflix.tv.ui.components.library.LibrarySortDialog
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    libraryId: String,
    libraryName: String,
    jellyfinClient: JellyfinClient,
    preferences: AppPreferences,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onNavigate: (NavItem) -> Unit,
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
    val homeButtonFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Calculate available letters from items
    val availableLetters by remember(state.items) {
        derivedStateOf {
            state.items
                .mapNotNull { item ->
                    item.name.firstOrNull()?.uppercaseChar()
                }
                .toSet()
        }
    }

    // Map letter to first item index
    val letterIndexMap by remember(state.items) {
        derivedStateOf {
            val map = mutableMapOf<Char, Int>()
            state.items.forEachIndexed { index, item ->
                val letter = item.name.firstOrNull()?.uppercaseChar()
                    ?: return@forEachIndexed

                val normalizedLetter = if (letter.isLetter()) letter else '#'
                if (normalizedLetter !in map) {
                    map[normalizedLetter] = index
                }
            }
            map.toMap()
        }
    }

    // Determine which NavItem is selected based on library type
    val selectedNavItem = remember(libraryName) {
        when {
            libraryName.contains("Movie", ignoreCase = true) -> NavItem.MOVIES
            libraryName.contains("TV", ignoreCase = true) ||
                libraryName.contains("Show", ignoreCase = true) -> NavItem.SHOWS
            libraryName.contains("Collection", ignoreCase = true) -> NavItem.COLLECTIONS
            else -> NavItem.HOME
        }
    }

    // Grid columns based on view mode
    val columns = when (state.filterState.viewMode) {
        LibraryViewMode.POSTER -> 7
        LibraryViewMode.THUMBNAIL -> 4
    }

    // Aspect ratio based on view mode
    val aspectRatio = when (state.filterState.viewMode) {
        LibraryViewMode.POSTER -> 2f / 3f
        LibraryViewMode.THUMBNAIL -> 16f / 9f
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            TopNavigationBar(
                selectedItem = selectedNavItem,
                onItemSelected = onNavigate,
                homeButtonFocusRequester = homeButtonFocusRequester,
                downFocusRequester = firstItemFocusRequester,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter bar
            LibraryFilterBar(
                libraryName = libraryName,
                totalItems = state.totalRecordCount,
                loadedItems = state.items.size,
                filterState = state.filterState,
                onViewModeChange = { viewModel.setViewMode(it) },
                onFilterClick = { showFilterDialog = true },
                onSortClick = { showSortDialog = true },
                onShuffleClick = {
                    viewModel.getShuffleItemId()?.let { itemId ->
                        onPlayClick(itemId)
                    }
                },
                onScrollToTopClick = {
                    scope.launch {
                        gridState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content with alphabet scroll bar
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
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Main grid content
                        LibraryGridContent(
                            state = state,
                            gridState = gridState,
                            columns = columns,
                            aspectRatio = aspectRatio,
                            jellyfinClient = jellyfinClient,
                            firstItemFocusRequester = firstItemFocusRequester,
                            onItemClick = onItemClick,
                            modifier = Modifier.weight(1f),
                        )

                        // Alphabet scroll bar on right edge
                        AlphabetScrollBar(
                            availableLetters = availableLetters,
                            onLetterClick = { letter ->
                                val targetLetter = if (!letter.isLetter()) '#' else letter
                                letterIndexMap[targetLetter]?.let { index ->
                                    scope.launch {
                                        gridState.animateScrollToItem(index)
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                        )
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
    columns: Int,
    aspectRatio: Float,
    jellyfinClient: JellyfinClient,
    firstItemFocusRequester: FocusRequester,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
            val imageUrl = if (aspectRatio > 1f) {
                // Thumbnail mode - prefer backdrop
                jellyfinClient.getBackdropUrl(item.id, item.backdropImageTags?.firstOrNull())
                    .takeIf { item.backdropImageTags?.isNotEmpty() == true }
                    ?: jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary)
            } else {
                // Poster mode - use primary
                jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary)
            }

            MediaCard(
                item = item,
                imageUrl = imageUrl,
                onClick = { onItemClick(item.id) },
                aspectRatio = aspectRatio,
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
