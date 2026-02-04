package dev.jausc.myflix.tv.channels

import android.content.Context
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import dev.jausc.myflix.core.common.model.JellyfinItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manages the Android TV "Watch Next" (Play Next) row.
 * Updates immediately when playback stops with a resume position.
 *
 * Thread-safe singleton with debouncing for rapid stop events.
 */
class WatchNextManager private constructor(
    private val context: Context,
) {
    private val mutex = Mutex()
    private val contentResolver = context.contentResolver

    companion object {
        private const val TAG = "WatchNextManager"

        /**
         * Minimum watch percentage to add to Watch Next (5%).
         * Don't add items barely started.
         */
        private const val MIN_WATCH_PERCENT = 0.05

        /**
         * Maximum watch percentage - if above this, item is "complete" (95%).
         * Remove from Watch Next when complete.
         */
        private const val MAX_WATCH_PERCENT = 0.95

        @Volatile
        private var instance: WatchNextManager? = null

        fun getInstance(context: Context): WatchNextManager {
            return instance ?: synchronized(this) {
                instance ?: WatchNextManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Update Watch Next when playback stops.
     * - If position is between 5-95% of duration: add/update entry
     * - If position >= 95%: remove entry (completed)
     * - If position < 5%: remove entry (barely started)
     *
     * @param item The JellyfinItem that was being played
     * @param positionMs Current playback position in milliseconds
     * @param serverUrl Jellyfin server URL for poster images
     */
    suspend fun onPlaybackStopped(
        item: JellyfinItem,
        positionMs: Long,
        serverUrl: String,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val durationMs = (item.runTimeTicks ?: 0) / 10_000L
                if (durationMs <= 0) {
                    Log.w(TAG, "Cannot update Watch Next: no duration for ${item.id}")
                    return@withContext
                }

                val watchPercent = positionMs.toDouble() / durationMs.toDouble()
                Log.d(TAG, "Playback stopped: ${item.name} at ${(watchPercent * 100).toInt()}%")

                when {
                    watchPercent >= MAX_WATCH_PERCENT -> {
                        // Completed - remove from Watch Next
                        removeFromWatchNext(item.id)
                        Log.d(TAG, "Removed completed item from Watch Next: ${item.name}")
                    }
                    watchPercent >= MIN_WATCH_PERCENT -> {
                        // In progress - add/update Watch Next
                        addOrUpdateWatchNext(item, positionMs, serverUrl)
                        Log.d(TAG, "Added/updated Watch Next: ${item.name}")
                    }
                    else -> {
                        // Barely started - remove if exists
                        removeFromWatchNext(item.id)
                        Log.d(TAG, "Removed barely-started item from Watch Next: ${item.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Watch Next for ${item.id}", e)
            }
        }
    }

    /**
     * Add or update a Watch Next entry.
     */
    private fun addOrUpdateWatchNext(
        item: JellyfinItem,
        positionMs: Long,
        serverUrl: String,
    ) {
        val existingId = findWatchNextProgram(item.id)
        val program = PreviewProgramBuilder.buildWatchNextProgram(
            item = item,
            positionMs = positionMs,
            serverUrl = serverUrl,
        )

        if (existingId != null) {
            // Update existing
            val updateUri = TvContractCompat.buildWatchNextProgramUri(existingId)
            contentResolver.update(updateUri, program.toContentValues(), null, null)
        } else {
            // Insert new
            contentResolver.insert(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                program.toContentValues(),
            )
        }
    }

    /**
     * Remove an item from Watch Next.
     */
    private fun removeFromWatchNext(itemId: String) {
        val existingId = findWatchNextProgram(itemId) ?: return
        val deleteUri = TvContractCompat.buildWatchNextProgramUri(existingId)
        contentResolver.delete(deleteUri, null, null)
    }

    /**
     * Find existing Watch Next program by internal provider ID.
     */
    private fun findWatchNextProgram(itemId: String): Long? {
        val cursor = contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            WatchNextProgram.PROJECTION,
            null,
            null,
            null,
        ) ?: return null

        cursor.use {
            while (it.moveToNext()) {
                val program = WatchNextProgram.fromCursor(it)
                if (program.internalProviderId == itemId) {
                    return program.id
                }
            }
        }
        return null
    }

    /**
     * Clear all Watch Next entries for this app.
     * Useful for logout or data reset.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val cursor = contentResolver.query(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI,
                    WatchNextProgram.PROJECTION,
                    null,
                    null,
                    null,
                ) ?: return@withContext

                cursor.use {
                    while (it.moveToNext()) {
                        val program = WatchNextProgram.fromCursor(it)
                        val deleteUri = TvContractCompat.buildWatchNextProgramUri(program.id)
                        contentResolver.delete(deleteUri, null, null)
                    }
                }
                Log.d(TAG, "Cleared all Watch Next entries")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing Watch Next", e)
            }
        }
    }
}
