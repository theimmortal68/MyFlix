package dev.jausc.myflix.core.network

import android.content.Context
import dev.jausc.myflix.core.common.model.AllThemeMediaResult
import dev.jausc.myflix.core.common.model.AuthResponse
import dev.jausc.myflix.core.common.model.CodecProfile
import dev.jausc.myflix.core.common.model.DeviceProfile
import dev.jausc.myflix.core.common.model.DirectPlayProfile
import dev.jausc.myflix.core.common.model.ItemsResponse
import dev.jausc.myflix.core.common.model.JellyfinGenre
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.LyricDto
import dev.jausc.myflix.core.common.model.MediaSegment
import dev.jausc.myflix.core.common.model.MediaSegmentsResponse
import dev.jausc.myflix.core.common.model.MediaSource
import dev.jausc.myflix.core.common.model.PlaybackInfoRequest
import dev.jausc.myflix.core.common.model.PlaybackInfoResponse
import dev.jausc.myflix.core.common.model.ProfileCondition
import dev.jausc.myflix.core.common.model.ServerInfo
import dev.jausc.myflix.core.common.model.SubtitleProfile
import dev.jausc.myflix.core.common.model.TranscodingProfile
import dev.jausc.myflix.core.common.util.MediaCodecCapabilities
import dev.jausc.myflix.core.network.syncplay.SyncPlayGroup
import dev.jausc.myflix.core.network.syncplay.UtcTimeResponse
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
        encodeDefaults = true // Required for DeviceProfile - server expects all fields
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

    /** Media codec capabilities for building dynamic DeviceProfile */
    private val mediaCodecCapabilities: MediaCodecCapabilities? by lazy {
        context?.let { MediaCodecCapabilities(it) }
    }

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

    /**
     * Register session capabilities with the server.
     * This enables remote control features (pause, stop, seek) from the Jellyfin dashboard.
     */
    suspend fun registerSessionCapabilities(): Result<Unit> = runCatching {
        // Use ClientCapabilitiesDto schema - only include fields that exist in the API
        val body = ClientCapabilitiesDto(
            playableMediaTypes = listOf("Video", "Audio"),
            supportedCommands = listOf(
                // Navigation
                "MoveUp", "MoveDown", "MoveLeft", "MoveRight",
                "PageUp", "PageDown",
                "PreviousLetter", "NextLetter",
                "Select", "Back",
                // Playback control
                "PlayState", "PlayNext", "Play",
                "PlayMediaSource", "PlayTrailers",
                "SetRepeatMode", "SetShuffleQueue", "SetPlaybackOrder",
                // Volume
                "Mute", "Unmute", "ToggleMute",
                "SetVolume", "VolumeUp", "VolumeDown",
                // Stream selection
                "SetAudioStreamIndex", "SetSubtitleStreamIndex",
                "SetMaxStreamingBitrate",
                // UI
                "ToggleOsd", "ToggleOsdMenu", "ToggleContextMenu", "ToggleFullscreen",
                "DisplayContent", "DisplayMessage",
                "GoHome", "GoToSettings", "GoToSearch",
                // Misc
                "SendKey", "SendString",
                "ChannelUp", "ChannelDown", "Guide",
                "ToggleStats",
            ),
            supportsMediaControl = true,
            supportsPersistentIdentifier = true, // Device has persistent deviceId
        )
        android.util.Log.d("JellyfinClient", "registerSessionCapabilities: sending ${body.supportedCommands.size} commands")
        val response = httpClient.post("$baseUrl/Sessions/Capabilities/Full") {
            header("Authorization", authHeader())
            setBody(body)
        }
        android.util.Log.d("JellyfinClient", "registerSessionCapabilities: status=${response.status}")
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "registerSessionCapabilities failed: $errorBody")
            throw Exception("Failed to register capabilities: ${response.status}")
        }
    }

    private val baseUrl: String get() = serverUrl ?: error("Server URL not set")

    /**
     * Get the configured server base URL.
     * Used for building URLs for external resources like subtitles.
     */
    fun getServerBaseUrl(): String = baseUrl

    private fun authHeader(token: String? = accessToken): String {
        val base = """MediaBrowser Client="MyFlix", Device="Android TV", DeviceId="$deviceId", Version="1.0.0""""
        return if (token != null) """$base, Token="$token"""" else base
    }

    // ==================== Field Constants ====================
    // Only request fields we actually need to minimize data transfer

    private object Fields {
        // Minimal fields for card display (home screen rows)
        // ChildCount/RecursiveItemCount needed to filter shows without episodes
        const val CARD = "Overview,ImageTags,BackdropImageTags,UserData,OfficialRating,CommunityRating,CriticRating,ChildCount,RecursiveItemCount,DisplayOrder"

        // Fields for episode cards (need series info)
        const val EPISODE_CARD = "Overview,ImageTags,BackdropImageTags,UserData,SeriesName,SeasonName,SeasonId,ParentId,OfficialRating,CommunityRating,CriticRating,DisplayOrder"

        // Full fields for detail screens
        const val DETAIL =
            "Overview,ImageTags,BackdropImageTags,UserData,MediaSources,MediaStreams,Genres,Studios,People," +
                "ExternalUrls,ProviderIds,Tags,Chapters,OfficialRating,CommunityRating,CriticRating,Taglines,CollectionIds," +
                "CollectionName,RemoteTrailers,LocalTrailerCount,ProductionLocations,Status,DisplayOrder,Trickplay"

        // Fields for episode listing
        const val EPISODE_LIST =
            "Overview,ImageTags,UserData,MediaSources,People,RunTimeTicks,OfficialRating,CommunityRating,CriticRating,PremiereDate,Chapters"
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
        isFavorite: Boolean? = null,
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
            isFavorite?.let { append("_f$it") }
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
                isFavorite?.let {
                    parameter("isFavorite", it)
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
                parameter("fields", Fields.CARD + ",ProviderIds")
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
        sortBy: String? = null,
        sortOrder: String? = null,
    ): Result<List<JellyfinItem>> {
        val key = CacheKeys.collections(limit) + if (excludeUniverseCollections) ":noUniverse" else "" + ":$sortBy:$sortOrder"
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "BoxSet")
                if (sortBy != null) parameter("sortBy", sortBy)
                if (sortOrder != null) parameter("sortOrder", sortOrder)
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
     * Supports alphabet navigation, sorting, and filtering by genres, watched status, etc.
     */
    suspend fun getCollectionsFiltered(
        limit: Int = 100,
        startIndex: Int = 0,
        sortBy: String? = null,
        sortOrder: String? = null,
        nameStartsWith: String? = null,
        excludeUniverseCollections: Boolean = false,
        genres: List<String>? = null,
        isPlayed: Boolean? = null,
        isFavorite: Boolean? = null,
        minCommunityRating: Float? = null,
        years: String? = null,
        officialRatings: List<String>? = null,
    ): Result<ItemsResponse> {
        val key = CacheKeys.collectionsFiltered(
            limit, startIndex, sortBy ?: "Default", sortOrder ?: "Default",
            nameStartsWith, excludeUniverseCollections, genres, isPlayed,
            isFavorite, minCommunityRating, years, officialRatings,
        )
        getCached<ItemsResponse>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "BoxSet")
                if (sortBy != null) parameter("sortBy", sortBy)
                if (sortOrder != null) parameter("sortOrder", sortOrder)
                parameter("recursive", true)
                parameter("startIndex", startIndex)
                parameter("limit", limit)
                parameter("fields", Fields.CARD + ",Tags,Genres")
                parameter("enableImageTypes", ImageTypes.HERO)
                // Handle letter filter - "#" means items starting with numbers/symbols
                nameStartsWith?.takeIf { it.isNotEmpty() }?.let { letter ->
                    if (letter == "#") {
                        parameter("nameLessThan", "A")
                    } else {
                        parameter("nameStartsWith", letter)
                    }
                }
                // Filter parameters
                genres?.takeIf { it.isNotEmpty() }?.let {
                    parameter("genres", it.joinToString("|"))
                }
                isPlayed?.let { parameter("isPlayed", it) }
                isFavorite?.let { parameter("isFavorite", it) }
                minCommunityRating?.let { parameter("minCommunityRating", it) }
                years?.takeIf { it.isNotEmpty() }?.let { parameter("years", it) }
                officialRatings?.takeIf { it.isNotEmpty() }?.let {
                    parameter("OfficialRatings", it.joinToString(","))
                }
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
     * Get genres available for collections (BoxSets).
     */
    suspend fun getCollectionGenres(): Result<List<JellyfinGenre>> {
        val key = CacheKeys.collectionGenres()
        getCached<List<JellyfinGenre>>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: GenresResponse = httpClient.get("$baseUrl/Genres") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("includeItemTypes", "BoxSet")
                parameter("sortBy", "SortName")
                parameter("sortOrder", "Ascending")
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get collections tagged with "universe-collection".
     * Universe collections are BoxSets with a special tag for franchise groupings.
     */
    suspend fun getUniverseCollections(
        limit: Int = 50,
        sortBy: String? = null,
        sortOrder: String? = null,
    ): Result<List<JellyfinItem>> {
        val key = CacheKeys.universeCollections(limit) + ":$sortBy:$sortOrder"
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "BoxSet")
                parameter("tags", "universe-collection")
                if (sortBy != null) parameter("sortBy", sortBy)
                if (sortOrder != null) parameter("sortOrder", sortOrder)
                parameter("recursive", true)
                parameter("limit", limit)
                parameter("fields", Fields.CARD)
                parameter("enableImageTypes", ImageTypes.HERO)
            }.body()
            r.items.also { putCache(key, it) }
        }
    }

    /**
     * Get universe collections with filtering support.
     * Universe collections are BoxSets with a special tag for franchise groupings.
     */
    suspend fun getUniverseCollectionsFiltered(
        limit: Int = 200,
        sortBy: String? = null,
        sortOrder: String? = null,
        genres: List<String>? = null,
        isPlayed: Boolean? = null,
        isFavorite: Boolean? = null,
        minCommunityRating: Float? = null,
        years: String? = null,
        officialRatings: List<String>? = null,
    ): Result<List<JellyfinItem>> {
        val key = "universeCollectionsFiltered:$limit:${sortBy ?: "Default"}:${sortOrder ?: "Default"}:" +
            "${genres?.joinToString(",") ?: ""}:${isPlayed ?: ""}:${isFavorite ?: ""}:" +
            "${minCommunityRating ?: ""}:${years ?: ""}:${officialRatings?.joinToString(",") ?: ""}"
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.LIBRARIES)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("includeItemTypes", "BoxSet")
                parameter("tags", "universe-collection")
                if (sortBy != null) parameter("sortBy", sortBy)
                if (sortOrder != null) parameter("sortOrder", sortOrder)
                parameter("recursive", true)
                parameter("limit", limit)
                parameter("fields", Fields.CARD + ",Tags,Genres")
                parameter("enableImageTypes", ImageTypes.HERO)
                // Filter parameters
                genres?.takeIf { it.isNotEmpty() }?.let {
                    parameter("genres", it.joinToString("|"))
                }
                isPlayed?.let { parameter("isPlayed", it) }
                isFavorite?.let { parameter("isFavorite", it) }
                minCommunityRating?.let { parameter("minCommunityRating", it) }
                years?.takeIf { it.isNotEmpty() }?.let { parameter("years", it) }
                officialRatings?.takeIf { it.isNotEmpty() }?.let {
                    parameter("OfficialRatings", it.joinToString(","))
                }
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
        sortBy: String? = null,
        sortOrder: String? = null,
    ): Result<List<JellyfinItem>> {
        val key = CacheKeys.collection(collectionId, limit, sortBy ?: "Default", sortOrder ?: "Default")
        getCached<List<JellyfinItem>>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            val r: ItemsResponse = httpClient.get("$baseUrl/Users/$userId/Items") {
                header("Authorization", authHeader())
                parameter("parentId", collectionId)
                if (sortBy != null) parameter("sortBy", sortBy)
                if (sortOrder != null) parameter("sortOrder", sortOrder)
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
     * Get theme media (songs and videos) for an item.
     * Used for background music on detail screens.
     *
     * @param itemId The item ID to get theme media for
     * @param inheritFromParent Whether to search parent items (important for episodes to get series themes)
     */
    suspend fun getThemeMedia(itemId: String, inheritFromParent: Boolean = true): Result<AllThemeMediaResult> {
        val key = CacheKeys.themeMedia(itemId, inheritFromParent)
        getCached<AllThemeMediaResult>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            val r: AllThemeMediaResult = httpClient.get("$baseUrl/Items/$itemId/ThemeMedia") {
                header("Authorization", authHeader())
                parameter("userId", userId)
                parameter("inheritFromParent", inheritFromParent)
            }.body()
            r.also { putCache(key, it) }
        }
    }

    /**
     * Build a URL for streaming an audio item (theme song).
     * Uses static streaming for direct playback without transcoding.
     *
     * @param itemId The audio item ID
     * @param audioCodec Audio codec to use (default: mp3)
     * @param audioBitrate Bitrate in bits per second (default: 128kbps)
     */
    fun getAudioStreamUrl(itemId: String, audioCodec: String = "mp3", audioBitrate: Int = 128_000,): String {
        return "$baseUrl/Audio/$itemId/stream?static=true" +
            "&audioCodec=$audioCodec" +
            "&audioBitrate=$audioBitrate" +
            "&api_key=$accessToken"
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
     * Get lyrics for an audio item.
     * Requires Jellyfin 10.9+ with lyrics support.
     *
     * @param itemId The audio item ID
     * @return LyricDto with metadata and lyric lines, or null if no lyrics
     */
    suspend fun getLyrics(itemId: String): Result<LyricDto?> {
        val key = CacheKeys.lyrics(itemId)
        getCached<LyricDto>(key, CacheKeys.Ttl.ITEM_DETAILS)?.let { return Result.success(it) }
        return runCatching {
            try {
                val response = httpClient.get("$baseUrl/Audio/$itemId/Lyrics") {
                    header("Authorization", authHeader())
                }
                if (response.status.value == 404) {
                    // No lyrics available
                    null
                } else {
                    response.body<LyricDto>().also { putCache(key, it) }
                }
            } catch (e: io.ktor.client.plugins.ClientRequestException) {
                // 404 means no lyrics
                if (e.response.status.value == 404) null else throw e
            }
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

    // ==================== Playlist Management ====================

    /**
     * Get user's playlists.
     */
    suspend fun getUserPlaylists(): Result<List<JellyfinItem>> = runCatching {
        val response = httpClient.get("$baseUrl/Items") {
            header("Authorization", authHeader())
            parameter("userId", userId)
            parameter("includeItemTypes", "Playlist")
            parameter("recursive", true)
            parameter("sortBy", "SortName")
            parameter("sortOrder", "Ascending")
            parameter("fields", "PrimaryImageAspectRatio")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to get playlists: ${response.status}")
        }
        response.body<ItemsResponse>().items
    }

    /**
     * Add item(s) to a playlist.
     */
    suspend fun addToPlaylist(playlistId: String, itemIds: List<String>): Result<Unit> = runCatching {
        val response = httpClient.post("$baseUrl/Playlists/$playlistId/Items") {
            header("Authorization", authHeader())
            parameter("userId", userId)
            parameter("ids", itemIds.joinToString(","))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to add to playlist: ${response.status}")
        }
    }

    /**
     * Create a new playlist.
     * @param name Playlist name
     * @param itemIds Optional initial items to add
     * @param mediaType Media type (Video, Audio, etc.)
     * @return The created playlist ID
     */
    suspend fun createPlaylist(
        name: String,
        itemIds: List<String> = emptyList(),
        mediaType: String = "Video",
    ): Result<String> = runCatching {
        val response = httpClient.post("$baseUrl/Playlists") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "Name" to name,
                    "Ids" to itemIds,
                    "UserId" to userId,
                    "MediaType" to mediaType,
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to create playlist: ${response.status}")
        }
        // Response contains { "Id": "..." }
        val responseBody = response.body<Map<String, String>>()
        responseBody["Id"] ?: throw Exception("No playlist ID in response")
    }

    // ==================== Device Profile Builder ====================

    /**
     * Build a DeviceProfile dynamically based on device codec capabilities.
     * This tells the server what the device can play directly vs what needs transcoding.
     */
    private fun buildDeviceProfile(
        maxStreamingBitrate: Int?,
        preferHdrOverDolbyVision: Boolean = false,
        av1DirectPlayEnabled: Boolean = true,
    ): DeviceProfile {
        val caps = mediaCodecCapabilities

        // Log capabilities on first use
        caps?.let {
            android.util.Log.d(
                "JellyfinClient",
                "Device capabilities: HEVC=${it.supportsHevc()} HEVC10=${it.supportsHevcMain10()} " +
                    "HDR10=${it.supportsHevcHdr10()} HDR10+=${it.supportsHevcHdr10Plus()} " +
                    "AV1=${it.supportsAv1()} AV110=${it.supportsAv1Main10()} AV1HDR10=${it.supportsAv1Hdr10()} " +
                    "AV1DV=${it.supportsAv1DolbyVision()} DV=${it.supportsDolbyVision()}",
            )
        }

        // Check HDR/DV capabilities
        val supportsHevc = caps?.supportsHevc() == true
        val supportsHevcMain10 = caps?.supportsHevcMain10() == true
        val supportsHevcHdr10 = caps?.supportsHevcHdr10() == true
        val supportsHevcHdr10Plus = caps?.supportsHevcHdr10Plus() == true
        val supportsHevcDolbyVision = caps?.supportsDolbyVision() == true
        val supportsAv1 = caps?.supportsAv1() == true
        val supportsAv1Main10 = caps?.supportsAv1Main10() == true
        // AV1 Main 10 can decode HDR10 content - display handles HDR output
        // Many devices don't advertise specific HDR10 profile but still work
        val supportsAv1Hdr10 = caps?.supportsAv1Hdr10() == true || supportsAv1Main10
        // DV Profile 10 uses AV1 Main 10 as base - devices with AV1 Main 10 can decode it
        // The DV RPU metadata can be processed by capable displays or safely ignored
        val supportsAv1DolbyVision = caps?.supportsAv1DolbyVision() == true || supportsAv1Main10

        // Build list of supported video codecs for direct play
        // Note: Don't add dvh1/dvhe - DV content uses hevc codec with DOVI VideoRangeType
        // DV support is controlled via CodecProfile VideoRangeType conditions below
        val videoCodecs = buildList {
            add("h264") // H.264 is universally supported
            if (supportsHevc) add("hevc")
            if (caps?.supportsVp8() == true) add("vp8")
            if (caps?.supportsVp9() == true) add("vp9")
            // Include AV1 if device has decoder (hardware or software) AND user hasn't disabled it
            if (supportsAv1 && av1DirectPlayEnabled) add("av1")
            // VC-1 for older WMV/ASF content (common in DVD rips)
            if (caps?.supportsVc1() == true) add("vc1")
            // MPEG-2 for DVD content
            add("mpeg2video")
            // MPEG-4 Part 2 (DivX/Xvid)
            add("mpeg4")
        }.ifEmpty { listOf("h264", "hevc", "vp8", "vp9") } // Fallback if no caps (exclude AV1 from fallback)

        // Build codec profiles to restrict to supported profiles
        val codecProfiles = buildList {
            // HEVC profile restrictions
            if (caps?.supportsHevc() == true) {
                val supportedHevcProfiles = caps.getSupportedHevcProfiles()
                if (supportedHevcProfiles.isNotEmpty() && !caps.supportsHevcMain10()) {
                    // Device supports HEVC but NOT Main 10 - restrict to "main" only
                    add(
                        CodecProfile(
                            type = "Video",
                            codec = "hevc",
                            conditions = listOf(
                                ProfileCondition(
                                    condition = "EqualsAny",
                                    property = "VideoProfile",
                                    value = supportedHevcProfiles.joinToString("|"),
                                    isRequired = true,
                                ),
                            ),
                        ),
                    )

                    // Also add level restriction if we have it
                    val hevcLevel = caps.getHevcMainLevel()
                    if (hevcLevel > 0) {
                        add(
                            CodecProfile(
                                type = "Video",
                                codec = "hevc",
                                conditions = listOf(
                                    ProfileCondition(
                                        condition = "LessThanEqual",
                                        property = "VideoLevel",
                                        value = hevcLevel.toString(),
                                        isRequired = false,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }

            // VideoRangeType support for HEVC
            // Following the official Jellyfin client pattern:
            // - Build list of unsupported range types
            // - Use NotEquals + applyConditions to tell server what we DON'T support
            // - Key insight: DOVIWithHDR10 can be played via HDR10 fallback even if DV decoder
            //   doesn't support the specific DV profile (e.g., Profile 8.6)
            val unsupportedHevcRangeTypes = buildSet {
                add("DOVIInvalid") // Always exclude invalid DV

                // If no DV decoder at all, exclude all DV types EXCEPT those with fallback support
                if (!supportsHevcDolbyVision) {
                    add("DOVI") // Pure DV with no fallback - requires DV decoder
                    // DOVIWithHDR10 can still play via HDR10 if we support HDR10
                    if (!supportsHevcHdr10) add("DOVIWithHDR10")
                    // DOVIWithHDR10Plus can still play via HDR10+ if we support HDR10+
                    if (!supportsHevcHdr10Plus) add("DOVIWithHDR10Plus")
                }

                // Enhancement Layer DV (Profile 7) - dual-layer MKV format
                // Many Android TV devices can decode Profile 7 but can't properly output DV
                // to the internal panel (works over HDMI but not from Android apps).
                // Always exclude so server converts to HDR10 fallback (which displays correctly).
                add("DOVIWithEL")
                add("DOVIWithELHDR10Plus")

                // HLG DV requires both DV and HLG support
                if (!supportsHevcDolbyVision) add("DOVIWithHLG")

                // HDR10+ support
                if (!supportsHevcHdr10Plus) {
                    add("HDR10Plus")
                    // Also exclude HDR10 if we don't support it
                    if (!supportsHevcHdr10) add("HDR10")
                }
            }

            android.util.Log.d(
                "JellyfinClient",
                "HEVC unsupported VideoRangeTypes: $unsupportedHevcRangeTypes",
            )

            if (unsupportedHevcRangeTypes.isNotEmpty() && supportsHevc) {
                val rangeTypeValue = unsupportedHevcRangeTypes.joinToString("|")
                add(
                    CodecProfile(
                        type = "Video",
                        codec = "hevc",
                        conditions = listOf(
                            ProfileCondition(
                                condition = "NotEquals",
                                property = "VideoRangeType",
                                value = rangeTypeValue,
                                isRequired = false,
                            ),
                        ),
                        applyConditions = listOf(
                            ProfileCondition(
                                condition = "EqualsAny",
                                property = "VideoRangeType",
                                value = rangeTypeValue,
                                isRequired = false,
                            ),
                        ),
                    ),
                )
            }

            // VideoRangeType support for AV1
            // If device supports AV1 Main 10, it can decode all AV1 content including DV Profile 10
            // The display handles HDR output and DV RPU processing
            android.util.Log.d(
                "JellyfinClient",
                "AV1 capabilities: Main10=$supportsAv1Main10 HDR10=$supportsAv1Hdr10 DV=$supportsAv1DolbyVision",
            )

            // Only add VideoRangeType restrictions if device lacks AV1 Main 10 support
            if (!supportsAv1Main10 && supportsAv1) {
                val unsupportedAv1RangeTypes = buildSet {
                    add("DOVIInvalid")
                    add("DOVI")
                    add("DOVIWithHDR10")
                    add("DOVIWithHDR10Plus")
                    add("DOVIWithHLG")
                    add("DOVIWithSDR")
                    add("HDR10")
                    add("HDR10Plus")
                }
                val rangeTypeValue = unsupportedAv1RangeTypes.joinToString("|")
                add(
                    CodecProfile(
                        type = "Video",
                        codec = "av1",
                        conditions = listOf(
                            ProfileCondition(
                                condition = "NotEquals",
                                property = "VideoRangeType",
                                value = rangeTypeValue,
                                isRequired = false,
                            ),
                        ),
                        applyConditions = listOf(
                            ProfileCondition(
                                condition = "EqualsAny",
                                property = "VideoRangeType",
                                value = rangeTypeValue,
                                isRequired = false,
                            ),
                        ),
                    ),
                )
            }
            // If supportsAv1Main10 is true, we don't add any VideoRangeType restrictions
            // This allows all AV1 content including DV Profile 10.x to direct play

            // AV1 profile restrictions
            if (caps?.supportsAv1() == true && !caps.supportsAv1Main10()) {
                // Device supports AV1 but NOT Main 10 - restrict to "Main" only
                add(
                    CodecProfile(
                        type = "Video",
                        codec = "av1",
                        conditions = listOf(
                            ProfileCondition(
                                condition = "EqualsAny",
                                property = "VideoProfile",
                                value = "Main",
                                isRequired = true,
                            ),
                        ),
                    ),
                )
            }

            // H.264 profile and level restrictions
            caps?.let {
                // Profile restriction - exclude High 10 if device doesn't support 10-bit H.264
                // High 10 is very rare but some older anime encodes use it
                val supportedAvcProfiles = it.getSupportedAvcProfiles()
                if (supportedAvcProfiles.isNotEmpty() && !it.supportsAvcHigh10()) {
                    add(
                        CodecProfile(
                            type = "Video",
                            codec = "h264",
                            conditions = listOf(
                                ProfileCondition(
                                    condition = "EqualsAny",
                                    property = "VideoProfile",
                                    value = supportedAvcProfiles.joinToString("|"),
                                    isRequired = true,
                                ),
                            ),
                        ),
                    )
                }

                // Level restriction
                val avcLevel = it.getAvcMainLevel()
                if (avcLevel > 0) {
                    add(
                        CodecProfile(
                            type = "Video",
                            codec = "h264",
                            conditions = listOf(
                                ProfileCondition(
                                    condition = "LessThanEqual",
                                    property = "VideoLevel",
                                    value = avcLevel.toString(),
                                    isRequired = false,
                                ),
                            ),
                        ),
                    )
                }
            }

            // Audio codec profiles - advertise channel support for high-channel audio

            // TrueHD (Atmos) - up to 16 channels, decoded locally via FFmpeg
            add(
                CodecProfile(
                    type = "Audio",
                    codec = "truehd",
                    conditions = listOf(
                        ProfileCondition(
                            condition = "LessThanEqual",
                            property = "AudioChannels",
                            value = "16",
                            isRequired = false,
                        ),
                    ),
                ),
            )

            // E-AC3/DD+ (Atmos/JOC) - supports up to 16 channels for object-based audio
            add(
                CodecProfile(
                    type = "Audio",
                    codec = "eac3",
                    conditions = listOf(
                        ProfileCondition(
                            condition = "LessThanEqual",
                            property = "AudioChannels",
                            value = "16",
                            isRequired = false,
                        ),
                    ),
                ),
            )

            // AC3 (Dolby Digital) - up to 6 channels (5.1)
            add(
                CodecProfile(
                    type = "Audio",
                    codec = "ac3",
                    conditions = listOf(
                        ProfileCondition(
                            condition = "LessThanEqual",
                            property = "AudioChannels",
                            value = "6",
                            isRequired = false,
                        ),
                    ),
                ),
            )

            // DTS and DCA - up to 8 channels (DTS-HD MA supports 7.1)
            add(
                CodecProfile(
                    type = "Audio",
                    codec = "dts",
                    conditions = listOf(
                        ProfileCondition(
                            condition = "LessThanEqual",
                            property = "AudioChannels",
                            value = "8",
                            isRequired = false,
                        ),
                    ),
                ),
            )
            add(
                CodecProfile(
                    type = "Audio",
                    codec = "dca",
                    conditions = listOf(
                        ProfileCondition(
                            condition = "LessThanEqual",
                            property = "AudioChannels",
                            value = "8",
                            isRequired = false,
                        ),
                    ),
                ),
            )

            // FLAC - up to 8 channels
            add(
                CodecProfile(
                    type = "Audio",
                    codec = "flac",
                    conditions = listOf(
                        ProfileCondition(
                            condition = "LessThanEqual",
                            property = "AudioChannels",
                            value = "8",
                            isRequired = false,
                        ),
                    ),
                ),
            )
        }

        // Build transcoding profile - prefer HEVC if supported, otherwise H.264
        val transcodingVideoCodec = if (caps?.supportsHevc() == true) {
            "hevc,h264" // Prefer HEVC, fallback to H.264
        } else {
            "h264"
        }

        // Match official Jellyfin client container and codec lists
        val videoContainers = "asf,avi,hls,m4v,mkv,mov,mp4,ogm,ogv,ts,vob,webm,wmv,xvid"
        val audioContainers = "aac,flac,m4a,mp3,ogg,opus,wav,wma"
        // Audio codecs for DIRECT PLAY - includes TrueHD/MLP for local FFmpeg decode
        val directPlayAudioCodecs = "aac,aac_latm,ac3,alac,dca,dts,eac3,flac,mlp,mp2,mp3,opus,pcm_alaw,pcm_mulaw,pcm_s16le,pcm_s20le,pcm_s24le,truehd,vorbis"
        // Audio codecs for TRANSCODING - excludes TrueHD/MLP as they don't work in HLS containers
        val transcodingAudioCodecs = "aac,ac3,eac3,mp3,alac,flac,opus"

        return DeviceProfile(
            name = "MyFlix-Android",
            maxStreamingBitrate = maxStreamingBitrate,
            directPlayProfiles = listOf(
                DirectPlayProfile(
                    type = "Video",
                    container = videoContainers,
                    videoCodec = videoCodecs.joinToString(","),
                    audioCodec = directPlayAudioCodecs,
                ),
                DirectPlayProfile(
                    type = "Audio",
                    container = audioContainers,
                    audioCodec = directPlayAudioCodecs,
                ),
            ),
            transcodingProfiles = listOf(
                // MPEG-TS HLS for broad compatibility
                TranscodingProfile(
                    type = "Video",
                    container = "ts",
                    videoCodec = transcodingVideoCodec,
                    audioCodec = transcodingAudioCodecs,
                    protocol = "hls",
                    context = "Streaming",
                    copyTimestamps = false,
                    enableSubtitlesInManifest = true,
                ),
                // fMP4 HLS for advanced audio codecs (excludes TrueHD - doesn't work in HLS)
                TranscodingProfile(
                    type = "Video",
                    container = "mp4",
                    videoCodec = transcodingVideoCodec,
                    audioCodec = transcodingAudioCodecs,
                    protocol = "hls",
                    context = "Streaming",
                    copyTimestamps = false,
                    enableSubtitlesInManifest = true,
                ),
                TranscodingProfile(
                    type = "Audio",
                    container = "ts",
                    videoCodec = "",
                    audioCodec = "aac",
                    protocol = "hls",
                    context = "Streaming",
                ),
            ),
            // Match official Jellyfin client subtitle profiles
            subtitleProfiles = listOf(
                // WebVTT - works with HLS
                SubtitleProfile(format = "vtt", method = "Embed"),
                SubtitleProfile(format = "vtt", method = "Hls"),
                SubtitleProfile(format = "vtt", method = "External"),
                SubtitleProfile(format = "webvtt", method = "Embed"),
                SubtitleProfile(format = "webvtt", method = "Hls"),
                SubtitleProfile(format = "webvtt", method = "External"),
                // SRT/SUBRIP
                SubtitleProfile(format = "srt", method = "Embed"),
                SubtitleProfile(format = "srt", method = "External"),
                SubtitleProfile(format = "subrip", method = "Embed"),
                SubtitleProfile(format = "subrip", method = "External"),
                // TTML
                SubtitleProfile(format = "ttml", method = "Embed"),
                SubtitleProfile(format = "ttml", method = "External"),
                // Image-based subtitles - ExoPlayer supports these natively (v0.19+ parity)
                // VobSub (DVD subtitles)
                SubtitleProfile(format = "dvdsub", method = "Embed"),
                SubtitleProfile(format = "dvdsub", method = "External"),
                SubtitleProfile(format = "vobsub", method = "Embed"),
                SubtitleProfile(format = "vobsub", method = "External"),
                SubtitleProfile(format = "idx", method = "External"),
                SubtitleProfile(format = "sub", method = "External"),
                // DVB subtitles (broadcast)
                SubtitleProfile(format = "dvbsub", method = "Embed"),
                SubtitleProfile(format = "dvbsub", method = "External"),
                // PGS (Blu-ray subtitles)
                SubtitleProfile(format = "pgs", method = "Embed"),
                SubtitleProfile(format = "pgs", method = "External"),
                SubtitleProfile(format = "pgssub", method = "Embed"),
                SubtitleProfile(format = "pgssub", method = "External"),
                // ASS/SSA - ExoPlayer has basic support, fallback to encode for complex styling
                SubtitleProfile(format = "ass", method = "Embed"),
                SubtitleProfile(format = "ass", method = "External"),
                SubtitleProfile(format = "ssa", method = "Embed"),
                SubtitleProfile(format = "ssa", method = "External"),
            ),
            codecProfiles = codecProfiles,
        )
    }

    // ==================== Playback Info API ====================

    /**
     * Get playback info from server, which determines if transcoding is needed
     * and returns the appropriate stream URL.
     *
     * @param itemId The item to play
     * @param mediaSourceId Optional media source ID
     * @param audioStreamIndex Audio stream index to select
     * @param subtitleStreamIndex Subtitle stream index to select
     * @param maxBitrateMbps Max streaming bitrate in Mbps (null = unlimited/direct play)
     * @return PlaybackInfoResponse with media sources and transcoding URLs
     */
    suspend fun getPlaybackInfo(
        itemId: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        maxBitrateMbps: Int? = null,
        preferHdrOverDolbyVision: Boolean = false,
        av1DirectPlayEnabled: Boolean = true,
    ): Result<PlaybackInfoResponse> = runCatching {
        val isDirectPlay = maxBitrateMbps == null || maxBitrateMbps == 0
        // Always provide a bitrate even for direct play - server uses this to configure
        // encoder if transcoding is needed (e.g., AV1+DV). Default 200 Mbps matches
        // official client behavior and prevents VAAPI encoder errors from -b:v 0.
        val bitrateBps = if (isDirectPlay) 200_000_000 else maxBitrateMbps * 1_000_000

        // Build device profile dynamically based on device codec capabilities
        val deviceProfile = buildDeviceProfile(
            maxStreamingBitrate = bitrateBps,
            preferHdrOverDolbyVision = preferHdrOverDolbyVision,
            av1DirectPlayEnabled = av1DirectPlayEnabled,
        )

        val request = PlaybackInfoRequest(
            mediaSourceId = mediaSourceId,
            // Use null for direct play to not limit, but use actual bitrate for transcode
            maxStreamingBitrate = if (isDirectPlay) null else bitrateBps,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            enableDirectPlay = isDirectPlay,
            enableDirectStream = isDirectPlay,
            // Always enable transcoding as fallback - even with direct play,
            // server may need to transcode if codec conditions aren't met (e.g., 10-bit HEVC)
            enableTranscoding = true,
            // Allow stream copy so server can build proper transcoding profile
            // CodecProfiles will still enforce codec restrictions (e.g., 10-bit rejection)
            allowVideoStreamCopy = true,
            allowAudioStreamCopy = true,
            enableAutoStreamCopy = true,
            autoOpenLiveStream = true,
            deviceProfile = deviceProfile,
        )

        // Log full DeviceProfile for debugging
        android.util.Log.d(
            "JellyfinClient",
            "getPlaybackInfo: POST $baseUrl/Items/$itemId/PlaybackInfo " +
                "directPlay=$isDirectPlay bitrate=$bitrateBps " +
                "videoCodecs=${deviceProfile.directPlayProfiles.firstOrNull()?.videoCodec}",
        )

        // Log CodecProfiles for VideoRangeType debugging
        deviceProfile.codecProfiles.forEach { profile ->
            if (profile.conditions.any { it.property == "VideoRangeType" }) {
                android.util.Log.d(
                    "JellyfinClient",
                    "VideoRangeType CodecProfile: codec=${profile.codec} " +
                        "conditions=${profile.conditions.map { "${it.condition}:${it.property}=${it.value}" }} " +
                        "applyConditions=${profile.applyConditions.map { "${it.condition}:${it.property}=${it.value}" }}",
                )
            }
        }

        // Log full DeviceProfile JSON for comparison with official client
        try {
            val profileJson = json.encodeToString(DeviceProfile.serializer(), deviceProfile)
            android.util.Log.d("JellyfinClient", "DeviceProfile JSON: $profileJson")
        } catch (e: Exception) {
            android.util.Log.e("JellyfinClient", "Failed to serialize DeviceProfile: ${e.message}")
        }

        val response = httpClient.post("$baseUrl/Items/$itemId/PlaybackInfo") {
            header("Authorization", authHeader())
            parameter("userId", userId)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "getPlaybackInfo failed: $errorBody")
            throw Exception("PlaybackInfo failed: ${response.status}")
        }

        val playbackInfo: PlaybackInfoResponse = response.body()
        android.util.Log.d(
            "JellyfinClient",
            "getPlaybackInfo: playSessionId=${playbackInfo.playSessionId} " +
                "sources=${playbackInfo.mediaSources.size}",
        )

        // Log transcoding URL and liveStreamId if present
        playbackInfo.mediaSources.firstOrNull()?.let { source ->
            android.util.Log.d(
                "JellyfinClient",
                "getPlaybackInfo: directPlay=${source.supportsDirectPlay} " +
                    "directStream=${source.supportsDirectStream} " +
                    "transcode=${source.supportsTranscoding} " +
                    "liveStreamId=${source.liveStreamId} " +
                    "transcodingUrl=${source.transcodingUrl?.take(100)}...",
            )
        }

        playbackInfo
    }

    /**
     * Result from getStreamUrlWithSession containing URL and session tracking info.
     */
    data class StreamUrlResult(
        val streamUrl: String,
        val playSessionId: String?,
        val liveStreamId: String?,
        val playMethod: String,
        val transcodeReasons: List<String>? = null,
        /** VideoRangeType from the selected media source (e.g., "DOVIWithHDR10", "HDR10", "SDR") */
        val videoRangeType: String? = null,
    )

    /**
     * Determine why transcoding is needed based on content and device capabilities.
     * Returns a list of transcode reason strings that can be included in the stream URL.
     */
    private fun determineTranscodeReasons(source: MediaSource): List<String> {
        val reasons = mutableListOf<String>()
        val videoStream = source.mediaStreams?.find { it.type == "Video" }

        if (videoStream != null) {
            val codec = videoStream.codec?.lowercase()
            val profile = videoStream.profile?.lowercase()
            val bitDepth = videoStream.bitDepth

            when (codec) {
                "hevc", "h265" -> {
                    if (mediaCodecCapabilities?.supportsHevc() != true) {
                        reasons.add("VideoCodecNotSupported")
                    } else if (profile?.contains("main 10") == true || bitDepth == 10) {
                        // Device supports HEVC but check if it supports 10-bit
                        if (mediaCodecCapabilities?.supportsHevcMain10() != true) {
                            reasons.add("VideoProfileNotSupported")
                        }
                    }
                }
                "av1" -> {
                    if (mediaCodecCapabilities?.supportsAv1() != true) {
                        reasons.add("VideoCodecNotSupported")
                    } else if (profile?.contains("main 10") == true || bitDepth == 10) {
                        if (mediaCodecCapabilities?.supportsAv1Main10() != true) {
                            reasons.add("VideoProfileNotSupported")
                        }
                    }
                }
                "vp9" -> {
                    if (mediaCodecCapabilities?.supportsVp9() != true) {
                        reasons.add("VideoCodecNotSupported")
                    } else if (bitDepth == 10) {
                        if (mediaCodecCapabilities?.supportsVp9Profile2() != true) {
                            reasons.add("VideoProfileNotSupported")
                        }
                    }
                }
                "h264", "avc" -> {
                    // H.264 is universally supported, but check level
                    val level = videoStream.level
                    if (level != null && level > 51) {
                        reasons.add("VideoLevelNotSupported")
                    }
                }
                else -> {
                    // Unknown codec - assume not supported
                    if (codec != null) {
                        reasons.add("VideoCodecNotSupported")
                    }
                }
            }

            // Check for HDR/Dolby Vision which may require transcoding
            val rangeType = videoStream.videoRangeType?.lowercase()
            if ((rangeType?.contains("dovi") == true || rangeType?.contains("dolby") == true) &&
                !reasons.contains("VideoProfileNotSupported")
            ) {
                // Dolby Vision often requires transcoding
                reasons.add("VideoRangeTypeNotSupported")
            }
        }

        // If no specific reason found but direct play is not supported, add generic reason
        if (reasons.isEmpty() && !source.supportsDirectPlay) {
            reasons.add("DirectPlayError")
        }

        return reasons
    }

    /**
     * Get the stream URL for playback.
     * Uses PlaybackInfo API when transcoding is needed for proper session tracking.
     *
     * @return StreamUrlResult with streamUrl, playSessionId, and liveStreamId
     */
    suspend fun getStreamUrlWithSession(
        itemId: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        maxBitrateMbps: Int? = null,
        preferHdrOverDolbyVision: Boolean = false,
        av1DirectPlayEnabled: Boolean = true,
    ): Result<StreamUrlResult> = runCatching {
        val isTranscoding = maxBitrateMbps != null && maxBitrateMbps > 0

        val playbackInfo = getPlaybackInfo(
            itemId = itemId,
            mediaSourceId = mediaSourceId,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            maxBitrateMbps = maxBitrateMbps,
            preferHdrOverDolbyVision = preferHdrOverDolbyVision,
            av1DirectPlayEnabled = av1DirectPlayEnabled,
        ).getOrThrow()

        val source = playbackInfo.mediaSources.firstOrNull()
            ?: throw Exception("No media sources available")

        val transcodingUrl = source.transcodingUrl

        // Determine play method based on server response
        val playMethod: String
        val streamUrl: String

        if (!transcodingUrl.isNullOrBlank()) {
            // Server determined transcoding is needed
            playMethod = "Transcode"
            val url = if (transcodingUrl.startsWith("http")) {
                transcodingUrl
            } else {
                "$baseUrl$transcodingUrl"
            }
            // Ensure API key is in URL
            streamUrl = if (!url.contains("api_key=") && !url.contains("ApiKey=")) {
                if (url.contains("?")) {
                    "$url&api_key=$accessToken"
                } else {
                    "$url?api_key=$accessToken"
                }
            } else {
                url
            }
        } else if (isTranscoding) {
            // Transcoding requested but no transcodingUrl returned - construct HLS URL directly
            playMethod = "Transcode"
            val bitrateBps = maxBitrateMbps * 1_000_000
            val actualMediaSourceId = mediaSourceId ?: source.id ?: itemId
            // Determine why transcoding is needed
            val reasons = determineTranscodeReasons(source)
            val params = buildList {
                add("DeviceId=$deviceId")
                add("MediaSourceId=$actualMediaSourceId")
                add("api_key=$accessToken")
                add("MaxStreamingBitrate=$bitrateBps")
                add("VideoBitrate=$bitrateBps")
                add("VideoCodec=h264")
                add("AudioCodec=aac")
                add("SegmentContainer=ts")
                add("EnableAutoStreamCopy=false")
                audioStreamIndex?.let { add("AudioStreamIndex=$it") }
                subtitleStreamIndex?.let {
                    add("SubtitleStreamIndex=$it")
                    // Burn subtitles into video stream for transcoding
                    add("SubtitleMethod=Encode")
                }
                playbackInfo.playSessionId?.let { add("PlaySessionId=$it") }
                if (reasons.isNotEmpty()) {
                    add("TranscodeReasons=${reasons.joinToString(",")}")
                }
            }
            streamUrl = "$baseUrl/Videos/$itemId/master.m3u8?${params.joinToString("&")}"
        } else if (source.supportsDirectPlay) {
            // Direct play - file can be played as-is
            playMethod = "DirectPlay"
            val params = buildList {
                add("Static=true")
                add("api_key=$accessToken")
                mediaSourceId?.let { add("MediaSourceId=$it") }
                audioStreamIndex?.let { add("AudioStreamIndex=$it") }
                subtitleStreamIndex?.let { add("SubtitleStreamIndex=$it") }
                playbackInfo.playSessionId?.let { add("PlaySessionId=$it") }
            }
            streamUrl = "$baseUrl/Videos/$itemId/stream?${params.joinToString("&")}"
        } else if (source.supportsDirectStream) {
            // Direct stream - container remuxing without transcoding
            playMethod = "DirectStream"
            val params = buildList {
                add("Static=true")
                add("api_key=$accessToken")
                mediaSourceId?.let { add("MediaSourceId=$it") }
                audioStreamIndex?.let { add("AudioStreamIndex=$it") }
                subtitleStreamIndex?.let { add("SubtitleStreamIndex=$it") }
                playbackInfo.playSessionId?.let { add("PlaySessionId=$it") }
            }
            streamUrl = "$baseUrl/Videos/$itemId/stream?${params.joinToString("&")}"
        } else {
            // Fallback to transcode if nothing else is supported
            // Use device capabilities to choose transcoding codec
            // Default bitrate of 20 Mbps for quality transcoding
            playMethod = "Transcode"
            val actualMediaSourceId = mediaSourceId ?: source.id ?: itemId
            val defaultBitrate = 20_000_000 // 20 Mbps default for quality transcoding

            // Choose transcoding codec based on device capabilities
            // Prefer HEVC if device supports it (better quality at same bitrate)
            val transcodingVideoCodec = if (mediaCodecCapabilities?.supportsHevc() == true) {
                "hevc"
            } else {
                "h264"
            }

            // Determine why transcoding is needed
            val reasons = determineTranscodeReasons(source)

            // Get video stream info for codec-specific parameters
            val videoStream = source.mediaStreams?.find { it.type == "Video" }
            val videoCodec = videoStream?.codec?.lowercase()
            val videoBitDepth = videoStream?.bitDepth
            val videoProfile = videoStream?.profile?.lowercase()?.replace(" ", "")

            val params = buildList {
                add("DeviceId=$deviceId")
                add("MediaSourceId=$actualMediaSourceId")
                add("api_key=$accessToken")
                add("MaxStreamingBitrate=$defaultBitrate")
                add("VideoBitrate=$defaultBitrate")
                add("VideoCodec=$transcodingVideoCodec")
                add("AudioCodec=aac")
                add("SegmentContainer=ts")
                add("EnableAutoStreamCopy=false")
                audioStreamIndex?.let { add("AudioStreamIndex=$it") }
                subtitleStreamIndex?.let {
                    add("SubtitleStreamIndex=$it")
                    // Burn subtitles into video stream for transcoding
                    add("SubtitleMethod=Encode")
                }
                playbackInfo.playSessionId?.let { add("PlaySessionId=$it") }
                // Add codec-specific parameters to help server identify transcoding reason
                if (videoCodec == "hevc" || videoCodec == "h265") {
                    videoBitDepth?.let { add("hevc-videobitdepth=$it") }
                    videoProfile?.let { add("hevc-profile=$it") }
                }
                if (reasons.isNotEmpty()) {
                    add("TranscodeReasons=${reasons.joinToString(",")}")
                }
            }
            streamUrl = "$baseUrl/Videos/$itemId/master.m3u8?${params.joinToString("&")}"
        }

        // Determine final transcode reasons - use server-provided if available, otherwise determine locally
        val finalTranscodeReasons = if (playMethod == "Transcode") {
            source.transcodeReasons?.takeIf { it.isNotEmpty() } ?: determineTranscodeReasons(source)
        } else {
            null
        }

        // Get VideoRangeType from the video stream for DV-aware player selection
        val videoRangeType = source.mediaStreams
            ?.firstOrNull { it.type == "Video" }
            ?.videoRangeType

        android.util.Log.d(
            "JellyfinClient",
            "getStreamUrlWithSession: playMethod=$playMethod supportsDirectPlay=${source.supportsDirectPlay} " +
                "supportsDirectStream=${source.supportsDirectStream} transcodingUrl=${transcodingUrl != null} " +
                "transcodeReasons=$finalTranscodeReasons videoRangeType=$videoRangeType",
        )
        StreamUrlResult(
            streamUrl = streamUrl,
            playSessionId = playbackInfo.playSessionId,
            liveStreamId = source.liveStreamId,
            playMethod = playMethod,
            transcodeReasons = finalTranscodeReasons,
            videoRangeType = videoRangeType,
        )
    }

    // ==================== URL Helpers ====================

    /** Simple stream URL for direct play (no bitrate limit) */
    fun getStreamUrl(itemId: String): String = getStreamUrl(itemId, null, null, null)

    /** Simple stream URL builder - use getStreamUrlWithSession for transcoding support */
    fun getStreamUrl(
        itemId: String,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        @Suppress("UNUSED_PARAMETER") maxBitrateMbps: Int? = null,
    ): String {
        // Simple direct stream URL - for transcoding, use getStreamUrlWithSession instead
        val params = buildList {
            add("Static=true")
            add("api_key=$accessToken")
            audioStreamIndex?.let { add("AudioStreamIndex=$it") }
            subtitleStreamIndex?.let { add("SubtitleStreamIndex=$it") }
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

    /**
     * Get trickplay tile image URL for seek preview thumbnails.
     * Trickplay images are tile grids containing multiple thumbnails.
     *
     * @param itemId The item ID
     * @param width The thumbnail width from TrickplayInfo
     * @param tileIndex The tile grid image index (0-based)
     * @param mediaSourceId Optional media source ID (uses first media source if null)
     */
    fun getTrickplayTileUrl(
        itemId: String,
        width: Int,
        tileIndex: Int,
        mediaSourceId: String? = null,
    ): String {
        val params = buildList {
            add("api_key=$accessToken")
            if (mediaSourceId != null) {
                add("mediaSourceId=$mediaSourceId")
            }
        }.joinToString("&")
        return "$baseUrl/Videos/$itemId/Trickplay/$width/$tileIndex.jpg?$params"
    }

    // ==================== Playback Reporting ====================

    private var currentPlaySessionId: String? = null

    suspend fun reportPlaybackStart(
        itemId: String,
        mediaSourceId: String? = null,
        positionTicks: Long = 0,
        playSessionId: String? = null,
        liveStreamId: String? = null,
        playMethod: String = "DirectPlay",
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        maxStreamingBitrate: Long? = null,
        transcodeReasons: List<String>? = null,
    ): Result<Unit> = runCatching {
        val sessionId = playSessionId ?: "${itemId}_${System.currentTimeMillis()}"
        currentPlaySessionId = sessionId
        val body = PlaybackStartInfo(
            itemId = itemId,
            mediaSourceId = mediaSourceId ?: itemId,
            positionTicks = positionTicks,
            playMethod = playMethod,
            playSessionId = sessionId,
            liveStreamId = liveStreamId,
            canSeek = true,
            isPaused = false,
            isMuted = false,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            maxStreamingBitrate = maxStreamingBitrate,
            transcodeReasons = transcodeReasons,
        )
        android.util.Log.d(
            "JellyfinClient",
            "reportPlaybackStart: POST $baseUrl/Sessions/Playing itemId=$itemId " +
                "playMethod=$playMethod playSessionId=$sessionId liveStreamId=$liveStreamId",
        )
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
        liveStreamId: String? = null,
        playMethod: String = "DirectPlay",
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        maxStreamingBitrate: Long? = null,
        transcodeReasons: List<String>? = null,
    ): Result<Unit> = runCatching {
        val body = PlaybackProgressInfo(
            itemId = itemId,
            mediaSourceId = mediaSourceId ?: itemId,
            positionTicks = positionTicks,
            isPaused = isPaused,
            isMuted = false,
            playMethod = playMethod,
            playSessionId = currentPlaySessionId,
            liveStreamId = liveStreamId,
            canSeek = true,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            maxStreamingBitrate = maxStreamingBitrate,
            transcodeReasons = transcodeReasons,
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
        liveStreamId: String? = null,
    ): Result<Unit> = runCatching {
        val body = PlaybackStopInfo(
            itemId = itemId,
            mediaSourceId = mediaSourceId ?: itemId,
            positionTicks = positionTicks,
            playSessionId = currentPlaySessionId,
            liveStreamId = liveStreamId,
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

    // ==================== Remote Subtitle Search ====================

    /**
     * Search for remote subtitles for an item using the server's OpenSubtitles plugin.
     * Requires the OpenSubtitles plugin to be installed and configured on the server.
     *
     * @param itemId The item ID to search subtitles for
     * @param language Two-letter ISO language code (e.g., "en", "es", "fr")
     * @return List of available remote subtitles sorted by rating and download count
     */
    suspend fun searchRemoteSubtitles(
        itemId: String,
        language: String,
    ): Result<List<RemoteSubtitleInfo>> = runCatching {
        android.util.Log.d("JellyfinClient", "searchRemoteSubtitles: itemId=$itemId language=$language")
        val response: List<RemoteSubtitleInfo> = httpClient.get("$baseUrl/Items/$itemId/RemoteSearch/Subtitles/$language") {
            header("Authorization", authHeader())
        }.body()
        // Sort by rating then download count
        response.sortedWith(
            compareByDescending<RemoteSubtitleInfo> { it.communityRating ?: 0.0 }
                .thenByDescending { it.downloadCount ?: 0 },
        )
    }

    /**
     * Download a remote subtitle to the server for an item.
     * The server will download the subtitle and add it to the item's available streams.
     *
     * @param itemId The item ID to add the subtitle to
     * @param subtitleId The remote subtitle ID (from search results)
     */
    suspend fun downloadRemoteSubtitle(
        itemId: String,
        subtitleId: String,
    ): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "downloadRemoteSubtitle: itemId=$itemId subtitleId=$subtitleId")
        val response = httpClient.post("$baseUrl/Items/$itemId/RemoteSearch/Subtitles/$subtitleId") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            android.util.Log.e("JellyfinClient", "downloadRemoteSubtitle failed: $errorBody")
            throw Exception("Failed to download subtitle: ${response.status}")
        }
    }

    // ==================== SyncPlay API ====================

    /**
     * Get available SyncPlay groups.
     */
    suspend fun syncPlayGetGroups(): Result<List<SyncPlayGroup>> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayGetGroups: GET $baseUrl/SyncPlay/List")
        val response = httpClient.get("$baseUrl/SyncPlay/List") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to get SyncPlay groups: ${response.status}")
        }
        response.body<List<SyncPlayGroup>>()
    }

    /**
     * Create a new SyncPlay group.
     */
    suspend fun syncPlayCreateGroup(groupName: String): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayCreateGroup: POST $baseUrl/SyncPlay/New")
        val response = httpClient.post("$baseUrl/SyncPlay/New") {
            header("Authorization", authHeader())
            setBody(mapOf("GroupName" to groupName))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to create SyncPlay group: ${response.status}")
        }
    }

    /**
     * Join an existing SyncPlay group.
     */
    suspend fun syncPlayJoinGroup(groupId: String): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayJoinGroup: POST $baseUrl/SyncPlay/Join")
        val response = httpClient.post("$baseUrl/SyncPlay/Join") {
            header("Authorization", authHeader())
            setBody(mapOf("GroupId" to groupId))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to join SyncPlay group: ${response.status}")
        }
    }

    /**
     * Leave the current SyncPlay group.
     */
    suspend fun syncPlayLeaveGroup(): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayLeaveGroup: POST $baseUrl/SyncPlay/Leave")
        val response = httpClient.post("$baseUrl/SyncPlay/Leave") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to leave SyncPlay group: ${response.status}")
        }
    }

    /**
     * Request group to start/resume playback.
     */
    suspend fun syncPlayPlay(): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayPlay: POST $baseUrl/SyncPlay/Unpause")
        val response = httpClient.post("$baseUrl/SyncPlay/Unpause") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to request SyncPlay play: ${response.status}")
        }
    }

    /**
     * Request group to pause playback.
     */
    suspend fun syncPlayPause(): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayPause: POST $baseUrl/SyncPlay/Pause")
        val response = httpClient.post("$baseUrl/SyncPlay/Pause") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to request SyncPlay pause: ${response.status}")
        }
    }

    /**
     * Request group to seek to position.
     */
    suspend fun syncPlaySeek(positionTicks: Long): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlaySeek: POST $baseUrl/SyncPlay/Seek position=$positionTicks")
        val response = httpClient.post("$baseUrl/SyncPlay/Seek") {
            header("Authorization", authHeader())
            setBody(mapOf("PositionTicks" to positionTicks))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to request SyncPlay seek: ${response.status}")
        }
    }

    /**
     * Request group to stop playback.
     */
    suspend fun syncPlayStop(): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayStop: POST $baseUrl/SyncPlay/Stop")
        val response = httpClient.post("$baseUrl/SyncPlay/Stop") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to request SyncPlay stop: ${response.status}")
        }
    }

    /**
     * Set the group's play queue.
     */
    suspend fun syncPlaySetQueue(
        itemIds: List<String>,
        startIndex: Int = 0,
        startPositionTicks: Long = 0,
    ): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlaySetQueue: POST $baseUrl/SyncPlay/SetNewQueue items=${itemIds.size}")
        val response = httpClient.post("$baseUrl/SyncPlay/SetNewQueue") {
            header("Authorization", authHeader())
            setBody(
                mapOf(
                    "PlayingQueue" to itemIds,
                    "PlayingItemPosition" to startIndex,
                    "StartPositionTicks" to startPositionTicks,
                ),
            )
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to set SyncPlay queue: ${response.status}")
        }
    }

    /**
     * Add items to the group's play queue.
     */
    suspend fun syncPlayQueueAdd(itemIds: List<String>): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayQueueAdd: POST $baseUrl/SyncPlay/Queue items=${itemIds.size}")
        val response = httpClient.post("$baseUrl/SyncPlay/Queue") {
            header("Authorization", authHeader())
            setBody(mapOf("ItemIds" to itemIds))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to add to SyncPlay queue: ${response.status}")
        }
    }

    /**
     * Request group to play next item in queue.
     */
    suspend fun syncPlayQueueNext(): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayQueueNext: POST $baseUrl/SyncPlay/NextItem")
        val response = httpClient.post("$baseUrl/SyncPlay/NextItem") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to request SyncPlay next: ${response.status}")
        }
    }

    /**
     * Request group to play previous item in queue.
     */
    suspend fun syncPlayQueuePrevious(): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayQueuePrevious: POST $baseUrl/SyncPlay/PreviousItem")
        val response = httpClient.post("$baseUrl/SyncPlay/PreviousItem") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to request SyncPlay previous: ${response.status}")
        }
    }

    /**
     * Report buffering state to group.
     */
    suspend fun syncPlayBuffering(
        positionTicks: Long,
        isPlaying: Boolean,
        playlistItemId: String,
    ): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayBuffering: POST $baseUrl/SyncPlay/Buffering")
        val serverTime = java.time.Instant.now().toString()
        val response = httpClient.post("$baseUrl/SyncPlay/Buffering") {
            header("Authorization", authHeader())
            setBody(
                mapOf(
                    "When" to serverTime,
                    "PositionTicks" to positionTicks,
                    "IsPlaying" to isPlaying,
                    "PlaylistItemId" to playlistItemId,
                ),
            )
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to report SyncPlay buffering: ${response.status}")
        }
    }

    /**
     * Report ready state to group.
     */
    suspend fun syncPlayReady(
        positionTicks: Long,
        isPlaying: Boolean,
        playlistItemId: String,
    ): Result<Unit> = runCatching {
        android.util.Log.d("JellyfinClient", "syncPlayReady: POST $baseUrl/SyncPlay/Ready")
        val serverTime = java.time.Instant.now().toString()
        val response = httpClient.post("$baseUrl/SyncPlay/Ready") {
            header("Authorization", authHeader())
            setBody(
                mapOf(
                    "When" to serverTime,
                    "PositionTicks" to positionTicks,
                    "IsPlaying" to isPlaying,
                    "PlaylistItemId" to playlistItemId,
                ),
            )
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to report SyncPlay ready: ${response.status}")
        }
    }

    /**
     * Send ping to server for latency tracking.
     */
    suspend fun syncPlayPing(ping: Long): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/SyncPlay/Ping") {
            header("Authorization", authHeader())
            setBody(mapOf("Ping" to ping))
        }
        // Ping failures are non-critical, don't throw
    }

    /**
     * Get UTC time from server for time synchronization.
     */
    suspend fun getUtcTime(): Result<UtcTimeResponse> = runCatching {
        val response = httpClient.get("$baseUrl/GetUtcTime") {
            header("Authorization", authHeader())
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to get UTC time: ${response.status}")
        }
        response.body<UtcTimeResponse>()
    }
}

