@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinChapter
import dev.jausc.myflix.core.common.util.TimeFormatUtil
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Horizontal row of chapter thumbnails with timestamps for TV.
 * Supports D-pad navigation with focus management.
 * Clicking a chapter seeks to that position in the video.
 * Matches home screen row styling with accent bar.
 */
@Suppress("UnusedParameter")
@Composable
fun ChaptersRow(
    chapters: List<JellyfinChapter>,
    itemId: String,
    getChapterImageUrl: (Int) -> String,
    onChapterClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFE5A00D),
) {
    if (chapters.isEmpty()) return

    val state = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }

    // Chapters row without title (title is redundant when inside Chapters tab)
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .focusRestorer(firstFocus),
    ) {
        itemsIndexed(chapters) { index, chapter ->
            val cardModifier = if (index == 0) {
                Modifier.focusRequester(firstFocus)
            } else {
                Modifier
            }.onFocusChanged {
                if (it.isFocused) {
                    focusedIndex = index
                }
            }

            ChapterCard(
                chapter = chapter,
                imageUrl = getChapterImageUrl(index),
                onClick = {
                    chapter.startPositionTicks?.let { ticks ->
                        // Convert ticks to milliseconds for playback
                        onChapterClick(ticks / 10_000)
                    }
                },
                modifier = cardModifier,
            )
        }
    }
}

/**
 * Individual chapter card with thumbnail and title for TV.
 * Focus border only on thumbnail, title and timestamp below.
 */
@Composable
private fun ChapterCard(
    chapter: JellyfinChapter,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier
            .width(201.dp)
            .onFocusChanged { isFocused = it.isFocused },
    ) {
        Column {
            // Thumbnail only - gets focus border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isFocused) {
                            Modifier.background(TvColors.BluePrimary.copy(alpha = 0.3f))
                        } else {
                            Modifier
                        },
                    ),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = chapter.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = TvColors.BluePrimary,
                                    shape = RoundedCornerShape(8.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }

            // Title and timestamp row below thumbnail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Chapter name - left aligned
                chapter.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Timestamp - right aligned
                chapter.startPositionTicks?.let { ticks ->
                    Text(
                        text = TimeFormatUtil.formatTicksToTime(ticks),
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.TextSecondary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
