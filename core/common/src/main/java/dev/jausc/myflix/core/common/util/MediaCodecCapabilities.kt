package dev.jausc.myflix.core.common.util

import android.content.Context
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.util.Size

/**
 * Detects device media codec capabilities for building accurate Jellyfin DeviceProfiles.
 *
 * This allows the app to:
 * - Include only codecs the device can actually decode
 * - Request appropriate transcoding when needed (e.g., HEVC to 8-bit HEVC instead of H.264)
 * - Set correct profile/level restrictions
 *
 * Usage:
 * ```
 * val capabilities = MediaCodecCapabilities(context)
 * if (capabilities.supportsHevcMain10()) {
 *     // Device can play 10-bit HEVC
 * }
 * ```
 */
@Suppress("UnusedPrivateProperty")
class MediaCodecCapabilities(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val mediaCodecList by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }

    // MIME types
    companion object {
        private const val TAG = "MediaCodecCapabilities"

        const val MIME_VIDEO_AVC = MediaFormat.MIMETYPE_VIDEO_AVC // "video/avc" (H.264)
        const val MIME_VIDEO_HEVC = MediaFormat.MIMETYPE_VIDEO_HEVC // "video/hevc" (H.265)
        const val MIME_VIDEO_VP8 = MediaFormat.MIMETYPE_VIDEO_VP8
        const val MIME_VIDEO_VP9 = MediaFormat.MIMETYPE_VIDEO_VP9
        const val MIME_VIDEO_AV1 = "video/av01" // AV1
        const val MIME_VIDEO_DOLBY_VISION = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
        const val MIME_VIDEO_VC1 = "video/vc1"

        // AVC levels as reported by ffprobe are multiplied by 10 (e.g., level 4.1 = 41)
        private val AVC_LEVELS = listOf(
            CodecProfileLevel.AVCLevel1b to 9,
            CodecProfileLevel.AVCLevel1 to 10,
            CodecProfileLevel.AVCLevel11 to 11,
            CodecProfileLevel.AVCLevel12 to 12,
            CodecProfileLevel.AVCLevel13 to 13,
            CodecProfileLevel.AVCLevel2 to 20,
            CodecProfileLevel.AVCLevel21 to 21,
            CodecProfileLevel.AVCLevel22 to 22,
            CodecProfileLevel.AVCLevel3 to 30,
            CodecProfileLevel.AVCLevel31 to 31,
            CodecProfileLevel.AVCLevel32 to 32,
            CodecProfileLevel.AVCLevel4 to 40,
            CodecProfileLevel.AVCLevel41 to 41,
            CodecProfileLevel.AVCLevel42 to 42,
            CodecProfileLevel.AVCLevel5 to 50,
            CodecProfileLevel.AVCLevel51 to 51,
            CodecProfileLevel.AVCLevel52 to 52,
        )

        // HEVC levels as reported by ffprobe are multiplied by 30 (e.g., level 4.1 = 123)
        private val HEVC_LEVELS = listOf(
            CodecProfileLevel.HEVCMainTierLevel1 to 30,
            CodecProfileLevel.HEVCMainTierLevel2 to 60,
            CodecProfileLevel.HEVCMainTierLevel21 to 63,
            CodecProfileLevel.HEVCMainTierLevel3 to 90,
            CodecProfileLevel.HEVCMainTierLevel31 to 93,
            CodecProfileLevel.HEVCMainTierLevel4 to 120,
            CodecProfileLevel.HEVCMainTierLevel41 to 123,
            CodecProfileLevel.HEVCMainTierLevel5 to 150,
            CodecProfileLevel.HEVCMainTierLevel51 to 153,
            CodecProfileLevel.HEVCMainTierLevel52 to 156,
            CodecProfileLevel.HEVCMainTierLevel6 to 180,
            CodecProfileLevel.HEVCMainTierLevel61 to 183,
            CodecProfileLevel.HEVCMainTierLevel62 to 186,
        )

        // AV1 profile constants (may not be available on older API levels)
        private val AV1_PROFILE_MAIN8 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CodecProfileLevel.AV1ProfileMain8
        } else {
            0x1
        }

        private val AV1_PROFILE_MAIN10 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CodecProfileLevel.AV1ProfileMain10
        } else {
            0x2
        }

        private val AV1_PROFILE_MAIN10_HDR10 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CodecProfileLevel.AV1ProfileMain10HDR10
        } else {
            0x1000
        }

        private val AV1_LEVEL_5 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CodecProfileLevel.AV1Level5
        } else {
            0x1000
        }

        // Dolby Vision profile for AV1 (Profile 10)
        private val DV_PROFILE_DVAV1_10 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            CodecProfileLevel.DolbyVisionProfileDvav110
        } else {
            0x400 // Literal value for older API levels
        }
    }

    // ==================== AVC (H.264) ====================

    /** Check if device has any AVC/H.264 decoder */
    fun supportsAvc(): Boolean = hasCodecForMime(MIME_VIDEO_AVC)

    /** Check if device supports AVC High 10 profile (10-bit H.264) */
    fun supportsAvcHigh10(): Boolean = hasDecoder(
        MIME_VIDEO_AVC,
        CodecProfileLevel.AVCProfileHigh10,
        CodecProfileLevel.AVCLevel4,
    )

    /** Get maximum supported AVC level for main profile */
    fun getAvcMainLevel(): Int = getAvcLevel(CodecProfileLevel.AVCProfileMain)

    /** Get maximum supported AVC level for high 10 profile */
    fun getAvcHigh10Level(): Int = getAvcLevel(CodecProfileLevel.AVCProfileHigh10)

    private fun getAvcLevel(profile: Int): Int {
        val level = getDecoderLevel(MIME_VIDEO_AVC, profile)
        return AVC_LEVELS.asReversed().find { it.first <= level }?.second ?: 0
    }

    /** Get supported AVC profiles as strings for DeviceProfile */
    fun getSupportedAvcProfiles(): List<String> = buildList {
        if (supportsAvc()) {
            add("high")
            add("main")
            add("baseline")
            add("constrained baseline")
            if (supportsAvcHigh10()) {
                add("high 10")
            }
        }
    }

    // ==================== HEVC (H.265) ====================

    /** Check if device has any HEVC decoder */
    fun supportsHevc(): Boolean = hasCodecForMime(MIME_VIDEO_HEVC, requireHardware = true)

    /** Check if device supports HEVC Main 10 profile (10-bit HEVC) */
    fun supportsHevcMain10(): Boolean = hasDecoder(
        MIME_VIDEO_HEVC,
        CodecProfileLevel.HEVCProfileMain10,
        CodecProfileLevel.HEVCMainTierLevel4,
        requireHardware = true,
    )

    /** Check if device supports HEVC HDR10 */
    @Suppress("DEPRECATION")
    fun supportsHevcHdr10(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        hasDecoder(
            MIME_VIDEO_HEVC,
            CodecProfileLevel.HEVCProfileMain10HDR10,
            CodecProfileLevel.HEVCMainTierLevel4,
            requireHardware = true,
        )

    /** Check if device supports HEVC HDR10+ */
    fun supportsHevcHdr10Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        hasDecoder(
            MIME_VIDEO_HEVC,
            CodecProfileLevel.HEVCProfileMain10HDR10Plus,
            CodecProfileLevel.HEVCMainTierLevel4,
            requireHardware = true,
        )

    /** Get maximum supported HEVC level for main profile */
    fun getHevcMainLevel(): Int = getHevcLevel(CodecProfileLevel.HEVCProfileMain)

    /** Get maximum supported HEVC level for main 10 profile */
    fun getHevcMain10Level(): Int = getHevcLevel(CodecProfileLevel.HEVCProfileMain10)

    private fun getHevcLevel(profile: Int): Int {
        val level = getDecoderLevel(MIME_VIDEO_HEVC, profile, requireHardware = true)
        return HEVC_LEVELS.asReversed().find { it.first <= level }?.second ?: 0
    }

    /** Get supported HEVC profiles as strings for DeviceProfile */
    fun getSupportedHevcProfiles(): List<String> = buildList {
        if (supportsHevc()) {
            add("main")
            if (supportsHevcMain10()) {
                add("main 10")
            }
        }
    }

    // ==================== AV1 ====================

    /** Check if device has any AV1 decoder (hardware or software like libgav1/dav1d) */
    fun supportsAv1(): Boolean = hasCodecForMime(MIME_VIDEO_AV1, requireHardware = false)

    /** Check if device supports AV1 Main 10 profile (10-bit AV1) */
    fun supportsAv1Main10(): Boolean = hasDecoder(
        MIME_VIDEO_AV1,
        AV1_PROFILE_MAIN10,
        AV1_LEVEL_5,
        requireHardware = false, // Allow software decoder (libdav1d/libgav1)
    )

    /** Check if device supports AV1 HDR10 */
    fun supportsAv1Hdr10(): Boolean = hasDecoder(
        MIME_VIDEO_AV1,
        AV1_PROFILE_MAIN10_HDR10,
        AV1_LEVEL_5,
        requireHardware = false, // Allow software decoder (libdav1d/libgav1)
    )

    /** Check if device supports AV1 Dolby Vision (DV Profile 10) */
    fun supportsAv1DolbyVision(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        hasDecoder(
            MIME_VIDEO_DOLBY_VISION,
            DV_PROFILE_DVAV1_10,
            CodecProfileLevel.DolbyVisionLevelHd24,
        )

    /** Get supported AV1 profiles as strings for DeviceProfile */
    fun getSupportedAv1Profiles(): List<String> = buildList {
        if (supportsAv1()) {
            add("Main")
            if (supportsAv1Main10()) {
                add("Main 10")
            }
        }
    }

    // ==================== VP8/VP9 ====================

    /** Check if device has VP8 decoder */
    fun supportsVp8(): Boolean = hasCodecForMime(MIME_VIDEO_VP8)

    /** Check if device has VP9 decoder */
    fun supportsVp9(): Boolean = hasCodecForMime(MIME_VIDEO_VP9)

    /** Check if device supports VP9 Profile 2 (10-bit) */
    fun supportsVp9Profile2(): Boolean = hasDecoder(
        MIME_VIDEO_VP9,
        CodecProfileLevel.VP9Profile2,
        CodecProfileLevel.VP9Level4,
    )

    // ==================== Other Codecs ====================

    /** Check if device has VC-1 decoder */
    fun supportsVc1(): Boolean = hasCodecForMime(MIME_VIDEO_VC1)

    /** Check if device has Dolby Vision decoder */
    fun supportsDolbyVision(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        hasCodecForMime(MIME_VIDEO_DOLBY_VISION)

    // ==================== Resolution Detection ====================

    /** Get maximum supported resolution for a codec */
    @Suppress("LoopWithTooManyJumpStatements")
    fun getMaxResolution(mime: String): Size {
        var maxWidth = 0
        var maxHeight = 0

        for (info in mediaCodecList.codecInfos) {
            if (info.isEncoder) continue

            try {
                val capabilities = info.getCapabilitiesForType(mime)
                val videoCapabilities = capabilities.videoCapabilities ?: continue
                val supportedWidth = videoCapabilities.supportedWidths?.upper ?: continue
                val supportedHeight = videoCapabilities.supportedHeights?.upper ?: continue

                maxWidth = maxOf(maxWidth, supportedWidth)
                maxHeight = maxOf(maxHeight, supportedHeight)
            } catch (_: IllegalArgumentException) {
                // Decoder not supported for this mime type
            }
        }

        Log.d(TAG, "Max resolution for $mime: ${maxWidth}x$maxHeight")
        return Size(maxWidth, maxHeight)
    }

    /** Get maximum resolution for AVC */
    fun getMaxAvcResolution(): Size = getMaxResolution(MIME_VIDEO_AVC)

    /** Get maximum resolution for HEVC */
    fun getMaxHevcResolution(): Size = getMaxResolution(MIME_VIDEO_HEVC)

    /** Get maximum resolution for AV1 */
    fun getMaxAv1Resolution(): Size = getMaxResolution(MIME_VIDEO_AV1)

    // ==================== Utility Methods ====================

    /** Check if device has a decoder for the given MIME type */
    private fun hasCodecForMime(mime: String, requireHardware: Boolean = false): Boolean {
        for (info in mediaCodecList.codecInfos) {
            if (info.isEncoder) continue
            if (requireHardware && !isHardwareDecoder(info)) continue

            if (info.supportedTypes.any { it.equals(mime, ignoreCase = true) }) {
                Log.d(TAG, "Found decoder ${info.name} for $mime")
                return true
            }
        }
        return false
    }

    /** Check if device has a decoder with specific profile and level */
    private fun hasDecoder(
        mime: String,
        profile: Int,
        minLevel: Int,
        requireHardware: Boolean = false,
    ): Boolean {
        for (info in mediaCodecList.codecInfos) {
            if (info.isEncoder) continue
            if (requireHardware && !isHardwareDecoder(info)) continue

            try {
                val capabilities = info.getCapabilitiesForType(mime)
                for (profileLevel in capabilities.profileLevels) {
                    if (profileLevel.profile == profile && profileLevel.level >= minLevel) {
                        Log.d(TAG, "Found decoder ${info.name} with profile=$profile level=${profileLevel.level}")
                        return true
                    }
                }
            } catch (_: IllegalArgumentException) {
                // Decoder not supported for this mime type
            }
        }
        return false
    }

    /** Get maximum supported level for a profile */
    private fun getDecoderLevel(mime: String, profile: Int, requireHardware: Boolean = false,): Int {
        var maxLevel = 0

        for (info in mediaCodecList.codecInfos) {
            if (info.isEncoder) continue
            if (requireHardware && !isHardwareDecoder(info)) continue

            try {
                val capabilities = info.getCapabilitiesForType(mime)
                for (profileLevel in capabilities.profileLevels) {
                    if (profileLevel.profile == profile) {
                        maxLevel = maxOf(maxLevel, profileLevel.level)
                    }
                }
            } catch (_: IllegalArgumentException) {
                // Decoder not supported for this mime type
            }
        }

        return maxLevel
    }

    private fun isHardwareDecoder(info: android.media.MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.isHardwareAccelerated && !info.isSoftwareOnly
        } else {
            val name = info.name.lowercase()
            !name.startsWith("omx.google.") && !name.startsWith("c2.android.") &&
                !name.contains("sw") && !name.contains("software")
        }
    }

    // ==================== Summary Methods ====================

    /**
     * Get a summary of device codec capabilities for logging/debugging.
     */
    fun getSummary(): String = buildString {
        appendLine("=== Device Codec Capabilities ===")
        appendLine("AVC (H.264):")
        appendLine("  Supported: ${supportsAvc()}")
        appendLine("  High 10: ${supportsAvcHigh10()}")
        appendLine("  Main Level: ${getAvcMainLevel()}")
        appendLine("  Max Resolution: ${getMaxAvcResolution()}")
        appendLine("HEVC (H.265):")
        appendLine("  Supported: ${supportsHevc()}")
        appendLine("  Main 10: ${supportsHevcMain10()}")
        appendLine("  HDR10: ${supportsHevcHdr10()}")
        appendLine("  Main Level: ${getHevcMainLevel()}")
        appendLine("  Main 10 Level: ${getHevcMain10Level()}")
        appendLine("  Max Resolution: ${getMaxHevcResolution()}")
        appendLine("AV1:")
        appendLine("  Supported: ${supportsAv1()}")
        appendLine("  Main 10: ${supportsAv1Main10()}")
        appendLine("  HDR10: ${supportsAv1Hdr10()}")
        appendLine("  Max Resolution: ${getMaxAv1Resolution()}")
        appendLine("VP8: ${supportsVp8()}")
        appendLine("VP9: ${supportsVp9()}")
        appendLine("VP9 Profile 2 (10-bit): ${supportsVp9Profile2()}")
        appendLine("VC-1: ${supportsVc1()}")
        appendLine("Dolby Vision: ${supportsDolbyVision()}")
    }

    /**
     * Log device codec capabilities at debug level.
     */
    fun logCapabilities() {
        getSummary().lines().forEach { line ->
            if (line.isNotBlank()) {
                Log.d(TAG, line)
            }
        }
    }
}
