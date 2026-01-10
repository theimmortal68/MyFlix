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
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.tv.ui.screens.*
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
    
    var isInitialized by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var splashFinished by remember { mutableStateOf(false) }
    
    // Collect preferences
    val hideWatchedFromRecent by tvPreferences.hideWatchedFromRecent.collectAsState()
    val useMpvPlayer by tvPreferences.useMpvPlayer.collectAsState()
    
    LaunchedEffect(Unit) {
        appState.initialize()
        isLoggedIn = appState.isLoggedIn
        isInitialized = true
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
            .background(TvColors.Background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "splash"
        ) {
            composable("splash") {
                SplashScreen(
                    onFinished = { splashFinished = true }
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
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    jellyfinClient = jellyfinClient,
                    hideWatchedFromRecent = hideWatchedFromRecent,
                    onLibraryClick = { libraryId, libraryName ->
                        navController.navigate("library/$libraryId/$libraryName")
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
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
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
                    }
                )
            }
            
            composable("settings") {
                PreferencesScreen(
                    preferences = tvPreferences,
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
                    }
                )
            }

            composable(
                route = "library/{libraryId}/{libraryName}",
                arguments = listOf(
                    navArgument("libraryId") { type = NavType.StringType },
                    navArgument("libraryName") { type = NavType.StringType }
                )
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
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                DetailScreen(
                    itemId = itemId,
                    jellyfinClient = jellyfinClient,
                    onPlayClick = { navController.navigate("player/$itemId") },
                    onEpisodeClick = { episodeId ->
                        navController.navigate("player/$episodeId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "player/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                PlayerScreen(
                    itemId = itemId,
                    jellyfinClient = jellyfinClient,
                    useMpvPlayer = useMpvPlayer,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
