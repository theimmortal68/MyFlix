@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
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
import dev.jausc.myflix.core.common.ui.IconColors
import dev.jausc.myflix.tv.ui.theme.TvColors

val MinButtonSize = 20.dp

private val DefaultButtonPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)

private val ButtonShape = RoundedCornerShape(6.dp)

/**
 * Standard row of expandable play buttons for movies.
 * Includes Play (or Resume & Restart), Trailer, Watched, Favorite.
 */
@Composable
fun ExpandablePlayButtons(
    resumePositionTicks: Long,
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: (resumePosition: Long) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
    onTrailerClick: (() -> Unit)? = null,
    playButtonFocusRequester: FocusRequester? = null,
    leftEdgeFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    val firstFocus = playButtonFocusRequester ?: remember { FocusRequester() }
    val hasProgress = resumePositionTicks > 0L

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
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
                        .focusProperties {
                            if (downFocusRequester != null) {
                                down = downFocusRequester
                            }
                            if (leftEdgeFocusRequester != null) {
                                left = leftEdgeFocusRequester
                            }
                        }
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
                    modifier = Modifier
                        .focusProperties {
                            if (downFocusRequester != null) {
                                down = downFocusRequester
                            }
                        }
                        .onFocusChanged(buttonOnFocusChanged),
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
                        .focusProperties {
                            if (downFocusRequester != null) {
                                down = downFocusRequester
                            }
                            if (leftEdgeFocusRequester != null) {
                                left = leftEdgeFocusRequester
                            }
                        }
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
                    icon = Icons.Outlined.OndemandVideo,
                    iconColor = IconColors.Trailer,
                    onClick = onTrailerClick,
                    modifier = Modifier
                        .focusProperties {
                            if (downFocusRequester != null) {
                                down = downFocusRequester
                            }
                        }
                        .onFocusChanged(buttonOnFocusChanged),
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
                modifier = Modifier
                    .focusProperties {
                        if (downFocusRequester != null) {
                            down = downFocusRequester
                        }
                    }
                    .onFocusChanged(buttonOnFocusChanged),
            )
        }

        // Favorite button
        item("favorite") {
            ExpandablePlayButton(
                title = if (favorite) "Remove Favorite" else "Add to Favorites",
                icon = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (favorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = Modifier
                    .focusProperties {
                        if (downFocusRequester != null) {
                            down = downFocusRequester
                        }
                    }
                    .onFocusChanged(buttonOnFocusChanged),
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
 * Includes Play (or Resume & Restart), Watched, optionally Favorite, More.
 * When resuming (hasProgress=true), Favorite moves to More popup to save space.
 * More popup contains: Media Info, Add to Playlist, optionally Favorite, Go to Season, Go to Show.
 */
@Composable
fun EpisodeActionButtons(
    resumePositionTicks: Long,
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: (resumePosition: Long) -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
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

        // Favorite button (only shown when no resume progress - otherwise in More popup)
        if (onFavoriteClick != null) {
            item("favorite") {
                ExpandablePlayButton(
                    title = if (favorite) "Remove Favorite" else "Add to Favorites",
                    icon = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    iconColor = if (favorite) IconColors.FavoriteFilled else IconColors.Favorite,
                    onClick = onFavoriteClick,
                    modifier = Modifier.onFocusChanged(buttonOnFocusChanged),
                )
            }
        }

        // More button (Media Info, Add to Playlist, optionally Favorite, Go to Season, Go to Show)
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
 * Action buttons for collection details.
 * Includes Shuffle, Mark Watched, and Favorite buttons.
 */
@Composable
fun CollectionActionButtons(
    watched: Boolean,
    favorite: Boolean,
    onShuffleClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    buttonOnFocusChanged: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
    shuffleFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    val firstFocus = shuffleFocusRequester ?: remember { FocusRequester() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = modifier
            .focusGroup()
            .focusRestorer(firstFocus),
    ) {
        // Shuffle button
        item("shuffle") {
            ExpandablePlayButton(
                title = "Shuffle",
                icon = Icons.Outlined.Shuffle,
                iconColor = IconColors.Shuffle,
                onClick = onShuffleClick,
                modifier = Modifier
                    .focusProperties {
                        if (downFocusRequester != null) {
                            down = downFocusRequester
                        }
                    }
                    .onFocusChanged(buttonOnFocusChanged)
                    .focusRequester(firstFocus),
            )
        }

        // Mark Watched button
        item("watched") {
            ExpandablePlayButton(
                title = if (watched) "Mark Unwatched" else "Mark Watched",
                icon = if (watched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                iconColor = IconColors.Watched,
                onClick = onWatchedClick,
                modifier = Modifier
                    .focusProperties {
                        if (downFocusRequester != null) {
                            down = downFocusRequester
                        }
                    }
                    .onFocusChanged(buttonOnFocusChanged),
            )
        }

        // Favorite button
        item("favorite") {
            ExpandablePlayButton(
                title = if (favorite) "Remove Favorite" else "Add to Favorites",
                icon = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                iconColor = if (favorite) IconColors.FavoriteFilled else IconColors.Favorite,
                onClick = onFavoriteClick,
                modifier = Modifier
                    .focusProperties {
                        if (downFocusRequester != null) {
                            down = downFocusRequester
                        }
                    }
                    .onFocusChanged(buttonOnFocusChanged),
            )
        }
    }
}

/**
 * An icon button that expands to show text when focused.
 * 20dp square when unfocused (icon only), expands when focused (icon + text).
 *
 * @param alwaysExpanded When true, always shows icon + text regardless of focus state.
 *                       Useful for hero sections where buttons are hidden when not in use.
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
    alwaysExpanded: Boolean = false,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    // Show expanded state when focused OR when alwaysExpanded is true
    val showExpanded = isFocused || alwaysExpanded

    Button(
        onClick = onClick,
        modifier = modifier.then(
            if (showExpanded) {
                Modifier.requiredSizeIn(
                    minWidth = MinButtonSize,
                    minHeight = MinButtonSize,
                    maxHeight = MinButtonSize,
                )
            } else {
                Modifier.size(MinButtonSize)
            }
        ).border(
            width = if (isFocused) 0.dp else 1.dp,
            color = Color.White.copy(alpha = 0.2f),
            shape = ButtonShape,
        ),
        contentPadding = if (showExpanded) {
            PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        } else {
            PaddingValues(0.dp)
        },
        interactionSource = interactionSource,
        shape = ButtonDefaults.shape(shape = ButtonShape),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.White else iconColor,
            modifier = Modifier
                .size(12.dp)
                .then(
                    if (mirrorIcon) {
                        Modifier.graphicsLayer { scaleX = -1f }
                    } else {
                        Modifier
                    }
                ),
        )
        if (showExpanded) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}
