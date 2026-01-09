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
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Netflix-style top navigation bar.
 * Search icon on left, main nav centered, Settings icon on right.
 */
@Composable
fun TopNavigationBar(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    homeButtonFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
    ) {
        // Left: Search icon button
        NavIconButton(
            icon = Icons.Default.Search,
            contentDescription = "Search",
            isSelected = selectedItem == NavItem.SEARCH,
            onClick = { onItemSelected(NavItem.SEARCH) },
            focusRequester = firstItemFocusRequester,
            downFocusRequester = downFocusRequester,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        
        // Center: Home, TV Shows, Movies, Collections, Universes
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Center)
        ) {
            listOf(
                NavItem.HOME,
                NavItem.SHOWS,
                NavItem.MOVIES,
                NavItem.COLLECTIONS,
                NavItem.UNIVERSES
            ).forEach { item ->
                NavTabButton(
                    item = item,
                    isSelected = selectedItem == item,
                    onClick = { onItemSelected(item) },
                    focusRequester = if (item == NavItem.HOME) homeButtonFocusRequester else null,
                    downFocusRequester = downFocusRequester
                )
            }
        }
        
        // Right: Settings icon button
        NavIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            isSelected = selectedItem == NavItem.SETTINGS,
            onClick = { onItemSelected(NavItem.SETTINGS) },
            downFocusRequester = downFocusRequester,
            modifier = Modifier.align(Alignment.CenterEnd)
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
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null
) {
    // Circular icon button - 20dp size, no scale on focus
    Button(
        onClick = onClick,
        modifier = modifier
            .size(20.dp) // Square for circular shape
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else Modifier
            )
            .focusProperties {
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
                up = FocusRequester.Cancel
            },
        contentPadding = PaddingValues(0.dp),
        scale = ButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f // No scale change on focus
        ),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun NavTabButton(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null
) {
    // 20dp height, no scale on focus
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(20.dp)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else Modifier
            )
            .focusProperties {
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
                up = FocusRequester.Cancel
            },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f // No scale change on focus
        ),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White
        )
    ) {
        Text(
            text = when (item) {
                NavItem.HOME -> "Home"
                NavItem.MOVIES -> "Movies"
                NavItem.SHOWS -> "TV Shows"
                NavItem.SEARCH -> "Search"
                NavItem.SETTINGS -> "Settings"
                NavItem.COLLECTIONS -> "Collections"
                NavItem.UNIVERSES -> "Universes"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
