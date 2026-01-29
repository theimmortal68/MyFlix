@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.formattedFullPremiereDate
import dev.jausc.myflix.core.common.ui.IconColors
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.CardSizes
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButton
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.util.Locale

/**
 * Episodes screen for browsing series episodes with focus-driven hero updates.
 *
 * Layout (top to bottom):
 * 1. Hero section with Ken Burns backdrop and episode details
 * 2. Season pill row for season selection
 * 3. Episode card row with initial focus on selected/next-up episode
 *
 * The hero content updates based on which episode card is focused.
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
    onPersonClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Currently focused episode drives hero content
    var focusedEpisode by remember { mutableStateOf<JellyfinItem?>(null) }

    // Track initial focus setup
    var initialFocusSet by remember { mutableStateOf(false) }

    // Focus requesters for navigation flow
    val actionButtonsFocusRequester = remember { FocusRequester() }
    val seasonRowFocusRequester = remember { FocusRequester() }
    val episodeRowFocusRequester = remember { FocusRequester() }

    // Episode focus requesters - keyed by episode ID
    val episodeFocusRequesters = remember(episodes) {
        episodes.associate { it.id to FocusRequester() }
    }

    // Find target episode for initial focus
    val targetEpisode = remember(episodes, selectedEpisodeId) {
        findTargetEpisode(episodes, selectedEpisodeId)
    }

    // Set initial focused episode and focus
    LaunchedEffect(targetEpisode, initialFocusSet) {
        if (targetEpisode != null && !initialFocusSet) {
            focusedEpisode = targetEpisode
            // Delay to ensure episode cards are composed and FocusRequesters attached
            kotlinx.coroutines.delay(100L)
            try {
                episodeFocusRequesters[targetEpisode.id]?.requestFocus()
            } catch (_: IllegalStateException) {
                // FocusRequester not yet attached, ignore
            }
            initialFocusSet = true
        }
    }

    // Build backdrop URL from focused episode
    val backdropUrl = remember(focusedEpisode?.id) {
        focusedEpisode?.let { episode ->
            // Try episode's own backdrop first, then series backdrop
            val backdropTag = episode.backdropImageTags?.firstOrNull()
            when {
                backdropTag != null -> jellyfinClient.getBackdropUrl(episode.id, backdropTag, maxWidth = 1920)
                episode.seriesId != null -> jellyfinClient.getBackdropUrl(episode.seriesId!!, null, maxWidth = 1920)
                else -> jellyfinClient.getPrimaryImageUrl(episode.id, episode.imageTags?.primary, maxWidth = 1920)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .focusGroup(),
    ) {
        // Ken Burns animated backdrop (top-right)
        KenBurnsBackdrop(
            imageUrl = backdropUrl,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .align(Alignment.TopEnd),
        )

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            // Hero section with episode details
            focusedEpisode?.let { episode ->
                EpisodeHeroContent(
                    episode = episode,
                    seriesName = seriesName,
                    onPlayClick = { onEpisodeClick(episode) },
                    onWatchedClick = { onWatchedClick(episode) },
                    onFavoriteClick = { onFavoriteClick(episode) },
                    actionButtonsFocusRequester = actionButtonsFocusRequester,
                    downFocusRequester = seasonRowFocusRequester,
                    modifier = Modifier.fillMaxWidth(),
                )
            } ?: run {
                // Placeholder while loading
                Spacer(modifier = Modifier.height(200.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Season pill row
            if (seasons.isNotEmpty()) {
                SeasonPillRow(
                    seasons = seasons,
                    selectedIndex = selectedSeasonIndex,
                    onSeasonSelected = { index ->
                        onSeasonSelected(index)
                    },
                    focusRequester = seasonRowFocusRequester,
                    upFocusRequester = actionButtonsFocusRequester,
                    downFocusRequester = episodeRowFocusRequester,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Episode row
            EpisodeCardRow(
                episodes = episodes,
                jellyfinClient = jellyfinClient,
                selectedEpisodeId = targetEpisode?.id,
                episodeFocusRequesters = episodeFocusRequesters,
                onEpisodeClick = onEpisodeClick,
                onEpisodeFocused = { episode -> focusedEpisode = episode },
                focusRequester = episodeRowFocusRequester,
                upFocusRequester = seasonRowFocusRequester,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Finds the target episode for initial focus.
 * Priority: selectedEpisodeId > in-progress > first unwatched > first episode
 */
