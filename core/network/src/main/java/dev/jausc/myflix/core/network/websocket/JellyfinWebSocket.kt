package dev.jausc.myflix.core.network.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for real-time communication with Jellyfin server.
 * Enables remote control features (pause, seek, display messages, etc.)
 * from the Jellyfin dashboard.
 *
 * Usage:
 * ```
 * val webSocket = JellyfinWebSocket(scope)
 * webSocket.connect(serverUrl, accessToken, deviceId)
 *
 * // Observe events
 * webSocket.events.collect { event ->
 *     when (event) {
 *         is WebSocketEvent.PlaystateCommand -> handlePlaystate(event)
 *         is WebSocketEvent.GeneralCommand -> handleGeneral(event)
 *         // ...
 *     }
 * }
 * ```
 */
class JellyfinWebSocket(
    private val scope: CoroutineScope,
    okHttpClient: OkHttpClient? = null,
) {
    private val client: OkHttpClient = okHttpClient ?: OkHttpClient.Builder()
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for WebSocket
        .build()

    private val eventParser = WebSocketEventParser()

    // Event flow for consumers
    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Internal state
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var stopped = false

    // Connection parameters (set on connect)
    private var serverUrl: String? = null
    private var accessToken: String? = null
    private var deviceId: String? = null

    // Exponential backoff for reconnection
    private var currentBackoffMs = INITIAL_BACKOFF_MS

    /**
     * Connect to the Jellyfin WebSocket server.
     *
     * @param serverUrl The base server URL (e.g., "http://192.168.1.7:8096")
     * @param accessToken The user's access token
     * @param deviceId The device identifier
     */
    fun connect(serverUrl: String, accessToken: String, deviceId: String) {
        Log.d(TAG, "Connecting to WebSocket: $serverUrl")

        // Store connection parameters for reconnection
        this.serverUrl = serverUrl
        this.accessToken = accessToken
        this.deviceId = deviceId
        this.stopped = false

        doConnect()
    }

    /**
     * Disconnect from the WebSocket server.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        stopped = true
        reconnectJob?.cancel()
        reconnectJob = null

        webSocket?.close(NORMAL_CLOSURE_STATUS, "Client disconnecting")
        webSocket = null

        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Check if currently connected.
     */
    val isConnected: Boolean
        get() = _connectionState.value == ConnectionState.Connected

    private fun doConnect() {
        val url = serverUrl ?: return
        val token = accessToken ?: return
        val device = deviceId ?: return

        _connectionState.value = ConnectionState.Connecting

        val wsUrl = buildWebSocketUrl(url, token, device)
        Log.d(TAG, "WebSocket URL: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                currentBackoffMs = INITIAL_BACKOFF_MS // Reset backoff on successful connect
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: code=$code, reason=$reason")
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: code=$code, reason=$reason")
                _connectionState.value = ConnectionState.Disconnected
                if (!stopped) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
                if (!stopped) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun buildWebSocketUrl(serverUrl: String, accessToken: String, deviceId: String): String {
        // Convert http(s) to ws(s)
        val wsScheme = if (serverUrl.startsWith("https://")) "wss://" else "ws://"
        val host = serverUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')

        return "${wsScheme}$host/socket?api_key=$accessToken&deviceId=$deviceId"
    }

    private fun handleMessage(text: String) {
        val event = eventParser.parse(text)
        when (event) {
            is WebSocketEvent.KeepAlive -> {
                // Respond to keep-alive to maintain connection
                sendKeepAlive()
            }
            null -> {
                // Unknown or unparseable message, ignore
            }
            else -> {
                // Emit event to consumers
                val emitted = _events.tryEmit(event)
                if (!emitted) {
                    Log.w(TAG, "Event buffer full, dropping event: $event")
                }
            }
        }
    }

    private fun sendKeepAlive() {
        val sent = webSocket?.send(KEEP_ALIVE_MESSAGE) ?: false
        if (!sent) {
            Log.w(TAG, "Failed to send KeepAlive response")
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Reconnecting in ${currentBackoffMs}ms")
            delay(currentBackoffMs)

            // Increase backoff for next attempt (exponential with max)
            currentBackoffMs = (currentBackoffMs * BACKOFF_MULTIPLIER)
                .toLong()
                .coerceAtMost(MAX_BACKOFF_MS)

            if (!stopped) {
                doConnect()
            }
        }
    }

    companion object {
        private const val TAG = "JellyfinWebSocket"
        private const val NORMAL_CLOSURE_STATUS = 1000
        private const val PING_INTERVAL_SECONDS = 30L
        private const val EVENT_BUFFER_SIZE = 64

        // Exponential backoff parameters
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 30000L
        private const val BACKOFF_MULTIPLIER = 2.0

        private const val KEEP_ALIVE_MESSAGE = """{"MessageType":"KeepAlive"}"""
    }
}
