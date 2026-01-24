package dev.jausc.myflix.core.common.ui

import androidx.compose.ui.graphics.Color

/**
 * Shared icon colors for action buttons across TV and mobile apps.
 * Uses Tailwind CSS color palette for consistency.
 */
object IconColors {
    // Primary actions
    val Play = Color(0xFF22C55E) // Green-500
    val Resume = Color(0xFF3B82F6) // Blue-500
    val Restart = Color(0xFFF97316) // Orange-500
    val Shuffle = Color(0xFFA855F7) // Purple-500

    // Media state
    val Watched = Color(0xFF06B6D4) // Cyan-500
    val Favorite = Color(0xFF94A3B8) // Slate-400 (unfilled)
    val FavoriteFilled = Color(0xFFEF4444) // Red-500

    // Content actions
    val Trailer = Color(0xFFFBBF24) // Amber-400
    val MediaInfo = Color(0xFF3B82F6) // Blue-500
    val Playlist = Color(0xFF8B5CF6) // Violet-500

    // Navigation
    val Navigation = Color(0xFF34D399) // Emerald-400
    val GoToSeries = Color(0xFF34D399) // Emerald-400

    // Utility
    val More = Color(0xFF94A3B8) // Slate-400
}
