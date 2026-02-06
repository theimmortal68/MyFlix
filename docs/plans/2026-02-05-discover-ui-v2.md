# Discover UI V2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement a new category-based carousel UI for Seerr/Jellyseerr integration, parallel to the existing UI, accessible via a separate NavRail icon.

**Architecture:** New `discover/` subdirectory with clean separation from existing Seerr screens. Uses MediaCategory enum to define row types, EnhancedMediaCarousel for horizontal TV navigation, and SeerrMediaCard for unified card display with status badges.

**Tech Stack:** Jetpack Compose for TV, Kotlin, existing SeerrRepository/SeerrClient

---

## Task 1: Add NavRail Entry for Discover V2

**Files:**
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/NavItem.kt`

**Step 1: Add DISCOVER_V2 enum entry**

Add below the existing DISCOVER entry:

```kotlin
DISCOVER(Icons.Outlined.Explore, "Discover", "seerr", Color(0xFF8B5CF6)),
DISCOVER_V2(Icons.Outlined.AutoAwesome, "Discover+", "discover_v2", Color(0xFFC084FC)),
```

Note: Using `AutoAwesome` icon (sparkle) to differentiate from existing Explore icon.

**Step 2: Add icon import**

Add to imports:
```kotlin
import androidx.compose.material.icons.outlined.AutoAwesome
```

**Step 3: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/NavItem.kt
git commit -m "feat(nav): add Discover V2 NavRail entry for new UI"
```

---

## Task 2: Create MediaCategory Enum

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/MediaCategory.kt`

**Step 1: Create discover directory**

```bash
mkdir -p app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover
```

**Step 2: Create MediaCategory enum**

```kotlin
package dev.jausc.myflix.tv.ui.screens.discover

/**
 * Categories for Discover home screen carousel rows.
 * Each category maps to a specific Seerr API endpoint or filter.
 */
enum class MediaCategory(
    val title: String,
    val apiEndpoint: String,
) {
    RECENT_REQUESTS("Recent Requests", "request"),
    TRENDING("Trending", "trending"),
    POPULAR_MOVIES("Popular Movies", "discover/movies"),
    POPULAR_TV("Popular TV Shows", "discover/tv"),
    UPCOMING_MOVIES("Upcoming Movies", "discover/movies/upcoming"),
    UPCOMING_TV("Upcoming TV Shows", "discover/tv/upcoming"),
    MOVIE_GENRES("Movie Genres", "genres/movie"),
    TV_GENRES("TV Genres", "genres/tv"),
    STUDIOS("Studios", "studio"),
    NETWORKS("Networks", "network"),
}
```

**Step 3: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/
git commit -m "feat(discover): add MediaCategory enum for carousel rows"
```

---

## Task 3: Create DiscoverMediaCard Component

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/components/DiscoverMediaCard.kt`

**Step 1: Create components directory**

```bash
mkdir -p app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/components
```

**Step 2: Create DiscoverMediaCard**

```kotlin
package dev.jausc.myflix.tv.ui.screens.discover.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.tv.ui.theme.TvColors

private object CardDimensions {
    val Width = 134.dp
    val Height = 185.dp
    val CornerRadius = 12.dp
    val BadgePadding = 6.dp
    val BadgeCornerRadius = 4.dp
}

private object CardColors {
    val MovieBadge = Color(0xFF2E5AC1)
    val TvBadge = Color(0xFF9D29BC)
    val FocusBorder = Color.White
    val GradientStart = Color(0xFF1F2937)
    val GradientEnd = Color(0xFF111827)
}

/**
 * Universal media card for Discover screens.
 * Displays poster with type badge and availability status.
 *
 * @param media The Seerr media item to display
 * @param onClick Called when card is selected
 * @param modifier Modifier for the card
 */
