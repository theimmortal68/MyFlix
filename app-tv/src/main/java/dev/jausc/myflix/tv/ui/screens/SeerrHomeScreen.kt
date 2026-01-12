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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrDiscoverSlider
import dev.jausc.myflix.core.seerr.SeerrDiscoverSliderType
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.rememberNavBarPopupState
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    // Preview item - shows focused card's media in hero section
    var previewItem by remember { mutableStateOf<SeerrMedia?>(null) }

    // The item to display in hero - preview takes precedence
    val heroDisplayItem = previewItem ?: featuredItem

    // Collect auth state as Compose state for proper recomposition
    val isAuthenticated by seerrClient.isAuthenticated.collectAsState()

    // Filter out items already in library, partially available, or already requested
    fun List<SeerrMedia>.filterDiscoverable() = filter {
        !it.isAvailable && !it.isPending && it.availabilityStatus != SeerrMediaStatus.PARTIALLY_AVAILABLE
    }

    // Load content and libraries - key on auth status to reload if auth changes
    LaunchedEffect(isAuthenticated) {
        // Load libraries for navigation
        jellyfinClient?.getLibraries()?.onSuccess { libs ->
            libraries = libs
        }

        if (!isAuthenticated) {
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
            NavItem.HOME -> { onNavigateHome() }
            NavItem.SEARCH -> { onNavigateSearch() }
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
            NavItem.SETTINGS -> { onNavigateSettings() }
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

                // Layer 1: Backdrop image (90% of screen, fades at edges)
                SeerrBackdropLayer(
                    media = heroDisplayItem,
                    seerrClient = seerrClient,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.9f)
                        .align(Alignment.TopEnd),
                )

                // Layer 2: Hero info (fixed) + Content rows (scrolling)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Fixed Hero Section (50% height to fully hide row above)
                    heroDisplayItem?.let { media ->
                        SeerrHeroSection(
                            media = media,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.50f),
                        )
                    }

                    // Scrolling content rows
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(contentFocusRequester),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        // Trending row
                        if (trending.isNotEmpty()) {
                            item {
                                SeerrContentRow(
                                    title = "Trending",
                                    items = trending,
                                    seerrClient = seerrClient,
                                    accentColor = Color(0xFF8B5CF6),
                                    onItemClick = { media ->
                                        onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                    },
                                    onItemFocused = { media -> previewItem = media },
                                )
                            }
                        }

                        // Popular Movies row
                        if (popularMovies.isNotEmpty()) {
                            item {
                                SeerrContentRow(
                                    title = "Popular Movies",
                                    items = popularMovies,
                                    seerrClient = seerrClient,
                                    accentColor = Color(0xFFFBBF24),
                                    onItemClick = { media ->
                                        onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                    },
                                    onItemFocused = { media -> previewItem = media },
                                )
                            }
                        }

                        // Popular TV row
                        if (popularTV.isNotEmpty()) {
                            item {
                                SeerrContentRow(
                                    title = "Popular TV Shows",
                                    items = popularTV,
                                    seerrClient = seerrClient,
                                    accentColor = Color(0xFF34D399),
                                    onItemClick = { media ->
                                        onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                    },
                                    onItemFocused = { media -> previewItem = media },
                                )
                            }
                        }

                        // Upcoming row
                        if (upcomingMovies.isNotEmpty()) {
                            item {
                                SeerrContentRow(
                                    title = "Coming Soon",
                                    items = upcomingMovies,
                                    seerrClient = seerrClient,
                                    accentColor = Color(0xFF60A5FA),
                                    onItemClick = { media ->
                                        onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                    },
                                    onItemFocused = { media -> previewItem = media },
                                )
                            }
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

/**
 * Backdrop layer for Seerr media - displays behind content with edge fading.
 */
@Composable
private fun SeerrBackdropLayer(
    media: SeerrMedia?,
    seerrClient: SeerrClient,
    modifier: Modifier = Modifier,
) {
    if (media == null) return

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = media,
            transitionSpec = {
                fadeIn(animationSpec = tween(800)) togetherWith
                    fadeOut(animationSpec = tween(800))
            },
            label = "seerr_backdrop_layer",
            modifier = Modifier.fillMaxSize(),
        ) { currentMedia ->
            AsyncImage(
                model = seerrClient.getBackdropUrl(currentMedia.backdropPath),
                contentDescription = currentMedia.displayTitle,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.9f)
                    .drawWithContent {
                        drawContent()
                        // Left edge fade - subtle fade for text readability
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.08f to Color.Black.copy(alpha = 0.5f),
                                    0.2f to Color.Black.copy(alpha = 0.85f),
                                    0.35f to Color.Black,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                        // Bottom edge fade - stronger fade for content row blending
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    0.5f to Color.Black.copy(alpha = 0.85f),
                                    0.7f to Color.Black.copy(alpha = 0.4f),
                                    0.85f to Color.Black.copy(alpha = 0.15f),
                                    1.0f to Color.Transparent,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
    }
}

/**
 * Hero section displaying Seerr media info (no backdrop - backdrop is separate layer).
 */
@Composable
private fun SeerrHeroSection(
    media: SeerrMedia,
    modifier: Modifier = Modifier,
) {
    // Content overlay (left side)
    AnimatedContent(
        targetState = media,
        transitionSpec = {
            fadeIn(animationSpec = tween(500, delayMillis = 200)) togetherWith
                fadeOut(animationSpec = tween(300))
        },
        label = "seerr_hero_content",
        modifier = modifier,
    ) { currentMedia ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .padding(start = 48.dp, top = 36.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            // Status badge
            val statusColor = when (currentMedia.availabilityStatus) {
                SeerrMediaStatus.AVAILABLE -> Color(0xFF22C55E)
                SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Color(0xFFFBBF24)
                SeerrMediaStatus.PARTIALLY_AVAILABLE -> Color(0xFF60A5FA)
                else -> Color(0xFF8B5CF6)
            }
            val statusIcon = when (currentMedia.availabilityStatus) {
                SeerrMediaStatus.AVAILABLE -> Icons.Outlined.Check
                SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Icons.Outlined.Schedule
                else -> Icons.Outlined.Add
            }
            val statusText = SeerrMediaStatus.toDisplayString(currentMedia.availabilityStatus)

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

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = currentMedia.displayTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rating row with release date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Full release date formatted (e.g., "November 14, 2025")
                currentMedia.displayReleaseDate?.let { dateStr ->
                    val formattedDate = try {
                        val date = LocalDate.parse(dateStr)
                        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
                    } catch (_: Exception) {
                        dateStr
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    )
                }
                Text(
                    text = if (currentMedia.isMovie) "Movie" else "TV Show",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                )
                // TMDb rating
                currentMedia.voteAverage?.let { rating ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "TMDb",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF01D277),
                        )
                        Text(
                            text = "%.1f".format(rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFBBF24),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Overview
            currentMedia.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(0.8f),
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
    onItemFocused: ((SeerrMedia) -> Unit)? = null,
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
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
                    onItemFocused = onItemFocused,
                )
            }
        }
    }
}

