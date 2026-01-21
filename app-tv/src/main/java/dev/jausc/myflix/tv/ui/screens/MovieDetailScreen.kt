@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.crew
import dev.jausc.myflix.core.common.model.directorNames
import dev.jausc.myflix.core.common.util.buildFeatureSections
import dev.jausc.myflix.core.common.util.extractYouTubeVideoKey
import dev.jausc.myflix.core.common.util.findNewestTrailer
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.detail.IconColors
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.ChaptersRow
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.components.detail.DetailBackdropLayer
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButtons
import dev.jausc.myflix.tv.ui.components.detail.GenreText
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.tv.ui.components.detail.OverviewDialog
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Row indices for focus management
private const val HEADER_ROW = 0
private const val CHAPTERS_ROW = HEADER_ROW + 1
private const val CAST_ROW = CHAPTERS_ROW + 1
private const val CREW_ROW = CAST_ROW + 1
private const val EXTRAS_ROW = CREW_ROW + 1
private const val COLLECTIONS_ROW = EXTRAS_ROW + 1
private const val SIMILAR_ROW = COLLECTIONS_ROW + 1

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
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onNavigate: (NavItem) -> Unit = {},
    showUniversesInNav: Boolean = false,
    showDiscoverInNav: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val movie = state.item ?: return
    val scope = rememberCoroutineScope()

    // Focus management
    var position by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(SIMILAR_ROW + 1) { FocusRequester() } }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Dialog state
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var mediaInfoItem by remember { mutableStateOf<JellyfinItem?>(null) }
    var showOverview by remember { mutableStateOf(false) }
    val descriptionFocusRequester = remember { FocusRequester() }

    val resumePositionTicks = movie.userData?.playbackPositionTicks ?: 0L
    val watched = movie.userData?.played == true
    val favorite = movie.userData?.isFavorite == true

    // Cast & crew (using extension properties from JellyfinItem)
    val cast = movie.actors
    val crew = movie.crew

    // Trailer detection - local trailer from special features or remote YouTube trailer
    val trailerItem = remember(state.specialFeatures) {
        findNewestTrailer(state.specialFeatures)
    }
    val trailerVideo = remember(movie.remoteTrailers) {
        movie.remoteTrailers
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

    // Build categorized feature sections (excludes the trailer shown on play button)
    val featureSections = remember(state.specialFeatures, trailerItem?.id) {
        buildFeatureSections(state.specialFeatures, trailerItem?.id?.let { setOf(it) } ?: emptySet())
    }

    // Backdrop URL and dynamic gradient colors
    val backdropUrl = remember(movie.id) {
        jellyfinClient.getBackdropUrl(movie.id, movie.backdropImageTags?.firstOrNull())
    }
    val gradientColors = rememberGradientColors(backdropUrl)

    val playFocusRequester = remember { FocusRequester() }

    // Focus play button on load
    LaunchedEffect(Unit) {
        delay(100)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus errors
        }
    }

    // Layered UI: DynamicBackground → NavigationRail + Content (DetailBackdropLayer → Content)
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background (covers full screen including nav rail)
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left: Navigation Rail
            NavigationRail(
                selectedItem = NavItem.MOVIES,
                onItemSelected = onNavigate,
                showUniverses = showUniversesInNav,
                showDiscover = showDiscoverInNav,
                contentFocusRequester = focusRequesters[HEADER_ROW],
            )

            // Right: Content area
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // Layer 2: Backdrop image (right side, behind content) - matches home page positioning
            DetailBackdropLayer(
                item = movie,
                jellyfinClient = jellyfinClient,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .align(Alignment.TopEnd),
            )

            // Layer 3: Content - Column with fixed hero + scrollable content (like SeriesDetailScreen)
            Column(modifier = Modifier.fillMaxSize()) {
                // Hero section - doesn't scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester),
                ) {
                    // Hero content (left 50%) - title, rating, genres, description, buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(start = 10.dp, top = 16.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    MovieDetailsHeader(
                        movie = movie,
                        onOverviewClick = { showOverview = true },
                        descriptionFocusRequester = descriptionFocusRequester,
                        downFocusRequester = playFocusRequester,
                    )

                    // Action buttons
                    ExpandablePlayButtons(
                        resumePositionTicks = resumePositionTicks,
                        watched = watched,
                        favorite = favorite,
                        onPlayClick = { resumeTicks ->
                            onPlayClick(resumeTicks / 10_000)
                        },
                        onWatchedClick = onWatchedClick,
                        onFavoriteClick = onFavoriteClick,
                        onTrailerClick = trailerAction,
                        onMoreClick = {
                            dialogParams = DialogParams(
                                title = movie.name,
                                items = listOf(
                                    DialogItem(
                                        text = "Media Info",
                                        icon = Icons.Outlined.Info,
                                        iconTint = IconColors.MediaInfo,
                                        onClick = { mediaInfoItem = movie },
                                    ),
                                ),
                            )
                        },
                        buttonOnFocusChanged = {
                            if (it.isFocused) {
                                position = HEADER_ROW
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                        playButtonFocusRequester = playFocusRequester,
                        modifier = Modifier
                            .focusRequester(focusRequesters[HEADER_ROW])
                            .focusProperties {
                                down = focusRequesters[CHAPTERS_ROW]
                                up = descriptionFocusRequester
                            }
                            .focusRestorer(playFocusRequester)
                            .focusGroup(),
                    )
                }
            }

            // Scrollable content rows (below fixed hero)
            LazyColumn(
                contentPadding = PaddingValues(start = 0.dp, end = 48.dp, top = 0.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[CHAPTERS_ROW]),
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

            // Categorized special features (Trailers, Featurettes, Behind the Scenes, etc.)
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
        } // End Column
            } // End Content Box
        } // End Row
    } // End outer Box

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

    // Overview dialog
    if (showOverview) {
        OverviewDialog(
            title = movie.name,
            overview = movie.overview.orEmpty(),
            genres = movie.genres.orEmpty(),
            onDismiss = { showOverview = false },
        )
    }
}

