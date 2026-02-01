@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.material3.Glow
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isDolbyVision
import dev.jausc.myflix.core.common.model.isHdr
import dev.jausc.myflix.core.common.model.progressPercent
import dev.jausc.myflix.tv.ui.theme.TvColors

// Status indicator colors
private val WatchedGreen = Color(0xFF4CAF50)
private val FavoritePink = Color(0xFFE91E63)

// HDR/DV Badge Colors
private val DolbyVisionOrange = Color(0xFFFF6B00)
private val HdrBlue = Color(0xFF4169E1)

/**
 * Standard card sizes used throughout the app.
 * These should be used consistently unless specifically overridden.
 *
 * Both card types share the same height (156dp) for visual consistency:
 * - Portrait cards (104dp × 156dp): 7 cards fit across with spacing
 * - Landscape cards (277dp × 156dp): 3 cards fit across with spacing
 */
object CardSizes {
    /** Portrait card width (2:3 aspect ratio) - sized for 7 cards across screen */
    val MediaCardWidth = 110.dp

    /** Landscape card width (16:9 aspect ratio) - for episode thumbnails, 4 across */
    val WideMediaCardWidth = 210.dp

    /** Season poster width - smaller for navigational use (10 across) */
    val SeasonPosterWidth = 80.dp

    /** Season poster height - maintains 5:7 aspect ratio */
    val SeasonPosterHeight = 112.dp
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
    onLongClick: (() -> Unit)? = null,
    onItemFocused: ((JellyfinItem) -> Unit)? = null,
) {
    MediaCardInternal(
        item = item,
        imageUrl = imageUrl,
        onClick = onClick,
        aspectRatio = 2f / 3f,
        modifier = modifier.width(CardSizes.MediaCardWidth),
        showLabel = showLabel,
        onLongClick = onLongClick,
        onItemFocused = onItemFocused,
    )
}

/**
 * Media card with configurable aspect ratio for library grid views.
 * Poster mode: 2:3 aspect ratio (7 columns)
 * Thumbnail mode: 16:9 aspect ratio (4 columns)
 */
@Composable
fun MediaCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onItemFocused: ((JellyfinItem) -> Unit)? = null,
) {
    MediaCardInternal(
        item = item,
        imageUrl = imageUrl,
        onClick = onClick,
        aspectRatio = aspectRatio,
        modifier = modifier,
        showLabel = showLabel,
        onLongClick = onLongClick,
        onItemFocused = onItemFocused,
    )
}

@Composable
private fun MediaCardInternal(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onItemFocused: ((JellyfinItem) -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onItemFocused?.invoke(item)
                }
            },
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium,
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevation = 12.dp,
                elevationColor = TvColors.BluePrimary.copy(alpha = 0.5f)
            )
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .then(
                            if (isFocused) {
                                Modifier.border(
                                    BorderStroke(2.dp, TvColors.BluePrimary),
                                    MaterialTheme.shapes.medium,
                                )
                            } else {
                                Modifier
                            },
                        ),
                    contentScale = ContentScale.Crop,
                )

                // Favorite indicator (top-left corner) - takes priority over HDR badges
                val isFavorite = item.userData?.isFavorite == true
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = FavoritePink,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else {
                    // HDR/DV badge (top-left corner) - only show if not favorite
                    if (item.isDolbyVision) {
                        HdrBadge(
                            text = "DV",
                            color = DolbyVisionOrange,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                    } else if (item.isHdr) {
                        HdrBadge(
                            text = "HDR",
                            color = HdrBlue,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                    }
                }

                // Watched indicator (top-right corner)
                val isWatched = item.userData?.played == true
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Watched",
                            tint = WatchedGreen,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                if (item.progressPercent > 0f && item.progressPercent < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TvColors.Surface.copy(alpha = 0.7f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.progressPercent)
                                .background(TvColors.BluePrimary),
                        )
                    }
                }
            }

            if (showLabel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    // Title with marquee when focused
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = TvColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isFocused) Modifier.basicMarquee() else Modifier,
                    )
                    // Year on separate line
                    item.productionYear?.let { year ->
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TvColors.TextSecondary,
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
 *
 * @param showBackground If true, shows a surface background. If false, only shows the image with border on focus.
 */
@Composable
fun WideMediaCard(
    item: JellyfinItem,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    showBackground: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onItemFocused: ((JellyfinItem) -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(CardSizes.WideMediaCardWidth)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onItemFocused?.invoke(item)
                }
            },
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium,
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (showBackground) TvColors.Surface else Color.Transparent,
            focusedContainerColor = if (showBackground) TvColors.FocusedSurface else Color.Transparent,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevation = 12.dp,
                elevationColor = TvColors.BluePrimary.copy(alpha = 0.5f)
            )
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )

                // HDR/DV badge (top-left corner)
                if (item.isDolbyVision) {
                    HdrBadge(
                        text = "DV",
                        color = DolbyVisionOrange,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                } else if (item.isHdr) {
                    HdrBadge(
                        text = "HDR",
                        color = HdrBlue,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                }

                // Progress bar at bottom
                if (item.progressPercent > 0f && item.progressPercent < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TvColors.Surface.copy(alpha = 0.7f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.progressPercent)
                                .background(TvColors.BluePrimary),
                        )
                    }
                }
            }

            if (showLabel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                            modifier = if (isFocused) Modifier.basicMarquee() else Modifier,
                        )
                    }

                    // Episode/Item name - scrolls when focused
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isFocused) Modifier.basicMarquee() else Modifier,
                    )
                }
            }
        }
    }
}

/**
 * HDR/Dolby Vision badge overlay for media cards
 */
@Composable
private fun HdrBadge(text: String, color: Color, modifier: Modifier = Modifier,) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}
