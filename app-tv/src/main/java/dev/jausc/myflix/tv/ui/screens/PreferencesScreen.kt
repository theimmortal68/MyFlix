@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Checkbox
import androidx.tv.material3.CheckboxDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.AppType
import dev.jausc.myflix.core.network.UpdateManager
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.UpdateInfo
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.data.SavedServer
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.DeviceHdrCapabilities
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.tv.BuildConfig
import dev.jausc.myflix.tv.TvPreferences
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.TvTextButton
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Preferences/Settings screen with toggle options.
 * Uses unified TopNavigationBar for consistent navigation across all screens.
 */
@Composable
fun PreferencesScreen(
    preferences: TvPreferences,
    jellyfinClient: JellyfinClient,
    appState: AppState,
    onNavigateHome: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateShows: () -> Unit,
    onNavigateDiscover: () -> Unit = {},
    onNavigateCollections: () -> Unit = {},
    onNavigateUniverses: () -> Unit = {},
    onNavigateLibrary: (libraryId: String, libraryName: String, collectionType: String?) -> Unit =
        { _, _, _ -> },
    onAddServer: () -> Unit = {},
    showUniversesInNav: Boolean = false,
) {
    var selectedNavItem by remember { mutableStateOf(NavItem.SETTINGS) }

    // Server state
    val servers by appState.servers.collectAsState()
    val activeServer by appState.activeServer.collectAsState()
    var showServerDialog by remember { mutableStateOf(false) }

    val hideWatched by preferences.hideWatchedFromRecent.collectAsState()
    val useMpvPlayer by preferences.useMpvPlayer.collectAsState()
    val useTrailerFallback by preferences.useTrailerFallback.collectAsState()
    val preferredAudioLanguage by preferences.preferredAudioLanguage.collectAsState()
    val preferredSubtitleLanguage by preferences.preferredSubtitleLanguage.collectAsState()
    val maxStreamingBitrate by preferences.maxStreamingBitrate.collectAsState()
    val showSeasonPremieres by preferences.showSeasonPremieres.collectAsState()
    val showGenreRows by preferences.showGenreRows.collectAsState()
    val enabledGenres by preferences.enabledGenres.collectAsState()
    val showCollections by preferences.showCollections.collectAsState()
    val pinnedCollections by preferences.pinnedCollections.collectAsState()
    val universesEnabled by preferences.universesEnabled.collectAsState()
    val showSuggestions by preferences.showSuggestions.collectAsState()
    val showSeerrRecentRequests by preferences.showSeerrRecentRequests.collectAsState()

    // Available genres and collections for selection dialogs
    var availableGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableCollections by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var showGenreDialog by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showAudioLanguageDialog by remember { mutableStateOf(false) }
    var showSubtitleLanguageDialog by remember { mutableStateOf(false) }
    var showBitrateDialog by remember { mutableStateOf(false) }

    // Focus requesters for navigation
    val contentFocusRequester = remember { FocusRequester() }

    // Load available genres, collections, and libraries
    LaunchedEffect(Unit) {
        jellyfinClient.getGenres().onSuccess { genres ->
            availableGenres = genres.map { it.name }
        }
        jellyfinClient.getCollections(limit = 50).onSuccess { collections ->
            availableCollections = collections
        }
        jellyfinClient.getLibraries().onSuccess { libs ->
            libraries = libs
        }
    }

    // Request initial focus on content
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            contentFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    val handleNavSelection: (NavItem) -> Unit = { item ->
        selectedNavItem = item
        when (item) {
            NavItem.HOME -> { onNavigateHome() }
            NavItem.SEARCH -> { onNavigateSearch() }
            NavItem.MOVIES -> {
                LibraryFinder.findMoviesLibrary(libraries)?.let {
                    onNavigateLibrary(it.id, it.name, it.collectionType)
                } ?: onNavigateMovies()
            }
            NavItem.SHOWS -> {
                LibraryFinder.findShowsLibrary(libraries)?.let {
                    onNavigateLibrary(it.id, it.name, it.collectionType)
                } ?: onNavigateShows()
            }
            NavItem.DISCOVER -> { onNavigateDiscover() }
            NavItem.COLLECTIONS -> { onNavigateCollections() }
            NavItem.UNIVERSES -> { onNavigateUniverses() }
            NavItem.SETTINGS -> { /* Already here */ }
        }
    }

    // Use Box to layer TopNavigationBar on top of content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        // Main Content Area (with top padding for nav bar)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp) // Space for TopNavigationBar
                .padding(horizontal = 48.dp, vertical = 24.dp),
        ) {
            PreferencesContent(
                // Server settings
                activeServer = activeServer,
                servers = servers,
                onManageServers = { showServerDialog = true },
                onAddServer = onAddServer,
                // Home screen settings
                hideWatched = hideWatched,
                onHideWatchedChanged = { preferences.setHideWatchedFromRecent(it) },
                useMpvPlayer = useMpvPlayer,
                onUseMpvPlayerChanged = { preferences.setUseMpvPlayer(it) },
                useTrailerFallback = useTrailerFallback,
                onUseTrailerFallbackChanged = { preferences.setUseTrailerFallback(it) },
                preferredAudioLanguage = preferredAudioLanguage,
                onEditAudioLanguage = { showAudioLanguageDialog = true },
                preferredSubtitleLanguage = preferredSubtitleLanguage,
                onEditSubtitleLanguage = { showSubtitleLanguageDialog = true },
                maxStreamingBitrate = maxStreamingBitrate,
                onEditMaxBitrate = { showBitrateDialog = true },
                showSeasonPremieres = showSeasonPremieres,
                onShowSeasonPremieresChanged = { preferences.setShowSeasonPremieres(it) },
                showGenreRows = showGenreRows,
                onShowGenreRowsChanged = { preferences.setShowGenreRows(it) },
                enabledGenres = enabledGenres,
                onEditGenres = { showGenreDialog = true },
                showCollections = showCollections,
                onShowCollectionsChanged = { preferences.setShowCollections(it) },
                pinnedCollections = pinnedCollections,
                availableCollections = availableCollections,
                onEditCollections = { showCollectionDialog = true },
                universesEnabled = universesEnabled,
                onUniversesEnabledChanged = { preferences.setUniversesEnabled(it) },
                showSuggestions = showSuggestions,
                onShowSuggestionsChanged = { preferences.setShowSuggestions(it) },
                showSeerrRecentRequests = showSeerrRecentRequests,
                onShowSeerrRecentRequestsChanged = { preferences.setShowSeerrRecentRequests(it) },
                contentFocusRequester = contentFocusRequester,
            )
        }

        // Top Navigation Bar (always visible)
        TopNavigationBarPopup(
            selectedItem = selectedNavItem,
            onItemSelected = handleNavSelection,
            showUniverses = showUniversesInNav,
            contentFocusRequester = contentFocusRequester,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    // Genre Selection Dialog
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
        )
    }

    // Collection Selection Dialog
    if (showCollectionDialog) {
        SelectionDialog(
            title = "Pin Collections",
            availableItems = availableCollections.map { it.id },
            availableItemLabels = availableCollections.associate { it.id to it.name },
            selectedItems = pinnedCollections,
            onDismiss = { showCollectionDialog = false },
            onConfirm = { newSelection ->
                preferences.setPinnedCollections(newSelection)
                showCollectionDialog = false
            },
        )
    }

    // Server Management Dialog
    if (showServerDialog) {
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

    // Audio Language Selection Dialog
    if (showAudioLanguageDialog) {
        LanguageSelectionDialog(
            title = "Preferred Audio Language",
            currentSelection = preferredAudioLanguage,
            onDismiss = { showAudioLanguageDialog = false },
            onSelect = { language ->
                preferences.setPreferredAudioLanguage(language)
                showAudioLanguageDialog = false
            },
        )
    }

    // Subtitle Language Selection Dialog
    if (showSubtitleLanguageDialog) {
        LanguageSelectionDialog(
            title = "Preferred Subtitle Language",
            currentSelection = preferredSubtitleLanguage,
            onDismiss = { showSubtitleLanguageDialog = false },
            onSelect = { language ->
                preferences.setPreferredSubtitleLanguage(language)
                showSubtitleLanguageDialog = false
            },
        )
    }

    // Bitrate Selection Dialog
    if (showBitrateDialog) {
        BitrateSelectionDialog(
            currentSelection = maxStreamingBitrate,
            onDismiss = { showBitrateDialog = false },
            onSelect = { bitrate ->
                preferences.setMaxStreamingBitrate(bitrate)
                showBitrateDialog = false
            },
        )
    }
}

