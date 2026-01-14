@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.network.JellyfinClient

/**
 * Backdrop layer for detail screens (movies and series).
 *
 * Similar to HeroBackdropLayer but with stronger left edge fading
 * to improve readability of metadata text on the left side.
 *
 * @param item The media item to display backdrop for
 * @param jellyfinClient Client for building image URLs
 * @param modifier Modifier for positioning and sizing
 */
@Composable
fun DetailBackdropLayer(
    item: JellyfinItem?,
    jellyfinClient: JellyfinClient,
    modifier: Modifier = Modifier,
) {
    if (item == null) return

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = item,
            transitionSpec = {
                fadeIn(animationSpec = tween(800)) togetherWith
                    fadeOut(animationSpec = tween(800))
            },
            label = "detail_backdrop_layer",
            modifier = Modifier.fillMaxSize(),
        ) { currentItem ->
            val backdropUrl = buildDetailBackdropUrl(currentItem, jellyfinClient)

            AsyncImage(
                model = backdropUrl,
                contentDescription = currentItem.name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.85f)
                    .drawWithContent {
                        drawContent()
                        // Left edge fade - stronger for detail screen metadata readability
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.05f to Color.Black.copy(alpha = 0.3f),
                                    0.15f to Color.Black.copy(alpha = 0.7f),
                                    0.30f to Color.Black.copy(alpha = 0.9f),
                                    0.45f to Color.Black,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                        // Bottom edge fade - blend into content rows
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    0.6f to Color.Black.copy(alpha = 0.9f),
                                    0.8f to Color.Black.copy(alpha = 0.5f),
                                    0.95f to Color.Black.copy(alpha = 0.2f),
                                    1.0f to Color.Transparent,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
    }
}

/**
 * Build backdrop URL for an item, falling back to series backdrop for episodes.
 */
private fun buildDetailBackdropUrl(item: JellyfinItem, jellyfinClient: JellyfinClient): String {
    // For episodes, use series backdrop if item has no backdrop
    val backdropId = if (!item.backdropImageTags.isNullOrEmpty()) {
        item.id
    } else if (item.isEpisode && item.seriesId != null) {
        item.seriesId!!
    } else {
        item.id
    }

    val tag = if (!item.backdropImageTags.isNullOrEmpty()) {
        item.backdropImageTags!!.firstOrNull()
    } else {
        null
    }

    return jellyfinClient.getBackdropUrl(backdropId, tag, maxWidth = 1920)
}
