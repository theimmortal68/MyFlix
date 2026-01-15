@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dot-separated row of metadata text with optional community rating.
 * Wholphin-style metadata display.
 */
@Composable
fun DotSeparatedRow(
    texts: List<String>,
    modifier: Modifier = Modifier,
    communityRating: Float? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    CompositionLocalProvider(LocalTextStyle provides textStyle) {
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
                    textStyle = textStyle,
                    modifier = Modifier.height(height),
                )
            }
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
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 1f))
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
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
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
            style = textStyle,
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
        textStyle = MaterialTheme.typography.titleSmall,
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
            status?.let { add(formatSeriesStatusBadge(it)) }
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
            textStyle = MaterialTheme.typography.titleSmall,
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
            .background(TvColors.SurfaceElevated.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TvColors.TextPrimary,
        )
    }
}

private fun formatSeriesStatusBadge(status: String): String {
    val normalized = status.trim().lowercase()
    return when {
        normalized.contains("continuing") -> "Airing"
        normalized.contains("returning") -> "Returning"
        normalized.contains("ended") -> "Ended"
        normalized.contains("canceled") || normalized.contains("cancelled") -> "Canceled"
        else -> status.trim()
    }
}

/**
 * Helper function to add runtime details to the list.
 */
private fun MutableList<String>.addRuntimeDetails(
    now: LocalDateTime,
    item: JellyfinItem,
) {
    val runtimeTicks = item.runTimeTicks ?: return
    val runtimeMinutes = (runtimeTicks / 600_000_000).toInt()
    if (runtimeMinutes <= 0) return

    // Format runtime as "1h 48m" or "48m"
    val hours = runtimeMinutes / 60
    val mins = runtimeMinutes % 60
    val runtimeText = when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
    add(runtimeText)

    // Calculate remaining time if there's playback progress
    val positionTicks = item.userData?.playbackPositionTicks ?: 0L
    if (positionTicks > 0L) {
        val remainingTicks = runtimeTicks - positionTicks
        val remainingMinutes = (remainingTicks / 600_000_000).toInt()
        if (remainingMinutes > 0) {
            val remainingHours = remainingMinutes / 60
            val remainingMins = remainingMinutes % 60
            val remainingText = when {
                remainingHours > 0 && remainingMins > 0 -> "${remainingHours}h ${remainingMins}m left"
                remainingHours > 0 -> "${remainingHours}h left"
                else -> "${remainingMins}m left"
            }
            add(remainingText)
        }
    }

    // Calculate "ends at" time
    val ticksToPlay = if (positionTicks > 0L) runtimeTicks - positionTicks else runtimeTicks
    val secondsToPlay = ticksToPlay / 10_000_000
    val endTime = now.plusSeconds(secondsToPlay)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    add("ends at ${endTime.format(timeFormatter)}")
}
