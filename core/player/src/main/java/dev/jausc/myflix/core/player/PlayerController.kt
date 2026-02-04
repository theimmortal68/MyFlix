package dev.jausc.myflix.core.player

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import dev.jausc.myflix.core.player.audio.PassthroughConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PlayerController - manages player backend selection and provides unified interface
 *
 * Backend selection strategy:
 * - Default: ExoPlayer (best compatibility)
 * - If useMpv=true: MPV for non-DV content (better codec support)
 * - Dolby Vision content + DV-capable device -> Always ExoPlayer
 *
 * Audio passthrough:
 * - OFF: Decode DTS/TrueHD via FFmpeg to PCM (all devices)
 * - AUTO: Passthrough to AVR if supported, otherwise decode
 * - ALWAYS: Force passthrough (requires compatible AVR)
 */
@Suppress("TooManyFunctions")
class PlayerController(
    private val context: Context,
    private val useMpv: Boolean = false,
    private val audioPassthroughMode: AudioPassthroughMode = AudioPassthroughMode.OFF,
    private val passthroughConfig: PassthroughConfig = PassthroughConfig(),
) {
    private var currentPlayer: UnifiedPlayer? = null
    private var _backend: PlayerBackend = PlayerBackend.EXOPLAYER
    private var currentMediaInfo: MediaInfo? = null
    private val isDvCapable: Boolean by lazy { isDeviceDolbyVisionCapable(context) }

    // Single state flow that forwards from current player
    private val _state = MutableStateFlow(PlaybackState(playerType = "Initializing..."))
    private var stateForwardingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val backend: PlayerBackend
        get() = _backend

    val state: StateFlow<PlaybackState>
        get() = _state.asStateFlow()

    private fun startForwardingState(player: UnifiedPlayer) {
        stateForwardingJob?.cancel()
        stateForwardingJob = scope.launch {
            player.state.collect { playerState ->
                _state.value = playerState
            }
        }
    }

    val isInitialized: Boolean
        get() = currentPlayer != null

    /**
     * Whether this device supports Dolby Vision
     */
    val deviceSupportsDolbyVision: Boolean
        get() = isDvCapable

    /**
     * Get the underlying ExoPlayer instance (for use with PlayerView)
     * Returns null if using MPV backend or not initialized
     */
    val exoPlayer: androidx.media3.exoplayer.ExoPlayer?
        get() = (currentPlayer as? ExoPlayerWrapper)?.exoPlayer

    /**
     * Initialize the player with automatic backend selection.
     * Call this before attachSurface/play, or use initializeForMedia() for content-aware selection.
     */
    fun initialize(): Boolean {
        if (currentPlayer != null) return true // Already initialized

        // Default to ExoPlayer, use MPV only if enabled and available
        return if (useMpv && MpvPlayer.isAvailable()) {
            initializeMpv()
        } else {
            initializeExoPlayer()
        }
    }

    /**
     * Initialize with content-aware backend selection.
     *
     * Logic:
     * - DV content + DV-capable device → ExoPlayer (proper DV playback)
     * - useMpv enabled + MPV available → MPV
     * - Otherwise → ExoPlayer (default)
     */
    fun initializeForMedia(mediaInfo: MediaInfo): Boolean {
        currentMediaInfo = mediaInfo

        // Always use ExoPlayer for DV content on DV-capable device
        val forceDvExoPlayer = mediaInfo.isDolbyVision && isDvCapable
        // Use MPV only if enabled, available, and not forced to ExoPlayer for DV
        val shouldUseMpv = useMpv && MpvPlayer.isAvailable() && !forceDvExoPlayer

        Log.d(
            TAG,
            "Media: ${mediaInfo.title}, isDV: ${mediaInfo.isDolbyVision}, " +
                "deviceDV: $isDvCapable, useMpvPref: $useMpv, shouldUseMpv: $shouldUseMpv",
        )

        // Release existing player if switching backends
        val currentIsMpv = currentPlayer is MpvPlayer

        if (currentPlayer != null) {
            if (!shouldUseMpv && currentIsMpv) {
                Log.d(TAG, "Switching from MPV to ExoPlayer")
                release()
            } else if (shouldUseMpv && !currentIsMpv) {
                Log.d(TAG, "Switching from ExoPlayer to MPV")
                release()
            } else {
                return true // Already using correct backend
            }
        }

        return if (shouldUseMpv) {
            Log.d(
                TAG,
                "Using MPV for: ${mediaInfo.title}" +
                    if (mediaInfo.isDolbyVision) " (DV content, but device not DV-capable - using HDR10 layer)" else "",
            )
            initializeMpv()
        } else {
            Log.d(
                TAG,
                "Using ExoPlayer for: ${mediaInfo.title}" +
                    if (forceDvExoPlayer) " (Dolby Vision)" else "",
            )
            initializeExoPlayer()
        }
    }

    private fun initializeMpv(): Boolean {
        Log.d(TAG, "Initializing MPV backend")
        val mpvPlayer = MpvPlayer(context)
        return if (mpvPlayer.initialize()) {
            currentPlayer = mpvPlayer
            _backend = PlayerBackend.MPV
            startForwardingState(mpvPlayer)
            Log.d(TAG, "MPV backend initialized successfully")
            true
        } else {
            Log.w(TAG, "MPV initialization failed, falling back to ExoPlayer")
            initializeExoPlayer()
        }
    }

    private fun initializeExoPlayer(): Boolean {
        Log.d(TAG, "Initializing ExoPlayer backend (passthrough=$audioPassthroughMode, config=$passthroughConfig)")
        val exoPlayerWrapper = ExoPlayerWrapper(context, audioPassthroughMode, passthroughConfig)
        return if (exoPlayerWrapper.initialize()) {
            currentPlayer = exoPlayerWrapper
            _backend = PlayerBackend.EXOPLAYER
            startForwardingState(exoPlayerWrapper)
            Log.d(TAG, "ExoPlayer backend initialized successfully")
            true
        } else {
            Log.e(TAG, "ExoPlayer initialization failed")
            false
        }
    }

    fun attachSurface(surface: Surface) {
        currentPlayer?.attachSurface(surface)
    }

    fun detachSurface() {
        currentPlayer?.detachSurface()
    }

    fun play(url: String, startPositionMs: Long = 0) {
        currentPlayer?.play(url, startPositionMs)
    }

    /**
     * Play with content-aware backend selection.
     * Automatically switches to ExoPlayer for DV content on DV-capable devices.
     */
    fun play(url: String, startPositionMs: Long = 0, mediaInfo: MediaInfo) {
        // Check if we need to switch backends
        val needsExoPlayer = mediaInfo.isDolbyVision && isDvCapable
        val currentIsMpv = currentPlayer is MpvPlayer

        if (needsExoPlayer && currentIsMpv) {
            Log.d(TAG, "DV content on DV device, switching to ExoPlayer")
            release()
            initializeExoPlayer()
            // Note: Surface needs to be reattached by the caller
        }

        currentMediaInfo = mediaInfo
        currentPlayer?.play(url, startPositionMs)
    }

    fun pause() {
        currentPlayer?.pause()
    }

    fun resume() {
        currentPlayer?.resume()
    }

    fun togglePause() {
        currentPlayer?.togglePause()
    }

    fun stop() {
        currentPlayer?.stop()
    }

    fun seekTo(positionMs: Long) {
        currentPlayer?.seekTo(positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        currentPlayer?.seekRelative(offsetMs)
    }

    fun setSpeed(speed: Float) {
        currentPlayer?.setSpeed(speed)
    }

    fun cycleAudio() {
        currentPlayer?.cycleAudio()
    }

    fun cycleSubtitles() {
        currentPlayer?.cycleSubtitles()
    }

    fun setSubtitleStyle(style: SubtitleStyle) {
        currentPlayer?.setSubtitleStyle(style)
    }

    // ==================== Audio Delay Control ====================

    /**
     * Sets the audio delay in milliseconds.
     * Positive values delay audio (audio plays later than video).
     * Negative values advance audio (audio plays earlier than video).
     * Range: -500ms to +500ms.
     * Note: Change takes effect on next seek or track change.
     */
    fun setAudioDelayMs(delayMs: Long) {
        (currentPlayer as? ExoPlayerWrapper)?.setAudioDelayMs(delayMs)
    }

    /**
     * Gets the currently pending audio delay in milliseconds.
     */
    fun getAudioDelayMs(): Long {
        return (currentPlayer as? ExoPlayerWrapper)?.getAudioDelayMs() ?: 0L
    }

    /**
     * Adjusts audio delay by the specified increment.
     * @param incrementMs Amount to adjust (positive or negative)
     */
    fun adjustAudioDelay(incrementMs: Long) {
        (currentPlayer as? ExoPlayerWrapper)?.adjustAudioDelay(incrementMs)
    }

    /**
     * Returns whether audio delay is currently being applied.
     */
    fun isAudioDelayActive(): Boolean {
        return (currentPlayer as? ExoPlayerWrapper)?.isAudioDelayActive() ?: false
    }

    // ==================== Subtitle Delay Control ====================

    /**
     * Sets the subtitle delay in milliseconds.
     * Positive values delay subtitles (show later).
     * Negative values advance subtitles (show earlier, best effort).
     * Range: -10000ms to +10000ms.
     *
     * Works with both ExoPlayer and MPV backends.
     */
    fun setSubtitleDelayMs(delayMs: Long) {
        currentPlayer?.setSubtitleDelayMs(delayMs)
    }

    /**
     * Gets the current subtitle delay in milliseconds.
     * Returns 0 if not set or not supported.
     */
    fun getSubtitleDelayMs(): Long {
        return when (val player = currentPlayer) {
            is ExoPlayerWrapper -> player.getSubtitleDelayMs()
            // MPV doesn't have a getter, so return 0 (delay is tracked at UI level)
            else -> 0L
        }
    }

    /**
     * Adjusts subtitle delay by the specified increment.
     * @param incrementMs Amount to adjust (positive or negative)
     */
    fun adjustSubtitleDelay(incrementMs: Long) {
        val currentDelay = getSubtitleDelayMs()
        val newDelay = (currentDelay + incrementMs).coerceIn(-10_000L, 10_000L)
        setSubtitleDelayMs(newDelay)
    }

    /**
     * Get the subtitle cues StateFlow for ExoPlayer.
     * Returns null for MPV (uses native subtitle rendering).
     */
    val subtitleCues: kotlinx.coroutines.flow.StateFlow<List<androidx.media3.common.text.Cue>>?
        get() = (currentPlayer as? ExoPlayerWrapper)?.subtitleCues

    // ==================== Night Mode Control ====================

    /**
     * Enables or disables night mode (dynamic range compression).
     */
    fun setNightModeEnabled(enabled: Boolean) {
        (currentPlayer as? ExoPlayerWrapper)?.setNightModeEnabled(enabled)
    }

    /**
     * Returns whether night mode is currently enabled.
     */
    fun isNightModeEnabled(): Boolean {
        return (currentPlayer as? ExoPlayerWrapper)?.isNightModeEnabled() ?: false
    }

    // ==================== Stereo Downmix Control ====================

    /**
     * Enables or disables stereo downmix.
     * When enabled, multi-channel audio (5.1, 7.1) is downmixed to stereo
     * using ITU-R BS.775-1 coefficients.
     */
    fun setStereoDownmixEnabled(enabled: Boolean) {
        (currentPlayer as? ExoPlayerWrapper)?.setStereoDownmixEnabled(enabled)
    }

    /**
     * Returns whether stereo downmix is pending to be enabled.
     */
    fun isStereoDownmixEnabled(): Boolean {
        return (currentPlayer as? ExoPlayerWrapper)?.isStereoDownmixEnabled() ?: false
    }

    /**
     * Returns whether stereo downmix is currently active.
     */
    fun isStereoDownmixActive(): Boolean {
        return (currentPlayer as? ExoPlayerWrapper)?.isStereoDownmixActive() ?: false
    }

    /**
     * Sets whether to include LFE channel in stereo downmix.
     */
    fun setStereoDownmixIncludeLfe(include: Boolean) {
        (currentPlayer as? ExoPlayerWrapper)?.setStereoDownmixIncludeLfe(include)
    }

    /**
     * Returns whether LFE is included in stereo downmix.
     */
    fun isStereoDownmixLfeIncluded(): Boolean {
        return (currentPlayer as? ExoPlayerWrapper)?.isStereoDownmixLfeIncluded() ?: false
    }

    /**
     * Switch to MPV backend as a fallback when ExoPlayer fails.
     * Returns true if MPV was initialized.
     */
    fun switchToMpvForError(reason: String? = null): Boolean {
        if (currentPlayer is MpvPlayer) return true
        if (!MpvPlayer.isAvailable()) return false

        val isDvContent = currentMediaInfo?.isDolbyVision == true
        if (isDvContent && isDvCapable) {
            Log.w(TAG, "ExoPlayer error but DV content on DV device; keeping ExoPlayer")
            return false
        }

        Log.w(TAG, "Falling back to MPV${reason?.let { ": $it" } ?: ""}")
        release()
        return initializeMpv()
    }

    fun release() {
        stateForwardingJob?.cancel()
        stateForwardingJob = null
        currentPlayer?.release()
        currentPlayer = null
        currentMediaInfo = null
        _state.value = PlaybackState(playerType = "Released")
    }

    companion object {
        private const val TAG = "PlayerController"

        // HDR type constants from Display.HdrCapabilities
        private const val HDR_TYPE_DOLBY_VISION = 1
        private const val HDR_TYPE_HDR10 = 2
        private const val HDR_TYPE_HLG = 3
        private const val HDR_TYPE_HDR10_PLUS = 4

        /**
         * Get all supported HDR types for this device
         */
        fun getDeviceHdrCapabilities(context: Context): DeviceHdrCapabilities {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return DeviceHdrCapabilities()
            }

            return try {
                val supportedTypes = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                        getHdrTypesApi34(context)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        getHdrTypesApi30(context)
                    }
                    else -> {
                        getHdrTypesLegacy(context)
                    }
                }

                DeviceHdrCapabilities(
                    supportsDolbyVision = supportedTypes.contains(HDR_TYPE_DOLBY_VISION),
                    supportsHdr10 = supportedTypes.contains(HDR_TYPE_HDR10),
                    supportsHdr10Plus = supportedTypes.contains(HDR_TYPE_HDR10_PLUS),
                    supportsHlg = supportedTypes.contains(HDR_TYPE_HLG),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check HDR capabilities", e)
                DeviceHdrCapabilities()
            }
        }

        /**
         * Check if device supports Dolby Vision playback
         */
        fun isDeviceDolbyVisionCapable(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return false
            }

            return try {
                val supportedTypes = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                        // API 34+: Use mode-specific HDR types from context.display
                        getHdrTypesApi34(context)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        // API 30-33: Use context.display with HdrCapabilities
                        getHdrTypesApi30(context)
                    }
                    else -> {
                        // API 24-29: Use WindowManager.defaultDisplay
                        getHdrTypesLegacy(context)
                    }
                }

                val hasDV = supportedTypes.contains(HDR_TYPE_DOLBY_VISION)
                Log.d(TAG, "Device HDR capabilities: ${supportedTypes.toList()}, DV capable: $hasDV")
                hasDV
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check DV capability", e)
                false
            }
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private fun getHdrTypesApi34(context: Context): IntArray = context.display.mode.supportedHdrTypes

        @RequiresApi(Build.VERSION_CODES.R)
        private fun getHdrTypesApi30(context: Context): IntArray {
            // hdrCapabilities.supportedHdrTypes is deprecated in API 34+ in favor of
            // mode.supportedHdrTypes, but there's no alternative for API 30-33
            @Suppress("DEPRECATION")
            return context.display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        }

        private fun getHdrTypesLegacy(context: Context): IntArray {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // defaultDisplay and supportedHdrTypes are deprecated in API 30+ but there's
            // no alternative for API 24-29 (our min SDK is 25)
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay ?: return intArrayOf()

            @Suppress("DEPRECATION")
            return display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        }

        // ==================== Refresh Rate Debugging ====================

        /**
         * Get available display modes with their refresh rates for debugging.
         * Requires API 23+ (Marshmallow).
         *
         * Note: For actual refresh rate switching, use RefreshRateManager.
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun getAvailableRefreshRates(context: Context): List<String> {
            return try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display
                } else {
                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                } ?: return emptyList()

                val modes = display.supportedModes
                val currentModeId = display.mode.modeId

                modes.map { mode ->
                    val current = if (mode.modeId == currentModeId) " (current)" else ""
                    "${mode.physicalWidth}x${mode.physicalHeight} @ ${mode.refreshRate}Hz$current"
                }.sortedBy { it }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get available refresh rates", e)
                emptyList()
            }
        }
    }
}

/**
 * Device HDR capability information
 */
