@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.imdbId
import dev.jausc.myflix.core.common.model.tmdbId
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.PlayQueueManager
import dev.jausc.myflix.core.player.QueueItem
import dev.jausc.myflix.core.player.QueueSource
import dev.jausc.myflix.mobile.ui.components.BottomSheetParams
import dev.jausc.myflix.mobile.ui.components.MenuItem
import dev.jausc.myflix.mobile.ui.components.MenuItemDivider
import dev.jausc.myflix.mobile.ui.components.MenuItemEntry
import dev.jausc.myflix.mobile.ui.components.MediaInfoBottomSheet
import dev.jausc.myflix.mobile.ui.components.MobileMediaCard
import dev.jausc.myflix.mobile.ui.components.MobileWideMediaCard
import dev.jausc.myflix.mobile.ui.components.PopupMenu
import dev.jausc.myflix.mobile.ui.components.detail.CastCrewSection
import dev.jausc.myflix.mobile.ui.components.detail.ExternalLinkItem
import dev.jausc.myflix.mobile.ui.components.detail.DotSeparatedRow
import dev.jausc.myflix.mobile.ui.components.detail.ExternalLinksRow
import dev.jausc.myflix.mobile.ui.components.detail.ItemRow
import dev.jausc.myflix.mobile.ui.components.detail.SeriesActionButtons
import java.util.Locale

/**
 * Season detail screen with seasons + episodes layout.
 */
