@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaSource
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlin.math.roundToInt

/**
 * Dialog displaying technical media information for a video item.
 * Shows video codec, resolution, audio tracks, subtitles, and container format.
 */
@Composable
fun MediaInfoDialog(
    item: JellyfinItem,
    onDismiss: () -> Unit,
) {
    val mediaSource = item.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams.orEmpty()
    val videoStream = mediaStreams.firstOrNull { it.type == "Video" }
    val audioStream = mediaStreams.firstOrNull { it.type == "Audio" && it.isDefault }
        ?: mediaStreams.firstOrNull { it.type == "Audio" }
    val subtitleStream = mediaStreams.firstOrNull { it.type == "Subtitle" && it.isDefault }
        ?: mediaStreams.firstOrNull { it.type == "Subtitle" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 640.dp, max = 960.dp)
                    .fillMaxHeight(0.85f)
                    .padding(32.dp),
                shape = MaterialTheme.shapes.large,
                colors = SurfaceDefaults.colors(
                    containerColor = TvColors.Surface,
                ),
            ) {
                val listState = rememberLazyListState()
                val closeFocusRequester = FocusRequester()

                val generalSection = buildGeneralRows(item, mediaSource)
                val videoSection = videoStream?.let { buildVideoRows(it) }
                val audioSection = audioStream?.let { buildAudioRows(it) }
                val subtitleSection = subtitleStream?.let { buildSubtitleRows(it) }

                // Build list of sections for LazyColumn
                val allSections = buildList {
                    add("General" to generalSection)
                    videoSection?.let { add("Video" to it) }
                    audioSection?.let { add("Audio" to it) }
                    subtitleSection?.let { add("Subtitle" to it) }
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                ) {
                    // Fixed header
                    Text(
                        text = "Media Information",
                        style = MaterialTheme.typography.titleLarge,
                        color = TvColors.TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TvColors.BluePrimary,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable content with focusable sections for D-pad navigation
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        items(
                            items = allSections,
                            key = { (label, _) -> label },
                        ) { (label, rows) ->
                            MediaInfoSection(
                                label = label,
                                rows = rows,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fixed footer with close button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                            .focusRequester(closeFocusRequester),
                    ) {
                        Text("Close")
                    }
                }

                // Focus the close button after a delay
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    try {
                        closeFocusRequester.requestFocus()
                    } catch (_: Exception) {
                        // Focus request failed
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaInfoSection(
    label: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TvColors.TextSecondary,
        )
        rows.forEach { (title, value) ->
            Text(
                text = "$title: $value",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextPrimary,
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
        else -> "${channels}.0"
    }
}

private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"
