package dev.jausc.myflix.core.common.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent

/**
 * Semantic colors for action menu items.
 * Used consistently across TV dialogs and mobile bottom sheets.
 */
object ActionColors {
    val Play = Color(0xFF34D399) // Green
    val GoTo = Color(0xFF60A5FA) // Blue
    val Watched = Color(0xFFFBBF24) // Yellow/Gold
    val Favorite = Color(0xFFEF4444) // Red
    val Info = Color(0xFF94A3B8) // Gray
    val Series = Color(0xFF8B5CF6) // Purple
    val Audio = Color(0xFFF59E0B) // Amber
    val Subtitles = Color(0xFF06B6D4) // Cyan
    val MediaInfo = Color(0xFF6366F1) // Indigo
    val Remove = Color(0xFFF97316) // Orange
}

/**
 * Represents an entry in an action menu (dialog or bottom sheet).
 * Platform-specific code maps these to DialogItem/MenuItem as needed.
 */
sealed interface ActionEntry

/**
 * A divider between action groups.
 */
data object ActionDivider : ActionEntry

/**
 * A single actionable item in the menu.
 *
 * @param id Unique identifier for this action (for accessibility and testing)
 * @param text Display text for the action
 * @param icon Material icon for the action
 * @param iconTint Color tint for the icon
 * @param onClick Callback when the action is selected
 */
data class ActionItem(
    val id: String,
    val text: String,
    val icon: ImageVector,
    val iconTint: Color = Color.White,
    val onClick: () -> Unit,
) : ActionEntry

/**
 * Actions available from the home screen context menu.
 */
data class HomeActions(
    val onGoTo: (String) -> Unit,
    val onPlay: (String) -> Unit,
    val onMarkWatched: (String, Boolean) -> Unit,
    val onToggleFavorite: (String, Boolean) -> Unit,
    val onGoToSeries: ((String) -> Unit)? = null,
    val onHideFromResume: ((String) -> Unit)? = null,
)

/**
 * Actions available from the detail screen context menu.
 */
data class DetailActions(
    val onPlay: (String) -> Unit,
    val onMarkWatched: (String, Boolean) -> Unit,
    val onToggleFavorite: (String, Boolean) -> Unit,
    val onShowMediaInfo: ((JellyfinItem) -> Unit)? = null,
    val onGoToSeries: ((String) -> Unit)? = null,
)

/**
 * Build action menu entries for an item on the home screen.
 *
 * @param item The JellyfinItem being acted upon
 * @param actions Callbacks for the various actions
 * @return List of ActionEntry for the menu
 */
fun buildHomeActionItems(item: JellyfinItem, actions: HomeActions): List<ActionEntry> = buildList {
    val isWatched = item.userData?.played == true
    val isFavorite = item.userData?.isFavorite == true
    val hasProgress = item.progressPercent > 0f && item.progressPercent < 1f
    val isEpisode = item.type == "Episode"

    // Go To - navigate to detail page
    add(
        ActionItem(
            id = "goto",
            text = "Go To",
            icon = Icons.Default.Info,
            iconTint = ActionColors.GoTo,
            onClick = { actions.onGoTo(item.id) },
        ),
    )

    // Play action - varies based on progress
    if (hasProgress) {
        add(
            ActionItem(
                id = "resume",
                text = "Resume",
                icon = Icons.Default.PlayArrow,
                iconTint = ActionColors.Play,
                onClick = { actions.onPlay(item.id) },
            ),
        )
        add(
            ActionItem(
                id = "restart",
                text = "Restart",
                icon = Icons.Default.Refresh,
                iconTint = ActionColors.Play,
                onClick = { actions.onPlay(item.id) },
            ),
        )
    } else {
        add(
            ActionItem(
                id = "play",
                text = "Play",
                icon = Icons.Default.PlayArrow,
                iconTint = ActionColors.Play,
                onClick = { actions.onPlay(item.id) },
            ),
        )
    }

    // Remove from Continue Watching (only if item has progress)
    if (hasProgress && actions.onHideFromResume != null) {
        add(ActionDivider)
        add(
            ActionItem(
                id = "hide_from_resume",
                text = "Remove from Continue Watching",
                icon = Icons.Default.Close,
                iconTint = ActionColors.Remove,
                onClick = { actions.onHideFromResume.invoke(item.id) },
            ),
        )
    }

    // Divider before state toggles
    add(ActionDivider)

    // Mark Watched / Unwatched
    add(
        ActionItem(
            id = "watched",
            text = if (isWatched) "Mark Unwatched" else "Mark Watched",
            icon = Icons.Default.Check,
            iconTint = ActionColors.Watched,
            onClick = { actions.onMarkWatched(item.id, !isWatched) },
        ),
    )

    // Favorite toggle
    add(
        ActionItem(
            id = "favorite",
            text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            iconTint = ActionColors.Favorite,
            onClick = { actions.onToggleFavorite(item.id, !isFavorite) },
        ),
    )

    // Go to Series (for episodes only)
    if (isEpisode && item.seriesId != null && actions.onGoToSeries != null) {
        add(ActionDivider)
        add(
            ActionItem(
                id = "goto_series",
                text = "Go to Series",
                icon = Icons.Default.Tv,
                iconTint = ActionColors.Series,
                onClick = { actions.onGoToSeries.invoke(item.seriesId) },
            ),
        )
    }
}

