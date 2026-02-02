@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
    "UnusedParameter",
    "UnusedPrivateMember",
)

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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transitionFactory
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.formattedFullPremiereDate
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.common.model.isMovie
import dev.jausc.myflix.core.common.model.isSeries
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButton
import dev.jausc.myflix.tv.ui.theme.IconColors
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.roundToInt

/**
 * Fixed height for hero shimmer placeholder.
 */
private val HERO_HEIGHT = 220.dp

/**
 * Standalone hero backdrop layer for use behind content.
 * Placed at the HomeScreen level to extend behind content rows.
 *
 * Features edge fading on left and bottom to blend with the UI.
 * The image fills 90% of the screen (matching 16:9 aspect ratio)
 * and fades to transparent at the edges.
 *
 * @param item The media item to display backdrop for
 * @param jellyfinClient Client for building image URLs
 * @param modifier Modifier for positioning and sizing
 */
@Composable
fun HeroBackdropLayer(
    item: JellyfinItem?,
    jellyfinClient: JellyfinClient,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (item == null) return

    val context = LocalContext.current
    val backdropUrl = remember(item.id) {
        buildBackdropUrl(item, jellyfinClient)
    }
    val request = remember(backdropUrl) {
        ImageRequest.Builder(context)
            .data(backdropUrl)
            .transitionFactory(CrossFadeFactory(800.milliseconds))
            .build()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = request,
            contentDescription = item.name,
            contentScale = contentScale,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val edgeBlendMask = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.10f to Color.Black.copy(alpha = 0.5f),
                            0.30f to Color.Black.copy(alpha = 0.8f),
                            0.50f to Color.Black,
                            1.0f to Color.Black,
                        ),
                    )
                    val bottomFadeMask = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            0.4f to Color.Black,
                            1.0f to Color.Transparent,
                        ),
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(edgeBlendMask, blendMode = BlendMode.DstIn)
                        drawRect(bottomFadeMask, blendMode = BlendMode.DstIn)
                    }
                },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to TvColors.Background.copy(alpha = 0.7f),
                            0.4f to TvColors.Background.copy(alpha = 0.6f),
                            0.7f to TvColors.Background.copy(alpha = 0.3f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/**
 * Hero section displaying featured media info (no backdrop - backdrop is separate layer).
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
 * @param onCurrentItemChanged Callback when the displayed item changes (for dynamic background)
 * @param onPreviewClear Callback when focus returns to hero buttons (to clear preview)
 */
@Composable
fun HeroSection(
    featuredItems: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onMoreInfoClick: (JellyfinItem) -> Unit,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewItem: JellyfinItem? = null,
    autoRotateIntervalMs: Long = 8000L,
    playButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    onCurrentItemChanged: ((JellyfinItem, String?) -> Unit)? = null,
    onPreviewClear: (() -> Unit)? = null,
    leftEdgeFocusRequester: FocusRequester? = null,
) {
    if (featuredItems.isEmpty() && previewItem == null) return

    var currentIndex by remember { mutableIntStateOf(0) }
    // Track if play button should have focus (to restore after rotation)
    var playButtonShouldHaveFocus by remember { mutableStateOf(false) }

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
    // Keys include items reference and interval to restart loop when content or timing changes
    LaunchedEffect(featuredItems, isPreviewMode, autoRotateIntervalMs) {
        if (!isPreviewMode && featuredItems.size > 1) {
            while (true) {
                delay(autoRotateIntervalMs)
                currentIndex = (currentIndex + 1) % featuredItems.size
            }
        }
    }

    // Content overlay with buttons OUTSIDE AnimatedContent to preserve focus
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top section: Animated text content (title, rating, description)
            AnimatedContent(
                targetState = displayItem,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500, delayMillis = 200)) togetherWith
                        fadeOut(animationSpec = tween(300))
                },
                label = "hero_text_content",
            ) { item ->
                Column {
                    // Title and subtitle (full width)
                    HeroTitleSection(item)

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(modifier = Modifier.fillMaxWidth(0.55f)) {
                        // Rating information row
                        HeroRatingRow(item)

                        Spacer(modifier = Modifier.height(6.dp))

                        // Description
                        HeroDescription(item, isPreviewMode)
                    }
                }
            }

            // Bottom section: Action buttons - positioned at bottom of hero area
            Box(modifier = Modifier.fillMaxWidth(0.55f)) {
                val (playButtonText, playButtonIconColor) = buildHeroPlayButtonText(
                    resumePositionTicks = displayItem.userData?.playbackPositionTicks ?: 0L,
                    runTimeTicks = displayItem.runTimeTicks ?: 0L,
                )
                HeroActionButtons(
                    onPlayClick = { onPlayClick(displayItem.id) },
                    onDetailsClick = { onMoreInfoClick(displayItem) },
                    playButtonFocusRequester = playButtonFocusRequester,
                    isPreviewMode = isPreviewMode,
                    playButtonText = playButtonText,
                    playButtonIconColor = playButtonIconColor,
                    onButtonFocused = {
                        playButtonShouldHaveFocus = true
                        onPreviewClear?.invoke()
                    },
                    leftEdgeFocusRequester = leftEdgeFocusRequester,
                )
            }
        }
    }
}

