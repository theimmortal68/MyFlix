# TV Channels / WatchNext Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Integrate MyFlix with Android TV home screen via WatchNext (Play Next row) and a custom preview channel showing Continue Watching + Next Up.

**Architecture:** Event-driven WatchNext updates on playback stop (via `WatchNextManager` singleton called from `PlayerViewModel.reportPlaybackStopped`), periodic channel sync via WorkManager (`ChannelSyncWorker`). Single combined channel for Continue Watching + Next Up. Deep links (`myflix://play/{itemId}`) handled in MainActivity.

**Tech Stack:** AndroidX TvProvider (`androidx.tvprovider:tvprovider`), WorkManager (`androidx.work:work-runtime-ktx`), TvContractCompat APIs, Kotlin Coroutines.

---

## Task 1: Add Dependencies

**Files:**
- Modify: `app-tv/build.gradle.kts`

**Step 1: Add tvprovider and work-runtime dependencies**

Add to the dependencies block in `app-tv/build.gradle.kts`:

```kotlin
// Android TV home screen integration
implementation("androidx.tvprovider:tvprovider:1.0.0")
// WorkManager for periodic channel sync
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

**Step 2: Sync Gradle**

Run: `./gradlew :app-tv:dependencies --configuration implementation | grep -E "(tvprovider|work-runtime)"`
Expected: Both dependencies appear in output

**Step 3: Commit**

```bash
git add app-tv/build.gradle.kts
git commit -m "build(tv): add tvprovider and WorkManager dependencies"
```

---

## Task 2: Create PreviewProgramBuilder

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/channels/PreviewProgramBuilder.kt`

**Step 1: Create the builder utility**

