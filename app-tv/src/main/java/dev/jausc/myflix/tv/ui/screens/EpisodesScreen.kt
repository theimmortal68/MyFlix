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
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.formattedFullPremiereDate
import dev.jausc.myflix.core.common.ui.IconColors
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.CardSizes
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButton
import dev.jausc.myflix.tv.ui.components.detail.PersonCard
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Tab options for the episode detail screen.
 */
private enum class EpisodeTab {
    Details,
    MediaInfo,
    CastCrew,
    GuestStars,
    Seasons,
}

/**
 * Episodes screen for browsing series episodes with focus-driven hero updates.
 *
 * Layout (top to bottom):
 * 1. Hero section with Ken Burns backdrop and episode details
 * 2. Episode card row directly below action buttons
 * 3. Tab section at bottom [Details, Media Info, Cast & Crew, Guest Stars, Seasons]
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
    val episodeRowFocusRequester = remember { FocusRequester() }
    val firstTabFocusRequester = remember { FocusRequester() }

    // Episode focus requesters - keyed by episode ID
    val episodeFocusRequesters = remember(episodes) {
        episodes.associate { it.id to FocusRequester() }
    }

    // Find target episode for initial focus
    val targetEpisode = remember(episodes, selectedEpisodeId) {
        findTargetEpisode(episodes, selectedEpisodeId)
    }

    // Set initial focused episode
    LaunchedEffect(targetEpisode, initialFocusSet) {
        if (targetEpisode != null && !initialFocusSet) {
            focusedEpisode = targetEpisode
            // Delay to ensure UI is composed
            delay(100L)
            try {
                // Focus the play button initially
                actionButtonsFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // FocusRequester not yet attached, ignore
            }
            initialFocusSet = true
        }
    }

    // Build backdrop URL from focused episode
    val backdropUrl = remember(focusedEpisode?.id) {
        focusedEpisode?.let { episode ->
            val backdropTag = episode.backdropImageTags?.firstOrNull()
            when {
                backdropTag != null -> jellyfinClient.getBackdropUrl(episode.id, backdropTag, maxWidth = 1920)
                episode.seriesId != null -> jellyfinClient.getBackdropUrl(episode.seriesId!!, null, maxWidth = 1920)
                else -> jellyfinClient.getPrimaryImageUrl(episode.id, episode.imageTags?.primary, maxWidth = 1920)
            }
        }
    }

    // Tab state - default to Seasons
    var selectedTab by rememberSaveable { mutableStateOf(EpisodeTab.Seasons) }

    // Get guest stars from focused episode
    val guestStars = remember(focusedEpisode) {
        focusedEpisode?.people?.filter { it.type == "GuestStar" } ?: emptyList()
    }

    // Filter tabs based on available data
    val availableTabs = remember(focusedEpisode, guestStars) {
        EpisodeTab.entries.filter { tab ->
            when (tab) {
                EpisodeTab.GuestStars -> guestStars.isNotEmpty()
                EpisodeTab.CastCrew -> focusedEpisode?.people?.isNotEmpty() == true
                EpisodeTab.MediaInfo -> focusedEpisode?.mediaSources?.firstOrNull()?.mediaStreams?.isNotEmpty() == true
                else -> true
            }
        }
    }

    // Handle tab selection when tab becomes unavailable
    LaunchedEffect(availableTabs) {
        if (selectedTab !in availableTabs) {
            selectedTab = availableTabs.firstOrNull() ?: EpisodeTab.Seasons
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
                .fillMaxHeight(0.85f)
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
                    episodeRowFocusRequester = episodeRowFocusRequester,
                    modifier = Modifier.fillMaxWidth(0.55f),
                )
            } ?: run {
                // Placeholder while loading
                Spacer(modifier = Modifier.height(150.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Episode row directly in hero area
            EpisodeCardRow(
                episodes = episodes,
                jellyfinClient = jellyfinClient,
                selectedEpisodeId = targetEpisode?.id,
                episodeFocusRequesters = episodeFocusRequesters,
                onEpisodeClick = onEpisodeClick,
                onEpisodeFocused = { episode -> focusedEpisode = episode },
                focusRequester = episodeRowFocusRequester,
                upFocusRequester = actionButtonsFocusRequester,
                downFocusRequester = firstTabFocusRequester,
                modifier = Modifier.fillMaxWidth(),
            )

            // Spacer pushes tabs to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Tab section with shaded background
            val coroutineScope = rememberCoroutineScope()
            var tabChangeJob by remember { mutableStateOf<Job?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 10.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.9f),
                            ),
                        ),
                    )
                    .padding(top = 12.dp),
            ) {
                Column {
                    // Tab row with debounced focus change
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        availableTabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == tab
                            var isFocused by remember { mutableStateOf(false) }

                            val tabModifier = if (index == 0) {
                                Modifier.focusRequester(firstTabFocusRequester)
                            } else {
                                Modifier
                            }

                            Column(
                                modifier = tabModifier
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            tabChangeJob?.cancel()
                                            tabChangeJob = coroutineScope.launch {
                                                delay(150)
                                                selectedTab = tab
                                            }
                                        }
                                    }
                                    .focusProperties {
                                        up = episodeRowFocusRequester
                                    }
                                    .focusable()
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { selectedTab = tab },
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = when (tab) {
                                        EpisodeTab.Details -> "Details"
                                        EpisodeTab.MediaInfo -> "Media Info"
                                        EpisodeTab.CastCrew -> "Cast & Crew"
                                        EpisodeTab.GuestStars -> "Guest Stars"
                                        EpisodeTab.Seasons -> "Seasons"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> Color.White
                                        isFocused -> Color.White.copy(alpha = 0.8f)
                                        else -> TvColors.TextSecondary
                                    },
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(if (isSelected) 40.dp else 0.dp)
                                        .height(2.dp)
                                        .background(
                                            if (isSelected) TvColors.BluePrimary else Color.Transparent,
                                            RoundedCornerShape(1.dp),
                                        ),
                                )
                            }
                        }
                    }

                    // Tab content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(start = 2.dp),
                    ) {
                        when (selectedTab) {
                            EpisodeTab.Details -> {
                                focusedEpisode?.let { episode ->
                                    DetailsTabContent(episode = episode)
                                }
                            }
                            EpisodeTab.MediaInfo -> {
                                focusedEpisode?.let { episode ->
                                    MediaInfoTabContent(
                                        mediaStreams = episode.mediaSources?.firstOrNull()?.mediaStreams ?: emptyList(),
                                    )
                                }
                            }
                            EpisodeTab.CastCrew -> {
                                focusedEpisode?.let { episode ->
                                    CastCrewTabContent(
                                        people = episode.people?.filter { it.type != "GuestStar" } ?: emptyList(),
                                        jellyfinClient = jellyfinClient,
                                        onPersonClick = onPersonClick,
                                    )
                                }
                            }
                            EpisodeTab.GuestStars -> {
                                GuestStarsTabContent(
                                    guestStars = guestStars,
                                    jellyfinClient = jellyfinClient,
                                    onPersonClick = onPersonClick,
                                )
                            }
                            EpisodeTab.Seasons -> {
                                SeasonsTabContent(
                                    seasons = seasons,
                                    selectedSeasonIndex = selectedSeasonIndex,
                                    jellyfinClient = jellyfinClient,
                                    onSeasonSelected = onSeasonSelected,
                                )
                            }
                        }
                    }
                }
            }
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
    episodeRowFocusRequester: FocusRequester,
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
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Episode name
        Text(
            text = episode.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata row
        EpisodeMetadataRow(episode = episode)

        Spacer(modifier = Modifier.height(6.dp))

        // Overview - 2 lines max
        episode.overview?.let { overview ->
            Text(
                text = overview,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth(0.9f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons row
        EpisodeActionButtons(
            isWatched = isWatched,
            isFavorite = isFavorite,
            hasProgress = hasProgress,
            remainingMinutes = remainingMinutes,
            onPlayClick = onPlayClick,
            onWatchedClick = onWatchedClick,
            onFavoriteClick = onFavoriteClick,
            focusRequester = actionButtonsFocusRequester,
            downFocusRequester = episodeRowFocusRequester,
        )
    }
}

@Composable
private fun EpisodeMetadataRow(episode: JellyfinItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Season and Episode number
        val season = episode.parentIndexNumber
        val episodeNum = episode.indexNumber
        if (season != null && episodeNum != null) {
            Text(
                text = "S$season E$episodeNum",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
        }

        // Air date
        episode.formattedFullPremiereDate?.let { date ->
            MetadataDot()
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
        }

        // Runtime
        episode.runTimeTicks?.let { ticks ->
            val minutes = (ticks / 600_000_000).toInt()
            if (minutes > 0) {
                MetadataDot()
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                )
            }
        }

        // Official rating
        episode.officialRating?.let { rating ->
            MetadataDot()
            RatingBadge(rating)
        }

        // Community rating
        episode.communityRating?.let { rating ->
            MetadataDot()
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
    downFocusRequester: FocusRequester,
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
        Text(
            text = "Episodes (${episodes.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TvColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
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
                            .focusProperties {
                                up = upFocusRequester
                                down = downFocusRequester
                                if (index == 0) {
                                    left = FocusRequester.Cancel
                                }
                            },
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
        // 16:9 thumbnail card
        Card(
            onClick = onClick,
            modifier = Modifier.size(width = 200.dp, height = 112.dp),
            scale = CardDefaults.scale(focusedScale = 1.03f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvColors.BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                ),
            ),
            shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Progress bar
                val positionTicks = episode.userData?.playbackPositionTicks ?: 0L
                if (positionTicks > 0L) {
                    val totalTicks = episode.runTimeTicks ?: 1L
                    val progress = (positionTicks.toFloat() / totalTicks.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(TvColors.BluePrimary),
                        )
                    }
                }
            }
        }

        // Episode info below card
        Column(modifier = Modifier.width(200.dp)) {
            Text(
                text = buildString {
                    episode.indexNumber?.let { append("$it. ") }
                    append(episode.name)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            episode.runTimeTicks?.let { ticks ->
                val minutes = (ticks / 600_000_000L).toInt()
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = TvColors.TextSecondary,
                )
            }
        }
    }
}

// endregion

// region Tab Content

/**
 * Details tab - shows episode overview, directors, writers, etc.
 */
