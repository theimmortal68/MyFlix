package dev.jausc.myflix.mobile.ui.components

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
 */
typealias HomeMenuActions = HomeActions
typealias DetailMenuActions = DetailActions

/**
 * Convert shared ActionEntry list to mobile-specific MenuItemEntry list.
 */
private fun List<ActionEntry>.toMenuItems(): List<MenuItemEntry> = map { entry ->
    when (entry) {
        is ActionDivider -> MenuItemDivider
        is ActionItem -> MenuItem(
            text = entry.text,
            icon = entry.icon,
            iconTint = entry.iconTint,
            onClick = entry.onClick,
        )
    }
}

/**
 * Build menu items for an item on the home screen.
 *
 * @param item The JellyfinItem being acted upon
 * @param actions Callbacks for the various actions
 * @return List of MenuItemEntry for the bottom sheet
 */
fun buildHomeMenuItems(item: JellyfinItem, actions: HomeMenuActions): List<MenuItemEntry> =
    buildHomeActionItems(item, actions).toMenuItems()

/**
 * Build menu items for an episode on the detail screen.
 *
 * @param item The episode being acted upon
 * @param actions Callbacks for the various actions
 * @return List of MenuItemEntry for the bottom sheet
 */
fun buildDetailMenuItems(item: JellyfinItem, actions: DetailMenuActions): List<MenuItemEntry> =
    buildDetailActionItems(item, actions).toMenuItems()
