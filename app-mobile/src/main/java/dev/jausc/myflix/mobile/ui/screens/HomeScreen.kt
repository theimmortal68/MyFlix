package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.jausc.myflix.core.common.HeroContentBuilder
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import kotlinx.coroutines.isActive
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.components.MobileContentRow
import dev.jausc.myflix.mobile.ui.components.MobileHeroSection
import dev.jausc.myflix.mobile.ui.components.MobileNavItem
import dev.jausc.myflix.mobile.ui.components.MobileRowColors
import dev.jausc.myflix.mobile.ui.components.MobileRowData
import dev.jausc.myflix.mobile.ui.components.MobileTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Background polling interval in milliseconds */
private const val POLL_INTERVAL_MS = 30_000L

/**
 * Mobile home screen with Netflix-style hero section and dropdown navigation.
 * 
 * Features:
 * - Full-width hero section with featured content (responsive sizing)
 * - Dropdown navigation menu in top-left
 * - Horizontal scrolling content rows (responsive card sizes)
 * - Touch-friendly card interactions
 * - Automatic layout adjustment for phones and foldables
 */
@Composable
fun HomeScreen(
    jellyfinClient: JellyfinClient,
    onLibraryClick: (String, String) -> Unit,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    // Track configuration changes for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    
    // Use configuration as key to trigger recomposition on screen changes
    val configKey = remember(screenWidthDp, screenHeightDp) {
        "$screenWidthDp-$screenHeightDp"
    }

    // Content state
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var nextUp by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentMovies by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentShows by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var recentEpisodes by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var featuredItems by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Navigation state
    var selectedNavItem by remember { mutableStateOf(MobileNavItem.HOME) }

    /**
     * Load or refresh all home screen content.
     */
    suspend fun loadContent(showLoading: Boolean = false) {
        if (showLoading) isLoading = true
        
        // Clear cache to get fresh data
        jellyfinClient.clearCache()
        
        // Get libraries first
        jellyfinClient.getLibraries().onSuccess { libs ->
            libraries = libs

            // Find libraries using shared finder
            val moviesLibrary = LibraryFinder.findMoviesLibrary(libs)
            val showsLibrary = LibraryFinder.findShowsLibrary(libs)
            
            // Get latest movies
            moviesLibrary?.let { lib ->
                jellyfinClient.getLatestMovies(lib.id, limit = 12).onSuccess { items ->
                    recentMovies = items
                }
            }
            
            // Get latest series
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
        
        // Get Next Up
        jellyfinClient.getNextUp(limit = 12).onSuccess { items ->
            nextUp = items
        }
        
        // Get Continue Watching
        jellyfinClient.getContinueWatching(limit = 12).onSuccess { items ->
            continueWatching = items
        }
        
        // Build featured items for hero section using shared logic
        featuredItems = HeroContentBuilder.buildFeaturedItems(
            continueWatching = continueWatching,
            nextUp = nextUp,
            recentMovies = recentMovies,
            recentShows = recentShows,
            config = HeroContentBuilder.mobileConfig
        )
        
        isLoading = false
    }

    // Initial load
    LaunchedEffect(Unit) {
        scope.launch {
            loadContent(showLoading = true)
        }
    }
    
    // Background polling for updates
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(POLL_INTERVAL_MS)
            loadContent(showLoading = false)
        }
    }
    
    // Handle navigation selection
    val handleNavSelection: (MobileNavItem) -> Unit = { item ->
        selectedNavItem = item
        when (item) {
            MobileNavItem.HOME -> { /* Already on home */ }
            MobileNavItem.SEARCH -> onSearchClick()
            MobileNavItem.MOVIES -> {
                LibraryFinder.findMoviesLibrary(libraries)?.let { onLibraryClick(it.id, it.name) }
            }
            MobileNavItem.SHOWS -> {
                LibraryFinder.findShowsLibrary(libraries)?.let { onLibraryClick(it.id, it.name) }
            }
            MobileNavItem.SETTINGS -> onSettingsClick()
        }
    }

    // Build content rows in specified order:
    // Continue Watching, Next Up, Recently Added Episodes, Recently Added Shows, Recently Added Movies
    val rows = remember(continueWatching, nextUp, recentEpisodes, recentShows, recentMovies) {
        buildList {
            if (continueWatching.isNotEmpty()) {
                add(MobileRowData(
                    key = "continue",
                    title = "Continue Watching",
                    items = continueWatching,
                    isWideCard = true,
                    accentColor = MobileRowColors.ContinueWatching
                ))
            }
            if (nextUp.isNotEmpty()) {
                add(MobileRowData(
                    key = "nextup",
                    title = "Next Up",
                    items = nextUp,
                    isWideCard = true,
                    accentColor = MobileRowColors.NextUp
                ))
            }
            if (recentEpisodes.isNotEmpty()) {
                add(MobileRowData(
                    key = "episodes",
                    title = "Recently Added Episodes",
                    items = recentEpisodes,
                    isWideCard = true,
                    accentColor = MobileRowColors.RecentlyAdded
                ))
            }
            if (recentShows.isNotEmpty()) {
                add(MobileRowData(
                    key = "shows",
                    title = "Recently Added Shows",
                    items = recentShows,
                    isWideCard = false,
                    accentColor = MobileRowColors.Shows
                ))
            }
            if (recentMovies.isNotEmpty()) {
                add(MobileRowData(
                    key = "movies",
                    title = "Recently Added Movies",
                    items = recentMovies,
                    isWideCard = false,
                    accentColor = MobileRowColors.Movies
                ))
            }
        }
    }

    // Key the entire layout to configuration to force redraw on screen changes
    key(configKey) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Main content - keyed to configuration for responsive updates
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Hero section
                    item(key = "hero") {
                        MobileHeroSection(
                            featuredItems = featuredItems,
                            jellyfinClient = jellyfinClient,
                            onItemClick = onItemClick,
                            onPlayClick = onPlayClick  // Now goes to player
                        )
                    }
                    
                    // Content rows
                    items(rows, key = { it.key }) { rowData ->
                        MobileContentRow(
                            title = rowData.title,
                            items = rowData.items,
                            jellyfinClient = jellyfinClient,
                            onItemClick = onItemClick,
                            accentColor = rowData.accentColor,
                            isWideCard = rowData.isWideCard
                        )
                    }
                }
            }
            
            // Top bar overlay (always visible over hero) - zIndex ensures touch events work
            MobileTopBar(
                selectedItem = selectedNavItem,
                onItemSelected = handleNavSelection,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
            )
        }
    }
}
