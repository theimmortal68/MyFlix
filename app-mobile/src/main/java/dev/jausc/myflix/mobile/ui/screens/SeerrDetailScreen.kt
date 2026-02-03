@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.util.DateFormatter
import dev.jausc.myflix.core.seerr.SeerrCastMember
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrCrewMember
import dev.jausc.myflix.core.seerr.SeerrImdbRating
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrQuotaDetails
import dev.jausc.myflix.core.seerr.SeerrRequestStatus
import dev.jausc.myflix.core.seerr.SeerrRottenTomatoesRating
import dev.jausc.myflix.core.seerr.SeerrSeasonStatus
import dev.jausc.myflix.core.seerr.SeerrStatusColors
import dev.jausc.myflix.core.seerr.SeerrVideo
import dev.jausc.myflix.core.seerr.buildQuotaText
import kotlinx.coroutines.launch

/**
 * Mobile Seerr media detail screen.
 *
 * Features:
 * - Full backdrop with gradient overlay
 * - Title, year, rating, runtime, genres
 * - Overview/synopsis
 * - Cast section
 * - Season count for TV shows
 * - Request button with status awareness
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeerrDetailScreen(
    mediaType: String,
    tmdbId: Int,
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
    onActorClick: ((Int) -> Unit)? = null,
    onNavigateGenre: ((mediaType: String, genreId: Int, genreName: String) -> Unit)? = null,
) {
    var isLoading by remember { mutableStateOf(true) }
    var media by remember { mutableStateOf<SeerrMedia?>(null) }
    var cast by remember { mutableStateOf<List<SeerrCastMember>>(emptyList()) }
    var crew by remember { mutableStateOf<List<SeerrCrewMember>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var similar by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRequesting by remember { mutableStateOf(false) }
    var isCanceling by remember { mutableStateOf(false) }
    var requestSuccess by remember { mutableStateOf(false) }
    var isBlacklisting by remember { mutableStateOf(false) }
    var selectedSeasons by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var request4k by remember { mutableStateOf(false) }
    var quotaDetails by remember { mutableStateOf<SeerrQuotaDetails?>(null) }
    var rtRating by remember { mutableStateOf<SeerrRottenTomatoesRating?>(null) }
    var imdbRating by remember { mutableStateOf<SeerrImdbRating?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load media details
    LaunchedEffect(mediaType, tmdbId) {
        isLoading = true
        errorMessage = null
        cast = emptyList()
        crew = emptyList()
        recommendations = emptyList()
        similar = emptyList()
        selectedSeasons = emptySet()
        request4k = false
        quotaDetails = null

        val result = if (mediaType == "movie") {
            seerrClient.getMovie(tmdbId)
        } else {
            seerrClient.getTVShow(tmdbId)
        }

        result
            .onSuccess { mediaItem ->
                media = mediaItem
                cast = mediaItem.credits?.cast?.take(20) ?: emptyList()
                crew = mediaItem.credits?.crew?.take(10) ?: emptyList()
            }
            .onFailure {
                errorMessage = it.message ?: "Failed to load details"
            }

        val recommendationsResult = if (mediaType == "movie") {
            seerrClient.getMovieRecommendations(tmdbId)
        } else {
            seerrClient.getTVRecommendations(tmdbId)
        }

        recommendationsResult
            .onSuccess { recommendations = it.results }
            .onFailure { }

        val similarResult = if (mediaType == "movie") {
            seerrClient.getSimilarMovies(tmdbId)
        } else {
            seerrClient.getSimilarTV(tmdbId)
        }

        similarResult
            .onSuccess { similar = it.results }
            .onFailure { }

        seerrClient.getUserQuota()
            .onSuccess { quota ->
                quotaDetails = if (mediaType == "movie") quota.movie else quota.tv
            }
            .onFailure { quotaDetails = null }

        // Load external ratings (RT, IMDB)
        rtRating = null
        imdbRating = null
        if (mediaType == "movie") {
            seerrClient.getMovieRatings(tmdbId)
                .onSuccess { ratings ->
                    rtRating = ratings.rt
                    imdbRating = ratings.imdb
                }
        } else {
            seerrClient.getTVRatings(tmdbId)
                .onSuccess { ratings ->
                    rtRating = ratings
                }
        }

        isLoading = false
    }

    fun requestMedia() {
        val currentMedia = media ?: return
        scope.launch {
            isRequesting = true
            val result = if (mediaType == "movie") {
                seerrClient.requestMovie(tmdbId, request4k)
            } else {
                // For TV shows, Overseerr requires explicit season numbers
                val seasons = if (selectedSeasons.isNotEmpty()) {
                    selectedSeasons.toList()
                } else {
                    // Request all seasons - generate list [1, 2, ..., numberOfSeasons]
                    val numSeasons = currentMedia.numberOfSeasons ?: 1
                    (1..numSeasons).toList()
                }
                seerrClient.requestTVShow(tmdbId, seasons, request4k)
            }

            result
                .onSuccess {
                    requestSuccess = true
                    // Refresh media to get updated status
                    val refreshResult = if (mediaType == "movie") {
                        seerrClient.getMovie(tmdbId)
                    } else {
                        seerrClient.getTVShow(tmdbId)
                    }
                    refreshResult.onSuccess { media = it }
                }
                .onFailure {
                    errorMessage = it.message ?: "Request failed"
                }
            isRequesting = false
        }
    }

    // Handle cancel request - also deletes media from Sonarr/Radarr
    fun handleCancelRequest() {
        val currentMedia = media ?: return
        // Find an active (non-declined) request to cancel
        val activeRequest = currentMedia.mediaInfo?.requests
            ?.firstOrNull { it.status != SeerrRequestStatus.DECLINED }
            ?: return

        scope.launch {
            isCanceling = true

            // First delete media to remove from Sonarr/Radarr (must be done before canceling)
            val mediaId = currentMedia.mediaInfo?.id
            if (mediaId != null) {
                seerrClient.deleteMedia(mediaId)
                // Ignore failures - media might not be in Sonarr/Radarr yet
            }

            // Then cancel the request
            seerrClient.cancelRequest(activeRequest.id)
                .onSuccess {
                    // Refresh media to get updated status
                    val refreshResult = if (mediaType == "movie") {
                        seerrClient.getMovie(tmdbId)
                    } else {
                        seerrClient.getTVShow(tmdbId)
                    }
                    refreshResult.onSuccess { media = it }
                }
                .onFailure { errorMessage = "Failed to cancel request: ${it.message}" }
            isCanceling = false
        }
    }

    fun handleBlacklist() {
        scope.launch {
            isBlacklisting = true
            seerrClient.addToBlacklist(tmdbId, mediaType)
                .onSuccess { onBack() } // Go back after blacklisting
                .onFailure { errorMessage = it.message ?: "Failed to blacklist" }
            isBlacklisting = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            }

            errorMessage != null && media == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }

            media != null -> {
                val currentMedia = media!!

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // Backdrop with overlay
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        ) {
                            AsyncImage(
                                model = seerrClient.getBackdropUrl(currentMedia.backdropPath),
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

                            // Back button
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        }
                    }

                    // Title and metadata
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            // Title
                            Text(
                                text = currentMedia.displayTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Metadata row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Full release date formatted
                                currentMedia.displayReleaseDate?.let { dateStr ->
                                    val formattedDate = DateFormatter.formatFull(dateStr)
                                        ?: currentMedia.year?.toString()
                                        ?: dateStr
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // Content rating (e.g., PG-13, R, TV-MA)
                                currentMedia.contentRating?.let { rating ->
                                    MobileContentRatingBadge(rating = rating)
                                }

                                // Rotten Tomatoes critics score
                                rtRating?.criticsScore?.let { score ->
                                    MobileRottenTomatoesBadge(
                                        score = score,
                                        isFresh = rtRating?.isCriticsFresh == true,
                                    )
                                }

                                // IMDB rating (movies only)
                                imdbRating?.criticsScore?.let { score ->
                                    MobileImdbRatingBadge(rating = score)
                                }

                                // TMDb Rating
                                currentMedia.voteAverage?.let { rating ->
                                    MobileTmdbRatingBadge(rating = rating)
                                }

                                // Type badge
                                Text(
                                    text = if (currentMedia.isMovie) "Movie" else "TV Show",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }

                            // Season info for TV shows
                            if (currentMedia.isTvShow && currentMedia.numberOfSeasons != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${currentMedia.numberOfSeasons} seasons • ${currentMedia.numberOfEpisodes ?: "?"} episodes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Genres (clickable)
                            if (!currentMedia.genres.isNullOrEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    currentMedia.genres!!.take(4).forEach { genre ->
                                        MobileGenreChip(
                                            name = genre.name,
                                            onClick = onNavigateGenre?.let { navigate ->
                                                {
                                                    navigate(mediaType, genre.id, genre.name)
                                                }
                                            },
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Status and Request button (uses mediaInfo.status for availability)
                            MobileSeerrRequestSection(
                                status = currentMedia.availabilityStatus,
                                isRequesting = isRequesting,
                                isCanceling = isCanceling,
                                requestSuccess = requestSuccess,
                                isBlacklisting = isBlacklisting,
                                isTvShow = currentMedia.isTvShow,
                                seasonCount = currentMedia.numberOfSeasons,
                                selectedSeasons = selectedSeasons,
                                quotaDetails = quotaDetails,
                                request4k = request4k,
                                seasonStatuses = currentMedia.mediaInfo?.seasons,
                                onSeasonToggle = { season ->
                                    selectedSeasons = if (selectedSeasons.contains(season)) {
                                        selectedSeasons - season
                                    } else {
                                        selectedSeasons + season
                                    }
                                },
                                onClearSeasons = { selectedSeasons = emptySet() },
                                onToggle4k = { request4k = !request4k },
                                onRequest = { requestMedia() },
                                onCancelRequest = { handleCancelRequest() },
                                onBlacklist = { handleBlacklist() },
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Overview
                            currentMedia.overview?.let { overview ->
                                Text(
                                    text = "Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4,
                                )
                            }
                        }
                    }

                    val tmdbUrl = "https://www.themoviedb.org/${if (currentMedia.isMovie) "movie" else "tv"}/$tmdbId"
                    val imdbUrl = currentMedia.imdbId?.let { "https://www.imdb.com/title/$it" }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "External Links",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tmdbUrl))
                                    context.startActivity(intent)
                                }) {
                                    Text("TMDb")
                                }
                                imdbUrl?.let { url ->
                                    Button(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }) {
                                        Text("IMDb")
                                    }
                                }
                            }
                        }
                    }

                    // Cast
                    if (cast.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Cast",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(cast, key = { it.id }) { member ->
                                    MobileSeerrCastCard(
                                        castMember = member,
                                        seerrClient = seerrClient,
                                        onClick = { onActorClick?.invoke(member.id) },
                                    )
                                }
                            }
                        }
                    }

                    if (crew.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Crew",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(crew, key = { it.id }) { member ->
                                    MobileSeerrCrewCard(
                                        crewMember = member,
                                        seerrClient = seerrClient,
                                    )
                                }
                            }
                        }
                    }

                    if (recommendations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Recommendations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(recommendations, key = { it.id }) { item ->
                                    MobileSeerrRelatedCard(
                                        media = item,
                                        seerrClient = seerrClient,
                                        onClick = {
                                            val targetType = if (item.mediaType.isNotBlank()) {
                                                item.mediaType
                                            } else if (item.isMovie) {
                                                "movie"
                                            } else {
                                                "tv"
                                            }
                                            onMediaClick(targetType, item.tmdbId ?: item.id)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (similar.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Similar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(similar, key = { it.id }) { item ->
                                    MobileSeerrRelatedCard(
                                        media = item,
                                        seerrClient = seerrClient,
                                        onClick = {
                                            val targetType = if (item.mediaType.isNotBlank()) {
                                                item.mediaType
                                            } else if (item.isMovie) {
                                                "movie"
                                            } else {
                                                "tv"
                                            }
                                            onMediaClick(targetType, item.tmdbId ?: item.id)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // Error snackbar
                errorMessage?.let { error ->
                    if (media != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class RequestStatusInfo(
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String,
    val allowsRequest: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MobileSeerrRequestSection(
    status: Int?,
    isRequesting: Boolean,
    isCanceling: Boolean,
    requestSuccess: Boolean,
    isBlacklisting: Boolean,
    isTvShow: Boolean,
    seasonCount: Int?,
    selectedSeasons: Set<Int>,
    quotaDetails: SeerrQuotaDetails?,
    request4k: Boolean,
    seasonStatuses: List<SeerrSeasonStatus>?,
    onSeasonToggle: (Int) -> Unit,
    onClearSeasons: () -> Unit,
    onToggle4k: () -> Unit,
    onRequest: () -> Unit,
    onCancelRequest: () -> Unit,
    onBlacklist: () -> Unit,
) {
    val statusInfo = when (status) {
        SeerrMediaStatus.AVAILABLE -> RequestStatusInfo(
            color = Color(0xFF22C55E),
            icon = Icons.Outlined.Check,
            text = "Available in Library",
            allowsRequest = false,
        )
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> RequestStatusInfo(
            color = Color(0xFFFBBF24),
            icon = Icons.Outlined.Schedule,
            text = "Request Pending",
            allowsRequest = false,
        )
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> RequestStatusInfo(
            color = Color(0xFF60A5FA),
            icon = Icons.Outlined.Check,
            text = "Partially Available",
            allowsRequest = true,
        )
        else -> RequestStatusInfo(
            color = Color(0xFF8B5CF6),
            icon = Icons.Outlined.Add,
            text = "Not Requested",
            allowsRequest = true,
        )
    }
    val statusColor = statusInfo.color
    val statusIcon = statusInfo.icon
    val statusText = statusInfo.text
    val statusAllowsRequest = statusInfo.allowsRequest

    val quotaRemaining = quotaDetails?.remaining
    val quotaAllowsRequest = quotaRemaining == null || quotaRemaining > 0
    val canRequest = statusAllowsRequest && quotaAllowsRequest
    val quotaText = buildQuotaText(quotaDetails)

    Column {
        // Status badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    statusColor.copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = statusColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = statusColor,
            )
        }

        quotaText?.let { text ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!quotaAllowsRequest && statusAllowsRequest) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quota reached",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (statusAllowsRequest) {
            if (isTvShow && seasonCount != null && seasonCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Seasons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedSeasons.isEmpty(),
                        onClick = onClearSeasons,
                        label = { Text("All") },
                    )
                    (1..seasonCount).forEach { season ->
                        val seasonStatus = seasonStatuses
                            ?.find { it.seasonNumber == season }
                            ?.status
                        val statusColor = getMobileSeasonStatusColor(seasonStatus)
                        FilterChip(
                            selected = selectedSeasons.contains(season),
                            onClick = { onSeasonToggle(season) },
                            label = { Text("S$season") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = statusColor.copy(alpha = 0.2f),
                                labelColor = statusColor,
                            ),
                        )
                    }
                }
                // Season status legend
                Spacer(modifier = Modifier.height(8.dp))
                MobileSeasonStatusLegend()
            }

            Spacer(modifier = Modifier.height(12.dp))
            FilterChip(
                selected = request4k,
                onClick = onToggle4k,
                label = { Text("4K") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        // Request button (for items that can be requested)
        if (statusAllowsRequest) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequest,
                enabled = !isRequesting && !requestSuccess && canRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isRequesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Requesting...")
                } else if (requestSuccess) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Requested!")
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request")
                }
            }
        }

        // Cancel Request button (for pending/processing items)
        if (status == SeerrMediaStatus.PENDING || status == SeerrMediaStatus.PROCESSING) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCancelRequest,
                enabled = !isCanceling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFBBF24),
                    contentColor = Color.Black,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isCanceling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Canceling...")
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Request")
                }
            }
        }

        // Blacklist button - hide from discover
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onBlacklist,
            enabled = !isBlacklisting,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F2937),
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isBlacklisting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFEF4444),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hide from Discover")
        }
    }
}

@Composable
private fun MobileSeerrCastCard(castMember: SeerrCastMember, seerrClient: SeerrClient, onClick: () -> Unit = {},) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = seerrClient.getProfileUrl(castMember.profilePath),
                contentDescription = castMember.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = castMember.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        castMember.character?.let { character ->
            Text(
                text = character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MobileSeerrCrewCard(crewMember: SeerrCrewMember, seerrClient: SeerrClient,) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = seerrClient.getProfileUrl(crewMember.profilePath),
                contentDescription = crewMember.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = crewMember.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        crewMember.job?.let { job ->
            Text(
                text = job,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MobileSeerrRelatedCard(media: SeerrMedia, seerrClient: SeerrClient, onClick: () -> Unit,) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = seerrClient.getPosterUrl(media.posterPath),
            contentDescription = media.displayTitle,
            modifier = Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = media.displayTitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MobileSeerrVideoCard(video: SeerrVideo, onClick: (videoKey: String, title: String?) -> Unit,) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                video.key?.let { key ->
                    onClick(key, video.name ?: video.type)
                }
            },
    ) {
        // YouTube thumbnail
        AsyncImage(
            model = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg",
            contentDescription = video.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Play icon overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = "Play video",
                modifier = Modifier.size(48.dp),
                tint = Color.White,
            )
        }

        // Video name
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    ),
                )
                .padding(8.dp),
        ) {
            Text(
                text = video.name ?: video.type ?: "Trailer",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MobileContentRatingBadge(rating: String) {
    Text(
        text = rating,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * TMDb rating badge matching Seerr web UI styling.
 * Displays rating as decimal (e.g., "7.8") not percentage.
 */