/**
 * Movie details header matching series detail hero style.
 * Uses full width of parent column (which is already constrained to 50% of screen).
 */
@Composable
private fun MovieDetailsHeader(
    movie: JellyfinItem,
    onOverviewClick: () -> Unit,
    descriptionFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Title - matches series hero style
        Text(
            text = movie.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Quick details: year, runtime, rating, studio - matches hero section style
        MovieHeroRatingRow(
            movie = movie,
            studioName = movie.studios?.firstOrNull()?.name,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Media badges: resolution, codec, HDR/DV, audio
        MediaBadgesRow(item = movie)

        Spacer(modifier = Modifier.height(6.dp))

        // Genres
        if (!movie.genres.isNullOrEmpty()) {
            GenreText(genres = movie.genres!!)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Tagline (italic)
        movie.taglines?.firstOrNull()?.let { tagline ->
            Text(
                text = tagline,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = TvColors.TextPrimary.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Description - 3 lines max, clickable to show full overview
        movie.overview?.let { overview ->
            OverviewText(
                overview = overview,
                maxLines = 3,
                onClick = onOverviewClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .focusRequester(descriptionFocusRequester)
                    .focusProperties {
                        down = downFocusRequester
                        upFocusRequester?.let { up = it }
                    },
                paddingValues = PaddingValues(0.dp),
            )
        }
    }
}

/**
 * Hero-style rating row for movies matching home page hero style.
 * Shows: Year · Runtime · Rating Badge · Star Rating · Critic Rating · Studio Logo
 */
@Composable
private fun MovieHeroRatingRow(
    movie: JellyfinItem,
    studioName: String?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var needsDot = false

        // Production year
        movie.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            needsDot = true
        }

        // Runtime
        movie.runTimeTicks?.let { ticks ->
            val minutes = (ticks / 600_000_000).toInt()
            if (minutes > 0) {
                if (needsDot) DotSeparator()
                RuntimeDisplay(minutes)
                needsDot = true
            }
        }

        // Official rating badge (PG-13, R, etc.)
        movie.officialRating?.let { rating ->
            if (needsDot) DotSeparator()
            RatingBadge(rating)
            needsDot = true
        }

        // Community rating (star rating)
        movie.communityRating?.let { rating ->
            if (needsDot) DotSeparator()
            StarRating(rating)
            needsDot = true
        }

        // Critic rating (Rotten Tomatoes style)
        movie.criticRating?.let { rating ->
            if (needsDot) DotSeparator()
            CriticRatingBadge(rating)
            needsDot = true
        }

        // Studio logo
        if (!studioName.isNullOrBlank()) {
            if (needsDot) DotSeparator()
            StudioLogo(studioName = studioName)
        }
    }
}

/**
 * Small dot separator for metadata rows.
 */
@Composable
private fun DotSeparator() {
    Text(
        text = "•",
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.6f),
    )
}

/**
 * Rating badge with colored background based on content rating.
 */
@Composable
private fun RatingBadge(text: String) {
    val backgroundColor = getRatingColor(text)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = Color.White,
        )
    }
}

