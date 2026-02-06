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
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.tv.ui.components.detail.ExpandablePlayButton

/**
 * Horizontal row of action buttons for the detail screen.
 * Uses ExpandablePlayButton style matching the movie detail screen.
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Primary action based on status
        when (media.availabilityStatus) {
            SeerrMediaStatus.AVAILABLE -> {
                ExpandablePlayButton(
                    title = "Play",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = { /* Play in Jellyfin */ },
                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                )
            }

            SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> {
                ExpandablePlayButton(
                    title = "Requested",
                    icon = Icons.Outlined.Close,
                    onClick = { /* TODO: Cancel logic */ },
                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                )
            }

            else -> {
                ExpandablePlayButton(
                    title = if (isRequesting) "Requesting..." else "Request",
                    icon = Icons.Outlined.Add,
                    onClick = onRequest,
                    modifier = Modifier.focusRequester(actionButtonFocusRequester),
                )
            }
        }

        // Trailer button
        trailer?.key?.let { videoKey ->
            ExpandablePlayButton(
                title = "Trailer",
                icon = Icons.Outlined.PlayArrow,
                onClick = { onTrailerClick(videoKey, trailer.name ?: trailer.type) },
            )
        }

        // Hide from Discover
        ExpandablePlayButton(
            title = "Hide",
            icon = Icons.Outlined.Block,
            onClick = { /* TODO: Blacklist via repository */ },
        )
    }
}
