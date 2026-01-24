@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.TvTextButton
import dev.jausc.myflix.tv.ui.components.library.AlphabetScrollBar
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

private const val COLUMNS = 7

/**
 * Collections library screen for browsing all BoxSet collections.
 * Library-style grid layout with alphabet navigation and pagination.
 */
@Composable
fun CollectionsLibraryScreen(
    jellyfinClient: JellyfinClient,
    onCollectionClick: (String) -> Unit,
    onNavigate: (NavItem) -> Unit,
    excludeUniverseCollections: Boolean = false,
    showUniversesInNav: Boolean = false,
    showDiscoverInNav: Boolean = false,
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
    val alphabetFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var didRequestInitialFocus by remember { mutableStateOf(false) }
    var lastFocusedForLetter by remember { mutableStateOf<Char?>(null) }

    // Track focused item for dynamic background
    var focusedImageUrl by remember { mutableStateOf<String?>(null) }
    val gradientColors = rememberGradientColors(focusedImageUrl)

    // All letters are always available - the API handles filtering
    // If a letter has no items, the user will see "0 items" which is acceptable
    // This provides immediate responsiveness rather than waiting for slow alphabet index
    val availableLetters = remember {
        setOf('#') + ('A'..'Z').toSet()
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

    // Focus on first item when loading completes
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(state.isLoading, state.items.isNotEmpty(), state.currentLetter) }
            .collect { (isLoading, hasItems, currentLetter) ->
                if (!isLoading && hasItems) {
                    val shouldFocus = !didRequestInitialFocus || lastFocusedForLetter != currentLetter
                    if (shouldFocus) {
                        didRequestInitialFocus = true
                        lastFocusedForLetter = currentLetter
                        delay(100)
                        try {
                            firstItemFocusRequester.requestFocus()
                        } catch (_: Exception) {
                            // Focus request failed
                        }
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic background that changes based on focused poster
        DynamicBackground(gradientColors = gradientColors)

        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Rail on the left
            NavigationRail(
                selectedItem = NavItem.COLLECTIONS,
                onItemSelected = onNavigate,
                showUniverses = showUniversesInNav,
                showDiscover = showDiscoverInNav,
                contentFocusRequester = firstItemFocusRequester,
            )

            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with title and count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Collections",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    if (!state.isLoading) {
                        val countText = if (state.currentLetter != null) {
                            "${state.items.size} of ${state.totalRecordCount}"
                        } else {
                            "${state.totalRecordCount} collections"
                        }
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary,
                        )
                    }
                }

                // Content
                when {
                    state.isLoading && state.items.isEmpty() -> {
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
                                text = "No collections available",
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
                                jellyfinClient = jellyfinClient,
                                firstItemFocusRequester = firstItemFocusRequester,
                                onCollectionClick = onCollectionClick,
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
        }
    }
}

@Composable
private fun CollectionsGridContent(
    state: CollectionsUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    jellyfinClient: JellyfinClient,
    firstItemFocusRequester: FocusRequester,
    onCollectionClick: (String) -> Unit,
    onItemFocused: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(COLUMNS),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, collection ->
            val isFirstItem = index == 0
            val imageUrl = jellyfinClient.getPrimaryImageUrl(
                collection.id,
                collection.imageTags?.primary,
            )

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
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .then(
                        if (isFirstItem) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onItemFocused(index, imageUrl)
                        }
                    },
            )
        }
    }
}
