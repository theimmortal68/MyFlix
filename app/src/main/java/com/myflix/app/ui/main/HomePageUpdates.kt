package com.myflix.app.ui.main

// ============================================================================
// HERO SECTION INTEGRATION FOR HomePage.kt
// ============================================================================
// Add these changes to your existing HomePage.kt file
// ============================================================================

/*
 * STEP 1: Add imports at the top of the file
 */
// import androidx.compose.runtime.collectAsState
// import androidx.compose.runtime.getValue

/*
 * STEP 2: Collect featured items state in your HomePage composable
 * 
 * Inside your HomePage function, add state collection:
 */

// === STATE COLLECTION TO ADD ===

/*
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetails: (BaseItemDto) -> Unit,
    onNavigateToPlayback: (BaseItemDto) -> Unit,
    // ... other parameters
) {
    // ADD THESE STATE COLLECTIONS:
    val featuredItems by viewModel.featuredItems.collectAsState()
    val isFeaturedLoading by viewModel.isFeaturedLoading.collectAsState()
    
    // Get server URL from your session/config
    val serverUrl = viewModel.serverUrl // or however you access this
    
    // ... rest of your composable
}
*/

/*
 * STEP 3: Add HeroSection at the top of your layout
 * 
 * Your HomePage likely has a TvLazyColumn or similar. 
 * Add the HeroSection as the first item:
 */

// === LAYOUT INTEGRATION ===

/*
// EXAMPLE: If using TvLazyColumn
TvLazyColumn(
    modifier = Modifier.fillMaxSize()
) {
    // ADD THIS AS THE FIRST ITEM:
    item(key = "hero_section") {
        HeroSection(
            featuredItems = featuredItems,
            serverUrl = serverUrl,
            onItemSelected = { item ->
                onNavigateToDetails(item)
            },
            onPlayClicked = { item ->
                onNavigateToPlayback(item)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    // Your existing rows follow...
    item(key = "continue_watching") {
        // Continue watching row
    }
    
    item(key = "recently_added") {
        // Recently added row
    }
    
    // ... other rows
}
*/

/*
 * ALTERNATIVE: If using Column with scroll
 */

/*
Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
) {
    // ADD THIS AT THE TOP:
    HeroSection(
        featuredItems = featuredItems,
        serverUrl = serverUrl,
        onItemSelected = { item ->
            onNavigateToDetails(item)
        },
        onPlayClicked = { item ->
            onNavigateToPlayback(item)
        },
        modifier = Modifier.fillMaxWidth()
    )
    
    // Your existing content follows...
    HomeRow(title = "Continue Watching", items = continueWatchingItems)
    HomeRow(title = "Recently Added", items = recentlyAddedItems)
    // ... other rows
}
*/

// ============================================================================
// COMPLETE EXAMPLE: Full HomePage Structure
// ============================================================================

/*
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetails: (BaseItemDto) -> Unit,
    onNavigateToPlayback: (BaseItemDto) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Collect states
    val uiState by viewModel.uiState.collectAsState()
    val featuredItems by viewModel.featuredItems.collectAsState()
    val serverUrl = viewModel.serverUrl
    
    Scaffold(
        topBar = {
            // Your navigation bar
            HomeTopBar(
                onSearchClick = onNavigateToSearch,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { padding ->
        TvLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section - 30% of screen height
            item(key = "hero") {
                HeroSection(
                    featuredItems = featuredItems,
                    serverUrl = serverUrl,
                    onItemSelected = onNavigateToDetails,
                    onPlayClicked = onNavigateToPlayback,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Continue Watching Row
            if (uiState.continueWatching.isNotEmpty()) {
                item(key = "continue_watching") {
                    MediaRow(
                        title = "Continue Watching",
                        items = uiState.continueWatching,
                        onItemClick = onNavigateToDetails,
                        serverUrl = serverUrl
                    )
                }
            }
            
            // Next Up Row
            if (uiState.nextUp.isNotEmpty()) {
                item(key = "next_up") {
                    MediaRow(
                        title = "Next Up",
                        items = uiState.nextUp,
                        onItemClick = onNavigateToDetails,
                        serverUrl = serverUrl
                    )
                }
            }
            
            // Recently Added Row
            if (uiState.recentlyAdded.isNotEmpty()) {
                item(key = "recently_added") {
                    MediaRow(
                        title = "Recently Added",
                        items = uiState.recentlyAdded,
                        onItemClick = onNavigateToDetails,
                        serverUrl = serverUrl
                    )
                }
            }
            
            // Collection Rows
            uiState.collectionRows.forEach { (collection, items) ->
                item(key = "collection_${collection.id}") {
                    MediaRow(
                        title = collection.name ?: "Collection",
                        items = items,
                        onItemClick = onNavigateToDetails,
                        serverUrl = serverUrl
                    )
                }
            }
            
            // Genre Rows
            uiState.genreRows.forEach { (genre, items) ->
                item(key = "genre_${genre}") {
                    MediaRow(
                        title = genre,
                        items = items,
                        onItemClick = onNavigateToDetails,
                        serverUrl = serverUrl
                    )
                }
            }
        }
    }
}
*/

// ============================================================================
// NOTES
// ============================================================================

/*
 * FOCUS HANDLING:
 * The HeroSection includes focusable buttons. When navigating with D-pad,
 * focus will move between the Play and More Info buttons.
 * 
 * When scrolling down, focus should move to the first row below the hero.
 * The TV LazyColumn handles this automatically.
 * 
 * CUSTOMIZATION:
 * - Change autoRotateIntervalMs to adjust how fast items cycle (default 8 seconds)
 * - Modify fillMaxHeight(0.30f) in HeroSection.kt to adjust the 30% height
 * 
 * LOADING STATE:
 * Consider showing a shimmer or placeholder while featured items load:
 * 
 * if (isFeaturedLoading) {
 *     HeroSectionShimmer()
 * } else if (featuredItems.isNotEmpty()) {
 *     HeroSection(...)
 * }
 */
