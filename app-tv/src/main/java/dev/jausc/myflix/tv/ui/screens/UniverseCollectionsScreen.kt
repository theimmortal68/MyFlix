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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay

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
    showDiscoverInNav: Boolean = false,
) {
    // State
    var collections by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var didRequestInitialFocus by remember { mutableStateOf(false) }

    // Track focused collection for dynamic background
    var focusedImageUrl by remember { mutableStateOf<String?>(null) }
    val gradientColors = rememberGradientColors(focusedImageUrl)

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
        if (!isLoading && collections.isNotEmpty()) {
            if (!didRequestInitialFocus) {
                didRequestInitialFocus = true
                delay(100)
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: Exception) {
                    // Focus request failed, ignore
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
                selectedItem = NavItem.UNIVERSES,
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
                        text = "Universes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    if (!isLoading) {
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
                        // Grid of collections
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(COLUMNS),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
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
                                                focusedImageUrl = imageUrl
                                            }
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
