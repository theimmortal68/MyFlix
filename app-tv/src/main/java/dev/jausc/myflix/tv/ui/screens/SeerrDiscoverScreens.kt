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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
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
import dev.jausc.myflix.core.seerr.SeerrMedia
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

@Composable
fun SeerrDiscoverTrendingScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrMediaGridScreen(
        title = "Trending",
        onBack = onBack,
        loadItems = { page -> seerrClient.getTrending(page) },
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
    )
}

@Composable
fun SeerrDiscoverMoviesScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrMediaGridScreen(
        title = "Discover Movies",
        onBack = onBack,
        loadItems = { page -> seerrClient.discoverMovies(page = page) },
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
    )
}

@Composable
fun SeerrDiscoverTvScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrMediaGridScreen(
        title = "Discover TV",
        onBack = onBack,
        loadItems = { page -> seerrClient.discoverTV(page = page) },
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
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
    SeerrMediaGridScreen(
        title = genreName,
        onBack = onBack,
        loadItems = { page ->
            if (mediaType == "movie") {
                seerrClient.discoverMovies(genreId = genreId, page = page)
            } else {
                seerrClient.discoverTV(genreId = genreId, page = page)
            }
        },
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
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
                items = if (append) {
                    (items + result.results).distinctBy { "${it.mediaType}-${it.id}" }
                } else {
                    result.results
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
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.Surface.copy(alpha = 0.7f),
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    tint = TvColors.TextPrimary,
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
                    columns = GridCells.Adaptive(minSize = 160.dp),
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
        modifier = modifier.size(width = 160.dp, height = 240.dp),
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
