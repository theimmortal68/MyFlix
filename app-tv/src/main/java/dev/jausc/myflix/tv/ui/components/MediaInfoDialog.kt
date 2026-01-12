@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.jausc.myflix.tv.ui.theme.TvColors

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
    val mediaStreams = mediaSource?.mediaStreams ?: emptyList()
    val container = mediaSource?.container

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
                    .widthIn(min = 400.dp, max = 600.dp)
                    .padding(32.dp),
                shape = MaterialTheme.shapes.large,
                colors = SurfaceDefaults.colors(
                    containerColor = TvColors.Surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Media Information",
                        style = MaterialTheme.typography.titleLarge,
                        color = TvColors.TextPrimary,
                    )

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TvColors.BluePrimary,
                    )

                    // Video stream info
                    mediaStreams.filter { it.type == "Video" }.firstOrNull()?.let { video ->
                        MediaInfoSection(
                            label = "VIDEO",
                            content = buildString {
                                append(video.codec?.uppercase() ?: "Unknown")
                                video.width?.let { w -> video.height?.let { h -> append(" • ${w}x$h") } }
                                video.videoRangeType?.let { append(" • $it") }
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
                                color = TvColors.TextSecondary,
                            )
                            audioStreams.take(3).forEach { audio ->
                                Text(
                                    text = buildString {
                                        append(audio.codec?.uppercase() ?: "Unknown")
                                        audio.channels?.let { append(" • ${it}ch") }
                                        audio.language?.let { append(" • $it") }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TvColors.TextPrimary,
                                )
                            }
                            if (audioStreams.size > 3) {
                                Text(
                                    text = "... and ${audioStreams.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TvColors.TextSecondary,
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
                                color = TvColors.TextSecondary,
                            )
                            Text(
                                text = subtitleStreams.take(5).mapNotNull { it.language }.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextPrimary,
                            )
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

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Close")
                    }
                }
            }
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
            color = TvColors.TextSecondary,
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextPrimary,
        )
    }
}
