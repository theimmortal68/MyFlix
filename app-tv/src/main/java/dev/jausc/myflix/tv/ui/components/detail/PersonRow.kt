@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinPerson
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal row of person cards for cast & crew.
 * Matches home screen row styling with accent bar.
 */
@Composable
fun CastCrewSection(
    people: List<JellyfinPerson>,
    jellyfinClient: JellyfinClient,
    onPersonClick: (JellyfinPerson) -> Unit,
    onPersonLongClick: (Int, JellyfinPerson) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Cast & Crew",
    accentColor: Color = Color(0xFFE5A00D),
    cardOnFocus: ((isFocused: Boolean, index: Int) -> Unit)? = null,
) {
    if (people.isEmpty()) return

    val state = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // Row header with accent bar - matches home screen style
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(accentColor, shape = MaterialTheme.shapes.small),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
                    cardOnFocus?.invoke(it.isFocused, index)
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
