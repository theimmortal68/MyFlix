package dev.jausc.myflix.tv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.Color
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent

/**
 * Icon colors for dialog items - semantic colors for different actions.
 */
object DialogIconColors {
    val Play = Color(0xFF34D399)       // Green
    val GoTo = Color(0xFF60A5FA)       // Blue
    val Watched = Color(0xFFFBBF24)    // Yellow/Gold
    val Favorite = Color(0xFFEF4444)   // Red
    val Info = Color(0xFF94A3B8)       // Gray
    val Series = Color(0xFF8B5CF6)     // Purple
    val Audio = Color(0xFFF59E0B)      // Amber
    val Subtitles = Color(0xFF06B6D4)  // Cyan
    val MediaInfo = Color(0xFF6366F1)  // Indigo
}

/**
 * Actions that can be performed from the home screen dialog.
 */
data class HomeDialogActions(
    val onGoTo: (String) -> Unit,
    val onPlay: (String) -> Unit,
    val onMarkWatched: (String, Boolean) -> Unit,
    val onToggleFavorite: (String, Boolean) -> Unit,
    val onGoToSeries: ((String) -> Unit)? = null
)

/**
 * Build dialog items for an item on the home screen.
 *
 * @param item The JellyfinItem being acted upon
 * @param actions Callbacks for the various actions
 * @return List of DialogItemEntry for the dialog
 */
fun buildHomeDialogItems(
    item: JellyfinItem,
    actions: HomeDialogActions
): List<DialogItemEntry> = buildList {
    val isWatched = item.userData?.played == true
    val isFavorite = item.userData?.isFavorite == true
    val hasProgress = item.progressPercent > 0f && item.progressPercent < 1f
    val isEpisode = item.type == "Episode"

    // Go To - navigate to detail page
    add(DialogItem(
        text = "Go To",
        icon = Icons.Default.Info,
        iconTint = DialogIconColors.GoTo,
        onClick = { actions.onGoTo(item.id) }
    ))

    // Play action - varies based on progress
    when {
        hasProgress -> {
            // Resume option
            add(DialogItem(
                text = "Resume",
                icon = Icons.Default.PlayArrow,
                iconTint = DialogIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
            // Restart option
            add(DialogItem(
                text = "Restart",
                icon = Icons.Default.Refresh,
                iconTint = DialogIconColors.Play,
                onClick = {
                    // Mark position as 0 then play
                    actions.onPlay(item.id)
                }
            ))
        }
        else -> {
            add(DialogItem(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                iconTint = DialogIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
        }
    }

    // Divider before state toggles
    add(DialogItemDivider)

    // Mark Watched / Unwatched
    add(DialogItem(
        text = if (isWatched) "Mark Unwatched" else "Mark Watched",
        icon = Icons.Default.Check,
        iconTint = DialogIconColors.Watched,
        onClick = { actions.onMarkWatched(item.id, !isWatched) }
    ))

    // Favorite toggle
    add(DialogItem(
        text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        iconTint = DialogIconColors.Favorite,
        onClick = { actions.onToggleFavorite(item.id, !isFavorite) }
    ))

    // Go to Series (for episodes only)
    if (isEpisode && item.seriesId != null && actions.onGoToSeries != null) {
        add(DialogItemDivider)
        add(DialogItem(
            text = "Go to Series",
            icon = Icons.Default.Tv,
            iconTint = DialogIconColors.Series,
            onClick = { actions.onGoToSeries.invoke(item.seriesId!!) }
        ))
    }
}

/**
 * Actions that can be performed from the detail screen dialog.
 * Extends home actions with media info.
 */
data class DetailDialogActions(
    val onPlay: (String) -> Unit,
    val onMarkWatched: (String, Boolean) -> Unit,
    val onToggleFavorite: (String, Boolean) -> Unit,
    val onShowMediaInfo: ((JellyfinItem) -> Unit)? = null,
    val onGoToSeries: ((String) -> Unit)? = null
)

/**
 * Build dialog items for an episode on the detail screen.
 * Similar to home but focused on playback actions.
 *
 * @param item The episode being acted upon
 * @param actions Callbacks for the various actions
 * @return List of DialogItemEntry for the dialog
 */
fun buildDetailDialogItems(
    item: JellyfinItem,
    actions: DetailDialogActions
): List<DialogItemEntry> = buildList {
    val isWatched = item.userData?.played == true
    val isFavorite = item.userData?.isFavorite == true
    val hasProgress = item.progressPercent > 0f && item.progressPercent < 1f

    // Play action - varies based on progress
    when {
        hasProgress -> {
            add(DialogItem(
                text = "Resume",
                icon = Icons.Default.PlayArrow,
                iconTint = DialogIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
            add(DialogItem(
                text = "Restart",
                icon = Icons.Default.Refresh,
                iconTint = DialogIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
        }
        else -> {
            add(DialogItem(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                iconTint = DialogIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
        }
    }

    // Divider before state toggles
    add(DialogItemDivider)

    // Mark Watched / Unwatched
    add(DialogItem(
        text = if (isWatched) "Mark Unwatched" else "Mark Watched",
        icon = Icons.Default.Check,
        iconTint = DialogIconColors.Watched,
        onClick = { actions.onMarkWatched(item.id, !isWatched) }
    ))

    // Favorite toggle
    add(DialogItem(
        text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        iconTint = DialogIconColors.Favorite,
        onClick = { actions.onToggleFavorite(item.id, !isFavorite) }
    ))

    // Media Information
    if (actions.onShowMediaInfo != null) {
        add(DialogItemDivider)
        add(DialogItem(
            text = "Media Information",
            icon = Icons.Default.VideoFile,
            iconTint = DialogIconColors.MediaInfo,
            onClick = { actions.onShowMediaInfo.invoke(item) }
        ))
    }

    // Go to Series (for episodes only)
    if (item.type == "Episode" && item.seriesId != null && actions.onGoToSeries != null) {
        add(DialogItem(
            text = "Go to Series",
            icon = Icons.Default.Tv,
            iconTint = DialogIconColors.Series,
            onClick = { actions.onGoToSeries.invoke(item.seriesId!!) }
        ))
    }
}
