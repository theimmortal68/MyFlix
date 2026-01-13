@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.network.JellyfinClient

/**
 * Horizontal row of cast and crew cards.
 */
@Composable
fun MobileCastCrewRow(
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
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(people, key = { it.id }) { person ->
                MobilePersonCard(
                    person = person,
                    jellyfinClient = jellyfinClient,
                    onClick = { onPersonClick(person) },
                )
            }
        }
    }
}

/**
 * Circular person card for cast and crew.
 */
@Composable
private fun MobilePersonCard(
    person: JellyfinPerson,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = jellyfinClient.getPersonImageUrl(person.id, person.primaryImageTag)

    Card(
        modifier = modifier
            .width(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Circular person image
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (person.primaryImageTag != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape),
                    )
                } else {
                    // Placeholder with initials
                    Text(
                        text = person.name?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Name
            Text(
                text = person.name ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            // Role (for actors)
            if (!person.role.isNullOrBlank()) {
                Text(
                    text = person.role!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
