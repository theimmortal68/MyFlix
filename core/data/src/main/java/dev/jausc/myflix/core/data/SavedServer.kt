package dev.jausc.myflix.core.data

import kotlinx.serialization.Serializable

/**
 * Represents a saved Jellyfin server configuration.
 *
 * Stored encrypted in SecureServerStore to protect access tokens.
 */
@Serializable
data class SavedServer(
    /** Unique identifier for this server (UUID) */
    val serverId: String,

    /** Display name for the server (from ServerInfo) */
    val serverName: String,

    /** Base URL of the Jellyfin server */
    val serverUrl: String,

    /** Authentication access token */
    val accessToken: String,

    /** Authenticated user ID */
    val userId: String,

    /** Display name of the authenticated user */
    val userName: String,

    /** Timestamp when this server was added (epoch millis) */
    val addedAt: Long,

    /** Timestamp when this server was last used (epoch millis) */
    val lastUsedAt: Long,
) {
    /**
     * Get a display string for the server (userName @ host).
     */
    val displayString: String
        get() {
            val host = serverUrl
                .removePrefix("https://")
                .removePrefix("http://")
                .removeSuffix("/")
            return "$userName @ $host"
        }

    /**
     * Get just the host portion of the server URL.
     */
    val host: String
        get() = serverUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/")
}
