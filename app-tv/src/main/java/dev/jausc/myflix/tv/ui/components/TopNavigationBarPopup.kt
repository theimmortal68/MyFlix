@file:Suppress(
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
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
        ExpandableNavButton(
            text = "Settings",
            icon = Icons.Outlined.Settings,
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
            val (icon, title) = when (item) {
                NavItem.HOME -> Icons.Outlined.Home to "Home"
                NavItem.MOVIES -> Icons.Outlined.Movie to "Movies"
                NavItem.SHOWS -> Icons.Outlined.Tv to "TV Shows"
                NavItem.COLLECTIONS -> Icons.Outlined.VideoLibrary to "Collections"
                NavItem.UNIVERSES -> Icons.Outlined.Hub to "Universes"
                NavItem.DISCOVER -> Icons.Outlined.Explore to "Discover"
                else -> Icons.Outlined.Home to "Home" // Fallback
            }
            
            ExpandableNavButton(
                text = title,
                icon = icon,
                isSelected = selectedItem == item,
                onClick = { onItemSelected(item) },
                contentFocusRequester = contentFocusRequester,
            )
        }

        // Search
        ExpandableNavButton(
            text = "Search",
            icon = Icons.Outlined.Search,
            isSelected = selectedItem == NavItem.SEARCH,
            onClick = { onItemSelected(NavItem.SEARCH) },
            contentFocusRequester = contentFocusRequester,
        )
    }
}

@Composable
private fun ExpandableNavButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        modifier = modifier
            .height(20.dp)
            .focusProperties {
                if (contentFocusRequester != null) {
                    down = contentFocusRequester
                }
                up = FocusRequester.Cancel
            },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        interactionSource = interactionSource,
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
            contentDescription = text,
            modifier = Modifier.size(14.dp),
        )
        AnimatedVisibility(visible = isFocused) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
