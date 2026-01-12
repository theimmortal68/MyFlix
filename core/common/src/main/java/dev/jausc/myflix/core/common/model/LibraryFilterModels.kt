package dev.jausc.myflix.core.common.model

/**
 * Sort options for library browsing with Jellyfin API mappings.
 */
enum class LibrarySortOption(val jellyfinValue: String, val label: String) {
    TITLE("SortName", "Title"),
    DATE_ADDED("DateCreated", "Date Added"),
    RELEASE_DATE("PremiereDate", "Release Date"),
    RATING("CommunityRating", "Rating"),
    RUNTIME("Runtime", "Runtime"),
    RANDOM("Random", "Random"),
    ;

    companion object {
        fun fromJellyfinValue(value: String): LibrarySortOption =
            entries.find { it.jellyfinValue == value } ?: TITLE
    }
}

/**
 * Sort order direction.
 */
enum class SortOrder(val jellyfinValue: String, val label: String) {
    ASCENDING("Ascending", "A-Z / Oldest"),
    DESCENDING("Descending", "Z-A / Newest"),
    ;

    companion object {
        fun fromJellyfinValue(value: String): SortOrder =
            entries.find { it.jellyfinValue == value } ?: ASCENDING
    }
}

/**
 * Library view mode - grid of posters or list with details.
 */
enum class LibraryViewMode {
    GRID,
    LIST,
    ;

    companion object {
        fun fromString(value: String): LibraryViewMode =
            entries.find { it.name == value } ?: GRID
    }
}

/**
 * Filter for watched/unwatched status.
 */
enum class WatchedFilter(val label: String) {
    ALL("All"),
    UNWATCHED("Unwatched"),
    WATCHED("Watched"),
    ;

    companion object {
        fun fromString(value: String): WatchedFilter =
            entries.find { it.name == value } ?: ALL
    }
}

/**
 * Year range filter.
 */
data class YearRange(
    val from: Int? = null,
    val to: Int? = null,
) {
    /**
     * Convert to Jellyfin years parameter (comma-separated).
     * If both from and to are set, generates all years in range.
     */
    fun toJellyfinParam(): String? {
        if (from == null && to == null) return null
        val start = from ?: MIN_YEAR
        val end = to ?: java.time.Year.now().value
        return (start..end).joinToString(",")
    }

    val isActive: Boolean
        get() = from != null || to != null

    companion object {
        const val MIN_YEAR = 1900
        val currentYear: Int
            get() = java.time.Year.now().value
    }
}

/**
 * Complete filter state for a library screen.
 */
data class LibraryFilterState(
    val sortBy: LibrarySortOption = LibrarySortOption.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val selectedGenres: Set<String> = emptySet(),
    val watchedFilter: WatchedFilter = WatchedFilter.ALL,
    val yearRange: YearRange = YearRange(),
    val ratingFilter: Float? = null,
) {
    /**
     * Check if any filters are active (beyond default sort).
     */
    val hasActiveFilters: Boolean
        get() = selectedGenres.isNotEmpty() ||
            watchedFilter != WatchedFilter.ALL ||
            yearRange.isActive ||
            ratingFilter != null

    /**
     * Count of active filters for badge display.
     */
    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedGenres.isNotEmpty()) count++
            if (watchedFilter != WatchedFilter.ALL) count++
            if (yearRange.isActive) count++
            if (ratingFilter != null) count++
            return count
        }

    /**
     * Get sort display text (e.g., "Title A-Z").
     */
    val sortDisplayText: String
        get() {
            val orderSuffix = when (sortBy) {
                LibrarySortOption.TITLE -> if (sortOrder == SortOrder.ASCENDING) "A-Z" else "Z-A"
                LibrarySortOption.RATING -> if (sortOrder == SortOrder.DESCENDING) "High-Low" else "Low-High"
                LibrarySortOption.RUNTIME -> if (sortOrder == SortOrder.DESCENDING) "Long-Short" else "Short-Long"
                LibrarySortOption.RANDOM -> ""
                else -> if (sortOrder == SortOrder.DESCENDING) "Newest" else "Oldest"
            }
            return if (orderSuffix.isEmpty()) sortBy.label else "${sortBy.label} $orderSuffix"
        }

    companion object {
        val DEFAULT = LibraryFilterState()
    }
}
