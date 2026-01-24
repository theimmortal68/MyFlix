@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.SortOrder

/**
 * Bottom sheet for selecting library sort option and order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortSheet(
    currentSortBy: LibrarySortOption,
    currentSortOrder: SortOrder,
    onSortSelected: (LibrarySortOption, SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Sort options
            LibrarySortOption.entries.forEach { option ->
                val isSelected = option == currentSortBy
                SortOptionItem(
                    label = option.label,
                    isSelected = isSelected,
                    onClick = { onSortSelected(option, currentSortOrder) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Order",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Sort order options
            SortOrder.entries.forEach { order ->
                val isSelected = order == currentSortOrder
                SortOptionItem(
                    label = order.label,
                    isSelected = isSelected,
                    onClick = { onSortSelected(currentSortBy, order) },
                )
            }
        }
    }
}

@Composable
private fun SortOptionItem(label: String, isSelected: Boolean, onClick: () -> Unit,) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