data class DeviceHdrCapabilities(
    val supportsDolbyVision: Boolean = false,
    val supportsHdr10: Boolean = false,
    val supportsHdr10Plus: Boolean = false,
    val supportsHlg: Boolean = false,
) {
    val supportsAnyHdr: Boolean
        get() = supportsDolbyVision || supportsHdr10 || supportsHdr10Plus || supportsHlg
}

/**
 * Media information for content-aware player selection
 */
data class MediaInfo(
    val title: String = "",
    val videoCodec: String? = null,
    val videoProfile: String? = null,
    val videoRangeType: String? = null, // "SDR", "HDR10", "HDR10Plus", "DolbyVision", "HLG"
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Long = 0,
) {
    /**
     * Check if content is Dolby Vision
     * Detection methods:
     * 1. videoRangeType contains "dolby" or "dv"
     * 2. videoCodec is "hevc" with DV profile
     * 3. videoProfile contains "dvhe" or "dvh1" or "dav1"
     */
    val isDolbyVision: Boolean
        get() {
            // Check video range type (most reliable - Jellyfin provides this)
            val rangeType = videoRangeType?.lowercase() ?: ""
            if (rangeType.contains("dolby") || rangeType.contains("dovi") || rangeType == "dv") {
                return true
            }

            // Check codec profile for DV indicators
            val profile = videoProfile?.lowercase() ?: ""
            if (profile.contains("dvhe") || // Dolby Vision HEVC
                profile.contains("dvh1") || // Dolby Vision HEVC (alternate)
                profile.contains("dav1") || // Dolby Vision AV1
                profile.contains("dolby vision")
            ) {
                return true
            }

            // Check codec string
            val codec = videoCodec?.lowercase() ?: ""
            if (codec.contains("dolby") || codec.contains("dovi")) {
                return true
            }

            return false
        }

    val isHdr: Boolean
        get() {
            val rangeType = videoRangeType?.lowercase() ?: ""
            return rangeType.contains("hdr") ||
                rangeType.contains("dolby") ||
                rangeType.contains("dovi") ||
                rangeType.contains("hlg") ||
                rangeType.contains("pq")
        }

    val is4K: Boolean
        get() = width >= 3840 || height >= 2160

    companion object {
        /**
         * Create MediaInfo from Jellyfin MediaStream data
         */
        fun fromJellyfinStream(
            title: String,
            codec: String?,
            profile: String?,
            videoRangeType: String?,
            width: Int,
            height: Int,
            bitrate: Long,
        ): MediaInfo {
            return MediaInfo(
                title = title,
                videoCodec = codec,
                videoProfile = profile,
                videoRangeType = videoRangeType,
                width = width,
                height = height,
                bitrate = bitrate,
            )
        }
    }
}
