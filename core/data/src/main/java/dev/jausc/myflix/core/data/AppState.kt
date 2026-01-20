package dev.jausc.myflix.core.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dev.jausc.myflix.core.common.preferences.PreferenceKeys
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PreferenceKeys.DataStore.STORE_NAME
)

class AppState(private val context: Context, val jellyfinClient: JellyfinClient) {
    val isLoggedIn: Boolean get() = jellyfinClient.isAuthenticated

    // Coroutine scope for fire-and-forget operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Server manager for multi-server support
    val serverManager: ServerManager by lazy {
        ServerManager.getInstance(context)
    }

    // Expose servers StateFlow for UI
    val servers: StateFlow<List<SavedServer>> get() = serverManager.servers
    val activeServer: StateFlow<SavedServer?> get() = serverManager.activeServer

    // Secure credential storage for Seerr integration
    private val secureStore: SecureCredentialStore by lazy {
        SecureCredentialStore.getInstance(context)
    }

    // Stored credentials for Seerr integration (loaded from encrypted storage)
    var username: String? = null
        private set
    var password: String? = null
        private set

    suspend fun initialize(): Result<Unit> = runCatching {
        val prefs = context.dataStore.data.first()
        val deviceId = prefs[PreferenceKeys.DataStore.DEVICE_ID] ?: generateDeviceId().also { saveDeviceId(it) }
        jellyfinClient.deviceId = deviceId

        // Load credentials from encrypted storage
        try {
            username = secureStore.getUsername()
            password = secureStore.getPassword()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load credentials: ${e.message}")
        }

        // Migrate from plain DataStore if present (one-time migration)
        try {
            migrateCredentialsIfNeeded(prefs)
        } catch (e: Exception) {
            Log.w(TAG, "Credential migration failed: ${e.message}")
        }

        // Migrate from single-server to multi-server storage
        try {
            migrateSingleServerIfNeeded(prefs, deviceId)
        } catch (e: Exception) {
            Log.w(TAG, "Single-server migration failed: ${e.message}")
        }

        // Configure JellyfinClient with active server if available
        val active = serverManager.getActiveServer()
        if (active != null) {
            jellyfinClient.configure(active.serverUrl, active.accessToken, active.userId, deviceId)
            jellyfinClient.setServerContext(active.serverId)
            // Register session capabilities for remote control support
            jellyfinClient.registerSessionCapabilities()
        }
    }

    /**
     * One-time migration from plain DataStore to encrypted storage.
     * Removes plain-text credentials after migration.
     */
    private suspend fun migrateCredentialsIfNeeded(prefs: Preferences) {
        val plainUsername = prefs[PreferenceKeys.DataStore.USERNAME_LEGACY]
        val plainPassword = prefs[PreferenceKeys.DataStore.PASSWORD_LEGACY]

        if (plainUsername != null || plainPassword != null) {
            // Migrate to encrypted storage if not already present
            if (username == null && plainUsername != null) {
                username = plainUsername
            }
            if (password == null && plainPassword != null) {
                password = plainPassword
            }

            // Save to encrypted storage
            secureStore.saveCredentials(username, password)

            // Remove plain-text credentials from DataStore
            context.dataStore.edit { editPrefs ->
                editPrefs.remove(PreferenceKeys.DataStore.USERNAME_LEGACY)
                editPrefs.remove(PreferenceKeys.DataStore.PASSWORD_LEGACY)
            }
        }
    }

