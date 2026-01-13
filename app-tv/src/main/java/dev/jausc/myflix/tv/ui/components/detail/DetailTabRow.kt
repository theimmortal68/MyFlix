@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.screens.DetailTab
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal tab row for detail screen navigation.
 * Uses TV Surface components for proper D-pad focus handling.
 */
@Composable
fun DetailTabRow(
    tabs: List<DetailTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabRowFocusRequester: FocusRequester = FocusRequester(),
    heroFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
) {
    val listState = rememberLazyListState()
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }

    // Scroll to selected tab when it changes
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex.coerceAtLeast(0))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TvColors.Background.copy(alpha = 0.95f)),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            itemsIndexed(tabs, key = { _, tab -> tab.name }) { index, tab ->
                val isSelected = index == selectedIndex
                val isFocused = index == focusedIndex

                TabItem(
                    tab = tab,
                    isSelected = isSelected,
                    onSelect = { onTabSelected(index) },
                    modifier = Modifier
                        .then(
                            if (index == 0) {
                                Modifier.focusRequester(tabRowFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                focusedIndex = index
                            }
                        }
                        .focusProperties {
                            heroFocusRequester?.let { up = it }
                            contentFocusRequester?.let { down = it }
                        },
                )
            }
        }

        // Animated indicator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .padding(horizontal = 48.dp),
        ) {
            // Calculate indicator position and width based on selected tab
            val indicatorOffset by animateDpAsState(
                targetValue = (selectedIndex * 120).dp,
                label = "indicator_offset",
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(80.dp)
                    .height(2.dp)
                    .background(TvColors.BluePrimary, RoundedCornerShape(1.dp)),
            )
        }
    }
}

/**
 * Individual tab item with focus handling.
 */
@Composable
private fun TabItem(
    tab: DetailTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) TvColors.TextPrimary else TvColors.TextSecondary,
        label = "tab_text_color",
    )

    Surface(
        onClick = onSelect,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvColors.SurfaceElevated.copy(alpha = 0.5f),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tab.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = textColor,
            )
        }
    }
}
