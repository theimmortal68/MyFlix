package dev.jausc.myflix.core.network

import android.content.Context
import dev.jausc.myflix.core.common.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

// ==================== Data Models ====================

@Serializable
data class DiscoveredServer(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Address") val address: String,
    @SerialName("EndpointAddress") val endpointAddress: String? = null,
    @SerialName("LocalAddress") val localAddress: String? = null
)

@Serializable
data class QuickConnectState(
    @SerialName("Secret") val secret: String,
    @SerialName("Code") val code: String,
    @SerialName("Authenticated") val authenticated: Boolean = false,
    @SerialName("DateAdded") val dateAdded: String? = null
)

class JellyfinClient(private val context: Context? = null) {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true 
    }
    
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { 
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        install(HttpRedirect) {
            checkHttpMethod = false  // Allow redirects for POST requests (fixes Quick Connect)
        }
        defaultRequest { contentType(ContentType.Application.Json) }
    }

    var serverUrl: String? = null
    var accessToken: String? = null
    var userId: String? = null
    var deviceId: String = "myflix_${System.currentTimeMillis()}"

    val isAuthenticated: Boolean get() = serverUrl != null && accessToken != null && userId != null

    private data class CacheEntry<T>(val data: T, val timestamp: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val cacheTtlMs = 60_000L

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCached(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > cacheTtlMs) { cache.remove(key); return null }
        return entry.data as? T
    }

    private fun <T : Any> putCache(key: String, data: T) { cache[key] = CacheEntry(data, System.currentTimeMillis()) }
    fun clearCache() { cache.clear() }

    fun configure(serverUrl: String, accessToken: String, userId: String, deviceId: String) {
        this.serverUrl = serverUrl
        this.accessToken = accessToken
        this.userId = userId
        this.deviceId = deviceId
    }

    fun logout() { 
        serverUrl = null
        accessToken = null
        userId = null
        clearCache() 
    }

    private val baseUrl: String get() = serverUrl ?: throw IllegalStateException("Server URL not set")
    
    private fun authHeader(token: String? = accessToken): String {
        val base = "MediaBrowser Client=\"MyFlix\", Device=\"Android TV\", DeviceId=\"$deviceId\", Version=\"1.0.0\""
        return if (token != null) "$base, Token=\"$token\"" else base
    }

    // ==================== Server Discovery ====================
    
    /**
     * Discover Jellyfin servers using UDP broadcast to port 7359.
     * Returns a Flow that emits servers as they're discovered.
     */
    fun discoverServersFlow(timeoutMs: Long = 5000): Flow<DiscoveredServer> = channelFlow {
        val seen = mutableSetOf<String>()
        
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 1000
                    reuseAddress = true
                }
                
                // Jellyfin discovery message
                val message = "who is JellyfinServer?"
                val sendData = message.toByteArray(Charsets.UTF_8)
                
                // Send broadcast
                try {
                    val address = InetAddress.getByName("255.255.255.255")
                    val packet = DatagramPacket(sendData, sendData.size, address, 7359)
                    socket.send(packet)
                    android.util.Log.d("JellyfinClient", "Sent UDP discovery broadcast")
                } catch (e: Exception) {
                    android.util.Log.e("JellyfinClient", "Failed to send discovery: ${e.message}")
                }
                
                // Receive responses
                val receiveBuffer = ByteArray(4096)
                val endTime = System.currentTimeMillis() + timeoutMs
                
                while (System.currentTimeMillis() < endTime && !isClosedForSend) {
                    try {
                        val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        socket.receive(receivePacket)
                        
                        val response = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8)
                        android.util.Log.d("JellyfinClient", "UDP response: $response")
                        
                        parseDiscoveryResponse(response, receivePacket.address.hostAddress)?.let { server ->
                            if (server.id !in seen) {
                                seen.add(server.id)
                                trySend(server)
                                android.util.Log.i("JellyfinClient", "Discovered: ${server.name} at ${server.address}")
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Normal - continue listening
                    } catch (e: Exception) {
                        if (!isClosedForSend) {
                            android.util.Log.w("JellyfinClient", "Receive error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("JellyfinClient", "UDP discovery failed", e)
            } finally {
                socket?.close()
            }
        }
    }
    
    /**
     * Simple blocking discovery that returns all found servers
     */
    suspend fun discoverServers(timeoutMs: Long = 5000): List<DiscoveredServer> {
        val servers = mutableListOf<DiscoveredServer>()
        
        try {
            withTimeout(timeoutMs + 1000) {
                discoverServersFlow(timeoutMs).collect { server ->
                    servers.add(server)
                }
            }
        } catch (e: TimeoutCancellationException) {
            // Expected - discovery timeout
        } catch (e: Exception) {
            android.util.Log.e("JellyfinClient", "Discovery error", e)
        }
        
        android.util.Log.i("JellyfinClient", "Discovery complete: found ${servers.size} servers")
        return servers
    }
    
    private fun parseDiscoveryResponse(response: String, senderIp: String?): DiscoveredServer? {
        return try {
            json.decodeFromString<DiscoveredServer>(response)
        } catch (e: Exception) {
            try {
                val parts = response.split("|")
                if (parts.size >= 3) {
                    DiscoveredServer(
                        id = parts[0],
                        name = parts[1],
                        address = parts[2],
                        endpointAddress = senderIp
                    )
                } else null
            } catch (e: Exception) {
                android.util.Log.w("JellyfinClient", "Failed to parse: $response")
                null
            }
        }
    }
    
    // ==================== Server Validation ====================
    
    suspend fun getServerInfo(serverUrl: String): Result<ServerInfo> = runCatching {
        httpClient.get("${serverUrl.trimEnd('/')}/System/Info/Public") {
            timeout { requestTimeoutMillis = 10_000 }
        }.body()
    }
    
    /**
     * Try to connect to a server using just the host.
     * Automatically tries common URL combinations:
     * - http://host:8096
     * - https://host:8920
     * - http://host (if includes port)
     * - https://host (if includes port)
     */
    suspend fun connectToServer(host: String): Result<ValidatedServer> {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        
        // Build list of URLs to try
        val urlsToTry = mutableListOf<String>()
        
        // Check if user provided a port
        val hasPort = cleanHost.contains(":")
        
        if (hasPort) {
            // User specified port - try both protocols
            urlsToTry.add("http://$cleanHost")
            urlsToTry.add("https://$cleanHost")
        } else {
            // No port - try defaults
            urlsToTry.add("http://$cleanHost:8096")
            urlsToTry.add("https://$cleanHost:8920")
            urlsToTry.add("http://$cleanHost")
            urlsToTry.add("https://$cleanHost")
        }
        
        android.util.Log.d("JellyfinClient", "Trying to connect to: $urlsToTry")
        
        for (url in urlsToTry) {
            try {
                android.util.Log.d("JellyfinClient", "Trying $url...")
                val serverInfo = httpClient.get("$url/System/Info/Public") {
                    timeout { 
                        requestTimeoutMillis = 5_000
                        connectTimeoutMillis = 3_000
                    }
                }.body<ServerInfo>()
                
                // Success! Check Quick Connect availability
                val quickConnectEnabled = try {
                    val response = httpClient.get("$url/QuickConnect/Enabled") {
                        timeout { requestTimeoutMillis = 3_000 }
                    }
                    response.bodyAsText().trim().lowercase() == "true"
                } catch (e: Exception) {
                    false
                }
                
                android.util.Log.i("JellyfinClient", "Connected to $url (Quick Connect: $quickConnectEnabled)")
                
                return Result.success(ValidatedServer(
                    url = url,
                    serverInfo = serverInfo,
                    quickConnectEnabled = quickConnectEnabled
                ))
            } catch (e: Exception) {
                android.util.Log.d("JellyfinClient", "Failed $url: ${e.message}")
                continue
            }
        }
        
        return Result.failure(Exception("Could not connect to server. Tried:\n${urlsToTry.joinToString("\n")}"))
    }
    
    @Deprecated("Use connectToServer instead", ReplaceWith("connectToServer(inputUrl)"))
    suspend fun validateServer(inputUrl: String): Result<ValidatedServer> = connectToServer(inputUrl)
    
    // ==================== Quick Connect ====================
    
    suspend fun isQuickConnectAvailable(serverUrl: String): Boolean {
        return try {
            val response = httpClient.get("${serverUrl.trimEnd('/')}/QuickConnect/Enabled") {
                timeout { requestTimeoutMillis = 5_000 }
            }
            response.status.isSuccess() && response.bodyAsText().trim().lowercase() == "true"
        } catch (e: Exception) {
            android.util.Log.w("JellyfinClient", "Quick Connect check failed: ${e.message}")
            false
        }
    }
    
    suspend fun initiateQuickConnect(serverUrl: String): Result<QuickConnectState> = runCatching {
        val url = serverUrl.trimEnd('/')
        httpClient.post("$url/QuickConnect/Initiate") {
            header("Authorization", authHeader(null))
        }.body()
    }
    
    suspend fun checkQuickConnectStatus(serverUrl: String, secret: String): Result<QuickConnectState> = runCatching {
        val url = serverUrl.trimEnd('/')
        httpClient.get("$url/QuickConnect/Connect") {
            parameter("secret", secret)
        }.body()
    }
    
    suspend fun authenticateWithQuickConnect(serverUrl: String, secret: String): Result<AuthResponse> = runCatching {
        val url = serverUrl.trimEnd('/')
        httpClient.post("$url/Users/AuthenticateWithQuickConnect") {
            header("Authorization", authHeader(null))
            setBody(mapOf("Secret" to secret))
        }.body()
    }
    
    fun quickConnectFlow(serverUrl: String): Flow<QuickConnectFlowState> = flow {
        emit(QuickConnectFlowState.Initializing)
        
        if (!isQuickConnectAvailable(serverUrl)) {
            emit(QuickConnectFlowState.NotAvailable)
            return@flow
        }
        
        val initResult = initiateQuickConnect(serverUrl)
        if (initResult.isFailure) {
            emit(QuickConnectFlowState.Error(initResult.exceptionOrNull()?.message ?: "Failed to initiate"))
            return@flow
        }
        
        var state = initResult.getOrThrow()
        emit(QuickConnectFlowState.WaitingForApproval(state.code, state.secret))
        
        while (!state.authenticated) {
            delay(3000)
            
            val statusResult = checkQuickConnectStatus(serverUrl, state.secret)
            if (statusResult.isFailure) {
                emit(QuickConnectFlowState.Error(statusResult.exceptionOrNull()?.message ?: "Status check failed"))
                return@flow
            }
            
            state = statusResult.getOrThrow()
            
            if (!state.authenticated) {
                emit(QuickConnectFlowState.WaitingForApproval(state.code, state.secret))
            }
        }
        
        emit(QuickConnectFlowState.Authenticating)
        
        val authResult = authenticateWithQuickConnect(serverUrl, state.secret)
        if (authResult.isFailure) {
            emit(QuickConnectFlowState.Error(authResult.exceptionOrNull()?.message ?: "Authentication failed"))
            return@flow
        }
        
        emit(QuickConnectFlowState.Authenticated(authResult.getOrThrow()))
    }

    // ==================== Standard Login ====================

    suspend fun login(serverUrl: String, username: String, password: String): Result<AuthResponse> = runCatching {
        val url = serverUrl.trimEnd('/')
        httpClient.post("$url/Users/AuthenticateByName") {
            header("Authorization", authHeader(null))
            setBody(mapOf("Username" to username, "Pw" to password))
        }.body()
    }
    
    suspend fun getPublicUsers(serverUrl: String): Result<List<PublicUser>> = runCatching {
        val url = serverUrl.trimEnd('/')
        httpClient.get("$url/Users/Public").body()
    }

    // ==================== Library & Content APIs ====================

    suspend fun getLibraries(): Result<List<JellyfinItem>> {
        getCached<List<JellyfinItem>>("libraries")?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Views") { 
                header("Authorization", authHeader()) 
            }.body()
            r.items.also { putCache("libraries", it) }
        }
    }

    suspend fun getLibraryItems(libraryId: String, limit: Int = 100): Result<ItemsResponse> {
        val key = "library:$libraryId:$limit"
        getCached<ItemsResponse>(key)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("ParentId", libraryId)
                parameter("Limit", limit)
                parameter("Fields", "Overview,ImageTags,BackdropImageTags,UserData")
            }.body<ItemsResponse>().also { putCache(key, it) }
        }
    }

    suspend fun getItem(itemId: String): Result<JellyfinItem> {
        getCached<JellyfinItem>("item:$itemId")?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Users/$userId/Items/$itemId") {
                header("Authorization", authHeader())
                parameter("Fields", "Overview,ImageTags,BackdropImageTags,UserData,MediaSources")
            }.body<JellyfinItem>().also { putCache("item:$itemId", it) }
        }
    }

    suspend fun getContinueWatching(limit: Int = 20): Result<List<JellyfinItem>> {
        getCached<List<JellyfinItem>>("continue:$limit")?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items/Resume") {
                header("Authorization", authHeader())
                parameter("Limit", limit)
                parameter("MediaTypes", "Video")
            }.body()
            r.items.also { putCache("continue:$limit", it) }
        }
    }

    suspend fun getNextUp(limit: Int = 20): Result<List<JellyfinItem>> {
        getCached<List<JellyfinItem>>("nextup:$limit")?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Shows/NextUp") {
                header("Authorization", authHeader())
                parameter("UserId", userId)
                parameter("Limit", limit)
            }.body()
            r.items.also { putCache("nextup:$limit", it) }
        }
    }

    suspend fun getLatestItems(libraryId: String, limit: Int = 20): Result<List<JellyfinItem>> {
        getCached<List<JellyfinItem>>("latest:$libraryId")?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Users/$userId/Items/Latest") {
                header("Authorization", authHeader())
                parameter("ParentId", libraryId)
                parameter("Limit", limit)
            }.body<List<JellyfinItem>>().also { putCache("latest:$libraryId", it) }
        }
    }

    suspend fun search(query: String, limit: Int = 50): Result<List<JellyfinItem>> {
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("SearchTerm", query)
                parameter("Limit", limit)
                parameter("Recursive", true)
                parameter("IncludeItemTypes", "Movie,Series,Episode")
                parameter("Fields", "Overview,ImageTags,BackdropImageTags,UserData")
            }.body()
            r.items
        }
    }

    suspend fun getSeasons(seriesId: String): Result<List<JellyfinItem>> {
        val key = "seasons:$seriesId"
        getCached<List<JellyfinItem>>(key)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Shows/$seriesId/Seasons") {
                header("Authorization", authHeader())
                parameter("UserId", userId)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    suspend fun getEpisodes(seriesId: String, seasonId: String): Result<List<JellyfinItem>> {
        val key = "episodes:$seriesId:$seasonId"
        getCached<List<JellyfinItem>>(key)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Shows/$seriesId/Episodes") {
                header("Authorization", authHeader())
                parameter("UserId", userId)
                parameter("SeasonId", seasonId)
                parameter("Fields", "Overview,ImageTags,UserData")
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    // ==================== URL Helpers ====================
    
    fun getStreamUrl(itemId: String): String = "$baseUrl/Videos/$itemId/stream?Static=true&api_key=$accessToken"
    fun getPrimaryImageUrl(itemId: String, tag: String?, maxWidth: Int = 400) = "$baseUrl/Items/$itemId/Images/Primary?maxWidth=$maxWidth&tag=$tag&quality=90"
    fun getBackdropUrl(itemId: String, tag: String?, maxWidth: Int = 1920) = "$baseUrl/Items/$itemId/Images/Backdrop?maxWidth=$maxWidth&tag=$tag&quality=90"
    fun getUserImageUrl(userId: String) = "$baseUrl/Users/$userId/Images/Primary?quality=90"

    // ==================== Playback Reporting ====================
    
    suspend fun reportPlaybackStart(
        itemId: String,
        mediaSourceId: String? = null,
        positionTicks: Long = 0
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/Sessions/Playing") {
            header("Authorization", authHeader())
            setBody(mapOf(
                "ItemId" to itemId,
                "MediaSourceId" to (mediaSourceId ?: itemId),
                "PositionTicks" to positionTicks,
                "PlayMethod" to "DirectStream",
                "PlaySessionId" to "${itemId}_${System.currentTimeMillis()}"
            ))
        }
        cache.remove("continue:20")
    }
    
    suspend fun reportPlaybackProgress(
        itemId: String,
        positionTicks: Long,
        isPaused: Boolean = false,
        mediaSourceId: String? = null
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/Sessions/Playing/Progress") {
            header("Authorization", authHeader())
            setBody(mapOf(
                "ItemId" to itemId,
                "MediaSourceId" to (mediaSourceId ?: itemId),
                "PositionTicks" to positionTicks,
                "IsPaused" to isPaused,
                "PlayMethod" to "DirectStream"
            ))
        }
    }
    
    suspend fun reportPlaybackStopped(
        itemId: String,
        positionTicks: Long,
        mediaSourceId: String? = null
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/Sessions/Playing/Stopped") {
            header("Authorization", authHeader())
            setBody(mapOf(
                "ItemId" to itemId,
                "MediaSourceId" to (mediaSourceId ?: itemId),
                "PositionTicks" to positionTicks
            ))
        }
        cache.remove("continue:20")
        cache.remove("item:$itemId")
    }
}

// ==================== Supporting Types ====================

@Serializable
data class ValidatedServer(
    val url: String,
    val serverInfo: ServerInfo,
    val quickConnectEnabled: Boolean
)

@Serializable
data class PublicUser(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
    @SerialName("HasPassword") val hasPassword: Boolean = true,
    @SerialName("HasConfiguredPassword") val hasConfiguredPassword: Boolean = true
)

sealed class QuickConnectFlowState {
    data object Initializing : QuickConnectFlowState()
    data object NotAvailable : QuickConnectFlowState()
    data class WaitingForApproval(val code: String, val secret: String) : QuickConnectFlowState()
    data object Authenticating : QuickConnectFlowState()
    data class Authenticated(val authResponse: AuthResponse) : QuickConnectFlowState()
    data class Error(val message: String) : QuickConnectFlowState()
}
