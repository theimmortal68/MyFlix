@file:Suppress(
    "LongMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
    "ModifierMissing",
    "ParameterNaming",
    "ComposableParamOrder",
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus

/**
 * Mobile Seerr home/discover screen.
 *
 * Features:
 * - Hero section with featured content
 * - Trending, Popular Movies, Popular TV rows
 * - Availability status badges
 * - Touch-friendly card interactions
 */
@Composable
fun SeerrHomeScreen(
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var trending by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var popularMovies by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var popularTV by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var upcomingMovies by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var featuredItem by remember { mutableStateOf<SeerrMedia?>(null) }

    // Filter out items already in library, partially available, or already requested
    fun List<SeerrMedia>.filterDiscoverable() = filter {
        !it.isAvailable && !it.isPending && it.availabilityStatus != SeerrMediaStatus.PARTIALLY_AVAILABLE
    }

    // Load content - key on auth status to reload if auth changes
    LaunchedEffect(seerrClient.isAuthenticated) {
        if (!seerrClient.isAuthenticated) {
            errorMessage = "Not connected to Seerr"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        // Load trending (filter out items already in library)
        seerrClient.getTrending()
            .onSuccess { result ->
                val filtered = result.results.filterDiscoverable()
                trending = filtered.take(12)
                featuredItem = filtered.firstOrNull()
            }
            .onFailure { errorMessage = it.message }

        // Load popular movies (filter out items already in library)
        seerrClient.getPopularMovies()
            .onSuccess { result ->
                popularMovies = result.results.filterDiscoverable().take(12)
            }

        // Load popular TV (filter out items already in library)
        seerrClient.getPopularTV()
            .onSuccess { result ->
                popularTV = result.results.filterDiscoverable().take(12)
            }

        // Load upcoming (these shouldn't be in library yet, but filter anyway)
        seerrClient.getUpcomingMovies()
            .onSuccess { result ->
                upcomingMovies = result.results.filterDiscoverable().take(12)
            }

        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Top bar
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
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Discover",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Failed to load content",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check Seerr settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // Hero section
                    item {
                        featuredItem?.let { media ->
                            MobileSeerrHero(
                                media = media,
                                seerrClient = seerrClient,
                                onClick = { onMediaClick(media.mediaType, media.tmdbId ?: media.id) },
                            )
                        }
                    }

                    // Trending row
                    if (trending.isNotEmpty()) {
                        item {
                            MobileSeerrRow(
                                title = "Trending",
                                items = trending,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFF8B5CF6),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                            )
                        }
                    }

                    // Popular Movies row
                    if (popularMovies.isNotEmpty()) {
                        item {
                            MobileSeerrRow(
                                title = "Popular Movies",
                                items = popularMovies,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFFFBBF24),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                            )
                        }
                    }

                    // Popular TV row
                    if (popularTV.isNotEmpty()) {
                        item {
                            MobileSeerrRow(
                                title = "Popular TV Shows",
                                items = popularTV,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFF34D399),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                            )
                        }
                    }

                    // Upcoming row
                    if (upcomingMovies.isNotEmpty()) {
                        item {
                            MobileSeerrRow(
                                title = "Coming Soon",
                                items = upcomingMovies,
                                seerrClient = seerrClient,
                                accentColor = Color(0xFF60A5FA),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSeerrHero(media: SeerrMedia, seerrClient: SeerrClient, onClick: () -> Unit,) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable(onClick = onClick),
    ) {
        // Backdrop
        AsyncImage(
            model = seerrClient.getBackdropUrl(media.backdropPath),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            // Status badge (uses mediaInfo.status for availability)
            MobileSeerrStatusBadge(status = media.availabilityStatus)

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = media.displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Year and type
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                media.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (media.isMovie) "Movie" else "TV Show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                media.voteAverage?.let { rating ->
                    Text(
                        text = "%.1f".format(rating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileSeerrStatusBadge(status: Int?) {
    val (color, icon, text) = when (status) {
        SeerrMediaStatus.AVAILABLE -> Triple(
            Color(0xFF22C55E),
            Icons.Outlined.Check,
            "Available",
        )
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Triple(
            Color(0xFFFBBF24),
            Icons.Outlined.Schedule,
            "Requested",
        )
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> Triple(
            Color(0xFF60A5FA),
            Icons.Outlined.Check,
            "Partial",
        )
        else -> Triple(
            Color(0xFF8B5CF6),
            Icons.Outlined.Add,
            "Not Requested",
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun MobileSeerrRow(
    title: String,
    items: List<SeerrMedia>,
    seerrClient: SeerrClient,
    accentColor: Color,
    onItemClick: (SeerrMedia) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Row header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(accentColor, RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { "${it.mediaType}_${it.id}" }) { media ->
                MobileSeerrCard(
                    media = media,
                    seerrClient = seerrClient,
                    onClick = { onItemClick(media) },
                )
            }
        }
    }
}

@Composable
private fun MobileSeerrCard(media: SeerrMedia, seerrClient: SeerrClient, onClick: () -> Unit,) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f),
                ) {
                    AsyncImage(
                        model = seerrClient.getPosterUrl(media.posterPath),
                        contentDescription = media.displayTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    // Availability badge (uses mediaInfo.status for availability)
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
                                .padding(6.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(color),
                        )
                    }
                }

                // Title
                Text(
                    text = media.displayTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )

                // Year
                media.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
