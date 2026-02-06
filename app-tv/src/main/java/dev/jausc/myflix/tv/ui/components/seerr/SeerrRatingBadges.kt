@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.seerr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * TMDb score badge with logo icon and green brand color.
 */
@Composable
fun SeerrTmdbScoreBadge(score: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_tmdb),
            contentDescription = "TMDb",
            modifier = Modifier
                .width(24.dp)
                .height(10.dp),
        )
        Text(
            text = String.format(java.util.Locale.US, "%.1f", score),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = Color(0xFF01D277),
        )
    }
}

/**
 * Rotten Tomatoes score badge with tomato icon.
 */
@Composable
fun SeerrRottenTomatoesScoreBadge(score: Int, isFresh: Boolean) {
    val iconRes = if (isFresh) R.drawable.ic_rotten_tomatoes_fresh else R.drawable.ic_rotten_tomatoes_rotten

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = if (isFresh) "Fresh" else "Rotten",
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "$score%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = if (isFresh) Color(0xFFFA320A) else Color(0xFF6AC238),
        )
    }
}

/**
 * IMDb score badge with IMDb icon.
 */
@Composable
fun SeerrImdbScoreBadge(score: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_imdb),
            contentDescription = "IMDb",
            modifier = Modifier
                .width(20.dp)
                .height(10.dp),
        )
        Text(
            text = String.format(java.util.Locale.US, "%.1f", score),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = Color(0xFFF5C518),
        )
    }
}

/**
 * Content rating badge with color-coded background (PG-13, R, TV-MA, etc.).
 */
@Composable
fun SeerrContentRatingBadge(text: String) {
    val backgroundColor = getContentRatingColor(text)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 1.dp),
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
 * Small dot separator for rating rows.
 */
@Composable
fun SeerrDotSeparator() {
    Text(
        text = "\u2022",
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.6f),
    )
}

/**
 * Color coding for content ratings.
 */
fun getContentRatingColor(rating: String): Color {
    val normalized = rating.uppercase().trim()
    return when {
        normalized in listOf("G", "TV-G", "TV-Y", "TV-Y7", "TV-Y7-FV") ->
            Color(0xFF2E7D32)
        normalized in listOf("PG", "TV-PG") ->
            Color(0xFF1565C0)
        normalized in listOf("PG-13", "TV-14", "16") ->
            Color(0xFFF57C00)
        normalized in listOf("R", "TV-MA", "NC-17", "NR", "UNRATED") ->
            Color(0xFFC62828)
        else -> Color(0xFF616161)
    }
}