@Composable
private fun MobileTmdbRatingBadge(rating: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "TMDB",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF01D277), // TMDb teal/green
        )
    }
}

/**
 * Rotten Tomatoes rating badge for mobile.
 */
@Composable
private fun MobileRottenTomatoesBadge(score: Int, isFresh: Boolean,) {
    // RT uses red tomato for fresh and green splat for rotten
    val color = if (isFresh) Color(0xFFFA320A) else Color(0xFF6AC238)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "RT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$score%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/**
 * IMDB rating badge for mobile.
 */
@Composable
private fun MobileImdbRatingBadge(rating: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "IMDB",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF5C518), // IMDB yellow
        )
    }
}

@Composable
private fun MobileGenreChip(name: String, onClick: (() -> Unit)?,) {
    val chipModifier = Modifier
        .background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp),
        )
        .padding(horizontal = 12.dp, vertical = 6.dp)

    if (onClick != null) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = chipModifier.clickable(onClick = onClick),
        )
    } else {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = chipModifier,
        )
    }
}

private fun getMobileSeasonStatusColor(status: Int?): Color = Color(SeerrStatusColors.getColorForStatus(status))

@Composable
private fun MobileSeasonStatusLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MobileSeasonLegendItem(color = Color(SeerrStatusColors.AVAILABLE), label = "Available")
        MobileSeasonLegendItem(color = Color(SeerrStatusColors.REQUESTED), label = "Requested")
        MobileSeasonLegendItem(color = Color(SeerrStatusColors.NOT_REQUESTED), label = "Not Requested")
    }
}

@Composable
private fun MobileSeasonLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
