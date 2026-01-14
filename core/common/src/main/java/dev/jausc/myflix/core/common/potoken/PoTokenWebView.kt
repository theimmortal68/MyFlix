package dev.jausc.myflix.core.common.potoken

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.MainThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebView-based PoToken generator that uses YouTube's BotGuard to generate tokens.
 */
class PoTokenWebView private constructor(
    context: Context,
    private val initDeferred: CompletableDeferred<PoTokenGenerator>,
) : PoTokenGenerator {

    private val webView = WebView(context)
    private val httpClient = OkHttpClient()
    private val poTokenDeferreds = mutableListOf<Pair<String, CompletableDeferred<String>>>()
    private lateinit var expirationInstant: Instant

    init {
        val webViewSettings = webView.settings
        @Suppress("SetJavaScriptEnabled")
        webViewSettings.javaScriptEnabled = true
        webViewSettings.userAgentString = USER_AGENT
        webViewSettings.blockNetworkLoads = true // WebView doesn't need internet

        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught")) {
                    val fmt = "\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"
                    val exception = BadWebViewException(fmt)
                    Log.e(TAG, "WebView implementation is broken: $fmt")

                    onInitializationError(exception)
                    popAllPoTokenDeferreds().forEach { (_, deferred) ->
                        deferred.completeExceptionally(exception)
                    }
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    /**
     * Loads the HTML and starts the BotGuard initialization process.
     */
    private fun loadHtmlAndObtainBotguard(context: Context) {
        Log.d(TAG, "loadHtmlAndObtainBotguard() called")

        try {
            val html = context.assets.open("po_token.html").bufferedReader().use { it.readText() }
            webView.loadDataWithBaseURL(
                "https://www.youtube.com",
                html.replaceFirst(
                    "</script>",
                    "\n$JS_INTERFACE.downloadAndRunBotguard()</script>"
                ),
                "text/html",
                "utf-8",
                null,
            )
        } catch (e: Exception) {
            onInitializationError(e)
        }
    }

    /**
     * Called by JavaScript after the WebView content has loaded.
     */
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        Log.d(TAG, "downloadAndRunBotguard() called")

        Thread {
            try {
                val responseBody = makeBotguardRequest(
                    "https://www.youtube.com/api/jnn/v1/Create",
                    "[ \"$REQUEST_KEY\" ]"
                )

                val parsedChallengeData = parseChallengeData(responseBody)
                Handler(Looper.getMainLooper()).post {
                    webView.evaluateJavascript(
                        """try {
                            data = $parsedChallengeData
                            runBotGuard(data).then(function (result) {
                                this.webPoSignalOutput = result.webPoSignalOutput
                                $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                            }, function (error) {
                                $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                            })
                        } catch (error) {
                            $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                        }""",
                        null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadAndRunBotguard", e)
                onInitializationError(e)
            }
        }.start()
    }

    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        Log.e(TAG, "Initialization error from JavaScript: $error")
        onInitializationError(buildExceptionForJsError(error))
    }

    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        Log.d(TAG, "botguardResponse received")

        Thread {
            try {
                val responseBody = makeBotguardRequest(
                    "https://www.youtube.com/api/jnn/v1/GenerateIT",
                    "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]"
                )

                val (integrityToken, expirationTimeInSeconds) = parseIntegrityTokenData(responseBody)
                expirationInstant = Instant.now().plusSeconds(expirationTimeInSeconds - 600)

                Handler(Looper.getMainLooper()).post {
                    webView.evaluateJavascript("this.integrityToken = $integrityToken") {
                        Log.d(TAG, "Initialization finished, expiration=${expirationTimeInSeconds}s")
                        initDeferred.complete(this@PoTokenWebView)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onRunBotguardResult", e)
                onInitializationError(e)
            }
        }.start()
    }

    override suspend fun generatePoToken(identifier: String): String {
        Log.d(TAG, "generatePoToken() called with identifier $identifier")

        return suspendCancellableCoroutine { continuation ->
            Handler(Looper.getMainLooper()).post {
                val deferred = CompletableDeferred<String>()
                addPoTokenDeferred(identifier, deferred)

                val u8Identifier = stringToU8(identifier)
                webView.evaluateJavascript(
                    """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }"""
                ) {}

                // Wait for the deferred to complete
                Thread {
                    try {
                        val result = kotlinx.coroutines.runBlocking { deferred.await() }
                        continuation.resume(result)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }.start()
            }
        }
    }

    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        Log.e(TAG, "obtainPoToken error: $error")
        popPoTokenDeferred(identifier)?.completeExceptionally(buildExceptionForJsError(error))
    }

    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
        Log.d(TAG, "Generated poToken for identifier=$identifier")
        try {
            val poToken = u8ToBase64(poTokenU8)
            popPoTokenDeferred(identifier)?.complete(poToken)
        } catch (t: Throwable) {
            popPoTokenDeferred(identifier)?.completeExceptionally(t)
        }
    }

    override fun isExpired(): Boolean {
        return Instant.now().isAfter(expirationInstant)
    }

    private fun addPoTokenDeferred(identifier: String, deferred: CompletableDeferred<String>) {
        synchronized(poTokenDeferreds) {
            poTokenDeferreds.add(Pair(identifier, deferred))
        }
    }

    private fun popPoTokenDeferred(identifier: String): CompletableDeferred<String>? {
        return synchronized(poTokenDeferreds) {
            poTokenDeferreds.indexOfFirst { it.first == identifier }.takeIf { it >= 0 }?.let {
                poTokenDeferreds.removeAt(it).second
            }
        }
    }

    private fun popAllPoTokenDeferreds(): List<Pair<String, CompletableDeferred<String>>> {
        return synchronized(poTokenDeferreds) {
            val result = poTokenDeferreds.toList()
            poTokenDeferreds.clear()
            result
        }
    }

    private fun makeBotguardRequest(url: String, data: String): String {
        val request = Request.Builder()
            .url(url)
            .post(data.toRequestBody("application/json+protobuf".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json+protobuf")
            .header("x-goog-api-key", GOOGLE_API_KEY)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .build()

        val response = httpClient.newCall(request).execute()
        if (response.code != 200) {
            throw PoTokenException("Invalid response code: ${response.code}")
        }
        return response.body?.string() ?: throw PoTokenException("Empty response body")
    }

    private fun onInitializationError(error: Throwable) {
        Handler(Looper.getMainLooper()).post {
            close()
            initDeferred.completeExceptionally(error)
        }
    }

    @MainThread
    override fun close() {
        webView.clearHistory()
        webView.clearCache(true)
        webView.loadUrl("about:blank")
        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
    }

    companion object : PoTokenGenerator.Factory {
        private const val TAG = "PoTokenWebView"
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JS_INTERFACE = "PoTokenWebView"

        /**
         * Checks if the device supports WebView.
         */
        fun supportsWebView(): Boolean {
            return try {
                CookieManager.getInstance()
                true
            } catch (ignored: Throwable) {
                false
            }
        }

        override suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator {
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val initDeferred = CompletableDeferred<PoTokenGenerator>()
                    val potWv = PoTokenWebView(context, initDeferred)
                    potWv.loadHtmlAndObtainBotguard(context)

                    Thread {
                        try {
                            val result = kotlinx.coroutines.runBlocking { initDeferred.await() }
                            continuation.resume(result)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }.start()
                }
            }
        }
    }
}
