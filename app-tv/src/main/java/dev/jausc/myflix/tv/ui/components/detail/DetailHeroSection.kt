@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.is4K
import dev.jausc.myflix.core.common.model.isDolbyVision
import dev.jausc.myflix.core.common.model.isHdr
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlin.math.roundToInt

/**
 * Hero section for Plex/VoidTV-style detail screens.
 * Shows full backdrop with gradient overlay, title, metadata with genre dropdown,
 * synopsis, quality badges, and action buttons.
 * Designed for single-scrolling layout (NOT tabbed).
 */
@Composable
fun DetailHeroSection(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMarkWatchedClick: () -> Unit,
    onMarkUnwatchedClick: () -> Unit,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    actionsFocusRequester: FocusRequester = FocusRequester(),
) {
    val backdropUrl = buildBackdropUrl(item, jellyfinClient)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        // Full backdrop with gradient overlay
        AsyncImage(
            model = backdropUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Left edge fade for text readability
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.05f to Color.Black.copy(alpha = 0.4f),
                                0.15f to Color.Black.copy(alpha = 0.7f),
                                0.35f to Color.Black.copy(alpha = 0.95f),
                                0.5f to Color.Black,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                    // Bottom gradient for content blending
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black,
                                0.5f to Color.Black,
                                0.75f to Color.Black.copy(alpha = 0.8f),
                                0.9f to Color.Black.copy(alpha = 0.5f),
                                1.0f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, top = 24.dp, bottom = 24.dp, end = 48.dp),
        ) {
            // Back button
            Button(
                onClick = onBackClick,
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                scale = ButtonDefaults.scale(focusedScale = 1f),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                    contentColor = TvColors.TextPrimary,
                    focusedContainerColor = TvColors.BluePrimary,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(14.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.55f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row with genres dropdown
            MetadataRow(
                item = item,
                onGenreSelected = onGenreSelected,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            item.taglines?.firstOrNull()?.let { tagline ->
                Text(
                    text = "\"$tagline\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = TvColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.55f),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Synopsis/Overview
            item.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.55f),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quality badges row
            QualityBadgesRow(item)

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            DetailActionsRow(
                item = item,
                onPlayClick = onPlayClick,
                onFavoriteClick = onFavoriteClick,
                onMarkWatchedClick = onMarkWatchedClick,
                onMarkUnwatchedClick = onMarkUnwatchedClick,
                focusRequester = actionsFocusRequester,
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
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
        }

        // Genres dropdown button
        if (!item.genres.isNullOrEmpty()) {
            GenresButton(
                genres = item.genres!!,
                onGenreSelected = onGenreSelected,
            )
        }

        // Community rating
        item.communityRating?.let { rating ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFD700),
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", rating),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TvColors.TextPrimary,
                )
            }
        }

        // Critic rating (Rotten Tomatoes)
        item.criticRating?.let { rating ->
            val percentage = rating.roundToInt()
            val isFresh = percentage >= 60
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isFresh) {
                            R.drawable.ic_rotten_tomatoes_fresh
                        } else {
                            R.drawable.ic_rotten_tomatoes_rotten
                        },
                    ),
                    contentDescription = if (isFresh) "Fresh" else "Rotten",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified,
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TvColors.TextPrimary,
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
                    color = TvColors.TextPrimary.copy(alpha = 0.9f),
                )
            }
        }

        // Official rating badge
        item.officialRating?.let { rating ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvColors.SurfaceElevated.copy(alpha = 0.8f)),
            ) {
                Text(
                    text = rating,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TvColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Quality badges row (4K, HDR, Dolby Vision).
 */
@Composable
private fun QualityBadgesRow(item: JellyfinItem) {
    val badges = mutableListOf<String>()
    if (item.is4K) badges.add("4K")
    if (item.isDolbyVision) badges.add("DV")
    else if (item.isHdr) badges.add("HDR")

    if (badges.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            badges.forEach { badge ->
                QualityBadge(badge)
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
            .background(TvColors.BluePrimary.copy(alpha = 0.9f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/**
 * Build the backdrop URL for an item.
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
    return jellyfinClient.getBackdropUrl(backdropId, tag, maxWidth = 1920)
}
