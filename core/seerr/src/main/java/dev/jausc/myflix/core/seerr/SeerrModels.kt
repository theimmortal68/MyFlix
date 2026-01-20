package dev.jausc.myflix.core.seerr

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
    val profilePath: String? = null,
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
    // Content ratings - movies use releases, TV uses contentRatings
    val releases: SeerrReleases? = null,
    val contentRatings: SeerrContentRatings? = null,
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

    /** Content rating (e.g., "PG-13", "R", "TV-MA") - prefers US rating */
    val contentRating: String?
        get() {
            // For movies: check releases
            releases?.results?.let { releaseResults ->
                // Prefer US certification
                val usRelease = releaseResults.find { it.iso31661 == "US" }
                usRelease?.releaseDates?.firstOrNull { !it.certification.isNullOrBlank() }
                    ?.certification?.let { return it }
                // Fallback to first available certification
                releaseResults.flatMap { it.releaseDates ?: emptyList() }
                    .firstOrNull { !it.certification.isNullOrBlank() }
                    ?.certification?.let { return it }
            }
            // For TV: check contentRatings
            contentRatings?.results?.let { ratings ->
                // Prefer US rating
                ratings.find { it.iso31661 == "US" }?.rating?.let { return it }
                // Fallback to first available rating
                ratings.firstOrNull { !it.rating.isNullOrBlank() }?.rating?.let { return it }
            }
            return null
        }
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
 * Collection details from Seerr.
 */
@Serializable
data class SeerrCollection(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val parts: List<SeerrMedia> = emptyList(),
)

/**
 * Discover slider definitions from Seerr settings.
 */
@Serializable
data class SeerrDiscoverSlider(
    val id: Int,
    val type: SeerrDiscoverSliderType,
    val title: String? = null,
    val data: String? = null,
    val isDefault: Boolean? = null,
    val isEnabled: Boolean? = null,
)

/**
 * Discover slider types supported by Seerr.
 */
@Serializable
enum class SeerrDiscoverSliderType {
    @SerialName("recentlyAdded")
    RECENTLY_ADDED,
    @SerialName("recentRequests")
    RECENT_REQUESTS,
    @SerialName("plexWatchlist")
    PLEX_WATCHLIST,
    @SerialName("trending")
    TRENDING,
    @SerialName("popularMovies")
    POPULAR_MOVIES,
    @SerialName("movieGenres")
    MOVIE_GENRES,
    @SerialName("upcomingMovies")
    UPCOMING_MOVIES,
    @SerialName("studios")
    STUDIOS,
    @SerialName("popularTV")
    POPULAR_TV,
    @SerialName("tvGenres")
    TV_GENRES,
    @SerialName("upcomingTV")
    UPCOMING_TV,
    @SerialName("networks")
    NETWORKS,
    @SerialName("tmdbMovieKeyword")
    TMDB_MOVIE_KEYWORD,
    @SerialName("tmdbTvKeyword")
    TMDB_TV_KEYWORD,
    @SerialName("tmdbMovieGenre")
    TMDB_MOVIE_GENRE,
    @SerialName("tmdbTvGenre")
    TMDB_TV_GENRE,
    @SerialName("tmdbStudio")
    TMDB_STUDIO,
    @SerialName("tmdbNetwork")
    TMDB_NETWORK,
    @SerialName("tmdbSearch")
    TMDB_SEARCH,
    @SerialName("tmdbMovieStreamingServices")
    TMDB_MOVIE_STREAMING_SERVICES,
    @SerialName("tmdbTvStreamingServices")
    TMDB_TV_STREAMING_SERVICES,
}

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
    val backdrops: List<String>? = null, // Backdrop image paths from Seerr API
)

/**
 * Get a color for a genre based on its ID.
 * Returns a consistent color for each genre.
 */
fun SeerrGenre.getColor(): Long {
    return GenreColors.forGenre(id, name)
}

/**
 * Predefined colors for common genres.
 */
