@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.seerr.components

import androidx.compose.foundation.Image
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.util.DateFormatter
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrRatingResponse
import dev.jausc.myflix.core.seerr.SeerrRottenTomatoesRating
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.components.seerr.SeerrContentRatingBadge
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.util.Locale

/**
 * Right-side info table for the detail screen.
 * Shows key-value pairs with a colored left accent border.
 */
@Composable
fun DetailInfoTable(
    media: SeerrMedia,
    ratings: SeerrRatingResponse?,
    tvRatings: SeerrRottenTomatoesRating?,
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

            // Content Rating (colored badge)
            media.contentRating?.let { rating ->
                if (rating.isNotBlank()) {
                    InfoRowContent(
                        label = {
                            Text(
                                text = "Rated",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = TvColors.TextSecondary,
                            )
                        },
                        value = { SeerrContentRatingBadge(rating) },
                    )
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

            // TMDb rating (logo + score with votes)
            media.voteAverage?.let { score ->
                if (score > 0.0) {
                    val voteText = buildString {
                        append(String.format(Locale.US, "%.1f", score))
                        media.voteCount?.let { count ->
                            if (count > 0) append(" ($count votes)")
                        }
                    }
                    InfoRowContent(
                        label = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tmdb),
                                contentDescription = "TMDb",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(10.dp),
                            )
                        },
                        value = {
                            Text(
                                text = voteText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color(0xFF01D277),
                            )
                        },
                    )
                }
            }

            // Rotten Tomatoes (logo left, tomato icon + score right)
            val rtScore = ratings?.rt?.criticsScore ?: tvRatings?.criticsScore
            val rtFresh = ratings?.rt?.isCriticsFresh ?: tvRatings?.isCriticsFresh
            rtScore?.let { score ->
                val tomatoRes = if (rtFresh == true) {
                    R.drawable.ic_rotten_tomatoes_fresh
                } else {
                    R.drawable.ic_rotten_tomatoes_rotten
                }
                val scoreColor = if (rtFresh == true) Color(0xFFFA320A) else Color(0xFF6AC238)
                InfoRowContent(
                    label = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rotten_tomatoes_logo),
                            contentDescription = "Rotten Tomatoes",
                            modifier = Modifier
                                .width(24.dp)
                                .height(10.dp),
                        )
                    },
                    value = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Image(
                                painter = painterResource(id = tomatoRes),
                                contentDescription = if (rtFresh == true) "Fresh" else "Rotten",
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "$score%",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = scoreColor,
                            )
                        }
                    },
                )
            }

            // IMDb (logo + score)
            ratings?.imdb?.criticsScore?.let { score ->
                InfoRowContent(
                    label = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_imdb),
                            contentDescription = "IMDb",
                            modifier = Modifier
                                .width(20.dp)
                                .height(10.dp),
                        )
                    },
                    value = {
                        Text(
                            text = String.format(Locale.US, "%.1f", score),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFFF5C518),
                        )
                    },
                )
            }
        }
    }
}

/**
 * Text-based info row with a fixed-width label column.
 */
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
            modifier = Modifier.width(72.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
        )
    }
}

/**
 * Composable-based info row for badge/icon content.
 */
@Composable
private fun InfoRowContent(
    label: @Composable () -> Unit,
    value: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.width(72.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            label()
        }
        Spacer(modifier = Modifier.width(8.dp))
        value()
    }
}

private fun getStatusDisplay(status: Int): Pair<String, Color> = when (status) {
    SeerrMediaStatus.AVAILABLE -> "Available" to Color(0xFF22C55E)
    SeerrMediaStatus.PARTIALLY_AVAILABLE -> "Partially Available" to Color(0xFF60A5FA)
    SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> "Requested" to Color(0xFFFBBF24)
    else -> "Not Requested" to TvColors.TextSecondary
}
