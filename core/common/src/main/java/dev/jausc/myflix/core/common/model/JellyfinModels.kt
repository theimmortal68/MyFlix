package dev.jausc.myflix.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("User") val user: JellyfinUser,
)

@Serializable
data class JellyfinUser(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
)

@Serializable
data class ServerInfo(
    @SerialName("ServerName") val serverName: String,
    @SerialName("Version") val version: String? = null,
    @SerialName("Id") val id: String? = null,
    @SerialName("LocalAddress") val localAddress: String? = null,
)

@Serializable
data class ItemsResponse(
    @SerialName("Items") val items: List<JellyfinItem>,
    @SerialName("TotalRecordCount") val totalRecordCount: Int,
)

@Serializable
data class JellyfinItem(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Type") val type: String,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("ImageTags") val imageTags: ImageTags? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String>? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("SeriesId") val seriesId: String? = null,
    @SerialName("SeasonName") val seasonName: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("CommunityRating") val communityRating: Float? = null,
    @SerialName("CriticRating") val criticRating: Float? = null,
    @SerialName("OfficialRating") val officialRating: String? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("UserData") val userData: UserData? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSource>? = null,
    // CollectionType is returned for library views (e.g., "movies", "tvshows")
    @SerialName("CollectionType") val collectionType: String? = null,
    // PremiereDate for upcoming episodes (ISO 8601 format)
    @SerialName("PremiereDate") val premiereDate: String? = null,
)

@Serializable
data class ImageTags(
    @SerialName("Primary") val primary: String? = null,
    @SerialName("Thumb") val thumb: String? = null,
    @SerialName("Backdrop") val backdrop: String? = null,
)

@Serializable
data class UserData(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0,
    @SerialName("PlayCount") val playCount: Int = 0,
    @SerialName("IsFavorite") val isFavorite: Boolean = false,
    @SerialName("Played") val played: Boolean = false,
)

@Serializable
data class MediaSource(
    @SerialName("Id") val id: String,
    @SerialName("Path") val path: String? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("MediaStreams") val mediaStreams: List<MediaStream>? = null,
)

@Serializable
data class MediaStream(
    @SerialName("Index") val index: Int,
    @SerialName("Type") val type: String,
    @SerialName("Codec") val codec: String? = null,
    @SerialName("Language") val language: String? = null,
    @SerialName("IsDefault") val isDefault: Boolean = false,
    // Video-specific fields for HDR/DV detection
    @SerialName("Profile") val profile: String? = null,
    @SerialName("VideoRange") val videoRange: String? = null,
    @SerialName("VideoRangeType") val videoRangeType: String? = null,
    @SerialName("VideoDoViTitle") val videoDoViTitle: String? = null,
    @SerialName("Width") val width: Int? = null,
    @SerialName("Height") val height: Int? = null,
    @SerialName("BitRate") val bitRate: Long? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    // Audio-specific fields
    @SerialName("Channels") val channels: Int? = null,
)

@Serializable
data class PlaybackInfoResponse(
    @SerialName("MediaSources") val mediaSources: List<MediaSource>,
    @SerialName("PlaySessionId") val playSessionId: String? = null,
)

val JellyfinItem.isMovie: Boolean get() = type == "Movie"
val JellyfinItem.isSeries: Boolean get() = type == "Series"
val JellyfinItem.isEpisode: Boolean get() = type == "Episode"
val JellyfinItem.isSeason: Boolean get() = type == "Season"

/**
 * Check if this is an upcoming episode (episode with future premiere date)
 */
val JellyfinItem.isUpcomingEpisode: Boolean
    get() {
        if (!isEpisode) return false
        val dateStr = premiereDate ?: return false
        return try {
            val instant = java.time.Instant.parse(dateStr)
            val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            localDate.isAfter(java.time.LocalDate.now()) || localDate.isEqual(java.time.LocalDate.now())
        } catch (_: Exception) {
            false
        }
    }

