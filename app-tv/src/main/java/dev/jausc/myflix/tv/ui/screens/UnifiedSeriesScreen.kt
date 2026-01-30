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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.crew
import dev.jausc.myflix.core.common.ui.IconColors
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.tv.ui.components.CardSizes
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButton
import dev.jausc.myflix.tv.ui.components.detail.PersonCard
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.util.Locale

/**
 * Tab options for the series detail screen.
 */
private enum class UnifiedSeriesTab {
    Seasons,
    Details,
    CastCrew,
    Trailers,
    Extras,
    Related,
}

/**
 * Unified series detail screen - Phase 1: Hero with action buttons.
 *
 * Displays series info with Ken Burns animated backdrop and action buttons.
 */
@Composable
fun UnifiedSeriesScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSeasonClick: (JellyfinItem) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    modifier: Modifier = Modifier,
    leftEdgeFocusRequester: FocusRequester? = null,
) {
    val series = state.item ?: return
    val isWatched = series.userData?.played == true
    val isFavorite = series.userData?.isFavorite == true

    // Focus requesters for NavRail restoration
    val playButtonFocusRequester = remember { FocusRequester() }
    val nextUpFocusRequester = remember { FocusRequester() }
    val firstTabFocusRequester = remember { FocusRequester() }

    // Track last focused section for NavRail exit restoration: "action", "nextup", "tabs"
    var lastFocusedSection by remember { mutableStateOf("action") }

    // Compute focus restoration target based on last focused section
    val focusRestorerTarget = when (lastFocusedSection) {
        "nextup" -> nextUpFocusRequester
        "tabs" -> firstTabFocusRequester
        else -> playButtonFocusRequester
    }

    // Tab state
    var selectedTab by rememberSaveable { mutableStateOf(UnifiedSeriesTab.Seasons) }

    // Split special features into trailers and other extras
    val trailers = remember(state.specialFeatures) {
        state.specialFeatures.filter {
            it.type.contains("Trailer", ignoreCase = true) ||
                it.name.contains("trailer", ignoreCase = true) ||
                it.name.contains("teaser", ignoreCase = true)
        }
    }
    val extras = remember(state.specialFeatures) {
        state.specialFeatures.filter {
            !it.type.contains("Trailer", ignoreCase = true) &&
                !it.name.contains("trailer", ignoreCase = true) &&
                !it.name.contains("teaser", ignoreCase = true)
        }
    }

    // Filter tabs based on available data
    val availableTabs = remember(trailers, extras) {
        UnifiedSeriesTab.entries.filter { tab ->
            when (tab) {
                UnifiedSeriesTab.Trailers -> trailers.isNotEmpty()
                UnifiedSeriesTab.Extras -> extras.isNotEmpty()
                else -> true
            }
        }
    }

    // Handle tab selection when tab becomes unavailable
    LaunchedEffect(availableTabs) {
        if (selectedTab !in availableTabs) {
            selectedTab = availableTabs.firstOrNull() ?: UnifiedSeriesTab.Seasons
        }
    }

    // Cast & crew - combine into single list
    val people = (series.actors + series.crew)

    // Build dynamic play button text based on next up episode
    val nextUp = state.nextUpEpisode
    val hasProgress = (nextUp?.userData?.playbackPositionTicks ?: 0L) > 0L
    val playButtonText = buildPlayButtonText(hasProgress)
    val isResume = hasProgress

    // Request initial focus on play button
    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    // Build backdrop URL
    val backdropUrl = remember(series.id) {
        val backdropTag = series.backdropImageTags?.firstOrNull()
        if (backdropTag != null) {
            jellyfinClient.getBackdropUrl(series.id, backdropTag, maxWidth = 1920)
        } else {
            jellyfinClient.getBackdropUrl(series.id, null, maxWidth = 1920)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .focusGroup()
            .focusRestorer(focusRestorerTarget),
    ) {
        // Layer 1: Ken Burns animated backdrop (top-right)
        KenBurnsBackdrop(
            imageUrl = backdropUrl,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .align(Alignment.TopEnd),
        )

        // Layer 2: Content column with hero at top, tabs at bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            // Hero content (left side) - matches home screen positioning
            SeriesHeroContent(
                series = series,
                isWatched = isWatched,
                isFavorite = isFavorite,
                playButtonText = playButtonText,
                isResume = isResume,
                nextUpEpisode = state.nextUpEpisode,
                firstEpisode = state.episodes.firstOrNull(),
                jellyfinClient = jellyfinClient,
                onPlayClick = onPlayClick,
                onShuffleClick = onShuffleClick,
                onWatchedClick = onWatchedClick,
                onFavoriteClick = onFavoriteClick,
                onEpisodeClick = { episode ->
                    val startPosition = episode.userData?.playbackPositionTicks?.let { it / 10_000 } ?: 0L
                    onPlayClick()
                },
                playButtonFocusRequester = playButtonFocusRequester,
                firstTabFocusRequester = firstTabFocusRequester,
                modifier = Modifier.fillMaxWidth(0.55f),
                leftEdgeFocusRequester = leftEdgeFocusRequester,
                nextUpFocusRequester = nextUpFocusRequester,
                onActionFocused = { lastFocusedSection = "action" },
                onNextUpFocused = { lastFocusedSection = "nextup" },
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
                    // Tab row with debounced focus change - only show available tabs
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

                            Column(
                                modifier = Modifier
                                    .then(
                                        if (isFirstTab) {
                                            Modifier.focusRequester(firstTabFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            lastFocusedSection = "tabs"
                                            // Cancel any pending tab change
                                            tabChangeJob?.cancel()
                                            // Start new debounced change
                                            tabChangeJob = coroutineScope.launch {
                                                delay(150) // 150ms debounce
                                                selectedTab = tab
                                            }
                                        }
                                    }
                                    .focusProperties {
                                        up = nextUpFocusRequester
                                        // First tab: left goes to NavRail sentinel
                                        if (isFirstTab && leftEdgeFocusRequester != null) {
                                            left = leftEdgeFocusRequester
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
                                        UnifiedSeriesTab.Seasons -> "Seasons"
                                        UnifiedSeriesTab.Details -> "Details"
                                        UnifiedSeriesTab.CastCrew -> "Cast & Crew"
                                        UnifiedSeriesTab.Trailers -> "Trailers"
                                        UnifiedSeriesTab.Extras -> "Extras"
                                        UnifiedSeriesTab.Related -> "Related"
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

                    // Tab content - single row needs less height
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
                            UnifiedSeriesTab.Seasons -> {
                                SeasonsTabContent(
                                    seasons = state.seasons,
                                    jellyfinClient = jellyfinClient,
                                    onSeasonClick = onSeasonClick,
                                    tabFocusRequester = firstTabFocusRequester,
                                )
                            }
                            UnifiedSeriesTab.Details -> {
                                DetailsTabContent(
                                    series = series,
                                    tabFocusRequester = firstTabFocusRequester,
                                )
                            }
                            UnifiedSeriesTab.CastCrew -> {
                                CastCrewTabContent(
                                    people = people,
                                    jellyfinClient = jellyfinClient,
                                    onPersonClick = onNavigateToPerson,
                                    tabFocusRequester = firstTabFocusRequester,
                                )
                            }
                            UnifiedSeriesTab.Trailers -> {
                                TrailersTabContent(
                                    trailers = trailers,
                                    jellyfinClient = jellyfinClient,
                                    onTrailerClick = { /* TODO: Handle trailer click */ },
                                    tabFocusRequester = firstTabFocusRequester,
                                )
                            }
                            UnifiedSeriesTab.Extras -> {
                                ExtrasTabContent(
                                    extras = extras,
                                    jellyfinClient = jellyfinClient,
                                    onExtraClick = { /* TODO: Handle extra click */ },
                                    tabFocusRequester = firstTabFocusRequester,
                                )
                            }
                            UnifiedSeriesTab.Related -> {
                                val similarWithEpisodes = state.similarItems.filter {
                                    (it.recursiveItemCount ?: 0) > 0
                                }
                                RelatedTabContent(
                                    similarItems = similarWithEpisodes,
                                    jellyfinClient = jellyfinClient,
                                    onItemClick = onNavigateToDetail,
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
 * Ken Burns effect backdrop - slow zoom and pan animation.
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

/**
 * Hero content showing series title, metadata, overview, and action buttons.
 * Matches home screen hero styling exactly.
 */
@Composable
private fun SeriesHeroContent(
    series: JellyfinItem,
    isWatched: Boolean,
    isFavorite: Boolean,
    playButtonText: String,
    isResume: Boolean,
    nextUpEpisode: JellyfinItem?,
    firstEpisode: JellyfinItem?,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEpisodeClick: (JellyfinItem) -> Unit,
    playButtonFocusRequester: FocusRequester,
    firstTabFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    leftEdgeFocusRequester: FocusRequester? = null,
    nextUpFocusRequester: FocusRequester? = null,
    onActionFocused: () -> Unit = {},
    onNextUpFocused: () -> Unit = {},
) {

    Column(modifier = modifier) {
        // Series title - matches home screen HeroTitleSection
        Text(
            text = series.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata row - matches home screen HeroRatingRow
        SeriesMetadataRow(series = series)

        Spacer(modifier = Modifier.height(6.dp))

        // Overview - matches home screen HeroDescription
        series.overview?.let { overview ->
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
        SeriesActionButtonsRow(
            isWatched = isWatched,
            isFavorite = isFavorite,
            playButtonText = playButtonText,
            isResume = isResume,
            onPlayClick = onPlayClick,
            onShuffleClick = onShuffleClick,
            onWatchedClick = onWatchedClick,
            onFavoriteClick = onFavoriteClick,
            playButtonFocusRequester = playButtonFocusRequester,
            downFocusRequester = nextUpFocusRequester,
            leftEdgeFocusRequester = leftEdgeFocusRequester,
            onButtonFocused = onActionFocused,
        )

        // Next Up card (show if has progress, otherwise show S1 E1)
        val displayEpisode = nextUpEpisode ?: firstEpisode

        displayEpisode?.let { episode ->
            val hasProgress = (episode.userData?.playbackPositionTicks ?: 0L) > 0L

            Column(
                modifier = Modifier.padding(top = 14.dp),
            ) {
                // "Continue Watching" or "Next Up" label
                Text(
                    text = if (hasProgress) "Continue Watching" else "Next Up",
                    style = MaterialTheme.typography.labelMedium,
                    color = TvColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // Card with thumbnail - explicit 16:9 dimensions
                Card(
                    onClick = { onEpisodeClick(episode) },
                    modifier = Modifier
                        .size(width = 200.dp, height = 112.dp)
                        .then(
                            if (nextUpFocusRequester != null) {
                                Modifier.focusRequester(nextUpFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                        .focusProperties {
                            up = playButtonFocusRequester
                            down = firstTabFocusRequester
                            if (leftEdgeFocusRequester != null) {
                                left = leftEdgeFocusRequester
                            }
                        }
                        .onFocusChanged { if (it.hasFocus) onNextUpFocused() },
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
                        // Thumbnail
                        AsyncImage(
                            model = jellyfinClient.getPrimaryImageUrl(episode.id, episode.imageTags?.primary, 400),
                            contentDescription = episode.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Progress bar (if has progress)
                        if (hasProgress) {
                            val positionTicks = episode.userData?.playbackPositionTicks ?: 0L
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

                // Episode title below thumbnail
                Text(
                    text = "S${episode.parentIndexNumber ?: 1} E${episode.indexNumber ?: 1} · ${episode.name ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(200.dp)
                        .padding(top = 8.dp),
                )

                // Time remaining or runtime
                val timeText = if (hasProgress) {
                    val remainingTicks = (episode.runTimeTicks ?: 0L) - (episode.userData?.playbackPositionTicks ?: 0L)
                    val remainingMinutes = remainingTicks / 600_000_000
                    "${remainingMinutes}m remaining"
                } else {
                    val runtimeMinutes = (episode.runTimeTicks ?: 0L) / 600_000_000
                    "${runtimeMinutes}m"
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TvColors.TextSecondary,
                )
            }
        }
    }
}

/**
 * Action buttons row for series: Play, Shuffle, Mark as Watched, Add to Favorites.
 */
@Composable
private fun SeriesActionButtonsRow(
    isWatched: Boolean,
    isFavorite: Boolean,
    playButtonText: String,
    isResume: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester? = null,
    leftEdgeFocusRequester: FocusRequester? = null,
    onButtonFocused: () -> Unit = {},
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier
            .focusGroup()
            .focusRestorer(playButtonFocusRequester),
    ) {
        // Play button - dynamic text based on next up episode
        // Left navigation goes to NavRail sentinel, down goes to Next Up card
        item("play") {
            ExpandablePlayButton(
                title = playButtonText,
                icon = Icons.Outlined.PlayArrow,
                iconColor = if (isResume) IconColors.Resume else IconColors.Play,
                onClick = onPlayClick,
                modifier = Modifier
                    .focusRequester(playButtonFocusRequester)
                    .focusProperties {
                        if (leftEdgeFocusRequester != null) {
                            left = leftEdgeFocusRequester
                        }
                        if (downFocusRequester != null) {
                            down = downFocusRequester
                        }
                    }
                    .onFocusChanged { if (it.hasFocus) onButtonFocused() },
            )
        }

        // Shuffle button
        item("shuffle") {
            ExpandablePlayButton(
                title = "Shuffle",
                icon = Icons.Outlined.Shuffle,
                iconColor = IconColors.Shuffle,
                onClick = onShuffleClick,
                modifier = if (downFocusRequester != null) {
                    Modifier.focusProperties { down = downFocusRequester }
                } else {
                    Modifier
                },
            )
        }

        // Watched button
        item("watched") {
            ExpandablePlayButton(
                title = if (isWatched) "Mark Unwatched" else "Mark Watched",
                icon = if (isWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = if (downFocusRequester != null) {
                    Modifier.focusProperties { down = downFocusRequester }
                } else {
                    Modifier
                },
            )
        }

        // Favorite button
        item("favorite") {
            ExpandablePlayButton(
                title = if (isFavorite) "Remove Favorite" else "Add to Favorites",
                icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (isFavorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = if (downFocusRequester != null) {
                    Modifier.focusProperties { down = downFocusRequester }
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * Metadata row: Year · Rating · Stars
 * Matches home screen HeroRatingRow for series.
 */
@Composable
private fun SeriesMetadataRow(series: JellyfinItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var needsDot = false

        // Year
        series.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            needsDot = true
        }

        // Official rating badge
        series.officialRating?.let { rating ->
            if (needsDot) DotSeparator()
            RatingBadge(text = rating)
            needsDot = true
        }

        // Community rating
        series.communityRating?.let { rating ->
            if (needsDot) DotSeparator()
            StarRating(rating = rating)
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
 * Badge for official ratings (PG-13, TV-MA, etc.) with color-coded backgrounds.
 * Matches home screen RatingBadge exactly.
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
 * Matches home screen StarRating exactly.
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
 * Builds the play button text based on progress state.
 * Shows "Resume" if has progress, "Play" otherwise.
 */
private fun buildPlayButtonText(hasProgress: Boolean): String {
    return if (hasProgress) "Resume" else "Play"
}

// =============================================================================
// Tab Content Composables
// =============================================================================

/**
 * Seasons tab content - shows season posters in a horizontal row.
 * Uses smaller poster size (80×112dp) for navigational use (10 across).
 */
@Composable
private fun SeasonsTabContent(
    seasons: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onSeasonClick: (JellyfinItem) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    if (seasons.isEmpty()) {
        Text(
            text = "No seasons available",
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextSecondary,
        )
        return
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(seasons, key = { _, season -> season.id }) { index, season ->
            SeasonCard(
                season = season,
                jellyfinClient = jellyfinClient,
                onClick = { onSeasonClick(season) },
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
 * Compact season card for tab content (80×112dp).
 */
@Composable
private fun SeasonCard(
    season: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.width(80.dp)) {
        Card(
            onClick = onClick,
            modifier = modifier
                .size(width = 80.dp, height = 112.dp)
                .onFocusChanged { isFocused = it.isFocused },
            scale = CardDefaults.scale(focusedScale = 1.05f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, Color.White),
                    shape = MaterialTheme.shapes.small,
                ),
            ),
        ) {
            AsyncImage(
                model = jellyfinClient.getPrimaryImageUrl(season.id, season.imageTags?.primary),
                contentDescription = season.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = season.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) Color.White else TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Cast & Crew tab content - single row combining cast first, then crew.
 */
@Composable
private fun CastCrewTabContent(
    people: List<JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (String) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    if (people.isEmpty()) {
        Text(
            text = "No cast or crew information available",
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextSecondary,
        )
        return
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(people, key = { _, person -> person.id ?: "${person.name}_${person.role}".hashCode() }) { index, person ->
            PersonCard(
                person = person,
                jellyfinClient = jellyfinClient,
                onClick = { person.id?.let { onPersonClick(it) } },
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
 * Person card for tab content - larger circular image with name below.
 */
@Composable
private fun PersonCard(
    person: JellyfinPerson,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            onClick = onClick,
            modifier = modifier
                .size(72.dp) // Larger photo
                .onFocusChanged { isFocused = it.isFocused },
            shape = CardDefaults.shape(CircleShape),
            scale = CardDefaults.scale(focusedScale = 1.08f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, Color.White),
                    shape = CircleShape,
                ),
            ),
        ) {
            AsyncImage(
                model = person.primaryImageTag?.let {
                    jellyfinClient.getPersonImageUrl(person.id ?: "", it)
                },
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter, // Prioritize faces
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Zoom in slightly to get better face framing
                        scaleX = 1.15f
                        scaleY = 1.15f
                        translationY = 8f // Shift down slightly to capture more face
                    },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = person.name?.split(" ")?.firstOrNull() ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = if (isFocused) Color.White else TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = person.role ?: person.type ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = TvColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Details tab content - shows metadata in a 3-column grid layout.
 * Made focusable for D-pad navigation from tab headers.
 */
@Composable
private fun DetailsTabContent(
    series: JellyfinItem,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        // Column 1
        item("column1") {
            Box(
                modifier = Modifier
                    .focusProperties {
                        if (tabFocusRequester != null) {
                            up = tabFocusRequester
                        }
                    }
                    .focusable(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailItem("Genres", series.genres?.joinToString(", ") ?: "—")
                    DetailItem("First Aired", series.premiereDate?.let { formatDate(it) } ?: "—")
                    DetailItem("Runtime", series.runTimeTicks?.let { "${it / 600_000_000} min" } ?: "—")
                }
            }
        }
        // Column 2
        item("column2") {
            Box(
                modifier = Modifier
                    .focusProperties {
                        if (tabFocusRequester != null) {
                            up = tabFocusRequester
                        }
                    }
                    .focusable(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailItem("Network", series.studios?.firstOrNull()?.name ?: "—")
                    DetailItem("Status", series.status?.replaceFirstChar { it.uppercase() } ?: "—")
                    DetailItem("Seasons", "${series.childCount ?: 0}")
                }
            }
        }
        // Column 3
        item("column3") {
            Box(
                modifier = Modifier
                    .focusProperties {
                        if (tabFocusRequester != null) {
                            up = tabFocusRequester
                        }
                    }
                    .focusable(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailItem("Rating", series.officialRating ?: "—")
                    DetailItem(
                        "Score",
                        series.communityRating?.let { "★ ${"%.1f".format(Locale.US, it)}" } ?: "—",
                    )
                    series.taglines?.firstOrNull()?.let { tagline ->
                        DetailItem("Tagline", "\"$tagline\"")
                    }
                }
            }
        }
    }
}

/**
 * Single detail item with label and value.
 */
@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TvColors.TextSecondary,
            fontSize = 10.sp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Format a date string for display.
 */
private fun formatDate(dateString: String): String {
    return try {
        val instant = java.time.Instant.parse(dateString)
        val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
            .format(localDate)
    } catch (_: Exception) {
        dateString.take(10) // Fallback to YYYY-MM-DD portion
    }
}

/**
 * Trailers tab content - single row of trailers.
 */
@Composable
private fun TrailersTabContent(
    trailers: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onTrailerClick: (JellyfinItem) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(trailers, key = { _, trailer -> trailer.id }) { index, trailer ->
            ExtraVideoCard(
                item = trailer,
                jellyfinClient = jellyfinClient,
                onClick = { onTrailerClick(trailer) },
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
 * Extras tab content - single row of featurettes and behind-the-scenes.
 */
@Composable
private fun ExtrasTabContent(
    extras: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onExtraClick: (JellyfinItem) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(extras, key = { _, extra -> extra.id }) { index, extra ->
            ExtraVideoCard(
                item = extra,
                jellyfinClient = jellyfinClient,
                onClick = { onExtraClick(extra) },
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
 * Video card for trailers and extras - 160dp wide with 16:9 thumbnail.
 */
@Composable
private fun ExtraVideoCard(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.width(160.dp)) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .onFocusChanged { isFocused = it.isFocused },
            scale = CardDefaults.scale(focusedScale = 1.05f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, Color.White),
                    shape = MaterialTheme.shapes.small,
                ),
            ),
        ) {
            Box {
                AsyncImage(
                    model = jellyfinClient.getBackdropUrl(item.id, null, maxWidth = 320),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                )

                // Play icon
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(4.dp),
                    )
                }

                // Duration badge
                item.runTimeTicks?.let { ticks ->
                    val totalSeconds = ticks / 10_000_000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    Text(
                        text = "$minutes:${seconds.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                // Type badge
                item.type.takeIf { it != "Video" }?.let { type ->
                    Text(
                        text = type.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(TvColors.BluePrimary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isFocused) Color.White else TvColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Related tab content - shows similar series with compact cards (80×112dp).
 */
@Composable
private fun RelatedTabContent(
    similarItems: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    if (similarItems.isEmpty()) {
        Text(
            text = "No related shows found",
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextSecondary,
        )
        return
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(similarItems, key = { _, item -> item.id }) { index, item ->
            RelatedItemCard(
                item = item,
                jellyfinClient = jellyfinClient,
                onClick = { onItemClick(item.id) },
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
 * Compact related item card (80×112dp poster size).
 */
@Composable
private fun RelatedItemCard(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.width(80.dp)) {
        Card(
            onClick = onClick,
            modifier = modifier
                .size(width = 80.dp, height = 112.dp)
                .onFocusChanged { isFocused = it.isFocused },
            scale = CardDefaults.scale(focusedScale = 1.05f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, Color.White),
                    shape = MaterialTheme.shapes.small,
                ),
            ),
        ) {
            AsyncImage(
                model = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) Color.White else TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )

        item.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = TvColors.TextSecondary,
                fontSize = 10.sp,
            )
        }
    }
}
