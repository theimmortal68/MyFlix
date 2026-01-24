@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Circular person card for cast and crew display.
 * Shows person photo with name and optional role below.
 */
@Suppress("UnusedParameter")
@Composable
fun PersonCard(
    person: JellyfinPerson,
    jellyfinClient: JellyfinClient,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showRole: Boolean = true,
) {
    val imageUrl = jellyfinClient.getPersonImageUrl(person.id, person.primaryImageTag)

    Surface(
        onClick = onClick,
        modifier = modifier.width(100.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface.copy(alpha = 0.5f),
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Circular person image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(TvColors.SurfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                if (person.primaryImageTag != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                    )
                } else {
                    // Placeholder with initials
                    Text(
                        text = person.name?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }

            // Name
            Text(
                text = person.name ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            // Role (for actors)
            if (showRole && !person.role.isNullOrBlank()) {
                Text(
                    text = person.role!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = TvColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Compact person row for credits lists in Details tab.
 * Shows name and role/type in a single line.
 */
@Composable
fun PersonRow(person: JellyfinPerson, modifier: Modifier = Modifier,) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        Text(
            text = person.name ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!person.role.isNullOrBlank()) {
            Text(
                text = person.role!!,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = person.type,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
