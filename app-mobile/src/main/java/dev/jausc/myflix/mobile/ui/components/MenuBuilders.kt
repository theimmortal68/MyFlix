package dev.jausc.myflix.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.Color
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent

/**
 * Icon colors for menu items - semantic colors for different actions.
 */
object MenuIconColors {
    val Play = Color(0xFF34D399)       // Green
    val GoTo = Color(0xFF60A5FA)       // Blue
    val Watched = Color(0xFFFBBF24)    // Yellow/Gold
    val Favorite = Color(0xFFEF4444)   // Red
    val Info = Color(0xFF94A3B8)       // Gray
    val Series = Color(0xFF8B5CF6)     // Purple
}

/**
 * Actions that can be performed from the home screen menu.
 */
data class HomeMenuActions(
    val onGoTo: (String) -> Unit,
    val onPlay: (String) -> Unit,
    val onMarkWatched: (String, Boolean) -> Unit,
    val onToggleFavorite: (String, Boolean) -> Unit,
    val onGoToSeries: ((String) -> Unit)? = null
)

/**
 * Build menu items for an item on the home screen.
 *
 * @param item The JellyfinItem being acted upon
 * @param actions Callbacks for the various actions
 * @return List of MenuItemEntry for the bottom sheet
 */
fun buildHomeMenuItems(
    item: JellyfinItem,
    actions: HomeMenuActions
): List<MenuItemEntry> = buildList {
    val isWatched = item.userData?.played == true
    val isFavorite = item.userData?.isFavorite == true
    val hasProgress = item.progressPercent > 0f && item.progressPercent < 1f
    val isEpisode = item.type == "Episode"

    // Go To - navigate to detail page
    add(MenuItem(
        text = "Go To",
        icon = Icons.Default.Info,
        iconTint = MenuIconColors.GoTo,
        onClick = { actions.onGoTo(item.id) }
    ))

    // Play action - varies based on progress
    when {
        hasProgress -> {
            // Resume option
            add(MenuItem(
                text = "Resume",
                icon = Icons.Default.PlayArrow,
                iconTint = MenuIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
            // Restart option
            add(MenuItem(
                text = "Restart",
                icon = Icons.Default.Refresh,
                iconTint = MenuIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
        }
        else -> {
            add(MenuItem(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                iconTint = MenuIconColors.Play,
                onClick = { actions.onPlay(item.id) }
            ))
        }
    }

    // Divider before state toggles
    add(MenuItemDivider)

    // Mark Watched / Unwatched
    add(MenuItem(
        text = if (isWatched) "Mark Unwatched" else "Mark Watched",
        icon = Icons.Default.Check,
        iconTint = MenuIconColors.Watched,
        onClick = { actions.onMarkWatched(item.id, !isWatched) }
    ))

    // Favorite toggle
    add(MenuItem(
        text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        iconTint = MenuIconColors.Favorite,
        onClick = { actions.onToggleFavorite(item.id, !isFavorite) }
    ))

    // Go to Series (for episodes only)
    if (isEpisode && item.seriesId != null && actions.onGoToSeries != null) {
        add(MenuItemDivider)
        add(MenuItem(
            text = "Go to Series",
            icon = Icons.Default.Tv,
            iconTint = MenuIconColors.Series,
            onClick = { actions.onGoToSeries.invoke(item.seriesId!!) }
        ))
    }
}
