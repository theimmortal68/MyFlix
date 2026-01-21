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
import androidx.compose.ui.text.font.FontStyle
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
import dev.jausc.myflix.core.common.model.directorNames
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.detail.IconColors
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.ChaptersRow
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButtons
import dev.jausc.myflix.tv.ui.components.detail.GenreText
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.tv.ui.components.detail.MovieQuickDetails
import dev.jausc.myflix.tv.ui.components.detail.OverviewDialog
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Row indices for focus management
private const val HEADER_ROW = 0
private const val CHAPTERS_ROW = HEADER_ROW + 1
private const val CAST_ROW = CHAPTERS_ROW + 1
private const val CREW_ROW = CAST_ROW + 1
private const val EXTRAS_ROW = CREW_ROW + 1
private const val COLLECTIONS_ROW = EXTRAS_ROW + 1
private const val SIMILAR_ROW = COLLECTIONS_ROW + 1

/**
 * Plex-style movie detail screen with backdrop hero.
 * Features layered UI with dynamic background, backdrop image, and left-aligned metadata.
 */
@Composable
fun MovieDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: (Long?) -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onNavigate: (NavItem) -> Unit = {},
    showUniversesInNav: Boolean = false,
    showDiscoverInNav: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val movie = state.item ?: return
    val scope = rememberCoroutineScope()

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(SIMILAR_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }
    var showOverview by remember { mutableStateOf(false) }
    val descriptionFocusRequester = remember { FocusRequester() }

    val resumePositionTicks = movie.userData?.playbackPositionTicks ?: 0L
    val watched = movie.userData?.played == true
    val favorite = movie.userData?.isFavorite == true

    // Cast & crew (using extension properties from JellyfinItem)
    val cast = movie.actors
    val crew = movie.crew

    // Backdrop URL and dynamic gradient colors
    val backdropUrl = remember(movie.id) {
        jellyfinClient.getBackdropUrl(movie.id, movie.backdropImageTags?.firstOrNull())
    }
    val gradientColors = rememberGradientColors(backdropUrl)

    val playFocusRequester = remember { FocusRequester() }

    // Focus play button on load
    LaunchedEffect(Unit) {
        delay(100)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus errors
        }
    }

    // Layered UI: DynamicBackground → NavigationRail + Content (DetailBackdropLayer → Content)
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background (covers full screen including nav rail)
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left: Navigation Rail
            NavigationRail(
                selectedItem = NavItem.MOVIES,
                onItemSelected = onNavigate,
                showUniverses = showUniversesInNav,
                showDiscover = showDiscoverInNav,
                contentFocusRequester = focusRequesters[HEADER_ROW],
            )

            // Right: Content area
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // Layer 2: Backdrop image (right side, behind content) - matches home page positioning
            DetailBackdropLayer(
                item = movie,
                jellyfinClient = jellyfinClient,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .align(Alignment.TopEnd),
            )

            // Layer 3: Content - Column with fixed hero + scrollable content (like SeriesDetailScreen)
            Column(modifier = Modifier.fillMaxSize()) {
                // Hero section - doesn't scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester),
                ) {
                    // Hero content (left 50%) - title, rating, genres, description, buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(start = 10.dp, top = 16.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    MovieDetailsHeader(
                        movie = movie,
                        onOverviewClick = { showOverview = true },
                        descriptionFocusRequester = descriptionFocusRequester,
                        downFocusRequester = playFocusRequester,
                    )

                    // Action buttons
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
                                title = movie.name,
                                items = listOf(
                                    DialogItem(
                                        text = "Media Info",
                                        icon = Icons.Outlined.Info,
                                        iconTint = IconColors.MediaInfo,
                                        onClick = { mediaInfoItem = movie },
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
                            .focusRequester(focusRequesters[HEADER_ROW])
                            .focusProperties {
                                down = focusRequesters[CHAPTERS_ROW]
                                up = descriptionFocusRequester
                            }
                            .focusRestorer(playFocusRequester)
                            .focusGroup(),
                    )
                }
            }

            // Scrollable content rows (below fixed hero)
            LazyColumn(
                contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 0.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
            // Chapters (first item beneath hero section)
            if (!movie.chapters.isNullOrEmpty()) {
                item(key = "chapters") {
                    ChaptersRow(
                        chapters = movie.chapters!!,
                        itemId = movie.id,
                        getChapterImageUrl = { index ->
                            jellyfinClient.getChapterImageUrl(movie.id, index)
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

            // Extras
            if (state.specialFeatures.isNotEmpty()) {
                item(key = "extras") {
                    ItemRow(
                        title = "Extras",
                        items = state.specialFeatures,
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

            // Similar Items (More Like This)
            if (state.similarItems.isNotEmpty()) {
                item(key = "similar") {
                    ItemRow(
                        title = "More Like This",
                        items = state.similarItems,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[SIMILAR_ROW]),
                    )
                }
            }
            }
        } // End Column
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

    // Overview dialog
    if (showOverview) {
        OverviewDialog(
            title = movie.name,
            overview = movie.overview.orEmpty(),
            genres = movie.genres.orEmpty(),
            onDismiss = { showOverview = false },
        )
    }
}

/**
 * Movie details header matching series detail hero style.
 * Uses full width of parent column (which is already constrained to 50% of screen).
 */
@Composable
private fun MovieDetailsHeader(
    movie: JellyfinItem,
    onOverviewClick: () -> Unit,
    descriptionFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Title - matches series hero style
        Text(
            text = movie.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Quick details: year, runtime, "ends at", rating
        MovieQuickDetails(item = movie)

        Spacer(modifier = Modifier.height(6.dp))

        // Media badges: resolution, codec, HDR/DV, audio
        MediaBadgesRow(item = movie)

        Spacer(modifier = Modifier.height(6.dp))

        // Genres
        if (!movie.genres.isNullOrEmpty()) {
            GenreText(genres = movie.genres!!)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Tagline (italic)
        movie.taglines?.firstOrNull()?.let { tagline ->
            Text(
                text = tagline,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = TvColors.TextPrimary.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Description - 3 lines max, clickable to show full overview
        movie.overview?.let { overview ->
            OverviewText(
                overview = overview,
                maxLines = 3,
                onClick = onOverviewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester)
                    .focusProperties {
                        down = downFocusRequester
                        upFocusRequester?.let { up = it }
                    },
                paddingValues = PaddingValues(0.dp),
            )
        }
    }
}
