package dev.jausc.myflix.core.seerr

import kotlinx.serialization.Serializable

/**
 * Seerr API data models.
 * Based on Seerr/Jellyseerr API specification.
 */

// ============================================================================
// Media Types
// ============================================================================

/**
 * Media item from Seerr (movie or TV show).
 */
@Serializable
data class SeerrMedia(
    val id: Int,
    val mediaType: String = "", // "movie" or "tv" - may be empty from detail endpoints
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val imdbId: String? = null,
    val status: String? = null, // Production status: "Released", "In Production", "Ended", etc.
    val title: String? = null,
    val name: String? = null, // TV shows use 'name' instead of 'title'
    val originalTitle: String? = null,
    val originalName: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val overview: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null, // For TV shows
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val popularity: Double? = null,
    val originalLanguage: String? = null,
    val genreIds: List<Int>? = null,
    val genres: List<SeerrGenre>? = null,
    val runtime: Int? = null,
    val numberOfSeasons: Int? = null,
    val numberOfEpisodes: Int? = null,
    val mediaInfo: SeerrMediaInfo? = null,
    val credits: SeerrCredits? = null,
    val externalIds: SeerrExternalIds? = null,
    val keywords: List<SeerrKeyword>? = null,
    val relatedVideos: List<SeerrVideo>? = null,
) {
    /** Display title (handles movie vs TV naming) */
    val displayTitle: String
        get() = title ?: name ?: originalTitle ?: originalName ?: "Unknown"

    /** Display release date (handles movie vs TV) */
    val displayReleaseDate: String?
        get() = releaseDate ?: firstAirDate

    /** Year extracted from release date */
    val year: Int?
        get() = displayReleaseDate?.take(4)?.toIntOrNull()

    /** Availability status from mediaInfo (1=unknown, 2=pending, 3=processing, 4=partial, 5=available) */
    val availabilityStatus: Int?
        get() = mediaInfo?.status

    /** Whether this media is available in the library */
    val isAvailable: Boolean
        get() = mediaInfo?.status == 5

    /** Whether this media has a pending request */
    val isPending: Boolean
        get() = mediaInfo?.status == 2 || mediaInfo?.status == 3

    /** Whether this is a movie (derives from mediaType or field presence) */
    val isMovie: Boolean
        get() = mediaType == "movie" || (mediaType.isEmpty() && title != null && name == null)

    /** Whether this is a TV show (derives from mediaType or field presence) */
    val isTvShow: Boolean
        get() = mediaType == "tv" || (mediaType.isEmpty() && name != null)
}

/**
 * Media info containing request status and details.
 */
@Serializable
data class SeerrMediaInfo(
    val id: Int? = null,
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val status: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val requests: List<SeerrRequest>? = null,
    val seasons: List<SeerrSeasonStatus>? = null,
)

/**
 * Season availability status.
 */
@Serializable
data class SeerrSeasonStatus(
    val id: Int,
    val seasonNumber: Int,
    val status: Int, // Same status codes as media
)

// ============================================================================
// Request Types
// ============================================================================

/**
 * Media request.
 */
@Serializable
data class SeerrRequest(
    val id: Int,
    val status: Int, // 1=pending_approval, 2=approved, 3=declined
    val media: SeerrRequestMedia? = null,
    val requestedBy: SeerrUser? = null,
    val modifiedBy: SeerrUser? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val seasons: List<SeerrSeasonRequest>? = null,
    val is4k: Boolean = false,
    val serverId: Int? = null,
    val profileId: Int? = null,
) {
    /** Whether the request is pending approval */
    val isPendingApproval: Boolean
        get() = status == 1

    /** Whether the request is approved */
    val isApproved: Boolean
        get() = status == 2

    /** Whether the request is declined */
    val isDeclined: Boolean
        get() = status == 3

    /** Human-readable status text */
    val statusText: String
        get() = when (status) {
            1 -> "Pending"
            2 -> "Approved"
            3 -> "Declined"
            else -> "Unknown"
        }
}

/**
 * Simplified media info in request context.
 */