/**
 * Format premiere date as "Mon DD" (e.g., "Jan 15")
 */
val JellyfinItem.formattedPremiereDate: String?
    get() {
        val dateStr = premiereDate ?: return null
        return try {
            val instant = java.time.Instant.parse(dateStr)
            val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
            localDate.format(formatter)
        } catch (_: Exception) {
            null
        }
    }

val JellyfinItem.runtimeMinutes: Int?
    get() = runTimeTicks?.let { (it / 600_000_000).toInt() }

val JellyfinItem.progressPercent: Float
    get() {
        val position = userData?.playbackPositionTicks ?: 0
        val total = runTimeTicks ?: return 0f
        return (position.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

/**
 * Get the primary video stream from this item
 */
val JellyfinItem.videoStream: MediaStream?
    get() = mediaSources?.firstOrNull()?.mediaStreams?.find { it.type == "Video" }

/**
 * Check if this item has Dolby Vision content
 */
val JellyfinItem.isDolbyVision: Boolean
    get() {
        val video = videoStream ?: return false

        // Check VideoRangeType (most reliable - Jellyfin provides this)
        val rangeType = video.videoRangeType?.lowercase() ?: ""
        if (rangeType.contains("dolby") || rangeType.contains("dovi") || rangeType == "dv") {
            return true
        }

        // Check VideoRange
        val range = video.videoRange?.lowercase() ?: ""
        if (range.contains("dolby") || range.contains("dovi")) {
            return true
        }

        // Check DoVi title field
        if (!video.videoDoViTitle.isNullOrBlank()) {
            return true
        }

        // Check profile for DV indicators
        val profile = video.profile?.lowercase() ?: ""
        if (profile.contains("dvhe") || // Dolby Vision HEVC
            profile.contains("dvh1") || // Dolby Vision HEVC (alternate)
            profile.contains("dav1") || // Dolby Vision AV1
            profile.contains("dolby")
        ) {
            return true
        }

        // Check display title (often contains "Dolby Vision" or "DV")
        val displayTitle = video.displayTitle?.lowercase() ?: ""
        if (displayTitle.contains("dolby vision") || displayTitle.contains(" dv ") ||
            displayTitle.contains("/dv/") || displayTitle.contains("dovi")
        ) {
            return true
        }

        return false
    }

/**
 * Check if this item has HDR content (any type)
 */
val JellyfinItem.isHdr: Boolean
    get() {
        val video = videoStream ?: return false
        val rangeType = video.videoRangeType?.lowercase() ?: ""
        val range = video.videoRange?.lowercase() ?: ""

        return rangeType.contains("hdr") ||
            rangeType.contains("dolby") ||
            rangeType.contains("dovi") ||
            rangeType.contains("hlg") ||
            rangeType.contains("pq") ||
            range.contains("hdr") ||
            range.contains("hlg") ||
            isDolbyVision
    }

/**
 * Check if this item is 4K resolution
 */
val JellyfinItem.is4K: Boolean
    get() {
        val video = videoStream ?: return false
        return (video.width ?: 0) >= 3840 || (video.height ?: 0) >= 2160
    }

/**
 * Get a human-readable video quality string
 */
val JellyfinItem.videoQualityLabel: String
    get() {
        val parts = mutableListOf<String>()

        if (is4K) {
            parts.add("4K")
        } else {
            val height = videoStream?.height
            if (height != null) {
                when {
                    height >= 1080 -> parts.add("1080p")
                    height >= 720 -> parts.add("720p")
                    height >= 480 -> parts.add("480p")
                }
            }
        }

        if (isDolbyVision) {
            parts.add("Dolby Vision")
        } else if (isHdr) {
            parts.add("HDR")
        }

        return parts.joinToString(" · ")
    }

/**
 * Genre model for genre-based content rows
 */
@Serializable
data class JellyfinGenre(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
)
