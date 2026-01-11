package dev.jausc.myflix.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen size categories for responsive layout.
 * Based on Material Design 3 window size classes.
 */
enum class ScreenSizeClass {
    COMPACT, // Standard phones (< 600dp)
    MEDIUM, // Large phones, small tablets, foldables (600-840dp)
    EXPANDED, // Tablets, foldables unfolded (> 840dp)
}

/**
 * Determine screen size class from screen width.
 */
fun getScreenSizeClass(screenWidthDp: Int): ScreenSizeClass {
    return when {
        screenWidthDp < 600 -> ScreenSizeClass.COMPACT
        screenWidthDp < 840 -> ScreenSizeClass.MEDIUM
        else -> ScreenSizeClass.EXPANDED
    }
}

/**
 * Remember the current screen size class, recomputing only when configuration changes.
 */
@Composable
fun rememberScreenSizeClass(): ScreenSizeClass {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        getScreenSizeClass(configuration.screenWidthDp)
    }
}

/**
 * Get horizontal padding based on screen size.
 */
fun getHorizontalPadding(screenSizeClass: ScreenSizeClass): Dp = when (screenSizeClass) {
    ScreenSizeClass.COMPACT -> 16.dp
    ScreenSizeClass.MEDIUM -> 24.dp
    ScreenSizeClass.EXPANDED -> 32.dp
}

/**
 * Get card spacing based on screen size.
 */
fun getCardSpacing(screenSizeClass: ScreenSizeClass): Dp = when (screenSizeClass) {
    ScreenSizeClass.COMPACT -> 12.dp
    ScreenSizeClass.MEDIUM -> 16.dp
    ScreenSizeClass.EXPANDED -> 20.dp
}
