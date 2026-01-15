@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.WideMediaCard
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal row of episode cards for series detail screen.
 * Displays episodes for the currently selected season.
 *
 * @param episodes List of episode items
 * @param jellyfinClient Client for building image URLs
 * @param onEpisodeClick Callback when an episode is clicked
 * @param onEpisodeLongClick Callback when an episode is long-pressed
 * @param isLoading Whether episodes are currently loading
 * @param modifier Modifier for the row
 * @param firstEpisodeFocusRequester Optional focus requester for the first episode
 */
@Composable
fun EpisodeGrid(
    episodes: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onEpisodeClick: (JellyfinItem) -> Unit,
    onEpisodeLongClick: (JellyfinItem) -> Unit,
    onEpisodeFocused: (JellyfinItem) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    firstEpisodeFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(TvColors.BluePrimary, shape = MaterialTheme.shapes.small),
            )
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
            if (episodes.isNotEmpty()) {
                Text(
                    text = "(${episodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TvColors.TextSecondary,
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(32.dp))
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
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextSecondary,
                    )
                }
            }

            else -> {
                EpisodeRow(
                    episodes = episodes,
                    jellyfinClient = jellyfinClient,
                    onEpisodeClick = onEpisodeClick,
                    onEpisodeLongClick = onEpisodeLongClick,
                    onEpisodeFocused = onEpisodeFocused,
                    firstEpisodeFocusRequester = firstEpisodeFocusRequester,
                    upFocusRequester = upFocusRequester,
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episodes: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onEpisodeClick: (JellyfinItem) -> Unit,
    onEpisodeLongClick: (JellyfinItem) -> Unit,
    onEpisodeFocused: (JellyfinItem) -> Unit,
    firstEpisodeFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val lazyRowState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }

    LazyRow(
        state = lazyRowState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .focusRestorer(firstFocus),
    ) {
        itemsIndexed(episodes, key = { _, episode -> episode.id }) { index, episode ->
            val cardModifier = if (index == 0) {
                Modifier
                    .focusRequester(firstFocus)
                    .then(
                        if (firstEpisodeFocusRequester != null) {
                            Modifier.focusRequester(firstEpisodeFocusRequester)
                        } else {
                            Modifier
                        },
                    )
            } else {
                Modifier
            }

            EpisodeCard(
                episode = episode,
                jellyfinClient = jellyfinClient,
                onClick = { onEpisodeClick(episode) },
                onLongClick = { onEpisodeLongClick(episode) },
                modifier = cardModifier
                    .focusProperties {
                        if (upFocusRequester != null) {
                            up = upFocusRequester
                        }
                    }
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            onEpisodeFocused(episode)
                        }
                    },
            )
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Use episode thumbnail or series backdrop as fallback
    val imageUrl = jellyfinClient.getPrimaryImageUrl(
        episode.id,
        episode.imageTags?.primary,
        maxWidth = 400,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        // Episode card with thumbnail
        WideMediaCard(
            item = episode,
            imageUrl = imageUrl,
            onClick = onClick,
            onLongClick = onLongClick,
            showLabel = false,
        )

        // Episode info below card
        Column(
            modifier = Modifier.width(210.dp), // Match WideMediaCard width
        ) {
            // Episode number and title
            Text(
                text = buildString {
                    episode.indexNumber?.let { append("$it. ") }
                    append(episode.name)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Runtime if available
            episode.runTimeTicks?.let { ticks ->
                val minutes = (ticks / 600_000_000L).toInt()
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
                )
            }
        }
    }
}
