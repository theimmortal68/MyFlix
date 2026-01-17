@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.tv.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.crew
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.theme.IconColors
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.tv.ui.components.detail.SeriesActionButtons
import dev.jausc.myflix.tv.ui.components.detail.SeriesQuickDetails
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.launch

import dev.jausc.myflix.tv.ui.components.detail.OverviewDialog
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import kotlinx.coroutines.delay

// Row indices for focus management
private const val HEADER_ROW = 0
private const val NEXT_UP_ROW = HEADER_ROW + 1
private const val SEASONS_ROW = NEXT_UP_ROW + 1
private const val CAST_ROW = SEASONS_ROW + 1
private const val CREW_ROW = CAST_ROW + 1
private const val EXTRAS_ROW = CREW_ROW + 1
private const val COLLECTIONS_ROW = EXTRAS_ROW + 1
private const val RECOMMENDED_ROW = COLLECTIONS_ROW + 1
private const val SIMILAR_ROW = RECOMMENDED_ROW + 1

/**
 * Plex-style series detail screen with backdrop hero and season tabs.
 * Features layered UI with dynamic background, backdrop image, and left-aligned metadata.
 * Episodes are displayed inline below season tabs.
 */
@Composable
fun SeriesDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onSeasonClick: (JellyfinItem) -> Unit,
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
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
    val navBarFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

    // Dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }
    var showOverview by remember { mutableStateOf(false) }
    var focusedSeason by remember { mutableStateOf<JellyfinItem?>(null) }

    // Focus play button on load
    LaunchedEffect(Unit) {
        delay(100)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus errors
        }
    }

    val watched = series.userData?.played == true
    val favorite = series.userData?.isFavorite == true

    // Cast & crew (using extension properties from JellyfinItem)
    val cast = series.actors
    val crew = series.crew

    val trailerItem = remember(state.specialFeatures) {
        findNewestTrailer(state.specialFeatures)
    }
    val trailerVideo = remember(series.remoteTrailers) {
        series.remoteTrailers
            ?.lastOrNull { !it.url.isNullOrBlank() && extractYouTubeVideoKey(it.url) != null }
    }
    val trailerAction: (() -> Unit)? = when {
        trailerItem != null -> {
            { onPlayItemClick(trailerItem.id, null) }
        }
        trailerVideo?.url != null -> {
            val key = extractYouTubeVideoKey(trailerVideo.url) ?: ""
            if (key.isBlank()) null else {
                { onTrailerClick(key, trailerVideo.name) }
            }
        }
        else -> null
    }

    val featureSections = remember(state.specialFeatures, trailerItem?.id) {
        buildFeatureSections(state.specialFeatures, trailerItem?.id?.let { setOf(it) } ?: emptySet())
    }

    // Backdrop URL and dynamic gradient colors
    val backdropUrl = remember(series.id) {
        jellyfinClient.getBackdropUrl(series.id, series.backdropImageTags?.firstOrNull())
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
            // Fixed hero section (48% height) - doesn't scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.48f)
                    .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                // Hero content (left 50%) - title, rating, description + action buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = 48.dp, top = 36.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    val displayDescription = focusedSeason?.overview?.takeIf { it.isNotBlank() } ?: series.overview
                    val displayTitle = if (focusedSeason != null && focusedSeason?.overview?.isNotBlank() == true) {
                        "${series.name} - ${focusedSeason?.name}"
                    } else {
                        series.name
                    }

                    SeriesDetailsHeader(
                        series = series,
                        title = displayTitle,
                        overview = displayDescription,
                        status = series.status,
                        studioNames = series.studios?.mapNotNull { it.name }.orEmpty(),
                        onOverviewClick = { showOverview = true },
                        focusRequester = descriptionFocusRequester,
                        downFocusRequester = playFocusRequester,
                        upFocusRequester = navBarFocusRequester,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons directly below description
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
                            dialogParams = DialogParams(
                                title = series.name,
                                items = listOf(
                                    DialogItem(
                                        text = "Media Info",
                                        icon = Icons.Outlined.Info,
                                        iconTint = IconColors.MediaInfo,
                                        onClick = { mediaInfoItem = series },
                                    ),
                                ),
                            )
                        },
                        onTrailerClick = trailerAction,
                        showMoreButton = true,
                        buttonOnFocusChanged = {
                            if (it.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                                focusedSeason = null // Reset to series info when focusing action buttons
                            }
                        },
                        playButtonFocusRequester = playFocusRequester,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .focusRequester(focusRequesters[HEADER_ROW])
                            .focusProperties {
                                down = focusRequesters[SEASONS_ROW]
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
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {

            // Next Up - only show if not S1E1 (i.e., watching is in progress)
            state.nextUpEpisode?.let { nextUp ->
                val isFirstEpisode = nextUp.parentIndexNumber == 1 && nextUp.indexNumber == 1
                if (!isFirstEpisode) {
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
                                    val thumbTag = item.imageTags?.thumb
                                    val imageUrl = if (thumbTag != null) {
                                        jellyfinClient.getThumbUrl(item.id, thumbTag)
                                    } else {
                                        jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary)
                                    }
                                    WideMediaCard(
                                        item = item,
                                        imageUrl = imageUrl,
                                        onClick = onClick,
                                        onLongClick = onLongClick,
                                        showBackground = false,
                                        modifier = cardModifier,
                                    )
                                }
                            },
                            cardOnFocus = { isFocused, _ ->
                                if (isFocused) focusedSeason = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[NEXT_UP_ROW]),
                        )
                    }
                }
            }

            // Seasons
            if (state.seasons.isNotEmpty()) {
                item(key = "seasons") {
                    ItemRow(
                        title = "Seasons (${state.seasons.size})",
                        items = state.seasons,
                        onItemClick = { _, season ->
                            position = SEASONS_ROW
                            onSeasonClick(season)
                        },
                        onItemLongClick = { _, _ ->
                            position = SEASONS_ROW
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
                                    onItemFocused = { focusedItem ->
                                        focusedSeason = focusedItem
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[SEASONS_ROW]),
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
                        cardOnFocus = { isFocused, _ ->
                            if (isFocused) focusedSeason = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[CAST_ROW]),
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
                        cardOnFocus = { isFocused, _ ->
                            if (isFocused) focusedSeason = null
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
                        cardOnFocus = { isFocused, _ ->
                            if (isFocused) focusedSeason = null
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
                                cardOnFocus = { isFocused, _ ->
                                    if (isFocused) focusedSeason = null
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
                        cardOnFocus = { isFocused, _ ->
                            if (isFocused) focusedSeason = null
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
                        onItemLongClick = { _, item ->
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
                        cardOnFocus = { isFocused, _ ->
                            if (isFocused) focusedSeason = null
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

    if (showOverview) {
        val dialogItem = focusedSeason?.takeIf { it.overview?.isNotBlank() == true } ?: series
        OverviewDialog(
            title = if (focusedSeason != null && focusedSeason?.overview?.isNotBlank() == true) {
                "${series.name} - ${focusedSeason?.name}"
            } else {
                series.name
            },
            overview = dialogItem.overview.orEmpty(),
            genres = series.genres.orEmpty(),
            onDismiss = { showOverview = false },
        )
    }
}

/**
 * Series details header matching home hero style.
 * Uses full width of parent column (which is already constrained to 50% of screen).
 */
@Composable
private fun SeriesDetailsHeader(
    series: JellyfinItem,
    title: String,
    overview: String?,
    status: String?,
    studioNames: List<String>,
    onOverviewClick: () -> Unit,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Title - matches home hero HeroTitleSection
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Rating row - matches home hero HeroRatingRow
        SeriesQuickDetails(
            item = series,
            status = status,
            studios = studioNames,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Media badges: resolution, codec, HDR/DV, audio
        MediaBadgesRow(item = series)

        // Description - allows 4 lines of text, uses full column width (50% of screen)
        overview?.let { text ->
            OverviewText(
                overview = text,
                maxLines = 4,
                onClick = onOverviewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusProperties {
                        down = downFocusRequester
                        up = upFocusRequester
                    },
                paddingValues = PaddingValues(0.dp)
            )
        }
    }
}
