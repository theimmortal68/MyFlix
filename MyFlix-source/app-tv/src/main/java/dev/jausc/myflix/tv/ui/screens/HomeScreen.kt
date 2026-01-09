package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.HeroSection
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Background polling interval in milliseconds */
private const val POLL_INTERVAL_MS = 30_000L

/**
 * Home screen with left navigation rail and content rows.
 * 
 * Rows displayed:
 * - Next Up (episodes to continue in series)
 * - Continue Watching (in-progress movies/episodes)
 * - Recently Added Episodes
 * - Recently Added Shows (new series only, not episodes)
 * - Recently Added Movies
 * 
 * Features:
 * - Refreshes content on each screen load
 * - Polls for updates every 30 seconds in the background
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
    // Content state
    var nextUp by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentMovies by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentShows by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentEpisodes by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var featuredItems by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Refresh trigger - increment to force reload
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Navigation state
    var selectedNavItem by remember { mutableStateOf(NavItem.HOME) }
    
    /**
     * Load or refresh all home screen content.
     * Clears cache to ensure fresh data from server.
     */
    suspend fun loadContent(showLoading: Boolean = false) {
        if (showLoading) isLoading = true
        
        // Clear cache to get fresh data
        jellyfinClient.clearCache()
        
        // Get libraries first to identify Movies and TV Shows libraries
        jellyfinClient.getLibraries().onSuccess { libs ->
            libraries = libs
            
            // Find libraries - prefer collectionType, fall back to name matching
            val moviesLibrary = libs.find { 
                it.collectionType == "movies" 
            } ?: libs.find { 
                it.name.contains("movie", ignoreCase = true) ||
                it.name.contains("film", ignoreCase = true)
            }
            val showsLibrary = libs.find { 
                it.collectionType == "tvshows" 
            } ?: libs.find { 
                it.name.contains("show", ignoreCase = true) ||
                it.name.contains("series", ignoreCase = true) ||
                it.name.equals("tv", ignoreCase = true)
            }
            
            android.util.Log.d("HomeScreen", "Libraries: movies=${moviesLibrary?.name}, shows=${showsLibrary?.name}")
            
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
        
        // Get Next Up (episodes in series user is watching)
        jellyfinClient.getNextUp(limit = 12).onSuccess { items ->
            nextUp = items
        }
        
        // Get Continue Watching (in-progress items)
        jellyfinClient.getContinueWatching(limit = 12).onSuccess { items ->
            continueWatching = items
        }
        
        // Build featured items for hero section
        // Priority: Continue Watching > Next Up > Recent Movies > Recent Shows
        val featured = mutableListOf<JellyfinItem>()
        
        // Add items with backdrops from continue watching
        featured.addAll(
            continueWatching.filter { item ->
                !item.backdropImageTags.isNullOrEmpty() || 
                (item.isEpisode && item.seriesId != null)
            }.take(3)
        )
        
        // Add next up items
        featured.addAll(
            nextUp.filter { item -> 
                featured.none { it.id == item.id }
            }.take(2)
        )
        
        // Add recent movies with backdrops
        featured.addAll(
            recentMovies.filter { item ->
                !item.backdropImageTags.isNullOrEmpty() &&
                featured.none { it.id == item.id }
            }.take(3)
        )
        
        // Add recent shows with backdrops
        featured.addAll(
            recentShows.filter { item ->
                !item.backdropImageTags.isNullOrEmpty() &&
                featured.none { it.id == item.id }
            }.take(2)
        )
        
        featuredItems = featured.take(10)
        
        isLoading = false
    }
    
    // Initial load and refresh on screen revisit
    LaunchedEffect(refreshTrigger) {
        loadContent(showLoading = refreshTrigger == 0)
    }
    
    // Refresh when screen becomes active (navigating back)
    LaunchedEffect(Unit) {
        refreshTrigger++
    }
    
    // Background polling for updates
    LaunchedEffect(Unit) {
        while (true) {
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
                libraries.find { 
                    it.name.contains("movie", ignoreCase = true) ||
                    it.name.contains("film", ignoreCase = true)
                }?.let { onLibraryClick(it.id, it.name) }
            }
            NavItem.SHOWS -> {
                libraries.find { 
                    it.name.contains("show", ignoreCase = true) ||
                    it.name.contains("series", ignoreCase = true) ||
                    it.name.equals("tv", ignoreCase = true)
                }?.let { onLibraryClick(it.id, it.name) }
            }
            NavItem.SETTINGS -> onSettingsClick()
        }
    }
    
    // Focus requesters
    val heroPlayFocusRequester = remember { FocusRequester() }
    val firstRowFocusRequester = remember { FocusRequester() }
    
    // Track current backdrop URL for dynamic background colors
    var currentBackdropUrl by remember { mutableStateOf<String?>(null) }
    
    // Extract gradient colors from current backdrop image
    val gradientColors = rememberGradientColors(currentBackdropUrl)
    
    // Track if we've focused the hero - reset when content changes
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
        
        // Content Row (on top of gradient)
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left Navigation Rail
            NavigationRail(
                selectedItem = selectedNavItem,
                onItemSelected = handleNavSelection
            )
            
            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Show loading until we have hero content
                if (!contentReady) {
                    TvLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                            onBackdropUrlChanged = { url -> currentBackdropUrl = url }
                        )
                    }
                }
            }
        }
    }
}

