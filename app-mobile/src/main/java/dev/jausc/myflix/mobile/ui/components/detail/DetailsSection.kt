@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * Technical details section showing metadata about the media item.
 */
@Composable
fun MobileDetailsSection(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    val directors = item.people?.filter { it.type == "Director" } ?: emptyList()
    val writers = item.people?.filter { it.type == "Writer" } ?: emptyList()

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section header
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Director
            if (directors.isNotEmpty()) {
                DetailRow(
                    label = if (directors.size > 1) "Directors" else "Director",
                    value = directors.joinToString(", ") { it.name ?: "Unknown" },
                )
            }

            // Writers
            if (writers.isNotEmpty()) {
                DetailRow(
                    label = if (writers.size > 1) "Writers" else "Writer",
                    value = writers.joinToString(", ") { it.name ?: "Unknown" },
                )
            }

            // Studios
            item.studios?.takeIf { it.isNotEmpty() }?.let { studios ->
                DetailRow(
                    label = if (studios.size > 1) "Studios" else "Studio",
                    value = studios.mapNotNull { it.name }.joinToString(", "),
                )
            }

            // Production year
            item.productionYear?.let { year ->
                DetailRow(label = "Year", value = year.toString())
            }

            // Community rating
            item.communityRating?.let { rating ->
                DetailRow(
                    label = "Rating",
                    value = "%.1f / 10".format(rating),
                )
            }

            // Video info from media sources
            item.mediaSources?.firstOrNull()?.let { source ->
                source.mediaStreams?.filter { it.type == "Video" }?.firstOrNull()?.let { video ->
                    val videoInfo = buildString {
                        video.codec?.let { append(it.uppercase()) }
                        video.width?.let { width ->
                            video.height?.let { height ->
                                if (isNotEmpty()) append(" ")
                                append("${width}x${height}")
                            }
                        }
                        video.videoRangeType?.let { hdr ->
                            if (hdr != "SDR") {
                                if (isNotEmpty()) append(" ")
                                append(hdr)
                            }
                        }
                    }
                    if (videoInfo.isNotEmpty()) {
                        DetailRow(label = "Video", value = videoInfo)
                    }
                }

                // Audio info
                source.mediaStreams?.filter { it.type == "Audio" }?.firstOrNull()?.let { audio ->
                    val audioInfo = buildString {
                        audio.language?.let { append(it.uppercase()) }
                        audio.codec?.let { codec ->
                            if (isNotEmpty()) append(" ")
                            append(codec.uppercase())
                        }
                        audio.channels?.let { channels ->
                            if (isNotEmpty()) append(" ")
                            append("${channels}ch")
                        }
                    }
                    if (audioInfo.isNotEmpty()) {
                        DetailRow(label = "Audio", value = audioInfo)
                    }
                }
            }

            // External URLs (IMDb, TMDb, etc.)
            item.externalUrls?.forEach { url ->
                val name = url.name ?: return@forEach
                val value = url.url ?: return@forEach
                if (name in listOf("IMDb", "TheMovieDb", "Trakt")) {
                    DetailRow(label = name, value = value)
                }
            }
        }
    }
}

/**
 * Single detail row with label and value.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.7f),
        )
    }
}
