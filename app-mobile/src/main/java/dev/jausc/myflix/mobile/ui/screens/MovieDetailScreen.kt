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
import androidx.compose.ui.text.font.FontStyle
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
import dev.jausc.myflix.mobile.ui.components.detail.MoviePlayButtons
import dev.jausc.myflix.mobile.ui.components.detail.MovieQuickDetails
import dev.jausc.myflix.mobile.ui.components.detail.OverviewDialog
import dev.jausc.myflix.mobile.ui.components.detail.OverviewText

/**
 * Wholphin-style movie detail screen for mobile.
 * Text-based header with title, metadata, and action buttons.
 */
@Composable
fun MovieDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: (resumePositionTicks: Long) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movie = state.item ?: return

    // Dialog state
    var showOverviewDialog by remember { mutableStateOf(false) }
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    val resumePositionTicks = movie.userData?.playbackPositionTicks ?: 0L
    val watched = movie.userData?.played == true
    val favorite = movie.userData?.isFavorite == true

    // Filter cast & crew
    val castAndCrew = remember(movie.people) {
        movie.people?.filter {
            it.type in listOf("Actor", "Director", "Writer", "Producer")
        } ?: emptyList()
    }

    // Get director name
    val directorName = remember(movie.people) {
        movie.people
            ?.filter { it.type == "Director" && !it.name.isNullOrBlank() }
            ?.joinToString(", ") { it.name!! }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        // Header with action buttons
        item(key = "header") {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                MovieDetailsHeader(
                    movie = movie,
                    directorName = directorName,
                    overviewOnClick = { showOverviewDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )

                MoviePlayButtons(
                    resumePositionTicks = resumePositionTicks,
                    watched = watched,
                    favorite = favorite,
                    onPlayClick = onPlayClick,
                    onWatchedClick = onWatchedClick,
                    onFavoriteClick = onFavoriteClick,
                    onMoreClick = { mediaInfoItem = movie },
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
    if (showOverviewDialog && movie.overview != null) {
        OverviewDialog(
            title = movie.name,
            overview = movie.overview!!,
            genres = movie.genres ?: emptyList(),
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
 * Movie details header with title, quick details, genres, tagline, overview, and director.
 */
@Composable
private fun MovieDetailsHeader(
    movie: JellyfinItem,
    directorName: String?,
    overviewOnClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // Title
        Text(
            text = movie.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Quick details: year, runtime, "ends at", rating
            MovieQuickDetails(
                item = movie,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Genres
            if (!movie.genres.isNullOrEmpty()) {
                GenreText(
                    genres = movie.genres!!,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Tagline (italic)
            movie.taglines?.firstOrNull()?.let { tagline ->
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }

            // Overview (clickable to expand)
            movie.overview?.let { overview ->
                OverviewText(
                    overview = overview,
                    maxLines = 4,
                    onClick = overviewOnClick,
                )
            }

            // Director
            directorName?.let {
                Text(
                    text = "Directed by $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}
