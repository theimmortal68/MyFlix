@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
    /** Home screen fade - matches HeroBackdropLayer's edge blending */
    HOME_SCREEN,
    /** Movie detail screen - balanced fade for cinematic feel */
    MOVIE,
    /** Discover detail screen - similar to movie but slightly softer */
    DISCOVER_DETAIL,
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
            leftFadeStart = 0.08f,
            leftFadeMid = 0.18f,
            bottomFadeMid = 0.7f,
            overlayAlpha = 0.15f,
            overlayFadeEnd = 0.05f,
        )
        KenBurnsFadePreset.HOME_SCREEN -> FadeConfig(
            leftFadeStart = 0.10f,
            leftFadeMid = 0.30f,
            bottomFadeMid = 0.4f,
            overlayAlpha = 0.7f,
            overlayFadeEnd = 0.4f,
        )
        KenBurnsFadePreset.MOVIE -> FadeConfig(
            leftFadeStart = 0.12f,
            leftFadeMid = 0.28f,
            bottomFadeMid = 0.55f,
            overlayAlpha = 0.75f,
            overlayFadeEnd = 0.45f,
        )
        KenBurnsFadePreset.DISCOVER_DETAIL -> FadeConfig(
            leftFadeStart = 0.10f,
            leftFadeMid = 0.25f,
            bottomFadeMid = 0.50f,
            overlayAlpha = 0.70f,
            overlayFadeEnd = 0.40f,
        )
    }

    Box(modifier = modifier.clipToBounds()) {
        // Crossfade between different backdrop images
        Crossfade(
            targetState = imageUrl,
            animationSpec = tween(600),
            label = "backdrop_crossfade",
        ) { currentUrl ->
            KenBurnsBackdropContent(
                imageUrl = currentUrl,
                fadeConfig = fadeConfig,
            )
        }

        // Gradient overlay for text readability (outside Crossfade so it's stable)
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
 * Internal content for Ken Burns backdrop - the animated image with edge fade masks.
 * Separated so Crossfade can manage transitions between different images.
 */
@Composable
private fun KenBurnsBackdropContent(
    imageUrl: String?,
    fadeConfig: FadeConfig,
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")

    // Slow zoom: 1.03 -> 1.12 over 18 seconds
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.03f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    // Subtle horizontal pan: -1.5% to +1.5% over 22 seconds
    val translateX by infiniteTransition.animateFloat(
        initialValue = -0.015f,
        targetValue = 0.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "translateX",
    )

    val request = remember<ImageRequest>(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .build()
    }

    // Mask container - stable in screen space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val leftFade = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.002f to Color.Transparent,
                        fadeConfig.leftFadeStart to Color.Black.copy(alpha = 0.5f),
                        fadeConfig.leftFadeMid to Color.Black,
                        1.0f to Color.Black,
                    ),
                )
                val bottomFade = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Black,
                        fadeConfig.bottomFadeMid to Color.Black,
                        0.998f to Color.Transparent,
                    ),
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(leftFade, blendMode = BlendMode.DstIn)
                    drawRect(bottomFade, blendMode = BlendMode.DstIn)
                }
            },
    ) {
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
                },
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
