@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.viewmodel.SeerrHomeViewModel
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.screens.discover.components.DiscoverCarousel
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.launch

/**
 * Main Discover home screen for Discover V2.
 * Displays a vertical list of horizontal carousels for different media categories.
 *
 * Features:
 * - Vertical LazyColumn of DiscoverCarousel rows
 * - D-pad navigation between rows (up/down)
 * - Up at first row allows NavRail activation
 * - Auto-load more items when approaching end of each row
 * - Focus restoration for NavRail exit
 *
 * @param viewModel The SeerrHomeViewModel providing media data
 * @param onMediaClick Callback when a media item is clicked
 */
@Composable
fun DiscoverHomeScreen(
    viewModel: SeerrHomeViewModel,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    // Stable map of FocusRequesters per category (pattern from CLAUDE.md)
    val rowFocusRequesters = remember { mutableStateMapOf<MediaCategory, FocusRequester>() }
    fun getRowFocusRequester(category: MediaCategory): FocusRequester =
        rowFocusRequesters.getOrPut(category) { FocusRequester() }

    // Focus management - use first row's requester for exit focus
    val firstRowRequester = getRowFocusRequester(MediaCategory.TRENDING)
    val updateExitFocus = rememberExitFocusRegistry(firstRowRequester)

    // Track focused row for scroll management
    var focusedRowIndex by remember { mutableIntStateOf(0) }

    // Lazy list state for vertical scrolling
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Track pagination state for each row
    val rowPages = remember { mutableStateMapOf<String, Int>() }

    // Auto-scroll to focused row
    LaunchedEffect(focusedRowIndex) {
        coroutineScope.launch {
            listState.animateScrollToItem(focusedRowIndex)
        }
    }

    // Request focus when content loads
    LaunchedEffect(uiState.rows) {
        if (uiState.rows.isNotEmpty()) {
            // Short delay to allow composition to complete
            kotlinx.coroutines.delay(100)
            try {
                // Request focus on first row using stable requester
                getRowFocusRequester(MediaCategory.TRENDING).requestFocus()
            } catch (_: Exception) {
                // Focus request failed, ignore
            }
        }
    }

    // Build the list of categories to display in order
    val categoryRows = remember(uiState.rows) {
        listOf(
            MediaCategory.TRENDING to uiState.trendingRow,
            MediaCategory.POPULAR_MOVIES to uiState.popularMoviesRow,
            MediaCategory.POPULAR_TV to uiState.popularTvRow,
            MediaCategory.UPCOMING_MOVIES to uiState.upcomingMoviesRow,
            MediaCategory.UPCOMING_TV to uiState.upcomingTvRow,
        ).filter { it.second != null }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        when {
            // Not authenticated - show message
            !isAuthenticated -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = "Seerr not configured",
                            modifier = Modifier.size(64.dp),
                            tint = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Seerr not configured",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure Seerr in Settings to discover new content",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Loading state
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            // Error state
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.errorMessage ?: "Failed to load content",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary,
                        )
                    }
                }
            }

            // Content loaded - show carousels
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                            when (event.key) {
                                Key.DirectionUp -> {
                                    if (focusedRowIndex > 0) {
                                        focusedRowIndex--
                                        true
                                    } else {
                                        // At first row - don't consume, allow NavRail activation
                                        false
                                    }
                                }
                                Key.DirectionDown -> {
                                    if (focusedRowIndex < categoryRows.size - 1) {
                                        focusedRowIndex++
                                        true
                                    } else {
                                        true // At last row, consume to prevent exit
                                    }
                                }
                                else -> false
                            }
                        },
                    contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items = categoryRows,
                        key = { _, pair -> pair.first.name },
                    ) { index, (category, row) ->
                        val discoverRow = row ?: return@itemsIndexed
                        // Use stable focus requester from map (not created inside loop)
                        val rowFocusRequester = getRowFocusRequester(category)

                        // Get current page for this row
                        val currentPage = rowPages[discoverRow.key] ?: 1

                        DiscoverCarousel(
                            category = category,
                            items = discoverRow.items,
                            onItemClick = { media ->
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                            modifier = Modifier.focusRequester(rowFocusRequester),
                            isLoading = false, // Could track loading state per row
                            onLoadMore = {
                                val nextPage = currentPage + 1
                                rowPages[discoverRow.key] = nextPage
                                viewModel.loadMoreForRow(discoverRow.key, nextPage)
                            },
                            onRowFocused = {
                                focusedRowIndex = index
                                // Use stable requester from map for exit focus
                                updateExitFocus(rowFocusRequester)
                            },
                            onItemFocused = { media ->
                                // Could update a preview/hero section here if desired
                            },
                            focusRequester = rowFocusRequester,
                            accentColor = getAccentColorForCategory(category),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns the accent color for a given media category.
 */
@Composable
private fun getAccentColorForCategory(category: MediaCategory): Color {
    return when (category) {
        MediaCategory.TRENDING -> TvColors.BlueAccent
        MediaCategory.POPULAR_MOVIES -> TvColors.BluePrimary
        MediaCategory.POPULAR_TV -> TvColors.BlueLight
        MediaCategory.UPCOMING_MOVIES -> TvColors.Success
        MediaCategory.UPCOMING_TV -> TvColors.Success.copy(alpha = 0.8f)
        else -> TvColors.BluePrimary
    }
}
