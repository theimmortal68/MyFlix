package com.myflix.app.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Hero section displaying featured media at the top of the home page.
 * Takes 30% of the screen height with a dynamic backdrop that fades to transparent.
 *
 * @param featuredItems List of items to cycle through in the hero
 * @param serverUrl Base URL for the Jellyfin server
 * @param onItemSelected Callback when user selects/clicks an item
 * @param onPlayClicked Callback when user clicks the play button
 * @param modifier Modifier for the hero section
 * @param autoRotateIntervalMs Time between automatic item rotations (default 8 seconds)
 */
@Composable
fun HeroSection(
    featuredItems: List<BaseItemDto>,
    serverUrl: String,
    onItemSelected: (BaseItemDto) -> Unit,
    onPlayClicked: (BaseItemDto) -> Unit,
    modifier: Modifier = Modifier,
    autoRotateIntervalMs: Long = 8000L
) {
    if (featuredItems.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentItem = featuredItems[currentIndex]

    // Auto-rotate through featured items
    LaunchedEffect(featuredItems.size) {
        if (featuredItems.size > 1) {
            while (true) {
                delay(autoRotateIntervalMs)
                currentIndex = (currentIndex + 1) % featuredItems.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.30f) // 30% of screen height
    ) {
        // Animated backdrop with crossfade
        AnimatedContent(
            targetState = currentItem,
            transitionSpec = {
                fadeIn(animationSpec = tween(700)) togetherWith
                    fadeOut(animationSpec = tween(700))
            },
            label = "hero_backdrop"
        ) { item ->
            HeroBackdrop(
                item = item,
                serverUrl = serverUrl
            )
        }

        // Gradient overlays for fade effect
        HeroGradientOverlays()

        // Content overlay (left half)
        AnimatedContent(
            targetState = currentItem,
            transitionSpec = {
                fadeIn(animationSpec = tween(500, delayMillis = 200)) togetherWith
                    fadeOut(animationSpec = tween(300))
            },
            label = "hero_content"
        ) { item ->
            HeroContentOverlay(
                item = item,
                onPlayClicked = { onPlayClicked(item) },
                onDetailsClicked = { onItemSelected(item) }
            )
        }

        // Page indicators (bottom center)
        if (featuredItems.size > 1) {
            HeroPageIndicators(
                itemCount = featuredItems.size,
                currentIndex = currentIndex,
                onIndicatorClicked = { index -> currentIndex = index },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

/**
 * Backdrop image for the hero section.
 */
@Composable
private fun HeroBackdrop(
    item: BaseItemDto,
    serverUrl: String
) {
    val context = LocalContext.current
    val backdropUrl = buildBackdropUrl(item, serverUrl)

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(backdropUrl)
            .crossfade(true)
            .build(),
        contentDescription = item.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Gradient overlays for the hero section.
 * Creates fade effects at the top (toward nav bar) and bottom (toward content rows).
 */
@Composable
private fun HeroGradientOverlays() {
    val backgroundColor = MaterialTheme.colorScheme.background

    // Top gradient (fade toward nav bar)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.25f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.9f),
                        backgroundColor.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
    )

    // Bottom gradient (fade toward content rows)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        backgroundColor.copy(alpha = 0.7f),
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )

    // Left side gradient for text readability
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.85f),
                        backgroundColor.copy(alpha = 0.5f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = Float.POSITIVE_INFINITY
                )
            )
    )
}

/**
 * Content overlay displaying media information on the left side.
 */
@Composable
private fun HeroContentOverlay(
    item: BaseItemDto,
    onPlayClicked: () -> Unit,
    onDetailsClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f) // Left half of screen
            .padding(start = 48.dp, top = 48.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Title and subtitle
        HeroTitleSection(item)

        Spacer(modifier = Modifier.height(12.dp))

        // Rating information row
        HeroRatingRow(item)

        Spacer(modifier = Modifier.height(16.dp))

        // Description (2 lines max)
        HeroDescription(item)

        Spacer(modifier = Modifier.height(20.dp))

        // Action buttons
        HeroActionButtons(
            onPlayClicked = onPlayClicked,
            onDetailsClicked = onDetailsClicked
        )
    }
}

/**
 * Title section with main title and subtitle for episodes.
 */
@Composable
private fun HeroTitleSection(item: BaseItemDto) {
    // For episodes, show series name as subtitle
    val isEpisode = item.type == BaseItemKind.EPISODE
    
    if (isEpisode && !item.seriesName.isNullOrBlank()) {
        Text(
            text = item.seriesName!!,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    // Main title
    Text(
        text = buildTitle(item),
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Row displaying rating information: Official rating, stars, Rotten Tomatoes, year, runtime.
 */
@Composable
private fun HeroRatingRow(item: BaseItemDto) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official rating (PG-13, TV-MA, etc.)
        item.officialRating?.let { rating ->
            RatingBadge(text = rating)
        }

        // Production year
        item.productionYear?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        // Community rating (stars)
        item.communityRating?.let { rating ->
            StarRating(rating = rating)
        }

        // Critic rating (Rotten Tomatoes style)
        item.criticRating?.let { rating ->
            CriticRatingBadge(rating = rating)
        }

        // Runtime
        item.runTimeTicks?.let { ticks ->
            val minutes = (ticks / 600_000_000).toInt()
            if (minutes > 0) {
                RuntimeDisplay(minutes = minutes)
            }
        }
    }
}

/**
 * Description text limited to 2 lines.
 */
@Composable
private fun HeroDescription(item: BaseItemDto) {
    item.overview?.let { overview ->
        Text(
            text = overview,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 24.sp
        )
    }
}

/**
 * Action buttons: Play and More Info.
 */
@Composable
private fun HeroActionButtons(
    onPlayClicked: () -> Unit,
    onDetailsClicked: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Play button
        Button(
            onClick = onPlayClicked,
            modifier = Modifier.focusable(),
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play")
        }

        // More Info button
        Button(
            onClick = onDetailsClicked,
            modifier = Modifier.focusable(),
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("More Info")
        }
    }
}

/**
 * Page indicators showing which item is currently displayed.
 */
@Composable
private fun HeroPageIndicators(
    itemCount: Int,
    currentIndex: Int,
    onIndicatorClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(itemCount) { index ->
            Surface(
                onClick = { onIndicatorClicked(index) },
                modifier = Modifier
                    .width(if (index == currentIndex) 24.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = if (index == currentIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            ) {}
        }
    }
}

// === Rating Components ===

/**
 * Badge for official ratings (PG-13, TV-MA, etc.)
 */
@Composable
private fun RatingBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFFFFD700) // Gold color
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

/**
 * Critic rating badge (Rotten Tomatoes style percentage).
 */
@Composable
private fun CriticRatingBadge(rating: Float) {
    val percentage = rating.roundToInt()
    val isFresh = percentage >= 60
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Tomato icon representation
        Surface(
            shape = RoundedCornerShape(2.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = if (isFresh) Color(0xFFFA320A) else Color(0xFF6C9A50)
            ),
            modifier = Modifier.size(16.dp)
        ) {}
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
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
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

// === Helper Functions ===

/**
 * Build the backdrop URL for an item.
 */
private fun buildBackdropUrl(item: BaseItemDto, serverUrl: String): String {
    // Try item's own backdrop first
    val backdropId = when {
        !item.backdropImageTags.isNullOrEmpty() -> item.id
        // For episodes, use series backdrop
        item.type == BaseItemKind.EPISODE && item.seriesId != null -> item.seriesId
        // For seasons, use series backdrop
        item.type == BaseItemKind.SEASON && item.seriesId != null -> item.seriesId
        // Fallback to primary image
        else -> item.id
    }

    val imageType = if (!item.backdropImageTags.isNullOrEmpty() || 
        (item.type == BaseItemKind.EPISODE && item.seriesId != null)) {
        "Backdrop"
    } else {
        "Primary"
    }

    return "$serverUrl/Items/$backdropId/Images/$imageType?maxWidth=1920&quality=90"
}

/**
 * Build display title for an item.
 */
private fun buildTitle(item: BaseItemDto): String {
    return when (item.type) {
        BaseItemKind.EPISODE -> {
            val seasonNum = item.parentIndexNumber
            val episodeNum = item.indexNumber
            val episodeName = item.name
            if (seasonNum != null && episodeNum != null) {
                "S$seasonNum E$episodeNum - $episodeName"
            } else {
                item.name ?: ""
            }
        }
        else -> item.name ?: ""
    }
}
