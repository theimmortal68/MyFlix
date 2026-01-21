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
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.imdbId
import dev.jausc.myflix.core.common.model.tmdbId
import dev.jausc.myflix.core.common.model.videoStream
import dev.jausc.myflix.core.common.model.writers
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
import dev.jausc.myflix.mobile.ui.components.detail.ChaptersRow
import dev.jausc.myflix.core.common.model.DetailInfoItem
import dev.jausc.myflix.mobile.ui.components.detail.DetailInfoSection
import dev.jausc.myflix.core.common.model.ExternalLinkItem
import dev.jausc.myflix.mobile.ui.components.detail.ExternalLinksRow
import dev.jausc.myflix.mobile.ui.components.detail.GenreText
import dev.jausc.myflix.mobile.ui.components.detail.ItemRow
import dev.jausc.myflix.mobile.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.mobile.ui.components.detail.MoviePlayButtons
import dev.jausc.myflix.mobile.ui.components.detail.MovieQuickDetails
import dev.jausc.myflix.mobile.ui.components.detail.OverviewDialog
import dev.jausc.myflix.mobile.ui.components.detail.OverviewText
import java.util.Locale

/**
 * Wholphin-style movie detail screen for mobile.
 * Text-based header with title, metadata, and action buttons.
 */
@Composable
fun MovieDetailScreen(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onPlayClick: (Long?) -> Unit,
    onPlayItemClick: (String, Long?) -> Unit,
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movie = state.item ?: return

    // Detect trailer for the trailer button
    val trailerItem = remember(state.specialFeatures) {
        findNewestTrailer(state.specialFeatures)
    }

    // Check for YouTube remote trailer if no local trailer
    val remoteTrailer = remember(movie.remoteTrailers) {
        movie.remoteTrailers?.firstOrNull()
    }

    // Build action for trailer button - prefer local, fallback to remote
    val trailerAction: (() -> Unit)? = remember(trailerItem, remoteTrailer) {
        when {
            trailerItem != null -> {
                { onPlayItemClick(trailerItem.id, null) }
            }
            remoteTrailer != null -> {
                extractYouTubeVideoKey(remoteTrailer.url)?.let { key ->
                    { onTrailerClick(key, remoteTrailer.name) }
                }
            }
            else -> null
        }
    }

    // Build categorized feature sections (excluding the trailer already shown)
    val featureSections = remember(state.specialFeatures, trailerItem?.id) {
        buildFeatureSections(state.specialFeatures, trailerItem?.id?.let { setOf(it) } ?: emptySet())
    }

    // Dialog state
    var showOverviewDialog by remember { mutableStateOf(false) }
    var popupMenuParams by remember { mutableStateOf<BottomSheetParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }

    val resumePositionTicks = movie.userData?.playbackPositionTicks ?: 0L
    val watched = movie.userData?.played == true
    val favorite = movie.userData?.isFavorite == true

    // Cast & crew
    val cast = remember(movie.people) {
        movie.people?.filter { it.type == "Actor" } ?: emptyList()
    }
    val crew = remember(movie.people) {
        movie.people?.filter { it.type != "Actor" } ?: emptyList()
    }

    // Get director name
    val directorName = remember(movie.people) {
        movie.people
            ?.filter { it.type == "Director" && !it.name.isNullOrBlank() }
            ?.joinToString(", ") { it.name!! }
    }

    val detailInfoItems = remember(movie) {
        buildList {
            movie.productionYear?.let { add(DetailInfoItem("Year", it.toString())) }
            movie.runTimeTicks?.let { ticks ->
                val minutes = ticks / 600_000_000
                if (minutes > 0) add(DetailInfoItem("Runtime", "${minutes}m"))
            }
            movie.communityRating?.let {
                add(DetailInfoItem("User Rating", String.format(Locale.US, "%.1f/10", it)))
            }
            movie.criticRating?.let {
                add(DetailInfoItem("Critic Rating", formatCriticRating(it)))
            }
            movie.studios?.mapNotNull { it.name }?.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Studios", it.joinToString(", ")))
            }
            movie.directors.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Director", it.joinToString(", ")))
            }
            movie.writers.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Writers", it.joinToString(", ")))
            }
            movie.genres?.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Genres", it.joinToString(", ")))
            }
            movie.tags?.takeIf { it.isNotEmpty() }?.let {
                add(DetailInfoItem("Tags", it.joinToString(", ")))
            }
            movie.videoStream?.let { video ->
                val videoLabel = buildString {
                    append(video.codec?.uppercase() ?: "Unknown")
                    video.width?.let { w -> video.height?.let { h -> append(" - ${w}x$h") } }
                    video.videoRangeType?.let { append(" - $it") }
                }
                add(DetailInfoItem("Video", videoLabel))
            }
            val audioStream = movie.mediaSources
                ?.firstOrNull()
                ?.mediaStreams
                ?.firstOrNull { it.type == "Audio" }
            audioStream?.let { audio ->
                val audioLabel = buildString {
                    append(audio.codec?.uppercase() ?: "Unknown")
                    audio.channels?.let { append(" - ${it}ch") }
                    audio.language?.let { append(" - $it") }
                }
                add(DetailInfoItem("Audio", audioLabel))
            }
            val subtitleLanguages = movie.mediaSources
                ?.firstOrNull()
                ?.mediaStreams
                ?.filter { it.type == "Subtitle" }
                ?.mapNotNull { it.language }
                ?.distinct()
                .orEmpty()
            if (subtitleLanguages.isNotEmpty()) {
                add(DetailInfoItem("Subtitles", subtitleLanguages.joinToString(", ")))
            }
        }
    }

    val externalLinks = remember(movie.externalUrls, movie.imdbId, movie.tmdbId) {
        buildExternalLinks(movie)
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
                    onPlayClick = { resumeTicks ->
                        onPlayClick(resumeTicks / 10_000)
                    },
                    onRestartClick = { onPlayItemClick(movie.id, 0L) },
                    onWatchedClick = onWatchedClick,
                    onFavoriteClick = onFavoriteClick,
                    onMoreClick = { mediaInfoItem = movie },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

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

        // Categorized feature sections (Trailers, Featurettes, Behind the Scenes, etc.)
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

            // Media badges: resolution, codec, HDR/DV, audio
            MediaBadgesRow(
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

private fun buildExternalLinks(movie: JellyfinItem): List<ExternalLinkItem> {
    val links = mutableListOf<ExternalLinkItem>()

    movie.externalUrls?.forEach { url ->
        val label = url.name?.trim().orEmpty()
        val link = url.url?.trim().orEmpty()
        if (label.isNotEmpty() && link.isNotEmpty()) {
            links.add(ExternalLinkItem(label, link))
        }
    }

    movie.imdbId?.let { imdbId ->
        val hasImdb = links.any { it.label.equals("imdb", ignoreCase = true) }
        if (!hasImdb) {
            links.add(ExternalLinkItem("IMDb", "https://www.imdb.com/title/$imdbId"))
        }
    }

    movie.tmdbId?.let { tmdbId ->
        val hasTmdb = links.any { it.label.equals("tmdb", ignoreCase = true) }
        if (!hasTmdb) {
            links.add(ExternalLinkItem("TMDB", "https://www.themoviedb.org/movie/$tmdbId"))
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