@Composable
private fun DetailsTabContent(episode: JellyfinItem) {
    val directors = episode.people?.filter { it.type == "Director" } ?: emptyList()
    val writers = episode.people?.filter { it.type == "Writer" } ?: emptyList()

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Overview section
        episode.overview?.let { overview ->
            item("overview") {
                Column(
                    modifier = Modifier.width(400.dp),
                ) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleSmall,
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        // Details section
        item("details") {
            Column(
                modifier = Modifier.width(200.dp),
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleSmall,
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                episode.premiereDate?.let {
                    DetailItem("Air Date", episode.formattedFullPremiereDate ?: "Unknown")
                }
                episode.runTimeTicks?.let { ticks ->
                    val minutes = (ticks / 600_000_000).toInt()
                    DetailItem("Runtime", "${minutes}m")
                }
                episode.officialRating?.let {
                    DetailItem("Rating", it)
                }
                episode.communityRating?.let {
                    DetailItem("Score", String.format(Locale.US, "%.1f", it))
                }
            }
        }

        // Directors
        if (directors.isNotEmpty()) {
            item("directors") {
                Column(
                    modifier = Modifier.width(200.dp),
                ) {
                    Text(
                        text = "Director${if (directors.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    directors.forEach { director ->
                        Text(
                            text = director.name ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary,
                        )
                    }
                }
            }
        }

        // Writers
        if (writers.isNotEmpty()) {
            item("writers") {
                Column(
                    modifier = Modifier.width(200.dp),
                ) {
                    Text(
                        text = "Writer${if (writers.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    writers.forEach { writer ->
                        Text(
                            text = writer.name ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Media Info tab - shows video/audio/subtitle streams.
 */
@Composable
private fun MediaInfoTabContent(mediaStreams: List<MediaStream>) {
    val videoStreams = mediaStreams.filter { it.type == "Video" }
    val audioStreams = mediaStreams.filter { it.type == "Audio" }
    val subtitleStreams = mediaStreams.filter { it.type == "Subtitle" }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Video info
        if (videoStreams.isNotEmpty()) {
            item("video") {
                Column(modifier = Modifier.width(200.dp)) {
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.titleSmall,
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    videoStreams.firstOrNull()?.let { stream ->
                        stream.codec?.let { DetailItem("Codec", it.uppercase()) }
                        if (stream.width != null && stream.height != null) {
                            DetailItem("Resolution", "${stream.width}x${stream.height}")
                        }
                        stream.aspectRatio?.let { DetailItem("Aspect", it) }
                        stream.bitRate?.let {
                            DetailItem("Bitrate", "${it / 1_000_000} Mbps")
                        }
                    }
                }
            }
        }

        // Audio info
        if (audioStreams.isNotEmpty()) {
            item("audio") {
                Column(modifier = Modifier.width(250.dp)) {
                    Text(
                        text = "Audio (${audioStreams.size} tracks)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    audioStreams.take(4).forEach { stream ->
                        val info = buildString {
                            append(stream.language?.uppercase() ?: "UND")
                            stream.codec?.let { append(" - ${it.uppercase()}") }
                            stream.channels?.let { append(" ${it}ch") }
                        }
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary,
                        )
                    }
                    if (audioStreams.size > 4) {
                        Text(
                            text = "+${audioStreams.size - 4} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        // Subtitle info
        if (subtitleStreams.isNotEmpty()) {
            item("subtitles") {
                Column(modifier = Modifier.width(200.dp)) {
                    Text(
                        text = "Subtitles (${subtitleStreams.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    subtitleStreams.take(6).forEach { stream ->
                        Text(
                            text = stream.displayTitle ?: stream.language?.uppercase() ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (subtitleStreams.size > 6) {
                        Text(
                            text = "+${subtitleStreams.size - 6} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cast & Crew tab - shows main series cast (excludes guest stars).
 */
@Composable
private fun CastCrewTabContent(
    people: List<JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (String) -> Unit,
) {
    val firstCardFocusRequester = remember { FocusRequester() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer(firstCardFocusRequester),
    ) {
        itemsIndexed(people, key = { _, person -> person.id }) { index, person ->
            PersonCard(
                person = person,
                jellyfinClient = jellyfinClient,
                onClick = { onPersonClick(person.id) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(firstCardFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * Guest Stars tab - shows episode-specific guests.
 */
@Composable
private fun GuestStarsTabContent(
    guestStars: List<JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (String) -> Unit,
) {
    val firstCardFocusRequester = remember { FocusRequester() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer(firstCardFocusRequester),
    ) {
        itemsIndexed(guestStars, key = { _, person -> person.id }) { index, person ->
            PersonCard(
                person = person,
                jellyfinClient = jellyfinClient,
                onClick = { onPersonClick(person.id) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(firstCardFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * Seasons tab - shows season poster cards to switch seasons.
 */
@Composable
private fun SeasonsTabContent(
    seasons: List<JellyfinItem>,
    selectedSeasonIndex: Int,
    jellyfinClient: JellyfinClient,
    onSeasonSelected: (Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val firstCardFocusRequester = remember { FocusRequester() }

    // Scroll to selected season
    LaunchedEffect(selectedSeasonIndex) {
        if (selectedSeasonIndex > 0) {
            lazyListState.scrollToItem(selectedSeasonIndex)
        }
    }

    LazyRow(
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer(firstCardFocusRequester),
    ) {
        itemsIndexed(seasons, key = { _, season -> season.id }) { index, season ->
            val isSelected = index == selectedSeasonIndex

            SeasonPosterCard(
                season = season,
                isSelected = isSelected,
                jellyfinClient = jellyfinClient,
                onClick = { onSeasonSelected(index) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(firstCardFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun SeasonPosterCard(
    season: JellyfinItem,
    isSelected: Boolean,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = jellyfinClient.getPrimaryImageUrl(
        season.id,
        season.imageTags?.primary,
        maxWidth = 300,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        // Season poster card (2:3 aspect ratio, 110dp x 165dp)
        Card(
            onClick = onClick,
            modifier = Modifier.size(width = CardSizes.MediaCardWidth, height = 165.dp),
            scale = CardDefaults.scale(focusedScale = 1.05f),
            border = CardDefaults.border(
                border = if (isSelected) {
                    Border(
                        border = BorderStroke(2.dp, TvColors.BluePrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                    )
                } else {
                    Border.None
                },
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvColors.BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                ),
            ),
            shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = season.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Season label
        Text(
            text = season.name.ifEmpty { "Season ${season.indexNumber ?: 0}" },
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (isSelected) TvColors.TextPrimary else TvColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(CardSizes.MediaCardWidth),
        )
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
