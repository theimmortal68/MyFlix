package dev.jausc.myflix.tv.ui.components

import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.ui.ActionDivider
import dev.jausc.myflix.core.common.ui.ActionEntry
import dev.jausc.myflix.core.common.ui.ActionItem
import dev.jausc.myflix.core.common.ui.DetailActions
import dev.jausc.myflix.core.common.ui.HomeActions
import dev.jausc.myflix.core.common.ui.buildDetailActionItems
import dev.jausc.myflix.core.common.ui.buildHomeActionItems

/**
 * Type aliases for backward compatibility.
 * These map shared action types to TV-specific dialog types.
 */
typealias HomeDialogActions = HomeActions
typealias DetailDialogActions = DetailActions

/**
 * Convert shared ActionEntry list to TV-specific DialogItemEntry list.
 */
private fun List<ActionEntry>.toDialogItems(): List<DialogItemEntry> = map { entry ->
    when (entry) {
        is ActionDivider -> DialogItemDivider
        is ActionItem -> DialogItem(
            text = entry.text,
            icon = entry.icon,
            iconTint = entry.iconTint,
            onClick = entry.onClick,
        )
    }
}

/**
 * Build dialog items for an item on the home screen.
 *
 * @param item The JellyfinItem being acted upon
 * @param actions Callbacks for the various actions
 * @return List of DialogItemEntry for the dialog
 */
fun buildHomeDialogItems(item: JellyfinItem, actions: HomeDialogActions): List<DialogItemEntry> =
    buildHomeActionItems(item, actions).toDialogItems()

/**
 * Build dialog items for an episode on the detail screen.
 *
 * @param item The episode being acted upon
 * @param actions Callbacks for the various actions
 * @return List of DialogItemEntry for the dialog
 */
fun buildDetailDialogItems(item: JellyfinItem, actions: DetailDialogActions): List<DialogItemEntry> =
    buildDetailActionItems(item, actions).toDialogItems()
