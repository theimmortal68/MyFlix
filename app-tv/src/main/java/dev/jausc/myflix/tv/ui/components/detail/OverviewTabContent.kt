@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.actors
import dev.jausc.myflix.core.common.model.creators
import dev.jausc.myflix.core.common.model.directors
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Overview tab content for detail screen.
 * Shows description, genres, cast preview, and quality info.
 */
@Composable
fun OverviewTabContent(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester = FocusRequester(),
) {
    val scrollState = rememberScrollState()
    val actors = item.actors.take(8)
    val directors = item.directors
    val creators = item.creators

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Description
        item.overview?.let { overview ->
            Column {
                SectionTitle("Synopsis")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(0.7f),
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
                    item.genres!!.forEach { genre ->
                        GenreChip(genre)
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
                    color = TvColors.TextPrimary,
                )
            }
        }

        // Cast preview
        if (actors.isNotEmpty()) {
            Column {
                SectionTitle("Cast")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 48.dp),
                    modifier = Modifier.focusRequester(contentFocusRequester),
                ) {
                    items(actors, key = { it.id }) { actor ->
                        PersonCard(
                            person = actor,
                            jellyfinClient = jellyfinClient,
                            onClick = { /* TODO: Navigate to person detail */ },
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
                    color = TvColors.TextPrimary,
                )
            }
        }

        // Add some bottom padding
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
        color = TvColors.TextPrimary,
    )
}

/**
 * Genre chip/tag.
 */
@Composable
private fun GenreChip(genre: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TvColors.SurfaceElevated),
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
