package dev.jausc.myflix.core.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for sensitive credentials using EncryptedSharedPreferences.
 *
 * Uses AES-256 encryption with a master key stored in Android Keystore.
 * Credentials are encrypted at rest and only accessible by this app.
 *
 * Used for storing Jellyfin credentials needed for Seerr integration.
 */
class SecureCredentialStore(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setKeyGenParameterSpec(
            KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save credentials securely.
     */
    fun saveCredentials(username: String?, password: String?) {
        encryptedPrefs.edit().apply {
            if (username != null) {
                putString(KEY_USERNAME, username)
            } else {
                remove(KEY_USERNAME)
            }
            if (password != null) {
                putString(KEY_PASSWORD, password)
            } else {
                remove(KEY_PASSWORD)
            }
            apply()
        }
    }

    /**
     * Get stored username.
     */
    fun getUsername(): String? = encryptedPrefs.getString(KEY_USERNAME, null)

    /**
     * Get stored password.
     */
    fun getPassword(): String? = encryptedPrefs.getString(KEY_PASSWORD, null)

    /**
     * Check if credentials are stored.
     */
    fun hasCredentials(): Boolean =
        encryptedPrefs.contains(KEY_USERNAME) && encryptedPrefs.contains(KEY_PASSWORD)

    /**
     * Clear all stored credentials.
     */
    fun clearCredentials() {
        encryptedPrefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "myflix_secure_credentials"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"

        @Volatile
        private var INSTANCE: SecureCredentialStore? = null

        /**
         * Get singleton instance.
         */
        fun getInstance(context: Context): SecureCredentialStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureCredentialStore(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
