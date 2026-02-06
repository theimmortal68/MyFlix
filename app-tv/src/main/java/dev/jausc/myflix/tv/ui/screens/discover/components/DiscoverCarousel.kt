@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.discover.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicatorSmall
import dev.jausc.myflix.tv.ui.screens.discover.MediaCategory
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * A horizontal carousel row for the Discover screen.
 *
 * Features:
 * - Category title with accent bar
 * - Horizontal scrolling media cards via natural Compose focus navigation
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
@OptIn(ExperimentalFoundationApi::class)
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
    var selectedIndex by remember { mutableIntStateOf(0) }

    // Auto-load more when approaching end of list
    LaunchedEffect(selectedIndex, items.size) {
        if (onLoadMore != null && items.isNotEmpty() && selectedIndex >= items.size - 3) {
            onLoadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Category title with accent bar
        CategoryTitle(
            title = category.title,
            accentColor = accentColor,
        )

        // Media cards row - uses natural Compose focus navigation (no manual key handling)
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides object : BringIntoViewSpec {
                @ExperimentalFoundationApi
                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float,
                ): Float {
                    return when {
                        offset < 0 -> offset
                        offset + size > containerSize -> offset + size - containerSize
                        else -> 0f
                    }
                }
            },
        ) {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 4.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(
                    items,
                    key = { _, media -> "${media.mediaType}_${media.id}" },
                ) { index, media ->
                    DiscoverMediaCard(
                        media = media,
                        onClick = { onItemClick(media) },
                        modifier = Modifier
                            .then(
                                if (index == 0 && focusRequester != null) {
                                    Modifier.focusRequester(focusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    selectedIndex = index
                                    onRowFocused?.invoke()
                                    onItemFocused?.invoke(media)
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
        modifier = modifier,
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
