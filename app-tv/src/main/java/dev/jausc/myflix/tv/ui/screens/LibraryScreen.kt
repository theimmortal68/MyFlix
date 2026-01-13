@file:Suppress(
    "MagicNumber",
    "LongMethod",
)

package dev.jausc.myflix.tv.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.library.AlphabetScrollBar
import dev.jausc.myflix.tv.ui.components.library.LibraryFilterBar
import dev.jausc.myflix.tv.ui.components.rememberNavBarPopupState
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun LibraryScreen(
    libraryId: String,
    libraryName: String,
    collectionType: String?,
    jellyfinClient: JellyfinClient,
    preferences: AppPreferences,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onNavigate: (NavItem) -> Unit,
) {
    // ViewModel with manual DI
    val viewModel: LibraryViewModel = viewModel(
        key = libraryId,
        factory = LibraryViewModel.Factory(libraryId, collectionType, jellyfinClient, preferences),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Nav bar popup state (auto-hides after 5 seconds like HomeScreen)
    val navBarState = rememberNavBarPopupState()

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val filterBarFocusRequester = remember { FocusRequester() }
    val filterBarFirstButtonFocusRequester = remember { FocusRequester() }
    val alphabetFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var didRequestInitialFocus by remember { mutableStateOf(false) }

    // Track focused item for dynamic background
    var focusedImageUrl by remember { mutableStateOf<String?>(null) }
    val gradientColors = rememberGradientColors(focusedImageUrl)

    // All letters are always available - the API handles filtering
    // If a letter has no items, the user will see "0 items" which is acceptable
    // This provides immediate responsiveness rather than waiting for slow alphabet index
    val availableLetters = remember {
        setOf('#') + ('A'..'Z').toSet()
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

    // Track the last letter we focused for to detect changes
    var lastFocusedForLetter by remember { mutableStateOf<Char?>(null) }

    // Focus on first item when loading completes
    // Uses snapshotFlow to reliably detect loading state transitions
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(state.isLoading, state.items.isNotEmpty(), state.currentLetter) }
            .collect { (isLoading, hasItems, currentLetter) ->
                if (!isLoading && hasItems) {
                    // Focus if initial load OR letter changed
                    val shouldFocus = !didRequestInitialFocus || lastFocusedForLetter != currentLetter
                    if (shouldFocus) {
                        didRequestInitialFocus = true
                        lastFocusedForLetter = currentLetter
                        // Delay to ensure grid is rendered
                        delay(300)
                        try {
                            firstItemFocusRequester.requestFocus()
                        } catch (_: Exception) {
                            // Focus request failed, ignore
                        }
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic background that changes based on focused poster
        DynamicBackground(gradientColors = gradientColors)

        Column(modifier = Modifier.fillMaxSize()) {
            // Space for nav bar (content starts below where nav bar would be)
            Spacer(modifier = Modifier.height(40.dp))

            // Filter bar - shows nav bar when navigating up
            LibraryFilterBar(
                libraryName = libraryName,
                totalItems = state.totalRecordCount,
                loadedItems = state.items.size,
                filterState = state.filterState,
                availableGenres = state.availableGenres,
                availableParentalRatings = state.availableParentalRatings,
                onViewModeChange = { viewModel.setViewMode(it) },
                onSortChange = { sortBy, sortOrder ->
                    viewModel.updateSort(sortBy, sortOrder)
                },
                onFilterChange = { watchedFilter, ratingFilter, yearRange ->
                    viewModel.applyFilters(watchedFilter, ratingFilter, yearRange)
                },
                onGenreToggle = { viewModel.toggleGenre(it) },
                onClearGenres = { viewModel.clearGenres() },
                onParentalRatingToggle = { viewModel.toggleParentalRating(it) },
                onClearParentalRatings = { viewModel.clearParentalRatings() },
                onShuffleClick = {
                    viewModel.getShuffleItemId()?.let { itemId ->
                        onItemClick(itemId)
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .focusRequester(filterBarFocusRequester),
                onUpNavigation = { navBarState.show() },
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
                            filterBarFocusRequester = filterBarFirstButtonFocusRequester,
                            alphabetFocusRequester = alphabetFocusRequester,
                            onItemClick = onItemClick,
                            onUpNavigation = { navBarState.show() },
                            onItemFocused = { _, imageUrl ->
                                focusedImageUrl = imageUrl
                            },
                            modifier = Modifier.weight(1f),
                        )

                        // Alphabet scroll bar on right edge
                        AlphabetScrollBar(
                            availableLetters = availableLetters,
                            currentLetter = state.currentLetter,
                            onLetterClick = { letter ->
                                // Use server-side nameStartsWith filter
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

        // Top Navigation Bar Popup (overlay, auto-hides)
        TopNavigationBarPopup(
            visible = navBarState.isVisible,
            selectedItem = selectedNavItem,
            onItemSelected = onNavigate,
            onDismiss = {
                navBarState.hide()
                try {
                    filterBarFocusRequester.requestFocus()
                } catch (_: Exception) {
                    try {
                        firstItemFocusRequester.requestFocus()
                    } catch (_: Exception) {
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )

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
    filterBarFocusRequester: FocusRequester,
    alphabetFocusRequester: FocusRequester,
    onItemClick: (String) -> Unit,
    onUpNavigation: () -> Unit,
    onItemFocused: (JellyfinItem, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dummy focus requester to block focus movement (requests to unattached requester fail)
    val focusTrap = remember { FocusRequester() }

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

            // Calculate row position
            val isFirstRow = index < columns
            val isLastColumn = (index + 1) % columns == 0

            // Calculate if this item is in the last row
            val totalRows = (state.items.size + columns - 1) / columns
            val itemRow = index / columns
            val isLastRow = itemRow == totalRows - 1

            // Block down navigation when loading more and on last row
            val shouldBlockDown = state.isLoadingMore && isLastRow

            MediaCard(
                item = item,
                imageUrl = imageUrl,
                onClick = { onItemClick(item.id) },
                aspectRatio = aspectRatio,
                showLabel = true,
                onItemFocused = { focusedItem ->
                    onItemFocused(focusedItem, imageUrl)
                },
                modifier = Modifier
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