@Composable
private fun PreferencesContent(
    // Server settings
    activeServer: SavedServer?,
    servers: List<SavedServer>,
    onManageServers: () -> Unit,
    onAddServer: () -> Unit,
    // Playback settings
    hideWatched: Boolean,
    onHideWatchedChanged: (Boolean) -> Unit,
    useMpvPlayer: Boolean,
    onUseMpvPlayerChanged: (Boolean) -> Unit,
    useTrailerFallback: Boolean,
    onUseTrailerFallbackChanged: (Boolean) -> Unit,
    preferredAudioLanguage: String?,
    onEditAudioLanguage: () -> Unit,
    preferredSubtitleLanguage: String?,
    onEditSubtitleLanguage: () -> Unit,
    maxStreamingBitrate: Int,
    onEditMaxBitrate: () -> Unit,
    // Home screen settings
    showSeasonPremieres: Boolean,
    onShowSeasonPremieresChanged: (Boolean) -> Unit,
    showGenreRows: Boolean,
    onShowGenreRowsChanged: (Boolean) -> Unit,
    enabledGenres: List<String>,
    onEditGenres: () -> Unit,
    showCollections: Boolean,
    onShowCollectionsChanged: (Boolean) -> Unit,
    pinnedCollections: List<String>,
    availableCollections: List<JellyfinItem>,
    onEditCollections: () -> Unit,
    universesEnabled: Boolean,
    onUniversesEnabledChanged: (Boolean) -> Unit,
    showSuggestions: Boolean,
    onShowSuggestionsChanged: (Boolean) -> Unit,
    showSeerrRecentRequests: Boolean,
    onShowSeerrRecentRequestsChanged: (Boolean) -> Unit,
    contentFocusRequester: FocusRequester,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(contentFocusRequester),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Pink accent bar (matches Settings nav item color)
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(
                                Color(0xFFF472B6),
                                shape = RoundedCornerShape(2.dp),
                            ),
                    )

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TextPrimary,
                    )
                }
            }
        }

        // Servers Section
        item {
            PreferencesSection(title = "Servers") {
                Column {
                    // Current server display
                    if (activeServer != null) {
                        ServerInfoItem(
                            server = activeServer,
                            isActive = true,
                            showManageButton = servers.size > 1,
                            onManageClick = onManageServers,
                        )
                    }

                    // Add Server button
                    PreferenceDivider()
                    ActionPreferenceItem(
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
            PreferencesSection(title = "Home Screen") {
                Column {
                    TogglePreferenceItem(
                        title = "Upcoming Episodes",
                        description = "Show upcoming episodes on home screen",
                        icon = Icons.Outlined.CalendarMonth,
                        iconTint = if (showSeasonPremieres) Color(0xFF60A5FA) else TvColors.TextSecondary,
                        checked = showSeasonPremieres,
                        onCheckedChange = onShowSeasonPremieresChanged,
                    )
                    PreferenceDivider()
                    ToggleWithEditItem(
                        title = "Genre Rows",
                        description = if (enabledGenres.isEmpty()) "No genres selected" else "${enabledGenres.size} genres selected",
                        icon = Icons.Outlined.Category,
                        iconTint = if (showGenreRows) Color(0xFFFBBF24) else TvColors.TextSecondary,
                        checked = showGenreRows,
                        onCheckedChange = onShowGenreRowsChanged,
                        showEditButton = showGenreRows,
                        onEditClick = onEditGenres,
                    )
                    PreferenceDivider()
                    ToggleWithEditItem(
                        title = "Pinned Collections",
                        description = if (pinnedCollections.isEmpty()) "No collections pinned" else "${pinnedCollections.size} collections pinned",
                        icon = Icons.Outlined.Collections,
                        iconTint = if (showCollections) Color(0xFF34D399) else TvColors.TextSecondary,
                        checked = showCollections,
                        onCheckedChange = onShowCollectionsChanged,
                        showEditButton = showCollections && availableCollections.isNotEmpty(),
                        onEditClick = onEditCollections,
                    )
                    PreferenceDivider()
                    TogglePreferenceItem(
                        title = "Universes",
                        description = "Show universe collections in nav bar (franchises)",
                        icon = Icons.Outlined.Public,
                        iconTint = if (universesEnabled) Color(0xFF8B5CF6) else TvColors.TextSecondary,
                        checked = universesEnabled,
                        onCheckedChange = onUniversesEnabledChanged,
                    )
                    PreferenceDivider()
                    TogglePreferenceItem(
                        title = "Suggestions",
                        description = "Show personalized recommendations",
                        icon = Icons.Outlined.Lightbulb,
                        iconTint = if (showSuggestions) Color(0xFFF472B6) else TvColors.TextSecondary,
                        checked = showSuggestions,
                        onCheckedChange = onShowSuggestionsChanged,
                    )
                    PreferenceDivider()
                    TogglePreferenceItem(
                        title = "Recent Requests",
                        description = "Show recent Seerr requests on home page (requires Seerr)",
                        icon = Icons.Outlined.Schedule,
                        iconTint = if (showSeerrRecentRequests) Color(0xFF22C55E) else TvColors.TextSecondary,
                        checked = showSeerrRecentRequests,
                        onCheckedChange = onShowSeerrRecentRequestsChanged,
                    )
                }
            }
        }

        // Display Section
        item {
            PreferencesSection(title = "Display") {
                TogglePreferenceItem(
                    title = "Hide Watched from Recently Added",
                    description = "Don't show already watched items in Recently Added Movies, Shows, and Episodes rows",
                    icon = if (hideWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    iconTint = if (hideWatched) Color(0xFF34D399) else TvColors.TextSecondary,
                    checked = hideWatched,
                    onCheckedChange = onHideWatchedChanged,
                )
            }
        }

        // Playback Section
        item {
            PreferencesSection(title = "Playback") {
                TogglePreferenceItem(
                    title = "Use MPV Player",
                    description = "Experimental. Enable MPV for better codec support. Falls back to ExoPlayer for Dolby Vision content.",
                    icon = Icons.Outlined.PlayCircle,
                    iconTint = if (useMpvPlayer) Color(0xFF9C27B0) else TvColors.TextSecondary,
                    checked = useMpvPlayer,
                    onCheckedChange = onUseMpvPlayerChanged,
                )
                PreferenceDivider()
                TogglePreferenceItem(
                    title = "Use WebView Trailer Fallback",
                    description = "Use the WebView fallback player for Seerr trailers.",
                    icon = Icons.Outlined.OndemandVideo,
                    iconTint = if (useTrailerFallback) Color(0xFF38BDF8) else TvColors.TextSecondary,
                    checked = useTrailerFallback,
                    onCheckedChange = onUseTrailerFallbackChanged,
                )
                PreferenceDivider()
                ActionPreferenceItem(
                    title = "Preferred Audio Language",
                    description = getLanguageDisplayName(preferredAudioLanguage),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconTint = if (preferredAudioLanguage != null) Color(0xFF34D399) else TvColors.TextSecondary,
                    onClick = onEditAudioLanguage,
                )
                PreferenceDivider()
                ActionPreferenceItem(
                    title = "Preferred Subtitle Language",
                    description = getLanguageDisplayName(preferredSubtitleLanguage),
                    icon = Icons.Outlined.Translate,
                    iconTint = if (preferredSubtitleLanguage != null) Color(0xFFFBBF24) else TvColors.TextSecondary,
                    onClick = onEditSubtitleLanguage,
                )
                PreferenceDivider()
                ActionPreferenceItem(
                    title = "Max Streaming Quality",
                    description = getBitrateDisplayName(maxStreamingBitrate),
                    icon = Icons.Outlined.Speed,
                    iconTint = if (maxStreamingBitrate > 0) Color(0xFFEC4899) else TvColors.TextSecondary,
                    onClick = onEditMaxBitrate,
                )
            }
        }

        // Device Section
        item {
            val context = LocalContext.current
            val capabilities = remember { PlayerController.getDeviceHdrCapabilities(context) }

            PreferencesSection(title = "Device") {
                DeviceCapabilityItem(capabilities = capabilities)
            }
        }

        // About Section
        item {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
            val updateManager = remember { UpdateManager(context) }

            DisposableEffect(Unit) {
                onDispose { updateManager.close() }
            }

            PreferencesSection(title = "About") {
                // Version info
                InfoPreferenceItem(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    value = BuildConfig.VERSION_NAME,
                )

                PreferenceDivider()

                // Check for updates
                UpdatePreferenceItem(
                    state = updateState,
                    onCheck = {
                        scope.launch {
                            updateState = UpdateState.Checking
                            updateManager.checkForUpdate(BuildConfig.VERSION_NAME, AppType.TV)
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
                        val url = info.downloadUrl ?: return@UpdatePreferenceItem
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

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "MyFlix for Android TV",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
            )
        }
    }
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
private fun PreferenceDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(TvColors.SurfaceLight.copy(alpha = 0.3f)),
    )
}

@Composable
private fun PreferencesSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TvColors.TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TvColors.Surface),
            content = content,
        )
    }
}

