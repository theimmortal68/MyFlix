@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Genre dropdown button that shows a dialog with genre list.
 * When a genre is selected, navigates to library filtered by that genre.
 */
@Composable
fun GenreDropdown(
    genres: List<String>,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (genres.isEmpty()) return

    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = TvColors.TextSecondary,
                focusedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                focusedContentColor = TvColors.TextPrimary,
            ),
        ) {
            Text(
                text = genres.firstOrNull() ?: "Genres",
                style = MaterialTheme.typography.labelSmall,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Show genres",
                modifier = Modifier.size(14.dp),
            )
        }

        if (showDialog) {
            GenreDialog(
                genres = genres,
                onGenreSelected = { genre ->
                    showDialog = false
                    onGenreSelected(genre)
                },
                onDismiss = { showDialog = false },
            )
        }
    }
}

/**
 * Genres text button for inline display in metadata row.
 * Shows first genre with dropdown indicator.
 */
@Composable
fun GenresButton(
    genres: List<String>,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (genres.isEmpty()) return

    var showDialog by remember { mutableStateOf(false) }
    val displayText = if (genres.size == 1) {
        genres.first()
    } else {
        "${genres.first()} +${genres.size - 1}"
    }

    Box(modifier = modifier) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.height(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors = ButtonDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = TvColors.TextSecondary,
                focusedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                focusedContentColor = TvColors.TextPrimary,
            ),
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Show all genres",
                modifier = Modifier.size(14.dp),
            )
        }

        if (showDialog) {
            GenreDialog(
                genres = genres,
                onGenreSelected = { genre ->
                    showDialog = false
                    onGenreSelected(genre)
                },
                onDismiss = { showDialog = false },
            )
        }
    }
}

/**
 * Dialog showing list of genres for TV with D-pad navigation.
 */
@Composable
private fun GenreDialog(
    genres: List<String>,
    onGenreSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TvColors.SurfaceElevated)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Select Genre",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                genres.forEach { genre ->
                    ListItem(
                        selected = false,
                        onClick = { onGenreSelected(genre) },
                        headlineContent = {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = TvColors.TextPrimary,
                            focusedContainerColor = TvColors.BluePrimary,
                            focusedContentColor = Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
