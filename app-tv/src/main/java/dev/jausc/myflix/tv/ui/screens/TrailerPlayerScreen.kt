package dev.jausc.myflix.tv.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.jausc.myflix.core.common.youtube.TrailerStreamService
import dev.jausc.myflix.core.player.MpvPlayer
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.tv.ui.components.TvIconTextButton
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

private const val TAG = "TrailerPlayer"

@Composable
fun TrailerPlayerScreen(
    videoKey: String,
    title: String?,
    onBack: () -> Unit,
    useMpvPlayer: Boolean = false,
) {
    val context = LocalContext.current
    val screenFocusRequester = remember { FocusRequester() }

    Log.d(TAG, "=== TrailerPlayerScreen composing === videoKey=$videoKey, title=$title, useMpv=$useMpvPlayer")

    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }

    // Create player controller based on app preference
    val playerController = remember {
        Log.d(TAG, "Creating PlayerController with useMpv=$useMpvPlayer")
        PlayerController(context, useMpv = useMpvPlayer)
    }

    // Request focus for key events
    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }
    val playbackState by playerController.state.collectAsState()

    // Log playback state changes
    LaunchedEffect(playbackState) {
        Log.d(TAG, "PlaybackState changed: isPlaying=${playbackState.isPlaying}, " +
            "isPaused=${playbackState.isPaused}, isBuffering=${playbackState.isBuffering}, " +
            "position=${playbackState.position}, duration=${playbackState.duration}, " +
            "error=${playbackState.error}")
    }

    // Initialize player
    DisposableEffect(Unit) {
        Log.d(TAG, "Initializing PlayerController")
        val success = playerController.initialize()
        Log.d(TAG, "PlayerController initialized: success=$success, exoPlayer=${playerController.exoPlayer}")
        onDispose {
            Log.d(TAG, "Releasing PlayerController")
            playerController.release()
        }
    }

    // Fetch stream URL from Jellyfin ExtrasDownloader plugin
    LaunchedEffect(videoKey) {
        Log.d(TAG, "Fetching stream URL for videoKey=$videoKey via ExtrasDownloader")
        isLoading = true
        errorMessage = null
        streamUrl = null

        try {
            val url = TrailerStreamService.getStreamUrl(videoKey)
            if (url != null) {
                Log.d(TAG, "Stream URL fetched successfully: ${url.take(80)}...")
                streamUrl = url
            } else {
                Log.e(TAG, "Failed to get stream URL - null returned")
                errorMessage = "Failed to load trailer. Server may be unreachable or extraction failed."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stream URL", e)
            errorMessage = e.message ?: "Failed to load trailer"
        }

        isLoading = false
        Log.d(TAG, "Fetch complete: isLoading=false, hasUrl=${streamUrl != null}, error=$errorMessage")
    }

    // Auto-hide controls after 4 seconds when playing
    LaunchedEffect(showControls, playbackState.isPlaying) {
        if (showControls && playbackState.isPlaying) {
            delay(4000)
            showControls = false
            // Return focus to screen for key handling
            screenFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val controlsWereHidden = !showControls

                    // Always show controls on any key press
                    showControls = true

                    when (event.key) {
                        Key.Back -> {
                            onBack()
                            true
                        }
                        // When controls were hidden, handle media keys directly
                        Key.DirectionLeft -> {
                            if (controlsWereHidden) {
                                playerController.seekRelative(-10_000)
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (controlsWereHidden) {
                                playerController.seekRelative(10_000)
                                true
                            } else false
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            if (controlsWereHidden) {
                                playerController.togglePause()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        Log.d(TAG, "Rendering: isLoading=$isLoading, error=$errorMessage, hasUrl=${streamUrl != null}")

        when {
            isLoading -> {
                Log.d(TAG, "Showing loading overlay")
                LoadingOverlay(text = "Loading trailer...")
            }
            errorMessage != null -> {
                Log.d(TAG, "Showing error overlay: $errorMessage")
                ErrorOverlay(
                    message = errorMessage!!,
                    onBack = onBack,
                    onOpenYouTube = { openYouTubeTrailer(context, videoKey) },
                )
            }
            streamUrl != null -> {
                Log.d(TAG, "Creating player surface for URL: ${streamUrl!!.take(80)}...")
                TrailerSurface(
                    playerController = playerController,
                    url = streamUrl!!,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (showControls && streamUrl != null && errorMessage == null) {
            TrailerControlsOverlay(
                title = title ?: "Trailer",
                isPlaying = playbackState.isPlaying,
                position = playbackState.position,
                duration = playbackState.duration,
                quality = null, // Quality determined by server-side yt-dlp format selection
                onPlayPause = { playerController.togglePause() },
                onRewind = { playerController.seekRelative(-10_000) },
                onForward = { playerController.seekRelative(10_000) },
                onClose = onBack,
            )
        }
    }
}

@Composable
private fun TrailerSurface(
    playerController: PlayerController,
    url: String,
    modifier: Modifier = Modifier,
) {
    Log.d(TAG, "TrailerSurface composing with URL: ${url.take(80)}...")

    var surfaceReady by remember { mutableStateOf(false) }
    val playbackState by playerController.state.collectAsState()

    // Start playback when surface is ready
    LaunchedEffect(surfaceReady, url) {
        if (surfaceReady) {
            Log.d(TAG, "Surface ready, starting playback of URL")
            playerController.play(url, startPositionMs = 0)
            Log.d(TAG, "play() called")
        }
    }

    // Calculate aspect ratio for proper sizing
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val videoAspect = if (playbackState.videoAspectRatio > 0) {
            playbackState.videoAspectRatio
        } else if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
            playbackState.videoWidth.toFloat() / playbackState.videoHeight.toFloat()
        } else {
            16f / 9f // Default to 16:9
        }

        // Calculate size maintaining aspect ratio (letterbox/pillarbox)
        val containerAspect = containerWidth / containerHeight
        val (surfaceWidth, surfaceHeight) = if (videoAspect > containerAspect) {
            containerWidth to containerWidth / videoAspect
        } else {
            containerHeight * videoAspect to containerHeight
        }

        val density = LocalDensity.current

        AndroidView(
            factory = { ctx ->
                Log.d(TAG, "Creating SurfaceView for player")
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            Log.d(TAG, "Surface created, attaching to player")
                            playerController.attachSurface(holder.surface)
                            surfaceReady = true
                        }
                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int,
                        ) {
                            Log.d(TAG, "Surface changed: ${width}x${height}")
                        }
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            Log.d(TAG, "Surface destroyed, detaching from player")
                            surfaceReady = false
                            playerController.detachSurface()
                        }
                    })
                }
            },
            modifier = with(density) {
                Modifier.size(
                    width = surfaceWidth.toDp(),
                    height = surfaceHeight.toDp(),
                )
            },
        )
    }
}

@Composable
private fun TrailerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    quality: String?,
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
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (quality != null) {
                    Text(
                        text = quality,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

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
private fun ErrorOverlay(message: String, onBack: () -> Unit, onOpenYouTube: () -> Unit) {
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
