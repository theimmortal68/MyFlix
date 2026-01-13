@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.is4K
import dev.jausc.myflix.core.common.model.isDolbyVision
import dev.jausc.myflix.core.common.model.isHdr
import dev.jausc.myflix.core.network.JellyfinClient

/**
 * Hero section for Plex/VoidTV-style detail screens on mobile.
 * Shows full backdrop with gradient overlay, title, metadata with genre dropdown,
 * synopsis, quality badges, and action buttons.
 */
@Composable
fun MobileDetailHeroSection(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMarkWatchedClick: () -> Unit,
    onMarkUnwatchedClick: () -> Unit,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdropUrl = buildBackdropUrl(item, jellyfinClient)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp),
    ) {
        // Full backdrop with gradient overlay
        AsyncImage(
            model = backdropUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Bottom gradient for text readability
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.3f to Color.Transparent,
                                0.6f to Color.Black.copy(alpha = 0.5f),
                                0.8f to Color.Black.copy(alpha = 0.8f),
                                1.0f to Color.Black.copy(alpha = 0.95f),
                            ),
                        ),
                    )
                },
        )

        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        // Content overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Title
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row with genres dropdown
            MetadataRow(
                item = item,
                onGenreSelected = onGenreSelected,
            )

            // Tagline if available
            item.taglines?.firstOrNull()?.let { tagline ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$tagline\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Synopsis/Overview
            item.overview?.let { overview ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Quality badges row
            val badges = mutableListOf<String>()
            if (item.is4K) badges.add("4K")
            if (item.isDolbyVision) badges.add("DV")
            else if (item.isHdr) badges.add("HDR")

            if (badges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    badges.forEach { badge ->
                        QualityBadge(badge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            MobileDetailActionsRow(
                item = item,
                onPlayClick = onPlayClick,
                onFavoriteClick = onFavoriteClick,
                onMarkWatchedClick = onMarkWatchedClick,
                onMarkUnwatchedClick = onMarkUnwatchedClick,
            )
        }
    }
}

/**
 * Metadata row showing year, genres dropdown, rating, runtime, content rating.
 */
@Composable
private fun MetadataRow(
    item: JellyfinItem,
    onGenreSelected: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Production year
        item.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        // Genres dropdown button
        if (!item.genres.isNullOrEmpty()) {
            MobileGenreDropdown(
                genres = item.genres!!,
                onGenreSelected = onGenreSelected,
            )
        }

        // Community rating
        item.communityRating?.let { rating ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFD700),
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", rating),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }
        }

        // Runtime
        item.runTimeTicks?.let { ticks ->
            val minutes = (ticks / 600_000_000).toInt()
            if (minutes > 0) {
                val hours = minutes / 60
                val mins = minutes % 60
                val text = when {
                    hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                    hours > 0 -> "${hours}h"
                    else -> "${mins}m"
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        // Official rating badge
        item.officialRating?.let { rating ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Text(
                    text = rating,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Quality badge for 4K, HDR, Dolby Vision.
 */
@Composable
private fun QualityBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/**
 * Build the backdrop URL for an item, using series backdrop for episodes.
 */
private fun buildBackdropUrl(item: JellyfinItem, jellyfinClient: JellyfinClient): String {
    val backdropId = if (!item.backdropImageTags.isNullOrEmpty()) {
        item.id
    } else if (item.type == "Episode" && item.seriesId != null) {
        item.seriesId!!
    } else {
        item.id
    }

    val tag = item.backdropImageTags?.firstOrNull()
    return jellyfinClient.getBackdropUrl(backdropId, tag, maxWidth = 1280)
}
