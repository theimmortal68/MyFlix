package dev.jausc.myflix.tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.jausc.myflix.tv.R
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen for Navigation Compose.
 * Shows the master art with animated spotlight overlay,
 * two pairs of lights cross sequentially, then navigates.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Total animation: 1.5s first pair + 2s second pair = 3.5s total
    val totalDurationMs = 3500
    
    // Navigate after animation completes
    LaunchedEffect(Unit) {
        delay(totalDurationMs.toLong() + 200L)
        onFinished()
    }
    
    SplashScreenContent(modifier = modifier)
}

@Composable
private fun SplashScreenContent(
    modifier: Modifier = Modifier
) {
    // Pair 1: Cross from sides (left-right, right-left)
    val sweep1A = remember { Animatable(-0.25f) }
    val sweep1B = remember { Animatable(1.25f) }
    
    // Pair 2: Cross from different angles (starts after pair 1)
    val sweep2A = remember { Animatable(-0.25f) }
    val sweep2B = remember { Animatable(1.25f) }
    
    // Animate pairs sequentially
    LaunchedEffect(Unit) {
        // Pair 1: both cross simultaneously
        launch {
            sweep1A.animateTo(1.25f, tween(1500, easing = FastOutSlowInEasing))
        }
        launch {
            sweep1B.animateTo(-0.25f, tween(1500, easing = FastOutSlowInEasing))
        }
        
        // Wait for pair 1 to finish
        delay(1500)
        
        // Pair 2: cross from different positions (3/4 speed = 2000ms)
        launch {
            sweep2A.animateTo(1.25f, tween(2000, easing = FastOutSlowInEasing))
        }
        launch {
            sweep2B.animateTo(-0.25f, tween(2000, easing = FastOutSlowInEasing))
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Master art (logo baked in)
        Image(
            painter = painterResource(R.drawable.myflix_splash_master),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Moving spotlights overlay - 2 pairs
        SpotlightOverlay(
            sweep1A = sweep1A.value,
            sweep1B = sweep1B.value,
            sweep2A = sweep2A.value,
            sweep2B = sweep2B.value,
            core = TvColors.BlueAccent.copy(alpha = 0.20f),
            mid = TvColors.BlueLight.copy(alpha = 0.13f),
            wide = TvColors.BluePrimary.copy(alpha = 0.07f),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SpotlightOverlay(
    sweep1A: Float,
    sweep1B: Float,
    sweep2A: Float,
    sweep2B: Float,
    core: Color,
    mid: Color,
    wide: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        fun drawBeam(sweep: Float, originY: Float, angleDeg: Float, xSpread: Float) {
            val x = (w * sweep).coerceIn(-w * 0.4f, w * 1.4f)
            val center = Offset(x, h * originY)
            
            rotate(degrees = angleDeg, pivot = Offset(w / 2f, h / 2f)) {
                // Wide
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(wide, Color.Transparent),
                        center = center,
                        radius = w * xSpread
                    ),
                    topLeft = Offset(-w, -h),
                    size = Size(w * 3f, h * 3f)
                )
                // Mid
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(mid, Color.Transparent),
                        center = center,
                        radius = w * (xSpread * 0.72f)
                    ),
                    topLeft = Offset(-w, -h),
                    size = Size(w * 3f, h * 3f)
                )
                // Core
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(core, Color.Transparent),
                        center = center,
                        radius = w * (xSpread * 0.46f)
                    ),
                    topLeft = Offset(-w, -h),
                    size = Size(w * 3f, h * 3f)
                )
            }
        }
        
        // Pair 1: Cross at upper level with steeper angles
        drawBeam(sweep1A, originY = 0.35f, angleDeg = -20f, xSpread = 0.50f)
        drawBeam(sweep1B, originY = 0.35f, angleDeg = 20f, xSpread = 0.50f)
        
        // Pair 2: Cross at lower level with shallower angles
        drawBeam(sweep2A, originY = 0.55f, angleDeg = -10f, xSpread = 0.55f)
        drawBeam(sweep2B, originY = 0.55f, angleDeg = 10f, xSpread = 0.55f)
    }
}
