package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBar
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateShows: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var isTextFieldFocused by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val searchFieldFocusRequester = remember { FocusRequester() }
    val resultsFocusRequester = remember { FocusRequester() }

    fun performSearch() {
        if (query.isNotBlank()) {
            scope.launch {
                isSearching = true
                jellyfinClient.search(query).onSuccess { items ->
                    results = items
                }
                hasSearched = true
                isSearching = false
            }
        }
    }

    // Focus search field on entry
    LaunchedEffect(Unit) {
        searchFieldFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp)
    ) {
        TopNavigationBar(
            selectedItem = NavItem.SEARCH,
            onItemSelected = { navItem ->
                when (navItem) {
                    NavItem.HOME -> onNavigateHome()
                    NavItem.MOVIES -> onNavigateMovies()
                    NavItem.SHOWS -> onNavigateShows()
                    NavItem.SEARCH -> { /* Already here */ }
                    NavItem.SETTINGS -> onNavigateSettings()
                    else -> { /* Collections/Universes not implemented */ }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Search input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Custom styled text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(TvColors.Surface, RoundedCornerShape(8.dp))
                    .border(
                        width = 2.dp,
                        color = if (isTextFieldFocused) TvColors.BluePrimary else TvColors.SurfaceLight,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search movies, shows...",
                        color = TvColors.TextSecondary,
                        fontSize = 16.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { newQuery -> query = newQuery },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TvColors.TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(TvColors.BluePrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFieldFocusRequester)
                        .onFocusChanged { isTextFieldFocused = it.isFocused }
                        .onKeyEvent { event ->
                            if (event.key == Key.DirectionDown && event.type == KeyEventType.KeyDown && results.isNotEmpty()) {
                                resultsFocusRequester.requestFocus()
                                true
                            } else if (event.key == Key.Back && event.type == KeyEventType.KeyDown) {
                                onBack()
                                true
                            } else {
                                false
                            }
                        }
                )
            }

            androidx.tv.material3.Button(
                onClick = { performSearch() },
                enabled = query.isNotBlank() && !isSearching
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Results area
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSearching -> {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
                hasSearched && results.isEmpty() -> {
                    Text(
                        text = "No results found for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextSecondary
                    )
                }
                results.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(results, key = { it.id }) { item ->
                            val focusModifier = if (results.indexOf(item) == 0) {
                                Modifier.focusRequester(resultsFocusRequester)
                            } else {
                                Modifier
                            }

                            MediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                                onClick = { onItemClick(item.id) },
                                modifier = focusModifier
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Search for movies, TV shows, and more",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary
                        )
                        Text(
                            text = "Type your search and press Enter or click Search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
