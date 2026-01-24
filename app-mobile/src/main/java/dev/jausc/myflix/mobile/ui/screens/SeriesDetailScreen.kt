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
import dev.jausc.myflix.core.common.model.ExternalLinkItem
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.imdbId
import dev.jausc.myflix.core.common.model.tmdbId
import dev.jausc.myflix.core.common.util.buildFeatureSections
import dev.jausc.myflix.core.common.util.extractYouTubeVideoKey
import dev.jausc.myflix.core.common.util.findNewestTrailer
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MediaInfoBottomSheet
import dev.jausc.myflix.mobile.ui.components.MobileMediaCard
import dev.jausc.myflix.mobile.ui.components.MobileWideMediaCard
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import dev.jausc.myflix.mobile.ui.components.detail.CastCrewSection
import dev.jausc.myflix.mobile.ui.components.detail.ExternalLinksRow
import dev.jausc.myflix.mobile.ui.components.detail.GenreText
import dev.jausc.myflix.mobile.ui.components.detail.ItemRow
import dev.jausc.myflix.mobile.ui.components.detail.SeriesActionButtons
import dev.jausc.myflix.mobile.ui.components.detail.SeriesQuickDetails

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
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val series = state.item ?: return

    // Dialog state
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
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
            if (key.isBlank()) {
                null
            } else {
                { onTrailerClick(key, trailerVideo.name) }
            }
        }
        else -> {
            null
        }
    }

    // Cast & crew
    val cast = remember(series.people) {
        series.people?.filter { it.type == "Actor" } ?: emptyList()
    }
    val crew = remember(series.people) {
        series.people?.filter { it.type != "Actor" } ?: emptyList()
    }

    val externalLinks = remember(series.externalUrls, series.imdbId, series.tmdbId) {
        buildExternalLinks(series)
    }
    val featureSections = remember(state.specialFeatures, trailerItem?.id) {
        buildFeatureSections(state.specialFeatures, trailerItem?.id?.let { setOf(it) } ?: emptySet())
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
                    status = series.status,
                    studioNames = series.studios?.mapNotNull { it.name }.orEmpty(),
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
                    onTrailerClick = trailerAction,
                    showMoreButton = false,
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
                            val thumbTag = item.imageTags?.thumb
                            val imageUrl = if (thumbTag != null) {
                                jellyfinClient.getThumbUrl(item.id, thumbTag)
                            } else {
                                jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary)
                            }
                            MobileWideMediaCard(
                                item = item,
                                imageUrl = imageUrl,
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

        featureSections.forEach { section ->
            item(key = "feature_${section.title}") {
                ItemRow(
                    title = section.title,
                    items = section.items,
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Quick details: year, status, studio
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
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
