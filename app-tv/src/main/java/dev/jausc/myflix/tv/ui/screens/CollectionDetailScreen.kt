@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.detail.CollectionActionButtons
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsBackdrop
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsFadePreset
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs


/**
 * Collection detail screen with Ken Burns animated backdrop.
 *
 * Features:
 * - Ken Burns animated backdrop synced with focused item
 * - Master-detail pattern: focused item updates hero content
 * - Collection items displayed in a horizontal row
 */
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onPlayClick: (String, Long?) -> Unit,
    onBack: () -> Unit,
) {
    // State
    var collection by remember { mutableStateOf<JellyfinItem?>(null) }
    var items by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Focus management
    val shuffleFocusRequester = remember { FocusRequester() }
    val itemsRowFocusRequester = remember { FocusRequester() }
    val updateExitFocus = rememberExitFocusRegistry(shuffleFocusRequester)

    // Track focused item for master-detail view
    var focusedItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Load collection data
    LaunchedEffect(collectionId) {
        isLoading = true
        error = null

        // Load collection details
        jellyfinClient.getItem(collectionId)
            .onSuccess { item ->
                collection = item

                // Load collection items (respects server Display Order - Timeline Order for Universe Collections)
                jellyfinClient.getCollectionItems(collectionId, limit = 100, sortBy = null)
                    .onSuccess { result ->
                        items = result
                    }
                    .onFailure {
                        if (error == null) {
                            error = it.message ?: "Failed to load collection items"
                        }
                    }
            }
            .onFailure {
                error = it.message ?: "Failed to load collection"
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

    // Determine which item to display in the hero/backdrop
    val displayItem = focusedItem ?: collection

    // Backdrop URL - use focused item's backdrop on Items tab, collection's on other tabs
    val backdropUrl = remember(displayItem?.id, displayItem?.backdropImageTags) {
        displayItem?.let { item ->
            val hasBackdrop = !item.backdropImageTags.isNullOrEmpty()
            if (hasBackdrop) {
                jellyfinClient.getBackdropUrl(item.id, item.backdropImageTags?.firstOrNull())
            } else {
                // Fall back to primary image
                item.imageTags?.primary?.let { primary ->
                    jellyfinClient.getPrimaryImageUrl(item.id, primary)
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
    // Check if all items in collection are watched
    val allWatched = items.isNotEmpty() && items.all { it.userData?.played == true }

    // Main layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .focusGroup(),
    ) {
        // Layer 1: Ken Burns animated backdrop (top-right)
        backdropUrl?.let { url ->
            KenBurnsBackdrop(
                imageUrl = url,
                fadePreset = KenBurnsFadePreset.MOVIE,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.9f)
                    .align(Alignment.TopEnd),
            )
        }

        // Layer 2: Content column with hero at top, tabs at bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            // Hero content (left side) - shows collection or focused item info
            CollectionHeroContent(
                collection = collectionItem,
                focusedItem = focusedItem,
                itemCount = items.size,
                watched = allWatched,
                favorite = favorite,
                onShuffleClick = {
                    if (items.isNotEmpty()) {
                        val randomItem = items.random()
                        onPlayClick(randomItem.id, null)
                    }
                },
                onWatchedClick = {
                    // TODO: Mark all items as watched/unwatched
                },
                onFavoriteClick = {
                    // TODO: Toggle favorite
                },
                shuffleFocusRequester = shuffleFocusRequester,
                itemsRowFocusRequester = itemsRowFocusRequester,
                updateExitFocus = updateExitFocus,
                onButtonFocusChanged = {
                    if (it.isFocused) {
                        focusedItem = null // Revert to collection info when buttons focused
                    }
                },
                modifier = Modifier.fillMaxWidth(0.5f).weight(0.6f),
            )

            // Collection items grid - one row of 7 visible, snaps between rows
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .clipToBounds()
                    .focusGroup()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.9f),
                            ),
                        ),
                    )
                    .padding(top = 12.dp),
            ) {
                CollectionItemsGrid(
                    items = items,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    onItemFocused = { item -> focusedItem = item },
                    gridFocusRequester = itemsRowFocusRequester,
                    upFocusRequester = shuffleFocusRequester,
                )
            }
        }
    }
}

/**
 * Hero content showing collection title, metadata, overview, and action buttons.
 * Updates to show focused item info when an item is focused (master-detail pattern).
 */
