@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dot-separated row of metadata text with optional community rating.
 * Wholphin-style metadata display for mobile.
 */
@Composable
fun DotSeparatedRow(
    texts: List<String>,
    modifier: Modifier = Modifier,
    communityRating: Float? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        texts.forEachIndexed { index, text ->
            Text(
                text = text,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (communityRating != null || index != texts.lastIndex) {
                Dot()
            }
        }
        val height = with(LocalDensity.current) { textStyle.fontSize.toDp() }
        communityRating?.let {
            SimpleStarRating(
                communityRating = it,
                modifier = Modifier.height(height),
            )
        }
    }
}

/**
 * Small dot separator for metadata rows.
 */
@Composable
fun Dot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            .size(4.dp),
    )
}

/**
 * Simple star rating display with icon and value.
 */
@Composable
fun SimpleStarRating(
    communityRating: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700), // Gold color
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = String.format(Locale.US, "%.1f", communityRating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * Quick details row for movies: year, runtime, "ends at", rating.
 */
@Composable
fun MovieQuickDetails(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    val now = LocalDateTime.now()
    val details = remember(item, now) {
        buildList {
            item.productionYear?.let { add(it.toString()) }
            addRuntimeDetails(now, item)
            item.officialRating?.let(::add)
        }
    }

    DotSeparatedRow(
        texts = details,
        communityRating = item.communityRating,
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

/**
 * Quick details row for series: premiere year, official rating.
 */
@Composable
fun SeriesQuickDetails(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
    status: String? = null,
    studios: List<String> = emptyList(),
) {
    val details = remember(item) {
        buildList {
            item.productionYear?.let { add(it.toString()) }
            item.officialRating?.let(::add)
        }
    }

    val badges = remember(status, studios) {
        buildList {
            status?.let { add(formatSeriesStatusBadge(it)) }
            studios.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    SeriesQuickDetailsRow(
        details = details,
        communityRating = item.communityRating,
        badges = badges,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesQuickDetailsRow(
    details: List<String>,
    communityRating: Float?,
    badges: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        DotSeparatedRow(
            texts = details,
            communityRating = communityRating,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        badges.forEach { badge ->
            MetadataBadge(text = badge)
        }
    }
}

@Composable
private fun MetadataBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatSeriesStatusBadge(status: String): String {
    val normalized = status.trim().lowercase()
    return when {
        normalized.contains("continuing") -> "Airing"
        normalized.contains("returning") -> "Returning"
        normalized.contains("ended") -> "Ended"
        normalized.contains("canceled") || normalized.contains("cancelled") -> "Canceled"
        else -> status.trim()
    }
}

/**
 * Badge type for colorful media badges.
 */
private enum class BadgeType {
    RESOLUTION,
    VIDEO_CODEC,
    HDR,
    AUDIO_CODEC,
    AUDIO_CHANNELS,
}

/**
 * Colorful badge for media information.
 */
@Composable
private fun ColoredMediaBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = when (type) {
        BadgeType.RESOLUTION -> Color(0xFF2563EB) to Color.White      // Blue
        BadgeType.VIDEO_CODEC -> Color(0xFFEA580C) to Color.White     // Orange
        BadgeType.HDR -> Color(0xFF9333EA) to Color.White             // Purple
        BadgeType.AUDIO_CODEC -> Color(0xFF0891B2) to Color.White     // Cyan
        BadgeType.AUDIO_CHANNELS -> Color(0xFF059669) to Color.White  // Green
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

/**
 * Row of colorful media badges showing resolution, HDR/DV, video codec, and audio info.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaBadgesRow(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    val mediaSource = item.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams.orEmpty()
    val videoStream = mediaStreams.firstOrNull { it.type == "Video" }
    val audioStream = mediaStreams.firstOrNull { it.type == "Audio" && it.isDefault }
        ?: mediaStreams.firstOrNull { it.type == "Audio" }

    val badges = remember(mediaSource) {
        buildMediaBadges(videoStream, audioStream)
    }

    if (badges.isEmpty()) return

    FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        badges.forEach { (text, type) ->
            ColoredMediaBadge(text = text, type = type)
        }
    }
}

private data class MediaBadge(val text: String, val type: BadgeType)

private fun buildMediaBadges(
    videoStream: MediaStream?,
    audioStream: MediaStream?,
): List<MediaBadge> = buildList {
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

    // Audio codec badge (Cyan)
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
            codec.contains("DTS") && (title.contains("hd ma") || displayTitle.contains("hd ma") ||
                title.contains("hd-ma") || displayTitle.contains("hd-ma")) ->
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

    // Audio channels badge (Green)
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
 * Helper function to add runtime details to the list.
 */
private fun MutableList<String>.addRuntimeDetails(
    now: LocalDateTime,
    item: JellyfinItem,
) {
    val runtimeTicks = item.runTimeTicks ?: return
    val runtimeMinutes = (runtimeTicks / 600_000_000).toInt()
    if (runtimeMinutes <= 0) return

    // Format runtime as "1h 48m" or "48m"
    val hours = runtimeMinutes / 60
    val mins = runtimeMinutes % 60
    val runtimeText = when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
    add(runtimeText)

    // Calculate remaining time if there's playback progress
    val positionTicks = item.userData?.playbackPositionTicks ?: 0L
    if (positionTicks > 0L) {
        val remainingTicks = runtimeTicks - positionTicks
        val remainingMinutes = (remainingTicks / 600_000_000).toInt()
        if (remainingMinutes > 0) {
            val remainingHours = remainingMinutes / 60
            val remainingMins = remainingMinutes % 60
            val remainingText = when {
                remainingHours > 0 && remainingMins > 0 -> "${remainingHours}h ${remainingMins}m left"
                remainingHours > 0 -> "${remainingHours}h left"
                else -> "${remainingMins}m left"
            }
            add(remainingText)
        }
    }

    // Calculate "ends at" time
    val ticksToPlay = if (positionTicks > 0L) runtimeTicks - positionTicks else runtimeTicks
    val secondsToPlay = ticksToPlay / 10_000_000
    val endTime = now.plusSeconds(secondsToPlay)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    add("ends at ${endTime.format(timeFormatter)}")
}
