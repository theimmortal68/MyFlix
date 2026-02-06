@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.seerr.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.tv.ui.components.TvIconTextButton

/**
 * Horizontal row of action buttons for the detail screen.
 * Shows contextual buttons based on media availability status.
 */
@Composable
fun DetailActionButtons(
    media: SeerrMedia,
    isRequesting: Boolean,
    onRequest: () -> Unit,
    onTrailerClick: (videoKey: String, title: String?) -> Unit,
    actionButtonFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val trailer = media.relatedVideos
        ?.filter { it.type == "Trailer" && it.site == "YouTube" }
        ?.lastOrNull()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Primary action based on status
        when (media.availabilityStatus) {
            SeerrMediaStatus.AVAILABLE -> {
                TvIconTextButton(
                    icon = Icons.Outlined.PlayArrow,
                    text = "Play",
                    onClick = { /* Play in Jellyfin */ },
                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                    containerColor = Color(0xFF22C55E),
                )
            }

            SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> {
                TvIconTextButton(
                    icon = Icons.Outlined.Close,
                    text = "Requested",
                    onClick = { /* TODO: Cancel logic */ },
                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                    containerColor = Color(0xFFFBBF24),
                )
            }

            else -> {
                TvIconTextButton(
                    icon = Icons.Outlined.Add,
                    text = if (isRequesting) "Requesting..." else "Request",
                    onClick = onRequest,
                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                    enabled = !isRequesting,
                    isLoading = isRequesting,
                    containerColor = Color(0xFF8B5CF6),
                )
            }
        }

        // Trailer button
        trailer?.key?.let { videoKey ->
            TvIconTextButton(
                icon = Icons.Outlined.PlayArrow,
                text = "Trailer",
                onClick = { onTrailerClick(videoKey, trailer.name ?: trailer.type) },
                containerColor = Color(0xFFFF0000),
            )
        }

        // Hide from Discover
        TvIconTextButton(
            icon = Icons.Outlined.Block,
            text = "Hide",
            onClick = { /* TODO: Blacklist via repository */ },
        )
    }
}
