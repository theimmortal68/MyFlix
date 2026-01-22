@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.crew
import dev.jausc.myflix.core.common.util.buildFeatureSections
import dev.jausc.myflix.core.common.util.extractYouTubeVideoKey
import dev.jausc.myflix.core.common.util.findNewestTrailer
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.theme.IconColors
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
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
private const val SIMILAR_ROW = COLLECTIONS_ROW + 1

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
    showDiscoverInNav: Boolean = false,
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

    // Layered UI: DynamicBackground → NavigationRail + Content (DetailBackdropLayer → Content)
    // Uses same structure as HomeScreen: fixed hero (37%) + scrollable content
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
                // Hero content (left 50%) - title, rating, description + action buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = 10.dp, top = 16.dp, bottom = 4.dp),
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
                        jellyfinClient = jellyfinClient,
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
                contentPadding = PaddingValues(start = 0.dp, end = 48.dp, top = 0.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
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
                                )
                            }
                        },
                        cardOnFocus = { isFocused, index ->
                            if (isFocused) {
                                focusedSeason = state.seasons.getOrNull(index)
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
            } // End Content Box
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
    jellyfinClient: JellyfinClient,
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

        // Rating row - matches home hero HeroRatingRow style with dot separators
        SeriesHeroRatingRow(
            series = series,
            status = status,
            networkId = series.studios?.firstOrNull()?.id,
            networkName = studioNames.firstOrNull(),
            jellyfinClient = jellyfinClient,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Media badges: resolution, codec, HDR/DV, audio
        MediaBadgesRow(item = series)

        // Description - allows 4 lines of text, uses 80% of column width (40% of screen)
        overview?.let { text ->
            OverviewText(
                overview = text,
                maxLines = 4,
                onClick = onOverviewClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
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

/**
 * Hero-style rating row for series matching home page style.
 * Shows: Year · Rating Badge · Star Rating · Status Badge
 */
@Composable
private fun SeriesHeroRatingRow(
    series: JellyfinItem,
    status: String?,
    networkId: String?,
    networkName: String?,
    jellyfinClient: JellyfinClient,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var needsDot = false

        // Production year
        series.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            needsDot = true
        }

        // Official rating badge (PG-13, TV-MA, etc.) with colored background
        series.officialRating?.let { rating ->
            if (needsDot) DotSeparator()
            RatingBadge(rating)
            needsDot = true
        }

        // Community rating (star rating)
        series.communityRating?.let { rating ->
            if (needsDot) DotSeparator()
            StarRating(rating)
            needsDot = true
        }

        // Network logo (between rating and status)
        if (networkId != null) {
            if (needsDot) DotSeparator()
            NetworkLogo(networkName = networkName, jellyfinClient = jellyfinClient)
            needsDot = true
        }

        // Status badge with colored background
        status?.let { statusText ->
            if (needsDot) DotSeparator()
            StatusBadge(statusText)
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
 * Rating badge with colored background based on content rating.
 * Height matches bodySmall text for consistent row alignment.
 */
@Composable
private fun RatingBadge(text: String) {
    val backgroundColor = getRatingColor(text)

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
 * Get the background color for a content rating.
 */
private fun getRatingColor(rating: String): Color {
    val normalizedRating = rating.uppercase().trim()
    return when {
        // Green - Family friendly
        normalizedRating in listOf("G", "TV-G", "TV-Y", "TV-Y7", "TV-Y7-FV") ->
            Color(0xFF2E7D32) // Green 800

        // Blue - General/Parental guidance
        normalizedRating in listOf("PG", "TV-PG") ->
            Color(0xFF1565C0) // Blue 800

        // Orange - Teen/Caution
        normalizedRating in listOf("PG-13", "TV-14", "16") ->
            Color(0xFFF57C00) // Orange 700

        // Red - Restricted/Mature
        normalizedRating in listOf("R", "TV-MA", "NC-17", "NR", "UNRATED") ->
            Color(0xFFC62828) // Red 800

        // Gray - Default/Unknown
        else -> Color(0xFF616161) // Gray 700
    }
}

/**
 * Star rating display with gold star icon.
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
            tint = Color(0xFFFFD700), // Gold color
        )
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Series status badge with colored background.
 * Height matches bodySmall text for consistent row alignment.
 */
@Composable
private fun StatusBadge(status: String) {
    val (displayText, backgroundColor) = getStatusInfo(status)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = Color.White,
        )
    }
}

/**
 * Get display text and color for series status.
 * Handles various Jellyfin/TVDB status values.
 */
private fun getStatusInfo(status: String): Pair<String, Color> {
    val normalized = status.trim().lowercase()
    return when {
        // Green - Currently airing (handles "Continuing", "Returning Series")
        normalized.contains("continuing") ||
            normalized == "returning series" ||
            normalized.contains("airing") -> "Continuing" to Color(0xFF2E7D32)

        // Blue - Returning for new season or upcoming
        normalized.contains("returning") && !normalized.contains("series") ->
            "Returning" to Color(0xFF1565C0)

        // Blue - In production or planned
        normalized.contains("production") || normalized.contains("pilot") ||
            normalized.contains("planned") -> "Upcoming" to Color(0xFF1565C0)

        // Red - Ended
        normalized.contains("ended") -> "Ended" to Color(0xFFC62828)

        // Red - Canceled
        normalized.contains("canceled") || normalized.contains("cancelled") ->
            "Canceled" to Color(0xFFC62828)

        // Default - show as-is with gray
        else -> status.trim() to Color(0xFF616161)
    }
}

/**
 * Map of Jellyfin network names to embedded drawable resources.
 * Logos sourced from tv-logos collection (prioritized) with TMDB fallback.
 */
private val networkLogoResources = mapOf(
    // US Broadcast Networks
    "ABC" to R.drawable.network_abc,
    "ABC (US)" to R.drawable.network_abc,
    "NBC" to R.drawable.network_nbc,
    "CBS" to R.drawable.network_cbs,
    "Fox" to R.drawable.network_fox,
    "FOX" to R.drawable.network_fox,
    "FOX (US)" to R.drawable.network_fox,
    "Fox Action Movies" to R.drawable.network_fox,
    "The CW" to R.drawable.network_cw,
    "CW" to R.drawable.network_cw,
    "PBS" to R.drawable.network_pbs,

    // Premium Cable
    "HBO" to R.drawable.network_hbo,
    "HBO Max" to R.drawable.network_hbo_max,
    "Max" to R.drawable.network_max,
    "HBO Films" to R.drawable.network_hbo_films,
    "Showtime" to R.drawable.network_showtime,
    "SHOWTIME" to R.drawable.network_showtime,
    "Starz" to R.drawable.network_starz,
    "STARZ" to R.drawable.network_starz,

    // Cable Networks
    "AMC" to R.drawable.network_amc,
    "AMC+" to R.drawable.network_amc,
    "FX" to R.drawable.network_fx,
    "FX Networks" to R.drawable.network_fx,
    "FXX" to R.drawable.network_fxx,
    "USA Network" to R.drawable.network_usa,
    "USA" to R.drawable.network_usa,
    "TNT" to R.drawable.network_tnt,
    "TNT (US)" to R.drawable.network_tnt,
    "TBS" to R.drawable.network_tbs,
    "Syfy" to R.drawable.network_syfy,
    "SyFy" to R.drawable.network_syfy,
    "SYFY" to R.drawable.network_syfy,
    "Bravo" to R.drawable.network_bravo,
    "E!" to R.drawable.network_e,
    "E! Entertainment" to R.drawable.network_e,
    "Oxygen" to R.drawable.network_oxygen,
    "Lifetime" to R.drawable.network_lifetime,
    "Hallmark Channel" to R.drawable.network_hallmark,
    "Hallmark" to R.drawable.network_hallmark,
    "Freeform" to R.drawable.network_freeform,
    "Paramount Network" to R.drawable.network_paramount_network,
    "A&E" to R.drawable.network_ae,
    "TV Land" to R.drawable.network_tv_land,
    "Pop" to R.drawable.network_pop,
    "Pop TV" to R.drawable.network_pop,
    "Reelz" to R.drawable.network_reelz,
    "WE tv" to R.drawable.network_we_tv,
    "TCM" to R.drawable.network_tcm,
    "Turner Classic Movies" to R.drawable.network_tcm,
    "truTV" to R.drawable.network_tru_tv,
    "TruTV" to R.drawable.network_tru_tv,
    "IFC" to R.drawable.network_ifc,
    "Sundance TV" to R.drawable.network_sundance,
    "SundanceTV" to R.drawable.network_sundance,

    // Kids/Animation
    "Adult Swim" to R.drawable.network_adult_swim,
    "Cartoon Network" to R.drawable.network_cartoon_network,
    "Nickelodeon" to R.drawable.network_nickelodeon,
    "Nick" to R.drawable.network_nick,
    "Disney Channel" to R.drawable.network_disney_channel,
    "Disney XD" to R.drawable.network_disney_xd,
    "Disney Jr." to R.drawable.network_disney_channel,
    "Disney Junior" to R.drawable.network_disney_channel,

    // Music/Entertainment
    "MTV" to R.drawable.network_mtv,
    "VH1" to R.drawable.network_vh1,
    "BET" to R.drawable.network_bet,
    "Comedy Central" to R.drawable.network_comedy_central,

    // Sports
    "ESPN" to R.drawable.network_espn,
    "ESPN2" to R.drawable.network_espn2,
    "ESPN 2" to R.drawable.network_espn2,
    "Fox Sports" to R.drawable.network_fox_sports,
    "Fox Sports 1" to R.drawable.network_fox_sports,
    "FS1" to R.drawable.network_fox_sports,

    // Documentary/Educational
    "National Geographic" to R.drawable.network_national_geographic,
    "Nat Geo" to R.drawable.network_national_geographic,
    "NatGeo" to R.drawable.network_national_geographic,
    "Nat Geo Wild" to R.drawable.network_nat_geo_wild,
    "History" to R.drawable.network_history,
    "History Channel" to R.drawable.network_history,
    "HISTORY" to R.drawable.network_history,
    "Discovery" to R.drawable.network_discovery,
    "Discovery Channel" to R.drawable.network_discovery,
    "Animal Planet" to R.drawable.network_animal_planet,
    "TLC" to R.drawable.network_tlc,

    // Lifestyle
    "Food Network" to R.drawable.network_food_network,
    "HGTV" to R.drawable.network_hgtv,
    "Travel Channel" to R.drawable.network_travel_channel,

    // Streaming Services
    "Netflix" to R.drawable.network_netflix,
    "Disney+" to R.drawable.network_disney_plus,
    "Disney Plus" to R.drawable.network_disney_plus,
    "Paramount+" to R.drawable.network_paramount_plus,
    "Paramount Plus" to R.drawable.network_paramount_plus,
    "Paramount+ with Showtime" to R.drawable.network_paramount_plus,
    "Peacock" to R.drawable.network_peacock,
    "Apple TV+" to R.drawable.network_apple_tv_plus,
    "Apple TV" to R.drawable.network_apple_tv_plus,
    "Apple TV Plus" to R.drawable.network_apple_tv_plus,
    "Amazon Prime" to R.drawable.network_amazon_prime,
    "Amazon Prime Video" to R.drawable.network_amazon_prime,
    "Prime Video" to R.drawable.network_amazon_prime,
    "Amazon" to R.drawable.network_amazon_prime,
    "Amazon Freevee" to R.drawable.network_amazon_prime,
    "Hulu" to R.drawable.network_hulu,
    "MGM+" to R.drawable.network_mgm_plus,
    "MGM Plus" to R.drawable.network_mgm_plus,
    "Epix" to R.drawable.network_epix,
    "EPIX" to R.drawable.network_epix,
    "CBS All Access" to R.drawable.network_paramount_plus,
    "DC Universe" to R.drawable.network_max,

    // UK Networks
    "BBC One" to R.drawable.network_bbc_one,
    "BBC 1" to R.drawable.network_bbc_one,
    "BBC Two" to R.drawable.network_bbc_two,
    "BBC 2" to R.drawable.network_bbc_two,
    "ITV" to R.drawable.network_itv,
    "ITV1" to R.drawable.network_itv1,
    "ITV 1" to R.drawable.network_itv1,
    "Sky Atlantic" to R.drawable.network_sky_atlantic,
    "Sky Atlantic (UK)" to R.drawable.network_sky_atlantic,
    "Channel 4" to R.drawable.network_channel_4,
    "Channel 5" to R.drawable.network_channel_5,
)

/**
 * Priority list for partial matching multi-network names.
 * Order matters - first match wins.
 */
private val networkMatchPriority = listOf(
    "Paramount+" to R.drawable.network_paramount_plus,
    "Showtime" to R.drawable.network_showtime,
    "HBO Max" to R.drawable.network_hbo_max,
    "Max" to R.drawable.network_max,
    "HBO" to R.drawable.network_hbo,
    "Netflix" to R.drawable.network_netflix,
    "Disney+" to R.drawable.network_disney_plus,
    "Disney" to R.drawable.network_disney_plus,
    "Peacock" to R.drawable.network_peacock,
    "Apple TV" to R.drawable.network_apple_tv_plus,
    "Prime Video" to R.drawable.network_amazon_prime,
    "Amazon" to R.drawable.network_amazon_prime,
    "Hulu" to R.drawable.network_hulu,
    "MGM" to R.drawable.network_mgm_plus,
    "Epix" to R.drawable.network_epix,
    "Starz" to R.drawable.network_starz,
    "Syfy" to R.drawable.network_syfy,
    "AMC" to R.drawable.network_amc,
    "FX" to R.drawable.network_fx,
    "BBC" to R.drawable.network_bbc_one,
    "Sky" to R.drawable.network_sky_atlantic,
    "ITV" to R.drawable.network_itv,
    "National Geographic" to R.drawable.network_national_geographic,
    "Nat Geo" to R.drawable.network_national_geographic,
    "Discovery" to R.drawable.network_discovery,
    "History" to R.drawable.network_history,
    "Cartoon Network" to R.drawable.network_cartoon_network,
    "Nickelodeon" to R.drawable.network_nickelodeon,
    "ESPN" to R.drawable.network_espn,
)

/**
 * Get embedded drawable resource ID for a network name.
 * Handles exact matches first, then tries partial matching for combined networks.
 */
private fun getNetworkLogoResource(name: String): Int? {
    // Try exact match first
    networkLogoResources[name]?.let { return it }
    networkLogoResources[name.trim()]?.let { return it }

    // Try partial match for combined network names (e.g., "Paramount+ with Showtime")
    val trimmedName = name.trim()
    for ((keyword, resource) in networkMatchPriority) {
        if (trimmedName.contains(keyword, ignoreCase = true)) {
            return resource
        }
    }

    return null
}

/**
 * Inline network logo for use in rating row.
 * Uses embedded drawable resources for instant display.
 * Falls back to styled badge if no mapping exists.
 */
@Suppress("UnusedParameter")
@Composable
private fun NetworkLogo(
    networkName: String?,
    jellyfinClient: JellyfinClient,
) {
    // Get embedded drawable resource (instant, no network request)
    val logoResource = remember(networkName) {
        networkName?.let { getNetworkLogoResource(it) }
    }

    if (logoResource != null) {
        // Show embedded logo - height matches badge (~12dp for labelSmall + padding)
        Image(
            painter = painterResource(id = logoResource),
            contentDescription = networkName ?: "Network",
            modifier = Modifier.height(12.dp),
            contentScale = ContentScale.Fit,
        )
    } else if (!networkName.isNullOrBlank()) {
        // Show styled badge with network name for unmapped networks
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF424242))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = networkName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}