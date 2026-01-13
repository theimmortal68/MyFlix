@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.core.common.model.runtimeMinutes
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.screens.DetailUiState
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Episodes tab content for TV series detail screen.
 * Shows season selector and episode list.
 */
@Composable
fun EpisodesTabContent(
    state: DetailUiState,
    jellyfinClient: JellyfinClient,
    onSeasonSelected: (JellyfinItem) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onEpisodeLongClick: (JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester = FocusRequester(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 16.dp),
    ) {
        // Season selector
        if (state.seasons.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.focusRequester(contentFocusRequester),
            ) {
                items(state.seasons, key = { it.id }) { season ->
                    val isSelected = state.selectedSeason?.id == season.id
                    SeasonChip(
                        season = season,
                        isSelected = isSelected,
                        onClick = { onSeasonSelected(season) },
                    )
                }
            }
        }

        // Loading indicator
        if (state.isLoadingEpisodes) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading episodes...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                )
            }
        } else if (state.episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No episodes available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                )
            }
        } else {
            // Episode list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(state.episodes, key = { it.id }) { episode ->
                    EpisodeCard(
                        episode = episode,
                        imageUrl = jellyfinClient.getPrimaryImageUrl(
                            episode.id,
                            episode.imageTags?.primary,
                        ),
                        onClick = { onEpisodeClick(episode.id) },
                        onLongClick = { onEpisodeLongClick(episode) },
                    )
                }
            }
        }
    }
}

/**
 * Season selector chip.
 */
@Composable
private fun SeasonChip(
    season: JellyfinItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) TvColors.BluePrimary else TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Text(
            text = season.name,
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * Episode card with thumbnail, number, title, runtime, and progress.
 */
@Composable
private fun EpisodeCard(
    episode: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Thumbnail with progress bar
            Box(
                modifier = Modifier
                    .width(210.dp)
                    .aspectRatio(16f / 9f),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episode.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )

                // Progress bar overlay
                val progress = episode.progressPercent
                if (progress > 0f && progress < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomStart)
                            .background(TvColors.Surface.copy(alpha = 0.5f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(TvColors.BluePrimary),
                        )
                    }
                }

                // Watched indicator (checkmark overlay)
                if (episode.userData?.played == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvColors.Success.copy(alpha = 0.9f)),
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = TvColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Episode info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Episode number
                Text(
                    text = "Episode ${episode.indexNumber ?: ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TvColors.BluePrimary,
                )

                // Episode title
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Runtime
                episode.runtimeMinutes?.let { runtime ->
                    Text(
                        text = "$runtime min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                    )
                }

                // Overview
                episode.overview?.let { overview ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