```kotlin
package dev.jausc.myflix.tv.channels

import android.content.Context
import android.net.Uri
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * Builds TvContractCompat preview programs from JellyfinItem data.
 */
object PreviewProgramBuilder {

    private const val TICKS_PER_MS = 10_000L

    /**
     * Build a PreviewProgram for the custom channel.
     */
    fun buildPreviewProgram(
        context: Context,
        item: JellyfinItem,
        channelId: Long,
        serverUrl: String,
    ): PreviewProgram {
        val type = when (item.type) {
            "Movie" -> TvContractCompat.PreviewPrograms.TYPE_MOVIE
            "Episode" -> TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE
            "Series" -> TvContractCompat.PreviewPrograms.TYPE_TV_SERIES
            else -> TvContractCompat.PreviewPrograms.TYPE_CLIP
        }

        val builder = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setType(type)
            .setTitle(getDisplayTitle(item))
            .setDescription(item.overview ?: "")
            .setIntentUri(buildPlaybackUri(item))
            .setInternalProviderId(item.id)
            .setPosterArtUri(buildPosterUri(item, serverUrl))
            .setContentId(item.id)

        // Episode-specific metadata
        if (item.type == "Episode") {
            item.indexNumber?.let { builder.setEpisodeNumber(it.toString()) }
            item.parentIndexNumber?.let { builder.setSeasonNumber(it.toString()) }
            item.seriesName?.let { builder.setSeasonTitle(it) }
        }

        // Duration
        item.runTimeTicks?.let { ticks ->
            val durationMs = ticks / TICKS_PER_MS
            builder.setDurationMillis(durationMs.toInt())
        }

        // Resume position (for Continue Watching items)
        val positionTicks = item.userData?.playbackPositionTicks ?: 0
        if (positionTicks > 0) {
            val positionMs = positionTicks / TICKS_PER_MS
            builder.setLastPlaybackPositionMillis(positionMs.toInt())
        }

        return builder.build()
    }

    /**
     * Build a WatchNextProgram for the Play Next row.
     */
    fun buildWatchNextProgram(
        item: JellyfinItem,
        positionMs: Long,
        serverUrl: String,
        watchNextType: Int = TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE,
    ): WatchNextProgram {
        val type = when (item.type) {
            "Movie" -> TvContractCompat.WatchNextPrograms.TYPE_MOVIE
            "Episode" -> TvContractCompat.WatchNextPrograms.TYPE_TV_EPISODE
            else -> TvContractCompat.WatchNextPrograms.TYPE_CLIP
        }

        val builder = WatchNextProgram.Builder()
            .setType(type)
            .setWatchNextType(watchNextType)
            .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
            .setTitle(getDisplayTitle(item))
            .setDescription(item.overview ?: "")
            .setIntentUri(buildPlaybackUri(item, positionMs))
            .setInternalProviderId(item.id)
            .setPosterArtUri(buildPosterUri(item, serverUrl))
            .setContentId(item.id)

        // Episode-specific metadata
        if (item.type == "Episode") {
            item.indexNumber?.let { builder.setEpisodeNumber(it.toString()) }
            item.parentIndexNumber?.let { builder.setSeasonNumber(it.toString()) }
            item.seriesName?.let { builder.setSeasonTitle(it) }
        }

        // Duration and position
        item.runTimeTicks?.let { ticks ->
            val durationMs = ticks / TICKS_PER_MS
            builder.setDurationMillis(durationMs.toInt())
        }
        if (positionMs > 0) {
            builder.setLastPlaybackPositionMillis(positionMs.toInt())
        }

        return builder.build()
    }

    /**
     * Get display title - for episodes include series and episode info.
     */
    private fun getDisplayTitle(item: JellyfinItem): String {
        return if (item.type == "Episode") {
            val series = item.seriesName ?: ""
            val season = item.parentIndexNumber?.let { "S$it" } ?: ""
            val episode = item.indexNumber?.let { "E$it" } ?: ""
            val episodeTitle = item.name
            if (series.isNotEmpty()) {
                "$series $season$episode - $episodeTitle"
            } else {
                episodeTitle
            }
        } else {
            item.name
        }
    }

    /**
     * Build deep link URI for playback.
     */
    private fun buildPlaybackUri(item: JellyfinItem, positionMs: Long = 0): Uri {
        val builder = Uri.Builder()
            .scheme("myflix")
            .authority("play")
            .appendPath(item.id)
        if (positionMs > 0) {
            builder.appendQueryParameter("startPositionMs", positionMs.toString())
        }
        return builder.build()
    }

    /**
     * Build poster image URI from Jellyfin server.
     */
    private fun buildPosterUri(item: JellyfinItem, serverUrl: String): Uri {
        // Use Primary image, fall back to series image for episodes
        val imageItemId = if (item.type == "Episode" && item.seriesPrimaryImageTag != null) {
            item.seriesId ?: item.id
        } else {
            item.id
        }
        return Uri.parse("$serverUrl/Items/$imageItemId/Images/Primary?maxWidth=400&quality=90")
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :app-tv:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/channels/PreviewProgramBuilder.kt
git commit -m "feat(tv): add PreviewProgramBuilder for TV home screen programs"
```

---

## Task 3: Create WatchNextManager

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/channels/WatchNextManager.kt`

**Step 1: Create the WatchNext manager singleton**

```kotlin
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
```

**Step 2: Verify compilation**

Run: `./gradlew :app-tv:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/channels/WatchNextManager.kt
git commit -m "feat(tv): add WatchNextManager for Play Next row updates"
```

---

## Task 4: Create TvChannelManager

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/channels/TvChannelManager.kt`

**Step 1: Create the channel manager singleton**

