@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMedia

@Composable
fun SeerrDiscoverTrendingScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    SeerrMediaListScreen(
        title = "Trending",
        onBack = onBack,
        loadItems = {
            seerrClient.getTrending().map { it.results }
        },
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
    SeerrMediaListScreen(
        title = "Discover Movies",
        onBack = onBack,
        loadItems = {
            seerrClient.discoverMovies().map { it.results }
        },
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
    SeerrMediaListScreen(
        title = "Discover TV",
        onBack = onBack,
        loadItems = {
            seerrClient.discoverTV().map { it.results }
        },
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
    SeerrMediaListScreen(
        title = "Watchlist",
        onBack = onBack,
        loadItems = {
            seerrClient.getWatchlist().map { it.results }
        },
        seerrClient = seerrClient,
        onMediaClick = onMediaClick,
    )
}

@Composable
private fun SeerrMediaListScreen(
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.id }) { media ->
                        SeerrMobileMediaCard(
                            media = media,
                            seerrClient = seerrClient,
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

@Composable
private fun SeerrMobileMediaCard(
    media: SeerrMedia,
    seerrClient: SeerrClient?,
    onClick: () -> Unit,
) {
    val posterUrl = seerrClient?.getPosterUrl(media.posterPath) ?: media.posterPath?.let {
        "https://image.tmdb.org/t/p/w500$it"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
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
