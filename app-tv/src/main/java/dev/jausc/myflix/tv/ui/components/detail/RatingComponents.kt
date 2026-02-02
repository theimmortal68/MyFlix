@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.util.Locale

/**
 * Small dot separator for metadata rows.
 */
@Composable
fun DotSeparator() {
    Text(
        text = "•",
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.6f),
    )
}

/**
 * Badge for official ratings (PG-13, TV-MA, etc.) with color-coded backgrounds.
 */
@Composable
fun RatingBadge(text: String) {
    val backgroundColor = getRatingColor(text)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = Color.White,
        )
    }
}

/**
 * Star rating display (community rating out of 10).
 *
 * @param rating The rating value (0-10)
 * @param contentDescription Optional accessibility description. If null, a default description is used.
 */
@Composable
fun StarRating(rating: Float, contentDescription: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = contentDescription ?: "Rating: ${String.format(Locale.US, "%.1f", rating)} out of 10",
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFFD700), // Gold color
        )
        Text(
            text = String.format(Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Rotten Tomatoes rating with tomato icon and percentage.
 * Shows fresh tomato for 60%+ and rotten for below 60%.
 *
 * @param percentage The RT critic score (0-100)
 */
@Composable
fun RottenTomatoesRating(percentage: Int) {
    val isFresh = percentage >= 60
    val iconRes = if (isFresh) R.drawable.ic_rotten_tomatoes_fresh else R.drawable.ic_rotten_tomatoes_rotten

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = if (isFresh) "Fresh" else "Rotten",
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Get the background color for a content rating.
 */
fun getRatingColor(rating: String): Color {
    val normalizedRating = rating.uppercase().trim()
    return when {
        // Green - Family friendly
        normalizedRating in listOf("G", "TV-G", "TV-Y", "TV-Y7", "TV-Y7-FV") ->
            Color(0xFF2E7D32) // Green 800

        // Blue - General/Parental guidance
        normalizedRating in listOf("PG", "TV-PG") ->
            Color(0xFF1565C0) // Blue 800

        // Orange - Teen/Caution
        normalizedRating in listOf("PG-13", "TV-14", "16") ->
            Color(0xFFF57C00) // Orange 700

        // Red - Restricted/Mature
        normalizedRating in listOf("R", "TV-MA", "NC-17", "NR", "UNRATED") ->
            Color(0xFFC62828) // Red 800

        // Gray - Default/Unknown
        else -> Color(0xFF616161) // Gray 700
    }
}
