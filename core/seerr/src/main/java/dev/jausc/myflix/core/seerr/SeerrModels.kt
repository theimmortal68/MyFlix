package dev.jausc.myflix.core.seerr

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Seerr API data models.
 * Based on Seerr/Jellyseerr OpenAPI specification.
 *
 * All field names use @SerialName matching the JSON field names from the Seerr API.
 */

// ============================================================================
// Media Types - Discovery and Browse Results
// ============================================================================

/**
 * Media item from Seerr (movie or TV show).
 * Used in discover, search, and browse results.
 */
@Serializable
data class SeerrMedia(
    @SerialName("id") val id: Int,
    @SerialName("mediaType") val mediaType: String = "",
    @SerialName("tmdbId") val tmdbId: Int? = null,
    @SerialName("tvdbId") val tvdbId: Int? = null,
    @SerialName("imdbId") val imdbId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("originalTitle") val originalTitle: String? = null,
    @SerialName("originalName") val originalName: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("profilePath") val profilePath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("originalLanguage") val originalLanguage: String? = null,
    @SerialName("genreIds") val genreIds: List<Int>? = null,
    @SerialName("genres") val genres: List<SeerrGenre>? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("adult") val adult: Boolean? = null,
    @SerialName("video") val video: Boolean? = null,
    @SerialName("originCountry") val originCountry: List<String>? = null,
    @SerialName("numberOfSeasons") val numberOfSeasons: Int? = null,
    @SerialName("numberOfEpisodes") val numberOfEpisodes: Int? = null,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
    @SerialName("credits") val credits: SeerrCredits? = null,
    @SerialName("externalIds") val externalIds: SeerrExternalIds? = null,
    @SerialName("keywords") val keywords: List<SeerrKeyword>? = null,
    @SerialName("relatedVideos") val relatedVideos: List<SeerrVideo>? = null,
    @SerialName("releases") val releases: SeerrReleases? = null,
    @SerialName("contentRatings") val contentRatings: SeerrContentRatings? = null,
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

    /** Availability status from mediaInfo */
    val availabilityStatus: Int?
        get() = mediaInfo?.status

    /** Whether this media is available in the library */
    val isAvailable: Boolean
        get() = mediaInfo?.status == SeerrMediaStatus.AVAILABLE

    /** Whether this media has a pending request */
    val isPending: Boolean
        get() = mediaInfo?.status == SeerrMediaStatus.PENDING ||
            mediaInfo?.status == SeerrMediaStatus.PROCESSING

    /** Whether this is a movie */
    val isMovie: Boolean
        get() = mediaType == "movie" || (mediaType.isEmpty() && title != null && name == null)

    /** Whether this is a TV show */
    val isTvShow: Boolean
        get() = mediaType == "tv" || (mediaType.isEmpty() && name != null)

    /** Content rating (e.g., "PG-13", "R", "TV-MA") - prefers US rating */
    val contentRating: String?
        get() {
            // For movies: check releases
            releases?.results?.let { releaseResults ->
                val usRelease = releaseResults.find { it.iso31661 == "US" }
                usRelease?.releaseDates?.firstOrNull { !it.certification.isNullOrBlank() }
                    ?.certification
                    ?.let { return it }
                releaseResults.flatMap { it.releaseDates ?: emptyList() }
                    .firstOrNull { !it.certification.isNullOrBlank() }
                    ?.certification
                    ?.let { return it }
            }
            // For TV: check contentRatings
            contentRatings?.results?.let { ratings ->
                ratings.find { it.iso31661 == "US" }?.rating?.let { return it }
                ratings.firstOrNull { !it.rating.isNullOrBlank() }?.rating?.let { return it }
            }
            return null
        }
}

/**
 * Media info containing availability status and request details.
 */
@Serializable
data class SeerrMediaInfo(
    @SerialName("id") val id: Int? = null,
    @SerialName("tmdbId") val tmdbId: Int? = null,
    @SerialName("tvdbId") val tvdbId: Int? = null,
    @SerialName("imdbId") val imdbId: String? = null,
    @SerialName("mediaType") val mediaType: String? = null,
    @SerialName("status") val status: Int? = null,
    @SerialName("status4k") val status4k: Int? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("lastSeasonChange") val lastSeasonChange: String? = null,
    @SerialName("mediaAddedAt") val mediaAddedAt: String? = null,
    @SerialName("serviceId") val serviceId: Int? = null,
    @SerialName("serviceId4k") val serviceId4k: Int? = null,
    @SerialName("externalServiceId") val externalServiceId: Int? = null,
    @SerialName("externalServiceId4k") val externalServiceId4k: Int? = null,
    @SerialName("externalServiceSlug") val externalServiceSlug: String? = null,
    @SerialName("externalServiceSlug4k") val externalServiceSlug4k: String? = null,
    @SerialName("ratingKey") val ratingKey: String? = null,
    @SerialName("ratingKey4k") val ratingKey4k: String? = null,
    @SerialName("plexUrl") val plexUrl: String? = null,
    @SerialName("iOSPlexUrl") val iOSPlexUrl: String? = null,
    @SerialName("mediaUrl") val mediaUrl: String? = null,
    @SerialName("serviceUrl") val serviceUrl: String? = null,
    @SerialName("requests") val requests: List<SeerrRequest>? = null,
    @SerialName("seasons") val seasons: List<SeerrSeasonStatus>? = null,
    @SerialName("issues") val issues: List<SeerrIssue>? = null,
    @SerialName("downloadStatus") val downloadStatus: List<SeerrDownloadStatus>? = null,
    @SerialName("downloadStatus4k") val downloadStatus4k: List<SeerrDownloadStatus>? = null,
) {
    /** Whether this media is available */
    val isAvailable: Boolean
        get() = status == SeerrMediaStatus.AVAILABLE

    /** Whether this media has a pending/processing request */
    val isPending: Boolean
        get() = status == SeerrMediaStatus.PENDING || status == SeerrMediaStatus.PROCESSING

    /** Whether this media is partially available */
    val isPartiallyAvailable: Boolean
        get() = status == SeerrMediaStatus.PARTIALLY_AVAILABLE
}

/**
 * Season availability status within MediaInfo.
 */
