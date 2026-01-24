@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jausc.myflix.core.common.ui.components.detail.GenreText as SharedGenreText

/**
 * Displays genres as a comma-separated text row.
 * Mobile wrapper with theme defaults.
 */
@Composable
fun GenreText(genres: List<String>, modifier: Modifier = Modifier,) {
    SharedGenreText(
        genres = genres,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
