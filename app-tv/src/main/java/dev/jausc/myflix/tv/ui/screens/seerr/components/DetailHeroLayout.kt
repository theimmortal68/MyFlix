@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.seerr.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrRottenTomatoesRating
import dev.jausc.myflix.core.seerr.SeerrRatingResponse
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

        // Column 2: Title, overview
        Column(
            modifier = Modifier.weight(1f),
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

            // Overview
            media.overview?.let { overview ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                    lineHeight = 20.sp,
                )
            }
        }

        // Column 3: Info table (top-aligned, right-aligned)
        Box(
            modifier = Modifier.align(Alignment.Top),
            contentAlignment = Alignment.TopEnd,
        ) {
            DetailInfoTable(
                media = media,
                ratings = ratings,
                tvRatings = tvRatings,
            )
        }
    }
}

