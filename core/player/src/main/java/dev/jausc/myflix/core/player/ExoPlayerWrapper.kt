package dev.jausc.myflix.core.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.Surface
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dev.jausc.myflix.core.player.audio.DelayAudioProcessor
import dev.jausc.myflix.core.player.audio.NightModeAudioProcessor
import dev.jausc.myflix.core.player.audio.PassthroughConfig
import dev.jausc.myflix.core.player.audio.PassthroughHelper
import dev.jausc.myflix.core.player.audio.StereoDownmixProcessor
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
 *
 * Stereo downmix:
 * - When enabled, downmixes multi-channel audio to stereo
 * - Uses ITU-R BS.775-1 coefficients for proper surround downmix
 */
@Suppress("TooManyFunctions")
@OptIn(UnstableApi::class)
class ExoPlayerWrapper(
    private val context: Context,
    private val audioPassthroughMode: AudioPassthroughMode = AudioPassthroughMode.OFF,
    private val passthroughConfig: PassthroughConfig = PassthroughConfig(),
    nightModeEnabled: Boolean = false,
    initialAudioDelayMs: Long = 0L,
    stereoDownmixEnabled: Boolean = false,
) : UnifiedPlayer {

    // Stereo downmix processor (must be first - changes channel count)
    private val stereoDownmixProcessor = StereoDownmixProcessor().apply {
        setEnabled(stereoDownmixEnabled)
    }

    // Night mode audio processor for dynamic range compression
    private val nightModeProcessor = NightModeAudioProcessor().apply {
        setEnabled(nightModeEnabled)
    }

    // Audio delay processor for A/V sync adjustment
    private val delayProcessor = DelayAudioProcessor().apply {
        setDelayMs(initialAudioDelayMs)
    }

    // Subtitle delay controller for subtitle timing adjustment
    private val subtitleDelayController = SubtitleDelayController()
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
            // Build AudioSink with audio processors in the chain
            // Order: stereoDownmix (changes channels) -> nightMode (DRC) -> delay (sync)
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(stereoDownmixProcessor, nightModeProcessor, delayProcessor))
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
     * Uses PassthroughHelper to build custom AudioCapabilities based on per-codec settings.
     * Injects the night mode audio processor for dynamic range compression.
     *
     * IMPORTANT: Uses EXTENSION_RENDERER_MODE_PREFER for video (AV1 software decode)
     * but explicitly excludes FFmpeg audio renderers to ensure audio passthrough works.
     * FFmpeg would decode E-AC3/TrueHD to PCM instead of passing through.
     */
    private inner class PassthroughRenderersFactory(
        private val factoryContext: Context,
        enableDecoderFallback: Boolean,
        private val mode: AudioPassthroughMode,
        private val config: PassthroughConfig,
    ) : DefaultRenderersFactory(factoryContext) {
        init {
            setEnableDecoderFallback(enableDecoderFallback)
            // PREFER: Use extension renderers (AV1 software) first, then hardware
            // This enables AV1 playback on devices like Shield TV without hardware AV1
            // Note: We override buildAudioRenderers to exclude FFmpeg for audio passthrough
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
        }

        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink {
            // Build custom AudioCapabilities based on passthrough mode and per-codec settings
            val customCapabilities = PassthroughHelper.buildAudioCapabilities(
                factoryContext,
                mode,
                config,
            )

            // Build AudioSink with audio processors in the chain
            // Order: stereoDownmix (changes channels) -> nightMode (DRC) -> delay (sync)
            // Note: For passthrough, these processors are bypassed as the bitstream goes directly through
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(stereoDownmixProcessor, nightModeProcessor, delayProcessor))
                .apply {
                    // Set custom audio capabilities for selective passthrough
                    if (customCapabilities != null) {
                        setAudioCapabilities(customCapabilities)
                    }
                }
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
            // For passthrough mode, only use MediaCodecAudioRenderer (not FFmpeg)
            // FFmpeg would decode E-AC3/TrueHD to PCM instead of passing through to receiver/TV
            // The MediaCodecAudioRenderer with proper AudioCapabilities will handle passthrough
            out.add(
                MediaCodecAudioRenderer(
                    context,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    audioSink,
                ),
            )
            Log.d(TAG, "Audio renderers built for passthrough (MediaCodec only, no FFmpeg)")
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

    // External subtitle state
    private var currentVideoUrl: String? = null
    private var externalSubtitleUrl: String? = null
    private var externalSubtitleMimeType: String? = null
    private var externalSubtitleLanguage: String? = null
    private var externalSubtitleLabel: String? = null

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
                        PassthroughRenderersFactory(
                            context,
                            enableDecoderFallback = true,
                            mode = AudioPassthroughMode.OFF,
                            config = passthroughConfig,
                        )
                    }
                }
                AudioPassthroughMode.AUTO, AudioPassthroughMode.ALWAYS -> {
                    // Use platform decoders with passthrough capability for audio
                    // Enable extension renderers for AV1 software decode on devices without hardware AV1
                    Log.d(TAG, "Using passthrough renderers (mode=$audioPassthroughMode)")
                    Log.d(TAG, "Passthrough config: DTS=${passthroughConfig.dtsEnabled}, " +
                        "TrueHD=${passthroughConfig.truehdEnabled}, " +
                        "E-AC3=${passthroughConfig.eac3Enabled}, " +
                        "AC3=${passthroughConfig.ac3Enabled}")
                    PassthroughRenderersFactory(
                        context,
                        enableDecoderFallback = audioPassthroughMode == AudioPassthroughMode.AUTO,
                        mode = audioPassthroughMode,
                        config = passthroughConfig,
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

        override fun onCues(cueGroup: CueGroup) {
            subtitleDelayController.onCues(cueGroup)
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
        currentVideoUrl = url
        player?.apply {
            setMediaItem(buildMediaItem(url))
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

    /**
     * Build a MediaItem with optional external subtitle configuration.
     */
    private fun buildMediaItem(videoUrl: String): MediaItem {
        val builder = MediaItem.Builder().setUri(videoUrl)

        // Add external subtitle if configured
        val subtitleUrl = externalSubtitleUrl
        val mimeType = externalSubtitleMimeType
        if (subtitleUrl != null && mimeType != null) {
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                .setMimeType(mimeType)
                .apply {
                    externalSubtitleLanguage?.let { setLanguage(it) }
                    externalSubtitleLabel?.let { setLabel(it) }
                }
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setId("external")
                .build()
            builder.setSubtitleConfigurations(listOf(subtitleConfig))
            Log.d(TAG, "MediaItem built with external subtitle: $subtitleUrl")
        }

        return builder.build()
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

    override fun setMuted(muted: Boolean) {
        player?.volume = if (muted) 0f else 1f
    }

    override fun isMuted(): Boolean = player?.volume == 0f

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
     * Delayed subtitle cues to display.
     * Observe this in the UI layer instead of the player's cues directly
     * when subtitle delay is enabled.
     */
    val subtitleCues: StateFlow<List<Cue>>
        get() = subtitleDelayController.cues

    override fun setSubtitleDelayMs(delayMs: Long) {
        subtitleDelayController.setDelayMs(delayMs)
        Log.d(TAG, "Subtitle delay set to ${delayMs}ms")
    }

    /**
     * Returns the current subtitle delay in milliseconds.
     */
    fun getSubtitleDelayMs(): Long = subtitleDelayController.getDelayMs()

    // ==================== External Subtitle Support ====================

    /**
     * Load an external subtitle from URL.
     * This triggers a soft restart to reload the media with the subtitle sideloaded.
     *
     * @param url The subtitle file URL
     * @param mimeType The MIME type (e.g., "application/x-subrip", "text/x-ssa")
     * @param language Optional language code
     * @param label Optional display label
     */
    override fun addExternalSubtitle(url: String, mimeType: String, language: String?, label: String?) {
        Log.d(TAG, "Adding external subtitle: $url (mimeType: $mimeType)")
        externalSubtitleUrl = url
        externalSubtitleMimeType = mimeType
        externalSubtitleLanguage = language
        externalSubtitleLabel = label

        // Perform soft restart if currently playing
        val videoUrl = currentVideoUrl
        if (videoUrl != null && player != null) {
            softRestartWithSubtitle()
        }
    }

    /**
     * Clear any loaded external subtitle.
     * This triggers a soft restart to reload the media without the subtitle.
     */
    override fun clearExternalSubtitle() {
        Log.d(TAG, "Clearing external subtitle")
        externalSubtitleUrl = null
        externalSubtitleMimeType = null
        externalSubtitleLanguage = null
        externalSubtitleLabel = null

        // Perform soft restart if currently playing
        val videoUrl = currentVideoUrl
        if (videoUrl != null && player != null) {
            softRestartWithSubtitle()
        }
    }

    /**
     * Soft restart: rebuild MediaItem and re-prepare while preserving position.
     */
    private fun softRestartWithSubtitle() {
        val videoUrl = currentVideoUrl ?: return
        val currentPosition = player?.currentPosition ?: 0L
        val wasPlaying = player?.isPlaying == true

        Log.d(TAG, "Soft restart at position ${currentPosition}ms, wasPlaying=$wasPlaying")

        player?.apply {
            setMediaItem(buildMediaItem(videoUrl))
            prepare()
            if (currentPosition > 0) {
                seekTo(currentPosition)
            }
            playWhenReady = wasPlaying
        }
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

    // ==================== Stereo Downmix Control ====================

    /**
     * Enables or disables stereo downmix.
     * When enabled, multi-channel audio (5.1, 7.1) is downmixed to stereo
     * using ITU-R BS.775-1 coefficients.
     * Change takes effect on next seek or track change.
     */
    fun setStereoDownmixEnabled(enabled: Boolean) {
        stereoDownmixProcessor.setEnabled(enabled)
        Log.d(TAG, "Stereo downmix set to $enabled (pending)")
    }

    /**
     * Returns whether stereo downmix is pending to be enabled.
     */
    fun isStereoDownmixEnabled(): Boolean = stereoDownmixProcessor.isPendingEnabled()

    /**
     * Returns whether stereo downmix is currently active.
     */
    fun isStereoDownmixActive(): Boolean = stereoDownmixProcessor.isEnabled()

    /**
     * Sets whether to include LFE (subwoofer) channel in stereo downmix.
     * When enabled, LFE is mixed at -10dB to both channels.
     */
    fun setStereoDownmixIncludeLfe(include: Boolean) {
        stereoDownmixProcessor.setIncludeLfe(include)
        Log.d(TAG, "Stereo downmix LFE set to $include (pending)")
    }

    /**
     * Returns whether LFE is included in stereo downmix.
     */
    fun isStereoDownmixLfeIncluded(): Boolean = stereoDownmixProcessor.isLfeIncluded()

    override fun release() {
        stopPositionTracking()
        subtitleDelayController.release()
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
