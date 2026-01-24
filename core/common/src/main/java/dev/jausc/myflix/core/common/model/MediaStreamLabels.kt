package dev.jausc.myflix.core.common.model

/**
 * Human-friendly labels for media streams.
 */
fun MediaStream.audioLabel(): String {
    if (!displayTitle.isNullOrBlank()) return displayTitle
    val parts = listOfNotNull(
        language?.uppercase(),
        codec?.uppercase(),
        channels?.let { "${it}ch" },
    )
    return parts.joinToString(" · ").ifBlank { "Audio $index" }
}

fun MediaStream.subtitleLabel(): String {
    if (!displayTitle.isNullOrBlank()) return displayTitle
    val parts = listOfNotNull(
        language?.uppercase(),
        codec?.uppercase(),
    )
    return parts.joinToString(" · ").ifBlank { "Subtitle $index" }
}
