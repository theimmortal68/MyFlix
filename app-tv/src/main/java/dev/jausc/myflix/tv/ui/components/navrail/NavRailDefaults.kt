package dev.jausc.myflix.tv.ui.components.navrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Dimension constants for the navigation rail.
 */
object NavRailDimensions {
    val CollapsedWidth = 48.dp
    val ExpandedWidth = 200.dp
    val IconSize = 16.dp
    val ItemHeight = 32.dp
    val ItemSpacing = 8.dp
    val ItemHorizontalPadding = 4.dp
    val IconContainerSize = 32.dp
    val HaloOuterSize = 28.dp
    val HaloInnerSize = 22.dp
    val SelectionIndicatorSize = 24.dp
    val LabelSpacing = 8.dp
    val VerticalPadding = 0.dp
    val SettingsBottomPadding = 0.dp
    val SettingsSpaceReservation = 40.dp
    val ItemCornerRadius = 8.dp
    val HaloOuterBlur = 6.dp
    val HaloInnerBlur = 3.dp
}

/**
 * Animation constants for the navigation rail.
 */
object NavRailAnimations {
    const val ExpandDurationMs = 250
    const val CollapseDurationMs = 200
    const val LabelFadeDelayMs = 80
    const val HaloDurationMs = 150
    const val IconScaleDurationMs = 100
    const val FocusTransferDelayMs = 100L
    const val SentinelStartupDelayMs = 2000L // Delay before sentinel accepts focus after navigation
}

/**
 * Alpha values for various rail states.
 */
object NavRailAlpha {
    const val ExpandedBackground = 0.88f
    const val ExpandedBackgroundMid = 0.65f
    const val ExpandedBackgroundEdge = 0.3f
    const val HaloOuter = 0.7f
    const val HaloInner = 0.5f
    const val SelectionIndicator = 0.2f
    const val SelectedUnfocused = 0.9f
    const val FocusedHalo = 0.8f
    const val Scrim = 0.5f
}

/**
 * Scale values for animations.
 */
object NavRailScale {
    const val FocusedIcon = 1.15f
    const val UnfocusedHalo = 0.5f
}

/**
 * Background color for the rail gradient.
 */
val NavRailBackgroundColor = Color.Black
