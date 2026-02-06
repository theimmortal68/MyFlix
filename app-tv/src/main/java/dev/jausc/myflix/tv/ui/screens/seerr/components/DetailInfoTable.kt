@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.seerr.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.util.DateFormatter
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.util.Locale

/**
 * Right-side info table for the detail screen.
 * Shows key-value pairs with a colored left accent border.
 */
@Composable
fun DetailInfoTable(
    media: SeerrMedia,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        // Accent left border
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(TvColors.BluePrimary),
        )

        Column(
            modifier = Modifier
                .background(TvColors.Surface.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Status
            media.availabilityStatus?.let { status ->
                val (statusText, statusColor) = getStatusDisplay(status)
                InfoRow(label = "Status", value = statusText, valueColor = statusColor)
            }

            // Genres
            media.genres?.let { genres ->
                if (genres.isNotEmpty()) {
                    InfoRow(
                        label = "Genres",
                        value = genres.joinToString(", ") { it.name },
                    )
                }
            }

            // Runtime (movies) or Seasons/Episodes (TV)
            if (media.isMovie) {
                media.runtime?.let { runtime ->
                    if (runtime > 0) {
                        val hours = runtime / 60
                        val minutes = runtime % 60
                        val formatted = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                        InfoRow(label = "Runtime", value = formatted)
                    }
                }
            } else if (media.isTvShow) {
                media.numberOfSeasons?.let { seasons ->
                    val episodes = media.numberOfEpisodes
                    val value = buildString {
                        append("$seasons Season${if (seasons != 1) "s" else ""}")
                        if (episodes != null) {
                            append(" \u2022 $episodes Episode${if (episodes != 1) "s" else ""}")
                        }
                    }
                    InfoRow(label = "Seasons", value = value)
                }
            }

            // Release Date
            val formattedDate = DateFormatter.formatFull(media.displayReleaseDate)
            formattedDate?.let { date ->
                InfoRow(
                    label = if (media.isMovie) "Released" else "First Aired",
                    value = date,
                )
            }

            // Content Rating
            media.contentRating?.let { rating ->
                if (rating.isNotBlank()) {
                    InfoRow(label = "Rated", value = rating)
                }
            }

            // Language
            media.originalLanguage?.let { lang ->
                if (lang.isNotBlank()) {
                    val displayLang = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.ENGLISH)
                    InfoRow(
                        label = "Language",
                        value = displayLang.replaceFirstChar { it.titlecase(Locale.ENGLISH) },
                    )
                }
            }

            // TMDb rating
            media.voteAverage?.let { score ->
                if (score > 0.0) {
                    val voteText = buildString {
                        append(String.format(Locale.US, "%.1f", score))
                        media.voteCount?.let { count ->
                            if (count > 0) append(" ($count votes)")
                        }
                    }
                    InfoRow(label = "TMDb", value = voteText, valueColor = Color(0xFF01D277))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = TvColors.TextPrimary,
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = TvColors.TextSecondary,
            modifier = Modifier.width(80.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
        )
    }
}

private fun getStatusDisplay(status: Int): Pair<String, Color> = when (status) {
    SeerrMediaStatus.AVAILABLE -> "Available" to Color(0xFF22C55E)
    SeerrMediaStatus.PARTIALLY_AVAILABLE -> "Partially Available" to Color(0xFF60A5FA)
    SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> "Requested" to Color(0xFFFBBF24)
    else -> "Not Requested" to TvColors.TextSecondary
}
