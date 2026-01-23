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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.tv.ui.components.TvDropdownMenu
import dev.jausc.myflix.tv.ui.components.TvDropdownMenuItem
import dev.jausc.myflix.tv.ui.components.TvDropdownMenuItemWithCheck
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
        (currentYear downTo currentYear - 20).forEach { year ->
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
 * Icon-based filter bar for Seerr discover screens with dropdown menus.
 * Similar to LibraryFilterBar but for Seerr content.
 *
 * Layout: Back | Title | Spacer | [Filter][Sort]
 */
@Composable
fun SeerrFilterBar(
    title: String,
    filterState: SeerrFilterState,
    genres: List<SeerrGenre>,
    showMediaTypeFilter: Boolean,
    showGenreFilter: Boolean,
    showReleaseStatusFilter: Boolean,
    onBack: () -> Unit,
    onMediaTypeChange: (SeerrMediaTypeOption) -> Unit,
    onReleaseStatusChange: (SeerrReleaseStatusOption) -> Unit,
    onSortChange: (SeerrSortOption) -> Unit,
    onRatingChange: (Float?) -> Unit,
    onYearChange: (Int?, Int?) -> Unit,
    onGenreToggle: (Int) -> Unit,
    onClearGenres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilterDropdown by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Back button
        FilterBarIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = TvColors.TextPrimary,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Filter button with dropdown
        Box {
            FilterBarIconButton(
                icon = Icons.Outlined.FilterAlt,
                contentDescription = "Filter",
                isSelected = filterState.hasActiveFilters,
                onClick = { showFilterDropdown = true },
            )

            SeerrFilterDropdownMenu(
                expanded = showFilterDropdown,
                filterState = filterState,
                genres = genres,
                showMediaTypeFilter = showMediaTypeFilter,
                showGenreFilter = showGenreFilter,
                showReleaseStatusFilter = showReleaseStatusFilter,
                onMediaTypeChange = onMediaTypeChange,
                onReleaseStatusChange = onReleaseStatusChange,
                onRatingChange = onRatingChange,
                onYearChange = onYearChange,
                onGenreToggle = onGenreToggle,
                onClearGenres = onClearGenres,
                onDismiss = { showFilterDropdown = false },
            )
        }

        // Sort button with dropdown
        Box {
            FilterBarIconButton(
                icon = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "Sort",
                isSelected = filterState.sortOption != SeerrSortOption.POPULARITY_DESC,
                onClick = { showSortDropdown = true },
            )

            SeerrSortDropdownMenu(
                expanded = showSortDropdown,
                currentSort = filterState.sortOption,
                onSortChange = onSortChange,
                onDismiss = { showSortDropdown = false },
            )
        }
    }
}

@Composable
private fun SeerrSortDropdownMenu(
    expanded: Boolean,
    currentSort: SeerrSortOption,
    onSortChange: (SeerrSortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val itemTextStyle = MaterialTheme.typography.bodySmall

    TvDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
    ) {
        SeerrSortOption.entries.forEach { option ->
            TvDropdownMenuItemWithCheck(
                text = option.label,
                isSelected = option == currentSort,
                textStyle = itemTextStyle,
                onClick = {
                    onSortChange(option)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun SeerrFilterDropdownMenu(
    expanded: Boolean,
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
) {
    val headerStyle = MaterialTheme.typography.titleSmall
    val itemTextStyle = MaterialTheme.typography.bodySmall
    val submenuOffset = DpOffset(220.dp, 0.dp)

    var showMediaTypeMenu by remember { mutableStateOf(false) }
    var showReleaseStatusMenu by remember { mutableStateOf(false) }
    var showRatingMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showGenreMenu by remember { mutableStateOf(false) }

    val closeAllSubmenus = {
        showMediaTypeMenu = false
        showReleaseStatusMenu = false
        showRatingMenu = false
        showYearMenu = false
        showGenreMenu = false
    }

    TvDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
    ) {
        // Media Type submenu
        if (showMediaTypeFilter) {
            Box {
                SubMenuItem(
                    label = "Media Type",
                    headerStyle = headerStyle,
                    onClick = {
                        closeAllSubmenus()
                        showMediaTypeMenu = true
                    },
                )
                TvDropdownMenu(
                    expanded = showMediaTypeMenu,
                    onDismissRequest = { showMediaTypeMenu = false },
                    offset = submenuOffset,
                ) {
                    SeerrMediaTypeOption.entries.forEach { option ->
                        TvDropdownMenuItemWithCheck(
                            text = option.label,
                            isSelected = option == filterState.mediaType,
                            textStyle = itemTextStyle,
                            onLeftPressed = { showMediaTypeMenu = false },
                            onClick = {
                                onMediaTypeChange(option)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }

        // Release Status submenu
        if (showReleaseStatusFilter) {
            Box {
                SubMenuItem(
                    label = "Release Status",
                    headerStyle = headerStyle,
                    onClick = {
                        closeAllSubmenus()
                        showReleaseStatusMenu = true
                    },
                )
                TvDropdownMenu(
                    expanded = showReleaseStatusMenu,
                    onDismissRequest = { showReleaseStatusMenu = false },
                    offset = submenuOffset,
                ) {
                    SeerrReleaseStatusOption.entries.forEach { option ->
                        TvDropdownMenuItemWithCheck(
                            text = option.label,
                            isSelected = option == filterState.releaseStatus,
                            textStyle = itemTextStyle,
                            onLeftPressed = { showReleaseStatusMenu = false },
                            onClick = {
                                onReleaseStatusChange(option)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }

        // Rating submenu
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
                seerrRatingOptions.forEach { option ->
                    TvDropdownMenuItemWithCheck(
                        text = option.label,
                        isSelected = option.value == filterState.minRating,
                        textStyle = itemTextStyle,
                        onLeftPressed = { showRatingMenu = false },
                        onClick = {
                            onRatingChange(option.value)
                            onDismiss()
                        },
                    )
                }
            }
        }

        // Year submenu
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
                buildYearOptions().forEach { option ->
                    val isSelected = option.from == filterState.yearFrom && option.to == filterState.yearTo
                    TvDropdownMenuItemWithCheck(
                        text = option.label,
                        isSelected = isSelected,
                        textStyle = itemTextStyle,
                        onLeftPressed = { showYearMenu = false },
                        onClick = {
                            onYearChange(option.from, option.to)
                            onDismiss()
                        },
                    )
                }
            }
        }

        // Genres submenu
        if (showGenreFilter && genres.isNotEmpty()) {
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
                    // All Genres option
                    TvDropdownMenuItemWithCheck(
                        text = "All Genres",
                        isSelected = filterState.selectedGenreIds.isEmpty(),
                        textStyle = itemTextStyle,
                        onLeftPressed = { showGenreMenu = false },
                        onClick = {
                            onClearGenres()
                            onDismiss()
                        },
                    )

                    HorizontalDivider(color = TvColors.SurfaceElevated)

                    // Individual genres
                    genres.forEach { genre ->
                        TvDropdownMenuItemWithCheck(
                            text = genre.name,
                            isSelected = filterState.selectedGenreIds.contains(genre.id),
                            textStyle = itemTextStyle,
                            onLeftPressed = { showGenreMenu = false },
                            onClick = {
                                onGenreToggle(genre.id)
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
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

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
