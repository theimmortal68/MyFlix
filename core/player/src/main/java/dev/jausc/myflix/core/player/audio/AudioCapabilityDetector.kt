package dev.jausc.myflix.core.player.audio

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities

/**
 * Detects device audio passthrough capabilities for surround sound codecs.
 *
 * This class queries the system's AudioCapabilities to determine which
 * surround sound formats can be passed through to an external receiver/soundbar.
 *
 * Capabilities are cached after first detection for performance.
 *
 * Supported codecs:
 * - AC3 (Dolby Digital)
 * - E-AC3 (Dolby Digital Plus)
 * - E-AC3 JOC (Dolby Atmos over DD+)
 * - DTS
 * - DTS-HD (includes DTS-HD MA and DTS:X)
 * - TrueHD (Dolby TrueHD, includes Atmos)
 */
@UnstableApi
class AudioCapabilityDetector(private val context: Context) {
    private var cachedCapabilities: DeviceAudioCapabilities? = null

    /**
     * Device audio passthrough capabilities.
     *
     * @property supportsAc3 AC3 (Dolby Digital) passthrough supported
     * @property supportsEac3 E-AC3 (Dolby Digital Plus) passthrough supported
     * @property supportsEac3Joc E-AC3 JOC (Dolby Atmos over DD+) passthrough supported
     * @property supportsDts DTS passthrough supported
     * @property supportsDtsHd DTS-HD (includes DTS:X) passthrough supported
     * @property supportsTruehd Dolby TrueHD (includes Atmos) passthrough supported
     * @property maxChannelCount Maximum audio channel count supported
     */
    data class DeviceAudioCapabilities(
        val supportsAc3: Boolean = false,
        val supportsEac3: Boolean = false,
        val supportsEac3Joc: Boolean = false,
        val supportsDts: Boolean = false,
        val supportsDtsHd: Boolean = false,
        val supportsTruehd: Boolean = false,
        val maxChannelCount: Int = 2,
    ) {
        /**
         * Returns a human-readable list of supported passthrough formats.
         */
        fun getSupportedFormatsString(): String {
            val formats = mutableListOf<String>()
            if (supportsAc3) formats.add("AC3")
            if (supportsEac3) formats.add("E-AC3")
            if (supportsEac3Joc) formats.add("Atmos")
            if (supportsDts) formats.add("DTS")
            if (supportsDtsHd) formats.add("DTS-HD")
            if (supportsTruehd) formats.add("TrueHD")
            return if (formats.isEmpty()) "None" else formats.joinToString(", ")
        }

        /**
         * Returns true if any surround sound passthrough is supported.
         */
        fun hasAnyPassthroughSupport(): Boolean =
            supportsAc3 || supportsEac3 || supportsEac3Joc ||
                supportsDts || supportsDtsHd || supportsTruehd
    }

    /**
     * Detects device audio passthrough capabilities.
     *
     * @param forceRefresh If true, bypasses cache and re-queries the system
     * @return DeviceAudioCapabilities with detected support flags
     */
    fun detectCapabilities(forceRefresh: Boolean = false): DeviceAudioCapabilities {
        if (!forceRefresh && cachedCapabilities != null) {
            return cachedCapabilities!!
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val systemCaps = AudioCapabilities.getCapabilities(context, audioAttributes, null)

        val capabilities = DeviceAudioCapabilities(
            supportsAc3 = systemCaps.supportsEncoding(C.ENCODING_AC3),
            supportsEac3 = systemCaps.supportsEncoding(C.ENCODING_E_AC3),
            supportsEac3Joc = systemCaps.supportsEncoding(C.ENCODING_E_AC3_JOC),
            supportsDts = systemCaps.supportsEncoding(C.ENCODING_DTS),
            supportsDtsHd = systemCaps.supportsEncoding(C.ENCODING_DTS_HD),
            supportsTruehd = systemCaps.supportsEncoding(C.ENCODING_DOLBY_TRUEHD),
            maxChannelCount = systemCaps.maxChannelCount,
        )

        Log.d(TAG, "Detected audio capabilities: ${capabilities.getSupportedFormatsString()}")
        Log.d(TAG, "Max channel count: ${capabilities.maxChannelCount}")

        cachedCapabilities = capabilities
        return capabilities
    }

    /**
     * Clears the cached capabilities, forcing re-detection on next call.
     * Useful when audio output changes (e.g., HDMI reconnect).
     */
    fun clearCache() {
        cachedCapabilities = null
    }

    companion object {
        private const val TAG = "AudioCapabilityDetector"

        /**
         * Encoding constant to human-readable name mapping.
         */
        fun getEncodingName(encoding: Int): String = when (encoding) {
            C.ENCODING_AC3 -> "AC3"
            C.ENCODING_E_AC3 -> "E-AC3"
            C.ENCODING_E_AC3_JOC -> "E-AC3 JOC (Atmos)"
            C.ENCODING_DTS -> "DTS"
            C.ENCODING_DTS_HD -> "DTS-HD"
            C.ENCODING_DOLBY_TRUEHD -> "TrueHD"
            C.ENCODING_PCM_16BIT -> "PCM 16-bit"
            C.ENCODING_PCM_FLOAT -> "PCM Float"
            else -> "Unknown ($encoding)"
        }
    }
}
