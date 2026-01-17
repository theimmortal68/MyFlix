@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.crew
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.PlayQueueManager
import dev.jausc.myflix.core.player.QueueItem
import dev.jausc.myflix.core.player.QueueSource
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogItemDivider
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.DotSeparatedRow
import dev.jausc.myflix.tv.ui.components.detail.EpisodeGrid
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.SeasonTabRow
import dev.jausc.myflix.tv.ui.components.detail.SeriesActionButtons
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

// Row indices for focus management
private const val HEADER_ROW = 0
private const val NEXT_UP_ROW = HEADER_ROW + 1
private const val EPISODES_ROW = NEXT_UP_ROW + 1
private const val CAST_ROW = EPISODES_ROW + 1
private const val GUEST_STARS_ROW = CAST_ROW + 1
private const val CREW_ROW = GUEST_STARS_ROW + 1
private const val EXTRAS_ROW = CREW_ROW + 1
private const val COLLECTIONS_ROW = EXTRAS_ROW + 1
private const val RECOMMENDED_ROW = COLLECTIONS_ROW + 1
private const val SIMILAR_ROW = RECOMMENDED_ROW + 1

/**
 * Season detail screen with backdrop hero and season tabs.
 */
@Composable
fun SeasonDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onSeasonSelected: (JellyfinItem) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEpisodeWatchedToggle: (String, Boolean) -> Unit,
    onEpisodeFavoriteToggle: (String, Boolean) -> Unit,
    onNavigate: (NavItem) -> Unit = {},
    showUniversesInNav: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val series = state.item ?: return
    val scope = rememberCoroutineScope()

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(SIMILAR_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val seasonTabFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val navBarFocusRequester = remember { FocusRequester() }

    // Dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var subtitleDialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }
    var preferredSubtitleIndex by remember { mutableStateOf<Int?>(null) }

    val watched = series.userData?.played == true
    val favorite = series.userData?.isFavorite == true

    // Cast & crew (using extension properties from JellyfinItem)
    val cast = series.actors
    val crew = series.crew

    var focusedEpisodeId by remember { mutableStateOf<String?>(null) }
    val focusedEpisode = remember(state.episodes, focusedEpisodeId) {
        focusedEpisodeId?.let { id -> state.episodes.firstOrNull { it.id == id } }
    }
    val selectedEpisode = remember(state.episodes, state.nextUpEpisode, focusedEpisode) {
        focusedEpisode ?: run {
            val nextUp = state.nextUpEpisode?.let { next ->
                state.episodes.firstOrNull { it.id == next.id }
            }
            nextUp ?: state.episodes.firstOrNull()
        }
    }
    val selectedSeasonIndex = remember(state.selectedSeason, state.seasons) {
        state.seasons.indexOfFirst { it.id == state.selectedSeason?.id }.coerceAtLeast(0)
    }
    val guestStars = remember(selectedEpisode?.people) {
        selectedEpisode?.people?.filter { it.type == "GuestStar" } ?: emptyList()
    }
    val featureSections = remember(state.specialFeatures) {
        buildFeatureSections(state.specialFeatures, emptySet())
    }

    // Auto-select first season if none selected
    LaunchedEffect(state.seasons) {
        if (state.selectedSeason == null && state.seasons.isNotEmpty()) {
            onSeasonSelected(state.seasons.first())
        }
    }

    // Focus play button on load
    LaunchedEffect(Unit) {
        delay(100)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus errors
        }
    }

    // Backdrop URL and dynamic gradient colors
    val backdropId = series.seriesId ?: series.id
    val backdropUrl = remember(backdropId) {
        jellyfinClient.getBackdropUrl(backdropId, series.backdropImageTags?.firstOrNull())
    }
    val gradientColors = rememberGradientColors(backdropUrl)

    // Layered UI: DynamicBackground → DetailBackdropLayer → Content
    // Uses same structure as HomeScreen: fixed hero (37%) + scrollable content
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Backdrop image (right side, behind content) - matches home page positioning
        DetailBackdropLayer(
            item = series,
            jellyfinClient = jellyfinClient,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .align(Alignment.TopEnd),
        )

        // Layer 3: Content - Column with fixed hero + scrollable content (like HomeScreen)
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed hero section (50% height for 5 lines of description) - doesn't scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                // Season tabs at top - centered between screen top and title
                SeasonTabRow(
                    seasons = state.seasons,
                    selectedSeasonIndex = selectedSeasonIndex,
                    onSeasonSelected = { _, season ->
                        position = HEADER_ROW
                        onSeasonSelected(season)
                    },
                    firstTabFocusRequester = seasonTabFocusRequester,
                    downFocusRequester = focusRequesters[HEADER_ROW], // Go to action buttons, not episodes
                    upFocusRequester = navBarFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 48.dp, end = 48.dp),
                )

                // Hero content (left 50%) - title, subtitle, rating, description
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = 48.dp, top = 40.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    SeasonHeroContent(
                        series = series,
                        selectedEpisode = selectedEpisode,
                    )
                }

                // Action buttons fixed at bottom of hero section
                SeriesActionButtons(
                        watched = watched,
                        favorite = favorite,
                        onPlayClick = {
                            position = HEADER_ROW
                            onPlayClick()
                        },
                        onShuffleClick = {
                            position = HEADER_ROW
                            onShuffleClick()
                        },
                        onWatchedClick = onWatchedClick,
                        onFavoriteClick = onFavoriteClick,
                        onMoreClick = {
                            val episode = selectedEpisode ?: return@SeriesActionButtons
                            val seasonLabel = buildSeasonEpisodeLabel(episode)
                            dialogParams = buildEpisodeMenu(
                                title = listOfNotNull(series.seriesName ?: series.name, seasonLabel)
                                    .joinToString(" - "),
                                subtitle = episode.name,
                                episode = episode,
                                onPlay = {
                                    PlayQueueManager.setSingleItem(
                                        itemId = episode.id,
                                        title = episode.name,
                                        episodeInfo = seasonLabel,
                                        thumbnailItemId = episode.id,
                                        subtitleStreamIndex = preferredSubtitleIndex,
                                    )
                                    onPlayItemClick(episode.id, null)
                                },
                                onChooseSubtitles = {
                                    subtitleDialogParams = buildSubtitleMenu(
                                        episode = episode,
                                        selectedIndex = preferredSubtitleIndex,
                                        onSelect = { index -> preferredSubtitleIndex = index },
                                    )
                                },
                                onAddToPlaylist = {
                                    val items = state.episodes.map { item ->
                                        QueueItem(
                                            itemId = item.id,
                                            title = item.name,
                                            episodeInfo = buildSeasonEpisodeLabel(item),
                                            thumbnailItemId = item.id,
                                        )
                                    }
                                    PlayQueueManager.setQueue(items, QueueSource.SEASON_PLAY_ALL)
                                },
                                onToggleWatched = {
                                    val isPlayed = episode.userData?.played == true
                                    onEpisodeWatchedToggle(episode.id, !isPlayed)
                                },
                                onToggleFavorite = {
                                    val isFavorite = episode.userData?.isFavorite == true
                                    onEpisodeFavoriteToggle(episode.id, !isFavorite)
                                },
                                onGoToSeries = { onNavigateToDetail(series.seriesId ?: series.id) },
                                onMediaInfo = { mediaInfoItem = episode },
                                onPlayWithTranscoding = {
                                    PlayQueueManager.setSingleItem(
                                        itemId = episode.id,
                                        title = episode.name,
                                        episodeInfo = seasonLabel,
                                        thumbnailItemId = episode.id,
                                        subtitleStreamIndex = preferredSubtitleIndex,
                                    )
                                    onPlayItemClick(episode.id, null)
                                },
                            )
                        },
                        buttonOnFocusChanged = {
                            if (it.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                        playButtonFocusRequester = playFocusRequester,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 48.dp, bottom = 8.dp)
                            .focusRequester(focusRequesters[HEADER_ROW])
                            .focusProperties {
                                down = focusRequesters[EPISODES_ROW]
                                up = seasonTabFocusRequester
                            }
                            .focusRestorer(playFocusRequester)
                            .focusGroup(),
                    )
            }

            // Scrollable content rows (below fixed hero)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
            // Episodes for selected season
            if (state.seasons.isNotEmpty()) {
                item(key = "episodes") {
                    EpisodeGrid(
                        episodes = state.episodes,
                        jellyfinClient = jellyfinClient,
                        onEpisodeClick = { episode ->
                            position = EPISODES_ROW
                            onEpisodeClick(episode.id)
                        },
                        onEpisodeLongClick = { episode ->
                            position = EPISODES_ROW
                            // TODO: Show episode context menu
                        },
                        onEpisodeFocused = { episode ->
                            focusedEpisodeId = episode.id
                        },
                        isLoading = state.isLoadingEpisodes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[EPISODES_ROW]),
                        firstEpisodeFocusRequester = focusRequesters[EPISODES_ROW],
                        upFocusRequester = playFocusRequester,
                    )
                }
            }

            // Next Up
            state.nextUpEpisode?.let { nextUp ->
                item(key = "next_up") {
                    ItemRow(
                        title = "Next Up",
                        items = listOf(nextUp),
                        onItemClick = { _, item ->
                            position = NEXT_UP_ROW
                            onPlayItemClick(item.id, null)
                        },
                        onItemLongClick = { _, _ ->
                            position = NEXT_UP_ROW
                            // TODO: Show episode context menu
                        },
                        cardContent = { _, item, cardModifier, onClick, onLongClick ->
                            if (item != null) {
                                WideMediaCard(
                                    item = item,
                                    imageUrl = jellyfinClient.getThumbUrl(
                                        item.id,
                                        item.imageTags?.thumb ?: item.imageTags?.primary,
                                    ),
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                    modifier = cardModifier,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[NEXT_UP_ROW]),
                    )
                }
            }

            // Cast
            if (cast.isNotEmpty()) {
                item(key = "people") {
                    CastCrewSection(
                        title = "Cast",
                        people = cast,
                        jellyfinClient = jellyfinClient,
                        onPersonClick = { person ->
                            position = CAST_ROW
                            onNavigateToPerson(person.id)
                        },
                        onPersonLongClick = { _, _ ->
                            position = CAST_ROW
                            // TODO: Show person context menu
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
                            // TODO: Show person context menu
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[GUEST_STARS_ROW]),
                    )
                }
            }

            // Crew
            if (crew.isNotEmpty()) {
                item(key = "crew") {
                    CastCrewSection(
                        title = "Crew",
                        people = crew,
                        jellyfinClient = jellyfinClient,
                        onPersonClick = { person ->
                            position = CREW_ROW
                            onNavigateToPerson(person.id)
                        },
                        onPersonLongClick = { _, _ ->
                            position = CREW_ROW
                            // TODO: Show person context menu
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[CREW_ROW]),
                    )
                }
            }

            featureSections.forEach { section ->
                item(key = "feature_${section.title}") {
                    ItemRow(
                        title = section.title,
                        items = section.items,
                        onItemClick = { _, item ->
                            position = EXTRAS_ROW
                            onPlayItemClick(item.id, null)
                        },
                        onItemLongClick = { _, _ ->
                            position = EXTRAS_ROW
                            // TODO: Show item context menu
                        },
                        cardContent = { _, item, cardModifier, onClick, onLongClick ->
                            if (item != null) {
                                WideMediaCard(
                                    item = item,
                                    imageUrl = jellyfinClient.getThumbUrl(
                                        item.id,
                                        item.imageTags?.thumb ?: item.imageTags?.primary,
                                    ),
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                    modifier = cardModifier,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[EXTRAS_ROW]),
                    )
                }
            }

            // Collections
            if (state.collections.isNotEmpty()) {
                state.collections.forEach { collection ->
                    val collectionItems = state.collectionItems[collection.id].orEmpty()
                    if (collectionItems.isNotEmpty()) {
                        item(key = "collection_${collection.id}") {
                            ItemRow(
                                title = "More in ${collection.name}",
                                items = collectionItems,
                                onItemClick = { _, item ->
                                    position = COLLECTIONS_ROW
                                    onNavigateToDetail(item.id)
                                },
                                onItemLongClick = { _, _ ->
                                    position = COLLECTIONS_ROW
                                    // TODO: Show item context menu
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
                                    .focusRequester(focusRequesters[COLLECTIONS_ROW]),
                            )
                        }
                    }
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
                            // TODO: Show item context menu
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

            // Similar Items (More Like This) - filter out series with no episodes
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
                            // TODO: Show item context menu
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
            contentFocusRequester = seasonTabFocusRequester,
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

    subtitleDialogParams?.let { params ->
        DialogPopup(
            params = params,
            onDismissRequest = { subtitleDialogParams = null },
        )
    }

    // Media info dialog
    mediaInfoItem?.let { item ->
        MediaInfoDialog(
            item = item,
            onDismiss = { mediaInfoItem = null },
        )
    }
}

/**
 * Season hero content matching home hero style.
 * Shows series title, episode title/subtitle, rating row, badges, and description.
 */
@Composable
private fun SeasonHeroContent(
    series: JellyfinItem,
    selectedEpisode: JellyfinItem?,
    modifier: Modifier = Modifier,
) {
    val showTitle = series.seriesName ?: series.name
    val episodeBadges = buildEpisodeBadges(selectedEpisode)

    Column(modifier = modifier) {
        // Series title - matches home hero HeroTitleSection
        Text(
            text = showTitle,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        selectedEpisode?.let { episode ->
            Spacer(modifier = Modifier.height(2.dp))

            // Episode subtitle - matches home hero episode subtitle
            Text(
                text = episode.name,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Episode rating row
            val details = buildEpisodeRatingLine(episode)
            if (details.isNotEmpty() || episode.communityRating != null) {
                DotSeparatedRow(
                    texts = details,
                    communityRating = episode.communityRating,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (episodeBadges.isNotEmpty()) {
                FormatBadgeRow(badges = episodeBadges)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description - 5 lines max, uses full column width (50% of screen)
            episode.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FormatBadgeRow(badges: List<String>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        badges.forEach { badge ->
            Surface(
                shape = MaterialTheme.shapes.small,
                colors = SurfaceDefaults.colors(
                    containerColor = dev.jausc.myflix.tv.ui.theme.TvColors.SurfaceElevated.copy(alpha = 0.85f),
                ),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = dev.jausc.myflix.tv.ui.theme.TvColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun buildEpisodeRatingLine(episode: JellyfinItem): List<String> = buildList {
    episode.officialRating?.let { add(it) }
    episode.runTimeTicks?.let { ticks ->
        val minutes = (ticks / 600_000_000L).toInt()
        if (minutes > 0) add("${minutes}m")
    }
}

private fun buildEpisodeBadges(episode: JellyfinItem?): List<String> {
    if (episode == null) return emptyList()
    val badges = mutableListOf<String>()
    val mediaSource = episode.mediaSources?.firstOrNull()
    val video = mediaSource?.mediaStreams?.firstOrNull { it.type == "Video" }
    val audio = mediaSource?.mediaStreams?.firstOrNull { it.type == "Audio" && it.isDefault } ?:
        mediaSource?.mediaStreams?.firstOrNull { it.type == "Audio" }

    val resolution = video?.height?.let { height ->
        when {
            height >= 2160 -> "4K"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            else -> null
        }
    }
    resolution?.let { badges.add(it) }
    video?.codec?.uppercase()?.let { badges.add(it) }

    if (episode.videoQualityLabel.contains("Dolby Vision")) {
        badges.add("Dolby Vision")
    } else if (episode.videoQualityLabel.contains("HDR")) {
        badges.add("HDR")
    }

    formatAudioBadge(audio)?.let { badges.add(it) }
    return badges.distinct()
}

private fun formatAudioBadge(stream: dev.jausc.myflix.core.common.model.MediaStream?): String? {
    if (stream == null) return null
    val language = stream.language?.replaceFirstChar { it.titlecase(Locale.US) } ?: "Unknown"
    val codec = stream.codec?.uppercase()
    val channels = formatChannelLayout(stream.channels)
    return listOfNotNull(language, codec, channels).joinToString(" ")
}

private fun formatChannelLayout(channels: Int?): String? {
    return when (channels) {
        null -> null
        1 -> "1.0"
        2 -> "2.0"
        6 -> "5.1"
        8 -> "7.1"
        else -> "${channels}.0"
    }
}

private fun buildSeasonEpisodeLabel(episode: JellyfinItem): String? {
    val season = episode.parentIndexNumber
    val number = episode.indexNumber
    return if (season != null && number != null) {
        "S$season E$number"
    } else {
        null
    }
}

private fun buildSubtitleMenu(
    episode: JellyfinItem,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
): DialogParams {
    val subtitleStreams = episode.mediaSources
        ?.firstOrNull()
        ?.mediaStreams
        ?.filter { it.type == "Subtitle" }
        .orEmpty()

    val items = buildList<dev.jausc.myflix.tv.ui.components.DialogItemEntry> {
        add(
            DialogItem(
                text = "Off",
                icon = Icons.Outlined.Subtitles,
                onClick = { onSelect(null) },
            ),
        )
        subtitleStreams.forEach { stream ->
            val label = stream.displayTitle ?: stream.language ?: "Subtitle ${stream.index}"
            add(
                DialogItem(
                    text = label,
                    icon = Icons.Outlined.ClosedCaption,
                    onClick = { onSelect(stream.index) },
                ),
            )
        }
    }

    return DialogParams(
        title = "Choose Subtitles",
        items = items,
    )
}

private fun buildEpisodeMenu(
    title: String,
    subtitle: String?,
    episode: JellyfinItem,
    onPlay: () -> Unit,
    onChooseSubtitles: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGoToSeries: () -> Unit,
    onMediaInfo: () -> Unit,
    onPlayWithTranscoding: () -> Unit,
): DialogParams {
    val watched = episode.userData?.played == true
    val favorite = episode.userData?.isFavorite == true

    val items = buildList<dev.jausc.myflix.tv.ui.components.DialogItemEntry> {
        add(
            DialogItem(
                text = "Play",
                icon = Icons.Outlined.PlayArrow,
                onClick = onPlay,
            ),
        )
        add(
            DialogItem(
                text = "Choose Subtitles",
                icon = Icons.Outlined.Subtitles,
                onClick = onChooseSubtitles,
            ),
        )
        add(
            DialogItem(
                text = "Add to playlist",
                icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                onClick = onAddToPlaylist,
            ),
        )
        add(
            DialogItem(
                text = if (watched) "Mark as unwatched" else "Mark as watched",
                icon = Icons.Outlined.Visibility,
                onClick = onToggleWatched,
            ),
        )
        add(
            DialogItem(
                text = if (favorite) "Remove favorite" else "Favorite",
                icon = Icons.Outlined.Favorite,
                onClick = onToggleFavorite,
            ),
        )
        add(
            DialogItem(
                text = "Go to series",
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                onClick = onGoToSeries,
            ),
        )
        add(DialogItemDivider)
        add(
            DialogItem(
                text = "Media Information",
                icon = Icons.Outlined.Info,
                onClick = onMediaInfo,
            ),
        )
        add(
            DialogItem(
                text = "Play with transcoding",
                icon = Icons.Outlined.PlayCircle,
                onClick = onPlayWithTranscoding,
            ),
        )
    }

    return DialogParams(
        title = title,
        items = items,
    )
}
