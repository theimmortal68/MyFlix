package dev.jausc.myflix.tv.ui.components.navrail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import dev.jausc.myflix.tv.ui.components.NavItem

/**
 * Navigation rail that only receives focus through explicit activation.
 *
 * Unlike auto-expanding rails, this component uses an explicit activation model:
 * - When inactive (isActive=false), items cannot receive focus via D-pad
 * - Activation happens only via Menu key or left-edge navigation (FocusSentinel)
 * - This prevents focus theft during screen loads
 *
 * @param selectedItem Currently selected navigation destination
 * @param onItemSelected Called when user selects an item (navigates)
 * @param isActive Whether rail can receive focus (controlled by parent)
 * @param isExpanded Whether rail is visually expanded
 * @param onExpandedChange Called to change expansion state
 * @param onDeactivate Called when rail should become inactive (user exits)
 * @param showUniverses Whether to show Universes nav item
 * @param showDiscover Whether to show Discover nav item
 * @param modifier Modifier for the rail
 * @param firstItemFocusRequester FocusRequester for the first focusable item (for focus transfer)
 * @param exitFocusRequester FocusRequester to focus when exiting rail to the right
 */
@Composable
fun NavigationRail(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    isActive: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDeactivate: () -> Unit,
    showUniverses: Boolean = false,
    showDiscover: Boolean = false,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester = remember { FocusRequester() },
    exitFocusRequester: FocusRequester = FocusRequester.Default,
) {
    val mainItems = remember(showUniverses, showDiscover) {
        buildMainNavItems(showUniverses, showDiscover)
    }

    // Find the index of the selected item to assign firstItemFocusRequester
    val selectedIndex = mainItems.indexOf(selectedItem)
    val isSettingsSelected = selectedItem == NavItem.SETTINGS

    // Assign firstItemFocusRequester to the selected item so it gets focus when rail activates
    val mainFocusRequesters = remember(mainItems.size, selectedIndex) {
        List(mainItems.size) { index ->
            if (index == selectedIndex) firstItemFocusRequester else FocusRequester()
        }
    }
    // Settings gets firstItemFocusRequester if it's the selected item
    val settingsFocusRequester = remember(isSettingsSelected) {
        if (isSettingsSelected) firstItemFocusRequester else FocusRequester()
    }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) {
            NavRailDimensions.ExpandedWidth
        } else {
            NavRailDimensions.CollapsedWidth
        },
        animationSpec = tween(
            durationMillis = if (isExpanded) {
                NavRailAnimations.ExpandDurationMs
            } else {
                NavRailAnimations.CollapseDurationMs
            }
        ),
        label = "railWidth",
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isExpanded) NavRailAlpha.ExpandedBackground else 0f,
        animationSpec = tween(
            durationMillis = if (isExpanded) {
                NavRailAnimations.ExpandDurationMs
            } else {
                NavRailAnimations.CollapseDurationMs
            }
        ),
        label = "backgroundAlpha",
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = NavRailAnimations.ExpandDurationMs,
            delayMillis = if (isExpanded) NavRailAnimations.LabelFadeDelayMs else 0,
        ),
        label = "labelAlpha",
    )

    // Handle Back key to collapse/deactivate
    val handleKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Back && isExpanded) {
            onExpandedChange(false)
            onDeactivate()
            true
        } else {
            false
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .background(createRailGradient(backgroundAlpha))
            .padding(vertical = NavRailDimensions.VerticalPadding)
            .focusGroup()
            .onKeyEvent(handleKeyEvent),
    ) {
        // Main navigation items (centered)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = NavRailDimensions.SettingsSpaceReservation),
            verticalArrangement = Arrangement.spacedBy(
                NavRailDimensions.ItemSpacing,
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.Start,
        ) {
            mainItems.forEachIndexed { index, item ->
                NavRailItem(
                    item = item,
                    isSelected = selectedItem == item,
                    isExpanded = isExpanded,
                    isActive = isActive,
                    labelAlpha = labelAlpha,
                    onClick = {
                        onItemSelected(item)
                        onExpandedChange(false)
                        onDeactivate()
                    },
                    onExitRight = {
                        onExpandedChange(false)
                        onDeactivate()
                    },
                    focusRequester = mainFocusRequesters[index],
                    upFocusRequester = mainFocusRequesters.getOrNull(index - 1)
                        ?: FocusRequester.Cancel,
                    downFocusRequester = mainFocusRequesters.getOrNull(index + 1)
                        ?: settingsFocusRequester,
                    rightFocusRequester = exitFocusRequester,
                )
            }
        }

        // Settings item (bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = NavRailDimensions.SettingsBottomPadding),
        ) {
            NavRailItem(
                item = NavItem.SETTINGS,
                isSelected = selectedItem == NavItem.SETTINGS,
                isExpanded = isExpanded,
                isActive = isActive,
                labelAlpha = labelAlpha,
                onClick = {
                    onItemSelected(NavItem.SETTINGS)
                    onExpandedChange(false)
                    onDeactivate()
                },
                onExitRight = {
                    onExpandedChange(false)
                    onDeactivate()
                },
                focusRequester = settingsFocusRequester,
                upFocusRequester = mainFocusRequesters.lastOrNull() ?: FocusRequester.Cancel,
                downFocusRequester = FocusRequester.Cancel,
                rightFocusRequester = exitFocusRequester,
            )
        }
    }
}

private fun buildMainNavItems(showUniverses: Boolean, showDiscover: Boolean): List<NavItem> {
    return buildList {
        add(NavItem.SEARCH)
        add(NavItem.HOME)
        add(NavItem.SHOWS)
        add(NavItem.MOVIES)
        add(NavItem.COLLECTIONS)
        if (showUniverses) add(NavItem.UNIVERSES)
        if (showDiscover) add(NavItem.DISCOVER)
    }
}

private fun createRailGradient(backgroundAlpha: Float): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            NavRailBackgroundColor.copy(alpha = backgroundAlpha),
            NavRailBackgroundColor.copy(alpha = backgroundAlpha * NavRailAlpha.ExpandedBackgroundMid),
            NavRailBackgroundColor.copy(alpha = backgroundAlpha * NavRailAlpha.ExpandedBackgroundEdge),
            Color.Transparent,
        ),
    )
}
