package dev.jausc.myflix.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "myflix_prefs")

class AppState(private val context: Context, val jellyfinClient: JellyfinClient) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    }

    val isLoggedIn: Boolean get() = jellyfinClient.isAuthenticated

    suspend fun initialize() {
        val prefs = context.dataStore.data.first()
        val serverUrl = prefs[KEY_SERVER_URL]
        val accessToken = prefs[KEY_ACCESS_TOKEN]
        val userId = prefs[KEY_USER_ID]
        val deviceId = prefs[KEY_DEVICE_ID] ?: generateDeviceId().also { saveDeviceId(it) }
        
        jellyfinClient.deviceId = deviceId
        if (serverUrl != null && accessToken != null && userId != null) {
            jellyfinClient.configure(serverUrl, accessToken, userId, deviceId)
        }
    }

    suspend fun login(serverUrl: String, accessToken: String, userId: String) {
        jellyfinClient.configure(serverUrl, accessToken, userId, jellyfinClient.deviceId)
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun logout() {
        jellyfinClient.logout()
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SERVER_URL); prefs.remove(KEY_ACCESS_TOKEN); prefs.remove(KEY_USER_ID)
        }
    }

    private suspend fun saveDeviceId(id: String) { context.dataStore.edit { it[KEY_DEVICE_ID] = id } }
    private fun generateDeviceId(): String = "myflix_${java.util.UUID.randomUUID().toString().take(8)}"
}
