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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.util.buildFeatureSections
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.tv.ui.components.AddToPlaylistDialog
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.DotSeparatedRow
import dev.jausc.myflix.tv.ui.components.detail.EpisodeListRow
import dev.jausc.myflix.tv.ui.theme.IconColors
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.OverviewDialog
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import dev.jausc.myflix.tv.ui.components.detail.SeasonActionButtons
import dev.jausc.myflix.tv.ui.components.detail.SeasonTabRow
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Row indices for focus management
private const val HEADER_ROW = 0
private const val NEXT_UP_ROW = HEADER_ROW + 1
private const val EPISODES_ROW = NEXT_UP_ROW + 1
private const val EXTRAS_ROW = EPISODES_ROW + 1
private const val COLLECTIONS_ROW = EXTRAS_ROW + 1

/**
 * Season detail screen with backdrop hero and season tabs.
 */
@Suppress("UnusedParameter")
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
    onRefreshEpisodes: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val series = state.item ?: return
    val scope = rememberCoroutineScope()

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(COLLECTIONS_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val seasonTabFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val navBarFocusRequester = remember { FocusRequester() }

    // Dialog state
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }
    var showOverview by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    val descriptionFocusRequester = remember { FocusRequester() }

    val watched = series.userData?.played == true
    val favorite = series.userData?.isFavorite == true

    val selectedSeasonIndex = remember(state.selectedSeason, state.seasons) {
        state.seasons.indexOfFirst { it.id == state.selectedSeason?.id }.coerceAtLeast(0)
    }
    val featureSections = remember(state.specialFeatures) {
        buildFeatureSections(state.specialFeatures, emptySet())
    }

    // Refresh episodes when screen resumes (e.g., returning from episode detail)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshEpisodes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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

    // Layered UI: DynamicBackground → NavigationRail + Content
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background (covers full screen including nav rail)
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

            // Content area
            Box(modifier = Modifier.fillMaxSize()) {
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
            // Fixed hero section - doesn't scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                // Season tabs at top - shifted down to avoid nav bar overlap
                SeasonTabRow(
                    seasons = state.seasons,
                    selectedSeasonIndex = selectedSeasonIndex,
                    onSeasonSelected = { _, season ->
                        position = HEADER_ROW
                        onSeasonSelected(season)
                    },
                    firstTabFocusRequester = seasonTabFocusRequester,
                    downFocusRequester = descriptionFocusRequester,
                    upFocusRequester = navBarFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 10.dp, end = 48.dp),
                )

                // Hero content (left 50%) - season info only (no episode info)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = 10.dp, top = 60.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    // Key on selected season ID to force recomposition when season changes
                    key(state.selectedSeason?.id) {
                        SeasonOnlyHeroContent(
                            series = series,
                            selectedSeason = state.selectedSeason,
                            episodeCount = state.episodes.size,
                            onOverviewClick = { showOverview = true },
                            descriptionFocusRequester = descriptionFocusRequester,
                            downFocusRequester = playFocusRequester,
                            upFocusRequester = seasonTabFocusRequester,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons directly below description
                    SeasonActionButtons(
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
                            val season = state.selectedSeason
                            dialogParams = DialogParams(
                                title = listOfNotNull(
                                    series.seriesName ?: series.name,
                                    season?.name,
                                ).joinToString(" - "),
                                items = listOf(
                                    DialogItem(
                                        text = "Go to Series",
                                        icon = Icons.Outlined.Tv,
                                        iconTint = IconColors.GoToSeries,
                                    ) {
                                        dialogParams = null
                                        onNavigateToDetail(series.seriesId ?: series.id)
                                    },
                                    DialogItem(
                                        text = "Add to Playlist",
                                        icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                                        iconTint = IconColors.Playlist,
                                    ) {
                                        dialogParams = null
                                        showPlaylistDialog = true
                                    },
                                ),
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
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .focusRequester(focusRequesters[HEADER_ROW])
                            .focusProperties {
                                down = focusRequesters[EPISODES_ROW]
                                up = descriptionFocusRequester
                            }
                            .focusRestorer(playFocusRequester)
                            .focusGroup(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Scrollable content rows (below fixed hero)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 10.dp, end = 48.dp, top = 0.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
            // Episodes for selected season - rendered directly in parent LazyColumn
            // (Not using EpisodeListSection which has its own LazyColumn - would cause crash)
            if (state.seasons.isNotEmpty()) {
                // Episodes section header
                item(key = "episodes_header") {
                    EpisodeSectionHeader(
                        episodeCount = state.episodes.size,
                        isLoading = state.isLoadingEpisodes,
                    )
                }

                // Episode rows
                if (state.isLoadingEpisodes) {
                    item(key = "episodes_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            dev.jausc.myflix.tv.ui.components.TvLoadingIndicator(
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                } else if (state.episodes.isEmpty()) {
                    item(key = "episodes_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No episodes available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TvColors.TextSecondary,
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = state.episodes,
                        key = { _, episode -> "episode_${episode.id}" },
                    ) { index, episode ->
                        EpisodeListRow(
                            episode = episode,
                            jellyfinClient = jellyfinClient,
                            onPlayClick = {
                                position = EPISODES_ROW
                                onPlayItemClick(episode.id, null)
                            },
                            onMoreInfoClick = {
                                position = EPISODES_ROW
                                onNavigateToDetail(episode.id)
                            },
                            onWatchedToggle = { newWatched ->
                                onEpisodeWatchedToggle(episode.id, newWatched)
                            },
                            onFavoriteToggle = { newFavorite ->
                                onEpisodeFavoriteToggle(episode.id, newFavorite)
                            },
                            upFocusRequester = if (index == 0) playFocusRequester else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(focusRequesters[EPISODES_ROW])
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
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
            }
            }
            } // End content Box
    } // End outer Box

    // Media info dialog
    mediaInfoItem?.let { item ->
        MediaInfoDialog(
            item = item,
            onDismiss = { mediaInfoItem = null },
        )
    }

    // Overview dialog - shows season overview
    if (showOverview) {
        val season = state.selectedSeason
        if (season != null) {
            val dialogTitle = listOfNotNull(series.seriesName ?: series.name, season.name)
                .joinToString(" - ")
            OverviewDialog(
                title = dialogTitle,
                overview = season.overview ?: series.overview.orEmpty(),
                genres = series.genres.orEmpty(),
                onDismiss = { showOverview = false },
            )
        }
    }

    // Playlist dialog
    if (showPlaylistDialog) {
        val season = state.selectedSeason
        AddToPlaylistDialog(
            itemId = season?.id ?: series.id,
            itemName = listOfNotNull(
                series.seriesName ?: series.name,
                season?.name,
            ).joinToString(" - "),
            jellyfinClient = jellyfinClient,
            onDismiss = { showPlaylistDialog = false },
            onSuccess = { showPlaylistDialog = false },
        )
    }

    // More popup dialog
    dialogParams?.let { params ->
        DialogPopup(
            params = params,
            onDismissRequest = { dialogParams = null },
        )
    }
}

/**
 * Season-only hero content - shows only season info (not episode info).
 * Displays series title, season name, rating row, and season overview.
 */
@Composable
private fun SeasonOnlyHeroContent(
    series: JellyfinItem,
    selectedSeason: JellyfinItem?,
    episodeCount: Int,
    onOverviewClick: () -> Unit,
    descriptionFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val showTitle = series.seriesName ?: series.name

    Column(modifier = modifier) {
        // Series title
        Text(
            text = showTitle,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
        )

        selectedSeason?.let { season ->
            Spacer(modifier = Modifier.height(2.dp))

            // Season name subtitle
            Text(
                text = season.name,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = TvColors.TextPrimary,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Season rating row: year, episode count, official rating
            val details = buildSeasonRatingLine(series, season, episodeCount.takeIf { it > 0 })
            if (details.isNotEmpty() || series.communityRating != null) {
                DotSeparatedRow(
                    texts = details,
                    communityRating = series.communityRating,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description - 4 lines max, clickable to show full overview
            // Use season overview if available, otherwise fall back to series overview
            val overview = season.overview?.takeIf { it.isNotBlank() } ?: series.overview
            overview?.let { text ->
                OverviewText(
                    overview = text,
                    maxLines = 4,
                    onClick = onOverviewClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
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
}

/**
 * Build the rating line for a season: "2024 · 10 episodes · TV-14"
 * @param episodeCount The actual loaded episode count (more accurate than season.childCount)
 */
private fun buildSeasonRatingLine(
    series: JellyfinItem,
    season: JellyfinItem,
    episodeCount: Int? = null,
): List<String> = buildList {
    // Premiere year
    season.premiereDate?.take(4)?.let { add(it) }
        ?: series.productionYear?.let { add(it.toString()) }

    // Episode count - prefer actual loaded count, fallback to season.childCount
    val count = episodeCount ?: season.childCount
    count?.let {
        add("$it episode${if (it != 1) "s" else ""}")
    }

    // Official rating
    series.officialRating?.let { add(it) }
}

/**
 * Section header for the episodes list showing title and count.
 */
@Composable
private fun EpisodeSectionHeader(episodeCount: Int, isLoading: Boolean, modifier: Modifier = Modifier,) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(TvColors.BluePrimary, shape = MaterialTheme.shapes.small),
        )
        Text(
            text = "Episodes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TvColors.TextPrimary,
        )
        if (!isLoading && episodeCount > 0) {
            Text(
                text = "($episodeCount)",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextSecondary,
            )
        }
    }
}
