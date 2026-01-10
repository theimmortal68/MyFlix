package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.HeroContentBuilder
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.HeroBackdropLayer
import dev.jausc.myflix.tv.ui.components.HeroSection
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBar
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Background polling interval in milliseconds */
private const val POLL_INTERVAL_MS = 30_000L

/**
 * Position in the grid: row index + column index within that row.
 * Following Wholphin's RowColumn pattern for stable scroll behavior.
 */
private data class RowColumn(val row: Int, val column: Int)

/**
 * Home screen with left navigation rail and content rows.
 */
@Composable
fun HomeScreen(
    jellyfinClient: JellyfinClient,
    hideWatchedFromRecent: Boolean = false,
    onLibraryClick: (String, String) -> Unit,
    onItemClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // Content state
    var nextUp by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentMovies by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentShows by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentEpisodes by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var featuredItems by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Refresh trigger - increment to force reload
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Navigation state
    var selectedNavItem by remember { mutableStateOf(NavItem.HOME) }
    
    /**
     * Load or refresh all home screen content.
     */
    suspend fun loadContent(showLoading: Boolean = false) {
        if (showLoading) isLoading = true
        errorMessage = null

        // Clear cache to get fresh data
        jellyfinClient.clearCache()

        // Get libraries first to identify Movies and TV Shows libraries
        jellyfinClient.getLibraries()
            .onSuccess { libs ->
                libraries = libs

                // Find libraries using shared finder
                val moviesLibrary = LibraryFinder.findMoviesLibrary(libs)
                val showsLibrary = LibraryFinder.findShowsLibrary(libs)

                // Get latest movies (excludes collections)
                moviesLibrary?.let { lib ->
                    jellyfinClient.getLatestMovies(lib.id, limit = 12).onSuccess { items ->
                        recentMovies = items
                    }
                }

                // Get latest series (new shows added, not episodes)
                showsLibrary?.let { lib ->
                    jellyfinClient.getLatestSeries(lib.id, limit = 12).onSuccess { items ->
                        recentShows = items
                    }
                }

                // Get latest episodes
                showsLibrary?.let { lib ->
                    jellyfinClient.getLatestEpisodes(lib.id, limit = 12).onSuccess { items ->
                        recentEpisodes = items
                    }
                }
            }
            .onFailure { e ->
                errorMessage = "Failed to load libraries: ${e.message ?: "Unknown error"}"
            }

        // Get Next Up (episodes in series user is watching)
        jellyfinClient.getNextUp(limit = 12)
            .onSuccess { items -> nextUp = items }
            .onFailure { /* Non-critical, continue */ }

        // Get Continue Watching (in-progress items)
        jellyfinClient.getContinueWatching(limit = 12)
            .onSuccess { items -> continueWatching = items }
            .onFailure { /* Non-critical, continue */ }

        // Build featured items for hero section using shared logic
        featuredItems = HeroContentBuilder.buildFeaturedItems(
            continueWatching = continueWatching,
            nextUp = nextUp,
            recentMovies = recentMovies,
            recentShows = recentShows,
            config = HeroContentBuilder.defaultConfig
        )
        isLoading = false
    }
    
    // Initial load and refresh on screen revisit
    LaunchedEffect(refreshTrigger) {
        loadContent(showLoading = refreshTrigger == 0)
    }
    
    // Refresh when screen becomes active
    LaunchedEffect(Unit) {
        refreshTrigger++
    }
    
    // Background polling for updates
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(POLL_INTERVAL_MS)
            loadContent(showLoading = false)
        }
    }
    
    // Handle nav item selection
    val handleNavSelection: (NavItem) -> Unit = { item ->
        selectedNavItem = item
        when (item) {
            NavItem.HOME -> { /* Already on home */ }
            NavItem.SEARCH -> onSearchClick()
            NavItem.MOVIES -> {
                LibraryFinder.findMoviesLibrary(libraries)?.let { onLibraryClick(it.id, it.name) }
            }
            NavItem.SHOWS -> {
                LibraryFinder.findShowsLibrary(libraries)?.let { onLibraryClick(it.id, it.name) }
            }
            NavItem.COLLECTIONS -> { /* TODO: Navigate to collections */ }
            NavItem.UNIVERSES -> { /* TODO: Placeholder for future feature */ }
            NavItem.SETTINGS -> onSettingsClick()
        }
    }
    
    // Focus requesters
    val heroPlayFocusRequester = remember { FocusRequester() }
    val firstRowFocusRequester = remember { FocusRequester() }
    val topNavFocusRequester = remember { FocusRequester() }
    val homeButtonFocusRequester = remember { FocusRequester() }
    
    // Track current backdrop URL for dynamic background colors
    var currentBackdropUrl by remember { mutableStateOf<String?>(null) }
    
    // Extract gradient colors from current backdrop image
    val gradientColors = rememberGradientColors(currentBackdropUrl)
    
    // Track if we've focused the hero
    val contentId = featuredItems.firstOrNull()?.id
    var heroFocused by remember(contentId) { mutableStateOf(false) }
    
    // Content is ready when we have featured items
    val contentReady = !isLoading && featuredItems.isNotEmpty()
    
    // Request focus on hero when content becomes ready
    LaunchedEffect(contentReady) {
        if (contentReady && !heroFocused) {
            delay(100)
            try {
                heroPlayFocusRequester.requestFocus()
                heroFocused = true
            } catch (_: Exception) {
                delay(200)
                try {
                    heroPlayFocusRequester.requestFocus()
                    heroFocused = true
                } catch (_: Exception) { }
            }
        }
    }
    
    // Use Box to layer DynamicBackground behind everything
    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic gradient background (behind everything)
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize()
        )
        
        // Main Content (full screen, nav bar overlays on top)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Show loading until we have hero content
            if (!contentReady && errorMessage == null) {
                TvLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null && featuredItems.isEmpty()) {
                // Error state with retry
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Text(
                        text = errorMessage ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.Error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch { loadContent(showLoading = true) }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                // Key forces complete recomposition when content changes
                key(featuredItems.firstOrNull()?.id) {
                    HomeContent(
                        featuredItems = featuredItems,
                        nextUp = nextUp,
                        continueWatching = continueWatching,
                        recentEpisodes = recentEpisodes,
                        recentShows = recentShows,
                        recentMovies = recentMovies,
                        jellyfinClient = jellyfinClient,
                        onItemClick = onItemClick,
                        hideWatchedFromRecent = hideWatchedFromRecent,
                        heroPlayFocusRequester = heroPlayFocusRequester,
                        firstRowFocusRequester = firstRowFocusRequester,
                        topNavFocusRequester = topNavFocusRequester,
                        homeButtonFocusRequester = homeButtonFocusRequester,
                        onBackdropUrlChanged = { url -> currentBackdropUrl = url },
                        onItemFocused = { },
                        // Top navigation config
                        selectedNavItem = selectedNavItem,
                        onNavItemSelected = handleNavSelection
                    )
                }
            }
        }
    }
}