@Composable
private fun CollectionHeroContent(
    collection: JellyfinItem,
    focusedItem: JellyfinItem?,
    itemCount: Int,
    watched: Boolean,
    favorite: Boolean,
    onShuffleClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    shuffleFocusRequester: FocusRequester,
    itemsRowFocusRequester: FocusRequester,
    updateExitFocus: (FocusRequester) -> Unit,
    onButtonFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Display focused item info or collection info
    val displayItem = focusedItem ?: collection
    val isShowingFocusedItem = focusedItem != null

    Column(
        modifier = modifier.padding(start = 10.dp),
    ) {
        // Title
        Text(
            text = displayItem.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata row - different for collection vs focused item
        if (isShowingFocusedItem) {
            // Focused item: Year • Rating • Runtime
            ItemMetadataRow(item = focusedItem)
        } else {
            // Collection: Item count • Genres (if available)
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Overview - fills available space, scrolls up if overflowing
        displayItem.overview?.let { overview ->
            AutoScrollingText(
                text = overview,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .weight(1f),
            )
        } ?: Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        CollectionActionButtons(
            watched = watched,
            favorite = favorite,
            onShuffleClick = onShuffleClick,
            onWatchedClick = onWatchedClick,
            onFavoriteClick = onFavoriteClick,
            buttonOnFocusChanged = { focusState ->
                onButtonFocusChanged(focusState)
                if (focusState.isFocused) {
                    updateExitFocus(shuffleFocusRequester)
                }
            },
            shuffleFocusRequester = shuffleFocusRequester,
            downFocusRequester = itemsRowFocusRequester,
        )
    }
}

/**
 * Metadata row for a focused item (Year • Rating • Runtime).
 */
@Composable
private fun ItemMetadataRow(item: JellyfinItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
            )
        }

        item.officialRating?.let { rating ->
            Box(
                modifier = Modifier
                    .background(
                        TvColors.SurfaceElevated.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = rating,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TvColors.TextPrimary,
                )
            }
        }

        item.communityRating?.let { rating ->
            Text(
                text = "★ ${"%.1f".format(rating)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD700),
            )
        }
    }
}

private const val GRID_COLUMNS = 7

/**
 * Collection items displayed in a grid, 7 across, one row visible at a time with snap scrolling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionItemsGrid(
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onItemFocused: (JellyfinItem) -> Unit,
    gridFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No items in this collection",
                color = TvColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    // Chunk items into rows of 7
    val rows = remember(items) { items.chunked(GRID_COLUMNS) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var lastFocusedRow by remember { mutableIntStateOf(-1) }

    // No-op BringIntoViewSpec to disable auto-scroll, we handle it manually
    val noOpBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float = 0f
        }
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides noOpBringIntoViewSpec) {
        LazyColumn(
            state = listState,
            modifier = modifier
                .focusRequester(gridFocusRequester)
                .clipToBounds(),
        ) {
            itemsIndexed(rows, key = { index, _ -> index }) { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .onFocusChanged { state ->
                            if (state.hasFocus && rowIndex != lastFocusedRow) {
                                lastFocusedRow = rowIndex
                                val info = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == rowIndex }
                                if (info != null && abs(info.offset) <= 2) return@onFocusChanged
                                coroutineScope.launch {
                                    listState.scrollToItem(rowIndex, 0)
                                }
                            }
                        }
                        .focusProperties {
                            if (rowIndex == 0) up = upFocusRequester
                        },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { item ->
                        CompactPosterCard(
                            item = item,
                            imageUrl = jellyfinClient.getPrimaryImageUrl(
                                item.id,
                                item.imageTags?.primary,
                            ),
                            onClick = { onItemClick(item.id) },
                            onFocused = { onItemFocused(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Fill remaining slots in the last row so cards stay same width
                    repeat(GRID_COLUMNS - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private const val POSTER_ASPECT_RATIO = 2f / 3f

/**
 * Compact poster card for collection items.
 * Uses 2:3 aspect ratio, sized by parent weight.
 */
@Composable
private fun CompactPosterCard(
    item: JellyfinItem,
    imageUrl: String?,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Poster with focus border
        androidx.tv.material3.Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier.onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(POSTER_ASPECT_RATIO)
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (isFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = TvColors.BluePrimary,
                                    shape = RoundedCornerShape(6.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }

        // Title only - single line with ellipsis
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

/**
 * Text that fills available space and auto-scrolls up once if content overflows.
 */
@Composable
private fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    scrollDuration: Int = 12000,
) {
    val scrollState = rememberScrollState()

    // Scroll to bottom once when content overflows
    LaunchedEffect(text, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            delay(3000)
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(durationMillis = scrollDuration, easing = LinearEasing),
            )
        }
    }

    Box(
        modifier = modifier.clipToBounds(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary.copy(alpha = 0.9f),
            lineHeight = 18.sp,
            modifier = Modifier.verticalScroll(scrollState),
        )
    }
}
