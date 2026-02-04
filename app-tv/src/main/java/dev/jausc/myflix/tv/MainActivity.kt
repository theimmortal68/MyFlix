@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
    "StringLiteralDuplication",
)

package dev.jausc.myflix.tv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.ui.SplashScreen
import dev.jausc.myflix.core.common.ui.SplashScreenTvConfig
import dev.jausc.myflix.core.common.util.NavigationHelper
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.data.DebugCredentials
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.viewmodel.DetailViewModel
import dev.jausc.myflix.core.viewmodel.SeerrHomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jausc.myflix.core.network.syncplay.SyncPlayManager
import dev.jausc.myflix.core.network.syncplay.TimeSyncManager
import dev.jausc.myflix.core.network.websocket.GeneralCommandType
import dev.jausc.myflix.core.network.websocket.PlayCommandType
import dev.jausc.myflix.core.network.websocket.WebSocketEvent
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.components.navrail.FocusSentinel
import dev.jausc.myflix.tv.ui.components.navrail.NavRailAnimations
import dev.jausc.myflix.tv.ui.components.navrail.NavRailDimensions
import dev.jausc.myflix.tv.ui.components.navrail.NavigationRail
import dev.jausc.myflix.tv.ui.screens.CollectionDetailScreen
import dev.jausc.myflix.tv.ui.screens.CollectionsLibraryScreen
import dev.jausc.myflix.tv.ui.screens.DetailScreen
import dev.jausc.myflix.tv.ui.screens.EpisodesScreen
import dev.jausc.myflix.tv.ui.screens.HomeScreen
import dev.jausc.myflix.tv.ui.screens.LibraryScreen
import dev.jausc.myflix.tv.ui.screens.LoginScreen
import dev.jausc.myflix.tv.ui.screens.PersonDetailScreen
import dev.jausc.myflix.tv.ui.screens.PlayerScreen
import dev.jausc.myflix.tv.ui.screens.PreferencesScreen
import dev.jausc.myflix.tv.ui.screens.SearchScreen
import dev.jausc.myflix.tv.ui.screens.SeerrActorDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrCollectionDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverByGenreScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverByNetworkScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverByStudioScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverMoviesScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverTrendingScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverTvScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverUpcomingMoviesScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverUpcomingTvScreen
import dev.jausc.myflix.tv.ui.screens.SeerrHomeScreen
import dev.jausc.myflix.tv.ui.screens.SeerrRequestsScreen
import dev.jausc.myflix.tv.ui.screens.SeerrSearchScreen
import dev.jausc.myflix.tv.ui.screens.SeerrSetupScreen
import dev.jausc.myflix.tv.ui.screens.TrailerPlayerScreen
import dev.jausc.myflix.tv.ui.screens.UniverseCollectionsScreen
import dev.jausc.myflix.tv.channels.ChannelSyncWorker
import dev.jausc.myflix.tv.channels.TvChannelManager
import dev.jausc.myflix.tv.channels.WatchNextManager
import dev.jausc.myflix.tv.ui.theme.MyFlixTvTheme
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.LocalExitFocusState
import dev.jausc.myflix.core.common.ui.theme.ThemePreset

/**
 * Deep link types for deferred navigation from TV home screen.
 */
private sealed class DeepLink {
    data class Play(val itemId: String, val startPositionMs: Long?) : DeepLink()
    data object Home : DeepLink()
}

class MainActivity : ComponentActivity() {
    /**
     * Pending deep link to process after login is confirmed.
     * Deep links from TV home screen (Watch Next, custom channels) are stored here
     * and processed once the user is logged in and the home screen is ready.
     */
    private var pendingDeepLink: DeepLink? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Collect theme preference at top level for proper theming
            val context = LocalContext.current
            val tvPreferences = remember { TvPreferences.getInstance(context) }
            val themePresetName by tvPreferences.themePreset.collectAsState()
            val themePreset = remember(themePresetName) {
                ThemePreset.fromName(themePresetName)
            }

            MyFlixTvTheme(themePreset = themePreset) {
                MyFlixTvApp(
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null },
                )
            }
        }
        // Handle deep link from launch intent
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Handle deep links from Android TV home screen.
     * Format: myflix://play/{itemId}?startPositionMs={position}
     *         myflix://home
     */
    private fun handleDeepLink(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        if (uri.scheme != "myflix") return false

        Log.d("MainActivity", "Handling deep link: $uri")

        return when (uri.host) {
            "play" -> {
                val itemId = uri.pathSegments.firstOrNull()
                if (itemId != null) {
                    val startPositionMs = uri.getQueryParameter("startPositionMs")?.toLongOrNull()
                    pendingDeepLink = DeepLink.Play(itemId, startPositionMs)
                    true
                } else {
                    false
                }
            }
            "home" -> {
                pendingDeepLink = DeepLink.Home
                true
            }
            else -> false
        }
    }
}