/**
 * Row data for building the content list
 */
private data class RowData(
    val key: String,
    val title: String,
    val items: List<JellyfinItem>,
    val isWideCard: Boolean,
    val accentColor: Color
)

/**
 * Main home content with hero section and scrollable rows.
 * Uses Wholphin's exact pattern for stable scroll behavior.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun HomeContent(
    featuredItems: List<JellyfinItem>,
    nextUp: List<JellyfinItem>,
    continueWatching: List<JellyfinItem>,
    recentEpisodes: List<JellyfinItem>,
    recentShows: List<JellyfinItem>,
    recentMovies: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    hideWatchedFromRecent: Boolean = false,
    heroPlayFocusRequester: FocusRequester = remember { FocusRequester() },
    firstRowFocusRequester: FocusRequester = remember { FocusRequester() },
    topNavFocusRequester: FocusRequester = remember { FocusRequester() },
    homeButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    onBackdropUrlChanged: (String?) -> Unit = {},
    onItemFocused: (JellyfinItem?) -> Unit = {},
    // Top navigation
    selectedNavItem: NavItem = NavItem.HOME,
    onNavItemSelected: (NavItem) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Filter watched items from Recently Added rows
    val filteredRecentEpisodes = if (hideWatchedFromRecent) {
        recentEpisodes.filter { it.userData?.played != true }
    } else recentEpisodes
    
    val filteredRecentShows = if (hideWatchedFromRecent) {
        recentShows.filter { it.userData?.played != true }
    } else recentShows
    
    val filteredRecentMovies = if (hideWatchedFromRecent) {
        recentMovies.filter { it.userData?.played != true }
    } else recentMovies
    
    val filteredFeaturedItems = if (hideWatchedFromRecent) {
        featuredItems.filter { it.userData?.played != true }
    } else featuredItems
    
    // Get IDs of items in Continue Watching to filter from Next Up
    val continueWatchingIds = remember(continueWatching) {
        continueWatching.map { it.id }.toSet()
    }
    
    // Filter Next Up to exclude items already in Continue Watching
    val filteredNextUp = remember(nextUp, continueWatchingIds) {
        nextUp.filter { it.id !in continueWatchingIds }
    }
    
    // Build list of row data - Continue Watching first, then Next Up
    val rows = remember(filteredNextUp, continueWatching, filteredRecentEpisodes, filteredRecentShows, filteredRecentMovies) {
        buildList {
            if (continueWatching.isNotEmpty()) add(RowData("continue", "Continue Watching", continueWatching, false, TvColors.BluePrimary))
            if (filteredNextUp.isNotEmpty()) add(RowData("next_up", "Next Up", filteredNextUp, false, TvColors.BlueAccent))
            if (filteredRecentEpisodes.isNotEmpty()) add(RowData("recent_ep", "Recently Added Episodes", filteredRecentEpisodes, false, TvColors.Success))
            if (filteredRecentShows.isNotEmpty()) add(RowData("recent_shows", "Recently Added Shows", filteredRecentShows, false, Color(0xFFFBBF24)))
            if (filteredRecentMovies.isNotEmpty()) add(RowData("recent_movies", "Recently Added Movies", filteredRecentMovies, false, TvColors.BluePrimary))
        }
    }
    
    // Position tracking (row and column)
    var position by remember { mutableStateOf(RowColumn(0, 0)) }
    
    // Currently focused/previewed item from content rows
    var previewItem by remember { mutableStateOf<JellyfinItem?>(null) }
    
    // Notify parent when preview item changes
    LaunchedEffect(previewItem) {
        onItemFocused(previewItem)
    }
    
    // LazyColumn state and FocusRequesters for each row (exact Wholphin pattern)
    val listState = rememberLazyListState()
    val rowFocusRequesters = remember(rows.size) { List(rows.size) { FocusRequester() } }
    
    // Track if first focus has happened
    var firstFocused by remember { mutableStateOf(false) }
    
    // Function to clear preview and scroll back to top
    val clearPreviewAndScrollToTop: () -> Unit = {
        previewItem = null
        coroutineScope.launch {
            listState.animateScrollToItem(0)
        }
    }
    
    // Initial focus when rows first load (exact Wholphin pattern)
    LaunchedEffect(rows) {
        if (!firstFocused && rows.isNotEmpty()) {
            val index = rows.indexOfFirst { it.items.isNotEmpty() }.coerceAtLeast(0)
            try {
                rowFocusRequesters.getOrNull(index)?.requestFocus()
            } catch (_: Exception) { }
            delay(50)
            listState.scrollToItem(index)
            firstFocused = true
        }
    }
    
    // KEY PATTERN: Restore focus and scroll on EVERY recomposition (e.g., returning from nav bar)
    // This runs BEFORE Compose's automatic bring-into-view can cause the shift
    LaunchedEffect(Unit) {
        if (firstFocused && rows.isNotEmpty()) {
            val index = position.row.coerceIn(0, rows.size - 1)
            try {
                rowFocusRequesters.getOrNull(index)?.requestFocus()
            } catch (_: Exception) { }
            delay(50)
            listState.scrollToItem(index)
        }
    }
    
    // Animate scroll when position changes (exact Wholphin pattern)
    LaunchedEffect(position) {
        if (position.row >= 0 && position.row < rows.size) {
            listState.animateScrollToItem(position.row)
        }
    }
    
    // Track the current hero display item for the backdrop layer
    var heroDisplayItem by remember { mutableStateOf<JellyfinItem?>(null) }
    
    // Layered UI architecture:
    // Layer 1 (back): Hero backdrop - 90% of screen with edge fading
    // Layer 2 (middle): Hero info (37%) + Content rows (63%)
    // Layer 3 (front): Top navigation bar with gradient
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Hero backdrop image (90% of screen, fades at edges)
        HeroBackdropLayer(
            item = previewItem ?: heroDisplayItem,
            jellyfinClient = jellyfinClient,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .align(Alignment.TopEnd)
        )
        
        // Layer 2: Hero info + Content rows
        Column(modifier = Modifier.fillMaxSize()) {
            // Hero Section - media info only (backdrop is in layer 1)
            if (filteredFeaturedItems.isNotEmpty() || previewItem != null) {
                HeroSection(
                    featuredItems = filteredFeaturedItems,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    onPlayClick = onItemClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.37f),
                    previewItem = previewItem,
                    playButtonFocusRequester = heroPlayFocusRequester,
                    downFocusRequester = firstRowFocusRequester,
                    upFocusRequester = homeButtonFocusRequester,
                    onCurrentItemChanged = { item, backdropUrl ->
                        heroDisplayItem = item
                        onBackdropUrlChanged(backdropUrl)
                    },
                    onPreviewClear = clearPreviewAndScrollToTop
                )
            }
            
            // Content rows with focusRestorer
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 300.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRestorer()
                    .onPreviewKeyEvent { keyEvent ->
                        // Intercept UP on first row to go to hero
                        if (keyEvent.type == KeyEventType.KeyDown && 
                            keyEvent.key == Key.DirectionUp &&
                            position.row == 0) {
                            previewItem = null
                            coroutineScope.launch {
                                listState.scrollToItem(0)
                                try {
                                    heroPlayFocusRequester.requestFocus()
                                } catch (_: Exception) { }
                            }
                            true
                        } else {
                            false
                        }
                    }
                    .focusProperties {
                        up = heroPlayFocusRequester
                    }
            ) {
            itemsIndexed(rows, key = { _, row -> row.key }) { rowIndex, rowData ->
                // Each row gets a focusRequester from the list (exact Wholphin pattern)
                ItemRow(
                    title = rowData.title,
                    items = rowData.items,
                    isWideCard = rowData.isWideCard,
                    accentColor = rowData.accentColor,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    onCardFocused = { cardIndex, item ->
                        previewItem = item
                        position = RowColumn(rowIndex, cardIndex)
                        // Mark as focused if this is the first card interaction
                        if (!firstFocused) firstFocused = true
                    },
                    firstCardFocusRequester = if (rowIndex == 0) firstRowFocusRequester else null,
                    upFocusRequester = if (rowIndex == 0) heroPlayFocusRequester else null, // First row UP -> hero
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rowFocusRequesters[rowIndex])
                        .animateItem()
                )
            }
        }
        } // End Column
        
        // Layer 3: Top Navigation Bar (with gradient overlay)
        TopNavigationBar(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected,
            firstItemFocusRequester = topNavFocusRequester,
            homeButtonFocusRequester = homeButtonFocusRequester,
            downFocusRequester = heroPlayFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Single row containing header + LazyRow of cards.
 * Uses focusGroup to make LazyColumn treat this as a single focus unit.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ItemRow(
    title: String,
    items: List<JellyfinItem>,
    isWideCard: Boolean,
    accentColor: Color,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onCardFocused: (Int, JellyfinItem) -> Unit,
    firstCardFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester? = null, // For first row: UP goes to hero
    modifier: Modifier = Modifier
) {
    val lazyRowState = rememberLazyListState()
    // Single FocusRequester for focus restoration (exact Wholphin pattern)
    val firstFocus = remember { FocusRequester() }
    
    // focusGroup makes LazyColumn see this row as ONE focus target
    // This prevents bring-into-view from scrolling to individual cards
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .padding(vertical = 8.dp)
            .focusGroup()
    ) {
        // Row header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 10.dp, end = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(accentColor, shape = MaterialTheme.shapes.small)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary
            )
        }
        
        // LazyRow with focusRestorer (exact Wholphin pattern)
        LazyRow(
            state = lazyRowState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer(firstFocus)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                // First card gets focusRequester (exact Wholphin pattern)
                val cardModifier = if (index == 0) {
                    Modifier
                        .focusRequester(firstFocus)
                        .then(
                            if (firstCardFocusRequester != null) {
                                Modifier.focusRequester(firstCardFocusRequester)
                            } else Modifier
                        )
                } else {
                    Modifier
                }
                
                // Apply upFocusRequester to ALL cards in this row (for first row -> hero)
                val focusPropertiesModifier = if (upFocusRequester != null) {
                    cardModifier.focusProperties { up = upFocusRequester }
                } else {
                    cardModifier
                }
                
                val focusModifier = focusPropertiesModifier.onFocusChanged { state ->
                    if (state.isFocused) {
                        onCardFocused(index, item)
                    }
                }
                
                if (isWideCard) {
                    val imageUrl = when {
                        item.type == "Episode" -> {
                            jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary, maxWidth = 600)
                        }
                        !item.backdropImageTags.isNullOrEmpty() -> {
                            jellyfinClient.getBackdropUrl(item.id, item.backdropImageTags?.firstOrNull(), maxWidth = 600)
                        }
                        else -> {
                            jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary, maxWidth = 600)
                        }
                    }
                    WideMediaCard(
                        item = item,
                        imageUrl = imageUrl,
                        onClick = { onItemClick(item.id) },
                        showLabel = false,
                        modifier = focusModifier
                    )
                } else {
                    // For portrait cards: use series poster for episodes, otherwise use item poster
                    val imageUrl = if (item.type == "Episode" && item.seriesId != null) {
                        // Use series poster for episodes in portrait view
                        jellyfinClient.getPrimaryImageUrl(item.seriesId!!, null)
                    } else {
                        jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary)
                    }
                    MediaCard(
                        item = item,
                        imageUrl = imageUrl,
                        onClick = { onItemClick(item.id) },
                        showLabel = false,
                        modifier = focusModifier
                    )
                }
            }
        }
    }
}