object GenreColors {
    // Movie/TV genre colors (TMDB genre IDs)
    private val genreColorMap = mapOf(
        // Movie Genres
        28 to 0xFFDC2626L,   // Action - Red
        12 to 0xFFF97316L,   // Adventure - Orange
        16 to 0xFF3B82F6L,   // Animation - Blue
        35 to 0xFFFBBF24L,   // Comedy - Yellow
        80 to 0xFF1F2937L,   // Crime - Dark Gray
        99 to 0xFF059669L,   // Documentary - Green
        18 to 0xFF7C3AEDL,   // Drama - Purple
        10751 to 0xFFEC4899L, // Family - Pink
        14 to 0xFF8B5CF6L,   // Fantasy - Violet
        36 to 0xFF92400EL,   // History - Brown
        27 to 0xFF991B1BL,   // Horror - Dark Red
        10402 to 0xFFDB2777L, // Music - Magenta
        9648 to 0xFF4B5563L,  // Mystery - Gray
        10749 to 0xFFE11D48L, // Romance - Rose
        878 to 0xFF0EA5E9L,   // Science Fiction - Cyan
        10770 to 0xFF6366F1L, // TV Movie - Indigo
        53 to 0xFF78350FL,    // Thriller - Amber Dark
        10752 to 0xFF4B5563L, // War - Slate
        37 to 0xFFD97706L,    // Western - Amber
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

    // Fallback colors based on name hash
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

    fun forGenre(id: Int, name: String): Long {
        return genreColorMap[id] ?: fallbackColors[name.hashCode().mod(fallbackColors.size)]
    }
}

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
// Person Types
// ============================================================================

/**
 * Person (actor/crew) details from Seerr.
 */
@Serializable
data class SeerrPerson(
    val id: Int,
    val name: String,
    val biography: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    val placeOfBirth: String? = null,
    val profilePath: String? = null,
    val knownForDepartment: String? = null,
    val alsoKnownAs: List<String>? = null,
    val combinedCredits: SeerrPersonCredits? = null,
) {
    /**
     * Formatted "Also known as" text (first 3 names).
     * Returns null if no alternate names.
     */
    val formattedAlsoKnownAs: String?
        get() = alsoKnownAs?.takeIf { it.isNotEmpty() }?.let { names ->
            "Also known as: ${names.take(3).joinToString(", ")}"
        }
}

/**
 * Person's combined credits (movies and TV shows they appeared in).
 */
@Serializable
data class SeerrPersonCredits(
    val cast: List<SeerrPersonCastCredit>? = null,
    val crew: List<SeerrPersonCrewCredit>? = null,
) {
    /**
     * Cast credits sorted by vote average (highest rated first).
     */
    val sortedCast: List<SeerrPersonCastCredit>
        get() = cast?.sortedByDescending { it.voteAverage ?: 0.0 } ?: emptyList()
}

/**
 * A cast credit for a person (a movie/show they acted in).
 */
@Serializable
data class SeerrPersonCastCredit(
    val id: Int,
    val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    val character: String? = null,
    val posterPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Double? = null,
) {
    val displayTitle: String
        get() = title ?: name ?: "Unknown"
    val displayDate: String?
        get() = releaseDate ?: firstAirDate
    val year: Int?
        get() = displayDate?.take(4)?.toIntOrNull()
}

/**
 * A crew credit for a person (a movie/show they worked on).
 */
@Serializable
data class SeerrPersonCrewCredit(
    val id: Int,
    val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    val job: String? = null,
    val department: String? = null,
    val posterPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
) {
    val displayTitle: String
        get() = title ?: name ?: "Unknown"
}

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

// ============================================================================
// Content Rating Types (for movies and TV)
// ============================================================================

/**
 * Movie releases container (contains release dates with certifications).
 */
@Serializable
data class SeerrReleases(
    val results: List<SeerrReleaseCountry>? = null,
)

/**
 * Release info per country.
 */
@Serializable
data class SeerrReleaseCountry(
    @SerialName("iso_3166_1")
    val iso31661: String? = null,
    @SerialName("release_dates")
    val releaseDates: List<SeerrReleaseDate>? = null,
)

/**
 * Individual release date with certification.
 */
@Serializable
data class SeerrReleaseDate(
    val certification: String? = null,
    @SerialName("iso_639_1")
    val iso6391: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    val type: Int? = null, // 1=Premiere, 2=Theatrical (limited), 3=Theatrical, etc.
)

/**
 * TV content ratings container.
 */
@Serializable
data class SeerrContentRatings(
    val results: List<SeerrContentRating>? = null,
)

/**
 * Content rating per country.
 */
@Serializable
data class SeerrContentRating(
    @SerialName("iso_3166_1")
    val iso31661: String? = null,
    val rating: String? = null,
)

// ============================================================================
// External Ratings (Rotten Tomatoes, IMDB)
// ============================================================================

/**
 * Combined ratings response for movies.
 * Returned by /api/v1/movie/{id}/ratingscombined
 */
@Serializable
data class SeerrRatingResponse(
    val rt: SeerrRottenTomatoesRating? = null,
    val imdb: SeerrImdbRating? = null,
)

/**
 * Rotten Tomatoes ratings.
 */
@Serializable
data class SeerrRottenTomatoesRating(
    val criticsRating: String? = null, // "Fresh", "Rotten", "Certified Fresh"
    val criticsScore: Int? = null,
    val audienceRating: String? = null, // "Upright", "Spilled"
    val audienceScore: Int? = null,
    val url: String? = null,
) {
    /** Whether critics rating is "Fresh" or "Certified Fresh" */
    val isCriticsFresh: Boolean
        get() = criticsRating == "Fresh" || criticsRating == "Certified Fresh"

    /** Whether audience rating is positive ("Upright") */
    val isAudienceFresh: Boolean
        get() = audienceRating == "Upright"
}

/**
 * IMDB rating.
 */
@Serializable
data class SeerrImdbRating(
    val criticsScore: Double? = null, // IMDB score (0-10 scale)
    val criticsScoreCount: Int? = null, // Number of votes
)

// ============================================================================
// Filter Extensions
// ============================================================================

/**
 * Filter out items that are already available, requested, or partially available.
 * Used in discover/browse screens to show only requestable items.
 */
fun List<SeerrMedia>.filterDiscoverable(): List<SeerrMedia> = filter { media ->
    !media.isAvailable &&
        !media.isPending &&
        media.availabilityStatus != SeerrMediaStatus.PARTIALLY_AVAILABLE
}

// ============================================================================
// Studio & Network Types
// ============================================================================

/**
 * Movie studio (production company) for browsing.
 * Uses TMDB production company IDs.
 */
data class SeerrStudio(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
) {
    /** Get a color for this studio based on its ID */
    fun getColor(): Long = StudioColors.forStudio(id)
}

/**
 * TV network for browsing.
 * Uses TMDB network IDs.
 */
data class SeerrNetwork(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
) {
    /** Get a color for this network based on its ID */
    fun getColor(): Long = NetworkColors.forNetwork(id)
}

/**
 * Predefined colors for popular studios.
 */
object StudioColors {
    // Studio colors mapped by TMDB company ID
    private val studioColorMap = mapOf(
        174 to 0xFF0046BEL,    // Warner Bros - Blue
        2 to 0xFF0072CEL,      // Walt Disney Pictures - Blue
        420 to 0xFFED1D24L,    // Marvel Studios - Red
        33 to 0xFF0033A0L,     // Universal Pictures - Blue
        4 to 0xFF0167B6L,      // Paramount Pictures - Blue
        3 to 0xFFF9F5E3L,      // Pixar - Off White
        5 to 0xFF002D62L,      // Columbia Pictures - Navy
        7 to 0xFF000000L,      // DreamWorks - Black
        25 to 0xFF003087L,     // 20th Century Fox - Blue
        9993 to 0xFFFFE21FL,   // DC Films - Yellow
        21 to 0xFF1F73B8L,     // Metro-Goldwyn-Mayer - Blue
        12 to 0xFF000000L,     // New Line Cinema - Black
        429 to 0xFFFA7D00L,    // Lucasfilm - Orange
        34 to 0xFF0165E1L,     // Sony Pictures - Blue
        1632 to 0xFF000000L,   // Lionsgate - Black
        923 to 0xFF8B5CF6L,    // Legendary - Purple
        11461 to 0xFFDC2626L,  // Bad Robot - Red
        82968 to 0xFF000000L,  // Apple Studios - Black
        127928 to 0xFF000000L, // A24 - Black
        2785 to 0xFF6366F1L,   // Castle Rock - Indigo
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
    )

