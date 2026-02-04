@file:Suppress("MagicNumber")

package dev.jausc.myflix.core.common.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Available theme presets for MyFlix.
 */
enum class ThemePreset(val displayName: String) {
    DEFAULT("Default"),
    OLED_DARK("OLED Dark"),
    HIGH_CONTRAST("High Contrast"),
    ;

    companion object {
        fun fromName(name: String): ThemePreset =
            entries.find { it.name == name } ?: DEFAULT
    }
}

/**
 * Color palette for a theme preset.
 * Contains all semantic colors needed by the app.
 */
data class ThemeColors(
    // Primary brand colors
    val primary: Color,
    val primaryVariant: Color,
    val primaryLight: Color,
    val primaryAccent: Color,

    // Background colors
    val background: Color,
    val surface: Color,
    val surfaceLight: Color,
    val surfaceElevated: Color,
    val cardBackground: Color,

    // Text colors
    val textPrimary: Color,
    val textSecondary: Color,

    // UI element colors
    val focusRing: Color,
    val focusedSurface: Color,

    // Semantic colors
    val success: Color,
    val error: Color,
)

/**
 * Theme color definitions for all presets.
 */
object ThemeColorSchemes {

    /**
     * Default theme - Blue accent on dark gray.
     * The original MyFlix theme.
     */
    val Default = ThemeColors(
        primary = Color(0xFF2563EB),
        primaryVariant = Color(0xFF1D4ED8),
        primaryLight = Color(0xFF3B82F6),
        primaryAccent = Color(0xFF60A5FA),

        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceLight = Color(0xFF1A1A1A),
        surfaceElevated = Color(0xFF202020),
        cardBackground = Color(0xFF121212),

        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),

        focusRing = Color(0xFF60A5FA),
        focusedSurface = Color(0xFF1A1A1A),

        success = Color(0xFF22C55E),
        error = Color(0xFFEF4444),
    )

    /**
     * OLED Dark theme - Pure black background for OLED displays.
     * Maximizes contrast and saves battery on OLED screens.
     * Uses slightly muted accent colors to reduce eye strain.
     */
    val OledDark = ThemeColors(
        primary = Color(0xFF3B82F6),
        primaryVariant = Color(0xFF2563EB),
        primaryLight = Color(0xFF60A5FA),
        primaryAccent = Color(0xFF93C5FD),

        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceLight = Color(0xFF0A0A0A),
        surfaceElevated = Color(0xFF121212),
        cardBackground = Color(0xFF0A0A0A),

        textPrimary = Color(0xFFE2E8F0),
        textSecondary = Color(0xFF64748B),

        focusRing = Color(0xFF93C5FD),
        focusedSurface = Color(0xFF0A0A0A),

        success = Color(0xFF22C55E),
        error = Color(0xFFEF4444),
    )

    /**
     * High Contrast theme - Enhanced visibility for accessibility.
     * Brighter colors and higher contrast ratios.
     */
    val HighContrast = ThemeColors(
        primary = Color(0xFF60A5FA),
        primaryVariant = Color(0xFF3B82F6),
        primaryLight = Color(0xFF93C5FD),
        primaryAccent = Color(0xFFBFDBFE),

        background = Color(0xFF000000),
        surface = Color(0xFF0F0F0F),
        surfaceLight = Color(0xFF1F1F1F),
        surfaceElevated = Color(0xFF2A2A2A),
        cardBackground = Color(0xFF0F0F0F),

        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFD1D5DB),

        focusRing = Color(0xFFFFFFFF),
        focusedSurface = Color(0xFF1F1F1F),

        success = Color(0xFF4ADE80),
        error = Color(0xFFF87171),
    )

    /**
     * Get the color scheme for a theme preset.
     */
    fun forPreset(preset: ThemePreset): ThemeColors = when (preset) {
        ThemePreset.DEFAULT -> Default
        ThemePreset.OLED_DARK -> OledDark
        ThemePreset.HIGH_CONTRAST -> HighContrast
    }
}
