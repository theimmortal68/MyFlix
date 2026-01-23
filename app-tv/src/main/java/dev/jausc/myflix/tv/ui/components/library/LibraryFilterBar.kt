@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.model.SeriesStatusFilter
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange
import dev.jausc.myflix.tv.ui.components.MenuAnchor
import dev.jausc.myflix.tv.ui.components.MenuAnchorAlignment
import dev.jausc.myflix.tv.ui.components.MenuAnchorPlacement
import dev.jausc.myflix.tv.ui.components.PlayerSlideOutMenu
import dev.jausc.myflix.tv.ui.components.PlayerSlideOutMenuSectioned
import dev.jausc.myflix.tv.ui.components.SlideOutMenuItem
import dev.jausc.myflix.tv.ui.components.SlideOutMenuSection
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Icon-based filter bar for library screens with slide-out menus.
 *
 * Layout: Title | ItemCount | Spacer | [Poster][Thumbnail] | [Filter][Sort][Shuffle]
 */
@Composable
fun LibraryFilterBar(
    libraryName: String,
    totalItems: Int,
    loadedItems: Int,
    filterState: LibraryFilterState,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onShuffleClick: () -> Unit,
    onFilterMenuRequested: () -> Unit,
    onSortMenuRequested: () -> Unit,
    onFilterAnchorChanged: (MenuAnchor) -> Unit,
    onSortAnchorChanged: (MenuAnchor) -> Unit,
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

    val density = LocalDensity.current

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
            style = MaterialTheme.typography.headlineLarge,
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
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                with(density) {
                    onFilterAnchorChanged(
                        MenuAnchor(
                            x = (position.x + size.width).toDp(),
                            y = (position.y + size.height).toDp(),
                            alignment = MenuAnchorAlignment.BottomEnd,
                            placement = MenuAnchorPlacement.Below,
                        ),
                    )
                }
            },
        ) {
            FilterBarButton(
                icon = Icons.Outlined.FilterAlt,
                contentDescription = "Filter",
                isSelected = filterState.hasActiveFilters,
                onClick = onFilterMenuRequested,
                modifier = Modifier
                    .focusRequester(filterFocusRequester)
                    .focusProperties {
                        left = thumbnailFocusRequester
                        right = sortFocusRequester
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
            )
        }

        // Sort button with dropdown
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                with(density) {
                    onSortAnchorChanged(
                        MenuAnchor(
                            x = (position.x + size.width).toDp(),
                            y = (position.y + size.height).toDp(),
                            alignment = MenuAnchorAlignment.BottomEnd,
                            placement = MenuAnchorPlacement.Below,
                        ),
                    )
                }
            },
        ) {
            FilterBarButton(
                icon = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "Sort",
                isSelected = false,
                onClick = onSortMenuRequested,
                modifier = Modifier
                    .focusRequester(sortFocusRequester)
                    .focusProperties {
                        left = filterFocusRequester
                        right = shuffleFocusRequester
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
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
internal fun LibrarySortMenu(
    visible: Boolean,
    currentSortBy: LibrarySortOption,
    currentSortOrder: SortOrder,
    onSortChange: (LibrarySortOption, SortOrder) -> Unit,
    onDismiss: () -> Unit,
    anchor: MenuAnchor?,
) {
    PlayerSlideOutMenuSectioned(
        visible = visible,
        title = "Sort",
        sections = listOf(
            SlideOutMenuSection(
                title = "Sort By",
                items = LibrarySortOption.entries.map { option ->
                    SlideOutMenuItem(
                        text = option.label,
                        selected = option == currentSortBy,
                        onClick = {
                            onSortChange(option, currentSortOrder)
                        },
                    )
                },
            ),
            SlideOutMenuSection(
                title = "Order",
                items = SortOrder.entries.map { order ->
                    SlideOutMenuItem(
                        text = order.label,
                        selected = order == currentSortOrder,
                        onClick = {
                            onSortChange(currentSortBy, order)
                        },
                    )
                },
            ),
        ),
        onDismiss = onDismiss,
        anchor = anchor,
    )
}

/**
 * Filter dropdown menu anchored to button.
 */
@Composable
internal fun LibraryFilterMenu(
    visible: Boolean,
    currentWatchedFilter: WatchedFilter,
    currentRatingFilter: Float?,
    currentYearRange: YearRange,
    currentSeriesStatus: SeriesStatusFilter,
    availableGenres: List<String>,
    selectedGenres: Set<String>,
    availableParentalRatings: List<String>,
    selectedParentalRatings: Set<String>,
    showSeriesStatusFilter: Boolean,
    onGenreToggle: (String) -> Unit,
    onClearGenres: () -> Unit,
    onParentalRatingToggle: (String) -> Unit,
    onClearParentalRatings: () -> Unit,
    onFilterChange: (WatchedFilter, Float?) -> Unit,
    onYearRangeChange: (YearRange) -> Unit,
    onSeriesStatusChange: (SeriesStatusFilter) -> Unit,
    onDismiss: () -> Unit,
    anchor: MenuAnchor?,
) {
    var activeSubmenu by remember { mutableStateOf<LibraryFilterSubmenu?>(null) }
    val submenuAnchors = remember { mutableStateMapOf<LibraryFilterSubmenu, MenuAnchor>() }
    val mainMenuFocusRequester = remember { FocusRequester() }
    val submenuFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (!visible) {
            activeSubmenu = null
        }
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
    val sortedGenres = availableGenres.sorted()
    val sortedRatings = availableParentalRatings.sorted()
    val submenuEntries = buildList {
        add(LibraryFilterSubmenuEntry(LibraryFilterSubmenu.Watched, "Watched Status"))
        add(LibraryFilterSubmenuEntry(LibraryFilterSubmenu.Rating, "Minimum Rating"))
        add(LibraryFilterSubmenuEntry(LibraryFilterSubmenu.Year, "Year"))
        add(LibraryFilterSubmenuEntry(LibraryFilterSubmenu.Genres, "Genres"))
        add(LibraryFilterSubmenuEntry(LibraryFilterSubmenu.Parental, "Parental Rating"))
        if (showSeriesStatusFilter) {
            add(LibraryFilterSubmenuEntry(LibraryFilterSubmenu.SeriesStatus, "Series Status"))
        }
    }

    PlayerSlideOutMenuSectioned(
        visible = visible,
        title = "Filter",
        sections = listOf(
            SlideOutMenuSection(
                title = "Filters",
                items = submenuEntries.map { entry ->
                    SlideOutMenuItem(
                        text = entry.label,
                        dismissOnClick = false,
                        onClick = { activeSubmenu = entry.submenu },
                    )
                },
            ),
        ),
        onDismiss = onDismiss,
        anchor = anchor,
        onItemAnchorChanged = { item, itemAnchor ->
            val entry = submenuEntries.firstOrNull { it.label == item.text }
            if (entry != null) {
                submenuAnchors[entry.submenu] = itemAnchor
            }
        },
        firstItemFocusRequester = mainMenuFocusRequester,
        rightFocusRequester = activeSubmenu?.let { submenuFocusRequester },
    )

    val submenuAnchor = activeSubmenu?.let { submenuAnchors[it] } ?: anchor
    when (activeSubmenu) {
        LibraryFilterSubmenu.Watched -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Watched Status",
                items = WatchedFilter.entries.map { filter ->
                    SlideOutMenuItem(
                        text = filter.label,
                        selected = filter == currentWatchedFilter,
                        onClick = { onFilterChange(filter, currentRatingFilter) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        LibraryFilterSubmenu.Rating -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Minimum Rating",
                items = ratingOptions.map { (rating, label) ->
                    SlideOutMenuItem(
                        text = label,
                        selected = currentRatingFilter == rating,
                        onClick = { onFilterChange(currentWatchedFilter, rating) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        LibraryFilterSubmenu.Year -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Year",
                items = yearOptions.map { (range, label) ->
                    SlideOutMenuItem(
                        text = label,
                        selected = range == currentYearRange,
                        onClick = { onYearRangeChange(range) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        LibraryFilterSubmenu.Genres -> {
            val items = if (sortedGenres.isEmpty()) {
                listOf(
                    SlideOutMenuItem(
                        text = "No genres available",
                        enabled = false,
                        onClick = {},
                    ),
                )
            } else {
                listOf(
                    SlideOutMenuItem(
                        text = "All Genres",
                        selected = selectedGenres.isEmpty(),
                        onClick = { onClearGenres() },
                    ),
                ) + sortedGenres.map { genre ->
                    SlideOutMenuItem(
                        text = genre,
                        selected = selectedGenres.contains(genre),
                        onClick = { onGenreToggle(genre) },
                    )
                }
            }
            PlayerSlideOutMenu(
                visible = true,
                title = "Genres",
                items = items,
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        LibraryFilterSubmenu.Parental -> {
            val items = if (sortedRatings.isEmpty()) {
                listOf(
                    SlideOutMenuItem(
                        text = "No ratings available",
                        enabled = false,
                        onClick = {},
                    ),
                )
            } else {
                listOf(
                    SlideOutMenuItem(
                        text = "All Ratings",
                        selected = selectedParentalRatings.isEmpty(),
                        onClick = { onClearParentalRatings() },
                    ),
                ) + sortedRatings.map { rating ->
                    SlideOutMenuItem(
                        text = rating,
                        selected = selectedParentalRatings.contains(rating),
                        onClick = { onParentalRatingToggle(rating) },
                    )
                }
            }
            PlayerSlideOutMenu(
                visible = true,
                title = "Parental Rating",
                items = items,
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        LibraryFilterSubmenu.SeriesStatus -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Series Status",
                items = SeriesStatusFilter.entries.map { status ->
                    SlideOutMenuItem(
                        text = status.label,
                        selected = status == currentSeriesStatus,
                        onClick = { onSeriesStatusChange(status) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        null -> {
            // No submenu shown
        }
    }
}

private enum class LibraryFilterSubmenu {
    Watched,
    Rating,
    Year,
    Genres,
    Parental,
    SeriesStatus,
}

private data class LibraryFilterSubmenuEntry(
    val submenu: LibraryFilterSubmenu,
    val label: String,
)

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
        modifier = modifier.height(24.dp),
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