@Composable
/**
 * Single home screen row model.
 *
 * Each row is rendered as a *single* LazyColumn item (header + LazyRow). This is critical for TV focus UX:
 * it prevents the parent LazyColumn from re-aligning between separate header/content items when focus moves
 * horizontally inside a row.
 */
private enum class HomeRowCardType { WIDE, POSTER }

private data class HomeRowModel(
    val key: String,
    val title: String,
    val accentColor: Color,
    val items: List<JellyfinItem>,
    val cardType: HomeRowCardType
)

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
    onBackdropUrlChanged: (String?) -> Unit = {},
    onItemFocused: (JellyfinItem?) -> Unit = {}
) {
    // Fresh list state - not saved/restored, always starts at top
    val listState = remember { LazyListState() }
    val coroutineScope = rememberCoroutineScope()

    // Ensure scroll starts at top
    LaunchedEffect(Unit) {
        listState.scrollToItem(0, 0)
    }

    // Track currently focused/previewed item from content rows
    var previewItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Notify parent when preview item changes
    LaunchedEffect(previewItem) {
        onItemFocused(previewItem)
    }

    // Function to clear preview and scroll back to top
    val clearPreviewAndScrollToTop: () -> Unit = {
        previewItem = null
        coroutineScope.launch {
            listState.animateScrollToItem(0)
        }
    }

    // Filter watched items from Recently Added rows if preference is enabled
    val filteredRecentEpisodes = if (hideWatchedFromRecent) {
        recentEpisodes.filter { it.userData?.played != true }
    } else recentEpisodes

    val filteredRecentShows = if (hideWatchedFromRecent) {
        recentShows.filter { it.userData?.played != true }
    } else recentShows

    val filteredRecentMovies = if (hideWatchedFromRecent) {
        recentMovies.filter { it.userData?.played != true }
    } else recentMovies

    // Filter featured items to exclude watched media if preference is enabled
    val filteredFeaturedItems = if (hideWatchedFromRecent) {
        featuredItems.filter { it.userData?.played != true }
    } else featuredItems

    // Track if rows have focus (to switch between carousel and preview mode)
    var rowsHaveFocus by remember { mutableStateOf(false) }

    // Build row models once per data change
    val homeRows = remember(
        nextUp,
        continueWatching,
        filteredRecentEpisodes,
        filteredRecentShows,
        filteredRecentMovies
    ) {
        buildList {
            if (nextUp.isNotEmpty()) {
                add(
                    HomeRowModel(
                        key = "next_up",
                        title = "Next Up",
                        accentColor = TvColors.BlueAccent,
                        items = nextUp,
                        cardType = HomeRowCardType.WIDE
                    )
                )
            }
            if (continueWatching.isNotEmpty()) {
                add(
                    HomeRowModel(
                        key = "continue_watching",
                        title = "Continue Watching",
                        accentColor = TvColors.BluePrimary,
                        items = continueWatching,
                        cardType = HomeRowCardType.WIDE
                    )
                )
            }
            if (filteredRecentEpisodes.isNotEmpty()) {
                add(
                    HomeRowModel(
                        key = "recent_episodes",
                        title = "Recently Added Episodes",
                        accentColor = TvColors.Success,
                        items = filteredRecentEpisodes,
                        cardType = HomeRowCardType.WIDE
                    )
                )
            }
            if (filteredRecentShows.isNotEmpty()) {
                add(
                    HomeRowModel(
                        key = "recent_shows",
                        title = "Recently Added Shows",
                        accentColor = Color(0xFFFBBF24),
                        items = filteredRecentShows,
                        cardType = HomeRowCardType.POSTER
                    )
                )
            }
            if (filteredRecentMovies.isNotEmpty()) {
                add(
                    HomeRowModel(
                        key = "recent_movies",
                        title = "Recently Added Movies",
                        accentColor = TvColors.BluePrimary,
                        items = filteredRecentMovies,
                        cardType = HomeRowCardType.POSTER
                    )
                )
            }
        }
    }

    // Column layout: Hero section (37%) + Rows section (63%)
    Column(modifier = Modifier.fillMaxSize()) {
        // Hero Section - Fixed 37% of screen height
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
                onCurrentItemChanged = { _, backdropUrl ->
                    onBackdropUrlChanged(backdropUrl)
                },
                onPreviewClear = clearPreviewAndScrollToTop
            )
        }

        // Track which row is currently focused (row index in LazyColumn)
        var focusedRowIndex by remember { mutableIntStateOf(0) }

        // Scroll ONLY when the focused row changes (vertical navigation)
        LaunchedEffect(focusedRowIndex) {
            if (focusedRowIndex in homeRows.indices) {
                listState.animateScrollToItem(focusedRowIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    // Only intercept UP on first row to go to hero
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.key == Key.DirectionUp &&
                        focusedRowIndex == 0
                    ) {
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
                .onFocusChanged { focusState ->
                    rowsHaveFocus = focusState.hasFocus
                },
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 300.dp)
        ) {
            itemsIndexed(homeRows, key = { _, row -> row.key }) { rowIndex, row ->
                HomeRowSection(
                    row = row,
                    rowIndex = rowIndex,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    onPreviewItemChanged = { previewItem = it },
                    onRowFocusGained = { focusedRowIndex = rowIndex },
                    firstRowFocusRequester = if (rowIndex == 0) firstRowFocusRequester else null
                )
            }
        }
    }
}

