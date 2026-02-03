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
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dev.jausc.myflix.core.player.audio.DelayAudioProcessor
import dev.jausc.myflix.core.player.audio.NightModeAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Audio passthrough modes for ExoPlayer.
 */
enum class AudioPassthroughMode {
    /** Decode all audio via FFmpeg to PCM (best compatibility, no spatial audio) */
    OFF,
    /** Use passthrough for formats the device reports as supported (preserves Atmos/DTS:X) */
    AUTO,
    /** Always attempt passthrough, fallback to decode if unsupported */
    ALWAYS,
}

/**
 * ExoPlayer wrapper implementing UnifiedPlayer interface
 * Supports DTS/DTS-HD/DTS:X/TrueHD via Jellyfin's FFmpeg extension or passthrough to AVR.
 *
 * Audio handling:
 * - OFF: Use FFmpeg to decode DTS/TrueHD to PCM (works on all devices, loses Atmos/DTS:X spatial)
 * - AUTO: Passthrough to AVR if device reports support, otherwise decode
 * - ALWAYS: Force passthrough attempt (may fail if AVR doesn't support format)
 *
 * Night mode (DRC):
 * - When enabled, applies dynamic range compression to reduce volume differences
 * - Boosts quiet dialogue, compresses loud sounds for late-night viewing
 *
 * Audio delay:
 * - Adjustable audio delay from -500ms to +500ms
 * - Positive delays audio (audio plays later), negative advances audio
 */
