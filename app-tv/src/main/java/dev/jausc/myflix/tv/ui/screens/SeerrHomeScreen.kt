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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import dev.jausc.myflix.core.common.ui.SeerrActionDivider
import dev.jausc.myflix.core.common.ui.SeerrActionItem
import dev.jausc.myflix.core.common.ui.SeerrMediaActions
import dev.jausc.myflix.core.common.ui.buildSeerrActionItems
import dev.jausc.myflix.core.seerr.GenreBackdropColors
import dev.jausc.myflix.core.seerr.PopularNetworks
import dev.jausc.myflix.core.seerr.PopularStudios
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrColors
import dev.jausc.myflix.core.seerr.SeerrDiscoverHelper
import dev.jausc.myflix.core.seerr.SeerrDiscoverRow
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.core.seerr.SeerrGenreRow
import dev.jausc.myflix.core.seerr.SeerrImdbRating
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrNetwork
import dev.jausc.myflix.core.seerr.SeerrNetworkRow
import dev.jausc.myflix.core.seerr.SeerrStudio
import dev.jausc.myflix.core.seerr.SeerrStudioRow
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrRequest
import dev.jausc.myflix.core.seerr.SeerrRequestRow
import dev.jausc.myflix.core.seerr.SeerrRottenTomatoesRating
import dev.jausc.myflix.core.seerr.SeerrRowType
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogItemDivider
import dev.jausc.myflix.tv.ui.components.DialogItemEntry
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.TvIconTextButton
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch
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
    onNavigateLibrary: (libraryId: String, libraryName: String, collectionType: String?) -> Unit =
        { _, _, _ -> },
    onNavigateSeerrSearch: () -> Unit = {},
    onNavigateSeerrRequests: () -> Unit = {},
    onNavigateDiscoverTrending: () -> Unit = {},
    onNavigateDiscoverMovies: () -> Unit = {},
    onNavigateDiscoverTv: () -> Unit = {},
    onNavigateDiscoverUpcomingMovies: () -> Unit = {},
    onNavigateDiscoverUpcomingTv: () -> Unit = {},
    onNavigateGenre: (mediaType: String, genreId: Int, genreName: String) -> Unit = { _, _, _ -> },
    onNavigateStudio: (studioId: Int, studioName: String) -> Unit = { _, _ -> },
    onNavigateNetwork: (networkId: Int, networkName: String) -> Unit = { _, _ -> },
    showUniversesInNav: Boolean = false,
) {
    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Focus requesters for navigation
    val contentFocusRequester = remember { FocusRequester() }
    val navBarFocusRequester = remember { FocusRequester() }

    // Content state
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<SeerrDiscoverRow>>(emptyList()) }
    var genreRows by remember { mutableStateOf<List<SeerrGenreRow>>(emptyList()) }
    var studiosRow by remember { mutableStateOf<SeerrStudioRow?>(null) }
    var networksRow by remember { mutableStateOf<SeerrNetworkRow?>(null) }
    var featuredItem by remember { mutableStateOf<SeerrMedia?>(null) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }

    // Preview item - shows focused card's media in hero section
    var previewItem by remember { mutableStateOf<SeerrMedia?>(null) }

    // Track if quick_actions row has focus (for nav bar trigger)
    var quickActionsHasFocus by remember { mutableStateOf(false) }

    // The item to display in hero - preview takes precedence
    val heroDisplayItem = previewItem ?: featuredItem

    // Ratings for hero item (RT and IMDB)
    var heroRtRating by remember { mutableStateOf<SeerrRottenTomatoesRating?>(null) }
    var heroImdbRating by remember { mutableStateOf<SeerrImdbRating?>(null) }

    // Dialog state for long-press context menu
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var dialogMedia by remember { mutableStateOf<SeerrMedia?>(null) }

    // Seerr actions for context menu
    val seerrActions = remember(onMediaClick, coroutineScope, seerrClient) {
        SeerrMediaActions(
            onGoTo = { mediaType, tmdbId -> onMediaClick(mediaType, tmdbId) },
            onRequest = { media ->
                coroutineScope.launch {
                    if (media.isMovie) {
                        seerrClient.requestMovie(media.tmdbId ?: media.id)
                    } else {
                        seerrClient.requestTVShow(media.tmdbId ?: media.id)
                    }
                }
            },
            onBlacklist = { media ->
                coroutineScope.launch {
                    seerrClient.addToBlacklist(media.tmdbId ?: media.id, media.mediaType)
                }
            },
        )
    }

    // Collect auth state as Compose state for proper recomposition
    val isAuthenticated by seerrClient.isAuthenticated.collectAsState()

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

        // Load discover rows using shared helper
        val sliders = seerrClient.getDiscoverSettings().getOrNull()
        val discoverRows = if (!sliders.isNullOrEmpty()) {
            SeerrDiscoverHelper.loadDiscoverRows(seerrClient, sliders)
        } else {
            SeerrDiscoverHelper.loadFallbackRows(seerrClient)
        }

        rows = discoverRows
        featuredItem = discoverRows.firstOrNull()?.items?.firstOrNull()

        // Load genre rows for browsing
        genreRows = SeerrDiscoverHelper.loadGenreRows(seerrClient)

        // Load studios and networks rows
        studiosRow = SeerrDiscoverHelper.getStudiosRow()
        networksRow = SeerrDiscoverHelper.getNetworksRow()

        isLoading = false
    }

    // Load ratings for hero item when it changes
    LaunchedEffect(heroDisplayItem) {
        val media = heroDisplayItem ?: return@LaunchedEffect
        val tmdbId = media.tmdbId ?: media.id

        // Clear previous ratings immediately for responsive UI
        heroRtRating = null
        heroImdbRating = null

        // Load ratings based on media type
        if (media.isMovie) {
            seerrClient.getMovieRatings(tmdbId).onSuccess { response ->
                heroRtRating = response.rt
                heroImdbRating = response.imdb
            }
        } else {
            seerrClient.getTVRatings(tmdbId).onSuccess { rtRating ->
                heroRtRating = rtRating
                // TV shows only have RT ratings from this endpoint
            }
        }
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
                    onNavigateLibrary(it.id, it.name, it.collectionType)
                } ?: onNavigateMovies()
            }
            NavItem.SHOWS -> {
                LibraryFinder.findShowsLibrary(libraries)?.let {
                    onNavigateLibrary(it.id, it.name, it.collectionType)
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
                    // Hero Section (sized to fit content)
                    heroDisplayItem?.let { media ->
                        SeerrHeroSection(
                            media = media,
                            rtRating = heroRtRating,
                            imdbRating = heroImdbRating,
                            modifier = Modifier.fillMaxWidth(),
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
                        // Quick action buttons row
                        item(key = "quick_actions") {
                            Row(
                                modifier = Modifier.padding(start = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TvIconTextButton(
                                    icon = Icons.Outlined.Search,
                                    text = "Search",
                                    onClick = onNavigateSeerrSearch,
                                    modifier = Modifier
                                        .onFocusChanged {
                                            quickActionsHasFocus = it.hasFocus
                                        }
                                        .focusProperties { up = navBarFocusRequester },
                                )
                                TvIconTextButton(
                                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                                    text = "Requests",
                                    onClick = onNavigateSeerrRequests,
                                    modifier = Modifier
                                        .onFocusChanged {
                                            quickActionsHasFocus = it.hasFocus
                                        }
                                        .focusProperties { up = navBarFocusRequester },
                                )
                            }
                        }

                        // Ordered rows:
                        // 1. Trending
                        // 2. Popular Movies
                        // 3. Movie Genres
                        // 4. Upcoming Movies
                        // 5. Studios
                        // 6. Popular TV
                        // 7. TV Genres
                        // 8. Upcoming TV
                        // 9. Networks

                        // Find rows by type for ordered rendering
                        val trendingRow = rows.find { it.rowType == SeerrRowType.TRENDING }
                        val popularMoviesRow = rows.find { it.rowType == SeerrRowType.POPULAR_MOVIES }
                        val movieGenresRow = genreRows.find { it.mediaType == "movie" }
                        val upcomingMoviesRow = rows.find { it.rowType == SeerrRowType.UPCOMING_MOVIES }
                        val popularTvRow = rows.find { it.rowType == SeerrRowType.POPULAR_TV }
                        val tvGenresRow = genreRows.find { it.mediaType == "tv" }
                        val upcomingTvRow = rows.find { it.rowType == SeerrRowType.UPCOMING_TV }

                        // Other rows not in the ordered list
                        val otherRows = rows.filter { row ->
                            row.rowType == SeerrRowType.OTHER
                        }

                        // Helper to render a content row
                        @Composable
                        fun RenderContentRow(row: SeerrDiscoverRow) {
                            val onViewAll: (() -> Unit)? = when (row.rowType) {
                                SeerrRowType.TRENDING -> onNavigateDiscoverTrending
                                SeerrRowType.POPULAR_MOVIES -> onNavigateDiscoverMovies
                                SeerrRowType.POPULAR_TV -> onNavigateDiscoverTv
                                SeerrRowType.UPCOMING_MOVIES -> onNavigateDiscoverUpcomingMovies
                                SeerrRowType.UPCOMING_TV -> onNavigateDiscoverUpcomingTv
                                else -> null
                            }
                            SeerrContentRow(
                                title = row.title,
                                items = row.items,
                                seerrClient = seerrClient,
                                accentColor = Color(row.accentColorValue),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                                onItemLongClick = { media ->
                                    dialogMedia = media
                                    dialogParams = DialogParams(
                                        title = media.displayTitle,
                                        items = buildSeerrDialogItems(media, seerrActions),
                                        fromLongClick = true,
                                    )
                                },
                                onItemFocused = { media -> previewItem = media },
                                onViewAll = onViewAll,
                            )
                        }

                        // 1. Trending
                        trendingRow?.let { row ->
                            item(key = row.key) { RenderContentRow(row) }
                        }

                        // 2. Popular Movies
                        popularMoviesRow?.let { row ->
                            item(key = row.key) { RenderContentRow(row) }
                        }

                        // 3. Movie Genres
                        movieGenresRow?.let { genreRow ->
                            item(key = genreRow.key) {
                                SeerrGenreBrowseRow(
                                    title = genreRow.title,
                                    genres = genreRow.genres,
                                    onGenreClick = { genre ->
                                        onNavigateGenre(genreRow.mediaType, genre.id, genre.name)
                                    },
                                )
                            }
                        }

                        // 4. Upcoming Movies
                        upcomingMoviesRow?.let { row ->
                            item(key = row.key) { RenderContentRow(row) }
                        }

                        // 5. Studios
                        studiosRow?.let { studioRow ->
                            item(key = studioRow.key) {
                                SeerrStudioBrowseRow(
                                    title = studioRow.title,
                                    studios = studioRow.studios,
                                    onStudioClick = { studio ->
                                        onNavigateStudio(studio.id, studio.name)
                                    },
                                )
                            }
                        }

                        // 6. Popular TV
                        popularTvRow?.let { row ->
                            item(key = row.key) { RenderContentRow(row) }
                        }

                        // 7. TV Genres
                        tvGenresRow?.let { genreRow ->
                            item(key = genreRow.key) {
                                SeerrGenreBrowseRow(
                                    title = genreRow.title,
                                    genres = genreRow.genres,
                                    onGenreClick = { genre ->
                                        onNavigateGenre(genreRow.mediaType, genre.id, genre.name)
                                    },
                                )
                            }
                        }

                        // 8. Upcoming TV
                        upcomingTvRow?.let { row ->
                            item(key = row.key) { RenderContentRow(row) }
                        }

                        // 9. Networks
                        networksRow?.let { networkRow ->
                            item(key = networkRow.key) {
                                SeerrNetworkBrowseRow(
                                    title = networkRow.title,
                                    networks = networkRow.networks,
                                    onNetworkClick = { network ->
                                        onNavigateNetwork(network.id, network.name)
                                    },
                                )
                            }
                        }

                        // Other/custom rows at the end
                        otherRows.forEach { row ->
                            item(key = row.key) { RenderContentRow(row) }
                        }
                    }
                }
            }
        }

        // Top Navigation Bar (always visible)
        TopNavigationBarPopup(
            selectedItem = NavItem.DISCOVER,
            onItemSelected = handleNavSelection,
            showUniverses = showUniversesInNav,
            contentFocusRequester = contentFocusRequester,
            focusRequester = navBarFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Long-press context menu dialog
        dialogParams?.let { params ->
            DialogPopup(
                params = params,
                onDismissRequest = {
                    dialogParams = null
                    dialogMedia = null
                },
            )
        }
    }
}

