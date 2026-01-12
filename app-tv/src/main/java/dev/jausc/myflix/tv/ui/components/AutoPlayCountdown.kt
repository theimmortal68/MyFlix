package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.QueueItem
import dev.jausc.myflix.tv.ui.theme.TvColors

private const val COUNTDOWN_TOTAL_SECONDS = 5

/**
 * Auto-play countdown overlay shown at the end of a video when there's a next item in the queue.
 *
 * @param nextItem The next item to play
 * @param countdownSeconds Current countdown value (5, 4, 3, 2, 1)
 * @param jellyfinClient Client for thumbnail URL
 * @param onPlayNow Called when user wants to skip countdown and play immediately
 * @param onCancel Called when user wants to cancel the queue
 * @param modifier Modifier for positioning
 */
@Composable
fun AutoPlayCountdown(
    nextItem: QueueItem,
    countdownSeconds: Int,
    jellyfinClient: JellyfinClient,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playNowFocusRequester = remember { FocusRequester() }

    // Auto-focus the Play Now button when shown
    LaunchedEffect(Unit) {
        playNowFocusRequester.requestFocus()
    }

    // Animate progress bar
    val progress by animateFloatAsState(
        targetValue = countdownSeconds.toFloat() / COUNTDOWN_TOTAL_SECONDS,
        animationSpec = tween(durationMillis = 300),
        label = "countdown_progress",
    )

    // Build thumbnail URL
    val thumbItemId = nextItem.thumbnailItemId ?: nextItem.itemId
    val thumbnailUrl: String = remember(thumbItemId) {
        jellyfinClient.getPrimaryImageUrl(thumbItemId, tag = null, maxWidth = 200)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.9f),
                    ),
                ),
            )
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    TvColors.Surface.copy(alpha = 0.95f),
                    RoundedCornerShape(12.dp),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Title and countdown info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Up Next",
                    color = TvColors.TextSecondary,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nextItem.title,
                    color = TvColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                nextItem.episodeInfo?.let { info ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = info,
                        color = TvColors.TextSecondary,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Custom progress bar (TV Material3 doesn't have LinearProgressIndicator)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TvColors.SurfaceLight),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(TvColors.BlueAccent),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Playing in $countdownSeconds second${if (countdownSeconds != 1) "s" else ""}",
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onPlayNow,
                    modifier = Modifier.focusRequester(playNowFocusRequester),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.BluePrimary,
                        focusedContainerColor = TvColors.BlueLight,
                    ),
                ) {
                    Text(
                        text = "Play Now",
                        color = TvColors.TextPrimary,
                    )
                }

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceLight,
                        focusedContainerColor = TvColors.Surface,
                    ),
                ) {
                    Text(
                        text = "Cancel",
                        color = TvColors.TextPrimary,
                    )
                }
            }
        }
    }
}
