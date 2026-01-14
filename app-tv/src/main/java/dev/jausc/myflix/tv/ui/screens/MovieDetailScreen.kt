@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.imdbId
import dev.jausc.myflix.core.common.model.tmdbId
import dev.jausc.myflix.core.common.model.writers
import dev.jausc.myflix.core.common.model.videoStream
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.DetailInfoItem
import dev.jausc.myflix.tv.ui.components.detail.DetailInfoSection
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButtons
import dev.jausc.myflix.tv.ui.components.detail.ExternalLinkItem
import dev.jausc.myflix.tv.ui.components.detail.ExternalLinksRow
import dev.jausc.myflix.tv.ui.components.detail.GenreText
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.MovieQuickDetails
import dev.jausc.myflix.tv.ui.components.detail.OverviewDialog
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlinx.coroutines.launch
import java.util.Locale

// Row indices for focus management
private const val HEADER_ROW = 0
private const val CAST_ROW = HEADER_ROW + 1
private const val CREW_ROW = CAST_ROW + 1
private const val EXTRAS_ROW = CREW_ROW + 1
private const val COLLECTIONS_ROW = EXTRAS_ROW + 1
private const val RECOMMENDED_ROW = COLLECTIONS_ROW + 1
private const val SIMILAR_ROW = RECOMMENDED_ROW + 1

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
    modifier: Modifier = Modifier,
) {
    val movie = state.item ?: return
    val scope = rememberCoroutineScope()

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(SIMILAR_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Dialog state
    var showOverviewDialog by remember { mutableStateOf(false) }
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
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

    // Backdrop URL and dynamic gradient colors
    val backdropUrl = remember(movie.id) {
        jellyfinClient.getBackdropUrl(movie.id, movie.backdropImageTags?.firstOrNull())
    }
    val gradientColors = rememberGradientColors(backdropUrl)

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
                    video.width?.let { w -> video.height?.let { h -> append(" • ${w}x$h") } }
                    video.videoRangeType?.let { append(" • $it") }
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
                    audio.channels?.let { append(" • ${it}ch") }
                    audio.language?.let { append(" • $it") }
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

    // Layered UI: DynamicBackground → DetailBackdropLayer → Content
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Backdrop image (right side, behind content)
        DetailBackdropLayer(
            item = movie,
            jellyfinClient = jellyfinClient,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .fillMaxHeight(0.9f)
                .align(Alignment.TopEnd),
        )

        // Layer 3: Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
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
                    MovieDetailsHeader(
                        movie = movie,
                        directorName = directorName,
                        overviewOnClick = { showOverviewDialog = true },
                        bringIntoViewRequester = bringIntoViewRequester,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 16.dp),
                    )

                    ExpandablePlayButtons(
                        resumePositionTicks = resumePositionTicks,
                        watched = watched,
                        favorite = favorite,
                        onPlayClick = { resumeTicks ->
                            this@item
                            onPlayClick(resumeTicks / 10_000)
                        },
                        onWatchedClick = onWatchedClick,
                        onFavoriteClick = onFavoriteClick,
                        onMoreClick = {
                            mediaInfoItem = movie
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
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .focusRequester(focusRequesters[HEADER_ROW]),
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
    if (showOverviewDialog && movie.overview != null) {
        OverviewDialog(
            title = movie.name,
            overview = movie.overview!!,
            genres = movie.genres ?: emptyList(),
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
 * Movie details header with title, quick details, genres, tagline, overview, and director.
 * Left-aligned content (45% width) to work with backdrop on the right.
 */
@Composable
private fun MovieDetailsHeader(
    movie: JellyfinItem,
    directorName: String?,
    overviewOnClick: () -> Unit,
    bringIntoViewRequester: BringIntoViewRequester,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // Title
        Text(
            text = movie.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.50f),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(0.45f),
        ) {
            val padding = 4.dp

            // Quick details: year, runtime, "ends at", rating
            MovieQuickDetails(
                item = movie,
                modifier = Modifier.padding(bottom = padding),
            )

            // Genres
            if (!movie.genres.isNullOrEmpty()) {
                GenreText(
                    genres = movie.genres!!,
                    modifier = Modifier.padding(bottom = padding),
                )
            }

            // Tagline (italic)
            movie.taglines?.firstOrNull()?.let { tagline ->
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Overview (clickable to expand)
            movie.overview?.let { overview ->
                OverviewText(
                    overview = overview,
                    maxLines = 3,
                    onClick = overviewOnClick,
                    textBoxHeight = Dp.Unspecified,
                    modifier = Modifier.onFocusChanged {
                        if (it.isFocused) {
                            scope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                )
            }

            // Director
            directorName?.let {
                Text(
                    text = "Directed by $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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