@Composable
private fun TogglePreferenceItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) TvColors.FocusedSurface else Color.Transparent,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onCheckedChange(!checked)
                    true
                } else {
                    false
                }
            }
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
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) TvColors.TextPrimary else TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
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
                uncheckedThumbColor = TvColors.TextSecondary,
                uncheckedTrackColor = TvColors.TextSecondary.copy(alpha = 0.3f),
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
    var isToggleFocused by remember { mutableStateOf(false) }
    var isEditFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Main toggle area (icon + text + switch)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isToggleFocused) TvColors.FocusedSurface else Color.Transparent,
                )
                .onFocusChanged { isToggleFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.DirectionCenter)
                    ) {
                        onCheckedChange(!checked)
                        true
                    } else {
                        false
                    }
                }
                .padding(8.dp),
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
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isToggleFocused) TvColors.TextPrimary else TvColors.TextPrimary.copy(alpha = 0.9f),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Toggle switch
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF34D399),
                    checkedTrackColor = Color(0xFF34D399).copy(alpha = 0.5f),
                    uncheckedThumbColor = TvColors.TextSecondary,
                    uncheckedTrackColor = TvColors.TextSecondary.copy(alpha = 0.3f),
                ),
            )
        }

        // Edit button - separate focusable item
        if (showEditButton) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isEditFocused) TvColors.FocusedSurface else TvColors.SurfaceLight.copy(alpha = 0.5f),
                    )
                    .onFocusChanged { isEditFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            (event.key == Key.Enter || event.key == Key.DirectionCenter)
                        ) {
                            onEditClick()
                            true
                        } else {
                            false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tint = if (isEditFocused) TvColors.TextPrimary else TvColors.TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    availableItems: List<String>,
    selectedItems: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    availableItemLabels: Map<String, String> = emptyMap(),
) {
    // Local state for the selection (ordered list)
    var localSelection by remember(selectedItems) { mutableStateOf(selectedItems.toMutableList()) }
    val firstItemFocusRequester = remember { FocusRequester() }

    // Request focus on first item when dialog opens
    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(TvColors.Surface)
                .padding(24.dp),
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Scrollable list of items
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(availableItems.size) { index ->
                    val itemId = availableItems[index]
                    val displayName = availableItemLabels[itemId] ?: itemId
                    val isSelected = localSelection.contains(itemId)
                    val selectionIndex = localSelection.indexOf(itemId)

                    SelectionItem(
                        name = displayName,
                        isSelected = isSelected,
                        selectionIndex = if (isSelected) selectionIndex + 1 else null,
                        canMoveUp = isSelected && selectionIndex > 0,
                        canMoveDown = isSelected && selectionIndex < localSelection.size - 1,
                        onToggle = {
                            localSelection = if (isSelected) {
                                localSelection.toMutableList().apply { remove(itemId) }
                            } else {
                                localSelection.toMutableList().apply { add(itemId) }
                            }
                        },
                        onMoveUp = {
                            if (selectionIndex > 0) {
                                localSelection = localSelection.toMutableList().apply {
                                    removeAt(selectionIndex)
                                    add(selectionIndex - 1, itemId)
                                }
                            }
                        },
                        onMoveDown = {
                            if (selectionIndex < localSelection.size - 1) {
                                localSelection = localSelection.toMutableList().apply {
                                    removeAt(selectionIndex)
                                    add(selectionIndex + 1, itemId)
                                }
                            }
                        },
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvTextButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp),
                )

                TvTextButton(
                    text = "Save",
                    onClick = { onConfirm(localSelection) },
                    containerColor = Color(0xFF34D399),
                )
            }
        }
    }
}

