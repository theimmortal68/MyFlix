package dev.jausc.myflix.core.data

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Reads debug credentials from a local file for auto-login during development.
 *
 * The credentials file should be placed at the project root as `debug.credentials`
 * and is gitignored for security.
 *
 * File format (one value per line):
 * ```
 * server=http://your-server:8096
 * username=your_username
 * password=your_password
 * ```
 */
object DebugCredentials {
    private const val TAG = "DebugCredentials"
    private const val FILENAME = "debug.credentials"

    data class Credentials(
        val server: String,
        val username: String,
        val password: String,
    )

    /**
     * Attempts to read debug credentials from the app's assets or external storage.
     * Returns null if credentials file doesn't exist or is invalid.
     *
     * Looks for credentials in this order:
     * 1. App's assets (bundled in APK)
     * 2. App's files directory (for runtime placement via adb push)
     */
    fun read(context: Context): Credentials? {
        // Try assets first (bundled in APK during debug builds)
        try {
            val inputStream = context.assets.open(FILENAME)
            val content = inputStream.bufferedReader().use { it.readText() }
            parseCredentials(content)?.let {
                Log.d(TAG, "Loaded credentials from assets")
                return it
            }
        } catch (_: Exception) {
            // Asset not found, try external
        }

        // Try app's files directory (can be pushed via adb)
        val file = File(context.filesDir, FILENAME)
        if (file.exists()) {
            try {
                val content = file.readText()
                parseCredentials(content)?.let {
                    Log.d(TAG, "Loaded credentials from files dir")
                    return it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read credentials from files dir", e)
            }
        }

        // Try external files directory as fallback
        val externalFile = context.getExternalFilesDir(null)?.let { File(it, FILENAME) }
        if (externalFile?.exists() == true) {
            try {
                val content = externalFile.readText()
                parseCredentials(content)?.let {
                    Log.d(TAG, "Loaded credentials from external files dir")
                    return it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read credentials from external files dir", e)
            }
        }

        Log.d(TAG, "No debug credentials found")
        return null
    }

    private fun parseCredentials(content: String): Credentials? {
        val lines = content.lines()
        var server: String? = null
        var username: String? = null
        var password: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val parts = trimmed.split("=", limit = 2)
            if (parts.size != 2) continue

            when (parts[0].trim().lowercase()) {
                "server" -> server = parts[1].trim()
                "username" -> username = parts[1].trim()
                "password" -> password = parts[1].trim()
            }
        }

        return if (server != null && username != null && password != null) {
            Credentials(server, username, password)
        } else {
            Log.w(TAG, "Incomplete credentials: server=$server, username=$username, password=${password?.let { "***" }}")
            null
        }
    }
}
