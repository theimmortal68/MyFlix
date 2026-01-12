package dev.jausc.myflix.core.common.ui

import dev.jausc.myflix.core.common.model.AuthResponse

/**
 * Server information from discovery.
 */
data class DiscoveredServerInfo(
    val name: String,
    val address: String,
)

/**
 * Validated server information after connection.
 */
data class ValidatedServerInfo(
    val url: String,
    val serverName: String,
    val version: String?,
    val quickConnectEnabled: Boolean,
)

/**
 * Public user information.
 */
data class PublicUserInfo(
    val id: String,
    val name: String,
    val primaryImageTag: String?,
)

/**
 * Authenticator interface for login operations.
 * Abstracts the JellyfinClient and AppState for dependency injection.
 */
interface LoginAuthenticator {
    suspend fun discoverServers(timeoutMs: Long): Result<List<DiscoveredServerInfo>>
    suspend fun connectToServer(address: String): Result<ValidatedServerInfo>
    suspend fun getPublicUsers(serverUrl: String): Result<List<PublicUserInfo>>
    suspend fun login(serverUrl: String, username: String, password: String): Result<AuthResponse>

    /**
     * Called after successful password login.
     * Should configure the client and persist credentials.
     */
    suspend fun onLoginSuccess(
        server: ValidatedServerInfo,
        response: AuthResponse,
        username: String,
        password: String,
    )

    /**
     * Called after successful Quick Connect authentication.
     * Should configure the client and persist credentials.
     */
    suspend fun onQuickConnectSuccess(server: ValidatedServerInfo, response: AuthResponse)
}