/**
 * Build action menu entries for an episode on the detail screen.
 *
 * @param item The episode being acted upon
 * @param actions Callbacks for the various actions
 * @return List of ActionEntry for the menu
 */
fun buildDetailActionItems(item: JellyfinItem, actions: DetailActions): List<ActionEntry> = buildList {
    val isWatched = item.userData?.played == true
    val isFavorite = item.userData?.isFavorite == true
    val hasProgress = item.progressPercent > 0f && item.progressPercent < 1f

    // Play action - varies based on progress
    if (hasProgress) {
        add(
            ActionItem(
                id = "resume",
                text = "Resume",
                icon = Icons.Default.PlayArrow,
                iconTint = ActionColors.Play,
                onClick = { actions.onPlay(item.id) },
            ),
        )
        add(
            ActionItem(
                id = "restart",
                text = "Restart",
                icon = Icons.Default.Refresh,
                iconTint = ActionColors.Play,
                onClick = { actions.onPlay(item.id) },
            ),
        )
    } else {
        add(
            ActionItem(
                id = "play",
                text = "Play",
                icon = Icons.Default.PlayArrow,
                iconTint = ActionColors.Play,
                onClick = { actions.onPlay(item.id) },
            ),
        )
    }

    // Divider before state toggles
    add(ActionDivider)

    // Mark Watched / Unwatched
    add(
        ActionItem(
            id = "watched",
            text = if (isWatched) "Mark Unwatched" else "Mark Watched",
            icon = Icons.Default.Check,
            iconTint = ActionColors.Watched,
            onClick = { actions.onMarkWatched(item.id, !isWatched) },
        ),
    )

    // Favorite toggle
    add(
        ActionItem(
            id = "favorite",
            text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            iconTint = ActionColors.Favorite,
            onClick = { actions.onToggleFavorite(item.id, !isFavorite) },
        ),
    )

    // Media Information
    if (actions.onShowMediaInfo != null) {
        add(ActionDivider)
        add(
            ActionItem(
                id = "media_info",
                text = "Media Information",
                icon = Icons.Default.VideoFile,
                iconTint = ActionColors.MediaInfo,
                onClick = { actions.onShowMediaInfo.invoke(item) },
            ),
        )
    }

    // Go to Series (for episodes only)
    if (item.type == "Episode" && item.seriesId != null && actions.onGoToSeries != null) {
        add(
            ActionItem(
                id = "goto_series",
                text = "Go to Series",
                icon = Icons.Default.Tv,
                iconTint = ActionColors.Series,
                onClick = { actions.onGoToSeries.invoke(item.seriesId) },
            ),
        )
    }
}
