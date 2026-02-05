@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens

import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.ui.SeerrSearchFilter
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun SeerrSearchScreen(
    seerrRepository: SeerrRepository,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onPersonClick: (personId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val firstItemFocusRequester = remember { FocusRequester() }

    // NavRail exit focus registration
    val updateExitFocus = rememberExitFocusRegistry(firstItemFocusRequester)

    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SeerrSearchFilter.ALL) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<SeerrMedia>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }

    suspend fun loadSearch(pageToLoad: Int, append: Boolean) {
        if (query.length < 2) return
        if (append) {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        errorMessage = null

        seerrRepository.search(query, pageToLoad)
            .onSuccess { response ->
                val filtered = response.results.filter { media ->
                    media.mediaType in allowedMediaTypes && selectedFilter.matches(media)
                }
                results = if (append) {
                    (results + filtered).distinctBy { "${it.mediaType}-${it.id}" }
                } else {
                    filtered
                }
                page = response.page
                totalPages = response.totalPages
            }
            .onFailure { error ->
                if (!append) {
                    errorMessage = error.message ?: "Search failed"
                }
            }

        if (append) {
            isLoadingMore = false
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(query, selectedFilter) {
        if (query.length < 2) {
            results = emptyList()
            errorMessage = null
            page = 1
            totalPages = 1
            isLoading = false
            isLoadingMore = false
            return@LaunchedEffect
        }

        results = emptyList()
        page = 1
        totalPages = 1
        loadSearch(pageToLoad = 1, append = false)
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                val shouldLoadMore = lastVisibleIndex >= results.lastIndex - 4
                val hasMore = page < totalPages
                if (shouldLoadMore && hasMore && !isLoading && !isLoadingMore && query.length >= 2) {
                    scope.launch { loadSearch(pageToLoad = page + 1, append = true) }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                scale = ButtonDefaults.scale(focusedScale = 1f),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                    contentColor = TvColors.TextPrimary,
                    focusedContainerColor = TvColors.BluePrimary,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Seerr Search",
                style = MaterialTheme.typography.headlineMedium,
                color = TvColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(
            factory = {
                EditText(context).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    setHintTextColor(android.graphics.Color.GRAY)
                    hint = "Search movies, TV, people"
                    textSize = 18f
                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    setSingleLine(true)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) = Unit
                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            query = s?.toString().orEmpty()
                        }
                        override fun afterTextChanged(s: Editable?) = Unit
                    })
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SeerrSearchFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                Button(
                    onClick = { selectedFilter = filter },
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = if (isSelected) {
                        ButtonDefaults.colors(
                            containerColor = TvColors.BluePrimary,
                            contentColor = TvColors.TextPrimary,
                            focusedContainerColor = TvColors.BluePrimary,
                        )
                    } else {
                        ButtonDefaults.colors(
                            containerColor = TvColors.Surface,
                            contentColor = TvColors.TextPrimary,
                            focusedContainerColor = TvColors.FocusedSurface,
                        )
                    },
                ) {
                    Text(filter.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Search failed",
                        color = TvColors.Error,
                    )
                }
            }
            results.isEmpty() && query.length >= 2 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found",
                        color = TvColors.TextSecondary,
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(results, key = { _, media -> "${media.mediaType}-${media.id}" }) { index, media ->
                        SeerrSearchPosterCard(
                            media = media,
                            seerrRepository = seerrRepository,
                            onClick = {
                                if (media.mediaType == "person") {
                                    onPersonClick(media.tmdbId ?: media.id)
                                } else {
                                    onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                                }
                            },
                            modifier = if (index == 0) {
                                Modifier
                                    .focusRequester(firstItemFocusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            updateExitFocus(firstItemFocusRequester)
                                        }
                                    }
                            } else {
                                Modifier.onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        updateExitFocus(firstItemFocusRequester)
                                    }
                                }
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                TvLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrSearchPosterCard(
    media: SeerrMedia,
    seerrRepository: SeerrRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = if (media.mediaType == "person") {
        seerrRepository.getProfileUrl(media.profilePath)
    } else {
        seerrRepository.getPosterUrl(media.posterPath)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.width(120.dp).aspectRatio(2f / 3f),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = media.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
            ) {
                Text(
                    text = media.displayTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = mediaTypeLabel(media),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

private val allowedMediaTypes = setOf("movie", "tv", "person")

private fun mediaTypeLabel(media: SeerrMedia): String = when (media.mediaType) {
    "movie" -> "Movie"
    "tv" -> "TV"
    "person" -> "Person"
    else -> ""
}
