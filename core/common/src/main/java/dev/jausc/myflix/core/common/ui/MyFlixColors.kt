package dev.jausc.myflix.core.common.ui

import androidx.compose.ui.graphics.Color

/**
 * MyFlix brand colors shared across all app modules.
 * These are the core brand colors used for consistent theming.
 */
object MyFlixColors {
    // Primary blue palette
    val BluePrimary = Color(0xFF2563EB)
    val BlueLight = Color(0xFF60A5FA)
    val BlueAccent = Color(0xFF93C5FD)
    val BlueDark = Color(0xFF1E40AF)

    // Neutrals
    val Background = Color(0xFF0F0F0F)
    val Surface = Color(0xFF1A1A1A)
    val SurfaceVariant = Color(0xFF2A2A2A)

    // Text
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)

    // Semantic
    val Error = Color(0xFFEF4444)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
}