@Composable
private fun SeerrMediaCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
    onItemFocused: ((SeerrMedia) -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .aspectRatio(2f / 3f)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onItemFocused?.invoke(media)
                }
            },
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium,
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
        ),
    ) {
        Box {
            AsyncImage(
                model = seerrClient.getPosterUrl(media.posterPath),
                contentDescription = media.displayTitle,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )

            // Availability badge
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
    }
}

private data class SeerrDiscoverRow(
    val key: String,
    val title: String,
    val items: List<SeerrMedia>,
    val accentColor: Color,
)

private suspend fun loadDiscoverRows(
    seerrClient: SeerrClient,
    sliders: List<SeerrDiscoverSlider>,
    filterDiscoverable: List<SeerrMedia>.() -> List<SeerrMedia>,
): List<SeerrDiscoverRow> {
    val rows = mutableListOf<SeerrDiscoverRow>()
    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

    for (slider in sliders) {
        val (title, color) = discoverTitleAndColor(slider)
        val items = when (slider.type) {
            SeerrDiscoverSliderType.TRENDING ->
                seerrClient.getTrending().map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.POPULAR_MOVIES ->
                seerrClient.getPopularMovies().map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.POPULAR_TV ->
                seerrClient.getPopularTV().map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.UPCOMING_MOVIES ->
                seerrClient.getUpcomingMovies().map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.UPCOMING_TV ->
                seerrClient.discoverTVWithParams(mapOf("firstAirDateGte" to today))
                    .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.PLEX_WATCHLIST ->
                seerrClient.getWatchlist().map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_MOVIE_KEYWORD ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverMoviesWithParams(mapOf("keywords" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_TV_KEYWORD ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverTVWithParams(mapOf("keywords" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_MOVIE_GENRE ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverMoviesWithParams(mapOf("genre" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_TV_GENRE ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverTVWithParams(mapOf("genre" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_STUDIO ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverMoviesWithParams(mapOf("studio" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_NETWORK ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverTVWithParams(mapOf("network" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_SEARCH ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.search(slider.data!!)
                        .map { it.results.filter { media -> media.mediaType == "movie" || media.mediaType == "tv" } }
                        .getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_MOVIE_STREAMING_SERVICES ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverMoviesWithParams(mapOf("watchProviders" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.TMDB_TV_STREAMING_SERVICES ->
                if (slider.data.isNullOrBlank()) emptyList() else
                    seerrClient.discoverTVWithParams(mapOf("watchProviders" to slider.data!!))
                        .map { it.results }.getOrDefault(emptyList())
            SeerrDiscoverSliderType.RECENTLY_ADDED,
            SeerrDiscoverSliderType.RECENT_REQUESTS,
            SeerrDiscoverSliderType.MOVIE_GENRES,
            SeerrDiscoverSliderType.TV_GENRES,
            SeerrDiscoverSliderType.STUDIOS,
            SeerrDiscoverSliderType.NETWORKS -> emptyList()
        }

        val filtered = items.filterDiscoverable().take(12)
        if (filtered.isNotEmpty()) {
            rows.add(
                SeerrDiscoverRow(
                    key = "discover_${slider.type.name.lowercase(Locale.US)}_${slider.id}",
                    title = title,
                    items = filtered,
                    accentColor = color,
                ),
            )
        }
    }

    return rows
}

private suspend fun loadFallbackRows(
    seerrClient: SeerrClient,
    filterDiscoverable: List<SeerrMedia>.() -> List<SeerrMedia>,
): List<SeerrDiscoverRow> {
    val rows = mutableListOf<SeerrDiscoverRow>()
    val trending = seerrClient.getTrending().map { it.results }.getOrDefault(emptyList())
    val popularMovies = seerrClient.getPopularMovies().map { it.results }.getOrDefault(emptyList())
    val popularTv = seerrClient.getPopularTV().map { it.results }.getOrDefault(emptyList())
    val upcoming = seerrClient.getUpcomingMovies().map { it.results }.getOrDefault(emptyList())

    listOf(
        "Trending" to Pair(Color(0xFF8B5CF6), trending),
        "Popular Movies" to Pair(Color(0xFFFBBF24), popularMovies),
        "Popular TV Shows" to Pair(Color(0xFF34D399), popularTv),
        "Coming Soon" to Pair(Color(0xFF60A5FA), upcoming),
    ).forEach { (title, data) ->
        val filtered = data.second.filterDiscoverable().take(12)
        if (filtered.isNotEmpty()) {
            rows.add(
                SeerrDiscoverRow(
                    key = "fallback_${title.lowercase(Locale.US).replace(" ", "_")}",
                    title = title,
                    items = filtered,
                    accentColor = data.first,
                ),
            )
        }
    }

    return rows
}

private fun discoverTitleAndColor(
    slider: SeerrDiscoverSlider,
): Pair<String, Color> {
    val defaultTitle = when (slider.type) {
        SeerrDiscoverSliderType.RECENTLY_ADDED -> "Recently Added"
        SeerrDiscoverSliderType.RECENT_REQUESTS -> "Recent Requests"
        SeerrDiscoverSliderType.PLEX_WATCHLIST -> "Watchlist"
        SeerrDiscoverSliderType.TRENDING -> "Trending"
        SeerrDiscoverSliderType.POPULAR_MOVIES -> "Popular Movies"
        SeerrDiscoverSliderType.MOVIE_GENRES -> "Movie Genres"
        SeerrDiscoverSliderType.UPCOMING_MOVIES -> "Upcoming Movies"
        SeerrDiscoverSliderType.STUDIOS -> "Studios"
        SeerrDiscoverSliderType.POPULAR_TV -> "Popular TV"
        SeerrDiscoverSliderType.TV_GENRES -> "TV Genres"
        SeerrDiscoverSliderType.UPCOMING_TV -> "Upcoming TV"
        SeerrDiscoverSliderType.NETWORKS -> "Networks"
        SeerrDiscoverSliderType.TMDB_MOVIE_KEYWORD -> slider.title ?: "Movie Keyword"
        SeerrDiscoverSliderType.TMDB_TV_KEYWORD -> slider.title ?: "TV Keyword"
        SeerrDiscoverSliderType.TMDB_MOVIE_GENRE -> slider.title ?: "Movie Genre"
        SeerrDiscoverSliderType.TMDB_TV_GENRE -> slider.title ?: "TV Genre"
        SeerrDiscoverSliderType.TMDB_STUDIO -> slider.title ?: "Studio"
        SeerrDiscoverSliderType.TMDB_NETWORK -> slider.title ?: "Network"
        SeerrDiscoverSliderType.TMDB_SEARCH -> slider.title ?: "Search"
        SeerrDiscoverSliderType.TMDB_MOVIE_STREAMING_SERVICES -> slider.title ?: "Streaming Movies"
        SeerrDiscoverSliderType.TMDB_TV_STREAMING_SERVICES -> slider.title ?: "Streaming TV"
    }

    val accentColor = when (slider.type) {
        SeerrDiscoverSliderType.TRENDING -> Color(0xFF8B5CF6)
        SeerrDiscoverSliderType.POPULAR_MOVIES -> Color(0xFFFBBF24)
        SeerrDiscoverSliderType.POPULAR_TV -> Color(0xFF34D399)
        SeerrDiscoverSliderType.UPCOMING_MOVIES -> Color(0xFF60A5FA)
        SeerrDiscoverSliderType.UPCOMING_TV -> Color(0xFF60A5FA)
        SeerrDiscoverSliderType.PLEX_WATCHLIST -> Color(0xFF22C55E)
        else -> Color(0xFF8B5CF6)
    }

    return defaultTitle to accentColor
}
