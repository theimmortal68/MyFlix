@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.syncplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.network.syncplay.SyncPlayGroup
import dev.jausc.myflix.tv.ui.components.TvCenteredPopup
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

/**
 * Dialog for creating or joining SyncPlay groups.
 * Displays available groups and allows creating new ones.
 *
 * @param visible Whether the dialog is visible
 * @param availableGroups List of available SyncPlay groups to join
 * @param isLoading Whether groups are currently being fetched
 * @param onCreateGroup Callback when creating a new group with the given name
 * @param onJoinGroup Callback when joining an existing group by ID
 * @param onRefresh Callback to refresh the available groups list
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun SyncPlayDialog(
    visible: Boolean,
    availableGroups: List<SyncPlayGroup>,
    isLoading: Boolean,
    onCreateGroup: (String) -> Unit,
    onJoinGroup: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val createGroupFocusRequester = remember { FocusRequester() }
    var showCreateForm by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    // Reset state when dialog becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            showCreateForm = false
            newGroupName = ""
            delay(100)
            createGroupFocusRequester.requestFocus()
        }
    }

    TvCenteredPopup(
        visible = visible,
        onDismiss = onDismiss,
        minWidth = 320.dp,
        maxWidth = 450.dp,
        maxHeight = 500.dp,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = TvColors.BluePrimary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "SyncPlay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                showCreateForm -> {
                    CreateGroupForm(
                        name = newGroupName,
                        onNameChange = { newGroupName = it },
                        onCancel = { showCreateForm = false },
                        onCreate = {
                            if (newGroupName.isNotBlank()) {
                                onCreateGroup(newGroupName)
                                onDismiss()
                            }
                        },
                    )
                }
                isLoading -> {
                    LoadingState()
                }
                else -> {
                    GroupSelectionContent(
                        availableGroups = availableGroups,
                        createGroupFocusRequester = createGroupFocusRequester,
                        onShowCreateForm = { showCreateForm = true },
                        onJoinGroup = { groupId ->
                            onJoinGroup(groupId)
                            onDismiss()
                        },
                        onRefresh = onRefresh,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupSelectionContent(
    availableGroups: List<SyncPlayGroup>,
    createGroupFocusRequester: FocusRequester,
    onShowCreateForm: () -> Unit,
    onJoinGroup: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Create New Group button
    Surface(
        onClick = onShowCreateForm,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(createGroupFocusRequester)
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = TvColors.BluePrimary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Create New Group",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TvColors.BluePrimary,
            )
        }
    }

    // Available Groups section
    if (availableGroups.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Available Groups",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 8.dp),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(200.dp),
        ) {
            items(availableGroups, key = { it.groupId }) { group ->
                GroupListItem(
                    group = group,
                    onClick = { onJoinGroup(group.groupId) },
                )
            }
        }
    } else {
        // Empty state
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No groups available",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Action buttons
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ActionButton(
            text = "Refresh",
            icon = Icons.Default.Refresh,
            onClick = onRefresh,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text = "Cancel",
            icon = Icons.Default.Close,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GroupListItem(
    group: SyncPlayGroup,
    onClick: () -> Unit,
) {
    val memberCount = group.participants.size
    val memberText = if (memberCount == 1) "1 member" else "$memberCount members"

    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = {
            Text(
                text = group.groupName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        },
        supportingContent = {
            Text(
                text = memberText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
        ),
        shape = ListItemDefaults.shape(
            shape = RoundedCornerShape(6.dp),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            },
    )
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
        ),
        modifier = modifier.focusProperties {
            left = FocusRequester.Cancel
            right = FocusRequester.Cancel
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = TvColors.BluePrimary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Loading groups...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun CreateGroupForm(
    name: String,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Group Name",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.6f),
        )

        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
            ),
            cursorBrush = SolidColor(TvColors.BluePrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCreate() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(12.dp)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box {
                    if (name.isEmpty()) {
                        Text(
                            text = "Enter group name...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ListItem(
                selected = false,
                onClick = onCancel,
                headlineContent = {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                ),
                shape = ListItemDefaults.shape(shape = RoundedCornerShape(6.dp)),
                modifier = Modifier.weight(1f),
            )
            ListItem(
                selected = false,
                onClick = onCreate,
                enabled = name.isNotBlank(),
                headlineContent = {
                    Text(
                        text = "Create",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (name.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = TvColors.BluePrimary.copy(alpha = 0.6f),
                    focusedContainerColor = TvColors.BluePrimary,
                ),
                shape = ListItemDefaults.shape(shape = RoundedCornerShape(6.dp)),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
