@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.rememberNavBarPopupState
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Seerr home/discover screen for TV.
 * Uses unified TopNavigationBar for consistent navigation across all screens.
 *
 * Features:
 * - Trending movies and TV shows
 * - Popular content rows
 * - Availability status indicators
 * - Navigation to request/detail screens
 */
@Composable
fun SeerrHomeScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateSearch: () -> Unit = {},
    onNavigateMovies: () -> Unit = {},
    onNavigateShows: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    jellyfinClient: JellyfinClient? = null,
    onNavigateLibrary: (String, String) -> Unit = { _, _ -> },
    @Suppress("UNUSED_PARAMETER") onNavigateSeerrSearch: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onNavigateSeerrRequests: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    // Focus requesters for navigation
    val homeButtonFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    // Popup nav bar state - visible on load, auto-hides after 5 seconds
    val navBarState = rememberNavBarPopupState()

    // Content state
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var trending by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var popularMovies by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var popularTV by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var upcomingMovies by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var featuredItem by remember { mutableStateOf<SeerrMedia?>(null) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }

    // Filter out items already in library, partially available, or already requested
    fun List<SeerrMedia>.filterDiscoverable() = filter {
        !it.isAvailable && !it.isPending && it.availabilityStatus != SeerrMediaStatus.PARTIALLY_AVAILABLE
    }

    // Load content and libraries
    LaunchedEffect(Unit) {
        // Load libraries for navigation
        jellyfinClient?.getLibraries()?.onSuccess { libs ->
            libraries = libs
        }

        if (!seerrClient.isAuthenticated) {
            errorMessage = "Not connected to Seerr"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        // Load trending (filter out items already in library)
        seerrClient.getTrending()
            .onSuccess { result ->
                val filtered = result.results.filterDiscoverable()
                trending = filtered.take(12)
                featuredItem = filtered.firstOrNull()
            }
            .onFailure { errorMessage = it.message }

        // Load popular movies (filter out items already in library)
        seerrClient.getPopularMovies()
            .onSuccess { result ->
                popularMovies = result.results.filterDiscoverable().take(12)
            }

        // Load popular TV (filter out items already in library)
        seerrClient.getPopularTV()
            .onSuccess { result ->
                popularTV = result.results.filterDiscoverable().take(12)
            }

        // Load upcoming (filter anyway)
        seerrClient.getUpcomingMovies()
            .onSuccess { result ->
                upcomingMovies = result.results.filterDiscoverable().take(12)
            }

        isLoading = false
    }

    // Request initial focus on content
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            contentFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    // Handle navigation
    val handleNavSelection: (NavItem) -> Unit = { item ->
        when (item) {
            NavItem.HOME -> onNavigateHome()
            NavItem.SEARCH -> onNavigateSearch()
            NavItem.MOVIES -> {
                LibraryFinder.findMoviesLibrary(libraries)?.let { 
                    onNavigateLibrary(it.id, it.name) 
                } ?: onNavigateMovies()
            }
            NavItem.SHOWS -> {
                LibraryFinder.findShowsLibrary(libraries)?.let { 
                    onNavigateLibrary(it.id, it.name) 
                } ?: onNavigateShows()
            }
            NavItem.DISCOVER -> { /* Already here */ }
            NavItem.SETTINGS -> onNavigateSettings()
            else -> {}
        }
    }

    // Use Box to layer TopNavigationBar on top of content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        // Main content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Failed to load content",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check Seerr settings in Settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            else -> {
                val lazyListState = rememberLazyListState()
                
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(contentFocusRequester),
                    contentPadding = PaddingValues(top = 48.dp, bottom = 32.dp),
                ) {
                    // Hero section
                    item {
                        featuredItem?.let { media ->
                            SeerrHeroSection(
                                media = media,
                                seerrClient = seerrClient,
                                onClick = { onMediaClick(media.mediaType, media.tmdbId ?: media.id) },
                            )
                        }
                    }

                    // Callback for showing nav bar when UP is pressed on first content row
                    val showNavBarOnUp: () -> Unit = {
                        navBarState.show()
                        scope.launch {
                            delay(150)
                            try {
                                homeButtonFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    // Track which row is first (to pass onUpPressed callback)
                    var isFirstContentRow = true

                    // Trending row
                    if (trending.isNotEmpty()) {
                        val isFirst = isFirstContentRow
                        isFirstContentRow = false
                        item {
                            SeerrContentRow(
                                title = "Trending",
                                items = trending,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFF8B5CF6),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                                onUpPressed = if (isFirst) showNavBarOnUp else null,
                            )
                        }
                    }

                    // Popular Movies row
                    if (popularMovies.isNotEmpty()) {
                        val isFirst = isFirstContentRow
                        isFirstContentRow = false
                        item {
                            SeerrContentRow(
                                title = "Popular Movies",
                                items = popularMovies,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFFFBBF24),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                                onUpPressed = if (isFirst) showNavBarOnUp else null,
                            )
                        }
                    }

                    // Popular TV row
                    if (popularTV.isNotEmpty()) {
                        val isFirst = isFirstContentRow
                        isFirstContentRow = false
                        item {
                            SeerrContentRow(
                                title = "Popular TV Shows",
                                items = popularTV,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFF34D399),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                                onUpPressed = if (isFirst) showNavBarOnUp else null,
                            )
                        }
                    }

                    // Upcoming row
                    if (upcomingMovies.isNotEmpty()) {
                        val isFirst = isFirstContentRow
                        @Suppress("UNUSED_VALUE")
                        isFirstContentRow = false
                        item {
                            SeerrContentRow(
                                title = "Coming Soon",
                                items = upcomingMovies,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFF60A5FA),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                                onUpPressed = if (isFirst) showNavBarOnUp else null,
                            )
                        }
                    }
                }
            }
        }

        // Top Navigation Bar (popup overlay)
        TopNavigationBarPopup(
            visible = navBarState.isVisible,
            selectedItem = NavItem.DISCOVER,
            onItemSelected = handleNavSelection,
            onDismiss = {
                navBarState.hide()
                try {
                    contentFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            },
            homeButtonFocusRequester = homeButtonFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Suppress("UnusedParameter")
@Composable
private fun SeerrHeroSection(media: SeerrMedia, seerrClient: SeerrClient, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        // Backdrop
        AsyncImage(
            model = seerrClient.getBackdropUrl(media.backdropPath),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TvColors.Background.copy(alpha = 0.7f),
                            TvColors.Background,
                        ),
                        startY = 0f,
                        endY = 1000f,
                    ),
                ),
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 32.dp),
        ) {
            // Status badge (uses mediaInfo.status for availability)
            val statusColor = when (media.availabilityStatus) {
                SeerrMediaStatus.AVAILABLE -> Color(0xFF22C55E)
                SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Color(0xFFFBBF24)
                SeerrMediaStatus.PARTIALLY_AVAILABLE -> Color(0xFF60A5FA)
                else -> Color(0xFF8B5CF6)
            }
            val statusIcon = when (media.availabilityStatus) {
                SeerrMediaStatus.AVAILABLE -> Icons.Outlined.Check
                SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Icons.Outlined.Schedule
                else -> Icons.Outlined.Add
            }
            val statusText = SeerrMediaStatus.toDisplayString(media.availabilityStatus)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = statusColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = media.displayTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
            )

            // Year and type
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                media.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }
                Text(
                    text = if (media.isMovie) "Movie" else "TV Show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                )
                media.voteAverage?.let { rating ->
                    Text(
                        text = "%.1f".format(rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFBBF24),
                    )
                }
            }

            // Overview
            media.overview?.let { overview ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = overview.take(200) + if (overview.length > 200) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun SeerrContentRow(
    title: String,
    items: List<SeerrMedia>,
    seerrClient: SeerrClient,
    accentColor: Color,
    onItemClick: (SeerrMedia) -> Unit,
    onUpPressed: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .onPreviewKeyEvent { event ->
                // Intercept UP key to show nav bar (only for first row)
                if (onUpPressed != null &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionUp
                ) {
                    onUpPressed()
                    true
                } else {
                    false
                }
            },
    ) {
        // Row header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(accentColor, RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }

        // Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items, key = { "${it.mediaType}_${it.id}" }) { media ->
                SeerrMediaCard(
                    media = media,
                    seerrClient = seerrClient,
                    onClick = { onItemClick(media) },
                )
            }
        }
    }
}

@Composable
private fun SeerrMediaCard(media: SeerrMedia, seerrClient: SeerrClient, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box {
        androidx.tv.material3.Surface(
            onClick = onClick,
            modifier = Modifier
                .width(120.dp)
                .focusable()
                .onFocusChanged { isFocused = it.isFocused },
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(8.dp),
            ),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.FocusedSurface,
            ),
            border = androidx.tv.material3.ClickableSurfaceDefaults.border(
                focusedBorder = androidx.tv.material3.Border(
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(8.dp),
                ),
            ),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f),
                ) {
                    AsyncImage(
                        model = seerrClient.getPosterUrl(media.posterPath),
                        contentDescription = media.displayTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    // Availability badge (uses mediaInfo.status for availability)
                    val badgeColor = when (media.availabilityStatus) {
                        SeerrMediaStatus.AVAILABLE -> Color(0xFF22C55E)
                        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Color(0xFFFBBF24)
                        SeerrMediaStatus.PARTIALLY_AVAILABLE -> Color(0xFF60A5FA)
                        else -> null
                    }

                    badgeColor?.let { color ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color),
                        )
                    }
                }

                // Title
                Text(
                    text = media.displayTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )

                // Year
                media.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