@Composable
fun SeasonDetailScreen(
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
    onEpisodeWatchedToggle: (String, Boolean) -> Unit,
    onEpisodeFavoriteToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val series = state.item ?: return

    // Dialog state
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
    var subtitleMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }
    var preferredSubtitleIndex by remember { mutableStateOf<Int?>(null) }

    val watched = series.userData?.played == true
    val favorite = series.userData?.isFavorite == true

    // Cast & crew
    val cast = remember(series.people) {
        series.people?.filter { it.type == "Actor" } ?: emptyList()
    }
    val crew = remember(series.people) {
        series.people?.filter { it.type != "Actor" } ?: emptyList()
    }

    val selectedEpisode = remember(state.episodes, state.nextUpEpisode) {
        // Priority: nextUp > first non-watched > first episode
        val nextUp = state.nextUpEpisode?.let { next ->
            state.episodes.firstOrNull { it.id == next.id }
        }
        nextUp ?: state.episodes.firstOrNull { it.userData?.played != true }
            ?: state.episodes.firstOrNull()
    }
    val selectedSeasonIndex = remember(state.selectedSeason, state.seasons) {
        state.seasons.indexOfFirst { it.id == state.selectedSeason?.id }.coerceAtLeast(0)
    }
    val guestStars = remember(selectedEpisode?.people) {
        selectedEpisode?.people?.filter { it.type == "GuestStar" } ?: emptyList()
    }

    val externalLinks = remember(series.externalUrls, series.imdbId, series.tmdbId) {
        buildExternalLinks(series)
    }
    val featureSections = remember(state.specialFeatures) {
        buildFeatureSections(state.specialFeatures, emptySet())
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
                SeasonDetailsHeader(
                    series = series,
                    seasons = state.seasons,
                    selectedSeasonIndex = selectedSeasonIndex,
                    onSeasonClick = onSeasonClick,
                    selectedEpisode = selectedEpisode,
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
                    onMoreClick = {
                        val episode = selectedEpisode ?: return@SeriesActionButtons
                            val seasonLabel = buildSeasonEpisodeLabel(episode)
                            popupMenuParams = buildEpisodeMenu(
                                title = listOfNotNull(series.seriesName ?: series.name, seasonLabel)
                                    .joinToString(" - "),
                                subtitle = episode.name,
                                episode = episode,
                                onPlay = {
                                    PlayQueueManager.setSingleItem(
                                        itemId = episode.id,
                                        title = episode.name,
                                        episodeInfo = seasonLabel,
                                        thumbnailItemId = episode.id,
                                        subtitleStreamIndex = preferredSubtitleIndex,
                                    )
                                    onPlayItemClick(episode.id, null)
                                },
                                onChooseSubtitles = {
                                    subtitleMenuParams = buildSubtitleMenu(
                                        episode = episode,
                                        selectedIndex = preferredSubtitleIndex,
                                        onSelect = { index -> preferredSubtitleIndex = index },
                                )
                            },
                            onAddToPlaylist = {
                                val items = state.episodes.map { item ->
                                    QueueItem(
                                        itemId = item.id,
                                        title = item.name,
                                        episodeInfo = buildSeasonEpisodeLabel(item),
                                        thumbnailItemId = item.id,
                                    )
                                }
                                PlayQueueManager.setQueue(items, QueueSource.SEASON_PLAY_ALL)
                            },
                            onToggleWatched = {
                                val isPlayed = episode.userData?.played == true
                                onEpisodeWatchedToggle(episode.id, !isPlayed)
                            },
                                                            onToggleFavorite = {
                                                                val isFavorite = episode.userData?.isFavorite == true
                                                                onEpisodeFavoriteToggle(episode.id, !isFavorite)
                                                            },
                                                            onGoToEpisode = { onNavigateToDetail(episode.id) },
                                                            onGoToSeries = { onNavigateToDetail(series.seriesId ?: series.id) },
                                                            onMediaInfo = { mediaInfoItem = episode },
                                                            onPlayWithTranscoding = {
                                                                PlayQueueManager.setSingleItem(
                                                                    itemId = episode.id,
                                                                    title = episode.name,
                                                                    episodeInfo = seasonLabel,
                                                                    thumbnailItemId = episode.id,
                                                                    subtitleStreamIndex = preferredSubtitleIndex,
                                                                )
                                                                onPlayItemClick(episode.id, null)
                                                            },
                                                        )
                                                    },
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
                                                onItemLongClick = { _, item ->
                                                    val seasonLabel = buildSeasonEpisodeLabel(item)
                                                    popupMenuParams = buildEpisodeMenu(
                                                        title = listOfNotNull(series.seriesName ?: series.name, seasonLabel)
                                                            .joinToString(" - "),
                                                        subtitle = item.name,
                                                        episode = item,
                                                        onPlay = {
                                                            PlayQueueManager.setSingleItem(
                                                                itemId = item.id,
                                                                title = item.name,
                                                                episodeInfo = seasonLabel,
                                                                thumbnailItemId = item.id,
                                                                subtitleStreamIndex = preferredSubtitleIndex,
                                                            )
                                                            onPlayItemClick(item.id, null)
                                                        },
                                                        onChooseSubtitles = {
                                                            subtitleMenuParams = buildSubtitleMenu(
                                                                episode = item,
                                                                selectedIndex = preferredSubtitleIndex,
                                                                onSelect = { index -> preferredSubtitleIndex = index },
                                                            )
                                                        },
                                                        onAddToPlaylist = {
                                                            PlayQueueManager.addItem(
                                                                QueueItem(
                                                                    itemId = item.id,
                                                                    title = item.name,
                                                                    episodeInfo = seasonLabel,
                                                                    thumbnailItemId = item.id,
                                                                ),
                                                            )
                                                        },
                                                        onToggleWatched = {
                                                            val isPlayed = item.userData?.played == true
                                                            onEpisodeWatchedToggle(item.id, !isPlayed)
                                                        },
                                                        onToggleFavorite = {
                                                            val isFavorite = item.userData?.isFavorite == true
                                                            onEpisodeFavoriteToggle(item.id, !isFavorite)
                                                        },
                                                        onGoToEpisode = { onNavigateToDetail(item.id) },
                                                        onGoToSeries = { onNavigateToDetail(series.seriesId ?: series.id) },
                                                        onMediaInfo = { mediaInfoItem = item },
                                                        onPlayWithTranscoding = {
                                                            PlayQueueManager.setSingleItem(
                                                                itemId = item.id,
                                                                title = item.name,
                                                                episodeInfo = seasonLabel,
                                                                thumbnailItemId = item.id,
                                                                subtitleStreamIndex = preferredSubtitleIndex,
                                                            )
                                                            onPlayItemClick(item.id, null)
                                                        },
                                                    )
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
                                                onItemLongClick = { _, item ->
                                                    val seasonLabel = buildSeasonEpisodeLabel(item)
                                                    popupMenuParams = buildEpisodeMenu(
                                                        title = listOfNotNull(series.seriesName ?: series.name, seasonLabel)
                                                            .joinToString(" - "),
                                                        subtitle = item.name,
                                                        episode = item,
                                                        onPlay = {
                                                            PlayQueueManager.setSingleItem(
                                                                itemId = item.id,
                                                                title = item.name,
                                                                episodeInfo = seasonLabel,
                                                                thumbnailItemId = item.id,
                                                                subtitleStreamIndex = preferredSubtitleIndex,
                                                            )
                                                            onPlayItemClick(item.id, null)
                                                        },
                                                        onChooseSubtitles = {
                                                            subtitleMenuParams = buildSubtitleMenu(
                                                                episode = item,
                                                                selectedIndex = preferredSubtitleIndex,
                                                                onSelect = { index -> preferredSubtitleIndex = index },
                                                            )
                                                        },
                                                        onAddToPlaylist = {
                                                            PlayQueueManager.addItem(
                                                                QueueItem(
                                                                    itemId = item.id,
                                                                    title = item.name,
                                                                    episodeInfo = seasonLabel,
                                                                    thumbnailItemId = item.id,
                                                                ),
                                                            )
                                                        },
                                                        onToggleWatched = {
                                                            val isPlayed = item.userData?.played == true
                                                            onEpisodeWatchedToggle(item.id, !isPlayed)
                                                        },
                                                        onToggleFavorite = {
                                                            val isFavorite = item.userData?.isFavorite == true
                                                            onEpisodeFavoriteToggle(item.id, !isFavorite)
                                                        },
                                                        onGoToEpisode = { onNavigateToDetail(item.id) },
                                                        onGoToSeries = { onNavigateToDetail(series.seriesId ?: series.id) },
                                                        onMediaInfo = { mediaInfoItem = item },
                                                        onPlayWithTranscoding = {
                                                            PlayQueueManager.setSingleItem(
                                                                itemId = item.id,
                                                                title = item.name,
                                                                episodeInfo = seasonLabel,
                                                                thumbnailItemId = item.id,
                                                                subtitleStreamIndex = preferredSubtitleIndex,
                                                            )
                                                            onPlayItemClick(item.id, null)
                                                        },
                                                    )
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

    // Popup menu
    popupMenuParams?.let { params ->
        PopupMenu(
            params = params,
            onDismiss = { popupMenuParams = null },
        )
    }

    subtitleMenuParams?.let { params ->
        PopupMenu(
            params = params,
            onDismiss = { subtitleMenuParams = null },
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

@Composable
private fun SeasonDetailsHeader(
    series: JellyfinItem,
    seasons: List<JellyfinItem>,
    selectedSeasonIndex: Int,
    onSeasonClick: (JellyfinItem) -> Unit,
    selectedEpisode: JellyfinItem?,
    modifier: Modifier = Modifier,
) {
    val showTitle = series.seriesName ?: series.name
    val episodeBadges = buildEpisodeBadges(selectedEpisode)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        SeasonChipRow(
            seasons = seasons,
            selectedIndex = selectedSeasonIndex,
            onSeasonClick = onSeasonClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = showTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )

        selectedEpisode?.let { episode ->
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                val details = buildEpisodeRatingLine(episode)
                if (details.isNotEmpty() || episode.communityRating != null) {
                    DotSeparatedRow(
                        texts = details,
                        communityRating = episode.communityRating,
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                }

                if (episodeBadges.isNotEmpty()) {
                    FormatBadgeRow(badges = episodeBadges)
                }
            }

            episode.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonChipRow(
    seasons: List<JellyfinItem>,
    selectedIndex: Int,
    onSeasonClick: (JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        seasons.forEachIndexed { index, season ->
            val selected = index == selectedIndex
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(20.dp),
                onClick = { onSeasonClick(season) },
            ) {
                Text(
                    text = season.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatBadgeRow(badges: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        badges.forEach { badge ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun buildEpisodeRatingLine(episode: JellyfinItem): List<String> = buildList {
    episode.officialRating?.let { add(it) }
    episode.runTimeTicks?.let { ticks ->
        val minutes = (ticks / 600_000_000L).toInt()
        if (minutes > 0) add("${minutes}m")
    }
}

private fun buildEpisodeBadges(episode: JellyfinItem?): List<String> {
    if (episode == null) return emptyList()
    val badges = mutableListOf<String>()
    val mediaSource = episode.mediaSources?.firstOrNull()
    val video = mediaSource?.mediaStreams?.firstOrNull { it.type == "Video" }
    val audio = mediaSource?.mediaStreams?.firstOrNull { it.type == "Audio" && it.isDefault } ?:
        mediaSource?.mediaStreams?.firstOrNull { it.type == "Audio" }

    val resolution = video?.height?.let { height ->
        when {
            height >= 2160 -> "4K"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            else -> null
        }
    }
    resolution?.let { badges.add(it) }
    video?.codec?.uppercase()?.let { badges.add(it) }

    if (episode.videoQualityLabel.contains("Dolby Vision")) {
        badges.add("Dolby Vision")
    } else if (episode.videoQualityLabel.contains("HDR")) {
        badges.add("HDR")
    }

    formatAudioBadge(audio)?.let { badges.add(it) }

    return badges.distinct()
}

private fun formatAudioBadge(stream: MediaStream?): String? {
    if (stream == null) return null
    val language = stream.language?.replaceFirstChar { it.titlecase(Locale.US) } ?: "Unknown"
    val codec = stream.codec?.uppercase()
    val channels = formatChannelLayout(stream.channels)
    return listOfNotNull(language, codec, channels).joinToString(" ")
}

private fun formatChannelLayout(channels: Int?): String? {
    return when (channels) {
        null -> null
        1 -> "1.0"
        2 -> "2.0"
        6 -> "5.1"
        8 -> "7.1"
        else -> "${channels}.0"
    }
}

private fun buildSeasonEpisodeLabel(episode: JellyfinItem): String? {
    val season = episode.parentIndexNumber
    val number = episode.indexNumber
    return if (season != null && number != null) {
        "S$season E$number"
    } else {
        null
    }
}

private fun buildSubtitleMenu(
    episode: JellyfinItem,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
): BottomSheetParams {
    val subtitleStreams = episode.mediaSources
        ?.firstOrNull()
        ?.mediaStreams
        ?.filter { it.type == "Subtitle" }
        .orEmpty()

    val items = buildList<MenuItemEntry> {
        add(
            MenuItem(
                text = "Off",
                icon = Icons.Outlined.Subtitles,
                onClick = { onSelect(null) },
            ),
        )
        subtitleStreams.forEach { stream ->
            val label = stream.displayTitle ?: stream.language ?: "Subtitle ${stream.index}"
            add(
                MenuItem(
                    text = label,
                    icon = Icons.Outlined.ClosedCaption,
                    onClick = { onSelect(stream.index) },
                ),
            )
        }
    }

    return BottomSheetParams(
        title = "Choose Subtitles",
        subtitle = episode.name,
        items = items,
    )
}

private fun buildEpisodeMenu(
    title: String,
    subtitle: String?,
    episode: JellyfinItem,
    onPlay: () -> Unit,
    onChooseSubtitles: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGoToEpisode: () -> Unit,
    onGoToSeries: () -> Unit,
    onMediaInfo: () -> Unit,
    onPlayWithTranscoding: () -> Unit,
): BottomSheetParams {
    val watched = episode.userData?.played == true
    val favorite = episode.userData?.isFavorite == true

    val items = buildList<MenuItemEntry> {
        add(
            MenuItem(
                text = "Play",
                icon = Icons.Outlined.PlayArrow,
                onClick = onPlay,
            ),
        )
        add(
            MenuItem(
                text = "Choose Subtitles",
                icon = Icons.Outlined.Subtitles,
                onClick = onChooseSubtitles,
            ),
        )
        add(
            MenuItem(
                text = "Episode Details",
                icon = Icons.Outlined.Info,
                onClick = onGoToEpisode,
            ),
        )
        add(
            MenuItem(
                text = "Add to playlist",
                icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                onClick = onAddToPlaylist,
            ),
        )
        add(
            MenuItem(
                text = if (watched) "Mark as unwatched" else "Mark as watched",
                icon = Icons.Outlined.Visibility,
                onClick = onToggleWatched,
            ),
        )
        add(
            MenuItem(
                text = if (favorite) "Remove favorite" else "Favorite",
                icon = Icons.Outlined.Favorite,
                onClick = onToggleFavorite,
            ),
        )
        add(
            MenuItem(
                text = "Go to series",
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                onClick = onGoToSeries,
            ),
        )
        add(MenuItemDivider)
        add(
            MenuItem(
                text = "Media Information",
                icon = Icons.Outlined.Info,
                onClick = onMediaInfo,
            ),
        )
        add(
            MenuItem(
                text = "Play with transcoding",
                icon = Icons.Outlined.PlayCircle,
                onClick = onPlayWithTranscoding,
            ),
        )
    }

    return BottomSheetParams(
        title = title,
        subtitle = subtitle,
        items = items,
    )
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