private fun findTargetEpisode(
    episodes: List<JellyfinItem>,
    selectedEpisodeId: String?,
): JellyfinItem? {
    if (episodes.isEmpty()) return null

    // 1. Specific episode from navigation
    if (selectedEpisodeId != null) {
        episodes.find { it.id == selectedEpisodeId }?.let { return it }
    }

    // 2. In-progress episode (Continue Watching)
    episodes.find { (it.userData?.playbackPositionTicks ?: 0L) > 0L }?.let { return it }

    // 3. First unwatched episode (Next Up)
    episodes.find { it.userData?.played != true }?.let { return it }

    // 4. First episode (fallback)
    return episodes.firstOrNull()
}

/**
 * Finds the target episode within a season for focus after season change.
 */
private fun findSeasonTargetEpisode(episodes: List<JellyfinItem>): JellyfinItem? {
    if (episodes.isEmpty()) return null

    // 1. In-progress episode (Continue Watching)
    episodes.find { (it.userData?.playbackPositionTicks ?: 0L) > 0L }?.let { return it }

    // 2. First unwatched episode (Next Up)
    episodes.find { it.userData?.played != true }?.let { return it }

    // 3. First episode (fallback)
    return episodes.firstOrNull()
}

// region Hero Content

/**
 * Hero content section showing focused episode details.
 */
@Composable
private fun EpisodeHeroContent(
    episode: JellyfinItem,
    seriesName: String,
    onPlayClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    actionButtonsFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val isWatched = episode.userData?.played == true
    val isFavorite = episode.userData?.isFavorite == true
    val positionTicks = episode.userData?.playbackPositionTicks ?: 0L
    val hasProgress = positionTicks > 0L

    val remainingMinutes = if (hasProgress && episode.runTimeTicks != null) {
        val remainingTicks = episode.runTimeTicks!! - positionTicks
        (remainingTicks / 600_000_000L).toInt().coerceAtLeast(0)
    } else {
        0
    }

    Column(modifier = modifier) {
        // Series name as main title
        Text(
            text = seriesName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Episode name
        Text(
            text = episode.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata and description constrained width
        Column(modifier = Modifier.fillMaxWidth(0.5f)) {
            // Metadata row
            EpisodeMetadataRow(episode = episode)

            Spacer(modifier = Modifier.height(8.dp))

            // Overview
            episode.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            EpisodeActionButtons(
                isWatched = isWatched,
                isFavorite = isFavorite,
                hasProgress = hasProgress,
                remainingMinutes = remainingMinutes,
                onPlayClick = onPlayClick,
                onWatchedClick = onWatchedClick,
                onFavoriteClick = onFavoriteClick,
                focusRequester = actionButtonsFocusRequester,
                downFocusRequester = downFocusRequester,
            )
        }
    }
}

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
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            needsDot = true
        }

        // Air date
        episode.formattedFullPremiereDate?.let { date ->
            if (needsDot) MetadataDot()
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
                if (needsDot) MetadataDot()
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
                needsDot = true
            }
        }

        // Official rating
        episode.officialRating?.let { rating ->
            if (needsDot) MetadataDot()
            RatingBadge(rating)
            needsDot = true
        }

        // Community rating
        episode.communityRating?.let { rating ->
            if (needsDot) MetadataDot()
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
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TvColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun MetadataDot() {
    Text(
        text = "•",
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.6f),
    )
}

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

