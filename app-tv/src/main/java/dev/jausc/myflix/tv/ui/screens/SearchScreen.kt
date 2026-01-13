@file:Suppress(
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.rememberNavBarPopupState
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.TvTextButton
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateShows: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    // ViewModel with manual DI
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(jellyfinClient),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    var isTextFieldFocused by remember { mutableStateOf(false) }
    val searchFieldFocusRequester = remember { FocusRequester() }
    val resultsFocusRequester = remember { FocusRequester() }
    val homeButtonFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Popup nav bar state - visible on load, auto-hides after 5 seconds
    val navBarState = rememberNavBarPopupState()

    // Focus search field on entry
    LaunchedEffect(Unit) {
        searchFieldFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .padding(top = 80.dp), // Space for nav bar
        ) {
            Spacer(modifier = Modifier.height(24.dp))

        // Search input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (state.query.isEmpty()) {
                    Text(
                        text = "Search movies, shows...",
                        color = TvColors.TextSecondary,
                        fontSize = 16.sp,
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TvColors.TextPrimary,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(TvColors.BluePrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFieldFocusRequester)
                        .onFocusChanged { isTextFieldFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            // Intercept UP to show nav bar popup
                            if (event.key == Key.DirectionUp && event.type == KeyEventType.KeyDown) {
                                navBarState.show()
                                coroutineScope.launch {
                                    delay(150) // Wait for animation
                                    try {
                                        homeButtonFocusRequester.requestFocus()
                                    } catch (_: Exception) {
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .onKeyEvent { event ->
                            when {
                                event.key == Key.DirectionDown &&
                                    event.type == KeyEventType.KeyDown &&
                                    state.results.isNotEmpty() -> {
                                    resultsFocusRequester.requestFocus()
                                    true
                                }
                                event.key == Key.Back && event.type == KeyEventType.KeyDown -> {
                                    onBack()
                                    true
                                }
                                else -> {
                                    false
                                }
                            }
                        },
                )
            }

            TvTextButton(
                text = "Search",
                onClick = { viewModel.performSearch() },
                enabled = state.canSearch,
                isPrimary = true,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Results area
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isSearching -> {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "Search failed",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.Error,
                    )
                }
                state.isEmpty -> {
                    Text(
                        text = "No results found for \"${state.query}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextSecondary,
                    )
                }
                state.results.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(state.results, key = { it.id }) { item ->
                            val focusModifier = if (state.results.indexOf(item) == 0) {
                                Modifier.focusRequester(resultsFocusRequester)
                            } else {
                                Modifier
                            }

                            MediaCard(
                                item = item,
                                imageUrl = jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary),
                                onClick = { onItemClick(item.id) },
                                modifier = focusModifier,
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Search for movies, TV shows, and more",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TvColors.TextSecondary,
                        )
                        Text(
                            text = "Type your search and press Enter or click Search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
        } // End Column

        // Top Navigation Bar (popup overlay)
        TopNavigationBarPopup(
            visible = navBarState.isVisible,
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
            },
            onDismiss = {
                navBarState.hide()
                try {
                    searchFieldFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            },
            homeButtonFocusRequester = homeButtonFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    } // End Box
}