// ==================== Session Capabilities ====================

@Serializable
private data class ClientCapabilitiesDto(
    @SerialName("PlayableMediaTypes") val playableMediaTypes: List<String>,
    @SerialName("SupportedCommands") val supportedCommands: List<String>,
    @SerialName("SupportsMediaControl") val supportsMediaControl: Boolean,
    @SerialName("SupportsPersistentIdentifier") val supportsPersistentIdentifier: Boolean,
)

// ==================== Playback Report Data Classes ====================

@Serializable
private data class PlaybackStartInfo(
    @SerialName("ItemId") val itemId: String,
    @SerialName("MediaSourceId") val mediaSourceId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("PlayMethod") val playMethod: String,
    @SerialName("PlaySessionId") val playSessionId: String,
    @SerialName("LiveStreamId") val liveStreamId: String? = null,
    @SerialName("CanSeek") val canSeek: Boolean,
    @SerialName("IsPaused") val isPaused: Boolean,
    @SerialName("IsMuted") val isMuted: Boolean,
    @SerialName("RepeatMode") val repeatMode: String = "RepeatNone",
    @SerialName("PlaybackOrder") val playbackOrder: String = "Default",
    @SerialName("AudioStreamIndex") val audioStreamIndex: Int? = null,
    @SerialName("SubtitleStreamIndex") val subtitleStreamIndex: Int? = null,
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Long? = null,
    @SerialName("TranscodeReasons") val transcodeReasons: List<String>? = null,
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
    @SerialName("LiveStreamId") val liveStreamId: String? = null,
    @SerialName("CanSeek") val canSeek: Boolean,
    @SerialName("RepeatMode") val repeatMode: String = "RepeatNone",
    @SerialName("PlaybackOrder") val playbackOrder: String = "Default",
    @SerialName("AudioStreamIndex") val audioStreamIndex: Int? = null,
    @SerialName("SubtitleStreamIndex") val subtitleStreamIndex: Int? = null,
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Long? = null,
    @SerialName("TranscodeReasons") val transcodeReasons: List<String>? = null,
)

@Serializable
private data class PlaybackStopInfo(
    @SerialName("ItemId") val itemId: String,
    @SerialName("MediaSourceId") val mediaSourceId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("PlaySessionId") val playSessionId: String?,
    @SerialName("LiveStreamId") val liveStreamId: String? = null,
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

/**
 * Remote subtitle information from OpenSubtitles or other providers.
 */
@Serializable
data class RemoteSubtitleInfo(
    @SerialName("ThreeLetterISOLanguageName") val threeLetterIsoLanguageName: String? = null,
    @SerialName("Id") val id: String? = null,
    @SerialName("ProviderName") val providerName: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("Format") val format: String? = null,
    @SerialName("Author") val author: String? = null,
    @SerialName("Comment") val comment: String? = null,
    @SerialName("DateCreated") val dateCreated: String? = null,
    @SerialName("CommunityRating") val communityRating: Double? = null,
    @SerialName("DownloadCount") val downloadCount: Int? = null,
    @SerialName("IsHashMatch") val isHashMatch: Boolean? = null,
    @SerialName("IsForced") val isForced: Boolean? = null,
    @SerialName("IsHearingImpaired") val isHearingImpaired: Boolean? = null,
)
