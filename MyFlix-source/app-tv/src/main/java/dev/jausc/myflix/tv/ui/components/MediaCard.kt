package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Standard card sizes used throughout the app.
 * These should be used consistently unless specifically overridden.
 * 
 * Sizing is calculated to fit exact number of cards on screen:
 * - Portrait cards (113dp): 7 cards fit across with 16dp spacing
 * - Landscape cards (210dp): 4 cards fit across with 16dp spacing
 */
object CardSizes {
    /** Portrait card width (2:3 aspect ratio) - for movie/series posters */
    val MediaCardWidth = 113.dp
    
    /** Landscape card width (16:9 aspect ratio) - for episode thumbnails */
    val WideMediaCardWidth = 210.dp
}

/**
 * Portrait media card for movies and series posters (2:3 aspect ratio)
 * Title scrolls when focused if it doesn't fit
 */
@Composable
fun MediaCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onItemFocused: ((JellyfinItem) -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(CardSizes.MediaCardWidth)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onItemFocused?.invoke(item)
                }
            },
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium
            )
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                if (item.progressPercent > 0f && item.progressPercent < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TvColors.Surface.copy(alpha = 0.7f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.progressPercent)
                                .background(TvColors.BluePrimary)
                        )
                    }
                }
            }

            if (showLabel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isFocused) Modifier.basicMarquee() else Modifier
                    )
                    item.productionYear?.let { year ->
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wide/landscape media card for episodes and continue watching (16:9 aspect ratio)
 * Shows episode thumbnail with series name and episode info
 * Titles scroll when focused if they don't fit
 */
@Composable
fun WideMediaCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onItemFocused: ((JellyfinItem) -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(CardSizes.WideMediaCardWidth)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onItemFocused?.invoke(item)
                }
            },
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium
            )
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                // Progress bar at bottom
                if (item.progressPercent > 0f && item.progressPercent < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TvColors.Surface.copy(alpha = 0.7f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.progressPercent)
                                .background(TvColors.BluePrimary)
                        )
                    }
                }
            }

            if (showLabel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Series name (if episode) - scrolls when focused
                    item.seriesName?.let { seriesName ->
                        Text(
                            text = seriesName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TvColors.BlueAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (isFocused) Modifier.basicMarquee() else Modifier
                        )
                    }
                    
                    // Episode/Item name - scrolls when focused
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isFocused) Modifier.basicMarquee() else Modifier
                    )
                }
            }
        }
    }
}
