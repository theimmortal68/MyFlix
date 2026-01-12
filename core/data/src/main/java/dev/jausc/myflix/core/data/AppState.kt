package dev.jausc.myflix.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dev.jausc.myflix.core.common.preferences.PreferenceKeys
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PreferenceKeys.DataStore.STORE_NAME
)

class AppState(private val context: Context, val jellyfinClient: JellyfinClient) {
    val isLoggedIn: Boolean get() = jellyfinClient.isAuthenticated

    // Secure credential storage for Seerr integration
    private val secureStore: SecureCredentialStore by lazy {
        SecureCredentialStore.getInstance(context)
    }

    // Stored credentials for Seerr integration (loaded from encrypted storage)
    var username: String? = null
        private set
    var password: String? = null
        private set

    suspend fun initialize() {
        val prefs = context.dataStore.data.first()
        val serverUrl = prefs[PreferenceKeys.DataStore.SERVER_URL]
        val accessToken = prefs[PreferenceKeys.DataStore.ACCESS_TOKEN]
        val userId = prefs[PreferenceKeys.DataStore.USER_ID]
        val deviceId = prefs[PreferenceKeys.DataStore.DEVICE_ID] ?: generateDeviceId().also { saveDeviceId(it) }

        // Load credentials from encrypted storage
        username = secureStore.getUsername()
        password = secureStore.getPassword()

        // Migrate from plain DataStore if present (one-time migration)
        migrateCredentialsIfNeeded(prefs)

        jellyfinClient.deviceId = deviceId
        if (serverUrl != null && accessToken != null && userId != null) {
            jellyfinClient.configure(serverUrl, accessToken, userId, deviceId)
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

    suspend fun login(
        serverUrl: String,
        accessToken: String,
        userId: String,
        username: String? = null,
        password: String? = null,
    ) {
        jellyfinClient.configure(serverUrl, accessToken, userId, jellyfinClient.deviceId)
        this.username = username
        this.password = password

        // Save auth tokens to DataStore (non-sensitive, revocable)
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.DataStore.SERVER_URL] = serverUrl
            prefs[PreferenceKeys.DataStore.ACCESS_TOKEN] = accessToken
            prefs[PreferenceKeys.DataStore.USER_ID] = userId
        }

        // Save credentials to encrypted storage (sensitive)
        secureStore.saveCredentials(username, password)
    }

    suspend fun logout() {
        jellyfinClient.logout()
        username = null
        password = null

        // Clear auth tokens from DataStore
        context.dataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.DataStore.SERVER_URL)
            prefs.remove(PreferenceKeys.DataStore.ACCESS_TOKEN)
            prefs.remove(PreferenceKeys.DataStore.USER_ID)
        }

        // Clear credentials from encrypted storage
        secureStore.clearCredentials()
    }

    private suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { it[PreferenceKeys.DataStore.DEVICE_ID] = id }
    }

    private fun generateDeviceId(): String = "myflix_${java.util.UUID.randomUUID().toString().take(8)}"
}
