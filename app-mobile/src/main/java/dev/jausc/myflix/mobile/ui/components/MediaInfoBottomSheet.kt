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

/**
 * Bottom sheet displaying technical media information for a video item.
 * Shows video codec, resolution, audio tracks, subtitles, and container format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoBottomSheet(
    item: JellyfinItem,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mediaSource = item.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams ?: emptyList()
    val container = mediaSource?.container

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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            HorizontalDivider()

            // Video stream info
            mediaStreams.filter { it.type == "Video" }.firstOrNull()?.let { video ->
                MediaInfoSection(
                    label = "VIDEO",
                    content = buildString {
                        append(video.codec?.uppercase() ?: "Unknown")
                        video.width?.let { w -> video.height?.let { h -> append(" \u2022 ${w}x$h") } }
                        video.videoRangeType?.let { append(" \u2022 $it") }
                    },
                )
            }

            // Audio streams
            val audioStreams = mediaStreams.filter { it.type == "Audio" }
            if (audioStreams.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "AUDIO (${audioStreams.size} tracks)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    audioStreams.take(4).forEach { audio ->
                        Text(
                            text = buildString {
                                append(audio.codec?.uppercase() ?: "Unknown")
                                audio.channels?.let { append(" \u2022 ${it}ch") }
                                audio.language?.let { append(" \u2022 $it") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (audioStreams.size > 4) {
                        Text(
                            text = "... and ${audioStreams.size - 4} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Subtitle streams
            val subtitleStreams = mediaStreams.filter { it.type == "Subtitle" }
            if (subtitleStreams.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SUBTITLES (${subtitleStreams.size} tracks)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = subtitleStreams.take(6).mapNotNull { it.language }.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (subtitleStreams.size > 6) {
                        Text(
                            text = "... and ${subtitleStreams.size - 6} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // File info
            container?.let { cont ->
                MediaInfoSection(
                    label = "CONTAINER",
                    content = cont.uppercase(),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MediaInfoSection(
    label: String,
    content: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
