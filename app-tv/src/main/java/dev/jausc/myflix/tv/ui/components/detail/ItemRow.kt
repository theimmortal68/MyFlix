@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Generic horizontal row of items with title.
 * Supports focus restoration for D-pad navigation.
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
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    cardOnFocus: ((isFocused: Boolean, index: Int) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val state = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TvColors.TextPrimary,
        )
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
            contentPadding = PaddingValues(start = horizontalPadding, top = 12.dp, end = horizontalPadding, bottom = 32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { clip = false }
                .focusRestorer(firstFocus),
        ) {
            itemsIndexed(items) { index, item ->
                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                var itemRect by remember { mutableStateOf(Rect.Zero) }

                val cardModifier = Modifier
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInParent()
                        val size = coordinates.size
                        itemRect = Rect(
                            left = position.x,
                            top = position.y,
                            right = position.x + size.width,
                            bottom = position.y + size.height,
                        )
                    }
                    .then(
                        if (index == 0) Modifier.focusRequester(firstFocus) else Modifier
                    )
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusedIndex = index
                            scope.launch {
                                // Expand rect to account for 1.1x scale on focus
                                // Scale is from center, so bottom extends by 5% of height
                                val scaleExpansion = itemRect.height * 0.05f
                                val expandedRect = itemRect.copy(
                                    bottom = itemRect.bottom + scaleExpansion,
                                )
                                bringIntoViewRequester.bringIntoView(expandedRect)
                            }
                        }
                        cardOnFocus?.invoke(it.isFocused, index)
                    }

                cardContent.invoke(
                    index,
                    item,
                    cardModifier,
                    { if (item != null) onItemClick.invoke(index, item) },
                    { if (item != null) onItemLongClick.invoke(index, item) },
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
        cardContent = { _, item, cardModifier, onClick, onLongClick ->
            if (item != null) {
                MediaCard(
                    item = item,
                    imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                    onClick = onClick,
                    onLongClick = onLongClick,
                    modifier = cardModifier,
                )
            }
        },
        modifier = modifier,
    )
}
