@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrDiscoverRow
import dev.jausc.myflix.core.seerr.SeerrGenreRow
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrNetworkRow
import dev.jausc.myflix.core.seerr.SeerrStudioRow
import dev.jausc.myflix.core.viewmodel.SeerrHomeViewModel
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsBackdrop
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsFadePreset
import dev.jausc.myflix.tv.ui.screens.discover.components.DiscoverCarousel
import dev.jausc.myflix.tv.ui.screens.discover.components.DiscoverGenreBrowseRow
import dev.jausc.myflix.tv.ui.screens.discover.components.DiscoverHeroSection
import dev.jausc.myflix.tv.ui.screens.discover.components.DiscoverNetworkBrowseRow
import dev.jausc.myflix.tv.ui.screens.discover.components.DiscoverStudioBrowseRow
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Sealed interface for the ordered row items in the Discover screen LazyColumn.
 * Allows interleaving media carousels with browse rows in a specific order.
 */
private sealed interface DiscoverRowItem {
    val key: String

    data class Carousel(
        val category: MediaCategory,
        val row: SeerrDiscoverRow,
    ) : DiscoverRowItem {
        override val key: String = category.name
    }

    data class GenreBrowse(
        override val key: String,
        val genreRow: SeerrGenreRow,
        val mediaType: String,
        val accentColor: Color,
    ) : DiscoverRowItem

    data class StudioBrowse(
        val studioRow: SeerrStudioRow,
    ) : DiscoverRowItem {
        override val key: String = "studios"
    }

    data class NetworkBrowse(
        val networkRow: SeerrNetworkRow,
    ) : DiscoverRowItem {
        override val key: String = "networks"
    }
}

