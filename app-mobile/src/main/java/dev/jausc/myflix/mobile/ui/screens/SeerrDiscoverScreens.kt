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
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrDiscoverResult
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.filterDiscoverable
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MenuItem
import dev.jausc.myflix.mobile.ui.components.MenuItemDivider
import dev.jausc.myflix.mobile.ui.components.MenuItemEntry
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

// ============================================================================
// Filter Configuration (shared types)
// ============================================================================

/**
 * Filter configuration for discover screens.
 */
data class MobileDiscoverFilterConfig(
    val showMediaTypeFilter: Boolean = false,
    val showGenreFilter: Boolean = false,
    val showReleaseStatusFilter: Boolean = false,
    val defaultMediaType: MobileMediaTypeFilter = MobileMediaTypeFilter.ALL,
)

enum class MobileMediaTypeFilter(val label: String, val apiValue: String?) {
    ALL("All", null),
    MOVIES("Movies", "movie"),
    TV_SHOWS("TV Shows", "tv"),
}

enum class MobileReleaseStatusFilter(val label: String) {
    ALL("All"),
    RELEASED("Released"),
    UPCOMING("Upcoming"),
}

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
        filterConfig = MobileDiscoverFilterConfig(
            showMediaTypeFilter = true,
            showGenreFilter = true,
            showReleaseStatusFilter = true,
        ),
        loadItems = { page, mediaType, genreIds, releaseStatus ->
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
        filterConfig = MobileDiscoverFilterConfig(
            showGenreFilter = true,
            showReleaseStatusFilter = true,
            defaultMediaType = MobileMediaTypeFilter.MOVIES,
        ),
        loadItems = { page, _, genreIds, releaseStatus ->
            loadMobileMoviesWithFilters(seerrClient, page, genreIds, releaseStatus)
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
        filterConfig = MobileDiscoverFilterConfig(
            showGenreFilter = true,
            showReleaseStatusFilter = true,
            defaultMediaType = MobileMediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, genreIds, releaseStatus ->
            loadMobileTvWithFilters(seerrClient, page, genreIds, releaseStatus)
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
        filterConfig = MobileDiscoverFilterConfig(
            showGenreFilter = true,
            defaultMediaType = MobileMediaTypeFilter.MOVIES,
        ),
        loadItems = { page, _, genreIds, _ ->
            loadMobileUpcomingWithFilters(seerrClient, page, MobileMediaTypeFilter.MOVIES, genreIds)
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
        filterConfig = MobileDiscoverFilterConfig(
            showGenreFilter = true,
            defaultMediaType = MobileMediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, genreIds, _ ->
            loadMobileUpcomingWithFilters(seerrClient, page, MobileMediaTypeFilter.TV_SHOWS, genreIds)
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
        filterConfig = MobileDiscoverFilterConfig(
            showReleaseStatusFilter = true,
            defaultMediaType = if (mediaType == "movie") MobileMediaTypeFilter.MOVIES
                else MobileMediaTypeFilter.TV_SHOWS,
        ),
        loadItems = { page, _, _, releaseStatus ->
            if (mediaType == "movie") {
                loadMobileMoviesWithFilters(seerrClient, page, setOf(genreId), releaseStatus)
            } else {
                loadMobileTvWithFilters(seerrClient, page, setOf(genreId), releaseStatus)
            }
        },
        genreMediaType = mediaType,
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
            is SeerrActionDivider -> MenuItemDivider
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
    filterConfig: MobileDiscoverFilterConfig,
    loadItems: suspend (
        page: Int,
        mediaType: MobileMediaTypeFilter,
        genreIds: Set<Int>,
        releaseStatus: MobileReleaseStatusFilter,
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
    var releaseStatusFilter by remember { mutableStateOf(MobileReleaseStatusFilter.ALL) }
    var selectedGenreIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var genres by remember { mutableStateOf<List<SeerrGenre>>(emptyList()) }
    var filterTrigger by remember { mutableIntStateOf(0) }

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
                MobileMediaTypeFilter.MOVIES -> "movie"
                MobileMediaTypeFilter.TV_SHOWS -> "tv"
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

        loadItems(pageToLoad, mediaTypeFilter, selectedGenreIds, releaseStatusFilter)
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
                    items(MobileMediaTypeFilter.entries) { filter ->
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
                    items(MobileReleaseStatusFilter.entries) { filter ->
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
    }
}

// ============================================================================
// Filter Helper Functions
// ============================================================================

private suspend fun loadMobileTrendingWithFilters(
    seerrClient: SeerrClient,
    page: Int,
    mediaType: MobileMediaTypeFilter,
    genreIds: Set<Int>,
    releaseStatus: MobileReleaseStatusFilter,
): Result<SeerrDiscoverResult> {
    return seerrClient.getTrending(page).map { result ->
        var filtered = result.results

        // Filter by media type
        if (mediaType != MobileMediaTypeFilter.ALL) {
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
    releaseStatus: MobileReleaseStatusFilter,
): Result<SeerrDiscoverResult> {
    val params = mutableMapOf<String, String>()

    // Add genre filter
    if (genreIds.isNotEmpty()) {
        params["genre"] = genreIds.joinToString(",")
    }

    // Add release status filter
    val today = java.time.LocalDate.now().toString()
    when (releaseStatus) {
        MobileReleaseStatusFilter.RELEASED -> {
            params["primaryReleaseDateLte"] = today
        }
        MobileReleaseStatusFilter.UPCOMING -> {
            params["primaryReleaseDateGte"] = today
        }
        MobileReleaseStatusFilter.ALL -> { /* No filter */ }
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
    releaseStatus: MobileReleaseStatusFilter,
): Result<SeerrDiscoverResult> {
    val params = mutableMapOf<String, String>()

    // Add genre filter
    if (genreIds.isNotEmpty()) {
        params["genre"] = genreIds.joinToString(",")
    }

    // Add release status filter
    val today = java.time.LocalDate.now().toString()
    when (releaseStatus) {
        MobileReleaseStatusFilter.RELEASED -> {
            params["firstAirDateLte"] = today
        }
        MobileReleaseStatusFilter.UPCOMING -> {
            params["firstAirDateGte"] = today
        }
        MobileReleaseStatusFilter.ALL -> { /* No filter */ }
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
    mediaType: MobileMediaTypeFilter,
    genreIds: Set<Int>,
): Result<SeerrDiscoverResult> {
    val today = java.time.LocalDate.now().toString()

    return when (mediaType) {
        MobileMediaTypeFilter.MOVIES, MobileMediaTypeFilter.ALL -> {
            val params = mutableMapOf<String, String>()
            params["primaryReleaseDateGte"] = today
            if (genreIds.isNotEmpty()) {
                params["genre"] = genreIds.joinToString(",")
            }
            seerrClient.discoverMoviesWithParams(params, page)
        }
        MobileMediaTypeFilter.TV_SHOWS -> {
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
    releaseStatus: MobileReleaseStatusFilter,
): List<SeerrMedia> {
    if (releaseStatus == MobileReleaseStatusFilter.ALL) return items

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
            MobileReleaseStatusFilter.RELEASED -> releaseDate == null || releaseDate <= today
            MobileReleaseStatusFilter.UPCOMING -> releaseDate != null && releaseDate > today
            MobileReleaseStatusFilter.ALL -> true
        }
    }
}
