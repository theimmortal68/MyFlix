@file:Suppress(
    "MagicNumber",
    "LongMethod",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.viewmodel.SearchSuggestion
import dev.jausc.myflix.core.viewmodel.SearchViewModel
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.components.TvTextButton
import dev.jausc.myflix.tv.ui.components.VoiceSpeechHelper
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry

/**
 * Unified search screen with 4 horizontal result rows:
 * Movies, Series, Episodes (Jellyfin), and Discover (Seerr).
 * All 4 rows are sized to fit on screen simultaneously.
 */
@Composable
fun SearchScreen(
    jellyfinClient: JellyfinClient,
    seerrRepository: SeerrRepository? = null,
    onItemClick: (String) -> Unit,
    onDiscoverClick: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> },
    onPersonClick: (personId: Int) -> Unit = {},
    onBack: () -> Unit,
) {
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(jellyfinClient, seerrRepository),
    )

    val state by viewModel.uiState.collectAsState()

    var isTextFieldFocused by remember { mutableStateOf(false) }
    var focusResultsOnLoad by remember { mutableStateOf(false) }
    val searchFieldFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }
    val firstSuggestionFocusRequester = remember { FocusRequester() }

    // NavRail exit focus registration
    val updateExitFocus = rememberExitFocusRegistry(searchFieldFocusRequester)

    // Voice search state
    val context = LocalContext.current
    var isVoiceListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val voiceSpeechHelper = remember {
        VoiceSpeechHelper(
            context = context,
            onResult = { result ->
                viewModel.updateQuery(result)
                viewModel.performSearch()
            },
            onListening = { listening ->
                isVoiceListening = listening
            },
            onError = { error ->
                voiceError = error
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceSpeechHelper.destroy()
        }
    }

    // Focus search field on entry
    LaunchedEffect(Unit) {
        searchFieldFocusRequester.requestFocus()
    }

    // After suggestion click, focus first result when results arrive
    LaunchedEffect(state.hasResults) {
        if (state.hasResults && focusResultsOnLoad) {
            focusResultsOnLoad = false
            firstResultFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp)
            .padding(top = 16.dp),
    ) {

        // ── Search input row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .background(TvColors.Surface, RoundedCornerShape(4.dp))
                    .border(
                        width = 2.dp,
                        color = if (isTextFieldFocused) TvColors.BluePrimary else TvColors.SurfaceLight,
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (state.query.isEmpty()) {
                    Text(
                        text = if (isVoiceListening) "Listening..." else "Search movies, shows, episodes...",
                        color = if (isVoiceListening) TvColors.BluePrimary else TvColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TvColors.TextPrimary,
                        fontSize = 12.sp,
                    ),
                    cursorBrush = SolidColor(TvColors.BluePrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFieldFocusRequester)
                        .onFocusChanged {
                            isTextFieldFocused = it.isFocused
                            if (it.isFocused) updateExitFocus(searchFieldFocusRequester)
                        }
                        .onPreviewKeyEvent { event ->
                            when {
                                event.key == Key.DirectionDown &&
                                    event.type == KeyEventType.KeyDown -> {
                                    when {
                                        state.suggestions.isNotEmpty() && !state.hasSearched -> {
                                            firstSuggestionFocusRequester.requestFocus()
                                            true
                                        }
                                        state.hasResults -> {
                                            firstResultFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                event.key == Key.Back && event.type == KeyEventType.KeyDown -> {
                                    onBack()
                                    true
                                }
                                else -> false
                            }
                        },
                )
            }

            // Voice search button
            VoiceSearchButton(
                isListening = isVoiceListening,
                isAvailable = voiceSpeechHelper.isAvailable(),
                onClick = {
                    if (isVoiceListening) {
                        voiceSpeechHelper.stopListening()
                    } else {
                        voiceError = null
                        voiceSpeechHelper.startListening()
                    }
                },
            )

            TvTextButton(
                text = "Search",
                onClick = { viewModel.performSearch() },
                enabled = state.canSearch,
                isPrimary = true,
            )
        }

        // Voice error message
        voiceError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.Error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Suggestions row
        if (state.suggestions.isNotEmpty() && !state.hasSearched) {
            SuggestionsRow(
                suggestions = state.suggestions,
                firstSuggestionFocusRequester = firstSuggestionFocusRequester,
                onSuggestionClick = { suggestion ->
                    focusResultsOnLoad = true
                    searchFieldFocusRequester.requestFocus()
                    viewModel.updateQuery(suggestion.name)
                    viewModel.performSearch()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Results area ──
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
                state.hasResults -> {
                    // Determine which row is first to receive the down-arrow focus requester
                    val firstRowKey = when {
                        state.movies.isNotEmpty() -> "Movies"
                        state.series.isNotEmpty() -> "Series"
                        state.episodes.isNotEmpty() -> "Episodes"
                        else -> "Discover"
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Movies row
                        if (state.movies.isNotEmpty()) {
                            SearchResultRow(
                                title = "Movies",
                                items = state.movies,
                                jellyfinClient = jellyfinClient,
                                onItemClick = onItemClick,
                                firstRowFocusRequester = if (firstRowKey == "Movies") firstResultFocusRequester else null,
                                updateExitFocus = updateExitFocus,
                            )
                        }

                        // Series row
                        if (state.series.isNotEmpty()) {
                            SearchResultRow(
                                title = "Series",
                                items = state.series,
                                jellyfinClient = jellyfinClient,
                                onItemClick = onItemClick,
                                firstRowFocusRequester = if (firstRowKey == "Series") firstResultFocusRequester else null,
                                updateExitFocus = updateExitFocus,
                            )
                        }

                        // Episodes row
                        if (state.episodes.isNotEmpty()) {
                            SearchResultRow(
                                title = "Episodes",
                                items = state.episodes,
                                jellyfinClient = jellyfinClient,
                                onItemClick = onItemClick,
                                firstRowFocusRequester = if (firstRowKey == "Episodes") firstResultFocusRequester else null,
                                updateExitFocus = updateExitFocus,
                            )
                        }

                        // Discover (Seerr) row
                        if (state.discoverResults.isNotEmpty()) {
                            DiscoverResultRow(
                                title = "Discover",
                                results = state.discoverResults,
                                seerrRepository = seerrRepository,
                                onMediaClick = onDiscoverClick,
                                onPersonClick = onPersonClick,
                                firstRowFocusRequester = if (firstRowKey == "Discover") firstResultFocusRequester else null,
                                updateExitFocus = updateExitFocus,
                            )
                        }
                    }
                }
                else -> {
                    // Initial state - no search performed yet
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
    }
}

// ── Card size constants ──
// All rows use landscape thumbs so 4 rows fit on a 1080p screen
private val THUMB_CARD_HEIGHT = 97.dp
private val THUMB_ASPECT_RATIO = 16f / 9f

/**
 * A single Jellyfin search result row with title and horizontal landscape thumb cards.
 */
@Composable
private fun SearchResultRow(
    title: String,
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    firstRowFocusRequester: FocusRequester?,
    updateExitFocus: (FocusRequester) -> Unit,
) {
    val rowFocusRequester = remember { FocusRequester() }

    Column {
        Text(
            text = "$title (${items.size})",
            style = MaterialTheme.typography.titleSmall,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 48.dp),
        ) {
            itemsIndexed(items, key = { _, item -> "${title}-${item.id}" }) { index, item ->
                val isEpisode = item.type?.equals("Episode", ignoreCase = true) == true
                val focusModifier = if (index == 0) {
                    Modifier.focusRequester(
                        if (firstRowFocusRequester != null) firstRowFocusRequester
                        else rowFocusRequester,
                    )
                } else {
                    Modifier
                }
                val exitFocusModifier = Modifier.onFocusChanged {
                    if (it.isFocused) {
                        updateExitFocus(firstRowFocusRequester ?: rowFocusRequester)
                    }
                }

                if (isEpisode) {
                    EpisodeThumbCard(
                        item = item,
                        imageUrl = jellyfinClient.getPrimaryImageUrl(
                            item.id,
                            item.imageTags?.primary,
                        ),
                        onClick = { onItemClick(item.id) },
                        modifier = Modifier
                            .height(THUMB_CARD_HEIGHT)
                            .aspectRatio(THUMB_ASPECT_RATIO)
                            .then(focusModifier)
                            .then(exitFocusModifier),
                    )
                } else {
                    // Use backdrop/thumb image for landscape cards, fall back to primary
                    val backdropTags = item.backdropImageTags
                    val tags = item.imageTags
                    val imageUrl = when {
                        backdropTags?.isNotEmpty() == true ->
                            jellyfinClient.getBackdropUrl(item.id, backdropTags.first(), maxWidth = 400)
                        tags?.thumb != null ->
                            jellyfinClient.getThumbUrl(item.id, tags.thumb, maxWidth = 400)
                        else ->
                            jellyfinClient.getPrimaryImageUrl(item.id, tags?.primary)
                    }
                    ThumbCard(
                        title = item.name ?: "",
                        imageUrl = imageUrl,
                        onClick = { onItemClick(item.id) },
                        modifier = Modifier
                            .height(THUMB_CARD_HEIGHT)
                            .aspectRatio(THUMB_ASPECT_RATIO)
                            .then(focusModifier)
                            .then(exitFocusModifier),
                    )
                }
            }
        }
    }
}

/**
 * Discover (Seerr) search result row with landscape backdrop cards.
 */
@Composable
private fun DiscoverResultRow(
    title: String,
    results: List<SeerrMedia>,
    seerrRepository: SeerrRepository?,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onPersonClick: (personId: Int) -> Unit,
    firstRowFocusRequester: FocusRequester?,
    updateExitFocus: (FocusRequester) -> Unit,
) {
    val rowFocusRequester = remember { FocusRequester() }

    Column {
        Text(
            text = "$title (${results.size})",
            style = MaterialTheme.typography.titleSmall,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 48.dp),
        ) {
            itemsIndexed(
                results,
                key = { _, media -> "discover-${media.mediaType}-${media.id}" },
            ) { index, media ->
                val focusModifier = if (index == 0) {
                    Modifier.focusRequester(
                        if (firstRowFocusRequester != null) firstRowFocusRequester
                        else rowFocusRequester,
                    )
                } else {
                    Modifier
                }

                // Prefer backdrop, fall back to poster for persons
                val imageUrl = if (media.mediaType == "person") {
                    seerrRepository?.getProfileUrl(media.profilePath)
                } else {
                    seerrRepository?.getBackdropUrl(media.backdropPath, "w780")
                        ?: seerrRepository?.getPosterUrl(media.posterPath)
                }

                ThumbCard(
                    title = media.displayTitle,
                    imageUrl = imageUrl,
                    badge = when (media.mediaType) {
                        "movie" -> "Movie"
                        "tv" -> "TV"
                        "person" -> "Person"
                        else -> null
                    },
                    onClick = {
                        if (media.mediaType == "person") {
                            onPersonClick(media.tmdbId ?: media.id)
                        } else {
                            onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                        }
                    },
                    modifier = Modifier
                        .height(THUMB_CARD_HEIGHT)
                        .aspectRatio(THUMB_ASPECT_RATIO)
                        .then(focusModifier)
                        .onFocusChanged {
                            if (it.isFocused) {
                                updateExitFocus(firstRowFocusRequester ?: rowFocusRequester)
                            }
                        },
                )
            }
        }
    }
}

/**
 * Episode thumbnail card with split overlays:
 * series name at top, season/episode info at bottom.
 */
@Composable
private fun EpisodeThumbCard(
    item: JellyfinItem,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seriesName = item.seriesName
    val s = item.parentIndexNumber
    val e = item.indexNumber
    val episodeInfo = buildString {
        if (s != null && e != null) {
            append("S${s.toString().padStart(2, '0')}E${e.toString().padStart(2, '0')}")
            item.name?.let { append(" - $it") }
        } else {
            item.name?.let { append(it) }
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.small,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = seriesName ?: episodeInfo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Series name overlay at top
            if (seriesName != null) {
                Text(
                    text = seriesName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            // Episode info overlay at bottom
            if (episodeInfo.isNotEmpty()) {
                Text(
                    text = episodeInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/**
 * Landscape thumbnail card with title overlay, used for Movies, Series, and Discover results.
 */
@Composable
private fun ThumbCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    maxTitleLines: Int = 1,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.small,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Title overlay at bottom
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = maxTitleLines,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            // Optional type badge
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 8.sp,
                    )
                }
            }
        }
    }
}

/**
 * Voice search button with visual feedback for listening state.
 */
@Composable
private fun VoiceSearchButton(
    isListening: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = isAvailable,
        modifier = modifier.size(28.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isListening) TvColors.BluePrimary else TvColors.Surface,
            focusedContainerColor = if (isListening) TvColors.BluePrimary else TvColors.FocusedSurface,
            disabledContainerColor = TvColors.Surface.copy(alpha = 0.5f),
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(4.dp),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop listening" else "Voice search",
                tint = when {
                    !isAvailable -> TvColors.TextSecondary.copy(alpha = 0.5f)
                    isListening -> Color.White
                    else -> TvColors.TextPrimary
                },
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Horizontal row of suggestion chips shown while typing.
 */
@Composable
private fun SuggestionsRow(
    suggestions: List<SearchSuggestion>,
    firstSuggestionFocusRequester: FocusRequester,
    onSuggestionClick: (SearchSuggestion) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 6.dp),
    ) {
        itemsIndexed(suggestions, key = { _, s -> s.id }) { index, suggestion ->
            Surface(
                onClick = { onSuggestionClick(suggestion) },
                modifier = if (index == 0) Modifier.focusRequester(firstSuggestionFocusRequester) else Modifier,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvColors.Surface,
                    focusedContainerColor = TvColors.FocusedSurface,
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(1.dp, TvColors.BluePrimary),
                        shape = RoundedCornerShape(4.dp),
                    ),
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = suggestion.name,
                        fontSize = 11.sp,
                        color = TvColors.TextPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = suggestion.type,
                        fontSize = 9.sp,
                        color = TvColors.TextSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
