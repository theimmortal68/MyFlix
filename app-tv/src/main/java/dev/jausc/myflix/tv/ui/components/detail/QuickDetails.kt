@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.ui.components.detail.Dot as SharedDot
import dev.jausc.myflix.core.common.ui.components.detail.DotSeparatedRow as SharedDotSeparatedRow
import dev.jausc.myflix.core.common.ui.components.detail.MediaBadgesRow as SharedMediaBadgesRow
import dev.jausc.myflix.core.common.ui.components.detail.MovieQuickDetails as SharedMovieQuickDetails
import dev.jausc.myflix.core.common.ui.components.detail.SeriesQuickDetails as SharedSeriesQuickDetails
import dev.jausc.myflix.core.common.ui.components.detail.SimpleStarRating as SharedSimpleStarRating
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Star icon for ratings using TV Material Icon.
 */
@Composable
private fun TvStarIcon() {
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = Color(0xFFFFD700), // Gold color
        modifier = Modifier.size(16.dp),
    )
}

/**
 * Dot-separated row of metadata text with optional community rating.
 * TV wrapper with theme defaults.
 */
@Composable
fun DotSeparatedRow(
    texts: List<String>,
    modifier: Modifier = Modifier,
    communityRating: Float? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    SharedDotSeparatedRow(
        texts = texts,
        modifier = modifier,
        communityRating = communityRating,
        textStyle = textStyle,
        textColor = MaterialTheme.colorScheme.onSurface,
        dotColor = MaterialTheme.colorScheme.onSurface,
        dotAlpha = 1f,
        starIcon = { TvStarIcon() },
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
        alpha = 1f,
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
        textStyle = MaterialTheme.typography.titleSmall,
        textColor = MaterialTheme.colorScheme.onSurface,
        starIcon = { TvStarIcon() },
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
        textStyle = MaterialTheme.typography.titleSmall,
        textColor = MaterialTheme.colorScheme.onSurface,
        dotColor = MaterialTheme.colorScheme.onSurface,
        dotAlpha = 1f,
        starIcon = { TvStarIcon() },
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
        textStyle = MaterialTheme.typography.titleSmall,
        textColor = MaterialTheme.colorScheme.onSurface,
        dotColor = MaterialTheme.colorScheme.onSurface,
        dotAlpha = 1f,
        starIcon = { TvStarIcon() },
        badgeContent = { text -> MetadataBadge(text = text) },
    )
}

/**
 * Metadata badge with TV-specific styling.
 */
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

/**
 * Row of colorful media badges showing resolution, HDR/DV, video codec, audio info, and edition.
 */
@Composable
fun MediaBadgesRow(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
) {
    SharedMediaBadgesRow(
        item = item,
        modifier = modifier,
        textStyle = MaterialTheme.typography.labelSmall,
    )
}
