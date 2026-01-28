package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
 * **Focus Behavior:**
 * - NavRail is only accessible via left D-pad or Menu key
 * - Normal D-pad navigation from content won't enter the NavRail
 * - Right D-pad or Back from NavRail returns focus to content and collapses
 *
 * @param currentRoute The current navigation route (used to auto-collapse after navigation)
 * @param selectedNavItem Currently selected navigation item
 * @param onNavItemSelected Callback when user selects a nav item
 * @param showUniverses Whether to show Universes in nav
 * @param showDiscover Whether to show Discover in nav
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
    modifier: Modifier = Modifier,
    content: @Composable (contentPadding: Modifier) -> Unit,
) {
    var isNavRailExpanded by remember { mutableStateOf(false) }

    // Tracks whether NavRail access was explicitly requested (left D-pad or Menu)
    var navRailAccessRequested by remember { mutableStateOf(false) }

    // Track if NavRail currently has focus
    var navRailHasFocus by remember { mutableStateOf(false) }

    // FocusRequester for the first nav item (to request focus on it directly)
    val firstNavItemFocusRequester = remember { FocusRequester() }

    // FocusRequester for content (to return focus when exiting NavRail)
    val contentFocusRequester = remember { FocusRequester() }

    // Track route changes to auto-collapse after navigation
    var previousRoute by remember { mutableStateOf(currentRoute) }

    // When route changes (navigation occurred), briefly keep expanded then collapse
    LaunchedEffect(currentRoute) {
        if (currentRoute != previousRoute && previousRoute != null && isNavRailExpanded) {
            // Brief delay to show the selection before collapsing
            delay(NavRailScaffoldConfig.CollapseDelayAfterNavigationMs)
            isNavRailExpanded = false
            navRailAccessRequested = false
        }
        previousRoute = currentRoute
    }

    // If NavRail gets focus but access wasn't requested, kick focus back to content
    // This prevents accidental navigation into the NavRail
    LaunchedEffect(navRailHasFocus, navRailAccessRequested) {
        if (navRailHasFocus && !navRailAccessRequested) {
            // Focus entered NavRail unexpectedly - redirect to content
            contentFocusRequester.requestFocus()
        }
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
        navRailAccessRequested = false
        contentFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            // Only open NavRail when access not already granted
                            if (!navRailAccessRequested) {
                                navRailAccessRequested = true
                                isNavRailExpanded = true
                                firstNavItemFocusRequester.requestFocus()
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionRight -> {
                            // When in NavRail, exit back to content
                            if (navRailAccessRequested && navRailHasFocus) {
                                collapseAndFocusContent()
                                true
                            } else {
                                false
                            }
                        }
                        Key.Back -> {
                            // Back also exits NavRail
                            if (navRailAccessRequested && navRailHasFocus) {
                                collapseAndFocusContent()
                                true
                            } else {
                                false
                            }
                        }
                        Key.Menu -> {
                            // Toggle NavRail from anywhere
                            if (navRailAccessRequested) {
                                collapseAndFocusContent()
                            } else {
                                navRailAccessRequested = true
                                isNavRailExpanded = true
                                firstNavItemFocusRequester.requestFocus()
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        // Content layer (behind NavRail and scrim)
        // Wrapped in a focusable Box to catch focus even when content is empty/loading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = SlideOutNavRailDimensions.CollapsedWidth)
                .focusRequester(contentFocusRequester)
                .focusable(),
        ) {
            content(Modifier)
        }

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
            firstItemFocusRequester = firstNavItemFocusRequester,
            onExpandedChange = { expanded ->
                isNavRailExpanded = expanded
                if (!expanded) {
                    navRailAccessRequested = false
                }
            },
            onCollapseAndFocusContent = collapseAndFocusContent,
            onFocusChanged = { hasFocus ->
                navRailHasFocus = hasFocus
            },
            modifier = Modifier.zIndex(NavRailScaffoldConfig.NavRailZIndex),
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