@Composable
private fun SelectionItem(
    name: String,
    isSelected: Boolean,
    selectionIndex: Int?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> TvColors.FocusedSurface
                    isSelected -> TvColors.SurfaceLight.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onToggle()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF34D399),
                uncheckedColor = TvColors.TextSecondary,
            ),
        )

        // Selection order number (if selected)
        if (selectionIndex != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF34D399).copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$selectionIndex",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399),
                )
            }
        }

        // Item name
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) TvColors.TextPrimary else TvColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )

        // Reorder buttons (only when selected)
        if (isSelected) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Move up",
                        tint = if (canMoveUp) TvColors.TextPrimary else TvColors.TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Move down",
                        tint = if (canMoveDown) TvColors.TextPrimary else TvColors.TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp),
                    )
                }
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
                imageVector = Icons.Outlined.Tv,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = TvColors.TextSecondary,
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "HDR Capability",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TvColors.TextPrimary,
                )
                Text(
                    text = "Video formats supported by this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
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
                if (supported) activeColor.copy(alpha = 0.2f) else TvColors.SurfaceLight.copy(alpha = 0.3f),
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (supported) activeColor else TvColors.TextSecondary.copy(alpha = 0.5f),
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
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) TvColors.FocusedSurface else Color.Transparent,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter) &&
                    showManageButton
                ) {
                    onManageClick()
                    true
                } else {
                    false
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Server icon
        Icon(
            imageVector = Icons.Outlined.Dns,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = if (isActive) Color(0xFF34D399) else TvColors.TextSecondary,
        )

        // Server info
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = server.serverName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TvColors.TextPrimary,
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
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Manage button indicator
        if (showManageButton && isFocused) {
            Text(
                text = "Manage",
                style = MaterialTheme.typography.labelMedium,
                color = TvColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ActionPreferenceItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) TvColors.FocusedSurface else Color.Transparent,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
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
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) TvColors.TextPrimary else TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
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
    val firstItemFocusRequester = remember { FocusRequester() }

    // Request focus on first item when dialog opens
    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(TvColors.Surface)
                .padding(24.dp),
        ) {
            // Title
            Text(
                text = "Manage Servers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Server list
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(servers) { server ->
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
                        modifier = if (server == servers.first()) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TvTextButton(
                    text = "Close",
                    onClick = onDismiss,
                )
            }
        }
    }

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
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var isRemoveFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> TvColors.FocusedSurface
                    isActive -> TvColors.SurfaceLight.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Main selectable area
        Row(
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.DirectionCenter)
                    ) {
                        onSelect()
                        true
                    } else {
                        false
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Server icon
            Icon(
                imageVector = Icons.Outlined.Dns,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isActive) Color(0xFF34D399) else TvColors.TextSecondary,
            )

            // Server info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = server.serverName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TvColors.TextPrimary,
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
                    color = TvColors.TextSecondary,
                )
            }
        }

        // Remove button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isRemoveFocused) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.Transparent,
                )
                .onFocusChanged { isRemoveFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.DirectionCenter)
                    ) {
                        onRemove()
                        true
                    } else {
                        false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Remove server",
                tint = if (isRemoveFocused) Color(0xFFEF4444) else TvColors.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
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
    val confirmFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        confirmFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(16.dp))
                .background(TvColors.Surface)
                .padding(24.dp),
        ) {
            Text(
                text = "Remove Server?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Are you sure you want to remove \"${server.serverName}\"? You will need to log in again to access this server.",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvTextButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp),
                )

                TvTextButton(
                    text = "Remove",
                    onClick = onConfirm,
                    containerColor = Color(0xFFEF4444),
                    modifier = Modifier.focusRequester(confirmFocusRequester),
                )
            }
        }
    }
}