@Composable
private fun MyFlixTvApp(
    pendingDeepLink: DeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val context = LocalContext.current

    val jellyfinClient = remember { JellyfinClient(context) }
    val appState = remember { AppState(context, jellyfinClient) }
    val tvPreferences = remember { TvPreferences.getInstance(context) }
    val seerrClient = remember { SeerrClient() }

    // SyncPlay infrastructure
    val syncPlayScope = rememberCoroutineScope()
    val timeSyncManager = remember {
        TimeSyncManager(
            syncTimeProvider = {
                jellyfinClient.getUtcTime().getOrThrow()
            },
        )
    }
    val syncPlayManager = remember(jellyfinClient) {
        SyncPlayManager(
            jellyfinClient = jellyfinClient,
            timeSyncManager = timeSyncManager,
            scope = syncPlayScope,
        )
    }

    var isInitialized by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var splashFinished by remember { mutableStateOf(false) }
    var isSeerrAuthenticated by remember { mutableStateOf(false) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }

    // Collect preferences (only ones used outside HomeScreen)
    val useMpvPlayer by tvPreferences.useMpvPlayer.collectAsState()
    val seerrEnabled by tvPreferences.seerrEnabled.collectAsState()
    val seerrUrl by tvPreferences.seerrUrl.collectAsState()
    val seerrApiKey by tvPreferences.seerrApiKey.collectAsState()
    val seerrSessionCookie by tvPreferences.seerrSessionCookie.collectAsState()
    val universesEnabled by tvPreferences.universesEnabled.collectAsState()
    val showDiscoverNav by tvPreferences.showDiscoverNav.collectAsState()

    LaunchedEffect(Unit) {
        appState.initialize()
        isLoggedIn = appState.isLoggedIn

        // Auto-login with debug credentials if available and not already logged in
        if (!isLoggedIn && BuildConfig.DEBUG) {
            DebugCredentials.read(context)?.let { creds ->
                android.util.Log.d("MyFlix", "Auto-login with debug credentials to ${creds.server}")
                jellyfinClient.login(creds.server, creds.username, creds.password)
                    .onSuccess { authResponse ->
                        appState.login(
                            serverUrl = creds.server,
                            accessToken = authResponse.accessToken,
                            userId = authResponse.user.id,
                            serverName = "Debug Server",
                            userName = authResponse.user.name,
                            username = creds.username,
                            password = creds.password,
                        )
                        isLoggedIn = true
                        android.util.Log.d("MyFlix", "Auto-login successful")
                    }
                    .onFailure { e ->
                        android.util.Log.e("MyFlix", "Auto-login failed: ${e.message}")
                    }
            }
        }

        isInitialized = true
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            jellyfinClient.getLibraries().onSuccess { libraries = it }

            // Crash recovery: Check for orphaned playback sessions
            tvPreferences.getActivePlaybackSession()?.let { session ->
                android.util.Log.d("MyFlix", "Crash recovery: Found orphaned session for item ${session.itemId}")
                jellyfinClient.reportPlaybackStopped(
                    session.itemId,
                    session.positionTicks,
                    mediaSourceId = session.mediaSourceId,
                )
                tvPreferences.clearActivePlaybackSession()
                android.util.Log.d("MyFlix", "Crash recovery: Reported playback stopped")
            }

            // Initialize TV home screen channel sync
            ChannelSyncWorker.schedule(context)
            ChannelSyncWorker.syncNow(context)

            // Request channel to be browsable (user must approve)
            val channelManager = TvChannelManager.getInstance(context)
            val channelId = channelManager.getOrCreateChannel()
            channelManager.requestChannelBrowsable(channelId)
        } else {
            libraries = emptyList()

            // Cancel channel sync and clear Watch Next on logout
            ChannelSyncWorker.cancel(context)
            WatchNextManager.getInstance(context).clearAll()
        }
    }

    // Initialize SeerrClient with stored session cookie, API key, or Jellyfin credentials
    LaunchedEffect(seerrUrl, seerrEnabled, seerrApiKey, seerrSessionCookie, isInitialized, isLoggedIn) {
        // Skip if already authenticated in this session (e.g., from setup screen)
        if (isSeerrAuthenticated) {
            android.util.Log.d("MyFlixSeerr", "Already authenticated, skipping")
            return@LaunchedEffect
        }

        android.util.Log.d(
            "MyFlixSeerr",
            "LaunchedEffect: init=$isInitialized, logged=$isLoggedIn, enabled=$seerrEnabled, url=$seerrUrl, hasCookie=${!seerrSessionCookie.isNullOrBlank()}, hasKey=${!seerrApiKey.isNullOrBlank()}",
        )
        if (isInitialized && isLoggedIn && seerrEnabled && !seerrUrl.isNullOrBlank()) {
            android.util.Log.d("MyFlixSeerr", "Connecting to Seerr at $seerrUrl")
            seerrClient.connectToServer(seerrUrl!!)
                .onSuccess {
                    android.util.Log.d("MyFlixSeerr", "Connected, trying auth...")

                    // Helper to try credentials as last resort
                    suspend fun tryCredentials() {
                        val username = appState.username
                        val password = appState.password
                        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                            seerrClient.loginWithJellyfin(username, password)
                                .onSuccess { user ->
                                    android.util.Log.d("MyFlixSeerr", "Credentials auth SUCCESS")
                                    isSeerrAuthenticated = true
                                    user.apiKey?.let { tvPreferences.setSeerrApiKey(it) }
                                    seerrClient.sessionCookie?.let { tvPreferences.setSeerrSessionCookie(it) }
                                }
                                .onFailure { e ->
                                    android.util.Log.e("MyFlixSeerr", "Credentials auth FAILED: ${e.message}")
                                    isSeerrAuthenticated = false
                                }
                        } else {
                            android.util.Log.w("MyFlixSeerr", "No credentials for Seerr auth")
                        }
                    }

                    // Try session cookie first (most reliable for persistence)
                    if (!seerrSessionCookie.isNullOrBlank()) {
                        android.util.Log.d("MyFlixSeerr", "Trying session cookie auth")
                        seerrClient.loginWithSessionCookie(seerrSessionCookie!!)
                            .onSuccess {
                                android.util.Log.d("MyFlixSeerr", "Session cookie auth SUCCESS")
                                isSeerrAuthenticated = true
                            }
                            .onFailure { e ->
                                android.util.Log.w(
                                    "MyFlixSeerr",
                                    "Session cookie auth FAILED: ${e.message}, trying credentials",
                                )
                                tryCredentials()
                            }
                    } else {
                        android.util.Log.d("MyFlixSeerr", "No session cookie, trying credentials")
                        tryCredentials()
                    }
                }
                .onFailure { e ->
                    android.util.Log.e("MyFlixSeerr", "Connect FAILED: ${e.message}")
                }
        } else if (!seerrEnabled) {
            android.util.Log.d("MyFlixSeerr", "Seerr disabled")
            isSeerrAuthenticated = false
        }
    }

    val navController = rememberNavController()

    // Track current route for NavigationRail
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Routes that should NOT show NavigationRail
    val routesWithoutNavRail = setOf("splash", "login", "player", "seerr/trailer")

    // Determine if nav rail should be visible
    val showNavRail = remember(currentRoute) {
        currentRoute?.let { route ->
            routesWithoutNavRail.none { route.startsWith(it) }
        } ?: false
    }

    // Track the last "source" NavItem for detail screens
    // When viewing detail/person screens, we want to keep the previous context highlighted
    var lastSourceNavItem by remember { mutableStateOf(NavItem.HOME) }

    // Determine current NavItem from route
    // Note: destination.route returns the route template (e.g., "library/{libraryId}/{libraryName}/{collectionType}")
    // so we need to use arguments to get actual values
    val currentNavItem = remember(currentRoute, currentBackStackEntry) {
        val isDetailScreen = currentRoute?.startsWith("detail/") == true ||
            currentRoute?.startsWith("person/") == true

        if (isDetailScreen) {
            // For detail screens, keep the previous source context
            lastSourceNavItem
        } else {
            // Determine NavItem from route
            val navItem = when {
                currentRoute?.startsWith("home") == true -> NavItem.HOME
                currentRoute?.startsWith("search") == true -> NavItem.SEARCH
                currentRoute?.startsWith("settings") == true -> NavItem.SETTINGS
                currentRoute?.startsWith("seerr") == true -> NavItem.DISCOVER
                currentRoute?.startsWith("library") == true -> getLibraryNavItem(currentBackStackEntry)
                currentRoute?.startsWith("collections") == true -> NavItem.COLLECTIONS
                currentRoute?.startsWith("collection/") == true -> NavItem.COLLECTIONS
                currentRoute?.startsWith("universes") == true -> NavItem.UNIVERSES
                currentRoute?.startsWith("universeCollection/") == true -> NavItem.UNIVERSES
                currentRoute?.startsWith("episodes") == true -> NavItem.SHOWS
                else -> NavItem.HOME
            }
            // Update the source for future detail screens
            lastSourceNavItem = navItem
            navItem
        }
    }

    // NavRail state - explicit activation model prevents focus stealing
    var isNavRailActive by remember { mutableStateOf(false) }
    var isNavRailExpanded by remember { mutableStateOf(false) }
    val navRailFocusRequester = remember { FocusRequester() }
    // Sentinel focus requester - passed to content screens for explicit left navigation
    val sentinelFocusRequester = remember { FocusRequester() }
    // Exit focus state - screens register their last focused item here for NavRail exit
    val exitFocusState = remember { mutableStateOf<FocusRequester?>(null) }

    // Sentinel enabled state - delayed after navigation to prevent focus stealing
    var sentinelEnabled by remember { mutableStateOf(false) }

    // Auto-collapse and deactivate NavRail when route changes (navigation occurred)
    var previousRoute by remember { mutableStateOf(currentRoute) }
    LaunchedEffect(currentRoute) {
        if (currentRoute != previousRoute && previousRoute != null) {
            // Disable sentinel during navigation
            sentinelEnabled = false
            if (isNavRailExpanded) {
                delay(400L) // Brief delay to show selection before collapsing
                isNavRailExpanded = false
                isNavRailActive = false
            }
        }
        previousRoute = currentRoute
        // Re-enable sentinel after delay to let screen content establish focus
        delay(NavRailAnimations.SentinelStartupDelayMs)
        sentinelEnabled = true
    }

    // Re-enable sentinel when NavRail becomes inactive (handles same-destination navigation)
    // This covers the case where the route doesn't change but the rail was deactivated
    LaunchedEffect(isNavRailActive) {
        if (!isNavRailActive) {
            // Wait for focus to settle on content before re-enabling sentinel
            delay(NavRailAnimations.SentinelStartupDelayMs)
            sentinelEnabled = true
        }
    }

    // Animate scrim alpha for NavRail (only show when active AND expanded)
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isNavRailActive && isNavRailExpanded) 0.5f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "scrimAlpha",
    )


    // Handle WebSocket remote control events
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            appState.webSocket.events.collect { event ->
                // Route SyncPlay events to SyncPlayManager
                when (event) {
                    is WebSocketEvent.SyncPlayCommand,
                    is WebSocketEvent.SyncPlayGroupJoined,
                    is WebSocketEvent.SyncPlayGroupLeft,
                    is WebSocketEvent.SyncPlayGroupStateUpdate,
                    is WebSocketEvent.SyncPlayPlayQueueUpdate,
                    is WebSocketEvent.SyncPlayUserJoined,
                    is WebSocketEvent.SyncPlayUserLeft -> {
                        syncPlayManager.onSyncPlayEvent(event)
                    }
                    else -> {}
                }

                // Existing event handling
                when (event) {
                    is WebSocketEvent.GeneralCommand -> {
                        when (event.name) {
                            GeneralCommandType.DisplayMessage -> {
                                val text = event.arguments["Text"]
                                val header = event.arguments["Header"]
                                val message = if (header != null) "$header: $text" else text
                                if (message != null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        message,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            GeneralCommandType.DisplayContent -> {
                                val itemId = event.arguments["ItemId"]
                                if (itemId != null) {
                                    navController.navigate("detail/$itemId")
                                }
                            }
                            GeneralCommandType.GoHome -> {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                            GeneralCommandType.GoToSearch -> {
                                navController.navigate("search")
                            }
                            GeneralCommandType.GoToSettings -> {
                                navController.navigate("settings")
                            }
                            else -> {
                                android.util.Log.d("WebSocket", "Unhandled command: ${event.name}")
                            }
                        }
                    }
                    is WebSocketEvent.PlaystateCommand -> {
                        // Handled by PlayerScreen when player is active
                        android.util.Log.d("WebSocket", "Playstate event: $event")
                    }
                    is WebSocketEvent.PlayCommand -> {
                        // Handle remote play commands from Jellyfin dashboard
                        android.util.Log.d("WebSocket", "Play command: ${event.playCommand} items=${event.itemIds}")
                        when (event.playCommand) {
                            PlayCommandType.PlayNow -> {
                                // Play the first item immediately
                                event.itemIds.firstOrNull()?.let { itemId ->
                                    val startPosition = event.startPositionTicks?.let { it / 10_000 }
                                    val route = if (startPosition != null && startPosition > 0) {
                                        "player/$itemId?startPositionMs=$startPosition"
                                    } else {
                                        "player/$itemId"
                                    }
                                    navController.navigate(route)
                                }
                            }
                            PlayCommandType.PlayNext,
                            PlayCommandType.PlayLast -> {
                                // Queue support could be added here
                                android.util.Log.d("WebSocket", "Queue command not fully implemented: ${event.playCommand}")
                                // For now, just play the first item
                                event.itemIds.firstOrNull()?.let { itemId ->
                                    navController.navigate("player/$itemId")
                                }
                            }
                            PlayCommandType.PlayInstantMix,
                            PlayCommandType.PlayShuffle -> {
                                // Music-related, not implemented for TV/Movies
                                android.util.Log.d("WebSocket", "Music command not implemented: ${event.playCommand}")
                            }
                        }
                    }
                    is WebSocketEvent.LibraryChanged -> {
                        // Refresh library content
                        jellyfinClient.getLibraries().onSuccess { libraries = it }
                        jellyfinClient.invalidateCache()
                    }
                    else -> {
                        // Unhandled event
                    }
                }
            }
        }
    }

    // Centralized navigation handler
    val handleNavigation: (NavItem) -> Unit = { navItem ->
        when (navItem) {
            NavItem.HOME -> {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
            NavItem.SEARCH -> { navController.navigate("search") }
            NavItem.SETTINGS -> { navController.navigate("settings") }
            NavItem.DISCOVER -> { navController.navigate("seerr") }
            NavItem.MOVIES -> {
                LibraryFinder.findMoviesLibrary(libraries)?.let {
                    navController.navigate(
                        NavigationHelper.buildLibraryRoute(it.id, it.name, it.collectionType)
                    )
                } ?: navController.navigate("home")
            }
            NavItem.SHOWS -> {
                LibraryFinder.findShowsLibrary(libraries)?.let {
                    navController.navigate(
                        NavigationHelper.buildLibraryRoute(it.id, it.name, it.collectionType)
                    )
                } ?: navController.navigate("home")
            }
            NavItem.COLLECTIONS -> { navController.navigate("collections") }
            NavItem.UNIVERSES -> { navController.navigate("universes") }
        }
    }

    // Navigate when BOTH splash animation completes AND initialization is done
    LaunchedEffect(splashFinished, isInitialized) {
        if (splashFinished && isInitialized) {
            val destination = if (isLoggedIn) "home" else "login"
            navController.navigate(destination) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // NavHost padding when NavRail is visible
    val navHostModifier = if (showNavRail) {
        Modifier
            .fillMaxSize()
            .padding(start = NavRailDimensions.CollapsedWidth)
    } else {
        Modifier.fillMaxSize()
    }

    CompositionLocalProvider(LocalExitFocusState provides exitFocusState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvColors.Background)
                .onKeyEvent { event ->
                    // Handle Menu key press from anywhere to toggle NavRail
                    if (showNavRail && event.type == KeyEventType.KeyDown && event.key == Key.Menu) {
                        if (isNavRailActive) {
                            // Deactivate and collapse - disable sentinel to prevent race condition
                            sentinelEnabled = false
                            isNavRailActive = false
                            isNavRailExpanded = false
                        } else {
                            // Activate and expand
                            isNavRailActive = true
                            isNavRailExpanded = true
                            navRailFocusRequester.requestFocus()
                        }
                        true
                    } else {
                        false
                    }
                },
        ) {
        // Main content - NavHost always rendered
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = navHostModifier,
        ) {
            composable("splash") {
                SplashScreen(
                    onFinished = { splashFinished = true },
                    config = SplashScreenTvConfig,
                )
            }

            composable("login") {
                LoginScreen(
                    appState = appState,
                    jellyfinClient = jellyfinClient,
                    onLoginSuccess = {
                        isLoggedIn = true
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }

            composable("home") {
                // Process pending deep link from TV home screen (Watch Next, custom channels)
                LaunchedEffect(pendingDeepLink) {
                    pendingDeepLink?.let { deepLink ->
                        onDeepLinkConsumed()
                        when (deepLink) {
                            is DeepLink.Play -> {
                                val route = if (deepLink.startPositionMs != null) {
                                    "player/${deepLink.itemId}?startPositionMs=${deepLink.startPositionMs}"
                                } else {
                                    "player/${deepLink.itemId}"
                                }
                                Log.d("MainActivity", "Processing deep link: navigating to $route")
                                navController.navigate(route)
                            }
                            is DeepLink.Home -> {
                                // Already at home, do nothing
                                Log.d("MainActivity", "Processing deep link: already at home")
                            }
                        }
                    }
                }
                HomeScreen(
                    jellyfinClient = jellyfinClient,
                    preferences = tvPreferences,
                    seerrClient = if (isSeerrAuthenticated) seerrClient else null,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onPlayClick = { itemId ->
                        navController.navigate(NavigationHelper.buildPlayerRoute(itemId))
                    },
                    onSeerrMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onEpisodeClick = { seriesId, seasonNumber, episodeId ->
                        navController.navigate("episodes/$seriesId?seasonNumber=$seasonNumber&episodeId=$episodeId")
                    },
                    onSeriesMoreInfoClick = { seriesId ->
                        // Navigate to EpisodesScreen for series, starting at season 1
                        navController.navigate("episodes/$seriesId?seasonNumber=1")
                    },
                    leftEdgeFocusRequester = sentinelFocusRequester,
                )
            }

            composable("search") {
                SearchScreen(
                    jellyfinClient = jellyfinClient,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("settings") {
                PreferencesScreen(
                    preferences = tvPreferences,
                    jellyfinClient = jellyfinClient,
                    appState = appState,
                    onAddServer = {
                        // Navigate to login to add a new server
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    onNavigateSeerrSetup = {
                        navController.navigate("seerr/setup")
                    },
                    isSeerrAuthenticated = isSeerrAuthenticated,
                )
            }

            // Seerr routes
            composable("seerr") {
                // Require actual authentication - isSeerrAuthenticated is set after successful login
                if (!isSeerrAuthenticated) {
                    SeerrSetupScreen(
                        seerrClient = seerrClient,
                        preferences = tvPreferences,
                        jellyfinUsername = appState.username,
                        jellyfinPassword = appState.password,
                        jellyfinServerUrl = jellyfinClient.serverUrl,
                        onSetupComplete = {
                            // Just set authenticated - recomposition will show SeerrHomeScreen
                            isSeerrAuthenticated = true
                        },
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    val seerrHomeViewModel: SeerrHomeViewModel = viewModel(
                        factory = SeerrHomeViewModel.Factory(seerrClient),
                    )
                    SeerrHomeScreen(
                        viewModel = seerrHomeViewModel,
                        seerrClient = seerrClient,
                        onMediaClick = { mediaType, tmdbId ->
                            navController.navigate("seerr/$mediaType/$tmdbId")
                        },
                        onNavigateSeerrSearch = {
                            navController.navigate(NavigationHelper.SEERR_SEARCH_ROUTE)
                        },
                        onNavigateSeerrRequests = {
                            navController.navigate(NavigationHelper.SEERR_REQUESTS_ROUTE)
                        },
                        onNavigateDiscoverTrending = {
                            navController.navigate("seerr/trending")
                        },
                        onNavigateDiscoverMovies = {
                            navController.navigate("seerr/movies")
                        },
                        onNavigateDiscoverTv = {
                            navController.navigate("seerr/tv")
                        },
                        onNavigateDiscoverUpcomingMovies = {
                            navController.navigate("seerr/upcoming/movies")
                        },
                        onNavigateDiscoverUpcomingTv = {
                            navController.navigate("seerr/upcoming/tv")
                        },
                        onNavigateGenre = { genreMediaType, genreId, genreName ->
                            val encodedName = NavigationHelper.encodeNavArg(genreName)
                            navController.navigate("seerr/genre/$genreMediaType/$genreId/$encodedName")
                        },
                        onNavigateStudio = { studioId, studioName ->
                            val encodedName = NavigationHelper.encodeNavArg(studioName)
                            navController.navigate("seerr/studio/$studioId/$encodedName")
                        },
                        onNavigateNetwork = { networkId, networkName ->
                            val encodedName = NavigationHelper.encodeNavArg(networkName)
                            navController.navigate("seerr/network/$networkId/$encodedName")
                        },
                    )
                }
            }

            composable("seerr/setup") {
                SeerrSetupScreen(
                    seerrClient = seerrClient,
                    preferences = tvPreferences,
                    jellyfinUsername = appState.username,
                    jellyfinPassword = appState.password,
                    jellyfinServerUrl = jellyfinClient.serverUrl,
                    onSetupComplete = {
                        navController.navigate("seerr") {
                            popUpTo("seerr/setup") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            // Seerr discover screens with pagination
            composable("seerr/trending") {
                SeerrDiscoverTrendingScreen(
                    seerrClient = seerrClient,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("seerr/movies") {
                SeerrDiscoverMoviesScreen(
                    seerrClient = seerrClient,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("seerr/tv") {
                SeerrDiscoverTvScreen(
                    seerrClient = seerrClient,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("seerr/upcoming/movies") {
                SeerrDiscoverUpcomingMoviesScreen(
                    seerrClient = seerrClient,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("seerr/upcoming/tv") {
                SeerrDiscoverUpcomingTvScreen(
                    seerrClient = seerrClient,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "seerr/{mediaType}/{tmdbId}",
                arguments = listOf(
                    navArgument("mediaType") { type = NavType.StringType },
                    navArgument("tmdbId") { type = NavType.IntType },
                ),
            ) { backStackEntry ->
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                val tmdbId = backStackEntry.arguments?.getInt("tmdbId") ?: return@composable
                SeerrDetailScreen(
                    mediaType = mediaType,
                    tmdbId = tmdbId,
                    seerrClient = seerrClient,
                    onMediaClick = { relatedMediaType, relatedTmdbId ->
                        navController.navigate("seerr/$relatedMediaType/$relatedTmdbId")
                    },
                    onTrailerClick = { videoKey, videoTitle ->
                        navController.navigate(NavigationHelper.buildSeerrTrailerRoute(videoKey, videoTitle))
                    },
                    onBack = { navController.popBackStack() },
                    onActorClick = { personId ->
                        navController.navigate("seerr/person/$personId")
                    },
                    onNavigateGenre = { genreMediaType, genreId, genreName ->
                        val encodedName = NavigationHelper.encodeNavArg(genreName)
                        navController.navigate("seerr/genre/$genreMediaType/$genreId/$encodedName")
                    },
                )
            }

            composable(
                route = "seerr/genre/{mediaType}/{genreId}/{genreName}",
                arguments = listOf(
                    navArgument("mediaType") { type = NavType.StringType },
                    navArgument("genreId") { type = NavType.IntType },
                    navArgument("genreName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val genreMediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                val genreId = backStackEntry.arguments?.getInt("genreId") ?: return@composable
                val genreNameEncoded = backStackEntry.arguments?.getString("genreName") ?: ""
                val genreName = NavigationHelper.decodeNavArg(genreNameEncoded)
                SeerrDiscoverByGenreScreen(
                    seerrClient = seerrClient,
                    mediaType = genreMediaType,
                    genreId = genreId,
                    genreName = genreName,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "seerr/studio/{studioId}/{studioName}",
                arguments = listOf(
                    navArgument("studioId") { type = NavType.IntType },
                    navArgument("studioName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val studioId = backStackEntry.arguments?.getInt("studioId") ?: return@composable
                val studioNameEncoded = backStackEntry.arguments?.getString("studioName") ?: ""
                val studioName = NavigationHelper.decodeNavArg(studioNameEncoded)
                SeerrDiscoverByStudioScreen(
                    seerrClient = seerrClient,
                    studioId = studioId,
                    studioName = studioName,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "seerr/network/{networkId}/{networkName}",
                arguments = listOf(
                    navArgument("networkId") { type = NavType.IntType },
                    navArgument("networkName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val networkId = backStackEntry.arguments?.getInt("networkId") ?: return@composable
                val networkNameEncoded = backStackEntry.arguments?.getString("networkName") ?: ""
                val networkName = NavigationHelper.decodeNavArg(networkNameEncoded)
                SeerrDiscoverByNetworkScreen(
                    seerrClient = seerrClient,
                    networkId = networkId,
                    networkName = networkName,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "seerr/person/{personId}",
                arguments = listOf(
                    navArgument("personId") { type = NavType.IntType },
                ),
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getInt("personId") ?: return@composable
                SeerrActorDetailScreen(
                    personId = personId,
                    seerrClient = seerrClient,
                    onBack = { navController.popBackStack() },
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                )
            }

            composable(
                route = NavigationHelper.SEERR_TRAILER_ROUTE,
                arguments = listOf(
                    navArgument("videoKey") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val videoKeyEncoded = backStackEntry.arguments?.getString("videoKey") ?: return@composable
                val titleEncoded = backStackEntry.arguments?.getString("title") ?: ""
                val videoKey = NavigationHelper.decodeNavArg(videoKeyEncoded)
                val title = NavigationHelper.decodeNavArg(titleEncoded).takeIf { it.isNotBlank() }
                TrailerPlayerScreen(
                    videoKey = videoKey,
                    title = title,
                    onBack = { navController.popBackStack() },
                    useMpvPlayer = useMpvPlayer,
                )
            }

            composable(NavigationHelper.SEERR_SEARCH_ROUTE) {
                SeerrSearchScreen(
                    seerrClient = seerrClient,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onPersonClick = { personId ->
                        navController.navigate("seerr/person/$personId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(NavigationHelper.SEERR_REQUESTS_ROUTE) {
                SeerrRequestsScreen(
                    seerrClient = seerrClient,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { tmdbId, mediaType ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                )
            }

            composable(
                route = "seerr/discover/{category}",
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category") ?: "trending"
                when (NavigationHelper.decodeNavArg(category)) {
                    "movies" -> SeerrDiscoverMoviesScreen(
                        seerrClient = seerrClient,
                        onMediaClick = { mediaType, tmdbId ->
                            navController.navigate("seerr/$mediaType/$tmdbId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                    "tv" -> SeerrDiscoverTvScreen(
                        seerrClient = seerrClient,
                        onMediaClick = { mediaType, tmdbId ->
                            navController.navigate("seerr/$mediaType/$tmdbId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                    "upcoming_movies" -> SeerrDiscoverUpcomingMoviesScreen(
                        seerrClient = seerrClient,
                        onMediaClick = { mediaType, tmdbId ->
                            navController.navigate("seerr/$mediaType/$tmdbId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                    "upcoming_tv" -> SeerrDiscoverUpcomingTvScreen(
                        seerrClient = seerrClient,
                        onMediaClick = { mediaType, tmdbId ->
                            navController.navigate("seerr/$mediaType/$tmdbId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                    else -> SeerrDiscoverTrendingScreen(
                        seerrClient = seerrClient,
                        onMediaClick = { mediaType, tmdbId ->
                            navController.navigate("seerr/$mediaType/$tmdbId")
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            composable(
                route = "seerr/collection/{collectionId}",
                arguments = listOf(
                    navArgument("collectionId") { type = NavType.IntType },
                ),
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getInt("collectionId") ?: return@composable
                SeerrCollectionDetailScreen(
                    collectionId = collectionId,
                    seerrClient = seerrClient,
                    onMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "library/{libraryId}/{libraryName}/{collectionType}",
                arguments = listOf(
                    navArgument("libraryId") { type = NavType.StringType },
                    navArgument("libraryName") { type = NavType.StringType },
                    navArgument("collectionType") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val libraryId = backStackEntry.arguments?.getString("libraryId") ?: return@composable
                val libraryNameEncoded = backStackEntry.arguments?.getString("libraryName") ?: ""
                val libraryName = NavigationHelper.decodeNavArg(libraryNameEncoded)
                val collectionTypeEncoded = backStackEntry.arguments?.getString("collectionType") ?: ""
                val collectionType = NavigationHelper.decodeNavArg(collectionTypeEncoded)
                    .takeIf { it.isNotEmpty() }
                LibraryScreen(
                    libraryId = libraryId,
                    libraryName = libraryName,
                    collectionType = collectionType,
                    jellyfinClient = jellyfinClient,
                    preferences = tvPreferences,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onPlayClick = { itemId ->
                        navController.navigate(NavigationHelper.buildPlayerRoute(itemId))
                    },
                )
            }

            // Collections routes
            composable("collections") {
                CollectionsLibraryScreen(
                    jellyfinClient = jellyfinClient,
                    onCollectionClick = { collectionId ->
                        navController.navigate("collection/$collectionId")
                    },
                    excludeUniverseCollections = universesEnabled,
                )
            }

            composable(
                route = "collection/{collectionId}",
                arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getString("collectionId") ?: return@composable
                CollectionDetailScreen(
                    collectionId = collectionId,
                    jellyfinClient = jellyfinClient,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onPlayClick = { itemId, startPositionMs ->
                        navController.navigate(NavigationHelper.buildPlayerRoute(itemId, startPositionMs))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            // Universe collections route (tagged with "universe-collection")
            composable("universes") {
                UniverseCollectionsScreen(
                    jellyfinClient = jellyfinClient,
                    onCollectionClick = { collectionId ->
                        navController.navigate("universeCollection/$collectionId")
                    },
                )
            }

            // Universe collection detail (keeps Universes nav item selected)
            composable(
                route = "universeCollection/{collectionId}",
                arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getString("collectionId") ?: return@composable
                CollectionDetailScreen(
                    collectionId = collectionId,
                    jellyfinClient = jellyfinClient,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onPlayClick = { itemId, startPositionMs ->
                        navController.navigate(NavigationHelper.buildPlayerRoute(itemId, startPositionMs))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            DetailScreen(
                itemId = itemId,
                jellyfinClient = jellyfinClient,
                appPreferences = tvPreferences,
                themeMusicPlayer = appState.themeMusicPlayer,
                onPlayClick = { id, startPositionMs ->
                    navController.navigate(NavigationHelper.buildPlayerRoute(id, startPositionMs))
                },
                onPlayItemClick = { id, startPositionMs ->
                    navController.navigate(NavigationHelper.buildPlayerRoute(id, startPositionMs))
                },
                onEpisodeClick = { episodeId ->
                    navController.navigate(NavigationHelper.buildPlayerRoute(episodeId))
                },
                onTrailerClick = { videoKey, title ->
                    navController.navigate(NavigationHelper.buildSeerrTrailerRoute(videoKey, title))
                },
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { relatedItemId ->
                    navController.navigate("detail/$relatedItemId")
                },
                    onNavigateToPerson = { personId ->
                        navController.navigate("person/$personId")
                    },
                    onNavigateToGenre = { genre, libraryType ->
                        // TODO: Navigate to library filtered by genre
                        // For now, navigate back to home
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onNavigateToEpisodes = { seriesId, seasonNumber, episodeId ->
                        val route = if (episodeId != null) {
                            "episodes/$seriesId?seasonNumber=$seasonNumber&episodeId=$episodeId"
                        } else {
                            "episodes/$seriesId?seasonNumber=$seasonNumber"
                        }
                        // Replace current screen to avoid back-navigating to loading state
                        navController.navigate(route) {
                            popUpTo("detail/$itemId") { inclusive = true }
                        }
                    },
                    leftEdgeFocusRequester = sentinelFocusRequester,
                )
            }

            composable(
                route = "person/{personId}",
                arguments = listOf(navArgument("personId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getString("personId") ?: return@composable
                PersonDetailScreen(
                    personId = personId,
                    jellyfinClient = jellyfinClient,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            // Episodes screen for series/season/episode browsing
            composable(
                route = "episodes/{seriesId}?seasonNumber={seasonNumber}&episodeId={episodeId}",
                arguments = listOf(
                    navArgument("seriesId") { type = NavType.StringType },
                    navArgument("seasonNumber") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("episodeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val seriesId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
                val seasonNumber = backStackEntry.arguments?.getInt("seasonNumber") ?: -1
                val episodeId = backStackEntry.arguments?.getString("episodeId")

                // Use DetailViewModel to load series data
                // Pass seasonNumber to let ViewModel auto-select the correct season,
                // avoiding race condition where Season 1 is selected before we can override
                val viewModel: DetailViewModel = viewModel(
                    key = "$seriesId-$seasonNumber", // Include seasonNumber in key to recreate VM if season changes
                    factory = DetailViewModel.Factory(
                        itemId = seriesId,
                        jellyfinClient = jellyfinClient,
                        preferredSeasonNumber = if (seasonNumber > 0) seasonNumber else -1,
                    ),
                )
                val state by viewModel.uiState.collectAsState()

                val seasonChangeScope = rememberCoroutineScope()

                // Derive initial season index reactively - null means not yet resolvable
                // This prevents the race condition where we render with index=0 before data loads
                val resolvedSeasonIndex by remember(state.seasons, seasonNumber) {
                    derivedStateOf {
                        if (state.seasons.isEmpty()) null
                        else if (seasonNumber >= 0) {
                            state.seasons.indexOfFirst { it.indexNumber == seasonNumber }
                                .takeIf { it >= 0 } ?: 0
                        } else 0
                    }
                }

                // Track user-selected season override (for manual season switching)
                var userSeasonIndex by remember { mutableStateOf<Int?>(null) }

                // Effective season index: user override takes precedence over initial
                val selectedSeasonIndex = userSeasonIndex ?: resolvedSeasonIndex

                // When season index changes, load episodes for that season
                LaunchedEffect(selectedSeasonIndex, state.seasons) {
                    selectedSeasonIndex?.let { index ->
                        state.seasons.getOrNull(index)?.let { season ->
                            if (season.id != state.selectedSeason?.id) {
                                viewModel.selectSeason(season)
                            }
                        }
                    }
                }

                // Show loading or episodes screen
                // Don't render until season index is resolved to prevent showing wrong season
                if (state.isLoading || state.item == null || selectedSeasonIndex == null) {
                    // Focusable loading state to capture focus and prevent NavRail auto-expand
                    val loadingFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        loadingFocusRequester.requestFocus()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TvColors.Background)
                            .focusRequester(loadingFocusRequester)
                            .focusable(),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.tv.material3.Text(
                            text = "Loading...",
                            color = TvColors.TextPrimary,
                        )
                    }
                } else {
                    EpisodesScreen(
                        seriesName = state.item?.name ?: "",
                        seasons = state.seasons,
                        episodes = state.episodes,
                        selectedSeasonIndex = selectedSeasonIndex!!,
                        selectedEpisodeId = episodeId,
                        jellyfinClient = jellyfinClient,
                        onSeasonSelected = { index ->
                            // Temporarily disable sentinel to prevent focus theft during season change
                            sentinelEnabled = false
                            userSeasonIndex = index
                            // Re-enable sentinel after content has time to establish focus
                            seasonChangeScope.launch {
                                delay(NavRailAnimations.SentinelStartupDelayMs)
                                sentinelEnabled = true
                            }
                        },
                        onEpisodeClick = { episode ->
                            // Navigate to player with selected episode
                            val startPosition = episode.userData?.playbackPositionTicks
                                ?.let { it / 10_000 } ?: 0L
                            navController.navigate(NavigationHelper.buildPlayerRoute(episode.id, startPosition))
                        },
                        onWatchedClick = { episode ->
                            val isCurrentlyWatched = episode.userData?.played == true
                            viewModel.setPlayed(episode.id, !isCurrentlyWatched)
                        },
                        onFavoriteClick = { episode ->
                            val isCurrentlyFavorite = episode.userData?.isFavorite == true
                            viewModel.setFavorite(episode.id, !isCurrentlyFavorite)
                        },
                        onPersonClick = { personId ->
                            navController.navigate("person/$personId")
                        },
                        onGoToSeries = {
                            navController.navigate("detail/$seriesId")
                        },
                        onBackClick = { navController.popBackStack() },
                        leftEdgeFocusRequester = sentinelFocusRequester,
                    )
                }
            }

            composable(
                route = "player/{itemId}?startPositionMs={startPositionMs}",
                arguments = listOf(
                    navArgument("itemId") { type = NavType.StringType },
                    navArgument("startPositionMs") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                val startPositionMs = backStackEntry.arguments?.getLong("startPositionMs")

                // Stop theme music when entering player
                LaunchedEffect(Unit) {
                    appState.themeMusicPlayer.stop()
                }

                PlayerScreen(
                    itemId = itemId,
                    startPositionMs = startPositionMs?.takeIf { it >= 0 },
                    jellyfinClient = jellyfinClient,
                    appPreferences = tvPreferences,
                    useMpvPlayer = useMpvPlayer,
                    webSocketEvents = appState.webSocket.events,
                    syncPlayManager = syncPlayManager,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Scrim layer - dims content when NavRail is expanded
        if (showNavRail && scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.5f)
                    .background(Color.Black.copy(alpha = scrimAlpha)),
            )
        }

        // Focus sentinel - detects left-edge navigation and activates rail
        // Positioned at x=0 (behind collapsed rail) so it's to the LEFT of content
        // Content screens use sentinelFocusRequester for explicit left navigation
        if (showNavRail) {
            FocusSentinel(
                isEnabled = !isNavRailActive && sentinelEnabled,
                onActivate = {
                    isNavRailActive = true
                    isNavRailExpanded = true
                },
                railFocusRequester = navRailFocusRequester,
                sentinelFocusRequester = sentinelFocusRequester,
                modifier = Modifier.zIndex(0.25f),
            )
        }

        // NavRail - uses explicit activation model to prevent focus stealing
        if (showNavRail) {
            NavigationRail(
                selectedItem = currentNavItem,
                onItemSelected = handleNavigation,
                isActive = isNavRailActive,
                isExpanded = isNavRailExpanded,
                onExpandedChange = { isNavRailExpanded = it },
                onDeactivate = {
                    // Disable sentinel immediately to prevent race condition where
                    // focus traversal through sentinel re-activates the rail
                    sentinelEnabled = false
                    isNavRailActive = false
                },
                showUniverses = universesEnabled,
                showDiscover = isSeerrAuthenticated && showDiscoverNav,
                firstItemFocusRequester = navRailFocusRequester,
                // Use screen's registered exit focus target, fall back to default focus traversal
                exitFocusRequester = exitFocusState.value ?: FocusRequester.Default,
                modifier = Modifier.zIndex(1f),
            )
        }
        }
    }
}

/**
 * Determines the NavItem for library routes based on the collection type.
 * Uses the actual route arguments rather than parsing the route template.
 */
private fun getLibraryNavItem(backStackEntry: NavBackStackEntry?): NavItem {
    val collectionType = backStackEntry?.arguments?.getString("collectionType") ?: ""
    return when {
        collectionType.contains("movie", ignoreCase = true) -> NavItem.MOVIES
        collectionType.contains("tvshow", ignoreCase = true) -> NavItem.SHOWS
        else -> NavItem.HOME
    }
}

