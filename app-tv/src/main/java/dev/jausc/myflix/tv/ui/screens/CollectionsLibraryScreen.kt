@file:Suppress(
    "MagicNumber",
    "LongMethod",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.ui.HomeActions
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MenuAnchor
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.TvTextButton
import dev.jausc.myflix.tv.ui.components.buildHomeDialogItems
import dev.jausc.myflix.tv.ui.components.library.AlphabetScrollBar
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterBar
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterMenu
import dev.jausc.myflix.tv.ui.components.library.LibrarySortMenu
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

/**
 * Collections library screen for browsing all BoxSet collections.
 * Library-style grid layout with filter bar, alphabet navigation, and pagination.
 */
@Composable
fun CollectionsLibraryScreen(
    jellyfinClient: JellyfinClient,
    onCollectionClick: (String) -> Unit,
    excludeUniverseCollections: Boolean = false,
) {
    // ViewModel with manual DI
    val viewModel: CollectionsViewModel = viewModel(
        key = "collections:$excludeUniverseCollections",
        factory = CollectionsViewModel.Factory(jellyfinClient, excludeUniverseCollections),
    )

    // Collect UI state
    val state by viewModel.uiState.collectAsState()

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val filterBarFocusRequester = remember { FocusRequester() }
    val filterBarFirstButtonFocusRequester = remember { FocusRequester() }
    val alphabetFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var didRequestInitialFocus by remember { mutableStateOf(false) }

    // Track last focused item for reliable focus restoration after NavRail exit
    var lastFocusedRequester by remember { mutableStateOf<FocusRequester?>(null) }

    // NavRail exit focus registration - use last focused item, fallback to first
    val updateExitFocus = rememberExitFocusRegistry(lastFocusedRequester ?: firstItemFocusRequester)
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var filterMenuAnchor by remember { mutableStateOf<MenuAnchor?>(null) }
    var sortMenuAnchor by remember { mutableStateOf<MenuAnchor?>(null) }

    // Long-press dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var dialogVisible by remember { mutableStateOf(false) }
    var dialogRestoreFocus by remember { mutableStateOf<FocusRequester?>(null) }

    // Dialog actions for long-press menu
    val dialogActions = remember(viewModel) {
        HomeActions(
            onGoTo = onCollectionClick,
            onPlay = { /* Collections don't have direct play */ },
            onMarkWatched = { itemId, watched ->
                viewModel.setPlayed(itemId, watched)
            },
            onToggleFavorite = { itemId, favorite ->
                viewModel.setFavorite(itemId, favorite)
            },
        )
    }

    // All letters are always available - the API handles filtering
    val availableLetters = remember {
        setOf('#') + ('A'..'Z').toSet()
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

    // Track the last letter we focused for to detect changes
    var lastFocusedForLetter by remember { mutableStateOf<Char?>(null) }

    // Focus on first item when loading completes
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(state.isLoading, state.items.isNotEmpty(), state.currentLetter) }
            .collect { (isLoading, hasItems, currentLetter) ->
                if (!isLoading && hasItems) {
                    val shouldFocus = !didRequestInitialFocus || lastFocusedForLetter != currentLetter
                    if (shouldFocus) {
                        didRequestInitialFocus = true
                        lastFocusedForLetter = currentLetter
                        gridState.scrollToItem(0)
                        delay(50)
                        repeat(5) { attempt ->
                            delay(if (attempt == 0) 50 else 100)
                            try {
                                firstItemFocusRequester.requestFocus()
                                return@collect
                            } catch (_: Exception) {
                                // Focus request failed, retry
                            }
                        }
                    }
                }
            }
    }

    // Outer wrapper - menus need to be outside focusGroup to avoid focusRestorer interference
    Box(modifier = Modifier.fillMaxSize()) {
        // Black background (like library screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvColors.Background),
        )

        // focusGroup + focusRestorer saves/restores focus when NavRail is entered/exited
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .focusRestorer(lastFocusedRequester ?: firstItemFocusRequester),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top padding for content
                    Spacer(modifier = Modifier.height(16.dp))

                    // Filter bar
                    LibraryFilterBar(
                        libraryName = "Collections",
                        totalItems = state.totalRecordCount,
                        loadedItems = state.items.size,
                        filterState = state.filterState,
                        onViewModeChange = { viewModel.setViewMode(it) },
                        onShuffleClick = {
                            viewModel.getShuffleItemId()?.let { itemId ->
                                onCollectionClick(itemId)
                            }
                        },
                        onFilterMenuRequested = { showFilterMenu = true },
                        onSortMenuRequested = { showSortMenu = true },
                        onFilterAnchorChanged = { filterMenuAnchor = it },
                        onSortAnchorChanged = { sortMenuAnchor = it },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .focusRequester(filterBarFocusRequester),
                        firstButtonFocusRequester = filterBarFirstButtonFocusRequester,
                        gridFocusRequester = firstItemFocusRequester,
                        alphabetFocusRequester = alphabetFocusRequester,
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
                                        text = state.error ?: "Error loading collections",
                                        color = TvColors.Error,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TvTextButton(
                                        text = "Retry",
                                        onClick = { viewModel.refresh() },
                                    )
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
                                        "No collections match your filters"
                                    } else {
                                        "No collections available"
                                    },
                                    color = TvColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                        else -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Main grid content
                                CollectionsGridContent(
                                    state = state,
                                    gridState = gridState,
                                    columns = columns,
                                    aspectRatio = aspectRatio,
                                    jellyfinClient = jellyfinClient,
                                    firstItemFocusRequester = firstItemFocusRequester,
                                    filterBarFocusRequester = filterBarFirstButtonFocusRequester,
                                    alphabetFocusRequester = alphabetFocusRequester,
                                    onCollectionClick = onCollectionClick,
                                    onCollectionLongClick = { item, focusRequester ->
                                        dialogRestoreFocus = focusRequester
                                        dialogParams = DialogParams(
                                            title = item.name,
                                            items = buildHomeDialogItems(item, dialogActions),
                                            fromLongClick = true,
                                            restoreFocusRequester = focusRequester,
                                        )
                                        dialogVisible = true
                                    },
                                    onItemFocused = { _, _, focusRequester ->
                                        lastFocusedRequester = focusRequester
                                        updateExitFocus(focusRequester)
                                    },
                                    modifier = Modifier.weight(1f),
                                )

                                // Alphabet scroll bar on right edge
                                AlphabetScrollBar(
                                    availableLetters = availableLetters,
                                    currentLetter = state.currentLetter,
                                    onLetterClick = { letter ->
                                        viewModel.jumpToLetter(letter)
                                    },
                                    onClearFilter = {
                                        viewModel.clearLetterFilter()
                                    },
                                    focusRequester = alphabetFocusRequester,
                                    gridFocusRequester = firstItemFocusRequester,
                                    modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Long-press context menu dialog - INSIDE focusGroup to prevent focus escape
            dialogParams?.let { params ->
                DialogPopup(
                    params = params,
                    visible = dialogVisible,
                    onDismissRequest = { dialogVisible = false },
                    onDismissed = {
                        dialogParams = null
                        dialogRestoreFocus = null
                    },
                )
            }
        }

        // Menus are OUTSIDE focusGroup to avoid focusRestorer interference
        LibraryFilterMenu(
            visible = showFilterMenu,
            currentWatchedFilter = state.filterState.watchedFilter,
            currentRatingFilter = state.filterState.ratingFilter,
            currentYearRange = state.filterState.yearRange,
            currentSeriesStatus = state.filterState.seriesStatus,
            currentFavoritesOnly = state.filterState.favoritesOnly,
            availableGenres = state.availableGenres.map { it.name },
            selectedGenres = state.filterState.selectedGenres,
            availableParentalRatings = state.availableParentalRatings,
            selectedParentalRatings = state.filterState.selectedParentalRatings,
            showSeriesStatusFilter = false, // Collections don't have series status
            onGenreToggle = { viewModel.toggleGenre(it) },
            onClearGenres = { viewModel.clearGenres() },
            onParentalRatingToggle = { viewModel.toggleParentalRating(it) },
            onClearParentalRatings = { viewModel.clearParentalRatings() },
            onFilterChange = { watched, rating ->
                viewModel.applyFilters(watched, rating, state.filterState.yearRange, state.filterState.favoritesOnly)
            },
            onYearRangeChange = { range ->
                viewModel.applyFilters(state.filterState.watchedFilter, state.filterState.ratingFilter, range, state.filterState.favoritesOnly)
            },
            onSeriesStatusChange = { /* Not used for collections */ },
            onFavoritesOnlyChange = { favorites ->
                viewModel.applyFilters(state.filterState.watchedFilter, state.filterState.ratingFilter, state.filterState.yearRange, favorites)
            },
            onDismiss = {
                showFilterMenu = false
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: Exception) {
                    // Focus request failed, ignore
                }
            },
            anchor = filterMenuAnchor,
        )

        LibrarySortMenu(
            visible = showSortMenu,
            currentSortBy = state.filterState.sortBy,
            currentSortOrder = state.filterState.sortOrder,
            onSortChange = { sortBy, sortOrder -> viewModel.updateSort(sortBy, sortOrder) },
            onDismiss = {
                showSortMenu = false
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: Exception) {
                    // Focus request failed, ignore
                }
            },
            anchor = sortMenuAnchor,
        )
    }
}

