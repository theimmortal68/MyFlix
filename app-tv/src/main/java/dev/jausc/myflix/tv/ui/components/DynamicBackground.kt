package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.jausc.myflix.tv.ui.util.GradientColors

// Dark base color for the gradient background (near black, no blue tint)
private val DarkBase = Color(0xFF0A0A0A)

/**
 * A dynamic gradient background that changes colors based on the current backdrop image.
 *
 * Uses a dark (near-black) base with colored radial gradients from corners.
 * The gradient colors are extracted from the hero image and animated smoothly.
 */
@Composable
fun DynamicBackground(gradientColors: GradientColors, modifier: Modifier = Modifier, animationDurationMs: Int = 1250) {
    // Get the actual colors, using dark base as fallback
    val primaryTarget = if (gradientColors.primary != Color.Unspecified) {
        gradientColors.primary
    } else {
        DarkBase
    }
    val secondaryTarget = if (gradientColors.secondary != Color.Unspecified) {
        gradientColors.secondary
    } else {
        DarkBase
    }
    val tertiaryTarget = if (gradientColors.tertiary != Color.Unspecified) {
        gradientColors.tertiary
    } else {
        DarkBase
    }

    // Animate colors for smooth transitions
    val animPrimary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = tween(animationDurationMs),
        label = "dynamic_bg_primary",
    )
    val animSecondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = tween(animationDurationMs),
        label = "dynamic_bg_secondary",
    )
    val animTertiary by animateColorAsState(
        targetValue = tertiaryTarget,
        animationSpec = tween(animationDurationMs),
        label = "dynamic_bg_tertiary",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Dark base
                drawRect(color = DarkBase)

                // Top-Left corner: Secondary color gradient (with alpha for blending)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(animSecondary.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.85f,
                    ),
                )

                // Bottom-Right corner: Primary color gradient
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(animPrimary.copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.85f,
                    ),
                )

                // Top-Right corner: Tertiary color gradient
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(animTertiary.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.85f,
                    ),
                )
            },
    )
}