@Composable
private fun InfoPreferenceItem(
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
            tint = TvColors.TextSecondary,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TvColors.TextPrimary.copy(alpha = 0.9f),
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextSecondary,
        )
    }
}

@Composable
private fun UpdatePreferenceItem(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (UpdateInfo) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

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
            description = "Press to download and install",
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
            .background(
                if (isFocused && isClickable) TvColors.FocusedSurface else Color.Transparent,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter) &&
                    isClickable
                ) {
                    when (state) {
                        is UpdateState.Available -> onDownload(state.info)
                        else -> onCheck()
                    }
                    true
                } else {
                    false
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

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) TvColors.TextPrimary else TvColors.TextPrimary.copy(alpha = 0.9f),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Progress indicator for downloading
        if (state is UpdateState.Downloading) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TvColors.SurfaceLight),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(state.progress)
                        .background(Color(0xFFFBBF24)),
                )
            }
        }
    }
}

private data class UpdateItemState(
    val title: String,
    val description: String,
    val iconTint: Color,
    val isClickable: Boolean,
)

// Common language codes (ISO 639-2/B)
private val LANGUAGE_OPTIONS = listOf(
    null to "Server Default",
    "eng" to "English",
    "spa" to "Spanish",
    "fre" to "French",
    "ger" to "German",
    "ita" to "Italian",
    "por" to "Portuguese",
    "rus" to "Russian",
    "jpn" to "Japanese",
    "kor" to "Korean",
    "chi" to "Chinese",
    "ara" to "Arabic",
    "hin" to "Hindi",
    "dut" to "Dutch",
    "pol" to "Polish",
    "tur" to "Turkish",
    "swe" to "Swedish",
    "nor" to "Norwegian",
    "dan" to "Danish",
    "fin" to "Finnish",
)