    fun forStudio(id: Int): Long {
        return studioColorMap[id] ?: fallbackColors[id.mod(fallbackColors.size)]
    }
}

/**
 * Predefined colors for popular networks.
 */
object NetworkColors {
    // Network colors mapped by TMDB network ID
    private val networkColorMap = mapOf(
        49 to 0xFF000000L,     // HBO - Black
        213 to 0xFFE50914L,    // Netflix - Red
        2739 to 0xFF00A8E1L,   // Disney+ - Blue
        1024 to 0xFFFF6600L,   // Amazon Prime - Orange
        2552 to 0xFF6B3FA0L,   // Apple TV+ - Purple
        67 to 0xFF68B244L,     // Showtime - Green
        16 to 0xFF003D7CL,     // CBS - Blue
        6 to 0xFF000000L,      // NBC - Black
        19 to 0xFF3D3D3DL,     // FOX - Gray
        2 to 0xFF014098L,      // ABC - Blue
        174 to 0xFFCD1C1EL,    // AMC - Red
        54 to 0xFFF7B32BL,     // Disney Channel - Yellow
        88 to 0xFF019934L,     // FX - Green
        453 to 0xFF004696L,    // Hulu - Green
        4330 to 0xFF5C16C5L,   // Paramount+ - Purple
        56 to 0xFFD81B60L,     // Cartoon Network - Pink
        318 to 0xFFF5B51DL,    // Starz - Gold
        359 to 0xFF97D700L,    // Syfy - Green
        21 to 0xFF000000L,     // The WB - Black
        65 to 0xFF0080FFL,     // History - Blue
        33 to 0xFF0073D4L,     // Peacock - Blue
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
    )