```kotlin
package dev.jausc.myflix.tv.channels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manages the MyFlix preview channel on the Android TV home screen.
 * Shows Continue Watching + Next Up items in a single row.
 */
class TvChannelManager private constructor(
    private val context: Context,
) {
    private val mutex = Mutex()
    private val contentResolver = context.contentResolver

    companion object {
        private const val TAG = "TvChannelManager"
        private const val CHANNEL_ID = "myflix_continue_watching"
        private const val CHANNEL_DISPLAY_NAME = "MyFlix"

        /**
         * Maximum programs to show in the channel.
         */
        private const val MAX_PROGRAMS = 20

        @Volatile
        private var instance: TvChannelManager? = null

        fun getInstance(context: Context): TvChannelManager {
            return instance ?: synchronized(this) {
                instance ?: TvChannelManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Get or create the MyFlix channel. Returns the channel ID.
     */
    suspend fun getOrCreateChannel(): Long = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Check for existing channel
            val existingId = findChannel()
            if (existingId != null) {
                return@withContext existingId
            }

            // Create new channel
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(CHANNEL_DISPLAY_NAME)
                .setAppLinkIntentUri(Uri.parse("myflix://home"))
                .setInternalProviderId(CHANNEL_ID)
                .build()

            val channelUri = contentResolver.insert(
                TvContractCompat.Channels.CONTENT_URI,
                channel.toContentValues(),
            )

            val channelId = channelUri?.let { TvContractCompat.parseChannelId(it) }
                ?: throw IllegalStateException("Failed to create channel")

            // Set channel logo
            try {
                val logoDrawable = androidx.core.content.ContextCompat.getDrawable(
                    context,
                    R.mipmap.ic_launcher,
                )
                if (logoDrawable != null) {
                    val bitmap = android.graphics.drawable.BitmapDrawable(
                        context.resources,
                        logoDrawable as android.graphics.drawable.BitmapDrawable,
                    ).bitmap
                    ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set channel logo", e)
            }

            Log.d(TAG, "Created MyFlix channel with ID: $channelId")
            channelId
        }
    }

    /**
     * Update channel programs with Continue Watching + Next Up items.
     *
     * @param continueWatching Items the user has started watching
     * @param nextUp Next episodes in series the user is watching
     * @param serverUrl Jellyfin server URL for poster images
     */
    suspend fun updatePrograms(
        continueWatching: List<JellyfinItem>,
        nextUp: List<JellyfinItem>,
        serverUrl: String,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val channelId = getOrCreateChannel()

                // Clear existing programs
                contentResolver.delete(
                    TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                    null,
                    null,
                )

                // Combine and dedupe: Continue Watching first, then Next Up
                val seenIds = mutableSetOf<String>()
                val allItems = mutableListOf<JellyfinItem>()

                continueWatching.forEach { item ->
                    if (seenIds.add(item.id)) {
                        allItems.add(item)
                    }
                }
                nextUp.forEach { item ->
                    if (seenIds.add(item.id)) {
                        allItems.add(item)
                    }
                }

                // Insert programs (limited to MAX_PROGRAMS)
                allItems.take(MAX_PROGRAMS).forEach { item ->
                    val program = PreviewProgramBuilder.buildPreviewProgram(
                        context = context,
                        item = item,
                        channelId = channelId,
                        serverUrl = serverUrl,
                    )
                    contentResolver.insert(
                        TvContractCompat.PreviewPrograms.CONTENT_URI,
                        program.toContentValues(),
                    )
                }

                Log.d(TAG, "Updated channel with ${allItems.take(MAX_PROGRAMS).size} programs")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating channel programs", e)
            }
        }
    }

    /**
     * Find existing MyFlix channel by internal provider ID.
     */
    private fun findChannel(): Long? {
        val cursor = contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            Channel.PROJECTION,
            null,
            null,
            null,
        ) ?: return null

        cursor.use {
            while (it.moveToNext()) {
                val channel = Channel.fromCursor(it)
                if (channel.internalProviderId == CHANNEL_ID) {
                    return channel.id
                }
            }
        }
        return null
    }

    /**
     * Request the system to make the channel visible (browsable).
     * User must approve this via system dialog.
     */
    fun requestChannelBrowsable(channelId: Long) {
        TvContractCompat.requestChannelBrowsable(context, channelId)
    }

    /**
     * Delete the channel and all its programs.
     */
    suspend fun deleteChannel() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val channelId = findChannel() ?: return@withContext
            contentResolver.delete(
                TvContractCompat.buildChannelUri(channelId),
                null,
                null,
            )
            Log.d(TAG, "Deleted MyFlix channel")
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :app-tv:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/channels/TvChannelManager.kt
git commit -m "feat(tv): add TvChannelManager for custom preview channel"
```

---

