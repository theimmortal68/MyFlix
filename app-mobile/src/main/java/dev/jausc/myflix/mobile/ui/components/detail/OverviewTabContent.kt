@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.creators
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient

/**
 * Overview tab content for mobile detail screen.
 */
@Composable
fun MobileOverviewTabContent(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val actors = item.actors.take(8)
    val directors = item.directors
    val creators = item.creators

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Description
        item.overview?.let { overview ->
            Column {
                SectionTitle("Synopsis")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Genres
        if (!item.genres.isNullOrEmpty()) {
            Column {
                SectionTitle("Genres")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.genres!!.take(5).forEach { genre ->
                        AssistChip(
                            onClick = { },
                            label = { Text(genre) },
                        )
                    }
                }
            }
        }

        // Directors/Creators
        if (directors.isNotEmpty() || creators.isNotEmpty()) {
            Column {
                SectionTitle(if (item.type == "Series") "Created By" else "Directed By")
                Spacer(modifier = Modifier.height(8.dp))
                val people = if (item.type == "Series") creators else directors
                Text(
                    text = people.mapNotNull { it.name }.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Cast preview
        if (actors.isNotEmpty()) {
            Column {
                SectionTitle("Cast")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp),
                ) {
                    items(actors, key = { it.id }) { actor ->
                        MobilePersonCard(
                            person = actor,
                            jellyfinClient = jellyfinClient,
                            onClick = { /* TODO: Navigate to person */ },
                        )
                    }
                }
            }
        }

        // Quality info
        val qualityLabel = item.videoQualityLabel
        if (qualityLabel.isNotBlank()) {
            Column {
                SectionTitle("Video Quality")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = qualityLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
