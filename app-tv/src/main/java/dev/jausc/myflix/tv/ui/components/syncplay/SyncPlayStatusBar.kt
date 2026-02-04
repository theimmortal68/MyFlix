@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.syncplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Sync status indicator for SyncPlay sessions.
 */
enum class SyncStatus {
    /** All participants are synced - green indicator */
    SYNCED,
    /** Speed correction is active - amber indicator */
    SYNCING,
    /** Waiting for group members to buffer - gray indicator */
    BUFFERING,
}

/**
 * A small status bar shown at the top of the player screen when in a SyncPlay session.
 * Displays the group name, member count, and current sync status.
 *
 * Visual design:
 * ```
 * ┌────────────────────────────────────────────────────────────┐
 * │ [Group Icon] Movie Night · 3 watching · ● Synced           │
 * └────────────────────────────────────────────────────────────┘
 * ```
 *
 * @param groupName The name of the SyncPlay group
 * @param memberCount Number of participants currently watching
 * @param syncStatus Current synchronization status
 * @param visible Whether the status bar is visible (controls animation)
 * @param modifier Optional modifier for the container
 */
@Composable
fun SyncPlayStatusBar(
    groupName: String,
    memberCount: Int,
    syncStatus: SyncStatus,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val (statusColor, statusText) = when (syncStatus) {
        SyncStatus.SYNCED -> TvColors.Success to "Synced"
        SyncStatus.SYNCING -> Color(0xFFFBBF24) to "Syncing" // Amber
        SyncStatus.BUFFERING -> Color.White.copy(alpha = 0.5f) to "Buffering"
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Group icon
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = TvColors.BluePrimary,
                    modifier = Modifier.size(20.dp),
                )

                // Group name
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )

                // Separator
                Text(
                    text = "\u00B7", // Middle dot
                    color = Color.White.copy(alpha = 0.5f),
                )

                // Member count
                Text(
                    text = "$memberCount watching",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )

                // Separator
                Text(
                    text = "\u00B7", // Middle dot
                    color = Color.White.copy(alpha = 0.5f),
                )

                // Status indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                    )

                    // Status text
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                    )
                }
            }
        }
    }
}
