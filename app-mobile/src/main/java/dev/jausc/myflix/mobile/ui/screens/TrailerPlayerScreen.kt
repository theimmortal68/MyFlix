package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.compose.ui.platform.LocalContext
import dev.jausc.myflix.core.common.youtube.YouTubeTrailerResolver
import dev.jausc.myflix.core.common.youtube.YouTubeStream
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import kotlinx.coroutines.delay

@Composable
fun TrailerPlayerScreen(
    videoKey: String,
    title: String?,
    onBack: () -> Unit,
) {
    val playerController = remember { PlayerController(LocalContext.current, useMpv = false) }
    val playbackState by playerController.state.collectAsState()

    var resolvedStream by remember { mutableStateOf<YouTubeStream?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        playerController.initialize()
        onDispose { playerController.release() }
    }

    LaunchedEffect(videoKey) {
        isLoading = true
        errorMessage = null
        resolvedStream = null
        YouTubeTrailerResolver.resolveTrailer(videoKey)
            .onSuccess { resolvedStream = it }
            .onFailure { errorMessage = it.message ?: "Failed to load trailer" }
        isLoading = false
    }

    LaunchedEffect(showControls, playbackState.isPlaying) {
        if (showControls && playbackState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls },
    ) {
        when {
            isLoading -> LoadingOverlay(text = "Loading trailer...")
            errorMessage != null -> ErrorOverlay(message = errorMessage!!, onBack = onBack)
            resolvedStream != null -> {
                TrailerVideoSurface(
                    playerController = playerController,
                    url = resolvedStream!!.url,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (showControls && resolvedStream != null && errorMessage == null) {
            val duration = playbackState.duration.coerceAtLeast(0)
            val currentPosition = playbackState.position.coerceAtLeast(0)
            val sliderValue = if (isScrubbing) scrubPosition.toFloat() else currentPosition.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = title ?: resolvedStream?.title ?: "Trailer",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Slider(
                        value = if (duration > 0) (sliderValue / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            if (duration > 0) {
                                isScrubbing = true
                                scrubPosition = (duration * fraction).toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (isScrubbing) {
                                playerController.seekTo(scrubPosition)
                            }
                            isScrubbing = false
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = PlayerUtils.formatTime(sliderValue.toLong()),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = PlayerUtils.formatTime(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { playerController.seekRelative(-10_000) }) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        IconButton(onClick = { playerController.togglePause() }) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        IconButton(onClick = { playerController.seekRelative(10_000) }) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailerVideoSurface(
    playerController: PlayerController,
    url: String,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(url) {
        playerController.play(url, startPositionMs = 0)
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = playerController.exoPlayer
                useController = false
            }
        },
        update = { view -> view.player = playerController.exoPlayer },
        modifier = modifier,
    )
}

@Composable
private fun LoadingOverlay(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = Color.White)
    }
}

@Composable
private fun ErrorOverlay(message: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Go back",
                color = Color.White,
                modifier = Modifier.clickable { onBack() },
            )
        }
    }
}