@Composable
private fun EpisodeActionButtons(
    isWatched: Boolean,
    isFavorite: Boolean,
    hasProgress: Boolean,
    remainingMinutes: Int,
    onPlayClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
) {
    val playButtonText = when {
        hasProgress && remainingMinutes > 0 -> "Resume · ${remainingMinutes}m left"
        hasProgress -> "Resume"
        else -> "Play"
    }

    val playFocusRequester = remember { FocusRequester() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusGroup()
            .focusRestorer(playFocusRequester),
    ) {
        item("play") {
            ExpandablePlayButton(
                title = playButtonText,
                icon = Icons.Outlined.PlayArrow,
                iconColor = if (hasProgress) IconColors.Resume else IconColors.Play,
                onClick = onPlayClick,
                modifier = Modifier
                    .focusRequester(playFocusRequester)
                    .focusProperties { down = downFocusRequester },
            )
        }

        item("watched") {
            ExpandablePlayButton(
                title = if (isWatched) "Mark Unwatched" else "Mark Watched",
                icon = if (isWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = Modifier.focusProperties { down = downFocusRequester },
            )
        }

        item("favorite") {
            ExpandablePlayButton(
                title = if (isFavorite) "Remove Favorite" else "Add to Favorites",
                icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (isFavorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = Modifier.focusProperties { down = downFocusRequester },
            )
        }
    }
}

// endregion

// region Season Pills

/**
 * Horizontal row of season pill chips.
 */
@Composable
private fun SeasonPillRow(
    seasons: List<JellyfinItem>,
    selectedIndex: Int,
    onSeasonSelected: (Int) -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val firstPillFocusRequester = remember { FocusRequester() }

    // Scroll to selected season when it changes
    LaunchedEffect(selectedIndex) {
        lazyListState.animateScrollToItem(selectedIndex.coerceIn(0, seasons.lastIndex.coerceAtLeast(0)))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        // Section label
        Text(
            text = "Seasons",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TvColors.TextSecondary,
        )

        LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusRestorer(firstPillFocusRequester),
        ) {
            itemsIndexed(seasons, key = { _, season -> season.id }) { index, season ->
                val isSelected = index == selectedIndex
                val pillFocusRequester = if (index == 0) firstPillFocusRequester else remember { FocusRequester() }

                SeasonPill(
                    season = season,
                    index = index,
                    isSelected = isSelected,
                    onClick = {
                        onSeasonSelected(index)
                    },
                    modifier = Modifier
                        .focusRequester(pillFocusRequester)
                        .focusProperties {
                            up = upFocusRequester
                            down = downFocusRequester
                        },
                )
            }
        }
    }
}

@Composable
private fun SeasonPill(
    season: JellyfinItem,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    // Display just the season number
    val displayNumber = remember(season.indexNumber, index) {
        (season.indexNumber ?: (index + 1)).toString()
    }

    // Match action button styling
    val backgroundColor = when {
        isFocused -> TvColors.BluePrimary
        isSelected -> TvColors.BluePrimary.copy(alpha = 0.3f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    val borderColor = when {
        isFocused -> Color.Transparent
        isSelected -> TvColors.BluePrimary.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.2f)
    }

    val textColor = when {
        isFocused -> Color.White
        isSelected -> TvColors.TextPrimary
        else -> TvColors.TextPrimary
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = TvColors.BluePrimary,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(6.dp),
            ),
            focusedBorder = Border(
                border = BorderStroke(0.dp, Color.Transparent),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp),
        ) {
            Text(
                text = displayNumber,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
            )
        }
    }
}

// endregion

// region Episode Row

/**
 * Horizontal row of episode cards with focus handling.
 */
@Composable
private fun EpisodeCardRow(
    episodes: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    selectedEpisodeId: String?,
    episodeFocusRequesters: Map<String, FocusRequester>,
    onEpisodeClick: (JellyfinItem) -> Unit,
    onEpisodeFocused: (JellyfinItem) -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    // Find index of selected episode for initial scroll
    val selectedIndex = remember(episodes, selectedEpisodeId) {
        episodes.indexOfFirst { it.id == selectedEpisodeId }.takeIf { it >= 0 } ?: 0
    }

    // Scroll to selected episode
    LaunchedEffect(selectedIndex) {
        if (selectedIndex > 0) {
            lazyListState.scrollToItem(selectedIndex)
        }
    }

    val firstCardFocusRequester = remember { FocusRequester() }

    Column(modifier = modifier) {
        // Section label with episode count
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TvColors.TextSecondary,
            )
            if (episodes.isNotEmpty()) {
                Text(
                    text = "(${episodes.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary.copy(alpha = 0.7f),
                )
            }
        }

        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No episodes available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                )
            }
        } else {
            LazyRow(
                state = lazyListState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusRestorer(firstCardFocusRequester),
            ) {
                itemsIndexed(episodes, key = { _, episode -> episode.id }) { index, episode ->
                    val cardFocusRequester = episodeFocusRequesters[episode.id]
                        ?: (if (index == 0) firstCardFocusRequester else remember { FocusRequester() })

                    EpisodeCard(
                        episode = episode,
                        jellyfinClient = jellyfinClient,
                        onClick = { onEpisodeClick(episode) },
                        onFocused = { onEpisodeFocused(episode) },
                        modifier = Modifier
                            .focusRequester(cardFocusRequester)
                            .focusProperties { up = upFocusRequester },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = jellyfinClient.getPrimaryImageUrl(
        episode.id,
        episode.imageTags?.primary,
        maxWidth = 400,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        WideMediaCard(
            item = episode,
            imageUrl = imageUrl,
            onClick = onClick,
            onLongClick = {},
            showLabel = false,
        )

        // Episode info below card
        Column(modifier = Modifier.width(CardSizes.WideMediaCardWidth)) {
            Text(
                text = buildString {
                    episode.indexNumber?.let { append("$it. ") }
                    append(episode.name)
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            episode.runTimeTicks?.let { ticks ->
                val minutes = (ticks / 600_000_000L).toInt()
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
                )
            }
        }
    }
}

// endregion

// region Ken Burns Backdrop

/**
 * Ken Burns effect backdrop with animated zoom and pan.
 */
@Composable
private fun KenBurnsBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

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

        // Gradient overlay for text readability
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

// endregion
