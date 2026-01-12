@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrCollection
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors

@Composable
fun SeerrCollectionDetailScreen(
    collectionId: Int,
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    onBack: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var collection by remember { mutableStateOf<SeerrCollection?>(null) }

    LaunchedEffect(collectionId) {
        isLoading = true
        errorMessage = null
        seerrClient.getCollection(collectionId)
            .onSuccess { collection = it }
            .onFailure { errorMessage = it.message ?: "Failed to load collection" }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack, modifier = Modifier.height(40.dp)) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = collection?.name ?: "Collection",
                style = MaterialTheme.typography.headlineMedium,
                color = TvColors.TextPrimary,
            )
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
                        text = errorMessage ?: "Failed to load collection",
                        color = TvColors.Error,
                    )
                }
            }
            else -> {
                val parts = collection?.parts ?: emptyList()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(parts, key = { it.id }) { media ->
                        SeerrCollectionPosterCard(
                            media = media,
                            seerrClient = seerrClient,
                            onClick = {
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrCollectionPosterCard(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
) {
    val posterUrl = seerrClient.getPosterUrl(media.posterPath)

    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(240.dp),
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
                model = posterUrl,
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
        }
    }
}
