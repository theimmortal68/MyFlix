package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

private const val MAX_GENRES_TO_SHOW = 4

/**
 * Genre text display for detail headers.
 * Shows comma-separated genres with a limit and "+N" suffix if needed.
 */
@Composable
fun GenreText(
    genres: List<String>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (genres.isEmpty()) return

    val text = remember(genres) {
        if (genres.size <= MAX_GENRES_TO_SHOW) {
            genres.joinToString(", ")
        } else {
            genres.subList(0, MAX_GENRES_TO_SHOW).joinToString(", ") +
                ", +${genres.size - MAX_GENRES_TO_SHOW}"
        }
    }

    Text(
        text = text,
        style = textStyle,
        color = color,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
