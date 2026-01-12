@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Dialog for advanced library filters: watched status, rating, year range.
 */
@Composable
fun LibraryFilterDialog(
    currentWatchedFilter: WatchedFilter,
    currentRatingFilter: Float?,
    currentYearRange: YearRange,
    onApply: (WatchedFilter, Float?, YearRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Local state for editing
    var watchedFilter by remember { mutableStateOf(currentWatchedFilter) }
    var ratingFilter by remember { mutableStateOf(currentRatingFilter) }
    var yearFrom by remember { mutableStateOf(currentYearRange.from) }
    var yearTo by remember { mutableStateOf(currentYearRange.to) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val ratingOptions = listOf(
        null to "Any",
        5f to "5+",
        6f to "6+",
        7f to "7+",
        8f to "8+",
    )

    val currentYear = YearRange.currentYear
    val yearOptions = listOf(null) + (currentYear downTo 1950).toList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(TvColors.Surface, MaterialTheme.shapes.medium)
                .padding(20.dp),
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Watched Status Section
            Text(
                text = "Watched Status",
                style = MaterialTheme.typography.titleSmall,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(WatchedFilter.entries) { filter ->
                    val isSelected = watchedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { watchedFilter = filter },
                        modifier = if (filter == WatchedFilter.ALL) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
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
                            containerColor = TvColors.SurfaceElevated,
                            contentColor = TvColors.TextSecondary,
                            focusedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                            focusedContentColor = TvColors.TextPrimary,
                            selectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.2f),
                            selectedContentColor = TvColors.BluePrimary,
                            focusedSelectedContainerColor = TvColors.BluePrimary.copy(alpha = 0.4f),
                            focusedSelectedContentColor = TvColors.BluePrimary,
                        ),
                    ) {
                        Text(filter.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating Section
            Text(
                text = "Minimum Rating",
                style = MaterialTheme.typography.titleSmall,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(ratingOptions) { (rating, label) ->
                    val isSelected = ratingFilter == rating
                    FilterChip(
                        selected = isSelected,
                        onClick = { ratingFilter = rating },
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
                            containerColor = TvColors.SurfaceElevated,
                            contentColor = TvColors.TextSecondary,
                            focusedContainerColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                            focusedContentColor = TvColors.TextPrimary,
                            selectedContainerColor = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            selectedContentColor = Color(0xFFFBBF24),
                            focusedSelectedContainerColor = Color(0xFFFBBF24).copy(alpha = 0.4f),
                            focusedSelectedContentColor = Color(0xFFFBBF24),
                        ),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Year Range Section
            Text(
                text = "Year Range",
                style = MaterialTheme.typography.titleSmall,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // From year
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.TextSecondary,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(yearOptions.take(20)) { year ->
                            val isSelected = yearFrom == year
                            FilterChip(
                                selected = isSelected,
                                onClick = { yearFrom = year },
                                colors = FilterChipDefaults.colors(
                                    containerColor = TvColors.SurfaceElevated,
                                    contentColor = TvColors.TextSecondary,
                                    focusedContainerColor = Color(0xFF34D399).copy(alpha = 0.3f),
                                    focusedContentColor = TvColors.TextPrimary,
                                    selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                                    selectedContentColor = Color(0xFF34D399),
                                    focusedSelectedContainerColor = Color(0xFF34D399).copy(alpha = 0.4f),
                                    focusedSelectedContentColor = Color(0xFF34D399),
                                ),
                            ) {
                                Text(
                                    text = year?.toString() ?: "Any",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // To year
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "To",
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.TextSecondary,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(yearOptions.take(20)) { year ->
                            val isSelected = yearTo == year
                            FilterChip(
                                selected = isSelected,
                                onClick = { yearTo = year },
                                colors = FilterChipDefaults.colors(
                                    containerColor = TvColors.SurfaceElevated,
                                    contentColor = TvColors.TextSecondary,
                                    focusedContainerColor = Color(0xFF34D399).copy(alpha = 0.3f),
                                    focusedContentColor = TvColors.TextPrimary,
                                    selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                                    selectedContentColor = Color(0xFF34D399),
                                    focusedSelectedContainerColor = Color(0xFF34D399).copy(alpha = 0.4f),
                                    focusedSelectedContentColor = Color(0xFF34D399),
                                ),
                            ) {
                                Text(
                                    text = year?.toString() ?: "Any",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                Button(
                    onClick = {
                        // Clear all filters
                        watchedFilter = WatchedFilter.ALL
                        ratingFilter = null
                        yearFrom = null
                        yearTo = null
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceElevated,
                        contentColor = TvColors.TextSecondary,
                        focusedContainerColor = TvColors.Error.copy(alpha = 0.8f),
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Text("Clear All")
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceElevated,
                        contentColor = TvColors.TextPrimary,
                    ),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onApply(watchedFilter, ratingFilter, YearRange(yearFrom, yearTo))
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.BluePrimary,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
