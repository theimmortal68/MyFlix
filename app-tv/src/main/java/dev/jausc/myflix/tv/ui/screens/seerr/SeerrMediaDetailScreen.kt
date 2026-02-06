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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.viewmodel.SeerrDetailViewModel
import dev.jausc.myflix.tv.ui.components.TvIconButton
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.seerr.SeerrCastCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrCrewCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrRelatedMediaCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrVideoCard
import dev.jausc.myflix.tv.ui.screens.seerr.components.DetailActionButtons
import dev.jausc.myflix.tv.ui.screens.seerr.components.DetailHeroLayout
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry

/**
 * Seerr media detail screen ported from SeerrTV reference.
 *
 * Layout:
 * - Full-width backdrop with gradient overlay
 * - Three-column hero: Poster | Content (title, ratings, genres, overview) | Info Table
 * - Action buttons below poster
 * - Horizontal carousels: Cast, Crew, Recommendations, Similar, Videos
 */
@Composable
fun SeerrMediaDetailScreen(
    mediaType: String,
    tmdbId: Int,
    seerrRepository: SeerrRepository,
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // Hero section: backdrop + gradient + back button + 3-column layout + buttons
                    item(key = "hero") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(520.dp),
                        ) {
                            // Backdrop image
                            AsyncImage(
                                model = seerrRepository.getBackdropUrl(currentMedia.backdropPath),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                contentScale = ContentScale.Crop,
                            )

                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                TvColors.Background.copy(alpha = 0.3f),
                                                TvColors.Background.copy(alpha = 0.7f),
                                                TvColors.Background,
                                            ),
                                        ),
                                    ),
                            )

                            // Back button
                            TvIconButton(
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                onClick = onBack,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 48.dp, top = 24.dp),
                            )

                            // Content: hero layout + action buttons
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 72.dp),
                            ) {
                                // Three-column hero
                                DetailHeroLayout(
                                    media = currentMedia,
                                    seerrRepository = seerrRepository,
                                    ratings = uiState.ratings,
                                    tvRatings = uiState.tvRatings,
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Action buttons (horizontal, below hero)
                                DetailActionButtons(
                                    media = currentMedia,
                                    isRequesting = uiState.isRequesting,
                                    onRequest = { handleRequest() },
                                    onTrailerClick = onTrailerClick,
                                    actionButtonFocusRequester = actionButtonFocusRequester,
                                    modifier = Modifier.padding(horizontal = 48.dp),
                                )

                                // Request error
                                uiState.requestError?.let { error ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TvColors.Error,
                                        modifier = Modifier.padding(horizontal = 48.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Cast carousel
                    currentMedia.credits?.cast?.let { cast ->
                        if (cast.isNotEmpty()) {
                            item(key = "cast") {
                                CarouselSection(title = "Cast") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 48.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(cast.take(20)) { member ->
                                            SeerrCastCard(
                                                member = member,
                                                seerrRepository = seerrRepository,
                                                onClick = {
                                                    onActorClick?.invoke(member.id)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Crew carousel
                    if (crew.isNotEmpty()) {
                        item(key = "crew") {
                            CarouselSection(title = "Crew") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(crew) { member ->
                                        SeerrCrewCard(
                                            member = member,
                                            seerrRepository = seerrRepository,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recommendations carousel
                    if (uiState.recommendations.isNotEmpty()) {
                        item(key = "recommendations") {
                            CarouselSection(title = "Recommendations") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(
                                        uiState.recommendations,
                                        key = { it.id },
                                    ) { item ->
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
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Similar carousel
                    if (uiState.similar.isNotEmpty()) {
                        item(key = "similar") {
                            CarouselSection(title = "Similar") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(uiState.similar, key = { it.id }) { item ->
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
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Videos by category
                    val youtubeVideos = currentMedia.relatedVideos?.filter { video ->
                        video.site?.equals("YouTube", ignoreCase = true) == true
                    } ?: emptyList()

                    val officialTrailer = youtubeVideos
                        .filter { it.type?.equals("Trailer", ignoreCase = true) == true }
                        .lastOrNull()

                    val videoCategories = listOf(
                        "Trailer" to "Trailers",
                        "Teaser" to "Teasers",
                        "Clip" to "Clips",
                        "Featurette" to "Featurettes",
                        "Behind the Scenes" to "Behind the Scenes",
                        "Blooper" to "Bloopers",
                    )

                    videoCategories.forEach { (apiType, displayTitle) ->
                        val videosInCategory = youtubeVideos.filter { video ->
                            video.type?.equals(apiType, ignoreCase = true) == true &&
                                (apiType != "Trailer" || video.key != officialTrailer?.key)
                        }

                        if (videosInCategory.isNotEmpty()) {
                            item(key = "videos_$apiType") {
                                CarouselSection(title = displayTitle) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 48.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(
                                            videosInCategory,
                                            key = { it.key ?: it.name ?: "" },
                                        ) { video ->
                                            SeerrVideoCard(
                                                video = video,
                                                onClick = { key, name ->
                                                    onTrailerClick(key, name)
                                                },
                                            )
                                        }
                                    }
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
 * Section wrapper for carousels with a title.
 */
@Composable
private fun CarouselSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
