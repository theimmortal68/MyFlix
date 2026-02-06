@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens.seerr

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrVideo
import dev.jausc.myflix.core.viewmodel.SeerrDetailViewModel
import dev.jausc.myflix.core.common.youtube.TrailerStreamService
import dev.jausc.myflix.tv.ui.components.TvIconButton
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsBackdrop
import dev.jausc.myflix.tv.ui.components.detail.KenBurnsFadePreset
import dev.jausc.myflix.tv.ui.components.detail.TvTabRow
import dev.jausc.myflix.tv.ui.components.detail.TvTabRowFocusConfig
import dev.jausc.myflix.tv.ui.components.seerr.SeerrCastCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrCrewCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrRelatedMediaCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrVideoCard
import dev.jausc.myflix.tv.ui.screens.seerr.components.DetailActionButtons
import dev.jausc.myflix.tv.ui.screens.seerr.components.DetailHeroLayout
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.launch

/**
 * Tab options for the Seerr media detail screen.
 */
private enum class SeerrDetailTab {
    Cast,
    Crew,
    Trailers,
    Teasers,
    Clips,
    Featurettes,
    BehindTheScenes,
    Bloopers,
    Recommendations,
    Similar,
}

/**
 * Seerr media detail screen.
 *
 * Layout:
 * - Full-width backdrop with gradient overlay
 * - Three-column hero: Poster | Content (title, overview) | Info Table
 * - Action buttons row
 * - Tabbed section: Cast, Crew, Recommendations, Similar, Videos
 */
