package dev.jausc.myflix.core.common.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import dev.jausc.myflix.core.seerr.SeerrMedia
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Semantic colors for Seerr action menu items.
 */
object SeerrActionColors {
    val Request = Color(0xFF8B5CF6) // Purple (Seerr brand)
    val Watchlist = Color(0xFFFBBF24) // Yellow/Gold
    val GoTo = Color(0xFF60A5FA) // Blue
    val Pending = Color(0xFFFBBF24) // Yellow (already requested)
    val Available = Color(0xFF22C55E) // Green (already in library)
}

/**
 * Represents an entry in a Seerr action menu.
 */
sealed interface SeerrActionEntry

/**
 * A divider between action groups.
 */
data object SeerrActionDivider : SeerrActionEntry

/**
 * A single actionable item in the Seerr menu.
 */
data class SeerrActionItem(
    val id: String,
    val text: String,
    val icon: ImageVector,
    val iconTint: Color = Color.White,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
) : SeerrActionEntry

/**
 * Actions available from Seerr context menus.
 */
data class SeerrMediaActions(
    val onGoTo: (mediaType: String, tmdbId: Int) -> Unit,
    val onRequest: (media: SeerrMedia) -> Unit,
    val onAddToWatchlist: (media: SeerrMedia) -> Unit,
    val onRemoveFromWatchlist: (media: SeerrMedia) -> Unit,
)

/**
 * Build action menu entries for a Seerr media item.
 *
 * @param media The SeerrMedia being acted upon
 * @param actions Callbacks for the various actions
 * @param isOnWatchlist Whether the item is currently on the user's watchlist
 * @return List of SeerrActionEntry for the menu
 */
fun buildSeerrActionItems(
    media: SeerrMedia,
    actions: SeerrMediaActions,
    isOnWatchlist: Boolean = false,
): List<SeerrActionEntry> = buildList {
    val isAvailable = media.isAvailable
    val isPending = media.isPending
    val tmdbId = media.tmdbId ?: media.id

    // Go To - navigate to detail page
    add(
        SeerrActionItem(
            id = "goto",
            text = "View Details",
            icon = Icons.Default.Info,
            iconTint = SeerrActionColors.GoTo,
            onClick = { actions.onGoTo(media.mediaType, tmdbId) },
        ),
    )

    add(SeerrActionDivider)

    // Request action
    when {
        isAvailable -> {
            // Already available in library
            add(
                SeerrActionItem(
                    id = "request",
                    text = "Already Available",
                    icon = Icons.Default.Add,
                    iconTint = SeerrActionColors.Available,
                    enabled = false,
                    onClick = {},
                ),
            )
        }
        isPending -> {
            // Already requested
            add(
                SeerrActionItem(
                    id = "request",
                    text = "Already Requested",
                    icon = Icons.Default.Schedule,
                    iconTint = SeerrActionColors.Pending,
                    enabled = false,
                    onClick = {},
                ),
            )
        }
        else -> {
            // Can request
            add(
                SeerrActionItem(
                    id = "request",
                    text = if (media.isMovie) "Request Movie" else "Request TV Show",
                    icon = Icons.Default.Add,
                    iconTint = SeerrActionColors.Request,
                    onClick = { actions.onRequest(media) },
                ),
            )
        }
    }

    add(SeerrActionDivider)

    // Watchlist toggle
    if (isOnWatchlist) {
        add(
            SeerrActionItem(
                id = "watchlist",
                text = "Remove from Watchlist",
                icon = Icons.Default.Bookmark,
                iconTint = SeerrActionColors.Watchlist,
                onClick = { actions.onRemoveFromWatchlist(media) },
            ),
        )
    } else {
        add(
            SeerrActionItem(
                id = "watchlist",
                text = "Add to Watchlist",
                icon = Icons.Default.BookmarkBorder,
                iconTint = SeerrActionColors.Watchlist,
                onClick = { actions.onAddToWatchlist(media) },
            ),
        )
    }
}
