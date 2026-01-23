@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Icon colors for action buttons.
 */
object IconColors {
    val Play = Color(0xFF4CAF50) // Green
    val Resume = Color(0xFF2196F3) // Blue
    val Restart = Color(0xFFFF9800) // Orange
    val Shuffle = Color(0xFF9C27B0) // Purple
    val Watched = Color(0xFF4CAF50) // Green
    val Favorite = Color(0xFFF44336) // Red
    val Trailer = Color(0xFFF59E0B) // Amber
}

/**
 * Play button for movies with resume/restart options.
 */
@Composable
fun MoviePlayButtons(
    resumePositionTicks: Long,
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: (resumePositionTicks: Long) -> Unit,
    onRestartClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasProgress = resumePositionTicks > 0L

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Primary play button
        Button(
            onClick = { onPlayClick(if (hasProgress) resumePositionTicks else 0L) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasProgress) IconColors.Resume else IconColors.Play,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (hasProgress) "Resume" else "Play")
        }

        // Restart button (if in progress)
        if (hasProgress) {
            FilledTonalButton(
                onClick = onRestartClick,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restart")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Watched toggle
        IconButton(onClick = onWatchedClick) {
            Icon(
                imageVector = if (watched) Icons.Outlined.CheckCircle else Icons.Outlined.CheckCircleOutline,
                contentDescription = if (watched) "Mark as unwatched" else "Mark as watched",
                tint = if (watched) IconColors.Watched else MaterialTheme.colorScheme.onSurface,
            )
        }

        // Favorite toggle
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                tint = if (favorite) IconColors.Favorite else MaterialTheme.colorScheme.onSurface,
            )
        }

        // More options
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More options",
            )
        }
    }
}

/**
 * Action buttons for series.
 */
@Composable
fun SeriesActionButtons(
    watched: Boolean,
    favorite: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onTrailerClick: (() -> Unit)? = null,
    onWatchedClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    showMoreButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play button
        Button(
            onClick = onPlayClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = IconColors.Play,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play")
        }

        // Shuffle button
        FilledTonalButton(onClick = onShuffleClick) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Shuffle")
        }

        if (onTrailerClick != null) {
            FilledTonalButton(onClick = onTrailerClick) {
                Icon(
                    imageVector = Icons.Outlined.OndemandVideo,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = IconColors.Trailer,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Trailer")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Watched toggle
        IconButton(onClick = onWatchedClick) {
            Icon(
                imageVector = if (watched) Icons.Outlined.CheckCircle else Icons.Outlined.CheckCircleOutline,
                contentDescription = if (watched) "Mark as unwatched" else "Mark as watched",
                tint = if (watched) IconColors.Watched else MaterialTheme.colorScheme.onSurface,
            )
        }

        // Favorite toggle
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                tint = if (favorite) IconColors.Favorite else MaterialTheme.colorScheme.onSurface,
            )
        }

        if (showMoreButton) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                )
            }
        }
    }
}