/**
 * Get display name for a language code.
 */
private fun getLanguageDisplayName(code: String?): String {
    return LANGUAGE_OPTIONS.find { it.first == code }?.second ?: code ?: "Server Default"
}

@Composable
private fun LanguageSelectionDialog(
    title: String,
    currentSelection: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(TvColors.Surface)
                .padding(24.dp),
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Language list
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(LANGUAGE_OPTIONS.size) { index ->
                    val (code, name) = LANGUAGE_OPTIONS[index]
                    val isSelected = code == currentSelection

                    LanguageItem(
                        name = name,
                        isSelected = isSelected,
                        onClick = { onSelect(code) },
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TvTextButton(
                    text = "Cancel",
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> TvColors.FocusedSurface
                    isSelected -> Color(0xFF34D399).copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Checkmark for selected item
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFF34D399), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        } else {
            Box(modifier = Modifier.size(20.dp))
        }

        // Language name
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color(0xFF34D399) else TvColors.TextPrimary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// Bitrate options (value in Mbps, 0 = unlimited)
private val BITRATE_OPTIONS = listOf(
    0 to "Unlimited (Direct Play)",
    120 to "120 Mbps (4K Max)",
    80 to "80 Mbps (4K)",
    60 to "60 Mbps (4K)",
    40 to "40 Mbps (1080p Max)",
    25 to "25 Mbps (1080p)",
    15 to "15 Mbps (1080p)",
    10 to "10 Mbps (720p Max)",
    8 to "8 Mbps (720p)",
    5 to "5 Mbps (480p)",
    3 to "3 Mbps (480p)",
    1 to "1 Mbps (Low)",
)

/**
 * Get display name for a bitrate value.
 */
private fun getBitrateDisplayName(bitrateMbps: Int): String {
    return BITRATE_OPTIONS.find { it.first == bitrateMbps }?.second
        ?: if (bitrateMbps == 0) "Unlimited" else "$bitrateMbps Mbps"
}

@Composable
private fun BitrateSelectionDialog(
    currentSelection: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(TvColors.Surface)
                .padding(24.dp),
        ) {
            // Title
            Text(
                text = "Max Streaming Quality",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Subtitle
            Text(
                text = "Lower values force transcoding for larger files",
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Bitrate list
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(BITRATE_OPTIONS.size) { index ->
                    val (bitrate, label) = BITRATE_OPTIONS[index]
                    val isSelected = bitrate == currentSelection

                    BitrateItem(
                        label = label,
                        isSelected = isSelected,
                        onClick = { onSelect(bitrate) },
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TvTextButton(
                    text = "Cancel",
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun BitrateItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> TvColors.FocusedSurface
                    isSelected -> Color(0xFFEC4899).copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Checkmark for selected item
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFEC4899), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        } else {
            Box(modifier = Modifier.size(20.dp))
        }

        // Bitrate label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color(0xFFEC4899) else TvColors.TextPrimary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
