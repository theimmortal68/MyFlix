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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.jausc.myflix.core.common.LibraryFinder
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.ui.SplashScreen
import dev.jausc.myflix.core.common.ui.SplashScreenTvConfig
import dev.jausc.myflix.core.common.ui.SplashScreen
import dev.jausc.myflix.core.common.ui.SplashScreenTvConfig
import dev.jausc.myflix.core.common.ui.SplashScreen
import dev.jausc.myflix.core.common.ui.SplashScreenTvConfig
import dev.jausc.myflix.core.common.util.NavigationHelper
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.data.DebugCredentials
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.tv.ui.screens.CollectionDetailScreen
import dev.jausc.myflix.tv.ui.screens.CollectionsLibraryScreen
import dev.jausc.myflix.tv.ui.screens.DetailScreen
import dev.jausc.myflix.tv.ui.screens.UniverseCollectionsScreen
import dev.jausc.myflix.tv.ui.screens.HomeScreen
import dev.jausc.myflix.tv.ui.screens.LibraryScreen
import dev.jausc.myflix.tv.ui.screens.LoginScreen
import dev.jausc.myflix.tv.ui.screens.PlayerScreen
import dev.jausc.myflix.tv.ui.screens.PreferencesScreen
import dev.jausc.myflix.tv.ui.screens.SearchScreen
import dev.jausc.myflix.tv.ui.screens.PersonDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrActorDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrCollectionDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverByGenreScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverByNetworkScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverByStudioScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverMoviesScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverTrendingScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverTvScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverUpcomingMoviesScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverUpcomingTvScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrHomeScreen
import dev.jausc.myflix.tv.ui.screens.SeerrRequestsScreen
import dev.jausc.myflix.tv.ui.screens.SeerrSearchScreen
import dev.jausc.myflix.tv.ui.screens.SeerrSetupScreen
import dev.jausc.myflix.tv.ui.screens.TrailerPlayerScreen
import dev.jausc.myflix.tv.ui.screens.TrailerWebViewScreen
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.theme.MyFlixTvTheme
import dev.jausc.myflix.tv.ui.theme.TvColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFlixTvTheme {
                MyFlixTvApp()
            }
        }
    }
}

