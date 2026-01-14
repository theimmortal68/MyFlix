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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.creators
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.imdbId
import dev.jausc.myflix.core.common.model.tmdbId
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.writers
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.DetailInfoItem
import dev.jausc.myflix.tv.ui.components.detail.DetailInfoSection
import dev.jausc.myflix.tv.ui.components.detail.EpisodeGrid
import dev.jausc.myflix.tv.ui.components.detail.ExternalLinkItem
import dev.jausc.myflix.tv.ui.components.detail.ExternalLinksRow
import dev.jausc.myflix.tv.ui.components.detail.GenreText
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.OverviewDialog
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import dev.jausc.myflix.tv.ui.components.detail.SeasonTabRow
import dev.jausc.myflix.tv.ui.components.detail.SeriesActionButtons
import dev.jausc.myflix.tv.ui.components.detail.SeriesQuickDetails
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.launch
import java.util.Locale

// Row indices for focus management
private const val HEADER_ROW = 0
private const val DETAILS_ROW = HEADER_ROW + 1
private const val LINKS_ROW = DETAILS_ROW + 1
private const val NEXT_UP_ROW = LINKS_ROW + 1
private const val SEASONS_ROW = NEXT_UP_ROW + 1
private const val EPISODES_ROW = SEASONS_ROW + 1
private const val CAST_ROW = EPISODES_ROW + 1
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
    onEpisodeClick: (String) -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onSeasonSelected: (JellyfinItem) -> Unit,
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
    var showOverviewDialog by remember { mutableStateOf(false) }
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    val watched = series.userData?.played == true
    val favorite = series.userData?.isFavorite == true

    // Cast & crew
    val cast = remember(series.people) {
        series.people?.filter { it.type == "Actor" } ?: emptyList()
    }
    val crew = remember(series.people) {
        series.people?.filter { it.type != "Actor" } ?: emptyList()
    }

    val detailInfoItems = remember(series) {
        buildList {
            series.productionYear?.let { add(DetailInfoItem("Year", it.toString())) }
            series.status?.let { add(DetailInfoItem("Status", it)) }
            series.childCount?.let { count ->
                add(DetailInfoItem("Seasons", count.toString()))
            }
            series.recursiveItemCount?.let { count ->
                add(DetailInfoItem("Episodes", count.toString()))
            }
            series.communityRating?.let {
                add(DetailInfoItem("User Rating", String.format(Locale.US, "%.1f/10", it)))
            }
            series.criticRating?.let {
                add(DetailInfoItem("Critic Rating", formatCriticRating(it)))
            }
            series.studios?.mapNotNull { it.name }?.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Studios", it.joinToString(", ")))
            }
            series.creators.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Creators", it.joinToString(", ")))
            }
            series.directors.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Directors", it.joinToString(", ")))
            }
            series.writers.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Writers", it.joinToString(", ")))
            }
            series.genres?.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Genres", it.joinToString(", ")))
            }
            series.tags?.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Tags", it.joinToString(", ")))
            }
        }
    }

    val externalLinks = remember(series.externalUrls, series.imdbId, series.tmdbId) {
        buildExternalLinks(series)
    }

    // Selected season index (derived from state.selectedSeason)
    val selectedSeasonIndex = remember(state.selectedSeason, state.seasons) {
        state.seasons.indexOfFirst { it.id == state.selectedSeason?.id }.coerceAtLeast(0)
    }

    // Auto-select first season if none selected
    LaunchedEffect(state.seasons) {
        if (state.selectedSeason == null && state.seasons.isNotEmpty()) {
            onSeasonSelected(state.seasons.first())
        }
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
                        overviewOnClick = { showOverviewDialog = true },
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

            // Details
            if (detailInfoItems.isNotEmpty()) {
                item(key = "details") {
                    DetailInfoSection(
                        title = "Details",
                        items = detailInfoItems,
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                }
            }

            // External links
            if (externalLinks.isNotEmpty()) {
                item(key = "links") {
                    ExternalLinksRow(
                        title = "External Links",
                        links = externalLinks,
                        modifier = Modifier.fillMaxWidth(0.6f),
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

            // Season tabs
            if (state.seasons.isNotEmpty()) {
                item(key = "seasons") {
                    SeasonTabRow(
                        seasons = state.seasons,
                        selectedSeasonIndex = selectedSeasonIndex,
                        onSeasonSelected = { _, season ->
                            position = SEASONS_ROW
                            onSeasonSelected(season)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[SEASONS_ROW]),
                    )
                }
            }

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
                        isLoading = state.isLoadingEpisodes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[EPISODES_ROW]),
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

    // Overview dialog
    if (showOverviewDialog && series.overview != null) {
        OverviewDialog(
            title = series.name,
            overview = series.overview!!,
            genres = series.genres ?: emptyList(),
            onDismiss = { showOverviewDialog = false },
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
}

/**
 * Series details header with title, quick details, genres, and overview.
 * Left-aligned content (45% width) to work with backdrop on the right.
 */
@Composable
private fun SeriesDetailsHeader(
    series: JellyfinItem,
    overviewOnClick: () -> Unit,
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
            )

            // Genres
            if (!series.genres.isNullOrEmpty()) {
                GenreText(
                    genres = series.genres!!,
                )
            }

            // Overview (clickable to expand)
            series.overview?.let { overview ->
                OverviewText(
                    overview = overview,
                    maxLines = 3,
                    onClick = overviewOnClick,
                    textBoxHeight = Dp.Unspecified,
                )
            }
        }
    }
}

private fun buildExternalLinks(series: JellyfinItem): List<ExternalLinkItem> {
    val links = mutableListOf<ExternalLinkItem>()

    series.externalUrls?.forEach { url ->
        val label = url.name?.trim().orEmpty()
        val link = url.url?.trim().orEmpty()
        if (label.isNotEmpty() && link.isNotEmpty()) {
            links.add(ExternalLinkItem(label, link))
        }
    }

    series.imdbId?.let { imdbId ->
        val hasImdb = links.any { it.label.equals("imdb", ignoreCase = true) }
        if (!hasImdb) {
            links.add(ExternalLinkItem("IMDb", "https://www.imdb.com/title/$imdbId"))
        }
    }

    series.tmdbId?.let { tmdbId ->
        val hasTmdb = links.any { it.label.equals("tmdb", ignoreCase = true) }
        if (!hasTmdb) {
            links.add(ExternalLinkItem("TMDB", "https://www.themoviedb.org/tv/$tmdbId"))
        }
    }

    return links
}

private fun formatCriticRating(rating: Float): String =
    if (rating > 10f) {
        "${rating.toInt()}%"
    } else {
        String.format(Locale.US, "%.1f/10", rating)
    }
