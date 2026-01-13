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

package dev.jausc.myflix.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.jausc.myflix.core.common.ui.SplashScreen
import dev.jausc.myflix.core.common.util.NavigationHelper
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.data.DebugCredentials
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.mobile.ui.screens.DetailScreen
import dev.jausc.myflix.mobile.ui.screens.HomeScreen
import dev.jausc.myflix.mobile.ui.screens.LibraryScreen
import dev.jausc.myflix.mobile.ui.screens.LoginScreen
import dev.jausc.myflix.mobile.ui.screens.PlayerScreen
import dev.jausc.myflix.mobile.ui.screens.SearchScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrActorDetailScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrCollectionDetailScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDiscoverByGenreScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDiscoverMoviesScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDiscoverTrendingScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDiscoverTvScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDiscoverUpcomingMoviesScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDiscoverUpcomingTvScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDetailScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrHomeScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrRequestsScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrSearchScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrSetupScreen
import dev.jausc.myflix.mobile.ui.screens.SettingsScreen
import dev.jausc.myflix.mobile.ui.theme.MyFlixMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display so content can draw behind system bars
        // This allows us to handle status bar insets manually for the overlay effect
        enableEdgeToEdge()

        setContent {
            MyFlixMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MyFlixMobileContent()
                }
            }
        }
    }
}

@Composable
fun MyFlixMobileContent() {
    val context = LocalContext.current

    val jellyfinClient = remember { JellyfinClient() }
    val appState = remember { AppState(context, jellyfinClient) }
    val mobilePreferences = remember { MobilePreferences.getInstance(context) }
    val seerrClient = remember { SeerrClient() }

    // Collect preferences (only ones used outside HomeScreen)
    val useMpvPlayer by mobilePreferences.useMpvPlayer.collectAsState()
    val seerrEnabled by mobilePreferences.seerrEnabled.collectAsState()
    val seerrUrl by mobilePreferences.seerrUrl.collectAsState()
    val seerrApiKey by mobilePreferences.seerrApiKey.collectAsState()
    val seerrSessionCookie by mobilePreferences.seerrSessionCookie.collectAsState()

    var isInitialized by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var splashFinished by remember { mutableStateOf(false) }
    var isSeerrAuthenticated by remember { mutableStateOf(false) }

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

    // Initialize SeerrClient with stored session cookie or Jellyfin credentials
    LaunchedEffect(seerrUrl, seerrEnabled, seerrSessionCookie, isInitialized, isLoggedIn) {
        // Skip if already authenticated in this session (e.g., from setup screen)
        if (isSeerrAuthenticated) return@LaunchedEffect

        android.util.Log.d(
            "MyFlixSeerr",
            "LaunchedEffect: init=$isInitialized, logged=$isLoggedIn, enabled=$seerrEnabled, url=$seerrUrl, hasCookie=${!seerrSessionCookie.isNullOrBlank()}",
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
                                    user.apiKey?.let { mobilePreferences.setSeerrApiKey(it) }
                                    seerrClient.sessionCookie?.let { mobilePreferences.setSeerrSessionCookie(it) }
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
                    android.util.Log.e("MyFlixSeerr", "Failed to connect to Seerr: ${e.message}")
                }
        } else if (!seerrEnabled) {
            android.util.Log.d("MyFlixSeerr", "Seerr is disabled")
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

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashScreen(
                onFinished = { splashFinished = true },
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
                preferences = mobilePreferences,
                seerrClient = if (isSeerrAuthenticated) seerrClient else null,
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
                onSeerrMediaClick = { mediaType, tmdbId ->
                    navController.navigate("seerr/$mediaType/$tmdbId")
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
            )
        }

        composable("settings") {
            SettingsScreen(
                preferences = mobilePreferences,
                jellyfinClient = jellyfinClient,
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
                preferences = mobilePreferences,
                onItemClick = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onPlayClick = { itemId ->
                    navController.navigate("player/$itemId")
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
                onNavigateToDetail = { relatedItemId ->
                    navController.navigate("detail/$relatedItemId")
                },
                onNavigateToGenre = { genre, libraryType ->
                    // TODO: Navigate to library filtered by genre
                    // For now, navigate back to home
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
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

        // Seerr routes
        composable("seerr") {
            // Require actual authentication - isSeerrAuthenticated is set after successful login
            if (!isSeerrAuthenticated) {
                SeerrSetupScreen(
                    seerrClient = seerrClient,
                    preferences = mobilePreferences,
                    jellyfinUsername = appState.username,
                    jellyfinPassword = appState.password,
                    jellyfinHost = jellyfinClient.serverUrl?.let { extractHost(it) },
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
                    onBack = { navController.popBackStack() },
                    onNavigateSearch = {
                        navController.navigate(NavigationHelper.SEERR_SEARCH_ROUTE)
                    },
                    onNavigateRequests = {
                        navController.navigate(NavigationHelper.SEERR_REQUESTS_ROUTE)
                    },
                    onNavigateDiscoverTrending = {
                        navController.navigate(NavigationHelper.buildSeerrDiscoverRoute("trending"))
                    },
                    onNavigateDiscoverMovies = {
                        navController.navigate(NavigationHelper.buildSeerrDiscoverRoute("movies"))
                    },
                    onNavigateDiscoverTv = {
                        navController.navigate(NavigationHelper.buildSeerrDiscoverRoute("tv"))
                    },
                    onNavigateDiscoverUpcomingMovies = {
                        navController.navigate(NavigationHelper.buildSeerrDiscoverRoute("upcoming_movies"))
                    },
                    onNavigateDiscoverUpcomingTv = {
                        navController.navigate(NavigationHelper.buildSeerrDiscoverRoute("upcoming_tv"))
                    },
                    onNavigateGenre = { genreMediaType, genreId, genreName ->
                        val encodedName = NavigationHelper.encodeNavArg(genreName)
                        navController.navigate("seerr/genre/$genreMediaType/$genreId/$encodedName")
                    },
                )
            }
        }

        composable("seerr/setup") {
            SeerrSetupScreen(
                seerrClient = seerrClient,
                preferences = mobilePreferences,
                jellyfinUsername = appState.username,
                jellyfinPassword = appState.password,
                jellyfinHost = jellyfinClient.serverUrl?.let { extractHost(it) },
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
    }
}

/**
 * Extract host from URL (removes protocol and port).
 */
private fun extractHost(url: String): String {
    return url
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore(":")
        .substringBefore("/")
}
