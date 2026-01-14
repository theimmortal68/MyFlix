package dev.jausc.myflix.tv.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.jausc.myflix.tv.ui.components.TvIconTextButton

@Composable
fun TrailerWebViewScreen(
    videoKey: String,
    title: String?,
    onBack: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                    onBack()
                    true
                } else {
                    false
                }
            },
    ) {
        TrailerWebView(
            videoKey = videoKey,
            onPageReady = { isLoading = false },
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TrailerWebView(
    videoKey: String,
    onPageReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val html = remember(videoKey) { buildEmbedHtml(videoKey) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageReady()
                    }
                }
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        modifier = modifier,
    )
}

private fun buildEmbedHtml(videoKey: String): String {
    return """
        <!DOCTYPE html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <style>
              html, body { margin: 0; padding: 0; background: black; height: 100%; }
              iframe { width: 100%; height: 100%; border: 0; }
            </style>
          </head>
          <body>
            <iframe
              src="https://www.youtube.com/embed/$videoKey?autoplay=1&playsinline=1&controls=1&modestbranding=1"
              allow="autoplay; encrypted-media"
              allowfullscreen>
            </iframe>
          </body>
        </html>
    """.trimIndent()
}
