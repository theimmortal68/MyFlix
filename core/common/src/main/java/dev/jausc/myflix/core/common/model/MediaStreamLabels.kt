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
        if (isExternalSubtitle) "[EXT]" else null,
    )
    return parts.joinToString(" · ").ifBlank { "Subtitle $index" }
}

/**
 * Returns true if this subtitle stream is delivered externally (not embedded in container).
 */
val MediaStream.isExternalSubtitle: Boolean
    get() = isExternal || deliveryMethod.equals("External", ignoreCase = true)

/**
 * Returns the MIME type for this subtitle stream based on codec.
 */
val MediaStream.subtitleMimeType: String
    get() = when (codec?.lowercase()) {
        "srt", "subrip" -> "application/x-subrip"
        "ass", "ssa" -> "text/x-ssa"
        "vtt", "webvtt" -> "text/vtt"
        "pgs", "pgssub" -> "application/pgs"
        "dvdsub", "dvd_subtitle" -> "application/dvdsub"
        else -> "application/x-subrip" // Default fallback
    }

/**
 * Returns a track ID for this stream, prefixed with "e:" for external subtitles.
 */
fun MediaStream.trackId(): String = if (isExternalSubtitle) "e:$index" else index.toString()
