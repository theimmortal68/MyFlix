package dev.jausc.myflix.core.common.model

import dev.jausc.myflix.core.common.util.TickConstants
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
    @SerialName("SeasonId") val seasonId: String? = null,
    @SerialName("ParentId") val parentId: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("CommunityRating") val communityRating: Float? = null,
    @SerialName("CriticRating") val criticRating: Float? = null,
    @SerialName("OfficialRating") val officialRating: String? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("UserData") val userData: UserData? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSource>? = null,
    // CollectionType is returned for library views (e.g., "movies", "tvshows")
    @SerialName("CollectionType") val collectionType: String? = null,
    // PremiereDate for upcoming episodes (ISO 8601 format)
    @SerialName("PremiereDate") val premiereDate: String? = null,
    // People (cast and crew)
    @SerialName("People") val people: List<JellyfinPerson>? = null,
    // Studios (production companies)
    @SerialName("Studios") val studios: List<JellyfinStudio>? = null,
    // Genres as list of strings
    @SerialName("Genres") val genres: List<String>? = null,
    // Taglines
    @SerialName("Taglines") val taglines: List<String>? = null,
    // Tags
    @SerialName("Tags") val tags: List<String>? = null,
    // External IDs
    @SerialName("ProviderIds") val providerIds: Map<String, String>? = null,
    @SerialName("ExternalUrls") val externalUrls: List<ExternalUrl>? = null,
    // Chapters
    @SerialName("Chapters") val chapters: List<JellyfinChapter>? = null,
    // Collections (BoxSets)
    @SerialName("CollectionIds") val collectionIds: List<String>? = null,
    @SerialName("CollectionName") val collectionName: String? = null,
    // Display order for collections (e.g., "PremiereDate", "SortName")
    @SerialName("DisplayOrder") val displayOrder: String? = null,
    // Trailers
    @SerialName("RemoteTrailers") val remoteTrailers: List<JellyfinRemoteTrailer>? = null,
    @SerialName("LocalTrailerCount") val localTrailerCount: Int? = null,
    // Extra type for special features (Featurette, BehindTheScenes, Trailer, etc.)
    @SerialName("ExtraType") val extraType: String? = null,
    // Production locations
    @SerialName("ProductionLocations") val productionLocations: List<String>? = null,
    // Child count (number of seasons for a series, number of episodes for a season)
    @SerialName("ChildCount") val childCount: Int? = null,
    // Recursive item count (total episodes for a series)
    @SerialName("RecursiveItemCount") val recursiveItemCount: Int? = null,
    // Trickplay data for seek preview thumbnails
    // Map<mediaSourceId, Map<width, TrickplayInfo>>
    @SerialName("Trickplay") val trickplay: Map<String, Map<String, TrickplayInfo>>? = null,
)

@Serializable
data class ImageTags(
    @SerialName("Primary") val primary: String? = null,
    @SerialName("Thumb") val thumb: String? = null,
    @SerialName("Backdrop") val backdrop: String? = null,
    @SerialName("Logo") val logo: String? = null,
    @SerialName("Banner") val banner: String? = null,
)

@Serializable
data class UserData(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0,
    @SerialName("PlayCount") val playCount: Int = 0,
    @SerialName("IsFavorite") val isFavorite: Boolean = false,
    @SerialName("Played") val played: Boolean = false,
    // For folders/seasons: number of unwatched child items
    @SerialName("UnplayedItemCount") val unplayedItemCount: Int? = null,
)

@Serializable
data class MediaSource(
    @SerialName("Id") val id: String,
    @SerialName("Path") val path: String? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("Bitrate") val bitrate: Long? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("MediaStreams") val mediaStreams: List<MediaStream>? = null,
    // Playback capability flags (from PlaybackInfo response)
    @SerialName("SupportsDirectPlay") val supportsDirectPlay: Boolean = true,
    @SerialName("SupportsDirectStream") val supportsDirectStream: Boolean = true,
    @SerialName("SupportsTranscoding") val supportsTranscoding: Boolean = true,
    @SerialName("TranscodingUrl") val transcodingUrl: String? = null,
    @SerialName("TranscodeReasons") val transcodeReasons: List<String>? = null,
    @SerialName("LiveStreamId") val liveStreamId: String? = null,
    @SerialName("ETag") val eTag: String? = null,
)

