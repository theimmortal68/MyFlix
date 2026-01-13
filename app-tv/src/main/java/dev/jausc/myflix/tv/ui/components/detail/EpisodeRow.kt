@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.core.common.model.runtimeMinutes
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal row of episode thumbnail cards for TV shows.
 * Used in the single-scrolling detail screen layout.
 */
@Composable
fun EpisodeRow(
    episodes: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onEpisodeClick: (String) -> Unit,
    onEpisodeLongClick: (JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Column(modifier = modifier) {
        // Section header
        Text(
            text = "Episodes",
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading episodes...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }
            episodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No episodes available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                ) {
                    items(episodes, key = { it.id }) { episode ->
                        EpisodeThumbnailCard(
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
}

/**
 * Episode thumbnail card for horizontal display.
 * Shows thumbnail, episode number, title, and progress.
 */
@Composable
private fun EpisodeThumbnailCard(
    episode: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.width(210.dp),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Column {
            // Thumbnail with progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episode.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
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

                // Watched indicator
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
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Episode number and runtime
                val episodeLabel = buildString {
                    append("E${episode.indexNumber ?: "?"}")
                    episode.runtimeMinutes?.let { append(" • ${it}m") }
                }
                Text(
                    text = episodeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TvColors.BluePrimary,
                )

                // Episode title
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
