package dev.jausc.myflix.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.screens.*
import dev.jausc.myflix.mobile.ui.theme.MyFlixMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFlixMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyFlixMobileApp()
                }
            }
        }
    }
}

@Composable
fun MyFlixMobileApp() {
    val context = LocalContext.current
    
    val jellyfinClient = remember { JellyfinClient() }
    val appState = remember { AppState(context, jellyfinClient) }
    
    var isInitialized by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        appState.initialize()
        isLoggedIn = appState.isLoggedIn
        isInitialized = true
    }
    
    if (!isInitialized) return

    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
                onLibraryClick = { libraryId, libraryName ->
                    navController.navigate("library/$libraryId/$libraryName")
                },
                onItemClick = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onSearchClick = {
                    navController.navigate("search")
                }
            )
        }

        composable("search") {
            SearchScreen(
                jellyfinClient = jellyfinClient,
                onItemClick = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onBack = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