    fun forNetwork(id: Int): Long {
        return networkColorMap[id] ?: fallbackColors[id.mod(fallbackColors.size)]
    }
}

/**
 * Popular movie studios for browsing.
 * TMDB production company IDs with logo paths from TMDb.
 */
object PopularStudios {
    val studios = listOf(
        SeerrStudio(2, "Disney", "/wdrCwmRnLFJhEoH8GSfymY85KHT.png"),
        SeerrStudio(127928, "20th Century Studios", "/h0rjX5vjW5r8yEnUBStFarjcLT4.png"),
        SeerrStudio(34, "Sony Pictures", "/GagSvqWlyPdkFHMfQ3pNq6ix9P.png"),
        SeerrStudio(174, "Warner Bros. Pictures", "/ky0xOc5OrhzkZ1N6KyUxacfQsCk.png"),
        SeerrStudio(33, "Universal", "/8lvHyhjr8oUKOOy2dKXoALWKdp0.png"),
        SeerrStudio(4, "Paramount", "/fycMZt242LVjagMByZOLUGbCvv3.png"),
        SeerrStudio(3, "Pixar", "/1TjvGVDMYsj6JBxOAkUHpPEwLf7.png"),
        SeerrStudio(521, "DreamWorks", "/kP7t6RwGz2AvvTkvnI1uteEwHet.png"),
        SeerrStudio(420, "Marvel Studios", "/hUzeosd33nzE5MCNsZxCGEKTXaQ.png"),
        SeerrStudio(9993, "DC", "/2Tc1P3Ac8M479naPp1kYT3izLS5.png"),
        SeerrStudio(41077, "A24", "/1ZXsGaFPgrgS6ZZGS37AqD5uU12.png"),
    )