@Composable
private fun HomeRowSection(
    row: HomeRowModel,
    rowIndex: Int,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onPreviewItemChanged: (JellyfinItem?) -> Unit,
    onRowFocusGained: () -> Unit,
    firstRowFocusRequester: FocusRequester?
) {
    // Guard so we only trigger vertical realignment when focus ENTERS the row.
    // Horizontal navigation within the row should not cause any LazyColumn scroll.
    var hadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusGroup()
            .onFocusChanged { state ->
                val hasFocusNow = state.hasFocus
                if (hasFocusNow && !hadFocus) {
                    onRowFocusGained()
                }
                hadFocus = hasFocusNow
            }
    ) {
        RowHeader(
            title = row.title,
            accentColor = row.accentColor
        )

        val rowLazyState = rememberLazyListState()


        if (row.cardType == HomeRowCardType.POSTER) {
            LazyRow(
                state = rowLazyState,
                contentPadding = PaddingValues(start = 4.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = rowLazyState),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(row.items, key = { _, item -> item.id }) { index, item ->
                    val isFirstCard = index == 0 && firstRowFocusRequester != null

                    val modifier = if (isFirstCard) {
                        Modifier.focusRequester(firstRowFocusRequester)
                    } else {
                        Modifier
                    }

                    MediaCard(
                        item = item,
                        imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                        onClick = { onItemClick(item.id) },
                        showLabel = false,
                        onItemFocused = { onPreviewItemChanged(it) },
                        modifier = modifier
                    )
                }
            }
        } else {
            LazyRow(
                state = rowLazyState,
                contentPadding = PaddingValues(start = 4.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(row.items, key = { _, item -> item.id }) { index, item ->
                    val isFirstCard = index == 0 && firstRowFocusRequester != null

                    val modifier = if (isFirstCard) {
                        Modifier.focusRequester(firstRowFocusRequester)
                    } else {
                        Modifier
                    }

                    val imageUrl = when (row.key) {
                        "continue_watching" -> {
                            when {
                                !item.backdropImageTags.isNullOrEmpty() -> {
                                    jellyfinClient.getBackdropUrl(
                                        item.id,
                                        item.backdropImageTags?.firstOrNull(),
                                        maxWidth = 600
                                    )
                                }
                                else -> jellyfinClient.getPrimaryImageUrl(
                                    item.id,
                                    item.imageTags?.primary,
                                    maxWidth = 600
                                )
                            }
                        }
                        else -> jellyfinClient.getPrimaryImageUrl(
                            item.id,
                            item.imageTags?.primary,
                            maxWidth = 600
                        )
                    }

                    WideMediaCard(
                        item = item,
                        imageUrl = imageUrl,
                        onClick = { onItemClick(item.id) },
                        showLabel = false,
                        onItemFocused = { onPreviewItemChanged(it) },
                        modifier = modifier
                    )
                }
            }
        }
 * Row header with accent bar and title
 */
@Composable
private fun RowHeader(
    title: String,
    accentColor: Color,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.padding(start = 4.dp, end = 32.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
