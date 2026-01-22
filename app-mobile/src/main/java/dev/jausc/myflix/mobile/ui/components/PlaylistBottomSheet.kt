@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.components

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.launch

/**
 * Bottom sheet for adding an item to a playlist.
 * Shows existing playlists and option to create a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    itemId: String,
    itemName: String,
    jellyfinClient: JellyfinClient,
    onDismiss: () -> Unit,
    onSuccess: (playlistName: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Subtitle showing item name
            Text(
                text = itemName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
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
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        // Create new option
                        item("create_new") {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "Create New Playlist",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TextButton(
                                onClick = { showCreateNew = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "Create New Playlist",
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        // Existing playlists
                        items(playlists, key = { it.id }) { playlist ->
                            TextButton(
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
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    playlist.childCount?.let { count ->
                                        Text(
                                            text = "$count items",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        // Empty state
                        if (playlists.isEmpty()) {
                            item("empty") {
                                Text(
                                    text = "No playlists yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Playlist Name") },
            placeholder = { Text("Enter playlist name...") },
            enabled = !isCreating,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCreate() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCreating,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            FilledTonalButton(
                onClick = onCreate,
                enabled = !isCreating && name.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create")
                }
            }
        }
    }
}
