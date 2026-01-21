@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogItemDivider
import dev.jausc.myflix.tv.ui.components.DialogItemEntry
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.ChaptersRow
import dev.jausc.myflix.tv.ui.components.detail.DotSeparatedRow
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButtons
import dev.jausc.myflix.tv.ui.components.detail.IconColors
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.tv.ui.components.detail.OverviewDialog
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Row indices for focus management
private const val HEADER_ROW = 0
private const val CHAPTERS_ROW = HEADER_ROW + 1
private const val CAST_ROW = CHAPTERS_ROW + 1
private const val GUEST_STARS_ROW = CAST_ROW + 1
private const val CREW_ROW = GUEST_STARS_ROW + 1
private const val SIMILAR_ROW = CREW_ROW + 1

/**
 * Episode detail screen showing full episode information.
 * Features episode thumbnail, metadata, overview, guest stars, and cast.
 */
@Composable
fun EpisodeDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: (Long?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onNavigate: (NavItem) -> Unit = {},
    showUniversesInNav: Boolean = false,
    showDiscoverInNav: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val episode = state.item ?: return
    val scope = rememberCoroutineScope()

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(SIMILAR_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val navBarFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }

    // Dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }
    var showOverview by remember { mutableStateOf(false) }

    // Focus play button on load
    LaunchedEffect(Unit) {
        delay(100)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus errors
        }
    }

    val resumePositionTicks = episode.userData?.playbackPositionTicks ?: 0L
    val watched = episode.userData?.played == true
    val favorite = episode.userData?.isFavorite == true

    // Guest stars and crew from episode
    val guestStars = remember(episode.people) {
        episode.people?.filter { it.type == "GuestStar" } ?: emptyList()
    }
    val episodeCrew = remember(episode.people) {
        episode.people?.filter { it.type in listOf("Director", "Writer", "Producer") } ?: emptyList()
    }

    // Series cast (fallback)
    val seriesCast = episode.actors

    // Use series backdrop for background
    val backdropId = episode.seriesId ?: episode.id
    val backdropUrl = remember(backdropId) {
        jellyfinClient.getBackdropUrl(backdropId, null)
    }
    val gradientColors = rememberGradientColors(backdropUrl)

    // Layered UI: DynamicBackground → NavigationRail + Content
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background (covers full screen including nav rail)
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left: Navigation Rail
            NavigationRail(
                selectedItem = NavItem.SHOWS,
                onItemSelected = onNavigate,
                showUniverses = showUniversesInNav,
                showDiscover = showDiscoverInNav,
                contentFocusRequester = playFocusRequester,
            )

            // Right: Content area
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // Layer 2: Content
            Column(modifier = Modifier.fillMaxSize()) {
            // Fixed hero section - doesn't scroll
            // Hero content - text on left with buttons at bottom, thumbnail on right (50% width)
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 16.dp, end = 48.dp)
                    .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                // Episode info column (left side) - fills height to align buttons with thumbnail bottom
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.weight(1f),
                ) {
                    // Top content: header info
                    EpisodeDetailsHeader(
                        episode = episode,
                        onOverviewClick = { showOverview = true },
                        descriptionFocusRequester = descriptionFocusRequester,
                        downFocusRequester = playFocusRequester,
                        upFocusRequester = navBarFocusRequester,
                    )

                    // Bottom content: action buttons aligned with thumbnail bottom
                    ExpandablePlayButtons(
                        resumePositionTicks = resumePositionTicks,
                        watched = watched,
                        favorite = favorite,
                        onPlayClick = { resumeTicks ->
                            onPlayClick(resumeTicks / 10_000)
                        },
                        onWatchedClick = onWatchedClick,
                        onFavoriteClick = onFavoriteClick,
                        onMoreClick = {
                            dialogParams = buildEpisodeMenu(
                                episode = episode,
                                onGoToSeason = {
                                    episode.parentId?.let { seasonId ->
                                        onNavigateToDetail(seasonId)
                                    }
                                },
                                onGoToShow = {
                                    episode.seriesId?.let { seriesId ->
                                        onNavigateToDetail(seriesId)
                                    }
                                },
                                onMediaInfo = { mediaInfoItem = episode },
                            )
                        },
                        buttonOnFocusChanged = {
                            if (it.isFocused) {
                                position = HEADER_ROW
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                        playButtonFocusRequester = playFocusRequester,
                        modifier = Modifier
                            .focusRequester(focusRequesters[HEADER_ROW])
                            .focusProperties {
                                down = focusRequesters[CHAPTERS_ROW]
                                up = descriptionFocusRequester
                            }
                            .focusRestorer(playFocusRequester)
                            .focusGroup(),
                    )
                }

                // Episode thumbnail (right side, 50% width)
                EpisodeHeroThumbnail(
                    episode = episode,
                    imageUrl = jellyfinClient.getPrimaryImageUrl(
                        episode.id,
                        episode.imageTags?.primary,
                        maxWidth = 960,
                    ),
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
            }

            // Scrollable content rows (below fixed hero)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Chapters (first item beneath hero section)
                if (!episode.chapters.isNullOrEmpty()) {
                    item(key = "chapters") {
                        ChaptersRow(
                            chapters = episode.chapters!!,
                            itemId = episode.id,
                            getChapterImageUrl = { index ->
                                jellyfinClient.getChapterImageUrl(episode.id, index)
                            },
                            onChapterClick = { positionMs ->
                                onPlayClick(positionMs)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[CHAPTERS_ROW]),
                        )
                    }
                }

                // Series Cast
                if (seriesCast.isNotEmpty()) {
                    item(key = "cast") {
                        CastCrewSection(
                            title = "Cast",
                            people = seriesCast,
                            jellyfinClient = jellyfinClient,
                            onPersonClick = { person ->
                                position = CAST_ROW
                                onNavigateToPerson(person.id)
                            },
                            onPersonLongClick = { _, _ ->
                                position = CAST_ROW
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[CAST_ROW]),
                        )
                    }
                }

                // Guest Stars
                if (guestStars.isNotEmpty()) {
                    item(key = "guest_stars") {
                        CastCrewSection(
                            title = "Guest Stars",
                            people = guestStars,
                            jellyfinClient = jellyfinClient,
                            onPersonClick = { person ->
                                position = GUEST_STARS_ROW
                                onNavigateToPerson(person.id)
                            },
                            onPersonLongClick = { _, _ ->
                                position = GUEST_STARS_ROW
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[GUEST_STARS_ROW]),
                        )
                    }
                }

                // Episode Crew (Directors, Writers)
                if (episodeCrew.isNotEmpty()) {
                    item(key = "crew") {
                        CastCrewSection(
                            title = "Crew",
                            people = episodeCrew,
                            jellyfinClient = jellyfinClient,
                            onPersonClick = { person ->
                                position = CREW_ROW
                                onNavigateToPerson(person.id)
                            },
                            onPersonLongClick = { _, _ ->
                                position = CREW_ROW
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[CREW_ROW]),
                        )
                    }
                }

                // Similar Items (More Like This)
                val similarWithEpisodes = state.similarItems.filter {
                    (it.recursiveItemCount ?: 0) > 0
                }
                if (similarWithEpisodes.isNotEmpty()) {
                    item(key = "similar") {
                        ItemRow(
                            title = "More Like This",
                            items = similarWithEpisodes,
                            onItemClick = { _, item ->
                                position = SIMILAR_ROW
                                onNavigateToDetail(item.id)
                            },
                            onItemLongClick = { _, _ ->
                                position = SIMILAR_ROW
                            },
                            cardContent = { _, item, cardModifier, onClick, onLongClick ->
                                if (item != null) {
                                    MediaCard(
                                        item = item,
                                        imageUrl = jellyfinClient.getPrimaryImageUrl(
                                            item.id,
                                            item.imageTags?.primary,
                                        ),
                                        onClick = onClick,
                                        onLongClick = onLongClick,
                                        modifier = cardModifier,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[SIMILAR_ROW]),
                        )
                    }
                }
            }
        }
            } // End content Box
        } // End Row
    } // End outer Box

    // Context menu dialog
    dialogParams?.let { params ->
        DialogPopup(
            params = params,
            onDismissRequest = { dialogParams = null },
        )
    }

    // Media info dialog
    mediaInfoItem?.let { item ->
        MediaInfoDialog(
            item = item,
            onDismiss = { mediaInfoItem = null },
        )
    }

    // Overview dialog
    if (showOverview) {
        val seasonLabel = buildSeasonEpisodeLabel(episode)
        val dialogTitle = listOfNotNull(
            episode.seriesName,
            seasonLabel,
            episode.name,
        ).joinToString(" - ")

        OverviewDialog(
            title = dialogTitle,
            overview = episode.overview.orEmpty(),
            genres = emptyList(),
            onDismiss = { showOverview = false },
        )
    }
}

