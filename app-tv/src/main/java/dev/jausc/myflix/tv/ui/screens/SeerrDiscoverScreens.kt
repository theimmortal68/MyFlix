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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors

@Composable
fun SeerrDiscoverTrendingScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrMediaGridScreen(
        title = "Trending",
        onBack = onBack,
        loadItems = { seerrClient.getTrending().map { it.results } },
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
        loadItems = { seerrClient.discoverMovies().map { it.results } },
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
        loadItems = { seerrClient.discoverTV().map { it.results } },
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
    )
}

@Composable
fun SeerrDiscoverWatchlistScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrMediaGridScreen(
        title = "Watchlist",
        onBack = onBack,
        loadItems = { seerrClient.getWatchlist().map { it.results } },
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
    )
}

@Composable
private fun SeerrMediaGridScreen(
    title: String,
    onBack: () -> Unit,
    loadItems: suspend () -> Result<List<SeerrMedia>>,
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        errorMessage = null
        loadItems()
            .onSuccess { items = it }
            .onFailure { errorMessage = it.message ?: "Failed to load content" }
        isLoading = false
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
                modifier = Modifier.height(40.dp),
            ) {
                Text("Back")
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
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(items, key = { it.id }) { media ->
                        SeerrTvPosterCard(
                            media = media,
                            seerrClient = seerrClient,
                            modifier = if (items.firstOrNull() == media) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                            onClick = {
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrTvPosterCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val posterUrl = seerrClient.getPosterUrl(media.posterPath)

    Surface(
        onClick = onClick,
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
