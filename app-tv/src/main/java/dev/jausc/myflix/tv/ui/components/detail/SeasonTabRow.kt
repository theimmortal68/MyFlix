@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
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
    modifier: Modifier = Modifier,
) {
    // Use season name if available, otherwise "Season X"
    val tabText = season.name.ifBlank {
        "Season ${season.indexNumber ?: 1}"
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) TvColors.BluePrimary else TvColors.SurfaceElevated,
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
        Text(
            text = tabText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}