@Serializable
data class MediaStream(
    @SerialName("Index") val index: Int,
    @SerialName("Type") val type: String,
    @SerialName("Codec") val codec: String? = null,
    @SerialName("Language") val language: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("IsDefault") val isDefault: Boolean = false,
    @SerialName("IsForced") val isForced: Boolean = false,
    @SerialName("IsExternal") val isExternal: Boolean = false,
    @SerialName("IsHearingImpaired") val isHearingImpaired: Boolean = false,
    // Video-specific fields for HDR/DV detection
    @SerialName("Profile") val profile: String? = null,
    @SerialName("VideoRange") val videoRange: String? = null,
    @SerialName("VideoRangeType") val videoRangeType: String? = null,
    @SerialName("VideoDoViTitle") val videoDoViTitle: String? = null,
    @SerialName("Width") val width: Int? = null,
    @SerialName("Height") val height: Int? = null,
    @SerialName("BitRate") val bitRate: Long? = null,
    @SerialName("FrameRate") val frameRate: Float? = null,
    @SerialName("AspectRatio") val aspectRatio: String? = null,
    @SerialName("Level") val level: Float? = null,
    @SerialName("PixelFormat") val pixelFormat: String? = null,
    @SerialName("RefFrames") val refFrames: Int? = null,
    @SerialName("NalLengthSize") val nalLengthSize: Int? = null,
    @SerialName("IsInterlaced") val isInterlaced: Boolean? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    // Audio-specific fields
    @SerialName("Channels") val channels: Int? = null,
    @SerialName("ChannelLayout") val channelLayout: String? = null,
    @SerialName("SampleRate") val sampleRate: Int? = null,
    @SerialName("BitDepth") val bitDepth: Int? = null,
)

@Serializable
data class JellyfinChapter(
    @SerialName("StartPositionTicks") val startPositionTicks: Long? = null,
    @SerialName("Name") val name: String? = null,
)

@Serializable
data class JellyfinRemoteTrailer(
    @SerialName("Name") val name: String? = null,
    @SerialName("Url") val url: String? = null,
)