## Task 5: Create ChannelSyncWorker

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/channels/ChannelSyncWorker.kt`

**Step 1: Create the WorkManager worker**

```kotlin
package dev.jausc.myflix.tv.channels

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
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
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<ChannelSyncWorker>()
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
            val jellyfinClient = JellyfinClient(applicationContext)

            // Check if logged in
            if (!jellyfinClient.isLoggedIn()) {
                Log.d(TAG, "Not logged in, skipping sync")
                return Result.success()
            }

            val serverUrl = jellyfinClient.getServerUrl() ?: run {
                Log.w(TAG, "No server URL, skipping sync")
                return Result.success()
            }

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
```

**Step 2: Verify compilation**

Run: `./gradlew :app-tv:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/channels/ChannelSyncWorker.kt
git commit -m "feat(tv): add ChannelSyncWorker for periodic channel updates"
```

---

## Task 6: Add Deep Link Handling to AndroidManifest

**Files:**
- Modify: `app-tv/src/main/AndroidManifest.xml`

**Step 1: Add deep link intent filter to MainActivity**

Add the following intent-filter inside the `<activity android:name=".MainActivity">` element, after the existing LEANBACK_LAUNCHER filter:

```xml
<!-- Deep links from TV home screen -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="myflix"
        android:host="play"
        android:pathPattern="/.*" />
</intent-filter>
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="myflix"
        android:host="home" />
</intent-filter>
```

**Step 2: Verify manifest is valid**

Run: `./gradlew :app-tv:processDebugManifest 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app-tv/src/main/AndroidManifest.xml
git commit -m "feat(tv): add deep link intent filters for TV home screen"
```

---

## Task 7: Handle Deep Links in MainActivity

**Files:**
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/MainActivity.kt`

**Step 1: Add deep link handling function**

Add this function in MainActivity (near other navigation helpers):

```kotlin
/**
 * Handle deep links from Android TV home screen.
 * Format: myflix://play/{itemId}?startPositionMs={position}
 *         myflix://home
 */
private fun handleDeepLink(intent: Intent?): Boolean {
    val uri = intent?.data ?: return false
    if (uri.scheme != "myflix") return false

    Log.d("MainActivity", "Handling deep link: $uri")

    return when (uri.host) {
        "play" -> {
            val itemId = uri.pathSegments.firstOrNull()
            if (itemId != null) {
                val startPositionMs = uri.getQueryParameter("startPositionMs")?.toLongOrNull()
                // Navigate to player after login check
                pendingDeepLink = DeepLink.Play(itemId, startPositionMs)
                true
            } else {
                false
            }
        }
        "home" -> {
            pendingDeepLink = DeepLink.Home
            true
        }
        else -> false
    }
}

/**
 * Deep link types for deferred navigation.
 */
private sealed class DeepLink {
    data class Play(val itemId: String, val startPositionMs: Long?) : DeepLink()
    data object Home : DeepLink()
}

private var pendingDeepLink: DeepLink? = null
```

**Step 2: Call handleDeepLink in onCreate and onNewIntent**

In `onCreate`, after `setContent` call, add:

```kotlin
// Handle deep link from launch
handleDeepLink(intent)
```

Add `onNewIntent` override if not present:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleDeepLink(intent)
}
```

**Step 3: Process pending deep link after login**

In the navigation section where login completes (the composable route for "home" or after login success), add logic to check and navigate:

```kotlin
// After successful login/navigation to home, check for pending deep link
LaunchedEffect(Unit) {
    pendingDeepLink?.let { deepLink ->
        pendingDeepLink = null
        when (deepLink) {
            is DeepLink.Play -> {
                val route = if (deepLink.startPositionMs != null) {
                    "player/${deepLink.itemId}?startPositionMs=${deepLink.startPositionMs}"
                } else {
                    "player/${deepLink.itemId}"
                }
                navController.navigate(route)
            }
            is DeepLink.Home -> {
                // Already at home, do nothing
            }
        }
    }
}
```

**Step 4: Verify compilation**

Run: `./gradlew :app-tv:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/MainActivity.kt
git commit -m "feat(tv): handle deep links from TV home screen"
```

---

## Task 8: Integrate WatchNextManager with PlayerViewModel

**Files:**
- Modify: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/PlayerViewModel.kt`

