@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.is4K
import dev.jausc.myflix.core.common.model.isDolbyVision
import dev.jausc.myflix.core.common.model.isHdr
import dev.jausc.myflix.core.common.model.videoStream
import dev.jausc.myflix.core.common.model.writers

/**
 * Details tab content showing full credits and technical information.
 */
@Composable
fun MobileDetailsTabContent(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val actors = item.actors
    val directors = item.directors
    val writers = item.writers
    val studios = item.studios

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Cast
        if (actors.isNotEmpty()) {
            Column {
                SectionTitle("Cast")
                Spacer(modifier = Modifier.height(8.dp))
                actors.forEach { actor ->
                    MobilePersonRow(person = actor)
                }
            }
        }

        // Directors
        if (directors.isNotEmpty()) {
            Column {
                SectionTitle("Director${if (directors.size > 1) "s" else ""}")
                Spacer(modifier = Modifier.height(8.dp))
                directors.forEach { director ->
                    MobilePersonRow(person = director)
                }
            }
        }

        // Writers
        if (writers.isNotEmpty()) {
            Column {
                SectionTitle("Writer${if (writers.size > 1) "s" else ""}")
                Spacer(modifier = Modifier.height(8.dp))
                writers.forEach { writer ->
                    MobilePersonRow(person = writer)
                }
            }
        }

        // Studios
        if (!studios.isNullOrEmpty()) {
            Column {
                SectionTitle("Studio${if (studios.size > 1) "s" else ""}")
                Spacer(modifier = Modifier.height(8.dp))
                studios.forEach { studio ->
                    Text(
                        text = studio.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Technical specifications
        TechnicalSpecsSection(item)

        // External links
        if (!item.externalUrls.isNullOrEmpty()) {
            ExternalLinksSection(item)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Section title text.
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

/**
 * Technical specifications section.
 */
@Composable
private fun TechnicalSpecsSection(item: JellyfinItem) {
    val videoStream = item.videoStream
    val mediaSources = item.mediaSources

    if (videoStream == null && mediaSources.isNullOrEmpty()) return

    Column {
        SectionTitle("Technical Details")
        Spacer(modifier = Modifier.height(8.dp))

        // Video info
        videoStream?.let { video ->
            // Resolution
            if (video.width != null && video.height != null) {
                DetailItem(
                    label = "Resolution",
                    value = buildString {
                        append("${video.width} x ${video.height}")
                        if (item.is4K) append(" (4K)")
                    },
                )
            }

            // Codec
            video.codec?.let { codec ->
                DetailItem(
                    label = "Video Codec",
                    value = codec.uppercase(),
                )
            }

            // HDR info
            val hdrInfo = buildString {
                if (item.isDolbyVision) append("Dolby Vision")
                else if (item.isHdr) append("HDR")
                else append("SDR")
            }
            DetailItem(label = "Dynamic Range", value = hdrInfo)

            // Bitrate
            video.bitRate?.let { bitrate ->
                val mbps = bitrate / 1_000_000f
                DetailItem(
                    label = "Video Bitrate",
                    value = String.format(java.util.Locale.US, "%.1f Mbps", mbps),
                )
            }
        }

        // Audio info
        val audioStream = mediaSources?.firstOrNull()?.mediaStreams
            ?.find { it.type == "Audio" && it.isDefault }

        audioStream?.let { audio ->
            // Audio codec
            audio.codec?.let { codec ->
                DetailItem(
                    label = "Audio Codec",
                    value = codec.uppercase(),
                )
            }

            // Channels
            audio.channels?.let { channels ->
                val channelName = when (channels) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    6 -> "5.1"
                    8 -> "7.1"
                    else -> "$channels channels"
                }
                DetailItem(label = "Audio Channels", value = channelName)
            }

            // Language
            audio.language?.let { lang ->
                DetailItem(label = "Language", value = lang.uppercase())
            }
        }

        // Container format
        mediaSources?.firstOrNull()?.container?.let { container ->
            DetailItem(label = "Container", value = container.uppercase())
        }
    }
}

/**
 * External links section.
 */
@Composable
private fun ExternalLinksSection(item: JellyfinItem) {
    Column {
        SectionTitle("External Links")
        Spacer(modifier = Modifier.height(8.dp))

        item.externalUrls?.forEach { url ->
            Text(
                text = url.name ?: url.url ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Single detail item with label and value.
 */
@Composable
private fun DetailItem(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
