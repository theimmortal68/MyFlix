@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
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
) {
    // State
    var collections by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val navBarFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var didRequestInitialFocus by remember { mutableStateOf(false) }

    // Track focused collection for hero section
    var focusedCollection by remember { mutableStateOf<JellyfinItem?>(null) }

    // Use focused collection for backdrop and gradient
    val backdropUrl = remember(focusedCollection?.id) {
        focusedCollection?.let {
            jellyfinClient.getBackdropUrl(it.id, it.backdropImageTags?.firstOrNull())
        }
    }
    val gradientColors = rememberGradientColors(backdropUrl)

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

    // Focus on first item when loading completes and set initial focused collection
    LaunchedEffect(isLoading, collections) {
        if (!isLoading && collections.isNotEmpty()) {
            // Set initial focused collection for hero
            if (focusedCollection == null) {
                focusedCollection = collections.first()
            }
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

    // Loading state
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            TvLoadingIndicator()
        }
        return
    }

    // Error state
    if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = error ?: "Error loading universe collections",
                color = TvColors.Error,
            )
        }
        return
    }

    // Empty state
    if (collections.isEmpty()) {
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
        return
    }

    // Layered UI: DynamicBackground → DetailBackdropLayer → Content
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Backdrop image (right side, behind content)
        focusedCollection?.let { collection ->
            DetailBackdropLayer(
                item = collection,
                jellyfinClient = jellyfinClient,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .align(Alignment.TopEnd),
            )
        }

        // Layer 3: Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed hero section (45% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f),
            ) {
                // Hero content (left 50%) - title, overview, item count
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = 48.dp, top = 48.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    focusedCollection?.let { collection ->
                        UniverseHeroContent(
                            collection = collection,
                        )
                    }
                }
            }

            // Grid of collections below hero
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(COLUMNS),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = collections,
                    key = { _, item -> item.id },
                ) { index, collection ->
                    val isFirstItem = index == 0
                    val isFirstRow = index < COLUMNS
                    val imageUrl = jellyfinClient.getPrimaryImageUrl(
                        collection.id,
                        collection.imageTags?.primary,
                    )

                    MediaCard(
                        item = collection,
                        imageUrl = imageUrl,
                        onClick = { onCollectionClick(collection.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .then(
                                if (isFirstRow) {
                                    Modifier.focusProperties { up = navBarFocusRequester }
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (isFirstItem) {
                                    Modifier.focusRequester(firstItemFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusedCollection = collection
                                }
                            },
                    )
                }
            }
        }

        // Top Navigation Bar (always visible)
        TopNavigationBarPopup(
            selectedItem = NavItem.UNIVERSES,
            onItemSelected = onNavigate,
            showUniverses = showUniversesInNav,
            contentFocusRequester = firstItemFocusRequester,
            focusRequester = navBarFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * Hero content for the focused universe collection.
 */
@Composable
private fun UniverseHeroContent(
    collection: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Page title
        Text(
            text = "Universes",
            style = MaterialTheme.typography.labelLarge,
            color = TvColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Collection title
        Text(
            text = collection.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Item count
        val itemCount = collection.childCount ?: collection.recursiveItemCount
        if (itemCount != null && itemCount > 0) {
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Overview/Description
        collection.overview?.let { overview ->
            Text(
                text = overview,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
        }
    }
}
