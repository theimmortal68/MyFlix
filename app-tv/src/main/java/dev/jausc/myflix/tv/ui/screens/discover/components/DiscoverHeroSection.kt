@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.discover.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.util.DateFormatter
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.viewmodel.DiscoverFocusedRatings
import dev.jausc.myflix.tv.ui.components.seerr.SeerrContentRatingBadge
import dev.jausc.myflix.tv.ui.components.seerr.SeerrDotSeparator
import dev.jausc.myflix.tv.ui.components.seerr.SeerrImdbScoreBadge
import dev.jausc.myflix.tv.ui.components.seerr.SeerrRottenTomatoesScoreBadge
import dev.jausc.myflix.tv.ui.components.seerr.SeerrTmdbScoreBadge
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Fixed height for the Discover hero section.
 */
val DISCOVER_HERO_HEIGHT = 220.dp

/**
 * Hero text overlay for the Discover screen.
 * Displays media info with AnimatedContent crossfade when the focused media changes.
 *
 * Rating row shows: full release date, TMDb score, RT score (with tomato icon), IMDb score.
 *
 * @param media The currently focused SeerrMedia item
 * @param ratings Optional RT/IMDb ratings fetched for the focused item
 * @param modifier Modifier for the section
 */
@Composable
fun DiscoverHeroSection(
    media: SeerrMedia?,
    ratings: DiscoverFocusedRatings? = null,
    modifier: Modifier = Modifier,
) {
    if (media == null) return

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        AnimatedContent(
            targetState = media,
            transitionSpec = {
                fadeIn(animationSpec = tween(500, delayMillis = 200)) togetherWith
                    fadeOut(animationSpec = tween(300))
            },
            label = "discover_hero_text",
        ) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Title
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    ),
                    color = TvColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(modifier = Modifier.fillMaxWidth(0.55f)) {
                    // Rating row
                    DiscoverRatingRow(item, ratings)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Description
                    item.overview?.let { overview ->
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextPrimary.copy(alpha = 0.9f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rating row: full release date · TMDb score · RT score · IMDb score.
 */
@Composable
private fun DiscoverRatingRow(media: SeerrMedia, ratings: DiscoverFocusedRatings?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var needsDot = false

        // Full release date (e.g., "November 14, 2025")
        val fullDate = DateFormatter.formatFull(media.displayReleaseDate)
        fullDate?.let { date ->
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
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
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                )
                needsDot = true
            }
        }

        // Content rating (PG-13, R, TV-MA, etc.)
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

        // Check if ratings match the current media
        val matchingRatings = ratings?.takeIf {
            it.tmdbId == (media.tmdbId ?: media.id)
        }

        // Rotten Tomatoes score
        matchingRatings?.rtScore?.let { score ->
            if (needsDot) SeerrDotSeparator()
            SeerrRottenTomatoesScoreBadge(score = score, isFresh = matchingRatings.rtFresh)
            needsDot = true
        }

        // IMDb score
        matchingRatings?.imdbScore?.let { score ->
            if (needsDot) SeerrDotSeparator()
            SeerrImdbScoreBadge(score)
        }
    }
}
