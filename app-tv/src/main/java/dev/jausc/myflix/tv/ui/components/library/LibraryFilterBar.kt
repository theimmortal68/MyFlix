@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Shuffle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Plex-style filter bar for library screens.
 *
 * Layout: [Grid][List] | Genre chips... | [Sort] [Filters] [Shuffle]
 */
@Composable
fun LibraryFilterBar(
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // View mode toggle buttons
        ViewModeButtons(
            currentMode = filterState.viewMode,
            onModeChange = onViewModeChange,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Genre chips (scrollable)
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(availableGenres) { genre ->
                val isSelected = filterState.selectedGenres.contains(genre)
                GenreChip(
                    genre = genre,
                    isSelected = isSelected,
                    onClick = { onGenreToggle(genre) },
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sort button
        SortChip(
            sortDisplayText = filterState.sortDisplayText,
            isNonDefault = filterState.sortBy != LibrarySortOption.TITLE,
            onClick = onSortClick,
        )

        // Filters button
        FiltersChip(
            activeFilterCount = filterState.activeFilterCount,
            onClick = onFiltersClick,
        )

        // Shuffle button
        ShuffleButton(onClick = onShuffleClick)
    }
}

@Composable
private fun ViewModeButtons(
    currentMode: LibraryViewMode,
    onModeChange: (LibraryViewMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Grid button
        Button(
            onClick = { onModeChange(LibraryViewMode.GRID) },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = if (currentMode == LibraryViewMode.GRID) {
                    TvColors.BluePrimary.copy(alpha = 0.3f)
                } else {
                    TvColors.SurfaceElevated.copy(alpha = 0.8f)
                },
                contentColor = if (currentMode == LibraryViewMode.GRID) {
                    TvColors.BluePrimary
                } else {
                    TvColors.TextSecondary
                },
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = "Grid View",
                modifier = Modifier.size(14.dp),
            )
        }

        // List button
        Button(
            onClick = { onModeChange(LibraryViewMode.LIST) },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = if (currentMode == LibraryViewMode.LIST) {
                    TvColors.BluePrimary.copy(alpha = 0.3f)
                } else {
                    TvColors.SurfaceElevated.copy(alpha = 0.8f)
                },
                contentColor = if (currentMode == LibraryViewMode.LIST) {
                    TvColors.BluePrimary
                } else {
                    TvColors.TextSecondary
                },
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ViewList,
                contentDescription = "List View",
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun GenreChip(
    genre: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.colors(
            containerColor = TvColors.Surface,
            contentColor = TvColors.TextSecondary,
            focusedContainerColor = TvColors.SurfaceElevated,
            focusedContentColor = TvColors.TextPrimary,
            selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
            selectedContentColor = Color(0xFF8B5CF6),
            focusedSelectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.3f),
            focusedSelectedContentColor = Color(0xFF8B5CF6),
        ),
    ) {
        Text(genre, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SortChip(
    sortDisplayText: String,
    isNonDefault: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isNonDefault,
        onClick = onClick,
        leadingIcon = if (isNonDefault) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.colors(
            containerColor = TvColors.Surface,
            contentColor = TvColors.TextSecondary,
            focusedContainerColor = TvColors.SurfaceElevated,
            focusedContentColor = TvColors.TextPrimary,
            selectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.2f),
            selectedContentColor = TvColors.BluePrimary,
            focusedSelectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
            focusedSelectedContentColor = TvColors.BluePrimary,
        ),
    ) {
        Text("Sort: $sortDisplayText", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FiltersChip(
    activeFilterCount: Int,
    onClick: () -> Unit,
) {
    val hasFilters = activeFilterCount > 0

    FilterChip(
        selected = hasFilters,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        colors = FilterChipDefaults.colors(
            containerColor = TvColors.Surface,
            contentColor = TvColors.TextSecondary,
            focusedContainerColor = TvColors.SurfaceElevated,
            focusedContentColor = TvColors.TextPrimary,
            selectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.2f),
            selectedContentColor = TvColors.BluePrimary,
            focusedSelectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
            focusedSelectedContentColor = TvColors.BluePrimary,
        ),
    ) {
        Text(
            text = if (hasFilters) "Filters ($activeFilterCount)" else "Filters",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ShuffleButton(
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(20.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
            contentColor = TvColors.TextSecondary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = Icons.Outlined.Shuffle,
            contentDescription = "Shuffle Play",
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Shuffle", style = MaterialTheme.typography.labelSmall)
    }
}
