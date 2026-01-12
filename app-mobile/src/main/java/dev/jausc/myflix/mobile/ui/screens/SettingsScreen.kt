@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.MobilePreferences

/**
 * Settings screen for mobile app.
 * Provides toggle options for display and playback preferences.
 */
@Composable
fun SettingsScreen(preferences: MobilePreferences, jellyfinClient: JellyfinClient? = null, onBack: () -> Unit,) {
    val hideWatched by preferences.hideWatchedFromRecent.collectAsState()
    val useMpvPlayer by preferences.useMpvPlayer.collectAsState()
    val showSeasonPremieres by preferences.showSeasonPremieres.collectAsState()
    val showGenreRows by preferences.showGenreRows.collectAsState()
    val enabledGenres by preferences.enabledGenres.collectAsState()
    val showCollections by preferences.showCollections.collectAsState()
    val pinnedCollections by preferences.pinnedCollections.collectAsState()
    val showSuggestions by preferences.showSuggestions.collectAsState()
    val showSeerrRecentRequests by preferences.showSeerrRecentRequests.collectAsState()

    // Available genres and collections from server
    var availableGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableCollections by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }

    // Dialog state
    var showGenreDialog by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }

    // Load available options when needed
    LaunchedEffect(showGenreRows, showCollections) {
        if (jellyfinClient != null) {
            if (availableGenres.isEmpty()) {
                jellyfinClient.getGenres().onSuccess { genres ->
                    availableGenres = genres.map { it.name }.sorted()
                }
            }
            if (availableCollections.isEmpty()) {
                jellyfinClient.getCollections(limit = 50).onSuccess { collections ->
                    availableCollections = collections.sortedBy { it.name }
                }
            }
        }
    }

    // Genre selection dialog
    if (showGenreDialog) {
        SelectionDialog(
            title = "Select Genres",
            availableItems = availableGenres,
            selectedItems = enabledGenres,
            onDismiss = { showGenreDialog = false },
            onConfirm = { newSelection ->
                preferences.setEnabledGenres(newSelection)
                showGenreDialog = false
            },
            accentColor = Color(0xFFFBBF24),
        )
    }

    // Collection selection dialog
    if (showCollectionDialog) {
        SelectionDialog(
            title = "Select Collections",
            availableItems = availableCollections.map { it.name },
            selectedItems = pinnedCollections.mapNotNull { id ->
                availableCollections.find { it.id == id }?.name
            },
            onDismiss = { showCollectionDialog = false },
            onConfirm = { newSelection ->
                // Convert names back to IDs
                val newIds = newSelection.mapNotNull { name ->
                    availableCollections.find { it.name == name }?.id
                }
                preferences.setPinnedCollections(newIds)
                showCollectionDialog = false
            },
            accentColor = Color(0xFF34D399),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Top bar with back button
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Home Screen Section
            item {
                SettingsSection(title = "Home Screen") {
                    Column {
                        ToggleSettingItem(
                            title = "Upcoming Episodes",
                            description = "Show upcoming season premieres on home screen",
                            icon = Icons.Outlined.CalendarMonth,
                            iconTint = if (showSeasonPremieres) {
                                Color(
                                    0xFF60A5FA,
                                )
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            checked = showSeasonPremieres,
                            onCheckedChange = { preferences.setShowSeasonPremieres(it) },
                        )
                        SettingsDivider()
                        ToggleWithEditItem(
                            title = "Genre Rows",
                            description = if (enabledGenres.isEmpty()) "No genres selected" else "${enabledGenres.size} genres selected",
                            icon = Icons.Outlined.Category,
                            iconTint = if (showGenreRows) {
                                Color(
                                    0xFFFBBF24,
                                )
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            checked = showGenreRows,
                            onCheckedChange = { preferences.setShowGenreRows(it) },
                            showEditButton = showGenreRows && availableGenres.isNotEmpty(),
                            onEditClick = { showGenreDialog = true },
                        )
                        SettingsDivider()
                        ToggleWithEditItem(
                            title = "Collections",
                            description = if (pinnedCollections.isEmpty()) "No collections pinned" else "${pinnedCollections.size} collections pinned",
                            icon = Icons.Outlined.Collections,
                            iconTint = if (showCollections) {
                                Color(
                                    0xFF34D399,
                                )
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            checked = showCollections,
                            onCheckedChange = { preferences.setShowCollections(it) },
                            showEditButton = showCollections && availableCollections.isNotEmpty(),
                            onEditClick = { showCollectionDialog = true },
                        )
                        SettingsDivider()
                        ToggleSettingItem(
                            title = "Suggestions",
                            description = "Show personalized recommendations",
                            icon = Icons.Outlined.Lightbulb,
                            iconTint = if (showSuggestions) {
                                Color(
                                    0xFFF472B6,
                                )
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            checked = showSuggestions,
                            onCheckedChange = { preferences.setShowSuggestions(it) },
                        )
                        SettingsDivider()
                        ToggleSettingItem(
                            title = "Recent Requests",
                            description = "Show recent Seerr requests on home page (requires Seerr)",
                            icon = Icons.Outlined.Schedule,
                            iconTint = if (showSeerrRecentRequests) {
                                Color(0xFF22C55E)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            checked = showSeerrRecentRequests,
                            onCheckedChange = { preferences.setShowSeerrRecentRequests(it) },
                        )
                    }
                }
            }

            // Display Section
            item {
                SettingsSection(title = "Display") {
                    ToggleSettingItem(
                        title = "Hide Watched from Recently Added",
                        description = "Don't show already watched items in Recently Added rows",
                        icon = if (hideWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        iconTint = if (hideWatched) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                        checked = hideWatched,
                        onCheckedChange = { preferences.setHideWatchedFromRecent(it) },
                    )
                }
            }

            // Playback Section
            item {
                SettingsSection(title = "Playback") {
                    ToggleSettingItem(
                        title = "Use MPV Player",
                        description = "Experimental. Enable MPV for better codec support. Falls back to ExoPlayer for Dolby Vision content.",
                        icon = Icons.Outlined.PlayCircle,
                        iconTint = if (useMpvPlayer) Color(0xFF9C27B0) else MaterialTheme.colorScheme.onSurfaceVariant,
                        checked = useMpvPlayer,
                        onCheckedChange = { preferences.setUseMpvPlayer(it) },
                    )
                }
            }

            // App Info
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MyFlix for Android",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit,) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            content()
        }
    }
}

@Composable
private fun ToggleSettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = iconTint,
        )

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Toggle switch
        Switch(
            checked = checked,
            onCheckedChange = null, // Handled by row click
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF34D399),
                checkedTrackColor = Color(0xFF34D399).copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun ToggleWithEditItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showEditButton: Boolean,
    onEditClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = iconTint,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (showEditButton) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF34D399),
                checkedTrackColor = Color(0xFF34D399).copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    availableItems: List<String>,
    selectedItems: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    accentColor: Color,
) {
    // Mutable list for reordering - selected items first, then unselected
    val orderedItems = remember(selectedItems, availableItems) {
        (selectedItems + availableItems.filter { it !in selectedItems }).toMutableStateList()
    }
    val checkedState = remember(selectedItems) {
        orderedItems.map { it in selectedItems }.toMutableStateList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 24.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Check to enable, use arrows to reorder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                ) {
                    itemsIndexed(orderedItems) { index, item ->
                        ReorderableCheckboxItem(
                            text = item,
                            checked = checkedState[index],
                            onCheckedChange = { checked ->
                                checkedState[index] = checked
                            },
                            canMoveUp = index > 0 && checkedState[index],
                            canMoveDown = index < orderedItems.lastIndex && checkedState[index],
                            onMoveUp = {
                                if (index > 0) {
                                    val itemToMove = orderedItems.removeAt(index)
                                    val checkToMove = checkedState.removeAt(index)
                                    orderedItems.add(index - 1, itemToMove)
                                    checkedState.add(index - 1, checkToMove)
                                }
                            },
                            onMoveDown = {
                                if (index < orderedItems.lastIndex) {
                                    val itemToMove = orderedItems.removeAt(index)
                                    val checkToMove = checkedState.removeAt(index)
                                    orderedItems.add(index + 1, itemToMove)
                                    checkedState.add(index + 1, checkToMove)
                                }
                            },
                            accentColor = accentColor,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Return only checked items in their current order
                    val result = orderedItems.filterIndexed { index, _ -> checkedState[index] }
                    onConfirm(result)
                },
            ) {
                Text("Save", color = accentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ReorderableCheckboxItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    accentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                checkmarkColor = Color.White,
            ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        if (checked) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Move up",
                    tint = if (canMoveUp) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Move down",
                    tint = if (canMoveDown) {
                        accentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.3f,
                        )
                    },
                )
            }
        }
    }
}
