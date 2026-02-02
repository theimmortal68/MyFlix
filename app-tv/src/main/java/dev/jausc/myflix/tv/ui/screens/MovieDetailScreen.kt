@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.crew
import dev.jausc.myflix.core.common.ui.getStudioLogoResource
import dev.jausc.myflix.core.common.util.buildFeatureSections
import dev.jausc.myflix.core.common.util.extractYouTubeVideoKey
import dev.jausc.myflix.core.common.util.findNewestTrailer
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.viewmodel.DetailUiState
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.components.detail.CastCrewSection
import dev.jausc.myflix.tv.ui.components.detail.ChaptersRow
import dev.jausc.myflix.tv.ui.components.detail.DotSeparator
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButtons
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsBackdrop
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsFadePreset
import dev.jausc.myflix.tv.ui.components.detail.MediaBadgesRow
import dev.jausc.myflix.tv.ui.components.detail.RatingBadge
import dev.jausc.myflix.tv.ui.components.detail.RottenTomatoesRating
import dev.jausc.myflix.tv.ui.components.detail.StarRating
import dev.jausc.myflix.tv.ui.components.detail.TvTabRow
import dev.jausc.myflix.tv.ui.components.detail.TvTabRowFocusConfig
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Tab options for the movie detail screen.
 */
private enum class MovieTab {
    Details,
    Chapters,
    CastCrew,
    MediaInfo,
    Extras,
    Collections,
    Similar,
}

