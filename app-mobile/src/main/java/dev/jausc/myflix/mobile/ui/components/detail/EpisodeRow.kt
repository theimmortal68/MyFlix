@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.core.common.model.runtimeMinutes
import dev.jausc.myflix.core.network.JellyfinClient

/**
 * Horizontal row of episode thumbnail cards for TV shows.
 */
@Composable
fun MobileEpisodeRow(
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
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            episodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No episodes available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(episodes, key = { it.id }) { episode ->
                        MobileEpisodeThumbnailCard(
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
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MobileEpisodeThumbnailCard(
    episode: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
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
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary),
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
                            .background(Color(0xFF22C55E).copy(alpha = 0.9f)),
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
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
                    color = MaterialTheme.colorScheme.primary,
                )

                // Episode title
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