/**
 * Get the background color for a content rating.
 */
private fun getRatingColor(rating: String): Color {
    val normalizedRating = rating.uppercase().trim()
    return when {
        // Green - Family friendly
        normalizedRating in listOf("G", "TV-G", "TV-Y", "TV-Y7", "TV-Y7-FV") ->
            Color(0xFF2E7D32) // Green 800

        // Blue - General/Parental guidance
        normalizedRating in listOf("PG", "TV-PG") ->
            Color(0xFF1565C0) // Blue 800

        // Orange - Teen/Caution
        normalizedRating in listOf("PG-13", "TV-14", "16") ->
            Color(0xFFF57C00) // Orange 700

        // Red - Restricted/Mature
        normalizedRating in listOf("R", "TV-MA", "NC-17", "NR", "UNRATED") ->
            Color(0xFFC62828) // Red 800

        // Gray - Default/Unknown
        else -> Color(0xFF616161) // Gray 700
    }
}

/**
 * Star rating display with gold star icon.
 */
@Composable
private fun StarRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFFD700), // Gold color
        )
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Critic rating with Rotten Tomatoes style indicator.
 */
@Composable
private fun CriticRatingBadge(rating: Float) {
    val percentage = rating.roundToInt()
    val isFresh = percentage >= 60

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(
                id = if (isFresh) R.drawable.ic_rotten_tomatoes_fresh else R.drawable.ic_rotten_tomatoes_rotten,
            ),
            contentDescription = if (isFresh) "Fresh" else "Rotten",
            modifier = Modifier.size(16.dp),
            tint = Color.Unspecified,
        )
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Runtime display in hours and minutes format.
 */
@Composable
private fun RuntimeDisplay(minutes: Int) {
    val hours = minutes / 60
    val mins = minutes % 60
    val text = when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.9f),
    )
}

/**
 * Map of movie studio names to embedded drawable resources.
 * Logos sourced from TMDB and Emby.
 * Studios without logos will display a text badge fallback.
 */
