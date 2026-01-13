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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.*
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
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.TvPreferences
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.TopNavigationBarPopup
import dev.jausc.myflix.tv.ui.components.TvTextButton
import dev.jausc.myflix.tv.ui.components.rememberNavBarPopupState
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Preferences/Settings screen with toggle options.
 * Uses unified TopNavigationBar for consistent navigation across all screens.
 */
@Composable
fun PreferencesScreen(
    preferences: TvPreferences,
    jellyfinClient: JellyfinClient,
    onNavigateHome: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateShows: () -> Unit,
    onNavigateDiscover: () -> Unit = {},
    onNavigateLibrary: (libraryId: String, libraryName: String, collectionType: String?) -> Unit =
        { _, _, _ -> },
) {
    val scope = rememberCoroutineScope()
    var selectedNavItem by remember { mutableStateOf(NavItem.SETTINGS) }

    val hideWatched by preferences.hideWatchedFromRecent.collectAsState()
    val useMpvPlayer by preferences.useMpvPlayer.collectAsState()
    val showSeasonPremieres by preferences.showSeasonPremieres.collectAsState()
    val showGenreRows by preferences.showGenreRows.collectAsState()
    val enabledGenres by preferences.enabledGenres.collectAsState()
    val showCollections by preferences.showCollections.collectAsState()
    val pinnedCollections by preferences.pinnedCollections.collectAsState()
    val showSuggestions by preferences.showSuggestions.collectAsState()
    val showSeerrRecentRequests by preferences.showSeerrRecentRequests.collectAsState()

    // Available genres and collections for selection dialogs
    var availableGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableCollections by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var showGenreDialog by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }

    // Focus requesters for navigation
    val homeButtonFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    // Popup nav bar state - visible on load, auto-hides after 5 seconds
    val navBarState = rememberNavBarPopupState()

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
            NavItem.COLLECTIONS -> { /* TODO: Navigate to collections */ }
            NavItem.UNIVERSES -> { /* TODO: Placeholder for future feature */ }
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
                hideWatched = hideWatched,
                onHideWatchedChanged = { preferences.setHideWatchedFromRecent(it) },
                useMpvPlayer = useMpvPlayer,
                onUseMpvPlayerChanged = { preferences.setUseMpvPlayer(it) },
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
                showSuggestions = showSuggestions,
                onShowSuggestionsChanged = { preferences.setShowSuggestions(it) },
                showSeerrRecentRequests = showSeerrRecentRequests,
                onShowSeerrRecentRequestsChanged = { preferences.setShowSeerrRecentRequests(it) },
                contentFocusRequester = contentFocusRequester,
                onShowNavBar = {
                    navBarState.show()
                    scope.launch {
                        delay(150) // Wait for animation
                        try {
                            homeButtonFocusRequester.requestFocus()
                        } catch (_: Exception) {
                        }
                    }
                },
            )
        }

        // Top Navigation Bar (popup overlay)
        TopNavigationBarPopup(
            visible = navBarState.isVisible,
            selectedItem = selectedNavItem,
            onItemSelected = handleNavSelection,
            onDismiss = {
                navBarState.hide()
                try {
                    contentFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            },
            homeButtonFocusRequester = homeButtonFocusRequester,
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
}

@Composable
private fun PreferencesContent(
    hideWatched: Boolean,
    onHideWatchedChanged: (Boolean) -> Unit,
    useMpvPlayer: Boolean,
    onUseMpvPlayerChanged: (Boolean) -> Unit,
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
    showSuggestions: Boolean,
    onShowSuggestionsChanged: (Boolean) -> Unit,
    showSeerrRecentRequests: Boolean,
    onShowSeerrRecentRequestsChanged: (Boolean) -> Unit,
    contentFocusRequester: FocusRequester,
    onShowNavBar: () -> Unit,
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
                        onUpPressed = onShowNavBar,
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
            }
        }

        // Info section
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "MyFlix for Android TV",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
            )
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextSecondary.copy(alpha = 0.7f),
            )
        }
    }
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
    onUpPressed: (() -> Unit)? = null,
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
            .onPreviewKeyEvent { event ->
                // Intercept UP key to show nav bar (for first preference item)
                if (onUpPressed != null &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionUp
                ) {
                    onUpPressed()
                    true
                } else {
                    false
                }
            }
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