@Composable
fun MyFlixTvApp() {
    val context = LocalContext.current

    val jellyfinClient = remember { JellyfinClient() }
    val appState = remember { AppState(context, jellyfinClient) }
    val tvPreferences = remember { TvPreferences.getInstance(context) }
    val seerrClient = remember { SeerrClient() }

    var isInitialized by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var splashFinished by remember { mutableStateOf(false) }
    var isSeerrAuthenticated by remember { mutableStateOf(false) }
    var libraries by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }

    // Collect preferences (only ones used outside HomeScreen)
    val useMpvPlayer by tvPreferences.useMpvPlayer.collectAsState()
    val useTrailerFallback by tvPreferences.useTrailerFallback.collectAsState()
    val seerrEnabled by tvPreferences.seerrEnabled.collectAsState()
    val seerrUrl by tvPreferences.seerrUrl.collectAsState()
    val seerrApiKey by tvPreferences.seerrApiKey.collectAsState()
    val seerrSessionCookie by tvPreferences.seerrSessionCookie.collectAsState()
    val universesEnabled by tvPreferences.universesEnabled.collectAsState()

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
        } else {
            libraries = emptyList()
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

    // Centralized navigation handler
    val handleNavigation: (NavItem) -> Unit = { navItem ->
        when (navItem) {
            NavItem.HOME -> navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
            NavItem.SEARCH -> navController.navigate("search")
            NavItem.SETTINGS -> navController.navigate("settings")
            NavItem.DISCOVER -> navController.navigate("seerr")
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
            NavItem.COLLECTIONS -> navController.navigate("collections")
            NavItem.UNIVERSES -> navController.navigate("universes")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        NavHost(
            navController = navController,
            startDestination = "splash",
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
                HomeScreen(
                    jellyfinClient = jellyfinClient,
                    preferences = tvPreferences,
                    seerrClient = if (isSeerrAuthenticated) seerrClient else null,
                    onLibraryClick = { libraryId, libraryName, collectionType ->
                        navController.navigate(
                            NavigationHelper.buildLibraryRoute(libraryId, libraryName, collectionType),
                        )
                    },
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onPlayClick = { itemId ->
                        navController.navigate(NavigationHelper.buildPlayerRoute(itemId))
                    },
                    onSearchClick = {
                        navController.navigate("search")
                    },
                    onDiscoverClick = {
                        navController.navigate("seerr")
                    },
                    onCollectionsClick = {
                        navController.navigate("collections")
                    },
                    onUniversesClick = {
                        navController.navigate("universes")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    onSeerrMediaClick = { mediaType, tmdbId ->
                        navController.navigate("seerr/$mediaType/$tmdbId")
                    },
                    showUniversesInNav = universesEnabled,
                )
            }

            composable("search") {
                SearchScreen(
                    jellyfinClient = jellyfinClient,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateHome = { handleNavigation(NavItem.HOME) },
                    onNavigateMovies = { handleNavigation(NavItem.MOVIES) },
                    onNavigateShows = { handleNavigation(NavItem.SHOWS) },
                    onNavigateSettings = { handleNavigation(NavItem.SETTINGS) },
                    showUniversesInNav = universesEnabled,
                )
            }

            composable("settings") {
                PreferencesScreen(
                    preferences = tvPreferences,
                    jellyfinClient = jellyfinClient,
                    appState = appState,
                    onNavigateHome = { handleNavigation(NavItem.HOME) },
                    onNavigateSearch = { handleNavigation(NavItem.SEARCH) },
                    onNavigateMovies = { handleNavigation(NavItem.MOVIES) },
                    onNavigateShows = { handleNavigation(NavItem.SHOWS) },
                    onNavigateDiscover = { handleNavigation(NavItem.DISCOVER) },
                    onNavigateCollections = { handleNavigation(NavItem.COLLECTIONS) },
                    onNavigateUniverses = { handleNavigation(NavItem.UNIVERSES) },
                    onNavigateLibrary = { libraryId, libraryName, collectionType ->
                        navController.navigate(
                            NavigationHelper.buildLibraryRoute(libraryId, libraryName, collectionType),
                        )
                    },
                    onAddServer = {
                        // Navigate to login to add a new server
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    showUniversesInNav = universesEnabled,
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
                    SeerrHomeScreen(
                        seerrClient = seerrClient,
                        onMediaClick = { mediaType, tmdbId ->
                            navController.navigate("seerr/$mediaType/$tmdbId")
                        },
                        onNavigateHome = { handleNavigation(NavItem.HOME) },
                        onNavigateSearch = { handleNavigation(NavItem.SEARCH) },
                        onNavigateMovies = { handleNavigation(NavItem.MOVIES) },
                        onNavigateShows = { handleNavigation(NavItem.SHOWS) },
                        onNavigateSettings = { handleNavigation(NavItem.SETTINGS) },
                        jellyfinClient = jellyfinClient,
                        onNavigateLibrary = { libraryId, libraryName, collectionType ->
                            navController.navigate(
                                NavigationHelper.buildLibraryRoute(libraryId, libraryName, collectionType),
                            )
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
                        onNavigateSeerrSearch = {
                            navController.navigate(NavigationHelper.SEERR_SEARCH_ROUTE)
                        },
                        onNavigateSeerrRequests = {
                            navController.navigate(NavigationHelper.SEERR_REQUESTS_ROUTE)
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
                        showUniversesInNav = universesEnabled,
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
                        if (useTrailerFallback) {
                            navController.navigate(NavigationHelper.buildSeerrTrailerFallbackRoute(videoKey, videoTitle))
                        } else {
                            navController.navigate(NavigationHelper.buildSeerrTrailerRoute(videoKey, videoTitle))
                        }
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
                )
            }

            composable(
                route = NavigationHelper.SEERR_TRAILER_FALLBACK_ROUTE,
                arguments = listOf(
                    navArgument("videoKey") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val videoKeyEncoded = backStackEntry.arguments?.getString("videoKey") ?: return@composable
                val titleEncoded = backStackEntry.arguments?.getString("title") ?: ""
                val videoKey = NavigationHelper.decodeNavArg(videoKeyEncoded)
                val title = NavigationHelper.decodeNavArg(titleEncoded).takeIf { it.isNotBlank() }
                TrailerWebViewScreen(
                    videoKey = videoKey,
                    title = title,
                    onBack = { navController.popBackStack() },
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
                        navController.navigate("seerr/detail/$tmdbId/$mediaType")
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
                    onNavigate = handleNavigation,
                    showUniversesInNav = universesEnabled,
                )
            }

            // Collections routes
            composable("collections") {
                CollectionsLibraryScreen(
                    jellyfinClient = jellyfinClient,
                    onCollectionClick = { collectionId ->
                        navController.navigate("collection/$collectionId")
                    },
                    onNavigate = handleNavigation,
                    excludeUniverseCollections = universesEnabled,
                    showUniversesInNav = universesEnabled,
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
                    onNavigate = handleNavigation,
                    showUniversesInNav = universesEnabled,
                )
            }

            // Universe collections route (tagged with "universe-collection")
            composable("universes") {
                UniverseCollectionsScreen(
                    jellyfinClient = jellyfinClient,
                    onCollectionClick = { collectionId ->
                        navController.navigate("collection/$collectionId")
                    },
                    onNavigate = handleNavigation,
                    showUniversesInNav = true,
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
                    val route = if (useTrailerFallback) {
                        NavigationHelper.buildSeerrTrailerFallbackRoute(videoKey, title)
                    } else {
                        NavigationHelper.buildSeerrTrailerRoute(videoKey, title)
                    }
                    navController.navigate(route)
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
                    onNavigate = handleNavigation,
                    showUniversesInNav = universesEnabled,
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
                PlayerScreen(
                    itemId = itemId,
                    startPositionMs = startPositionMs?.takeIf { it >= 0 },
                    jellyfinClient = jellyfinClient,
                    appPreferences = tvPreferences,
                    useMpvPlayer = useMpvPlayer,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
