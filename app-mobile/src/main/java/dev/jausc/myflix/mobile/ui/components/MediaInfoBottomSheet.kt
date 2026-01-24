@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaSource
import dev.jausc.myflix.core.common.model.MediaStream
import kotlin.math.roundToInt

/**
 * Bottom sheet displaying technical media information for a video item.
 * Shows video codec, resolution, audio tracks, subtitles, and container format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoBottomSheet(item: JellyfinItem, onDismiss: () -> Unit,) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mediaSource = item.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams.orEmpty()
    val videoStream = mediaStreams.firstOrNull { it.type == "Video" }
    val audioStream = mediaStreams.firstOrNull { it.type == "Audio" && it.isDefault }
        ?: mediaStreams.firstOrNull { it.type == "Audio" }
    val subtitleStream = mediaStreams.firstOrNull { it.type == "Subtitle" && it.isDefault }
        ?: mediaStreams.firstOrNull { it.type == "Subtitle" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Media Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            HorizontalDivider()

            MediaInfoSection(
                label = "General",
                rows = buildGeneralRows(item, mediaSource),
            )

            val descriptionRows = buildDescriptionRows(item)
            if (descriptionRows.isNotEmpty()) {
                MediaInfoSection(
                    label = "Description",
                    rows = descriptionRows,
                )
            }

            videoStream?.let { stream ->
                MediaInfoSection(
                    label = "Video",
                    rows = buildVideoRows(stream),
                )
            }

            audioStream?.let { stream ->
                MediaInfoSection(
                    label = "Audio",
                    rows = buildAudioRows(stream),
                )
            }

            subtitleStream?.let { stream ->
                MediaInfoSection(
                    label = "Subtitle",
                    rows = buildSubtitleRows(stream),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MediaInfoSection(label: String, rows: List<Pair<String, String>>,) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        rows.forEach { (title, value) ->
            Text(
                text = "$title: $value",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun buildGeneralRows(item: JellyfinItem, mediaSource: MediaSource?): List<Pair<String, String>> {
    val runtimeTicks = mediaSource?.runTimeTicks ?: item.runTimeTicks
    return buildList {
        mediaSource?.container?.let { add("Container" to it.uppercase()) }
        mediaSource?.path?.let { add("Path" to it) }
        add("ID" to item.id)
        formatBytes(mediaSource?.size)?.let { add("Size" to it) }
        formatBitrate(mediaSource?.bitrate)?.let { add("Bitrate" to it) }
        formatRuntime(runtimeTicks)?.let { add("Runtime" to it) }
    }
}

private fun buildDescriptionRows(item: JellyfinItem): List<Pair<String, String>> {
    val description = item.overview?.trim().orEmpty()
    return if (description.isNotBlank()) listOf("Overview" to description) else emptyList()
}

private fun buildVideoRows(stream: MediaStream): List<Pair<String, String>> = buildList {
    stream.codec?.uppercase()?.let { add("Codec" to it) }
    if (stream.width != null && stream.height != null) {
        add("Resolution" to "${stream.width}x${stream.height}")
    }
    stream.aspectRatio?.let { add("Aspect Ratio" to it) }
    formatBitrate(stream.bitRate)?.let { add("Bitrate" to it) }
    stream.frameRate?.let { add("Framerate" to String.format("%.3f", it)) }
    stream.videoRangeType?.let { add("Video range type" to it) }
    stream.profile?.let { add("Profile" to it) }
    stream.level?.let { add("Level" to String.format("%.1f", it)) }
    stream.pixelFormat?.let { add("Pixel format" to it) }
    stream.refFrames?.let { add("Ref frames" to it.toString()) }
    stream.nalLengthSize?.let { add("NAL" to it.toString()) }
    stream.isInterlaced?.let { add("Interlaced" to yesNo(it)) }
}

private fun buildAudioRows(stream: MediaStream): List<Pair<String, String>> = buildList {
    stream.language?.let { add("Language" to it) }
    stream.codec?.uppercase()?.let { add("Codec" to it) }
    stream.channelLayout?.let { add("Layout" to it) }
        ?: formatChannels(stream.channels)?.let { add("Layout" to it) }
    stream.channels?.let { add("Channels" to it.toString()) }
    formatBitrate(stream.bitRate)?.let { add("Bitrate" to it) }
    stream.sampleRate?.let { add("Sample rate" to "$it Hz") }
    add("Default" to yesNo(stream.isDefault))
}

private fun buildSubtitleRows(stream: MediaStream): List<Pair<String, String>> = buildList {
    val title = stream.title ?: stream.displayTitle
    title?.let { add("Title" to it) }
    stream.language?.let { add("Language" to it) }
    stream.codec?.uppercase()?.let { add("Codec" to it) }
    add("Default" to yesNo(stream.isDefault))
    add("Forced" to yesNo(stream.isForced))
    add("External" to yesNo(stream.isExternal))
    add("Hearing Impaired" to yesNo(stream.isHearingImpaired))
}

private fun formatBytes(size: Long?): String? {
    if (size == null) return null
    val mb = size / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        val gb = mb / 1024.0
        String.format("%.2f GB", gb)
    } else {
        String.format("%.2f MB", mb)
    }
}

private fun formatBitrate(bitrate: Long?): String? {
    if (bitrate == null || bitrate <= 0) return null
    val mbps = bitrate / 1_000_000.0
    return String.format("%.2f Mbps", mbps)
}

private fun formatRuntime(ticks: Long?): String? {
    if (ticks == null || ticks <= 0) return null
    val totalSeconds = (ticks / 10_000_000.0).roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        String.format("%dh %02dm", hours, remainingMinutes)
    } else {
        String.format("%dm %02ds", minutes, seconds)
    }
}

private fun formatChannels(channels: Int?): String? {
    return when (channels) {
        null -> null
        1 -> "mono"
        2 -> "stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$channels.0"
    }
}

private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"
