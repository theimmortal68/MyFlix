@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange

/**
 * Bottom sheet for advanced library filters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterSheet(
    currentWatchedFilter: WatchedFilter,
    currentRatingFilter: Float?,
    currentYearRange: YearRange,
    onApply: (WatchedFilter, Float?, YearRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    // Local state for editing
    var watchedFilter by remember { mutableStateOf(currentWatchedFilter) }
    var ratingFilter by remember { mutableStateOf(currentRatingFilter) }
    var yearFrom by remember { mutableStateOf(currentYearRange.from) }
    var yearTo by remember { mutableStateOf(currentYearRange.to) }

    val ratingOptions = listOf(
        null to "Any",
        5f to "5+",
        6f to "6+",
        7f to "7+",
        8f to "8+",
    )

    val currentYear = YearRange.currentYear
    val yearOptions = listOf(null) + (currentYear downTo 1980).toList()

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
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // Watched Status Section
            Text(
                text = "Watched Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                WatchedFilter.entries.forEach { filter ->
                    val isSelected = watchedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { watchedFilter = filter },
                        label = { Text(filter.label) },
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
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rating Section
            Text(
                text = "Minimum Rating",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                ratingOptions.forEach { (rating, label) ->
                    val isSelected = ratingFilter == rating
                    FilterChip(
                        selected = isSelected,
                        onClick = { ratingFilter = rating },
                        label = { Text(label) },
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
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Year Range Section
            Text(
                text = "Year Range",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // From Year
            Text(
                text = "From",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                yearOptions.take(15).forEach { year ->
                    val isSelected = yearFrom == year
                    FilterChip(
                        selected = isSelected,
                        onClick = { yearFrom = year },
                        label = { Text(year?.toString() ?: "Any") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // To Year
            Text(
                text = "To",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                yearOptions.take(15).forEach { year ->
                    val isSelected = yearTo == year
                    FilterChip(
                        selected = isSelected,
                        onClick = { yearTo = year },
                        label = { Text(year?.toString() ?: "Any") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        watchedFilter = WatchedFilter.ALL
                        ratingFilter = null
                        yearFrom = null
                        yearTo = null
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear All")
                }

                Button(
                    onClick = {
                        onApply(watchedFilter, ratingFilter, YearRange(yearFrom, yearTo))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
