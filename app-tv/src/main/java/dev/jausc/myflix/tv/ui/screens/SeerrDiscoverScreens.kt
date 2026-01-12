@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.ui.SeerrActionDivider
import dev.jausc.myflix.core.common.ui.SeerrActionItem
import dev.jausc.myflix.core.common.ui.SeerrMediaActions
import dev.jausc.myflix.core.common.ui.buildSeerrActionItems
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrDiscoverResult
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.filterDiscoverable
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogItemDivider
import dev.jausc.myflix.tv.ui.components.DialogItemEntry
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Filter configuration for discover screens.
 */
data class DiscoverFilterConfig(
    val showMediaTypeFilter: Boolean = false,
    val showGenreFilter: Boolean = false,
    val showReleaseStatusFilter: Boolean = false,
    val defaultMediaType: MediaTypeFilter = MediaTypeFilter.ALL,
)

enum class MediaTypeFilter(val label: String, val apiValue: String?) {
    ALL("All", null),
    MOVIES("Movies", "movie"),
    TV_SHOWS("TV Shows", "tv"),
}

enum class ReleaseStatusFilter(val label: String) {
    ALL("All"),
    RELEASED("Released"),
    UPCOMING("Upcoming"),
}

enum class SortFilter(val label: String, val movieValue: String, val tvValue: String) {
    POPULARITY_DESC("Most Popular", "popularity.desc", "popularity.desc"),
    POPULARITY_ASC("Least Popular", "popularity.asc", "popularity.asc"),
    RATING_DESC("Highest Rated", "vote_average.desc", "vote_average.desc"),
    RATING_ASC("Lowest Rated", "vote_average.asc", "vote_average.asc"),
    RELEASE_DESC("Newest First", "primary_release_date.desc", "first_air_date.desc"),
    RELEASE_ASC("Oldest First", "primary_release_date.asc", "first_air_date.asc"),
    TITLE_ASC("Title A-Z", "title.asc", "name.asc"),
    TITLE_DESC("Title Z-A", "title.desc", "name.desc"),
}

@Composable
fun SeerrDiscoverTrendingScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaGridScreen(
        title = "Trending",
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showMediaTypeFilter = true,
            showGenreFilter = true,
            showReleaseStatusFilter = true,
        ),
        loadItems = { page, mediaType, genreIds, releaseStatus, _, _, _, _ ->
            loadTrendingWithFilters(seerrClient, page, mediaType, genreIds, releaseStatus)
        },
    )
}

@Composable
fun SeerrDiscoverMoviesScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaGridScreen(
        title = "Discover Movies",
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            showReleaseStatusFilter = true,
            defaultMediaType = MediaTypeFilter.MOVIES,
        ),
        loadItems = { page, _, genreIds, releaseStatus, sort, rating, fromYear, toYear ->
            loadMoviesWithFilters(seerrClient, page, genreIds, releaseStatus, sort, rating, fromYear, toYear)
        },
        genreMediaType = "movie",
    )
}

@Composable
fun SeerrDiscoverTvScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaGridScreen(
        title = "Discover TV",
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            showReleaseStatusFilter = true,
            defaultMediaType = MediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, genreIds, releaseStatus, sort, rating, fromYear, toYear ->
            loadTvWithFilters(seerrClient, page, genreIds, releaseStatus, sort, rating, fromYear, toYear)
        },
        genreMediaType = "tv",
    )
}

@Composable
fun SeerrDiscoverUpcomingMoviesScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaGridScreen(
        title = "Upcoming Movies",
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            defaultMediaType = MediaTypeFilter.MOVIES,
        ),
        loadItems = { page, _, genreIds, _, _, _, _, _ ->
            loadUpcomingWithFilters(seerrClient, page, MediaTypeFilter.MOVIES, genreIds)
        },
        genreMediaType = "movie",
    )
}

@Composable
fun SeerrDiscoverUpcomingTvScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaGridScreen(
        title = "Upcoming TV",
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            defaultMediaType = MediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, genreIds, _, _, _, _, _ ->
            loadUpcomingWithFilters(seerrClient, page, MediaTypeFilter.TV_SHOWS, genreIds)
        },
        genreMediaType = "tv",
    )
}

