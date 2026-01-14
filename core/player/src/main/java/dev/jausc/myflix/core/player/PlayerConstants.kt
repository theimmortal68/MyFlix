package dev.jausc.myflix.core.player

/**
 * Shared player constants used across TV and mobile apps.
 */
object PlayerConstants {
    /**
     * Jellyfin uses ticks (100 nanoseconds) for time values.
     * 10,000 ticks = 1 millisecond.
     */
    const val TICKS_PER_MS = 10_000L

    /**
     * Auto-hide player controls after this duration when playing.
     */
    const val CONTROLS_AUTO_HIDE_MS = 4_000L

    /**
     * Seek forward/backward step duration in milliseconds (short seek).
     */
    const val SEEK_STEP_MS = 10_000L

    /**
     * Long seek forward/backward step duration in milliseconds (e.g., up/down on TV).
     */
    const val SEEK_STEP_LONG_MS = 60_000L

    /**
     * Interval for reporting playback progress to server in milliseconds.
     */
    const val PROGRESS_REPORT_INTERVAL_MS = 10_000L

    /**
     * Sentinel value for disabling subtitle tracks.
     */
    const val TRACK_DISABLED = -1

    /**
     * Minimum position (in ms) to consider meaningful for resume.
     * Positions below this are treated as "start from beginning".
     */
    const val MIN_RESUME_POSITION_MS = 5_000L

    /**
     * Convert Jellyfin ticks to milliseconds.
     */
    fun ticksToMs(ticks: Long): Long = ticks / TICKS_PER_MS

    /**
     * Convert milliseconds to Jellyfin ticks.
     */
    fun msToTicks(ms: Long): Long = ms * TICKS_PER_MS
}
