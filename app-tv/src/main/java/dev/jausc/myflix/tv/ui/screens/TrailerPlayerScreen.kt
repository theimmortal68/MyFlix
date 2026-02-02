package dev.jausc.myflix.tv.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import dev.jausc.myflix.core.common.youtube.YouTubeStream
import dev.jausc.myflix.core.common.youtube.YouTubeTrailerResolver
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.tv.ui.components.TvIconTextButton
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

private const val TAG = "TrailerPlayerScreen"

@Composable
fun TrailerPlayerScreen(
    videoKey: String,
    title: String?,
    onBack: () -> Unit,
    useMpvPlayer: Boolean = false,
) {
    val context = LocalContext.current
    val playerController = remember(useMpvPlayer) { PlayerController(context, useMpv = useMpvPlayer) }
    val playbackState by playerController.state.collectAsState()

    var resolvedStream by remember { mutableStateOf<YouTubeStream?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }

    android.util.Log.d(TAG, "TrailerPlayerScreen composing: videoKey=$videoKey, title=$title")

    DisposableEffect(Unit) {
        android.util.Log.d(TAG, "Initializing PlayerController...")
        val initResult = playerController.initialize()
        android.util.Log.d(TAG, "PlayerController initialized: $initResult")
        onDispose {
            android.util.Log.d(TAG, "Releasing PlayerController")
            playerController.release()
        }
    }

    LaunchedEffect(videoKey) {
        android.util.Log.d(TAG, "LaunchedEffect: Starting trailer resolution for $videoKey")
        isLoading = true
        errorMessage = null
        resolvedStream = null
        val result = YouTubeTrailerResolver.resolveTrailer(context, videoKey)
        android.util.Log.d(TAG, "Resolution result: isSuccess=${result.isSuccess}, isFailure=${result.isFailure}")
        result
            .onSuccess {
                android.util.Log.d(TAG, "SUCCESS: url=${it.url.take(100)}..., title=${it.title}, duration=${it.durationMs}ms, isHls=${it.isHls}")
                resolvedStream = it
            }
            .onFailure {
                android.util.Log.e(TAG, "FAILURE: ${it.message}", it)
                errorMessage = it.message ?: "Failed to load trailer"
            }
        isLoading = false
        android.util.Log.d(TAG, "Resolution complete: isLoading=$isLoading, hasStream=${resolvedStream != null}, error=$errorMessage")
    }

    LaunchedEffect(showControls, playbackState.isPlaying) {
        if (showControls && playbackState.isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    showControls = true
                    when (event.key) {
                        Key.DirectionLeft -> {
                            playerController.seekRelative(-10_000)
                            true
                        }
                        Key.DirectionRight -> {
                            playerController.seekRelative(10_000)
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            playerController.togglePause()
                            true
                        }
                        Key.Back -> {
                            onBack()
                            true
                        }
                        else -> { false }
                    }
                } else {
                    false
                }
            },
    ) {
        when {
            isLoading -> {
                LoadingOverlay(text = "Loading trailer...")
            }
            errorMessage != null -> {
                ErrorOverlay(
                    message = errorMessage!!,
                    onBack = onBack,
                    onOpenYouTube = { openYouTubeTrailer(context, videoKey) },
                )
            }
            resolvedStream != null -> {
                TrailerVideoSurface(
                    playerController = playerController,
                    url = resolvedStream!!.url,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (showControls && resolvedStream != null && errorMessage == null) {
            TrailerControlsOverlay(
                title = title ?: resolvedStream?.title ?: "Trailer",
                isPlaying = playbackState.isPlaying,
                position = playbackState.position,
                duration = playbackState.duration,
                onPlayPause = { playerController.togglePause() },
                onRewind = { playerController.seekRelative(-10_000) },
                onForward = { playerController.seekRelative(10_000) },
                onClose = onBack,
            )
        }
    }
}

@Composable
private fun TrailerVideoSurface(playerController: PlayerController, url: String, modifier: Modifier = Modifier,) {
    android.util.Log.d(TAG, "TrailerVideoSurface: url=${url.take(100)}...")

    LaunchedEffect(url) {
        android.util.Log.d(TAG, "LaunchedEffect: Calling playerController.play()")
        playerController.play(url, startPositionMs = 0)
        android.util.Log.d(TAG, "playerController.play() called, exoPlayer=${playerController.exoPlayer}")
    }

    AndroidView(
        factory = { ctx ->
            android.util.Log.d(TAG, "AndroidView factory: Creating PlayerView")
            PlayerView(ctx).apply {
                player = playerController.exoPlayer
                useController = false
                android.util.Log.d(TAG, "PlayerView created, player attached: ${player != null}")
            }
        },
        update = { view ->
            android.util.Log.d(TAG, "AndroidView update: player=${playerController.exoPlayer}")
            view.player = playerController.exoPlayer
        },
        modifier = modifier,
    )
}

@Composable
private fun TrailerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onClose: () -> Unit,
) {
    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        playFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TvIconTextButton(
                        icon = Icons.Outlined.FastRewind,
                        text = "Rewind",
                        onClick = onRewind,
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    TvIconTextButton(
                        icon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        text = if (isPlaying) "Pause" else "Play",
                        onClick = onPlayPause,
                        modifier = Modifier.focusRequester(playFocusRequester),
                        containerColor = TvColors.BluePrimary,
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    TvIconTextButton(
                        icon = Icons.Outlined.FastForward,
                        text = "Forward",
                        onClick = onForward,
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    TvIconTextButton(
                        icon = Icons.Outlined.Close,
                        text = "Close",
                        onClick = onClose,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = PlayerUtils.formatTime(position),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = PlayerUtils.formatTime(duration.coerceAtLeast(0)),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = Color.White)
    }
}

@Composable
private fun ErrorOverlay(message: String, onBack: () -> Unit, onOpenYouTube: () -> Unit,) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvIconTextButton(
                    icon = Icons.Outlined.PlayArrow,
                    text = "Open in YouTube",
                    onClick = onOpenYouTube,
                )
                TvIconTextButton(
                    icon = Icons.Outlined.Close,
                    text = "Back",
                    onClick = onBack,
                )
            }
        }
    }
}

private fun openYouTubeTrailer(context: android.content.Context, videoKey: String) {
    val appUri = Uri.parse("vnd.youtube:$videoKey")
    val webUri = Uri.parse("https://www.youtube.com/watch?v=$videoKey")
    val intent = Intent(Intent.ACTION_VIEW, appUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
