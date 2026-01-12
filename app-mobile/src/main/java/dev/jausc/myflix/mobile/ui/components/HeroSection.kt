@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * Default values for hero section layout and behavior.
 */
object HeroSectionDefaults {
    /** Auto-rotate interval between hero items in milliseconds */
    const val AUTO_ROTATE_INTERVAL_MS = 8000L

    /** Portrait height ratios by screen size class */
    const val PORTRAIT_HEIGHT_RATIO_COMPACT = 0.33f
    const val PORTRAIT_HEIGHT_RATIO_MEDIUM = 0.45f
    const val PORTRAIT_HEIGHT_RATIO_EXPANDED = 0.40f

    /** Landscape heights by screen size class */
    val LANDSCAPE_HEIGHT_COMPACT = 280.dp
    val LANDSCAPE_HEIGHT_MEDIUM = 320.dp
    val LANDSCAPE_HEIGHT_EXPANDED = 360.dp

    /** Gradient overlay height for text readability */
    val GRADIENT_HEIGHT = 200.dp

    /** Horizontal image bias for compact screens (shifts image left) */
    const val IMAGE_HORIZONTAL_BIAS_COMPACT = -0.30f

    /** Vertical image bias (shows more of top) */
    const val IMAGE_VERTICAL_BIAS = -0.7f
}

/**
 * Get hero height based on screen size and orientation.
 */
fun getHeroHeight(screenWidthDp: Int, screenHeightDp: Int): Dp {
    val sizeClass = getScreenSizeClass(screenWidthDp)
    val isLandscape = screenWidthDp > screenHeightDp

    return when (sizeClass) {
        ScreenSizeClass.COMPACT -> {
            if (isLandscape) {
                HeroSectionDefaults.LANDSCAPE_HEIGHT_COMPACT
            } else {
                (screenHeightDp * HeroSectionDefaults.PORTRAIT_HEIGHT_RATIO_COMPACT).dp
            }
        }
        ScreenSizeClass.MEDIUM -> {
            if (isLandscape) {
                HeroSectionDefaults.LANDSCAPE_HEIGHT_MEDIUM
            } else {
                (screenHeightDp * HeroSectionDefaults.PORTRAIT_HEIGHT_RATIO_MEDIUM).dp
            }
        }
        ScreenSizeClass.EXPANDED -> {
            if (isLandscape) {
                HeroSectionDefaults.LANDSCAPE_HEIGHT_EXPANDED
            } else {
                (screenHeightDp * HeroSectionDefaults.PORTRAIT_HEIGHT_RATIO_EXPANDED).dp
            }
        }
    }
}

/**
 * Get horizontal image bias based on screen size.
 * Shifts image left on compact phones.
 */
fun getHeroImageHorizontalBias(screenSizeClass: ScreenSizeClass): Float =
    if (screenSizeClass == ScreenSizeClass.COMPACT) {
        HeroSectionDefaults.IMAGE_HORIZONTAL_BIAS_COMPACT
    } else {
        0f
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
    autoRotateIntervalMs: Long = HeroSectionDefaults.AUTO_ROTATE_INTERVAL_MS,
) {
    if (featuredItems.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val heroHeight = getHeroHeight(screenWidthDp, screenHeightDp)
    val screenSizeClass = getScreenSizeClass(screenWidthDp)

    val pagerState = rememberPagerState(pageCount = { featuredItems.size })

    // Auto-rotate through items - with safety checks
    // Keys include items reference and interval to restart loop when content or timing changes
    LaunchedEffect(featuredItems, autoRotateIntervalMs) {
        if (featuredItems.size > 1) {
            while (true) {
                delay(autoRotateIntervalMs)
                try {
                    val currentSize = featuredItems.size
                    if (currentSize > 0) {
                        val nextPage = (pagerState.currentPage + 1) % currentSize
                        pagerState.animateScrollToPage(nextPage)
                    }
                } catch (_: Exception) {
                    // Ignore animation errors when list changes
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        // Swipeable pager for hero content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = featuredItems[page]
            HeroCard(
                item = item,
                jellyfinClient = jellyfinClient,
                onItemClick = { onItemClick(item.id) },
                onPlayClick = { onPlayClick(item.id) },
                screenSizeClass = screenSizeClass,
            )
        }

        // Page indicators
        if (featuredItems.size > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
            ) {
                repeat(featuredItems.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.5f)
                                },
                            )
                            .semantics {
                                contentDescription = "Page ${index + 1} of ${featuredItems.size}"
                            },
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
    screenSizeClass: ScreenSizeClass,
) {
    val backdropUrl = buildBackdropUrl(item, jellyfinClient)

    // For episodes, get display info
    val displayTitle = if (item.isEpisode) {
        item.seriesName ?: item.name
    } else {
        item.name
    }

    val episodeInfo = if (item.isEpisode) {
        val season = item.parentIndexNumber
        val episode = item.indexNumber
        if (season != null && episode != null) {
            "S$season:E$episode - ${item.name}"
        } else {
            item.name
        }
    } else {
        null
    }

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
                horizontalBias = horizontalBias,
                verticalBias = HeroSectionDefaults.IMAGE_VERTICAL_BIAS,
            ),
            modifier = Modifier.fillMaxSize(),
        )

        // Separate bottom gradient overlay for text readability only
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeroSectionDefaults.GRADIENT_HEIGHT)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.4f to Color.Black.copy(alpha = 0.5f),
                            1.0f to Color.Black.copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )

        // Content overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Title
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = titleSize,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Episode info (if applicable)
            if (episodeInfo != null) {
                Text(
                    text = episodeInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Year
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
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
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = descriptionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Play button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.height(buttonHeight),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // More Info button
                FilledTonalButton(
                    onClick = onItemClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.3f),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.height(buttonHeight),
                ) {
                    Text(
                        text = "More Info",
                        fontWeight = FontWeight.Medium,
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
            .background(Color.White.copy(alpha = 0.2f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun StarRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = "Rating",
            modifier = Modifier.size(16.dp),
            tint = Color(0xFFFFD700),
        )
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
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
        color = Color.White.copy(alpha = 0.8f),
    )
}
