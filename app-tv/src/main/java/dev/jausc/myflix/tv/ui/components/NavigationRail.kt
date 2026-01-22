@file:Suppress(
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Width of the navigation rail.
 * Use this constant to offset content to the right of the rail.
 */
const val NAV_RAIL_WIDTH_DP = 32

/**
 * Navigation items for the rail
 */
enum class NavItem(
    val icon: ImageVector,
    val label: String,
    val route: String,
    val color: Color,
) {
    SEARCH(Icons.Outlined.Search, "Search", "search", Color(0xFFA78BFA)),
    HOME(Icons.Outlined.Home, "Home", "home", Color(0xFF60A5FA)),
    SHOWS(Icons.Outlined.Tv, "Shows", "shows", Color(0xFF34D399)),
    MOVIES(Icons.Outlined.Movie, "Movies", "movies", Color(0xFFFBBF24)),
    COLLECTIONS(Icons.Outlined.VideoLibrary, "Collections", "collections", Color(0xFFFF7043)),
    UNIVERSES(Icons.Outlined.Hub, "Universes", "universes", Color(0xFF9575CD)),
    DISCOVER(Icons.Outlined.Explore, "Discover", "seerr", Color(0xFF8B5CF6)),
    SETTINGS(Icons.Outlined.Settings, "Settings", "settings", Color(0xFFF472B6)),
}

/**
 * Left navigation rail with icon-only buttons.
 * Search at top, Settings at bottom, main nav items in between.
 *
 * @param selectedItem Currently selected navigation item
 * @param onItemSelected Callback when a nav item is clicked
 * @param showUniverses Whether to show the Universes nav item
 * @param showDiscover Whether to show the Discover nav item
 * @param contentFocusRequester FocusRequester for the main content to the right
 * @param modifier Modifier for the rail
 */
@Composable
fun NavigationRail(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    showUniverses: Boolean = false,
    showDiscover: Boolean = false,
    contentFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    // Focus requesters for explicit navigation across the spacer
    val searchFocusRequester = remember { FocusRequester() }
    val homeFocusRequester = remember { FocusRequester() }
    val showsFocusRequester = remember { FocusRequester() }
    val moviesFocusRequester = remember { FocusRequester() }
    val collectionsFocusRequester = remember { FocusRequester() }
    val universesFocusRequester = remember { FocusRequester() }
    val discoverFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }

    // Determine which item is above settings (after the spacer)
    val lastMainItem = when {
        showDiscover -> discoverFocusRequester
        showUniverses -> universesFocusRequester
        else -> collectionsFocusRequester
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(NAV_RAIL_WIDTH_DP.dp)
            .padding(vertical = 12.dp)
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Search at top
        NavRailItem(
            item = NavItem.SEARCH,
            isSelected = selectedItem == NavItem.SEARCH,
            onClick = { onItemSelected(NavItem.SEARCH) },
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.focusRequester(searchFocusRequester),
            upFocusRequester = FocusRequester.Cancel,
            downFocusRequester = homeFocusRequester,
        )

        // Main nav items
        NavRailItem(
            item = NavItem.HOME,
            isSelected = selectedItem == NavItem.HOME,
            onClick = { onItemSelected(NavItem.HOME) },
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.focusRequester(homeFocusRequester),
            upFocusRequester = searchFocusRequester,
            downFocusRequester = showsFocusRequester,
        )

        NavRailItem(
            item = NavItem.SHOWS,
            isSelected = selectedItem == NavItem.SHOWS,
            onClick = { onItemSelected(NavItem.SHOWS) },
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.focusRequester(showsFocusRequester),
            upFocusRequester = homeFocusRequester,
            downFocusRequester = moviesFocusRequester,
        )

        NavRailItem(
            item = NavItem.MOVIES,
            isSelected = selectedItem == NavItem.MOVIES,
            onClick = { onItemSelected(NavItem.MOVIES) },
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.focusRequester(moviesFocusRequester),
            upFocusRequester = showsFocusRequester,
            downFocusRequester = collectionsFocusRequester,
        )

        NavRailItem(
            item = NavItem.COLLECTIONS,
            isSelected = selectedItem == NavItem.COLLECTIONS,
            onClick = { onItemSelected(NavItem.COLLECTIONS) },
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.focusRequester(collectionsFocusRequester),
            upFocusRequester = moviesFocusRequester,
            downFocusRequester = if (showUniverses) universesFocusRequester
                else if (showDiscover) discoverFocusRequester
                else settingsFocusRequester,
        )

        if (showUniverses) {
            NavRailItem(
                item = NavItem.UNIVERSES,
                isSelected = selectedItem == NavItem.UNIVERSES,
                onClick = { onItemSelected(NavItem.UNIVERSES) },
                contentFocusRequester = contentFocusRequester,
                modifier = Modifier.focusRequester(universesFocusRequester),
                upFocusRequester = collectionsFocusRequester,
                downFocusRequester = if (showDiscover) discoverFocusRequester else settingsFocusRequester,
            )
        }

        if (showDiscover) {
            NavRailItem(
                item = NavItem.DISCOVER,
                isSelected = selectedItem == NavItem.DISCOVER,
                onClick = { onItemSelected(NavItem.DISCOVER) },
                contentFocusRequester = contentFocusRequester,
                modifier = Modifier.focusRequester(discoverFocusRequester),
                upFocusRequester = if (showUniverses) universesFocusRequester else collectionsFocusRequester,
                downFocusRequester = settingsFocusRequester,
            )
        }

        // Spacer pushes Settings to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Settings at bottom
        NavRailItem(
            item = NavItem.SETTINGS,
            isSelected = selectedItem == NavItem.SETTINGS,
            onClick = { onItemSelected(NavItem.SETTINGS) },
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.focusRequester(settingsFocusRequester),
            upFocusRequester = lastMainItem,
            downFocusRequester = FocusRequester.Cancel,
        )
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    // Animation for the halo effect
    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "haloAlpha",
    )

    val haloScale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.5f,
        animationSpec = tween(durationMillis = 200),
        label = "haloScale",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "iconScale",
    )

    Box(
        modifier = modifier
            .size(32.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusProperties {
                if (contentFocusRequester != null) {
                    right = contentFocusRequester
                }
                // Explicit vertical navigation to handle Spacer
                if (upFocusRequester != null) {
                    up = upFocusRequester
                }
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
                // Prevent focus from going off-screen
                left = FocusRequester.Cancel
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Halo effect (blurred circle behind the icon)
        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(haloScale)
                .alpha(haloAlpha * 0.4f)
                .blur(6.dp)
                .clip(CircleShape)
                .background(item.color),
        )

        // Secondary inner glow
        Box(
            modifier = Modifier
                .size(22.dp)
                .scale(haloScale)
                .alpha(haloAlpha * 0.3f)
                .blur(3.dp)
                .clip(CircleShape)
                .background(item.color),
        )

        // Selection indicator (subtle ring when selected but not focused)
        if (isSelected && !isFocused) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.2f)),
            )
        }

        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(16.dp)
                .scale(iconScale),
            tint = when {
                isFocused -> item.color
                isSelected -> item.color.copy(alpha = 0.9f)
                else -> TvColors.TextSecondary
            },
        )
    }
}
