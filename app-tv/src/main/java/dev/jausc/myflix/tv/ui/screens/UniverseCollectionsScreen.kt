@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.rememberNavBarPopupState
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS = 7

/**
 * Universe collections screen for browsing BoxSet collections tagged with "universe-collection".
 * These are franchise groupings like Marvel Universe, Star Wars Universe, etc.
 * Library-style grid layout matching Collections screen.
 */
@Composable
fun UniverseCollectionsScreen(
    jellyfinClient: JellyfinClient,
    onCollectionClick: (String) -> Unit,
    onNavigate: (NavItem) -> Unit,
    showUniversesInNav: Boolean = true,
) {
    // State
    var collections by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val homeButtonFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var didRequestInitialFocus by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Track which row is focused (for up navigation to show nav bar)
    var focusedIndex by remember { mutableIntStateOf(0) }

    // Track focused item for dynamic background
    var focusedImageUrl by remember { mutableStateOf<String?>(null) }
    val gradientColors = rememberGradientColors(focusedImageUrl)

    // Nav bar popup state
    val navBarState = rememberNavBarPopupState()

    // Load universe collections (filtered by tag)
    LaunchedEffect(Unit) {
        jellyfinClient.getUniverseCollections(limit = 200)
            .onSuccess {
                collections = it
                isLoading = false
            }
            .onFailure {
                error = it.message ?: "Failed to load universe collections"
                isLoading = false
            }
    }

    // Focus on first item when loading completes
    LaunchedEffect(isLoading, collections) {
        if (!isLoading && collections.isNotEmpty() && !didRequestInitialFocus) {
            didRequestInitialFocus = true
            delay(100)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus request failed, ignore
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic background that changes based on focused poster
        DynamicBackground(gradientColors = gradientColors)

        Column(modifier = Modifier.fillMaxSize()) {
            // Space for nav bar
            Spacer(modifier = Modifier.height(40.dp))

            // Header with title and count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Universes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TvColors.TextPrimary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                if (!isLoading && collections.isNotEmpty()) {
                    Text(
                        text = "${collections.size} universes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TvLoadingIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = error ?: "Error loading universe collections",
                            color = TvColors.Error,
                        )
                    }
                }
                collections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No universe collections found",
                                color = TvColors.TextSecondary,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tag collections with \"universe-collection\" to see them here",
                                color = TvColors.TextSecondary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                else -> {
                    // Grid of collections (library-style layout)
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(COLUMNS),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .onPreviewKeyEvent { keyEvent ->
                                // Show nav bar when pressing UP from first row
                                val firstVisibleIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                                val isAtTop = firstVisibleIndex < COLUMNS
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    keyEvent.key == Key.DirectionUp &&
                                    isAtTop
                                ) {
                                    navBarState.show()
                                    coroutineScope.launch {
                                        try {
                                            homeButtonFocusRequester.requestFocus()
                                        } catch (_: Exception) {
                                            // Ignore focus errors
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                    ) {
                        itemsIndexed(
                            items = collections,
                            key = { _, item -> item.id },
                        ) { index, collection ->
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
                                            focusedIndex = index
                                            focusedImageUrl = imageUrl
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }

        // Top Navigation Bar Popup (overlay, auto-hides)
        TopNavigationBarPopup(
            visible = navBarState.isVisible,
            selectedItem = NavItem.UNIVERSES,
            onItemSelected = onNavigate,
            onDismiss = {
                navBarState.hide()
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: Exception) {
                    // Ignore focus errors
                }
            },
            showUniverses = showUniversesInNav,
            homeButtonFocusRequester = homeButtonFocusRequester,
        )
    }
}
