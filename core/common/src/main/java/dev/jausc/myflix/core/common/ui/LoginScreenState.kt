package dev.jausc.myflix.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.jausc.myflix.core.common.model.AuthResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for LoginScreen.
 * Manages server discovery, connection, and authentication state across TV and mobile platforms.
 *
 * Note: Each platform may have different login flows/steps, but this state manages the
 * common elements: discovered servers, connected server, loading states, and errors.
 */
@Stable
class LoginScreenState(
    private val authenticator: LoginAuthenticator,
    private val scope: CoroutineScope,
) {
    // Server discovery state
    var discoveredServers by mutableStateOf<List<DiscoveredServerInfo>>(emptyList())
        private set

    var isSearching by mutableStateOf(false)
        private set

    // Connection state
    var connectedServer by mutableStateOf<ValidatedServerInfo?>(null)
        private set

    var isConnecting by mutableStateOf(false)
        private set

    var publicUsers by mutableStateOf<List<PublicUserInfo>>(emptyList())
        private set

    // Authentication state
    var isAuthenticating by mutableStateOf(false)
        private set

    // Error state
    var error by mutableStateOf<String?>(null)
        private set

    /**
     * Whether we have a connected server.
     */
    val hasConnectedServer: Boolean
        get() = connectedServer != null

    /**
     * Whether Quick Connect is available on the connected server.
     */
    val isQuickConnectAvailable: Boolean
        get() = connectedServer?.quickConnectEnabled == true

    /**
     * Clear the current error.
     */
    fun clearError() {
        error = null
    }

    /**
     * Discover servers on the local network.
     */
    fun discoverServers(timeoutMs: Long = 5000L) {
        scope.launch {
            isSearching = true
            error = null

            authenticator.discoverServers(timeoutMs)
                .onSuccess { servers -> discoveredServers = servers }
                .onFailure { error = it.message }

            isSearching = false
        }
    }

    /**
     * Connect to a server by address.
     *
     * @param address Server address (IP or hostname)
     * @param onSuccess Callback when connection succeeds, receives the server and public users
     */
    fun connectToServer(
        address: String,
        onSuccess: (ValidatedServerInfo, List<PublicUserInfo>) -> Unit = { _, _ -> },
    ) {
        if (isConnecting) return

        scope.launch {
            isConnecting = true
            error = null

            authenticator.connectToServer(address)
                .onSuccess { server ->
                    connectedServer = server
                    // Fetch public users
                    val users = authenticator.getPublicUsers(server.url)
                        .getOrDefault(emptyList())
                    publicUsers = users
                    onSuccess(server, users)
                }
                .onFailure { error = it.message ?: "Connection failed" }

            isConnecting = false
        }
    }

    /**
     * Disconnect from the current server and reset state.
     */
    fun disconnectServer() {
        connectedServer = null
        publicUsers = emptyList()
    }

    /**
     * Login with username and password.
     *
     * @param username Username
     * @param password Password
     * @param onSuccess Callback when login succeeds
     */
    fun login(
        username: String,
        password: String,
        onSuccess: () -> Unit,
    ) {
        val server = connectedServer ?: return

        if (username.isBlank()) {
            error = "Please enter a username"
            return
        }

        scope.launch {
            isAuthenticating = true
            error = null

            authenticator.login(server.url, username, password)
                .onSuccess { response ->
                    authenticator.onLoginSuccess(server, response, username, password)
                    onSuccess()
                }
                .onFailure { e ->
                    error = when {
                        e.message?.contains("401") == true -> "Invalid username or password"
                        e.message?.contains("timeout", true) == true -> "Connection timed out"
                        else -> e.message ?: "Login failed"
                    }
                }

            isAuthenticating = false
        }
    }

    /**
     * Handle Quick Connect authentication success.
     *
     * @param response The auth response from Quick Connect
     * @param onSuccess Callback when processing succeeds
     */
    fun handleQuickConnectSuccess(
        response: AuthResponse,
        onSuccess: () -> Unit,
    ) {
        val server = connectedServer ?: return

        scope.launch {
            authenticator.onQuickConnectSuccess(server, response)
            onSuccess()
        }
    }
}

/**
 * Server information from discovery.
 */
data class DiscoveredServerInfo(
    val name: String,
    val address: String,
)

/**
 * Validated server information after connection.
 */
data class ValidatedServerInfo(
    val url: String,
    val serverName: String,
    val version: String?,
    val quickConnectEnabled: Boolean,
)

/**
 * Public user information.
 */
data class PublicUserInfo(
    val id: String,
    val name: String,
    val primaryImageTag: String?,
)

/**
 * Authenticator interface for login operations.
 * Abstracts the JellyfinClient and AppState for dependency injection.
 */
interface LoginAuthenticator {
    suspend fun discoverServers(timeoutMs: Long): Result<List<DiscoveredServerInfo>>
    suspend fun connectToServer(address: String): Result<ValidatedServerInfo>
    suspend fun getPublicUsers(serverUrl: String): Result<List<PublicUserInfo>>
    suspend fun login(serverUrl: String, username: String, password: String): Result<AuthResponse>

    /**
     * Called after successful password login.
     * Should configure the client and persist credentials.
     */
    suspend fun onLoginSuccess(
        server: ValidatedServerInfo,
        response: AuthResponse,
        username: String,
        password: String,
    )

    /**
     * Called after successful Quick Connect authentication.
     * Should configure the client and persist credentials.
     */
    suspend fun onQuickConnectSuccess(server: ValidatedServerInfo, response: AuthResponse)
}

/**
 * Creates and remembers a [LoginScreenState].
 *
 * @param authenticator Authenticator for login operations
 * @return A [LoginScreenState] for managing login UI state
 */
@Composable
fun rememberLoginScreenState(
    authenticator: LoginAuthenticator,
): LoginScreenState {
    val scope = rememberCoroutineScope()
    return remember {
        LoginScreenState(authenticator, scope)
    }
}
