@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal season selection bar for TV shows.
 * Shows season chips that can be selected via D-pad.
 */
@Composable
fun SeasonBar(
    seasons: List<JellyfinItem>,
    selectedSeason: JellyfinItem?,
    onSeasonSelected: (JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
) {
    if (seasons.isEmpty()) return

    LazyRow(
        modifier = modifier.focusRequester(focusRequester),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 48.dp),
    ) {
        items(seasons, key = { it.id }) { season ->
            val isSelected = selectedSeason?.id == season.id
            SeasonChip(
                season = season,
                isSelected = isSelected,
                onClick = { onSeasonSelected(season) },
            )
        }
    }
}

/**
 * Season selector chip.
 */
@Composable
private fun SeasonChip(
    season: JellyfinItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) TvColors.BluePrimary else TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Text(
            text = season.name,
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
