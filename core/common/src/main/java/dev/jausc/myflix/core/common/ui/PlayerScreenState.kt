package dev.jausc.myflix.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.videoStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Ticks per millisecond for Jellyfin time conversion.
 */
private const val TICKS_PER_MS = 10_000L

/**
 * Progress report interval in milliseconds.
 */
private const val PROGRESS_REPORT_INTERVAL_MS = 10_000L

/**
 * Auto-hide controls delay in milliseconds.
 */
private const val CONTROLS_AUTO_HIDE_MS = 5_000L

/**
 * Percentage of video that must be watched to mark as played.
 */
private const val COMPLETION_THRESHOLD = 0.95f

/**
 * State holder for PlayerScreen.
 * Manages item loading, playback reporting, and controls visibility across TV and mobile platforms.
 */
@Stable
class PlayerScreenState(
    val itemId: String,
    private val reporter: PlaybackReporter,
    private val scope: CoroutineScope,
) {
    // Item state
    var item by mutableStateOf<JellyfinItem?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var streamUrl by mutableStateOf<String?>(null)
        private set

    var startPositionMs by mutableLongStateOf(0L)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Player state
    var playerReady by mutableStateOf(false)

    // Controls visibility
    var showControls by mutableStateOf(true)
        private set

    // Internal tracking
    private var playbackStarted = false
    private var markedAsPlayed = false
    private var progressReportJob: Job? = null
    private var controlsHideJob: Job? = null

    /**
     * MediaInfo for content-aware player selection.
     * Available after item is loaded.
     */
    val mediaInfo: PlayerMediaInfo?
        get() = item?.let { loadedItem ->
            val videoStream = loadedItem.videoStream
            PlayerMediaInfo(
                title = loadedItem.name,
                videoCodec = videoStream?.codec,
                videoProfile = videoStream?.profile,
                videoRangeType = videoStream?.videoRangeType,
                width = videoStream?.width ?: 0,
                height = videoStream?.height ?: 0,
                bitrate = videoStream?.bitRate ?: 0,
            )
        }

    /**
     * Load the item and prepare for playback.
     */
    internal fun loadItem() {
        scope.launch {
            isLoading = true
            error = null

            reporter.loadItem(itemId)
                .onSuccess { loadedItem ->
                    item = loadedItem
                    streamUrl = reporter.getStreamUrl(itemId)
                    startPositionMs = loadedItem.userData?.playbackPositionTicks?.let {
                        it / TICKS_PER_MS
                    } ?: 0L
                }
                .onFailure { e ->
                    error = e.message ?: "Failed to load item"
                }

            isLoading = false
        }
    }

    /**
     * Called when playback actually starts.
     * Reports playback start to the server.
     */
    fun onPlaybackStarted(positionMs: Long) {
        if (playbackStarted) return
        playbackStarted = true

        scope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            reporter.reportPlaybackStart(itemId, positionTicks)
        }

        // Start progress reporting
        startProgressReporting()
    }

    /**
     * Start periodic progress reporting.
     */
    private fun startProgressReporting() {
        progressReportJob?.cancel()
        progressReportJob = scope.launch {
            while (isActive) {
                delay(PROGRESS_REPORT_INTERVAL_MS)
                // Progress will be reported via onPlaybackProgress
            }
        }
    }

    /**
     * Report playback progress.
     * Called periodically during playback.
     */
    fun reportProgress(positionMs: Long, isPaused: Boolean) {
        if (!playbackStarted) return

        scope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            reporter.reportPlaybackProgress(itemId, positionTicks, isPaused)
        }
    }

    /**
     * Report pause state change.
     */
    fun onPauseStateChanged(positionMs: Long, isPaused: Boolean) {
        if (!playbackStarted) return

        scope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            reporter.reportPlaybackProgress(itemId, positionTicks, isPaused)
        }
    }

    /**
     * Check if video is complete (95% watched) and mark as played.
     */
    fun checkVideoCompletion(positionMs: Long, durationMs: Long) {
        if (!playbackStarted || markedAsPlayed || durationMs <= 0) return

        val watchedPercent = positionMs.toFloat() / durationMs.toFloat()
        if (watchedPercent >= COMPLETION_THRESHOLD) {
            markedAsPlayed = true
            scope.launch {
                reporter.setPlayed(itemId, true)
            }
        }
    }

    /**
     * Report playback stopped.
     * Should be called synchronously on dispose (use runBlocking if needed).
     */
    suspend fun reportPlaybackStopped(positionMs: Long) {
        progressReportJob?.cancel()
        val positionTicks = positionMs * TICKS_PER_MS
        reporter.reportPlaybackStopped(itemId, positionTicks)
    }

    /**
     * Show controls and reset auto-hide timer.
     */
    fun showControls() {
        showControls = true
        resetControlsHideTimer()
    }

    /**
     * Hide controls.
     */
    fun hideControls() {
        showControls = false
        controlsHideJob?.cancel()
    }

    /**
     * Toggle controls visibility.
     */
    fun toggleControls() {
        if (showControls) {
            hideControls()
        } else {
            showControls()
        }
    }

    /**
     * Reset the auto-hide timer for controls.
     * Call this when user interacts with the player.
     */
    fun resetControlsHideTimer() {
        controlsHideJob?.cancel()
        controlsHideJob = scope.launch {
            delay(CONTROLS_AUTO_HIDE_MS)
            showControls = false
        }
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        progressReportJob?.cancel()
        controlsHideJob?.cancel()
    }
}

/**
 * Media information for player backend selection.
 */
data class PlayerMediaInfo(
    val title: String,
    val videoCodec: String?,
    val videoProfile: String?,
    val videoRangeType: String?,
    val width: Int,
    val height: Int,
    val bitrate: Long,
)

/**
 * Reporter interface for playback events.
 * Abstracts the JellyfinClient for dependency injection.
 */
interface PlaybackReporter {
    suspend fun loadItem(itemId: String): Result<JellyfinItem>
    fun getStreamUrl(itemId: String): String
    suspend fun reportPlaybackStart(itemId: String, positionTicks: Long)
    suspend fun reportPlaybackProgress(itemId: String, positionTicks: Long, isPaused: Boolean)
    suspend fun reportPlaybackStopped(itemId: String, positionTicks: Long)
    suspend fun setPlayed(itemId: String, played: Boolean)
}

/**
 * Creates and remembers a [PlayerScreenState].
 *
 * @param itemId The ID of the item to play
 * @param reporter Reporter for playback events
 * @return A [PlayerScreenState] for managing player screen UI state
 */
@Composable
fun rememberPlayerScreenState(
    itemId: String,
    reporter: PlaybackReporter,
): PlayerScreenState {
    val scope = rememberCoroutineScope()
    val state = remember(itemId) {
        PlayerScreenState(itemId, reporter, scope)
    }

    LaunchedEffect(itemId) {
        state.loadItem()
    }

    return state
}
