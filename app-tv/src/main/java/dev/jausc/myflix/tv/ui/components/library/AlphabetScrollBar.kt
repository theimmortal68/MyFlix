@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
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
    focusRequester: FocusRequester? = null,
) {
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    val firstAvailableIndex = alphabet.indexOfFirst { letter ->
        letter in availableLetters
    }.takeIf { it >= 0 } ?: 0

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
        alphabet.forEachIndexed { index, letter ->
            val isAvailable = letter in availableLetters

            AlphabetLetter(
                letter = letter,
                isAvailable = isAvailable,
                onClick = { onLetterClick(letter) },
                modifier = if (index == firstAvailableIndex && focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun AlphabetLetter(
    letter: Char,
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontSize = (MaterialTheme.typography.labelSmall.fontSize.value - 1f).coerceAtLeast(8f).sp

    Button(
        onClick = { if (isAvailable) onClick() },
        enabled = isAvailable,
        modifier = modifier.padding(vertical = 1.dp),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(2.dp)),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (isAvailable) TvColors.TextPrimary else TvColors.TextSecondary.copy(alpha = 0.3f),
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = fontSize,
            ),
        )
    }
}
