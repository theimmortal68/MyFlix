@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Action buttons row for detail screen.
 * Contains Play/Resume, Favorite toggle, and More options.
 */
@Composable
fun DetailActionsRow(
    item: JellyfinItem,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMarkWatchedClick: () -> Unit,
    onMarkUnwatchedClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
) {
    val isFavorite = item.userData?.isFavorite == true
    val isPlayed = item.userData?.played == true
    val hasProgress = (item.userData?.playbackPositionTicks ?: 0L) > 0L

    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play/Resume button
        Button(
            onClick = onPlayClick,
            modifier = Modifier
                .height(20.dp)
                .focusRequester(focusRequester),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                contentColor = TvColors.TextPrimary,
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (hasProgress) "Resume" else "Play",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        // Favorite button
        Button(
            onClick = onFavoriteClick,
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                contentColor = if (isFavorite) TvColors.Error else TvColors.TextPrimary,
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                modifier = Modifier.size(14.dp),
            )
        }

        // More options button with dialog
        Button(
            onClick = { showMoreMenu = true },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                contentColor = TvColors.TextPrimary,
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                modifier = Modifier.size(14.dp),
            )
        }

        if (showMoreMenu) {
            MoreOptionsDialog(
                isPlayed = isPlayed,
                onDismiss = { showMoreMenu = false },
                onMarkWatchedClick = {
                    showMoreMenu = false
                    onMarkWatchedClick()
                },
                onMarkUnwatchedClick = {
                    showMoreMenu = false
                    onMarkUnwatchedClick()
                },
            )
        }
    }
}

@Composable
private fun MoreOptionsDialog(
    isPlayed: Boolean,
    onDismiss: () -> Unit,
    onMarkWatchedClick: () -> Unit,
    onMarkUnwatchedClick: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TvColors.SurfaceElevated)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (isPlayed) {
                ListItem(
                    selected = false,
                    onClick = onMarkUnwatchedClick,
                    headlineContent = {
                        Text(
                            text = "Mark as unwatched",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ListItem(
                    selected = false,
                    onClick = onMarkWatchedClick,
                    headlineContent = {
                        Text(
                            text = "Mark as watched",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
