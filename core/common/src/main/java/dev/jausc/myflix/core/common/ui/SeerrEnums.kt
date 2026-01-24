package dev.jausc.myflix.core.common.ui

import dev.jausc.myflix.core.seerr.SeerrMedia

/**
 * Shared Seerr-related enums used across TV and mobile apps.
 */

// ============================================================================
// Request Screen Enums
// ============================================================================

enum class SeerrRequestScope(val label: String) {
    MINE("Mine"),
    ALL("All"),
}

enum class SeerrRequestFilter(val label: String, val filterValue: String) {
    ALL("All", "all"),
    PENDING("Pending", "pending"),
    APPROVED("Approved", "approved"),
    AVAILABLE("Available", "available"),
    DECLINED("Declined", "declined"),
}

enum class SeerrRequestSort(val label: String, val sortValue: String) {
    ADDED("Added", "added"),
    MODIFIED("Modified", "modified"),
}

// ============================================================================
// Setup Screen Enums
// ============================================================================

enum class SeerrAuthMode(val label: String) {
    JELLYFIN("Jellyfin"),
    LOCAL("Local"),
}

// ============================================================================
// Search Screen Enums
// ============================================================================

enum class SeerrSearchFilter(val label: String, val mediaType: String?) {
    ALL("All", null),
    MOVIES("Movies", "movie"),
    TV("TV", "tv"),
    PEOPLE("People", "person"),
    ;

    fun matches(media: SeerrMedia): Boolean = mediaType == null || media.mediaType == mediaType
}
