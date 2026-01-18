package dev.jausc.myflix.core.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Secure storage for saved server configurations using EncryptedSharedPreferences.
 *
 * Uses AES-256 encryption with a master key stored in Android Keystore.
 * Server credentials (including access tokens) are encrypted at rest.
 */
class SecureServerStore(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save the list of servers.
     */
    fun saveServers(servers: List<SavedServer>) {
        try {
            val jsonString = json.encodeToString(servers)
            encryptedPrefs.edit()
                .putString(KEY_SERVERS, jsonString)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save servers", e)
        }
    }

    /**
     * Get all saved servers.
     */
    fun getServers(): List<SavedServer> {
        return try {
            val jsonString = encryptedPrefs.getString(KEY_SERVERS, null)
            if (jsonString != null) {
                json.decodeFromString<List<SavedServer>>(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load servers", e)
            emptyList()
        }
    }

    /**
     * Set the active server ID.
     */
    fun setActiveServerId(serverId: String?) {
        encryptedPrefs.edit().apply {
            if (serverId != null) {
                putString(KEY_ACTIVE_SERVER_ID, serverId)
            } else {
                remove(KEY_ACTIVE_SERVER_ID)
            }
            apply()
        }
    }

    /**
     * Get the active server ID.
     */
    fun getActiveServerId(): String? =
        encryptedPrefs.getString(KEY_ACTIVE_SERVER_ID, null)

    /**
     * Add or update a server in the list.
     * If a server with the same serverId exists, it will be replaced.
     */
    fun addOrUpdateServer(server: SavedServer) {
        val servers = getServers().toMutableList()
        val existingIndex = servers.indexOfFirst { it.serverId == server.serverId }
        if (existingIndex >= 0) {
            servers[existingIndex] = server
        } else {
            servers.add(server)
        }
        saveServers(servers)
    }

    /**
     * Remove a server by ID.
     */
    fun removeServer(serverId: String) {
        val servers = getServers().filter { it.serverId != serverId }
        saveServers(servers)

        // If removing the active server, clear active server ID
        if (getActiveServerId() == serverId) {
            setActiveServerId(null)
        }
    }

    /**
     * Get a server by ID.
     */
    fun getServer(serverId: String): SavedServer? =
        getServers().find { it.serverId == serverId }

    /**
     * Check if any servers are saved.
     */
    fun hasServers(): Boolean = getServers().isNotEmpty()

    /**
     * Clear all saved servers.
     */
    fun clearAll() {
        encryptedPrefs.edit()
            .remove(KEY_SERVERS)
            .remove(KEY_ACTIVE_SERVER_ID)
            .apply()
    }

    companion object {
        private const val TAG = "SecureServerStore"
        private const val PREFS_NAME = "myflix_secure_servers"
        private const val KEY_SERVERS = "servers"
        private const val KEY_ACTIVE_SERVER_ID = "active_server_id"

        @Volatile
        private var INSTANCE: SecureServerStore? = null

        /**
         * Get singleton instance.
         */
        fun getInstance(context: Context): SecureServerStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureServerStore(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
