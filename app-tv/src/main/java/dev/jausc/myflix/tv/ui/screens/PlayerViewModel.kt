package dev.jausc.myflix.tv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.videoStream
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
 * ViewModel for the TV player screen.
 * Manages item loading, playback reporting, and controls visibility.
 */
class PlayerViewModel(
    private val itemId: String,
    private val jellyfinClient: JellyfinClient,
) : ViewModel() {

    /**
     * Factory for creating PlayerViewModel with manual dependency injection.
     */
    class Factory(
        private val itemId: String,
        private val jellyfinClient: JellyfinClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(itemId, jellyfinClient) as T
        }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Internal tracking
    private var playbackStarted = false
    private var markedAsPlayed = false
    private var controlsHideJob: Job? = null

    init {
        loadItem()
    }

    /**
     * Load the item and prepare for playback.
     */
    private fun loadItem() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            jellyfinClient.getItem(itemId)
                .onSuccess { loadedItem ->
                    val streamUrl = jellyfinClient.getStreamUrl(itemId)
                    val startPositionMs = loadedItem.userData?.playbackPositionTicks?.let {
                        it / TICKS_PER_MS
                    } ?: 0L

                    _uiState.update {
                        it.copy(
                            item = loadedItem,
                            streamUrl = streamUrl,
                            startPositionMs = startPositionMs,
                            isLoading = false,
                        )
                    }
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
            jellyfinClient.reportPlaybackStart(itemId, positionTicks = positionTicks)
        }
    }

    /**
     * Report playback progress.
     * Called periodically during playback.
     */
    fun reportProgress(positionMs: Long, isPaused: Boolean) {
        if (!playbackStarted) return

        viewModelScope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            jellyfinClient.reportPlaybackProgress(itemId, positionTicks, isPaused)
        }
    }

    /**
     * Report pause state change.
     */
    fun onPauseStateChanged(positionMs: Long, isPaused: Boolean) {
        if (!playbackStarted) return

        viewModelScope.launch {
            val positionTicks = positionMs * TICKS_PER_MS
            jellyfinClient.reportPlaybackProgress(itemId, positionTicks, isPaused)
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
                jellyfinClient.setPlayed(itemId, true)
            }
        }
    }

    /**
     * Report playback stopped.
     * Should be called synchronously on dispose (use runBlocking if needed).
     */
    suspend fun reportPlaybackStopped(positionMs: Long) {
        val positionTicks = positionMs * TICKS_PER_MS
        jellyfinClient.reportPlaybackStopped(itemId, positionTicks)
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

    override fun onCleared() {
        super.onCleared()
        controlsHideJob?.cancel()
    }
}
