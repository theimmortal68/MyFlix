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
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.writers
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MediaInfoBottomSheet
import dev.jausc.myflix.mobile.ui.components.MobileMediaCard
import dev.jausc.myflix.mobile.ui.components.MobileWideMediaCard
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import dev.jausc.myflix.mobile.ui.components.detail.CastCrewSection
import dev.jausc.myflix.mobile.ui.components.detail.ChaptersRow
import dev.jausc.myflix.core.common.model.DetailInfoItem
import dev.jausc.myflix.mobile.ui.components.detail.DetailInfoSection
import dev.jausc.myflix.mobile.ui.components.detail.DotSeparatedRow
import dev.jausc.myflix.mobile.ui.components.detail.GenreText
import dev.jausc.myflix.mobile.ui.components.detail.ItemRow
import dev.jausc.myflix.mobile.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.mobile.ui.components.detail.MoviePlayButtons
import dev.jausc.myflix.mobile.ui.components.detail.OverviewDialog
import dev.jausc.myflix.mobile.ui.components.detail.OverviewText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Episode detail screen for mobile.
 * Follows the same style as MovieDetailScreen but adapted for episodes.
 */
@Composable
fun EpisodeDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: (Long?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val episode = state.item ?: return

    // Dialog state
    var showOverviewDialog by remember { mutableStateOf(false) }
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    val resumePositionTicks = episode.userData?.playbackPositionTicks ?: 0L
    val watched = episode.userData?.played == true
    val favorite = episode.userData?.isFavorite == true

    // People from episode (Guest stars, directors, writers)
    val guestStars = remember(episode.people) {
        episode.people?.filter { it.type == "GuestStar" } ?: emptyList()
    }
    val crew = remember(episode.people) {
        episode.people?.filter { it.type != "Actor" && it.type != "GuestStar" } ?: emptyList()
    }

    val detailInfoItems = remember(episode) {
        buildList {
            episode.parentIndexNumber?.let { add(DetailInfoItem("Season", it.toString())) }
            episode.indexNumber?.let { add(DetailInfoItem("Episode", it.toString())) }
            episode.premiereDate?.let { dateStr ->
                runCatching {
                    val date = LocalDate.parse(dateStr.take(10))
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                    add(DetailInfoItem("Aired", date.format(formatter)))
                }
            }
            episode.runTimeTicks?.let { ticks ->
                val minutes = ticks / 600_000_000
                if (minutes > 0) add(DetailInfoItem("Runtime", "${minutes}m"))
            }
            episode.communityRating?.let {
                add(DetailInfoItem("Rating", String.format(Locale.US, "%.1f/10", it)))
            }
            episode.directors.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Director", it.joinToString(", ")))
            }
            episode.writers.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Writers", it.joinToString(", ")))
            }
        }
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
                EpisodeDetailsHeader(
                    episode = episode,
                    overviewOnClick = { showOverviewDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )

                MoviePlayButtons(
                    resumePositionTicks = resumePositionTicks,
                    watched = watched,
                    favorite = favorite,
                    onPlayClick = { resumeTicks ->
                        onPlayClick(resumeTicks / 10_000)
                    },
                    onRestartClick = { onPlayClick(0L) },
                    onWatchedClick = onWatchedClick,
                    onFavoriteClick = onFavoriteClick,
                    onMoreClick = { mediaInfoItem = episode },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Chapters (first item beneath hero section)
        if (!episode.chapters.isNullOrEmpty()) {
            item(key = "chapters") {
                ChaptersRow(
                    chapters = episode.chapters!!,
                    itemId = episode.id,
                    getChapterImageUrl = { index ->
                        jellyfinClient.getChapterImageUrl(episode.id, index)
                    },
                    onChapterClick = { positionMs ->
                        onPlayClick(positionMs)
                    },
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

        // Guest Stars
        if (guestStars.isNotEmpty()) {
            item(key = "guest_stars") {
                CastCrewSection(
                    title = "Guest Stars",
                    people = guestStars,
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
    if (showOverviewDialog && episode.overview != null) {
        OverviewDialog(
            title = episode.name,
            overview = episode.overview!!,
            genres = emptyList(),
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
 * Episode details header with series name, title, quick details, and overview.
 */
@Composable
private fun EpisodeDetailsHeader(
    episode: JellyfinItem,
    overviewOnClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // Series Name
        episode.seriesName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }

        // Title
        Text(
            text = episode.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Quick details: S1 E3 · year · runtime · rating
            val metadata = buildList {
                val season = episode.parentIndexNumber
                val epNum = episode.indexNumber
                if (season != null && epNum != null) {
                    add("S$season E$epNum")
                }
                episode.premiereDate?.take(4)?.let { add(it) }
                episode.runTimeTicks?.let { ticks ->
                    val minutes = ticks / 600_000_000
                    if (minutes > 0) add("${minutes}m")
                }
            }

            if (metadata.isNotEmpty() || episode.communityRating != null) {
                DotSeparatedRow(
                    texts = metadata,
                    communityRating = episode.communityRating,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }

            // Media badges: resolution, codec, HDR/DV, audio
            MediaBadgesRow(
                item = episode,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Overview (clickable to expand)
            episode.overview?.let { overview ->
                OverviewText(
                    overview = overview,
                    maxLines = 4,
                    onClick = overviewOnClick,
                )
            }
        }
    }
}
