@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.components.MobileMediaCard

/**
 * Generic horizontal row of items with title.
 *
 * @param T The type of items in the row
 * @param title Row title displayed above the items
 * @param items List of items to display
 * @param onItemClick Callback when an item is clicked
 * @param onItemLongClick Callback when an item is long-clicked
 * @param cardContent Composable for rendering each card
 * @param modifier Modifier for the row
 * @param horizontalPadding Horizontal padding between cards
 */
@Composable
fun <T> ItemRow(
    title: String,
    items: List<T?>,
    onItemClick: (Int, T) -> Unit,
    onItemLongClick: (Int, T) -> Unit,
    cardContent: @Composable (
        index: Int,
        item: T?,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
) {
    if (items.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items) { index, item ->
                cardContent(
                    index,
                    item,
                    { if (item != null) onItemClick(index, item) },
                    { if (item != null) onItemLongClick(index, item) },
                )
            }
        }
    }
}

/**
 * Convenience function for displaying a row of JellyfinItems with poster cards.
 */
@Composable
fun PosterItemRow(
    title: String,
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onItemLongClick: (JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    ItemRow(
        title = title,
        items = items,
        onItemClick = { _, item -> onItemClick(item.id) },
        onItemLongClick = { _, item -> onItemLongClick(item) },
        cardContent = { _, item, onClick, onLongClick ->
            if (item != null) {
                MobileMediaCard(
                    item = item,
                    imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            }
        },
        modifier = modifier,
    )
}