/**
 * Movie detail screen with Ken Burns animated backdrop and tab-based content.
 * Matches the design patterns from UnifiedSeriesScreen and EpisodesScreen.
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
    leftEdgeFocusRequester: FocusRequester? = null,
) {
    val movie = state.item ?: return

    // Focus requesters for NavRail restoration
    val playButtonFocusRequester = remember { FocusRequester() }

    // Item-level focus tracking for NavRail exit restoration
    val updateExitFocus = rememberExitFocusRegistry(playButtonFocusRequester)

    // Tab state
    var selectedTab by rememberSaveable { mutableStateOf(MovieTab.Details) }

    // Stable focus requesters for each tab (keyed by enum for stability across recomposition)
    val tabFocusRequesters = remember { mutableStateMapOf<MovieTab, FocusRequester>() }
    fun getTabFocusRequester(tab: MovieTab): FocusRequester =
        tabFocusRequesters.getOrPut(tab) { FocusRequester() }

    // Track which tab should receive focus when navigating up from content
    var lastFocusedTab by remember { mutableStateOf(MovieTab.Details) }

    val resumePositionTicks = movie.userData?.playbackPositionTicks ?: 0L
    val watched = movie.userData?.played == true
    val favorite = movie.userData?.isFavorite == true

    // Cast & crew
    val cast = movie.actors
    val crew = movie.crew

    // Trailer detection
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
            if (key.isBlank()) null else { { onTrailerClick(key, trailerVideo.name) } }
        }
        else -> null
    }

    // Build categorized feature sections
    val featureSections = remember(state.specialFeatures, trailerItem?.id) {
        buildFeatureSections(state.specialFeatures, trailerItem?.id?.let { setOf(it) } ?: emptySet())
    }
    val hasExtras = featureSections.isNotEmpty()

    // Filter tabs based on available data
    val availableTabs = remember(movie.chapters, cast, crew, featureSections, state.collections, state.similarItems) {
        MovieTab.entries.filter { tab ->
            when (tab) {
                MovieTab.Details -> true
                MovieTab.Chapters -> !movie.chapters.isNullOrEmpty()
                MovieTab.CastCrew -> cast.isNotEmpty() || crew.isNotEmpty()
                MovieTab.MediaInfo -> true
                MovieTab.Extras -> hasExtras
                MovieTab.Collections -> state.collections.isNotEmpty()
                MovieTab.Similar -> state.similarItems.isNotEmpty()
            }
        }
    }

    // Handle tab selection when tab becomes unavailable
    LaunchedEffect(availableTabs) {
        if (selectedTab !in availableTabs) {
            selectedTab = availableTabs.firstOrNull() ?: MovieTab.Details
        }
    }

    // Backdrop URL
    val backdropUrl = remember(movie.id) {
        jellyfinClient.getBackdropUrl(movie.id, movie.backdropImageTags?.firstOrNull(), maxWidth = 1920)
    }

    // Request initial focus on play button
    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .focusGroup(),
    ) {
        // Layer 1: Ken Burns animated backdrop (top-right)
        KenBurnsBackdrop(
            imageUrl = backdropUrl,
            fadePreset = KenBurnsFadePreset.MOVIE,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.9f)
                .align(Alignment.TopEnd),
        )

        // Layer 2: Content column with hero at top, tabs at bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            // Hero content (left side)
            MovieHeroContent(
                movie = movie,
                resumePositionTicks = resumePositionTicks,
                watched = watched,
                favorite = favorite,
                onPlayClick = { resumeTicks ->
                    onPlayClick(resumeTicks / 10_000)
                },
                onWatchedClick = onWatchedClick,
                onFavoriteClick = onFavoriteClick,
                onTrailerClick = trailerAction,
                playButtonFocusRequester = playButtonFocusRequester,
                firstTabFocusRequester = getTabFocusRequester(selectedTab),
                leftEdgeFocusRequester = leftEdgeFocusRequester,
                updateExitFocus = updateExitFocus,
                modifier = Modifier.fillMaxWidth(0.5f),
            )

            // Spacer pushes tabs to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Tab section with shaded background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 10.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.9f),
                            ),
                        ),
                    )
                    .padding(top = 12.dp),
            ) {
                Column {
                    // Tab row
                    TvTabRow(
                        tabs = availableTabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        tabLabel = { tab ->
                            when (tab) {
                                MovieTab.Details -> "Details"
                                MovieTab.Chapters -> "Chapters"
                                MovieTab.CastCrew -> "Cast & Crew"
                                MovieTab.MediaInfo -> "Media Info"
                                MovieTab.Extras -> "Extras"
                                MovieTab.Collections -> "Collections"
                                MovieTab.Similar -> "Similar"
                            }
                        },
                        getTabFocusRequester = ::getTabFocusRequester,
                        onTabFocused = { tab, requester ->
                            lastFocusedTab = tab
                            updateExitFocus(requester)
                        },
                        focusConfig = TvTabRowFocusConfig(
                            upFocusRequester = playButtonFocusRequester,
                            leftEdgeFocusRequester = leftEdgeFocusRequester,
                        ),
                    )

                    // Tab content area - navigates up to last focused tab
                    // Height of 220dp accommodates MediaCards with labels (165dp poster + 55dp title+year)
                    val selectedTabRequester = getTabFocusRequester(lastFocusedTab)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(start = 2.dp)
                            .focusProperties {
                                up = selectedTabRequester
                            },
                    ) {
                        when (selectedTab) {
                            MovieTab.Details -> {
                                MovieDetailsTabContent(
                                    movie = movie,
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            MovieTab.Chapters -> {
                                ChaptersTabContent(
                                    movie = movie,
                                    jellyfinClient = jellyfinClient,
                                    onChapterClick = { positionMs -> onPlayClick(positionMs) },
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            MovieTab.CastCrew -> {
                                MovieCastCrewTabContent(
                                    cast = cast,
                                    crew = crew,
                                    jellyfinClient = jellyfinClient,
                                    onPersonClick = onNavigateToPerson,
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            MovieTab.MediaInfo -> {
                                MediaInfoTabContent(
                                    movie = movie,
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            MovieTab.Extras -> {
                                ExtrasTabContent(
                                    featureSections = featureSections,
                                    jellyfinClient = jellyfinClient,
                                    onExtraClick = { item -> onPlayItemClick(item.id, null) },
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            MovieTab.Collections -> {
                                CollectionsTabContent(
                                    collections = state.collections,
                                    collectionItems = state.collectionItems,
                                    jellyfinClient = jellyfinClient,
                                    onItemClick = onNavigateToDetail,
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                            MovieTab.Similar -> {
                                SimilarTabContent(
                                    similarItems = state.similarItems,
                                    jellyfinClient = jellyfinClient,
                                    onItemClick = onNavigateToDetail,
                                    tabFocusRequester = selectedTabRequester,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

/**
 * Hero content showing movie title, metadata, overview, and action buttons.
 */
