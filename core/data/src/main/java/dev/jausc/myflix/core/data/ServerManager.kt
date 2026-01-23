package dev.jausc.myflix.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages multiple Jellyfin server configurations.
 *
 * Provides reactive StateFlows for UI binding and handles:
 * - Server CRUD operations
 * - Active server tracking
 * - Server switching
 */
class ServerManager(context: Context) {

    private val store = SecureServerStore.getInstance(context)

    private val _servers = MutableStateFlow<List<SavedServer>>(emptyList())
    val servers: StateFlow<List<SavedServer>> = _servers.asStateFlow()

    private val _activeServer = MutableStateFlow<SavedServer?>(null)
    val activeServer: StateFlow<SavedServer?> = _activeServer.asStateFlow()

    init {
        // Load initial state from storage
        refreshFromStorage()
    }

    /**
     * Refresh StateFlows from persistent storage.
     */
    private fun refreshFromStorage() {
        _servers.value = store.getServers()
        val activeId = store.getActiveServerId()
        _activeServer.value = activeId?.let { id ->
            _servers.value.find { it.serverId == id }
        }
    }

    /**
     * Add a new server or update an existing one.
     *
     * @param serverUrl Base URL of the server
     * @param serverName Display name for the server
     * @param accessToken Authentication token
     * @param userId Authenticated user ID
     * @param userName Display name of the user
     * @param existingServerId If updating an existing server, provide its ID
     * @return The saved server
     */
    fun addOrUpdateServer(
        serverUrl: String,
        serverName: String,
        accessToken: String,
        userId: String,
        userName: String,
        existingServerId: String? = null,
    ): SavedServer {
        val now = System.currentTimeMillis()

        // Check if we're updating an existing server or if this server URL already exists
        val existingServer = existingServerId?.let { store.getServer(it) }
            ?: _servers.value.find {
                it.serverUrl.equals(serverUrl, ignoreCase = true) &&
                    it.userId == userId
            }

        val server = if (existingServer != null) {
            // Update existing server
            existingServer.copy(
                serverName = serverName,
                serverUrl = serverUrl,
                accessToken = accessToken,
                userId = userId,
                userName = userName,
                lastUsedAt = now,
            )
        } else {
            // Create new server
            SavedServer(
                serverId = UUID.randomUUID().toString(),
                serverName = serverName,
                serverUrl = serverUrl,
                accessToken = accessToken,
                userId = userId,
                userName = userName,
                addedAt = now,
                lastUsedAt = now,
            )
        }

        store.addOrUpdateServer(server)
        refreshFromStorage()
        return server
    }

    /**
     * Remove a server by ID.
     *
     * @return true if a server was removed, false if not found
     */
    fun removeServer(serverId: String): Boolean {
        val existed = _servers.value.any { it.serverId == serverId }
        if (existed) {
            store.removeServer(serverId)
            refreshFromStorage()
        }
        return existed
    }

    /**
     * Set the active server.
     *
     * @param serverId ID of the server to activate
     * @return The activated server, or null if not found
     */
    fun setActiveServer(serverId: String): SavedServer? {
        val server = _servers.value.find { it.serverId == serverId }
        if (server != null) {
            // Update last used timestamp
            val updatedServer = server.copy(lastUsedAt = System.currentTimeMillis())
            store.addOrUpdateServer(updatedServer)
            store.setActiveServerId(serverId)
            refreshFromStorage()
        }
        return _activeServer.value
    }

    /**
     * Clear the active server (disconnect without removing).
     */
    fun clearActiveServer() {
        store.setActiveServerId(null)
        _activeServer.value = null
    }

    /**
     * Get a server by ID.
     */
    fun getServer(serverId: String): SavedServer? =
        _servers.value.find { it.serverId == serverId }

    /**
     * Get all servers.
     */
    fun getAllServers(): List<SavedServer> = _servers.value

    /**
     * Get the currently active server.
     */
    fun getActiveServer(): SavedServer? = _activeServer.value

    /**
     * Check if there are any saved servers.
     */
    fun hasServers(): Boolean = _servers.value.isNotEmpty()

    /**
     * Get the number of saved servers.
     */
    fun serverCount(): Int = _servers.value.size

    /**
     * Update the access token for a server (e.g., after re-authentication).
     */
    fun updateServerToken(serverId: String, newToken: String): SavedServer? {
        val server = _servers.value.find { it.serverId == serverId } ?: return null
        val updatedServer = server.copy(
            accessToken = newToken,
            lastUsedAt = System.currentTimeMillis(),
        )
        store.addOrUpdateServer(updatedServer)
        refreshFromStorage()
        return _servers.value.find { it.serverId == serverId }
    }

    /**
     * Clear all servers (for testing or complete reset).
     */
    fun clearAllServers() {
        store.clearAll()
        _servers.value = emptyList()
        _activeServer.value = null
    }

    companion object {
        @Volatile
        private var INSTANCE: ServerManager? = null

        /**
         * Get singleton instance.
         */
        fun getInstance(context: Context): ServerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServerManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
