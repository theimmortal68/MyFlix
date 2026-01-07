package dev.jausc.myflix.core.player

import android.content.Context
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ExoPlayer wrapper implementing UnifiedPlayer interface
 */
class ExoPlayerWrapper(private val context: Context) : UnifiedPlayer {
    
    companion object {
        private const val TAG = "ExoPlayerWrapper"
    }
    
    private val _state = MutableStateFlow(PlaybackState(playerType = "ExoPlayer"))
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()
    
    private var player: ExoPlayer? = null
    private var initialized = false
    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var pendingUrl: String? = null
    private var pendingStartPosition: Long = 0
    
    val exoPlayer: ExoPlayer?
        get() = player
    
    override fun initialize(): Boolean {
        if (initialized) return true
        
        return try {
            player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.value = _state.value.copy(
                            isPlaying = isPlaying,
                            isPaused = !isPlaying && _state.value.duration > 0
                        )
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_IDLE -> {
                                _state.value = _state.value.copy(isIdle = true, isBuffering = false)
                            }
                            Player.STATE_BUFFERING -> {
                                _state.value = _state.value.copy(isBuffering = true, isIdle = false)
                            }
                            Player.STATE_READY -> {
                                _state.value = _state.value.copy(
                                    isBuffering = false,
                                    isIdle = false,
                                    duration = player?.duration ?: 0
                                )
                            }
                            Player.STATE_ENDED -> {
                                _state.value = _state.value.copy(
                                    isPlaying = false,
                                    isIdle = true
                                )
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error", error)
                        _state.value = _state.value.copy(
                            error = error.message ?: "Playback error",
                            isPlaying = false
                        )
                    }
                    
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        Log.d(TAG, "Video size: ${videoSize.width}x${videoSize.height}")
                    }
                })
            }
            
            initialized = true
            Log.d(TAG, "ExoPlayer initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer", e)
            _state.value = _state.value.copy(error = "ExoPlayer init failed: ${e.message}")
            false
        }
    }
    
    override fun attachSurface(surface: Surface) {
        player?.setVideoSurface(surface)
        Log.d(TAG, "Surface attached")
        
        // If we have a pending URL, start playback now
        pendingUrl?.let { url ->
            playInternal(url, pendingStartPosition)
            pendingUrl = null
            pendingStartPosition = 0
        }
    }
    
    override fun detachSurface() {
        player?.setVideoSurface(null)
        Log.d(TAG, "Surface detached")
    }
    
    override fun play(url: String, startPositionMs: Long) {
        if (!initialized) {
            _state.value = _state.value.copy(error = "Player not initialized")
            return
        }
        
        Log.d(TAG, "Play requested: $url, startPosition: ${startPositionMs}ms")
        playInternal(url, startPositionMs)
    }
    
    private fun playInternal(url: String, startPositionMs: Long) {
        player?.apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            
            if (startPositionMs > 0) {
                seekTo(startPositionMs)
            }
            
            playWhenReady = true
        }
        
        _state.value = _state.value.copy(
            isBuffering = true,
            isIdle = false,
            error = null
        )
        
        startPositionTracking()
    }
    
    private fun startPositionTracking() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            while (isActive) {
                player?.let { p ->
                    _state.value = _state.value.copy(
                        position = p.currentPosition,
                        duration = p.duration.coerceAtLeast(0)
                    )
                }
                delay(500)
            }
        }
    }
    
    private fun stopPositionTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }
    
    override fun pause() {
        player?.pause()
        _state.value = _state.value.copy(isPaused = true, isPlaying = false)
    }
    
    override fun resume() {
        player?.play()
        _state.value = _state.value.copy(isPaused = false, isPlaying = true)
    }
    
    override fun togglePause() {
        player?.let {
            if (it.isPlaying) pause() else resume()
        }
    }
    
    override fun stop() {
        stopPositionTracking()
        player?.stop()
        _state.value = _state.value.copy(isPlaying = false, isIdle = true, position = 0)
    }
    
    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }
    
    override fun seekRelative(offsetMs: Long) {
        player?.let {
            val newPosition = (it.currentPosition + offsetMs).coerceIn(0, it.duration)
            it.seekTo(newPosition)
        }
    }
    
    override fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }
    
    override fun release() {
        stopPositionTracking()
        scope.cancel()
        player?.release()
        player = null
        initialized = false
        Log.d(TAG, "ExoPlayer released")
    }
}
