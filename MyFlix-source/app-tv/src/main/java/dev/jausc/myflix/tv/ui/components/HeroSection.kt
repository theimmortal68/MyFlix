package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Fixed height for hero section - approximately 30% of a 720p TV screen.
 */
private val HERO_HEIGHT = 220.dp

/**
 * Hero section displaying featured media at the top of the home page.
 * Backdrop image fades into the background with gradient edges.
 * 
 * When previewItem is set, displays that item instead of auto-rotating (preview mode).
 * Preview mode hides action buttons but keeps them focusable for navigation.
 *
 * @param featuredItems List of items to cycle through in the hero
 * @param jellyfinClient Client for building image URLs
 * @param onItemClick Callback when user selects an item for details
 * @param onPlayClick Callback when user clicks the play button
 * @param modifier Modifier for the hero section
 * @param previewItem Optional item to display instead of auto-rotation (buttons hidden)
 * @param autoRotateIntervalMs Time between automatic item rotations (default 8 seconds)
 * @param playButtonFocusRequester Focus requester for the play button (for initial focus)
 * @param downFocusRequester Focus target when navigating down from buttons
 * @param onCurrentItemChanged Callback when the displayed item changes (for dynamic background)
 * @param onPreviewClear Callback when focus returns to hero buttons (to clear preview)
 */
@Composable
fun HeroSection(
    featuredItems: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewItem: JellyfinItem? = null,
    autoRotateIntervalMs: Long = 8000L,
    playButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    downFocusRequester: FocusRequester? = null,
    onCurrentItemChanged: ((JellyfinItem, String?) -> Unit)? = null,
    onPreviewClear: (() -> Unit)? = null
) {
    if (featuredItems.isEmpty() && previewItem == null) return

    var currentIndex by remember { mutableIntStateOf(0) }
    
    // Use previewItem if set, otherwise use the rotating featured item
    val displayItem = previewItem ?: featuredItems.getOrNull(currentIndex) ?: return
    val isPreviewMode = previewItem != null
    
    // Build backdrop URL for current item and notify parent
    val backdropUrl = remember(displayItem.id) {
        buildBackdropUrl(displayItem, jellyfinClient)
    }
    
    // Notify parent when current item changes (for dynamic background colors)
    LaunchedEffect(displayItem.id) {
        onCurrentItemChanged?.invoke(displayItem, backdropUrl)
    }

    // Auto-rotate through featured items (only when not in preview mode)
    LaunchedEffect(featuredItems.size, isPreviewMode) {
        if (!isPreviewMode && featuredItems.size > 1) {
            while (true) {
                delay(autoRotateIntervalMs)
                currentIndex = (currentIndex + 1) % featuredItems.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Background is now transparent - DynamicBackground at HomeScreen level provides the color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        )

        // Backdrop image - positioned at top-end, fades into background
        // Scaled 20% larger without affecting other elements
        AnimatedContent(
            targetState = displayItem,
            transitionSpec = {
                fadeIn(animationSpec = tween(800)) togetherWith
                    fadeOut(animationSpec = tween(800))
            },
            label = "hero_backdrop",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.7f)
                .graphicsLayer {
                    scaleX = 1.25f
                    scaleY = 1.25f
                    // Anchor scale to top-end corner
                    transformOrigin = TransformOrigin(1f, 0f)
                }
        ) { item ->
            HeroBackdrop(
                item = item,
                jellyfinClient = jellyfinClient
            )
        }

        // Content overlay (left side)
        AnimatedContent(
            targetState = displayItem,
            transitionSpec = {
                fadeIn(animationSpec = tween(500, delayMillis = 200)) togetherWith
                    fadeOut(animationSpec = tween(300))
            },
            label = "hero_content"
        ) { item ->
            // Always render content overlay with buttons - buttons are invisible in preview mode
            // but still focusable so up navigation works
            HeroContentOverlay(
                item = item,
                onPlayClick = { onPlayClick(item.id) },
                onDetailsClick = { onItemClick(item.id) },
                playButtonFocusRequester = playButtonFocusRequester,
                downFocusRequester = downFocusRequester,
                isPreviewMode = isPreviewMode,
                onButtonFocused = onPreviewClear
            )
        }
    }
}

