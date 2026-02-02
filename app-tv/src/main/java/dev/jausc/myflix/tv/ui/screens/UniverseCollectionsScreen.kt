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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
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
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterBar
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterMenu
import dev.jausc.myflix.tv.ui.components.library.LibrarySortMenu
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.delay

/**
 * Universe collections screen for browsing BoxSet collections tagged with "universe-collection".
 * These are franchise groupings like Marvel Universe, Star Wars Universe, etc.
 * Library-style grid layout with filter bar and long-press menu.
 */
@Composable
fun UniverseCollectionsScreen(
    jellyfinClient: JellyfinClient,
    onCollectionClick: (String) -> Unit,
) {
    // ViewModel with manual DI
    val viewModel: UniverseCollectionsViewModel = viewModel(
        key = "universe-collections",
        factory = UniverseCollectionsViewModel.Factory(jellyfinClient),
    )

    // Collect UI state
    val state by viewModel.uiState.collectAsState()

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val filterBarFocusRequester = remember { FocusRequester() }
    val filterBarFirstButtonFocusRequester = remember { FocusRequester() }
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

    // Focus on first item when loading completes
    LaunchedEffect(Unit) {
        snapshotFlow { state.isLoading to state.items.isNotEmpty() }
            .collect { (isLoading, hasItems) ->
                if (!isLoading && hasItems && !didRequestInitialFocus) {
                    didRequestInitialFocus = true
                    delay(100)
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
                        libraryName = "Universes",
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
                        alphabetFocusRequester = null, // No alphabet bar for universe collections
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
                                        text = state.error ?: "Error loading universe collections",
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (state.filterState.hasActiveFilters) {
                                            "No universes match your filters"
                                        } else {
                                            "No universe collections found"
                                        },
                                        color = TvColors.TextSecondary,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    if (!state.filterState.hasActiveFilters) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tag collections with \"universe-collection\" to see them here",
                                            color = TvColors.TextSecondary.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // Grid of collections
                            UniverseCollectionsGridContent(
                                state = state,
                                gridState = gridState,
                                columns = columns,
                                aspectRatio = aspectRatio,
                                jellyfinClient = jellyfinClient,
                                firstItemFocusRequester = firstItemFocusRequester,
                                filterBarFocusRequester = filterBarFirstButtonFocusRequester,
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
                                modifier = Modifier.fillMaxSize(),
                            )
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
private fun UniverseCollectionsGridContent(
    state: UniverseCollectionsUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    columns: Int,
    aspectRatio: Float,
    jellyfinClient: JellyfinClient,
    firstItemFocusRequester: FocusRequester,
    filterBarFocusRequester: FocusRequester,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (JellyfinItem, FocusRequester) -> Unit,
    onItemFocused: (JellyfinItem, String, FocusRequester) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stable map of focus requesters for each item (for long-press focus restoration)
    val itemFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun getItemFocusRequester(itemId: String): FocusRequester =
        itemFocusRequesters.getOrPut(itemId) { FocusRequester() }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
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
                    },
            )
        }
    }
}
