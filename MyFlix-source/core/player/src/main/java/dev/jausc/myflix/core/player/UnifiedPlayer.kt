package dev.jausc.myflix.core.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Unified player state shared across all player implementations
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val isIdle: Boolean = true,
    val position: Long = 0L,        // milliseconds
    val duration: Long = 0L,        // milliseconds
    val speed: Float = 1.0f,
    val error: String? = null,
    val playerType: String = "Unknown"
) {
    val progress: Float
        get() = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    
    val remainingTime: Long
        get() = (duration - position).coerceAtLeast(0L)
}

/**
 * Unified player interface - implemented by both MPV and ExoPlayer wrappers
 */
interface UnifiedPlayer {
    val state: StateFlow<PlaybackState>
    
    fun initialize(): Boolean
    fun release()
    
    fun play(url: String, startPositionMs: Long = 0)
    fun pause()
    fun resume()
    fun togglePause()
    fun stop()
    
    fun seekTo(positionMs: Long)
    fun seekRelative(offsetMs: Long)
    
    fun setSpeed(speed: Float)
    
    // Optional features - may not be supported by all backends
    fun cycleAudio() {}
    fun cycleSubtitles() {}
    fun setAudioTrack(trackId: Int) {}
    fun setSubtitleTrack(trackId: Int) {}
    
    // Surface management
    fun attachSurface(surface: android.view.Surface)
    fun detachSurface()
}

/**
 * Player backend type
 */
enum class PlayerBackend {
    MPV,
    EXOPLAYER
}