    /** Build the image URL for a studio logo */
    fun getLogoUrl(logoPath: String): String =
        "https://image.tmdb.org/t/p/w300$logoPath"
}

/**
 * Popular TV networks for browsing.
 * TMDB network IDs with logo paths from TMDb.
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
        SeerrNetwork(41, "TNT", "/6ISsKwa2XUhSC6oBtHZjYf6xFqv.png"),
        SeerrNetwork(30, "USA Network", "/g1e0H0Ka97IG5SyInMXdJkHGKiH.png"),
        SeerrNetwork(68, "TBS", "/65r0kR6MfOBYF0gEQsJGM6v5fEG.png"),
        SeerrNetwork(80, "Adult Swim", "/9AKyspxVzywuaMuZ1Bvilu8sXly.png"),
        SeerrNetwork(56, "Cartoon Network", "/c5OC6oVCg6QP4eqzW6XIq17CQjI.png"),
        SeerrNetwork(4, "BBC One", "/mVn7xESaTNmjBUyUtGNvDQd3CT1.png"),
        SeerrNetwork(14, "PBS", "/4Fn4eQmEmJZ9YWjiIhZ6cF1QHAi.png"),
    )

    /** Build the image URL for a network logo */
    fun getLogoUrl(logoPath: String): String =
        "https://image.tmdb.org/t/p/w300$logoPath"
}

/**
 * Genre backdrop color tones for duotone filter.
 * Maps genre IDs to color pairs [dark, light] for the TMDB duotone filter.
 * URL format: https://image.tmdb.org/t/p/w1280_filter(duotone,{dark},{light}){backdropPath}
 */
object GenreBackdropColors {
    // Color tone pairs [dark color, light color] for duotone filter
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

    // Genre ID to color tone name mapping (from seerr source)
    private val genreColorMap = mapOf(
        // Movie Genres
        28 to "red",           // Action
        12 to "darkpurple",    // Adventure
        16 to "blue",          // Animation
        35 to "orange",        // Comedy
        80 to "darkblue",      // Crime
        99 to "lightgreen",    // Documentary
        18 to "pink",          // Drama
        10751 to "yellow",     // Family
        14 to "lightblue",     // Fantasy
        36 to "orange",        // History
        27 to "black",         // Horror
        10402 to "blue",       // Music
        9648 to "purple",      // Mystery
        10749 to "pink",       // Romance
        878 to "lightblue",    // Science Fiction
        10770 to "red",        // TV Movie
        53 to "black",         // Thriller
        10752 to "darkred",    // War
        37 to "orange",        // Western
        // TV Genres
        10759 to "darkpurple", // Action & Adventure
        10762 to "blue",       // Kids
        10763 to "black",      // News
        10764 to "darkorange", // Reality
        10765 to "lightblue",  // Sci-Fi & Fantasy
        10766 to "pink",       // Soap
        10767 to "lightgreen", // Talk
        10768 to "darkred",    // War & Politics
    )

    /**
     * Get duotone color pair for a genre.
     * Returns pair of (darkColor, lightColor) hex strings without # prefix.
     */
    fun getColorPair(genreId: Int): Pair<String, String> {
        val colorName = genreColorMap[genreId] ?: "black"
        return colorTones[colorName] ?: ("1F2937" to "D1D5DB")
    }

    /**
     * Build duotone filtered backdrop URL for a genre.
     */
    fun getBackdropUrl(backdropPath: String, genreId: Int): String {
        val (dark, light) = getColorPair(genreId)
        return "https://image.tmdb.org/t/p/w1280_filter(duotone,$dark,$light)$backdropPath"
    }
}
