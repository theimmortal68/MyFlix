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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import dev.jausc.myflix.core.seerr.SeerrCastMember
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrCrewMember
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrVideo
import dev.jausc.myflix.core.common.util.DateFormatter
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
) {
    var isLoading by remember { mutableStateOf(true) }
    var media by remember { mutableStateOf<SeerrMedia?>(null) }
    var cast by remember { mutableStateOf<List<SeerrCastMember>>(emptyList()) }
    var crew by remember { mutableStateOf<List<SeerrCrewMember>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var similar by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRequesting by remember { mutableStateOf(false) }
    var requestSuccess by remember { mutableStateOf(false) }
    var isWatchlisted by remember { mutableStateOf<Boolean?>(null) }
    var isWatchlistUpdating by remember { mutableStateOf(false) }

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
        isWatchlisted = null

        val result = if (mediaType == "movie") {
            seerrClient.getMovie(tmdbId)
        } else {
            seerrClient.getTVShow(tmdbId)
        }

        result
            .onSuccess { mediaItem ->
                media = mediaItem
                cast = mediaItem.credits?.cast?.take(10) ?: emptyList()
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

        seerrClient.getWatchlist()
            .onSuccess { response ->
                isWatchlisted = response.results.any { item ->
                    item.tmdbId == tmdbId && item.mediaType == mediaType
                }
            }
            .onFailure { isWatchlisted = null }

        isLoading = false
    }

    fun requestMedia() {
        val currentMedia = media ?: return
        scope.launch {
            isRequesting = true
            val result = if (mediaType == "movie") {
                seerrClient.requestMovie(tmdbId)
            } else {
                // Request all seasons for TV
                seerrClient.requestTVShow(tmdbId, null)
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

    fun toggleWatchlist() {
        val watchlisted = isWatchlisted ?: return
        scope.launch {
            isWatchlistUpdating = true
            val result = if (watchlisted) {
                seerrClient.removeFromWatchlist(tmdbId, mediaType)
            } else {
                seerrClient.addToWatchlist(tmdbId, mediaType)
            }
            result
                .onSuccess { isWatchlisted = !watchlisted }
                .onFailure { errorMessage = it.message ?: "Watchlist update failed" }
            isWatchlistUpdating = false
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

                                // TMDb Rating with label
                                currentMedia.voteAverage?.let { rating ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "TMDb",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF01D277),
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFFBBF24),
                                        )
                                        Text(
                                            text = "%.1f".format(rating),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24),
                                        )
                                    }
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

                            // Genres
                            if (!currentMedia.genres.isNullOrEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    currentMedia.genres!!.take(4).forEach { genre ->
                                        Text(
                                            text = genre.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    RoundedCornerShape(16.dp),
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Status and Request button (uses mediaInfo.status for availability)
                            MobileSeerrRequestSection(
                                status = currentMedia.availabilityStatus,
                                isRequesting = isRequesting,
                                requestSuccess = requestSuccess,
                                isWatchlisted = isWatchlisted,
                                isWatchlistUpdating = isWatchlistUpdating,
                                onRequest = { requestMedia() },
                                onWatchlistToggle = { toggleWatchlist() },
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

                    // Trailers
                    val trailers = currentMedia.relatedVideos?.filter { video ->
                        video.site?.equals("YouTube", ignoreCase = true) == true &&
                            (
                                video.type?.equals("Trailer", ignoreCase = true) == true ||
                                    video.type?.equals("Teaser", ignoreCase = true) == true
                                )
                    } ?: emptyList()

                    if (trailers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Trailers",
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
                                items(trailers, key = { it.key ?: it.name ?: "" }) { video ->
                                    MobileSeerrTrailerCard(video = video)
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

@Composable
private fun MobileSeerrRequestSection(
    status: Int?,
    isRequesting: Boolean,
    requestSuccess: Boolean,
    isWatchlisted: Boolean?,
    isWatchlistUpdating: Boolean,
    onRequest: () -> Unit,
    onWatchlistToggle: () -> Unit,
) {
    val (statusColor, statusIcon, statusText, canRequest) = when (status) {
        SeerrMediaStatus.AVAILABLE -> {
            listOf(
                Color(0xFF22C55E),
                Icons.Outlined.Check,
                "Available in Library",
                false,
            )
        }
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> {
            listOf(
                Color(0xFFFBBF24),
                Icons.Outlined.Schedule,
                "Request Pending",
                false,
            )
        }
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> {
            listOf(
                Color(0xFF60A5FA),
                Icons.Outlined.Check,
                "Partially Available",
                true,
            )
        }
        else -> {
            listOf(
                Color(0xFF8B5CF6),
                Icons.Outlined.Add,
                "Not Requested",
                true,
            )
        }
    }

    Column {
        // Status badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    (statusColor as Color).copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = statusIcon as androidx.compose.ui.graphics.vector.ImageVector,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = statusColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText as String,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = statusColor,
            )
        }

        // Request button
        if (canRequest as Boolean) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequest,
                enabled = !isRequesting && !requestSuccess,
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

        if (isWatchlisted != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onWatchlistToggle,
                enabled = !isWatchlistUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWatchlisted) Color(0xFF1F2937) else Color(0xFF2563EB),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isWatchlistUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (isWatchlisted) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isWatchlisted) "In Watchlist" else "Add to Watchlist")
            }
        }
    }
}

@Composable
private fun MobileSeerrCastCard(
    castMember: SeerrCastMember,
    seerrClient: SeerrClient,
    onClick: () -> Unit = {},
) {
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
private fun MobileSeerrCrewCard(
    crewMember: SeerrCrewMember,
    seerrClient: SeerrClient,
) {
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
private fun MobileSeerrRelatedCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
) {
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
private fun MobileSeerrTrailerCard(video: SeerrVideo) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(200.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                video.key?.let { key ->
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/watch?v=$key"),
                    )
                    context.startActivity(intent)
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
                contentDescription = "Play trailer",
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
