@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.seerr.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.util.DateFormatter
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrRottenTomatoesRating
import dev.jausc.myflix.core.seerr.SeerrRatingResponse
import dev.jausc.myflix.tv.ui.components.seerr.SeerrContentRatingBadge
import dev.jausc.myflix.tv.ui.components.seerr.SeerrDotSeparator
import dev.jausc.myflix.tv.ui.components.seerr.SeerrImdbScoreBadge
import dev.jausc.myflix.tv.ui.components.seerr.SeerrRottenTomatoesScoreBadge
import dev.jausc.myflix.tv.ui.components.seerr.SeerrTmdbScoreBadge
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Three-column hero layout for the detail screen.
 * Column 1: Poster
 * Column 2: Title, rating row, genre chips, overview
 * Column 3: Info table
 */
@Composable
fun DetailHeroLayout(
    media: SeerrMedia,
    seerrRepository: SeerrRepository,
    ratings: SeerrRatingResponse?,
    tvRatings: SeerrRottenTomatoesRating?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Column 1: Poster
        AsyncImage(
            model = seerrRepository.getPosterUrl(media.posterPath),
            contentDescription = media.displayTitle,
            modifier = Modifier
                .width(150.dp)
                .height(225.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        // Column 2: Title, rating row, genres, overview
        Column(
            modifier = Modifier.weight(0.6f),
        ) {
            // Title
            Text(
                text = media.displayTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating row
            DetailRatingRow(media, ratings, tvRatings)

            // Genres
            media.genres?.let { genres ->
                if (genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = genres.joinToString(" \u2022 ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                    )
                }
            }

            // Overview
            media.overview?.let { overview ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                )
            }
        }

        // Column 3: Info table
        DetailInfoTable(
            media = media,
            modifier = Modifier.weight(0.4f),
        )
    }
}

/**
 * Rating row: date · runtime · content rating · TMDb · RT · IMDb
 */
@Composable
private fun DetailRatingRow(
    media: SeerrMedia,
    ratings: SeerrRatingResponse?,
    tvRatings: SeerrRottenTomatoesRating?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var needsDot = false

        // Release date
        val fullDate = DateFormatter.formatFull(media.displayReleaseDate)
        fullDate?.let { date ->
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
            )
            needsDot = true
        }

        // Runtime (only if > 0)
        media.runtime?.let { runtime ->
            if (runtime > 0) {
                if (needsDot) SeerrDotSeparator()
                val hours = runtime / 60
                val minutes = runtime % 60
                val formatted = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
                )
                needsDot = true
            }
        }

        // Content rating
        media.contentRating?.let { rating ->
            if (rating.isNotBlank()) {
                if (needsDot) SeerrDotSeparator()
                SeerrContentRatingBadge(rating)
                needsDot = true
            }
        }

        // TMDb score
        media.voteAverage?.let { score ->
            if (score > 0.0) {
                if (needsDot) SeerrDotSeparator()
                SeerrTmdbScoreBadge(score)
                needsDot = true
            }
        }

        // Rotten Tomatoes
        val rtScore = ratings?.rt?.criticsScore ?: tvRatings?.criticsScore
        val rtFresh = ratings?.rt?.isCriticsFresh ?: tvRatings?.isCriticsFresh
        rtScore?.let { score ->
            if (needsDot) SeerrDotSeparator()
            SeerrRottenTomatoesScoreBadge(score = score, isFresh = rtFresh == true)
            needsDot = true
        }

        // IMDb (movies only)
        ratings?.imdb?.criticsScore?.let { score ->
            if (needsDot) SeerrDotSeparator()
            SeerrImdbScoreBadge(score)
        }
    }
}
