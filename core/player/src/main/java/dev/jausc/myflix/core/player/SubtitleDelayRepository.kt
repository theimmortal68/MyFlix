package dev.jausc.myflix.core.player

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for persisting subtitle delay settings per media item.
 *
 * Stores delay values keyed by Jellyfin item ID, allowing users to set
 * different subtitle timing adjustments for different content.
 */
class SubtitleDelayRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    /**
     * Get the stored subtitle delay for an item.
     *
     * @param itemId Jellyfin item ID
     * @return Delay in milliseconds, or 0 if none set
     */
    fun getDelayMs(itemId: String): Long {
        return prefs.getLong(itemId, 0L)
    }

    /**
     * Store the subtitle delay for an item.
     *
     * @param itemId Jellyfin item ID
     * @param delayMs Delay in milliseconds. If 0, the entry is removed.
     */
    fun setDelayMs(itemId: String, delayMs: Long) {
        if (delayMs == 0L) {
            prefs.edit().remove(itemId).apply()
        } else {
            prefs.edit().putLong(itemId, delayMs).apply()
        }
    }

    /**
     * Clear all stored subtitle delays.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "subtitle_delays"
    }
}
