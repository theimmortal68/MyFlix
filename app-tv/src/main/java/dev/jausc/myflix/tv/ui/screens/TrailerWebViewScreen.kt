package dev.jausc.myflix.tv.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.jausc.myflix.tv.ui.components.TvIconTextButton
import kotlinx.coroutines.delay

private const val TAG = "TrailerWebViewScreen"

@Composable
fun TrailerWebViewScreen(
    videoKey: String,
    title: String?,
    onBack: () -> Unit,
) {
    Log.d(TAG, "=== TrailerWebViewScreen composing: videoKey=$videoKey, title=$title ===")

    var isLoading by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val focusRequester = remember { FocusRequester() }

    // Auto-hide controls after 3 seconds of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    // Request focus on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Clean up WebView on dispose
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                // Only handle Back - let other keys pass through to YouTube's controls
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    onBack()
                    true
                } else {
                    false
                }
            },
    ) {
        TrailerWebView(
            videoKey = videoKey,
            onWebViewCreated = { webViewRef = it },
            onPageReady = { isLoading = false },
            onPlayStateChanged = { playing -> isPlaying = playing },
            onTimeUpdate = { time, dur ->
                currentTime = time
                duration = dur
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Overlay controls
        if (showControls || !isPlaying) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Top bar with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title ?: "Trailer",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TvIconTextButton(
                        icon = Icons.Outlined.Close,
                        text = "Close",
                        onClick = onBack,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom progress bar and time
                if (duration > 0f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { if (duration > 0f) currentTime / duration else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Color.Red,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = formatTime(currentTime.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = formatTime(duration.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Loading trailer...", color = Color.White)
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

@Suppress("UNUSED_VARIABLE") // hasError and onTimeUpdate reserved for future postMessage API integration
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TrailerWebView(
    videoKey: String,
    onWebViewCreated: (WebView) -> Unit,
    onPageReady: () -> Unit,
    onPlayStateChanged: (Boolean) -> Unit,
    onTimeUpdate: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Error state and time tracking reserved for future YouTube postMessage API integration
    var hasError by remember { mutableStateOf(false) }

    // Use iframe HTML with TV-optimized parameters
    // - iv_load_policy=3: Hide video annotations
    // - fs=0: Disable fullscreen button (we're already fullscreen)
    // - origin: Required for postMessage API
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body {
                    width: 100%;
                    height: 100%;
                    background: #000;
                    overflow: hidden;
                    -webkit-overflow-scrolling: touch;
                }
                iframe {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: none;
                }
            </style>
        </head>
        <body>
            <iframe
                id="player"
                src="https://www.youtube.com/embed/$videoKey?autoplay=1&controls=1&modestbranding=1&rel=0&playsinline=1&enablejsapi=1&iv_load_policy=3&fs=0&origin=https://www.youtube.com"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                // Enable hardware acceleration for video rendering
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                // Core settings for video playback
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.databaseEnabled = true

                // Content settings
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                // Viewport settings for proper scaling
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false

                // Set Chrome user agent for best YouTube compatibility
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; SHIELD Android TV) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                // Custom WebChromeClient for fullscreen video support
                webChromeClient = object : WebChromeClient() {
                    private var customView: View? = null
                    private var customViewCallback: CustomViewCallback? = null

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        Log.d(TAG, "onShowCustomView called")
                        customView = view
                        customViewCallback = callback
                        // The video is already in our fullscreen container
                    }

                    override fun onHideCustomView() {
                        Log.d(TAG, "onHideCustomView called")
                        customViewCallback?.onCustomViewHidden()
                        customView = null
                        customViewCallback = null
                    }

                    override fun getDefaultVideoPoster(): android.graphics.Bitmap? {
                        // Return a black bitmap as placeholder while video loads
                        return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "Page finished loading: $url")
                        onPageReady()
                        onPlayStateChanged(true)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e(TAG, "WebView error: ${error?.errorCode} - ${error?.description}")
                        // Only treat main frame errors as fatal
                        if (request?.isForMainFrame == true) {
                            hasError = true
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        // Keep YouTube navigation within WebView
                        val url = request?.url?.toString() ?: return false
                        return !url.contains("youtube.com")
                    }
                }

                // Enable debugging in debug builds
                WebView.setWebContentsDebuggingEnabled(true)

                // Load HTML with iframe
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )

                onWebViewCreated(this)
            }
        },
        modifier = modifier,
    )
}