/**
 * Build the backdrop URL for an item, using series backdrop for episodes.
 */
internal fun buildBackdropUrl(item: JellyfinItem, jellyfinClient: JellyfinClient): String {
    val backdropTag = item.backdropImageTags?.firstOrNull()
    if (backdropTag != null) {
        return jellyfinClient.getBackdropUrl(item.id, backdropTag, maxWidth = 1920)
    }

    // For episodes, try series backdrop first
    if (item.isEpisode && item.seriesId != null) {
        return jellyfinClient.getBackdropUrl(item.seriesId!!, null, maxWidth = 1920)
    }

    // Fallback to thumbnail when available
    item.imageTags?.thumb?.let { thumb ->
        return jellyfinClient.getThumbUrl(item.id, thumb, maxWidth = 1920)
    }

    // Last resort: request backdrop without tag
    return jellyfinClient.getBackdropUrl(item.id, null, maxWidth = 1920)
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
                    fontSize = 24.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Episode name as subtitle (bumped up 1 from titleSmall)
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            // Non-episodes: just show the name as main title
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                ),
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Row displaying rating information with dot separators.
 * Episodes: S# E# · air date · duration · parental rating · community rating
 * Series: Year · parental rating · community rating
 * Movies: Year · duration · parental rating · community rating · critic rating
 */
@Composable
private fun HeroRatingRow(item: JellyfinItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            item.isEpisode -> {
                // Episode-specific order: S# E# · air date · duration · rating · stars
                var needsDot = false

                // Season and Episode number
                val season = item.parentIndexNumber
                val episode = item.indexNumber
                if (season != null && episode != null) {
                    Text(
                        text = "S$season E$episode",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    )
                    needsDot = true
                }

                // Full air date
                item.formattedFullPremiereDate?.let { date ->
                    if (needsDot) DotSeparator()
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    )
                    needsDot = true
                }

                // Runtime
                item.runTimeTicks?.let { ticks ->
                    val minutes = (ticks / 600_000_000).toInt()
                    if (minutes > 0) {
                        if (needsDot) DotSeparator()
                        RuntimeDisplay(minutes)
                        needsDot = true
                    }
                }

                // Official rating badge (PG-13, TV-MA, etc.)
                item.officialRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    RatingBadge(rating)
                    needsDot = true
                }

                // Community rating (star rating)
                item.communityRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    StarRating(rating)
                }
            }

            item.isSeries -> {
                // Series order: year · parental rating · community rating
                var needsDot = false

                // Production year
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    )
                    needsDot = true
                }

                // Official rating badge (PG-13, TV-MA, etc.)
                item.officialRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    RatingBadge(rating)
                    needsDot = true
                }

                // Community rating (star rating)
                item.communityRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    StarRating(rating)
                }
            }

            item.isMovie -> {
                // Movies: year · duration · parental rating · community rating · critic rating
                var needsDot = false

                // Production year
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    )
                    needsDot = true
                }

                // Runtime
                item.runTimeTicks?.let { ticks ->
                    val minutes = (ticks / 600_000_000).toInt()
                    if (minutes > 0) {
                        if (needsDot) DotSeparator()
                        RuntimeDisplay(minutes)
                        needsDot = true
                    }
                }

                // Official rating badge (PG-13, TV-MA, etc.)
                item.officialRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    RatingBadge(rating)
                    needsDot = true
                }

                // Community rating (star rating)
                item.communityRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    StarRating(rating)
                    needsDot = true
                }

                // Critic rating (Rotten Tomatoes style with icon and percentage) - movies only
                item.criticRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    CriticRatingBadge(rating)
                }
            }

            else -> {
                // Other items: year · parental rating · community rating
                var needsDot = false

                // Production year
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextPrimary.copy(alpha = 0.9f),
                    )
                    needsDot = true
                }

                // Official rating badge (PG-13, TV-MA, etc.)
                item.officialRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    RatingBadge(rating)
                    needsDot = true
                }

                // Community rating (star rating)
                item.communityRating?.let { rating ->
                    if (needsDot) DotSeparator()
                    StarRating(rating)
                }
            }
        }
    }
}

/**
 * Small dot separator for metadata rows.
 */
@Composable
private fun DotSeparator() {
    Text(
        text = "•",
        style = MaterialTheme.typography.bodySmall,
        color = TvColors.TextPrimary.copy(alpha = 0.6f),
    )
}

/**
 * Premiere date badge for upcoming episodes.
 * Styled to match RatingBadge (TV-14 style).
 */
@Composable
private fun PremiereDateBadge(date: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TvColors.SurfaceElevated.copy(alpha = 0.8f))
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = TvColors.TextPrimary,
        )
    }
}

/**
 * Description text - 3 lines normally, 4 lines in preview mode.
 */
@Composable
private fun HeroDescription(item: JellyfinItem, isPreviewMode: Boolean = false) {
    item.overview?.let { overview ->
        Text(
            text = overview,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary.copy(alpha = 0.9f), // Bright white, slightly softened
            maxLines = if (isPreviewMode) 4 else 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(0.8f),
        )
    }
}