@Serializable
data class SeerrSeasonStatus(
    @SerialName("id") val id: Int,
    @SerialName("seasonNumber") val seasonNumber: Int,
    @SerialName("status") val status: Int,
    @SerialName("status4k") val status4k: Int? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

/**
 * Download status for media being downloaded.
 */
@Serializable
data class SeerrDownloadStatus(
    @SerialName("externalId") val externalId: Int,
    @SerialName("mediaType") val mediaType: String,
    @SerialName("title") val title: String,
    @SerialName("status") val status: String,
    @SerialName("size") val size: Long,
    @SerialName("sizeLeft") val sizeLeft: Long? = null,
    @SerialName("timeLeft") val timeLeft: String? = null,
    @SerialName("estimatedCompletionTime") val estimatedCompletionTime: String? = null,
)

// ============================================================================
// Movie Details
// ============================================================================

/**
 * Full movie details from /movie/{id} endpoint.
 */
@Serializable
data class SeerrMovieDetails(
    @SerialName("id") val id: Int,
    @SerialName("imdbId") val imdbId: String? = null,
    @SerialName("adult") val adult: Boolean? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("budget") val budget: Long? = null,
    @SerialName("genres") val genres: List<SeerrGenre>? = null,
    @SerialName("homepage") val homepage: String? = null,
    @SerialName("relatedVideos") val relatedVideos: List<SeerrVideo>? = null,
    @SerialName("originalLanguage") val originalLanguage: String? = null,
    @SerialName("originalTitle") val originalTitle: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("productionCompanies") val productionCompanies: List<SeerrProductionCompany>? = null,
    @SerialName("productionCountries") val productionCountries: List<SeerrProductionCountry>? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("releases") val releases: SeerrReleases? = null,
    @SerialName("revenue") val revenue: Long? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("spokenLanguages") val spokenLanguages: List<SeerrSpokenLanguage>? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("tagline") val tagline: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("video") val video: Boolean? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("credits") val credits: SeerrCredits? = null,
    @SerialName("collection") val collection: SeerrCollectionInfo? = null,
    @SerialName("externalIds") val externalIds: SeerrExternalIds? = null,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
    @SerialName("watchProviders") val watchProviders: List<SeerrWatchProviderRegion>? = null,
    @SerialName("keywords") val keywords: List<SeerrKeyword>? = null,
) {
    /** Display title */
    val displayTitle: String
        get() = title ?: originalTitle ?: "Unknown"

    /** Year extracted from release date */
    val year: Int?
        get() = releaseDate?.take(4)?.toIntOrNull()

    /** Runtime formatted as hours and minutes */
    val runtimeFormatted: String?
        get() = runtime?.let {
            val hours = it / 60
            val minutes = it % 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    /** Content rating (prefers US) */
    val contentRating: String?
        get() = releases?.results?.let { releaseResults ->
            val usRelease = releaseResults.find { it.iso31661 == "US" }
            usRelease?.releaseDates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
                ?: releaseResults.flatMap { it.releaseDates ?: emptyList() }
                    .firstOrNull { !it.certification.isNullOrBlank() }?.certification
        }

    /** Whether this movie is available in the library */
    val isAvailable: Boolean
        get() = mediaInfo?.status == SeerrMediaStatus.AVAILABLE
}

// ============================================================================
// TV Details
// ============================================================================

/**
 * Full TV series details from /tv/{id} endpoint.
 */
@Serializable
data class SeerrTvDetails(
    @SerialName("id") val id: Int,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("contentRatings") val contentRatings: SeerrContentRatings? = null,
    @SerialName("createdBy") val createdBy: List<SeerrCreatedBy>? = null,
    @SerialName("episodeRunTime") val episodeRunTime: List<Int>? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("genres") val genres: List<SeerrGenre>? = null,
    @SerialName("homepage") val homepage: String? = null,
    @SerialName("inProduction") val inProduction: Boolean? = null,
    @SerialName("languages") val languages: List<String>? = null,
    @SerialName("lastAirDate") val lastAirDate: String? = null,
    @SerialName("lastEpisodeToAir") val lastEpisodeToAir: SeerrEpisode? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("nextEpisodeToAir") val nextEpisodeToAir: SeerrEpisode? = null,
    @SerialName("networks") val networks: List<SeerrNetworkInfo>? = null,
    @SerialName("numberOfEpisodes") val numberOfEpisodes: Int? = null,
    @SerialName("numberOfSeasons") val numberOfSeasons: Int? = null,
    @SerialName("originCountry") val originCountry: List<String>? = null,
    @SerialName("originalLanguage") val originalLanguage: String? = null,
    @SerialName("originalName") val originalName: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("productionCompanies") val productionCompanies: List<SeerrProductionCompany>? = null,
    @SerialName("productionCountries") val productionCountries: List<SeerrProductionCountry>? = null,
    @SerialName("spokenLanguages") val spokenLanguages: List<SeerrSpokenLanguage>? = null,
    @SerialName("seasons") val seasons: List<SeerrSeason>? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("tagline") val tagline: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("credits") val credits: SeerrCredits? = null,
    @SerialName("externalIds") val externalIds: SeerrExternalIds? = null,
    @SerialName("keywords") val keywords: List<SeerrKeyword>? = null,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
    @SerialName("watchProviders") val watchProviders: List<SeerrWatchProviderRegion>? = null,
    @SerialName("relatedVideos") val relatedVideos: List<SeerrVideo>? = null,
) {
    /** Display title */
    val displayTitle: String
        get() = name ?: originalName ?: "Unknown"

    /** Year extracted from first air date */
    val year: Int?
        get() = firstAirDate?.take(4)?.toIntOrNull()

    /** Average episode runtime formatted */
    val averageRuntimeFormatted: String?
        get() = episodeRunTime?.average()?.toInt()?.let { "${it}m" }

    /** Content rating (prefers US) */
    val contentRating: String?
        get() = contentRatings?.results?.let { ratings ->
            ratings.find { it.iso31661 == "US" }?.rating
                ?: ratings.firstOrNull { !it.rating.isNullOrBlank() }?.rating
        }

    /** Whether this series is available in the library */
    val isAvailable: Boolean
        get() = mediaInfo?.status == SeerrMediaStatus.AVAILABLE
}

/**
 * TV show creator info.
 */
@Serializable
data class SeerrCreatedBy(
    @SerialName("id") val id: Int,
    @SerialName("credit_id") val creditId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("gender") val gender: Int? = null,
    @SerialName("profilePath") val profilePath: String? = null,
)

// ============================================================================
// Season and Episode Types
// ============================================================================

/**
 * TV season details.
 */
@Serializable
data class SeerrSeason(
    @SerialName("id") val id: Int,
    @SerialName("seasonNumber") val seasonNumber: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("airDate") val airDate: String? = null,
    @SerialName("episodeCount") val episodeCount: Int? = null,
    @SerialName("episodes") val episodes: List<SeerrEpisode>? = null,
) {
    /** Year extracted from air date */
    val year: Int?
        get() = airDate?.take(4)?.toIntOrNull()
}

/**
 * TV episode details.
 */
@Serializable
data class SeerrEpisode(
    @SerialName("id") val id: Int,
    @SerialName("episodeNumber") val episodeNumber: Int,
    @SerialName("seasonNumber") val seasonNumber: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("stillPath") val stillPath: String? = null,
    @SerialName("airDate") val airDate: String? = null,
    @SerialName("productionCode") val productionCode: String? = null,
    @SerialName("showId") val showId: Int? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
)

// ============================================================================
// Request Types
// ============================================================================

/**
 * Media request.
 */
@Serializable
data class SeerrRequest(
    @SerialName("id") val id: Int,
    @SerialName("status") val status: Int,
    @SerialName("media") val media: SeerrMediaInfo? = null,
    @SerialName("requestedBy") val requestedBy: SeerrUser? = null,
    @SerialName("modifiedBy") val modifiedBy: SeerrUser? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("seasons") val seasons: List<SeerrSeasonRequest>? = null,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("serverId") val serverId: Int? = null,
    @SerialName("profileId") val profileId: Int? = null,
    @SerialName("profileName") val profileName: String? = null,
    @SerialName("rootFolder") val rootFolder: String? = null,
    @SerialName("languageProfileId") val languageProfileId: Int? = null,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("isAutoRequest") val isAutoRequest: Boolean = false,
    @SerialName("type") val type: String? = null,
    @SerialName("seasonCount") val seasonCount: Int? = null,
) {
    /** Whether the request is pending approval */
    val isPendingApproval: Boolean
        get() = status == SeerrRequestStatus.PENDING_APPROVAL

    /** Whether the request is approved */
    val isApproved: Boolean
        get() = status == SeerrRequestStatus.APPROVED

    /** Whether the request is declined */
    val isDeclined: Boolean
        get() = status == SeerrRequestStatus.DECLINED

    /** Human-readable status text */
    val statusText: String
        get() = SeerrRequestStatus.toDisplayString(status)
}

/**
 * Season request details.
 */
@Serializable
data class SeerrSeasonRequest(
    @SerialName("id") val id: Int,
    @SerialName("seasonNumber") val seasonNumber: Int,
    @SerialName("status") val status: Int,
)

/**
 * Request body for creating a new media request.
 */
@Serializable
data class SeerrCreateRequest(
    @SerialName("mediaType") val mediaType: String,
    @SerialName("mediaId") val mediaId: Int,
    @SerialName("tvdbId") val tvdbId: Int? = null,
    @SerialName("seasons") val seasons: List<Int>? = null,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("serverId") val serverId: Int? = null,
    @SerialName("profileId") val profileId: Int? = null,
    @SerialName("rootFolder") val rootFolder: String? = null,
    @SerialName("languageProfileId") val languageProfileId: Int? = null,
    @SerialName("userId") val userId: Int? = null,
    @SerialName("tags") val tags: List<String>? = null,
)

/**
 * Response from creating/updating a request.
 */
@Serializable
data class SeerrRequestResponse(
    @SerialName("id") val id: Int,
    @SerialName("status") val status: Int,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("media") val media: SeerrMediaInfo? = null,
)

// ============================================================================
// Issue Types
// ============================================================================

/**
 * Media issue (problem report).
 */
@Serializable
data class SeerrIssue(
    @SerialName("id") val id: Int,
    @SerialName("issueType") val issueType: Int,
    @SerialName("status") val status: Int,
    @SerialName("problemSeason") val problemSeason: Int? = null,
    @SerialName("problemEpisode") val problemEpisode: Int? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("media") val media: SeerrMediaInfo? = null,
    @SerialName("createdBy") val createdBy: SeerrUser? = null,
    @SerialName("modifiedBy") val modifiedBy: SeerrUser? = null,
    @SerialName("comments") val comments: List<SeerrIssueComment>? = null,
) {
    /** Whether this issue is open */
    val isOpen: Boolean
        get() = status == SeerrIssueStatus.OPEN

    /** Whether this issue is resolved */
    val isResolved: Boolean
        get() = status == SeerrIssueStatus.RESOLVED

    /** Human-readable issue type */
    val issueTypeText: String
        get() = SeerrIssueType.toDisplayString(issueType)
}

/**
 * Comment on an issue.
 */
@Serializable
data class SeerrIssueComment(
    @SerialName("id") val id: Int,
    @SerialName("message") val message: String,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("user") val user: SeerrUser? = null,
)

/**
 * Request body for creating an issue.
 */
@Serializable
data class SeerrCreateIssue(
    @SerialName("issueType") val issueType: Int,
    @SerialName("message") val message: String,
    @SerialName("mediaId") val mediaId: Int,
    @SerialName("problemSeason") val problemSeason: Int? = null,
    @SerialName("problemEpisode") val problemEpisode: Int? = null,
)

// ============================================================================
// User Types
// ============================================================================

/**
 * Seerr user.
 */
@Serializable
data class SeerrUser(
    @SerialName("id") val id: Int,
    @SerialName("email") val email: String? = null,
    @SerialName("plexUsername") val plexUsername: String? = null,
    @SerialName("jellyfinUsername") val jellyfinUsername: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("requestCount") val requestCount: Int? = null,
    @SerialName("movieQuotaLimit") val movieQuotaLimit: Int? = null,
    @SerialName("movieQuotaDays") val movieQuotaDays: Int? = null,
    @SerialName("tvQuotaLimit") val tvQuotaLimit: Int? = null,
    @SerialName("tvQuotaDays") val tvQuotaDays: Int? = null,
    @SerialName("permissions") val permissions: Int? = null,
    @SerialName("userType") val userType: Int? = null,
    @SerialName("plexId") val plexId: Int? = null,
    @SerialName("jellyfinUserId") val jellyfinUserId: String? = null,
    @SerialName("plexToken") val plexToken: String? = null,
    @SerialName("jellyfinAuthToken") val jellyfinAuthToken: String? = null,
    @SerialName("warnings") val warnings: List<String>? = null,
    @SerialName("recoveryLinkExpirationDate") val recoveryLinkExpirationDate: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
) {
    /** Display name for UI */
    val name: String
        get() = displayName ?: username ?: jellyfinUsername ?: plexUsername ?: email ?: "User"

    /** Whether this is a Jellyfin user */
    val isJellyfinUser: Boolean
        get() = userType == SeerrUserType.JELLYFIN

    /** Whether this is a Plex user */
    val isPlexUser: Boolean
        get() = userType == SeerrUserType.PLEX

    /** Whether this is a local user */
    val isLocalUser: Boolean
        get() = userType == SeerrUserType.LOCAL
}

/**
 * User quota information.
 */
@Serializable
data class SeerrQuota(
    @SerialName("movie") val movie: SeerrQuotaDetails? = null,
    @SerialName("tv") val tv: SeerrQuotaDetails? = null,
)

/**
 * Quota details for a media type.
 */
@Serializable
data class SeerrQuotaDetails(
    @SerialName("limit") val limit: Int? = null,
    @SerialName("days") val days: Int? = null,
    @SerialName("used") val used: Int = 0,
    @SerialName("remaining") val remaining: Int? = null,
) {
    /** Whether quota is unlimited */
    val isUnlimited: Boolean
        get() = limit == null || limit == 0

    /** Whether user has exceeded quota */
    val isExceeded: Boolean
        get() = remaining != null && remaining <= 0
}

// ============================================================================
// Credits Types
// ============================================================================

/**
 * Cast and crew credits.
 */
@Serializable
data class SeerrCredits(
    @SerialName("cast") val cast: List<SeerrCastMember>? = null,
    @SerialName("crew") val crew: List<SeerrCrewMember>? = null,
)

/**
 * Cast member (actor).
 */
@Serializable
data class SeerrCastMember(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("character") val character: String? = null,
    @SerialName("profilePath") val profilePath: String? = null,
    @SerialName("order") val order: Int? = null,
    @SerialName("castId") val castId: Int? = null,
    @SerialName("creditId") val creditId: String? = null,
    @SerialName("gender") val gender: Int? = null,
)

/**
 * Crew member (director, writer, etc.).
 */
@Serializable
data class SeerrCrewMember(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("job") val job: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("profilePath") val profilePath: String? = null,
    @SerialName("creditId") val creditId: String? = null,
    @SerialName("gender") val gender: Int? = null,
)

// ============================================================================
// Person Types
// ============================================================================

/**
 * Person details from /person/{id} endpoint.
 */
@Serializable
data class SeerrPerson(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("biography") val biography: String? = null,
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("deathday") val deathday: String? = null,
    @SerialName("placeOfBirth") val placeOfBirth: String? = null,
    @SerialName("profilePath") val profilePath: String? = null,
    @SerialName("knownForDepartment") val knownForDepartment: String? = null,
    @SerialName("alsoKnownAs") val alsoKnownAs: List<String>? = null,
    @SerialName("gender") val gender: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("adult") val adult: Boolean? = null,
    @SerialName("imdbId") val imdbId: String? = null,
    @SerialName("homepage") val homepage: String? = null,
) {
    /** Formatted "Also known as" text */
    val formattedAlsoKnownAs: String?
        get() = alsoKnownAs?.takeIf { it.isNotEmpty() }?.let { names ->
            "Also known as: ${names.take(3).joinToString(", ")}"
        }

    /** Age calculated from birthday */
    val age: Int?
        get() {
            val birthYear = birthday?.take(4)?.toIntOrNull() ?: return null
            val currentYear = java.time.LocalDate.now().year
            val deathYear = deathday?.take(4)?.toIntOrNull()
            return (deathYear ?: currentYear) - birthYear
        }
}

/**
 * Person's combined credits (filmography).
 */
@Serializable
data class SeerrPersonCredits(
    @SerialName("id") val id: Int,
    @SerialName("cast") val cast: List<SeerrPersonCastCredit>? = null,
    @SerialName("crew") val crew: List<SeerrPersonCrewCredit>? = null,
) {
    /** Cast credits sorted by popularity */
    val sortedCast: List<SeerrPersonCastCredit>
        get() = cast?.sortedByDescending { it.popularity ?: 0.0 } ?: emptyList()

    /** Crew credits sorted by popularity */
    val sortedCrew: List<SeerrPersonCrewCredit>
        get() = crew?.sortedByDescending { it.popularity ?: 0.0 } ?: emptyList()
}

/**
 * A cast credit for a person.
 */
@Serializable
data class SeerrPersonCastCredit(
    @SerialName("id") val id: Int,
    @SerialName("mediaType") val mediaType: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("character") val character: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("originalLanguage") val originalLanguage: String? = null,
    @SerialName("originalTitle") val originalTitle: String? = null,
    @SerialName("originalName") val originalName: String? = null,
    @SerialName("genreIds") val genreIds: List<Int>? = null,
    @SerialName("episodeCount") val episodeCount: Int? = null,
    @SerialName("creditId") val creditId: String? = null,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
) {
    val displayTitle: String
        get() = title ?: name ?: originalTitle ?: originalName ?: "Unknown"

    val displayDate: String?
        get() = releaseDate ?: firstAirDate

    val year: Int?
        get() = displayDate?.take(4)?.toIntOrNull()
}

/**
 * A crew credit for a person.
 */
@Serializable
data class SeerrPersonCrewCredit(
    @SerialName("id") val id: Int,
    @SerialName("mediaType") val mediaType: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("job") val job: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("originalTitle") val originalTitle: String? = null,
    @SerialName("originalName") val originalName: String? = null,
    @SerialName("creditId") val creditId: String? = null,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
) {
    val displayTitle: String
        get() = title ?: name ?: originalTitle ?: originalName ?: "Unknown"
}

// ============================================================================
// External Ratings (Rotten Tomatoes, IMDB)
// ============================================================================

/**
 * Combined ratings response.
 */
@Serializable
data class SeerrRatings(
    @SerialName("rt") val rt: SeerrRottenTomatoesRating? = null,
    @SerialName("imdb") val imdb: SeerrImdbRating? = null,
)

/**
 * Rotten Tomatoes ratings.
 */
@Serializable
data class SeerrRottenTomatoesRating(
    @SerialName("title") val title: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("criticsRating") val criticsRating: String? = null,
    @SerialName("criticsScore") val criticsScore: Int? = null,
    @SerialName("audienceRating") val audienceRating: String? = null,
    @SerialName("audienceScore") val audienceScore: Int? = null,
    @SerialName("year") val year: Int? = null,
) {
    /** Whether critics rating is fresh */
    val isCriticsFresh: Boolean
        get() = criticsRating == "Fresh" || criticsRating == "Certified Fresh"

    /** Whether audience rating is positive */
    val isAudienceFresh: Boolean
        get() = audienceRating == "Upright"
}

/**
 * IMDB rating.
 */
@Serializable
data class SeerrImdbRating(
    @SerialName("title") val title: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("criticsScore") val criticsScore: Double? = null,
)

// ============================================================================
// Discovery/Search Response Types
// ============================================================================

/**
 * Paginated discover/search result.
 */
@Serializable
data class SeerrDiscoverResponse(
    @SerialName("page") val page: Int,
    @SerialName("totalPages") val totalPages: Int,
    @SerialName("totalResults") val totalResults: Int,
    @SerialName("results") val results: List<SeerrMedia>,
)

/**
 * Search response with polymorphic results.
 */
@Serializable
data class SeerrSearchResponse(
    @SerialName("page") val page: Int = 1,
    @SerialName("totalPages") val totalPages: Int = 1,
    @SerialName("totalResults") val totalResults: Int = 0,
    @SerialName("results") val results: List<SeerrSearchResult> = emptyList(),
)

/**
 * Search result - can be movie, tv, person, or collection.
 */
@Serializable(with = SeerrSearchResultSerializer::class)
sealed interface SeerrSearchResult {
    val id: Int
    val mediaType: String?
    val posterPath: String?
    val backdropPath: String?
    val popularity: Double?
}

/**
 * Movie search result.
 */
@Serializable
data class SeerrSearchMovie(
    @SerialName("id") override val id: Int,
    @SerialName("mediaType") override val mediaType: String? = "movie",
    @SerialName("posterPath") override val posterPath: String? = null,
    @SerialName("backdropPath") override val backdropPath: String? = null,
    @SerialName("popularity") override val popularity: Double? = null,
    @SerialName("title") val title: String = "",
    @SerialName("originalTitle") val originalTitle: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("adult") val adult: Boolean = false,
    @SerialName("genreIds") val genreIds: List<Int>? = null,
    @SerialName("originalLanguage") val originalLanguage: String? = null,
    @SerialName("video") val video: Boolean = false,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
) : SeerrSearchResult {
    val displayTitle: String get() = title.ifBlank { originalTitle ?: "Unknown" }
    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
}

/**
 * TV show search result.
 */
@Serializable
data class SeerrSearchTv(
    @SerialName("id") override val id: Int,
    @SerialName("mediaType") override val mediaType: String? = "tv",
    @SerialName("posterPath") override val posterPath: String? = null,
    @SerialName("backdropPath") override val backdropPath: String? = null,
    @SerialName("popularity") override val popularity: Double? = null,
    @SerialName("name") val name: String = "",
    @SerialName("originalName") val originalName: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("genreIds") val genreIds: List<Int>? = null,
    @SerialName("originalLanguage") val originalLanguage: String? = null,
    @SerialName("originCountry") val originCountry: List<String>? = null,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
) : SeerrSearchResult {
    val displayTitle: String get() = name.ifBlank { originalName ?: "Unknown" }
    val year: Int? get() = firstAirDate?.take(4)?.toIntOrNull()
}

/**
 * Person search result.
 */
@Serializable
data class SeerrSearchPerson(
    @SerialName("id") override val id: Int,
    @SerialName("mediaType") override val mediaType: String? = "person",
    @SerialName("posterPath") override val posterPath: String? = null,
    @SerialName("backdropPath") override val backdropPath: String? = null,
    @SerialName("popularity") override val popularity: Double? = null,
    @SerialName("name") val name: String = "",
    @SerialName("profilePath") val profilePath: String? = null,
    @SerialName("knownFor") val knownFor: List<SeerrKnownForMedia>? = null,
) : SeerrSearchResult

/**
 * Collection search result.
 */
@Serializable
data class SeerrSearchCollection(
    @SerialName("id") override val id: Int,
    @SerialName("mediaType") override val mediaType: String? = "collection",
    @SerialName("posterPath") override val posterPath: String? = null,
    @SerialName("backdropPath") override val backdropPath: String? = null,
    @SerialName("popularity") override val popularity: Double? = null,
    @SerialName("name") val name: String = "",
    @SerialName("overview") val overview: String? = null,
    @SerialName("adult") val adult: Boolean = false,
) : SeerrSearchResult

/**
 * Known for media item in person search.
 */
@Serializable
data class SeerrKnownForMedia(
    @SerialName("id") val id: Int,
    @SerialName("mediaType") val mediaType: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
}

/**
 * Polymorphic serializer for search results.
 */
object SeerrSearchResultSerializer : JsonContentPolymorphicSerializer<SeerrSearchResult>(SeerrSearchResult::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out SeerrSearchResult> {
        return when (val type = element.jsonObject["mediaType"]?.jsonPrimitive?.content) {
            "movie" -> SeerrSearchMovie.serializer()
            "tv" -> SeerrSearchTv.serializer()
            "person" -> SeerrSearchPerson.serializer()
            "collection" -> SeerrSearchCollection.serializer()
            null -> {
                // Fallback based on field presence
                when {
                    "title" in element.jsonObject -> SeerrSearchMovie.serializer()
                    "firstAirDate" in element.jsonObject -> SeerrSearchTv.serializer()
                    "profilePath" in element.jsonObject -> SeerrSearchPerson.serializer()
                    else -> SeerrSearchCollection.serializer()
                }
            }
            else -> throw SerializationException("Unknown media type: $type")
        }
    }
}

/**
 * Paginated request list result.
 */
@Serializable
data class SeerrRequestListResponse(
    @SerialName("pageInfo") val pageInfo: SeerrPageInfo,
    @SerialName("results") val results: List<SeerrRequest>,
)

/**
 * Paginated issue list result.
 */
@Serializable
data class SeerrIssueListResponse(
    @SerialName("pageInfo") val pageInfo: SeerrPageInfo,
    @SerialName("results") val results: List<SeerrIssue>,
)

/**
 * Paginated user list result.
 */
@Serializable
data class SeerrUserListResponse(
    @SerialName("pageInfo") val pageInfo: SeerrPageInfo,
    @SerialName("results") val results: List<SeerrUser>,
)

/**
 * Page info for paginated responses.
 */
@Serializable
data class SeerrPageInfo(
    @SerialName("page") val page: Int,
    @SerialName("pages") val pages: Int,
    @SerialName("pageSize") val pageSize: Int,
    @SerialName("results") val results: Int,
)

// ============================================================================
// Supporting Types
// ============================================================================

/**
 * Genre.
 */
@Serializable
data class SeerrGenre(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("backdrops") val backdrops: List<String>? = null,
)

/**
 * Keyword.
 */
@Serializable
data class SeerrKeyword(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
)

/**
 * Network info (for TV shows).
 */
@Serializable
data class SeerrNetworkInfo(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("logoPath") val logoPath: String? = null,
    @SerialName("originCountry") val originCountry: String? = null,
)

/**
 * Production company.
 */
@Serializable
data class SeerrProductionCompany(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("logoPath") val logoPath: String? = null,
    @SerialName("originCountry") val originCountry: String? = null,
)

/**
 * Production country.
 */
@Serializable
data class SeerrProductionCountry(
    @SerialName("iso_3166_1") val iso31661: String,
    @SerialName("name") val name: String,
)

/**
 * Spoken language.
 */
@Serializable
data class SeerrSpokenLanguage(
    @SerialName("englishName") val englishName: String? = null,
    @SerialName("iso_639_1") val iso6391: String? = null,
    @SerialName("name") val name: String? = null,
)

/**
 * External IDs (IMDB, TVDB, social media).
 */
@Serializable
data class SeerrExternalIds(
    @SerialName("imdbId") val imdbId: String? = null,
    @SerialName("facebookId") val facebookId: String? = null,
    @SerialName("instagramId") val instagramId: String? = null,
    @SerialName("twitterId") val twitterId: String? = null,
    @SerialName("freebaseId") val freebaseId: String? = null,
    @SerialName("freebaseMid") val freebaseMid: String? = null,
    @SerialName("tvdbId") val tvdbId: Int? = null,
    @SerialName("tvrageId") val tvrageId: Int? = null,
)

/**
 * Related video (trailer, teaser, etc.).
 */
@Serializable
data class SeerrVideo(
    @SerialName("id") val id: String? = null,
    @SerialName("key") val key: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("site") val site: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("size") val size: Int? = null,
    @SerialName("url") val url: String? = null,
) {
    /** YouTube URL if site is YouTube */
    val youtubeUrl: String?
        get() = if (site == "YouTube" && key != null) {
            "https://www.youtube.com/watch?v=$key"
        } else {
            url
        }
}

/**
 * Collection info (embedded in movie details).
 */
@Serializable
data class SeerrCollectionInfo(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
)

/**
 * Full collection details from /collection/{id} endpoint.
 */
@Serializable
data class SeerrCollection(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("parts") val parts: List<SeerrMedia> = emptyList(),
)

/**
 * Watch provider region data.
 */
@Serializable
data class SeerrWatchProviderRegion(
    @SerialName("iso_3166_1") val iso31661: String,
    @SerialName("link") val link: String? = null,
    @SerialName("buy") val buy: List<SeerrWatchProvider>? = null,
    @SerialName("flatrate") val flatrate: List<SeerrWatchProvider>? = null,
)

/**
 * Watch provider (streaming service).
 */
@Serializable
data class SeerrWatchProvider(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("logoPath") val logoPath: String? = null,
    @SerialName("displayPriority") val displayPriority: Int? = null,
)

// ============================================================================
// Content Rating Types
// ============================================================================

/**
 * Movie releases container.
 */
@Serializable
data class SeerrReleases(
    @SerialName("results") val results: List<SeerrReleaseCountry>? = null,
)

/**
 * Release info per country.
 */
@Serializable
data class SeerrReleaseCountry(
    @SerialName("iso_3166_1") val iso31661: String? = null,
    @SerialName("release_dates") val releaseDates: List<SeerrReleaseDate>? = null,
)

/**
 * Individual release date with certification.
 */
@Serializable
data class SeerrReleaseDate(
    @SerialName("certification") val certification: String? = null,
    @SerialName("iso_639_1") val iso6391: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("type") val type: Int? = null,
    @SerialName("note") val note: String? = null,
)

/**
 * TV content ratings container.
 */
@Serializable
data class SeerrContentRatings(
    @SerialName("results") val results: List<SeerrContentRating>? = null,
)

/**
 * Content rating per country.
 */
@Serializable
data class SeerrContentRating(
    @SerialName("iso_3166_1") val iso31661: String? = null,
    @SerialName("rating") val rating: String? = null,
)

// ============================================================================
// Server/Settings Types
// ============================================================================

/**
 * Seerr server status.
 */
@Serializable
data class SeerrServerInfo(
    @SerialName("version") val version: String,
    @SerialName("commitTag") val commitTag: String? = null,
    @SerialName("updateAvailable") val updateAvailable: Boolean = false,
    @SerialName("commitsBehind") val commitsBehind: Int = 0,
    @SerialName("restartRequired") val restartRequired: Boolean = false,
)

/**
 * Seerr main settings.
 */
@Serializable
data class SeerrMainSettings(
    @SerialName("apiKey") val apiKey: String? = null,
    @SerialName("appLanguage") val appLanguage: String? = null,
    @SerialName("applicationTitle") val applicationTitle: String? = null,
    @SerialName("applicationUrl") val applicationUrl: String? = null,
    @SerialName("hideAvailable") val hideAvailable: Boolean = false,
    @SerialName("partialRequestsEnabled") val partialRequestsEnabled: Boolean = false,
    @SerialName("localLogin") val localLogin: Boolean = true,
    @SerialName("mediaServerType") val mediaServerType: Int? = null,
    @SerialName("newPlexLogin") val newPlexLogin: Boolean = true,
    @SerialName("defaultPermissions") val defaultPermissions: Int? = null,
    @SerialName("enableSpecialEpisodes") val enableSpecialEpisodes: Boolean = false,
)

/**
 * Public settings (no auth required).
 */
@Serializable
data class SeerrPublicSettings(
    @SerialName("initialized") val initialized: Boolean = false,
)

/**
 * Discover slider configuration.
 */
@Serializable
data class SeerrDiscoverSlider(
    @SerialName("id") val id: Int,
    @SerialName("type") val typeId: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("data") val data: String? = null,
    @SerialName("isBuiltIn") val isBuiltIn: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = true,
) {
    /** The slider type enum */
    val type: SeerrDiscoverSliderType
        get() = SeerrDiscoverSliderType.fromId(typeId)
}

/**
 * Discover slider types.
 * Based on Jellyseerr/Overseerr slider type IDs.
 */
enum class SeerrDiscoverSliderType(val id: Int) {
    RECENTLY_ADDED(1),
    RECENT_REQUESTS(2),
    PLEX_WATCHLIST(3),
    TRENDING(4),
    POPULAR_MOVIES(5),
    MOVIE_GENRES(6),
    UPCOMING_MOVIES(7),
    STUDIOS(8),
    POPULAR_TV(9),
    TV_GENRES(10),
    UPCOMING_TV(11),
    NETWORKS(12),
    TMDB_MOVIE_KEYWORD(13),
    TMDB_TV_KEYWORD(14),
    TMDB_MOVIE_GENRE(15),
    TMDB_TV_GENRE(16),
    TMDB_STUDIO(17),
    TMDB_NETWORK(18),
    TMDB_SEARCH(19),
    TMDB_MOVIE_STREAMING_SERVICES(20),
    TMDB_TV_STREAMING_SERVICES(21),
    UNKNOWN(0);

    companion object {
        fun fromId(id: Int): SeerrDiscoverSliderType =
            entries.find { it.id == id } ?: UNKNOWN
    }
}

// ============================================================================
// Authentication Types
// ============================================================================

/**
 * Jellyfin auth request body.
 */
@Serializable
data class SeerrJellyfinAuthRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("hostname") val hostname: String? = null,
)

/**
 * Local auth request body.
 */
@Serializable
data class SeerrLocalAuthRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

// ============================================================================
// Status Constants
// ============================================================================

/**
 * Media availability status codes.
 */
object SeerrMediaStatus {
    const val UNKNOWN = 1
    const val PENDING = 2
    const val PROCESSING = 3
    const val PARTIALLY_AVAILABLE = 4
    const val AVAILABLE = 5
    const val DELETED = 6

    fun toDisplayString(status: Int?): String = when (status) {
        UNKNOWN -> "Not Requested"
        PENDING -> "Pending"
        PROCESSING -> "Processing"
        PARTIALLY_AVAILABLE -> "Partially Available"
        AVAILABLE -> "Available"
        DELETED -> "Deleted"
        else -> "Unknown"
    }
}

/**
 * Request status codes.
 */
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

/**
 * Issue type codes.
 */
object SeerrIssueType {
    const val VIDEO = 1
    const val AUDIO = 2
    const val SUBTITLE = 3
    const val OTHER = 4

    fun toDisplayString(type: Int): String = when (type) {
        VIDEO -> "Video"
        AUDIO -> "Audio"
        SUBTITLE -> "Subtitle"
        OTHER -> "Other"
        else -> "Unknown"
    }
}

/**
 * Issue status codes.
 */
object SeerrIssueStatus {
    const val OPEN = 1
    const val RESOLVED = 2

    fun toDisplayString(status: Int): String = when (status) {
        OPEN -> "Open"
        RESOLVED -> "Resolved"
        else -> "Unknown"
    }
}

/**
 * User type codes.
 */
object SeerrUserType {
    const val PLEX = 1
    const val LOCAL = 2
    const val JELLYFIN = 3
}

/**
 * Discover slider type codes.
 */
object SeerrSliderType {
    const val RECENTLY_ADDED = 1
    const val RECENT_REQUESTS = 2
    const val PLEX_WATCHLIST = 3
    const val TRENDING = 4
    const val POPULAR_MOVIES = 5
    const val MOVIE_GENRES = 6
    const val UPCOMING_MOVIES = 7
    const val STUDIOS = 8
    const val POPULAR_TV = 9
    const val TV_GENRES = 10
    const val UPCOMING_TV = 11
    const val NETWORKS = 12
    const val TMDB_MOVIE_KEYWORD = 13
    const val TMDB_TV_KEYWORD = 14
    const val TMDB_MOVIE_GENRE = 15
    const val TMDB_TV_GENRE = 16
    const val TMDB_STUDIO = 17
    const val TMDB_NETWORK = 18
    const val TMDB_SEARCH = 19
    const val TMDB_MOVIE_STREAMING_SERVICES = 20
    const val TMDB_TV_STREAMING_SERVICES = 21
}

// ============================================================================
// Helper Extensions
// ============================================================================

/**
 * Filter out items that are already available, requested, or partially available.
 */
fun List<SeerrMedia>.filterDiscoverable(): List<SeerrMedia> = filter { media ->
    !media.isAvailable &&
        !media.isPending &&
        media.availabilityStatus != SeerrMediaStatus.PARTIALLY_AVAILABLE
}

/**
 * Get a color for a genre based on its ID.
 */
fun SeerrGenre.getColor(): Long = GenreColors.forGenre(id, name)

// ============================================================================
// Color Constants (for UI)
// ============================================================================

/**
 * Predefined colors for common genres.
 */
object GenreColors {
    private val genreColorMap = mapOf(
        // Movie Genres
        28 to 0xFFDC2626L, // Action - Red
        12 to 0xFFF97316L, // Adventure - Orange
        16 to 0xFF3B82F6L, // Animation - Blue
        35 to 0xFFFBBF24L, // Comedy - Yellow
        80 to 0xFF1F2937L, // Crime - Dark Gray
        99 to 0xFF059669L, // Documentary - Green
        18 to 0xFF7C3AEDL, // Drama - Purple
        10751 to 0xFFEC4899L, // Family - Pink
        14 to 0xFF8B5CF6L, // Fantasy - Violet
        36 to 0xFF92400EL, // History - Brown
        27 to 0xFF991B1BL, // Horror - Dark Red
        10402 to 0xFFDB2777L, // Music - Magenta
        9648 to 0xFF4B5563L, // Mystery - Gray
        10749 to 0xFFE11D48L, // Romance - Rose
        878 to 0xFF0EA5E9L, // Science Fiction - Cyan
        10770 to 0xFF6366F1L, // TV Movie - Indigo
        53 to 0xFF78350FL, // Thriller - Amber Dark
        10752 to 0xFF4B5563L, // War - Slate
        37 to 0xFFD97706L, // Western - Amber
        // TV Genres
        10759 to 0xFFDC2626L, // Action & Adventure - Red
        10762 to 0xFFEC4899L, // Kids - Pink
        10763 to 0xFF0D9488L, // News - Teal
        10764 to 0xFF8B5CF6L, // Reality - Violet
        10765 to 0xFF0EA5E9L, // Sci-Fi & Fantasy - Cyan
        10766 to 0xFFDB2777L, // Soap - Magenta
        10767 to 0xFF6366F1L, // Talk - Indigo
        10768 to 0xFF4B5563L, // War & Politics - Slate
    )

    private val fallbackColors = listOf(
        0xFF8B5CF6L, // Purple
        0xFF3B82F6L, // Blue
        0xFF0EA5E9L, // Cyan
        0xFF059669L, // Green
        0xFFFBBF24L, // Yellow
        0xFFF97316L, // Orange
        0xFFDC2626L, // Red
        0xFFEC4899L, // Pink
        0xFF7C3AEDL, // Violet
        0xFF6366F1L, // Indigo
    )

    fun forGenre(id: Int, name: String): Long =
        genreColorMap[id] ?: fallbackColors[name.hashCode().mod(fallbackColors.size)]
}

/**
 * Genre backdrop color tones for duotone filter.
 */
object GenreBackdropColors {
    private val colorTones = mapOf(
        "red" to ("991B1B" to "FCA5A5"),
        "darkred" to ("1F2937" to "F87171"),
        "blue" to ("032541" to "01b4e4"),
        "lightblue" to ("1F2937" to "60A5FA"),
        "darkblue" to ("1F2937" to "2864d2"),
        "orange" to ("92400E" to "FCD34D"),
        "lightgreen" to ("065F46" to "6EE7B7"),
        "green" to ("087d29" to "21cb51"),
        "purple" to ("5B21B6" to "C4B5FD"),
        "yellow" to ("777e0d" to "e4ed55"),
        "darkorange" to ("552c01" to "d47c1d"),
        "black" to ("1F2937" to "D1D5DB"),
        "pink" to ("9D174D" to "F9A8D4"),
        "darkpurple" to ("480c8b" to "a96bef"),
    )

    private val genreColorMap = mapOf(
        28 to "red", 12 to "darkpurple", 16 to "blue", 35 to "orange",
        80 to "darkblue", 99 to "lightgreen", 18 to "pink", 10751 to "yellow",
        14 to "lightblue", 36 to "orange", 27 to "black", 10402 to "blue",
        9648 to "purple", 10749 to "pink", 878 to "lightblue", 10770 to "red",
        53 to "black", 10752 to "darkred", 37 to "orange",
        10759 to "darkpurple", 10762 to "blue", 10763 to "black", 10764 to "darkorange",
        10765 to "lightblue", 10766 to "pink", 10767 to "lightgreen", 10768 to "darkred",
    )

    fun getColorPair(genreId: Int): Pair<String, String> {
        val colorName = genreColorMap[genreId] ?: "black"
        return colorTones[colorName] ?: ("1F2937" to "D1D5DB")
    }

    fun getBackdropUrl(backdropPath: String, genreId: Int): String {
        val (dark, light) = getColorPair(genreId)
        return "https://image.tmdb.org/t/p/w1280_filter(duotone,$dark,$light)$backdropPath"
    }
}

/**
 * Movie studio for browsing.
 */
data class SeerrStudio(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
) {
    fun getColor(): Long = StudioColors.forStudio(id)
}

/**
 * TV network for browsing.
 */
data class SeerrNetwork(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
) {
    fun getColor(): Long = NetworkColors.forNetwork(id)
}

object StudioColors {
    private val studioColorMap = mapOf(
        174 to 0xFF0046BEL, // Warner Bros
        2 to 0xFF0072CEL, // Walt Disney
        420 to 0xFFED1D24L, // Marvel Studios
        33 to 0xFF0033A0L, // Universal
        4 to 0xFF0167B6L, // Paramount
        3 to 0xFFF9F5E3L, // Pixar
        5 to 0xFF002D62L, // Columbia
        7 to 0xFF000000L, // DreamWorks
        25 to 0xFF003087L, // 20th Century Fox
        9993 to 0xFFFFE21FL, // DC Films
    )

    private val fallbackColors = listOf(
        0xFF8B5CF6L, 0xFF3B82F6L, 0xFF0EA5E9L, 0xFF059669L,
        0xFFFBBF24L, 0xFFF97316L, 0xFFDC2626L, 0xFFEC4899L,
    )

    fun forStudio(id: Int): Long = studioColorMap[id] ?: fallbackColors[id.mod(fallbackColors.size)]
}

object NetworkColors {
    private val networkColorMap = mapOf(
        49 to 0xFF000000L, // HBO
        213 to 0xFFE50914L, // Netflix
        2739 to 0xFF00A8E1L, // Disney+
        1024 to 0xFFFF6600L, // Amazon Prime
        2552 to 0xFF6B3FA0L, // Apple TV+
        67 to 0xFF68B244L, // Showtime
        16 to 0xFF003D7CL, // CBS
        6 to 0xFF000000L, // NBC
        19 to 0xFF3D3D3DL, // FOX
        2 to 0xFF014098L, // ABC
    )

    private val fallbackColors = listOf(
        0xFF8B5CF6L, 0xFF3B82F6L, 0xFF0EA5E9L, 0xFF059669L,
        0xFFFBBF24L, 0xFFF97316L, 0xFFDC2626L, 0xFFEC4899L,
    )

    fun forNetwork(id: Int): Long = networkColorMap[id] ?: fallbackColors[id.mod(fallbackColors.size)]
}

/**
 * Popular movie studios for browsing.
 */
object PopularStudios {
    val studios = listOf(
        SeerrStudio(2, "Disney", "/wdrCwmRnLFJhEoH8GSfymY85KHT.png"),
        SeerrStudio(127_928, "20th Century Studios", "/h0rjX5vjW5r8yEnUBStFarjcLT4.png"),
        SeerrStudio(34, "Sony Pictures", "/GagSvqWlyPdkFHMfQ3pNq6ix9P.png"),
        SeerrStudio(174, "Warner Bros. Pictures", "/ky0xOc5OrhzkZ1N6KyUxacfQsCk.png"),
        SeerrStudio(33, "Universal", "/8lvHyhjr8oUKOOy2dKXoALWKdp0.png"),
        SeerrStudio(4, "Paramount", "/fycMZt242LVjagMByZOLUGbCvv3.png"),
        SeerrStudio(3, "Pixar", "/1TjvGVDMYsj6JBxOAkUHpPEwLf7.png"),
        SeerrStudio(521, "DreamWorks", "/kP7t6RwGz2AvvTkvnI1uteEwHet.png"),
        SeerrStudio(420, "Marvel Studios", "/hUzeosd33nzE5MCNsZxCGEKTXaQ.png"),
        SeerrStudio(9993, "DC", "/2Tc1P3Ac8M479naPp1kYT3izLS5.png"),
        SeerrStudio(41_077, "A24", "/1ZXsGaFPgrgS6ZZGS37AqD5uU12.png"),
    )

    fun getLogoUrl(logoPath: String): String =
        "https://image.tmdb.org/t/p/w780_filter(duotone,ffffff,bababa)$logoPath"
}

/**
 * Popular TV networks for browsing.
 */
object PopularNetworks {
    val networks = listOf(
        SeerrNetwork(213, "Netflix", "/wwemzKWzjKYJFfCeiB57q3r4Bcm.png"),
        SeerrNetwork(1024, "Amazon", "/ifhbNuuVnlwYy5oXA5VIb2YR8AZ.png"),
        SeerrNetwork(2739, "Disney+", "/gJ8VX6JSu3ciXHuC2dDGAo2lvwM.png"),
        SeerrNetwork(453, "Hulu", "/pqUTCleNUiTLAVlelGxUgWn1ELh.png"),
        SeerrNetwork(3186, "Max", "/nmU0UMDJB3dRRQSTUqawzF2Od1a.png"),
        SeerrNetwork(2552, "Apple TV+", "/bngHRFi794mnMq34gfVcm9nDxN1.png"),
        SeerrNetwork(4330, "Paramount+", "/fi83B1oztoS47xxcemFdPMhIzK.png"),
        SeerrNetwork(3353, "Peacock", "/gIAcGTjKKr0KOHL5s4O36roJ8p7.png"),
        SeerrNetwork(16, "CBS", "/nm8d7P7MJNiBLdgIzUK0gkuEA4r.png"),
        SeerrNetwork(2, "ABC", "/ndAvF4JLsliGreX87jAc9GdjmJY.png"),
        SeerrNetwork(6, "NBC", "/o3OedEP0f9mfZr33jz2BfXOUK5.png"),
        SeerrNetwork(19, "Fox", "/1DSpHrWyOORkL9N2QHX7Adt31mQ.png"),
        SeerrNetwork(174, "AMC", "/pmvRmATOCaDykE6JrVoeYxlFHw3.png"),
        SeerrNetwork(71, "The CW", "/ge9hzeaU7nMtQ4PjkFlc68dGAJ9.png"),
        SeerrNetwork(88, "FX", "/aexGjtcs42DgRtZh7zOxayiry4J.png"),
    )

    fun getLogoUrl(logoPath: String): String =
        "https://image.tmdb.org/t/p/w780_filter(duotone,ffffff,bababa)$logoPath"
}

// ============================================================================
// API Response Types (used by SeerrClient)
// ============================================================================

/**
 * Server status response.
 */
@Serializable
data class SeerrStatus(
    @SerialName("version") val version: String? = null,
    @SerialName("commitTag") val commitTag: String? = null,
    @SerialName("updateAvailable") val updateAvailable: Boolean = false,
    @SerialName("commitsBehind") val commitsBehind: Int = 0,
)

/**
 * Discover/search result response with pagination.
 */
@Serializable
data class SeerrDiscoverResult(
    @SerialName("page") val page: Int = 1,
    @SerialName("totalPages") val totalPages: Int = 1,
    @SerialName("totalResults") val totalResults: Int = 0,
    @SerialName("results") val results: List<SeerrMedia> = emptyList(),
)

/**
 * Request list result response with pagination.
 */
@Serializable
data class SeerrRequestResult(
    @SerialName("pageInfo") val pageInfo: SeerrPageInfo,
    @SerialName("results") val results: List<SeerrRequest> = emptyList(),
)

/**
 * Create media request body.
 */
@Serializable
data class CreateMediaRequest(
    @SerialName("mediaType") val mediaType: String,
    @SerialName("mediaId") val mediaId: Int,
    @SerialName("tvdbId") val tvdbId: Int? = null,
    @SerialName("seasons") val seasons: List<Int>? = null,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("serverId") val serverId: Int? = null,
    @SerialName("profileId") val profileId: Int? = null,
    @SerialName("rootFolder") val rootFolder: String? = null,
    @SerialName("languageProfileId") val languageProfileId: Int? = null,
    @SerialName("userId") val userId: Int? = null,
)

/**
 * Combined ratings response (Rotten Tomatoes + IMDB).
 */
@Serializable
data class SeerrRatingResponse(
    @SerialName("rt") val rt: SeerrRottenTomatoesRating? = null,
    @SerialName("imdb") val imdb: SeerrImdbRating? = null,
)

/**
 * Quick Connect state response.
 */
@Serializable
data class SeerrQuickConnectState(
    @SerialName("code") val code: String,
    @SerialName("secret") val secret: String,
    @SerialName("authenticated") val authenticated: Boolean = false,
)

/**
 * Quick Connect flow states for UI.
 */
sealed class SeerrQuickConnectFlowState {
    data object Initializing : SeerrQuickConnectFlowState()
    data object NotAvailable : SeerrQuickConnectFlowState()
    data class WaitingForApproval(val code: String, val secret: String) : SeerrQuickConnectFlowState()
    data object Authenticating : SeerrQuickConnectFlowState()
    data class Authenticated(val user: SeerrUser) : SeerrQuickConnectFlowState()
    data class Error(val message: String) : SeerrQuickConnectFlowState()
}

/**
 * User quota information.
 */
@Serializable
data class SeerrUserQuota(
    @SerialName("movie") val movie: SeerrQuotaDetails? = null,
    @SerialName("tv") val tv: SeerrQuotaDetails? = null,
)

/**
 * Request counts by status.
 */
@Serializable
data class SeerrRequestCounts(
    @SerialName("total") val total: Int = 0,
    @SerialName("movie") val movie: Int = 0,
    @SerialName("tv") val tv: Int = 0,
    @SerialName("pending") val pending: Int = 0,
    @SerialName("approved") val approved: Int = 0,
    @SerialName("declined") val declined: Int = 0,
    @SerialName("processing") val processing: Int = 0,
    @SerialName("available") val available: Int = 0,
    @SerialName("completed") val completed: Int = 0,
)

/**
 * Issue counts by type/status.
 */
@Serializable
data class SeerrIssueCounts(
    @SerialName("total") val total: Int = 0,
    @SerialName("video") val video: Int = 0,
    @SerialName("audio") val audio: Int = 0,
    @SerialName("subtitles") val subtitles: Int = 0,
    @SerialName("others") val others: Int = 0,
    @SerialName("open") val open: Int = 0,
    @SerialName("closed") val closed: Int = 0,
)

/**
 * Radarr server configuration (non-sensitive).
 */
@Serializable
data class SeerrRadarrServer(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("isDefault") val isDefault: Boolean = false,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("activeProfileId") val activeProfileId: Int? = null,
    @SerialName("activeProfileName") val activeProfileName: String? = null,
    @SerialName("activeDirectory") val activeDirectory: String? = null,
)

/**
 * Sonarr server configuration (non-sensitive).
 */
@Serializable
data class SeerrSonarrServer(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("isDefault") val isDefault: Boolean = false,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("activeProfileId") val activeProfileId: Int? = null,
    @SerialName("activeProfileName") val activeProfileName: String? = null,
    @SerialName("activeDirectory") val activeDirectory: String? = null,
    @SerialName("activeLanguageProfileId") val activeLanguageProfileId: Int? = null,
    @SerialName("activeAnimeProfileId") val activeAnimeProfileId: Int? = null,
    @SerialName("activeAnimeLanguageProfileId") val activeAnimeLanguageProfileId: Int? = null,
    @SerialName("activeAnimeDirectory") val activeAnimeDirectory: String? = null,
    @SerialName("enableSeasonFolders") val enableSeasonFolders: Boolean = true,
)