@Composable
private fun CollectionsGridContent(
    state: CollectionsUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    columns: Int,
    aspectRatio: Float,
    jellyfinClient: JellyfinClient,
    firstItemFocusRequester: FocusRequester,
    filterBarFocusRequester: FocusRequester,
    alphabetFocusRequester: FocusRequester,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (JellyfinItem, FocusRequester) -> Unit,
    onItemFocused: (JellyfinItem, String, FocusRequester) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dummy focus requester to block focus movement (requests to unattached requester fail)
    val focusTrap = remember { FocusRequester() }

    // Stable map of focus requesters for each item (for long-press focus restoration)
    val itemFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun getItemFocusRequester(itemId: String): FocusRequester =
        itemFocusRequesters.getOrPut(itemId) { FocusRequester() }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, collection ->
            val imageUrl = if (aspectRatio > 1f) {
                // Thumbnail mode - prefer backdrop
                jellyfinClient.getBackdropUrl(collection.id, collection.backdropImageTags?.firstOrNull())
                    .takeIf { collection.backdropImageTags?.isNotEmpty() == true }
                    ?: jellyfinClient.getPrimaryImageUrl(collection.id, collection.imageTags?.primary)
            } else {
                // Poster mode - use primary
                jellyfinClient.getPrimaryImageUrl(collection.id, collection.imageTags?.primary)
            }

            // Calculate row position
            val isFirstRow = index < columns
            val isLastColumn = (index + 1) % columns == 0

            // Calculate if this item is in the last row
            val totalRows = (state.items.size + columns - 1) / columns
            val itemRow = index / columns
            val isLastRow = itemRow == totalRows - 1

            // Block down navigation when loading more and on last row
            val shouldBlockDown = state.isLoadingMore && isLastRow

            val itemFocusRequester = getItemFocusRequester(collection.id)

            // Build label with item count
            val itemCount = collection.childCount ?: collection.recursiveItemCount
            val label = if (itemCount != null && itemCount > 0) {
                "${collection.name} • $itemCount items"
            } else {
                collection.name
            }

            MediaCard(
                item = collection.copy(name = label),
                imageUrl = imageUrl,
                onClick = { onCollectionClick(collection.id) },
                onLongClick = { onCollectionLongClick(collection, itemFocusRequester) },
                aspectRatio = aspectRatio,
                showLabel = true,
                onItemFocused = { focusedItem ->
                    onItemFocused(focusedItem, imageUrl, itemFocusRequester)
                },
                modifier = Modifier
                    .focusRequester(itemFocusRequester)
                    .then(
                        if (index == 0) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .focusProperties {
                        if (isFirstRow) {
                            up = filterBarFocusRequester
                        }
                        if (isLastColumn) {
                            right = alphabetFocusRequester
                        }
                        if (shouldBlockDown) {
                            down = focusTrap
                        }
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
