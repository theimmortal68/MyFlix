package dev.jausc.myflix.core.player

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        Log.d(TAG, "Initializing ExoPlayer backend (passthrough=$audioPassthroughMode)")
        val exoPlayerWrapper = ExoPlayerWrapper(context, audioPassthroughMode)
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

        // ==================== Refresh Rate Management ====================

        /**
         * Get available display modes with their refresh rates.
         * Requires API 23+ (Marshmallow).
         */
        fun getAvailableRefreshRates(context: Context): List<RefreshRateInfo> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return emptyList()
            }

            return try {
                val display = getDisplay(context) ?: return emptyList()
                val modes = display.supportedModes

                modes.map { mode ->
                    RefreshRateInfo(
                        modeId = mode.modeId,
                        refreshRate = mode.refreshRate,
                        width = mode.physicalWidth,
                        height = mode.physicalHeight,
                    )
                }.distinctBy { it.refreshRate.toInt() }
                    .sortedBy { it.refreshRate }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get available refresh rates", e)
                emptyList()
            }
        }

        /**
         * Apply refresh rate based on the mode setting.
         *
         * @param activity The activity whose window will be modified
         * @param mode Refresh rate mode: "OFF", "AUTO", "60", "120"
         * @param videoFrameRate Video frame rate for AUTO mode (e.g., 23.976, 24, 25, 30, 60)
         */
        fun applyRefreshRate(activity: Activity, mode: String, videoFrameRate: Float = 0f) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.d(TAG, "Refresh rate switching not supported below API 23")
                return
            }

            when (mode) {
                "OFF" -> resetRefreshRate(activity)
                "AUTO" -> setRefreshRateForVideo(activity, videoFrameRate)
                "60" -> setRefreshRate(activity, 60f)
                "120" -> setRefreshRate(activity, 120f)
                else -> Log.w(TAG, "Unknown refresh rate mode: $mode")
            }
        }

        /**
         * Set display to a specific refresh rate.
         * Finds the best matching display mode for the target refresh rate.
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun setRefreshRate(activity: Activity, targetHz: Float) {
            try {
                val display = getDisplay(activity) ?: return
                val currentMode = display.mode
                val modes = display.supportedModes

                // Find mode with same resolution but target refresh rate
                val targetMode = modes
                    .filter {
                        it.physicalWidth == currentMode.physicalWidth &&
                            it.physicalHeight == currentMode.physicalHeight
                    }
                    .minByOrNull { abs(it.refreshRate - targetHz) }

                if (targetMode != null && abs(targetMode.refreshRate - targetHz) < 5f) {
                    Log.d(TAG, "Setting refresh rate to ${targetMode.refreshRate}Hz (mode ${targetMode.modeId})")
                    setPreferredDisplayMode(activity, targetMode.modeId)
                } else {
                    Log.d(TAG, "No suitable mode found for ${targetHz}Hz, available: ${modes.map { it.refreshRate }}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set refresh rate to ${targetHz}Hz", e)
            }
        }

        /**
         * Set refresh rate based on video frame rate.
         * Matches video FPS to optimal display refresh rate.
         */
        @RequiresApi(Build.VERSION_CODES.M)
        private fun setRefreshRateForVideo(activity: Activity, videoFps: Float) {
            if (videoFps <= 0f) {
                Log.d(TAG, "Invalid video FPS: $videoFps, skipping refresh rate adjustment")
                return
            }

            val targetHz = getOptimalRefreshRate(videoFps)
            Log.d(TAG, "Video FPS: $videoFps -> Target refresh rate: ${targetHz}Hz")
            setRefreshRate(activity, targetHz)
        }

        /**
         * Reset display to system default refresh rate.
         */
        fun resetRefreshRate(activity: Activity) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

            try {
                Log.d(TAG, "Resetting refresh rate to system default")
                setPreferredDisplayMode(activity, 0) // 0 = system default
            } catch (e: Exception) {
                Log.w(TAG, "Failed to reset refresh rate", e)
            }
        }

        /**
         * Calculate optimal refresh rate for a given video frame rate.
         */
        private fun getOptimalRefreshRate(videoFps: Float): Float {
            return when {
                // 23.976 / 24 fps -> 24Hz (or 48Hz if available)
                videoFps in 23.5f..24.5f -> 24f
                // 25 fps (PAL) -> 50Hz
                videoFps in 24.5f..25.5f -> 50f
                // 29.97 / 30 fps -> 60Hz
                videoFps in 29.5f..30.5f -> 60f
                // 50 fps -> 50Hz
                videoFps in 49.5f..50.5f -> 50f
                // 59.94 / 60 fps -> 60Hz
                videoFps in 59.5f..60.5f -> 60f
                // 120 fps -> 120Hz
                videoFps in 119.5f..120.5f -> 120f
                // Default to 60Hz for other frame rates
                else -> 60f
            }
        }

        /**
         * Set the preferred display mode on the activity window.
         */
        private fun setPreferredDisplayMode(activity: Activity, modeId: Int) {
            activity.runOnUiThread {
                val params = activity.window.attributes
                params.preferredDisplayModeId = modeId
                activity.window.attributes = params
            }
        }

        /**
         * Get the display for the given context.
         */
        @RequiresApi(Build.VERSION_CODES.M)
        private fun getDisplay(context: Context): Display? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
        }
    }
}

/**
 * Information about a display refresh rate mode.
 */
data class RefreshRateInfo(
    val modeId: Int,
    val refreshRate: Float,
    val width: Int,
    val height: Int,
) {
    /** Display-friendly label for this refresh rate */
    val label: String
        get() = "${refreshRate.toInt()}Hz"
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
