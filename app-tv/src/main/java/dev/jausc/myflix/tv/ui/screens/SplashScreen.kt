@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Splash screen with animated spotlight overlay.
 * Shows the master art with two lights tracing independent figure-8 patterns.
 * Animation loops until home page is loaded (minimum 3.5s).
 */
@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    // Minimum display time before allowing navigation
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3500L)
        onFinished()
    }

    SplashScreenContent(modifier = modifier)
}

@Composable
private fun SplashScreenContent(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight")

    // Animation cycles 0→1 in 5.2 seconds for 2 complete figure-8s (50% slower)
    // Each light uses this value differently based on phase offset
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
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
        // Master art (logo baked in)
        Image(
            painter = painterResource(R.drawable.myflix_splash_master),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Moving spotlights overlay - two independent figure-8 patterns
        Figure8SpotlightOverlay(
            progress = animationProgress,
            core = TvColors.BlueAccent.copy(alpha = 0.35f),
            mid = TvColors.BlueLight.copy(alpha = 0.20f),
            wide = TvColors.BluePrimary.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Two spotlights tracing independent figure-8 patterns.
 * Light 1: Standard figure-8, starts at center-right
 * Light 2: Rotated/offset figure-8, starts at different position
 */
@Composable
private fun Figure8SpotlightOverlay(
    progress: Float, // 0 to 1 for full animation cycle (2 figure-8s)
    core: Color,
    mid: Color,
    wide: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Letter region bounds
        val letterLeft = w * 0.10f
        val letterRight = w * 0.90f
        val letterTop = h * 0.35f
        val letterBottom = h * 0.55f
        val letterWidth = letterRight - letterLeft
        val letterHeight = letterBottom - letterTop
        val letterCenterX = (letterLeft + letterRight) / 2f
        val letterCenterY = (letterTop + letterBottom) / 2f

        // Clip path with padding for glow
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

        // Draw beams clipped to letter region
        clipPath(clipPath, ClipOp.Intersect) {
            /**
             * Calculate figure-8 position using Lissajous curve.
             * x = sin(t) traces left-right
             * y = sin(2t) creates the crossing pattern
             *
             * @param t Parameter from 0 to 4π for 2 complete figure-8s
             * @return Offset position within letter bounds
             */
            fun figure8Position(t: Float): Offset {
                // Lissajous curve for figure-8
                val normalizedX = sin(t) // -1 to 1
                val normalizedY = sin(2 * t) * 0.8f // -0.8 to 0.8 (slightly compressed vertically)

                // Map to letter bounds
                val x = letterCenterX + (normalizedX * letterWidth * 0.45f)
                val y = letterCenterY + (normalizedY * letterHeight * 0.45f)

                return Offset(x, y)
            }

            fun drawBeam(center: Offset) {
                val beamRadius = letterHeight * 4.5f

                // Wide glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(wide, Color.Transparent),
                        center = center,
                        radius = beamRadius,
                    ),
                    center = center,
                    radius = beamRadius,
                )
                // Mid glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(mid, Color.Transparent),
                        center = center,
                        radius = beamRadius * 0.6f,
                    ),
                    center = center,
                    radius = beamRadius * 0.6f,
                )
                // Core glow
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

            // Convert progress (0-1) to radians for 2 figure-8s (0 to 4π)
            val baseAngle = progress * 4f * PI.toFloat()

            // Light 1: Standard figure-8
            val pos1 = figure8Position(baseAngle)
            drawBeam(pos1)

            // Light 2: Phase-shifted figure-8 (offset by ~40% of cycle + slight frequency variation)
            // This creates a different path that doesn't sync with light 1
            val pos2 = figure8Position(baseAngle * 1.15f + PI.toFloat() * 0.8f)
            drawBeam(pos2)
        }
    }
}