/**
 * Convert Seerr action items to TV dialog items.
 */
private fun buildSeerrDialogItems(
    media: SeerrMedia,
    actions: SeerrMediaActions,
): List<DialogItemEntry> {
    return buildSeerrActionItems(media, actions).map { entry ->
        when (entry) {
            is SeerrActionDivider -> DialogItemDivider
            is SeerrActionItem -> DialogItem(
                text = entry.text,
                icon = entry.icon,
                iconTint = entry.iconTint,
                enabled = entry.enabled,
                onClick = entry.onClick,
            )
        }
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
    rtRating: SeerrRottenTomatoesRating? = null,
    imdbRating: SeerrImdbRating? = null,
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
        contentAlignment = Alignment.TopStart,
    ) { currentMedia ->
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(start = 48.dp, top = 36.dp, bottom = 8.dp),
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
                // Rotten Tomatoes rating
                rtRating?.criticsScore?.let { score ->
                    val isFresh = rtRating.isCriticsFresh
                    val rtColor = if (isFresh) Color(0xFFFA320A) else Color(0xFF6AC238)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "RT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TvColors.TextSecondary,
                        )
                        Text(
                            text = "$score%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = rtColor,
                        )
                    }
                }
                // IMDB rating
                imdbRating?.criticsScore?.let { score ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "IMDb",
                            style = MaterialTheme.typography.labelSmall,
                            color = TvColors.TextSecondary,
                        )
                        Text(
                            text = "%.1f".format(score),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5C518),
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
                    maxLines = 5,
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
    onItemLongClick: ((SeerrMedia) -> Unit)? = null,
    onItemFocused: ((SeerrMedia) -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
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
                    onLongClick = onItemLongClick?.let { { it(media) } },
                    onItemFocused = onItemFocused,
                )
            }
            // View All card at the end of the row
            if (onViewAll != null) {
                item(key = "view_all_$title") {
                    TvViewAllCard(onClick = onViewAll)
                }
            }
        }
    }
}

