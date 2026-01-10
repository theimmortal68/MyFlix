package dev.jausc.myflix.core.player

import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.Surface
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ExoPlayer wrapper implementing UnifiedPlayer interface
 * Supports DTS/DTS-HD/DTS:X/TrueHD via Jellyfin's FFmpeg extension
 * 
 * TODO: Add Audio Passthrough option in settings for users with AVR/soundbar
 *       that can decode DTS natively. When enabled, skip FFmpeg decoding and
 *       pass raw bitstream to audio output.
 */
class ExoPlayerWrapper(private val context: Context) : UnifiedPlayer {
    
    companion object {
        private const val TAG = "ExoPlayerWrapper"
        
        /**
         * Check if FFmpeg audio decoder is available
         */
        fun isFfmpegAvailable(): Boolean {
            return try {
                val ffmpegLib = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegLibrary")
                val isAvailable = ffmpegLib.getMethod("isAvailable").invoke(null) as Boolean
                Log.d(TAG, "FFmpeg library available: $isAvailable")
                isAvailable
            } catch (e: Exception) {
                Log.w(TAG, "FFmpeg library not found: ${e.message}")
                false
            }
        }
        
        /**
         * Get supported audio passthrough formats on this device
         * TODO: Use this for passthrough settings option
         */
        fun getPassthroughCapabilities(context: Context): String {
            val capabilities = AudioCapabilities.getCapabilities(context)
            val formats = mutableListOf<String>()
            
            if (capabilities.supportsEncoding(C.ENCODING_AC3)) formats.add("AC3")
            if (capabilities.supportsEncoding(C.ENCODING_E_AC3)) formats.add("E-AC3")
            if (capabilities.supportsEncoding(C.ENCODING_E_AC3_JOC)) formats.add("Atmos")
            if (capabilities.supportsEncoding(C.ENCODING_DTS)) formats.add("DTS")
            if (capabilities.supportsEncoding(C.ENCODING_DTS_HD)) formats.add("DTS-HD")
            if (capabilities.supportsEncoding(C.ENCODING_DOLBY_TRUEHD)) formats.add("TrueHD")
            
            return if (formats.isEmpty()) "None" else formats.joinToString(", ")
        }
    }
    
    /**
     * Custom RenderersFactory that uses FFmpeg for audio and video decoding
     * This enables software decode of DTS, DTS-HD, DTS:X, TrueHD, HEVC, etc.
     */
    private class FfmpegRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

        init {
            // Enable decoder fallback for when hardware decoders fail
            setEnableDecoderFallback(true)
            // Use FFmpeg extension as fallback when MediaCodec fails
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
        }

        override fun buildAudioRenderers(
            context: Context,
            extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            enableDecoderFallback: Boolean,
            audioSink: AudioSink,
            eventHandler: Handler,
            eventListener: AudioRendererEventListener,
            out: ArrayList<Renderer>
        ) {
            // Add FFmpeg audio renderer FIRST for DTS/TrueHD support
            // This decodes to PCM which any device can play
            try {
                out.add(FfmpegAudioRenderer(eventHandler, eventListener, audioSink))
                Log.d(TAG, "FFmpeg audio renderer added (DTS/TrueHD decode enabled)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add FFmpeg renderer", e)
            }

            // Then add platform decoders as fallback
            super.buildAudioRenderers(
                context,
                extensionRendererMode,
                mediaCodecSelector,
                enableDecoderFallback,
                audioSink,
                eventHandler,
                eventListener,
                out
            )
        }
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
            // Log capabilities
            val ffmpegAvailable = isFfmpegAvailable()
            val passthroughFormats = getPassthroughCapabilities(context)
            Log.d(TAG, "FFmpeg available: $ffmpegAvailable")
            Log.d(TAG, "Passthrough capabilities: $passthroughFormats")
            
            // Configure track selector with fallback options
            val trackSelector = DefaultTrackSelector(context).apply {
                parameters = buildUponParameters()
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    // Allow trying decoders even when they report format as unsupported
                    // This helps with emulators and devices with limited hardware decoders
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .build()
            }
            
            // Use FFmpeg renderers factory for DTS/TrueHD/HEVC software decode
            // TODO: Add option to use DefaultRenderersFactory with passthrough
            //       for users with AVR that can decode DTS natively
            val renderersFactory = if (ffmpegAvailable) {
                FfmpegRenderersFactory(context)
            } else {
                Log.w(TAG, "FFmpeg not available, using default renderers")
                DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            }
            
            player = ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    /* handleAudioFocus= */ true
                )
                .build().apply {
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
