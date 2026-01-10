package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.TvPreferences
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.NavigationRail
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Preferences/Settings screen with toggle options.
 * Layout matches HomeScreen with NavigationRail on left.
 */
@Composable
fun PreferencesScreen(
    preferences: TvPreferences,
    onNavigateHome: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateShows: () -> Unit
) {
    var selectedNavItem by remember { mutableStateOf(NavItem.SETTINGS) }

    val hideWatched by preferences.hideWatchedFromRecent.collectAsState()
    val useMpvPlayer by preferences.useMpvPlayer.collectAsState()
    
    val handleNavSelection: (NavItem) -> Unit = { item ->
        selectedNavItem = item
        when (item) {
            NavItem.HOME -> onNavigateHome()
            NavItem.SEARCH -> onNavigateSearch()
            NavItem.MOVIES -> onNavigateMovies()
            NavItem.SHOWS -> onNavigateShows()
            NavItem.COLLECTIONS -> { /* TODO: Navigate to collections */ }
            NavItem.UNIVERSES -> { /* TODO: Placeholder for future feature */ }
            NavItem.SETTINGS -> { /* Already here */ }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        // Left Navigation Rail
        NavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = handleNavSelection
        )
        
        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            PreferencesContent(
                hideWatched = hideWatched,
                onHideWatchedChanged = { preferences.setHideWatchedFromRecent(it) },
                useMpvPlayer = useMpvPlayer,
                onUseMpvPlayerChanged = { preferences.setUseMpvPlayer(it) }
            )
        }
    }
}

@Composable
private fun PreferencesContent(
    hideWatched: Boolean,
    onHideWatchedChanged: (Boolean) -> Unit,
    useMpvPlayer: Boolean,
    onUseMpvPlayerChanged: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pink accent bar (matches Settings nav item color)
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(
                                Color(0xFFF472B6),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TextPrimary
                    )
                }
            }
        }

        // Display Section
        item {
            PreferencesSection(title = "Display") {
                TogglePreferenceItem(
                    title = "Hide Watched from Recently Added",
                    description = "Don't show already watched items in Recently Added Movies, Shows, and Episodes rows",
                    icon = if (hideWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    iconTint = if (hideWatched) Color(0xFF34D399) else TvColors.TextSecondary,
                    checked = hideWatched,
                    onCheckedChange = onHideWatchedChanged
                )
            }
        }

        // Playback Section
        item {
            PreferencesSection(title = "Playback") {
                TogglePreferenceItem(
                    title = "Use MPV Player",
                    description = "Experimental. Enable MPV for better codec support. Falls back to ExoPlayer for Dolby Vision content.",
                    icon = Icons.Outlined.PlayCircle,
                    iconTint = if (useMpvPlayer) Color(0xFF9C27B0) else TvColors.TextSecondary,
                    checked = useMpvPlayer,
                    onCheckedChange = onUseMpvPlayerChanged
                )
            }
        }

        // Info section
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "MyFlix for Android TV",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary
            )
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PreferencesSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TvColors.TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TvColors.Surface),
            content = content
        )
    }
}

@Composable
private fun TogglePreferenceItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) TvColors.FocusedSurface else Color.Transparent
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onCheckedChange(!checked)
                    true
                } else {
                    false
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = iconTint
        )
        
        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) TvColors.TextPrimary else TvColors.TextPrimary.copy(alpha = 0.9f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Toggle switch
        Switch(
            checked = checked,
            onCheckedChange = null, // Handled by row click
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF34D399),
                checkedTrackColor = Color(0xFF34D399).copy(alpha = 0.5f),
                uncheckedThumbColor = TvColors.TextSecondary,
                uncheckedTrackColor = TvColors.TextSecondary.copy(alpha = 0.3f)
            )
        )
    }
}
