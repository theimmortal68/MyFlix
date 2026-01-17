@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Shuffle
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
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
import kotlinx.coroutines.launch

// Grid configuration
private const val GRID_COLUMNS = 7

// Row indices for focus management
private const val HEADER_ROW = 0
private const val ITEMS_ROW = HEADER_ROW + 1

/**
 * Collection detail screen showing collection info and all items within it.
 * Plex-style hero layout with backdrop and 7-column grid of items.
 */
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onPlayClick: (String, Long?) -> Unit,
    onBack: () -> Unit,
    onNavigate: (NavItem) -> Unit = {},
    showUniversesInNav: Boolean = false,
) {
    val scope = rememberCoroutineScope()

    // State
    var collection by remember { mutableStateOf<JellyfinItem?>(null) }
    var items by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(ITEMS_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val shuffleFocusRequester = remember { FocusRequester() }
    val navBarFocusRequester = remember { FocusRequester() }

    // Load collection data
    LaunchedEffect(collectionId) {
        isLoading = true
        error = null

        // Load collection details
        jellyfinClient.getItem(collectionId)
            .onSuccess { item ->
                collection = item
            }
            .onFailure {
                error = it.message ?: "Failed to load collection"
            }

        // Load collection items
        jellyfinClient.getCollectionItems(collectionId, limit = 100)
            .onSuccess { result ->
                items = result
            }
            .onFailure {
                if (error == null) {
                    error = it.message ?: "Failed to load collection items"
                }
            }

        isLoading = false
    }

    // Request initial focus
    LaunchedEffect(isLoading, collection) {
        if (!isLoading && collection != null) {
            delay(300)
            try {
                shuffleFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus request failed
            }
        }
    }

    // Backdrop URL and dynamic gradient colors
    val backdropUrl = remember(collection?.id) {
        collection?.let {
            jellyfinClient.getBackdropUrl(it.id, it.backdropImageTags?.firstOrNull())
        }
    }
    val gradientColors = rememberGradientColors(backdropUrl)

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
    if (error != null || collection == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = error ?: "Collection not found",
                color = TvColors.Error,
            )
        }
        return
    }

    val collectionItem = collection!!
    val favorite = collectionItem.userData?.isFavorite == true

    // Layered UI: DynamicBackground → DetailBackdropLayer → Content
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Backdrop image (right side, behind content)
        DetailBackdropLayer(
            item = collectionItem,
            jellyfinClient = jellyfinClient,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .align(Alignment.TopEnd),
        )

        // Layer 3: Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed hero section (50% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                // Hero content (left 50%) - title, overview, item count
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = 48.dp, top = 48.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    CollectionHeroContent(
                        collection = collectionItem,
                        itemCount = items.size,
                    )
                }

                // Action buttons fixed at bottom of hero section
                CollectionActionButtons(
                    favorite = favorite,
                    onShuffleClick = {
                        position = HEADER_ROW
                        if (items.isNotEmpty()) {
                            val randomItem = items.random()
                            onPlayClick(randomItem.id, null)
                        }
                    },
                    onFavoriteClick = {
                        // TODO: Toggle favorite
                    },
                    buttonOnFocusChanged = {
                        if (it.isFocused) {
                            scope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    shuffleFocusRequester = shuffleFocusRequester,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 48.dp, bottom = 8.dp)
                        .focusRequester(focusRequesters[HEADER_ROW])
                        .focusProperties {
                            down = focusRequesters[ITEMS_ROW]
                            up = navBarFocusRequester
                        }
                        .focusRestorer(shuffleFocusRequester)
                        .focusGroup(),
                )
            }

            // Scrollable grid content (below fixed hero) - 7 columns wrapping
            if (items.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequesters[ITEMS_ROW]),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        MediaCard(
                            item = item,
                            imageUrl = jellyfinClient.getPrimaryImageUrl(
                                item.id,
                                item.imageTags?.primary,
                            ),
                            onClick = {
                                position = ITEMS_ROW
                                onItemClick(item.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No items in this collection",
                        color = TvColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        // Top Navigation Bar (always visible)
        TopNavigationBarPopup(
            selectedItem = NavItem.COLLECTIONS,
            onItemSelected = onNavigate,
            showUniverses = showUniversesInNav,
            contentFocusRequester = shuffleFocusRequester,
            focusRequester = navBarFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * Collection hero content showing title, overview, and item count.
 */
@Composable
private fun CollectionHeroContent(
    collection: JellyfinItem,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Title
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
        Text(
            text = "$itemCount items",
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Overview/Description
        collection.overview?.let { overview ->
            Text(
                text = overview,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
        }
    }
}

/**
 * Action buttons for collection detail screen.
 */
@Composable
private fun CollectionActionButtons(
    favorite: Boolean,
    onShuffleClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    buttonOnFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    shuffleFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // Shuffle Play button
        Button(
            onClick = onShuffleClick,
            modifier = Modifier
                .height(20.dp)
                .focusRequester(shuffleFocusRequester),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(scale = 1f, focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                contentColor = TvColors.TextPrimary,
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Shuffle", style = MaterialTheme.typography.labelSmall)
        }

        // Favorite button
        Button(
            onClick = onFavoriteClick,
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(scale = 1f, focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                contentColor = if (favorite) TvColors.Error else TvColors.TextPrimary,
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
