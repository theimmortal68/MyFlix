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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.preferences.PlaybackOptions
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.data.SavedServer
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.common.model.AppType
import dev.jausc.myflix.core.network.UpdateManager
import dev.jausc.myflix.core.common.model.UpdateInfo
import dev.jausc.myflix.core.player.DeviceHdrCapabilities
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.mobile.BuildConfig
import dev.jausc.myflix.mobile.MobilePreferences

/**
 * Settings screen for mobile app.
 * Provides toggle options for display and playback preferences.
 */
@Composable
fun SettingsScreen(
    preferences: MobilePreferences,
    jellyfinClient: JellyfinClient? = null,
    appState: AppState? = null,
    onBack: () -> Unit,
    onAddServer: () -> Unit = {},
) {
    // Server state
    val servers by appState?.servers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val activeServer by appState?.activeServer?.collectAsState() ?: remember { mutableStateOf(null) }
    var showServerDialog by remember { mutableStateOf(false) }

    val hideWatched by preferences.hideWatchedFromRecent.collectAsState()
    val useMpvPlayer by preferences.useMpvPlayer.collectAsState()
    val useTrailerFallback by preferences.useTrailerFallback.collectAsState()
    val skipIntroMode by preferences.skipIntroMode.collectAsState()
    val skipCreditsMode by preferences.skipCreditsMode.collectAsState()
    val skipForwardSeconds by preferences.skipForwardSeconds.collectAsState()
    val skipBackwardSeconds by preferences.skipBackwardSeconds.collectAsState()
    val refreshRateMode by preferences.refreshRateMode.collectAsState()
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
    var showSkipIntroModeDialog by remember { mutableStateOf(false) }
    var showSkipCreditsModeDialog by remember { mutableStateOf(false) }
    var showSkipForwardDialog by remember { mutableStateOf(false) }
    var showSkipBackwardDialog by remember { mutableStateOf(false) }
    var showRefreshRateModeDialog by remember { mutableStateOf(false) }

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

    // Server management dialog
    if (showServerDialog && appState != null) {
        ServerManagementDialog(
            servers = servers,
            activeServerId = activeServer?.serverId,
            onDismiss = { showServerDialog = false },
            onSwitchServer = { serverId ->
                appState.switchServer(serverId)
                showServerDialog = false
            },
            onRemoveServer = { serverId ->
                appState.removeServer(serverId)
            },
        )
    }

    // Skip intro mode dialog
    if (showSkipIntroModeDialog) {
        SkipModeSelectionDialog(
            title = "Skip Intro",
            currentSelection = skipIntroMode,
            onDismiss = { showSkipIntroModeDialog = false },
            onSelect = { mode ->
                preferences.setSkipIntroMode(mode)
                showSkipIntroModeDialog = false
            },
        )
    }

    // Skip credits mode dialog
    if (showSkipCreditsModeDialog) {
        SkipModeSelectionDialog(
            title = "Skip Credits",
            currentSelection = skipCreditsMode,
            onDismiss = { showSkipCreditsModeDialog = false },
            onSelect = { mode ->
                preferences.setSkipCreditsMode(mode)
                showSkipCreditsModeDialog = false
            },
        )
    }

    // Skip forward duration dialog
    if (showSkipForwardDialog) {
        SkipDurationSelectionDialog(
            title = "Skip Forward",
            currentSelection = skipForwardSeconds,
            onDismiss = { showSkipForwardDialog = false },
            onSelect = { duration ->
                preferences.setSkipForwardSeconds(duration)
                showSkipForwardDialog = false
            },
        )
    }

    // Skip backward duration dialog
    if (showSkipBackwardDialog) {
        SkipDurationSelectionDialog(
            title = "Skip Backward",
            currentSelection = skipBackwardSeconds,
            onDismiss = { showSkipBackwardDialog = false },
            onSelect = { duration ->
                preferences.setSkipBackwardSeconds(duration)
                showSkipBackwardDialog = false
            },
        )
    }

    // Refresh rate mode dialog
    if (showRefreshRateModeDialog) {
        RefreshRateModeSelectionDialog(
            currentSelection = refreshRateMode,
            onDismiss = { showRefreshRateModeDialog = false },
            onSelect = { mode ->
                preferences.setRefreshRateMode(mode)
                showRefreshRateModeDialog = false
            },
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
            // Servers Section
            item {
                SettingsSection(title = "Servers") {
                    Column {
                        // Current server display
                        if (activeServer != null) {
                            ServerInfoItem(
                                server = activeServer!!,
                                isActive = true,
                                showManageButton = servers.size > 1,
                                onManageClick = { showServerDialog = true },
                            )
                        }

                        // Add Server button
                        SettingsDivider()
                        ActionSettingItem(
                            title = "Add Server",
                            description = "Connect to another Jellyfin server",
                            icon = Icons.Outlined.Add,
                            iconTint = Color(0xFF60A5FA),
                            onClick = onAddServer,
                        )
                    }
                }
            }

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
                    SettingsDivider()
                    ToggleSettingItem(
                        title = "Use WebView Trailer Fallback",
                        description = "Use the WebView fallback player for Seerr trailers.",
                        icon = Icons.Outlined.OndemandVideo,
                        iconTint = if (useTrailerFallback) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant,
                        checked = useTrailerFallback,
                        onCheckedChange = { preferences.setUseTrailerFallback(it) },
                    )
                    SettingsDivider()
                    ActionSettingItem(
                        title = "Skip Intro",
                        description = getSkipModeDisplayName(skipIntroMode),
                        icon = Icons.Outlined.Schedule,
                        iconTint = if (skipIntroMode != "OFF") Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { showSkipIntroModeDialog = true },
                    )
                    SettingsDivider()
                    ActionSettingItem(
                        title = "Skip Credits",
                        description = getSkipModeDisplayName(skipCreditsMode),
                        icon = Icons.Outlined.Schedule,
                        iconTint = if (skipCreditsMode != "OFF") Color(0xFFF97316) else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { showSkipCreditsModeDialog = true },
                    )
                    SettingsDivider()
                    ActionSettingItem(
                        title = "Skip Forward",
                        description = getSkipDurationDisplayName(skipForwardSeconds),
                        icon = Icons.Default.FastForward,
                        iconTint = Color(0xFF60A5FA),
                        onClick = { showSkipForwardDialog = true },
                    )
                    SettingsDivider()
                    ActionSettingItem(
                        title = "Skip Backward",
                        description = getSkipDurationDisplayName(skipBackwardSeconds),
                        icon = Icons.Default.FastRewind,
                        iconTint = Color(0xFF60A5FA),
                        onClick = { showSkipBackwardDialog = true },
                    )
                    SettingsDivider()
                    ActionSettingItem(
                        title = "Refresh Rate",
                        description = getRefreshRateModeDisplayName(refreshRateMode),
                        icon = Icons.Outlined.Smartphone,
                        iconTint = if (refreshRateMode != "OFF") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { showRefreshRateModeDialog = true },
                    )
                }
            }

            // Device Section
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val capabilities = remember { PlayerController.getDeviceHdrCapabilities(context) }

                SettingsSection(title = "Device") {
                    DeviceCapabilityItem(capabilities = capabilities)
                }
            }

            // About Section
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()
                var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
                val updateManager = remember { UpdateManager(context) }

                DisposableEffect(Unit) {
                    onDispose { updateManager.close() }
                }

                SettingsSection(title = "About") {
                    Column {
                        // Version info
                        InfoSettingItem(
                            icon = Icons.Outlined.Info,
                            title = "Version",
                            value = BuildConfig.VERSION_NAME,
                        )

                        SettingsDivider()

                        // Check for updates
                        UpdateSettingItem(
                            state = updateState,
                            onCheck = {
                                scope.launch {
                                    updateState = UpdateState.Checking
                                    updateManager.checkForUpdate(BuildConfig.VERSION_NAME, AppType.MOBILE)
                                        .onSuccess { info ->
                                            updateState = if (info.hasUpdate) {
                                                UpdateState.Available(info)
                                            } else {
                                                UpdateState.UpToDate
                                            }
                                        }
                                        .onFailure { e ->
                                            updateState = UpdateState.Error(e.message ?: "Check failed")
                                        }
                                }
                            },
                            onDownload = { info ->
                                val url = info.downloadUrl ?: return@UpdateSettingItem
                                scope.launch {
                                    updateState = UpdateState.Downloading(0f)
                                    updateManager.downloadApk(url) { progress ->
                                        updateState = UpdateState.Downloading(progress)
                                    }.onSuccess { file ->
                                        updateManager.installApk(file)
                                        updateState = UpdateState.Idle
                                    }.onFailure { e ->
                                        updateState = UpdateState.Error(e.message ?: "Download failed")
                                    }
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MyFlix for Android",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

// HDR/DV Badge Colors
private val DolbyVisionOrange = Color(0xFFFF6B00)
private val HdrBlue = Color(0xFF4169E1)

@Composable
private fun DeviceCapabilityItem(capabilities: DeviceHdrCapabilities) {
    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "HDR Capability",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Video formats supported by this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // HDR format chips
        Row(
            modifier = Modifier.padding(top = 12.dp, start = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CapabilityChip("Dolby Vision", capabilities.supportsDolbyVision, DolbyVisionOrange)
            CapabilityChip("HDR10", capabilities.supportsHdr10, HdrBlue)
            CapabilityChip("HDR10+", capabilities.supportsHdr10Plus, HdrBlue)
            CapabilityChip("HLG", capabilities.supportsHlg, HdrBlue)
        }
    }
}

@Composable
private fun CapabilityChip(label: String, supported: Boolean, activeColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (supported) {
                    activeColor.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (supported) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ServerInfoItem(
    server: SavedServer,
    isActive: Boolean,
    showManageButton: Boolean,
    onManageClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showManageButton) { onManageClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Server icon
        Icon(
            imageVector = Icons.Outlined.Dns,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = if (isActive) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Server info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = server.serverName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF34D399).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34D399),
                        )
                    }
                }
            }
            Text(
                text = "${server.userName} • ${server.host}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Chevron indicator for manage
        if (showManageButton) {
            Text(
                text = "Manage",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ActionSettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
    }
}

@Composable
private fun ServerManagementDialog(
    servers: List<SavedServer>,
    activeServerId: String?,
    onDismiss: () -> Unit,
    onSwitchServer: (String) -> Unit,
    onRemoveServer: (String) -> Unit,
) {
    var serverToRemove by remember { mutableStateOf<SavedServer?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage Servers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(servers.size) { index ->
                    val server = servers[index]
                    val isActive = server.serverId == activeServerId
                    ServerListItem(
                        server = server,
                        isActive = isActive,
                        onSelect = {
                            if (!isActive) {
                                onSwitchServer(server.serverId)
                            }
                        },
                        onRemove = { serverToRemove = server },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )

    // Confirm remove dialog
    if (serverToRemove != null) {
        ConfirmRemoveServerDialog(
            server = serverToRemove!!,
            onConfirm = {
                onRemoveServer(serverToRemove!!.serverId)
                serverToRemove = null
            },
            onDismiss = { serverToRemove = null },
        )
    }
}

@Composable
private fun ServerListItem(
    server: SavedServer,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    Color.Transparent
                },
            )
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Server icon
        Icon(
            imageVector = Icons.Outlined.Dns,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isActive) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Server info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = server.serverName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF34D399).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34D399),
                        )
                    }
                }
            }
            Text(
                text = "${server.userName} • ${server.host}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Remove server",
                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ConfirmRemoveServerDialog(
    server: SavedServer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remove Server?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = "Are you sure you want to remove \"${server.serverName}\"? You will need to log in again to access this server.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = Color(0xFFEF4444))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Update state for the update check UI.
 */
private sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Composable
private fun InfoSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpdateSettingItem(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (UpdateInfo) -> Unit,
) {
    val (title, description, iconTint, isClickable) = when (state) {
        is UpdateState.Idle -> UpdateItemState(
            title = "Check for Updates",
            description = "Check GitHub for new releases",
            iconTint = Color(0xFF60A5FA),
            isClickable = true,
        )
        is UpdateState.Checking -> UpdateItemState(
            title = "Checking...",
            description = "Connecting to GitHub",
            iconTint = Color(0xFF60A5FA),
            isClickable = false,
        )
        is UpdateState.Available -> UpdateItemState(
            title = "Update to ${state.info.latestVersion}",
            description = "Tap to download and install",
            iconTint = Color(0xFF34D399),
            isClickable = true,
        )
        is UpdateState.Downloading -> UpdateItemState(
            title = "Downloading... ${(state.progress * 100).toInt()}%",
            description = "Please wait",
            iconTint = Color(0xFFFBBF24),
            isClickable = false,
        )
        is UpdateState.UpToDate -> UpdateItemState(
            title = "Up to Date",
            description = "You have the latest version",
            iconTint = Color(0xFF34D399),
            isClickable = true,
        )
        is UpdateState.Error -> UpdateItemState(
            title = "Check for Updates",
            description = state.message,
            iconTint = Color(0xFFEF4444),
            isClickable = true,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) {
                if (state is UpdateState.Available) {
                    onDownload(state.info)
                } else {
                    onCheck()
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.SystemUpdate,
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

        // Progress indicator for downloading
        if (state is UpdateState.Downloading) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.width(60.dp),
                color = Color(0xFFFBBF24),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

private data class UpdateItemState(
    val title: String,
    val description: String,
    val iconTint: Color,
    val isClickable: Boolean,
)

// Skip mode options (OFF, ASK, AUTO)
private val SKIP_MODE_OPTIONS = listOf(
    "OFF" to "Off",
    "ASK" to "Ask (show button)",
    "AUTO" to "Auto (skip automatically)",
)

/**
 * Get display name for a skip mode value.
 */
private fun getSkipModeDisplayName(mode: String): String =
    SKIP_MODE_OPTIONS.find { it.first == mode }?.second ?: mode

/**
 * Get display name for a refresh rate mode value.
 */
private fun getRefreshRateModeDisplayName(mode: String): String =
    PlaybackOptions.getRefreshRateModeLabel(mode)

@Composable
private fun SkipModeSelectionDialog(
    title: String,
    currentSelection: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
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
                    text = "Choose behavior when segment is detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                SKIP_MODE_OPTIONS.forEach { (mode, label) ->
                    val isSelected = mode == currentSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF22C55E).copy(alpha = 0.15f) else Color.Transparent
                            )
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Radio-style indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF22C55E) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                )
                            }
                        }

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun RefreshRateModeSelectionDialog(
    currentSelection: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
        title = {
            Text(
                text = "Refresh Rate",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Match display refresh rate to video for smoother playback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PlaybackOptions.REFRESH_RATE_MODE_OPTIONS.forEach { (mode, label) ->
                    val isSelected = mode == currentSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent
                            )
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Radio-style indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                )
                            }
                        }

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Available skip duration options in seconds.
 */
private val SKIP_DURATION_OPTIONS = listOf(5, 10, 15, 20, 30, 45, 60)

/**
 * Get display name for skip duration.
 */
private fun getSkipDurationDisplayName(seconds: Int): String {
    return if (seconds >= 60) {
        "${seconds / 60}m"
    } else {
        "${seconds}s"
    }
}

/**
 * Dialog for selecting skip duration.
 */
@Composable
private fun SkipDurationSelectionDialog(
    title: String,
    currentSelection: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                SKIP_DURATION_OPTIONS.forEach { seconds ->
                    val isSelected = seconds == currentSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent
                            )
                            .clickable { onSelect(seconds) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Radio-style indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                )
                            }
                        }

                        Text(
                            text = getSkipDurationDisplayName(seconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