/**
 * Backdrop image with edge fading using BlendMode.DstIn.
 * Only fades on left and bottom edges.
 */
@Composable
private fun HeroBackdrop(
    item: JellyfinItem,
    jellyfinClient: JellyfinClient
) {
    val backdropUrl = buildBackdropUrl(item, jellyfinClient)

    AsyncImage(
        model = backdropUrl,
        contentDescription = item.name,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopEnd,
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.9f)
            .drawWithContent {
                drawContent()
                // Left edge fade - image fades to transparent on left
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.15f to Color.Black.copy(alpha = 0.3f),
                            0.4f to Color.Black.copy(alpha = 0.7f),
                            1.0f to Color.Black,
                        ),
                    ),
                    blendMode = BlendMode.DstIn,
                )
                // Bottom edge fade - image fades to transparent at bottom
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            0.6f to Color.Black.copy(alpha = 0.9f),
                            0.85f to Color.Black.copy(alpha = 0.5f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
    )
}

/**
 * Build the backdrop URL for an item, using series backdrop for episodes.
 */
private fun buildBackdropUrl(item: JellyfinItem, jellyfinClient: JellyfinClient): String {
    // For episodes, use series backdrop if available
    val backdropId = when {
        !item.backdropImageTags.isNullOrEmpty() -> item.id
        item.isEpisode && item.seriesId != null -> item.seriesId!!
        else -> item.id
    }
    
    val tag = when {
        !item.backdropImageTags.isNullOrEmpty() -> item.backdropImageTags!!.firstOrNull()
        else -> null
    }
    
    return jellyfinClient.getBackdropUrl(backdropId, tag, maxWidth = 1920)
}

/**
 * Content overlay displaying media information on the left side.
 */
@Composable
private fun HeroContentOverlay(
    item: JellyfinItem,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester? = null,
    isPreviewMode: Boolean = false,
    onButtonFocused: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .padding(start = 48.dp, top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Top padding (16dp as requested)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title and subtitle
        HeroTitleSection(item)

        Spacer(modifier = Modifier.height(6.dp))

        // Rating information row
        HeroRatingRow(item)

        Spacer(modifier = Modifier.height(6.dp))

        // Description (3 lines max)
        HeroDescription(item)

        Spacer(modifier = Modifier.height(10.dp))

        // Action buttons (24dp height) - hidden in preview mode but still focusable
        HeroActionButtons(
            onPlayClick = onPlayClick,
            onDetailsClick = onDetailsClick,
            playButtonFocusRequester = playButtonFocusRequester,
            downFocusRequester = downFocusRequester,
            isPreviewMode = isPreviewMode,
            onButtonFocused = onButtonFocused
        )
    }
}

/**
 * Title section with main title and subtitle for episodes.
 * For episodes: Series name is large, episode name is smaller subtitle.
 */
@Composable
private fun HeroTitleSection(item: JellyfinItem) {
    Column {
        // For episodes, show series name as the main large title
        if (item.isEpisode && !item.seriesName.isNullOrBlank()) {
            Text(
                text = item.seriesName!!,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Episode name as subtitle (bumped up 1 from titleSmall)
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp
                ),
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            // Non-episodes: just show the name as main title
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Row displaying rating information: Official rating, stars, critic rating, year, runtime.
 */
@Composable
private fun HeroRatingRow(item: JellyfinItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Official rating badge (PG-13, TV-MA, etc.)
        item.officialRating?.let { rating ->
            RatingBadge(rating)
        }

        // Production year
        item.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary
            )
        }

        // Community rating (star rating)
        item.communityRating?.let { rating ->
            StarRating(rating)
        }

        // Critic rating (Rotten Tomatoes style)
        item.criticRating?.let { rating ->
            CriticRatingBadge(rating)
        }

        // Runtime
        item.runTimeTicks?.let { ticks ->
            val minutes = (ticks / 600_000_000).toInt()
            if (minutes > 0) {
                RuntimeDisplay(minutes)
            }
        }
    }
}

