package dev.jausc.myflix.core.player.audio

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities
import dev.jausc.myflix.core.player.AudioPassthroughMode

/**
 * Passthrough configuration for individual codecs.
 *
 * @property dtsEnabled Enable passthrough for DTS and DTS-HD
 * @property truehdEnabled Enable passthrough for Dolby TrueHD
 * @property eac3Enabled Enable passthrough for E-AC3 and Atmos (E-AC3 JOC)
 * @property ac3Enabled Enable passthrough for AC3 (Dolby Digital)
 */
data class PassthroughConfig(
    val dtsEnabled: Boolean = true,
    val truehdEnabled: Boolean = true,
    val eac3Enabled: Boolean = true,
    val ac3Enabled: Boolean = true,
)

/**
 * Helper class to build custom AudioCapabilities for selective passthrough.
 *
 * Based on the AudioPassthroughMode and per-codec toggles, this builds
 * an AudioCapabilities object that tells ExoPlayer which formats can
 * be passed through to the receiver.
 *
 * Modes:
 * - OFF: Returns null (use FFmpeg to decode all surround formats to PCM)
 * - AUTO: Only enable passthrough for formats the device reports as supported AND user has enabled
 * - ALWAYS: Enable passthrough for all user-enabled formats regardless of device detection
 *           (useful to override faulty device capability detection)
 */
@UnstableApi
object PassthroughHelper {
    private const val TAG = "PassthroughHelper"

    /**
     * Builds custom AudioCapabilities based on passthrough mode and per-codec settings.
     *
     * @param context Android context for capability detection
     * @param mode The global passthrough mode (OFF, AUTO, ALWAYS)
     * @param config Per-codec passthrough toggles
     * @return AudioCapabilities to use with DefaultAudioSink, or null if passthrough is disabled
     */
    fun buildAudioCapabilities(
        context: Context,
        mode: AudioPassthroughMode,
        config: PassthroughConfig,
    ): AudioCapabilities? {
        // OFF mode: return null to let ExoPlayer use FFmpeg decoding
        if (mode == AudioPassthroughMode.OFF) {
            Log.d(TAG, "Passthrough OFF - using FFmpeg decode")
            return null
        }

        val detector = AudioCapabilityDetector(context)
        val deviceCaps = detector.detectCapabilities()

        // Build list of encodings to enable
        val enabledEncodings = mutableListOf<Int>()

        // Always include PCM as fallback
        enabledEncodings.add(C.ENCODING_PCM_16BIT)

        // Helper to conditionally add an encoding
        fun addIfEnabled(encoding: Int, userEnabled: Boolean, deviceSupports: Boolean) {
            val shouldAdd = when (mode) {
                AudioPassthroughMode.OFF -> false
                AudioPassthroughMode.AUTO -> userEnabled && deviceSupports
                AudioPassthroughMode.ALWAYS -> userEnabled
            }
            if (shouldAdd) {
                enabledEncodings.add(encoding)
                val status = if (deviceSupports) "device supported" else "forced by user"
                Log.d(TAG, "Enabled ${AudioCapabilityDetector.getEncodingName(encoding)} ($status)")
            }
        }

        // AC3 (Dolby Digital)
        addIfEnabled(C.ENCODING_AC3, config.ac3Enabled, deviceCaps.supportsAc3)

        // E-AC3 (Dolby Digital Plus)
        addIfEnabled(C.ENCODING_E_AC3, config.eac3Enabled, deviceCaps.supportsEac3)

        // E-AC3 JOC (Dolby Atmos over DD+)
        addIfEnabled(C.ENCODING_E_AC3_JOC, config.eac3Enabled, deviceCaps.supportsEac3Joc)

        // DTS
        addIfEnabled(C.ENCODING_DTS, config.dtsEnabled, deviceCaps.supportsDts)

        // DTS-HD (includes DTS-HD MA and DTS:X)
        addIfEnabled(C.ENCODING_DTS_HD, config.dtsEnabled, deviceCaps.supportsDtsHd)

        // Dolby TrueHD (includes TrueHD Atmos)
        addIfEnabled(C.ENCODING_DOLBY_TRUEHD, config.truehdEnabled, deviceCaps.supportsTruehd)

        Log.d(
            TAG,
            "Built AudioCapabilities with ${enabledEncodings.size} encodings, " +
                "mode=$mode, maxChannels=${deviceCaps.maxChannelCount}",
        )

        return AudioCapabilities(enabledEncodings.toIntArray(), deviceCaps.maxChannelCount)
    }

    /**
     * Gets a human-readable summary of the current passthrough configuration.
     */
    fun getConfigSummary(
        context: Context,
        mode: AudioPassthroughMode,
        config: PassthroughConfig,
    ): String {
        if (mode == AudioPassthroughMode.OFF) {
            return "Passthrough disabled (decoding to PCM)"
        }

        val detector = AudioCapabilityDetector(context)
        val deviceCaps = detector.detectCapabilities()

        val enabled = mutableListOf<String>()

        if (config.ac3Enabled && (mode == AudioPassthroughMode.ALWAYS || deviceCaps.supportsAc3)) {
            enabled.add("AC3")
        }
        if (config.eac3Enabled && (mode == AudioPassthroughMode.ALWAYS || deviceCaps.supportsEac3)) {
            enabled.add("E-AC3")
        }
        if (config.eac3Enabled && (mode == AudioPassthroughMode.ALWAYS || deviceCaps.supportsEac3Joc)) {
            enabled.add("Atmos")
        }
        if (config.dtsEnabled && (mode == AudioPassthroughMode.ALWAYS || deviceCaps.supportsDts)) {
            enabled.add("DTS")
        }
        if (config.dtsEnabled && (mode == AudioPassthroughMode.ALWAYS || deviceCaps.supportsDtsHd)) {
            enabled.add("DTS-HD")
        }
        if (config.truehdEnabled && (mode == AudioPassthroughMode.ALWAYS || deviceCaps.supportsTruehd)) {
            enabled.add("TrueHD")
        }

        return if (enabled.isEmpty()) {
            "No passthrough formats enabled"
        } else {
            "Passthrough: ${enabled.joinToString(", ")}"
        }
    }
}
