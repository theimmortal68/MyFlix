@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jausc.myflix.core.common.model.BadgeType
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.ui.components.detail.ColoredMediaBadge
import dev.jausc.myflix.core.common.ui.components.detail.Dot as SharedDot
import dev.jausc.myflix.core.common.ui.components.detail.DotSeparatedRow as SharedDotSeparatedRow
import dev.jausc.myflix.core.common.ui.components.detail.MovieQuickDetails as SharedMovieQuickDetails
import dev.jausc.myflix.core.common.ui.components.detail.SeriesQuickDetails as SharedSeriesQuickDetails
import dev.jausc.myflix.core.common.ui.components.detail.SimpleStarRating as SharedSimpleStarRating
import dev.jausc.myflix.core.common.util.MediaBadgeUtil
import dev.jausc.myflix.mobile.R

/**
 * Star icon for ratings using Mobile Material Icon.
 */
@Composable
private fun MobileStarIcon() {
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = Color(0xFFFFD700), // Gold color
        modifier = Modifier.size(14.dp),
    )
}

/**
 * Dot-separated row of metadata text with optional community rating.
 * Mobile wrapper with theme defaults.
 */
@Composable
fun DotSeparatedRow(
    texts: List<String>,
    modifier: Modifier = Modifier,
    communityRating: Float? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    SharedDotSeparatedRow(
        texts = texts,
        modifier = modifier,
        communityRating = communityRating,
        textStyle = textStyle,
        textColor = MaterialTheme.colorScheme.onSurface,
        dotColor = MaterialTheme.colorScheme.onSurface,
        dotAlpha = 0.6f,
        starIcon = { MobileStarIcon() },
    )
}

/**
 * Small dot separator for metadata rows.
 */
@Composable
fun Dot(modifier: Modifier = Modifier) {
    SharedDot(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        alpha = 0.6f,
    )
}

/**
 * Simple star rating display with icon and value.
 */
@Composable
fun SimpleStarRating(
    communityRating: Float,
    modifier: Modifier = Modifier,
) {
    SharedSimpleStarRating(
        communityRating = communityRating,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium,
        textColor = MaterialTheme.colorScheme.onSurface,
        starIcon = { MobileStarIcon() },
    )
}

/**
 * Quick details row for movies: year, runtime, "ends at", rating.
 */
@Composable
fun MovieQuickDetails(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    SharedMovieQuickDetails(
        item = item,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium,
        textColor = MaterialTheme.colorScheme.onSurface,
        dotColor = MaterialTheme.colorScheme.onSurface,
        dotAlpha = 0.6f,
        starIcon = { MobileStarIcon() },
    )
}

/**
 * Quick details row for series: premiere year, official rating.
 */
@Composable
fun SeriesQuickDetails(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
    status: String? = null,
    studios: List<String> = emptyList(),
) {
    SharedSeriesQuickDetails(
        item = item,
        modifier = modifier,
        status = status,
        studios = studios,
        textStyle = MaterialTheme.typography.bodyMedium,
        textColor = MaterialTheme.colorScheme.onSurface,
        dotColor = MaterialTheme.colorScheme.onSurface,
        dotAlpha = 0.6f,
        starIcon = { MobileStarIcon() },
        badgeContent = { text -> MetadataBadge(text = text) },
    )
}

/**
 * Metadata badge with Mobile-specific styling.
 */
