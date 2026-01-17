@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
 * Wholphin-style metadata display for mobile.
 */
@Composable
fun DotSeparatedRow(
    texts: List<String>,
    modifier: Modifier = Modifier,
    communityRating: Float? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        texts.forEachIndexed { index, text ->
            Text(
                text = text,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (communityRating != null || index != texts.lastIndex) {
                Dot()
            }
        }
        val height = with(LocalDensity.current) { textStyle.fontSize.toDp() }
        communityRating?.let {
            SimpleStarRating(
                communityRating = it,
                modifier = Modifier.height(height),
            )
        }
    }
}

/**
 * Small dot separator for metadata rows.
 */
@Composable
fun Dot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            .size(4.dp),
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700), // Gold color
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = String.format(Locale.US, "%.1f", communityRating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * Quick details row for movies: year, runtime, "ends at", rating.
 */
@Composable
fun MovieQuickDetails(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
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
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
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

    SeriesQuickDetailsRow(
        details = details,
        communityRating = item.communityRating,
        badges = badges,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesQuickDetailsRow(
    details: List<String>,
    communityRating: Float?,
    badges: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        DotSeparatedRow(
            texts = details,
            communityRating = communityRating,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        badges.forEach { badge ->
            MetadataBadge(text = badge)
        }
    }
}

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
 * Colorful badge for media information.
 */
@Composable
private fun ColoredMediaBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = when (type) {
        BadgeType.RESOLUTION -> Color(0xFF2563EB) to Color.White      // Blue
        BadgeType.VIDEO_CODEC -> Color(0xFFEA580C) to Color.White     // Orange
        BadgeType.HDR -> Color(0xFF9333EA) to Color.White             // Purple
        BadgeType.AUDIO_CODEC -> Color(0xFF0891B2) to Color.White     // Cyan
        BadgeType.AUDIO_CHANNELS -> Color(0xFF059669) to Color.White  // Green
        BadgeType.EDITION -> Color(0xFFCA8A04) to Color.White         // Gold/Yellow
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

/**
 * Row of colorful media badges showing resolution, HDR/DV, video codec, and audio info.
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

    val badges = remember(mediaSource, item.name, item.tags) {
        MediaBadgeUtil.buildMediaBadges(videoStream, audioStream, item.name, item.tags)
    }

    if (badges.isEmpty()) return

    FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        badges.forEach { (text, type) ->
            ColoredMediaBadge(text = text, type = type)
        }
    }
}
