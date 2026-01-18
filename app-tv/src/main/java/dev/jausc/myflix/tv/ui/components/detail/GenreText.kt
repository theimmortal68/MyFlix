package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.MaterialTheme
import dev.jausc.myflix.core.common.ui.components.detail.GenreText as SharedGenreText

/**
 * Genre text display for TV detail headers.
 * Delegates to shared GenreText with TV Material theme defaults.
 */
@Composable
fun GenreText(
    genres: List<String>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    SharedGenreText(
        genres = genres,
        modifier = modifier,
        textStyle = textStyle,
        color = color,
    )
}
