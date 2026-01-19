package dev.jausc.myflix.core.network

import android.content.Context
import dev.jausc.myflix.core.common.model.AuthResponse
import dev.jausc.myflix.core.common.model.ItemsResponse
import dev.jausc.myflix.core.common.model.JellyfinGenre
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaSegment
import dev.jausc.myflix.core.common.model.MediaSegmentsResponse
import dev.jausc.myflix.core.common.model.ServerInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Dns
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
    @SerialName("LocalAddress") val localAddress: String? = null,
)

@Serializable
data class QuickConnectState(
    @SerialName("Secret") val secret: String,
    @SerialName("Code") val code: String,
    @SerialName("Authenticated") val authenticated: Boolean = false,
    @SerialName("DateAdded") val dateAdded: String? = null,
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
@Suppress("TooManyFunctions", "LargeClass", "StringLiteralDuplication")
class JellyfinClient(
    @Suppress("UnusedPrivateProperty") private val context: Context? = null,
    httpClient: HttpClient? = null, // Allow injection for testing
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // DNS cache to prevent intermittent resolution failures during Quick Connect polling
    private val dnsCache = ConcurrentHashMap<String, Pair<List<InetAddress>, Long>>()

    private val cachingDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val cached = dnsCache[hostname]
            if (cached != null && System.currentTimeMillis() - cached.second < CacheKeys.Ttl.DNS) {
                android.util.Log.d("JellyfinClient", "DNS cache hit for $hostname: ${cached.first}")
                return cached.first
            }

            val addresses = Dns.SYSTEM.lookup(hostname)
            dnsCache[hostname] = Pair(addresses, System.currentTimeMillis())
            android.util.Log.d("JellyfinClient", "DNS resolved $hostname: $addresses")
            return addresses
        }
    }

    private val httpClient = httpClient ?: HttpClient(OkHttp) {
        engine {
            config {
                dns(cachingDns)
                followRedirects(true)
                followSslRedirects(true)
                // Longer read timeout for large library queries
                readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) { json(this@JellyfinClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000
        }
        install(HttpRedirect) {
            checkHttpMethod = false // Follow redirects for POST requests too
            allowHttpsDowngrade = false
        }
        defaultRequest { contentType(ContentType.Application.Json) }
    }

    var serverUrl: String? = null
    var accessToken: String? = null
    var userId: String? = null
    var deviceId: String = "myflix_${System.currentTimeMillis()}"

    /** Current server ID for multi-server support. Used to scope cached data. */
    private var currentServerId: String? = null

    val isAuthenticated: Boolean get() = serverUrl != null && accessToken != null && userId != null

    // ==================== Caching ====================

    private data class CacheEntry<T>(val data: T, val timestamp: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCached(key: String, ttlMs: Long = CacheKeys.Ttl.DEFAULT): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            cache.remove(key)
            return null
        }
        return entry.data as? T
    }

    private fun <T : Any> putCache(key: String, data: T) {
        // Prevent unbounded growth
        if (cache.size > 500) {
            // Simple eviction: remove arbitrary 100 items
            var removed = 0
            val iterator = cache.keys.iterator()
            while (iterator.hasNext() && removed < 100) {
                iterator.next()
                iterator.remove()
                removed++
            }
        }
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    fun clearCache() {
        cache.clear()
    }

    /** Clear specific cache entries (useful after playback state changes) */
    fun invalidateCache(vararg patterns: String) {
        patterns.forEach { pattern ->
            cache.keys.filter { it.startsWith(pattern) }.forEach { cache.remove(it) }
        }
    }

    fun configure(
        serverUrl: String,
        accessToken: String,
        userId: String,
        deviceId: String
    ) {
        this.serverUrl = serverUrl
        this.accessToken = accessToken
        this.userId = userId
        this.deviceId = deviceId
    }

    /**
     * Set the server context for multi-server support.
     * Clears the cache when switching to a different server to prevent data mixing.
     *
     * @param serverId The unique identifier for the server being switched to
     */
    fun setServerContext(serverId: String) {
        if (currentServerId != serverId) {
            android.util.Log.d("JellyfinClient", "Switching server context: $currentServerId -> $serverId")
            clearCache()
            currentServerId = serverId
        }
    }

    fun logout() {
        serverUrl = null
        accessToken = null
        userId = null
        clearCache()
    }

    private val baseUrl: String get() = serverUrl ?: error("Server URL not set")

    private fun authHeader(token: String? = accessToken): String {
        val base = """MediaBrowser Client="MyFlix", Device="Android TV", DeviceId="$deviceId", Version="1.0.0""""
        return if (token != null) """$base, Token="$token"""" else base
    }

    // ==================== Field Constants ====================
    // Only request fields we actually need to minimize data transfer

    private object Fields {
        // Minimal fields for card display (home screen rows)
        // ChildCount/RecursiveItemCount needed to filter shows without episodes
        const val CARD = "Overview,ImageTags,BackdropImageTags,UserData,OfficialRating,CriticRating,ChildCount,RecursiveItemCount"

        // Fields for episode cards (need series info)
        const val EPISODE_CARD = "Overview,ImageTags,BackdropImageTags,UserData,SeriesName,SeasonName,SeasonId,ParentId,OfficialRating,CriticRating"

        // Full fields for detail screens
        const val DETAIL =
            "Overview,ImageTags,BackdropImageTags,UserData,MediaSources,MediaStreams,Genres,Studios,People," +
                "ExternalUrls,ProviderIds,Tags,Chapters,OfficialRating,CriticRating,Taglines,CollectionIds," +
                "CollectionName,RemoteTrailers,LocalTrailerCount,ProductionLocations,Status,DisplayOrder"

        // Fields for episode listing
        const val EPISODE_LIST =
            "Overview,ImageTags,UserData,MediaSources,People,RunTimeTicks,OfficialRating,CommunityRating,PremiereDate"
    }

    // Image types for enableImageTypes parameter
    private object ImageTypes {
        const val PRIMARY = "Primary"
        const val BACKDROP = "Backdrop"
        const val THUMB = "Thumb"
        const val LOGO = "Logo"
        const val BANNER = "Banner"

        // Common combinations
        const val CARD = "$PRIMARY,$BACKDROP,$THUMB"
        const val HERO = "$PRIMARY,$BACKDROP"
        const val EPISODE = "$PRIMARY,$THUMB"
    }

    // ==================== Server Discovery ====================

    /**
     * Discover Jellyfin servers using UDP broadcast to port 7359.
     * Returns a Flow that emits servers as they're discovered.
     */
    @Suppress("NestedBlockDepth", "CognitiveComplexMethod")
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
                } catch (_: Exception) {
                    android.util.Log.e("JellyfinClient", "Failed to send discovery")
                }

                // Receive responses
                val receiveBuffer = ByteArray(4096)
                val endTime = System.currentTimeMillis() + timeoutMs

                while (System.currentTimeMillis() < endTime && isActive) {
                    try {
                        val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        socket.receive(receivePacket)

                        val response = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8)
                        android.util.Log.d("JellyfinClient", "UDP response: $response")

                        parseDiscoveryResponse(response, receivePacket.address.hostAddress)?.let { server ->
                            if (server.id !in seen) {
                                seen.add(server.id)
                                val sendResult = trySend(server)
                                if (sendResult.isSuccess) {
                                    android.util.Log.i(
                                        "JellyfinClient",
                                        "Discovered: ${server.name} at ${server.address}",
                                    )
                                }
                            }
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // Normal - continue listening
                    } catch (_: Exception) {
                        if (isActive) {
                            android.util.Log.w("JellyfinClient", "Receive error during discovery")
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
        } catch (_: TimeoutCancellationException) {
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
        } catch (_: Exception) {
            try {
                val parts = response.split("|")
                if (parts.size >= 3) {
                    DiscoveredServer(
                        id = parts[0],
                        name = parts[1],
                        address = parts[2],
                        endpointAddress = senderIp,
                    )
                } else {
                    null
                }
            } catch (_: Exception) {
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
     *
     * Includes retry logic for transient DNS/network errors.
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "CognitiveComplexMethod")
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

        // Retry logic for transient DNS/network errors
        val maxRetries = 3
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            for (url in urlsToTry) {
                try {
                    android.util.Log.d("JellyfinClient", "Trying $url (attempt $attempt/$maxRetries)...")
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
                        } catch (_: Exception) {
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
                    } catch (_: Exception) {
                        false
                    }

                    android.util.Log.i("JellyfinClient", "Connected to $finalUrl (Quick Connect: $quickConnectEnabled)")

                    return Result.success(
                        ValidatedServer(
                            url = finalUrl,
                            serverInfo = serverInfo,
                            quickConnectEnabled = quickConnectEnabled,
                        ),
                    )
                } catch (e: Exception) {
                    android.util.Log.d("JellyfinClient", "Failed $url: ${e.message}")
                    lastException = e

                    // Check if this is a DNS resolution error - worth retrying
                    val isDnsError = e.message?.contains("resolve", ignoreCase = true) == true ||
                        e.message?.contains("unknown host", ignoreCase = true) == true ||
                        e.message?.contains("no address", ignoreCase = true) == true

                    if (isDnsError && attempt < maxRetries) {
                        android.util.Log.w("JellyfinClient", "DNS error on attempt $attempt, will retry after delay")
                        // Break inner loop to retry with delay
                        break
                    }
                    continue
                }
            }

            // If we get here without returning, check if we should retry
            if (attempt < maxRetries) {
                val delayMs = 1000L * attempt // 1s, 2s, 3s exponential backoff
                android.util.Log.d("JellyfinClient", "Retrying connection in ${delayMs}ms...")
                delay(delayMs)
            }
        }

        val errorMsg = lastException?.message ?: "Unknown error"
        val triedUrls = urlsToTry.joinToString(separator = "\n")
        return Result.failure(
            Exception(
                """
                    |Could not connect to server after $maxRetries attempts. Error: $errorMsg
                    |Tried:
                    |$triedUrls
                """.trimMargin(),
            ),
        )
    }

    @Deprecated("Use connectToServer instead", ReplaceWith("connectToServer(inputUrl)"))
    suspend fun validateServer(inputUrl: String): Result<ValidatedServer> = connectToServer(inputUrl)

    // ==================== Quick Connect ====================

    suspend fun isQuickConnectAvailable(serverUrl: String): Boolean {
        val checkUrl = "${serverUrl.trimEnd('/')}/QuickConnect/Enabled"
        android.util.Log.d("JellyfinClient", "isQuickConnectAvailable checking: $checkUrl")
        return try {
            val response = httpClient.get(checkUrl) {
                timeout { requestTimeoutMillis = 5_000 }
            }
            val body = response.bodyAsText().trim().lowercase()
            val isAvailable = response.status.isSuccess() && body == "true"
            android.util.Log.d(
                "JellyfinClient",
                "isQuickConnectAvailable result: status=${response.status}, body=$body, available=$isAvailable",
            )
            isAvailable
        } catch (e: Exception) {
            android.util.Log.w(
                "JellyfinClient",
                "Quick Connect check failed for $checkUrl: ${e.javaClass.simpleName}: ${e.message}",
            )
            false
        }
    }

    suspend fun initiateQuickConnect(serverUrl: String): Result<QuickConnectState> = runCatching {
        val url = serverUrl.trimEnd('/')
        httpClient.post("$url/QuickConnect/Initiate") {
            header("Authorization", authHeader(null))
        }.body()
    }

    suspend fun checkQuickConnectStatus(serverUrl: String, secret: String): Result<QuickConnectState> {
        val url = serverUrl.trimEnd('/')
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                android.util.Log.d("JellyfinClient", "checkQuickConnectStatus attempt $attempt to $url")
                val response = httpClient.get("$url/QuickConnect/Connect") {
                    parameter("secret", secret)
                    timeout {
                        requestTimeoutMillis = 15_000
                        connectTimeoutMillis = 10_000
                        socketTimeoutMillis = 15_000
                    }
                }
                return Result.success(response.body())
            } catch (e: Exception) {
                android.util.Log.w("JellyfinClient", "checkQuickConnectStatus attempt $attempt failed: ${e.message}")

                // Check if retryable (timeout, network errors)
                val isRetryable = e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("resolve", ignoreCase = true) == true ||
                    e.message?.contains("unknown host", ignoreCase = true) == true ||
                    e.message?.contains("no address", ignoreCase = true) == true ||
                    e.message?.contains("connect", ignoreCase = true) == true

                if (isRetryable && attempt < maxRetries) {
                    android.util.Log.d("JellyfinClient", "Retrying status check in 1 second...")
                    delay(1000)
                    continue
                }

                return Result.failure(e)
            }
        }

        return Result.failure(Exception("Failed to check status after $maxRetries attempts"))
    }

    suspend fun authenticateWithQuickConnect(serverUrl: String, secret: String): Result<AuthResponse> {
        val url = serverUrl.trimEnd('/')
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                android.util.Log.d(
                    "JellyfinClient",
                    "authenticateWithQuickConnect attempt $attempt/$maxRetries to $url",
                )
                val response = httpClient.post("$url/Users/AuthenticateWithQuickConnect") {
                    header("Authorization", authHeader(null))
                    setBody(mapOf("Secret" to secret))
                    timeout {
                        requestTimeoutMillis = 10_000
                        connectTimeoutMillis = 5_000
                    }
                }
                return Result.success(response.body())
            } catch (e: Exception) {
                android.util.Log.w("JellyfinClient", "authenticateWithQuickConnect failed: ${e.message}")

                // Check if DNS/network error worth retrying
                val isRetryable = e.message?.contains("resolve", ignoreCase = true) == true ||
                    e.message?.contains("unknown host", ignoreCase = true) == true ||
                    e.message?.contains("no address", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true

                if (isRetryable && attempt < maxRetries) {
                    val delayMs = 1000L * attempt
                    android.util.Log.d("JellyfinClient", "Retrying in ${delayMs}ms...")
                    delay(delayMs)
                    continue
                }

                return Result.failure(e)
            }
        }

        return Result.failure(Exception("Failed after $maxRetries attempts"))
    }

    /**
     * Authorize a Quick Connect code (called by an already-authenticated client).
     * This allows a logged-in mobile device to approve a Quick Connect request from a TV.
     *
     * POST /QuickConnect/Authorize?code={code}
     *
     * @param code The Quick Connect code to authorize
     * @return Result containing true if authorization succeeded
     */
    suspend fun authorizeQuickConnect(code: String): Result<Boolean> = runCatching {
        requireNotNull(serverUrl) { "Server URL not configured" }
        requireNotNull(accessToken) { "Not authenticated" }

        val response = httpClient.post("$serverUrl/QuickConnect/Authorize") {
            parameter("code", code)
            header("Authorization", "MediaBrowser Token=\"$accessToken\"")
        }
        response.status.isSuccess()
    }

    @Suppress("NestedBlockDepth", "LabeledExpression")
    fun quickConnectFlow(serverUrl: String): Flow<QuickConnectFlowState> = flow {
        android.util.Log.d("JellyfinClient", "quickConnectFlow started with serverUrl: $serverUrl")
        emit(QuickConnectFlowState.Initializing)

        if (!isQuickConnectAvailable(serverUrl)) {
            android.util.Log.w("JellyfinClient", "Quick Connect not available on $serverUrl")
            emit(QuickConnectFlowState.NotAvailable)
            return@flow
        }

        android.util.Log.d("JellyfinClient", "Initiating Quick Connect on $serverUrl...")
        val initResult = initiateQuickConnect(serverUrl)
        if (initResult.isFailure) {
            android.util.Log.e(
                "JellyfinClient",
                "Quick Connect initiate failed: ${initResult.exceptionOrNull()?.message}",
            )
            emit(QuickConnectFlowState.Error(initResult.exceptionOrNull()?.message ?: "Failed to initiate"))
            return@flow
        }

        var state = initResult.getOrThrow()
        android.util.Log.d("JellyfinClient", "Quick Connect code: ${state.code}")
        emit(QuickConnectFlowState.WaitingForApproval(state.code, state.secret))

        while (!state.authenticated) {
            delay(3000)

            val statusResult = checkQuickConnectStatus(serverUrl, state.secret)
            if (statusResult.isFailure) {
                android.util.Log.e(
                    "JellyfinClient",
                    "Quick Connect status check failed: ${statusResult.exceptionOrNull()?.message}",
                )
                emit(QuickConnectFlowState.Error(statusResult.exceptionOrNull()?.message ?: "Status check failed"))
                return@flow
            }

            state = statusResult.getOrThrow()

            if (!state.authenticated) {
                emit(QuickConnectFlowState.WaitingForApproval(state.code, state.secret))
            }
        }

        android.util.Log.d("JellyfinClient", "Quick Connect approved! Authenticating with serverUrl: $serverUrl")
        emit(QuickConnectFlowState.Authenticating)

        val authResult = authenticateWithQuickConnect(serverUrl, state.secret)
        if (authResult.isFailure) {
            android.util.Log.e("JellyfinClient", "Quick Connect auth failed: ${authResult.exceptionOrNull()?.message}")
            emit(QuickConnectFlowState.Error(authResult.exceptionOrNull()?.message ?: "Authentication failed"))
            return@flow
        }

        android.util.Log.i("JellyfinClient", "Quick Connect authentication successful!")
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
        getCached<List<JellyfinItem>>(CacheKeys.LIBRARIES, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Views") {
                header("Authorization", authHeader())
            }.body()
            r.items.also { putCache(CacheKeys.LIBRARIES, it) }
        }
    }

    suspend fun getLibraryItems(
        libraryId: String,
        limit: Int = 100,
        startIndex: Int = 0,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending",
    ): Result<ItemsResponse> {
        val key = CacheKeys.library(libraryId, limit, startIndex, sortBy)
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
                parameter("enableImageTypes", ImageTypes.CARD)
            }.body<ItemsResponse>().also { putCache(key, it) }
        }
    }

    /**
     * Get library items with full filter support.
     * Supports sorting, genre filtering, watched status, year range, rating, and parental ratings filters.
     *
     * @param libraryId The parent library ID
     * @param limit Maximum items to return
     * @param startIndex Pagination offset
     * @param sortBy Jellyfin sort field (SortName, DateCreated, PremiereDate, CommunityRating, Runtime, Random)
     * @param sortOrder Ascending or Descending
     * @param genres Optional list of genre names to filter by (OR logic)
     * @param isPlayed Optional watched status filter (true=watched, false=unwatched, null=all)
     * @param minCommunityRating Optional minimum community rating (0-10)
     * @param years Optional comma-separated years to filter by
     * @param officialRatings Optional list of parental ratings to filter by
     * @param includeItemTypes Optional list of item types to include (e.g., "Movie", "Series")
     * @param seriesStatus Optional series status filter ("Continuing", "Ended")
     */
    @Suppress("LongParameterList")
    suspend fun getLibraryItemsFiltered(
        libraryId: String,
        limit: Int = 100,
        startIndex: Int = 0,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending",
        genres: List<String>? = null,
        isPlayed: Boolean? = null,
        minCommunityRating: Float? = null,
        years: String? = null,
        officialRatings: List<String>? = null,
        nameStartsWith: String? = null,
        includeItemTypes: List<String>? = null,
        excludeItemTypes: List<String>? = null,
        seriesStatus: String? = null,
    ): Result<ItemsResponse> {
        // Build cache key including filter parameters
        val filterSuffix = buildString {
            genres?.let { append("_g${it.hashCode()}") }
            isPlayed?.let { append("_p$it") }
            minCommunityRating?.let { append("_r$it") }
            years?.let { append("_y${it.hashCode()}") }
            officialRatings?.let { append("_o${it.hashCode()}") }
            nameStartsWith?.let { append("_l$it") }
            includeItemTypes?.let { append("_t${it.hashCode()}") }
            excludeItemTypes?.let { append("_e${it.hashCode()}") }
            seriesStatus?.let { append("_s$it") }
        }
        val key = CacheKeys.library(libraryId, limit, startIndex, sortBy) + filterSuffix

        // Skip cache for random sort as results should vary
        if (sortBy != "Random") {
            getCached<ItemsResponse>(key)?.let { return Result.success(it) }
        }

        return runCatching {
            httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("parentId", libraryId)
                parameter("limit", limit)
                parameter("startIndex", startIndex)
                parameter("sortBy", sortBy)
                parameter("sortOrder", sortOrder)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.CARD)
                parameter("recursive", true)

                // Longer timeout for library queries (sorting can be slow on large libraries)
                timeout {
                    requestTimeoutMillis = 60_000
                    socketTimeoutMillis = 60_000
                }

                // Apply optional filters
                genres?.takeIf { it.isNotEmpty() }?.let {
                    parameter("genres", it.joinToString("|"))
                }
                isPlayed?.let {
                    parameter("isPlayed", it)
                }
                minCommunityRating?.let {
                    parameter("minCommunityRating", it)
                }
                years?.takeIf { it.isNotEmpty() }?.let {
                    parameter("years", it)
                }
                officialRatings?.takeIf { it.isNotEmpty() }?.let {
                    parameter("OfficialRatings", it.joinToString(","))
                }
                // Handle letter filter - "#" means items starting with numbers/symbols
                nameStartsWith?.takeIf { it.isNotEmpty() }?.let { letter ->
                    if (letter == "#") {
                        // Use nameLessThan to get items that sort before "A" (numbers, symbols)
                        parameter("nameLessThan", "A")
                    } else {
                        parameter("nameStartsWith", letter)
                    }
                }
                // Filter by item types (e.g., "Movie" for movies, "Series" for TV shows)
                includeItemTypes?.takeIf { it.isNotEmpty() }?.let {
                    parameter("IncludeItemTypes", it.joinToString(","))
                }
                // Exclude item types (e.g., "BoxSet" to hide collections from libraries)
                excludeItemTypes?.takeIf { it.isNotEmpty() }?.let {
                    parameter("ExcludeItemTypes", it.joinToString(","))
                }
                // Filter by series status (Continuing, Ended) - TV shows only
                seriesStatus?.let {
                    parameter("SeriesStatus", it)
                }
            }.body<ItemsResponse>().also {
                if (sortBy != "Random") {
                    putCache(key, it)
                }
            }
        }
    }

    suspend fun getItem(itemId: String): Result<JellyfinItem> {
        val key = CacheKeys.item(itemId)
        getCached<JellyfinItem>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Users/$userId/Items/$itemId") {
                header("Authorization", authHeader())
                parameter("fields", Fields.DETAIL)
            }.body<JellyfinItem>().also { putCache(key, it) }
        }
    }

    /**
     * Get items the user is currently watching (in-progress).
     * Uses /UserItems/Resume endpoint per API spec.
     */
    suspend fun getContinueWatching(limit: Int = 20): Result<List<JellyfinItem>> {
        val key = CacheKeys.resume(limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.RESUME)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/UserItems/Resume") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("limit", limit)
                parameter("mediaTypes", "Video")
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableImageTypes", ImageTypes.CARD)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get next episodes to watch in series the user is following.
     * Supports rewatching completed series.
     */
    suspend fun getNextUp(limit: Int = 20, enableRewatching: Boolean = false,): Result<List<JellyfinItem>> {
        val key = CacheKeys.nextUp(limit, enableRewatching)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.NEXT_UP)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Shows/NextUp") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("limit", limit)
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableRewatching", enableRewatching)
                parameter("enableImageTypes", ImageTypes.CARD)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get the next episode to watch for a specific series.
     * Supports rewatching completed series.
     */
    suspend fun getNextUpForSeries(
        seriesId: String,
        limit: Int = 1,
        enableRewatching: Boolean = false,
    ): Result<List<JellyfinItem>> {
        val key = CacheKeys.nextUpSeries(seriesId, limit, enableRewatching)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.NEXT_UP)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Shows/NextUp") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("limit", limit)
                parameter("seriesId", seriesId)
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableRewatching", enableRewatching)
                parameter("enableImageTypes", ImageTypes.CARD)
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
        val key = CacheKeys.latest(libraryId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Items/Latest") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("parentId", libraryId)
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.CARD)
            }.body<List<JellyfinItem>>().also { putCache(key, it) }
        }
    }

    /**
     * Get latest Movies added to a library.
     * Excludes collections (BoxSets).
     */
    suspend fun getLatestMovies(libraryId: String, limit: Int = 20): Result<List<JellyfinItem>> {
        val key = CacheKeys.latestMovies(libraryId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LATEST)?.let { return Result.success(it) }
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
                parameter("enableImageTypes", ImageTypes.HERO)
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
        val key = CacheKeys.latestSeries(libraryId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LATEST)?.let { return Result.success(it) }
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
                parameter("enableImageTypes", ImageTypes.HERO)
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
        val key = CacheKeys.latestEpisodes(libraryId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            httpClient.get("$baseUrl/Items/Latest") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("parentId", libraryId)
                parameter("limit", limit)
                parameter("includeItemTypes", "Episode")
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableImageTypes", ImageTypes.EPISODE)
            }.body<List<JellyfinItem>>().also { putCache(key, it) }
        }
    }

    /**
     * Search across all libraries.
     */
    suspend fun search(
        query: String,
        limit: Int = 50,
        includeItemTypes: String = "Movie,Series,Episode",
    ): Result<List<JellyfinItem>> {
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("searchTerm", query)
                parameter("limit", limit)
                parameter("recursive", true)
                parameter("includeItemTypes", includeItemTypes)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.CARD)
            }.body()
            r.items
        }
    }

    /**
     * Get upcoming episodes within the next 30 days.
     * Excludes specials (season 0).
     */
    suspend fun getUpcomingEpisodes(libraryId: String? = null, limit: Int = 20): Result<List<JellyfinItem>> {
        val key = CacheKeys.upcoming(libraryId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            val today = java.time.LocalDate.now()
            val future = today.plusDays(30)

            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                libraryId?.let { parameter("parentId", it) }
                parameter("includeItemTypes", "Episode")
                parameter("minPremiereDate", "${today}T00:00:00Z")
                parameter("maxPremiereDate", "${future}T23:59:59Z")
                parameter("sortBy", "PremiereDate")
                parameter("sortOrder", "Ascending")
                parameter("recursive", true)
                parameter("limit", limit * 2) // Fetch extra to account for filtering
                parameter("fields", Fields.EPISODE_CARD)
                parameter("enableImageTypes", ImageTypes.EPISODE)
            }.body()
            // Filter out specials (season 0)
            r.items.filter { episode ->
                (episode.parentIndexNumber ?: 0) >= 1 // Season 1+ (exclude specials)
            }.take(limit).also { putCache(key, it) }
        }
    }

    /**
     * Get all genres from a library or across all libraries.
     */
    suspend fun getGenres(libraryId: String? = null): Result<List<JellyfinGenre>> {
        val key = CacheKeys.genres(libraryId)
        getCached<List<JellyfinGenre>>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: GenresResponse = httpClient.get("$baseUrl/Genres") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                libraryId?.let { parameter("parentId", it) }
                parameter("sortBy", "SortName")
                parameter("sortOrder", "Ascending")
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get items by genre name.
     */
    suspend fun getItemsByGenre(
        genreName: String,
        libraryId: String? = null,
        limit: Int = 20,
        includeItemTypes: String = "Movie,Series",
    ): Result<List<JellyfinItem>> {
        val key = CacheKeys.genre(genreName, libraryId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                libraryId?.let { parameter("parentId", it) }
                parameter("genres", genreName)
                parameter("includeItemTypes", includeItemTypes)
                parameter("sortBy", "Random")
                parameter("recursive", true)
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.HERO)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get all collections (BoxSets) from the server.
     *
     * @param limit Maximum number of collections to return.
     * @param excludeUniverseCollections If true, filters out collections tagged with "universe-collection".
     */
    suspend fun getCollections(
        limit: Int = 50,
        excludeUniverseCollections: Boolean = false,
    ): Result<List<JellyfinItem>> {
        val key = CacheKeys.collections(limit) + if (excludeUniverseCollections) ":noUniverse" else ""
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "BoxSet")
                // Use ProductionYear for chronological order if filtering for franchises
                val sortBy = if (excludeUniverseCollections) "ProductionYear,SortName" else "SortName"
                parameter("sortBy", sortBy)
                parameter("sortOrder", "Ascending")
                parameter("recursive", true)
                parameter("limit", limit)
                parameter("fields", Fields.CARD + ",Tags")
                parameter("enableImageTypes", ImageTypes.HERO)
            }.body()
            val items = if (excludeUniverseCollections) {
                r.items.filter { item ->
                    item.tags?.contains("universe-collection") != true
                }
            } else {
                r.items
            }
            items.also { putCache(key, it) }
        }
    }

    /**
     * Get collections with pagination and filtering support (for library-style view).
     * Supports alphabet navigation and sorting.
     */
    suspend fun getCollectionsFiltered(
        limit: Int = 100,
        startIndex: Int = 0,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending",
        nameStartsWith: String? = null,
        excludeUniverseCollections: Boolean = false,
    ): Result<ItemsResponse> {
        val key = CacheKeys.collectionsFiltered(limit, startIndex, sortBy, sortOrder, nameStartsWith, excludeUniverseCollections)
        getCached<ItemsResponse>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "BoxSet")
                // Respect passed sortBy, but default to ProductionYear if it is SortName and we are excluding universes
                val finalSortBy = if (sortBy == "SortName" && excludeUniverseCollections) "ProductionYear,SortName" else sortBy
                parameter("sortBy", finalSortBy)
                parameter("sortOrder", sortOrder)
                parameter("recursive", true)
                parameter("startIndex", startIndex)
                parameter("limit", limit)
                parameter("fields", Fields.CARD + ",Tags")
                parameter("enableImageTypes", ImageTypes.HERO)
                nameStartsWith?.let { parameter("nameStartsWith", it) }
            }.body()
            val items = if (excludeUniverseCollections) {
                r.items.filter { item ->
                    item.tags?.contains("universe-collection") != true
                }
            } else {
                r.items
            }
            // Adjust total count if filtering client-side
            val adjustedTotal = if (excludeUniverseCollections) {
                // Approximate - actual count may differ on subsequent pages
                r.totalRecordCount - (r.items.size - items.size)
            } else {
                r.totalRecordCount
            }
            ItemsResponse(items, adjustedTotal).also { putCache(key, it) }
        }
    }

    /**
     * Get collections tagged with "universe-collection".
     * Universe collections are BoxSets with a special tag for franchise groupings.
     * Sorted by ProductionYear (Chronological) to ensure timeline order.
     */
    suspend fun getUniverseCollections(limit: Int = 50): Result<List<JellyfinItem>> {
        val key = CacheKeys.universeCollections(limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "BoxSet")
                parameter("tags", "universe-collection")
                parameter("sortBy", "ProductionYear,SortName")
                parameter("sortOrder", "Ascending")
                parameter("recursive", true)
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.HERO)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get items in a specific collection (BoxSet).
     */
    suspend fun getCollectionItems(
        collectionId: String,
        limit: Int = 50,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending",
    ): Result<List<JellyfinItem>> {
        val key = CacheKeys.collection(collectionId, limit, sortBy, sortOrder)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("parentId", collectionId)
                parameter("sortBy", sortBy)
                parameter("sortOrder", sortOrder)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.HERO)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get special features (extras, trailers) for an item.
     */
    suspend fun getSpecialFeatures(itemId: String, limit: Int = 20): Result<List<JellyfinItem>> {
        val key = CacheKeys.specialFeatures(itemId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            val r: List<JellyfinItem> = httpClient.get("$baseUrl/Items/$itemId/SpecialFeatures") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.CARD)
            }.body()
            r.take(limit).also { putCache(key, it) }
        }
    }

    /**
     * Get ancestor items for a specific item (used to discover collections).
     */
    suspend fun getItemAncestors(itemId: String): Result<List<JellyfinItem>> {
        val key = CacheKeys.ancestors(itemId)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            val r: List<JellyfinItem> = httpClient.get("$baseUrl/Items/$itemId/Ancestors") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("fields", Fields.CARD)
            }.body()
            r.also { putCache(key, it) }
        }
    }

    /**
     * Get suggested items based on a specific item (recommendations).
     * Uses the Similar endpoint for relevant suggestions.
     */
    suspend fun getSuggestions(limit: Int = 20): Result<List<JellyfinItem>> {
        val key = CacheKeys.suggestions(limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LATEST)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "Movie,Series")
                parameter("sortBy", "Random")
                parameter("recursive", true)
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.HERO)
                // Get unwatched items the user might like
                parameter("isPlayed", false)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    suspend fun getSeasons(seriesId: String): Result<List<JellyfinItem>> {
        val key = CacheKeys.seasons(seriesId)
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
        val key = CacheKeys.episodes(seriesId, seasonId)
        getCached<List<JellyfinItem>>(key)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Shows/$seriesId/Episodes") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("seasonId", seasonId)
                parameter("fields", Fields.EPISODE_LIST)
                parameter("enableImageTypes", ImageTypes.EPISODE)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get similar items for recommendations.
     */
    suspend fun getSimilarItems(itemId: String, limit: Int = 12): Result<List<JellyfinItem>> {
        val key = CacheKeys.similar(itemId, limit)
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
     * Get media segments for an item (intro, outro, recap, etc.).
     * Used for skip intro/outro functionality.
     * Requires Jellyfin 10.9+ with Media Segments plugin or built-in support.
     */
    suspend fun getMediaSegments(itemId: String): Result<List<MediaSegment>> {
        val key = CacheKeys.mediaSegments(itemId)
        getCached<List<MediaSegment>>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            val r: MediaSegmentsResponse = httpClient.get("$baseUrl/MediaSegments/$itemId") {
                header("Authorization", authHeader())
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get items featuring a specific person (filmography).
     */
    suspend fun getItemsByPerson(personId: String, limit: Int = 30): Result<List<JellyfinItem>> {
        val key = CacheKeys.personItems(personId, limit)
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("personIds", personId)
                parameter("includeItemTypes", "Movie,Series")
                parameter("recursive", true)
                parameter("sortBy", "PremiereDate,SortName")
                parameter("sortOrder", "Descending")
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.CARD)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get user's favorite items.
     */
    suspend fun getFavorites(limit: Int = 50, includeItemTypes: String? = null,): Result<List<JellyfinItem>> {
        val key = CacheKeys.favorites(limit, includeItemTypes)
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
        invalidateCache(CacheKeys.Patterns.FAVORITES, CacheKeys.item(itemId))
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
        invalidateCache(CacheKeys.item(itemId), CacheKeys.Patterns.RESUME, CacheKeys.Patterns.NEXT_UP)
    }

    /**
     * Hide item from Continue Watching by clearing playback position.
     * Uses /UserItems/{itemId}/UserData endpoint per API spec.
     */
    suspend fun hideFromResume(itemId: String): Result<Unit> = runCatching {
        val response = httpClient.post("$baseUrl/UserItems/$itemId/UserData") {
            header("Authorization", authHeader())
            parameter("userId", userId)
            setBody(mapOf("PlaybackPositionTicks" to 0))
        }
        android.util.Log.d("JellyfinClient", "hideFromResume: itemId=$itemId status=${response.status}")
        // Invalidate resume cache
        invalidateCache(CacheKeys.item(itemId), CacheKeys.Patterns.RESUME)
    }

    // ==================== URL Helpers ====================

    fun getStreamUrl(itemId: String): String = getStreamUrl(itemId, null, null, null)

    fun getStreamUrl(
        itemId: String,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        maxBitrateMbps: Int? = null,
    ): String {
        // If no bitrate limit, use direct stream (Static=true)
        // If bitrate limit set, request transcoding with MaxStreamingBitrate
        val isDirectStream = maxBitrateMbps == null || maxBitrateMbps == 0
        val params = buildList {
            add("Static=$isDirectStream")
            add("api_key=$accessToken")
            audioStreamIndex?.let { add("AudioStreamIndex=$it") }
            subtitleStreamIndex?.let { add("SubtitleStreamIndex=$it") }
            if (!isDirectStream && maxBitrateMbps != null) {
                // Convert Mbps to bits per second
                val bitrateBps = maxBitrateMbps * 1_000_000L
                add("MaxStreamingBitrate=$bitrateBps")
            }
        }
        return "$baseUrl/Videos/$itemId/stream?${params.joinToString("&")}"
    }

    /**
     * Get primary image URL (posters for movies/series, thumbnails for episodes).
     * Uses WebP format for smaller file sizes.
     */
    fun getPrimaryImageUrl(itemId: String, tag: String?, maxWidth: Int = 400): String =
        buildImageUrl(itemId, ImageTypes.PRIMARY, tag, maxWidth)

    /**
     * Get backdrop image URL for backgrounds.
     */
    fun getBackdropUrl(itemId: String, tag: String?, maxWidth: Int = 1920): String =
        buildImageUrl(itemId, ImageTypes.BACKDROP, tag, maxWidth)

    /**
     * Get thumb image URL (landscape thumbnails).
     */
    fun getThumbUrl(itemId: String, tag: String?, maxWidth: Int = 600): String =
        buildImageUrl(itemId, ImageTypes.THUMB, tag, maxWidth)

    /**
     * Get logo image URL.
     */
    fun getLogoUrl(itemId: String, tag: String?, maxWidth: Int = 600): String =
        buildImageUrl(itemId, ImageTypes.LOGO, tag, maxWidth)

    /**
     * Build image URL with consistent parameters.
     * Uses WebP format for better compression.
     */
    private fun buildImageUrl(
        itemId: String,
        imageType: String,
        tag: String?,
        maxWidth: Int,
        quality: Int = 90,
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

    /**
     * Get person image URL for cast/crew photos.
     */
    fun getPersonImageUrl(personId: String, tag: String?, maxWidth: Int = 200): String {
        val params = buildString {
            append("maxWidth=$maxWidth")
            append("&quality=90")
            append("&format=Webp")
            if (tag != null) append("&tag=$tag")
        }
        return "$baseUrl/Items/$personId/Images/Primary?$params"
    }

    /**
     * Get chapter image URL for chapter thumbnails.
     * Chapter images are extracted from the video file at the chapter's position.
     */
    fun getChapterImageUrl(itemId: String, chapterIndex: Int, maxWidth: Int = 320): String {
        val params = buildString {
            append("maxWidth=$maxWidth")
            append("&quality=90")
            append("&format=Webp")
        }
        return "$baseUrl/Items/$itemId/Images/Chapter/$chapterIndex?$params"
    }

    // ==================== Playback Reporting ====================

    private var currentPlaySessionId: String? = null

    suspend fun reportPlaybackStart(
        itemId: String,
        mediaSourceId: String? = null,
        positionTicks: Long = 0,
    ): Result<Unit> = runCatching {
        val sessionId = "${itemId}_${System.currentTimeMillis()}"
        currentPlaySessionId = sessionId
        val body = PlaybackStartInfo(
            itemId = itemId,
            mediaSourceId = mediaSourceId ?: itemId,
            positionTicks = positionTicks,
            playMethod = "DirectPlay",
            playSessionId = sessionId,
            canSeek = true,
            isPaused = false,
            isMuted = false,
        )
        android.util.Log.d("JellyfinClient", "reportPlaybackStart: POST $baseUrl/Sessions/Playing itemId=$itemId")
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
        invalidateCache(CacheKeys.Patterns.RESUME)
    }

    suspend fun reportPlaybackProgress(
        itemId: String,
        positionTicks: Long,
        isPaused: Boolean = false,
        mediaSourceId: String? = null,
    ): Result<Unit> = runCatching {
        val body = PlaybackProgressInfo(
            itemId = itemId,
            mediaSourceId = mediaSourceId ?: itemId,
            positionTicks = positionTicks,
            isPaused = isPaused,
            isMuted = false,
            playMethod = "DirectPlay",
            playSessionId = currentPlaySessionId,
            canSeek = true,
        )
        val response = httpClient.post("$baseUrl/Sessions/Playing/Progress") {
            header("Authorization", authHeader())
            setBody(body)
        }
        android.util.Log.d(
            "JellyfinClient",
            "reportPlaybackProgress: pos=${positionTicks / 10_000}ms paused=$isPaused status=${response.status}",
        )
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "reportPlaybackProgress failed: $errorBody")
        }
    }

    suspend fun reportPlaybackStopped(
        itemId: String,
        positionTicks: Long,
        mediaSourceId: String? = null,
    ): Result<Unit> = runCatching {
        val body = PlaybackStopInfo(
            itemId = itemId,
            mediaSourceId = mediaSourceId ?: itemId,
            positionTicks = positionTicks,
            playSessionId = currentPlaySessionId,
        )
        android.util.Log.d(
            "JellyfinClient",
            "reportPlaybackStopped: POST $baseUrl/Sessions/Playing/Stopped itemId=$itemId",
        )
        val response = httpClient.post("$baseUrl/Sessions/Playing/Stopped") {
            header("Authorization", authHeader())
            setBody(body)
        }
        android.util.Log.d(
            "JellyfinClient",
            "reportPlaybackStopped: pos=${positionTicks / 10_000}ms status=${response.status}",
        )
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "reportPlaybackStopped failed: $errorBody")
        }
        currentPlaySessionId = null
        // Invalidate caches that depend on playback state
        invalidateCache(CacheKeys.Patterns.RESUME, CacheKeys.Patterns.NEXT_UP, CacheKeys.item(itemId))
    }
}

