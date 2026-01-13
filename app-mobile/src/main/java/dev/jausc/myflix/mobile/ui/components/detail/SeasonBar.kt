@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * Horizontal season selection bar for TV shows.
 * Shows season chips that can be tapped.
 */
@Composable
fun MobileSeasonBar(
    seasons: List<JellyfinItem>,
    selectedSeason: JellyfinItem?,
    onSeasonSelected: (JellyfinItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (seasons.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(seasons, key = { it.id }) { season ->
            val isSelected = selectedSeason?.id == season.id
            FilterChip(
                selected = isSelected,
                onClick = { onSeasonSelected(season) },
                label = { Text(season.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}
