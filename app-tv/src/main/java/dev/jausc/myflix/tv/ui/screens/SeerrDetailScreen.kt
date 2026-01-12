@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.util.DateFormatter
import dev.jausc.myflix.core.seerr.SeerrCastMember
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrCrewMember
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrQuotaDetails
import dev.jausc.myflix.core.seerr.SeerrSeason
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

/**
 * Seerr media detail screen for TV.
 *
 * Features:
 * - Full media details (title, overview, cast)
 * - Request button for unavailable content
 * - Season selection for TV shows
 * - Play button if available in Jellyfin
 */
@Suppress("UnusedParameter")
@Composable
fun SeerrDetailScreen(
    mediaType: String,
    tmdbId: Int,
    seerrClient: SeerrClient,
    onPlayInJellyfin: ((String) -> Unit)? = null, // Jellyfin item ID if available
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
    onActorClick: ((Int) -> Unit)? = null, // Person ID
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Focus requester for the main action button
    val actionButtonFocusRequester = remember { FocusRequester() }

    var isLoading by remember { mutableStateOf(true) }
    var media by remember { mutableStateOf<SeerrMedia?>(null) }
    var crew by remember { mutableStateOf<List<SeerrCrewMember>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var similar by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }

    @Suppress("UnusedPrivateProperty")
    var seasons by remember { mutableStateOf<List<SeerrSeason>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRequesting by remember { mutableStateOf(false) }
    var requestSuccess by remember { mutableStateOf(false) }
    var selectedSeasons by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isWatchlisted by remember { mutableStateOf<Boolean?>(null) }
    var isWatchlistUpdating by remember { mutableStateOf(false) }
    var request4k by remember { mutableStateOf(false) }
    var quotaDetails by remember { mutableStateOf<SeerrQuotaDetails?>(null) }

    // Request focus on action button when content loads
    LaunchedEffect(media) {
        if (media != null) {
            kotlinx.coroutines.delay(100)
            try {
                actionButtonFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    // Load media details
    LaunchedEffect(mediaType, tmdbId) {
        isLoading = true
        errorMessage = null
        crew = emptyList()
        recommendations = emptyList()
        similar = emptyList()
        isWatchlisted = null
        selectedSeasons = emptySet()
        request4k = false
        quotaDetails = null

        val result = if (mediaType == "movie") {
            seerrClient.getMovie(tmdbId)
        } else {
            seerrClient.getTVShow(tmdbId)
        }

        result
            .onSuccess {
                media = it
                crew = it.credits?.crew?.take(10) ?: emptyList()
            }
            .onFailure { errorMessage = it.message }

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

        seerrClient.getUserQuota()
            .onSuccess { quota ->
                quotaDetails = if (mediaType == "movie") quota.movie else quota.tv
            }
            .onFailure { quotaDetails = null }

        // Load seasons for TV shows
        if (mediaType == "tv") {
            // Get season info from the media details
            media?.numberOfSeasons?.let { numSeasons ->
                // We'll use season numbers 1 to numSeasons
                // In a real implementation, you'd fetch detailed season info
            }
        }

        isLoading = false
    }

    // Handle request
    fun handleRequest() {
        val currentMedia = media ?: return
        scope.launch {
            isRequesting = true
            val result = if (currentMedia.isMovie) {
                seerrClient.requestMovie(tmdbId, request4k)
            } else {
                val seasonsToRequest = if (selectedSeasons.isNotEmpty()) {
                    selectedSeasons.toList()
                } else {
                    null // Request all seasons
                }
                seerrClient.requestTVShow(tmdbId, seasonsToRequest, request4k)
            }

            result
                .onSuccess {
                    requestSuccess = true
                    // Refresh media to get updated status
                    if (currentMedia.isMovie) {
                        seerrClient.getMovie(tmdbId).onSuccess { media = it }
                    } else {
                        seerrClient.getTVShow(tmdbId).onSuccess { media = it }
                    }
                }
                .onFailure { errorMessage = "Request failed: ${it.message}" }

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
            .background(TvColors.Background),
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            errorMessage != null && media == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Failed to load media",
                        color = TvColors.Error,
                    )
                }
            }

            media != null -> {
                val currentMedia = media!!

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // Hero section with backdrop
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp),
                        ) {
                            // Backdrop
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
                                                TvColors.Background.copy(alpha = 0.8f),
                                                TvColors.Background,
                                            ),
                                        ),
                                    ),
                            )

                            // Back button
                            Button(
                                onClick = onBack,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(24.dp),
                                colors = ButtonDefaults.colors(
                                    containerColor = TvColors.Surface.copy(alpha = 0.7f),
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TvColors.TextPrimary,
                                )
                            }

                            // Content overlay
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 48.dp, vertical = 24.dp),
                            ) {
                                // Poster
                                AsyncImage(
                                    model = seerrClient.getPosterUrl(currentMedia.posterPath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(225.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )

                                Spacer(modifier = Modifier.width(24.dp))

                                // Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentMedia.displayTitle,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TvColors.TextPrimary,
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metadata row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Full release date formatted
                                        currentMedia.displayReleaseDate?.let { dateStr ->
                                            val formattedDate = DateFormatter.formatFull(dateStr) ?: dateStr
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = TvColors.TextSecondary,
                                            )
                                        }

                                        currentMedia.runtime?.let { runtime ->
                                            Text(
                                                text = "${runtime}m",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = TvColors.TextSecondary,
                                            )
                                        }

                                        // TMDb rating with label
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
                                                Text(
                                                    text = "%.1f".format(rating),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFBBF24),
                                                )
                                            }
                                        }

                                        // Status badge (uses mediaInfo.status for availability)
                                        StatusBadge(status = currentMedia.availabilityStatus)
                                    }

                                    // Genres
                                    currentMedia.genres?.let { genres ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = genres.joinToString(" • ") { it.name },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TvColors.TextSecondary,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Request eligibility checks (defined outside Row for later use)
                                    val statusAllowsRequest = currentMedia.availabilityStatus !in listOf(
                                        SeerrMediaStatus.AVAILABLE,
                                        SeerrMediaStatus.PENDING,
                                        SeerrMediaStatus.PROCESSING,
                                    )
                                    val quotaRemaining = quotaDetails?.remaining
                                    val quotaAllowsRequest = quotaRemaining == null || quotaRemaining > 0

                                    // Action buttons
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Request/Available button (uses mediaInfo.status for availability)
                                        when (currentMedia.availabilityStatus) {
                                            SeerrMediaStatus.AVAILABLE -> {
                                                Button(
                                                    onClick = { /* Play in Jellyfin */ },
                                                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                                                    colors = ButtonDefaults.colors(
                                                        containerColor = Color(0xFF22C55E),
                                                    ),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Play")
                                                }
                                            }

                                            SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> {
                                                Button(
                                                    onClick = { },
                                                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                                                    enabled = false,
                                                    colors = ButtonDefaults.colors(
                                                        containerColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                                                        disabledContainerColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                                                    ),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Schedule,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Requested")
                                                }
                                            }

                                            else -> {
                                                Button(
                                                    onClick = { handleRequest() },
                                                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                                                    enabled = !isRequesting && statusAllowsRequest && quotaAllowsRequest,
                                                    colors = ButtonDefaults.colors(
                                                        containerColor = Color(0xFF8B5CF6),
                                                    ),
                                                ) {
                                                    if (isRequesting) {
                                                        TvLoadingIndicator(
                                                            modifier = Modifier.size(20.dp),
                                                            color = TvColors.TextPrimary,
                                                            strokeWidth = 2.dp,
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Add,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(20.dp),
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(if (isRequesting) "Requesting..." else "Request")
                                                }
                                            }
                                        }

                                        if (isWatchlisted != null) {
                                            Button(
                                                onClick = { toggleWatchlist() },
                                                enabled = !isWatchlistUpdating,
                                                colors = ButtonDefaults.colors(
                                                    containerColor = if (isWatchlisted == true) {
                                                        TvColors.Surface
                                                    } else {
                                                        TvColors.BluePrimary
                                                    },
                                                ),
                                            ) {
                                                if (isWatchlistUpdating) {
                                                    TvLoadingIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        color = TvColors.TextPrimary,
                                                        strokeWidth = 2.dp,
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = if (isWatchlisted == true) {
                                                            Icons.Outlined.Check
                                                        } else {
                                                            Icons.Outlined.Add
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isWatchlisted == true) "Watchlist" else "Add to Watchlist")
                                            }
                                        }

                                        // Trailer button (if available)
                                        val trailer = currentMedia.relatedVideos?.find {
                                            it.type == "Trailer" && it.site == "YouTube"
                                        }
                                        trailer?.key?.let { videoKey ->
                                            Button(
                                                onClick = {
                                                    val intent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://www.youtube.com/watch?v=$videoKey"),
                                                    )
                                                    context.startActivity(intent)
                                                },
                                                colors = ButtonDefaults.colors(
                                                    containerColor = Color(0xFFFF0000),
                                                ),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Trailer")
                                            }
                                        }
                                    }

                                    val quotaText = buildQuotaText(quotaDetails)
                                    if (quotaText != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = quotaText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TvColors.TextSecondary,
                                        )
                                    }
                                    if (!quotaAllowsRequest && statusAllowsRequest) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Quota reached",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TvColors.Error,
                                        )
                                    }

                                    if (statusAllowsRequest) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(
                                                onClick = { request4k = !request4k },
                                                colors = if (request4k) {
                                                    ButtonDefaults.colors(
                                                        containerColor = TvColors.BluePrimary,
                                                        contentColor = TvColors.TextPrimary,
                                                        focusedContainerColor = TvColors.BluePrimary,
                                                    )
                                                } else {
                                                    ButtonDefaults.colors(
                                                        containerColor = TvColors.Surface,
                                                        contentColor = TvColors.TextPrimary,
                                                        focusedContainerColor = TvColors.FocusedSurface,
                                                    )
                                                },
                                            ) {
                                                Text("4K")
                                            }
                                        }

                                        val seasonCount = currentMedia.numberOfSeasons
                                        if (currentMedia.isTvShow && seasonCount != null) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Seasons",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = TvColors.TextSecondary,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                item {
                                                    Button(
                                                        onClick = { selectedSeasons = emptySet() },
                                                        colors = if (selectedSeasons.isEmpty()) {
                                                            ButtonDefaults.colors(
                                                                containerColor = TvColors.BluePrimary,
                                                                contentColor = TvColors.TextPrimary,
                                                                focusedContainerColor = TvColors.BluePrimary,
                                                            )
                                                        } else {
                                                            ButtonDefaults.colors(
                                                                containerColor = TvColors.Surface,
                                                                contentColor = TvColors.TextPrimary,
                                                                focusedContainerColor = TvColors.FocusedSurface,
                                                            )
                                                        },
                                                    ) {
                                                        Text("All")
                                                    }
                                                }
                                                items(seasonCount) { index ->
                                                    val seasonNumber = index + 1
                                                    val selected = selectedSeasons.contains(seasonNumber)
                                                    Button(
                                                        onClick = {
                                                            selectedSeasons = if (selected) {
                                                                selectedSeasons - seasonNumber
                                                            } else {
                                                                selectedSeasons + seasonNumber
                                                            }
                                                        },
                                                        colors = if (selected) {
                                                            ButtonDefaults.colors(
                                                                containerColor = TvColors.BluePrimary,
                                                                contentColor = TvColors.TextPrimary,
                                                                focusedContainerColor = TvColors.BluePrimary,
                                                            )
                                                        } else {
                                                            ButtonDefaults.colors(
                                                                containerColor = TvColors.Surface,
                                                                contentColor = TvColors.TextPrimary,
                                                                focusedContainerColor = TvColors.FocusedSurface,
                                                            )
                                                        },
                                                    ) {
                                                        Text("S$seasonNumber")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Overview
                    currentMedia.overview?.let { overview ->
                        item {
                            Column(modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)) {
                                Text(
                                    text = "Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvColors.TextPrimary,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TvColors.TextSecondary,
                                )
                            }
                        }
                    }

                    // TV Show seasons
                    if (currentMedia.isTvShow && currentMedia.numberOfSeasons != null) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)) {
                                Text(
                                    text = "Seasons",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvColors.TextPrimary,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${currentMedia.numberOfSeasons} seasons • ${currentMedia.numberOfEpisodes ?: "?"} episodes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TvColors.TextSecondary,
                                )
                            }
                        }
                    }

                    // Cast
                    currentMedia.credits?.cast?.let { cast ->
                        if (cast.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                    Text(
                                        text = "Cast",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TvColors.TextPrimary,
                                        modifier = Modifier.padding(horizontal = 48.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 48.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(cast.take(20)) { member ->
                                            CastCard(
                                                member = member,
                                                seerrClient = seerrClient,
                                                onClick = {
                                                    onActorClick?.invoke(member.id)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (crew.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                Text(
                                    text = "Crew",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 48.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(crew) { member ->
                                        CrewCard(
                                            member = member,
                                            seerrClient = seerrClient,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (recommendations.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                Text(
                                    text = "Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 48.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(recommendations, key = { it.id }) { item ->
                                        RelatedMediaCard(
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
                    }

                    if (similar.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                Text(
                                    text = "Similar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TvColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 48.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(similar, key = { it.id }) { item ->
                                        RelatedMediaCard(
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
                    }
                }

                // Success message
                if (requestSuccess) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp)
                            .background(Color(0xFF22C55E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request submitted successfully!",
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: Int?) {
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
        else -> return // Don't show badge for unknown/not requested
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

private fun buildQuotaText(quotaDetails: SeerrQuotaDetails?): String? {
    val limit = quotaDetails?.limit ?: return null
    val days = quotaDetails.days
    val remaining = quotaDetails.remaining
    val used = quotaDetails.used

    val windowText = days?.let { " per $it days" } ?: ""
    return if (remaining != null) {
        "Quota: $remaining/$limit remaining$windowText"
    } else {
        "Quota: $used/$limit used$windowText"
    }
}

@Composable
private fun CastCard(
    member: SeerrCastMember,
    seerrClient: SeerrClient,
    onClick: () -> Unit = {},
) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp),
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvColors.Surface,
        ),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
        ) {
            AsyncImage(
                model = seerrClient.getProfileUrl(member.profilePath),
                contentDescription = member.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            member.character?.let { character ->
                Text(
                    text = character,
                    style = MaterialTheme.typography.labelSmall,
                    color = TvColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CrewCard(
    member: SeerrCrewMember,
    seerrClient: SeerrClient,
) {
    androidx.tv.material3.Surface(
        onClick = {},
        modifier = Modifier.width(120.dp),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp),
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvColors.Surface,
        ),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
        ) {
            AsyncImage(
                model = seerrClient.getProfileUrl(member.profilePath),
                contentDescription = member.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            member.job?.let { job ->
                Text(
                    text = job,
                    style = MaterialTheme.typography.labelSmall,
                    color = TvColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RelatedMediaCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(240.dp),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp),
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = seerrClient.getPosterUrl(media.posterPath),
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
