package dev.jausc.myflix.mobile.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.common.model.runtimeMinutes
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.delay

/**
 * Screen size categories for responsive layout.
 */
enum class ScreenSizeClass {
    COMPACT,    // Standard phones (< 600dp)
    MEDIUM,     // Large phones, small tablets, foldables (600-840dp)
    EXPANDED    // Tablets, foldables unfolded (> 840dp)
}

/**
 * Determine screen size class from screen width.
 */
fun getScreenSizeClass(screenWidthDp: Int): ScreenSizeClass {
    return when {
        screenWidthDp < 600 -> ScreenSizeClass.COMPACT
        screenWidthDp < 840 -> ScreenSizeClass.MEDIUM
        else -> ScreenSizeClass.EXPANDED
    }
}

/**
 * Get hero height based on screen size and orientation.
 */
fun getHeroHeight(screenWidthDp: Int, screenHeightDp: Int): Dp {
    val sizeClass = getScreenSizeClass(screenWidthDp)
    val isLandscape = screenWidthDp > screenHeightDp
    
    return when (sizeClass) {
        ScreenSizeClass.COMPACT -> {
            // Standard phones: ~33% of screen height in portrait (was 50%)
            if (isLandscape) 280.dp else (screenHeightDp * 0.33).dp
        }
        ScreenSizeClass.MEDIUM -> {
            // Foldables/large phones: ~45% of screen height (unchanged)
            if (isLandscape) 320.dp else (screenHeightDp * 0.45).dp
        }
        ScreenSizeClass.EXPANDED -> {
            // Tablets: ~40% of screen height
            if (isLandscape) 360.dp else (screenHeightDp * 0.40).dp
        }
    }
}

/**
 * Get horizontal image bias based on screen size.
 * Shifts image left on compact phones.
 */
fun getHeroImageHorizontalBias(screenSizeClass: ScreenSizeClass): Float {
    return when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> -0.30f  // Shift left ~60dp equivalent
        else -> 0f  // Centered for foldables/tablets
    }
}

/**
 * Mobile hero section displaying featured media with swipeable pager.
 * Responsive sizing for standard phones, foldables, and tablets.
 *
 * @param featuredItems List of items to display in the hero
 * @param jellyfinClient Client for building image URLs
 * @param onItemClick Callback when user taps for more info
 * @param onPlayClick Callback when user taps play button
 * @param modifier Modifier for the hero section
 * @param autoRotateIntervalMs Time between automatic rotations (default 8 seconds)
 */
@Composable
fun MobileHeroSection(
    featuredItems: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoRotateIntervalMs: Long = 8000L
) {
    if (featuredItems.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val heroHeight = getHeroHeight(screenWidthDp, screenHeightDp)
    val screenSizeClass = getScreenSizeClass(screenWidthDp)

    val pagerState = rememberPagerState(pageCount = { featuredItems.size })

    // Auto-rotate through items - with safety checks
    LaunchedEffect(featuredItems.size) {
        if (featuredItems.size > 1) {
            while (true) {
                delay(autoRotateIntervalMs)
                try {
                    val currentSize = featuredItems.size
                    if (currentSize > 0) {
                        val nextPage = (pagerState.currentPage + 1) % currentSize
                        pagerState.animateScrollToPage(nextPage)
                    }
                } catch (e: Exception) {
                    // Ignore animation errors when list changes
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        // Swipeable pager for hero content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = featuredItems[page]
            HeroCard(
                item = item,
                jellyfinClient = jellyfinClient,
                onItemClick = { onItemClick(item.id) },
                onPlayClick = { onPlayClick(item.id) },
                screenSizeClass = screenSizeClass
            )
        }

        // Page indicators
        if (featuredItems.size > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                repeat(featuredItems.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Single hero card with backdrop image and content overlay.
 * Image is shifted down 30dp to prevent top cropping.
 */
@Composable
private fun HeroCard(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient,
    onItemClick: () -> Unit,
    onPlayClick: () -> Unit,
    screenSizeClass: ScreenSizeClass
) {
    val backdropUrl = buildBackdropUrl(item, jellyfinClient)
    
    // For episodes, get display info
    val displayTitle = when {
        item.isEpisode -> item.seriesName ?: item.name
        else -> item.name
    }
    
    val episodeInfo = if (item.isEpisode) {
        val season = item.parentIndexNumber
        val episode = item.indexNumber
        if (season != null && episode != null) {
            "S${season}:E${episode} - ${item.name}"
        } else item.name
    } else null

    // Responsive text sizes
    val titleSize = when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> 26.sp
        ScreenSizeClass.MEDIUM -> 32.sp
        ScreenSizeClass.EXPANDED -> 38.sp
    }
    
    val descriptionMaxLines = when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> 2
        ScreenSizeClass.MEDIUM -> 3
        ScreenSizeClass.EXPANDED -> 4
    }
    
    val horizontalPadding = when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> 20.dp
        ScreenSizeClass.MEDIUM -> 32.dp
        ScreenSizeClass.EXPANDED -> 48.dp
    }
    
    val buttonHeight = when (screenSizeClass) {
        ScreenSizeClass.COMPACT -> 42.dp
        ScreenSizeClass.MEDIUM -> 48.dp
        ScreenSizeClass.EXPANDED -> 52.dp
    }
    
    // Get horizontal bias for image positioning
    val horizontalBias = getHeroImageHorizontalBias(screenSizeClass)

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop image - shifted based on screen size
        AsyncImage(
            model = backdropUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            // Align towards top of image and shift left on phones
            alignment = BiasAlignment(
                horizontalBias = horizontalBias,  // Shift left on compact phones
                verticalBias = -0.7f              // Show more of top
            ),
            modifier = Modifier.fillMaxSize()
        )
        
        // Separate bottom gradient overlay for text readability only
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.4f to Color.Black.copy(alpha = 0.5f),
                            1.0f to Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Content overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = titleSize
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Episode info (if applicable)
            if (episodeInfo != null) {
                Text(
                    text = episodeInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Rating badge
                item.officialRating?.let { rating ->
                    RatingBadge(text = rating)
                }

                // Star rating
                item.communityRating?.let { rating ->
                    StarRating(rating = rating)
                }

                // Runtime
                item.runtimeMinutes?.let { minutes ->
                    RuntimeDisplay(minutes = minutes)
                }
            }

            // Description (truncated)
            item.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = descriptionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // More Info button
                FilledTonalButton(
                    onClick = onItemClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.3f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Text(
                        text = "More Info",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Build backdrop URL for an item.
 */
private fun buildBackdropUrl(item: JellyfinItem, jellyfinClient: JellyfinClient): String {
    return when {
        // For episodes, prefer series backdrop
        item.isEpisode && item.seriesId != null -> {
            jellyfinClient.getBackdropUrl(item.seriesId!!, null, maxWidth = 1920)
        }
        // Use item's own backdrop if available
        !item.backdropImageTags.isNullOrEmpty() -> {
            jellyfinClient.getBackdropUrl(item.id, item.backdropImageTags?.firstOrNull(), maxWidth = 1920)
        }
        // Fallback to primary image
        else -> {
            jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary, maxWidth = 1920)
        }
    }
}

// === Rating Components ===

@Composable
private fun RatingBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StarRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFFFFD700)
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
    }
}

@Composable
private fun RuntimeDisplay(minutes: Int) {
    val hours = minutes / 60
    val mins = minutes % 60
    val text = when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.8f)
    )
}
