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

    // Stored credentials for Seerr integration
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

        // Load stored credentials for Seerr
        username = prefs[PreferenceKeys.DataStore.USERNAME]
        password = prefs[PreferenceKeys.DataStore.PASSWORD]

        jellyfinClient.deviceId = deviceId
        if (serverUrl != null && accessToken != null && userId != null) {
            jellyfinClient.configure(serverUrl, accessToken, userId, deviceId)
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
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.DataStore.SERVER_URL] = serverUrl
            prefs[PreferenceKeys.DataStore.ACCESS_TOKEN] = accessToken
            prefs[PreferenceKeys.DataStore.USER_ID] = userId
            if (username != null) prefs[PreferenceKeys.DataStore.USERNAME] = username
            if (password != null) prefs[PreferenceKeys.DataStore.PASSWORD] = password
        }
    }

    suspend fun logout() {
        jellyfinClient.logout()
        username = null
        password = null
        context.dataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.DataStore.SERVER_URL)
            prefs.remove(PreferenceKeys.DataStore.ACCESS_TOKEN)
            prefs.remove(PreferenceKeys.DataStore.USER_ID)
            prefs.remove(PreferenceKeys.DataStore.USERNAME)
            prefs.remove(PreferenceKeys.DataStore.PASSWORD)
        }
    }

    private suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { it[PreferenceKeys.DataStore.DEVICE_ID] = id }
    }

    private fun generateDeviceId(): String = "myflix_${java.util.UUID.randomUUID().toString().take(8)}"
}
