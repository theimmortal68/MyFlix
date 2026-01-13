@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

private const val MAX_GENRES_TO_SHOW = 4

/**
 * Displays genres as a comma-separated text row.
 */
@Composable
fun GenreText(
    genres: List<String>,
    modifier: Modifier = Modifier,
) {
    if (genres.isEmpty()) return

    val displayGenres = if (genres.size > MAX_GENRES_TO_SHOW) {
        genres.take(MAX_GENRES_TO_SHOW)
    } else {
        genres
    }

    Text(
        text = displayGenres.joinToString(separator = ", "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
