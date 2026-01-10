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
import kotlinx.coroutines.channels.awaitClose
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

/**
 * Jellyfin API client with optimized endpoints based on official OpenAPI spec.
 * 
 * Key optimizations:
 * - Uses modern endpoint paths (/Items/Latest instead of /Users/{id}/Items/Latest)
 * - Requests only necessary fields to minimize data transfer
 * - WebP format for images (smaller file sizes)
 * - Variable cache TTL based on data volatility
 * - Proper parameter casing per API spec
 */
class JellyfinClient(
    private val context: Context? = null,
    httpClient: HttpClient? = null  // Allow injection for testing
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = httpClient ?: HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
                followSslRedirects(true)
            }
        }
        install(ContentNegotiation) { json(this@JellyfinClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        install(HttpRedirect) {
            checkHttpMethod = false  // Follow redirects for POST requests too
            allowHttpsDowngrade = false
        }
        defaultRequest { contentType(ContentType.Application.Json) }
    }

    var serverUrl: String? = null
    var accessToken: String? = null
    var userId: String? = null
    var deviceId: String = "myflix_${System.currentTimeMillis()}"

    val isAuthenticated: Boolean get() = serverUrl != null && accessToken != null && userId != null

    // ==================== Caching ====================
    
    private data class CacheEntry<T>(val data: T, val timestamp: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    
    // Variable TTL based on data type
    private object CacheTtl {
        const val LIBRARIES = 300_000L      // 5 min - rarely changes
        const val ITEM_DETAILS = 120_000L   // 2 min - moderate
        const val RESUME = 30_000L          // 30 sec - changes frequently
        const val NEXT_UP = 60_000L         // 1 min
        const val LATEST = 60_000L          // 1 min
        const val DEFAULT = 60_000L         // 1 min
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCached(key: String, ttlMs: Long = CacheTtl.DEFAULT): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) { 
            cache.remove(key)
            return null 
        }
        return entry.data as? T
    }

    private fun <T : Any> putCache(key: String, data: T) { 
        cache[key] = CacheEntry(data, System.currentTimeMillis()) 
    }
    
    fun clearCache() { cache.clear() }
    
    /** Clear specific cache entries (useful after playback state changes) */
    fun invalidateCache(vararg patterns: String) {
        patterns.forEach { pattern ->
            cache.keys.filter { it.startsWith(pattern) }.forEach { cache.remove(it) }
        }
    }

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
    
    // ==================== Field Constants ====================
    // Only request fields we actually need to minimize data transfer
    
    private object Fields {
        // Minimal fields for card display (home screen rows)
        const val CARD = "Overview,ImageTags,BackdropImageTags,UserData,OfficialRating,CriticRating"
        
        // Fields for episode cards (need series info)
        const val EPISODE_CARD = "Overview,ImageTags,BackdropImageTags,UserData,SeriesName,SeasonName,OfficialRating,CriticRating"
        
        // Full fields for detail screens
        const val DETAIL = "Overview,ImageTags,BackdropImageTags,UserData,MediaSources,MediaStreams,Genres,Studios,People,ExternalUrls,ProviderIds,Tags,Chapters,OfficialRating,CriticRating"
        
        // Fields for series detail (need child count)
        const val SERIES_DETAIL = "Overview,ImageTags,BackdropImageTags,UserData,ChildCount,RecursiveItemCount,Genres,Studios,People,ExternalUrls,OfficialRating,CriticRating"
        
        // Fields for episode listing
        const val EPISODE_LIST = "Overview,ImageTags,UserData,MediaSources"
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
     * - https://host (preferred)
     * - https://host:8920
     * - http://host (with HTTPS upgrade check)
     * - http://host:8096
     */
    suspend fun connectToServer(host: String): Result<ValidatedServer> {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        
        // Build list of URLs to try - prefer HTTPS first for security
        val urlsToTry = mutableListOf<String>()
        
        // Check if user provided a port
        val hasPort = cleanHost.contains(":")
        
        if (hasPort) {
            // User specified port - try HTTPS first
            urlsToTry.add("https://$cleanHost")
            urlsToTry.add("http://$cleanHost")
        } else {
            // No port - try HTTPS first, then defaults
            urlsToTry.add("https://$cleanHost")
            urlsToTry.add("https://$cleanHost:8920")
            urlsToTry.add("http://$cleanHost")
            urlsToTry.add("http://$cleanHost:8096")
        }
        
        android.util.Log.d("JellyfinClient", "Trying to connect to: $urlsToTry")
        
        for (url in urlsToTry) {
            try {
                android.util.Log.d("JellyfinClient", "Trying $url...")
                val response = httpClient.get("$url/System/Info/Public") {
                    timeout { 
                        requestTimeoutMillis = 5_000
                        connectTimeoutMillis = 3_000
                    }
                }
                val serverInfo = response.body<ServerInfo>()
                
                // Determine the canonical URL to use
                var finalUrl = url
                
                // If we connected via HTTP, check if HTTPS works (server may redirect HTTP->HTTPS)
                if (url.startsWith("http://")) {
                    val httpsUrl = url.replace("http://", "https://")
                    try {
                        httpClient.get("$httpsUrl/System/Info/Public") {
                            timeout { 
                                requestTimeoutMillis = 3_000
                                connectTimeoutMillis = 2_000 
                            }
                        }
                        // HTTPS works - use it as the canonical URL
                        finalUrl = httpsUrl
                        android.util.Log.d("JellyfinClient", "HTTP worked but HTTPS also works - using $finalUrl")
                    } catch (e: Exception) {
                        // HTTPS doesn't work, stick with HTTP
                        android.util.Log.d("JellyfinClient", "HTTPS not available, using HTTP: $url")
                    }
                }
                
                // Check Quick Connect availability using final URL
                val quickConnectEnabled = try {
                    val qcResponse = httpClient.get("$finalUrl/QuickConnect/Enabled") {
                        timeout { requestTimeoutMillis = 3_000 }
                    }
                    qcResponse.bodyAsText().trim().lowercase() == "true"
                } catch (e: Exception) {
                    false
                }
                
                android.util.Log.i("JellyfinClient", "Connected to $finalUrl (Quick Connect: $quickConnectEnabled)")
                
                return Result.success(ValidatedServer(
                    url = finalUrl,
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
        getCached<List<JellyfinItem>>("libraries", CacheTtl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Views") { 
                header("Authorization", authHeader()) 
            }.body()
            r.items.also { putCache("libraries", it) }
        }
    }

    suspend fun getLibraryItems(
        libraryId: String, 
        limit: Int = 100,
        startIndex: Int = 0,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending"
    ): Result<ItemsResponse> {
        val key = "library:$libraryId:$limit:$startIndex:$sortBy"
        getCached<ItemsResponse>(key)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("parentId", libraryId)
                parameter("limit", limit)
                parameter("startIndex", startIndex)
                parameter("sortBy", sortBy)
                parameter("sortOrder", sortOrder)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", "Primary,Backdrop,Thumb")
            }.body<ItemsResponse>().also { putCache(key, it) }
        }
    }

    suspend fun getItem(itemId: String): Result<JellyfinItem> {
        getCached<JellyfinItem>("item:$itemId", CacheTtl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Users/$userId/Items/$itemId") {
                header("Authorization", authHeader())
                parameter("fields", Fields.DETAIL)
            }.body<JellyfinItem>().also { putCache("item:$itemId", it) }
        }
    }

    /**
     * Get items the user is currently watching (in-progress).
     * Uses /UserItems/Resume endpoint per API spec.
     */
    suspend fun getContinueWatching(limit: Int = 20): Result<List<JellyfinItem>> {
        getCached<List<JellyfinItem>>("resume:$limit", CacheTtl.RESUME)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/UserItems/Resume") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("limit", limit)
                parameter("mediaTypes", "Video")
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableImageTypes", "Primary,Backdrop,Thumb")
            }.body()
            r.items.also { putCache("resume:$limit", it) }
        }
    }

    /**
     * Get next episodes to watch in series the user is following.
     * Supports rewatching completed series.
     */
    suspend fun getNextUp(
        limit: Int = 20,
        enableRewatching: Boolean = false
    ): Result<List<JellyfinItem>> {
        val key = "nextup:$limit:$enableRewatching"
        getCached<List<JellyfinItem>>(key, CacheTtl.NEXT_UP)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Shows/NextUp") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("limit", limit)
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableRewatching", enableRewatching)
                parameter("enableImageTypes", "Primary,Backdrop,Thumb")
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get latest items added to a library.
     * Generic endpoint - returns whatever was most recently added.
     * Note: Use getLatestMovies or getLatestSeries for filtered results.
     */
    suspend fun getLatestItems(libraryId: String, limit: Int = 20): Result<List<JellyfinItem>> {
        val key = "latest:$libraryId:$limit"
        getCached<List<JellyfinItem>>(key, CacheTtl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Items/Latest") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("parentId", libraryId)
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", "Primary,Backdrop,Thumb")
            }.body<List<JellyfinItem>>().also { putCache(key, it) }
        }
    }
    
    /**
     * Get latest Movies added to a library.
     * Excludes collections (BoxSets).
     */
    suspend fun getLatestMovies(libraryId: String, limit: Int = 20): Result<List<JellyfinItem>> {
        val key = "latestMovies:$libraryId:$limit"
        getCached<List<JellyfinItem>>(key, CacheTtl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("parentId", libraryId)
                parameter("includeItemTypes", "Movie")
                parameter("sortBy", "DateCreated,SortName")
                parameter("sortOrder", "Descending")
                parameter("recursive", true)
                parameter("limit", limit + 10) // Request extra to account for filtering
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", "Primary,Backdrop")
            }.body()
            // Filter out any BoxSet/Collection items that slip through
            r.items
                .filter { it.type == "Movie" }
                .take(limit)
                .also { putCache(key, it) }
        }
    }
    
    /**
     * Get latest Series (new shows) added to a TV library.
     * Excludes unaired shows (no premiered episodes) and collections.
     */
    suspend fun getLatestSeries(libraryId: String, limit: Int = 20): Result<List<JellyfinItem>> {
        val key = "latestSeries:$libraryId:$limit"
        getCached<List<JellyfinItem>>(key, CacheTtl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("parentId", libraryId)
                parameter("includeItemTypes", "Series")
                parameter("sortBy", "DateCreated,SortName")
                parameter("sortOrder", "Descending")
                parameter("recursive", true)
                parameter("limit", limit + 10) // Request extra to account for filtering
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", "Primary,Backdrop")
                // Exclude shows that haven't aired yet
                parameter("maxPremiereDate", java.time.Instant.now().toString())
            }.body()
            // Filter out any BoxSet/Collection items that slip through
            r.items
                .filter { it.type == "Series" }
                .take(limit)
                .also { putCache(key, it) }
        }
    }
    
    /**
     * Get latest Episodes added to a TV library.
     * Returns episode items with series context.
     */
    suspend fun getLatestEpisodes(libraryId: String, limit: Int = 20): Result<List<JellyfinItem>> {
        val key = "latestEpisodes:$libraryId:$limit"
        getCached<List<JellyfinItem>>(key, CacheTtl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Items/Latest") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("parentId", libraryId)
                parameter("limit", limit)
                parameter("includeItemTypes", "Episode")
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableImageTypes", "Primary,Thumb")
            }.body<List<JellyfinItem>>().also { putCache(key, it) }
        }
    }

    /**
     * Search across all libraries.
     */
    suspend fun search(
        query: String, 
        limit: Int = 50,
        includeItemTypes: String = "Movie,Series,Episode"
    ): Result<List<JellyfinItem>> {
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("searchTerm", query)
                parameter("limit", limit)
                parameter("recursive", true)
                parameter("includeItemTypes", includeItemTypes)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", "Primary,Backdrop,Thumb")
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
                parameter("userId", userId)
                parameter("fields", "ImageTags,UserData,ChildCount")
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
                parameter("userId", userId)
                parameter("seasonId", seasonId)
                parameter("fields", Fields.EPISODE_LIST)
                parameter("enableImageTypes", "Primary,Thumb")
            }.body()
            r.items.also { putCache(key, it) }
        }
    }
    
    /**
     * Get similar items for recommendations.
     */
    suspend fun getSimilarItems(itemId: String, limit: Int = 12): Result<List<JellyfinItem>> {
        val key = "similar:$itemId:$limit"
        getCached<List<JellyfinItem>>(key)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Items/$itemId/Similar") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }
    
    /**
     * Get user's favorite items.
     */
    suspend fun getFavorites(
        limit: Int = 50,
        includeItemTypes: String? = null
    ): Result<List<JellyfinItem>> {
        val key = "favorites:$limit:$includeItemTypes"
        getCached<List<JellyfinItem>>(key)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("isFavorite", true)
                parameter("recursive", true)
                parameter("limit", limit)
                parameter("sortBy", "SortName")
                parameter("fields", Fields.CARD)
                includeItemTypes?.let { parameter("includeItemTypes", it) }
            }.body()
            r.items.also { putCache(key, it) }
        }
    }
    
    /**
     * Toggle favorite status for an item.
     */
    suspend fun setFavorite(itemId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        if (isFavorite) {
            httpClient.post("$baseUrl/Users/$userId/FavoriteItems/$itemId") {
                header("Authorization", authHeader())
            }
        } else {
            httpClient.delete("$baseUrl/Users/$userId/FavoriteItems/$itemId") {
                header("Authorization", authHeader())
            }
        }
        // Invalidate relevant caches
        invalidateCache("favorites", "item:$itemId")
    }
    
    /**
     * Mark item as played/unplayed.
     * Uses /UserPlayedItems/{itemId} endpoint per API spec.
     */
    suspend fun setPlayed(itemId: String, played: Boolean): Result<Unit> = runCatching {
        val response = if (played) {
            httpClient.post("$baseUrl/UserPlayedItems/$itemId") {
                header("Authorization", authHeader())
                parameter("userId", userId)
            }
        } else {
            httpClient.delete("$baseUrl/UserPlayedItems/$itemId") {
                header("Authorization", authHeader())
                parameter("userId", userId)
            }
        }
        android.util.Log.d("JellyfinClient", "setPlayed: itemId=$itemId played=$played status=${response.status}")
        // Invalidate relevant caches
        invalidateCache("item:$itemId", "resume", "nextup")
    }

    // ==================== URL Helpers ====================
    
    fun getStreamUrl(itemId: String): String = 
        "$baseUrl/Videos/$itemId/stream?Static=true&api_key=$accessToken"
    
    /**
     * Get primary image URL (posters for movies/series, thumbnails for episodes).
     * Uses WebP format for smaller file sizes.
     */
    fun getPrimaryImageUrl(itemId: String, tag: String?, maxWidth: Int = 400): String {
        return buildImageUrl(itemId, "Primary", tag, maxWidth)
    }
    
    /**
     * Get backdrop image URL for backgrounds.
     */
    fun getBackdropUrl(itemId: String, tag: String?, maxWidth: Int = 1920): String {
        return buildImageUrl(itemId, "Backdrop", tag, maxWidth)
    }
    
    /**
     * Get thumb image URL (landscape thumbnails).
     */
    fun getThumbUrl(itemId: String, tag: String?, maxWidth: Int = 600): String {
        return buildImageUrl(itemId, "Thumb", tag, maxWidth)
    }
    
    /**
     * Build image URL with consistent parameters.
     * Uses WebP format for better compression.
     */
    private fun buildImageUrl(
        itemId: String, 
        imageType: String, 
        tag: String?, 
        maxWidth: Int,
        quality: Int = 90
    ): String {
        val params = buildString {
            append("maxWidth=$maxWidth")
            append("&quality=$quality")
            append("&format=Webp")
            if (tag != null) append("&tag=$tag")
        }
        return "$baseUrl/Items/$itemId/Images/$imageType?$params"
    }
    
    /**
     * Get blurred backdrop for background use.
     */
    fun getBlurredBackdropUrl(itemId: String, tag: String?, blur: Int = 20): String {
        val params = buildString {
            append("maxWidth=1280")
            append("&quality=70")
            append("&format=Webp")
            append("&blur=$blur")
            if (tag != null) append("&tag=$tag")
        }
        return "$baseUrl/Items/$itemId/Images/Backdrop?$params"
    }
    
    fun getUserImageUrl(userId: String) = "$baseUrl/Users/$userId/Images/Primary?quality=90&format=Webp"

    // ==================== Playback Reporting ====================
    
    private var currentPlaySessionId: String? = null
    
    suspend fun reportPlaybackStart(
        itemId: String,
        mediaSourceId: String? = null,
        positionTicks: Long = 0
    ): Result<Unit> = runCatching {
        currentPlaySessionId = "${itemId}_${System.currentTimeMillis()}"
        val body = mapOf(
            "ItemId" to itemId,
            "MediaSourceId" to (mediaSourceId ?: itemId),
            "PositionTicks" to positionTicks,
            "PlayMethod" to "DirectPlay",
            "PlaySessionId" to currentPlaySessionId,
            "CanSeek" to true,
            "IsPaused" to false,
            "IsMuted" to false
        )
        android.util.Log.d("JellyfinClient", "reportPlaybackStart: POST $baseUrl/Sessions/Playing body=$body")
        val response = httpClient.post("$baseUrl/Sessions/Playing") {
            header("Authorization", authHeader())
            setBody(body)
        }
        android.util.Log.d("JellyfinClient", "reportPlaybackStart: status=${response.status}")
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "reportPlaybackStart failed: $errorBody")
        }
        // Invalidate resume cache since playback state changed
        invalidateCache("resume")
    }
    
    suspend fun reportPlaybackProgress(
        itemId: String,
        positionTicks: Long,
        isPaused: Boolean = false,
        mediaSourceId: String? = null
    ): Result<Unit> = runCatching {
        val body = mapOf(
            "ItemId" to itemId,
            "MediaSourceId" to (mediaSourceId ?: itemId),
            "PositionTicks" to positionTicks,
            "IsPaused" to isPaused,
            "IsMuted" to false,
            "PlayMethod" to "DirectPlay",
            "PlaySessionId" to currentPlaySessionId,
            "CanSeek" to true
        )
        val response = httpClient.post("$baseUrl/Sessions/Playing/Progress") {
            header("Authorization", authHeader())
            setBody(body)
        }
        android.util.Log.d("JellyfinClient", "reportPlaybackProgress: pos=${positionTicks/10_000}ms paused=$isPaused status=${response.status}")
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "reportPlaybackProgress failed: $errorBody")
        }
    }
    
    suspend fun reportPlaybackStopped(
        itemId: String,
        positionTicks: Long,
        mediaSourceId: String? = null
    ): Result<Unit> = runCatching {
        val body = mapOf(
            "ItemId" to itemId,
            "MediaSourceId" to (mediaSourceId ?: itemId),
            "PositionTicks" to positionTicks,
            "PlaySessionId" to currentPlaySessionId
        )
        android.util.Log.d("JellyfinClient", "reportPlaybackStopped: POST $baseUrl/Sessions/Playing/Stopped body=$body")
        val response = httpClient.post("$baseUrl/Sessions/Playing/Stopped") {
            header("Authorization", authHeader())
            setBody(body)
        }
        android.util.Log.d("JellyfinClient", "reportPlaybackStopped: pos=${positionTicks/10_000}ms status=${response.status}")
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "reportPlaybackStopped failed: $errorBody")
        }
        currentPlaySessionId = null
        // Invalidate caches that depend on playback state
        invalidateCache("resume", "nextup", "item:$itemId")
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
