package dev.jausc.myflix.core.common.potoken

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper

/**
 * Implementation of PoTokenProvider that uses a WebView-based BotGuard to generate poTokens.
 * This is required for YouTube playback on NewPipeExtractor v0.25.0+.
 */
object PoTokenProviderImpl : PoTokenProvider {
    private const val TAG = "PoTokenProviderImpl"

    private var appContext: Context? = null
    private val webViewSupported by lazy { PoTokenWebView.supportsWebView() }
    private var webViewBadImpl = false

    private val mutex = Mutex()
    private var webPoTokenVisitorData: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenGenerator? = null

    /**
     * Initialize the provider with the application context.
     * Must be called before using the provider.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        if (!webViewSupported || webViewBadImpl) {
            return null
        }

        val context = appContext ?: run {
            Log.e(TAG, "PoTokenProviderImpl not initialized - call initialize() first")
            return null
        }

        return try {
            runBlocking {
                getWebClientPoTokenInternal(context, videoId, forceRecreate = false)
            }
        } catch (e: Exception) {
            when (val cause = e.cause ?: e) {
                is BadWebViewException -> {
                    Log.e(TAG, "Could not obtain poToken because WebView is broken", e)
                    webViewBadImpl = true
                    null
                }
                else -> {
                    Log.e(TAG, "Error getting poToken: ${e.message}")
                    throw cause
                }
            }
        }
    }

    private suspend fun getWebClientPoTokenInternal(
        context: Context,
        videoId: String,
        forceRecreate: Boolean
    ): PoTokenResult {
        data class TokenState(
            val generator: PoTokenGenerator,
            val visitorData: String,
            val streamingPot: String,
            val wasRecreated: Boolean
        )

        val state = mutex.withLock {
            val shouldRecreate = webPoTokenGenerator == null ||
                forceRecreate ||
                webPoTokenGenerator!!.isExpired()

            if (shouldRecreate) {
                Log.d(TAG, "Creating new PoTokenGenerator")

                // Get visitor data from YouTube
                val innertubeClientRequestInfo = InnertubeClientRequestInfo.ofWebClient()
                innertubeClientRequestInfo.clientInfo.clientVersion =
                    YoutubeParsingHelper.getClientVersion()

                webPoTokenVisitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
                    innertubeClientRequestInfo,
                    NewPipe.getPreferredLocalization(),
                    NewPipe.getPreferredContentCountry(),
                    YoutubeParsingHelper.getYouTubeHeaders(),
                    YoutubeParsingHelper.YOUTUBEI_V1_URL,
                    null,
                    false
                )

                // Close existing generator on main thread
                webPoTokenGenerator?.let { gen ->
                    Handler(Looper.getMainLooper()).post { gen.close() }
                }

                // Create new generator
                webPoTokenGenerator = PoTokenWebView.newPoTokenGenerator(context)

                // Generate streaming poToken first (required before other tokens)
                webPoTokenStreamingPot = webPoTokenGenerator!!
                    .generatePoToken(webPoTokenVisitorData!!)
            }

            TokenState(
                webPoTokenGenerator!!,
                webPoTokenVisitorData!!,
                webPoTokenStreamingPot!!,
                shouldRecreate
            )
        }

        val playerPot = try {
            state.generator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (state.wasRecreated) {
                // Already recreated, nothing more we can do
                throw throwable
            } else {
                // Retry with a fresh generator
                Log.e(TAG, "Failed to obtain poToken, retrying", throwable)
                return getWebClientPoTokenInternal(context, videoId, forceRecreate = true)
            }
        }

        Log.d(TAG, "poToken for $videoId: playerPot=$playerPot, " +
            "streamingPot=${state.streamingPot}, visitor_data=${state.visitorData}")

        return PoTokenResult(state.visitorData, playerPot, state.streamingPot)
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null

    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = null

    override fun getIosClientPoToken(videoId: String): PoTokenResult? = null
}
