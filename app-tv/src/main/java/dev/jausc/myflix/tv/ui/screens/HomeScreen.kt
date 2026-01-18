@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "LabeledExpression",
    "LambdaParameterInRestartableEffect",
    "ModifierMissing",
    "ParameterNaming",
    "ComposableParamOrder",
    "MutableStateAutoboxing",
)

package dev.jausc.myflix.tv.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.ui.PlayAllData
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.PlayQueueManager
import dev.jausc.myflix.core.player.QueueItem
import dev.jausc.myflix.core.player.QueueSource
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrColors
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRequest
import dev.jausc.myflix.core.seerr.SeerrRequestStatus
import dev.jausc.myflix.tv.TvPreferences
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.ExitConfirmationDialog
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.HeroBackdropLayer
import dev.jausc.myflix.tv.ui.components.HeroSection
import dev.jausc.myflix.tv.ui.components.HomeDialogActions
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.TvTextButton
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.buildHomeDialogItems
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    preferences: TvPreferences,
    onLibraryClick: (libraryId: String, libraryName: String, collectionType: String?) -> Unit,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onDiscoverClick: () -> Unit = {},
    onCollectionsClick: () -> Unit = {},
    onUniversesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    seerrClient: SeerrClient? = null,
    onSeerrMediaClick: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> },
    showUniversesInNav: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Exit confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    // Handle back button press - show exit confirmation
    BackHandler(enabled = !showExitDialog) {
        showExitDialog = true
    }

    // ViewModel with manual DI
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(jellyfinClient, preferences, seerrClient, HeroContentBuilder.defaultConfig),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Collect preference values for UI
    val hideWatchedFromRecent by viewModel.hideWatchedFromRecent.collectAsState()
    val showSeasonPremieres by viewModel.showSeasonPremieres.collectAsState()
    val showGenreRows by viewModel.showGenreRows.collectAsState()
    val showCollections by viewModel.showCollections.collectAsState()
    val showSuggestions by viewModel.showSuggestions.collectAsState()
    val showSeerrRecentRequests by viewModel.showSeerrRecentRequests.collectAsState()

    // Navigation state
    var selectedNavItem by remember { mutableStateOf(NavItem.HOME) }

    // Long-press dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    // Dialog actions
    val dialogActions = remember(scope, viewModel) {
        HomeDialogActions(
            onGoTo = onItemClick,
            onPlay = onPlayClick,
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

    // Handle nav item selection
    val handleNavSelection: (NavItem) -> Unit = { item ->
        selectedNavItem = item
        when (item) {
            NavItem.HOME -> Unit // Already on home
            NavItem.SEARCH -> onSearchClick()
            NavItem.MOVIES ->
                LibraryFinder.findMoviesLibrary(state.libraries)?.let {
                    onLibraryClick(it.id, it.name, it.collectionType)
                }
            NavItem.SHOWS ->
                LibraryFinder.findShowsLibrary(state.libraries)?.let {
                    onLibraryClick(it.id, it.name, it.collectionType)
                }
            NavItem.DISCOVER -> onDiscoverClick()
            NavItem.COLLECTIONS -> onCollectionsClick()
            NavItem.UNIVERSES -> onUniversesClick()
            NavItem.SETTINGS -> onSettingsClick()
        }
    }

    // Focus requesters
    val heroPlayFocusRequester = remember { FocusRequester() }
    val firstRowFocusRequester = remember { FocusRequester() }
    val homeButtonFocusRequester = remember { FocusRequester() }

    // Track current backdrop URL for dynamic background colors
    var currentBackdropUrl by remember { mutableStateOf<String?>(null) }

    // Extract gradient colors from current backdrop image
    val gradientColors = rememberGradientColors(currentBackdropUrl)

    // Track if we've focused the hero
    val contentId = state.featuredItems.firstOrNull()?.id
    var heroFocused by remember(contentId) { mutableStateOf(false) }

    // Request focus on hero when content becomes ready (but not when exit dialog is shown)
    LaunchedEffect(state.contentReady, showExitDialog) {
        if (state.contentReady && !heroFocused && !showExitDialog) {
            delay(100)
            try {
                heroPlayFocusRequester.requestFocus()
                heroFocused = true
            } catch (_: Exception) {
                delay(200)
                try {
                    heroPlayFocusRequester.requestFocus()
                    heroFocused = true
                } catch (_: Exception) {
                }
            }
        }
    }

    // Use Box to layer DynamicBackground behind everything
    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic gradient background (behind everything)
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        // Main Content (full screen, nav bar overlays on top)
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Show loading until we have hero content
            if (!state.contentReady && state.error == null) {
                TvLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (state.error != null && state.featuredItems.isEmpty()) {
                // Error state with retry
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                ) {
                    Text(
                        text = state.error ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.Error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TvTextButton(
                        text = "Retry",
                        onClick = { viewModel.refresh() },
                    )
                }
            } else {
                // Key forces complete recomposition when content changes
                key(state.featuredItems.firstOrNull()?.id) {
                    HomeContent(
                        featuredItems = state.featuredItems,
                        nextUp = state.nextUp,
                        continueWatching = state.continueWatching,
                        recentEpisodes = state.recentEpisodes,
                        recentShows = state.recentShows,
                        recentMovies = state.recentMovies,
                        seasonPremieres = state.seasonPremieres,
                        pinnedCollectionData = state.pinnedCollectionsData,
                        suggestions = state.suggestions,
                        genreRowsData = state.genreRowsData,
                        recentRequests = state.recentRequests,
                        showSeasonPremieres = showSeasonPremieres,
                        showGenreRows = showGenreRows,
                        showCollections = showCollections,
                        showSuggestions = showSuggestions,
                        showSeerrRecentRequests = showSeerrRecentRequests,
                        jellyfinClient = jellyfinClient,
                        seerrClient = seerrClient,
                        onItemClick = onItemClick,
                        onPlayClick = onPlayClick,
                        onItemLongClick = { item ->
                            dialogParams = DialogParams(
                                title = item.name,
                                items = buildHomeDialogItems(item, dialogActions),
                                fromLongClick = true,
                            )
                        },
                        onSeerrMediaClick = onSeerrMediaClick,
                        hideWatchedFromRecent = hideWatchedFromRecent,
                        heroPlayFocusRequester = heroPlayFocusRequester,
                        firstRowFocusRequester = firstRowFocusRequester,
                        homeButtonFocusRequester = homeButtonFocusRequester,
                        onBackdropUrlChanged = { url -> currentBackdropUrl = url },
                        onItemFocused = { },
                        // Top navigation config
                        selectedNavItem = selectedNavItem,
                        onNavItemSelected = handleNavSelection,
                        showUniversesInNav = showUniversesInNav,
                    )
                }
            }
        }
    }

    // Long-press context menu dialog
    dialogParams?.let { params ->
        DialogPopup(
            params = params,
            onDismissRequest = { dialogParams = null },
        )
    }

    // Media Information dialog
    mediaInfoItem?.let { item ->
        MediaInfoDialog(
            item = item,
            onDismiss = { mediaInfoItem = null },
        )
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirmExit = {
                (context as? Activity)?.finish()
            },
            onCancel = {
                showExitDialog = false
            },
        )
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
    val accentColor: Color,
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
    seasonPremieres: List<JellyfinItem>,
    pinnedCollectionData: Map<String, Pair<String, List<JellyfinItem>>>,
    suggestions: List<JellyfinItem>,
    genreRowsData: Map<String, List<JellyfinItem>>,
    recentRequests: List<SeerrRequest>,
    showSeasonPremieres: Boolean,
    showGenreRows: Boolean,
    showCollections: Boolean,
    showSuggestions: Boolean,
    showSeerrRecentRequests: Boolean,
    jellyfinClient: JellyfinClient,
    seerrClient: SeerrClient?,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onItemLongClick: (JellyfinItem) -> Unit,
    onSeerrMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    hideWatchedFromRecent: Boolean = false,
    heroPlayFocusRequester: FocusRequester = remember { FocusRequester() },
    firstRowFocusRequester: FocusRequester = remember { FocusRequester() },
    homeButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    onBackdropUrlChanged: (String?) -> Unit = {},
    onItemFocused: (JellyfinItem?) -> Unit = {},
    // Top navigation
    selectedNavItem: NavItem = NavItem.HOME,
    onNavItemSelected: (NavItem) -> Unit = {},
    showUniversesInNav: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()

    // Filter watched items from Recently Added rows
    val filteredRecentEpisodes = if (hideWatchedFromRecent) {
        recentEpisodes.filter { it.userData?.played != true }
    } else {
        recentEpisodes
    }

    val filteredRecentShows = if (hideWatchedFromRecent) {
        recentShows.filter { it.userData?.played != true }
    } else {
        recentShows
    }

    val filteredRecentMovies = if (hideWatchedFromRecent) {
        recentMovies.filter { it.userData?.played != true }
    } else {
        recentMovies
    }

    val filteredFeaturedItems = if (hideWatchedFromRecent) {
        featuredItems.filter { it.userData?.played != true }
    } else {
        featuredItems
    }

    // Get IDs of items in Continue Watching to filter from Next Up
    val continueWatchingIds = remember(continueWatching) {
        continueWatching.map { it.id }.toSet()
    }

    // Filter Next Up to exclude items already in Continue Watching
    val filteredNextUp = remember(nextUp, continueWatchingIds) {
        nextUp.filter { it.id !in continueWatchingIds }
    }

    // Genre row colors for variety
    val genreColors = listOf(
        Color(0xFF9C27B0), // Purple
        Color(0xFFE91E63), // Pink
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF9800), // Orange
        Color(0xFF4CAF50), // Green
        Color(0xFF673AB7), // Deep Purple
    )

    // Pinned collection colors
    val pinnedCollectionColors = listOf(
        Color(0xFF34D399), // Emerald
        Color(0xFF60A5FA), // Blue
        Color(0xFFFBBF24), // Amber
        Color(0xFFF472B6), // Pink
        Color(0xFFA78BFA), // Purple
    )

    // Build list of row data - Continue Watching first, then Next Up
    val rows = remember(
        filteredNextUp, continueWatching, filteredRecentEpisodes, filteredRecentShows, filteredRecentMovies,
        seasonPremieres, pinnedCollectionData, suggestions, genreRowsData,
        showSeasonPremieres, showGenreRows, showCollections, showSuggestions,
    ) {
        buildList {
            if (continueWatching.isNotEmpty()) {
                add(
                    RowData("continue", "Continue Watching", continueWatching, false, TvColors.BluePrimary),
                )
            }
            if (filteredNextUp.isNotEmpty()) {
                add(
                    RowData("next_up", "Next Up", filteredNextUp, false, TvColors.BlueAccent),
                )
            }

            if (filteredRecentEpisodes.isNotEmpty()) {
                add(
                    RowData("recent_ep", "Recently Added Episodes", filteredRecentEpisodes, false, TvColors.Success),
                )
            }
            if (filteredRecentShows.isNotEmpty()) {
                add(
                    RowData("recent_shows", "Recently Added Shows", filteredRecentShows, false, Color(0xFFFBBF24)),
                )
            }

            // Upcoming Episodes (after Recently Added Shows)
            if (showSeasonPremieres && seasonPremieres.isNotEmpty()) {
                add(RowData("premieres", "Upcoming Episodes", seasonPremieres, false, Color(0xFF60A5FA)))
            }

            if (filteredRecentMovies.isNotEmpty()) {
                add(
                    RowData("recent_movies", "Recently Added Movies", filteredRecentMovies, false, TvColors.BluePrimary),
                )
            }

            // Suggestions (above pinned collections and genres)
            if (showSuggestions && suggestions.isNotEmpty()) {
                add(RowData("suggestions", "You Might Like", suggestions, false, Color(0xFFF472B6)))
            }

            // Pinned Collections (individual rows, not a single "Collections" row)
            if (showCollections) {
                pinnedCollectionData.entries.forEachIndexed { index, (collectionId, data) ->
                    val (name, items) = data
                    if (items.isNotEmpty()) {
                        val color = pinnedCollectionColors[index % pinnedCollectionColors.size]
                        add(RowData("collection_$collectionId", name, items, false, color))
                    }
                }
            }

            // Genre Rows
            if (showGenreRows) {
                genreRowsData.entries.forEachIndexed { index, (genreName, items) ->
                    if (items.isNotEmpty()) {
                        val color = genreColors[index % genreColors.size]
                        add(RowData("genre_$genreName", genreName, items, false, color))
                    }
                }
            }
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
            } catch (_: Exception) {
            }
            delay(50)
            listState.scrollToItem(index)
            firstFocused = true
        }
    }

    // KEY PATTERN: Restore focus and scroll when rows load or position changes
    // Keys ensure this re-runs when content loads or focus state changes
    LaunchedEffect(firstFocused, rows.size, position.row) {
        if (firstFocused && rows.isNotEmpty()) {
            val index = position.row.coerceIn(0, rows.size - 1)
            try {
                rowFocusRequesters.getOrNull(index)?.requestFocus()
            } catch (_: Exception) {
            }
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
                .align(Alignment.TopEnd),
        )

        // Layer 2: Hero info + Content rows
        Column(modifier = Modifier.fillMaxSize()) {
            // Hero Section - media info only (backdrop is in layer 1)
            if (filteredFeaturedItems.isNotEmpty() || previewItem != null) {
                HeroSection(
                    featuredItems = filteredFeaturedItems,
                    jellyfinClient = jellyfinClient,
                    onItemClick = onItemClick,
                    onPlayClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.37f),
                    previewItem = previewItem,
                    playButtonFocusRequester = heroPlayFocusRequester,
                    downFocusRequester = firstRowFocusRequester,
                    onCurrentItemChanged = { item, backdropUrl ->
                        heroDisplayItem = item
                        onBackdropUrlChanged(backdropUrl)
                    },
                    onPreviewClear = clearPreviewAndScrollToTop,
                    navBarVisible = true, // Nav bar is always visible
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
                            position.row == 0
                        ) {
                            previewItem = null
                            coroutineScope.launch {
                                listState.scrollToItem(0)
                                try {
                                    heroPlayFocusRequester.requestFocus()
                                } catch (_: Exception) {
                                }
                            }
                            true
                        } else {
                            false
                        }
                    }
                    .focusProperties {
                        up = heroPlayFocusRequester
                    },
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
                        onItemLongClick = onItemLongClick,
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
                            .animateItem(),
                    )
                }

                // Recent Requests row (from Seerr)
                if (showSeerrRecentRequests && recentRequests.isNotEmpty() && seerrClient != null) {
                    item(key = "recent_requests") {
                        SeerrRequestRow(
                            title = "Recent Requests",
                            requests = recentRequests,
                            seerrClient = seerrClient,
                            accentColor = Color(SeerrColors.GREEN),
                            onRequestClick = { request ->
                                request.media?.let { media ->
                                    onSeerrMediaClick(media.mediaType ?: "movie", media.tmdbId ?: 0)
                                }
                            },
                        )
                    }
                }
            }
        } // End Column

        // Layer 3: Top Navigation Bar (always visible)
        TopNavigationBarPopup(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected,
            showUniverses = showUniversesInNav,
            contentFocusRequester = heroPlayFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
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
    onItemLongClick: (JellyfinItem) -> Unit,
    onCardFocused: (Int, JellyfinItem) -> Unit,
    firstCardFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester? = null, // For first row: UP goes to hero
    modifier: Modifier = Modifier,
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
            .focusGroup(),
    ) {
        // Row header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 10.dp, end = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(accentColor, shape = MaterialTheme.shapes.small),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }

        // LazyRow with focusRestorer (exact Wholphin pattern)
        LazyRow(
            state = lazyRowState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer(firstFocus),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                // First card gets focusRequester (exact Wholphin pattern)
                val cardModifier = if (index == 0) {
                    Modifier
                        .focusRequester(firstFocus)
                        .then(
                            if (firstCardFocusRequester != null) {
                                Modifier.focusRequester(firstCardFocusRequester)
                            } else {
                                Modifier
                            },
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
                            jellyfinClient.getBackdropUrl(
                                item.id,
                                item.backdropImageTags?.firstOrNull(),
                                maxWidth = 600,
                            )
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
                        onLongClick = { onItemLongClick(item) },
                        modifier = focusModifier,
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
                        onLongClick = { onItemLongClick(item) },
                        modifier = focusModifier,
                    )
                }
            }
        }
    }
}

/**
 * Row displaying recent Seerr requests with poster cards and status badges.
 */
@Composable
private fun SeerrRequestRow(
    title: String,
    requests: List<SeerrRequest>,
    seerrClient: SeerrClient,
    accentColor: Color,
    onRequestClick: (SeerrRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .padding(vertical = 8.dp)
            .focusGroup(),
    ) {
        // Row header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 10.dp, end = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(accentColor, shape = MaterialTheme.shapes.small),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }

        // LazyRow of request cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(requests, key = { it.id }) { request ->
                SeerrRequestCard(
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
private fun SeerrRequestCard(
    request: SeerrRequest,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaType = request.media?.mediaType
    val tmdbId = request.media?.tmdbId

    // Lazily fetch media details to get poster path
    var mediaDetails by remember { mutableStateOf<SeerrMedia?>(null) }

    LaunchedEffect(tmdbId, mediaType) {
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
        modifier = modifier.size(width = 140.dp, height = 210.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
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
                        .background(TvColors.SurfaceElevated, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mediaDetails?.displayTitle?.take(1) ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TvColors.TextSecondary,
                    )
                }
            }

            // Status badge overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
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