/**
 * Action buttons: Play and More Info (20dp height).
 * In preview mode, buttons are invisible but still focusable for navigation.
 */
@Composable
private fun HeroActionButtons(
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
    isPreviewMode: Boolean = false,
    playButtonText: String = "Play",
    playButtonIconColor: Color = IconColors.Play,
    onButtonFocused: (() -> Unit)? = null,
    leftEdgeFocusRequester: FocusRequester? = null,
) {
    // Alpha is 0 in preview mode (invisible but focusable)
    val buttonsAlpha = if (isPreviewMode) 0f else 1f

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.alpha(buttonsAlpha),
    ) {
        // Play button - receives initial focus on app launch
        // Left navigation goes to NavRail sentinel
        // alwaysExpanded = true because buttons are hidden when not in hero focus area
        ExpandablePlayButton(
            title = playButtonText,
            icon = Icons.Outlined.PlayArrow,
            iconColor = playButtonIconColor,
            onClick = onPlayClick,
            alwaysExpanded = true,
            modifier = Modifier
                .focusRequester(playButtonFocusRequester)
                .then(
                    if (leftEdgeFocusRequester != null) {
                        Modifier.focusProperties { left = leftEdgeFocusRequester }
                    } else {
                        Modifier
                    }
                )
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onButtonFocused?.invoke()
                    }
                },
        )

        // More Info button
        ExpandablePlayButton(
            title = "More Info",
            icon = Icons.Outlined.Info,
            iconColor = IconColors.Info,
            onClick = onDetailsClick,
            alwaysExpanded = true,
            modifier = Modifier
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onButtonFocused?.invoke()
                    }
                },
        )
    }
}

/**
 * Builds the play button text and icon color based on resume state.
 * Returns Pair of (text, iconColor).
 */
private fun buildHeroPlayButtonText(
    resumePositionTicks: Long,
    runTimeTicks: Long,
): Pair<String, Color> {
    return if (resumePositionTicks > 0L && runTimeTicks > 0L) {
        val remainingTicks = runTimeTicks - resumePositionTicks
        val remainingMinutes = (remainingTicks / 600_000_000L).toInt()
        val text = if (remainingMinutes > 0) {
            "Resume · ${remainingMinutes}m left"
        } else {
            "Resume"
        }
        text to IconColors.Resume
    } else {
        "Play" to IconColors.Play
    }
}

// === Rating Components ===

/**
 * Badge for official ratings (PG-13, TV-MA, etc.) with color-coded backgrounds.
 */
@Composable
private fun RatingBadge(text: String) {
    val backgroundColor = getRatingColor(text)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = Color.White,
        )
    }
}

/**
 * Get the background color for a content rating.
 */
private fun getRatingColor(rating: String): Color {
    val normalizedRating = rating.uppercase().trim()
    return when {
        // Green - Family friendly
        normalizedRating in listOf("G", "TV-G", "TV-Y", "TV-Y7", "TV-Y7-FV") ->
            Color(0xFF2E7D32) // Green 800

        // Blue - General/Parental guidance
        normalizedRating in listOf("PG", "TV-PG") ->
            Color(0xFF1565C0) // Blue 800

        // Orange - Teen/Caution
        normalizedRating in listOf("PG-13", "TV-14", "16") ->
            Color(0xFFF57C00) // Orange 700

        // Red - Restricted/Mature
        normalizedRating in listOf("R", "TV-MA", "NC-17", "NR", "UNRATED") ->
            Color(0xFFC62828) // Red 800

        // Default gray for unknown ratings
        else -> Color(0xFF616161) // Gray 700
    }
}

/**
 * Star rating display (community rating out of 10).
 */
@Composable
private fun StarRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFFD700), // Gold color
        )
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Tomato/Splat icon
        Icon(
            painter = painterResource(
                id = if (isFresh) R.drawable.ic_rotten_tomatoes_fresh else R.drawable.ic_rotten_tomatoes_rotten,
            ),
            contentDescription = if (isFresh) "Fresh" else "Rotten",
            modifier = Modifier.size(16.dp),
            tint = Color.Unspecified, // Use original icon colors
        )
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = TvColors.TextPrimary,
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
        color = TvColors.TextPrimary.copy(alpha = 0.9f), // Bright white
    )
}

// === Shimmer Loading Placeholder ===

/**
 * Shimmer loading placeholder for the hero section.
 */
@Composable
fun HeroSectionShimmer(modifier: Modifier = Modifier,) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HERO_HEIGHT)
            .background(TvColors.SurfaceElevated),
    ) {
        // Placeholder content on left side
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .padding(top = 32.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvColors.TextSecondary.copy(alpha = 0.2f)),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating row placeholder
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvColors.TextSecondary.copy(alpha = 0.15f)),
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
                    .background(TvColors.TextSecondary.copy(alpha = 0.15f)),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvColors.TextSecondary.copy(alpha = 0.15f)),
            )
        }
    }
}