    /**
     * One-time migration from single-server storage to multi-server storage.
     * Existing single-server users will have their server added to ServerManager.
     */
    private suspend fun migrateSingleServerIfNeeded(prefs: Preferences, deviceId: String) {
        // Skip if already have servers in new storage
        if (serverManager.hasServers()) return

        val serverUrl = prefs[PreferenceKeys.DataStore.SERVER_URL]
        val accessToken = prefs[PreferenceKeys.DataStore.ACCESS_TOKEN]
        val userId = prefs[PreferenceKeys.DataStore.USER_ID]

        // Check if single-server data exists
        if (serverUrl != null && accessToken != null && userId != null) {
            Log.d(TAG, "Migrating single-server to multi-server storage")

            // Try to get server name from server info
            jellyfinClient.configure(serverUrl, accessToken, userId, deviceId)
            val serverName = jellyfinClient.getServerInfo(serverUrl)
                .getOrNull()?.serverName ?: "My Server"

            // Get username from credentials if available
            val userName = username ?: "User"

            // Add server to new multi-server storage
            val server = serverManager.addOrUpdateServer(
                serverUrl = serverUrl,
                serverName = serverName,
                accessToken = accessToken,
                userId = userId,
                userName = userName,
            )
            serverManager.setActiveServer(server.serverId)

            // Remove old single-server keys from DataStore
            context.dataStore.edit { editPrefs ->
                editPrefs.remove(PreferenceKeys.DataStore.SERVER_URL)
                editPrefs.remove(PreferenceKeys.DataStore.ACCESS_TOKEN)
                editPrefs.remove(PreferenceKeys.DataStore.USER_ID)
            }

            Log.d(TAG, "Migration complete: ${server.serverName}")
        }
    }

    /**
     * Login to a server and save it.
     * If the server already exists (same URL and user), updates the token.
     */
    suspend fun login(
        serverUrl: String,
        accessToken: String,
        userId: String,
        serverName: String = "My Server",
        userName: String = "User",
        username: String? = null,
        password: String? = null,
    ) {
        // Add or update server in ServerManager
        val server = serverManager.addOrUpdateServer(
            serverUrl = serverUrl,
            serverName = serverName,
            accessToken = accessToken,
            userId = userId,
            userName = userName,
        )
        serverManager.setActiveServer(server.serverId)

        // Configure JellyfinClient
        jellyfinClient.configure(serverUrl, accessToken, userId, jellyfinClient.deviceId)
        jellyfinClient.setServerContext(server.serverId)

        // Register session capabilities for remote control support
        jellyfinClient.registerSessionCapabilities()

        // Save Seerr credentials
        this.username = username
        this.password = password
        secureStore.saveCredentials(username, password)
    }

    /**
     * Switch to a different saved server.
     *
     * @param serverId ID of the server to switch to
     * @return Result with the server if successful
     */
    fun switchServer(serverId: String): Result<SavedServer> {
        val server = serverManager.setActiveServer(serverId)
            ?: return Result.failure(IllegalArgumentException("Server not found: $serverId"))

        // Configure JellyfinClient with new server
        jellyfinClient.configure(server.serverUrl, server.accessToken, server.userId, jellyfinClient.deviceId)
        jellyfinClient.setServerContext(server.serverId)

        // Register session capabilities for remote control support (fire and forget)
        scope.launch { jellyfinClient.registerSessionCapabilities() }

        Log.d(TAG, "Switched to server: ${server.serverName}")
        return Result.success(server)
    }

    /**
     * Remove a saved server.
     * If removing the active server, will disconnect.
     *
     * @param serverId ID of the server to remove
     * @return true if server was removed
     */
    fun removeServer(serverId: String): Boolean {
        val isActive = serverManager.getActiveServer()?.serverId == serverId
        val removed = serverManager.removeServer(serverId)

        if (removed && isActive) {
            // Disconnected - clear JellyfinClient
            jellyfinClient.logout()
        }

        return removed
    }

    /**
     * Disconnect from the current server without removing it.
     */
    suspend fun logout() {
        jellyfinClient.logout()
        serverManager.clearActiveServer()
        username = null
        password = null
        secureStore.clearCredentials()
    }

    /**
     * Disconnect from current server AND remove it from saved servers.
     */
    suspend fun logoutAndRemove() {
        val activeId = serverManager.getActiveServer()?.serverId
        if (activeId != null) {
            serverManager.removeServer(activeId)
        }
        jellyfinClient.logout()
        username = null
        password = null
        secureStore.clearCredentials()
    }

    private suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { it[PreferenceKeys.DataStore.DEVICE_ID] = id }
    }

    private fun generateDeviceId(): String = "myflix_${java.util.UUID.randomUUID().toString().take(8)}"

    companion object {
        private const val TAG = "AppState"
    }
}
