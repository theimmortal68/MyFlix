package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

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
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
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
            if (isLoading) {
                TvLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                HomeContent(
                    nextUp = nextUp,
                    continueWatching = continueWatching,
                    recentEpisodes = recentEpisodes,
                    recentShows = recentShows,
                    recentMovies = recentMovies,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    hideWatchedFromRecent = hideWatchedFromRecent
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    nextUp: List<JellyfinItem>,
    continueWatching: List<JellyfinItem>,
    recentEpisodes: List<JellyfinItem>,
    recentShows: List<JellyfinItem>,
    recentMovies: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    hideWatchedFromRecent: Boolean = false
) {
    val listState = rememberLazyListState()
    
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
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 32.dp)
    ) {
        // Next Up row (wide cards with episode thumbnails)
        if (nextUp.isNotEmpty()) {
            item(key = "next_up") {
                WideMediaRow(
                    title = "Next Up",
                    subtitle = null,
                    items = nextUp,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    accentColor = TvColors.BlueAccent
                )
            }
        }
        
        // Continue Watching row (wide cards with thumbnails)
        if (continueWatching.isNotEmpty()) {
            item(key = "continue_watching") {
                WideMediaRow(
                    title = "Continue Watching",
                    subtitle = null,
                    items = continueWatching,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    accentColor = TvColors.BluePrimary
                )
            }
        }
        
        // Recently Added Episodes (wide cards with thumbnails)
        if (filteredRecentEpisodes.isNotEmpty()) {
            item(key = "recent_episodes") {
                WideMediaRow(
                    title = "Recently Added Episodes",
                    subtitle = null,
                    items = filteredRecentEpisodes,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    accentColor = Color(0xFF34D399) // Green
                )
            }
        }
        
        // Recently Added Shows (portrait posters - new series only)
        if (filteredRecentShows.isNotEmpty()) {
            item(key = "recent_shows") {
                MediaRow(
                    title = "Recently Added Shows",
                    subtitle = null,
                    items = filteredRecentShows,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    accentColor = Color(0xFF34D399) // Green
                )
            }
        }
        
        // Recently Added Movies (portrait posters)
        if (filteredRecentMovies.isNotEmpty()) {
            item(key = "recent_movies") {
                MediaRow(
                    title = "Recently Added Movies",
                    subtitle = null,
                    items = filteredRecentMovies,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    accentColor = Color(0xFFFBBF24) // Gold/Yellow
                )
            }
        }
        
        // Empty state
        if (nextUp.isEmpty() && continueWatching.isEmpty() && 
            filteredRecentEpisodes.isEmpty() && filteredRecentShows.isEmpty() && filteredRecentMovies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Welcome to MyFlix",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TvColors.TextPrimary
                        )
                        Text(
                            text = "Your library appears to be empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(
    title: String,
    subtitle: String?,
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    accentColor: Color = TvColors.BluePrimary
) {
    Column(
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        // Row header
        Column(
            modifier = Modifier.padding(start = 4.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Accent bar
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
        
        // Horizontal scrolling cards with snap to card boundaries
        val lazyListState = rememberLazyListState()
        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(start = 4.dp, end = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
        ) {
            items(items, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                    onClick = { onItemClick(item.id) },
                    showLabel = false
                )
            }
        }
    }
}

/**
 * Wide media row for episodes/continue watching with landscape thumbnails (16:9)
 * 
 * TODO: Implement proper D-pad scroll snapping to keep 4 complete cards visible.
 *       The snap fling behavior only works for touch/fling gestures, not D-pad navigation.
 *       Need to investigate TvLazyRow or custom focus-based scroll handling.
 */
@Composable
private fun WideMediaRow(
    title: String,
    subtitle: String?,
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    accentColor: Color = TvColors.BluePrimary
) {
    Column(
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        // Row header
        Column(
            modifier = Modifier.padding(start = 4.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Accent bar
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
        
        // Horizontal scrolling wide cards
        LazyRow(
            contentPadding = PaddingValues(start = 4.dp, end = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
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
                    showLabel = false
                )
            }
        }
    }
}
