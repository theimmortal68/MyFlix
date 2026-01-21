@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.formattedPremiereDate
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.common.model.isUpcomingEpisode
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.theme.IconColors
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
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
fun HeroBackdropLayer(item: JellyfinItem?, jellyfinClient: JellyfinClient, modifier: Modifier = Modifier,) {
    if (item == null) return

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = item,
            transitionSpec = {
                fadeIn(animationSpec = tween(800)) togetherWith
                    fadeOut(animationSpec = tween(800))
            },
            label = "hero_backdrop_layer",
            modifier = Modifier.fillMaxSize(),
        ) { currentItem ->
            val backdropUrl = buildBackdropUrl(currentItem, jellyfinClient)

            AsyncImage(
                model = backdropUrl,
                contentDescription = currentItem.name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.9f)
                    .drawWithContent {
                        drawContent()
                        // Left edge fade - opaque 50-100%, half at 40%, transparent at 0%
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.4f to Color.Black.copy(alpha = 0.5f),
                                    0.5f to Color.Black,
                                    1.0f to Color.Black,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                        // Bottom edge fade - for content row blending
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    0.5f to Color.Black.copy(alpha = 0.85f),
                                    0.7f to Color.Black.copy(alpha = 0.4f),
                                    0.85f to Color.Black.copy(alpha = 0.15f),
                                    1.0f to Color.Transparent,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
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
 * @param downFocusRequester Focus target when navigating down from buttons
 * @param onCurrentItemChanged Callback when the displayed item changes (for dynamic background)
 * @param onPreviewClear Callback when focus returns to hero buttons (to clear preview)
 * @param onUpPressed Callback when UP is pressed on hero buttons (to show nav bar)
 * @param navBarVisible Whether the nav bar popup is currently visible (prevents focus stealing)
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
    upFocusRequester: FocusRequester? = null,
    onCurrentItemChanged: ((JellyfinItem, String?) -> Unit)? = null,
    onPreviewClear: (() -> Unit)? = null,
    onUpPressed: (() -> Unit)? = null,
    navBarVisible: Boolean = false,
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

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        // Content overlay (left side) - backdrop is in HeroBackdropLayer
        AnimatedContent(
            targetState = displayItem,
            transitionSpec = {
                fadeIn(animationSpec = tween(500, delayMillis = 200)) togetherWith
                    fadeOut(animationSpec = tween(300))
            },
            label = "hero_content",
        ) { item ->
            // Always render content overlay with buttons - buttons are invisible in preview mode
            // but still focusable so up navigation works
            HeroContentOverlay(
                item = item,
                itemId = item.id,
                onPlayClick = { onPlayClick(item.id) },
                onDetailsClick = { onItemClick(item.id) },
                playButtonFocusRequester = playButtonFocusRequester,
                downFocusRequester = downFocusRequester,
                upFocusRequester = upFocusRequester,
                isPreviewMode = isPreviewMode,
                onButtonFocused = {
                    // Track that hero buttons have focus (for restoration after rotation)
                    playButtonShouldHaveFocus = true
                    onPreviewClear?.invoke()
                },
                shouldRestoreFocus = playButtonShouldHaveFocus && !isPreviewMode && !navBarVisible,
                onUpPressed = onUpPressed,
            )
        }
    }
}

/**
 * Build the backdrop URL for an item, using series backdrop for episodes.
 */
private fun buildBackdropUrl(item: JellyfinItem, jellyfinClient: JellyfinClient): String {
    // For episodes, use series backdrop if available
    val backdropId = if (!item.backdropImageTags.isNullOrEmpty()) {
        item.id
    } else if (item.isEpisode && item.seriesId != null) {
        item.seriesId!!
    } else {
        item.id
    }

    val tag = if (!item.backdropImageTags.isNullOrEmpty()) {
        item.backdropImageTags!!.firstOrNull()
    } else {
        null
    }

    return jellyfinClient.getBackdropUrl(backdropId, tag, maxWidth = 1920)
}

/**
 * Content overlay displaying media information on the left side.
 */
