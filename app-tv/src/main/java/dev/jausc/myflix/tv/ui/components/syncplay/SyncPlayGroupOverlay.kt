@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.syncplay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.network.syncplay.GroupMember
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

/**
 * Full-screen overlay for viewing SyncPlay group members and performing actions.
 * Slides in from the right side during playback, showing group info with a semi-transparent backdrop.
 *
 * Visual design:
 * ```
 * ┌───────────────────────────┬─────────────────────────────┐
 * │                           │                             │
 * │    (video continues       │   SyncPlay: Movie Night     │
 * │     playing behind        │                             │
 * │     semi-transparent      │   ───────────────────────   │
 * │     backdrop)             │   Members (3)               │
 * │                           │   ┌─────────────────────┐   │
 * │                           │   │ 👤 John (Host)      │   │
 * │                           │   └─────────────────────┘   │
 * │                           │   ┌─────────────────────┐   │
 * │                           │   │ 👤 Sarah            │   │
 * │                           │   └─────────────────────┘   │
 * │                           │   ┌─────────────────────┐   │
 * │                           │   │ 👤 Mike             │   │
 * │                           │   └─────────────────────┘   │
 * │                           │                             │
 * │                           │   ───────────────────────   │
 * │                           │   Actions                   │
 * │                           │   ┌─────────────────────┐   │
 * │                           │   │ [+] Add to Queue    │   │
 * │                           │   └─────────────────────┘   │
 * │                           │   ┌─────────────────────┐   │
 * │                           │   │ [X] Leave Group     │   │
 * │                           │   └─────────────────────┘   │
 * │                           │                             │
 * └───────────────────────────┴─────────────────────────────┘
 * ```
 *
 * @param visible Whether the overlay is visible (controls animation)
 * @param groupName The name of the SyncPlay group
 * @param members List of group members
 * @param onAddToQueue Callback when "Add to Queue" action is selected
 * @param onLeaveGroup Callback when "Leave Group" action is selected
 * @param onDismiss Callback when the overlay should be dismissed (back press or backdrop click)
 * @param modifier Optional modifier for the container
 */
@Composable
fun SyncPlayGroupOverlay(
    visible: Boolean,
    groupName: String,
    members: List<GroupMember>,
    onAddToQueue: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstFocusRequester = remember { FocusRequester() }

    // Request focus on first action button when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            delay(200) // Wait for animation
            firstFocusRequester.requestFocus()
        }
    }

    // Back handler to dismiss overlay
    BackHandler(enabled = visible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            // Panel on right side
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(380.dp)
                    .background(
                        Color(0xF2181818),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // Consume clicks to prevent dismissal
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    // Header
                    Text(
                        text = "SyncPlay",
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.BluePrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(Modifier.height(24.dp))

                    // Members section
                    Text(
                        text = "Members (${members.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(members, key = { it.userId }) { member ->
                            MemberItem(member = member)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Actions section
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(8.dp))

                    // Action buttons with focus management
                    OverlayActionButton(
                        text = "Add to Queue",
                        icon = Icons.Default.Add,
                        onClick = onAddToQueue,
                        modifier = Modifier.focusRequester(firstFocusRequester),
                    )
                    Spacer(Modifier.height(8.dp))
                    OverlayActionButton(
                        text = "Leave Group",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        onClick = onLeaveGroup,
                        isDestructive = true,
                    )
                }
            }
        }
    }
}

/**
 * Display item for a group member (non-focusable).
 */
@Composable
private fun MemberItem(member: GroupMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = if (member.isHost) TvColors.BluePrimary else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = member.userName,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        if (member.isHost) {
            Text(
                text = "Host",
                style = MaterialTheme.typography.labelSmall,
                color = TvColors.BluePrimary,
            )
        }
    }
}

/**
 * Focusable action button for the overlay.
 */
@Composable
private fun OverlayActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    val focusedColor = if (isDestructive) TvColors.Error else TvColors.BluePrimary

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = focusedColor.copy(alpha = 0.2f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) TvColors.Error else Color.White,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDestructive) TvColors.Error else Color.White,
            )
        }
    }
}
