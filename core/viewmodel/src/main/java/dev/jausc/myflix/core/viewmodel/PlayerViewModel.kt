package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaSegment
import dev.jausc.myflix.core.common.model.MediaSegmentType
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.videoStream
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.PlayQueueManager
import dev.jausc.myflix.core.player.QueueItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ticks per millisecond for Jellyfin time conversion.
 */
private const val TICKS_PER_MS = 10_000L

/**
 * Auto-hide controls delay in milliseconds.
 */
private const val CONTROLS_AUTO_HIDE_MS = 5_000L

/**
 * Percentage of video that must be watched to mark as played.
 */
private const val COMPLETION_THRESHOLD = 0.95f

/**
 * Default countdown duration for auto-play.
 */
private const val AUTO_PLAY_COUNTDOWN_SECONDS = 5

/**
 * UI state for the player screen.
 */
data class PlayerUiState(
    val item: JellyfinItem? = null,
    val isLoading: Boolean = true,
    val streamUrl: String? = null,
    val startPositionMs: Long = 0L,
    val error: String? = null,
    val playerReady: Boolean = false,
    val showControls: Boolean = true,
    val mediaSourceId: String? = null,
    val audioStreams: List<MediaStream> = emptyList(),
    val subtitleStreams: List<MediaStream> = emptyList(),
    val selectedAudioStreamIndex: Int? = null,
    val selectedSubtitleStreamIndex: Int? = null,
    // Playback session tracking
    val playSessionId: String? = null,
    val playMethod: String = "DirectPlay",
    val maxStreamingBitrate: Long? = null,
    // Queue/Auto-play state
    val showAutoPlayCountdown: Boolean = false,
    val nextQueueItem: QueueItem? = null,
    val countdownSecondsRemaining: Int = AUTO_PLAY_COUNTDOWN_SECONDS,
    val isQueueMode: Boolean = false,
    // Media segments (intro, outro, etc.)
    val mediaSegments: List<MediaSegment> = emptyList(),
    val activeSegment: MediaSegment? = null,
) {
    /**
     * MediaInfo for content-aware player selection.
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
 * Shared ViewModel for player screens.
 * Manages item loading, playback reporting, and controls visibility.
 */
class PlayerViewModel(
    private val itemId: String,
    private val jellyfinClient: JellyfinClient,
    private val appPreferences: dev.jausc.myflix.core.common.preferences.AppPreferences?,
    private val preferredAudioLanguage: String? = null,
    private val preferredSubtitleLanguage: String? = null,
    private val maxStreamingBitrateMbps: Int = 0,
    private var startPositionOverrideMs: Long? = null,
) : ViewModel() {

    /**
     * Factory for creating PlayerViewModel with manual dependency injection.
     */
    class Factory(
        private val itemId: String,
        private val jellyfinClient: JellyfinClient,
        private val appPreferences: dev.jausc.myflix.core.common.preferences.AppPreferences?,
        private val preferredAudioLanguage: String? = null,
        private val preferredSubtitleLanguage: String? = null,
        private val maxStreamingBitrateMbps: Int = 0,
        private val startPositionMs: Long? = null,
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
            ) as T
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Internal tracking
    private var currentItemId: String = itemId
    private var playbackStarted = false
    private var markedAsPlayed = false
    private var controlsHideJob: Job? = null
    private var countdownJob: Job? = null
    private var lastKnownPositionMs: Long = 0L

    init {
        // Check if we're in queue mode
        val isQueueMode = PlayQueueManager.isQueueMode()
        _uiState.update { it.copy(isQueueMode = isQueueMode) }
        loadItem()
    }

    /**
     * Load the item and prepare for playback.
     */
    private fun loadItem() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            jellyfinClient.getItem(currentItemId)
                .onSuccess { loadedItem ->
                    val mediaSource = loadedItem.mediaSources?.firstOrNull()
                    val mediaStreams = mediaSource?.mediaStreams.orEmpty()
                    val audioStreams = mediaStreams.filter { it.type == "Audio" }.sortedBy { it.index }
                    val subtitleStreams = mediaStreams.filter { it.type == "Subtitle" }.sortedBy { it.index }
                    val defaultAudioIndex = selectAudioStreamIndex(audioStreams)
                    val defaultSubtitleIndex = selectSubtitleStreamIndex(subtitleStreams)
                    val queueItem = PlayQueueManager.getCurrentItem()?.takeIf { it.itemId == currentItemId }
                    val selectedAudioIndex = queueItem?.audioStreamIndex ?: defaultAudioIndex
                    val selectedSubtitleIndex = queueItem?.subtitleStreamIndex ?: defaultSubtitleIndex

                    // Get stream URL with proper session tracking (uses PlaybackInfo API)
                    val maxBitrate = maxStreamingBitrateMbps.takeIf { it > 0 }
                    val isTranscoding = maxBitrate != null
                    val (streamUrl, playSessionId) = jellyfinClient.getStreamUrlWithSession(
                        itemId = currentItemId,
                        mediaSourceId = mediaSource?.id,
                        audioStreamIndex = selectedAudioIndex,
                        subtitleStreamIndex = selectedSubtitleIndex,
                        maxBitrateMbps = maxBitrate,
                    ).getOrElse {
                        // Fallback to simple URL on error
                        android.util.Log.w("PlayerViewModel", "PlaybackInfo failed, using fallback URL", it)
                        Pair(jellyfinClient.getStreamUrl(currentItemId, selectedAudioIndex, selectedSubtitleIndex), null)
                    }
                    val playMethod = if (isTranscoding) "Transcode" else "DirectPlay"

                    val defaultStartPositionMs = loadedItem.userData?.playbackPositionTicks?.let {
                        it / TICKS_PER_MS
                    } ?: 0L
                    val startPositionMs = startPositionOverrideMs ?: defaultStartPositionMs
                    startPositionOverrideMs = null

                    _uiState.update {
                        it.copy(
                            item = loadedItem,
                            streamUrl = streamUrl,
                            startPositionMs = startPositionMs,
                            isLoading = false,
                            mediaSourceId = mediaSource?.id,
                            audioStreams = audioStreams,
                            subtitleStreams = subtitleStreams,
                            selectedAudioStreamIndex = selectedAudioIndex,
                            selectedSubtitleStreamIndex = selectedSubtitleIndex,
                            playSessionId = playSessionId,
                            playMethod = playMethod,
                            maxStreamingBitrate = maxBitrate?.let { it.toLong() * 1_000_000L },
                            // Clear segments from previous item
                            mediaSegments = emptyList(),
                            activeSegment = null,
                        )
                    }

                    // Load media segments (async, non-blocking)
                    loadMediaSegments(currentItemId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Failed to load item",
                            isLoading = false,
                        )
                    }
                }
        }
    }

    /**
     * Set player ready state.
     */
    fun setPlayerReady(ready: Boolean) {
        _uiState.update { it.copy(playerReady = ready) }
    }

    /**
     * Called when playback actually starts.
     * Reports playback start to the server.
     */
    fun onPlaybackStarted(positionMs: Long) {
        if (playbackStarted) return
        playbackStarted = true

        viewModelScope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            val state = _uiState.value
            jellyfinClient.reportPlaybackStart(
                currentItemId,
                mediaSourceId = state.mediaSourceId,
                positionTicks = positionTicks,
                playSessionId = state.playSessionId,
                playMethod = state.playMethod,
                audioStreamIndex = state.selectedAudioStreamIndex,
                subtitleStreamIndex = state.selectedSubtitleStreamIndex,
                maxStreamingBitrate = state.maxStreamingBitrate,
            )

            // Persist active session for crash recovery
            appPreferences?.setActivePlaybackSession(currentItemId, positionTicks, state.mediaSourceId)
        }
    }

    /**
     * Report playback progress.
     * Called periodically during playback.
     */
    fun reportProgress(positionMs: Long, isPaused: Boolean) {
        if (!playbackStarted) return

        // Track position for cleanup reporting
        lastKnownPositionMs = positionMs

        viewModelScope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            val state = _uiState.value
            jellyfinClient.reportPlaybackProgress(
                currentItemId,
                positionTicks,
                isPaused,
                mediaSourceId = state.mediaSourceId,
                playMethod = state.playMethod,
                audioStreamIndex = state.selectedAudioStreamIndex,
                subtitleStreamIndex = state.selectedSubtitleStreamIndex,
                maxStreamingBitrate = state.maxStreamingBitrate,
            )

            // Update persisted session position for crash recovery
            appPreferences?.setActivePlaybackSession(currentItemId, positionTicks, state.mediaSourceId)
        }
    }

    /**
     * Report pause state change.
     */
    fun onPauseStateChanged(positionMs: Long, isPaused: Boolean) {
        if (!playbackStarted) return

        viewModelScope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            val state = _uiState.value
            jellyfinClient.reportPlaybackProgress(
                currentItemId,
                positionTicks,
                isPaused,
                mediaSourceId = state.mediaSourceId,
                playMethod = state.playMethod,
                audioStreamIndex = state.selectedAudioStreamIndex,
                subtitleStreamIndex = state.selectedSubtitleStreamIndex,
                maxStreamingBitrate = state.maxStreamingBitrate,
            )
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
            viewModelScope.launch {
                jellyfinClient.setPlayed(currentItemId, true)
            }
        }
    }

    /**
     * Report playback stopped.
     * Should be called synchronously on dispose (use runBlocking if needed).
     */
    private suspend fun reportPlaybackStopped(positionMs: Long) {
        val positionTicks = positionMs * TICKS_PER_MS
        val mediaSourceId = _uiState.value.mediaSourceId
        android.util.Log.d("PlayerViewModel", "reportPlaybackStopped: itemId=$currentItemId position=${positionMs}ms")
        jellyfinClient.reportPlaybackStopped(currentItemId, positionTicks, mediaSourceId = mediaSourceId)

        // Clear persisted session since we properly reported stop
        appPreferences?.clearActivePlaybackSession()
        android.util.Log.d("PlayerViewModel", "reportPlaybackStopped: completed and cleared session")
    }

    /**
     * Stop playback and report to server. Call this before navigation.
     * Uses runBlocking to ensure the network call completes.
     */
    fun stopPlayback(currentPositionMs: Long) {
        if (!playbackStarted) {
            android.util.Log.d("PlayerViewModel", "stopPlayback: playback not started, skipping report")
            return
        }
        android.util.Log.d("PlayerViewModel", "stopPlayback: reporting stop at ${currentPositionMs}ms")
        kotlinx.coroutines.runBlocking {
            reportPlaybackStopped(currentPositionMs)
        }
        playbackStarted = false // Prevent duplicate reporting in onCleared
    }

    fun setAudioStreamIndex(index: Int?) {
        _uiState.update { it.copy(selectedAudioStreamIndex = index) }
    }

    fun setSubtitleStreamIndex(index: Int?) {
        _uiState.update { it.copy(selectedSubtitleStreamIndex = index) }
    }

    /**
     * Show controls and reset auto-hide timer.
     */
    fun showControls() {
        _uiState.update { it.copy(showControls = true) }
        resetControlsHideTimer()
    }

    /**
     * Hide controls.
     */
    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
        controlsHideJob?.cancel()
    }

    /**
     * Toggle controls visibility.
     */
    fun toggleControls() {
        if (_uiState.value.showControls) {
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
        controlsHideJob = viewModelScope.launch {
            delay(CONTROLS_AUTO_HIDE_MS)
            _uiState.update { it.copy(showControls = false) }
        }
    }

    // ==================== Queue Management ====================

    /**
     * Called when the current video ends.
     * Starts auto-play countdown if there's a next item in the queue.
     */
    fun onVideoEnded() {
        val nextItem = PlayQueueManager.peekNext()
        if (nextItem != null) {
            _uiState.update {
                it.copy(
                    showAutoPlayCountdown = true,
                    nextQueueItem = nextItem,
                    countdownSecondsRemaining = AUTO_PLAY_COUNTDOWN_SECONDS,
                )
            }
            startCountdown()
        }
    }

    /**
     * Start the auto-play countdown timer.
     */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (seconds in AUTO_PLAY_COUNTDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(countdownSecondsRemaining = seconds) }
                delay(1000)
            }
            // Countdown finished, advance to next
            advanceToNextInQueue()
        }
    }

    /**
     * Immediately play the next item in the queue (skip countdown).
     */
    fun playNextNow() {
        countdownJob?.cancel()
        advanceToNextInQueue()
    }

    /**
     * Advance to the next item in the queue.
     */
    private fun advanceToNextInQueue() {
        val nextItem = PlayQueueManager.advanceToNext()
        if (nextItem != null) {
            // Reset playback state for new item
            playbackStarted = false
            markedAsPlayed = false
            currentItemId = nextItem.itemId

            // Hide countdown and load new item
            _uiState.update {
                it.copy(
                    showAutoPlayCountdown = false,
                    nextQueueItem = null,
                    isLoading = true,
                    streamUrl = null,
                    startPositionMs = 0L,
                    playerReady = false,
                )
            }

            loadItemById(nextItem.itemId)
        }
    }

    /**
     * Cancel the queue and hide countdown.
     * Called when user presses Cancel or Back during countdown.
     */
    fun cancelQueue() {
        countdownJob?.cancel()
        PlayQueueManager.clear()
        _uiState.update {
            it.copy(
                showAutoPlayCountdown = false,
                nextQueueItem = null,
                isQueueMode = false,
            )
        }
    }

    /**
     * Load a specific item by ID (for queue advancement).
     */
    private fun loadItemById(newItemId: String) {
        viewModelScope.launch {
            jellyfinClient.getItem(newItemId)
                .onSuccess { loadedItem ->
                    val mediaSource = loadedItem.mediaSources?.firstOrNull()
                    val mediaStreams = mediaSource?.mediaStreams.orEmpty()
                    val audioStreams = mediaStreams.filter { it.type == "Audio" }.sortedBy { it.index }
                    val subtitleStreams = mediaStreams.filter { it.type == "Subtitle" }.sortedBy { it.index }
                    val defaultAudioIndex = selectAudioStreamIndex(audioStreams)
                    val defaultSubtitleIndex = selectSubtitleStreamIndex(subtitleStreams)
                    val queueItem = PlayQueueManager.getCurrentItem()?.takeIf { it.itemId == newItemId }
                    val selectedAudioIndex = queueItem?.audioStreamIndex ?: defaultAudioIndex
                    val selectedSubtitleIndex = queueItem?.subtitleStreamIndex ?: defaultSubtitleIndex

                    // Get stream URL with proper session tracking (uses PlaybackInfo API)
                    val maxBitrate = maxStreamingBitrateMbps.takeIf { it > 0 }
                    val isTranscoding = maxBitrate != null
                    val (streamUrl, playSessionId) = jellyfinClient.getStreamUrlWithSession(
                        itemId = newItemId,
                        mediaSourceId = mediaSource?.id,
                        audioStreamIndex = selectedAudioIndex,
                        subtitleStreamIndex = selectedSubtitleIndex,
                        maxBitrateMbps = maxBitrate,
                    ).getOrElse {
                        android.util.Log.w("PlayerViewModel", "PlaybackInfo failed, using fallback URL", it)
                        Pair(jellyfinClient.getStreamUrl(newItemId, selectedAudioIndex, selectedSubtitleIndex), null)
                    }
                    val playMethod = if (isTranscoding) "Transcode" else "DirectPlay"

                    val startPositionMs = loadedItem.userData?.playbackPositionTicks?.let {
                        it / TICKS_PER_MS
                    } ?: 0L

                    _uiState.update {
                        it.copy(
                            item = loadedItem,
                            streamUrl = streamUrl,
                            startPositionMs = startPositionMs,
                            isLoading = false,
                            mediaSourceId = mediaSource?.id,
                            audioStreams = audioStreams,
                            subtitleStreams = subtitleStreams,
                            selectedAudioStreamIndex = selectedAudioIndex,
                            selectedSubtitleStreamIndex = selectedSubtitleIndex,
                            playSessionId = playSessionId,
                            playMethod = playMethod,
                            maxStreamingBitrate = maxBitrate?.let { it.toLong() * 1_000_000L },
                            // Clear segments from previous item
                            mediaSegments = emptyList(),
                            activeSegment = null,
                        )
                    }

                    // Load media segments (async, non-blocking)
                    loadMediaSegments(newItemId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Failed to load next item",
                            isLoading = false,
                            showAutoPlayCountdown = false,
                        )
                    }
                }
        }
    }

    /**
     * Select the best audio stream index based on user preference.
     * Priority:
     * 1. Audio stream matching preferred language
     * 2. Server-marked default audio stream
     * 3. First available audio stream
     */
    private fun selectAudioStreamIndex(audioStreams: List<MediaStream>): Int? {
        // Try to find stream matching preferred language
        if (preferredAudioLanguage != null) {
            val matchingStream = audioStreams.firstOrNull {
                it.language.equals(preferredAudioLanguage, ignoreCase = true)
            }
            if (matchingStream != null) {
                return matchingStream.index
            }
        }

        // Fall back to server default or first available
        return audioStreams.firstOrNull { it.isDefault }?.index
            ?: audioStreams.firstOrNull()?.index
    }

    /**
     * Select the best subtitle stream index based on user preference.
     * Priority:
     * 1. Subtitle stream matching preferred language
     * 2. Server-marked default subtitle stream
     * 3. null (no subtitles by default unless user selects one)
     */
    private fun selectSubtitleStreamIndex(subtitleStreams: List<MediaStream>): Int? {
        // Try to find stream matching preferred language
        if (preferredSubtitleLanguage != null) {
            val matchingStream = subtitleStreams.firstOrNull {
                it.language.equals(preferredSubtitleLanguage, ignoreCase = true)
            }
            if (matchingStream != null) {
                return matchingStream.index
            }
        }

        // Fall back to server default (don't auto-enable if no preference)
        return subtitleStreams.firstOrNull { it.isDefault }?.index
    }

    // ==================== Media Segments ====================

    /**
     * Load media segments for the current item.
     * Called after item is loaded to fetch intro/outro markers.
     */
    private fun loadMediaSegments(itemId: String) {
        viewModelScope.launch {
            jellyfinClient.getMediaSegments(itemId)
                .onSuccess { segments ->
                    _uiState.update { it.copy(mediaSegments = segments) }
                }
                .onFailure {
                    // Silently fail - segments are optional
                    _uiState.update { it.copy(mediaSegments = emptyList()) }
                }
        }
    }

    /**
     * Update the active segment based on current playback position.
     * Should be called periodically during playback.
     *
     * @param positionMs Current playback position in milliseconds
     */
    fun updateActiveSegment(positionMs: Long) {
        val segments = _uiState.value.mediaSegments
        val currentActive = _uiState.value.activeSegment

        // Find segment containing current position (only Intro and Outro are skippable)
        val newActiveSegment = segments.find { segment ->
            segment.containsPosition(positionMs) &&
                segment.type in listOf(MediaSegmentType.Intro, MediaSegmentType.Outro)
        }

        // Only update if changed
        if (newActiveSegment?.id != currentActive?.id) {
            _uiState.update { it.copy(activeSegment = newActiveSegment) }
        }
    }

    /**
     * Get the skip target position for the active segment.
     * Returns the end position of the active segment, or null if no active segment.
     */
    fun getSkipTargetMs(): Long? {
        return _uiState.value.activeSegment?.endMs
    }

    /**
     * Get a user-friendly label for the skip button.
     */
    fun getSkipButtonLabel(): String {
        return when (_uiState.value.activeSegment?.type) {
            MediaSegmentType.Intro -> "Skip Intro"
            MediaSegmentType.Outro -> "Skip Outro"
            MediaSegmentType.Recap -> "Skip Recap"
            MediaSegmentType.Preview -> "Skip Preview"
            else -> "Skip"
        }
    }

    override fun onCleared() {
        super.onCleared()
        controlsHideJob?.cancel()
        countdownJob?.cancel()

        // Report playback stopped if not already done via stopPlayback()
        if (playbackStarted && lastKnownPositionMs > 0) {
            android.util.Log.d("PlayerViewModel", "onCleared: reporting stop (fallback)")
            kotlinx.coroutines.runBlocking {
                reportPlaybackStopped(lastKnownPositionMs)
            }
            playbackStarted = false
        } else {
            android.util.Log.d("PlayerViewModel", "onCleared: stop already reported or not started")
        }

        // Clear queue when player is destroyed
        PlayQueueManager.clear()
    }
}
