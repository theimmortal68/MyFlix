@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Clickable overview text that shows limited lines and expands on click.
 * Shows a focus highlight when focused for TV D-pad navigation.
 */
@Composable
fun OverviewText(
    overview: String,
    maxLines: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textBoxHeight: Dp = maxLines * 20.dp,
    enabled: Boolean = true,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor = if (isFocused) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            },
    ) {
        Text(
            text = overview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(8.dp)
                .then(
                    if (textBoxHeight != Dp.Unspecified) {
                        Modifier.height(textBoxHeight)
                    } else {
                        Modifier
                    }
                ),
        )
    }
}
