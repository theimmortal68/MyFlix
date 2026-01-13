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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
 * Fixed hero section for detail screens.
 * Shows backdrop with title, metadata, quality badges, and action buttons.
 * Designed for Netflix-style tabbed layout with fixed height (~40% of screen).
 */
@Composable
fun DetailHeroSection(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester = FocusRequester(),
    tabRowFocusRequester: FocusRequester? = null,
) {
    val backdropUrl = buildBackdropUrl(item, jellyfinClient)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        // Backdrop image with edge fading
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
                                0.1f to Color.Black.copy(alpha = 0.6f),
                                0.25f to Color.Black.copy(alpha = 0.9f),
                                0.4f to Color.Black,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                    // Bottom edge fade for tab blending
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black,
                                0.6f to Color.Black,
                                0.85f to Color.Black.copy(alpha = 0.7f),
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
                .padding(start = 48.dp, top = 24.dp, bottom = 16.dp, end = 48.dp),
        ) {
            // Back button row
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.5f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row: Year, Rating, Runtime, Content Rating, Quality badges
            MetadataRow(item)

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline if available
            item.taglines?.firstOrNull()?.let { tagline ->
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                    color = TvColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Play button
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .height(20.dp)
                        .focusRequester(playButtonFocusRequester)
                        .focusProperties {
                            tabRowFocusRequester?.let { down = it }
                        },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    scale = ButtonDefaults.scale(scale = 1f, focusedScale = 1f),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", style = MaterialTheme.typography.labelSmall)
                }

                // Add to List button
                Button(
                    onClick = { /* TODO: Implement add to list */ },
                    modifier = Modifier
                        .height(20.dp)
                        .focusProperties {
                            tabRowFocusRequester?.let { down = it }
                        },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    scale = ButtonDefaults.scale(scale = 1f, focusedScale = 1f),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("My List", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Metadata row showing year, rating, runtime, content rating, and quality badges.
 */
@Composable
private fun MetadataRow(item: JellyfinItem) {
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

        // Community rating (star)
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
                        id = if (isFresh) R.drawable.ic_rotten_tomatoes_fresh else R.drawable.ic_rotten_tomatoes_rotten,
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

        // Official rating badge (PG-13, TV-MA, etc.)
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

        // Quality badges (4K, HDR, Dolby Vision)
        if (item.is4K) {
            QualityBadge("4K")
        }
        if (item.isDolbyVision) {
            QualityBadge("DV")
        } else if (item.isHdr) {
            QualityBadge("HDR")
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
    return jellyfinClient.getBackdropUrl(backdropId, tag, maxWidth = 1920)
}
