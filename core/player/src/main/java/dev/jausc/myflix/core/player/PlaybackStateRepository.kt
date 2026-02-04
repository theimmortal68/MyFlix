package dev.jausc.myflix.core.player

import dev.jausc.myflix.core.common.model.JellyfinItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the current global playback state.
 * Used by the Dream Service to show "Now Playing" info.
 */
sealed interface NowPlayingState {
    /**
     * No active playback.
     */
    data object None : NowPlayingState

    /**
     * Active playback in progress.
     *
     * @param item The media item being played
     * @param positionMs Current playback position in milliseconds
     * @param durationMs Total duration in milliseconds
     * @param isPaused Whether playback is paused
     * @param posterUrl URL for the item's poster/thumbnail image
     * @param backdropUrl URL for the item's backdrop image (optional)
     */
    data class Playing(
        val item: JellyfinItem,
        val positionMs: Long,
        val durationMs: Long,
        val isPaused: Boolean,
        val posterUrl: String?,
        val backdropUrl: String?,
    ) : NowPlayingState {
        val progress: Float
            get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

        val remainingMs: Long
            get() = (durationMs - positionMs).coerceAtLeast(0L)
    }
}

/**
 * Singleton repository for tracking global playback state.
 *
 * This allows components outside the player (like DreamService) to
 * observe whether media is currently playing and show appropriate UI.
 *
 * Usage:
 * - PlayerViewModel calls [startPlayback] when playback begins
 * - PlayerViewModel calls [updateProgress] periodically during playback
 * - PlayerViewModel calls [stopPlayback] when playback ends
 * - DreamViewModel collects [state] to show "Now Playing" info
 */
object PlaybackStateRepository {
    private val _state = MutableStateFlow<NowPlayingState>(NowPlayingState.None)
    val state: StateFlow<NowPlayingState> = _state.asStateFlow()

    /**
     * Called when playback starts.
     */
    fun startPlayback(
        item: JellyfinItem,
        durationMs: Long,
        posterUrl: String?,
        backdropUrl: String?,
    ) {
        _state.value = NowPlayingState.Playing(
            item = item,
            positionMs = 0L,
            durationMs = durationMs,
            isPaused = false,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
        )
    }

    /**
     * Called to update playback progress.
     */
    fun updateProgress(positionMs: Long, isPaused: Boolean) {
        val current = _state.value
        if (current is NowPlayingState.Playing) {
            _state.value = current.copy(
                positionMs = positionMs,
                isPaused = isPaused,
            )
        }
    }

    /**
     * Called when playback stops.
     */
    fun stopPlayback() {
        _state.value = NowPlayingState.None
    }

    /**
     * Check if there's active playback (playing or paused).
     */
    val isActive: Boolean
        get() = _state.value is NowPlayingState.Playing
}
