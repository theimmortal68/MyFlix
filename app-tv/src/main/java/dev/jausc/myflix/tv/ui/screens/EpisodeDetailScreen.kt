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
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
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
private const val GUEST_STARS_ROW = HEADER_ROW + 1
private const val CAST_ROW = GUEST_STARS_ROW + 1
private const val CREW_ROW = CAST_ROW + 1
private const val RECOMMENDED_ROW = CREW_ROW + 1
private const val SIMILAR_ROW = RECOMMENDED_ROW + 1

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

    // Layered UI: DynamicBackground → DetailBackdropLayer → Content
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Backdrop image (right side, behind content)
        // DetailBackdropLayer automatically uses series backdrop for episodes
        DetailBackdropLayer(
            item = episode,
            jellyfinClient = jellyfinClient,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .align(Alignment.TopEnd),
        )

        // Layer 3: Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed hero section - doesn't scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                // Hero content - thumbnail on left, info on right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(start = 48.dp, top = 36.dp),
                ) {
                    // Episode thumbnail (280dp × 158dp, 16:9)
                    EpisodeHeroThumbnail(
                        episode = episode,
                        imageUrl = jellyfinClient.getPrimaryImageUrl(
                            episode.id,
                            episode.imageTags?.primary,
                            maxWidth = 560,
                        ),
                    )

                    // Episode info column
                    Column(
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.weight(1f),
                    ) {
                        EpisodeDetailsHeader(
                            episode = episode,
                            onOverviewClick = { showOverview = true },
                            descriptionFocusRequester = descriptionFocusRequester,
                            downFocusRequester = playFocusRequester,
                            upFocusRequester = navBarFocusRequester,
                        )
                    }
                }

                // Action buttons fixed at bottom of hero section
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
                        dialogParams = DialogParams(
                            title = episode.name,
                            items = listOf(
                                DialogItem(
                                    text = "Media Info",
                                    icon = Icons.Outlined.Info,
                                    iconTint = IconColors.MediaInfo,
                                    onClick = { mediaInfoItem = episode },
                                ),
                            ),
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
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 48.dp, bottom = 8.dp)
                        .focusRequester(focusRequesters[HEADER_ROW])
                        .focusProperties {
                            down = focusRequesters[GUEST_STARS_ROW]
                            up = descriptionFocusRequester
                        }
                        .focusRestorer(playFocusRequester)
                        .focusGroup(),
                )
            }

            // Scrollable content rows (below fixed hero)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
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

                // Recommended Items
                if (state.recommendations.isNotEmpty()) {
                    item(key = "recommended") {
                        ItemRow(
                            title = "Recommended",
                            items = state.recommendations,
                            onItemClick = { _, item ->
                                position = RECOMMENDED_ROW
                                onNavigateToDetail(item.id)
                            },
                            onItemLongClick = { _, _ ->
                                position = RECOMMENDED_ROW
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
                                .focusRequester(focusRequesters[RECOMMENDED_ROW]),
                        )
                    }
                }

                // Similar Items
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

        // Top Navigation Bar (always visible)
        TopNavigationBarPopup(
            selectedItem = NavItem.SHOWS,
            onItemSelected = onNavigate,
            showUniverses = showUniversesInNav,
            contentFocusRequester = focusRequesters[HEADER_ROW],
            focusRequester = navBarFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

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
 */
@Composable
private fun EpisodeHeroThumbnail(
    episode: JellyfinItem,
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(280.dp)
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
        // Series name + season/episode
        val seasonLabel = buildSeasonEpisodeLabel(episode)
        val seriesLine = listOfNotNull(seasonLabel, episode.seriesName).joinToString(" · ")
        if (seriesLine.isNotEmpty()) {
            Text(
                text = seriesLine,
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.BlueAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Episode title
        Text(
            text = episode.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata line: 45m · TV-14 · ★8.5 · Jan 15, 2024
        val details = buildEpisodeDetailLine(episode)
        if (details.isNotEmpty() || episode.communityRating != null) {
            DotSeparatedRow(
                texts = details,
                communityRating = episode.communityRating,
                textStyle = MaterialTheme.typography.titleSmall,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Media badges: resolution, codec, HDR/DV, audio
        MediaBadgesRow(item = episode)

        Spacer(modifier = Modifier.height(6.dp))

        // Description - 4 lines max, clickable to show full overview
        episode.overview?.let { overview ->
            OverviewText(
                overview = overview,
                maxLines = 4,
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
 * Build the metadata detail line for an episode: "45m · TV-14 · Jan 15, 2024"
 */
private fun buildEpisodeDetailLine(episode: JellyfinItem): List<String> = buildList {
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
