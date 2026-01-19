@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.core.common.R

/**
 * Navigation items for the rail
 */
enum class NavItem(
    val iconRes: Int,
    val label: String,
    val route: String,
    val color: Color,
) {
    HOME(R.drawable.ic_nav_home_color, "Home", "home", Color(0xFF60A5FA)), // Blue
    SEARCH(R.drawable.ic_nav_search_color, "Search", "search", Color(0xFFA78BFA)), // Purple
    SHOWS(R.drawable.ic_nav_shows_color, "Shows", "shows", Color(0xFF34D399)), // Green
    MOVIES(R.drawable.ic_nav_movies_color, "Movies", "movies", Color(0xFFFBBF24)), // Yellow/Gold
    DISCOVER(R.drawable.ic_nav_discover_color, "Discover", "seerr", Color(0xFF8B5CF6)), // Violet (Seerr)
    COLLECTIONS(R.drawable.ic_nav_collections_color, "Collections", "collections", Color(0xFFFF7043)), // Orange
    UNIVERSES(R.drawable.ic_nav_universes_color, "Universes", "universes", Color(0xFF9575CD)), // Deep Purple
    SETTINGS(R.drawable.ic_nav_settings_color, "Settings", "settings", Color(0xFFF472B6)), // Pink
}

// White halo color for focus effect
private val HaloColor = Color.White

/**
 * Left navigation rail with Material Outlined icons and halo focus effect.
 */
@Composable
fun NavigationRail(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    onFocusExitRight: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(56.dp)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
    ) {
        // Spacer at top for visual balance
        Spacer(modifier = Modifier.height(32.dp))

        NavItem.entries.forEach { item ->
            NavRailItem(
                item = item,
                isSelected = item == selectedItem,
                onClick = { onItemSelected(item) },
                onFocusExitRight = onFocusExitRight,
            )
        }
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocusExitRight: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }

    // Animation for the halo effect
    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "haloAlpha",
    )

    val haloScale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.5f,
        animationSpec = tween(durationMillis = 200),
        label = "haloScale",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "iconScale",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                // Call callback BEFORE focus moves right
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onFocusExitRight()
                }
                false // Don't consume - let focus move normally
            }
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
        // Halo effect (subtle blurred white circle behind the icon)
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(haloScale)
                .alpha(haloAlpha * 0.3f)
                .blur(8.dp)
                .clip(CircleShape)
                .background(HaloColor),
        )

        // Secondary inner glow
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(haloScale)
                .alpha(haloAlpha * 0.2f)
                .blur(4.dp)
                .clip(CircleShape)
                .background(HaloColor),
        )

        // Selection indicator (subtle ring when selected but not focused)
        if (isSelected && !isFocused) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.15f)),
            )
        }

        // Icon
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.label,
            modifier = Modifier
                .size(28.dp)
                .scale(iconScale),
            tint = if (isFocused || isSelected) Color.Unspecified else TvColors.TextSecondary,
        )
    }
}

/**
 * Compact nav rail item showing label on focus
 */
@Composable
fun NavRailItemWithLabel(item: NavItem, isSelected: Boolean, onClick: () -> Unit,) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "haloAlpha",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "iconScale",
    )

    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Halo effect
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .alpha(haloAlpha)
                    .blur(10.dp)
                    .clip(CircleShape)
                    .background(item.color),
            )

            // Selection indicator
            if (isSelected && !isFocused) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(item.color.copy(alpha = 0.12f)),
                )
            }

            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.label,
                modifier = Modifier
                    .size(26.dp)
                    .scale(iconScale),
                tint = if (isFocused || isSelected) Color.Unspecified else TvColors.TextSecondary,
            )
        }

        // Label appears on focus
        if (isFocused) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = item.color,
            )
        }
    }
}