@Composable
private fun HeroContentOverlay(
    item: JellyfinItem,
    itemId: String,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    isPreviewMode: Boolean = false,
    onButtonFocused: (() -> Unit)? = null,
    shouldRestoreFocus: Boolean = false,
    onUpPressed: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .padding(start = 10.dp, top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        // Title and subtitle
        HeroTitleSection(item)

        Spacer(modifier = Modifier.height(6.dp))

        // Rating information row
        HeroRatingRow(item)

        Spacer(modifier = Modifier.height(6.dp))

        // Description (3 lines normal, 4 lines in preview mode)
        HeroDescription(item, isPreviewMode)

        Spacer(modifier = Modifier.height(10.dp))

        // Action buttons (20dp height) - hidden in preview mode but still focusable
        HeroActionButtons(
            itemId = itemId,
            onPlayClick = onPlayClick,
            onDetailsClick = onDetailsClick,
            playButtonFocusRequester = playButtonFocusRequester,
            downFocusRequester = downFocusRequester,
            upFocusRequester = upFocusRequester,
            isPreviewMode = isPreviewMode,
            onButtonFocused = onButtonFocused,
            shouldRestoreFocus = shouldRestoreFocus,
            onUpPressed = onUpPressed,
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
 * Row displaying rating information: Official rating, stars, critic rating, year, runtime.
 */
@Composable
private fun HeroRatingRow(item: JellyfinItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
                color = TvColors.TextPrimary.copy(alpha = 0.9f), // Bright white
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

        // Premiere date (only for upcoming episodes)
        if (item.isUpcomingEpisode) {
            item.formattedPremiereDate?.let { date ->
                PremiereDateBadge(date)
            }
        }
    }
}

/**
 * Premiere date badge for upcoming episodes.
 * Styled to match RatingBadge (TV-14 style).
 */
@Composable
private fun PremiereDateBadge(date: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TvColors.SurfaceElevated.copy(alpha = 0.8f)),
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
 * Uses onPreviewKeyEvent to intercept UP key for showing nav bar.
 */
@Composable
private fun HeroActionButtons(
    itemId: String,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    playButtonFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    isPreviewMode: Boolean = false,
    onButtonFocused: (() -> Unit)? = null,
    shouldRestoreFocus: Boolean = false,
    onUpPressed: (() -> Unit)? = null,
) {
    // Alpha is 0 in preview mode (invisible but focusable)
    val buttonsAlpha = if (isPreviewMode) 0f else 1f

    // Restore focus when item changes - delay to wait for AnimatedContent transition
    // The transition is fadeIn(500ms, 200ms delay) + fadeOut(300ms), so wait 600ms
    LaunchedEffect(shouldRestoreFocus, itemId) {
        if (shouldRestoreFocus) {
            delay(600L)
            try {
                playButtonFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .alpha(buttonsAlpha)
            .onPreviewKeyEvent { keyEvent ->
                // Intercept UP key to show nav bar popup
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                    onUpPressed?.invoke()
                    onUpPressed != null // Consume event if callback exists
                } else {
                    false
                }
            },
    ) {
        // Play button - receives initial focus
        ExpandableHeroButton(
            text = "Play",
            icon = Icons.Outlined.PlayArrow,
            iconTint = IconColors.Play,
            onClick = onPlayClick,
            modifier = Modifier
                .focusRequester(playButtonFocusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onButtonFocused?.invoke()
                    }
                }
                .focusProperties {
                    if (downFocusRequester != null) {
                        down = downFocusRequester
                    }
                    if (upFocusRequester != null) {
                        up = upFocusRequester
                    }
                },
        )

        // More Info button
        ExpandableHeroButton(
            text = "More Info",
            icon = Icons.Outlined.Info,
            iconTint = IconColors.Info,
            onClick = onDetailsClick,
            modifier = Modifier
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onButtonFocused?.invoke()
                    }
                }
                .focusProperties {
                    if (downFocusRequester != null) {
                        down = downFocusRequester
                    }
                    if (upFocusRequester != null) {
                        up = upFocusRequester
                    }
                },
        )
    }
}

@Composable
private fun ExpandableHeroButton(
    text: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        modifier = modifier.height(20.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        interactionSource = interactionSource,
        scale = ButtonDefaults.scale(focusedScale = 1f),
        glow = ButtonDefaults.glow(
            focusedGlow = Glow(
                elevation = 10.dp,
                elevationColor = TvColors.BluePrimary.copy(alpha = 0.5f)
            )
        ),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isFocused) Color.White else iconTint
        )
        AnimatedVisibility(visible = isFocused) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
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
            .background(TvColors.SurfaceElevated.copy(alpha = 0.8f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                .padding(start = 10.dp, top = 32.dp, bottom = 16.dp),
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