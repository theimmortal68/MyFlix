@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Alphabet quick-scroll bar for library grids.
 * Shows A-Z letters on right edge, clicking filters to items starting with that letter.
 * Uses server-side nameStartsWith filtering for proper alphabet navigation.
 */
@Composable
fun AlphabetScrollBar(
    availableLetters: Set<Char>,
    onLetterClick: (Char) -> Unit,
    modifier: Modifier = Modifier,
    currentLetter: Char? = null,
    onClearFilter: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    gridFocusRequester: FocusRequester? = null,
) {
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    val firstAvailableIndex = alphabet.indexOfFirst { letter ->
        letter in availableLetters
    }.takeIf { it >= 0 } ?: 0

    Column(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        alphabet.forEachIndexed { index, letter ->
            val isAvailable = letter in availableLetters
            val isSelected = letter == currentLetter

            AlphabetLetter(
                letter = letter,
                isAvailable = isAvailable,
                isSelected = isSelected,
                onClick = {
                    if (isSelected && onClearFilter != null) {
                        // Clicking the already-selected letter clears the filter
                        onClearFilter()
                    } else {
                        onLetterClick(letter)
                    }
                },
                modifier = if (index == firstAvailableIndex && focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
                gridFocusRequester = gridFocusRequester,
            )
        }
    }
}

@Composable
private fun AlphabetLetter(
    letter: Char,
    isAvailable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridFocusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    // Colors based on state
    val containerColor = when {
        isFocused -> TvColors.BluePrimary
        isSelected -> TvColors.BluePrimary.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isFocused -> Color.White
        isSelected -> Color.White
        isAvailable -> TvColors.TextPrimary
        else -> TvColors.TextSecondary.copy(alpha = 0.3f)
    }

    Surface(
        onClick = { if (isAvailable) onClick() },
        enabled = isAvailable,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (gridFocusRequester != null) {
                    Modifier.focusProperties { left = gridFocusRequester }
                } else {
                    Modifier
                }
            ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(2.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                ),
            )
        }
    }
}
