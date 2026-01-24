@file:Suppress("MagicNumber")

package dev.jausc.myflix.core.common.util

import dev.jausc.myflix.core.common.model.BadgeType
import dev.jausc.myflix.core.common.model.MediaBadge
import dev.jausc.myflix.core.common.model.MediaStream

/**
 * Utility object for building media badges from stream information.
 */
object MediaBadgeUtil {
    /**
     * Build a list of media badges from video and audio stream information.
     *
     * @param videoStream The video stream to analyze
     * @param audioStream The audio stream to analyze
     * @param itemName The item name to check for edition info
     * @param tags Optional tags to check for edition info
     * @return List of media badges to display
     */
    fun buildMediaBadges(
        videoStream: MediaStream?,
        audioStream: MediaStream?,
        itemName: String = "",
        tags: List<String>? = null,
    ): List<MediaBadge> = buildList {
        // Edition badge (Gold) - check name and tags for edition info
        detectEdition(itemName, tags)?.let { edition ->
            add(MediaBadge(edition, BadgeType.EDITION))
        }

        // Resolution badge (Blue)
        videoStream?.let { video ->
            val width = video.width ?: 0
            val height = video.height ?: 0
            when {
                width >= 3840 || height >= 2160 -> add(MediaBadge("4K", BadgeType.RESOLUTION))
                height >= 1080 -> add(MediaBadge("1080p", BadgeType.RESOLUTION))
                height >= 720 -> add(MediaBadge("720p", BadgeType.RESOLUTION))
                height >= 480 -> add(MediaBadge("480p", BadgeType.RESOLUTION))
            }
        }

        // Video codec badge (Orange)
        videoStream?.codec?.let { codec ->
            val codecUpper = codec.uppercase()
            when {
                codecUpper.contains("HEVC") || codecUpper.contains("H265") ->
                    add(MediaBadge("HEVC", BadgeType.VIDEO_CODEC))
                codecUpper.contains("AV1") -> add(MediaBadge("AV1", BadgeType.VIDEO_CODEC))
                codecUpper.contains("VP9") -> add(MediaBadge("VP9", BadgeType.VIDEO_CODEC))
                codecUpper.contains("AVC") || codecUpper.contains("H264") ->
                    add(MediaBadge("H.264", BadgeType.VIDEO_CODEC))
            }
        }

        // HDR/Dolby Vision badge (Purple)
        addHdrBadge(videoStream)

        // Audio codec badge (Cyan)
        addAudioCodecBadge(audioStream)

        // Audio channels badge (Green)
        addAudioChannelsBadge(audioStream)
    }

    private fun MutableList<MediaBadge>.addHdrBadge(videoStream: MediaStream?) {
        videoStream?.let { video ->
            val rangeType = video.videoRangeType?.lowercase() ?: ""
            val range = video.videoRange?.lowercase() ?: ""
            val profile = video.profile?.lowercase() ?: ""

            when {
                rangeType.contains("dolby") || rangeType.contains("dovi") ||
                    profile.contains("dvhe") || profile.contains("dvh1") ||
                    !video.videoDoViTitle.isNullOrBlank() ->
                        add(MediaBadge("Dolby Vision", BadgeType.HDR))
                rangeType.contains("hdr10+") || rangeType.contains("hdr10plus") ->
                    add(MediaBadge("HDR10+", BadgeType.HDR))
                rangeType.contains("hdr10") -> add(MediaBadge("HDR10", BadgeType.HDR))
                rangeType.contains("hlg") || range.contains("hlg") ->
                    add(MediaBadge("HLG", BadgeType.HDR))
                rangeType.contains("hdr") || range.contains("hdr") ->
                    add(MediaBadge("HDR", BadgeType.HDR))
            }
        }
    }

