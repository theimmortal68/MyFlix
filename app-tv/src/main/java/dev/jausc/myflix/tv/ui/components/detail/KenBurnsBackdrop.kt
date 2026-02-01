@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Preset configurations for Ken Burns backdrop fade effects.
 */
enum class KenBurnsFadePreset {
    /** Standard fade for series detail screen - stronger gradient overlay */
    SERIES_DETAIL,
    /** Softer fade for episodes screen - more image visible */
    EPISODES,
}

/**
 * Ken Burns effect backdrop - slow zoom and pan animation with configurable fade.
 *
 * @param imageUrl The URL of the backdrop image
 * @param modifier Modifier for the backdrop
 * @param fadePreset The preset configuration for fade effects
 */
@Composable
fun KenBurnsBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fadePreset: KenBurnsFadePreset = KenBurnsFadePreset.SERIES_DETAIL,
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")

    // Slow zoom: 1.0 -> 1.1 over 20 seconds
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    // Subtle horizontal pan: -2% to +2%
    val translateX by infiniteTransition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "translateX",
    )

    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .build()
    }

    // Get fade configuration based on preset
    val fadeConfig = when (fadePreset) {
        KenBurnsFadePreset.SERIES_DETAIL -> FadeConfig(
            leftFadeStart = 0.15f,
            leftFadeMid = 0.35f,
            bottomFadeMid = 0.6f,
            overlayAlpha = 0.8f,
            overlayFadeEnd = 0.3f,
        )
        KenBurnsFadePreset.EPISODES -> FadeConfig(
            leftFadeStart = 0.05f,
            leftFadeMid = 0.10f,
            bottomFadeMid = 0.7f,
            overlayAlpha = 0.15f,
            overlayFadeEnd = 0.05f,
        )
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = size.width * translateX
                }
                .drawWithCache {
                    // Edge fade masks
                    val leftFade = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            fadeConfig.leftFadeStart to Color.Black.copy(alpha = 0.5f),
                            fadeConfig.leftFadeMid to Color.Black,
                            1.0f to Color.Black,
                        ),
                    )
                    val bottomFade = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            fadeConfig.bottomFadeMid to Color.Black,
                            1.0f to Color.Transparent,
                        ),
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(leftFade, blendMode = BlendMode.DstIn)
                        drawRect(bottomFade, blendMode = BlendMode.DstIn)
                    }
                },
        )

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to TvColors.Background.copy(alpha = fadeConfig.overlayAlpha),
                            fadeConfig.overlayFadeEnd to TvColors.Background.copy(alpha = fadeConfig.overlayAlpha * 0.5f),
                            fadeConfig.overlayFadeEnd * 2 to Color.Transparent,
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/**
 * Internal configuration for fade effects.
 */
private data class FadeConfig(
    val leftFadeStart: Float,
    val leftFadeMid: Float,
    val bottomFadeMid: Float,
    val overlayAlpha: Float,
    val overlayFadeEnd: Float,
)
