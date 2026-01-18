@file:Suppress("MagicNumber")

package dev.jausc.myflix.core.common.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.BadgeType
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.util.MediaBadgeUtil
import dev.jausc.myflix.core.common.util.MediaInfoUtil
import dev.jausc.myflix.core.common.util.MediaInfoUtil.addRuntimeDetails
import java.time.LocalDateTime
import java.util.Locale

/**
 * Dot-separated row of metadata text with optional community rating.
 * Uses BasicText for platform-agnostic rendering.
 *
 * @param texts List of text items to display separated by dots
 * @param modifier Modifier for the row
 * @param communityRating Optional rating to display with star icon
 * @param textStyle Text style to apply (pass from MaterialTheme)
 * @param textColor Text color to apply
 * @param dotColor Color for the dot separators
 * @param dotAlpha Alpha value for dots (default 1.0)
 * @param starIcon Composable slot for the star icon (platform-specific)
 */
@Composable
fun DotSeparatedRow(
    texts: List<String>,
    modifier: Modifier = Modifier,
    communityRating: Float? = null,
    textStyle: TextStyle = TextStyle.Default,
    textColor: Color = Color.Unspecified,
    dotColor: Color = Color.Unspecified,
    dotAlpha: Float = 1f,
    starIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        texts.forEachIndexed { index, text ->
            BasicText(
                text = text,
                style = textStyle.copy(color = textColor),
                maxLines = 1,
            )
            if (communityRating != null || index != texts.lastIndex) {
                Dot(color = dotColor, alpha = dotAlpha)
            }
        }
        val height = with(LocalDensity.current) { textStyle.fontSize.toDp() }
        communityRating?.let { rating ->
            SimpleStarRating(
                communityRating = rating,
                textStyle = textStyle,
                textColor = textColor,
                starIcon = starIcon,
                modifier = Modifier.height(height),
            )
        }
    }
}

/**
 * Small dot separator for metadata rows.
 *
 * @param modifier Modifier for the dot
 * @param color Dot color
 * @param alpha Alpha value for the dot
 * @param size Size of the dot
 */
@Composable
fun Dot(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    alpha: Float = 1f,
    size: Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
            .size(size),
    )
}

/**
 * Simple star rating display with icon and value.
 * Uses BasicText for platform-agnostic rendering.
 *
 * @param communityRating The rating value to display
 * @param modifier Modifier for the row
 * @param textStyle Text style for the rating value
 * @param textColor Text color for the rating value
 * @param starIcon Composable slot for the star icon (pass platform-specific Icon)
 */
