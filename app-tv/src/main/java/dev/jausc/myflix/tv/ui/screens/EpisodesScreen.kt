@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.formattedFullPremiereDate
import dev.jausc.myflix.core.common.ui.IconColors
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButton
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.util.Locale

/**
 * Episodes screen for browsing series episodes.
 * Displays a Ken Burns animated backdrop with episode content overlaid.
 */
@Composable
fun EpisodesScreen(
    seriesName: String,
    seasons: List<JellyfinItem>,
    episodes: List<JellyfinItem>,
    selectedSeasonIndex: Int,
    selectedEpisodeId: String? = null,
    jellyfinClient: JellyfinClient,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (JellyfinItem) -> Unit,
    onWatchedClick: (JellyfinItem) -> Unit = {},
    onFavoriteClick: (JellyfinItem) -> Unit = {},
    onPersonClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Get the selected season for backdrop
    val selectedSeason = seasons.getOrNull(selectedSeasonIndex)

    // Build backdrop URL - prefer season backdrop, fallback to first episode backdrop
    val backdropUrl = remember(selectedSeason?.id, episodes.firstOrNull()?.id) {
        val seasonBackdropTag = selectedSeason?.backdropImageTags?.firstOrNull()
        when {
            seasonBackdropTag != null -> {
                jellyfinClient.getBackdropUrl(selectedSeason.id, seasonBackdropTag, maxWidth = 1920)
            }
            selectedSeason != null -> {
                // Try series backdrop via season's seriesId
                selectedSeason.seriesId?.let { seriesId ->
                    jellyfinClient.getBackdropUrl(seriesId, null, maxWidth = 1920)
                }
            }
            else -> {
                // Fallback to first episode's backdrop
                episodes.firstOrNull()?.let { episode ->
                    jellyfinClient.getBackdropUrl(episode.id, episode.imageTags?.primary, maxWidth = 1920)
                }
            }
        }
    }

    // Get episode from the list
    val episodeFromList = if (selectedEpisodeId != null) {
        episodes.find { it.id == selectedEpisodeId }
    } else {
        episodes.firstOrNull()
    }

    // Stable episode state - survives recomposition and prevents null flicker
    var stableEpisode by remember { mutableStateOf<JellyfinItem?>(null) }
    var directlyFetchedEpisode by remember { mutableStateOf<JellyfinItem?>(null) }

    // Fetch episode directly if not in list (handles wrong season loaded case)
    LaunchedEffect(selectedEpisodeId) {
        if (selectedEpisodeId != null && stableEpisode?.id != selectedEpisodeId) {
            jellyfinClient.getItem(selectedEpisodeId)
                .onSuccess { episode ->
                    directlyFetchedEpisode = episode
                }
        }
    }

    // Update stableEpisode only when we have a valid new episode
    // This prevents null flicker during state transitions
    LaunchedEffect(episodeFromList, directlyFetchedEpisode, selectedEpisodeId) {
        val newEpisode = when {
            selectedEpisodeId != null && directlyFetchedEpisode?.id == selectedEpisodeId -> directlyFetchedEpisode
            episodeFromList != null -> episodeFromList
            directlyFetchedEpisode != null -> directlyFetchedEpisode
            else -> null
        }
        // Only update if we have a valid episode - never set back to null
        if (newEpisode != null) {
            stableEpisode = newEpisode
        }
    }

    val selectedEpisode = stableEpisode

    // Focus requester for play button (default focus)
    val playButtonFocusRequester = remember { FocusRequester() }

    // Track if initial focus has been set (to avoid repeated focus stealing)
    var initialFocusSet by remember { mutableStateOf(false) }

    // Request initial focus on play button once episode data is available
    LaunchedEffect(selectedEpisode) {
        if (selectedEpisode != null && !initialFocusSet) {
            playButtonFocusRequester.requestFocus()
            initialFocusSet = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .focusGroup()
            .focusProperties {
                // Block left escape to NavRail during loading/transitions
                left = FocusRequester.Cancel
            },
    ) {
        // Ken Burns animated backdrop (top-right, same as UnifiedSeriesScreen)
        KenBurnsBackdrop(
            imageUrl = backdropUrl,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .align(Alignment.TopEnd),
        )

        // Hero text content overlay (left side, matching HomeScreen exactly)
        selectedEpisode?.let { episode ->
            EpisodeHeroText(
                episode = episode,
                seriesName = seriesName,
                onPlayClick = { onEpisodeClick(episode) },
                onWatchedClick = { onWatchedClick(episode) },
                onFavoriteClick = { onFavoriteClick(episode) },
                playButtonFocusRequester = playButtonFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
            )
        }
    }
}

/**
 * Episode hero text content matching HomeScreen HeroSection exactly.
 * Shows series name, episode name, metadata row, description, and action buttons.
 */
@Composable
private fun EpisodeHeroText(
    episode: JellyfinItem,
    seriesName: String,
    onPlayClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val isWatched = episode.userData?.played == true
    val isFavorite = episode.userData?.isFavorite == true
    val positionTicks = episode.userData?.playbackPositionTicks ?: 0L
    val hasProgress = positionTicks > 0L

    // Calculate remaining time for Resume button
    val remainingMinutes = if (hasProgress && episode.runTimeTicks != null) {
        val remainingTicks = episode.runTimeTicks!! - positionTicks
        (remainingTicks / 600_000_000L).toInt().coerceAtLeast(0)
    } else {
        0
    }

    Column(modifier = modifier) {
        // Title section - Series name large, episode name as subtitle
        Column {
            // Series name as main large title
            Text(
                text = seriesName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Episode name as subtitle
            Text(
                text = episode.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata and description constrained to 55% width
        Column(modifier = Modifier.fillMaxWidth(0.55f)) {
            // Rating/metadata row
            EpisodeMetadataRow(episode = episode)

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            episode.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(0.8f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons row
            EpisodeActionButtonsRow(
                isWatched = isWatched,
                isFavorite = isFavorite,
                hasProgress = hasProgress,
                remainingMinutes = remainingMinutes,
                onPlayClick = onPlayClick,
                onWatchedClick = onWatchedClick,
                onFavoriteClick = onFavoriteClick,
                playButtonFocusRequester = playButtonFocusRequester,
            )
        }
    }
}

/**
 * Action buttons row for episode: Play/Resume, Mark as Watched, Add to Favorites.
 */
@Composable
private fun EpisodeActionButtonsRow(
    isWatched: Boolean,
    isFavorite: Boolean,
    hasProgress: Boolean,
    remainingMinutes: Int,
    onPlayClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
) {
    // Build play button text with remaining time
    val playButtonText = when {
        hasProgress && remainingMinutes > 0 -> "Resume · ${remainingMinutes}m left"
        hasProgress -> "Resume"
        else -> "Play"
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier
            .focusGroup()
            .focusRestorer(playButtonFocusRequester),
    ) {
        // Play/Resume button
        item("play") {
            ExpandablePlayButton(
                title = playButtonText,
                icon = Icons.Outlined.PlayArrow,
                iconColor = if (hasProgress) IconColors.Resume else IconColors.Play,
                onClick = onPlayClick,
                modifier = Modifier.focusRequester(playButtonFocusRequester),
            )
        }

        // Watched button
        item("watched") {
            ExpandablePlayButton(
                title = if (isWatched) "Mark Unwatched" else "Mark Watched",
                icon = if (isWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
            )
        }

        // Favorite button
        item("favorite") {
            ExpandablePlayButton(
                title = if (isFavorite) "Remove Favorite" else "Add to Favorites",
                icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (isFavorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
            )
        }
    }
}

/**
 * Episode metadata row: S# E# · air date · duration · parental rating · community rating
 * Matches HomeScreen HeroRatingRow for episodes exactly.
 */
@Composable
private fun EpisodeMetadataRow(episode: JellyfinItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var needsDot = false

        // Season and Episode number
        val season = episode.parentIndexNumber
        val episodeNum = episode.indexNumber
        if (season != null && episodeNum != null) {
            Text(
                text = "S$season E$episodeNum",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            needsDot = true
        }

        // Full air date
        episode.formattedFullPremiereDate?.let { date ->
            if (needsDot) DotSeparator()
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            needsDot = true
        }

        // Runtime
        episode.runTimeTicks?.let { ticks ->
            val minutes = (ticks / 600_000_000).toInt()
            if (minutes > 0) {
                if (needsDot) DotSeparator()
                RuntimeDisplay(minutes)
                needsDot = true
            }
        }

        // Official rating badge (PG-13, TV-MA, etc.)
        episode.officialRating?.let { rating ->
            if (needsDot) DotSeparator()
            RatingBadge(rating)
            needsDot = true
        }

        // Community rating (star rating)
        episode.communityRating?.let { rating ->
            if (needsDot) DotSeparator()
            StarRating(rating)
        }
    }
}

/**
 * Small dot separator for metadata rows.
 */
@Composable
private fun DotSeparator() {
    Text(
        text = "•",
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.6f),
    )
}

/**
 * Runtime display in hours and minutes format.
 */
@Composable
private fun RuntimeDisplay(minutes: Int) {
    val hours = minutes / 60
    val mins = minutes % 60
    val text = when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.9f),
    )
}

/**
 * Badge for official ratings (PG-13, TV-MA, etc.) with color-coded backgrounds.
 */
@Composable
private fun RatingBadge(text: String) {
    val backgroundColor = when (text.uppercase()) {
        "G", "TV-G", "TV-Y", "TV-Y7", "TV-Y7-FV" -> Color(0xFF2E7D32)
        "PG", "TV-PG" -> Color(0xFF1565C0)
        "PG-13", "TV-14", "16" -> Color(0xFFF57C00)
        "R", "TV-MA", "NC-17", "NR", "UNRATED" -> Color(0xFFC62828)
        else -> Color(0xFF616161)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = Color.White,
        )
    }
}

/**
 * Star rating display (community rating out of 10).
 */
@Composable
private fun StarRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFFD700),
        )
        Text(
            text = String.format(Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Ken Burns effect backdrop - slow zoom and pan animation.
 * Copied from UnifiedSeriesScreen for consistency.
 */
@Composable
private fun KenBurnsBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")

    // Slow zoom: 1.0 -> 1.1 over 20 seconds
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    // Subtle horizontal pan: -2% to +2%
    val translateX by infiniteTransition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "translateX",
    )

    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .build()
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = size.width * translateX
                }
                .drawWithCache {
                    // Edge fade masks
                    val leftFade = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.15f to Color.Black.copy(alpha = 0.5f),
                            0.35f to Color.Black,
                            1.0f to Color.Black,
                        ),
                    )
                    val bottomFade = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            0.6f to Color.Black,
                            1.0f to Color.Transparent,
                        ),
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(leftFade, blendMode = BlendMode.DstIn)
                        drawRect(bottomFade, blendMode = BlendMode.DstIn)
                    }
                },
        )

        // Additional gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to TvColors.Background.copy(alpha = 0.8f),
                            0.3f to TvColors.Background.copy(alpha = 0.4f),
                            0.6f to Color.Transparent,
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}