**Step 1: Add callback interface for playback stop events**

Add at the top of the file (after imports):

```kotlin
/**
 * Callback interface for playback stop events.
 * Used by TV app to update Watch Next row.
 */
interface PlaybackStopCallback {
    suspend fun onPlaybackStopped(item: JellyfinItem, positionMs: Long)
}
```

**Step 2: Add callback parameter to PlayerViewModel**

Modify the constructor to accept an optional callback:

```kotlin
class PlayerViewModel(
    private val itemId: String,
    private val jellyfinClient: JellyfinClient,
    private val appPreferences: dev.jausc.myflix.core.common.preferences.AppPreferences?,
    private val preferredAudioLanguage: String? = null,
    private val preferredSubtitleLanguage: String? = null,
    private val maxStreamingBitrateMbps: Int = 0,
    private var startPositionOverrideMs: Long? = null,
    private val preferHdrOverDv: Boolean = false,
    private val playbackStopCallback: PlaybackStopCallback? = null,  // NEW
) : ViewModel() {
```

**Step 3: Update Factory to accept callback**

```kotlin
class Factory(
    private val itemId: String,
    private val jellyfinClient: JellyfinClient,
    private val appPreferences: dev.jausc.myflix.core.common.preferences.AppPreferences?,
    private val preferredAudioLanguage: String? = null,
    private val preferredSubtitleLanguage: String? = null,
    private val maxStreamingBitrateMbps: Int = 0,
    private val startPositionMs: Long? = null,
    private val preferHdrOverDv: Boolean = false,
    private val playbackStopCallback: PlaybackStopCallback? = null,  // NEW
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlayerViewModel(
            itemId,
            jellyfinClient,
            appPreferences,
            preferredAudioLanguage,
            preferredSubtitleLanguage,
            maxStreamingBitrateMbps,
            startPositionMs,
            preferHdrOverDv,
            playbackStopCallback,  // NEW
        ) as T
}
```

**Step 4: Call callback in reportPlaybackStopped**

In the `reportPlaybackStopped` function, add callback invocation after the API call:

```kotlin
private suspend fun reportPlaybackStopped(positionMs: Long) {
    val positionTicks = positionMs * TICKS_PER_MS
    val state = _uiState.value
    android.util.Log.d("PlayerViewModel", "reportPlaybackStopped: itemId=$currentItemId position=${positionMs}ms")
    jellyfinClient.reportPlaybackStopped(
        currentItemId,
        positionTicks,
        mediaSourceId = state.mediaSourceId,
        liveStreamId = state.liveStreamId,
    )

    // Notify callback for Watch Next updates (TV)
    state.item?.let { item ->
        playbackStopCallback?.onPlaybackStopped(item, positionMs)
    }

    // Clear persisted session since we properly reported stop
    appPreferences?.clearActivePlaybackSession()
    android.util.Log.d("PlayerViewModel", "reportPlaybackStopped: completed and cleared session")
}
```

**Step 5: Verify compilation**

Run: `./gradlew :core:viewmodel:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/PlayerViewModel.kt
git commit -m "feat(viewmodel): add PlaybackStopCallback for TV Watch Next integration"
```

---

## Task 9: Wire Up WatchNextManager in TV PlayerScreen

**Files:**
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/PlayerScreen.kt`

**Step 1: Create PlaybackStopCallback implementation**

In PlayerScreen.kt, add the callback implementation that will be passed to PlayerViewModel:

```kotlin
// At the top of the PlayerScreen composable, create the callback
val watchNextManager = remember { WatchNextManager.getInstance(context) }
val serverUrl = remember { jellyfinClient.getServerUrl() ?: "" }

