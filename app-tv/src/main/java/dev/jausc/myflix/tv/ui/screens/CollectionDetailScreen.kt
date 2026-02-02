@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import dev.jausc.myflix.tv.ui.components.detail.TvTabRow
import dev.jausc.myflix.tv.ui.components.detail.TvTabRowFocusConfig
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.delay


/**
 * Tab options for the collection detail screen.
 */
private enum class CollectionTab {
    Items,
    Details,
    Similar,
}

/**
 * Collection detail screen with Ken Burns animated backdrop and tab-based content.
 * Matches the design patterns from MovieDetailScreen, UnifiedSeriesScreen, and EpisodesScreen.
 *
 * Features:
 * - Ken Burns animated backdrop synced with focused item (on Items tab)
 * - Tab-based content: Items, Details, Similar
 * - Master-detail pattern: focused item updates hero content on Items tab
 * - Hybrid display: horizontal row for small collections (≤15), grid for large (>15)
 */
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onPlayClick: (String, Long?) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // State
    var collection by remember { mutableStateOf<JellyfinItem?>(null) }
    var items by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var similarCollections by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Focus management
    val shuffleFocusRequester = remember { FocusRequester() }
    val updateExitFocus = rememberExitFocusRegistry(shuffleFocusRequester)

    // Tab state
    var selectedTab by rememberSaveable { mutableStateOf(CollectionTab.Items) }

    // Stable focus requesters for each tab
    val tabFocusRequesters = remember { mutableStateMapOf<CollectionTab, FocusRequester>() }
    fun getTabFocusRequester(tab: CollectionTab): FocusRequester =
        tabFocusRequesters.getOrPut(tab) { FocusRequester() }

    // Track which tab should receive focus when navigating up from content
    var lastFocusedTab by remember { mutableStateOf(CollectionTab.Items) }

    // Track focused item in the grid for Master-Detail view (only used on Items tab)
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

                // Load similar collections
                jellyfinClient.getSimilarItems(collectionId, limit = 20)
                    .onSuccess { result ->
                        similarCollections = result
                    }
                    .onFailure {
                        // Silently fail - Similar tab will just be empty
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

    // Filter tabs based on available data
    val availableTabs = remember(similarCollections) {
        CollectionTab.entries.filter { tab ->
            when (tab) {
                CollectionTab.Items -> true
                CollectionTab.Details -> true
                CollectionTab.Similar -> similarCollections.isNotEmpty()
            }
        }
    }

    // Handle tab selection when tab becomes unavailable
    LaunchedEffect(availableTabs) {
        if (selectedTab !in availableTabs) {
            selectedTab = availableTabs.firstOrNull() ?: CollectionTab.Items
        }
    }

    // Determine which item to display in the hero/backdrop
    // On Items tab: focused item (master-detail) OR collection
    // On other tabs: always collection (static)
    val displayItem = when (selectedTab) {
        CollectionTab.Items -> focusedItem ?: collection
        else -> collection
    }

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
                focusedItem = if (selectedTab == CollectionTab.Items) focusedItem else null,
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
                firstTabFocusRequester = getTabFocusRequester(selectedTab),
                updateExitFocus = updateExitFocus,
                onButtonFocusChanged = {
                    if (it.isFocused) {
                        focusedItem = null // Revert to collection info when buttons focused
                    }
                },
                modifier = Modifier.fillMaxWidth(0.5f),
            )

            // Spacer pushes tabs to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Tab section with shaded background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 10.dp)
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
                Column {
                    // Tab row
                    TvTabRow(
                        tabs = availableTabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        tabLabel = { tab ->
                            when (tab) {
                                CollectionTab.Items -> "Items"
                                CollectionTab.Details -> "Details"
                                CollectionTab.Similar -> "Similar"
                            }
                        },
                        getTabFocusRequester = ::getTabFocusRequester,
                        onTabFocused = { tab, requester ->
                            lastFocusedTab = tab
                            updateExitFocus(requester)
                        },
                        focusConfig = TvTabRowFocusConfig(
                            upFocusRequester = shuffleFocusRequester,
                        ),
                    )

                    // Tab content area - 210dp height accommodates larger posters (174dp + title)
                    val selectedTabRequester = getTabFocusRequester(lastFocusedTab)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(start = 2.dp)
                            .focusProperties {
                                up = selectedTabRequester
                            },
                    ) {
                        when (selectedTab) {
                            CollectionTab.Items -> {
                                CollectionItemsTabContent(
                                    items = items,
                                    jellyfinClient = jellyfinClient,
                                    onItemClick = onItemClick,
                                    onItemFocused = { item -> focusedItem = item },
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            CollectionTab.Details -> {
                                CollectionDetailsTabContent(
                                    collection = collectionItem,
                                    itemCount = items.size,
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            CollectionTab.Similar -> {
                                SimilarCollectionsTabContent(
                                    collections = similarCollections,
                                    jellyfinClient = jellyfinClient,
                                    onCollectionClick = onItemClick,
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                        }
                    }
                }
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
    firstTabFocusRequester: FocusRequester,
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
            ItemMetadataRow(item = focusedItem!!)
        } else {
            // Collection: Item count • Genres (if available)
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Overview - dynamically sized with auto-scroll for long descriptions
        displayItem.overview?.let { overview ->
            AutoScrollingText(
                text = overview,
                modifier = Modifier.fillMaxWidth(0.9f),
            )
        }

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
            downFocusRequester = firstTabFocusRequester,
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

/**
 * Items tab content - displays collection items in a horizontal row.
 */
@Composable
private fun CollectionItemsTabContent(
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onItemFocused: (JellyfinItem) -> Unit,
    tabFocusRequester: FocusRequester?,
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

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
            CompactPosterCard(
                item = item,
                imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                onClick = { onItemClick(item.id) },
                onFocused = { onItemFocused(item) },
                tabFocusRequester = tabFocusRequester,
            )
        }
    }
}

/**
 * Details tab content - collection overview and metadata.
 */
@Composable
private fun CollectionDetailsTabContent(
    collection: JellyfinItem,
    itemCount: Int,
    tabFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        // Column 1: Overview
        item("overview") {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester },
            ) {
                DetailLabel("Overview")
                Text(
                    text = collection.overview ?: "No description available",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
            }
        }

        // Column 2: Collection info
        item("info") {
            Column(
                modifier = Modifier.width(150.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailItem("Items", "$itemCount")
                collection.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                    DetailItem("Genres", genres.joinToString(", "))
                }
                collection.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                    DetailItem("Tags", tags.take(5).joinToString(", "))
                }
            }
        }
    }
}

