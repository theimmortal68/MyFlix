@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode

/**
 * Mobile filter bar for library screens.
 * Horizontal scrolling chips with icon buttons for actions.
 */
@Composable
fun MobileLibraryFilterBar(
    filterState: LibraryFilterState,
    availableGenres: List<String>,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onGenreToggle: (String) -> Unit,
    onSortClick: () -> Unit,
    onFiltersClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // View mode toggle
        IconButton(
            onClick = {
                val newMode = when (filterState.viewMode) {
                    LibraryViewMode.POSTER -> LibraryViewMode.THUMBNAIL
                    LibraryViewMode.THUMBNAIL -> LibraryViewMode.POSTER
                }
                onViewModeChange(newMode)
            },
        ) {
            Icon(
                imageVector = when (filterState.viewMode) {
                    LibraryViewMode.POSTER -> Icons.Outlined.GridView
                    LibraryViewMode.THUMBNAIL -> Icons.AutoMirrored.Outlined.ViewList
                },
                contentDescription = "Toggle view mode",
            )
        }

        // Scrollable chips row
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Sort chip
            FilterChip(
                selected = filterState.sortBy != LibrarySortOption.TITLE,
                onClick = onSortClick,
                label = { Text("Sort: ${filterState.sortDisplayText}") },
                leadingIcon = if (filterState.sortBy != LibrarySortOption.TITLE) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    null
                },
            )

            // Filters chip
            FilterChip(
                selected = filterState.hasActiveFilters,
                onClick = onFiltersClick,
                label = {
                    Text(
                        if (filterState.activeFilterCount > 0) {
                            "Filters (${filterState.activeFilterCount})"
                        } else {
                            "Filters"
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )

            // Genre chips
            availableGenres.forEach { genre ->
                val isSelected = filterState.selectedGenres.contains(genre)
                FilterChip(
                    selected = isSelected,
                    onClick = { onGenreToggle(genre) },
                    label = { Text(genre) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Shuffle button
        IconButton(onClick = onShuffleClick) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = "Shuffle play",
            )
        }
    }
}
