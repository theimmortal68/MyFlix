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
import dev.jausc.myflix.core.common.ui.SplashScreen
import dev.jausc.myflix.core.common.ui.SplashScreenTvConfig
import dev.jausc.myflix.core.common.util.NavigationHelper
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.tv.ui.screens.DetailScreen
import dev.jausc.myflix.tv.ui.screens.HomeScreen
import dev.jausc.myflix.tv.ui.screens.LibraryScreen
import dev.jausc.myflix.tv.ui.screens.LoginScreen
import dev.jausc.myflix.tv.ui.screens.PlayerScreen
import dev.jausc.myflix.tv.ui.screens.PreferencesScreen
import dev.jausc.myflix.tv.ui.screens.SearchScreen
import dev.jausc.myflix.tv.ui.screens.SeerrActorDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrCollectionDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverMoviesScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverTrendingScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverTvScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDiscoverWatchlistScreen
import dev.jausc.myflix.tv.ui.screens.SeerrDetailScreen
import dev.jausc.myflix.tv.ui.screens.SeerrHomeScreen
import dev.jausc.myflix.tv.ui.screens.SeerrRequestsScreen
import dev.jausc.myflix.tv.ui.screens.SeerrSearchScreen
import dev.jausc.myflix.tv.ui.screens.SeerrSetupScreen
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

    // Collect preferences (only ones used outside HomeScreen)
    val useMpvPlayer by tvPreferences.useMpvPlayer.collectAsState()
    val seerrEnabled by tvPreferences.seerrEnabled.collectAsState()
    val seerrUrl by tvPreferences.seerrUrl.collectAsState()
    val seerrApiKey by tvPreferences.seerrApiKey.collectAsState()
    val seerrSessionCookie by tvPreferences.seerrSessionCookie.collectAsState()

    LaunchedEffect(Unit) {
        appState.initialize()
        isLoggedIn = appState.isLoggedIn
        isInitialized = true
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
                    onLibraryClick = { libraryId, libraryName ->
                        navController.navigate(NavigationHelper.buildLibraryRoute(libraryId, libraryName))
                    },
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onPlayClick = { itemId ->
                        navController.navigate("player/$itemId")
                    },
                    onSearchClick = {
                        navController.navigate("search")
                    },
                    onDiscoverClick = {
                        navController.navigate("seerr")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                )
            }

            composable("search") {
                SearchScreen(
                    jellyfinClient = jellyfinClient,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onNavigateMovies = {
                        navController.navigate("home")
                    },
                    onNavigateShows = {
                        navController.navigate("home")
                    },
                    onNavigateSettings = {
                        navController.navigate("settings")
                    },
                )
            }

            composable("settings") {
                PreferencesScreen(
                    preferences = tvPreferences,
                    jellyfinClient = jellyfinClient,
                    onNavigateHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onNavigateSearch = {
                        navController.navigate("search")
                    },
                    onNavigateMovies = {
                        navController.navigate("home")
                    },
                    onNavigateShows = {
                        navController.navigate("home")
                    },
                    onNavigateDiscover = {
                        navController.navigate("seerr")
                    },
                    onNavigateLibrary = { libraryId, libraryName ->
                        navController.navigate(NavigationHelper.buildLibraryRoute(libraryId, libraryName))
                    },
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
                        onNavigateHome = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateSearch = {
                            navController.navigate("search")
                        },
                        onNavigateMovies = {
                            navController.navigate("home")
                        },
                        onNavigateShows = {
                            navController.navigate("home")
                        },
                        onNavigateSettings = {
                            navController.navigate("settings")
                        },
                        jellyfinClient = jellyfinClient,
                        onNavigateLibrary = { libraryId, libraryName ->
                            navController.navigate(NavigationHelper.buildLibraryRoute(libraryId, libraryName))
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
                    onBack = { navController.popBackStack() },
                    onActorClick = { personId ->
                        navController.navigate("seerr/person/$personId")
                    },
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
                    "watchlist" -> SeerrDiscoverWatchlistScreen(
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
                route = "library/{libraryId}/{libraryName}",
                arguments = listOf(
                    navArgument("libraryId") { type = NavType.StringType },
                    navArgument("libraryName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val libraryId = backStackEntry.arguments?.getString("libraryId") ?: return@composable
                val libraryNameEncoded = backStackEntry.arguments?.getString("libraryName") ?: ""
                val libraryName = NavigationHelper.decodeNavArg(libraryNameEncoded)
                LibraryScreen(
                    libraryId = libraryId,
                    libraryName = libraryName,
                    jellyfinClient = jellyfinClient,
                    onItemClick = { itemId ->
                        navController.navigate("detail/$itemId")
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
                    onPlayClick = { navController.navigate("player/$itemId") },
                    onEpisodeClick = { episodeId ->
                        navController.navigate("player/$episodeId")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "player/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                PlayerScreen(
                    itemId = itemId,
                    jellyfinClient = jellyfinClient,
                    useMpvPlayer = useMpvPlayer,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
