@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

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
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.launch

/**
 * Dialog for adding an item to a playlist.
 * Shows existing playlists and option to create a new one.
 */
@Composable
fun AddToPlaylistDialog(
    itemId: String,
    itemName: String,
    jellyfinClient: JellyfinClient,
    onDismiss: () -> Unit,
    onSuccess: (playlistName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    // Load playlists
    LaunchedEffect(Unit) {
        jellyfinClient.getUserPlaylists()
            .onSuccess { playlists = it }
            .onFailure { error = it.message }
        isLoading = false
    }

    TvCenteredPopup(
        visible = true,
        onDismiss = onDismiss,
        minWidth = 350.dp,
        maxWidth = 450.dp,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = null,
                    tint = TvColors.BluePrimary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            // Subtitle showing item name
            Text(
                text = itemName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(4.dp))

            when {
                isLoading -> {
                    Text(
                        text = "Loading playlists...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.Error,
                    )
                }
                showCreateNew -> {
                    // Create new playlist form
                    CreatePlaylistForm(
                        name = newPlaylistName,
                        onNameChange = { newPlaylistName = it },
                        isCreating = isCreating,
                        onCancel = { showCreateNew = false },
                        onCreate = {
                            if (newPlaylistName.isNotBlank()) {
                                isCreating = true
                                scope.launch {
                                    jellyfinClient.createPlaylist(
                                        name = newPlaylistName,
                                        itemIds = listOf(itemId),
                                    ).onSuccess {
                                        onSuccess(newPlaylistName)
                                        onDismiss()
                                    }.onFailure {
                                        error = it.message
                                        isCreating = false
                                    }
                                }
                            }
                        },
                    )
                }
                else -> {
                    // Playlist list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // Create new option
                        item("create_new") {
                            ListItem(
                                selected = false,
                                onClick = { showCreateNew = true },
                                headlineContent = {
                                    Text(
                                        text = "Create New Playlist",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TvColors.BluePrimary,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = TvColors.BluePrimary,
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
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Existing playlists
                        items(playlists, key = { it.id }) { playlist ->
                            ListItem(
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        jellyfinClient.addToPlaylist(playlist.id, listOf(itemId))
                                            .onSuccess {
                                                onSuccess(playlist.name)
                                                onDismiss()
                                            }
                                            .onFailure { error = it.message }
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                    )
                                },
                                supportingContent = playlist.childCount?.let { count ->
                                    {
                                        Text(
                                            text = "$count items",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.5f),
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                ),
                                shape = ListItemDefaults.shape(
                                    shape = RoundedCornerShape(6.dp),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Empty state
                        if (playlists.isEmpty()) {
                            item("empty") {
                                Text(
                                    text = "No playlists yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistForm(
    name: String,
    onNameChange: (String) -> Unit,
    isCreating: Boolean,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Playlist Name",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.6f),
        )

        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            enabled = !isCreating,
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
                            text = "Enter playlist name...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    }
                    innerTextField()
                }
            },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ListItem(
                selected = false,
                onClick = onCancel,
                enabled = !isCreating,
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
                enabled = !isCreating && name.isNotBlank(),
                headlineContent = {
                    Text(
                        text = if (isCreating) "Creating..." else "Create",
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