@Composable
private fun SeerrMediaCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onItemFocused: ((SeerrMedia) -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
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

@Composable
private fun TvViewAllCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .aspectRatio(2f / 3f),
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = TvColors.BluePrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    color = TvColors.BluePrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * A row of genre cards for browsing by genre.
 */
@Composable
private fun SeerrGenreBrowseRow(
    title: String,
    genres: List<SeerrGenre>,
    onGenreClick: (SeerrGenre) -> Unit,
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
                    .background(Color(SeerrColors.PURPLE), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }

        // Genre cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(genres, key = { it.id }) { genre ->
                SeerrGenreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre) },
                )
            }
        }
    }
}

/**
 * A card displaying a genre with duotone-filtered backdrop image.
 */
@Composable
private fun SeerrGenreCard(
    genre: SeerrGenre,
    onClick: () -> Unit,
) {
    // Get the first backdrop from the genre's backdrops list
    val backdropPath = genre.backdrops?.firstOrNull()
    val backdropUrl = backdropPath?.let { GenreBackdropColors.getBackdropUrl(it, genre.id) }

    // Fallback colors if no backdrop available
    val (darkHex, lightHex) = GenreBackdropColors.getColorPair(genre.id)
    val darkColor = Color(android.graphics.Color.parseColor("#$darkHex"))
    val lightColor = Color(android.graphics.Color.parseColor("#$lightHex"))

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(100.dp),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(12.dp),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(12.dp),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        ) {
            // Background: backdrop image with duotone filter, or gradient fallback
            if (backdropUrl != null) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Fallback gradient based on genre colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(darkColor, lightColor),
                            ),
                        ),
                )
            }

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                            ),
                        ),
                    ),
            )

            // Genre name
            Text(
                text = genre.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
    }
}

