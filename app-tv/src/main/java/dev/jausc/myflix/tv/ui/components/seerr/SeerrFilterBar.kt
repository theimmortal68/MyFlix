@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.seerr

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
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
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.tv.ui.components.MenuAnchor
import dev.jausc.myflix.tv.ui.components.MenuAnchorAlignment
import dev.jausc.myflix.tv.ui.components.MenuAnchorPlacement
import dev.jausc.myflix.tv.ui.components.PlayerSlideOutMenu
import dev.jausc.myflix.tv.ui.components.PlayerSlideOutMenuSectioned
import dev.jausc.myflix.tv.ui.components.SlideOutMenuItem
import dev.jausc.myflix.tv.ui.components.SlideOutMenuSection
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Sort options for Seerr discover screens.
 */
enum class SeerrSortOption(val label: String, val movieValue: String, val tvValue: String) {
    POPULARITY_DESC("Most Popular", "popularity.desc", "popularity.desc"),
    POPULARITY_ASC("Least Popular", "popularity.asc", "popularity.asc"),
    RATING_DESC("Highest Rated", "vote_average.desc", "vote_average.desc"),
    RATING_ASC("Lowest Rated", "vote_average.asc", "vote_average.asc"),
    RELEASE_DESC("Newest First", "primary_release_date.desc", "first_air_date.desc"),
    RELEASE_ASC("Oldest First", "primary_release_date.asc", "first_air_date.asc"),
    TITLE_ASC("Title A-Z", "title.asc", "name.asc"),
    TITLE_DESC("Title Z-A", "title.desc", "name.desc"),
}

/**
 * Media type filter options.
 */
enum class SeerrMediaTypeOption(val label: String, val apiValue: String?) {
    ALL("All", null),
    MOVIES("Movies", "movie"),
    TV_SHOWS("TV Shows", "tv"),
}

/**
 * Release status filter options.
 */
enum class SeerrReleaseStatusOption(val label: String) {
    ALL("All"),
    RELEASED("Released"),
    UPCOMING("Upcoming"),
}

/**
 * Rating filter options.
 */
data class SeerrRatingOption(val value: Float?, val label: String)

val seerrRatingOptions = listOf(
    SeerrRatingOption(null, "Any Rating"),
    SeerrRatingOption(5f, "5+ Rating"),
    SeerrRatingOption(6f, "6+ Rating"),
    SeerrRatingOption(7f, "7+ Rating"),
    SeerrRatingOption(8f, "8+ Rating"),
    SeerrRatingOption(9f, "9+ Rating"),
)

/**
 * Year range options.
 */
data class SeerrYearOption(val from: Int?, val to: Int?, val label: String)

fun buildYearOptions(): List<SeerrYearOption> {
    val currentYear = java.time.Year.now().value
    return buildList {
        add(SeerrYearOption(null, null, "Any Year"))
        add(SeerrYearOption(currentYear - 4, currentYear, "Last 5 Years"))
        add(SeerrYearOption(currentYear - 9, currentYear, "Last 10 Years"))
        // Add individual years
        for (year in currentYear downTo currentYear - 20) {
            add(SeerrYearOption(year, year, year.toString()))
        }
    }
}

/**
 * State holder for Seerr filter bar.
 */
data class SeerrFilterState(
    val mediaType: SeerrMediaTypeOption = SeerrMediaTypeOption.ALL,
    val releaseStatus: SeerrReleaseStatusOption = SeerrReleaseStatusOption.ALL,
    val sortOption: SeerrSortOption = SeerrSortOption.POPULARITY_DESC,
    val minRating: Float? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val selectedGenreIds: Set<Int> = emptySet(),
) {
    val hasActiveFilters: Boolean
        get() = mediaType != SeerrMediaTypeOption.ALL ||
            releaseStatus != SeerrReleaseStatusOption.ALL ||
            minRating != null ||
            yearFrom != null ||
            yearTo != null ||
            selectedGenreIds.isNotEmpty()
}

/**
 * Icon-based filter bar for Seerr discover screens with slide-out menus.
 * Uses anchor-based positioning like LibraryFilterBar.
 *
 * Layout: Back | Title | Spacer | [Filter][Sort]
 */
@Composable
fun SeerrFilterBar(
    title: String,
    filterState: SeerrFilterState,
    onBack: () -> Unit,
    onFilterMenuRequested: () -> Unit,
    onSortMenuRequested: () -> Unit,
    onFilterAnchorChanged: (MenuAnchor) -> Unit,
    onSortAnchorChanged: (MenuAnchor) -> Unit,
    modifier: Modifier = Modifier,
    onUpNavigation: () -> Unit = {},
    gridFocusRequester: FocusRequester? = null,
) {
    val backFocusRequester = remember { FocusRequester() }
    val filterFocusRequester = remember { FocusRequester() }
    val sortFocusRequester = remember { FocusRequester() }

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
        // Back button
        FilterBarIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier
                .focusRequester(backFocusRequester)
                .focusProperties {
                    left = FocusRequester.Cancel
                    right = filterFocusRequester
                    down = gridFocusRequester ?: FocusRequester.Default
                },
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = TvColors.TextPrimary,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Filter button with anchor tracking
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
            FilterBarIconButton(
                icon = Icons.Outlined.FilterAlt,
                contentDescription = "Filter",
                isSelected = filterState.hasActiveFilters,
                onClick = onFilterMenuRequested,
                modifier = Modifier
                    .focusRequester(filterFocusRequester)
                    .focusProperties {
                        left = backFocusRequester
                        right = sortFocusRequester
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
            )
        }

        // Sort button with anchor tracking
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
            FilterBarIconButton(
                icon = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "Sort",
                isSelected = filterState.sortOption != SeerrSortOption.POPULARITY_DESC,
                onClick = onSortMenuRequested,
                modifier = Modifier
                    .focusRequester(sortFocusRequester)
                    .focusProperties {
                        left = filterFocusRequester
                        right = FocusRequester.Cancel
                        down = gridFocusRequester ?: FocusRequester.Default
                    },
            )
        }
    }
}

