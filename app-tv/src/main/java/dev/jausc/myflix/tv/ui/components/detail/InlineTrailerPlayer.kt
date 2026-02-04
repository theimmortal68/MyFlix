package dev.jausc.myflix.tv.ui.components.detail

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * State holder for inline trailer playback.
 */
data class InlineTrailerState(
    val streamUrl: String? = null,
    val shouldPlay: Boolean = false,
    val isMuted: Boolean = true,
)

/**
 * Inline trailer player for movie/series detail screens.
 *
 * Uses ExoPlayer with proper lifecycle management to play trailers
 * as a background layer behind the detail screen content.
 *
 * @param state The trailer state containing URL, play state, and mute state
 * @param alpha The alpha value for crossfade transition (0f = invisible, 1f = fully visible)
 * @param modifier Modifier for positioning
 * @param onPlaybackStarted Callback when video starts rendering
 * @param onPlaybackEnded Callback when video ends
 * @param onError Callback when playback fails
 */
@OptIn(UnstableApi::class)
@Composable
fun InlineTrailerPlayer(
    state: InlineTrailerState,
    alpha: Float,
    modifier: Modifier = Modifier,
    onPlaybackStarted: () -> Unit = {},
    onPlaybackEnded: () -> Unit = {},
    onError: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Create and remember ExoPlayer instance
    val player = remember {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    // Track if we've started rendering video
    var hasStartedRendering by remember { mutableStateOf(false) }

    // Player listener for events
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasStartedRendering = true
                onPlaybackStarted()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> onPlaybackEnded()
                    Player.STATE_IDLE -> {
                        if (player.playerError != null) {
                            onError()
                        }
                    }
                    else -> {}
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Lifecycle management - pause on stop, release on destroy
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                }
                Lifecycle.Event.ON_STOP -> {
                    player.pause()
                    player.clearMediaItems()
                    hasStartedRendering = false
                }
                Lifecycle.Event.ON_DESTROY -> {
                    player.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Release player when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    // Update media source when URL changes
    LaunchedEffect(state.streamUrl) {
        if (state.streamUrl != null) {
            player.setMediaItem(MediaItem.fromUri(state.streamUrl))
            player.prepare()
        } else {
            player.clearMediaItems()
            hasStartedRendering = false
        }
    }

    // Control playback state
    LaunchedEffect(state.shouldPlay) {
        player.playWhenReady = state.shouldPlay
    }

    // Control mute state
    LaunchedEffect(state.isMuted) {
        player.volume = if (state.isMuted) 0f else 1f
    }

    // Only render if we have a URL and alpha > 0
    if (state.streamUrl != null && alpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(Color.Black),
        ) {
            // Use SurfaceView for video rendering (more efficient for video)
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).also { surfaceView ->
                        player.setVideoSurfaceView(surfaceView)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Remember and create an InlineTrailerState.
 */
@Composable
fun rememberInlineTrailerState(
    streamUrl: String? = null,
    shouldPlay: Boolean = false,
    isMuted: Boolean = true,
): InlineTrailerState {
    return remember(streamUrl, shouldPlay, isMuted) {
        InlineTrailerState(streamUrl, shouldPlay, isMuted)
    }
}
