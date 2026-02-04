package dev.jausc.myflix.core.player

import android.content.Context
import android.util.Log
import android.view.Surface
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MPV Player implementation using libmpv native libraries.
 *
 * Uses prebuilt libraries from mpv-android project.
 */
@Suppress("TooManyFunctions", "StringLiteralDuplication")
class MpvPlayer(private val context: Context) : UnifiedPlayer, MPVLib.EventObserver {
    private val _state = MutableStateFlow(PlaybackState(playerType = "MPV"))
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var initialized = false
    private var mpvCreated = false
    private var currentSurface: Surface? = null
    private var pendingUrl: String? = null
    private var pendingStartPosition: Long = 0

    companion object {
        private const val TAG = "MpvPlayer"

        /**
         * Check if MPV native libraries are available
         */
        fun isAvailable(): Boolean {
            return try {
                System.loadLibrary("mpv")
                System.loadLibrary("player")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "MPV libraries not available", e)
                false
            }
        }

        /**
         * Check if running on an emulator (for development fallback)
         */
        fun isEmulator(): Boolean {
            return (android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                android.os.Build.BRAND.startsWith("generic") ||
                android.os.Build.DEVICE.startsWith("generic") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu") ||
                android.os.Build.PRODUCT.contains("sdk") ||
                android.os.Build.PRODUCT.contains("emulator"))
        }
    }

    // Just mark as ready - actual init happens when surface is attached
    // This allows mediacodec_embed to work properly
    override fun initialize(): Boolean = true

    private fun initializeMpv(surface: Surface): Boolean {
        if (initialized) return true

        val isEmulator = isEmulator()
        Log.d(TAG, "Initializing MPV (isEmulator=$isEmulator)")

        return try {
            if (!mpvCreated) {
                MPVLib.create(context)
                mpvCreated = true
            }

            if (isEmulator) {
                // Emulator: Use GPU-based software rendering (goldfish hw decoder is unreliable)
                Log.d(TAG, "Using GPU software rendering for emulator")
                MPVLib.setOptionString("vo", "gpu")
                MPVLib.setOptionString("gpu-context", "android")
                MPVLib.setOptionString("hwdec", "no") // Force software decoding
                
                // Set the surface as window ID
                val surfaceId = System.identityHashCode(surface).toLong()
                MPVLib.setOptionString("wid", surfaceId.toString())
            } else {
                // Real device: Use mediacodec_embed for direct hardware rendering
                // Must set wid (surface) before init for mediacodec_embed to work
                MPVLib.setOptionString("vo", "mediacodec_embed,gpu")
                MPVLib.setOptionString("gpu-context", "android")

                // Set the surface as window ID - critical for mediacodec_embed
                val surfaceId = System.identityHashCode(surface).toLong()
                MPVLib.setOptionString("wid", surfaceId.toString())

                // Hardware decoding - direct rendering
                MPVLib.setOptionString("hwdec", "mediacodec")
                MPVLib.setOptionString("hwdec-codecs", "all")
            }

            // Aspect ratio - maintain original, fit within surface
            MPVLib.setOptionString("keepaspect", "yes")
            MPVLib.setOptionString("video-aspect-override", "-1") // Use container/stream aspect
            MPVLib.setOptionString("panscan", "0.0") // No cropping

            // Audio output
            MPVLib.setOptionString("ao", "audiotrack,opensles")

            // Network/streaming - larger buffers for 4K
            MPVLib.setOptionString("tls-verify", "no")
            MPVLib.setOptionString("cache", "yes")
            MPVLib.setOptionString("cache-secs", "120")
            MPVLib.setOptionString("demuxer-max-bytes", "150MiB")
            MPVLib.setOptionString("demuxer-max-back-bytes", "75MiB")
            MPVLib.setOptionString("demuxer-readahead-secs", "20")

            // Initialization
            MPVLib.setOptionString("force-window", "yes")
            MPVLib.setOptionString("idle", "yes")

            MPVLib.init()
            MPVLib.addObserver(this)

            // Observe properties for state updates
            MPVLib.observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            MPVLib.observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            MPVLib.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("seeking", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("eof-reached", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("speed", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("video-params/w", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            MPVLib.observeProperty("video-params/h", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            MPVLib.observeProperty("video-params/aspect", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)

            // Attach surface after init
            MPVLib.attachSurface(surface)

            initialized = true
            Log.d(TAG, "MPV initialized successfully with ${if (isEmulator) "GPU software" else "mediacodec_embed hardware"} rendering")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MPV", e)
            _state.value = _state.value.copy(error = "MPV init failed: ${e.message}")
            false
        }
    }

    override fun attachSurface(surface: Surface) {
        currentSurface = surface

        if (!initialized) {
            // Initialize MPV now that we have a surface
            if (initializeMpv(surface)) {
                // If there was a pending play request, execute it now
                pendingUrl?.let { url ->
                    play(url, pendingStartPosition)
                    pendingUrl = null
                    pendingStartPosition = 0
                }
            }
        } else {
            try {
                MPVLib.attachSurface(surface)
                Log.d(TAG, "Surface attached")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach surface", e)
            }
        }
    }

    override fun detachSurface() {
        currentSurface = null
        if (!initialized) return
        try {
            MPVLib.setPropertyString("vo", "null")
            MPVLib.detachSurface()
            Log.d(TAG, "Surface detached")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detach surface", e)
        }
    }

    override fun play(url: String, startPositionMs: Long) {
        if (!initialized) {
            // Queue the play request until surface is ready
            pendingUrl = url
            pendingStartPosition = startPositionMs
            Log.d(TAG, "Play queued, waiting for surface")
            return
        }

        Log.d(TAG, "Playing: $url, startPosition: ${startPositionMs}ms")

        try {
            // Set start position if resuming
            if (startPositionMs > 0) {
                val startSeconds = startPositionMs / 1000.0
                MPVLib.setOptionString("start", startSeconds.toString())
            }

            MPVLib.command(arrayOf("loadfile", url))

            _state.value = _state.value.copy(
                isBuffering = true,
                isIdle = false,
                error = null,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play", e)
            _state.value = _state.value.copy(error = "Playback failed: ${e.message}")
        }
    }

    override fun pause() {
        if (!initialized) return
        MPVLib.setPropertyBoolean("pause", true)
    }

    override fun resume() {
        if (!initialized) return
        MPVLib.setPropertyBoolean("pause", false)
    }

    override fun togglePause() {
        if (!initialized) return
        val paused = MPVLib.getPropertyBoolean("pause") ?: false
        MPVLib.setPropertyBoolean("pause", !paused)
    }

    override fun stop() {
        if (!initialized) return
        MPVLib.command(arrayOf("stop"))
        _state.value = _state.value.copy(isPlaying = false, isIdle = true, position = 0)
    }

    override fun seekTo(positionMs: Long) {
        if (!initialized) return
        val positionSec = positionMs / 1000.0
        MPVLib.command(arrayOf("seek", positionSec.toString(), "absolute"))
    }

    override fun seekRelative(offsetMs: Long) {
        if (!initialized) return
        val offsetSec = offsetMs / 1000.0
        MPVLib.command(arrayOf("seek", offsetSec.toString(), "relative"))
    }

    override fun setSpeed(speed: Float) {
        if (!initialized) return
        MPVLib.setPropertyDouble("speed", speed.toDouble())
        _state.value = _state.value.copy(speed = speed)
    }

    override fun cycleAudio() {
        if (!initialized) return
        MPVLib.command(arrayOf("cycle", "audio"))
    }

    override fun cycleSubtitles() {
        if (!initialized) return
        MPVLib.command(arrayOf("cycle", "sub"))
    }

    override fun setAudioTrack(trackId: Int) {
        if (!initialized) return
        MPVLib.setPropertyInt("aid", trackId)
    }

    override fun setSubtitleTrack(trackId: Int) {
        if (!initialized) return
        if (trackId == PlayerConstants.TRACK_DISABLED) {
            MPVLib.setPropertyString("sid", "no")
        } else {
            MPVLib.setPropertyInt("sid", trackId)
        }
    }

    /**
     * Add an external audio track from a URL.
     * Used for YouTube split streams where video and audio are separate.
     *
     * @param audioUrl URL of the external audio track
     * @param select Whether to automatically select this audio track
     */
    fun addExternalAudio(audioUrl: String, select: Boolean = true) {
        if (!initialized) {
            Log.w(TAG, "Cannot add external audio - MPV not initialized")
            return
        }
        try {
            // audio-add <url> [flags] [title] [lang]
            // flags: select, auto, cached
            val flags = if (select) "select" else "auto"
            MPVLib.command(arrayOf("audio-add", audioUrl, flags))
            Log.d(TAG, "Added external audio: $audioUrl (select=$select)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add external audio", e)
        }
    }

    override fun setSubtitleStyle(style: SubtitleStyle) {
        if (!initialized) return
        try {
            // Font size (MPV uses points, our enum has mpvPoints)
            MPVLib.setPropertyInt("sub-font-size", style.fontSize.mpvPoints)

            // Font color (MPV format: "#RRGGBB")
            MPVLib.setPropertyString("sub-color", style.mpvFontColor)

            // Background color with opacity (MPV format: "#AARRGGBB")
            MPVLib.setPropertyString("sub-back-color", style.mpvBackgroundColor)

            // Add outline for better readability
            MPVLib.setPropertyString("sub-border-color", "#FF000000") // Black outline
            MPVLib.setPropertyInt("sub-border-size", 2)

            Log.d(TAG, "Subtitle style applied: size=${style.fontSize.mpvPoints}, color=${style.mpvFontColor}, bg=${style.mpvBackgroundColor}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply subtitle style", e)
        }
    }

    override fun setSubtitleDelayMs(delayMs: Long) {
        if (!initialized) return
        try {
            // MPV sub-delay is in seconds, positive = delay subtitles (show later)
            val delaySeconds = delayMs / 1000.0
            MPVLib.setPropertyDouble("sub-delay", delaySeconds)
            Log.d(TAG, "Subtitle delay set to ${delayMs}ms (${delaySeconds}s)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set subtitle delay", e)
        }
    }

    override fun addExternalSubtitle(url: String, mimeType: String, language: String?, label: String?) {
        if (!initialized) {
            Log.w(TAG, "Cannot add external subtitle - MPV not initialized")
            return
        }
        try {
            // sub-add <url> [flags] [title] [lang]
            // flags: select, auto, cached
            // MPV handles ASS/SSA/SRT natively, mimeType is ignored
            val title = label ?: ""
            val lang = language ?: ""
            MPVLib.command(arrayOf("sub-add", url, "select", title, lang))
            Log.d(TAG, "Added external subtitle: $url (language=$language, label=$label)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add external subtitle", e)
        }
    }

    override fun clearExternalSubtitle() {
        if (!initialized) return
        try {
            // Disable subtitle track entirely
            MPVLib.setPropertyString("sid", "no")
            Log.d(TAG, "External subtitle cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear external subtitle", e)
        }
    }

    override fun release() {
        if (initialized) {
            MPVLib.removeObserver(this)
            MPVLib.destroy()
            initialized = false
            Log.d(TAG, "MPV destroyed")
        }
    }

    // MPVLib.EventObserver implementation

    override fun eventProperty(property: String) {
        // Property changed but no value provided
    }

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "time-pos" -> {
                _state.value = _state.value.copy(position = value * 1000)
            }
            "duration" -> {
                _state.value = _state.value.copy(duration = value * 1000)
            }
            "video-params/w" -> {
                Log.d(TAG, "Video width: $value")
                _state.value = _state.value.copy(videoWidth = value.toInt())
            }
            "video-params/h" -> {
                Log.d(TAG, "Video height: $value")
                _state.value = _state.value.copy(videoHeight = value.toInt())
            }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> {
                _state.value = _state.value.copy(
                    isPaused = value,
                    isPlaying = !value && !_state.value.isIdle,
                )
            }
            "paused-for-cache" -> {
                _state.value = _state.value.copy(isBuffering = value)
            }
            "seeking" -> {
                _state.value = _state.value.copy(isBuffering = value)
            }
            "eof-reached" -> {
                if (value) {
                    _state.value = _state.value.copy(isPlaying = false, isIdle = true)
                }
            }
        }
    }

    override fun eventProperty(property: String, value: String) {
        when (property) {
            "media-title" -> {
                Log.d(TAG, "Media title: $value")
            }
        }
    }

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "speed" -> {
                _state.value = _state.value.copy(speed = value.toFloat())
            }
            "time-pos" -> {
                _state.value = _state.value.copy(position = (value * 1000).toLong())
            }
            "duration" -> {
                _state.value = _state.value.copy(duration = (value * 1000).toLong())
            }
            "video-params/aspect" -> {
                Log.d(TAG, "Video aspect ratio: $value")
                _state.value = _state.value.copy(videoAspectRatio = value.toFloat())
            }
        }
    }

    override fun event(eventId: Int) {
        when (eventId) {
            MPVLib.MpvEvent.MPV_EVENT_START_FILE -> {
                _state.value = _state.value.copy(isIdle = false, isBuffering = true)
                Log.d(TAG, "Event: START_FILE")
            }
            MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                _state.value = _state.value.copy(isPlaying = true, isBuffering = false)
                Log.d(TAG, "Event: FILE_LOADED")
            }
            MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                _state.value = _state.value.copy(isPlaying = true, isBuffering = false)
                Log.d(TAG, "Event: PLAYBACK_RESTART")
            }
            MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                _state.value = _state.value.copy(isPlaying = false, isIdle = true)
                Log.d(TAG, "Event: END_FILE")
            }
            MPVLib.MpvEvent.MPV_EVENT_SEEK -> {
                Log.d(TAG, "Event: SEEK")
            }
        }
    }
}
