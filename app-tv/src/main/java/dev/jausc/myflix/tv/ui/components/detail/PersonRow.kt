@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal row of person cards for cast & crew.
 */
@Composable
fun CastCrewSection(
    people: List<JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (JellyfinPerson) -> Unit,
    onPersonLongClick: (Int, JellyfinPerson) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Cast & Crew",
) {
    if (people.isEmpty()) return

    val state = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TvColors.TextPrimary,
        )
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer(firstFocus),
        ) {
            itemsIndexed(people) { index, person ->
                val cardModifier = if (index == 0) {
                    Modifier.focusRequester(firstFocus)
                } else {
                    Modifier
                }.onFocusChanged {
                    if (it.isFocused) {
                        focusedIndex = index
                    }
                }

                PersonCard(
                    person = person,
                    jellyfinClient = jellyfinClient,
                    onClick = { onPersonClick(person) },
                    onLongClick = { onPersonLongClick(index, person) },
                    modifier = cardModifier,
                )
            }
        }
    }
}
