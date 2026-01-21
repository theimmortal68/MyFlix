@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Vertical list of episode rows for the season detail screen.
 * Each row shows thumbnail, description, and action buttons.
 */
@Composable
fun EpisodeListSection(
    episodes: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onPlayClick: (JellyfinItem) -> Unit,
    onMoreInfoClick: (JellyfinItem) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    firstRowFocusRequester: FocusRequester? = null,
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
                val firstFocus = remember { FocusRequester() }

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRestorer(firstFocus),
                ) {
                    itemsIndexed(episodes, key = { _, episode -> episode.id }) { index, episode ->
                        val rowModifier = if (index == 0) {
                            Modifier
                                .focusRequester(firstFocus)
                                .then(
                                    if (firstRowFocusRequester != null) {
                                        Modifier.focusRequester(firstRowFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                        } else {
                            Modifier
                        }

                        EpisodeListRow(
                            episode = episode,
                            jellyfinClient = jellyfinClient,
                            onPlayClick = { onPlayClick(episode) },
                            onMoreInfoClick = { onMoreInfoClick(episode) },
                            modifier = rowModifier
                                .focusProperties {
                                    if (index == 0 && upFocusRequester != null) {
                                        up = upFocusRequester
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single episode row with thumbnail, description, and action buttons.
 *
 * Layout:
 * ┌──────────┐  Episode Title
 * │  Thumb   │  S1 E3 · 45m · TV-14
 * │  210x118 │  Episode description text...
 * │  (16:9)  │  ┌────────┐ ┌─────────────┐
 * │ Progress │  │ ▶ Play │ │ ⓘ More Info │
 * └──────────┘  └────────┘ └─────────────┘
 */
@Composable
fun EpisodeListRow(
    episode: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onMoreInfoClick: () -> Unit,
    onWatchedToggle: ((Boolean) -> Unit)? = null,
    onFavoriteToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val playFocus = playButtonFocusRequester ?: remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    // Bring entire row into view when any child gains focus
                    scope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
            .focusGroup()
            .focusRestorer(playFocus),
    ) {
        // Thumbnail (210dp × 118dp, 16:9)
        EpisodeThumbnail(
            episode = episode,
            imageUrl = jellyfinClient.getPrimaryImageUrl(
                episode.id,
                episode.imageTags?.primary,
                maxWidth = 400,
            ),
        )

        // Episode info column
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            // Episode title
            Text(
                text = episode.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Metadata line: S1 E3 · 45m · TV-14
            val metadataText = buildEpisodeMetadataLine(episode)
            if (metadataText.isNotEmpty()) {
                Text(
                    text = metadataText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
                )
            }

            // Description (3 lines max)
            episode.overview?.let { overview ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action buttons
            val isWatched = episode.userData?.played == true
            val isFavorite = episode.userData?.isFavorite == true

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.focusGroup(),
            ) {
                EpisodeActionButton(
                    title = "Play",
                    icon = Icons.Outlined.PlayArrow,
                    iconColor = IconColors.Play,
                    onClick = onPlayClick,
                    modifier = Modifier
                        .focusRequester(playFocus)
                        .then(
                            if (upFocusRequester != null) {
                                Modifier.focusProperties { up = upFocusRequester }
                            } else {
                                Modifier
                            },
                        ),
                )

                // Mark as Watched button
                onWatchedToggle?.let { onToggle ->
                    EpisodeActionButton(
                        title = if (isWatched) "Unwatch" else "Watched",
                        icon = if (isWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        iconColor = IconColors.Watched,
                        onClick = { onToggle(!isWatched) },
                    )
                }

                // Favorite button
                onFavoriteToggle?.let { onToggle ->
                    EpisodeActionButton(
                        title = if (isFavorite) "Remove Favorite" else "Add to Favorites",
                        icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        iconColor = if (isFavorite) IconColors.FavoriteFilled else IconColors.Favorite,
                        onClick = { onToggle(!isFavorite) },
                    )
                }

                EpisodeActionButton(
                    title = "More Info",
                    icon = Icons.Outlined.Info,
                    iconColor = IconColors.MediaInfo,
                    onClick = onMoreInfoClick,
                )
            }
        }
    }
}

/**
 * Episode thumbnail with progress bar overlay.
 */
@Composable
private fun EpisodeThumbnail(
    episode: JellyfinItem,
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(210.dp)
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.medium),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = episode.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Progress bar at bottom
        if (episode.progressPercent > 0f && episode.progressPercent < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TvColors.Surface.copy(alpha = 0.7f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(episode.progressPercent)
                        .background(TvColors.BluePrimary),
                )
            }
        }

        // Watched indicator (checkmark badge in corner)
        if (episode.userData?.played == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvColors.BluePrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Hero-style action button for episode rows.
 * Shows icon only when unfocused, expands to show text when focused.
 */
@Composable
private fun EpisodeActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        modifier = modifier.height(MinButtonSize),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        interactionSource = interactionSource,
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Box(
            modifier = Modifier.height(MinButtonSize),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.White else iconColor,
                modifier = Modifier.size(14.dp),
            )
        }
        AnimatedVisibility(isFocused) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

/**
 * Build the metadata line for an episode: "S1 E3 · 45m · TV-14"
 */
private fun buildEpisodeMetadataLine(episode: JellyfinItem): String = buildList {
    // Season and episode number
    val season = episode.parentIndexNumber
    val epNum = episode.indexNumber
    if (season != null && epNum != null) {
        add("S$season E$epNum")
    } else if (epNum != null) {
        add("E$epNum")
    }

    // Runtime
    episode.runTimeTicks?.let { ticks ->
        val minutes = (ticks / 600_000_000L).toInt()
        if (minutes > 0) add("${minutes}m")
    }

    // Official rating
    episode.officialRating?.let { add(it) }
}.joinToString(" · ")
