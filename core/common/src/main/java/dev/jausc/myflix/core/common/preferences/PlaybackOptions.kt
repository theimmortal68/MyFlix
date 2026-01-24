package dev.jausc.myflix.core.common.preferences

/**
 * Shared playback option constants used across TV and Mobile apps.
 * Centralizes bitrate and refresh rate options for consistency.
 */
object PlaybackOptions {
    /**
     * Streaming bitrate options (Mbps to display label).
     * 0 = no limit (direct play).
     */
    val BITRATE_OPTIONS: List<Pair<Int, String>> = listOf(
        0 to "Maximum (Direct Play)",
        120 to "120 Mbps (4K)",
        80 to "80 Mbps",
        60 to "60 Mbps",
        40 to "40 Mbps (1080p)",
        25 to "25 Mbps",
        15 to "15 Mbps (720p)",
        10 to "10 Mbps",
        8 to "8 Mbps",
        5 to "5 Mbps",
        3 to "3 Mbps (480p)",
        2 to "2 Mbps",
        1 to "1 Mbps",
    )

    /**
     * Refresh rate mode options (mode key to display label).
     */
    val REFRESH_RATE_MODE_OPTIONS: List<Pair<String, String>> = listOf(
        "OFF" to "Off (system default)",
        "AUTO" to "Auto (match video)",
        "60" to "60 Hz",
        "120" to "120 Hz",
    )

    /**
     * Get display label for a bitrate value.
     * @param bitrateMbps Bitrate in Mbps (0 = direct play)
     * @return Human-readable label or the Mbps value if not found
     */
    fun getBitrateLabel(bitrateMbps: Int): String {
        return BITRATE_OPTIONS.find { it.first == bitrateMbps }?.second
            ?: if (bitrateMbps == 0) "Maximum" else "$bitrateMbps Mbps"
    }

    /**
     * Get display label for a refresh rate mode.
     * @param mode Mode key (OFF, AUTO, 60, 120)
     * @return Human-readable label or the mode key if not found
     */
    fun getRefreshRateModeLabel(mode: String): String =
        REFRESH_RATE_MODE_OPTIONS.find { it.first == mode }?.second ?: mode
}