@Composable
fun SeerrDiscoverByGenreScreen(
    seerrClient: SeerrClient,
    mediaType: String,
    genreId: Int,
    genreName: String,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaGridScreen(
        title = genreName,
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showReleaseStatusFilter = true,
            defaultMediaType = if (mediaType == "movie") MediaTypeFilter.MOVIES else MediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, _, releaseStatus, sort, rating, fromYear, toYear ->
            if (mediaType == "movie") {
                loadMoviesWithFilters(seerrClient, page, setOf(genreId), releaseStatus, sort, rating, fromYear, toYear)
            } else {
                loadTvWithFilters(seerrClient, page, setOf(genreId), releaseStatus, sort, rating, fromYear, toYear)
            }
        },
        genreMediaType = mediaType,
    )
}

@Composable
private fun SeerrMediaGridScreen(
    title: String,
    onBack: () -> Unit,
    loadItems: suspend (page: Int) -> Result<SeerrDiscoverResult>,
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val firstItemFocusRequester = remember { FocusRequester() }

    // Dialog state for long-press context menu
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }

    // Seerr actions for context menu
    val seerrActions = remember(onMediaClick, scope, seerrClient) {
        SeerrMediaActions(
            onGoTo = { mediaType, tmdbId -> onMediaClick(mediaType, tmdbId) },
            onRequest = { media ->
                scope.launch {
                    if (media.isMovie) {
                        seerrClient.requestMovie(media.tmdbId ?: media.id)
                    } else {
                        seerrClient.requestTVShow(media.tmdbId ?: media.id)
                    }
                }
            },
            onBlacklist = { media ->
                scope.launch {
                    seerrClient.addToBlacklist(media.tmdbId ?: media.id, media.mediaType)
                }
            },
        )
    }

    suspend fun loadPage(pageToLoad: Int, append: Boolean) {
        if (append) {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        errorMessage = null

        loadItems(pageToLoad)
            .onSuccess { result ->
                val filtered = result.results.filterDiscoverable()
                items = if (append) {
                    (items + filtered).distinctBy { "${it.mediaType}-${it.id}" }
                } else {
                    filtered
                }
                page = result.page
                totalPages = result.totalPages
            }
            .onFailure { error ->
                if (!append) {
                    errorMessage = error.message ?: "Failed to load content"
                }
            }

        if (append) {
            isLoadingMore = false
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(refreshTrigger) {
        items = emptyList()
        page = 1
        totalPages = 1
        loadPage(pageToLoad = 1, append = false)
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                val shouldLoadMore = lastVisibleIndex >= items.lastIndex - 4
                val hasMore = page < totalPages
                if (shouldLoadMore && hasMore && !isLoading && !isLoadingMore) {
                    scope.launch { loadPage(pageToLoad = page + 1, append = true) }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                scale = ButtonDefaults.scale(focusedScale = 1f),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                    contentColor = TvColors.TextPrimary,
                    focusedContainerColor = TvColors.BluePrimary,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TvColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Failed to load content",
                            color = TvColors.Error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Press OK to retry",
                            color = TvColors.BluePrimary,
                            modifier = Modifier
                                .focusRequester(firstItemFocusRequester)
                                .onFocusChanged { if (it.isFocused) refreshTrigger++ },
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(7),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items,
                        key = { _, media -> "${media.mediaType}-${media.id}" },
                    ) { index, media ->
                        SeerrTvPosterCard(
                            media = media,
                            seerrClient = seerrClient,
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                            onClick = {
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                            onLongClick = {
                                dialogParams = DialogParams(
                                    title = media.displayTitle,
                                    items = buildSeerrDialogItems(media, seerrActions),
                                    fromLongClick = true,
                                )
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                TvLoadingIndicator()
                            }
                        }
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

@Composable
private fun SeerrTvPosterCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val posterUrl = seerrClient.getPosterUrl(media.posterPath)

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = media.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
            ) {
                Text(
                    text = media.displayTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

// ============================================================================
// Filterable Media Grid Screen
// ============================================================================

@Composable
private fun SeerrFilterableMediaGridScreen(
    title: String,
    onBack: () -> Unit,
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    filterConfig: DiscoverFilterConfig,
    loadItems: suspend (
        page: Int,
        mediaType: MediaTypeFilter,
        genreIds: Set<Int>,
        releaseStatus: ReleaseStatusFilter,
        sortFilter: SortFilter,
        minRating: Float?,
        yearFrom: Int?,
        yearTo: Int?,
    ) -> Result<SeerrDiscoverResult>,
    genreMediaType: String? = null,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Content state
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    val firstItemFocusRequester = remember { FocusRequester() }

    // Filter state
    var mediaTypeFilter by remember { mutableStateOf(filterConfig.defaultMediaType) }
    var releaseStatusFilter by remember { mutableStateOf(ReleaseStatusFilter.ALL) }
    var selectedGenreIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var genres by remember { mutableStateOf<List<SeerrGenre>>(emptyList()) }
    var filterTrigger by remember { mutableIntStateOf(0) }

    // Advanced filter state
    var sortFilter by remember { mutableStateOf(SortFilter.POPULARITY_DESC) }
    var minRating by remember { mutableStateOf<Float?>(null) }
    var yearFrom by remember { mutableStateOf<Int?>(null) }
    var yearTo by remember { mutableStateOf<Int?>(null) }

    // Dialog state for advanced filters
    var showSortDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }

    // Dialog state for long-press context menu
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }

    // Seerr actions for context menu
    val seerrActions = remember(onMediaClick, scope, seerrClient) {
        SeerrMediaActions(
            onGoTo = { mediaType, tmdbId -> onMediaClick(mediaType, tmdbId) },
            onRequest = { media ->
                scope.launch {
                    if (media.isMovie) {
                        seerrClient.requestMovie(media.tmdbId ?: media.id)
                    } else {
                        seerrClient.requestTVShow(media.tmdbId ?: media.id)
                    }
                }
            },
            onBlacklist = { media ->
                scope.launch {
                    seerrClient.addToBlacklist(media.tmdbId ?: media.id, media.mediaType)
                }
            },
        )
    }

    // Load genres on first composition
    LaunchedEffect(Unit) {
        if (filterConfig.showGenreFilter) {
            // Determine which genres to load based on genreMediaType or default
            val genreType = genreMediaType ?: when (filterConfig.defaultMediaType) {
                MediaTypeFilter.MOVIES -> "movie"
                MediaTypeFilter.TV_SHOWS -> "tv"
                else -> "movie" // Default to movie genres for mixed
            }
            val genreResult = if (genreType == "tv") {
                seerrClient.getTVGenres()
            } else {
                seerrClient.getMovieGenres()
            }
            genreResult.onSuccess { genres = it }
        }
    }

    suspend fun loadPage(pageToLoad: Int, append: Boolean) {
        if (append) {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        errorMessage = null

        loadItems(
            pageToLoad,
            mediaTypeFilter,
            selectedGenreIds,
            releaseStatusFilter,
            sortFilter,
            minRating,
            yearFrom,
            yearTo,
        )
            .onSuccess { result ->
                val filtered = result.results.filterDiscoverable()
                items = if (append) {
                    (items + filtered).distinctBy { "${it.mediaType}-${it.id}" }
                } else {
                    filtered
                }
                page = result.page
                totalPages = result.totalPages
            }
            .onFailure { error ->
                if (!append) {
                    errorMessage = error.message ?: "Failed to load content"
                }
            }

        if (append) {
            isLoadingMore = false
        } else {
            isLoading = false
        }
    }

    // Reload when filters change
    LaunchedEffect(filterTrigger) {
        items = emptyList()
        page = 1
        totalPages = 1
        loadPage(pageToLoad = 1, append = false)
    }

    // Pagination
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                val shouldLoadMore = lastVisibleIndex >= items.lastIndex - 4
                val hasMore = page < totalPages
                if (shouldLoadMore && hasMore && !isLoading && !isLoadingMore) {
                    scope.launch { loadPage(pageToLoad = page + 1, append = true) }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(24.dp),
    ) {
        // Header row with back button and title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                scale = ButtonDefaults.scale(focusedScale = 1f),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                    contentColor = TvColors.TextPrimary,
                    focusedContainerColor = TvColors.BluePrimary,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TvColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter row
        val hasActiveFilters = filterConfig.showMediaTypeFilter ||
            filterConfig.showGenreFilter ||
            filterConfig.showReleaseStatusFilter

        if (hasActiveFilters) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Media type filter chips
                if (filterConfig.showMediaTypeFilter) {
                    items(MediaTypeFilter.entries) { filter ->
                        FilterChip(
                            selected = mediaTypeFilter == filter,
                            onClick = {
                                if (mediaTypeFilter != filter) {
                                    mediaTypeFilter = filter
                                    filterTrigger++
                                }
                            },
                            leadingIcon = if (mediaTypeFilter == filter) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.colors(
                                containerColor = TvColors.Surface,
                                contentColor = TvColors.TextSecondary,
                                focusedContainerColor = TvColors.SurfaceElevated,
                                focusedContentColor = TvColors.TextPrimary,
                                selectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.2f),
                                selectedContentColor = TvColors.BluePrimary,
                                focusedSelectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                                focusedSelectedContentColor = TvColors.BluePrimary,
                            ),
                        ) {
                            Text(filter.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Release status filter chips
                if (filterConfig.showReleaseStatusFilter) {
                    items(ReleaseStatusFilter.entries) { filter ->
                        FilterChip(
                            selected = releaseStatusFilter == filter,
                            onClick = {
                                if (releaseStatusFilter != filter) {
                                    releaseStatusFilter = filter
                                    filterTrigger++
                                }
                            },
                            leadingIcon = if (releaseStatusFilter == filter) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.colors(
                                containerColor = TvColors.Surface,
                                contentColor = TvColors.TextSecondary,
                                focusedContainerColor = TvColors.SurfaceElevated,
                                focusedContentColor = TvColors.TextPrimary,
                                selectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.2f),
                                selectedContentColor = TvColors.BluePrimary,
                                focusedSelectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                                focusedSelectedContentColor = TvColors.BluePrimary,
                            ),
                        ) {
                            Text(filter.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Genre filter chips (multi-select)
                if (filterConfig.showGenreFilter && genres.isNotEmpty()) {
                    items(genres) { genre ->
                        val isSelected = selectedGenreIds.contains(genre.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedGenreIds = if (isSelected) {
                                    selectedGenreIds - genre.id
                                } else {
                                    selectedGenreIds + genre.id
                                }
                                filterTrigger++
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.colors(
                                containerColor = TvColors.Surface,
                                contentColor = TvColors.TextSecondary,
                                focusedContainerColor = TvColors.SurfaceElevated,
                                focusedContentColor = TvColors.TextPrimary,
                                selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                selectedContentColor = Color(0xFF8B5CF6),
                                focusedSelectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.3f),
                                focusedSelectedContentColor = Color(0xFF8B5CF6),
                            ),
                        ) {
                            Text(genre.name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Sort filter chip
                item {
                    FilterChip(
                        selected = sortFilter != SortFilter.POPULARITY_DESC,
                        onClick = { showSortDialog = true },
                        leadingIcon = if (sortFilter != SortFilter.POPULARITY_DESC) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.colors(
                            containerColor = TvColors.Surface,
                            contentColor = TvColors.TextSecondary,
                            focusedContainerColor = TvColors.SurfaceElevated,
                            focusedContentColor = TvColors.TextPrimary,
                            selectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.2f),
                            selectedContentColor = TvColors.BluePrimary,
                            focusedSelectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                            focusedSelectedContentColor = TvColors.BluePrimary,
                        ),
                    ) {
                        Text("Sort: ${sortFilter.label}", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Rating filter chip
                item {
                    FilterChip(
                        selected = minRating != null,
                        onClick = { showRatingDialog = true },
                        leadingIcon = if (minRating != null) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.colors(
                            containerColor = TvColors.Surface,
                            contentColor = TvColors.TextSecondary,
                            focusedContainerColor = TvColors.SurfaceElevated,
                            focusedContentColor = TvColors.TextPrimary,
                            selectedContainerColor = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            selectedContentColor = Color(0xFFFBBF24),
                            focusedSelectedContainerColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                            focusedSelectedContentColor = Color(0xFFFBBF24),
                        ),
                    ) {
                        Text(
                            text = minRating?.let { "Rating: ${it.toInt()}+" } ?: "Rating",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                // Year filter chip
                item {
                    FilterChip(
                        selected = yearFrom != null || yearTo != null,
                        onClick = { showYearDialog = true },
                        leadingIcon = if (yearFrom != null || yearTo != null) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.colors(
                            containerColor = TvColors.Surface,
                            contentColor = TvColors.TextSecondary,
                            focusedContainerColor = TvColors.SurfaceElevated,
                            focusedContentColor = TvColors.TextPrimary,
                            selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                            selectedContentColor = Color(0xFF34D399),
                            focusedSelectedContainerColor = Color(0xFF34D399).copy(alpha = 0.3f),
                            focusedSelectedContentColor = Color(0xFF34D399),
                        ),
                    ) {
                        Text(
                            text = when {
                                yearFrom != null && yearTo != null -> "Year: $yearFrom-$yearTo"
                                yearFrom != null -> "Year: $yearFrom+"
                                yearTo != null -> "Year: -$yearTo"
                                else -> "Year"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Content grid
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Failed to load content",
                            color = TvColors.Error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { filterTrigger++ },
                            colors = ButtonDefaults.colors(
                                containerColor = TvColors.BluePrimary,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found",
                        color = TvColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(7),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items,
                        key = { _, media -> "${media.mediaType}-${media.id}" },
                    ) { index, media ->
                        SeerrTvPosterCard(
                            media = media,
                            seerrClient = seerrClient,
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                            onClick = {
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                            onLongClick = {
                                dialogParams = DialogParams(
                                    title = media.displayTitle,
                                    items = buildSeerrDialogItems(media, seerrActions),
                                    fromLongClick = true,
                                )
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                TvLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }

        // Sort selection dialog
        if (showSortDialog) {
            SortSelectionDialog(
                currentSort = sortFilter,
                onSortSelected = { selected ->
                    sortFilter = selected
                    filterTrigger++
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false },
            )
        }

        // Rating selection dialog
        if (showRatingDialog) {
            RatingSelectionDialog(
                currentRating = minRating,
                onRatingSelected = { selected ->
                    minRating = selected
                    filterTrigger++
                    showRatingDialog = false
                },
                onDismiss = { showRatingDialog = false },
            )
        }

        // Year range dialog
        if (showYearDialog) {
            YearRangeDialog(
                yearFrom = yearFrom,
                yearTo = yearTo,
                onConfirm = { from, to ->
                    yearFrom = from
                    yearTo = to
                    filterTrigger++
                    showYearDialog = false
                },
                onDismiss = { showYearDialog = false },
            )
        }

        // Long-press context menu dialog
        dialogParams?.let { params ->
            DialogPopup(
                params = params,
                onDismissRequest = { dialogParams = null },
            )
        }
    }
}

// ============================================================================
// Advanced Filter Dialogs
// ============================================================================

@Composable
private fun SortSelectionDialog(
    currentSort: SortFilter,
    onSortSelected: (SortFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sortOptions = SortFilter.entries
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .background(TvColors.Surface, MaterialTheme.shapes.medium)
                .padding(16.dp),
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            sortOptions.forEachIndexed { index, option ->
                val isSelected = option == currentSort
                Surface(
                    onClick = { onSortSelected(option) },
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                    shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) TvColors.BluePrimary.copy(alpha = 0.2f) else Color.Transparent,
                        focusedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TvColors.BluePrimary else TvColors.TextPrimary,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TvColors.BluePrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated,
                    contentColor = TvColors.TextPrimary,
                ),
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun RatingSelectionDialog(
    currentRating: Float?,
    onRatingSelected: (Float?) -> Unit,
    onDismiss: () -> Unit,
) {
    val ratingOptions = listOf(
        null to "Any",
        5f to "5+",
        6f to "6+",
        7f to "7+",
        8f to "8+",
        9f to "9+",
    )
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .background(TvColors.Surface, MaterialTheme.shapes.medium)
                .padding(16.dp),
        ) {
            Text(
                text = "Minimum Rating",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            ratingOptions.forEachIndexed { index, (rating, label) ->
                val isSelected = rating == currentRating
                Surface(
                    onClick = { onRatingSelected(rating) },
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                    shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) Color(0xFFFBBF24).copy(alpha = 0.2f) else Color.Transparent,
                        focusedContainerColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color(0xFFFBBF24) else TvColors.TextPrimary,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFFBBF24),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated,
                    contentColor = TvColors.TextPrimary,
                ),
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun YearRangeDialog(
    yearFrom: Int?,
    yearTo: Int?,
    onConfirm: (Int?, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var from by remember { mutableStateOf(yearFrom) }
    var to by remember { mutableStateOf(yearTo) }
    val currentYear = java.time.Year.now().value
    val yearOptions = listOf(null) + (currentYear downTo 1950).toList()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .background(TvColors.Surface, MaterialTheme.shapes.medium)
                .padding(16.dp),
        ) {
            Text(
                text = "Year Range",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // From year
            Text(
                text = "From:",
                style = MaterialTheme.typography.labelMedium,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                items(yearOptions.take(30)) { year ->
                    val isFirst = year == null
                    FilterChip(
                        selected = from == year,
                        onClick = { from = year },
                        modifier = if (isFirst) Modifier.focusRequester(focusRequester) else Modifier,
                        colors = FilterChipDefaults.colors(
                            containerColor = TvColors.SurfaceElevated,
                            contentColor = TvColors.TextSecondary,
                            selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                            selectedContentColor = Color(0xFF34D399),
                            focusedContainerColor = TvColors.SurfaceElevated,
                            focusedSelectedContainerColor = Color(0xFF34D399).copy(alpha = 0.3f),
                        ),
                    ) {
                        Text(year?.toString() ?: "Any", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // To year
            Text(
                text = "To:",
                style = MaterialTheme.typography.labelMedium,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                items(yearOptions.take(30)) { year ->
                    FilterChip(
                        selected = to == year,
                        onClick = { to = year },
                        colors = FilterChipDefaults.colors(
                            containerColor = TvColors.SurfaceElevated,
                            contentColor = TvColors.TextSecondary,
                            selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                            selectedContentColor = Color(0xFF34D399),
                            focusedContainerColor = TvColors.SurfaceElevated,
                            focusedSelectedContainerColor = Color(0xFF34D399).copy(alpha = 0.3f),
                        ),
                    ) {
                        Text(year?.toString() ?: "Any", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceElevated,
                        contentColor = TvColors.TextPrimary,
                    ),
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(from, to) },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF34D399),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

// ============================================================================
// Filter Helper Functions
// ============================================================================

private suspend fun loadTrendingWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    mediaType: MediaTypeFilter,
    genreIds: Set<Int>,
    releaseStatus: ReleaseStatusFilter,
): Result<SeerrDiscoverResult> {
    // Trending endpoint doesn't support filters, so we need to post-filter
    return seerrClient.getTrending(page).map { result ->
        var filtered = result.results

        // Filter by media type
        if (mediaType != MediaTypeFilter.ALL) {
            filtered = filtered.filter { it.mediaType == mediaType.apiValue }
        }

        // Filter by release status
        filtered = filterByReleaseStatus(filtered, releaseStatus)

        // Note: Genre filtering for trending requires client-side filtering since the API
        // doesn't support it directly. For now, we skip genre filtering on trending.

        result.copy(results = filtered)
    }
}

private suspend fun loadMoviesWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    genreIds: Set<Int>,
    releaseStatus: ReleaseStatusFilter,
    sortFilter: SortFilter,
    minRating: Float?,
    yearFrom: Int?,
    yearTo: Int?,
): Result<SeerrDiscoverResult> {
    val params = mutableMapOf<String, String>()

    // Add sort filter
    if (sortFilter != SortFilter.POPULARITY_DESC) {
        params["sortBy"] = sortFilter.movieValue
    }

    // Add genre filter
    if (genreIds.isNotEmpty()) {
        params["genre"] = genreIds.joinToString(",")
    }

    // Add minimum rating filter
    minRating?.let { params["voteAverageGte"] = it.toString() }

    // Add year range filter
    yearFrom?.let { params["primaryReleaseDateGte"] = "$it-01-01" }
    yearTo?.let { params["primaryReleaseDateLte"] = "$it-12-31" }

    // Add release status filter (may override year filters for upcoming)
    val today = java.time.LocalDate.now().toString()
    when (releaseStatus) {
        ReleaseStatusFilter.RELEASED -> {
            params["primaryReleaseDateLte"] = today
        }
        ReleaseStatusFilter.UPCOMING -> {
            params["primaryReleaseDateGte"] = today
        }
        ReleaseStatusFilter.ALL -> { /* No filter */ }
    }

    return if (params.isEmpty()) {
        seerrClient.discoverMovies(page = page)
    } else {
        seerrClient.discoverMoviesWithParams(params, page)
    }
}

private suspend fun loadTvWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    genreIds: Set<Int>,
    releaseStatus: ReleaseStatusFilter,
    sortFilter: SortFilter,
    minRating: Float?,
    yearFrom: Int?,
    yearTo: Int?,
): Result<SeerrDiscoverResult> {
    val params = mutableMapOf<String, String>()

    // Add sort filter
    if (sortFilter != SortFilter.POPULARITY_DESC) {
        params["sortBy"] = sortFilter.tvValue
    }

    // Add genre filter
    if (genreIds.isNotEmpty()) {
        params["genre"] = genreIds.joinToString(",")
    }

    // Add minimum rating filter
    minRating?.let { params["voteAverageGte"] = it.toString() }

    // Add year range filter
    yearFrom?.let { params["firstAirDateGte"] = "$it-01-01" }
    yearTo?.let { params["firstAirDateLte"] = "$it-12-31" }

    // Add release status filter (may override year filters for upcoming)
    val today = java.time.LocalDate.now().toString()
    when (releaseStatus) {
        ReleaseStatusFilter.RELEASED -> {
            params["firstAirDateLte"] = today
        }
        ReleaseStatusFilter.UPCOMING -> {
            params["firstAirDateGte"] = today
        }
        ReleaseStatusFilter.ALL -> { /* No filter */ }
    }

    return if (params.isEmpty()) {
        seerrClient.discoverTV(page = page)
    } else {
        seerrClient.discoverTVWithParams(params, page)
    }
}

private suspend fun loadUpcomingWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    mediaType: MediaTypeFilter,
    genreIds: Set<Int>,
): Result<SeerrDiscoverResult> {
    val today = java.time.LocalDate.now().toString()

    return when (mediaType) {
        MediaTypeFilter.MOVIES, MediaTypeFilter.ALL -> {
            val params = mutableMapOf<String, String>()
            params["primaryReleaseDateGte"] = today
            if (genreIds.isNotEmpty()) {
                params["genre"] = genreIds.joinToString(",")
            }
            seerrClient.discoverMoviesWithParams(params, page)
        }
        MediaTypeFilter.TV_SHOWS -> {
            val params = mutableMapOf<String, String>()
            params["firstAirDateGte"] = today
            if (genreIds.isNotEmpty()) {
                params["genre"] = genreIds.joinToString(",")
            }
            seerrClient.discoverTVWithParams(params, page)
        }
    }
}

private fun filterByReleaseStatus(
    items: List<SeerrMedia>,
    releaseStatus: ReleaseStatusFilter,
): List<SeerrMedia> {
    if (releaseStatus == ReleaseStatusFilter.ALL) return items

    val today = java.time.LocalDate.now()
    return items.filter { media ->
        val releaseDate = media.displayReleaseDate?.let {
            try {
                java.time.LocalDate.parse(it)
            } catch (_: Exception) {
                null
            }
        }

        when (releaseStatus) {
            ReleaseStatusFilter.RELEASED -> releaseDate == null || releaseDate <= today
            ReleaseStatusFilter.UPCOMING -> releaseDate != null && releaseDate > today
            ReleaseStatusFilter.ALL -> true
        }
    }
}
