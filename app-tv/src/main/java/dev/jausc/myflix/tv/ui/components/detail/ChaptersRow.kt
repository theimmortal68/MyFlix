@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
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
 */
@Composable
fun ChaptersRow(
    chapters: List<JellyfinChapter>,
    itemId: String,
    getChapterImageUrl: (Int) -> String,
    onChapterClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty()) return

    val state = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Chapters",
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
}

/**
 * Individual chapter card with thumbnail and title for TV.
 * Uses Surface for proper focus indication.
 */
@Composable
private fun ChapterCard(
    chapter: JellyfinChapter,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvColors.BluePrimary,
        ),
        modifier = modifier.width(210.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Thumbnail with timestamp overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = chapter.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )

                // Gradient overlay for timestamp visibility
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f),
                                ),
                                startY = 50f,
                            ),
                        ),
                )

                // Timestamp badge
                chapter.startPositionTicks?.let { ticks ->
                    Text(
                        text = TimeFormatUtil.formatTicksToTime(ticks),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                    )
                }
            }

            // Chapter name
            chapter.name?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}
