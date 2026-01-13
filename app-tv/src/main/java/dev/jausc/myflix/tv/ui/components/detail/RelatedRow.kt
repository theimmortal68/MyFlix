@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal row of related/similar items.
 * Shows poster cards for recommendations.
 */
@Composable
fun RelatedRow(
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onItemLongClick: (JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "More Like This",
    isLoading: Boolean = false,
) {
    Column(modifier = modifier) {
        // Section header
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }
            items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No similar items found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            imageUrl = jellyfinClient.getPrimaryImageUrl(
                                item.id,
                                item.imageTags?.primary,
                            ),
                            onClick = { onItemClick(item.id) },
                            onLongClick = { onItemLongClick(item) },
                        )
                    }
                }
            }
        }
    }
}
