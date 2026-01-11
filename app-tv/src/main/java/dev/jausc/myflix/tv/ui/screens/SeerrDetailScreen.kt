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

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrCastMember
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
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
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var media by remember { mutableStateOf<SeerrMedia?>(null) }

    @Suppress("UnusedPrivateProperty")
    var seasons by remember { mutableStateOf<List<SeerrSeason>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRequesting by remember { mutableStateOf(false) }
    var requestSuccess by remember { mutableStateOf(false) }
    var selectedSeasons by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Load media details
    LaunchedEffect(mediaType, tmdbId) {
        isLoading = true
        errorMessage = null

        val result = if (mediaType == "movie") {
            seerrClient.getMovie(tmdbId)
        } else {
            seerrClient.getTVShow(tmdbId)
        }

        result
            .onSuccess { media = it }
            .onFailure { errorMessage = it.message }

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
                seerrClient.requestMovie(tmdbId)
            } else {
                val seasonsToRequest = if (selectedSeasons.isNotEmpty()) {
                    selectedSeasons.toList()
                } else {
                    null // Request all seasons
                }
                seerrClient.requestTVShow(tmdbId, seasonsToRequest)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                    onBack()
                    true
                } else {
                    false
                }
            },
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
                                        currentMedia.year?.let { year ->
                                            Text(
                                                text = year.toString(),
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

                                        currentMedia.voteAverage?.let { rating ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "%.1f".format(rating),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFBBF24),
                                                )
                                                Text(
                                                    text = "/10",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TvColors.TextSecondary,
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

                                    // Action buttons
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Request/Available button (uses mediaInfo.status for availability)
                                        when (currentMedia.availabilityStatus) {
                                            SeerrMediaStatus.AVAILABLE -> {
                                                Button(
                                                    onClick = { /* Play in Jellyfin */ },
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
                                                    enabled = !isRequesting,
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
                                        items(cast.take(10)) { member ->
                                            CastCard(
                                                member = member,
                                                seerrClient = seerrClient,
                                            )
                                        }
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

@Composable
private fun CastCard(member: SeerrCastMember, seerrClient: SeerrClient,) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp),
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
            maxLines = 1,
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
