@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.GenreText
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.SeriesActionButtons
import dev.jausc.myflix.tv.ui.components.detail.SeriesQuickDetails
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier,
) {
    val series = state.item ?: return
    val scope = rememberCoroutineScope()

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(SIMILAR_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val playFocusRequester = remember { FocusRequester() }

    // Dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    val watched = series.userData?.played == true
    val favorite = series.userData?.isFavorite == true

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

    // Cast & crew
    val cast = remember(series.people) {
        series.people?.filter { it.type == "Actor" } ?: emptyList()
    }
    val crew = remember(series.people) {
        series.people?.filter { it.type != "Actor" } ?: emptyList()
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
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Backdrop image (right side, behind content)
        DetailBackdropLayer(
            item = series,
            jellyfinClient = jellyfinClient,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .fillMaxHeight(0.9f)
                .align(Alignment.TopEnd),
        )

        // Layer 3: Content
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header with action buttons
            item(key = "header") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester),
                ) {
                    SeriesDetailsHeader(
                        series = series,
                        status = series.status,
                        studioNames = series.studios?.mapNotNull { it.name }.orEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 16.dp),
                    )

                    // Action buttons row
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
                        onMoreClick = { mediaInfoItem = series },
                        onTrailerClick = trailerAction,
                        showMoreButton = false,
                        buttonOnFocusChanged = {
                            if (it.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .focusRequester(focusRequesters[HEADER_ROW])
                            .focusRestorer(playFocusRequester)
                            .focusGroup(),
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
                                        item.imageTags?.thumb,
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
                            // TODO: Show season context menu
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
}

/**
 * Series details header with title, quick details, genres, and overview.
 * Left-aligned content (45% width) to work with backdrop on the right.
 */
@Composable
private fun SeriesDetailsHeader(
    series: JellyfinItem,
    status: String?,
    studioNames: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // Title
        Text(
            text = series.name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(0.50f),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(0.45f),
        ) {
            // Quick details: year, episode count, status
            SeriesQuickDetails(
                item = series,
                status = status,
                studios = studioNames,
            )

            // Genres
            if (!series.genres.isNullOrEmpty()) {
                GenreText(
                    genres = series.genres!!,
                )
            }

            // Overview (full text)
            series.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }
        }
    }
}
