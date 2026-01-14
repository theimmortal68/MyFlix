@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.creators
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.imdbId
import dev.jausc.myflix.core.common.model.tmdbId
import dev.jausc.myflix.core.common.model.writers
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MediaInfoBottomSheet
import dev.jausc.myflix.mobile.ui.components.MobileMediaCard
import dev.jausc.myflix.mobile.ui.components.MobileWideMediaCard
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import dev.jausc.myflix.mobile.ui.components.detail.CastCrewSection
import dev.jausc.myflix.mobile.ui.components.detail.DetailInfoItem
import dev.jausc.myflix.mobile.ui.components.detail.DetailInfoSection
import dev.jausc.myflix.mobile.ui.components.detail.ExternalLinkItem
import dev.jausc.myflix.mobile.ui.components.detail.ExternalLinksRow
import dev.jausc.myflix.mobile.ui.components.detail.GenreText
import dev.jausc.myflix.mobile.ui.components.detail.ItemRow
import dev.jausc.myflix.mobile.ui.components.detail.OverviewDialog
import dev.jausc.myflix.mobile.ui.components.detail.OverviewText
import dev.jausc.myflix.mobile.ui.components.detail.SeriesActionButtons
import dev.jausc.myflix.mobile.ui.components.detail.SeriesQuickDetails
import java.util.Locale

/**
 * Wholphin-style series detail screen for mobile.
 * Text-based header with seasons, cast, and similar items rows.
 */
@Composable
fun SeriesDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSeasonClick: (JellyfinItem) -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val series = state.item ?: return

    // Dialog state
    var showOverviewDialog by remember { mutableStateOf(false) }
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
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

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        // Header with action buttons
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                SeriesDetailsHeader(
                    series = series,
                    overviewOnClick = { showOverviewDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )

                // Action buttons row
                SeriesActionButtons(
                    watched = watched,
                    favorite = favorite,
                    onPlayClick = onPlayClick,
                    onShuffleClick = onShuffleClick,
                    onWatchedClick = onWatchedClick,
                    onFavoriteClick = onFavoriteClick,
                    onMoreClick = { mediaInfoItem = series },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Details
        if (detailInfoItems.isNotEmpty()) {
            item(key = "details") {
                DetailInfoSection(
                    title = "Details",
                    items = detailInfoItems,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // External links
        if (externalLinks.isNotEmpty()) {
            item(key = "links") {
                ExternalLinksRow(
                    title = "External Links",
                    links = externalLinks,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Next Up
        state.nextUpEpisode?.let { nextUp ->
            item(key = "next_up") {
                ItemRow(
                    title = "Next Up",
                    items = listOf(nextUp),
                    onItemClick = { _, item -> onPlayItemClick(item.id, null) },
                    onItemLongClick = { _, _ ->
                        // TODO: Show episode context menu
                    },
                    cardContent = { _, item, onClick, onLongClick ->
                        if (item != null) {
                            MobileWideMediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getThumbUrl(
                                    item.id,
                                    item.imageTags?.thumb ?: item.imageTags?.primary,
                                ),
                                onClick = onClick,
                                onLongClick = onLongClick,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Episodes row (selected season)
        if (state.episodes.isNotEmpty()) {
            item(key = "episodes") {
                ItemRow(
                    title = "Episodes",
                    items = state.episodes,
                    onItemClick = { _, episode -> onPlayItemClick(episode.id, null) },
                    onItemLongClick = { _, _ ->
                        // TODO: Show episode context menu
                    },
                    cardContent = { _, item, onClick, onLongClick ->
                        if (item != null) {
                            MobileWideMediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getThumbUrl(
                                    item.id,
                                    item.imageTags?.thumb ?: item.imageTags?.primary,
                                ),
                                onClick = onClick,
                                onLongClick = onLongClick,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Seasons row
        if (state.seasons.isNotEmpty()) {
            item(key = "seasons") {
                ItemRow(
                    title = "Seasons (${state.seasons.size})",
                    items = state.seasons,
                    onItemClick = { _, season -> onSeasonClick(season) },
                    onItemLongClick = { _, _ ->
                        // TODO: Show season context menu
                    },
                    cardContent = { _, item, onClick, onLongClick ->
                        if (item != null) {
                            MobileMediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getPrimaryImageUrl(
                                    item.id,
                                    item.imageTags?.primary,
                                ),
                                onClick = onClick,
                                onLongClick = onLongClick,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                        onNavigateToPerson(person.id)
                    },
                    onPersonLongClick = { _, _ ->
                        // TODO: Show person context menu
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                        onNavigateToPerson(person.id)
                    },
                    onPersonLongClick = { _, _ ->
                        // TODO: Show person context menu
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Extras
        if (state.specialFeatures.isNotEmpty()) {
            item(key = "extras") {
                ItemRow(
                    title = "Extras",
                    items = state.specialFeatures,
                    onItemClick = { _, item -> onPlayItemClick(item.id, null) },
                    onItemLongClick = { _, _ ->
                        // TODO: Show item context menu
                    },
                    cardContent = { _, item, onClick, onLongClick ->
                        if (item != null) {
                            MobileWideMediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getThumbUrl(
                                    item.id,
                                    item.imageTags?.thumb ?: item.imageTags?.primary,
                                ),
                                onClick = onClick,
                                onLongClick = onLongClick,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                            onItemClick = { _, item -> onNavigateToDetail(item.id) },
                            onItemLongClick = { _, _ ->
                                // TODO: Show item context menu
                            },
                            cardContent = { _, item, onClick, onLongClick ->
                                if (item != null) {
                                    MobileMediaCard(
                                        item = item,
                                        imageUrl = jellyfinClient.getPrimaryImageUrl(
                                            item.id,
                                            item.imageTags?.primary,
                                        ),
                                        onClick = onClick,
                                        onLongClick = onLongClick,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
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
                    onItemClick = { _, item -> onNavigateToDetail(item.id) },
                    onItemLongClick = { _, _ ->
                        // TODO: Show item context menu
                    },
                    cardContent = { _, item, onClick, onLongClick ->
                        if (item != null) {
                            MobileMediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getPrimaryImageUrl(
                                    item.id,
                                    item.imageTags?.primary,
                                ),
                                onClick = onClick,
                                onLongClick = onLongClick,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Similar Items (More Like This)
        if (state.similarItems.isNotEmpty()) {
            item(key = "similar") {
                ItemRow(
                    title = "More Like This",
                    items = state.similarItems,
                    onItemClick = { _, item -> onNavigateToDetail(item.id) },
                    onItemLongClick = { _, _ ->
                        // TODO: Show item context menu
                    },
                    cardContent = { _, item, onClick, onLongClick ->
                        if (item != null) {
                            MobileMediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getPrimaryImageUrl(
                                    item.id,
                                    item.imageTags?.primary,
                                ),
                                onClick = onClick,
                                onLongClick = onLongClick,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
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

    // Popup menu
    popupMenuParams?.let { params ->
        PopupMenu(
            params = params,
            onDismiss = { popupMenuParams = null },
        )
    }

    // Media info bottom sheet
    mediaInfoItem?.let { item ->
        MediaInfoBottomSheet(
            item = item,
            onDismiss = { mediaInfoItem = null },
        )
    }
}

/**
 * Series details header with title, quick details, genres, and overview.
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Quick details: year, status
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
                    maxLines = 4,
                    onClick = overviewOnClick,
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