/**
 * Description text limited to 3 lines.
 */
@Composable
private fun HeroDescription(item: JellyfinItem) {
    item.overview?.let { overview ->
        Text(
            text = overview,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextSecondary.copy(alpha = 0.9f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

/**
 * Action buttons: Play and More Info (24dp height).
 * In preview mode, buttons are invisible but still focusable for navigation.
 */
@Composable
private fun HeroActionButtons(
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester? = null,
    isPreviewMode: Boolean = false,
    onButtonFocused: (() -> Unit)? = null
) {
    // Alpha is 0 in preview mode (invisible but focusable)
    val buttonsAlpha = if (isPreviewMode) 0f else 1f
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.alpha(buttonsAlpha)
    ) {
        // Play button - receives initial focus
        Button(
            onClick = onPlayClick,
            modifier = Modifier
                .height(24.dp)
                .focusRequester(playButtonFocusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused && isPreviewMode) {
                        onButtonFocused?.invoke()
                    }
                }
                .focusProperties {
                    // Block UP navigation - can only go left (nav), right (more info), or down (rows)
                    up = FocusRequester.Cancel
                    if (downFocusRequester != null) {
                        down = downFocusRequester
                    }
                },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.BluePrimary,
                contentColor = Color.White,
                focusedContainerColor = TvColors.BlueAccent,
                focusedContentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Play",
                style = MaterialTheme.typography.labelMedium
            )
        }

        // More Info button
        Button(
            onClick = onDetailsClick,
            modifier = Modifier
                .height(24.dp)
                .onFocusChanged { state ->
                    if (state.isFocused && isPreviewMode) {
                        onButtonFocused?.invoke()
                    }
                }
                .focusProperties {
                    // Block UP navigation - can only go left (play), or down (rows)
                    up = FocusRequester.Cancel
                    if (downFocusRequester != null) {
                        down = downFocusRequester
                    }
                },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            colors = ButtonDefaults.colors(
                containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                contentColor = TvColors.TextPrimary,
                focusedContainerColor = TvColors.SurfaceElevated,
                focusedContentColor = TvColors.TextPrimary
            )
        ) {
            Text(
                text = "More Info",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// === Rating Components ===

/**
 * Badge for official ratings (PG-13, TV-MA, etc.)
 */
@Composable
private fun RatingBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TvColors.SurfaceElevated.copy(alpha = 0.8f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Star rating display (community rating out of 10).
 */
@Composable
private fun StarRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFFD700) // Gold color
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TvColors.TextPrimary
        )
    }
}

/**
 * Critic rating with Rotten Tomatoes style indicator.
 * Fresh (≥60%): Red tomato
 * Rotten (<60%): Green splat
 */
@Composable
private fun CriticRatingBadge(rating: Float) {
    val percentage = rating.roundToInt()
    val isFresh = percentage >= 60
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Tomato/Splat icon
        Icon(
            painter = painterResource(
                id = if (isFresh) R.drawable.ic_rotten_tomatoes_fresh else R.drawable.ic_rotten_tomatoes_rotten
            ),
            contentDescription = if (isFresh) "Fresh" else "Rotten",
            modifier = Modifier.size(16.dp),
            tint = Color.Unspecified // Use original icon colors
        )
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TvColors.TextPrimary
        )
    }
}

/**
 * Runtime display in hours and minutes format.
 */
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
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextSecondary
    )
}

// === Shimmer Loading Placeholder ===

/**
 * Shimmer loading placeholder for the hero section.
 */
@Composable
fun HeroSectionShimmer(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HERO_HEIGHT)
            .background(TvColors.SurfaceElevated)
    ) {
        // Placeholder content on left side
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .padding(start = 48.dp, top = 32.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvColors.TextSecondary.copy(alpha = 0.2f))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rating row placeholder
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvColors.TextSecondary.copy(alpha = 0.15f))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvColors.TextSecondary.copy(alpha = 0.15f))
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvColors.TextSecondary.copy(alpha = 0.15f))
            )
        }
    }
}