val playbackStopCallback = remember {
    object : PlaybackStopCallback {
        override suspend fun onPlaybackStopped(item: JellyfinItem, positionMs: Long) {
            if (serverUrl.isNotEmpty()) {
                watchNextManager.onPlaybackStopped(item, positionMs, serverUrl)
            }
        }
    }
}
```

**Step 2: Pass callback to PlayerViewModel.Factory**

Update the ViewModel creation to include the callback:

```kotlin
val viewModel: PlayerViewModel = viewModel(
    key = itemId,
    factory = PlayerViewModel.Factory(
        itemId = itemId,
        jellyfinClient = jellyfinClient,
        appPreferences = appPreferences,
        preferredAudioLanguage = preferredAudioLanguage,
        preferredSubtitleLanguage = preferredSubtitleLanguage,
        maxStreamingBitrateMbps = maxStreamingBitrate,
        startPositionMs = startPositionMs,
        preferHdrOverDv = preferHdrOverDv,
        playbackStopCallback = playbackStopCallback,  // NEW
    ),
)
```

**Step 3: Add required imports**

```kotlin
import dev.jausc.myflix.core.viewmodel.PlaybackStopCallback
import dev.jausc.myflix.tv.channels.WatchNextManager
```

**Step 4: Verify compilation**

Run: `./gradlew :app-tv:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/PlayerScreen.kt
git commit -m "feat(tv): wire WatchNextManager to PlayerScreen for Watch Next updates"
```

---

## Task 10: Initialize Channel Sync on App Start

**Files:**
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/MainActivity.kt`

**Step 1: Schedule channel sync after login**

In the section where login completes successfully (after navigating to home), add:

```kotlin
// Start periodic channel sync
ChannelSyncWorker.schedule(context)

// Also trigger immediate sync
ChannelSyncWorker.syncNow(context)

// Request channel to be browsable (first time setup)
LaunchedEffect(Unit) {
    val channelManager = TvChannelManager.getInstance(context)
    val channelId = channelManager.getOrCreateChannel()
    channelManager.requestChannelBrowsable(channelId)
}
```

**Step 2: Cancel sync on logout**

If there's a logout function, add:

```kotlin
// Cancel channel sync on logout
ChannelSyncWorker.cancel(context)

// Clear Watch Next entries
val watchNextManager = WatchNextManager.getInstance(context)
lifecycleScope.launch {
    watchNextManager.clearAll()
}
```

**Step 3: Add required imports**

```kotlin
import dev.jausc.myflix.tv.channels.ChannelSyncWorker
import dev.jausc.myflix.tv.channels.TvChannelManager
import dev.jausc.myflix.tv.channels.WatchNextManager
```

**Step 4: Verify compilation**

Run: `./gradlew :app-tv:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/MainActivity.kt
git commit -m "feat(tv): initialize channel sync and Watch Next on app start"
```

---

## Task 11: Build and Test

**Step 1: Full build**

Run: `./gradlew :app-tv:assembleDebug 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL

**Step 2: Install on device**

Run: `adb -s 192.168.1.136:5555 install -r "app-tv/build/outputs/apk/debug/app-tv-universal-debug.apk"`
Expected: Success

**Step 3: Manual testing checklist**

1. Launch app, log in
2. Play a video, stop at ~50%
3. Go to Android TV home screen
4. Verify "Play Next" row shows the item with progress
5. Verify "MyFlix" channel row appears (may need to add via "Customize channels")
6. Click item in Play Next - verify it resumes at correct position
7. Complete a video (watch to end)
8. Verify item is removed from Play Next

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat(tv): complete TV Channels / WatchNext implementation"
```

---

## Summary

| File | Action | Purpose |
|------|--------|---------|
| `app-tv/build.gradle.kts` | Modify | Add tvprovider + WorkManager deps |
| `PreviewProgramBuilder.kt` | Create | Build TvContractCompat programs |
| `WatchNextManager.kt` | Create | Manage Play Next row (immediate updates) |
| `TvChannelManager.kt` | Create | Manage custom MyFlix channel |
| `ChannelSyncWorker.kt` | Create | Periodic background sync |
| `AndroidManifest.xml` | Modify | Add deep link intent filters |
| `MainActivity.kt` | Modify | Handle deep links, init sync |
| `PlayerViewModel.kt` | Modify | Add PlaybackStopCallback |
| `PlayerScreen.kt` | Modify | Wire WatchNextManager |
