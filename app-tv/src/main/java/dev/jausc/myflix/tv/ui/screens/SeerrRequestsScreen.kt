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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrRequest
import dev.jausc.myflix.core.seerr.SeerrRequestStatus
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors

@Composable
fun SeerrRequestsScreen(
    seerrClient: SeerrClient,
    onBack: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requests by remember { mutableStateOf<List<SeerrRequest>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        errorMessage = null
        seerrClient.getMyRequests()
            .onSuccess { requests = it.results }
            .onFailure { errorMessage = it.message ?: "Failed to load requests" }
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
                text = "Requests",
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
                        text = errorMessage ?: "Failed to load requests",
                        color = TvColors.Error,
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(requests, key = { it.id }) { request ->
                        SeerrRequestRow(request = request)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeerrRequestRow(request: SeerrRequest) {
    val statusText = SeerrRequestStatus.toDisplayString(request.status)
    val mediaType = request.media?.mediaType ?: "unknown"
    val mediaId = request.media?.tmdbId ?: request.media?.id

    Surface(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Request #${request.id}",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$mediaType • ${mediaId ?: "unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.BluePrimary,
            )
        }
    }
}