@Suppress("TooManyFunctions")
@OptIn(UnstableApi::class)
class ExoPlayerWrapper(
    private val context: Context,
    private val audioPassthroughMode: AudioPassthroughMode = AudioPassthroughMode.OFF,
    nightModeEnabled: Boolean = false,
    initialAudioDelayMs: Long = 0L,
) : UnifiedPlayer {

    // Night mode audio processor for dynamic range compression
    private val nightModeProcessor = NightModeAudioProcessor().apply {
        setEnabled(nightModeEnabled)
    }

    // Audio delay processor for A/V sync adjustment
    private val delayProcessor = DelayAudioProcessor().apply {
        setDelayMs(initialAudioDelayMs)
    }
    /**
     * Custom RenderersFactory that uses FFmpeg for audio and video decoding.
     * This enables software decode of DTS, DTS-HD, DTS:X, TrueHD, HEVC, etc.
     * Also injects the night mode audio processor for dynamic range compression.
     */
    private inner class FfmpegRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
        init {
            // Enable decoder fallback for when hardware decoders fail
            setEnableDecoderFallback(true)
            // Use FFmpeg extension as fallback when MediaCodec fails
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
        }

        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink {
            // Build AudioSink with night mode processor in the chain
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(nightModeProcessor, delayProcessor))
                .build()
        }

        override fun buildAudioRenderers(
            context: Context,
            extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            enableDecoderFallback: Boolean,
            audioSink: AudioSink,
            eventHandler: Handler,
            eventListener: AudioRendererEventListener,
            out: ArrayList<Renderer>,
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
                out,
            )
        }
    }

    /**
     * Custom RenderersFactory for passthrough mode.
     * Injects the night mode audio processor for dynamic range compression.
     */
    private inner class PassthroughRenderersFactory(
        context: Context,
        enableDecoderFallback: Boolean,
    ) : DefaultRenderersFactory(context) {
        init {
            setEnableDecoderFallback(enableDecoderFallback)
            // PREFER: Use extension renderers (AV1 software) first, then hardware
            // This enables AV1 playback on devices like Shield TV without hardware AV1
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
        }

        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink {
            // Build AudioSink with night mode processor in the chain
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(nightModeProcessor, delayProcessor))
                .build()
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

    @Suppress("CyclomaticComplexMethod")
    override fun initialize(): Boolean {
        if (initialized) return true

        return try {
            // Log capabilities
            val ffmpegAvailable = isFfmpegAvailable()
            val av1Available = isAv1Available()
            val passthroughFormats = getPassthroughCapabilities(context)
            Log.d(TAG, "FFmpeg available: $ffmpegAvailable")
            Log.d(TAG, "AV1 software decoder available: $av1Available")
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

            // Choose renderers factory based on passthrough mode
            // All factories now use custom AudioSink with night mode processor
            val renderersFactory = when (audioPassthroughMode) {
                AudioPassthroughMode.OFF -> {
                    // Use FFmpeg to decode DTS/TrueHD to PCM
                    if (ffmpegAvailable) {
                        Log.d(TAG, "Using FFmpeg renderers (passthrough OFF)")
                        FfmpegRenderersFactory(context)
                    } else {
                        Log.w(TAG, "FFmpeg not available, using passthrough renderers")
                        PassthroughRenderersFactory(context, enableDecoderFallback = true)
                    }
                }
                AudioPassthroughMode.AUTO, AudioPassthroughMode.ALWAYS -> {
                    // Use platform decoders with passthrough capability for audio
                    // Enable extension renderers for AV1 software decode on devices without hardware AV1
                    Log.d(TAG, "Using passthrough renderers (mode=$audioPassthroughMode)")
                    PassthroughRenderersFactory(
                        context,
                        enableDecoderFallback = audioPassthroughMode == AudioPassthroughMode.AUTO,
                    )
                }
            }

            // Configure audio attributes for passthrough
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            player = ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .setAudioAttributes(audioAttributes, true)
                .build()
                .apply {
                    playWhenReady = true

                    addListener(createPlayerListener())
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

    private fun createPlayerListener() = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(
                isPlaying = isPlaying,
                isPaused = !isPlaying && _state.value.duration > 0,
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
                        duration = player?.duration ?: 0,
                    )
                }
                Player.STATE_ENDED -> {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isIdle = true,
                        isEnded = true,
                    )
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error", error)
            _state.value = _state.value.copy(
                error = error.message ?: "Playback error",
                isPlaying = false,
            )
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            Log.d(TAG, "Video size: ${videoSize.width}x${videoSize.height}")
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
            isEnded = false,
            error = null,
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
                        duration = p.duration.coerceAtLeast(0),
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
            val duration = it.duration
            // Skip seek if duration is not yet known (C.TIME_UNSET = Long.MIN_VALUE + 1)
            if (duration <= 0) return@let

            val newPosition = (it.currentPosition + offsetMs).coerceIn(0, duration)
            it.seekTo(newPosition)
        }
    }

    override fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    /**
     * Current subtitle style configuration.
     * For ExoPlayer, subtitle styling is typically applied via CaptionStyleCompat
     * in the UI layer when using client-side subtitle rendering.
     * When using Jellyfin's server-side subtitle burning (via URL parameters),
     * styling is controlled server-side and this has no effect.
     */
    var subtitleStyle: SubtitleStyle = SubtitleStyle.DEFAULT
        private set

    override fun setSubtitleStyle(style: SubtitleStyle) {
        subtitleStyle = style
        Log.d(TAG, "Subtitle style stored: size=${style.fontSize}, color=${style.fontColor}, opacity=${style.backgroundOpacity}")
        // Note: For client-side subtitle rendering, apply this to SubtitleView via CaptionStyleCompat
        // in the UI layer. Server-side subtitles are styled by Jellyfin, not the player.
    }

    /**
     * Enables or disables night mode (dynamic range compression).
     * Boosts quiet sounds and compresses loud sounds for late-night viewing.
     * Note: Change takes effect on next seek or track change.
     */
    fun setNightModeEnabled(enabled: Boolean) {
        nightModeProcessor.setEnabled(enabled)
        Log.d(TAG, "Night mode ${if (enabled) "enabled" else "disabled"} (pending)")
    }

    /**
     * Returns whether night mode is currently active.
     */
    fun isNightModeEnabled(): Boolean = nightModeProcessor.isEnabled()

    /**
     * Returns whether night mode is pending to be enabled (takes effect on next seek).
     */
    fun isNightModePending(): Boolean = nightModeProcessor.isPendingEnabled()

    /**
     * Sets the audio delay in milliseconds.
     * Positive values delay audio (audio plays later than video).
     * Negative values advance audio (audio plays earlier than video).
     * Range: -500ms to +500ms.
     * Note: Change takes effect on next seek or track change.
     */
    fun setAudioDelayMs(delayMs: Long) {
        delayProcessor.setDelayMs(delayMs)
        Log.d(TAG, "Audio delay set to ${delayMs}ms (pending)")
    }

    /**
     * Gets the currently pending audio delay in milliseconds.
     */
    fun getAudioDelayMs(): Long = delayProcessor.getPendingDelayMs()

    /**
     * Gets the currently active audio delay in milliseconds.
     */
    fun getActiveAudioDelayMs(): Long = delayProcessor.getActiveDelayMs()

    /**
     * Returns whether audio delay is currently being applied.
     */
    fun isAudioDelayActive(): Boolean = delayProcessor.isDelayActive()

    /**
     * Adjusts audio delay by the specified increment.
     * @param incrementMs Amount to adjust (positive or negative)
     */
    fun adjustAudioDelay(incrementMs: Long) {
        val newDelay = (delayProcessor.getPendingDelayMs() + incrementMs)
            .coerceIn(DelayAudioProcessor.MIN_DELAY_MS, DelayAudioProcessor.MAX_DELAY_MS)
        setAudioDelayMs(newDelay)
    }

    override fun release() {
        stopPositionTracking()
        scope.cancel()
        player?.release()
        player = null
        initialized = false
        Log.d(TAG, "ExoPlayer released")
    }

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
         * Check if AV1 software decoder (libgav1) is available
         */
        fun isAv1Available(): Boolean {
            return try {
                val av1Lib = Class.forName("androidx.media3.decoder.av1.Gav1Library")
                val isAvailable = av1Lib.getMethod("isAvailable").invoke(null) as Boolean
                Log.d(TAG, "AV1 (libgav1) library available: $isAvailable")
                isAvailable
            } catch (e: Exception) {
                Log.w(TAG, "AV1 library not found: ${e.message}")
                false
            }
        }

        /**
         * Get supported audio passthrough formats on this device
         * TODO: Use this for passthrough settings option
         */
        fun getPassthroughCapabilities(context: Context): String {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            val capabilities = AudioCapabilities.getCapabilities(context, audioAttributes, null)
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
}
