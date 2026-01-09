package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * A circular progress indicator compatible with TV Compose.
 * Uses Canvas drawing instead of Material3's CircularProgressIndicator.
 * 
 * @param modifier Modifier for sizing and positioning
 * @param progress Optional progress value (0f to 1f). If null, shows indeterminate spinner.
 * @param showPercentage Whether to show percentage text in the center (only when progress is set)
 * @param color The color of the progress arc
 * @param trackColor The color of the background track (only shown in determinate mode)
 * @param strokeWidth Width of the progress arc
 */
@Composable
fun TvLoadingIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    showPercentage: Boolean = false,
    color: Color = TvColors.BluePrimary,
    trackColor: Color = TvColors.Surface,
    strokeWidth: Dp = 4.dp
) {
    if (progress != null) {
        // Determinate mode - show progress
        DeterminateIndicator(
            progress = progress.coerceIn(0f, 1f),
            showPercentage = showPercentage,
            color = color,
            trackColor = trackColor,
            strokeWidth = strokeWidth,
            modifier = modifier
        )
    } else {
        // Indeterminate mode - animated spinner
        IndeterminateIndicator(
            color = color,
            strokeWidth = strokeWidth,
            modifier = modifier
        )
    }
}

@Composable
private fun DeterminateIndicator(
    progress: Float,
    showPercentage: Boolean,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    // Animate progress changes smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(48.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcSize = Size(
                size.width - strokeWidthPx,
                size.height - strokeWidthPx
            )
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            
            // Draw background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // Draw progress arc
            drawArc(
                color = color,
                startAngle = -90f, // Start from top
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
        
        // Show percentage text
        if (showPercentage) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun IndeterminateIndicator(
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )
    
    Canvas(modifier = modifier.size(48.dp)) {
        val strokeWidthPx = strokeWidth.toPx()
        val arcSize = Size(
            size.width - strokeWidthPx,
            size.height - strokeWidthPx
        )
        
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    }
}

/**
 * Small loading indicator for inline use
 */
@Composable
fun TvLoadingIndicatorSmall(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = TvColors.BluePrimary
) {
    TvLoadingIndicator(
        modifier = modifier.size(20.dp),
        progress = progress,
        showPercentage = false,
        color = color,
        strokeWidth = 2.dp
    )
}
