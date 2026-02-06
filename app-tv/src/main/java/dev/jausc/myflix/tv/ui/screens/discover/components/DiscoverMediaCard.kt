@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.discover.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Card dimensions for Discover media cards.
 */
object DiscoverCardSizes {
    val CardWidth = 110.dp
    val CardHeight = 165.dp
    val CornerRadius = 12.dp
}

/**
 * Badge colors for type indicators.
 */
private object BadgeColors {
    val MovieBlue = Color(0xFF2E5AC1)
    val TvPurple = Color(0xFF9D29BC)
    val AvailableGreen = Color(0xFF22C55E)
    val PendingPurple = Color(0xFF9D29BC)
}

/**
 * A media card for the Discover screen showing poster, type badge, and availability status.
 *
 * @param media The SeerrMedia item to display
 * @param onClick Callback when the card is clicked
 * @param modifier Optional modifier
 * @param onFocusChanged Optional callback when focus state changes
 */
@Composable
fun DiscoverMediaCard(
    media: SeerrMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    // Scale animation when focused
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale",
    )

    val cardShape = RoundedCornerShape(DiscoverCardSizes.CornerRadius)

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(DiscoverCardSizes.CardWidth)
            .height(DiscoverCardSizes.CardHeight)
            .scale(scale)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                onFocusChanged?.invoke(focusState.isFocused)
            },
        shape = ClickableSurfaceDefaults.shape(shape = cardShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f), // We handle scale ourselves
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
                .then(
                    if (isFocused) {
                        Modifier.border(
                            BorderStroke(2.dp, TvColors.BluePrimary),
                            cardShape,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            // Background - either poster or gradient
            val posterUrl = media.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" }

            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = media.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Gradient background when no poster
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF16213E),
                                ),
                            ),
                        ),
                )
            }

            // Type badge (top-left): MOVIE or TV
            TypeBadge(
                isMovie = media.isMovie,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )

            // Status indicator (top-right): Available or Pending
            media.availabilityStatus?.let { status ->
                StatusIndicator(
                    status = status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }
    }
}

/**
 * Type badge showing MOVIE or TV.
 */
@Composable
private fun TypeBadge(
    isMovie: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isMovie) BadgeColors.MovieBlue else BadgeColors.TvPurple
    val text = if (isMovie) "MOVIE" else "TV"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

/**
 * Status indicator showing availability state.
 */
@Composable
private fun StatusIndicator(
    status: Int,
    modifier: Modifier = Modifier,
) {
    when (status) {
        SeerrMediaStatus.AVAILABLE, SeerrMediaStatus.PARTIALLY_AVAILABLE -> {
            // Green checkmark for available
            Box(
                modifier = modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Available",
                    tint = BadgeColors.AvailableGreen,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> {
            // Purple clock for pending
            Box(
                modifier = modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = "Pending",
                    tint = BadgeColors.PendingPurple,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        // No indicator for other statuses
    }
}
