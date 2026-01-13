@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal row of cast and crew cards.
 * Shows person photos with names and roles.
 */
@Composable
fun CastCrewRow(
    people: List<JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (JellyfinPerson) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Cast & Crew",
) {
    if (people.isEmpty()) return

    Column(modifier = modifier) {
        // Section header
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 48.dp),
        ) {
            items(people, key = { it.id }) { person ->
                PersonCard(
                    person = person,
                    jellyfinClient = jellyfinClient,
                    onClick = { onPersonClick(person) },
                )
            }
        }
    }
}
