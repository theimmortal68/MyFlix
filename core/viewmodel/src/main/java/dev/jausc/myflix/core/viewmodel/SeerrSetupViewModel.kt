package dev.jausc.myflix.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Connection status for Seerr server.
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * Authentication method for Seerr.
 */
enum class AuthMethod {
    JELLYFIN,
    LOCAL,
    API_KEY,
}

/**
 * UI state for Seerr setup screen.
 */
data class SeerrSetupUiState(
    val isLoading: Boolean = false,
    val serverUrl: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val authMethod: AuthMethod = AuthMethod.JELLYFIN,
    val username: String = "",
    val password: String = "",
    val apiKey: String = "",
    val error: String? = null,
    val isAuthenticating: Boolean = false,
    val user: SeerrUser? = null,
    val serverVersion: String? = null,
    val isDetectingServer: Boolean = false,
    val detectedServerUrl: String? = null,
)

/**
 * ViewModel for Seerr setup and configuration.
 * Handles server detection, connection, and authentication.
 */
class SeerrSetupViewModel(
    private val seerrRepository: SeerrRepository,
) : ViewModel() {

    /**
     * Factory for creating SeerrSetupViewModel with manual dependency injection.
     */
    class Factory(
        private val seerrRepository: SeerrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeerrSetupViewModel(seerrRepository) as T
    }

    private val _uiState = MutableStateFlow(SeerrSetupUiState())
    val uiState: StateFlow<SeerrSetupUiState> = _uiState.asStateFlow()

    /** Authentication state from repository */
    val isAuthenticated = seerrRepository.isAuthenticated

    /** Current user from repository */
    val currentUser = seerrRepository.currentUser

    companion object {
        private const val TAG = "SeerrSetupViewModel"
    }

    init {
        // Initialize with current state from repository
        val existingUrl = seerrRepository.baseUrl
        val existingUser = seerrRepository.currentUser.value
        if (existingUrl != null) {
            _uiState.update {
                it.copy(
                    serverUrl = existingUrl,
                    connectionStatus = if (seerrRepository.isAuthenticated.value) {
                        ConnectionStatus.CONNECTED
                    } else {
                        ConnectionStatus.DISCONNECTED
                    },
                    user = existingUser,
                )
            }
        }
    }

    /**
     * Set the server URL.
     *
     * @param url Seerr server URL
     */
    fun setServerUrl(url: String) {
        _uiState.update {
            it.copy(
                serverUrl = url,
                connectionStatus = ConnectionStatus.DISCONNECTED,
                error = null,
            )
        }
    }

    /**
     * Auto-detect Seerr server URL based on Jellyfin host.
     *
     * @param jellyfinHost Jellyfin server hostname/IP
     */
    fun detectServer(jellyfinHost: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetectingServer = true,
                    error = null,
                    detectedServerUrl = null,
                )
            }

            seerrRepository.detectServer(jellyfinHost)
                .onSuccess { detectedUrl ->
                    _uiState.update {
                        it.copy(
                            isDetectingServer = false,
                            detectedServerUrl = detectedUrl,
                            serverUrl = detectedUrl,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Server detection failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isDetectingServer = false,
                            error = "Could not auto-detect Seerr server",
                        )
                    }
                }
        }
    }

    /**
     * Connect to the configured Seerr server.
     */
    fun connectToServer() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Server URL is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTING,
                    error = null,
                )
            }

            seerrRepository.connectToServer(url)
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.CONNECTED,
                            serverVersion = status.version,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Connection failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.ERROR,
                            error = error.message ?: "Failed to connect to server",
                        )
                    }
                }
        }
    }

    /**
     * Set the authentication method.
     *
     * @param method Authentication method to use
     */
    fun setAuthMethod(method: AuthMethod) {
        _uiState.update { it.copy(authMethod = method, error = null) }
    }

    /**
     * Set credentials for username/password authentication.
     *
     * @param username Username or email
     * @param password Password
     */
    fun setCredentials(username: String, password: String) {
        _uiState.update {
            it.copy(
                username = username,
                password = password,
                error = null,
            )
        }
    }

    /**
     * Set API key for API key authentication.
     *
     * @param apiKey Seerr API key
     */
    fun setApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey, error = null) }
    }

    /**
     * Perform login based on selected authentication method.
     * Routes to the appropriate login method.
     *
     * @param jellyfinHost Optional Jellyfin host for Jellyfin auth
     */
    fun login(jellyfinHost: String? = null) {
        val state = _uiState.value

        when (state.authMethod) {
            AuthMethod.JELLYFIN -> loginWithJellyfin(jellyfinHost)
            AuthMethod.LOCAL -> loginWithLocal()
            AuthMethod.API_KEY -> loginWithApiKey()
        }
    }

    /**
     * Login using Jellyfin credentials.
     */
    private fun loginWithJellyfin(jellyfinHost: String?) {
        val state = _uiState.value

        if (state.username.isBlank()) {
            _uiState.update { it.copy(error = "Username is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, error = null) }

            seerrRepository.loginWithJellyfin(
                username = state.username,
                password = state.password,
                jellyfinHost = jellyfinHost,
            )
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            user = user,
                            connectionStatus = ConnectionStatus.CONNECTED,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Jellyfin login failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            error = error.message ?: "Authentication failed",
                        )
                    }
                }
        }
    }

    /**
     * Login using local account credentials.
     */
    private fun loginWithLocal() {
        val state = _uiState.value

        if (state.username.isBlank()) {
            _uiState.update { it.copy(error = "Email is required") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "Password is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, error = null) }

            seerrRepository.loginWithLocal(
                email = state.username,
                password = state.password,
            )
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            user = user,
                            connectionStatus = ConnectionStatus.CONNECTED,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Local login failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            error = error.message ?: "Authentication failed",
                        )
                    }
                }
        }
    }

    /**
     * Login using API key.
     */
    private fun loginWithApiKey() {
        val state = _uiState.value

        if (state.apiKey.isBlank()) {
            _uiState.update { it.copy(error = "API key is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, error = null) }

            seerrRepository.loginWithApiKey(state.apiKey)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            user = user,
                            connectionStatus = ConnectionStatus.CONNECTED,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "API key login failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            error = error.message ?: "Authentication failed",
                        )
                    }
                }
        }
    }

    /**
     * Logout from Seerr.
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            seerrRepository.logout()
                .onSuccess {
                    _uiState.update {
                        SeerrSetupUiState(
                            serverUrl = it.serverUrl,
                            connectionStatus = ConnectionStatus.DISCONNECTED,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Logout failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Logout failed",
                        )
                    }
                }
        }
    }

    /**
     * Reset all setup state.
     */
    fun reset() {
        seerrRepository.reset()
        _uiState.update { SeerrSetupUiState() }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