@Composable
fun SeerrMediaDetailScreen(
    mediaType: String,
    tmdbId: Int,
    seerrRepository: SeerrRepository,
    jellyfinServerUrl: String? = null,
    onPlayInJellyfin: ((String) -> Unit)? = null,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    onBack: () -> Unit,
    onActorClick: ((Int) -> Unit)? = null,
    onNavigateGenre: ((mediaType: String, genreId: Int, genreName: String) -> Unit)? = null,
) {
    val viewModel: SeerrDetailViewModel = viewModel(
        factory = SeerrDetailViewModel.Factory(seerrRepository, tmdbId, mediaType),
    )
    val uiState by viewModel.uiState.collectAsState()

    val actionButtonFocusRequester = remember { FocusRequester() }
    val updateExitFocus = rememberExitFocusRegistry(actionButtonFocusRequester)

    val media = uiState.movieDetails ?: uiState.tvDetails
    val crew = media?.credits?.crew?.take(10) ?: emptyList()
    val cast = media?.credits?.cast?.take(20) ?: emptyList()

    // Tab state
    var selectedTab by rememberSaveable { mutableStateOf(SeerrDetailTab.Cast) }
    val tabFocusRequesters = remember { mutableStateMapOf<SeerrDetailTab, FocusRequester>() }
    fun getTabFocusRequester(tab: SeerrDetailTab): FocusRequester =
        tabFocusRequesters.getOrPut(tab) { FocusRequester() }
    var lastFocusedTab by remember { mutableStateOf(SeerrDetailTab.Cast) }

    // Compute YouTube videos and categories
    val youtubeVideos = media?.relatedVideos?.filter { video ->
        video.site?.equals("YouTube", ignoreCase = true) == true
    } ?: emptyList()

    val officialTrailer = youtubeVideos
        .filter { it.type?.equals("Trailer", ignoreCase = true) == true }
        .lastOrNull()

    fun videosForTab(tab: SeerrDetailTab): List<SeerrVideo> {
        val apiType = when (tab) {
            SeerrDetailTab.Trailers -> "Trailer"
            SeerrDetailTab.Teasers -> "Teaser"
            SeerrDetailTab.Clips -> "Clip"
            SeerrDetailTab.Featurettes -> "Featurette"
            SeerrDetailTab.BehindTheScenes -> "Behind the Scenes"
            SeerrDetailTab.Bloopers -> "Blooper"
            else -> return emptyList()
        }
        return youtubeVideos.filter { video ->
            video.type?.equals(apiType, ignoreCase = true) == true &&
                (apiType != "Trailer" || video.key != officialTrailer?.key)
        }
    }

    // Prefetch YouTube stream URLs via ExtrasDownloader for instant playback
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(youtubeVideos, jellyfinServerUrl) {
        if (jellyfinServerUrl != null && youtubeVideos.isNotEmpty()) {
            TrailerStreamService.configure(jellyfinServerUrl)
            // Prefetch primary trailer first for instant hero button playback
            officialTrailer?.key?.let { key ->
                coroutineScope.launch { TrailerStreamService.prefetch(key) }
            }
            // Prefetch all other video stream URLs in background
            youtubeVideos.forEach { video ->
                val key = video.key ?: return@forEach
                if (key != officialTrailer?.key) {
                    coroutineScope.launch { TrailerStreamService.prefetch(key) }
                }
            }
        }
    }

    // Filter tabs based on available data
    val availableTabs = remember(
        cast, crew, uiState.recommendations, uiState.similar, youtubeVideos,
    ) {
        SeerrDetailTab.entries.filter { tab ->
            when (tab) {
                SeerrDetailTab.Cast -> cast.isNotEmpty()
                SeerrDetailTab.Crew -> crew.isNotEmpty()
                SeerrDetailTab.Recommendations -> uiState.recommendations.isNotEmpty()
                SeerrDetailTab.Similar -> uiState.similar.isNotEmpty()
                else -> videosForTab(tab).isNotEmpty()
            }
        }
    }

    // Handle tab selection when tab becomes unavailable
    LaunchedEffect(availableTabs) {
        if (selectedTab !in availableTabs) {
            selectedTab = availableTabs.firstOrNull() ?: SeerrDetailTab.Cast
        }
    }

    // Request focus on action button when content loads
    LaunchedEffect(media) {
        if (media != null) {
            kotlinx.coroutines.delay(100)
            try {
                actionButtonFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    // Handle request (always request all seasons for TV, no 4K)
    fun handleRequest() {
        val currentMedia = media ?: return
        if (currentMedia.isMovie) {
            viewModel.requestMovie(false)
        } else {
            viewModel.requestTv(false, null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            uiState.error != null && media == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error ?: "Failed to load media",
                        color = TvColors.Error,
                    )
                }
            }

            media != null -> {
                val currentMedia = media

                Column(modifier = Modifier.fillMaxSize()) {
                    // Hero section: backdrop + gradient + back button + layout + buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(352.dp),
                    ) {
                        // Ken Burns animated backdrop
                        KenBurnsBackdrop(
                            imageUrl = seerrRepository.getBackdropUrl(currentMedia.backdropPath),
                            fadePreset = KenBurnsFadePreset.DISCOVER_DETAIL,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .fillMaxHeight(0.9f)
                                .align(Alignment.TopEnd),
                        )

                        // Content: hero layout with action buttons
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 16.dp),
                        ) {
                            // Three-column hero layout with buttons in poster column
                            DetailHeroLayout(
                                media = currentMedia,
                                seerrRepository = seerrRepository,
                                ratings = uiState.ratings,
                                tvRatings = uiState.tvRatings,
                                actionButtons = {
                                    Column(
                                        modifier = Modifier.focusProperties {
                                            down = getTabFocusRequester(
                                                availableTabs.firstOrNull() ?: SeerrDetailTab.Cast,
                                            )
                                        },
                                    ) {
                                        DetailActionButtons(
                                            media = currentMedia,
                                            isRequesting = uiState.isRequesting,
                                            onRequest = { handleRequest() },
                                            onTrailerClick = onTrailerClick,
                                            actionButtonFocusRequester = actionButtonFocusRequester,
                                        )

                                        uiState.requestError?.let { error ->
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = error,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TvColors.Error,
                                            )
                                        }
                                    }
                                },
                            )

                                // Tab row directly under buttons
                                if (availableTabs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(32.dp))
                                    TvTabRow(
                                        tabs = availableTabs,
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it },
                                        tabLabel = { tab ->
                                            when (tab) {
                                                SeerrDetailTab.Cast -> "Cast"
                                                SeerrDetailTab.Crew -> "Crew"
                                                SeerrDetailTab.Recommendations -> "Recommendations"
                                                SeerrDetailTab.Similar -> "Similar"
                                                SeerrDetailTab.Trailers -> "Trailers"
                                                SeerrDetailTab.Teasers -> "Teasers"
                                                SeerrDetailTab.Clips -> "Clips"
                                                SeerrDetailTab.Featurettes -> "Featurettes"
                                                SeerrDetailTab.BehindTheScenes -> "Behind the Scenes"
                                                SeerrDetailTab.Bloopers -> "Bloopers"
                                            }
                                        },
                                        getTabFocusRequester = ::getTabFocusRequester,
                                        onTabFocused = { tab, requester ->
                                            lastFocusedTab = tab
                                            updateExitFocus(requester)
                                        },
                                        focusConfig = TvTabRowFocusConfig(
                                            upFocusRequester = actionButtonFocusRequester,
                                        ),
                                        modifier = Modifier.padding(end = 16.dp),
                                    )
                                }
                        }
                    }

                    // Tab content area below the hero
                    if (availableTabs.isNotEmpty()) {
                        val selectedTabRequester = getTabFocusRequester(lastFocusedTab)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(start = 2.dp)
                                .focusProperties {
                                    up = selectedTabRequester
                                },
                            contentAlignment = Alignment.TopStart,
                        ) {
                            when (selectedTab) {
                                SeerrDetailTab.Cast -> {
                                    SeerrCastTabContent(
                                        cast = cast,
                                        seerrRepository = seerrRepository,
                                        onActorClick = onActorClick,
                                        tabFocusRequester = selectedTabRequester,
                                    )
                                }
                                SeerrDetailTab.Crew -> {
                                    SeerrCrewTabContent(
                                        crew = crew,
                                        seerrRepository = seerrRepository,
                                        tabFocusRequester = selectedTabRequester,
                                    )
                                }
                                SeerrDetailTab.Recommendations -> {
                                    SeerrRelatedTabContent(
                                        items = uiState.recommendations,
                                        seerrRepository = seerrRepository,
                                        onMediaClick = onMediaClick,
                                        tabFocusRequester = selectedTabRequester,
                                    )
                                }
                                SeerrDetailTab.Similar -> {
                                    SeerrRelatedTabContent(
                                        items = uiState.similar,
                                        seerrRepository = seerrRepository,
                                        onMediaClick = onMediaClick,
                                        tabFocusRequester = selectedTabRequester,
                                    )
                                }
                                else -> {
                                    SeerrVideosTabContent(
                                        videos = videosForTab(selectedTab),
                                        onTrailerClick = onTrailerClick,
                                        tabFocusRequester = selectedTabRequester,
                                    )
                                }
                            }
                        }
                    }
                }

                // Success toast
                if (uiState.lastRequest != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp)
                            .background(Color(0xFF22C55E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request submitted successfully!",
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cast tab content - horizontal row of cast cards.
 */
@Composable
private fun SeerrCastTabContent(
    cast: List<dev.jausc.myflix.core.seerr.SeerrCastMember>,
    seerrRepository: SeerrRepository,
    onActorClick: ((Int) -> Unit)?,
    tabFocusRequester: FocusRequester? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 4.dp),
    ) {
        items(cast, key = { it.id }) { member ->
            SeerrCastCard(
                member = member,
                seerrRepository = seerrRepository,
                onClick = { onActorClick?.invoke(member.id) },
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) up = tabFocusRequester
                },
            )
        }
    }
}

