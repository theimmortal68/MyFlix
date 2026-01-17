@file:Suppress(
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Height of the navigation bar including padding.
 * Use this constant to offset content below the nav bar.
 */
const val NAV_BAR_HEIGHT_DP = 30

/**
 * Always-visible Netflix-style top navigation bar.
 * Settings icon on left, main nav centered, Search icon on right.
 *
 * @param selectedItem Currently selected navigation item
 * @param onItemSelected Callback when a nav item is clicked
 * @param modifier Modifier for the nav bar
 * @param showUniverses Whether to show the Universes nav item
 * @param contentFocusRequester FocusRequester for the main content below the nav bar
 * @param focusRequester FocusRequester for the navigation bar itself
 */
@Composable
fun TopNavigationBarPopup(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    showUniverses: Boolean = false,
    contentFocusRequester: FocusRequester? = null,
    focusRequester: FocusRequester? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NAV_BAR_HEIGHT_DP.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.8f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(start = 24.dp, top = 8.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        // Settings
        NavIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            isSelected = selectedItem == NavItem.SETTINGS,
            onClick = { onItemSelected(NavItem.SETTINGS) },
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            ),
        )

        // Main nav items: Home, TV Shows, Movies, Collections, Universes (if enabled), Discover
        val navItems = buildList {
            add(NavItem.HOME)
            add(NavItem.SHOWS)
            add(NavItem.MOVIES)
            add(NavItem.COLLECTIONS)
            if (showUniverses) add(NavItem.UNIVERSES)
            add(NavItem.DISCOVER)
        }
        navItems.forEach { item ->
            NavTabButton(
                item = item,
                isSelected = selectedItem == item,
                onClick = { onItemSelected(item) },
                contentFocusRequester = contentFocusRequester,
            )
        }

        // Search
        NavIconButton(
            icon = Icons.Default.Search,
            contentDescription = "Search",
            isSelected = selectedItem == NavItem.SEARCH,
            onClick = { onItemSelected(NavItem.SEARCH) },
            contentFocusRequester = contentFocusRequester,
        )
    }
}

@Composable
private fun NavIconButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .size(20.dp)
            .focusProperties {
                if (contentFocusRequester != null) {
                    down = contentFocusRequester
                }
                up = FocusRequester.Cancel
            },
        contentPadding = PaddingValues(0.dp),
        scale = ButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
        ),
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) {
                TvColors.BluePrimary.copy(alpha = 0.3f)
            } else {
                Color.Transparent
            },
            contentColor = if (isSelected) {
                TvColors.BluePrimary
            } else {
                TvColors.TextPrimary.copy(alpha = 0.8f)
            },
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun NavTabButton(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentFocusRequester: FocusRequester? = null,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(20.dp)
            .focusProperties {
                if (contentFocusRequester != null) {
                    down = contentFocusRequester
                }
                up = FocusRequester.Cancel
            },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
        ),
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) {
                TvColors.BluePrimary.copy(alpha = 0.3f)
            } else {
                Color.Transparent
            },
            contentColor = if (isSelected) {
                TvColors.BluePrimary
            } else {
                TvColors.TextPrimary.copy(alpha = 0.8f)
            },
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Text(
            text = when (item) {
                NavItem.HOME -> "Home"
                NavItem.MOVIES -> "Movies"
                NavItem.SHOWS -> "TV Shows"
                NavItem.SEARCH -> "Search"
                NavItem.DISCOVER -> "Discover"
                NavItem.SETTINGS -> "Settings"
                NavItem.COLLECTIONS -> "Collections"
                NavItem.UNIVERSES -> "Universes"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