@Composable
fun DiscoverMediaCard(
    media: SeerrMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(CardDimensions.Width)
            .scale(scale),
    ) {
        Box(
            modifier = Modifier
                .size(CardDimensions.Width, CardDimensions.Height)
                .clip(RoundedCornerShape(CardDimensions.CornerRadius))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CardColors.GradientStart, CardColors.GradientEnd)
                    )
                )
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 2.dp,
                            color = CardColors.FocusBorder,
                            shape = RoundedCornerShape(CardDimensions.CornerRadius)
                        )
                    } else {
                        Modifier
                    }
                )
                .onFocusChanged { isFocused = it.isFocused },
        ) {
            // Poster image
            media.posterPath?.let { posterPath ->
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w300$posterPath",
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } ?: run {
                // Fallback when no poster
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = media.displayTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            // Type badge (MOVIE or TV)
            TypeBadge(
                isMovie = media.isMovie,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(CardDimensions.BadgePadding),
            )

            // Status indicator
            media.availabilityStatus?.let { status ->
                StatusIndicator(
                    status = status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(CardDimensions.BadgePadding),
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(
    isMovie: Boolean,
    modifier: Modifier = Modifier,
) {
    val badgeColor = if (isMovie) CardColors.MovieBadge else CardColors.TvBadge
    val badgeText = if (isMovie) "MOVIE" else "TV"

    Box(
        modifier = modifier
            .background(
                color = badgeColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(CardDimensions.BadgeCornerRadius),
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun StatusIndicator(
    status: Int,
    modifier: Modifier = Modifier,
) {
    val (color, icon) = when (status) {
        SeerrMediaStatus.AVAILABLE -> Color(0xFF4ADE80) to "✓"
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> Color(0xFF4ADE80) to "−"
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Color(0xFF858BF5) to "◷"
        else -> return // Don't show indicator for unknown/not requested
    }

    Box(
        modifier = modifier
            .size(18.dp)
            .background(color, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon,
            color = Color.White,
            fontSize = 10.sp,
        )
    }
}
```

**Step 3: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/components/
git commit -m "feat(discover): add DiscoverMediaCard component with badges"
```

---

## Task 4: Create DiscoverCarousel Component

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/components/DiscoverCarousel.kt`

**Step 1: Create DiscoverCarousel**

```kotlin
package dev.jausc.myflix.tv.ui.screens.discover.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.tv.ui.screens.discover.MediaCategory
import kotlinx.coroutines.launch

private object CarouselDimensions {
    val TitlePadding = 16.dp
    val ContentHeight = 210.dp
    val ItemSpacing = 12.dp
    val HorizontalPadding = 48.dp
}

/**
 * Horizontal carousel row for a media category.
 * Supports D-pad navigation and auto-load more at end.
 *
 * @param category The category this carousel represents
 * @param items List of media items to display
 * @param isLoading Whether more items are being loaded
 * @param onItemClick Called when an item is selected
 * @param onLoadMore Called when user reaches end of list
 * @param onRowFocused Called when this row gains focus
 * @param modifier Modifier for the carousel
 */
@Composable
fun DiscoverCarousel(
    category: MediaCategory,
    items: List<SeerrMedia>,
    isLoading: Boolean,
    onItemClick: (SeerrMedia) -> Unit,
    onLoadMore: () -> Unit,
    onRowFocused: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isRowFocused by remember { mutableStateOf(false) }

    // Auto-load more when reaching end
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex >= items.size - 3 && !isLoading && items.isNotEmpty()) {
            onLoadMore()
        }
    }

    // Scroll to keep selected item visible
    LaunchedEffect(selectedIndex) {
        if (items.isNotEmpty() && selectedIndex >= 0) {
            // Center the selected item (position 3 on screen)
            val targetPosition = (selectedIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetPosition)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Category title
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(
                start = CarouselDimensions.HorizontalPadding,
                bottom = 8.dp,
            ),
        )

        // Carousel row
        Box(
            modifier = Modifier
                .height(CarouselDimensions.ContentHeight)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.hasFocus && !isRowFocused) {
                        isRowFocused = true
                        onRowFocused()
                    } else if (!focusState.hasFocus) {
                        isRowFocused = false
                    }
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                    when (event.key) {
                        Key.DirectionRight -> {
                            if (selectedIndex < items.size - 1) {
                                selectedIndex++
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionLeft -> {
                            if (selectedIndex > 0) {
                                selectedIndex--
                                true
                            } else {
                                false // Allow focus to move to NavRail
                            }
                        }
                        Key.Enter, Key.DirectionCenter -> {
                            if (items.isNotEmpty() && selectedIndex in items.indices) {
                                onItemClick(items[selectedIndex])
                            }
                            true
                        }
                        else -> false
                    }
                }
                .focusable(),
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = CarouselDimensions.HorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(CarouselDimensions.ItemSpacing),
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> "${category.name}_${item.id}" },
                ) { index, item ->
                    DiscoverMediaCard(
                        media = item,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.then(
                            if (index == selectedIndex && isRowFocused) {
                                Modifier // Card handles its own focus styling
                            } else {
                                Modifier
                            }
                        ),
                    )
                }

                // Loading indicator at end
                if (isLoading) {
                    item {
                        CarouselLoadingPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .height(185.dp)
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = "Loading...",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
```

**Step 2: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/components/DiscoverCarousel.kt
git commit -m "feat(discover): add DiscoverCarousel with D-pad nav and auto-load"
```

---

## Task 5: Create DiscoverHomeScreen

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/DiscoverHomeScreen.kt`

**Step 1: Create DiscoverHomeScreen**

```kotlin
package dev.jausc.myflix.tv.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.viewmodel.SeerrHomeViewModel
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.screens.discover.components.DiscoverCarousel
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry

/**
 * New Discover home screen with category-based carousel layout.
 * Parallel implementation to SeerrHomeScreen for A/B comparison.
 *
 * Features:
 * - Vertical list of horizontal carousels by category
 * - D-pad navigation between rows
 * - Auto-load more items per category
 * - Status indicators on media cards
 */
@Composable
fun DiscoverHomeScreen(
    viewModel: SeerrHomeViewModel,
    seerrRepository: SeerrRepository,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    // Focus management
    val contentFocusRequester = remember { FocusRequester() }
    val updateExitFocus = rememberExitFocusRegistry(contentFocusRequester)

    // Row focus tracking
    var focusedRowIndex by remember { mutableIntStateOf(0) }
    val rowFocusRequesters = remember {
        MediaCategory.entries.map { FocusRequester() }
    }

    val columnState = rememberLazyListState()

    // Categories to display
    val visibleCategories = remember {
        listOf(
            MediaCategory.TRENDING,
            MediaCategory.POPULAR_MOVIES,
            MediaCategory.POPULAR_TV,
            MediaCategory.UPCOMING_MOVIES,
            MediaCategory.UPCOMING_TV,
        )
    }

    // Scroll to focused row
    LaunchedEffect(focusedRowIndex) {
        if (focusedRowIndex >= 0) {
            columnState.animateScrollToItem(focusedRowIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        when {
            !isAuthenticated -> {
                // Not authenticated message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Seerr not configured",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                }
            }
            uiState.isLoading && uiState.trendingItems.isEmpty() -> {
                // Initial loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator()
                }
            }
            else -> {
                LazyColumn(
                    state = columnState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp)
                        .focusRequester(contentFocusRequester)
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                            when (event.key) {
                                Key.DirectionDown -> {
                                    if (focusedRowIndex < visibleCategories.size - 1) {
                                        focusedRowIndex++
                                        rowFocusRequesters.getOrNull(focusedRowIndex)?.requestFocus()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                Key.DirectionUp -> {
                                    if (focusedRowIndex > 0) {
                                        focusedRowIndex--
                                        rowFocusRequesters.getOrNull(focusedRowIndex)?.requestFocus()
                                        true
                                    } else {
                                        false // Allow NavRail activation
                                    }
                                }
                                else -> false
                            }
                        },
                ) {
                    itemsIndexed(visibleCategories) { index, category ->
                        val items = getCategoryItems(category, uiState)
                        val isLoading = getCategoryLoading(category, uiState)

                        DiscoverCarousel(
                            category = category,
                            items = items,
                            isLoading = isLoading,
                            onItemClick = { media ->
                                val mediaType = if (media.isMovie) "movie" else "tv"
                                onMediaClick(mediaType, media.id)
                            },
                            onLoadMore = {
                                loadMoreForCategory(category, viewModel)
                            },
                            onRowFocused = {
                                focusedRowIndex = index
                                updateExitFocus(rowFocusRequesters[index])
                            },
                            focusRequester = rowFocusRequesters[index],
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Request initial focus
                LaunchedEffect(Unit) {
                    rowFocusRequesters.firstOrNull()?.requestFocus()
                }
            }
        }
    }
}

/**
 * Get items for a specific category from UI state.
 */
private fun getCategoryItems(
    category: MediaCategory,
    uiState: dev.jausc.myflix.core.viewmodel.SeerrHomeUiState,
): List<SeerrMedia> {
    return when (category) {
        MediaCategory.TRENDING -> uiState.trendingItems
        MediaCategory.POPULAR_MOVIES -> uiState.popularMovies
        MediaCategory.POPULAR_TV -> uiState.popularTv
        MediaCategory.UPCOMING_MOVIES -> uiState.upcomingMovies
        MediaCategory.UPCOMING_TV -> uiState.upcomingTv
        else -> emptyList()
    }
}

/**
 * Check if a category is currently loading.
 */
private fun getCategoryLoading(
    category: MediaCategory,
    uiState: dev.jausc.myflix.core.viewmodel.SeerrHomeUiState,
): Boolean {
    // TODO: Add per-category loading tracking to SeerrHomeViewModel
    return uiState.isLoading
}

/**
 * Trigger load more for a specific category.
 */
private fun loadMoreForCategory(
    category: MediaCategory,
    viewModel: SeerrHomeViewModel,
) {
    when (category) {
        MediaCategory.TRENDING -> viewModel.loadMoreTrending()
        MediaCategory.POPULAR_MOVIES -> viewModel.loadMorePopularMovies()
        MediaCategory.POPULAR_TV -> viewModel.loadMorePopularTv()
        MediaCategory.UPCOMING_MOVIES -> viewModel.loadMoreUpcomingMovies()
        MediaCategory.UPCOMING_TV -> viewModel.loadMoreUpcomingTv()
        else -> { /* No-op for unsupported categories */ }
    }
}
```

**Step 2: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/discover/DiscoverHomeScreen.kt
git commit -m "feat(discover): add DiscoverHomeScreen with carousel layout"
```

---

## Task 6: Wire Up Navigation Routes

**Files:**
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/MainActivity.kt`

**Step 1: Add DiscoverHomeScreen import**

Add to imports section:
```kotlin
import dev.jausc.myflix.tv.ui.screens.discover.DiscoverHomeScreen
```

**Step 2: Add discover_v2 route in NavHost**

Find the NavHost section and add the new route after the existing "seerr" route:

```kotlin
composable(NavItem.DISCOVER_V2.route) {
    DiscoverHomeScreen(
        viewModel = seerrHomeViewModel,
        seerrRepository = seerrRepository,
        onMediaClick = { mediaType, tmdbId ->
            navController.navigate("seerr_detail/$mediaType/$tmdbId")
        },
    )
}
```

**Step 3: Update NavRail items list**

Find where NavItem entries are filtered/listed and ensure DISCOVER_V2 is included:

```kotlin
val navItems = remember(showDiscoverNav, universesEnabled) {
    NavItem.entries.filter { item ->
        when (item) {
            NavItem.DISCOVER -> showDiscoverNav
            NavItem.DISCOVER_V2 -> showDiscoverNav // Show alongside original
            NavItem.UNIVERSES -> universesEnabled
            else -> true
        }
    }
}
```

**Step 4: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/MainActivity.kt
git commit -m "feat(nav): wire up Discover V2 route in MainActivity"
```

---

## Task 7: Verify Build and Test

**Step 1: Build the TV app**

```bash
/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -Command "$env:JAVA_HOME = 'C:\Users\jausc\AppData\Local\Programs\Android Studio\jbr'; cd 'C:\Users\jausc\StudioProjects\MyFlix'; .\gradlew.bat :app-tv:assembleDebug"
```

Expected: BUILD SUCCESSFUL

**Step 2: Install and manually test**

```bash
adb -s 192.168.1.136:5555 install -r "/mnt/c/Users/jausc/StudioProjects/MyFlix/app-tv/build/outputs/apk/debug/app-tv-universal-debug.apk"
```

**Manual Test Checklist:**
- [ ] NavRail shows both "Discover" and "Discover+" icons
- [ ] Selecting "Discover+" navigates to new DiscoverHomeScreen
- [ ] Carousels display with category titles
- [ ] D-pad left/right navigates within carousels
- [ ] D-pad up/down navigates between carousel rows
- [ ] Pressing Enter on a card navigates to detail screen
- [ ] Focus returns to NavRail when pressing Left on first item

**Step 3: Commit verified state**

```bash
git add -A
git commit -m "feat(discover): complete Discover V2 initial implementation

- Add MediaCategory enum for carousel row types
- Add DiscoverMediaCard with type badges and status indicators
- Add DiscoverCarousel with D-pad navigation
- Add DiscoverHomeScreen with vertical carousel layout
- Wire up navigation routes in MainActivity
- Parallel to existing SeerrHomeScreen for A/B comparison"
```

---

## Future Tasks (Phase 2)

After the initial implementation is working, these screens should be added:

1. **DiscoverSearchScreen** - Search with carousel results by type
2. **DiscoverDetailScreen** - Media detail with request actions
3. **DiscoverRequestsScreen** - Pending/approved request management
4. **Genre/Studio/Network browse screens** - Category-filtered views

---

## Notes

- The new UI reuses `SeerrHomeViewModel` and `SeerrRepository` from the existing implementation
- Navigation to detail screens currently routes to existing `SeerrDetailScreen` - this can be replaced later
- The `rememberExitFocusRegistry` pattern ensures proper NavRail focus restoration
- Card focus styling matches existing MyFlix patterns (scale animation, white border)
