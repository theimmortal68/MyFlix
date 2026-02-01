package dev.jausc.myflix.core.common.util

import dev.jausc.myflix.core.common.model.JellyfinItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility object for media information formatting.
 */
object MediaInfoUtil {
    /**
     * Format series status into a user-friendly badge label.
     *
     * @param status The raw status string from Jellyfin
     * @return Formatted status badge label
     */
    fun formatSeriesStatusBadge(status: String): String {
        val normalized = status.trim().lowercase()
        return when {
            normalized.contains("continuing") -> "Airing"
            normalized.contains("returning") -> "Returning"
            normalized.contains("ended") -> "Ended"
            normalized.contains("canceled") || normalized.contains("cancelled") -> "Canceled"
            else -> status.trim()
        }
    }

    /**
     * Build runtime details list including runtime, remaining time, and end time.
     *
     * @param now Current time for calculating end time
     * @param item The Jellyfin item with runtime information
     * @return List of runtime-related detail strings
     */
    fun buildRuntimeDetails(now: LocalDateTime, item: JellyfinItem,): List<String> = buildList {
        addRuntimeDetails(now, item)
    }

    /**
     * Extension function to add runtime details to a mutable list.
     */
    fun MutableList<String>.addRuntimeDetails(now: LocalDateTime, item: JellyfinItem,) {
        val runtimeTicks = item.runTimeTicks ?: return
        val runtimeMinutes = (runtimeTicks / TickConstants.TICKS_PER_MINUTE).toInt()
        if (runtimeMinutes <= 0) return

        // Format runtime as "1h 48m" or "48m"
        val hours = runtimeMinutes / 60
        val mins = runtimeMinutes % 60
        val runtimeText = when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
        add(runtimeText)

        // Calculate remaining time if there's playback progress
        val positionTicks = item.userData?.playbackPositionTicks ?: 0L
        if (positionTicks > 0L) {
            val remainingTicks = runtimeTicks - positionTicks
            val remainingMinutes = (remainingTicks / TickConstants.TICKS_PER_MINUTE).toInt()
            if (remainingMinutes > 0) {
                val remainingHours = remainingMinutes / 60
                val remainingMins = remainingMinutes % 60
                val remainingText = when {
                    remainingHours > 0 && remainingMins > 0 -> "${remainingHours}h ${remainingMins}m left"
                    remainingHours > 0 -> "${remainingHours}h left"
                    else -> "${remainingMins}m left"
                }
                add(remainingText)
            }
        }

        // Calculate "ends at" time
        val ticksToPlay = if (positionTicks > 0L) runtimeTicks - positionTicks else runtimeTicks
        val secondsToPlay = ticksToPlay / TickConstants.TICKS_PER_SECOND
        val endTime = now.plusSeconds(secondsToPlay)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        add("ends at ${endTime.format(timeFormatter)}")
    }
}
