@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlin.math.abs

/**
 * Subtitle delay control component for TV player overlay.
 *
 * Shows a compact horizontal strip with:
 * - Current delay value
 * - Left/right nudge buttons (±500ms)
 * - Reset button (when delay != 0)
 *
 * Designed for D-pad navigation:
 * - LEFT/RIGHT: Adjust delay by ±500ms
 * - SELECT on arrows: Adjust delay
 * - SELECT on reset: Reset to 0
 */
@Composable
fun SubtitleDelayControl(
    visible: Boolean,
    delayMs: Long,
    onDelayChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        SubtitleDelayControlContent(
            delayMs = delayMs,
            onDelayChange = onDelayChange,
            focusRequester = focusRequester,
        )
    }
}

@Composable
private fun SubtitleDelayControlContent(
    delayMs: Long,
    onDelayChange: (Long) -> Unit,
    focusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }

    val decrementFocusRequester = remember { FocusRequester() }
    val incrementFocusRequester = remember { FocusRequester() }
    val resetFocusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Label
        Text(
            text = "Subtitle Delay",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
        )

        Spacer(Modifier.width(12.dp))

        // Decrement button
        DelayButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Decrease subtitle delay",
            onClick = { onDelayChange((delayMs - NUDGE_INCREMENT_MS).coerceAtLeast(MIN_DELAY_MS)) },
            enabled = delayMs > MIN_DELAY_MS,
            focusRequester = decrementFocusRequester,
        )

        Spacer(Modifier.width(8.dp))

        // Current value display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(80.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onDelayChange((delayMs - NUDGE_INCREMENT_MS).coerceAtLeast(MIN_DELAY_MS))
                            true
                        }
                        Key.DirectionRight -> {
                            onDelayChange((delayMs + NUDGE_INCREMENT_MS).coerceAtMost(MAX_DELAY_MS))
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Text(
                text = formatDelayMs(delayMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Increment button
        DelayButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Increase subtitle delay",
            onClick = { onDelayChange((delayMs + NUDGE_INCREMENT_MS).coerceAtMost(MAX_DELAY_MS)) },
            enabled = delayMs < MAX_DELAY_MS,
            focusRequester = incrementFocusRequester,
        )

        // Reset button (only visible when delay != 0)
        AnimatedVisibility(
            visible = delayMs != 0L,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row {
                Spacer(Modifier.width(12.dp))
                DelayButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = "Reset subtitle delay",
                    onClick = { onDelayChange(0L) },
                    enabled = true,
                    focusRequester = resetFocusRequester,
                )
            }
        }
    }
}

@Composable
private fun DelayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(CircleShape),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.3f),
            disabledContainerColor = Color.White.copy(alpha = 0.05f),
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        modifier = Modifier
            .size(32.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) {
                    if (isFocused) Color.White else Color.White.copy(alpha = 0.7f)
                } else {
                    Color.White.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Format delay in milliseconds to a human-readable string.
 * Examples: "-2.0s", "0.0s", "+1.5s"
 */
private fun formatDelayMs(delayMs: Long): String {
    val seconds = delayMs / 1000.0
    val sign = when {
        delayMs > 0 -> "+"
        delayMs < 0 -> ""
        else -> ""
    }
    return "$sign${"%.1f".format(seconds)}s"
}

private const val MIN_DELAY_MS = -10_000L
private const val MAX_DELAY_MS = 10_000L
private const val NUDGE_INCREMENT_MS = 500L