private val studioLogoResources = mapOf(
    // Major Hollywood Studios
    "Warner Bros." to R.drawable.studio_warner_bros,
    "Warner Bros" to R.drawable.studio_warner_bros,
    "Warner Bros. Pictures" to R.drawable.studio_warner_bros,
    "Warner Bros. Animation" to R.drawable.studio_warner_bros_animation,
    "Paramount Pictures" to R.drawable.studio_paramount,
    "Paramount" to R.drawable.studio_paramount,
    "Walt Disney Pictures" to R.drawable.studio_disney,
    "Disney" to R.drawable.studio_disney,
    "Walt Disney Animation Studios" to R.drawable.studio_disney_animation,
    "Sony Pictures" to R.drawable.studio_sony,
    "Sony" to R.drawable.studio_sony,
    "Sony Pictures Animation" to R.drawable.studio_sony_animation,
    "Columbia Pictures" to R.drawable.studio_columbia,
    "Columbia" to R.drawable.studio_columbia,
    "Marvel Studios" to R.drawable.studio_marvel,
    "Marvel" to R.drawable.studio_marvel,
    "Pixar" to R.drawable.studio_pixar,
    "Pixar Animation Studios" to R.drawable.studio_pixar,
    "DreamWorks" to R.drawable.studio_dreamworks,
    "DreamWorks Pictures" to R.drawable.studio_dreamworks,
    "DreamWorks Animation" to R.drawable.studio_dreamworks_animation,
    "Lionsgate" to R.drawable.studio_lionsgate,
    "Lions Gate Films" to R.drawable.studio_lionsgate,
    "Legendary Pictures" to R.drawable.studio_legendary,
    "Legendary" to R.drawable.studio_legendary,
    "20th Century Studios" to R.drawable.studio_20th_century,
    "20th Century Fox" to R.drawable.studio_20th_century,
    "Searchlight Pictures" to R.drawable.studio_searchlight,
    "Fox Searchlight Pictures" to R.drawable.studio_searchlight,
    "Touchstone Pictures" to R.drawable.studio_touchstone,
    "Touchstone" to R.drawable.studio_touchstone,

    // DC and Comic Book Studios
    "DC Studios" to R.drawable.studio_dc,
    "DC Films" to R.drawable.studio_dc_films,
    "DC Comics" to R.drawable.studio_dc_comics,
    "DC Entertainment" to R.drawable.studio_dc_entertainment,

    // Boutique/Independent Studios
    "A24" to R.drawable.studio_a24,
    "Blumhouse Productions" to R.drawable.studio_blumhouse,
    "Blumhouse" to R.drawable.studio_blumhouse,
    "Bad Robot" to R.drawable.studio_bad_robot,
    "Bad Robot Productions" to R.drawable.studio_bad_robot,
    "Skydance Media" to R.drawable.studio_skydance,
    "Skydance" to R.drawable.studio_skydance,
    "Plan B Entertainment" to R.drawable.studio_plan_b,
    "Plan B" to R.drawable.studio_plan_b,
    "Focus Features" to R.drawable.studio_focus_features,
    "Regency Enterprises" to R.drawable.studio_regency,
    "Village Roadshow Pictures" to R.drawable.studio_village_roadshow,

    // European Studios
    "StudioCanal" to R.drawable.studio_studiocanal,
    "Canal+" to R.drawable.studio_canal_plus,
    "EuropaCorp" to R.drawable.studio_europacorp,
    "BBC Film" to R.drawable.studio_bbc_film,
    "Film4 Productions" to R.drawable.studio_film4,
    "Film4" to R.drawable.studio_film4,

    // Japanese Studios
    "Studio Ghibli" to R.drawable.studio_ghibli,
    "Toho" to R.drawable.studio_toho,
    "Toho Company" to R.drawable.studio_toho,
    "Toei Animation" to R.drawable.studio_toei_animation,
    "Toei Company" to R.drawable.studio_toei,
    "Aniplex" to R.drawable.studio_aniplex,
    "MAPPA" to R.drawable.studio_mappa,
    "Madhouse" to R.drawable.studio_madhouse,
    "Bones" to R.drawable.studio_bones,
    "Wit Studio" to R.drawable.studio_wit,
    "WIT STUDIO" to R.drawable.studio_wit,
    "ufotable" to R.drawable.studio_ufotable,
    "Ufotable" to R.drawable.studio_ufotable,
    "Sunrise" to R.drawable.studio_sunrise,
    "Kyoto Animation" to R.drawable.studio_kyoto_animation,
    "A-1 Pictures" to R.drawable.studio_a1_pictures,
    "CloverWorks" to R.drawable.studio_cloverworks,
    "Production I.G" to R.drawable.studio_production_ig,
    "TRIGGER" to R.drawable.studio_trigger,
    "Trigger" to R.drawable.studio_trigger,
    "Pierrot" to R.drawable.studio_pierrot,
    "Studio Pierrot" to R.drawable.studio_pierrot,
    "J.C.Staff" to R.drawable.studio_jc_staff,
    "Studio Deen" to R.drawable.studio_deen,
    "Science SARU" to R.drawable.studio_science_saru,
    "David Production" to R.drawable.studio_david_production,
    "Kinema Citrus" to R.drawable.studio_kinema_citrus,
    "White Fox" to R.drawable.studio_white_fox,
    "P.A. Works" to R.drawable.studio_pa_works,
    "P.A.Works" to R.drawable.studio_pa_works,
    "Gainax" to R.drawable.studio_gainax,
    "Studio Khara" to R.drawable.studio_khara,
    "khara" to R.drawable.studio_khara,
    "Studio Chizu" to R.drawable.studio_chizu,
    "Cartoon Saloon" to R.drawable.studio_cartoon_saloon,
    "Aardman Animations" to R.drawable.studio_aardman,
    "Aardman" to R.drawable.studio_aardman,

    // Streaming Studios (reuse network logos)
    "Netflix" to R.drawable.network_netflix,
    "Amazon Studios" to R.drawable.network_amazon,
    "Amazon MGM Studios" to R.drawable.network_amazon_mgm,
    "Apple Studios" to R.drawable.network_apple,
    "Apple Original Films" to R.drawable.network_apple,
    "HBO Films" to R.drawable.network_hbo_films,
    "Max Original" to R.drawable.network_hbo_max,
    "HBO Max" to R.drawable.network_hbo_max,
)

