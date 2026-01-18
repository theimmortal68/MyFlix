package dev.jausc.myflix.core.player

import android.graphics.Color

/**
 * Subtitle font size options.
 * Maps to specific sizes for ExoPlayer (sp) and MPV (points).
 */
enum class SubtitleFontSize(val label: String, val exoPlayerSp: Int, val mpvPoints: Int) {
    SMALL("Small", 14, 40),
    MEDIUM("Medium", 18, 55),
    LARGE("Large", 24, 70),
    EXTRA_LARGE("Extra Large", 32, 90),
    ;

    companion object {
        val DEFAULT = MEDIUM

        fun fromName(name: String): SubtitleFontSize =
            entries.find { it.name == name } ?: DEFAULT
    }
}

/**
 * Predefined subtitle colors for easy selection.
 */
enum class SubtitleColor(val label: String, val argb: Int) {
    WHITE("White", Color.WHITE),
    YELLOW("Yellow", Color.YELLOW),
    GREEN("Green", Color.GREEN),
    CYAN("Cyan", Color.CYAN),
    BLUE("Blue", 0xFF4FC3F7.toInt()),
    MAGENTA("Magenta", Color.MAGENTA),
    ;

    companion object {
        val DEFAULT = WHITE

        fun fromName(name: String): SubtitleColor =
            entries.find { it.name == name } ?: DEFAULT
    }
}

/**
 * Subtitle style configuration.
 * Used by both ExoPlayer and MPV backends.
 */
data class SubtitleStyle(
    val fontSize: SubtitleFontSize = SubtitleFontSize.DEFAULT,
    val fontColor: SubtitleColor = SubtitleColor.DEFAULT,
    val backgroundOpacity: Int = 75, // 0-100 percent
) {
    /**
     * Returns the background color with the configured opacity.
     * Used for ExoPlayer's CaptionStyleCompat.
     */
    val backgroundArgb: Int
        get() {
            val alpha = (backgroundOpacity * 255 / 100).coerceIn(0, 255)
            return (alpha shl 24) or 0x000000 // Black with configured alpha
        }

    /**
     * Returns the MPV-compatible color string (e.g., "#FFFFFF").
     */
    val mpvFontColor: String
        get() = String.format("#%06X", fontColor.argb and 0xFFFFFF)

    /**
     * Returns the MPV-compatible background color string with alpha (e.g., "#80000000").
     */
    val mpvBackgroundColor: String
        get() {
            val alpha = (backgroundOpacity * 255 / 100).coerceIn(0, 255)
            return String.format("#%02X000000", alpha)
        }

    companion object {
        val DEFAULT = SubtitleStyle()
    }
}
