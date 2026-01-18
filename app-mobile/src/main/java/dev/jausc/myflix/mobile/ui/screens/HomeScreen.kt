@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRequestStatus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jausc.myflix.core.common.HeroContentBuilder
import dev.jausc.myflix.core.viewmodel.HomeUiState
import dev.jausc.myflix.core.viewmodel.HomeViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.ui.PlayAllData
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.PlayQueueManager
import dev.jausc.myflix.core.player.QueueItem
import dev.jausc.myflix.core.player.QueueSource
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrColors
import dev.jausc.myflix.core.seerr.SeerrRequest
import kotlinx.coroutines.launch
import dev.jausc.myflix.mobile.MobilePreferences
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.HomeMenuActions
import dev.jausc.myflix.mobile.ui.components.MediaInfoBottomSheet
import dev.jausc.myflix.mobile.ui.components.MobileContentRow
import dev.jausc.myflix.mobile.ui.components.MobileHeroSection
import dev.jausc.myflix.mobile.ui.components.MobileNavItem
import dev.jausc.myflix.mobile.ui.components.MobileRowColors
import dev.jausc.myflix.mobile.ui.components.MobileRowData
import dev.jausc.myflix.mobile.ui.components.MobileTopBar
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import dev.jausc.myflix.mobile.ui.components.buildHomeMenuItems

/**
 * Mobile home screen with Netflix-style hero section and dropdown navigation.
 *
 * Features:
 * - Full-width hero section with featured content (responsive sizing)
 * - Dropdown navigation menu in top-left
 * - Horizontal scrolling content rows (responsive card sizes)
 * - Touch-friendly card interactions
 * - Automatic layout adjustment for phones and foldables
 * - Long-press context menu for quick actions
 */
