@file:Suppress(
    "MagicNumber",
)

package dev.jausc.myflix.core.common.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.jausc.myflix.core.common.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * Configuration for splash screen appearance.
 *
 * @param contentScale How the splash image should be scaled.
 *        Use [ContentScale.Crop] for TV (fills screen) or [ContentScale.Fit] for mobile (letterboxed).
 * @param coreGlowColor Color for the innermost spotlight glow
 * @param midGlowColor Color for the middle spotlight glow
 * @param wideGlowColor Color for the outer spotlight glow
 */
data class SplashScreenConfig(
    val contentScale: ContentScale = ContentScale.Fit,
    val coreGlowColor: Color = MyFlixColors.BlueAccent.copy(alpha = 0.35f),
    val midGlowColor: Color = MyFlixColors.BlueLight.copy(alpha = 0.20f),
    val wideGlowColor: Color = MyFlixColors.BluePrimary.copy(alpha = 0.12f),
)

/**
 * Pre-configured splash screen settings for TV apps.
 * Uses [ContentScale.Crop] to fill the TV screen edge-to-edge.
 */
val SplashScreenTvConfig = SplashScreenConfig(
    contentScale = ContentScale.Crop,
)

/**
 * Pre-configured splash screen settings for mobile apps.
 * Uses [ContentScale.Fit] to show the full splash image with letterboxing.
 */
val SplashScreenMobileConfig = SplashScreenConfig(
    contentScale = ContentScale.Fit,
)

private const val MINIMUM_DISPLAY_TIME_MS = 3500L
private const val ANIMATION_DURATION_MS = 5200

/**
 * Splash screen with animated spotlight overlay.
 * Shows the master art with two lights tracing independent figure-8 patterns.
 * Animation loops until home page is loaded (minimum 3.5s).
 *
 * @param onFinished Callback invoked after the minimum display time has elapsed
 * @param config Configuration for appearance customization
 * @param modifier Modifier for the root container
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    config: SplashScreenConfig = SplashScreenMobileConfig,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(MINIMUM_DISPLAY_TIME_MS)
        onFinished()
    }

    SplashScreenContent(config = config, modifier = modifier)
}

@Composable
private fun SplashScreenContent(config: SplashScreenConfig, modifier: Modifier = Modifier,) {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight")

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.myflix_splash_master),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = config.contentScale,
        )

        Figure8SpotlightOverlay(
            progress = animationProgress,
            core = config.coreGlowColor,
            mid = config.midGlowColor,
            wide = config.wideGlowColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Two spotlights tracing independent figure-8 patterns using Lissajous curves.
 * Light 1: Standard figure-8, starts at center-right
 * Light 2: Rotated/offset figure-8, starts at different position
 */
@Composable
private fun Figure8SpotlightOverlay(
    progress: Float,
    core: Color,
    mid: Color,
    wide: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Letter region bounds (where the logo text appears)
        val letterLeft = w * 0.10f
        val letterRight = w * 0.90f
        val letterTop = h * 0.35f
        val letterBottom = h * 0.55f
        val letterWidth = letterRight - letterLeft
        val letterHeight = letterBottom - letterTop
        val letterCenterX = (letterLeft + letterRight) / 2f
        val letterCenterY = (letterTop + letterBottom) / 2f

        // Clip path with padding for glow effect
        val clipPadding = letterHeight * 2.5f
        val clipPath = Path().apply {
            addRect(
                Rect(
                    left = letterLeft - clipPadding,
                    top = letterTop - clipPadding,
                    right = letterRight + clipPadding,
                    bottom = letterBottom + clipPadding,
                ),
            )
        }

        clipPath(clipPath, ClipOp.Intersect) {
            fun figure8Position(t: Float): Offset {
                val normalizedX = sin(t)
                val normalizedY = sin(2 * t) * 0.8f

                val x = letterCenterX + (normalizedX * letterWidth * 0.45f)
                val y = letterCenterY + (normalizedY * letterHeight * 0.45f)

                return Offset(x, y)
            }

            fun drawBeam(center: Offset) {
                val beamRadius = letterHeight * 4.5f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(wide, Color.Transparent),
                        center = center,
                        radius = beamRadius,
                    ),
                    center = center,
                    radius = beamRadius,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(mid, Color.Transparent),
                        center = center,
                        radius = beamRadius * 0.6f,
                    ),
                    center = center,
                    radius = beamRadius * 0.6f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(core, Color.Transparent),
                        center = center,
                        radius = beamRadius * 0.3f,
                    ),
                    center = center,
                    radius = beamRadius * 0.3f,
                )
            }

            val baseAngle = progress * 4f * PI.toFloat()

            val pos1 = figure8Position(baseAngle)
            drawBeam(pos1)

            val pos2 = figure8Position(baseAngle * 1.15f + PI.toFloat() * 0.8f)
            drawBeam(pos2)
        }
    }
}