/**
 * Episode thumbnail for hero section with progress bar.
 * Width is controlled by the modifier parameter.
 */
@Composable
private fun EpisodeHeroThumbnail(
    episode: JellyfinItem,
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = episode.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Progress bar at bottom
        if (episode.progressPercent > 0f && episode.progressPercent < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TvColors.Surface.copy(alpha = 0.7f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(episode.progressPercent)
                        .background(TvColors.BluePrimary),
                )
            }
        }
    }
}

/**
 * Episode details header with series name, episode title, metadata, and description.
 * Matches the home screen HeroTitleSection styling: series name is large, episode name is subtitle.
 */
@Composable
private fun EpisodeDetailsHeader(
    episode: JellyfinItem,
    onOverviewClick: () -> Unit,
    descriptionFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Series name - large title (matches home screen hero)
        episode.seriesName?.let { seriesName ->
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
        }

        // Episode name - smaller subtitle (matches home screen hero episode style)
        Text(
            text = episode.name,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata line: S1 E3 · 45m · TV-14 · ★8.5 · Jan 15, 2024
        val details = buildEpisodeDetailLine(episode)
        if (details.isNotEmpty() || episode.communityRating != null) {
            DotSeparatedRow(
                texts = details,
                communityRating = episode.communityRating,
                textStyle = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Media badges: resolution, codec, HDR/DV, audio
        MediaBadgesRow(item = episode)

        Spacer(modifier = Modifier.height(8.dp))

        // Description - 5 lines max, clickable to show full overview
        episode.overview?.let { overview ->
            OverviewText(
                overview = overview,
                maxLines = 5,
                onClick = onOverviewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester)
                    .focusProperties {
                        down = downFocusRequester
                        up = upFocusRequester
                    },
                paddingValues = PaddingValues(0.dp),
            )
        }
    }
}

/**
 * Build the metadata detail line for an episode: "S1 E3 · 45m · TV-14 · Jan 15, 2024"
 */
private fun buildEpisodeDetailLine(episode: JellyfinItem): List<String> = buildList {
    // Season and episode number
    buildSeasonEpisodeLabel(episode)?.let { add(it) }

    // Runtime
    episode.runTimeTicks?.let { ticks ->
        val minutes = (ticks / 600_000_000L).toInt()
        if (minutes > 0) {
            val hours = minutes / 60
            val mins = minutes % 60
            when {
                hours > 0 && mins > 0 -> add("${hours}h ${mins}m")
                hours > 0 -> add("${hours}h")
                else -> add("${mins}m")
            }
        }
    }

    // Official rating
    episode.officialRating?.let { add(it) }

    // Premiere date
    episode.premiereDate?.let { dateStr ->
        runCatching {
            val date = LocalDate.parse(dateStr.take(10))
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            add(date.format(formatter))
        }
    }
}

/**
 * Build "S1 E3" label from episode.
 */
private fun buildSeasonEpisodeLabel(episode: JellyfinItem): String? {
    val season = episode.parentIndexNumber
    val number = episode.indexNumber
    return if (season != null && number != null) {
        "S$season E$number"
    } else {
        null
    }
}

/**
 * Build the episode context menu with navigation and media info options.
 */
private fun buildEpisodeMenu(
    episode: JellyfinItem,
    onGoToSeason: () -> Unit,
    onGoToShow: () -> Unit,
    onMediaInfo: () -> Unit,
): DialogParams {
    val items = buildList<DialogItemEntry> {
        // Go to Season - only show if episode has a parent (season)
        if (episode.parentId != null) {
            add(
                DialogItem(
                    text = "Go to Season",
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    iconTint = IconColors.Navigation,
                    onClick = onGoToSeason,
                ),
            )
        }

        // Go to Show - only show if episode has a series
        if (episode.seriesId != null) {
            add(
                DialogItem(
                    text = "Go to Show",
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    iconTint = IconColors.Navigation,
                    onClick = onGoToShow,
                ),
            )
        }

        // Divider before Media Info
        if (isNotEmpty()) {
            add(DialogItemDivider)
        }

        add(
            DialogItem(
                text = "Media Info",
                icon = Icons.Outlined.Info,
                iconTint = IconColors.MediaInfo,
                onClick = onMediaInfo,
            ),
        )
    }

    return DialogParams(
        title = episode.name,
        items = items,
    )
}
