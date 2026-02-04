@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HearingDisabled
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.network.RemoteSubtitleInfo
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

/**
 * State for subtitle search dialog.
 */
private sealed interface SubtitleSearchState {
    data object Idle : SubtitleSearchState
    data object Searching : SubtitleSearchState
    data class Success(val results: List<RemoteSubtitleInfo>) : SubtitleSearchState
    data class Error(val message: String) : SubtitleSearchState
    data class Downloading(val subtitle: RemoteSubtitleInfo) : SubtitleSearchState
}

/**
 * Dialog for searching and downloading subtitles from OpenSubtitles via Jellyfin.
 * Requires the OpenSubtitles plugin to be installed on the Jellyfin server.
 *
 * @param itemId The Jellyfin item ID to search subtitles for
 * @param itemName Display name of the item (movie/episode title)
 * @param jellyfinClient Client for API calls
 * @param onDismiss Called when the dialog should close
 * @param onSubtitleDownloaded Called when a subtitle has been successfully downloaded
 * @param modifier Optional modifier
 */
@Composable
fun SubtitleSearchDialog(
    itemId: String,
    itemName: String,
    jellyfinClient: JellyfinClient,
    onDismiss: () -> Unit,
    onSubtitleDownloaded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var searchLanguage by remember { mutableStateOf("eng") }
    var state by remember { mutableStateOf<SubtitleSearchState>(SubtitleSearchState.Idle) }
    val searchFocusRequester = remember { FocusRequester() }
    val resultsFocusRequester = remember { FocusRequester() }

    // Focus search field on open
    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    // Focus first result when search completes
    LaunchedEffect(state) {
        if (state is SubtitleSearchState.Success) {
            val results = (state as SubtitleSearchState.Success).results
            if (results.isNotEmpty()) {
                try {
                    resultsFocusRequester.requestFocus()
                } catch (_: Exception) {
                    // Ignore focus errors
                }
            }
        }
    }

    fun performSearch() {
        if (searchLanguage.isBlank()) return
        state = SubtitleSearchState.Searching
        scope.launch {
            jellyfinClient.searchRemoteSubtitles(itemId, searchLanguage.trim())
                .onSuccess { results ->
                    state = if (results.isEmpty()) {
                        SubtitleSearchState.Error("No subtitles found for '$searchLanguage'")
                    } else {
                        SubtitleSearchState.Success(results)
                    }
                }
                .onFailure { error ->
                    val message = when {
                        error.message?.contains("404") == true ->
                            "OpenSubtitles plugin not installed on server"
                        error.message?.contains("401") == true ||
                            error.message?.contains("403") == true ->
                            "Not authorized to search subtitles"
                        else -> error.message ?: "Search failed"
                    }
                    state = SubtitleSearchState.Error(message)
                }
        }
    }

    fun downloadSubtitle(subtitle: RemoteSubtitleInfo) {
        val subtitleId = subtitle.id ?: return
        state = SubtitleSearchState.Downloading(subtitle)
        scope.launch {
            jellyfinClient.downloadRemoteSubtitle(itemId, subtitleId)
                .onSuccess {
                    onSubtitleDownloaded()
                    onDismiss()
                }
                .onFailure { error ->
                    state = SubtitleSearchState.Error(
                        error.message ?: "Failed to download subtitle",
                    )
                }
        }
    }

    TvCenteredPopup(
        visible = true,
        onDismiss = onDismiss,
        minWidth = 450.dp,
        maxWidth = 600.dp,
        maxHeight = 500.dp,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Subtitles,
                    contentDescription = null,
                    tint = TvColors.BluePrimary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Search Subtitles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            // Item name subtitle
            Text(
                text = itemName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Search input row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Language input
                BasicTextField(
                    value = searchLanguage,
                    onValueChange = { searchLanguage = it },
                    enabled = state !is SubtitleSearchState.Searching &&
                        state !is SubtitleSearchState.Downloading,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                    ),
                    cursorBrush = SolidColor(TvColors.BluePrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                        .focusRequester(searchFocusRequester),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchLanguage.isEmpty()) {
                                Text(
                                    text = "Language code (eng, spa, fra...)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.4f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                // Search button
                ListItem(
                    selected = false,
                    onClick = { performSearch() },
                    enabled = state !is SubtitleSearchState.Searching &&
                        state !is SubtitleSearchState.Downloading &&
                        searchLanguage.isNotBlank(),
                    headlineContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = TvColors.BluePrimary.copy(alpha = 0.6f),
                        focusedContainerColor = TvColors.BluePrimary,
                        disabledContentColor = Color.White.copy(alpha = 0.4f),
                    ),
                    shape = ListItemDefaults.shape(shape = RoundedCornerShape(6.dp)),
                    modifier = Modifier.width(110.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Results area
            when (val currentState = state) {
                is SubtitleSearchState.Idle -> {
                    Text(
                        text = "Enter a 3-letter language code and press Search.\n" +
                            "Requires OpenSubtitles plugin on server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp),
                    )
                }

                is SubtitleSearchState.Searching -> {
                    Text(
                        text = "Searching for subtitles...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(8.dp),
                    )
                }

                is SubtitleSearchState.Error -> {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.Error,
                        modifier = Modifier.padding(8.dp),
                    )
                }

                is SubtitleSearchState.Downloading -> {
                    Text(
                        text = "Downloading: ${currentState.subtitle.name ?: "subtitle"}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.BluePrimary,
                        modifier = Modifier.padding(8.dp),
                    )
                }

                is SubtitleSearchState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                    ) {
                        items(
                            items = currentState.results,
                            key = { it.id ?: it.name ?: it.hashCode().toString() },
                        ) { subtitle ->
                            val isFirst = currentState.results.first() == subtitle
                            SubtitleResultItem(
                                subtitle = subtitle,
                                onClick = { downloadSubtitle(subtitle) },
                                modifier = if (isFirst) {
                                    Modifier.focusRequester(resultsFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleResultItem(
    subtitle: RemoteSubtitleInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = {
            Text(
                text = subtitle.name ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Provider
                subtitle.providerName?.let { provider ->
                    Text(
                        text = provider,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }

                // Language
                subtitle.threeLetterIsoLanguageName?.let { lang ->
                    Text(
                        text = lang.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.BluePrimary.copy(alpha = 0.8f),
                    )
                }

                // Downloads count
                subtitle.downloadCount?.let { count ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Downloads",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = formatDownloadCount(count),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }

                // Rating
                subtitle.communityRating?.let { rating ->
                    if (rating > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
            // Hearing impaired indicator
            if (subtitle.isHearingImpaired == true) {
                Icon(
                    imageVector = Icons.Outlined.HearingDisabled,
                    contentDescription = "Hearing Impaired",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
        ),
        shape = ListItemDefaults.shape(shape = RoundedCornerShape(6.dp)),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Format download count with K/M suffix for large numbers.
 */
private fun formatDownloadCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