    private fun MutableList<MediaBadge>.addAudioCodecBadge(audioStream: MediaStream?) {
        audioStream?.let { audio ->
            val codec = audio.codec?.uppercase() ?: ""
            val title = audio.title?.lowercase() ?: ""
            val displayTitle = audio.displayTitle?.lowercase() ?: ""

            when {
                title.contains("atmos") || displayTitle.contains("atmos") ->
                    add(MediaBadge("Atmos", BadgeType.AUDIO_CODEC))
                codec.contains("TRUEHD") -> add(MediaBadge("TrueHD", BadgeType.AUDIO_CODEC))
                title.contains("dts:x") || displayTitle.contains("dts:x") ||
                    title.contains("dts-x") || displayTitle.contains("dts-x") ->
                        add(MediaBadge("DTS:X", BadgeType.AUDIO_CODEC))
                codec.contains("DTS") && (
                    title.contains("hd ma") || displayTitle.contains("hd ma") ||
                    title.contains("hd-ma") || displayTitle.contains("hd-ma")
                ) ->
                        add(MediaBadge("DTS-HD MA", BadgeType.AUDIO_CODEC))
                codec.contains("DTS") -> add(MediaBadge("DTS", BadgeType.AUDIO_CODEC))
                codec.contains("EAC3") || codec.contains("E-AC-3") ->
                    add(MediaBadge("EAC3", BadgeType.AUDIO_CODEC))
                codec.contains("AC3") || codec.contains("AC-3") ->
                    add(MediaBadge("AC3", BadgeType.AUDIO_CODEC))
                codec.contains("AAC") -> add(MediaBadge("AAC", BadgeType.AUDIO_CODEC))
                codec.contains("FLAC") -> add(MediaBadge("FLAC", BadgeType.AUDIO_CODEC))
            }
        }
    }

    private fun MutableList<MediaBadge>.addAudioChannelsBadge(audioStream: MediaStream?) {
        audioStream?.let { audio ->
            val channels = audio.channels
            val layout = audio.channelLayout?.lowercase() ?: ""
            when {
                channels == 8 || layout.contains("7.1") ->
                    add(MediaBadge("7.1", BadgeType.AUDIO_CHANNELS))
                channels == 6 || layout.contains("5.1") ->
                    add(MediaBadge("5.1", BadgeType.AUDIO_CHANNELS))
                channels == 2 || layout.contains("stereo") ->
                    add(MediaBadge("Stereo", BadgeType.AUDIO_CHANNELS))
            }
        }
    }

    /**
     * Detect edition from item name or tags.
     * Returns the display label for the edition, or null if no edition detected.
     *
     * @param itemName The item name to check
     * @param tags Optional tags to check
     * @return The edition label or null
     */
    fun detectEdition(itemName: String, tags: List<String>?): String? {
        val nameLower = itemName.lowercase()
        val tagsLower = tags?.map { it.lowercase() }.orEmpty()

        // Check both name and tags for edition patterns
        // Order matters - more specific patterns first
        val editionPatterns = listOf(
            // Director's cuts and special cuts
            listOf("director's cut", "directors cut", "director cut") to "Director's Cut",
            listOf("final cut") to "Final Cut",
            listOf("ultimate cut") to "Ultimate Cut",
            listOf("theatrical cut", "theatrical") to "Theatrical",
            listOf("producer's cut", "producers cut") to "Producer's Cut",
            listOf("assembly cut") to "Assembly Cut",
            // Extended versions
            listOf("extended edition", "extended cut", "extended version", "extended") to "Extended",
            listOf("unrated", "unrated edition", "unrated cut") to "Unrated",
            listOf("uncut", "uncut edition") to "Uncut",
            // Special editions
            listOf("special edition") to "Special Edition",
            listOf("collector's edition", "collectors edition") to "Collector's Edition",
            listOf("anniversary edition", "anniversary") to "Anniversary",
            listOf("criterion collection", "criterion") to "Criterion",
            listOf("remastered", "remaster") to "Remastered",
            listOf("restored", "4k restoration", "restoration") to "Restored",
            // IMAX
            listOf("imax enhanced") to "IMAX Enhanced",
            listOf("imax edition", "imax") to "IMAX",
            // 3D
            listOf("3d") to "3D",
            // Other
            listOf("black and chrome", "black & chrome") to "Black & Chrome",
            listOf("open matte") to "Open Matte",
            listOf("superbit") to "Superbit",
        )

        for ((patterns, label) in editionPatterns) {
            for (pattern in patterns) {
                if (nameLower.contains(pattern) || tagsLower.any { it.contains(pattern) }) {
                    return label
                }
            }
        }

        return null
    }
}