/**
 * Priority list for partial matching studio names.
 * Order matters - first match wins.
 */
private val studioMatchPriority = listOf(
    "Warner Bros" to R.drawable.studio_warner_bros,
    "Paramount" to R.drawable.studio_paramount,
    "Disney" to R.drawable.studio_disney,
    "Sony" to R.drawable.studio_sony,
    "Columbia" to R.drawable.studio_columbia,
    "Marvel" to R.drawable.studio_marvel,
    "Pixar" to R.drawable.studio_pixar,
    "DreamWorks" to R.drawable.studio_dreamworks,
    "Lionsgate" to R.drawable.studio_lionsgate,
    "Legendary" to R.drawable.studio_legendary,
    "20th Century" to R.drawable.studio_20th_century,
    "Searchlight" to R.drawable.studio_searchlight,
    "DC" to R.drawable.studio_dc,
    "A24" to R.drawable.studio_a24,
    "Blumhouse" to R.drawable.studio_blumhouse,
    "Bad Robot" to R.drawable.studio_bad_robot,
    "Ghibli" to R.drawable.studio_ghibli,
    "Toho" to R.drawable.studio_toho,
    "Netflix" to R.drawable.network_netflix,
    "Amazon" to R.drawable.network_amazon,
    "Apple" to R.drawable.network_apple,
)

/**
 * Get embedded drawable resource ID for a studio name.
 */
private fun getStudioLogoResource(name: String): Int? {
    // Try exact match first
    studioLogoResources[name]?.let { return it }
    studioLogoResources[name.trim()]?.let { return it }

    // Try partial match for combined studio names
    val trimmedName = name.trim()
    for ((keyword, resource) in studioMatchPriority) {
        if (trimmedName.contains(keyword, ignoreCase = true)) {
            return resource
        }
    }

    return null
}

/**
 * Inline studio logo for use in rating row.
 * Uses embedded drawable resources for instant display.
 * Falls back to styled badge if no mapping exists.
 */
@Composable
private fun StudioLogo(studioName: String) {
    val logoResource = remember(studioName) {
        getStudioLogoResource(studioName)
    }

    if (logoResource != null) {
        Image(
            painter = painterResource(id = logoResource),
            contentDescription = studioName,
            modifier = Modifier.height(14.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        // Show styled badge with studio name for unmapped studios
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF424242))
                .padding(horizontal = 6.dp),
        ) {
            Text(
                text = studioName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                ),
                color = Color.White,
            )
        }
    }
}
