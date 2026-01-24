package dev.jausc.myflix.tv.dream.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val ANIMATION_DURATION_MS = 30_000
private const val MIN_SCALE = 1.0f
private const val MAX_SCALE = 1.1f

/**
 * A container that applies a Ken Burns zoom effect to its content.
 *
 * The Ken Burns effect slowly zooms in and out on images, creating
 * a subtle motion that keeps static images visually interesting.
 * This is commonly used in screensavers and photo slideshows.
 *
 * @param modifier Modifier to apply to the container
 * @param content The content to apply the zoom effect to
 */
@Composable
fun ZoomBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit,) {
    val infiniteTransition = rememberInfiniteTransition(label = "kenBurns")

    val scale by infiniteTransition.animateFloat(
        initialValue = MIN_SCALE,
        targetValue = MAX_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = ANIMATION_DURATION_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "zoomScale",
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        content = content,
    )
}