/**
 * A row of studio cards for browsing by studio.
 */
@Composable
private fun SeerrStudioBrowseRow(
    title: String,
    studios: List<SeerrStudio>,
    onStudioClick: (SeerrStudio) -> Unit,
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
                    .background(Color(SeerrColors.YELLOW), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }

        // Studio cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(studios, key = { it.id }) { studio ->
                SeerrStudioCard(
                    studio = studio,
                    onClick = { onStudioClick(studio) },
                )
            }
        }
    }
}

/**
 * A card displaying a studio with TMDb logo using duotone filter.
 */
@Composable
private fun SeerrStudioCard(
    studio: SeerrStudio,
    onClick: () -> Unit,
) {
    val logoUrl = studio.logoPath?.let { PopularStudios.getLogoUrl(it) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(12.dp),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.SurfaceLight,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(12.dp),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = studio.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = studio.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TvColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * A row of network cards for browsing by network.
 */
@Composable
private fun SeerrNetworkBrowseRow(
    title: String,
    networks: List<SeerrNetwork>,
    onNetworkClick: (SeerrNetwork) -> Unit,
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
                    .background(Color(SeerrColors.TEAL), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }

        // Network cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(networks, key = { it.id }) { network ->
                SeerrNetworkCard(
                    network = network,
                    onClick = { onNetworkClick(network) },
                )
            }
        }
    }
}

/**
 * A card displaying a network with TMDb logo using duotone filter.
 */
@Composable
private fun SeerrNetworkCard(
    network: SeerrNetwork,
    onClick: () -> Unit,
) {
    val logoUrl = network.logoPath?.let { PopularNetworks.getLogoUrl(it) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(12.dp),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.SurfaceLight,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(12.dp),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = network.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = network.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TvColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 2,
                )
            }
        }
    }
}

