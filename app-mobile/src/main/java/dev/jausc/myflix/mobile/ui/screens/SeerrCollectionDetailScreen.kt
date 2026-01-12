@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrCollection
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import kotlinx.coroutines.launch

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
    var refreshTrigger by remember { mutableStateOf(0) }
    var requestingId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(collectionId, refreshTrigger) {
        isLoading = true
        errorMessage = null
        seerrClient.getCollection(collectionId)
            .onSuccess { collection = it }
            .onFailure { errorMessage = it.message ?: "Failed to load collection" }
        isLoading = false
    }

    fun requestMedia(media: SeerrMedia) {
        scope.launch {
            requestingId = media.id
            val mediaType = if (media.mediaType.isNotBlank()) media.mediaType else if (media.isMovie) "movie" else "tv"
            val tmdbId = media.tmdbId ?: media.id
            val requestResult = if (mediaType == "movie") {
                seerrClient.requestMovie(tmdbId)
            } else {
                seerrClient.requestTVShow(tmdbId, null)
            }
            requestResult
                .onSuccess { refreshTrigger++ }
                .onFailure { errorMessage = it.message ?: "Request failed" }
            requestingId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = collection?.name ?: "Collection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Failed to load collection",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                val parts = collection?.parts ?: emptyList()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(parts, key = { it.id }) { media ->
                        SeerrCollectionMediaRow(
                            media = media,
                            seerrClient = seerrClient,
                            isRequesting = requestingId == media.id,
                            onClick = {
                                onMediaClick(media.mediaType, media.tmdbId ?: media.id)
                            },
                            onRequest = { requestMedia(media) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrCollectionMediaRow(
    media: SeerrMedia,
    seerrClient: SeerrClient,
    isRequesting: Boolean,
    onClick: () -> Unit,
    onRequest: () -> Unit,
) {
    val posterUrl = seerrClient.getPosterUrl(media.posterPath)
    val availabilityStatus = media.availabilityStatus
    val (requestLabel, canRequest) = when (availabilityStatus) {
        SeerrMediaStatus.AVAILABLE -> "Available" to false
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> "Requested" to false
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> "Request" to true
        else -> "Request" to true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeerrPosterThumbnail(posterUrl = posterUrl, title = media.displayTitle, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = media.year?.toString() ?: "Unknown year",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onRequest,
            enabled = canRequest && !isRequesting,
        ) {
            Text(text = if (isRequesting) "Requesting..." else requestLabel)
        }
    }
}

@Composable
private fun SeerrPosterThumbnail(
    posterUrl: String?,
    title: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        SeerrPosterImage(posterUrl = posterUrl, title = title)
    }
}

@Composable
private fun SeerrPosterImage(
    posterUrl: String?,
    title: String,
) {
    AsyncImage(
        model = posterUrl,
        contentDescription = title,
        modifier = Modifier
            .width(56.dp)
            .height(84.dp),
    )
}
