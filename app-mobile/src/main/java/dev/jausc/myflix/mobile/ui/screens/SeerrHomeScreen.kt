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

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.rememberCoroutineScope
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import dev.jausc.myflix.core.common.ui.SeerrActionDivider
import dev.jausc.myflix.core.common.ui.SeerrActionItem
import dev.jausc.myflix.core.common.ui.SeerrMediaActions
import dev.jausc.myflix.core.common.ui.buildSeerrActionItems
import dev.jausc.myflix.core.seerr.GenreBackdropColors
import dev.jausc.myflix.core.seerr.PopularNetworks
import dev.jausc.myflix.core.seerr.PopularStudios
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrColors
import dev.jausc.myflix.core.seerr.SeerrDiscoverHelper
import dev.jausc.myflix.core.seerr.SeerrDiscoverRow
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.core.seerr.SeerrGenreRow
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrNetwork
import dev.jausc.myflix.core.seerr.SeerrNetworkRow
import dev.jausc.myflix.core.seerr.SeerrRowType
import dev.jausc.myflix.core.seerr.SeerrStudio
import dev.jausc.myflix.core.seerr.SeerrStudioRow
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MenuItem
import dev.jausc.myflix.mobile.ui.components.MenuItemDivider
import dev.jausc.myflix.mobile.ui.components.MenuItemEntry
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import kotlinx.coroutines.launch

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
    onNavigateDiscoverUpcomingMovies: () -> Unit = {},
    onNavigateDiscoverUpcomingTv: () -> Unit = {},
    onNavigateGenre: (mediaType: String, genreId: Int, genreName: String) -> Unit = { _, _, _ -> },
    onNavigateStudio: (studioId: Int, studioName: String) -> Unit = { _, _ -> },
    onNavigateNetwork: (networkId: Int, networkName: String) -> Unit = { _, _ -> },
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<SeerrDiscoverRow>>(emptyList()) }
    var genreRows by remember { mutableStateOf<List<SeerrGenreRow>>(emptyList()) }
    var studiosRow by remember { mutableStateOf<SeerrStudioRow?>(null) }
    var networksRow by remember { mutableStateOf<SeerrNetworkRow?>(null) }

    // Coroutine scope for actions
    val scope = rememberCoroutineScope()

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

    // Collect auth state as Compose state for proper recomposition
    val isAuthenticated by seerrClient.isAuthenticated.collectAsState()

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
            SeerrDiscoverHelper.loadDiscoverRows(seerrClient, sliders)
        } else {
            SeerrDiscoverHelper.loadFallbackRows(seerrClient)
        }

        rows = discoverRows

        // Load genre rows for browsing
        genreRows = SeerrDiscoverHelper.loadGenreRows(seerrClient)

        // Load studios and networks rows
        studiosRow = SeerrDiscoverHelper.getStudiosRow()
        networksRow = SeerrDiscoverHelper.getNetworksRow()

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
                    // Ordered rows:
                    // 1. Trending
                    // 2. Popular Movies
                    // 3. Movie Genres
                    // 4. Upcoming Movies
                    // 5. Studios
                    // 6. Popular TV
                    // 7. TV Genres
                    // 8. Upcoming TV
                    // 9. Networks

                    // Find rows by type for ordered rendering
                    val trendingRow = rows.find { it.rowType == SeerrRowType.TRENDING }
                    val popularMoviesRow = rows.find { it.rowType == SeerrRowType.POPULAR_MOVIES }
                    val movieGenresRow = genreRows.find { it.mediaType == "movie" }
                    val upcomingMoviesRow = rows.find { it.rowType == SeerrRowType.UPCOMING_MOVIES }
                    val popularTvRow = rows.find { it.rowType == SeerrRowType.POPULAR_TV }
                    val tvGenresRow = genreRows.find { it.mediaType == "tv" }
                    val upcomingTvRow = rows.find { it.rowType == SeerrRowType.UPCOMING_TV }

                    // Other rows not in the ordered list
                    val otherRows = rows.filter { row ->
                        row.rowType == SeerrRowType.OTHER
                    }

                    // Helper to render a content row
                    @Composable
                    fun RenderContentRow(row: SeerrDiscoverRow) {
                        val onViewAll: (() -> Unit)? = when (row.rowType) {
                            SeerrRowType.TRENDING -> onNavigateDiscoverTrending
                            SeerrRowType.POPULAR_MOVIES -> onNavigateDiscoverMovies
                            SeerrRowType.POPULAR_TV -> onNavigateDiscoverTv
                            SeerrRowType.UPCOMING_MOVIES -> onNavigateDiscoverUpcomingMovies
                            SeerrRowType.UPCOMING_TV -> onNavigateDiscoverUpcomingTv
                            else -> null
                        }
                        MobileSeerrRow(
                            title = row.title,
                            items = row.items,
                            seerrClient = seerrClient,
                            accentColor = Color(row.accentColorValue),
                            onItemClick = { media ->
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                            onItemLongClick = { media ->
                                menuParams = BottomSheetParams(
                                    title = media.displayTitle,
                                    items = buildSeerrMenuItems(media, seerrActions),
                                )
                            },
                            onViewAll = onViewAll,
                        )
                    }

                    // 1. Trending
                    trendingRow?.let { row ->
                        item(key = row.key) { RenderContentRow(row) }
                    }

                    // 2. Popular Movies
                    popularMoviesRow?.let { row ->
                        item(key = row.key) { RenderContentRow(row) }
                    }

                    // 3. Movie Genres
                    movieGenresRow?.let { genreRow ->
                        item(key = genreRow.key) {
                            MobileSeerrGenreBrowseRow(
                                title = genreRow.title,
                                genres = genreRow.genres,
                                onGenreClick = { genre ->
                                    onNavigateGenre(genreRow.mediaType, genre.id, genre.name)
                                },
                            )
                        }
                    }

                    // 4. Upcoming Movies
                    upcomingMoviesRow?.let { row ->
                        item(key = row.key) { RenderContentRow(row) }
                    }

                    // 5. Studios
                    studiosRow?.let { studioRow ->
                        item(key = studioRow.key) {
                            MobileSeerrStudioBrowseRow(
                                title = studioRow.title,
                                studios = studioRow.studios,
                                onStudioClick = { studio ->
                                    onNavigateStudio(studio.id, studio.name)
                                },
                            )
                        }
                    }

                    // 6. Popular TV
                    popularTvRow?.let { row ->
                        item(key = row.key) { RenderContentRow(row) }
                    }

                    // 7. TV Genres
                    tvGenresRow?.let { genreRow ->
                        item(key = genreRow.key) {
                            MobileSeerrGenreBrowseRow(
                                title = genreRow.title,
                                genres = genreRow.genres,
                                onGenreClick = { genre ->
                                    onNavigateGenre(genreRow.mediaType, genre.id, genre.name)
                                },
                            )
                        }
                    }

                    // 8. Upcoming TV
                    upcomingTvRow?.let { row ->
                        item(key = row.key) { RenderContentRow(row) }
                    }

                    // 9. Networks
                    networksRow?.let { networkRow ->
                        item(key = networkRow.key) {
                            MobileSeerrNetworkBrowseRow(
                                title = networkRow.title,
                                networks = networkRow.networks,
                                onNetworkClick = { network ->
                                    onNavigateNetwork(network.id, network.name)
                                },
                            )
                        }
                    }

                    // Other/custom rows at the end
                    otherRows.forEach { row ->
                        item(key = row.key) { RenderContentRow(row) }
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

@Suppress("UnusedPrivateMember")
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
    onItemLongClick: ((SeerrMedia) -> Unit)? = null,
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
                    onLongClick = onItemLongClick?.let { { it(media) } },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MobileSeerrCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
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

/**
 * A row of genre cards for browsing by genre.
 */
@Composable
private fun MobileSeerrGenreBrowseRow(
    title: String,
    genres: List<SeerrGenre>,
    onGenreClick: (SeerrGenre) -> Unit,
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
                    .background(Color(SeerrColors.PURPLE), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Genre cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(genres, key = { it.id }) { genre ->
                MobileSeerrGenreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre) },
                )
            }
        }
    }
}

