package dev.jausc.myflix.tv.channels

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.jausc.myflix.core.data.ServerManager
import dev.jausc.myflix.core.network.JellyfinClient
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically syncs the MyFlix channel
 * with Continue Watching + Next Up from Jellyfin.
 */
class ChannelSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ChannelSyncWorker"
        private const val WORK_NAME = "myflix_channel_sync"

        /**
         * Sync interval in minutes.
         */
        private const val SYNC_INTERVAL_MINUTES = 30L

        /**
         * Schedule periodic channel sync.
         * Call this on app startup after login.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ChannelSyncWorker>(
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest,
            )

            Log.d(TAG, "Scheduled channel sync every $SYNC_INTERVAL_MINUTES minutes")
        }

        /**
         * Cancel periodic sync (e.g., on logout).
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled channel sync")
        }

        /**
         * Run sync immediately (one-shot).
         */
        fun syncNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<ChannelSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Triggered immediate channel sync")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting channel sync")

        return try {
            // Get active server from ServerManager
            val serverManager = ServerManager.getInstance(applicationContext)
            val activeServer = serverManager.getActiveServer()

            if (activeServer == null) {
                Log.d(TAG, "No active server, skipping sync")
                return Result.success()
            }

            // Create and configure JellyfinClient
            val jellyfinClient = JellyfinClient(applicationContext).apply {
                configure(
                    activeServer.serverUrl,
                    activeServer.accessToken,
                    activeServer.userId,
                    deviceId,
                )
            }

            // Check if authenticated
            if (!jellyfinClient.isAuthenticated) {
                Log.d(TAG, "Not authenticated, skipping sync")
                return Result.success()
            }

            val serverUrl = activeServer.serverUrl

            // Fetch Continue Watching
            val continueWatchingResult = jellyfinClient.getContinueWatching(limit = 10)
            val continueWatching = continueWatchingResult.getOrNull() ?: emptyList()

            // Fetch Next Up
            val nextUpResult = jellyfinClient.getNextUp(limit = 10)
            val nextUp = nextUpResult.getOrNull() ?: emptyList()

            Log.d(TAG, "Fetched ${continueWatching.size} continue watching, ${nextUp.size} next up")

            // Update channel
            val channelManager = TvChannelManager.getInstance(applicationContext)
            channelManager.updatePrograms(
                continueWatching = continueWatching,
                nextUp = nextUp,
                serverUrl = serverUrl,
            )

            Log.d(TAG, "Channel sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Channel sync failed", e)
            Result.retry()
        }
    }
}
