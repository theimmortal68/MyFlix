package dev.jausc.myflix.tv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation items for the TV navigation rail.
 *
 * Each item has an icon, label, route, and accent color for focused/selected states.
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