/**
 * A card displaying a genre with duotone-filtered backdrop image.
 */
@Composable
private fun MobileSeerrGenreCard(
    genre: SeerrGenre,
    onClick: () -> Unit,
) {
    // Get the first backdrop from the genre's backdrops list
    val backdropPath = genre.backdrops?.firstOrNull()
    val backdropUrl = backdropPath?.let { GenreBackdropColors.getBackdropUrl(it, genre.id) }

    // Fallback colors if no backdrop available
    val (darkHex, lightHex) = GenreBackdropColors.getColorPair(genre.id)
    val darkColor = Color(android.graphics.Color.parseColor("#$darkHex"))
    val lightColor = Color(android.graphics.Color.parseColor("#$lightHex"))

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        ) {
            // Background: backdrop image with duotone filter, or gradient fallback
            if (backdropUrl != null) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Fallback gradient based on genre colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(darkColor, lightColor),
                            ),
                        ),
                )
            }

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                            ),
                        ),
                    ),
            )

            // Genre name
            Text(
                text = genre.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            )
        }
    }
}

/**
 * A row of studio cards for browsing by studio.
 */
@Composable
private fun MobileSeerrStudioBrowseRow(
    title: String,
    studios: List<SeerrStudio>,
    onStudioClick: (SeerrStudio) -> Unit,
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
                    .background(Color(SeerrColors.YELLOW), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Studio cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(studios, key = { it.id }) { studio ->
                MobileSeerrStudioCard(
                    studio = studio,
                    onClick = { onStudioClick(studio) },
                )
            }
        }
    }
}

/**
 * A card displaying a studio with TMDb logo using duotone filter.
 */
@Composable
private fun MobileSeerrStudioCard(
    studio: SeerrStudio,
    onClick: () -> Unit,
) {
    val logoUrl = studio.logoPath?.let { PopularStudios.getLogoUrl(it) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                val painter = rememberAsyncImagePainter(model = logoUrl)
                val state = painter.state.collectAsState().value

                if (state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Empty) {
                    Text(
                        text = studio.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        maxLines = 2,
                    )
                } else {
                    Image(
                        painter = painter,
                        contentDescription = studio.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            } else {
                Text(
                    text = studio.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * A row of network cards for browsing by network.
 */
@Composable
private fun MobileSeerrNetworkBrowseRow(
    title: String,
    networks: List<SeerrNetwork>,
    onNetworkClick: (SeerrNetwork) -> Unit,
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
                    .background(Color(SeerrColors.TEAL), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Network cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(networks, key = { it.id }) { network ->
                MobileSeerrNetworkCard(
                    network = network,
                    onClick = { onNetworkClick(network) },
                )
            }
        }
    }
}

/**
 * A card displaying a network with TMDb logo.
 */
@Composable
private fun MobileSeerrNetworkCard(
    network: SeerrNetwork,
    onClick: () -> Unit,
) {
    val logoUrl = network.logoPath?.let { PopularNetworks.getLogoUrl(it) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                val painter = rememberAsyncImagePainter(model = logoUrl)
                val state = painter.state.collectAsState().value

                if (state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Empty) {
                    Text(
                        text = network.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        maxLines = 2,
                    )
                } else {
                    Image(
                        painter = painter,
                        contentDescription = network.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            } else {
                Text(
                    text = network.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 2,
                )
            }
        }
    }
}
