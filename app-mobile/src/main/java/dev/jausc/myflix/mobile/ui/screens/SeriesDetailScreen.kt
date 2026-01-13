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
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MediaInfoBottomSheet
import dev.jausc.myflix.mobile.ui.components.MobileMediaCard
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import dev.jausc.myflix.mobile.ui.components.detail.CastCrewSection
import dev.jausc.myflix.mobile.ui.components.detail.GenreText
import dev.jausc.myflix.mobile.ui.components.detail.ItemRow
import dev.jausc.myflix.mobile.ui.components.detail.OverviewDialog
import dev.jausc.myflix.mobile.ui.components.detail.OverviewText
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
    onNavigateToDetail: (String) -> Unit,
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

    // Filter cast & crew
    val castAndCrew = remember(series.people) {
        series.people?.filter {
            it.type in listOf("Actor", "Director", "Writer", "Producer")
        } ?: emptyList()
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

        // Cast & Crew
        if (castAndCrew.isNotEmpty()) {
            item(key = "people") {
                CastCrewSection(
                    people = castAndCrew,
                    jellyfinClient = jellyfinClient,
                    onPersonClick = { _ ->
                        // TODO: Navigate to person detail
                    },
                    onPersonLongClick = { _, _ ->
                        // TODO: Show person context menu
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