@Composable
private fun MetadataBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Row of media badges using image badges for resolution/HDR and audio codec.
 * Falls back to text badges for video codec, audio channels, and edition.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaBadgesRow(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    val mediaSource = item.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams.orEmpty()
    val videoStream = mediaStreams.firstOrNull { it.type == "Video" }
    val audioStream = mediaStreams.firstOrNull { it.type == "Audio" && it.isDefault }
        ?: mediaStreams.firstOrNull { it.type == "Audio" }

    // Get resolution and HDR info for combined image badge
    val resolutionImageRes = remember(videoStream) {
        getResolutionImageResource(videoStream)
    }

    // Get audio codec image resource
    val audioImageRes = remember(audioStream) {
        getAudioCodecImageResource(audioStream)
    }

    // Get edition image resource (check mediaSource path, name, and tags)
    val itemPath = mediaSource?.path
    val editionImageRes = remember(item.name, item.tags, itemPath) {
        getEditionImageResource(item.name, item.tags, itemPath)
    }

    // Get remaining text badges (video codec, audio channels)
    val textBadges = remember(videoStream, audioStream, item.name, item.tags) {
        MediaBadgeUtil.buildMediaBadges(videoStream, audioStream, item.name, item.tags)
            .filter { it.type == BadgeType.VIDEO_CODEC || it.type == BadgeType.AUDIO_CHANNELS }
    }

    val hasContent = resolutionImageRes != null || audioImageRes != null || editionImageRes != null || textBadges.isNotEmpty()
    if (!hasContent) return

    val textStyle = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        // Resolution + HDR/DV combined image badge
        resolutionImageRes?.let { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Resolution",
                modifier = Modifier.height(12.dp),
                contentScale = ContentScale.Fit,
            )
        }

        // Video codec text badge
        textBadges.find { it.type == BadgeType.VIDEO_CODEC }?.let { badge ->
            ColoredMediaBadge(text = badge.text, type = badge.type, textStyle = textStyle)
        }

        // Audio codec image badge
        audioImageRes?.let { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Audio",
                modifier = Modifier.height(12.dp),
                contentScale = ContentScale.Fit,
            )
        }

        // Audio channels text badge
        textBadges.find { it.type == BadgeType.AUDIO_CHANNELS }?.let { badge ->
            ColoredMediaBadge(text = badge.text, type = badge.type, textStyle = textStyle)
        }

        // Edition image badge
        editionImageRes?.let { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Edition",
                modifier = Modifier.height(12.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/**
 * Get the combined resolution + HDR/DV image resource.
 */
private fun getResolutionImageResource(videoStream: MediaStream?): Int? {
    if (videoStream == null) return null

    val width = videoStream.width ?: 0
    val height = videoStream.height ?: 0

    // Determine resolution
    val resolution = when {
        width >= 3840 || height >= 2160 -> "4k"
        height >= 1080 -> "1080p"
        height >= 720 -> "720p"
        height >= 576 -> "576p"
        height >= 480 -> "480p"
        else -> return null
    }

    // Determine HDR type
    val rangeType = videoStream.videoRangeType?.lowercase() ?: ""
    val range = videoStream.videoRange?.lowercase() ?: ""
    val profile = videoStream.profile?.lowercase() ?: ""

    val isDolbyVision = rangeType.contains("dolby") || rangeType.contains("dovi") ||
        profile.contains("dvhe") || profile.contains("dvh1") ||
        !videoStream.videoDoViTitle.isNullOrBlank()

    val isHdr = rangeType.contains("hdr") || range.contains("hdr")

    // Return combined image resource
    return when {
        isDolbyVision && isHdr -> when (resolution) {
            "4k" -> R.drawable.badge_4k_dv_hdr
            "1080p" -> R.drawable.badge_1080p_dv_hdr
            else -> R.drawable.badge_dv_hdr
        }
        isDolbyVision -> when (resolution) {
            "4k" -> R.drawable.badge_4k_dv
            "1080p" -> R.drawable.badge_1080p_dv
            "720p" -> R.drawable.badge_720p_dv
            else -> R.drawable.badge_dv
        }
        isHdr -> when (resolution) {
            "4k" -> R.drawable.badge_4k_hdr
            "1080p" -> R.drawable.badge_1080p_hdr
            "720p" -> R.drawable.badge_720p_hdr
            else -> R.drawable.badge_hdr
        }
        else -> when (resolution) {
            "4k" -> R.drawable.badge_4k
            "1080p" -> R.drawable.badge_1080p
            "720p" -> R.drawable.badge_720p
            "576p" -> R.drawable.badge_576p
            "480p" -> R.drawable.badge_480p
            else -> null
        }
    }
}

/**
 * Get the audio codec image resource.
 */
private fun getAudioCodecImageResource(audioStream: MediaStream?): Int? {
    if (audioStream == null) return null

    val codec = audioStream.codec?.uppercase() ?: ""
    val title = audioStream.title?.lowercase() ?: ""
    val displayTitle = audioStream.displayTitle?.lowercase() ?: ""

    return when {
        // Atmos variants
        (title.contains("atmos") || displayTitle.contains("atmos")) && codec.contains("TRUEHD") ->
            R.drawable.badge_truehd_atmos
        (title.contains("atmos") || displayTitle.contains("atmos")) && (codec.contains("EAC3") || codec.contains("E-AC-3")) ->
            R.drawable.badge_eac3_atmos
        title.contains("atmos") || displayTitle.contains("atmos") ->
            R.drawable.badge_dolby_atmos

        // DTS variants
        title.contains("dts:x") || displayTitle.contains("dts:x") ||
            title.contains("dts-x") || displayTitle.contains("dts-x") ->
                R.drawable.badge_dtsx
        codec.contains("DTS") && (title.contains("hd ma") || displayTitle.contains("hd ma") ||
            title.contains("hd-ma") || displayTitle.contains("hd-ma")) ->
                R.drawable.badge_dts_hdma
        codec.contains("DTS") && (title.contains("hra") || displayTitle.contains("hra") ||
            title.contains("hd hra") || displayTitle.contains("hd hra")) ->
                R.drawable.badge_dts_hra
        codec.contains("DTS") -> R.drawable.badge_dts

        // Dolby variants
        codec.contains("TRUEHD") -> R.drawable.badge_truehd
        codec.contains("EAC3") || codec.contains("E-AC-3") -> R.drawable.badge_dolby_digital_plus
        codec.contains("AC3") || codec.contains("AC-3") -> R.drawable.badge_dolby_digital

        // Other codecs
        codec.contains("AAC") -> R.drawable.badge_aac
        codec.contains("FLAC") -> R.drawable.badge_flac
        codec.contains("PCM") -> R.drawable.badge_pcm

        else -> null
    }
}

/**
 * Get the edition image resource based on item path, name, and tags.
 * Edition info is typically in the file path in square brackets like [Director's Cut].
 */
private fun getEditionImageResource(itemName: String, tags: List<String>?, path: String?): Int? {
    val nameLower = itemName.lowercase()
    val tagsLower = tags?.map { it.lowercase() }.orEmpty()
    val pathLower = path?.lowercase() ?: ""

    // Extract text from square brackets in path (e.g., "[Director's Cut]")
    val bracketPattern = Regex("\\[([^\\]]+)\\]")
    val pathBrackets = bracketPattern.findAll(pathLower).map { it.groupValues[1] }.toList()

    // Helper to check if any source contains the pattern
    fun containsPattern(pattern: String): Boolean {
        return nameLower.contains(pattern) ||
            tagsLower.any { it.contains(pattern) } ||
            pathBrackets.any { it.contains(pattern) }
    }

    // Check patterns in order of specificity
    return when {
        // Director's cuts and special cuts
        containsPattern("director's cut") || containsPattern("directors cut") ->
            R.drawable.badge_directors_cut
        containsPattern("final cut") -> R.drawable.badge_final_cut
        containsPattern("ultimate cut") -> R.drawable.badge_ultimate_cut
        containsPattern("producer's cut") || containsPattern("producers cut") ->
            R.drawable.badge_producers_cut
        containsPattern("theatrical") -> R.drawable.badge_theatrical

        // Extended versions
        containsPattern("extended") -> R.drawable.badge_extended
        containsPattern("unrated") -> R.drawable.badge_unrated
        containsPattern("uncut") -> R.drawable.badge_uncut

        // Special editions
        containsPattern("special edition") -> R.drawable.badge_special_edition
        containsPattern("collector's edition") || containsPattern("collectors edition") ->
            R.drawable.badge_collectors_edition
        containsPattern("anniversary") -> R.drawable.badge_anniversary
        containsPattern("criterion") -> R.drawable.badge_criterion
        containsPattern("remastered") || containsPattern("remaster") ->
            R.drawable.badge_remastered

        // IMAX
        containsPattern("imax enhanced") -> R.drawable.badge_imax_enhanced
        containsPattern("imax") -> R.drawable.badge_imax

        // Other
        containsPattern("black and chrome") || containsPattern("black & chrome") ->
            R.drawable.badge_black_chrome
        containsPattern("open matte") -> R.drawable.badge_open_matte

        else -> null
    }
}
