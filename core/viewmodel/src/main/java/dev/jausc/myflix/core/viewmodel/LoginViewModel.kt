package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.AuthResponse
import dev.jausc.myflix.core.common.ui.DiscoveredServerInfo
import dev.jausc.myflix.core.common.ui.LoginAuthenticator
import dev.jausc.myflix.core.common.ui.PublicUserInfo
import dev.jausc.myflix.core.common.ui.ValidatedServerInfo
import dev.jausc.myflix.core.data.AppState
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the login screen.
 */
data class LoginUiState(
    // Server discovery state
    val discoveredServers: List<DiscoveredServerInfo> = emptyList(),
    val isSearching: Boolean = false,
    // Connection state
    val connectedServer: ValidatedServerInfo? = null,
    val isConnecting: Boolean = false,
    val publicUsers: List<PublicUserInfo> = emptyList(),
    // Authentication state
    val isAuthenticating: Boolean = false,
    // Error state
    val error: String? = null,
) {
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
}

/**
 * Shared ViewModel for the login screen.
 * Manages server discovery, connection, and authentication state.
 */
class LoginViewModel(
    private val authenticator: LoginAuthenticator,
) : ViewModel() {

    /**
     * Factory for creating LoginViewModel with manual dependency injection.
     */
    class Factory(
        private val jellyfinClient: JellyfinClient,
        private val appState: AppState,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val authenticator = DefaultLoginAuthenticator(jellyfinClient, appState)
            return LoginViewModel(authenticator) as T
        }
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Clear the current error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Discover servers on the local network.
     */
    fun discoverServers(timeoutMs: Long = 5000L) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            authenticator.discoverServers(timeoutMs)
                .onSuccess { servers ->
                    _uiState.update { it.copy(discoveredServers = servers) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }

            _uiState.update { it.copy(isSearching = false) }
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
        if (_uiState.value.isConnecting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, error = null) }

            authenticator.connectToServer(address)
                .onSuccess { server ->
                    // Fetch public users
                    val users = authenticator.getPublicUsers(server.url)
                        .getOrDefault(emptyList())
                    _uiState.update {
                        it.copy(
                            connectedServer = server,
                            publicUsers = users,
                        )
                    }
                    onSuccess(server, users)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Connection failed") }
                }

            _uiState.update { it.copy(isConnecting = false) }
        }
    }

    /**
     * Disconnect from the current server and reset state.
     */
    fun disconnectServer() {
        _uiState.update {
            it.copy(
                connectedServer = null,
                publicUsers = emptyList(),
            )
        }
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
        val server = _uiState.value.connectedServer ?: return

        if (username.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a username") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, error = null) }

            authenticator.login(server.url, username, password)
                .onSuccess { response ->
                    authenticator.onLoginSuccess(server, response, username, password)
                    onSuccess()
                }
                .onFailure { e ->
                    val errorMessage = when {
                        e.message?.contains("401") == true -> "Invalid username or password"
                        e.message?.contains("timeout", true) == true -> "Connection timed out"
                        else -> e.message ?: "Login failed"
                    }
                    _uiState.update { it.copy(error = errorMessage) }
                }

            _uiState.update { it.copy(isAuthenticating = false) }
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
        val server = _uiState.value.connectedServer ?: return

        viewModelScope.launch {
            authenticator.onQuickConnectSuccess(server, response)
            onSuccess()
        }
    }
}

/**
 * Default implementation of LoginAuthenticator.
 * Bridges the LoginViewModel to JellyfinClient and AppState.
 */
internal class DefaultLoginAuthenticator(
    private val jellyfinClient: JellyfinClient,
    private val appState: AppState,
) : LoginAuthenticator {
    override suspend fun discoverServers(timeoutMs: Long): Result<List<DiscoveredServerInfo>> {
        return runCatching {
            jellyfinClient.discoverServers(timeoutMs).map { server ->
                DiscoveredServerInfo(
                    name = server.name,
                    address = server.address,
                )
            }
        }
    }

    override suspend fun connectToServer(address: String): Result<ValidatedServerInfo> {
        return jellyfinClient.connectToServer(address).map { server ->
            ValidatedServerInfo(
                url = server.url,
                serverName = server.serverInfo.serverName,
                version = server.serverInfo.version,
                quickConnectEnabled = server.quickConnectEnabled,
            )
        }
    }

    override suspend fun getPublicUsers(serverUrl: String): Result<List<PublicUserInfo>> {
        return jellyfinClient.getPublicUsers(serverUrl).map { users ->
            users.map { user ->
                PublicUserInfo(
                    id = user.id,
                    name = user.name,
                    primaryImageTag = user.primaryImageTag,
                )
            }
        }
    }

    override suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
    ): Result<AuthResponse> =
        jellyfinClient.login(serverUrl, username, password)

    override suspend fun onLoginSuccess(
        server: ValidatedServerInfo,
        response: AuthResponse,
        username: String,
        password: String,
    ) {
        jellyfinClient.configure(
            server.url,
            response.accessToken,
            response.user.id,
            jellyfinClient.deviceId,
        )
        appState.login(server.url, response.accessToken, response.user.id, username, password)
    }

    override suspend fun onQuickConnectSuccess(server: ValidatedServerInfo, response: AuthResponse) {
        jellyfinClient.configure(
            server.url,
            response.accessToken,
            response.user.id,
            jellyfinClient.deviceId,
        )
        appState.login(server.url, response.accessToken, response.user.id)
    }
}
