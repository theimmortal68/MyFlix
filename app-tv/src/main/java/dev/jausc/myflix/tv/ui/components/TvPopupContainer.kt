@file:Suppress("MagicNumber")
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.jausc.myflix.tv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Anchor position for popup placement.
 * Represents the position where the popup should appear from.
 */
data class PopupAnchor(
    val x: Dp,
    val y: Dp,
)

/**
 * Base container for all TV popup menus and dialogs.
 * Provides consistent styling, animations, and positioning.
 *
 * Features:
 * - Netflix-style dark gradient background
 * - Scale-in animation from anchor point (or center)
 * - Semi-transparent backdrop scrim
 * - Back button handling
 * - Automatic bounds checking
 *
 * @param visible Whether the popup is visible
 * @param onDismiss Called when the popup should close
 * @param anchor Optional anchor point for positioning (null = center of screen)
 * @param minWidth Minimum width of the popup
 * @param maxWidth Maximum width of the popup
 * @param maxHeight Maximum height of the popup (null = no limit)
 * @param modifier Modifier for the popup container
 * @param content Content to display inside the popup
 */
@Composable
fun TvPopupContainer(
    visible: Boolean,
    onDismiss: () -> Unit,
    anchor: PopupAnchor? = null,
    minWidth: Dp = 200.dp,
    maxWidth: Dp = 400.dp,
    maxHeight: Dp? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Handle back button
    if (visible) {
        BackHandler { onDismiss() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val estimatedHeight = maxHeight ?: 400.dp
        val estimatedWidth = (minWidth + maxWidth) / 2

        // Calculate position based on anchor
        val offsetX = if (anchor != null) {
            with(density) {
                val anchorPx = anchor.x.toPx()
                val menuWidthPx = estimatedWidth.toPx()
                val maxWidthPx = maxWidth.toPx()
                val screenWidthPx = this@BoxWithConstraints.maxWidth.toPx()
                val targetX = (anchorPx - menuWidthPx / 2).coerceIn(24f, screenWidthPx - maxWidthPx - 24f)
                targetX.toInt()
            }
        } else {
            0 // Center alignment will handle it
        }

        val offsetY = if (anchor != null) {
            with(density) {
                val anchorPx = anchor.y.toPx()
                val menuHeightPx = estimatedHeight.toPx()
                // Try to position above the anchor, but ensure it stays on screen
                val targetY = (anchorPx - menuHeightPx - 16.dp.toPx()).coerceAtLeast(48f)
                targetY.toInt()
            }
        } else {
            0 // Center alignment will handle it
        }

        // Transform origin for scale animation
        val transformOrigin = if (anchor != null) {
            TransformOrigin(0.5f, 1f) // Scale from bottom center (toward anchor)
        } else {
            TransformOrigin(0.5f, 0.5f) // Scale from center
        }

        @Suppress("UnusedPrivateProperty")
        val alignment = if (anchor != null) Alignment.TopStart else Alignment.Center

        // Backdrop scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .focusable(false),
            )
        }

        // Popup content
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = tween(200),
                initialScale = 0.85f,
                transformOrigin = transformOrigin,
            ) + fadeIn(tween(200)),
            exit = scaleOut(
                animationSpec = tween(150),
                targetScale = 0.85f,
                transformOrigin = transformOrigin,
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .focusGroup()
                .focusProperties {
                    // Trap focus within the popup - prevent escaping to content behind
                    @Suppress("DEPRECATION")
                    exit = { FocusRequester.Cancel }
                }
                .then(
                    if (anchor != null) {
                        Modifier.offset { IntOffset(offsetX, offsetY) }
                    } else {
                        Modifier.align(Alignment.Center)
                    }
                ),
        ) {
            Box(
                modifier = modifier
                    .widthIn(min = minWidth, max = maxWidth)
                    .then(
                        if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF2181818),
                                Color(0xF5141414),
                            ),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(16.dp),
                content = content,
            )
        }
    }
}

/**
 * Simplified popup container that always appears centered.
 * Use this for dialogs that don't need anchor positioning.
 */
@Composable
fun TvCenteredPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    minWidth: Dp = 280.dp,
    maxWidth: Dp = 450.dp,
    maxHeight: Dp? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    TvPopupContainer(
        visible = visible,
        onDismiss = onDismiss,
        anchor = null,
        minWidth = minWidth,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        modifier = modifier,
        content = content,
    )
}
