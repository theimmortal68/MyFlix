package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/**
 * Configuration constants for the NavRailScaffold.
 */
object NavRailScaffoldConfig {
    const val ScrimAlphaExpanded = 0.5f
    const val ScrimAnimationDurationMs = 300
    const val CollapseDelayAfterNavigationMs = 400L
    const val ScrimZIndex = 0.5f
    const val NavRailZIndex = 1f
}

/**
 * Scaffold that provides a slide-out NavigationRail overlay with content dimming.
 *
 * The NavRail overlays content on the left side. Content receives left padding
 * equal to [SlideOutNavRailDimensions.CollapsedWidth] to avoid being obscured
 * by the collapsed rail icons.
 *
 * When the NavRail expands, a semi-transparent scrim dims the content behind it.
 *
 * @param currentRoute The current navigation route (used to auto-collapse after navigation)
 * @param selectedNavItem Currently selected navigation item
 * @param onNavItemSelected Callback when user selects a nav item
 * @param showUniverses Whether to show Universes in nav
 * @param showDiscover Whether to show Discover in nav
 * @param navRailFocusRequester FocusRequester to request focus on the NavRail
 * @param modifier Modifier for the scaffold
 * @param content The screen content. Receives a Modifier with appropriate left padding.
 */
@Composable
fun NavRailScaffold(
    currentRoute: String?,
    selectedNavItem: NavItem,
    onNavItemSelected: (NavItem) -> Unit,
    showUniverses: Boolean = false,
    showDiscover: Boolean = false,
    navRailFocusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier,
    content: @Composable (contentPadding: Modifier) -> Unit,
) {
    var isNavRailExpanded by remember { mutableStateOf(false) }

    // Track route changes to auto-collapse after navigation
    var previousRoute by remember { mutableStateOf(currentRoute) }

    // When route changes (navigation occurred), briefly keep expanded then collapse
    LaunchedEffect(currentRoute) {
        if (currentRoute != previousRoute && previousRoute != null && isNavRailExpanded) {
            // Brief delay to show the selection before collapsing
            delay(NavRailScaffoldConfig.CollapseDelayAfterNavigationMs)
            isNavRailExpanded = false
        }
        previousRoute = currentRoute
    }

    // Animate scrim alpha
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isNavRailExpanded) NavRailScaffoldConfig.ScrimAlphaExpanded else 0f,
        animationSpec = tween(durationMillis = NavRailScaffoldConfig.ScrimAnimationDurationMs),
        label = "scrimAlpha",
    )

    // Callback to collapse rail and return focus to content
    val collapseAndFocusContent: () -> Unit = {
        isNavRailExpanded = false
        // Focus will naturally return to content when NavRail loses focus
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                // Handle Menu key press from anywhere to toggle NavRail
                if (event.type == KeyEventType.KeyDown && event.key == Key.Menu) {
                    isNavRailExpanded = !isNavRailExpanded
                    if (isNavRailExpanded) {
                        navRailFocusRequester.requestFocus()
                    }
                    true
                } else {
                    false
                }
            },
    ) {
        // Content layer (behind NavRail and scrim)
        content(
            Modifier.padding(start = SlideOutNavRailDimensions.CollapsedWidth)
        )

        // Scrim layer (between content and NavRail) - only render when visible
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(NavRailScaffoldConfig.ScrimZIndex)
                    .background(Color.Black.copy(alpha = scrimAlpha)),
            )
        }

        // NavRail layer (on top)
        SlideOutNavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected,
            showUniverses = showUniverses,
            showDiscover = showDiscover,
            isExpanded = isNavRailExpanded,
            onExpandedChange = { isNavRailExpanded = it },
            onCollapseAndFocusContent = collapseAndFocusContent,
            modifier = Modifier
                .zIndex(NavRailScaffoldConfig.NavRailZIndex)
                .focusRequester(navRailFocusRequester),
        )
    }
}

/**
 * Determines which NavItem corresponds to the current route.
 *
 * @param route The current navigation route
 * @param collectionType Optional collection type for library routes
 * @param showUniverses Whether Universes nav item is visible
 * @param showDiscover Whether Discover nav item is visible
 * @return The appropriate NavItem for the route
 */
fun getNavItemForRoute(
    route: String?,
    collectionType: String? = null,
    showUniverses: Boolean = false,
    showDiscover: Boolean = false,
): NavItem {
    return when {
        route == null -> {
            NavItem.HOME
        }
        route.startsWith("home") -> {
            NavItem.HOME
        }
        route.startsWith("search") -> {
            NavItem.SEARCH
        }
        route.startsWith("settings") || route.startsWith("preferences") -> {
            NavItem.SETTINGS
        }
        route.startsWith("seerr") -> {
            if (showDiscover) NavItem.DISCOVER else NavItem.HOME
        }
        route.startsWith("library") -> {
            // Use collection type to determine Movies vs Shows
            when {
                collectionType?.contains("movie", ignoreCase = true) == true -> NavItem.MOVIES
                collectionType?.contains("tvshow", ignoreCase = true) == true -> NavItem.SHOWS
                else -> NavItem.HOME
            }
        }
        route.startsWith("collections") || route.startsWith("collection/") -> {
            NavItem.COLLECTIONS
        }
        route.startsWith("universes") || route.startsWith("universeCollection/") -> {
            if (showUniverses) NavItem.UNIVERSES else NavItem.COLLECTIONS
        }
        // Detail and person screens should preserve the previous NavItem context
        // This is handled externally by the caller
        route.startsWith("detail/") || route.startsWith("person/") -> {
            NavItem.HOME
        }
        else -> {
            NavItem.HOME
        }
    }
}
