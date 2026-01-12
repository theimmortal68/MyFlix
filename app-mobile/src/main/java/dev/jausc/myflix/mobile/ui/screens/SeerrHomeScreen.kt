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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import dev.jausc.myflix.core.seerr.SeerrDiscoverHelper
import dev.jausc.myflix.core.seerr.SeerrDiscoverRow
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrRowType

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
    onNavigateSearch: () -> Unit = {},
    onNavigateRequests: () -> Unit = {},
    onNavigateDiscoverTrending: () -> Unit = {},
    onNavigateDiscoverMovies: () -> Unit = {},
    onNavigateDiscoverTv: () -> Unit = {},
    onNavigateWatchlist: () -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<SeerrDiscoverRow>>(emptyList()) }

    // Collect auth state as Compose state for proper recomposition
    val isAuthenticated by seerrClient.isAuthenticated.collectAsState()

    // Filter out items already in library, partially available, or already requested
    fun List<SeerrMedia>.filterDiscoverable() = filter {
        !it.isAvailable && !it.isPending && it.availabilityStatus != SeerrMediaStatus.PARTIALLY_AVAILABLE
    }

    // Load content - key on auth status to reload if auth changes
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            errorMessage = "Not connected to Seerr"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        val sliders = seerrClient.getDiscoverSettings().getOrNull()
        val discoverRows = if (!sliders.isNullOrEmpty()) {
            SeerrDiscoverHelper.loadDiscoverRows(seerrClient, sliders) { filterDiscoverable() }
        } else {
            SeerrDiscoverHelper.loadFallbackRows(seerrClient) { filterDiscoverable() }
        }

        rows = discoverRows
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

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val actions = listOf(
                "Trending" to onNavigateDiscoverTrending,
                "Movies" to onNavigateDiscoverMovies,
                "TV" to onNavigateDiscoverTv,
                "Watchlist" to onNavigateWatchlist,
                "Search" to onNavigateSearch,
                "Requests" to onNavigateRequests,
            )
            itemsIndexed(actions, key = { _, item -> item.first }) { _, item ->
                SeerrQuickActionChip(text = item.first, onClick = item.second)
            }
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
                    rows.forEach { row ->
                        item(key = row.key) {
                            val onViewAll: (() -> Unit)? = when (row.rowType) {
                                SeerrRowType.TRENDING -> onNavigateDiscoverTrending
                                SeerrRowType.POPULAR_MOVIES -> onNavigateDiscoverMovies
                                SeerrRowType.POPULAR_TV -> onNavigateDiscoverTv
                                SeerrRowType.WATCHLIST -> onNavigateWatchlist
                                SeerrRowType.OTHER -> null
                            }
                            MobileSeerrRow(
                                title = row.title,
                                items = row.items,
                                seerrClient = seerrClient,
                                accentColor = Color(row.accentColorValue),
                                onItemClick = { media ->
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                },
                                onViewAll = onViewAll,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrQuickActionChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
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
    onViewAll: (() -> Unit)? = null,
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
            // View All card at the end of the row
            if (onViewAll != null) {
                item(key = "view_all_$title") {
                    MobileViewAllCard(onClick = onViewAll)
                }
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

@Composable
private fun MobileViewAllCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .aspectRatio(2f / 3f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF8B5CF6),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