/**
 * Sort slide-out menu for Seerr screens.
 */
@Composable
fun SeerrSortMenu(
    visible: Boolean,
    currentSort: SeerrSortOption,
    onSortChange: (SeerrSortOption) -> Unit,
    onDismiss: () -> Unit,
    anchor: MenuAnchor?,
) {
    PlayerSlideOutMenu(
        visible = visible,
        title = "Sort",
        items = SeerrSortOption.entries.map { option ->
            SlideOutMenuItem(
                text = option.label,
                selected = option == currentSort,
                onClick = { onSortChange(option) },
            )
        },
        onDismiss = onDismiss,
        anchor = anchor,
    )
}

/**
 * Filter slide-out menu for Seerr screens.
 */
@Composable
fun SeerrFilterMenu(
    visible: Boolean,
    filterState: SeerrFilterState,
    genres: List<SeerrGenre>,
    showMediaTypeFilter: Boolean,
    showGenreFilter: Boolean,
    showReleaseStatusFilter: Boolean,
    onMediaTypeChange: (SeerrMediaTypeOption) -> Unit,
    onReleaseStatusChange: (SeerrReleaseStatusOption) -> Unit,
    onRatingChange: (Float?) -> Unit,
    onYearChange: (Int?, Int?) -> Unit,
    onGenreToggle: (Int) -> Unit,
    onClearGenres: () -> Unit,
    onDismiss: () -> Unit,
    anchor: MenuAnchor?,
) {
    var activeSubmenu by remember { mutableStateOf<SeerrFilterSubmenu?>(null) }
    val submenuAnchors = remember { mutableStateMapOf<SeerrFilterSubmenu, MenuAnchor>() }
    val mainMenuFocusRequester = remember { FocusRequester() }
    val submenuFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (!visible) {
            activeSubmenu = null
        }
    }

    val yearOptions = remember { buildYearOptions() }
    val sortedGenres = remember(genres) { genres.sortedBy { it.name } }

    val submenuEntries = remember(showMediaTypeFilter, showReleaseStatusFilter, showGenreFilter, genres) {
        buildList {
            if (showMediaTypeFilter) {
                add(SeerrFilterSubmenuEntry(SeerrFilterSubmenu.MediaType, "Media Type"))
            }
            if (showReleaseStatusFilter) {
                add(SeerrFilterSubmenuEntry(SeerrFilterSubmenu.ReleaseStatus, "Release Status"))
            }
            add(SeerrFilterSubmenuEntry(SeerrFilterSubmenu.Rating, "Minimum Rating"))
            add(SeerrFilterSubmenuEntry(SeerrFilterSubmenu.Year, "Year"))
            if (showGenreFilter && genres.isNotEmpty()) {
                add(SeerrFilterSubmenuEntry(SeerrFilterSubmenu.Genres, "Genres"))
            }
        }
    }

    // Main filter menu with submenu navigation
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

    // Render active submenu
    val submenuAnchor = activeSubmenu?.let { submenuAnchors[it] } ?: anchor
    when (activeSubmenu) {
        SeerrFilterSubmenu.MediaType -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Media Type",
                items = SeerrMediaTypeOption.entries.map { option ->
                    SlideOutMenuItem(
                        text = option.label,
                        selected = option == filterState.mediaType,
                        onClick = { onMediaTypeChange(option) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        SeerrFilterSubmenu.ReleaseStatus -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Release Status",
                items = SeerrReleaseStatusOption.entries.map { option ->
                    SlideOutMenuItem(
                        text = option.label,
                        selected = option == filterState.releaseStatus,
                        onClick = { onReleaseStatusChange(option) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        SeerrFilterSubmenu.Rating -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Minimum Rating",
                items = seerrRatingOptions.map { option ->
                    SlideOutMenuItem(
                        text = option.label,
                        selected = option.value == filterState.minRating,
                        onClick = { onRatingChange(option.value) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        SeerrFilterSubmenu.Year -> {
            PlayerSlideOutMenu(
                visible = true,
                title = "Year",
                items = yearOptions.map { option ->
                    val isSelected = option.from == filterState.yearFrom && option.to == filterState.yearTo
                    SlideOutMenuItem(
                        text = option.label,
                        selected = isSelected,
                        onClick = { onYearChange(option.from, option.to) },
                    )
                },
                onDismiss = { activeSubmenu = null },
                anchor = submenuAnchor,
                firstItemFocusRequester = submenuFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
            )
        }
        SeerrFilterSubmenu.Genres -> {
            val genreItems = if (sortedGenres.isEmpty()) {
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
                        selected = filterState.selectedGenreIds.isEmpty(),
                        onClick = { onClearGenres() },
                    ),
                ) + sortedGenres.map { genre ->
                    SlideOutMenuItem(
                        text = genre.name,
                        selected = filterState.selectedGenreIds.contains(genre.id),
                        onClick = { onGenreToggle(genre.id) },
                    )
                }
            }
            PlayerSlideOutMenu(
                visible = true,
                title = "Genres",
                items = genreItems,
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

private enum class SeerrFilterSubmenu {
    MediaType,
    ReleaseStatus,
    Rating,
    Year,
    Genres,
}

private data class SeerrFilterSubmenuEntry(
    val submenu: SeerrFilterSubmenu,
    val label: String,
)

@Composable
private fun FilterBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
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
