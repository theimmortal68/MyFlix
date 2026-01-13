package dev.jausc.myflix.core.seerr

/**
 * Shared Seerr utility functions and types used by both TV and mobile apps.
 */

// ============================================================================
// Quota Formatting
// ============================================================================

/**
 * Builds a human-readable quota text string from quota details.
 * Returns null if no limit is set.
 */
fun buildQuotaText(quotaDetails: SeerrQuotaDetails?): String? {
    val limit = quotaDetails?.limit ?: return null
    val days = quotaDetails.days
    val remaining = quotaDetails.remaining
    val used = quotaDetails.used

    val windowText = days?.let { " per $it days" } ?: ""
    return if (remaining != null) {
        "Quota: $remaining/$limit remaining$windowText"
    } else {
        "Quota: $used/$limit used$windowText"
    }
}

// ============================================================================
// Discover Filter Types
// ============================================================================

/**
 * Configuration for which filters to show on a discover screen.
 */
data class DiscoverFilterConfig(
    val showMediaTypeFilter: Boolean = false,
    val showGenreFilter: Boolean = false,
    val showReleaseStatusFilter: Boolean = false,
    val defaultMediaType: MediaTypeFilter = MediaTypeFilter.ALL,
)

/**
 * Filter by media type (movies, TV shows, or all).
 */
enum class MediaTypeFilter(val label: String, val apiValue: String?) {
    ALL("All", null),
    MOVIES("Movies", "movie"),
    TV_SHOWS("TV Shows", "tv"),
}

/**
 * Filter by release status.
 */
enum class ReleaseStatusFilter(val label: String) {
    ALL("All"),
    RELEASED("Released"),
    UPCOMING("Upcoming"),
}

/**
 * Sort options for discover results.
 * Contains both movie and TV API values since they differ.
 */
enum class SortFilter(val label: String, val movieValue: String, val tvValue: String) {
    POPULARITY_DESC("Most Popular", "popularity.desc", "popularity.desc"),
    POPULARITY_ASC("Least Popular", "popularity.asc", "popularity.asc"),
    RATING_DESC("Highest Rated", "vote_average.desc", "vote_average.desc"),
    RATING_ASC("Lowest Rated", "vote_average.asc", "vote_average.asc"),
    RELEASE_DESC("Newest First", "primary_release_date.desc", "first_air_date.desc"),
    RELEASE_ASC("Oldest First", "primary_release_date.asc", "first_air_date.asc"),
    TITLE_ASC("Title A-Z", "title.asc", "name.asc"),
    TITLE_DESC("Title Z-A", "title.desc", "name.desc"),
}

// ============================================================================
// Season Status Colors
// ============================================================================

/**
 * Common color values for season/media status indicators.
 * These are ARGB Long values that can be converted to platform-specific Color types.
 */
object SeerrStatusColors {
    /** Green - Available/downloaded content */
    const val AVAILABLE: Long = 0xFF22C55E

    /** Blue - Partially available content */
    const val PARTIALLY_AVAILABLE: Long = 0xFF60A5FA

    /** Yellow - Pending/processing requests */
    const val REQUESTED: Long = 0xFFFBBF24

    /** Gray - Not requested/default state */
    const val NOT_REQUESTED: Long = 0xFF6B7280

    /**
     * Get the appropriate status color for a media status value.
     * Returns the ARGB Long value for the status.
     */
    fun getColorForStatus(status: Int?): Long = when (status) {
        SeerrMediaStatus.AVAILABLE -> AVAILABLE
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> PARTIALLY_AVAILABLE
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> REQUESTED
        else -> NOT_REQUESTED
    }
}
