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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Icon-based filter bar for library screens.
 *
 * Layout: Title | ItemCount | Spacer | [Poster][Thumbnail] | [Filter][Sort][Shuffle][ScrollUp]
 */
@Composable
fun LibraryFilterBar(
    libraryName: String,
    totalItems: Int,
    loadedItems: Int,
    filterState: LibraryFilterState,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onScrollToTopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Library title
        Text(
            text = libraryName,
            style = MaterialTheme.typography.headlineSmall,
            color = TvColors.TextPrimary,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Item count
        Text(
            text = "$loadedItems of $totalItems items",
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextSecondary,
        )

        Spacer(modifier = Modifier.weight(1f))

        // View mode toggle buttons
        ViewModeButtons(
            currentMode = filterState.viewMode,
            onModeChange = onViewModeChange,
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Action buttons (icon-only): Filter, Sort, Shuffle, Scroll Up
        ActionIconButton(
            icon = Icons.Outlined.FilterAlt,
            contentDescription = "Filter",
            hasActiveState = filterState.hasActiveFilters,
            onClick = onFilterClick,
        )

        ActionIconButton(
            icon = Icons.AutoMirrored.Outlined.Sort,
            contentDescription = "Sort",
            hasActiveState = false,
            onClick = onSortClick,
        )

        ActionIconButton(
            icon = Icons.Outlined.Shuffle,
            contentDescription = "Shuffle",
            hasActiveState = false,
            onClick = onShuffleClick,
        )

        ActionIconButton(
            icon = Icons.Outlined.KeyboardArrowUp,
            contentDescription = "Scroll to Top",
            hasActiveState = false,
            onClick = onScrollToTopClick,
        )
    }
}

@Composable
private fun ViewModeButtons(
    currentMode: LibraryViewMode,
    onModeChange: (LibraryViewMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Poster button (grid icon)
        Button(
            onClick = { onModeChange(LibraryViewMode.POSTER) },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = if (currentMode == LibraryViewMode.POSTER) {
                    TvColors.BluePrimary.copy(alpha = 0.3f)
                } else {
                    TvColors.SurfaceElevated.copy(alpha = 0.8f)
                },
                contentColor = if (currentMode == LibraryViewMode.POSTER) {
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
                contentDescription = "Poster View",
                modifier = Modifier.size(14.dp),
            )
        }

        // Thumbnail button (wide view icon)
        Button(
            onClick = { onModeChange(LibraryViewMode.THUMBNAIL) },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = if (currentMode == LibraryViewMode.THUMBNAIL) {
                    TvColors.BluePrimary.copy(alpha = 0.3f)
                } else {
                    TvColors.SurfaceElevated.copy(alpha = 0.8f)
                },
                contentColor = if (currentMode == LibraryViewMode.THUMBNAIL) {
                    TvColors.BluePrimary
                } else {
                    TvColors.TextSecondary
                },
                focusedContainerColor = TvColors.BluePrimary,
                focusedContentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.ViewModule,
                contentDescription = "Thumbnail View",
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    hasActiveState: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(20.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = if (hasActiveState) {
                TvColors.BluePrimary.copy(alpha = 0.3f)
            } else {
                TvColors.SurfaceElevated.copy(alpha = 0.8f)
            },
            contentColor = if (hasActiveState) {
                TvColors.BluePrimary
            } else {
                TvColors.TextSecondary
            },
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
        )
    }
}
