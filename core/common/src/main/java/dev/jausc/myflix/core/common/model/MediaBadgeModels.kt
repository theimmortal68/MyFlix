package dev.jausc.myflix.core.common.model

/**
 * Badge type categories for colorful media badges.
 * Each type has associated display colors defined in the UI layer.
 */
enum class BadgeType {
    RESOLUTION,
    VIDEO_CODEC,
    HDR,
    AUDIO_CODEC,
    AUDIO_CHANNELS,
    EDITION,
}

/**
 * Represents a media badge with display text and type.
 */
data class MediaBadge(
    val text: String,
    val type: BadgeType,
)
