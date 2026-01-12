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
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.mobile.ui.screens.DetailScreen
import dev.jausc.myflix.mobile.ui.screens.HomeScreen
import dev.jausc.myflix.mobile.ui.screens.LibraryScreen
import dev.jausc.myflix.mobile.ui.screens.LoginScreen
import dev.jausc.myflix.mobile.ui.screens.PlayerScreen
import dev.jausc.myflix.mobile.ui.screens.SearchScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrActorDetailScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrDetailScreen
import dev.jausc.myflix.mobile.ui.screens.SeerrHomeScreen
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
            val libraryName = backStackEntry.arguments?.getString("libraryName") ?: ""
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
