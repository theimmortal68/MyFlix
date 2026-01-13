@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * Action buttons row for mobile detail screen.
 * Contains Play/Resume, Favorite toggle, and More options.
 */
@Composable
fun MobileDetailActionsRow(
    item: JellyfinItem,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMarkWatchedClick: () -> Unit,
    onMarkUnwatchedClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (hasProgress) "Resume" else "Play")
        }

        // Favorite button
        OutlinedIconButton(
            onClick = onFavoriteClick,
        ) {
            Icon(
                imageVector = if (isFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }

        // More options button
        IconButton(
            onClick = { showMoreMenu = true },
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
            )
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
            ) {
                if (isPlayed) {
                    DropdownMenuItem(
                        text = { Text("Mark as unwatched") },
                        onClick = {
                            showMoreMenu = false
                            onMarkUnwatchedClick()
                        },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Mark as watched") },
                        onClick = {
                            showMoreMenu = false
                            onMarkWatchedClick()
                        },
                    )
                }
            }
        }
    }
}
