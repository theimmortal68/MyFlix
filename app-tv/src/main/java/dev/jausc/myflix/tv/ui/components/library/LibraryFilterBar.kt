@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange
import dev.jausc.myflix.tv.ui.components.TvDropdownMenu
import dev.jausc.myflix.tv.ui.components.TvDropdownMenuItem
import dev.jausc.myflix.tv.ui.components.TvDropdownMenuItemWithCheck
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Icon-based filter bar for library screens with inline dropdown menus.
 *
 * Layout: Title | ItemCount | Spacer | [Poster][Thumbnail] | [Filter][Sort][Shuffle]
 */
@Composable
fun LibraryFilterBar(
    libraryName: String,
    totalItems: Int,
    loadedItems: Int,
    filterState: LibraryFilterState,
    availableGenres: List<String>,
    availableParentalRatings: List<String>,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onSortChange: (LibrarySortOption, SortOrder) -> Unit,
    onFilterChange: (WatchedFilter, Float?, YearRange) -> Unit,
    onGenreToggle: (String) -> Unit,
    onClearGenres: () -> Unit,
    onParentalRatingToggle: (String) -> Unit,
    onClearParentalRatings: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUpNavigation: () -> Unit = {},
    firstButtonFocusRequester: FocusRequester? = null,
    gridFocusRequester: FocusRequester? = null,
    alphabetFocusRequester: FocusRequester? = null,
) {
    // Focus requesters for button chaining
    val posterFocusRequester = remember { FocusRequester() }
    val thumbnailFocusRequester = remember { FocusRequester() }
    val filterFocusRequester = remember { FocusRequester() }
    val sortFocusRequester = remember { FocusRequester() }
    val shuffleFocusRequester = remember { FocusRequester() }

    // Dropdown states
    var showSortDropdown by remember { mutableStateOf(false) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyDown) {
                    onUpNavigation()
                    true
                } else {
                    false
                }
            },
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterBarButton(
                icon = Icons.Outlined.GridView,
                contentDescription = "Poster View",
                isSelected = filterState.viewMode == LibraryViewMode.POSTER,
                onClick = { onViewModeChange(LibraryViewMode.POSTER) },
                modifier = Modifier
                    .focusRequester(posterFocusRequester)
                    .then(
                        if (firstButtonFocusRequester != null) {
                            Modifier.focusRequester(firstButtonFocusRequester)
                        } else {
                            Modifier
                        }
                    )
                    .focusProperties {
                        left = FocusRequester.Cancel
                        right = thumbnailFocusRequester
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
            )

            FilterBarButton(
                icon = Icons.Outlined.ViewModule,
                contentDescription = "Thumbnail View",
                isSelected = filterState.viewMode == LibraryViewMode.THUMBNAIL,
                onClick = { onViewModeChange(LibraryViewMode.THUMBNAIL) },
                modifier = Modifier
                    .focusRequester(thumbnailFocusRequester)
                    .focusProperties {
                        left = posterFocusRequester
                        right = filterFocusRequester
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Filter button with dropdown
        Box {
            FilterBarButton(
                icon = Icons.Outlined.FilterAlt,
                contentDescription = "Filter",
                isSelected = filterState.hasActiveFilters,
                onClick = { showFilterDropdown = true },
                modifier = Modifier
                    .focusRequester(filterFocusRequester)
                    .focusProperties {
                        left = thumbnailFocusRequester
                        right = sortFocusRequester
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
            )

            FilterDropdownMenu(
                expanded = showFilterDropdown,
                currentWatchedFilter = filterState.watchedFilter,
                currentRatingFilter = filterState.ratingFilter,
                currentYearRange = filterState.yearRange,
                availableGenres = availableGenres,
                selectedGenres = filterState.selectedGenres,
                availableParentalRatings = availableParentalRatings,
                selectedParentalRatings = filterState.selectedParentalRatings,
                onGenreToggle = onGenreToggle,
                onClearGenres = onClearGenres,
                onParentalRatingToggle = onParentalRatingToggle,
                onClearParentalRatings = onClearParentalRatings,
                onFilterChange = { watched, rating ->
                    onFilterChange(watched, rating, filterState.yearRange)
                },
                onYearRangeChange = { range ->
                    onFilterChange(filterState.watchedFilter, filterState.ratingFilter, range)
                },
                onDismiss = { showFilterDropdown = false },
            )
        }

        // Sort button with dropdown
        Box {
            FilterBarButton(
                icon = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "Sort",
                isSelected = false,
                onClick = { showSortDropdown = true },
                modifier = Modifier
                    .focusRequester(sortFocusRequester)
                    .focusProperties {
                        left = filterFocusRequester
                        right = shuffleFocusRequester
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
            )

            SortDropdownMenu(
                expanded = showSortDropdown,
                currentSortBy = filterState.sortBy,
                currentSortOrder = filterState.sortOrder,
                onSortChange = onSortChange,
                onDismiss = { showSortDropdown = false },
            )
        }

        // Shuffle button
        FilterBarButton(
            icon = Icons.Outlined.Shuffle,
            contentDescription = "Shuffle",
            isSelected = false,
            onClick = onShuffleClick,
            modifier = Modifier
                .focusRequester(shuffleFocusRequester)
                .focusProperties {
                    left = sortFocusRequester
                    right = alphabetFocusRequester ?: FocusRequester.Cancel
                    down = gridFocusRequester ?: FocusRequester.Default
                },
        )
    }
}

/**
 * Sort dropdown menu anchored to button.
 */
@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    currentSortBy: LibrarySortOption,
    currentSortOrder: SortOrder,
    onSortChange: (LibrarySortOption, SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val itemTextStyle = MaterialTheme.typography.bodySmall
    TvDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
    ) {
        // Sort options
        LibrarySortOption.entries.forEach { option ->
            val isSelected = option == currentSortBy
            TvDropdownMenuItemWithCheck(
                text = option.label,
                isSelected = isSelected,
                textStyle = itemTextStyle,
                onClick = {
                    onSortChange(option, currentSortOrder)
                    onDismiss()
                },
            )
        }

        HorizontalDivider(color = TvColors.SurfaceElevated)

        // Sort order options
        SortOrder.entries.forEach { order ->
            val isSelected = order == currentSortOrder
            TvDropdownMenuItemWithCheck(
                text = order.label,
                isSelected = isSelected,
                textStyle = itemTextStyle,
                onClick = {
                    onSortChange(currentSortBy, order)
                    onDismiss()
                },
            )
        }
    }
}

/**
 * Filter dropdown menu anchored to button.
 */
@Composable
private fun FilterDropdownMenu(
    expanded: Boolean,
    currentWatchedFilter: WatchedFilter,
    currentRatingFilter: Float?,
    currentYearRange: YearRange,
    availableGenres: List<String>,
    selectedGenres: Set<String>,
    availableParentalRatings: List<String>,
    selectedParentalRatings: Set<String>,
    onGenreToggle: (String) -> Unit,
    onClearGenres: () -> Unit,
    onParentalRatingToggle: (String) -> Unit,
    onClearParentalRatings: () -> Unit,
    onFilterChange: (WatchedFilter, Float?) -> Unit,
    onYearRangeChange: (YearRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val headerStyle = MaterialTheme.typography.titleSmall
    val itemTextStyle = MaterialTheme.typography.bodySmall
    val submenuOffset = DpOffset(220.dp, 0.dp)
    var showWatchedMenu by remember { mutableStateOf(false) }
    var showRatingMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showGenreMenu by remember { mutableStateOf(false) }
    var showParentalMenu by remember { mutableStateOf(false) }

    val closeAllSubmenus = {
        showWatchedMenu = false
        showRatingMenu = false
        showYearMenu = false
        showGenreMenu = false
        showParentalMenu = false
    }
    val ratingOptions = listOf(
        null to "Any Rating",
        5f to "5+ Rating",
        6f to "6+ Rating",
        7f to "7+ Rating",
        8f to "8+ Rating",
    )
    val currentYear = YearRange.currentYear
    val recentYears = (currentYear downTo (currentYear - 20).coerceAtLeast(YearRange.MIN_YEAR)).toList()
    val yearOptions = buildList {
        val lastFiveStart = (currentYear - 4).coerceAtLeast(YearRange.MIN_YEAR)
        val lastTenStart = (currentYear - 9).coerceAtLeast(YearRange.MIN_YEAR)
        add(YearRange() to "Any Year")
        add(YearRange(lastFiveStart, currentYear) to "Last 5 Years")
        add(YearRange(lastTenStart, currentYear) to "Last 10 Years")
        recentYears.forEach { year ->
            add(YearRange(year, year) to year.toString())
        }
    }

    TvDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
    ) {
        LaunchedEffect(expanded) {
            if (!expanded) {
                closeAllSubmenus()
            }
        }

        Box {
            SubMenuItem(
                label = "Watched Status",
                headerStyle = headerStyle,
                onClick = {
                    closeAllSubmenus()
                    showWatchedMenu = true
                },
            )
            TvDropdownMenu(
                expanded = showWatchedMenu,
                onDismissRequest = { showWatchedMenu = false },
                offset = submenuOffset,
            ) {
                WatchedFilter.entries.forEach { filter ->
                    val isSelected = filter == currentWatchedFilter
                    TvDropdownMenuItemWithCheck(
                        text = filter.label,
                        isSelected = isSelected,
                        textStyle = itemTextStyle,
                        onLeftPressed = { showWatchedMenu = false },
                        onClick = {
                            onFilterChange(filter, currentRatingFilter)
                            onDismiss()
                        },
                    )
                }
            }
        }

        Box {
            SubMenuItem(
                label = "Minimum Rating",
                headerStyle = headerStyle,
                onClick = {
                    closeAllSubmenus()
                    showRatingMenu = true
                },
            )
            TvDropdownMenu(
                expanded = showRatingMenu,
                onDismissRequest = { showRatingMenu = false },
                offset = submenuOffset,
            ) {
                ratingOptions.forEach { (rating, label) ->
                    val isSelected = currentRatingFilter == rating
                    TvDropdownMenuItemWithCheck(
                        text = label,
                        isSelected = isSelected,
                        textStyle = itemTextStyle,
                        onLeftPressed = { showRatingMenu = false },
                        onClick = {
                            onFilterChange(currentWatchedFilter, rating)
                            onDismiss()
                        },
                    )
                }
            }
        }

        Box {
            SubMenuItem(
                label = "Year",
                headerStyle = headerStyle,
                onClick = {
                    closeAllSubmenus()
                    showYearMenu = true
                },
            )
            TvDropdownMenu(
                expanded = showYearMenu,
                onDismissRequest = { showYearMenu = false },
                offset = submenuOffset,
            ) {
                yearOptions.forEach { (range, label) ->
                    val isSelected = range == currentYearRange
                    TvDropdownMenuItemWithCheck(
                        text = label,
                        isSelected = isSelected,
                        textStyle = itemTextStyle,
                        onLeftPressed = { showYearMenu = false },
                        onClick = {
                            onYearRangeChange(range)
                            onDismiss()
                        },
                    )
                }
            }
        }

        Box {
            SubMenuItem(
                label = "Genres",
                headerStyle = headerStyle,
                onClick = {
                    closeAllSubmenus()
                    showGenreMenu = true
                },
            )
            TvDropdownMenu(
                expanded = showGenreMenu,
                onDismissRequest = { showGenreMenu = false },
                offset = submenuOffset,
            ) {
                val sortedGenres = availableGenres.sorted()
                if (sortedGenres.isEmpty()) {
                    TvDropdownMenuItem(
                        text = "No genres available",
                        onClick = {},
                        enabled = false,
                        textStyle = itemTextStyle,
                        onLeftPressed = { showGenreMenu = false },
                    )
                } else {
                    TvDropdownMenuItemWithCheck(
                        text = "All Genres",
                        isSelected = selectedGenres.isEmpty(),
                        textStyle = itemTextStyle,
                        onLeftPressed = { showGenreMenu = false },
                        onClick = {
                            onClearGenres()
                            onDismiss()
                        },
                    )
                    sortedGenres.forEach { genre ->
                        TvDropdownMenuItemWithCheck(
                            text = genre,
                            isSelected = selectedGenres.contains(genre),
                            textStyle = itemTextStyle,
                            onLeftPressed = { showGenreMenu = false },
                            onClick = {
                                onGenreToggle(genre)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }

        Box {
            SubMenuItem(
                label = "Parental Rating",
                headerStyle = headerStyle,
                onClick = {
                    closeAllSubmenus()
                    showParentalMenu = true
                },
            )
            TvDropdownMenu(
                expanded = showParentalMenu,
                onDismissRequest = { showParentalMenu = false },
                offset = submenuOffset,
            ) {
                val sortedRatings = availableParentalRatings.sorted()
                if (sortedRatings.isEmpty()) {
                    TvDropdownMenuItem(
                        text = "No ratings available",
                        onClick = {},
                        enabled = false,
                        textStyle = itemTextStyle,
                        onLeftPressed = { showParentalMenu = false },
                    )
                } else {
                    TvDropdownMenuItemWithCheck(
                        text = "All Ratings",
                        isSelected = selectedParentalRatings.isEmpty(),
                        textStyle = itemTextStyle,
                        onLeftPressed = { showParentalMenu = false },
                        onClick = {
                            onClearParentalRatings()
                            onDismiss()
                        },
                    )
                    sortedRatings.forEach { rating ->
                        TvDropdownMenuItemWithCheck(
                            text = rating,
                            isSelected = selectedParentalRatings.contains(rating),
                            textStyle = itemTextStyle,
                            onLeftPressed = { showParentalMenu = false },
                            onClick = {
                                onParentalRatingToggle(rating)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubMenuItem(
    label: String,
    headerStyle: TextStyle,
    onClick: () -> Unit,
) {
    TvDropdownMenuItem(
        text = label,
        onClick = onClick,
        textStyle = headerStyle,
        onRightPressed = onClick,
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

/**
 * Unified button component for the filter bar.
 */
@Composable
private fun FilterBarButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(20.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) {
                TvColors.BluePrimary.copy(alpha = 0.3f)
            } else {
                TvColors.SurfaceElevated.copy(alpha = 0.8f)
            },
            contentColor = if (isSelected) {
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