// ==================== Playback Report Data Classes ====================

@Serializable
private data class PlaybackStartInfo(
    @SerialName("ItemId") val itemId: String,
    @SerialName("MediaSourceId") val mediaSourceId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("PlayMethod") val playMethod: String,
    @SerialName("PlaySessionId") val playSessionId: String,
    @SerialName("CanSeek") val canSeek: Boolean,
    @SerialName("IsPaused") val isPaused: Boolean,
    @SerialName("IsMuted") val isMuted: Boolean,
)

@Serializable
private data class PlaybackProgressInfo(
    @SerialName("ItemId") val itemId: String,
    @SerialName("MediaSourceId") val mediaSourceId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("IsPaused") val isPaused: Boolean,
    @SerialName("IsMuted") val isMuted: Boolean,
    @SerialName("PlayMethod") val playMethod: String,
    @SerialName("PlaySessionId") val playSessionId: String?,
    @SerialName("CanSeek") val canSeek: Boolean,
)

@Serializable
private data class PlaybackStopInfo(
    @SerialName("ItemId") val itemId: String,
    @SerialName("MediaSourceId") val mediaSourceId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("PlaySessionId") val playSessionId: String?,
)

// ==================== Supporting Types ====================

@Serializable
data class ValidatedServer(
    val url: String,
    val serverInfo: ServerInfo,
    val quickConnectEnabled: Boolean,
)

@Serializable
data class PublicUser(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
    @SerialName("HasPassword") val hasPassword: Boolean = true,
    @SerialName("HasConfiguredPassword") val hasConfiguredPassword: Boolean = true,
)

@Serializable
data class GenresResponse(
    @SerialName("Items") val items: List<JellyfinGenre>,
    @SerialName("TotalRecordCount") val totalRecordCount: Int,
)

sealed class QuickConnectFlowState {
    data object Initializing : QuickConnectFlowState()
    data object NotAvailable : QuickConnectFlowState()
    data class WaitingForApproval(val code: String, val secret: String) : QuickConnectFlowState()
    data object Authenticating : QuickConnectFlowState()
    data class Authenticated(val authResponse: AuthResponse) : QuickConnectFlowState()
    data class Error(val message: String) : QuickConnectFlowState()
}