@Composable
private fun MovieHeroContent(
    movie: JellyfinItem,
    resumePositionTicks: Long,
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: (Long) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onTrailerClick: (() -> Unit)?,
    playButtonFocusRequester: FocusRequester,
    firstTabFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    leftEdgeFocusRequester: FocusRequester? = null,
    updateExitFocus: (FocusRequester) -> Unit = {},
) {
    Column(
        modifier = modifier.padding(start = 10.dp),
    ) {
        // Title
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

        // Metadata row: Year · Runtime · Rating · Stars · RT · Studio
        MovieHeroRatingRow(
            movie = movie,
            studioName = movie.studios?.firstOrNull()?.name,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Media badges
        MediaBadgesRow(item = movie)

        Spacer(modifier = Modifier.height(6.dp))

        // Tagline
        movie.taglines?.firstOrNull()?.let { tagline ->
            Text(
                text = tagline,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = TvColors.TextPrimary.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Overview - full text with auto-scroll if it overflows
        movie.overview?.let { overview ->
            AutoScrollingText(
                text = overview,
                modifier = Modifier.fillMaxWidth(0.85f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        ExpandablePlayButtons(
            resumePositionTicks = resumePositionTicks,
            watched = watched,
            favorite = favorite,
            onPlayClick = onPlayClick,
            onWatchedClick = onWatchedClick,
            onFavoriteClick = onFavoriteClick,
            onTrailerClick = onTrailerClick,
            buttonOnFocusChanged = { focusState ->
                if (focusState.isFocused) {
                    updateExitFocus(playButtonFocusRequester)
                }
            },
            playButtonFocusRequester = playButtonFocusRequester,
            leftEdgeFocusRequester = leftEdgeFocusRequester,
            downFocusRequester = firstTabFocusRequester,
            modifier = Modifier.focusRequester(playButtonFocusRequester),
        )
    }
}

/**
 * Hero-style rating row for movies.
 * Shows: Year · Runtime · Rating Badge · Star Rating · Critic Rating · Studio Logo
 */
@Composable
private fun MovieHeroRatingRow(movie: JellyfinItem, studioName: String?) {
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
            StudioBadge(studioName = studioName)
        }
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
 * Studio logo or text badge for rating row.
 */
@Composable
private fun StudioBadge(studioName: String) {
    val logoRes = getStudioLogoResource(studioName)

    if (logoRes != null) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = studioName,
            modifier = Modifier.height(14.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
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

// =============================================================================
// Tab Content Composables
// =============================================================================

/**
 * Details tab - shows full synopsis and production info.
 */
@Composable
private fun MovieDetailsTabContent(
    movie: JellyfinItem,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        // Column 1: Tagline first, then basic info
        item("column1") {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester }
                    .focusable(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    movie.taglines?.firstOrNull()?.let { tagline ->
                        DetailItem("Tagline", "\"$tagline\"")
                    }
                    DetailItem("Genres", movie.genres?.joinToString(", ") ?: "—")
                    DetailItem("Released", movie.premiereDate?.let { formatDate(it) } ?: "—")
                    DetailItem(
                        "Runtime",
                        movie.runTimeTicks?.let {
                            val mins = (it / 600_000_000).toInt()
                            "${mins / 60}h ${mins % 60}m"
                        } ?: "—",
                    )
                }
            }
        }
        // Column 2: Studio/production
        item("column2") {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester }
                    .focusable(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailItem("Studio", movie.studios?.firstOrNull()?.name ?: "—")
                    DetailItem("Director", movie.crew.find { it.type == "Director" }?.name ?: "—")
                    DetailItem("Writer", movie.crew.find { it.type == "Writer" }?.name ?: "—")
                }
            }
        }
        // Column 3: Ratings including RT score with icon
        item("column3") {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester }
                    .focusable(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailItem("Rating", movie.officialRating ?: "—")
                    DetailItem(
                        "TMDb Score",
                        movie.communityRating?.let { "★ ${"%.1f".format(Locale.US, it)}" } ?: "—",
                    )
                    movie.criticRating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "RT",
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextSecondary,
                            )
                            RottenTomatoesRating(percentage = rating.roundToInt())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chapters tab - horizontal row of chapter thumbnails.
 */
@Composable
private fun ChaptersTabContent(
    movie: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onChapterClick: (Long) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val chapters = movie.chapters ?: return

    ChaptersRow(
        chapters = chapters,
        itemId = movie.id,
        getChapterImageUrl = { index -> jellyfinClient.getChapterImageUrl(movie.id, index) },
        onChapterClick = onChapterClick,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester },
    )
}

/**
 * Cast & Crew tab - separate rows for cast and crew.
 */
@Composable
private fun MovieCastCrewTabContent(
    cast: List<dev.jausc.myflix.core.common.model.JellyfinPerson>,
    crew: List<dev.jausc.myflix.core.common.model.JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (String) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (cast.isNotEmpty()) {
            CastCrewSection(
                title = "Cast",
                showTitle = false,
                people = cast,
                jellyfinClient = jellyfinClient,
                onPersonClick = { person -> onPersonClick(person.id) },
                onPersonLongClick = { _, _ -> },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester },
            )
        }
        if (crew.isNotEmpty() && cast.isEmpty()) {
            CastCrewSection(
                title = "Crew",
                showTitle = false,
                people = crew,
                jellyfinClient = jellyfinClient,
                onPersonClick = { person -> onPersonClick(person.id) },
                onPersonLongClick = { _, _ -> },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester },
            )
        }
    }
}

/**
 * Media Info tab - shows video/audio stream details.
 * Format matches EpisodesScreen MediaInfo: Video, Audio (tracks list), Subtitles (list), File.
 */
@Composable
private fun MediaInfoTabContent(
    movie: JellyfinItem,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val mediaSource = movie.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams ?: emptyList()
    val videoStreams = mediaStreams.filter { it.type == "Video" }
    val audioStreams = mediaStreams.filter { it.type == "Audio" }
    val subtitleStreams = mediaStreams.filter { it.type == "Subtitle" }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        // Video info
        if (videoStreams.isNotEmpty()) {
            item("video") {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester }
                        .focusable(),
                ) {
                    Column {
                        Text(
                            text = "Video",
                            style = MaterialTheme.typography.titleSmall,
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        videoStreams.firstOrNull()?.let { stream ->
                            stream.codec?.let { MediaInfoDetailItem("Codec", it.uppercase()) }
                            if (stream.width != null && stream.height != null) {
                                MediaInfoDetailItem("Resolution", "${stream.width}x${stream.height}")
                            }
                            stream.aspectRatio?.let { MediaInfoDetailItem("Aspect", it) }
                            // Dynamic Range (HDR info)
                            val dynamicRange = getDynamicRangeLabel(stream)
                            if (dynamicRange != null) {
                                MediaInfoDetailItem("Dynamic Range", dynamicRange)
                            }
                            stream.bitRate?.let {
                                MediaInfoDetailItem("Bitrate", "${it / 1_000_000} Mbps")
                            }
                        }
                    }
                }
            }
        }

        // Audio info - list format with full codec info
        if (audioStreams.isNotEmpty()) {
            item("audio") {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester }
                        .focusable(),
                ) {
                    Column {
                        Text(
                            text = "Audio (${audioStreams.size} tracks)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        audioStreams.take(4).forEach { stream ->
                            val info = buildString {
                                append(stream.language?.uppercase() ?: "UND")
                                append(" - ")
                                append(getFullAudioCodecLabel(stream))
                                stream.channels?.let { append(" ${it}ch") }
                            }
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextSecondary,
                            )
                        }
                        if (audioStreams.size > 4) {
                            Text(
                                text = "+${audioStreams.size - 4} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextSecondary.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        // Subtitles info - compact list format
        if (subtitleStreams.isNotEmpty()) {
            item("subtitles") {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester }
                        .focusable(),
                ) {
                    Column {
                        Text(
                            text = "Subtitles (${subtitleStreams.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        subtitleStreams.take(6).forEach { stream ->
                            Text(
                                text = stream.displayTitle ?: stream.language?.uppercase() ?: "Unknown",
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (subtitleStreams.size > 6) {
                            Text(
                                text = "+${subtitleStreams.size - 6} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextSecondary.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        // File info
        item("file") {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester }
                    .focusable(),
            ) {
                Column {
                    Text(
                        text = "File",
                        style = MaterialTheme.typography.titleSmall,
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaInfoDetailItem("Container", mediaSource?.container?.uppercase() ?: "—")
                    mediaSource?.size?.let { size ->
                        MediaInfoDetailItem("Size", formatFileSize(size))
                    }
                }
            }
        }
    }
}

/**
 * Detail item for Media Info tab with label: value format.
 */
@Composable
private fun MediaInfoDetailItem(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Extras tab - trailers, featurettes, behind the scenes.
 * Uses chapter-card-like layout: thumbnail gets blue border focus, title below.
 */
@Composable
private fun ExtrasTabContent(
    featureSections: List<dev.jausc.myflix.core.common.util.FeatureSection>,
    jellyfinClient: JellyfinClient,
    onExtraClick: (JellyfinItem) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    // Flatten all sections into a single row
    val allExtras = featureSections.flatMap { it.items }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(allExtras, key = { _, item -> item.id }) { _, item ->
            // Try primary image first (more likely to exist), then thumb, then backdrop
            val imageTag = item.imageTags?.primary
                ?: item.imageTags?.thumb
                ?: item.backdropImageTags?.firstOrNull()
            val imageUrl = when {
                item.imageTags?.primary != null -> jellyfinClient.getPrimaryImageUrl(item.id, imageTag)
                item.imageTags?.thumb != null -> jellyfinClient.getThumbUrl(item.id, imageTag)
                else -> jellyfinClient.getBackdropUrl(item.id, imageTag, maxWidth = 400)
            }
            ExtraCard(
                item = item,
                imageUrl = imageUrl,
                onClick = { onExtraClick(item) },
                tabFocusRequester = tabFocusRequester,
            )
        }
    }
}

/**
 * Individual extra card with chapter-like layout.
 * Thumbnail gets blue border focus, title displayed below.
 */
@Composable
private fun ExtraCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    tabFocusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(210.dp)
            .focusProperties { if (tabFocusRequester != null) up = tabFocusRequester },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Thumbnail with focus border
        androidx.tv.material3.Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = TvColors.BluePrimary,
                                    shape = RoundedCornerShape(8.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }

        // Title below thumbnail
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/**
 * Collections tab - movies in same collection(s).
 */
@Composable
private fun CollectionsTabContent(
    collections: List<JellyfinItem>,
    collectionItems: Map<String, List<JellyfinItem>>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    // Flatten all collection items into a single row
    val allItems = collections.flatMap { collection ->
        collectionItems[collection.id].orEmpty()
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(allItems, key = { _, item -> item.id }) { _, item ->
            MediaCard(
                item = item,
                imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                onClick = { onItemClick(item.id) },
                showLabel = true,
                onLongClick = { },
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) up = tabFocusRequester
                },
            )
        }
    }
}

/**
 * Similar tab - movies similar to this one.
 */
@Composable
private fun SimilarTabContent(
    similarItems: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    tabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(similarItems, key = { _, item -> item.id }) { _, item ->
            MediaCard(
                item = item,
                imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                onClick = { onItemClick(item.id) },
                showLabel = true,
                onLongClick = { },
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) up = tabFocusRequester
                },
            )
        }
    }
}

// =============================================================================
// Utility Composables
// =============================================================================

/**
 * Single detail item with label and value.
 */
@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TvColors.TextSecondary,
            fontSize = 10.sp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Format a date string for display.
 */
private fun formatDate(dateString: String): String {
    return try {
        val instant = java.time.Instant.parse(dateString)
        val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
            .format(localDate)
    } catch (_: Exception) {
        dateString.take(10)
    }
}

/**
 * Format file size in human-readable format.
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "${"%.1f".format(Locale.US, bytes / 1_000_000_000.0)} GB"
        bytes >= 1_000_000 -> "${"%.0f".format(Locale.US, bytes / 1_000_000.0)} MB"
        else -> "${"%.0f".format(Locale.US, bytes / 1_000.0)} KB"
    }
}

/**
 * Get dynamic range label for video stream (HDR, HDR10, HDR10+, Dolby Vision, etc.)
 */
private fun getDynamicRangeLabel(stream: dev.jausc.myflix.core.common.model.MediaStream): String? {
    // Check for Dolby Vision first (highest priority)
    val videoDoViTitle = stream.videoDoViTitle
    if (!videoDoViTitle.isNullOrBlank()) {
        return "Dolby Vision"
    }

    // Check videoRangeType for specific HDR formats
    val rangeType = stream.videoRangeType?.uppercase()
    if (rangeType != null) {
        return when {
            rangeType.contains("DOVI") || rangeType.contains("DOLBY") -> "Dolby Vision"
            rangeType.contains("HDR10+") || rangeType.contains("HDR10PLUS") -> "HDR10+"
            rangeType.contains("HDR10") -> "HDR10"
            rangeType.contains("HLG") -> "HLG"
            rangeType.contains("HDR") -> "HDR"
            rangeType == "SDR" -> null // Don't show SDR
            else -> null
        }
    }

    // Fall back to videoRange
    val videoRange = stream.videoRange?.uppercase()
    if (videoRange != null && videoRange != "SDR") {
        return videoRange
    }

    return null
}

/**
 * Get full audio codec label including profile (DTS-HD MA, TrueHD Atmos, etc.)
 */
private fun getFullAudioCodecLabel(stream: dev.jausc.myflix.core.common.model.MediaStream): String {
    val codec = stream.codec?.uppercase() ?: return "Unknown"
    val profile = stream.profile?.uppercase()

    // Build full codec string based on codec and profile
    return when {
        // DTS variants
        codec == "DTS" && profile != null -> when {
            profile.contains("MA") || profile.contains("HD MA") -> "DTS-HD MA"
            profile.contains("HD") -> "DTS-HD"
            profile.contains("X") -> "DTS:X"
            else -> "DTS"
        }
        codec.contains("DTS") -> codec

        // TrueHD / Atmos
        codec == "TRUEHD" && profile?.contains("ATMOS") == true -> "TrueHD Atmos"
        codec == "TRUEHD" -> "TrueHD"

        // EAC3 / Atmos
        codec == "EAC3" && profile?.contains("ATMOS") == true -> "EAC3 Atmos"
        codec == "EAC3" -> "EAC3"

        // AC3
        codec == "AC3" -> "AC3"

        // AAC variants
        codec == "AAC" && profile != null -> "AAC $profile"
        codec == "AAC" -> "AAC"

        // FLAC, PCM, etc.
        else -> codec
    }
}

/**
 * Text that dynamically expands to fit content, with auto-scroll when overflow is detected.
 * Expands up to maxHeight, then scrolls if content is taller.
 */
@Composable
private fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 120,
    scrollDuration: Int = 12000,
) {
    val scrollState = rememberScrollState()
    var needsScroll by remember { mutableStateOf(false) }
    var textHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val maxHeightPx = with(density) { maxHeight.dp.toPx() }

    // Auto-scroll animation when content overflows
    LaunchedEffect(needsScroll) {
        if (needsScroll && scrollState.maxValue > 0) {
            while (true) {
                // Pause at top
                kotlinx.coroutines.delay(3000)
                // Scroll to bottom
                scrollState.animateScrollTo(
                    scrollState.maxValue,
                    animationSpec = tween(durationMillis = scrollDuration, easing = LinearEasing),
                )
                // Pause at bottom
                kotlinx.coroutines.delay(2000)
                // Scroll back to top
                scrollState.animateScrollTo(
                    0,
                    animationSpec = tween(durationMillis = scrollDuration, easing = LinearEasing),
                )
            }
        }
    }

    // Dynamic height: wraps content up to maxHeight, then clips and scrolls
    val dynamicHeight = with(density) {
        if (textHeight > 0 && textHeight <= maxHeightPx.toInt()) {
            textHeight.toDp()
        } else {
            maxHeight.dp
        }
    }

    Box(
        modifier = modifier
            .height(dynamicHeight)
            .clipToBounds(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary.copy(alpha = 0.9f),
            lineHeight = 18.sp,
            modifier = Modifier
                .verticalScroll(scrollState)
                .onSizeChanged { size ->
                    textHeight = size.height
                    needsScroll = size.height > maxHeightPx
                },
        )
    }
}
