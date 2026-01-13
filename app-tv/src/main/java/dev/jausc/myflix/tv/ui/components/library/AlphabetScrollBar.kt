@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Alphabet quick-scroll bar for library grids.
 * Shows A-Z letters on right edge, clicking scrolls to first item starting with that letter.
 */
@Composable
fun AlphabetScrollBar(
    availableLetters: Set<Char>,
    onLetterClick: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alphabet = remember { ('A'..'Z').toList() + listOf('#') }

    Column(
        modifier = modifier
            .width(24.dp)
            .background(
                color = TvColors.SurfaceElevated.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        alphabet.forEach { letter ->
            val isAvailable = if (letter == '#') {
                // '#' represents numbers and special characters
                availableLetters.any { !it.isLetter() }
            } else {
                letter in availableLetters
            }

            AlphabetLetter(
                letter = letter,
                isAvailable = isAvailable,
                onClick = { onLetterClick(letter) },
            )
        }
    }
}

@Composable
private fun AlphabetLetter(
    letter: Char,
    isAvailable: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = isAvailable,
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(2.dp),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (isAvailable) TvColors.TextPrimary else TvColors.TextSecondary.copy(alpha = 0.3f),
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        modifier = Modifier.padding(vertical = 1.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