@Composable
fun HomeScreen(
    jellyfinClient: JellyfinClient,
    preferences: MobilePreferences,
    seerrClient: SeerrClient? = null,
    onLibraryClick: (libraryId: String, libraryName: String, collectionType: String?) -> Unit,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onDiscoverClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSeerrMediaClick: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> },
) {
    // ViewModel with manual DI
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(jellyfinClient, preferences, seerrClient, HeroContentBuilder.mobileConfig),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Collect preference values for UI
    val showSeasonPremieres by viewModel.showSeasonPremieres.collectAsState()
    val showGenreRows by viewModel.showGenreRows.collectAsState()
    val showCollections by viewModel.showCollections.collectAsState()
    val showSuggestions by viewModel.showSuggestions.collectAsState()
    val pinnedCollections by viewModel.pinnedCollections.collectAsState()
    val showSeerrRecentRequests by viewModel.showSeerrRecentRequests.collectAsState()

    // Scope for async menu actions
    val scope = rememberCoroutineScope()

    // Popup menu state for long-press
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Menu actions for long-press
    val menuActions = remember(viewModel, scope) {
        HomeMenuActions(
            onGoTo = { itemId -> onItemClick(itemId) },
            onPlay = { itemId -> onPlayClick(itemId) },
            onMarkWatched = { itemId, watched ->
                viewModel.setPlayed(itemId, watched)
            },
            onToggleFavorite = { itemId, favorite ->
                viewModel.setFavorite(itemId, favorite)
            },
            onGoToSeries = { seriesId -> onItemClick(seriesId) },
            onGoToSeason = { seasonId -> onItemClick(seasonId) },
            onHideFromResume = { itemId ->
                viewModel.hideFromResume(itemId)
            },
            onPlayAllFromEpisode = { data: PlayAllData ->
                scope.launch {
                    jellyfinClient.getEpisodes(data.seriesId, data.seasonId)
                        .onSuccess { episodes ->
                            // Filter to episodes from the selected one onwards
                            val sortedEpisodes = episodes.sortedBy { it.indexNumber ?: 0 }
                            val startIndex = sortedEpisodes.indexOfFirst { it.id == data.itemId }
                                .coerceAtLeast(0)
                            val episodesToPlay = sortedEpisodes.drop(startIndex)

                            if (episodesToPlay.isNotEmpty()) {
                                // Build queue items
                                val queueItems = episodesToPlay.map { episode ->
                                    QueueItem(
                                        itemId = episode.id,
                                        title = episode.name,
                                        episodeInfo = buildString {
                                            episode.parentIndexNumber?.let { append("S$it ") }
                                            episode.indexNumber?.let { append("E$it") }
                                        }.takeIf { it.isNotBlank() },
                                        thumbnailItemId = episode.seriesId,
                                    )
                                }

                                // Set queue and navigate to first item
                                PlayQueueManager.setQueue(
                                    items = queueItems,
                                    source = QueueSource.EPISODE_PLAY_ALL,
                                    startIndex = 0,
                                )
                                onPlayClick(episodesToPlay.first().id)
                            }
                        }
                }
            },
            onShowMediaInfo = { item -> mediaInfoItem = item },
        )
    }

    // Handle long-press on items
    val handleItemLongClick: (JellyfinItem) -> Unit = { item ->
        val subtitle = when {
            item.type == "Episode" && item.seriesName != null ->
                "${item.seriesName} - S${item.parentIndexNumber ?: "?"}E${item.indexNumber ?: "?"}"
            item.productionYear != null -> item.productionYear.toString()
            else -> item.type
        }
        popupMenuParams = BottomSheetParams(
            title = item.name,
            subtitle = subtitle,
            items = buildHomeMenuItems(item, menuActions),
        )
    }

    // Track configuration changes for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    // Use configuration as key to trigger recomposition on screen changes
    val configKey = remember(screenWidthDp, screenHeightDp) {
        "$screenWidthDp-$screenHeightDp"
    }

    // Navigation state
    var selectedNavItem by remember { mutableStateOf(MobileNavItem.HOME) }

    // Handle navigation selection
    val handleNavSelection: (MobileNavItem) -> Unit = { item ->
        selectedNavItem = item
        when (item) {
            MobileNavItem.HOME -> Unit // Already on home
            MobileNavItem.SEARCH -> onSearchClick()
            MobileNavItem.MOVIES ->
                LibraryFinder.findMoviesLibrary(state.libraries)?.let {
                    onLibraryClick(it.id, it.name, it.collectionType)
                }
            MobileNavItem.SHOWS ->
                LibraryFinder.findShowsLibrary(state.libraries)?.let {
                    onLibraryClick(it.id, it.name, it.collectionType)
                }
            MobileNavItem.DISCOVER -> onDiscoverClick()
            MobileNavItem.SETTINGS -> onSettingsClick()
        }
    }

    // Build content rows in specified order
    val rows = remember(
        state.continueWatching, state.nextUp, state.recentEpisodes, state.recentShows, state.recentMovies,
        state.seasonPremieres, state.collections, state.suggestions, state.genreRowsData, state.pinnedCollectionsData,
        showSeasonPremieres, showGenreRows, showCollections, showSuggestions, pinnedCollections,
    ) {
        buildList {
            if (state.continueWatching.isNotEmpty()) {
                add(
                    MobileRowData(
                        key = "continue",
                        title = "Continue Watching",
                        items = state.continueWatching,
                        isWideCard = true,
                        accentColor = MobileRowColors.ContinueWatching,
                    ),
                )
            }
            if (state.nextUp.isNotEmpty()) {
                add(
                    MobileRowData(
                        key = "nextup",
                        title = "Next Up",
                        items = state.nextUp,
                        isWideCard = true,
                        accentColor = MobileRowColors.NextUp,
                    ),
                )
            }

            if (state.recentEpisodes.isNotEmpty()) {
                add(
                    MobileRowData(
                        key = "episodes",
                        title = "Recently Added Episodes",
                        items = state.recentEpisodes,
                        isWideCard = true,
                        accentColor = MobileRowColors.RecentlyAdded,
                    ),
                )
            }
            if (state.recentShows.isNotEmpty()) {
                add(
                    MobileRowData(
                        key = "shows",
                        title = "Recently Added Shows",
                        items = state.recentShows,
                        isWideCard = false,
                        accentColor = MobileRowColors.Shows,
                    ),
                )
            }

            // Upcoming Episodes (season premieres) - show series poster with episode badge and air date
            if (showSeasonPremieres && state.seasonPremieres.isNotEmpty()) {
                add(
                    MobileRowData(
                        key = "premieres",
                        title = "Upcoming Episodes",
                        items = state.seasonPremieres,
                        isWideCard = false, // Use series poster instead of episode thumb
                        accentColor = MobileRowColors.Premieres,
                        isUpcomingEpisodes = true,
                    ),
                )
            }

            if (state.recentMovies.isNotEmpty()) {
                add(
                    MobileRowData(
                        key = "movies",
                        title = "Recently Added Movies",
                        items = state.recentMovies,
                        isWideCard = false,
                        accentColor = MobileRowColors.Movies,
                    ),
                )
            }

            // Suggestions (before collections and genres)
            if (showSuggestions && state.suggestions.isNotEmpty()) {
                add(
                    MobileRowData(
                        key = "suggestions",
                        title = "You Might Like",
                        items = state.suggestions,
                        isWideCard = false,
                        accentColor = MobileRowColors.Suggestions,
                    ),
                )
            }

            // Pinned collection rows (each pinned collection shows its items)
            if (showCollections && state.pinnedCollectionsData.isNotEmpty()) {
                state.pinnedCollectionsData.entries.forEachIndexed { index, (collectionId, pair) ->
                    val (collectionName, items) = pair
                    if (items.isNotEmpty()) {
                        val color = MobileRowColors.pinnedCollectionColors[index % MobileRowColors.pinnedCollectionColors.size]
                        add(
                            MobileRowData(
                                key = "pinned_$collectionId",
                                title = collectionName,
                                items = items,
                                isWideCard = false,
                                accentColor = color,
                            ),
                        )
                    }
                }
            }

            // Genre Rows
            if (showGenreRows) {
                state.genreRowsData.entries.forEachIndexed { index, (genreName, items) ->
                    if (items.isNotEmpty()) {
                        val color = MobileRowColors.genreColors[index % MobileRowColors.genreColors.size]
                        add(
                            MobileRowData(
                                key = "genre_$genreName",
                                title = genreName,
                                items = items,
                                isWideCard = false,
                                accentColor = color,
                            ),
                        )
                    }
                }
            }
        }
    }

    // Key the entire layout to configuration to force redraw on screen changes
    key(configKey) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (state.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null && state.featuredItems.isEmpty() && rows.isEmpty()) {
                // Error state with retry
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(
                            text = state.error ?: "Something went wrong",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                // Main content - keyed to configuration for responsive updates
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // Hero section
                    item(key = "hero") {
                        MobileHeroSection(
                            featuredItems = state.featuredItems,
                            jellyfinClient = jellyfinClient,
                            onItemClick = onItemClick,
                            onPlayClick = onPlayClick, // Now goes to player
                        )
                    }

                    // Content rows
                    items(rows, key = { it.key }) { rowData ->
                        MobileContentRow(
                            title = rowData.title,
                            items = rowData.items,
                            jellyfinClient = jellyfinClient,
                            onItemClick = onItemClick,
                            onItemLongClick = handleItemLongClick,
                            accentColor = rowData.accentColor,
                            isWideCard = rowData.isWideCard,
                            isUpcomingEpisodes = rowData.isUpcomingEpisodes,
                        )
                    }

                    // Recent Requests row (from Seerr)
                    if (showSeerrRecentRequests && state.recentRequests.isNotEmpty() && seerrClient != null) {
                        item(key = "recent_requests") {
                            MobileSeerrRequestRow(
                                title = "Recent Requests",
                                requests = state.recentRequests,
                                seerrClient = seerrClient,
                                accentColor = androidx.compose.ui.graphics.Color(SeerrColors.GREEN),
                                onRequestClick = { request ->
                                    request.media?.let { media ->
                                        onSeerrMediaClick(media.mediaType ?: "movie", media.tmdbId ?: 0)
                                    }
                                },
                            )
                        }
                    }
                }

                // Popup menu for long-press
                popupMenuParams?.let { params ->
                    PopupMenu(
                        params = params,
                        onDismiss = { popupMenuParams = null },
                    )
                }

                // Media Information bottom sheet
                mediaInfoItem?.let { item ->
                    MediaInfoBottomSheet(
                        item = item,
                        onDismiss = { mediaInfoItem = null },
                    )
                }
            }

            // Top bar overlay (always visible over hero) - zIndex ensures touch events work
            MobileTopBar(
                selectedItem = selectedNavItem,
                onItemSelected = handleNavSelection,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f),
            )
        }
    }
}