/**
 * Crew tab content - horizontal row of crew cards.
 */
@Composable
private fun SeerrCrewTabContent(
    crew: List<dev.jausc.myflix.core.seerr.SeerrCrewMember>,
    seerrRepository: SeerrRepository,
    tabFocusRequester: FocusRequester? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 4.dp),
    ) {
        items(crew, key = { "${it.id}_${it.job}" }) { member ->
            SeerrCrewCard(
                member = member,
                seerrRepository = seerrRepository,
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) up = tabFocusRequester
                },
            )
        }
    }
}

/**
 * Related media tab content (Recommendations / Similar) - horizontal row of poster cards.
 */
@Composable
private fun SeerrRelatedTabContent(
    items: List<dev.jausc.myflix.core.seerr.SeerrMedia>,
    seerrRepository: SeerrRepository,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    tabFocusRequester: FocusRequester? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 4.dp),
    ) {
        items(items, key = { it.id }) { item ->
            SeerrRelatedMediaCard(
                media = item,
                seerrRepository = seerrRepository,
                onClick = {
                    val targetType = if (item.mediaType.isNotBlank()) {
                        item.mediaType
                    } else if (item.isMovie) {
                        "movie"
                    } else {
                        "tv"
                    }
                    onMediaClick(targetType, item.tmdbId ?: item.id)
                },
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) up = tabFocusRequester
                },
            )
        }
    }
}

/**
 * Videos tab content - horizontal row of video cards.
 */
@Composable
private fun SeerrVideosTabContent(
    videos: List<SeerrVideo>,
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    tabFocusRequester: FocusRequester? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 4.dp),
    ) {
        items(videos, key = { it.key ?: it.name ?: "" }) { video ->
            SeerrVideoCard(
                video = video,
                onClick = { key, name -> onTrailerClick(key, name) },
                modifier = Modifier.focusProperties {
                    if (tabFocusRequester != null) up = tabFocusRequester
                },
            )
        }
    }
}