@Composable
fun SimpleStarRating(
    communityRating: Float,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    textColor: Color = Color.Unspecified,
    starIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        starIcon?.invoke()
        BasicText(
            text = String.format(Locale.US, "%.1f", communityRating),
            style = textStyle.copy(color = textColor),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * Quick details row for movies: year, runtime, "ends at", rating.
 *
 * @param item The Jellyfin item to display details for
 * @param modifier Modifier for the row
 * @param textStyle Text style to apply
 * @param textColor Text color to apply
 * @param dotColor Color for dot separators
 * @param dotAlpha Alpha for dots
 * @param starIcon Composable slot for star icon
 */
@Composable
fun MovieQuickDetails(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    textColor: Color = Color.Unspecified,
    dotColor: Color = Color.Unspecified,
    dotAlpha: Float = 1f,
    starIcon: @Composable (() -> Unit)? = null,
) {
    val now = LocalDateTime.now()
    val details = remember(item, now) {
        buildList {
            item.productionYear?.let { add(it.toString()) }
            addRuntimeDetails(now, item)
            item.officialRating?.let(::add)
        }
    }

    DotSeparatedRow(
        texts = details,
        communityRating = item.communityRating,
        textStyle = textStyle,
        textColor = textColor,
        dotColor = dotColor,
        dotAlpha = dotAlpha,
        starIcon = starIcon,
        modifier = modifier,
    )
}

/**
 * Quick details row for series: premiere year, official rating, with optional badges.
 *
 * @param item The Jellyfin item to display details for
 * @param modifier Modifier for the row
 * @param status Series status (e.g., "Continuing", "Ended")
 * @param studios List of studio names
 * @param textStyle Text style to apply
 * @param textColor Text color to apply
 * @param dotColor Color for dot separators
 * @param dotAlpha Alpha for dots
 * @param starIcon Composable slot for star icon
 * @param badgeContent Composable slot for rendering badges
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeriesQuickDetails(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
    status: String? = null,
    studios: List<String> = emptyList(),
    textStyle: TextStyle = TextStyle.Default,
    textColor: Color = Color.Unspecified,
    dotColor: Color = Color.Unspecified,
    dotAlpha: Float = 1f,
    starIcon: @Composable (() -> Unit)? = null,
    badgeContent: @Composable ((String) -> Unit)? = null,
) {
    val details = remember(item) {
        buildList {
            item.productionYear?.let { add(it.toString()) }
            item.officialRating?.let(::add)
        }
    }

    val badges = remember(status, studios) {
        buildList {
            status?.let { add(MediaInfoUtil.formatSeriesStatusBadge(it)) }
            studios.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        DotSeparatedRow(
            texts = details,
            communityRating = item.communityRating,
            textStyle = textStyle,
            textColor = textColor,
            dotColor = dotColor,
            dotAlpha = dotAlpha,
            starIcon = starIcon,
        )
        if (badgeContent != null) {
            badges.forEach { badge ->
                badgeContent(badge)
            }
        }
    }
}

/**
 * Badge color configuration for media badges.
 */
object MediaBadgeColors {
    val Resolution = Color(0xFF2563EB)      // Blue
    val VideoCodec = Color(0xFFEA580C)      // Orange
    val Hdr = Color(0xFF9333EA)             // Purple
    val AudioCodec = Color(0xFF0891B2)      // Cyan
    val AudioChannels = Color(0xFF059669)   // Green
    val Edition = Color(0xFFCA8A04)         // Gold/Yellow

    /**
     * Get background color for a badge type.
     */
    fun getColor(type: BadgeType): Color = when (type) {
        BadgeType.RESOLUTION -> Resolution
        BadgeType.VIDEO_CODEC -> VideoCodec
        BadgeType.HDR -> Hdr
        BadgeType.AUDIO_CODEC -> AudioCodec
        BadgeType.AUDIO_CHANNELS -> AudioChannels
        BadgeType.EDITION -> Edition
    }
}

/**
 * Colorful badge for media information (resolution, codec, HDR, etc.).
 * Uses BasicText for platform-agnostic rendering.
 *
 * @param text Badge text
 * @param type Badge type (determines color)
 * @param modifier Modifier for the badge
 * @param textStyle Text style for the badge text
 */
@Composable
fun ColoredMediaBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
) {
    val backgroundColor = MediaBadgeColors.getColor(type)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        BasicText(
            text = text,
            style = textStyle.copy(color = Color.White),
        )
    }
}

/**
 * Row of colorful media badges showing resolution, HDR/DV, video codec, audio info, and edition.
 *
 * @param item The Jellyfin item to extract badge info from
 * @param modifier Modifier for the row
 * @param textStyle Text style for badge text
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaBadgesRow(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
) {
    val mediaSource = item.mediaSources?.firstOrNull()
    val mediaStreams = mediaSource?.mediaStreams.orEmpty()
    val videoStream = mediaStreams.firstOrNull { it.type == "Video" }
    val audioStream = mediaStreams.firstOrNull { it.type == "Audio" && it.isDefault }
        ?: mediaStreams.firstOrNull { it.type == "Audio" }

    val badges = remember(mediaSource, item.name, item.tags) {
        MediaBadgeUtil.buildMediaBadges(videoStream, audioStream, item.name, item.tags)
    }

    if (badges.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        badges.forEach { (text, type) ->
            ColoredMediaBadge(text = text, type = type, textStyle = textStyle)
        }
    }
}
