@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal scrollable season tabs for series detail screen.
 * Displays "Season 1", "Season 2", etc. with selected tab highlighted.
 *
 * @param seasons List of season items
 * @param selectedSeasonIndex Index of currently selected season
 * @param onSeasonSelected Callback when a season tab is selected (index, season)
 * @param modifier Modifier for the row
 * @param firstTabFocusRequester Optional focus requester for the first tab
 */
@Composable
fun SeasonTabRow(
    seasons: List<JellyfinItem>,
    selectedSeasonIndex: Int,
    onSeasonSelected: (Int, JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
    firstTabFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    if (seasons.isEmpty()) return

    val lazyListState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }

    // Scroll to selected season when it changes
    LaunchedEffect(selectedSeasonIndex) {
        if (selectedSeasonIndex >= 0 && selectedSeasonIndex < seasons.size) {
            lazyListState.animateScrollToItem(selectedSeasonIndex)
        }
    }

    LazyRow(
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
        modifier = modifier.focusRestorer(firstFocus),
    ) {
        itemsIndexed(seasons, key = { _, season -> season.id }) { index, season ->
            val isSelected = index == selectedSeasonIndex
            val tabModifier = if (index == 0) {
                Modifier
                    .focusRequester(firstFocus)
                    .then(
                        if (firstTabFocusRequester != null) {
                            Modifier.focusRequester(firstTabFocusRequester)
                        } else {
                            Modifier
                        },
                    )
            } else {
                Modifier
            }

            SeasonTab(
                season = season,
                isSelected = isSelected,
                onClick = { onSeasonSelected(index, season) },
                downFocusRequester = downFocusRequester,
                upFocusRequester = upFocusRequester,
                modifier = tabModifier,
            )
        }
    }
}

/**
 * Individual season tab button.
 */
@Composable
private fun SeasonTab(
    season: JellyfinItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    downFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    // Use season name if available, otherwise "Season X"
    val tabText = season.name.ifBlank {
        "Season ${season.indexNumber ?: 1}"
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(16.dp)
            .focusProperties {
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
                if (upFocusRequester != null) {
                    up = upFocusRequester
                }
            },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) TvColors.BluePrimary else TvColors.SurfaceElevated.copy(alpha = 0.8f),
            focusedContainerColor = TvColors.BluePrimary,
            contentColor = if (isSelected) Color.White else TvColors.TextPrimary,
            focusedContentColor = Color.White,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = tabText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