/**
 * Similar collections tab content.
 */
@Composable
private fun SimilarCollectionsTabContent(
    collections: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onCollectionClick: (String) -> Unit,
    tabFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    if (collections.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No similar collections found",
                color = TvColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(collections, key = { _, item -> item.id }) { _, collection ->
            CompactPosterCard(
                item = collection,
                imageUrl = jellyfinClient.getPrimaryImageUrl(collection.id, collection.imageTags?.primary),
                onClick = { onCollectionClick(collection.id) },
                onFocused = { },
                tabFocusRequester = tabFocusRequester,
            )
        }
    }
}

// Poster dimensions sized for 7-item grid display (2:3 aspect ratio)
private val POSTER_WIDTH = 116.dp
private val POSTER_HEIGHT = 174.dp

/**
 * Compact poster card for collection items.
 * Sized to fit 7 items in grid/row display (116×174dp, 2:3 aspect ratio).
 */
@Composable
private fun CompactPosterCard(
    item: JellyfinItem,
    imageUrl: String?,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    tabFocusRequester: FocusRequester?,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(POSTER_WIDTH)
            .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester },
    ) {
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
                    .width(POSTER_WIDTH)
                    .height(POSTER_HEIGHT)
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
 * Detail section label.
 */
@Composable
private fun DetailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TvColors.TextSecondary,
        fontSize = 10.sp,
    )
    Spacer(modifier = Modifier.height(4.dp))
}

/**
 * Detail item with label and value.
 */
@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TvColors.TextSecondary,
            fontSize = 10.sp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Text that dynamically expands to fit content, with auto-scroll when overflow is detected.
 * Expands up to maxHeight, then scrolls if content is taller.
 */
@Composable
private fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 120,
    scrollDuration: Int = 12000,
) {
    val scrollState = rememberScrollState()
    var needsScroll by remember { mutableStateOf(false) }
    var textHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val maxHeightPx = with(density) { maxHeight.dp.toPx() }

    // Auto-scroll animation when content overflows
    LaunchedEffect(needsScroll) {
        if (needsScroll && scrollState.maxValue > 0) {
            while (true) {
                // Pause at top
                delay(3000)
                // Scroll to bottom
                scrollState.animateScrollTo(
                    scrollState.maxValue,
                    animationSpec = tween(durationMillis = scrollDuration, easing = LinearEasing),
                )
                // Pause at bottom
                delay(2000)
                // Scroll back to top
                scrollState.animateScrollTo(
                    0,
                    animationSpec = tween(durationMillis = scrollDuration, easing = LinearEasing),
                )
            }
        }
    }

    // Dynamic height: wraps content up to maxHeight, then clips and scrolls
    val dynamicHeight = with(density) {
        if (textHeight > 0 && textHeight <= maxHeightPx.toInt()) {
            textHeight.toDp()
        } else {
            maxHeight.dp
        }
    }

    Box(
        modifier = modifier
            .height(dynamicHeight)
            .clipToBounds(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary.copy(alpha = 0.9f),
            lineHeight = 18.sp,
            modifier = Modifier
                .verticalScroll(scrollState)
                .onSizeChanged { size ->
                    textHeight = size.height
                    needsScroll = size.height > maxHeightPx
                },
        )
    }
}
