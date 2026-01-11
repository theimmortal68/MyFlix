package dev.jausc.myflix.core.player

/**
 * Shared player utility functions
 */
object PlayerUtils {
    /**
     * Format milliseconds to human-readable time string
     * e.g., 125000 -> "2:05" or 3725000 -> "1:02:05"
     */
    fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"

        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /**
     * Format milliseconds to compact time string with units
     * e.g., 125000 -> "2m 5s" or 3725000 -> "1h 2m"
     */
    fun formatTimeCompact(ms: Long): String {
        if (ms <= 0) return "0s"

        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Convert Jellyfin ticks to milliseconds.
     * @see PlayerConstants.ticksToMs
     */
    fun ticksToMs(ticks: Long): Long = PlayerConstants.ticksToMs(ticks)

    /**
     * Convert milliseconds to Jellyfin ticks.
     * @see PlayerConstants.msToTicks
     */
    fun msToTicks(ms: Long): Long = PlayerConstants.msToTicks(ms)

    /**
     * Calculate progress percentage (0.0 to 1.0)
     */
    fun calculateProgress(position: Long, duration: Long): Float {
        return if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}
