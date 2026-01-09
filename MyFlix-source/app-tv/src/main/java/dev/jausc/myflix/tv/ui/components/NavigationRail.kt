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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Navigation items for the rail
 */
enum class NavItem(
    val icon: ImageVector,
    val label: String,
    val route: String,
    val color: Color
) {
    HOME(Icons.Outlined.Home, "Home", "home", Color(0xFF60A5FA)),           // Blue
    SEARCH(Icons.Outlined.Search, "Search", "search", Color(0xFFA78BFA)),    // Purple
    SHOWS(Icons.Outlined.Tv, "Shows", "shows", Color(0xFF34D399)),           // Green
    MOVIES(Icons.Outlined.Movie, "Movies", "movies", Color(0xFFFBBF24)),     // Yellow/Gold
    SETTINGS(Icons.Outlined.Settings, "Settings", "settings", Color(0xFFF472B6)) // Pink
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(56.dp)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
    ) {
        // Spacer at top for visual balance
        Spacer(modifier = Modifier.height(32.dp))
        
        NavItem.entries.forEach { item ->
            NavRailItem(
                item = item,
                isSelected = item == selectedItem,
                onClick = { onItemSelected(item) }
            )
        }
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Animation for the halo effect
    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "haloAlpha"
    )
    
    val haloScale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.5f,
        animationSpec = tween(durationMillis = 200),
        label = "haloScale"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "iconScale"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && 
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Halo effect (subtle blurred white circle behind the icon)
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(haloScale)
                .alpha(haloAlpha * 0.3f)
                .blur(8.dp)
                .clip(CircleShape)
                .background(HaloColor)
        )
        
        // Secondary inner glow
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(haloScale)
                .alpha(haloAlpha * 0.2f)
                .blur(4.dp)
                .clip(CircleShape)
                .background(HaloColor)
        )
        
        // Selection indicator (subtle ring when selected but not focused)
        if (isSelected && !isFocused) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.15f))
            )
        }
        
        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(28.dp)
                .scale(iconScale),
            tint = if (isFocused || isSelected) item.color else TvColors.TextSecondary
        )
    }
}

/**
 * Compact nav rail item showing label on focus
 */
@Composable
fun NavRailItemWithLabel(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "haloAlpha"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "iconScale"
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
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // Halo effect
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .alpha(haloAlpha)
                    .blur(10.dp)
                    .clip(CircleShape)
                    .background(item.color)
            )
            
            // Selection indicator
            if (isSelected && !isFocused) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(item.color.copy(alpha = 0.12f))
                )
            }
            
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier
                    .size(26.dp)
                    .scale(iconScale),
                tint = if (isFocused || isSelected) item.color else TvColors.TextSecondary
            )
        }
        
        // Label appears on focus
        if (isFocused) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = item.color
            )
        }
    }
}
