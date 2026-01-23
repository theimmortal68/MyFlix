@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.ui.SeerrActionDivider
import dev.jausc.myflix.core.common.ui.SeerrActionItem
import dev.jausc.myflix.core.common.ui.SeerrMediaActions
import dev.jausc.myflix.core.common.ui.buildSeerrActionItems
import dev.jausc.myflix.core.seerr.DiscoverFilterConfig
import dev.jausc.myflix.core.seerr.MediaTypeFilter
import dev.jausc.myflix.core.seerr.ReleaseStatusFilter
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrDiscoverResult
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SortFilter
import dev.jausc.myflix.core.seerr.filterDiscoverable
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MenuItem
import dev.jausc.myflix.mobile.ui.components.MenuItemDivider
import dev.jausc.myflix.mobile.ui.components.MenuItemEntry
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun SeerrDiscoverTrendingScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaListScreen(
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
            loadMobileTrendingWithFilters(seerrClient, page, mediaType, genreIds, releaseStatus)
        },
    )
}

@Composable
fun SeerrDiscoverMoviesScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaListScreen(
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
            loadMobileMoviesWithFilters(seerrClient, page, genreIds, releaseStatus, sort, rating, fromYear, toYear)
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
    SeerrFilterableMediaListScreen(
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
            loadMobileTvWithFilters(seerrClient, page, genreIds, releaseStatus, sort, rating, fromYear, toYear)
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
    SeerrFilterableMediaListScreen(
        title = "Upcoming Movies",
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            defaultMediaType = MediaTypeFilter.MOVIES,
        ),
        loadItems = { page, _, genreIds, _, _, _, _, _ ->
            loadMobileUpcomingWithFilters(seerrClient, page, MediaTypeFilter.MOVIES, genreIds)
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
    SeerrFilterableMediaListScreen(
        title = "Upcoming TV",
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            defaultMediaType = MediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, genreIds, _, _, _, _, _ ->
            loadMobileUpcomingWithFilters(seerrClient, page, MediaTypeFilter.TV_SHOWS, genreIds)
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
    SeerrFilterableMediaListScreen(
        title = genreName,
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showReleaseStatusFilter = true,
            defaultMediaType = if (mediaType == "movie") {
                MediaTypeFilter.MOVIES
            } else {
                MediaTypeFilter.TV_SHOWS
            },
        ),
        loadItems = { page, _, _, releaseStatus, sort, rating, fromYear, toYear ->
            if (mediaType == "movie") {
                loadMobileMoviesWithFilters(seerrClient, page, setOf(genreId), releaseStatus, sort, rating, fromYear, toYear)
            } else {
                loadMobileTvWithFilters(seerrClient, page, setOf(genreId), releaseStatus, sort, rating, fromYear, toYear)
            }
        },
        genreMediaType = mediaType,
    )
}

@Composable
fun SeerrDiscoverByStudioScreen(
    seerrClient: SeerrClient,
    studioId: Int,
    studioName: String,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaListScreen(
        title = studioName,
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            showReleaseStatusFilter = true,
            defaultMediaType = MediaTypeFilter.MOVIES,
        ),
        loadItems = { page, _, genreIds, releaseStatus, sort, rating, fromYear, toYear ->
            loadMobileMoviesWithFilters(
                seerrClient, page, genreIds, releaseStatus, sort, rating, fromYear, toYear,
                companyId = studioId,
            )
        },
        genreMediaType = "movie",
    )
}

@Composable
fun SeerrDiscoverByNetworkScreen(
    seerrClient: SeerrClient,
    networkId: Int,
    networkName: String,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrFilterableMediaListScreen(
        title = networkName,
        onBack = onBack,
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
        filterConfig = DiscoverFilterConfig(
            showGenreFilter = true,
            showReleaseStatusFilter = true,
            defaultMediaType = MediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, genreIds, releaseStatus, sort, rating, fromYear, toYear ->
            loadMobileTvWithFilters(
                seerrClient, page, genreIds, releaseStatus, sort, rating, fromYear, toYear,
                networkId = networkId,
            )
        },
        genreMediaType = "tv",
    )
}

@Composable
private fun SeerrMediaListScreen(
    title: String,
    onBack: () -> Unit,
    loadItems: suspend (page: Int) -> Result<SeerrDiscoverResult>,
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Menu state for long-press context menu
    var menuParams by remember { mutableStateOf<BottomSheetParams?>(null) }

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

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        SeerrMobileTopBar(title = title, onBack = onBack)

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            }
            errorMessage != null -> {
                SeerrMobileErrorState(
                    message = errorMessage ?: "Failed to load content",
                    onRetry = { refreshTrigger++ },
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items,
                        key = { _, media -> "${media.mediaType}-${media.id}" },
                    ) { _, media ->
                        SeerrMobileMediaCard(
                            media = media,
                            seerrClient = seerrClient,
                            onClick = {
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                            onLongClick = {
                                menuParams = BottomSheetParams(
                                    title = media.displayTitle,
                                    items = buildSeerrMenuItems(media, seerrActions),
                                )
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long-press context menu
        menuParams?.let { params ->
            PopupMenu(
                params = params,
                onDismiss = { menuParams = null },
            )
        }
    }
}

/**
 * Convert Seerr action items to mobile menu items.
 */
private fun buildSeerrMenuItems(
    media: SeerrMedia,
    actions: SeerrMediaActions,
): List<MenuItemEntry> {
    return buildSeerrActionItems(media, actions).mapNotNull { entry ->
        when (entry) {
            is SeerrActionDivider -> {
                MenuItemDivider
            }
            is SeerrActionItem -> {
                if (entry.enabled) {
                    MenuItem(
                        text = entry.text,
                        icon = entry.icon,
                        iconTint = entry.iconTint,
                        onClick = entry.onClick,
                    )
                } else {
                    null // Skip disabled items on mobile
                }
            }
        }
    }
}

@Composable
private fun SeerrMobileTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SeerrMobileErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap to retry",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onRetry() },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeerrMobileMediaCard(
    media: SeerrMedia,
    seerrClient: SeerrClient?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val posterUrl = seerrClient?.getPosterUrl(media.posterPath) ?: media.posterPath?.let {
        "https://image.tmdb.org/t/p/w500$it"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = media.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = media.year?.toString() ?: "Unknown year",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ============================================================================
// Filterable Media List Screen
// ============================================================================

@Composable
private fun SeerrFilterableMediaListScreen(
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Content state
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }

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

    // Bottom sheet state for advanced filters
    var showSortSheet by remember { mutableStateOf(false) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var showYearSheet by remember { mutableStateOf(false) }

    // Menu state for long-press context menu
    var menuParams by remember { mutableStateOf<BottomSheetParams?>(null) }

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
            val genreType = genreMediaType ?: when (filterConfig.defaultMediaType) {
                MediaTypeFilter.MOVIES -> "movie"
                MediaTypeFilter.TV_SHOWS -> "tv"
                else -> "movie"
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
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        SeerrMobileTopBar(title = title, onBack = onBack)

        // Filter row
        val hasActiveFilters = filterConfig.showMediaTypeFilter ||
            filterConfig.showGenreFilter ||
            filterConfig.showReleaseStatusFilter

        if (hasActiveFilters) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            label = { Text(filter.label) },
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF8B5CF6),
                                selectedLeadingIconColor = Color(0xFF8B5CF6),
                            ),
                        )
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
                            label = { Text(filter.label) },
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF8B5CF6),
                                selectedLeadingIconColor = Color(0xFF8B5CF6),
                            ),
                        )
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
                            label = { Text(genre.name) },
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF8B5CF6),
                                selectedLeadingIconColor = Color(0xFF8B5CF6),
                            ),
                        )
                    }
                }

                // Sort filter chip
                item {
                    FilterChip(
                        selected = sortFilter != SortFilter.POPULARITY_DESC,
                        onClick = { showSortSheet = true },
                        label = { Text("Sort: ${sortFilter.label}") },
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF3B82F6),
                            selectedLeadingIconColor = Color(0xFF3B82F6),
                        ),
                    )
                }

                // Rating filter chip
                item {
                    FilterChip(
                        selected = minRating != null,
                        onClick = { showRatingSheet = true },
                        label = { Text(minRating?.let { "Rating: ${it.toInt()}+" } ?: "Rating") },
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFFBBF24),
                            selectedLeadingIconColor = Color(0xFFFBBF24),
                        ),
                    )
                }

                // Year filter chip
                item {
                    val yearLabel = when {
                        yearFrom != null && yearTo != null -> "Year: $yearFrom-$yearTo"
                        yearFrom != null -> "Year: $yearFrom+"
                        yearTo != null -> "Year: -$yearTo"
                        else -> "Year"
                    }
                    FilterChip(
                        selected = yearFrom != null || yearTo != null,
                        onClick = { showYearSheet = true },
                        label = { Text(yearLabel) },
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF34D399),
                            selectedLeadingIconColor = Color(0xFF34D399),
                        ),
                    )
                }
            }
        }

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            }
            errorMessage != null -> {
                SeerrMobileErrorState(
                    message = errorMessage ?: "Failed to load content",
                    onRetry = { filterTrigger++ },
                )
            }
            items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items,
                        key = { _, media -> "${media.mediaType}-${media.id}" },
                    ) { _, media ->
                        SeerrMobileMediaCard(
                            media = media,
                            seerrClient = seerrClient,
                            onClick = {
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                            onLongClick = {
                                menuParams = BottomSheetParams(
                                    title = media.displayTitle,
                                    items = buildSeerrMenuItems(media, seerrActions),
                                )
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long-press context menu
        menuParams?.let { params ->
            PopupMenu(
                params = params,
                onDismiss = { menuParams = null },
            )
        }

        // Sort selection bottom sheet
        if (showSortSheet) {
            PopupMenu(
                params = BottomSheetParams(
                    title = "Sort By",
                    items = SortFilter.entries.map { option ->
                        MenuItem(
                            text = option.label,
                            icon = if (sortFilter == option) Icons.Outlined.Check else null,
                            iconTint = Color(0xFF3B82F6),
                            onClick = {
                                sortFilter = option
                                filterTrigger++
                                showSortSheet = false
                            },
                        )
                    },
                ),
                onDismiss = { showSortSheet = false },
            )
        }

        // Rating selection bottom sheet
        if (showRatingSheet) {
            val ratingOptions = listOf(
                null to "Any",
                5f to "5+",
                6f to "6+",
                7f to "7+",
                8f to "8+",
                9f to "9+",
            )
            PopupMenu(
                params = BottomSheetParams(
                    title = "Minimum Rating",
                    items = ratingOptions.map { (rating, label) ->
                        MenuItem(
                            text = label,
                            icon = if (minRating == rating) Icons.Outlined.Check else null,
                            iconTint = Color(0xFFFBBF24),
                            onClick = {
                                minRating = rating
                                filterTrigger++
                                showRatingSheet = false
                            },
                        )
                    },
                ),
                onDismiss = { showRatingSheet = false },
            )
        }

        // Year selection bottom sheet
        if (showYearSheet) {
            val currentYear = java.time.Year.now().value
            val yearOptions = listOf("Any") + (currentYear downTo 1990).map { it.toString() }
            PopupMenu(
                params = BottomSheetParams(
                    title = "Year Range",
                    items = listOf(
                        MenuItem(
                            text = "Clear Year Filter",
                            onClick = {
                                yearFrom = null
                                yearTo = null
                                filterTrigger++
                                showYearSheet = false
                            },
                        ),
                        MenuItemDivider,
                    ) + listOf(
                        MenuItem(
                            text = "Last 5 Years (${currentYear - 5}-$currentYear)",
                            icon = if (yearFrom == currentYear - 5 && yearTo == currentYear) {
                                Icons.Outlined.Check
                            } else {
                                null
                            },
                            iconTint = Color(0xFF34D399),
                            onClick = {
                                yearFrom = currentYear - 5
                                yearTo = currentYear
                                filterTrigger++
                                showYearSheet = false
                            },
                        ),
                        MenuItem(
                            text = "Last 10 Years (${currentYear - 10}-$currentYear)",
                            icon = if (yearFrom == currentYear - 10 && yearTo == currentYear) {
                                Icons.Outlined.Check
                            } else {
                                null
                            },
                            iconTint = Color(0xFF34D399),
                            onClick = {
                                yearFrom = currentYear - 10
                                yearTo = currentYear
                                filterTrigger++
                                showYearSheet = false
                            },
                        ),
                        MenuItem(
                            text = "2020s (2020-$currentYear)",
                            icon = if (yearFrom == 2020 && yearTo == currentYear) {
                                Icons.Outlined.Check
                            } else {
                                null
                            },
                            iconTint = Color(0xFF34D399),
                            onClick = {
                                yearFrom = 2020
                                yearTo = currentYear
                                filterTrigger++
                                showYearSheet = false
                            },
                        ),
                        MenuItem(
                            text = "2010s (2010-2019)",
                            icon = if (yearFrom == 2010 && yearTo == 2019) {
                                Icons.Outlined.Check
                            } else {
                                null
                            },
                            iconTint = Color(0xFF34D399),
                            onClick = {
                                yearFrom = 2010
                                yearTo = 2019
                                filterTrigger++
                                showYearSheet = false
                            },
                        ),
                        MenuItem(
                            text = "2000s (2000-2009)",
                            icon = if (yearFrom == 2000 && yearTo == 2009) {
                                Icons.Outlined.Check
                            } else {
                                null
                            },
                            iconTint = Color(0xFF34D399),
                            onClick = {
                                yearFrom = 2000
                                yearTo = 2009
                                filterTrigger++
                                showYearSheet = false
                            },
                        ),
                        MenuItem(
                            text = "Classic (Before 2000)",
                            icon = if (yearFrom == null && yearTo == 1999) {
                                Icons.Outlined.Check
                            } else {
                                null
                            },
                            iconTint = Color(0xFF34D399),
                            onClick = {
                                yearFrom = null
                                yearTo = 1999
                                filterTrigger++
                                showYearSheet = false
                            },
                        ),
                    ),
                ),
                onDismiss = { showYearSheet = false },
            )
        }
    }
}

// ============================================================================
// Filter Helper Functions
// ============================================================================

private suspend fun loadMobileTrendingWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    mediaType: MediaTypeFilter,
    genreIds: Set<Int>,
    releaseStatus: ReleaseStatusFilter,
): Result<SeerrDiscoverResult> {
    return seerrClient.getTrending(page).map { result ->
        var filtered = result.results

        // Filter by media type
        if (mediaType != MediaTypeFilter.ALL) {
            filtered = filtered.filter { it.mediaType == mediaType.apiValue }
        }

        // Filter by release status
        filtered = filterMobileByReleaseStatus(filtered, releaseStatus)

        result.copy(results = filtered)
    }
}

private suspend fun loadMobileMoviesWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    genreIds: Set<Int>,
    releaseStatus: ReleaseStatusFilter,
    sortFilter: SortFilter,
    minRating: Float?,
    yearFrom: Int?,
    yearTo: Int?,
    companyId: Int? = null,
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

    // Add company (studio) filter
    companyId?.let { params["studio"] = it.toString() }

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

private suspend fun loadMobileTvWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    genreIds: Set<Int>,
    releaseStatus: ReleaseStatusFilter,
    sortFilter: SortFilter,
    minRating: Float?,
    yearFrom: Int?,
    yearTo: Int?,
    networkId: Int? = null,
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

    // Add network filter
    networkId?.let { params["network"] = it.toString() }

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

private suspend fun loadMobileUpcomingWithFilters(
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

private fun filterMobileByReleaseStatus(
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
