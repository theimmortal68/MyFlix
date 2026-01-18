@file:Suppress("MagicNumber")

package dev.jausc.myflix.core.common.ui.components.detail

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

private const val MAX_GENRES_TO_SHOW = 4

/**
 * Genre text display for detail headers.
 * Shows comma-separated genres with a limit and "+N" suffix if needed.
 *
 * Uses BasicText for platform-agnostic rendering - works with both TV and Mobile Material themes.
 *
 * @param genres List of genre names to display
 * @param modifier Modifier for the text
 * @param textStyle Text style to apply (pass from MaterialTheme)
 * @param color Text color to apply (pass from MaterialTheme)
 * @param maxLines Maximum number of lines (default 1)
 */
@Composable
fun GenreText(
    genres: List<String>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
) {
    if (genres.isEmpty()) return

    val text = remember(genres) {
        if (genres.size <= MAX_GENRES_TO_SHOW) {
            genres.joinToString(", ")
        } else {
            genres.take(MAX_GENRES_TO_SHOW).joinToString(", ") +
                ", +${genres.size - MAX_GENRES_TO_SHOW}"
        }
    }

    BasicText(
        text = text,
        style = textStyle.copy(color = color),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        modifier = modifier,
    )
}
