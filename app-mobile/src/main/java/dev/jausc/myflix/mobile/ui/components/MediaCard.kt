package dev.jausc.myflix.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.common.model.progressPercent

/**
 * Progress bar overlay for continue watching items.
 * Shows at the bottom of the card when item has partial progress.
 */
@Composable
private fun ProgressOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (progress > 0f && progress < 1f) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Black.copy(alpha = 0.5f)
        )
    }
}

/**
 * Responsive card sizes based on screen width.
 */
object MobileCardSizes {
    // Compact (standard phones < 600dp)
    val CompactPosterWidth = 110.dp
    val CompactWideCardWidth = 180.dp
    
    // Medium (large phones, foldables 600-840dp)
    val MediumPosterWidth = 130.dp
    val MediumWideCardWidth = 220.dp
    
    // Expanded (tablets > 840dp)
    val ExpandedPosterWidth = 150.dp
    val ExpandedWideCardWidth = 280.dp
    
    /**
     * Get poster width based on screen size class.
     */
    fun getPosterWidth(screenSizeClass: ScreenSizeClass): Dp = when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> CompactPosterWidth
        ScreenSizeClass.MEDIUM -> MediumPosterWidth
        ScreenSizeClass.EXPANDED -> ExpandedPosterWidth
    }
    
    /**
     * Get wide card width based on screen size class.
     */
    fun getWideCardWidth(screenSizeClass: ScreenSizeClass): Dp = when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> CompactWideCardWidth
        ScreenSizeClass.MEDIUM -> MediumWideCardWidth
        ScreenSizeClass.EXPANDED -> ExpandedWideCardWidth
    }
}

/**
 * Portrait media card for movies/series posters.
 * Shows title and year below the image.
 * Responsive sizing based on screen width.
 */
@Composable
fun MobileMediaCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    screenSizeClass: ScreenSizeClass = getScreenSizeClass(LocalConfiguration.current.screenWidthDp),
    showLabel: Boolean = true
) {
    val width = MobileCardSizes.getPosterWidth(screenSizeClass)
    
    Column(
        modifier = modifier.width(width)
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                )
                
                // Progress bar for continue watching
                ProgressOverlay(
                    progress = item.progressPercent,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (showLabel) {
            Column(
                modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * Wide/landscape media card for episodes and thumbnails.
 * Shows episode info overlay on the image.
 * Responsive sizing based on screen width.
 */
@Composable
fun MobileWideMediaCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    screenSizeClass: ScreenSizeClass = getScreenSizeClass(LocalConfiguration.current.screenWidthDp),
    showLabel: Boolean = true
) {
    val width = MobileCardSizes.getWideCardWidth(screenSizeClass)
    
    Column(
        modifier = modifier.width(width)
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
                
                // Episode number badge (for episodes)
                if (item.isEpisode) {
                    val episodeNumber = item.indexNumber
                    val seasonNumber = item.parentIndexNumber
                    if (episodeNumber != null) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = if (seasonNumber != null) "S$seasonNumber E$episodeNumber" else "E$episodeNumber",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Progress bar for continue watching
                ProgressOverlay(
                    progress = item.progressPercent,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (showLabel) {
            Column(
                modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
            ) {
                // For episodes, show the episode name
                if (item.isEpisode) {
                    Text(
                        text = item.seriesName ?: item.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