@Serializable
data class SeerrRequestMedia(
    val id: Int,
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val status: Int? = null,
    val mediaType: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * Season request details.
 */
@Serializable
data class SeerrSeasonRequest(
    val id: Int,
    val seasonNumber: Int,
    val status: Int, // Same as request status
)

/**
 * Request body for creating a new media request.
 */
@Serializable
data class CreateMediaRequest(
    val mediaType: String,
    val mediaId: Int,
    val tvdbId: Int? = null,
    val seasons: List<Int>? = null, // For TV shows - which seasons to request
    val is4k: Boolean = false,
    val serverId: Int? = null,
    val profileId: Int? = null,
)

// ============================================================================
// User Types
// ============================================================================

/**
 * Seerr user.
 */
@Serializable
data class SeerrUser(
    val id: Int,
    val email: String? = null,
    val plexUsername: String? = null,
    val jellyfinUsername: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatar: String? = null,
    val requestCount: Int? = null,
    val movieQuotaLimit: Int? = null,
    val movieQuotaDays: Int? = null,
    val tvQuotaLimit: Int? = null,
    val tvQuotaDays: Int? = null,
    val permissions: Int? = null,
    val userType: Int? = null, // 1=plex, 2=local, 3=jellyfin
    val apiKey: String? = null, // API key for persistent authentication
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    /** Display name for UI */
    val name: String
        get() = displayName ?: username ?: jellyfinUsername ?: plexUsername ?: email ?: "User"

    /** Whether this is a Jellyfin user */
    val isJellyfinUser: Boolean
        get() = userType == 3

    /** Remaining movie quota (null if unlimited) */
    val movieQuotaRemaining: Int?
        get() = if (movieQuotaLimit != null && requestCount != null) {
            (movieQuotaLimit - requestCount).coerceAtLeast(0)
        } else {
            null
        }

    /** Remaining TV quota (null if unlimited) */
    val tvQuotaRemaining: Int?
        get() = if (tvQuotaLimit != null && requestCount != null) {
            (tvQuotaLimit - requestCount).coerceAtLeast(0)
        } else {
            null
        }
}

/**
 * User quota info response.
 */
@Serializable
data class SeerrUserQuota(
    val movie: SeerrQuotaDetails? = null,
    val tv: SeerrQuotaDetails? = null,
)

@Serializable
data class SeerrQuotaDetails(
    val limit: Int? = null,
    val days: Int? = null,
    val used: Int = 0,
    val remaining: Int? = null,
)

// ============================================================================
// Discovery/Search Types
// ============================================================================

/**
 * Paginated discover/search result.
 */
@Serializable
data class SeerrDiscoverResult(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<SeerrMedia>,
)

/**
 * Paginated request list result.
 */
@Serializable
data class SeerrRequestResult(
    val pageInfo: SeerrPageInfo,
    val results: List<SeerrRequest>,
)

@Serializable
data class SeerrPageInfo(
    val pages: Int,
    val pageSize: Int,
    val results: Int,
    val page: Int,
)

/**
 * Genre.
 */
@Serializable
data class SeerrGenre(
    val id: Int,
    val name: String,
)

/**
 * Keyword.
 */
@Serializable
data class SeerrKeyword(
    val id: Int,
    val name: String,
)

// ============================================================================
// TV Show Season/Episode Types
// ============================================================================

/**
 * TV season details.
 */
@Serializable
data class SeerrSeason(
    val id: Int,
    val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val airDate: String? = null,
    val episodeCount: Int? = null,
    val episodes: List<SeerrEpisode>? = null,
)

/**
 * TV episode details.
 */
@Serializable
data class SeerrEpisode(
    val id: Int,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val stillPath: String? = null,
    val airDate: String? = null,
    val runtime: Int? = null,
    val voteAverage: Double? = null,
)

// ============================================================================
// Credits Types
// ============================================================================

/**
 * Cast and crew credits.
 */
@Serializable
data class SeerrCredits(
    val cast: List<SeerrCastMember>? = null,
    val crew: List<SeerrCrewMember>? = null,
)

@Serializable
data class SeerrCastMember(
    val id: Int,
    val name: String,
    val character: String? = null,
    val profilePath: String? = null,
    val order: Int? = null,
)

@Serializable
data class SeerrCrewMember(
    val id: Int,
    val name: String,
    val job: String? = null,
    val department: String? = null,
    val profilePath: String? = null,
)

// ============================================================================
// External IDs and Videos
// ============================================================================

@Serializable
data class SeerrExternalIds(
    val imdbId: String? = null,
    val facebookId: String? = null,
    val instagramId: String? = null,
    val twitterId: String? = null,
)

@Serializable
data class SeerrVideo(
    val id: String? = null,
    val key: String? = null,
    val name: String? = null,
    val site: String? = null, // Usually "YouTube"
    val type: String? = null, // "Trailer", "Teaser", etc.
    val size: Int? = null,
)

// ============================================================================
// Server/Status Types
// ============================================================================

/**
 * Seerr server status.
 */
@Serializable
data class SeerrStatus(
    val version: String,
    val commitTag: String? = null,
    val updateAvailable: Boolean = false,
    val commitsBehind: Int = 0,
)

/**
 * Seerr settings (partial - for detection/validation).
 */
@Serializable
data class SeerrSettings(
    val initialized: Boolean = false,
    val applicationTitle: String? = null,
    val applicationUrl: String? = null,
    val localLogin: Boolean = true,
    val defaultPermissions: Int? = null,
)

// ============================================================================
// Authentication Types
// ============================================================================

/**
 * Jellyfin auth request body.
 */
@Serializable
data class SeerrJellyfinAuthRequest(
    val username: String,
    val password: String,
    val hostname: String? = null,
)

/**
 * Local auth request body.
 */
@Serializable
data class SeerrLocalAuthRequest(
    val email: String,
    val password: String,
)

/**
 * Quick Connect state from Seerr server.
 * Returned by initiate and authorize endpoints.
 */
@Serializable
data class SeerrQuickConnectState(
    val code: String,
    val secret: String,
    val authenticated: Boolean = false,
)

/**
 * Quick Connect flow state for UI updates.
 * Follows the same pattern as Jellyfin's QuickConnectFlowState.
 */
sealed class SeerrQuickConnectFlowState {
    /** Initializing Quick Connect */
    data object Initializing : SeerrQuickConnectFlowState()

    /** Quick Connect not available on this server (requires Seerr with PR #2212) */
    data object NotAvailable : SeerrQuickConnectFlowState()

    /** Waiting for user to approve the code on Jellyfin server */
    data class WaitingForApproval(val code: String, val secret: String) : SeerrQuickConnectFlowState()

    /** Code approved, completing authentication */
    data object Authenticating : SeerrQuickConnectFlowState()

    /** Successfully authenticated */
    data class Authenticated(val user: SeerrUser) : SeerrQuickConnectFlowState()

    /** Error during Quick Connect */
    data class Error(val message: String) : SeerrQuickConnectFlowState()
}

// ============================================================================
// Media Status Constants
// ============================================================================

object SeerrMediaStatus {
    const val UNKNOWN = 1
    const val PENDING = 2
    const val PROCESSING = 3
    const val PARTIALLY_AVAILABLE = 4
    const val AVAILABLE = 5

    fun toDisplayString(status: Int?): String = when (status) {
        UNKNOWN -> "Not Requested"
        PENDING -> "Pending"
        PROCESSING -> "Processing"
        PARTIALLY_AVAILABLE -> "Partially Available"
        AVAILABLE -> "Available"
        else -> "Unknown"
    }
}

object SeerrRequestStatus {
    const val PENDING_APPROVAL = 1
    const val APPROVED = 2
    const val DECLINED = 3

    fun toDisplayString(status: Int): String = when (status) {
        PENDING_APPROVAL -> "Pending Approval"
        APPROVED -> "Approved"
        DECLINED -> "Declined"
        else -> "Unknown"
    }
}
