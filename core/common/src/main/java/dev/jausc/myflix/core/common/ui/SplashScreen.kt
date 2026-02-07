@file:Suppress(
    "MagicNumber",
)

package dev.jausc.myflix.core.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.jausc.myflix.core.common.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Configuration for splash screen appearance.
 *
 * @param contentScale How the splash image should be scaled.
 *        Use [ContentScale.Crop] for TV (fills screen) or [ContentScale.Fit] for mobile (letterboxed).
 */
data class SplashScreenConfig(
    val contentScale: ContentScale = ContentScale.Fit,
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

private const val MINIMUM_DISPLAY_TIME_MS = 800L
private const val MAXIMUM_DISPLAY_TIME_MS = 8000L
private const val FIXED_DISPLAY_TIME_MS = 3500L

/**
 * Splash screen showing static master art.
 *
 * When [isDataReady] is provided, uses dynamic timing:
 * - Minimum display: 800ms (prevents flash)
 * - Maximum display: 8s (safety timeout)
 * - Dismisses when min time passed AND data ready (or max time reached)
 *
 * When [isDataReady] is null, uses fixed 3.5s display time (mobile fallback).
 *
 * @param onFinished Callback invoked when splash should be dismissed
 * @param isDataReady Optional signal that home data is ready to display
 * @param config Configuration for appearance customization
 * @param modifier Modifier for the root container
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    isDataReady: StateFlow<Boolean>? = null,
    config: SplashScreenConfig = SplashScreenMobileConfig,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        if (isDataReady != null) {
            // Dynamic: wait for min time AND data ready, with max timeout
            kotlinx.coroutines.delay(MINIMUM_DISPLAY_TIME_MS)
            withTimeoutOrNull(MAXIMUM_DISPLAY_TIME_MS - MINIMUM_DISPLAY_TIME_MS) {
                isDataReady.first { it }
            }
            onFinished()
        } else {
            // Fallback: fixed display for screens that don't provide readiness signal
            kotlinx.coroutines.delay(FIXED_DISPLAY_TIME_MS)
            onFinished()
        }
    }

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
    }
}