/**
 * Row displaying recent Seerr requests with poster cards and status badges.
 */
@Composable
private fun MobileSeerrRequestRow(
    title: String,
    requests: List<SeerrRequest>,
    seerrClient: SeerrClient,
    accentColor: Color,
    onRequestClick: (SeerrRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        // Row header
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp)),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // LazyRow of request cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(requests, key = { it.id }) { request ->
                MobileSeerrRequestCard(
                    request = request,
                    seerrClient = seerrClient,
                    onClick = { onRequestClick(request) },
                )
            }
        }
    }
}

/**
 * Card displaying a Seerr request with poster image and status badge.
 * Lazily fetches media details to get the poster path.
 */
@Composable
private fun MobileSeerrRequestCard(
    request: SeerrRequest,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaType = request.media?.mediaType
    val tmdbId = request.media?.tmdbId

    // Lazily fetch media details to get poster path
    var mediaDetails by remember { mutableStateOf<SeerrMedia?>(null) }

    androidx.compose.runtime.LaunchedEffect(tmdbId, mediaType) {
        if (tmdbId != null && mediaType != null) {
            val result = if (mediaType == "tv") {
                seerrClient.getTVShow(tmdbId)
            } else {
                seerrClient.getMovie(tmdbId)
            }
            result.onSuccess { mediaDetails = it }
        }
    }

    val posterUrl = mediaDetails?.posterPath?.let { seerrClient.getPosterUrl(it) }
    val statusColor = when (request.status) {
        SeerrRequestStatus.PENDING_APPROVAL -> Color(SeerrColors.YELLOW)
        SeerrRequestStatus.APPROVED -> Color(SeerrColors.GREEN)
        SeerrRequestStatus.DECLINED -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.size(width = 110.dp, height = 165.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster image
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = mediaDetails?.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                // Placeholder while loading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        text = mediaDetails?.displayTitle?.take(1) ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Status badge overlay
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(4.dp)
                    .background(statusColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = request.statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

