@file:Suppress("MagicNumber")
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
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
import dev.jausc.myflix.core.common.model.JellyfinChapter
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.formattedFullPremiereDate
import dev.jausc.myflix.core.common.model.imdbId
import dev.jausc.myflix.core.common.model.tmdbId
import dev.jausc.myflix.core.common.ui.IconColors
import dev.jausc.myflix.core.common.util.TimeFormatUtil
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.CardSizes
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButton
import dev.jausc.myflix.tv.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.tv.ui.components.detail.PersonCard
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Tab options for the episode detail screen.
 * Order matters - Seasons first for quick season switching.
 */
private enum class EpisodeTab {
    Seasons,
    Chapters,
    Details,
    MediaInfo,
    CastCrew,
    GuestStars,
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
    leftEdgeFocusRequester: FocusRequester? = null,
) {
    // Currently focused episode drives hero content
    var focusedEpisode by remember { mutableStateOf<JellyfinItem?>(null) }

    // Sync focusedEpisode with updated episodes list (for optimistic updates)
    // When episodes list changes, find the same episode by ID and update reference
    LaunchedEffect(episodes) {
        val currentId = focusedEpisode?.id
        if (currentId != null) {
            val updatedEpisode = episodes.find { it.id == currentId }
            if (updatedEpisode != null && updatedEpisode != focusedEpisode) {
                focusedEpisode = updatedEpisode
            }
        }
    }

    // Track focus setup - reset when season changes
    var focusSetForSeason by remember { mutableStateOf(-1) }
    // Focus requesters for NavRail restoration
    val episodeRowFocusRequester = remember { FocusRequester() }
    val firstTabFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }

    // Item-level focus tracking for NavRail exit restoration
    val updateExitFocus = rememberExitFocusRegistry(episodeRowFocusRequester)

    // Stable map of episode FocusRequesters - lazily create and reuse
    val episodeFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Find target episode for focus
    // When switching seasons, selectedEpisodeId won't be in new episodes list,
    // so it falls through to find in-progress > first unwatched > first episode
    val targetEpisode = remember(episodes, selectedEpisodeId, selectedSeasonIndex) {
        findTargetEpisode(episodes, selectedEpisodeId)
    }

    // Track the target episode index for coordinated scroll and focus
    val targetEpisodeIndex = remember(episodes, targetEpisode) {
        if (targetEpisode != null) {
            episodes.indexOfFirst { it.id == targetEpisode.id }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }
    }

    // Track which season and episode set we've focused (using first episode ID as stable identifier)
    var lastFocusedKey by remember { mutableStateOf("") }

    // Set focused episode when episodes change (season switch or initial load)
    // Focus request is now handled by EpisodeCardRow's focusRestorer
    LaunchedEffect(episodes, selectedSeasonIndex) {
        // Create a unique key for this season's episode set
        val currentKey = "${selectedSeasonIndex}_${episodes.firstOrNull()?.id ?: ""}"

        if (targetEpisode != null && episodes.isNotEmpty() && currentKey != lastFocusedKey) {
            focusedEpisode = targetEpisode
            lastFocusedKey = currentKey

            // Small delay to ensure composition is complete
            delay(50L)

            // Focus the row container - this captures focus during transition
            // and prevents it from escaping to NavRail
            // The row's focusRestorer will then delegate to the target episode
            try {
                episodeRowFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // Row not ready yet
            }
        }
    }

    // Build thumbnail URL from focused episode (primary image, not backdrop)
    val thumbnailUrl = remember(focusedEpisode?.id) {
        focusedEpisode?.let { episode ->
            // Use episode thumbnail (primary image) for the backdrop display
            jellyfinClient.getPrimaryImageUrl(episode.id, episode.imageTags?.primary, maxWidth = 800)
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
                EpisodeTab.Chapters -> focusedEpisode?.chapters?.isNotEmpty() == true
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
        // Episode thumbnail backdrop (top-right) - 40% of screen
        KenBurnsBackdrop(
            imageUrl = thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .fillMaxHeight(0.4f)
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
                    playFocusRequester = playFocusRequester,
                    episodeRowFocusRequester = episodeRowFocusRequester,
                    modifier = Modifier.fillMaxWidth(0.58f),
                    leftEdgeFocusRequester = leftEdgeFocusRequester,
                    updateExitFocus = updateExitFocus,
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
                targetEpisodeIndex = targetEpisodeIndex,
                episodeFocusRequesters = episodeFocusRequesters,
                onEpisodeClick = onEpisodeClick,
                onEpisodeFocused = { episode -> focusedEpisode = episode },
                focusRequester = episodeRowFocusRequester,
                upFocusRequester = playFocusRequester,
                downFocusRequester = firstTabFocusRequester,
                modifier = Modifier.fillMaxWidth(),
                leftEdgeFocusRequester = leftEdgeFocusRequester,
                updateExitFocus = updateExitFocus,
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
                            val isFirstTab = index == 0

                            val tabFocusRequester = remember { FocusRequester() }
                            Column(
                                modifier = Modifier
                                    .focusRequester(
                                        if (isFirstTab) firstTabFocusRequester else tabFocusRequester,
                                    )
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            updateExitFocus(
                                                if (isFirstTab) firstTabFocusRequester else tabFocusRequester,
                                            )
                                            tabChangeJob?.cancel()
                                            tabChangeJob = coroutineScope.launch {
                                                delay(150)
                                                selectedTab = tab
                                            }
                                        }
                                    }
                                    .focusProperties {
                                        up = episodeRowFocusRequester
                                        left = when {
                                            isFirstTab && leftEdgeFocusRequester != null -> leftEdgeFocusRequester
                                            isFirstTab -> FocusRequester.Cancel
                                            else -> FocusRequester.Default
                                        }
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
                                        EpisodeTab.Seasons -> "Seasons"
                                        EpisodeTab.Chapters -> "Chapters"
                                        EpisodeTab.Details -> "Details"
                                        EpisodeTab.MediaInfo -> "Media Info"
                                        EpisodeTab.CastCrew -> "Cast & Crew"
                                        EpisodeTab.GuestStars -> "Guest Stars"
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
                    // focusGroup + focusRestorer allows the container to receive focus
                    // from tab headers and delegate to first focusable child
                    // Tab content area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(start = 2.dp)
                            .focusProperties {
                                up = firstTabFocusRequester
                            },
                    ) {
                        when (selectedTab) {
                            EpisodeTab.Chapters -> {
                                focusedEpisode?.let { episode ->
                                    ChaptersTabContent(
                                        chapters = episode.chapters ?: emptyList(),
                                        itemId = episode.id,
                                        jellyfinClient = jellyfinClient,
                                        onChapterClick = { positionMs ->
                                            onEpisodeClick(episode)
                                        },
                                        tabFocusRequester = firstTabFocusRequester,
                                    )
                                }
                            }
                            EpisodeTab.Details -> {
                                focusedEpisode?.let { episode ->
                                    DetailsTabContent(
                                        episode = episode,
                                        tabFocusRequester = firstTabFocusRequester,
                                    )
                                }
                            }
                            EpisodeTab.MediaInfo -> {
                                focusedEpisode?.let { episode ->
                                    MediaInfoTabContent(
                                        mediaStreams = episode.mediaSources?.firstOrNull()?.mediaStreams ?: emptyList(),
                                        tabFocusRequester = firstTabFocusRequester,
                                    )
                                }
                            }
                            EpisodeTab.CastCrew -> {
                                focusedEpisode?.let { episode ->
                                    CastCrewTabContent(
                                        people = episode.people?.filter { it.type != "GuestStar" } ?: emptyList(),
                                        jellyfinClient = jellyfinClient,
                                        onPersonClick = onPersonClick,
                                        tabFocusRequester = firstTabFocusRequester,
                                    )
                                }
                            }
                            EpisodeTab.GuestStars -> {
                                GuestStarsTabContent(
                                    guestStars = guestStars,
                                    jellyfinClient = jellyfinClient,
                                    onPersonClick = onPersonClick,
                                    tabFocusRequester = firstTabFocusRequester,
                                )
                            }
                            EpisodeTab.Seasons -> {
                                SeasonsTabContent(
                                    seasons = seasons,
                                    selectedSeasonIndex = selectedSeasonIndex,
                                    jellyfinClient = jellyfinClient,
                                    onSeasonSelected = onSeasonSelected,
                                    tabFocusRequester = firstTabFocusRequester,
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
    playFocusRequester: FocusRequester,
    episodeRowFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    leftEdgeFocusRequester: FocusRequester? = null,
    updateExitFocus: (FocusRequester) -> Unit = {},
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

        // Media badges: resolution, codec, HDR/DV, audio
        MediaBadgesRow(item = episode)

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
            focusRequester = playFocusRequester,
            downFocusRequester = episodeRowFocusRequester,
            leftEdgeFocusRequester = leftEdgeFocusRequester,
            updateExitFocus = updateExitFocus,
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

        // IMDB badge
        episode.imdbId?.let {
            MetadataDot()
            ProviderBadge(text = "IMDb", backgroundColor = Color(0xFFF5C518))
        }

        // TMDB badge
        episode.tmdbId?.let {
            MetadataDot()
            ProviderBadge(text = "TMDB", backgroundColor = Color(0xFF01D277))
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

/**
 * Provider badge (IMDB, TMDB) showing that external metadata is available.
 */
@Composable
private fun ProviderBadge(text: String, backgroundColor: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
            ),
            color = Color.Black,
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
    leftEdgeFocusRequester: FocusRequester? = null,
    updateExitFocus: (FocusRequester) -> Unit = {},
) {
    val playButtonText = when {
        hasProgress && remainingMinutes > 0 -> "Resume · ${remainingMinutes}m left"
        hasProgress -> "Resume"
        else -> "Play"
    }

    // FocusRequesters for each action button
    val playFocusRequester = remember { FocusRequester() }
    val watchedFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember { FocusRequester() }

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
                    .focusProperties {
                        down = downFocusRequester
                        if (leftEdgeFocusRequester != null) {
                            left = leftEdgeFocusRequester
                        }
                    }
                    .onFocusChanged { if (it.hasFocus) updateExitFocus(playFocusRequester) },
            )
        }

        item("watched") {
            ExpandablePlayButton(
                title = if (isWatched) "Mark Unwatched" else "Mark Watched",
                icon = if (isWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = Modifier
                    .focusRequester(watchedFocusRequester)
                    .focusProperties { down = downFocusRequester }
                    .onFocusChanged { if (it.hasFocus) updateExitFocus(watchedFocusRequester) },
            )
        }

        item("favorite") {
            ExpandablePlayButton(
                title = if (isFavorite) "Remove Favorite" else "Add to Favorites",
                icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (isFavorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = Modifier
                    .focusRequester(favoriteFocusRequester)
                    .focusProperties { down = downFocusRequester }
                    .onFocusChanged { if (it.hasFocus) updateExitFocus(favoriteFocusRequester) },
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
    targetEpisodeIndex: Int,
    episodeFocusRequesters: MutableMap<String, FocusRequester>,
    onEpisodeClick: (JellyfinItem) -> Unit,
    onEpisodeFocused: (JellyfinItem) -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    leftEdgeFocusRequester: FocusRequester? = null,
    updateExitFocus: (FocusRequester) -> Unit = {},
) {
    val lazyListState = rememberLazyListState()

    // Scroll to target episode when it changes
    LaunchedEffect(targetEpisodeIndex, episodes) {
        if (episodes.isNotEmpty()) {
            lazyListState.scrollToItem(targetEpisodeIndex.coerceIn(0, episodes.lastIndex))
        }
    }

    // Get FocusRequester for target episode (for focusRestorer)
    val targetEpisodeId = episodes.getOrNull(targetEpisodeIndex)?.id
    val targetCardFocusRequester = remember(targetEpisodeId) {
        if (targetEpisodeId != null) {
            episodeFocusRequesters.getOrPut(targetEpisodeId) { FocusRequester() }
        } else {
            FocusRequester()
        }
    }

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
                    .focusRestorer(targetCardFocusRequester),
            ) {
                itemsIndexed(episodes, key = { _, episode -> episode.id }) { index, episode ->
                    // Get or create FocusRequester for this episode
                    val cardFocusRequester = episodeFocusRequesters.getOrPut(episode.id) {
                        FocusRequester()
                    }

                    EpisodeCard(
                        episode = episode,
                        jellyfinClient = jellyfinClient,
                        onClick = { onEpisodeClick(episode) },
                        onFocused = {
                            onEpisodeFocused(episode)
                            updateExitFocus(cardFocusRequester)
                        },
                        modifier = Modifier
                            .focusRequester(cardFocusRequester)
                            .focusProperties {
                                up = upFocusRequester
                                down = downFocusRequester
                                if (index == 0 && leftEdgeFocusRequester != null) {
                                    left = leftEdgeFocusRequester
                                } else if (index == 0) {
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
        modifier = modifier.onFocusChanged { if (it.hasFocus) onFocused() },
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

                // Favorite indicator (top-left corner)
                val isFavorite = episode.userData?.isFavorite == true
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }

                // Watched indicator (top-right corner)
                val isWatched = episode.userData?.played == true
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Watched",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(12.dp),
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
 * Made focusable for D-pad navigation from tab headers.
 */
@Composable
private fun DetailsTabContent(
    episode: JellyfinItem,
    tabFocusRequester: FocusRequester? = null,
) {
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
                Box(
                    modifier = Modifier
                        .focusProperties {
                            if (tabFocusRequester != null) {
                                up = tabFocusRequester
                            }
                        }
                        .focusable(),
                ) {
                    OverviewSection(overview = overview)
                }
            }
        }

        // Details section
        item("details") {
            Box(
                modifier = Modifier
                    .focusProperties {
                        if (tabFocusRequester != null) {
                            up = tabFocusRequester
                        }
                    }
                    .focusable(),
            ) {
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
        }

        // Directors & Writers combined into one column
        if (directors.isNotEmpty() || writers.isNotEmpty()) {
            item("crew") {
                Box(
                    modifier = Modifier
                        .focusProperties {
                            if (tabFocusRequester != null) {
                                up = tabFocusRequester
                            }
                        }
                        .focusable(),
                ) {
                    Column(
                        modifier = Modifier.width(200.dp),
                    ) {
                        if (directors.isNotEmpty()) {
                            Text(
                                text = "Director${if (directors.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TvColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            directors.forEach { director ->
                                Text(
                                    text = director.name ?: "Unknown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TvColors.TextSecondary,
                                )
                            }
                        }

                        if (directors.isNotEmpty() && writers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (writers.isNotEmpty()) {
                            Text(
                                text = "Writer${if (writers.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TvColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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

        // Tags section
        if (!episode.tags.isNullOrEmpty()) {
            item("tags") {
                Box(
                    modifier = Modifier
                        .focusProperties {
                            if (tabFocusRequester != null) {
                                up = tabFocusRequester
                            }
                        }
                        .focusable(),
                ) {
                    Column(
                        modifier = Modifier.width(250.dp),
                    ) {
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.titleSmall,
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = episode.tags!!.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
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
 * Overview section with auto-scroll for long text.
 * Scrolls automatically when text overflows the visible area.
 */
@Composable
private fun OverviewSection(overview: String) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll animation for long content
    LaunchedEffect(overview) {
        // Wait for layout to complete
        delay(500)
        val maxScroll = scrollState.maxValue
        if (maxScroll > 0) {
            // Scroll down slowly
            while (true) {
                delay(3000) // Pause at top
                coroutineScope.launch {
                    // Scroll to bottom over 8 seconds
                    val steps = 100
                    val stepDelay = 8000L / steps
                    for (i in 1..steps) {
                        scrollState.scrollTo((maxScroll * i) / steps)
                        delay(stepDelay)
                    }
                }
                delay(8000 + 2000) // Wait for scroll + pause at bottom
                coroutineScope.launch {
                    // Scroll back to top
                    scrollState.scrollTo(0)
                }
                delay(500)
            }
        }
    }

    Column(
        modifier = Modifier
            .width(500.dp)
            .fillMaxHeight(),
    ) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleSmall,
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
        ) {
            Text(
                text = overview,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
                lineHeight = 20.sp,
                // Bottom padding ensures last line is fully visible when scrolled
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

/**
 * Media Info tab - shows video/audio/subtitle streams.
 * Made focusable for D-pad navigation from tab headers.
 */
@Composable
private fun MediaInfoTabContent(
    mediaStreams: List<MediaStream>,
    tabFocusRequester: FocusRequester? = null,
) {
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
                Box(
                    modifier = Modifier
                        .focusProperties {
                            if (tabFocusRequester != null) {
                                up = tabFocusRequester
                            }
                        }
                        .focusable(),
                ) {
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
        }

        // Audio info
        if (audioStreams.isNotEmpty()) {
            item("audio") {
                Box(
                    modifier = Modifier
                        .focusProperties {
                            if (tabFocusRequester != null) {
                                up = tabFocusRequester
                            }
                        }
                        .focusable(),
                ) {
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
        }

        // Subtitle info
        if (subtitleStreams.isNotEmpty()) {
            item("subtitles") {
                Box(
                    modifier = Modifier
                        .focusProperties {
                            if (tabFocusRequester != null) {
                                up = tabFocusRequester
                            }
                        }
                        .focusable(),
                ) {
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
}

/**
 * Cast & Crew tab - shows main series cast (excludes guest stars).
 */
@Composable
private fun CastCrewTabContent(
    people: List<JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (String) -> Unit,
    tabFocusRequester: FocusRequester? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(people, key = { _, person -> person.id }) { index, person ->
            PersonCard(
                person = person,
                jellyfinClient = jellyfinClient,
                onClick = { onPersonClick(person.id) },
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) {
                        up = tabFocusRequester
                    }
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
    tabFocusRequester: FocusRequester? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(guestStars, key = { _, person -> person.id }) { index, person ->
            PersonCard(
                person = person,
                jellyfinClient = jellyfinClient,
                onClick = { onPersonClick(person.id) },
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) {
                        up = tabFocusRequester
                    }
                },
            )
        }
    }
}

/**
 * Chapters tab - shows chapter thumbnails with timestamps.
 * Clicking a chapter starts playback at that position.
 */
@Composable
private fun ChaptersTabContent(
    chapters: List<JellyfinChapter>,
    itemId: String,
    jellyfinClient: JellyfinClient,
    onChapterClick: (Long) -> Unit,
    tabFocusRequester: FocusRequester? = null,
) {
    if (chapters.isEmpty()) return

    val firstFocus = remember { FocusRequester() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer(firstFocus),
    ) {
        itemsIndexed(chapters, key = { index, _ -> "chapter_$index" }) { index, chapter ->
            val imageUrl = jellyfinClient.getChapterImageUrl(itemId, index)

            ChapterCard(
                chapter = chapter,
                imageUrl = imageUrl,
                onClick = {
                    chapter.startPositionTicks?.let { ticks ->
                        onChapterClick(ticks / 10_000)
                    }
                },
                modifier = Modifier
                    .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                    .focusProperties {
                        if (tabFocusRequester != null) {
                            up = tabFocusRequester
                        }
                    },
            )
        }
    }
}

/**
 * Individual chapter card with thumbnail and timestamp.
 */
@Composable
private fun ChapterCard(
    chapter: JellyfinChapter,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(width = 200.dp, height = 140.dp),
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
            // Chapter thumbnail
            AsyncImage(
                model = imageUrl,
                contentDescription = chapter.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Gradient for text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
                            ),
                        ),
                    ),
            )

            // Timestamp badge (top-right)
            chapter.startPositionTicks?.let { ticks ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = TimeFormatUtil.formatTicksToTime(ticks),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            // Chapter name (bottom)
            chapter.name?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                )
            }
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
    tabFocusRequester: FocusRequester? = null,
) {
    val lazyListState = rememberLazyListState()

    // FocusRequester for the selected season (for focusRestorer)
    val selectedSeasonFocusRequester = remember { FocusRequester() }

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
            .focusRestorer(selectedSeasonFocusRequester),
    ) {
        itemsIndexed(seasons, key = { _, season -> season.id }) { index, season ->
            val isSelected = index == selectedSeasonIndex

            SeasonPosterCard(
                season = season,
                isSelected = isSelected,
                jellyfinClient = jellyfinClient,
                onClick = { onSeasonSelected(index) },
                modifier = Modifier
                    .then(
                        if (isSelected) {
                            Modifier.focusRequester(selectedSeasonFocusRequester)
                        } else {
                            Modifier
                        }
                    )
                    .focusProperties {
                        if (tabFocusRequester != null) {
                            up = tabFocusRequester
                        }
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

    // Only show indicator when season is completely watched
    val isFullyWatched = season.userData?.played == true

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Season poster card (80x112dp to match Series screen)
        Card(
            onClick = onClick,
            modifier = modifier.size(width = CardSizes.SeasonPosterWidth, height = CardSizes.SeasonPosterHeight),
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
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = season.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Watched indicator (checkmark) - only shown when fully watched
                if (isFullyWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Fully watched",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
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
            modifier = Modifier.width(CardSizes.SeasonPosterWidth),
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
                    // Soft edge fade - 90% fully visible, gentle fade at left edge only
                    val leftFade = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.05f to Color.Black.copy(alpha = 0.5f),
                            0.10f to Color.Black,
                            1.0f to Color.Black,
                        ),
                    )
                    // Bottom fade for smooth transition
                    val bottomFade = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            0.7f to Color.Black,
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

        // Minimal gradient overlay at the very edge for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to TvColors.Background.copy(alpha = 0.15f),
                            0.05f to Color.Transparent,
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

// endregion