@Serializable
data class PlaybackInfoResponse(
    @SerialName("MediaSources") val mediaSources: List<MediaSource> = emptyList(),
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

/**
 * Format premiere date as "Month DD, YYYY" (e.g., "January 15, 2024")
 */
val JellyfinItem.formattedFullPremiereDate: String?
    get() {
        val dateStr = premiereDate ?: return null
        return try {
            val instant = java.time.Instant.parse(dateStr)
            val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
            localDate.format(formatter)
        } catch (_: Exception) {
            null
        }
    }

val JellyfinItem.runtimeMinutes: Int?
    get() = runTimeTicks?.let { (it / TickConstants.TICKS_PER_MINUTE).toInt() }

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
 * Get a human-readable video quality string (resolution only)
 */
val JellyfinItem.videoQualityLabel: String
    get() {
        if (is4K) return "4K"
        val height = videoStream?.height ?: return ""
        return when {
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            else -> ""
        }
    }

/**
 * Get the primary audio stream from this item
 */
val JellyfinItem.audioStream: MediaStream?
    get() = mediaSources?.firstOrNull()?.mediaStreams?.find { it.type == "Audio" && it.isDefault }
        ?: mediaSources?.firstOrNull()?.mediaStreams?.find { it.type == "Audio" }

/**
 * Get a human-readable video codec label
 */
val JellyfinItem.videoCodecLabel: String
    get() {
        val codec = videoStream?.codec?.lowercase() ?: return ""
        return when {
            codec == "hevc" || codec == "h265" -> "HEVC"
            codec == "av1" -> "AV1"
            codec == "h264" || codec == "avc" -> "H.264"
            codec == "vp9" -> "VP9"
            codec == "mpeg4" -> "MPEG-4"
            codec == "mpeg2video" -> "MPEG-2"
            codec == "vc1" -> "VC-1"
            else -> codec.uppercase()
        }
    }

/**
 * Get a human-readable dynamic range label (HDR type)
 */
val JellyfinItem.videoRangeLabel: String
    get() {
        val video = videoStream ?: return ""
        val rangeType = video.videoRangeType?.lowercase() ?: ""
        val doViTitle = video.videoDoViTitle

        // Check for Dolby Vision first
        if (rangeType.contains("dolby") || rangeType.contains("dovi") || rangeType == "dv") {
            // Include profile info if available (e.g., "DV P8" for profile 8)
            return if (!doViTitle.isNullOrBlank()) {
                "Dolby Vision"
            } else {
                "Dolby Vision"
            }
        }

        // Check for HDR10+
        if (rangeType.contains("hdr10plus") || rangeType.contains("hdr10+")) {
            return "HDR10+"
        }

        // Check for HDR10
        if (rangeType.contains("hdr10") || rangeType == "hdr") {
            return "HDR10"
        }

        // Check for HLG
        if (rangeType.contains("hlg")) {
            return "HLG"
        }

        // Fallback to videoRange field
        val range = video.videoRange?.lowercase() ?: ""
        return when {
            range.contains("hdr") -> "HDR"
            range.contains("hlg") -> "HLG"
            else -> ""
        }
    }

/**
 * Get a human-readable audio codec label
 */
val JellyfinItem.audioCodecLabel: String
    get() {
        val audio = audioStream ?: return ""
        val codec = audio.codec?.lowercase() ?: return ""
        val profile = audio.profile?.lowercase() ?: ""
        val channels = audio.channels ?: 0
        val channelLayout = audio.channelLayout?.lowercase() ?: ""

        // Check for Dolby Atmos (usually in profile or title)
        val isAtmos = profile.contains("atmos") ||
            audio.displayTitle?.lowercase()?.contains("atmos") == true

        // Build codec label
        val codecName = when {
            codec == "truehd" -> if (isAtmos) "TrueHD Atmos" else "TrueHD"
            codec == "eac3" || codec == "ec-3" -> if (isAtmos) "DD+ Atmos" else "DD+"
            codec == "ac3" -> "DD"
            codec == "dts" -> when {
                profile.contains("dts-hd ma") || profile.contains("dts:x") -> "DTS-HD MA"
                profile.contains("dts-hd hra") -> "DTS-HD HRA"
                profile.contains("dts:x") -> "DTS:X"
                else -> "DTS"
            }
            codec == "flac" -> "FLAC"
            codec == "aac" -> "AAC"
            codec == "opus" -> "Opus"
            codec == "vorbis" -> "Vorbis"
            codec == "mp3" -> "MP3"
            codec == "pcm_s16le" || codec == "pcm_s24le" || codec.startsWith("pcm") -> "PCM"
            else -> codec.uppercase()
        }

        // Add channel info for surround formats
        val channelSuffix = when {
            channels >= 8 || channelLayout.contains("7.1") -> "7.1"
            channels >= 6 || channelLayout.contains("5.1") -> "5.1"
            channels == 2 -> "2.0"
            else -> ""
        }

        return if (channelSuffix.isNotEmpty() && !codecName.contains("Atmos")) {
            "$codecName $channelSuffix"
        } else {
            codecName
        }
    }

/**
 * Genre model for genre-based content rows
 */
@Serializable
data class JellyfinGenre(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
)

/**
 * Person model for cast and crew information
 */
@Serializable
data class JellyfinPerson(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String? = null,
    @SerialName("Role") val role: String? = null,
    @SerialName("Type") val type: String = "Unknown",
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
)

/**
 * Studio model for production companies
 */
@Serializable
data class JellyfinStudio(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String? = null,
)

/**
 * External URL model for links to IMDb, TMDb, etc.
 */
@Serializable
data class ExternalUrl(
    @SerialName("Name") val name: String? = null,
    @SerialName("Url") val url: String? = null,
)

/**
 * Get actors from the people list
 */
val JellyfinItem.actors: List<JellyfinPerson>
    get() = people?.filter { it.type == "Actor" } ?: emptyList()

/**
 * Get crew (non-actors) from the people list
 */
val JellyfinItem.crew: List<JellyfinPerson>
    get() = people?.filter { it.type != "Actor" } ?: emptyList()

/**
 * Get directors from the people list
 */
val JellyfinItem.directors: List<JellyfinPerson>
    get() = people?.filter { it.type == "Director" } ?: emptyList()

/**
 * Get director names as a comma-separated string
 */
val JellyfinItem.directorNames: String?
    get() = directors
        .mapNotNull { it.name?.takeIf(String::isNotBlank) }
        .joinToString(", ")
        .takeIf { it.isNotEmpty() }

/**
 * Get writers from the people list
 */
val JellyfinItem.writers: List<JellyfinPerson>
    get() = people?.filter { it.type == "Writer" } ?: emptyList()

/**
 * Get creators/showrunners from the people list (typically for TV series)
 */
val JellyfinItem.creators: List<JellyfinPerson>
    get() = people?.filter { it.type == "Creator" || it.type == "Producer" } ?: emptyList()

val JellyfinItem.imdbId: String?
    get() = providerIds?.get("Imdb") ?: providerIds?.get("IMDB")

val JellyfinItem.tmdbId: String?
    get() = providerIds?.get("Tmdb") ?: providerIds?.get("TMDB")

// ==================== Media Segments ====================

/**
 * Media segment types as defined by Jellyfin API.
 */
enum class MediaSegmentType {
    Unknown,
    Intro,
    Outro,
    Recap,
    Preview,
    Commercial,
    ;

    companion object {
        fun fromString(value: String): MediaSegmentType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: Unknown
    }
}

/**
 * Media segment action types.
 */
enum class MediaSegmentAction {
    None,
    Auto,
    ;

    companion object {
        fun fromString(value: String): MediaSegmentAction =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: None
    }
}

/**
 * A media segment (intro, outro, recap, etc.) for skip functionality.
 */
@Serializable
@Suppress("DataClassContainsFunctions")
data class MediaSegment(
    @SerialName("Id") val id: String? = null,
    @SerialName("ItemId") val itemId: String,
    @SerialName("Type") val typeString: String,
    @SerialName("StartTicks") val startTicks: Long,
    @SerialName("EndTicks") val endTicks: Long,
    @SerialName("Action") val actionString: String? = null,
) {
    val type: MediaSegmentType
        get() = MediaSegmentType.fromString(typeString)

    val action: MediaSegmentAction
        get() = MediaSegmentAction.fromString(actionString ?: "None")

    /** Start position in milliseconds */
    val startMs: Long
        get() = startTicks / 10_000

    /** End position in milliseconds */
    val endMs: Long
        get() = endTicks / 10_000

    /** Duration in milliseconds */
    val durationMs: Long
        get() = endMs - startMs

    /** Check if a position (in ms) is within this segment */
    fun containsPosition(positionMs: Long): Boolean = positionMs in startMs..endMs
}

/**
 * Response wrapper for media segments API.
 */
@Serializable
data class MediaSegmentsResponse(
    @SerialName("Items") val items: List<MediaSegment>,
)

// ==================== Theme Media API Models ====================

/**
 * Response from GET /Items/{itemId}/ThemeMedia.
 * Contains theme songs, theme videos, and soundtrack songs.
 */
@Serializable
data class AllThemeMediaResult(
    @SerialName("ThemeSongsResult") val themeSongsResult: ThemeMediaResult? = null,
    @SerialName("ThemeVideosResult") val themeVideosResult: ThemeMediaResult? = null,
    @SerialName("SoundtrackSongsResult") val soundtrackSongsResult: ThemeMediaResult? = null,
)

/**
 * Theme media result containing items and metadata.
 */
@Serializable
data class ThemeMediaResult(
    @SerialName("Items") val items: List<JellyfinItem> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0,
    @SerialName("StartIndex") val startIndex: Int = 0,
    @SerialName("OwnerId") val ownerId: String? = null,
)

// ==================== Playback Info API Models ====================

/**
 * Request body for POST /Items/{itemId}/PlaybackInfo
 * Used to get proper streaming URLs with transcoding support.
 */
@Serializable
data class PlaybackInfoRequest(
    @SerialName("MediaSourceId") val mediaSourceId: String? = null,
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Int? = null,
    @SerialName("StartTimeTicks") val startTimeTicks: Long? = null,
    @SerialName("AudioStreamIndex") val audioStreamIndex: Int? = null,
    @SerialName("SubtitleStreamIndex") val subtitleStreamIndex: Int? = null,
    @SerialName("EnableDirectPlay") val enableDirectPlay: Boolean = true,
    @SerialName("EnableDirectStream") val enableDirectStream: Boolean = true,
    @SerialName("EnableTranscoding") val enableTranscoding: Boolean = true,
    @SerialName("AllowVideoStreamCopy") val allowVideoStreamCopy: Boolean = true,
    @SerialName("AllowAudioStreamCopy") val allowAudioStreamCopy: Boolean = true,
    @SerialName("EnableAutoStreamCopy") val enableAutoStreamCopy: Boolean = true,
    @SerialName("AutoOpenLiveStream") val autoOpenLiveStream: Boolean = true,
    @SerialName("DeviceProfile") val deviceProfile: DeviceProfile? = null,
)

/**
 * Device profile that tells the server what codecs and formats the client supports.
 * The server uses this to decide whether to direct play or transcode.
 */
@Serializable
data class DeviceProfile(
    @SerialName("Name") val name: String = "MyFlix-Android",
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Int? = null,
    @SerialName("DirectPlayProfiles") val directPlayProfiles: List<DirectPlayProfile> = emptyList(),
    @SerialName("TranscodingProfiles") val transcodingProfiles: List<TranscodingProfile> = emptyList(),
    @SerialName("SubtitleProfiles") val subtitleProfiles: List<SubtitleProfile> = emptyList(),
    @SerialName("CodecProfiles") val codecProfiles: List<CodecProfile> = emptyList(),
)

/**
 * Defines what formats/codecs can be played directly without transcoding.
 */
@Serializable
data class DirectPlayProfile(
    @SerialName("Type") val type: String = "Video",
    @SerialName("Container") val container: String? = null,
    @SerialName("VideoCodec") val videoCodec: String? = null,
    @SerialName("AudioCodec") val audioCodec: String? = null,
)

/**
 * Defines transcode targets when direct play is not possible.
 */
@Serializable
data class TranscodingProfile(
    @SerialName("Type") val type: String = "Video",
    @SerialName("Container") val container: String = "ts",
    @SerialName("VideoCodec") val videoCodec: String = "h264",
    @SerialName("AudioCodec") val audioCodec: String = "aac,mp3",
    @SerialName("Protocol") val protocol: String = "hls",
    @SerialName("Context") val context: String = "Streaming",
    @SerialName("MaxAudioChannels") val maxAudioChannels: Int? = null,
    @SerialName("CopyTimestamps") val copyTimestamps: Boolean = false,
    @SerialName("EnableSubtitlesInManifest") val enableSubtitlesInManifest: Boolean = true,
)

/**
 * Defines subtitle format support.
 */
@Serializable
data class SubtitleProfile(
    @SerialName("Format") val format: String,
    @SerialName("Method") val method: String,
)

/**
 * Defines codec-specific conditions for direct play.
 * Used to limit what codecs can be played based on properties like bit depth, resolution, etc.
 */
@Serializable
data class CodecProfile(
    @SerialName("Type") val type: String = "Video",
    @SerialName("Codec") val codec: String? = null,
    @SerialName("Conditions") val conditions: List<ProfileCondition> = emptyList(),
)

/**
 * A condition that must be met for a codec to be considered playable.
 */
@Serializable
data class ProfileCondition(
    @SerialName("Condition") val condition: String,
    @SerialName("Property") val property: String,
    @SerialName("Value") val value: String,
    @SerialName("IsRequired") val isRequired: Boolean = true,
)

// ==================== Trickplay (Seek Preview) ====================

/**
 * Trickplay info for seek preview thumbnails.
 * Jellyfin generates trickplay images as tile grids (e.g., 10x10 = 100 thumbnails per image).
 */
@Serializable
data class TrickplayInfo(
    @SerialName("Width") val width: Int,
    @SerialName("Height") val height: Int,
    @SerialName("TileWidth") val tileWidth: Int,
    @SerialName("TileHeight") val tileHeight: Int,
    @SerialName("ThumbnailCount") val thumbnailCount: Int,
    @SerialName("Interval") val interval: Int,
) {
    /** Number of thumbnails per tile grid image */
    val thumbnailsPerTile: Int get() = tileWidth * tileHeight
}