/**
 * Main Discover home screen for Discover V2.
 *
 * Uses the same 3-layer pattern as HomeScreen:
 * - Layer 0: Ken Burns backdrop (80% width/height, TopEnd aligned)
 * - Layer 1: Carousel rows below hero (clipped, explicit scroll on focus)
 * - Layer 2: Hero text overlay (fixed height at top)
 *
 * Row order: Trending, Popular Movies, Movie Genres, Upcoming Movies, Studios,
 *            Popular TV, TV Genres, Upcoming TV, Networks
 *
 * @param viewModel The SeerrHomeViewModel providing media data
 * @param onMediaClick Callback when a media item is clicked
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverHomeScreen(
    viewModel: SeerrHomeViewModel,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onNavigateGenre: (mediaType: String, genreId: Int, genreName: String) -> Unit = { _, _, _ -> },
    onNavigateStudio: (studioId: Int, studioName: String) -> Unit = { _, _ -> },
    onNavigateNetwork: (networkId: Int, networkName: String) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val focusedRatings by viewModel.focusedRatings.collectAsState()

    // Track the currently focused media for the hero section
    var focusedMedia by remember { mutableStateOf<SeerrMedia?>(null) }

    // Build backdrop URL from focused media
    val backdropUrl = remember(focusedMedia?.id) {
        focusedMedia?.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
    }

    // Fetch ratings when focused media changes
    LaunchedEffect(focusedMedia?.id) {
        focusedMedia?.let { media ->
            viewModel.fetchRatingsForMedia(
                tmdbId = media.tmdbId ?: media.id,
                isMovie = media.isMovie,
            )
        }
    }

    // Stable map of FocusRequesters per category (pattern from CLAUDE.md)
    val rowFocusRequesters = remember { mutableStateMapOf<MediaCategory, FocusRequester>() }
    fun getRowFocusRequester(category: MediaCategory): FocusRequester =
        rowFocusRequesters.getOrPut(category) { FocusRequester() }

    // Focus management - use first row's requester for exit focus
    val firstRowRequester = getRowFocusRequester(MediaCategory.TRENDING)
    val updateExitFocus = rememberExitFocusRegistry(firstRowRequester)

    // Lazy list state for vertical scrolling
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Track last focused row for explicit scroll management (HomeScreen pattern)
    var lastFocusedRow by remember { mutableIntStateOf(-1) }

    // Track pagination state for each row
    val rowPages = remember { mutableStateMapOf<String, Int>() }

    // Request focus when content loads, and set initial hero media
    LaunchedEffect(uiState.rows) {
        if (uiState.rows.isNotEmpty()) {
            // Set initial hero to first item of trending row
            if (focusedMedia == null) {
                uiState.trendingRow?.items?.firstOrNull()?.let { firstMedia ->
                    focusedMedia = firstMedia
                }
            }
            // Short delay to allow composition to complete
            kotlinx.coroutines.delay(100)
            try {
                getRowFocusRequester(MediaCategory.TRENDING).requestFocus()
            } catch (_: Exception) {
                // Focus request failed, ignore
            }
        }
    }

    // Build the ordered list of rows:
    // Trending, Popular Movies, Movie Genres, Upcoming Movies, Studios,
    // Popular TV, TV Genres, Upcoming TV, Networks
    val orderedRows = remember(uiState.rows, uiState.genreRows, uiState.studiosRow, uiState.networksRow) {
        buildList {
            uiState.trendingRow?.let {
                add(DiscoverRowItem.Carousel(MediaCategory.TRENDING, it))
            }
            uiState.popularMoviesRow?.let {
                add(DiscoverRowItem.Carousel(MediaCategory.POPULAR_MOVIES, it))
            }
            uiState.movieGenresRow?.let {
                add(DiscoverRowItem.GenreBrowse("genre_movies", it, "movie", Color(0xFF8B5CF6)))
            }
            uiState.upcomingMoviesRow?.let {
                add(DiscoverRowItem.Carousel(MediaCategory.UPCOMING_MOVIES, it))
            }
            uiState.studiosRow?.let {
                add(DiscoverRowItem.StudioBrowse(it))
            }
            uiState.popularTvRow?.let {
                add(DiscoverRowItem.Carousel(MediaCategory.POPULAR_TV, it))
            }
            uiState.tvGenresRow?.let {
                add(DiscoverRowItem.GenreBrowse("genre_tv", it, "tv", Color(0xFFA855F7)))
            }
            uiState.upcomingTvRow?.let {
                add(DiscoverRowItem.Carousel(MediaCategory.UPCOMING_TV, it))
            }
            uiState.networksRow?.let {
                add(DiscoverRowItem.NetworkBrowse(it))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        when {
            // Not authenticated - show message
            !isAuthenticated -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = "Seerr not configured",
                            modifier = Modifier.size(64.dp),
                            tint = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Seerr not configured",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure Seerr in Settings to discover new content",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Loading state
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            // Error state
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = TvColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.errorMessage ?: "Failed to load content",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary,
                        )
                    }
                }
            }

            // Content loaded - 3-layer layout matching HomeScreen
            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val heroHeight = maxHeight * 0.37f
                    val heroImageWidth = maxWidth * 0.8f
                    val heroImageHeight = maxHeight * 0.8f

                    // Disable automatic focus scrolling; we snap rows explicitly on focus.
                    val noOpBringIntoViewSpec = remember {
                        object : BringIntoViewSpec {
                            @ExperimentalFoundationApi
                            override fun calculateScrollDistance(
                                offset: Float,
                                size: Float,
                                containerSize: Float,
                            ): Float = 0f
                        }
                    }

                    // Layer 0: Ken Burns backdrop sized and aligned like HomeScreen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f),
                    ) {
                        KenBurnsBackdrop(
                            imageUrl = backdropUrl,
                            fadePreset = KenBurnsFadePreset.HOME_SCREEN,
                            modifier = Modifier
                                .size(heroImageWidth, heroImageHeight)
                                .align(Alignment.TopEnd),
                        )
                    }

                    // Layer 1: All rows below hero, clipped
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides noOpBringIntoViewSpec,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = heroHeight)
                                .clipToBounds()
                                .zIndex(1f),
                        ) {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 300.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(
                                    items = orderedRows,
                                    key = { _, item -> item.key },
                                ) { index, rowItem ->
                                    when (rowItem) {
                                        is DiscoverRowItem.Carousel -> {
                                            val rowFocusRequester = getRowFocusRequester(rowItem.category)
                                            val currentPage = rowPages[rowItem.row.key] ?: 1

                                            DiscoverCarousel(
                                                category = rowItem.category,
                                                items = rowItem.row.items,
                                                onItemClick = { media ->
                                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (index == 0) {
                                                            Modifier.focusRequester(firstRowRequester)
                                                        } else {
                                                            Modifier
                                                        },
                                                    ),
                                                isLoading = false,
                                                onLoadMore = {
                                                    val nextPage = currentPage + 1
                                                    rowPages[rowItem.row.key] = nextPage
                                                    viewModel.loadMoreForRow(rowItem.row.key, nextPage)
                                                },
                                                onRowFocused = {
                                                    if (index == lastFocusedRow) return@DiscoverCarousel
                                                    lastFocusedRow = index
                                                    updateExitFocus(rowFocusRequester)
                                                    val info = listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == index }
                                                    if (info != null && abs(info.offset) <= 2) {
                                                        return@DiscoverCarousel
                                                    }
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(index, 0)
                                                    }
                                                },
                                                onItemFocused = { media ->
                                                    focusedMedia = media
                                                },
                                                focusRequester = rowFocusRequester,
                                                accentColor = getAccentColorForCategory(rowItem.category),
                                            )
                                        }

                                        is DiscoverRowItem.GenreBrowse -> {
                                            DiscoverGenreBrowseRow(
                                                title = rowItem.genreRow.title,
                                                genres = rowItem.genreRow.genres,
                                                onGenreClick = { genre ->
                                                    onNavigateGenre(rowItem.mediaType, genre.id, genre.name)
                                                },
                                                accentColor = rowItem.accentColor,
                                                modifier = Modifier.onFocusChanged { state ->
                                                    if (state.hasFocus && index != lastFocusedRow) {
                                                        lastFocusedRow = index
                                                        coroutineScope.launch {
                                                            listState.scrollToItem(index, 0)
                                                        }
                                                    }
                                                },
                                            )
                                        }

                                        is DiscoverRowItem.StudioBrowse -> {
                                            DiscoverStudioBrowseRow(
                                                title = rowItem.studioRow.title,
                                                studios = rowItem.studioRow.studios,
                                                onStudioClick = { studio ->
                                                    onNavigateStudio(studio.id, studio.name)
                                                },
                                                modifier = Modifier.onFocusChanged { state ->
                                                    if (state.hasFocus && index != lastFocusedRow) {
                                                        lastFocusedRow = index
                                                        coroutineScope.launch {
                                                            listState.scrollToItem(index, 0)
                                                        }
                                                    }
                                                },
                                            )
                                        }

                                        is DiscoverRowItem.NetworkBrowse -> {
                                            DiscoverNetworkBrowseRow(
                                                title = rowItem.networkRow.title,
                                                networks = rowItem.networkRow.networks,
                                                onNetworkClick = { network ->
                                                    onNavigateNetwork(network.id, network.name)
                                                },
                                                modifier = Modifier.onFocusChanged { state ->
                                                    if (state.hasFocus && index != lastFocusedRow) {
                                                        lastFocusedRow = index
                                                        coroutineScope.launch {
                                                            listState.scrollToItem(index, 0)
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Layer 2: Hero text overlay at top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight)
                            .zIndex(2f),
                    ) {
                        DiscoverHeroSection(
                            media = focusedMedia,
                            ratings = focusedRatings,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns the accent color for a given media category.
 */
@Composable
private fun getAccentColorForCategory(category: MediaCategory): Color {
    return when (category) {
        MediaCategory.TRENDING -> TvColors.BlueAccent
        MediaCategory.POPULAR_MOVIES -> TvColors.BluePrimary
        MediaCategory.POPULAR_TV -> TvColors.BlueLight
        MediaCategory.UPCOMING_MOVIES -> TvColors.Success
        MediaCategory.UPCOMING_TV -> TvColors.Success.copy(alpha = 0.8f)
        else -> TvColors.BluePrimary
    }
}
