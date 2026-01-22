@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.TvOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

val MinButtonSize = 24.dp

private val DefaultButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)

/**
 * Icon colors for different button actions.
 */
object IconColors {
    val Play = Color(0xFF22C55E) // Green
    val Resume = Color(0xFF3B82F6) // Blue
    val Restart = Color(0xFFF97316) // Orange
    val Shuffle = Color(0xFFA855F7) // Purple
    val Watched = Color(0xFF06B6D4) // Cyan
    val Favorite = Color(0xFF94A3B8) // Gray
    val FavoriteFilled = Color(0xFFEF4444) // Red
    val More = Color(0xFF94A3B8) // Gray
    val Trailer = Color(0xFFFBBF24) // Yellow
    val MediaInfo = Color(0xFF3B82F6) // Blue - for media information
    val Navigation = Color(0xFF34D399) // Green - for navigation arrows (Go to Season/Show)
    val Playlist = Color(0xFF8B5CF6) // Violet - for add to playlist
    val GoToSeries = Color(0xFF34D399) // Green - for Go to Series navigation
}

/**
 * Standard row of expandable play buttons for movies.
 * Includes Play (or Resume & Restart), Trailer, Watched, Favorite, More.
 * More popup contains: Media Info, Add to Playlist.
 */
@Composable
fun ExpandablePlayButtons(
    resumePositionTicks: Long,
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: (resumePosition: Long) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
    onTrailerClick: (() -> Unit)? = null,
    playButtonFocusRequester: FocusRequester? = null,
) {
    val firstFocus = playButtonFocusRequester ?: remember { FocusRequester() }
    val hasProgress = resumePositionTicks > 0L

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(8.dp),
        modifier = modifier
            .focusGroup()
            .focusRestorer(firstFocus),
    ) {
        if (hasProgress) {
            // Resume button
            item("play") {
                ExpandablePlayButton(
                    title = "Resume",
                    icon = Icons.Outlined.PlayArrow,
                    iconColor = IconColors.Resume,
                    onClick = { onPlayClick(resumePositionTicks) },
                    modifier = Modifier
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus),
                )
            }
            // Restart button
            item("restart") {
                ExpandablePlayButton(
                    title = "Restart",
                    icon = Icons.Outlined.Refresh,
                    iconColor = IconColors.Restart,
                    onClick = { onPlayClick(0L) },
                    modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
                    mirrorIcon = true,
                )
            }
        } else {
            // Play button
            item("play") {
                ExpandablePlayButton(
                    title = "Play",
                    icon = Icons.Outlined.PlayArrow,
                    iconColor = IconColors.Play,
                    onClick = { onPlayClick(0L) },
                    modifier = Modifier
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus),
                )
            }
        }

        // Trailer button (only shown if callback is provided)
        if (onTrailerClick != null) {
            item("trailer") {
                ExpandablePlayButton(
                    title = "Trailer",
                    icon = Icons.Outlined.PlayArrow,
                    iconColor = IconColors.Trailer,
                    onClick = onTrailerClick,
                    modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
                )
            }
        }

        // Watched button
        item("watched") {
            ExpandablePlayButton(
                title = if (watched) "Mark Unwatched" else "Mark Watched",
                icon = if (watched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // Favorite button
        item("favorite") {
            ExpandablePlayButton(
                title = if (favorite) "Remove Favorite" else "Add to Favorites",
                icon = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (favorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // More button (Media Info, Add to Playlist)
        item("more") {
            ExpandablePlayButton(
                title = "More",
                icon = Icons.Outlined.MoreVert,
                iconColor = IconColors.More,
                onClick = onMoreClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }
    }
}

/**
 * Standard row of action buttons for series.
 * Includes Play (next), Shuffle, Trailer, Watched, More.
 * More popup contains: Add to Favorites, Add to Playlist.
 */
@Composable
fun SeriesActionButtons(
    watched: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onMoreClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    playButtonFocusRequester: FocusRequester? = null,
    onTrailerClick: (() -> Unit)? = null,
) {
    val firstFocus = playButtonFocusRequester ?: remember { FocusRequester() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = contentPadding,
        modifier = modifier
            .focusGroup()
            .focusRestorer(firstFocus),
    ) {
        // Play button
        item("play") {
            ExpandablePlayButton(
                title = "Play",
                icon = Icons.Outlined.PlayArrow,
                iconColor = IconColors.Play,
                onClick = onPlayClick,
                modifier = Modifier
                    .onFocusChanged(buttonOnFocusChanged)
                    .focusRequester(firstFocus),
            )
        }

        // Shuffle button
        item("shuffle") {
            ExpandablePlayButton(
                title = "Shuffle",
                icon = Icons.Outlined.Shuffle,
                iconColor = IconColors.Shuffle,
                onClick = onShuffleClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // Trailer button (only shown if callback is provided)
        if (onTrailerClick != null) {
            item("trailer") {
                ExpandablePlayButton(
                    title = "Trailer",
                    icon = Icons.Outlined.OndemandVideo,
                    iconColor = IconColors.Trailer,
                    onClick = onTrailerClick,
                    modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
                )
            }
        }

        // Watched button
        item("watched") {
            ExpandablePlayButton(
                title = if (watched) "Mark Unwatched" else "Mark Watched",
                icon = if (watched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // More button (Add to Favorites, Add to Playlist)
        item("more") {
            ExpandablePlayButton(
                title = "More",
                icon = Icons.Outlined.MoreVert,
                iconColor = IconColors.More,
                onClick = onMoreClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }
    }
}

/**
 * Action buttons for season details.
 * Includes Play, Shuffle, Watched, Favorite, More.
 * More popup contains: Go to Series, Add to Playlist.
 */
@Composable
fun SeasonActionButtons(
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    playButtonFocusRequester: FocusRequester? = null,
) {
    val firstFocus = playButtonFocusRequester ?: remember { FocusRequester() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = contentPadding,
        modifier = modifier
            .focusGroup()
            .focusRestorer(firstFocus),
    ) {
        // Play button
        item("play") {
            ExpandablePlayButton(
                title = "Play",
                icon = Icons.Outlined.PlayArrow,
                iconColor = IconColors.Play,
                onClick = onPlayClick,
                modifier = Modifier
                    .onFocusChanged(buttonOnFocusChanged)
                    .focusRequester(firstFocus),
            )
        }

        // Shuffle button
        item("shuffle") {
            ExpandablePlayButton(
                title = "Shuffle",
                icon = Icons.Outlined.Shuffle,
                iconColor = IconColors.Shuffle,
                onClick = onShuffleClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // Watched button
        item("watched") {
            ExpandablePlayButton(
                title = if (watched) "Mark Unwatched" else "Mark Watched",
                icon = if (watched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // Favorite button
        item("favorite") {
            ExpandablePlayButton(
                title = if (favorite) "Remove Favorite" else "Add to Favorites",
                icon = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (favorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // More button (Go to Series, Add to Playlist)
        item("more") {
            ExpandablePlayButton(
                title = "More",
                icon = Icons.Outlined.MoreVert,
                iconColor = IconColors.More,
                onClick = onMoreClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }
    }
}

/**
 * Action buttons for episode details.
 * Includes Play (or Resume & Restart), Watched, Favorite, More.
 * More popup contains: Media Info, Add to Playlist.
 */
@Composable
fun EpisodeActionButtons(
    resumePositionTicks: Long,
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: (resumePosition: Long) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    playButtonFocusRequester: FocusRequester? = null,
) {
    val firstFocus = playButtonFocusRequester ?: remember { FocusRequester() }
    val hasProgress = resumePositionTicks > 0L

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = contentPadding,
        modifier = modifier
            .focusGroup()
            .focusRestorer(firstFocus),
    ) {
        if (hasProgress) {
            // Resume button
            item("play") {
                ExpandablePlayButton(
                    title = "Resume",
                    icon = Icons.Outlined.PlayArrow,
                    iconColor = IconColors.Resume,
                    onClick = { onPlayClick(resumePositionTicks) },
                    modifier = Modifier
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus),
                )
            }
            // Restart button
            item("restart") {
                ExpandablePlayButton(
                    title = "Restart",
                    icon = Icons.Outlined.Refresh,
                    iconColor = IconColors.Restart,
                    onClick = { onPlayClick(0L) },
                    modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
                    mirrorIcon = true,
                )
            }
        } else {
            // Play button
            item("play") {
                ExpandablePlayButton(
                    title = "Play",
                    icon = Icons.Outlined.PlayArrow,
                    iconColor = IconColors.Play,
                    onClick = { onPlayClick(0L) },
                    modifier = Modifier
                        .onFocusChanged(buttonOnFocusChanged)
                        .focusRequester(firstFocus),
                )
            }
        }

        // Watched button
        item("watched") {
            ExpandablePlayButton(
                title = if (watched) "Mark Unwatched" else "Mark Watched",
                icon = if (watched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // Favorite button
        item("favorite") {
            ExpandablePlayButton(
                title = if (favorite) "Remove Favorite" else "Add to Favorites",
                icon = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (favorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }

        // More button (Media Info, Add to Playlist)
        item("more") {
            ExpandablePlayButton(
                title = "More",
                icon = Icons.Outlined.MoreVert,
                iconColor = IconColors.More,
                onClick = onMoreClick,
                modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
            )
        }
    }
}

/**
 * An icon button that expands to show text when focused.
 * 24dp minimum size, icon only when unfocused, icon + text when focused.
 */
@Composable
fun ExpandablePlayButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    mirrorIcon: Boolean = false,
    iconColor: Color = Color.Unspecified,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        modifier = modifier.requiredSizeIn(
            minWidth = MinButtonSize,
            minHeight = MinButtonSize,
            maxHeight = MinButtonSize,
        ),
        contentPadding = DefaultButtonPadding,
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
                modifier = Modifier
                    .size(14.dp)
                    .then(
                        if (mirrorIcon) {
                            Modifier.graphicsLayer { scaleX = -1f }
                        } else {
                            Modifier
                        }
                    ),
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
