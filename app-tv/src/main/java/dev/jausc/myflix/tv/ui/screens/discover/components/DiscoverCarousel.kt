@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicatorSmall
import dev.jausc.myflix.tv.ui.screens.discover.MediaCategory
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

/**
 * A horizontal carousel row for the Discover screen.
 *
 * Features:
 * - Category title with accent bar
 * - Horizontal scrolling media cards
 * - D-pad navigation with proper edge handling
 * - Auto-scroll to keep selected item centered
 * - Auto-load more when approaching end of list
 * - Loading placeholder at row end
 *
 * @param category The media category for this row
 * @param items List of media items to display
 * @param onItemClick Callback when an item is clicked
 * @param modifier Optional modifier
 * @param isLoading Whether more items are being loaded
 * @param onLoadMore Callback to load more items (triggered near end of list)
 * @param onRowFocused Callback when the row gains focus
 * @param onItemFocused Callback when an item gains focus
 * @param focusRequester Optional focus requester for parent focus management
 * @param accentColor Color for the category title accent bar
 */
@Composable
fun DiscoverCarousel(
    category: MediaCategory,
    items: List<SeerrMedia>,
    onItemClick: (SeerrMedia) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    onRowFocused: (() -> Unit)? = null,
    onItemFocused: ((SeerrMedia) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    accentColor: Color = TvColors.BluePrimary,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedIndex by remember { mutableIntStateOf(0) }

    // Auto-load more when approaching end of list
    LaunchedEffect(selectedIndex, items.size) {
        if (onLoadMore != null && items.isNotEmpty() && selectedIndex >= items.size - 3) {
            onLoadMore()
        }
    }

    // Auto-scroll to keep selected item visible (centered when at position 3+)
    LaunchedEffect(selectedIndex) {
        if (items.isEmpty()) return@LaunchedEffect

        // Center the selected item when it's at position 3 or beyond
        val targetIndex = if (selectedIndex >= 3) {
            (selectedIndex - 2).coerceAtLeast(0)
        } else {
            0
        }

        coroutineScope.launch {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Category title with accent bar
        CategoryTitle(
            title = category.title,
            accentColor = accentColor,
        )

        // Media cards row
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (selectedIndex > 0) {
                                selectedIndex--
                                true // Consume event
                            } else {
                                false // At first item, don't consume - allow NavRail focus
                            }
                        }
                        Key.DirectionRight -> {
                            if (selectedIndex < items.size - 1) {
                                selectedIndex++
                                true // Consume event
                            } else if (isLoading) {
                                // At end but loading more, stay put
                                true
                            } else {
                                false // At last item with no more loading
                            }
                        }
                        else -> false
                    }
                },
        ) {
            itemsIndexed(items, key = { _, media -> "${media.mediaType}_${media.id}" }) { index, media ->
                val itemFocusRequester = remember { FocusRequester() }

                DiscoverMediaCard(
                    media = media,
                    onClick = { onItemClick(media) },
                    modifier = Modifier
                        .then(
                            if (index == 0 && focusRequester != null) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier.focusRequester(itemFocusRequester)
                            },
                        )
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                selectedIndex = index
                                onRowFocused?.invoke()
                                onItemFocused?.invoke(media)
                            }
                        },
                    onFocusChanged = { isFocused ->
                        if (isFocused) {
                            selectedIndex = index
                        }
                    },
                )
            }

            // Loading placeholder at end
            if (isLoading) {
                item(key = "loading_${category.name}") {
                    LoadingPlaceholder()
                }
            }
        }
    }
}

/**
 * Category title with accent bar.
 */
@Composable
private fun CategoryTitle(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 48.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(accentColor, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Loading placeholder card shown at the end of the row.
 */
@Composable
private fun LoadingPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(DiscoverCardSizes.CardWidth)
            .height(DiscoverCardSizes.CardHeight)
            .background(
                TvColors.SurfaceElevated,
                RoundedCornerShape(DiscoverCardSizes.CornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        TvLoadingIndicatorSmall(
            modifier = Modifier.size(24.dp),
            color = TvColors.TextSecondary,
        )
    }
}
