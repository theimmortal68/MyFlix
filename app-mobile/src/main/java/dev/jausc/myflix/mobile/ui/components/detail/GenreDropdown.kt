@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Genre dropdown button for mobile.
 * Shows first genre with dropdown indicator, opens menu to select genre.
 */
@Composable
fun MobileGenreDropdown(
    genres: List<String>,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (genres.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val displayText = if (genres.size == 1) {
        genres.first()
    } else {
        "${genres.first()} +${genres.size - 1}"
    }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Show all genres",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            genres.forEach { genre ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onGenreSelected(genre)
                    },
                )
            }
        }
    }
}
