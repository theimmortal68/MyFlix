package dev.jausc.myflix.core.common.util

/**
 * Constants for Jellyfin time conversions.
 * Jellyfin uses 10-million ticks per second (100 nanoseconds per tick).
 */
object TickConstants {
    /** Number of ticks per second (10 million) */
    const val TICKS_PER_SECOND = 10_000_000L

    /** Number of ticks per millisecond */
    const val TICKS_PER_MS = TICKS_PER_SECOND / 1000

    /** Number of ticks per minute (600 million) */
    const val TICKS_PER_MINUTE = TICKS_PER_SECOND * 60
}

/**
 * Utility functions for formatting time values.
 */
object TimeFormatUtil {
    /**
     * Format Jellyfin ticks to human-readable time format (HH:MM:SS or MM:SS).
     * Jellyfin ticks are in 10-million-per-second units (10,000,000 ticks = 1 second).
     *
     * @param ticks Jellyfin time in ticks
     * @return Formatted time string (e.g., "1:23:45" or "23:45")
     */
    fun formatTicksToTime(ticks: Long): String {
        val totalSeconds = ticks / TickConstants.TICKS_PER_SECOND
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Format milliseconds to human-readable time format (HH:MM:SS or MM:SS).
     *
     * @param milliseconds Time in milliseconds
     * @return Formatted time string (e.g., "1:23:45" or "23:45")
     */
    fun formatMillisToTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Format runtime ticks to human-readable duration (e.g., "2h 15m" or "45m").
     *
     * @param ticks Jellyfin runtime in ticks
     * @return Formatted duration string
     */
    fun formatRuntimeTicks(ticks: Long): String {
        val totalMinutes = ticks / TickConstants.TICKS_PER_MINUTE
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
