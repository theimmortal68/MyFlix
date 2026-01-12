@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlayerBackend
import dev.jausc.myflix.core.player.PlayerConstants
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.tv.ui.components.AutoPlayCountdown
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    useMpvPlayer: Boolean = false,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val stopReportScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    var latestPositionMs by remember { mutableStateOf(0L) }

    // ViewModel with manual DI
    val viewModel: PlayerViewModel = viewModel(
        key = itemId,
        factory = PlayerViewModel.Factory(itemId, jellyfinClient),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Player controller from core module - pass MPV preference
    val playerController = remember { PlayerController(context, useMpv = useMpvPlayer) }

    // Collect player state
    val playbackState by playerController.state.collectAsState()

    // Initialize player when item is loaded
    LaunchedEffect(state.item, state.streamUrl) {
        val mediaInfo = state.mediaInfo
        if (mediaInfo != null && state.streamUrl != null) {
            // Create MediaInfo for DV-aware player selection
            val coreMediaInfo = MediaInfo(
                title = mediaInfo.title,
                videoCodec = mediaInfo.videoCodec,
                videoProfile = mediaInfo.videoProfile,
                videoRangeType = mediaInfo.videoRangeType,
                width = mediaInfo.width,
                height = mediaInfo.height,
                bitrate = mediaInfo.bitrate,
            )
            viewModel.setPlayerReady(playerController.initializeForMedia(coreMediaInfo))
            focusRequester.requestFocus()
        }
    }

    // Report playback start when playback begins
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying) {
            viewModel.onPlaybackStarted(playbackState.position)
        }
    }

    // Report progress periodically while playing
    LaunchedEffect(playbackState.isPlaying, playbackState.isPaused) {
        if (playbackState.isPlaying && !playbackState.isPaused) {
            while (isActive) {
                delay(PlayerConstants.PROGRESS_REPORT_INTERVAL_MS)
                viewModel.reportProgress(playerController.state.value.position, isPaused = false)
            }
        }
    }

    // Report pause/unpause
    LaunchedEffect(playbackState.isPaused) {
        viewModel.onPauseStateChanged(playbackState.position, playbackState.isPaused)
    }

    // Auto-hide controls
    LaunchedEffect(state.showControls, playbackState.isPlaying) {
        if (state.showControls && playbackState.isPlaying) {
            viewModel.resetControlsHideTimer()
        }
    }

    // Detect video completion (95% watched = mark as played)
    LaunchedEffect(playbackState.position, playbackState.duration) {
        latestPositionMs = playbackState.position
        viewModel.checkVideoCompletion(playbackState.position, playbackState.duration)
    }

    // Detect video end and trigger auto-play countdown
    LaunchedEffect(playbackState.isEnded) {
        if (playbackState.isEnded && !state.showAutoPlayCountdown) {
            viewModel.onVideoEnded()
        }
    }

    // Cleanup - report playback stopped
    DisposableEffect(Unit) {
        onDispose {
            // Fire-and-forget: don't block main thread, let server handle eventual consistency
            stopReportScope.launch {
                viewModel.reportPlaybackStopped(latestPositionMs)
                stopReportScope.cancel()
            }
            playerController.stop()
            playerController.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            if (state.showControls) {
                                playerController.togglePause()
                            } else {
                                viewModel.showControls()
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            playerController.seekRelative(-PlayerConstants.SEEK_STEP_MS)
                            viewModel.showControls()
                            true
                        }
                        Key.DirectionRight -> {
                            playerController.seekRelative(PlayerConstants.SEEK_STEP_MS)
                            viewModel.showControls()
                            true
                        }
                        Key.DirectionUp -> {
                            playerController.seekRelative(PlayerConstants.SEEK_STEP_LONG_MS)
                            viewModel.showControls()
                            true
                        }
                        Key.DirectionDown -> {
                            playerController.seekRelative(-PlayerConstants.SEEK_STEP_LONG_MS)
                            viewModel.showControls()
                            true
                        }
                        Key.Back -> {
                            if (state.showAutoPlayCountdown) {
                                viewModel.cancelQueue()
                            } else {
                                onBack()
                            }
                            true
                        }
                        else -> {
                            viewModel.showControls()
                            false
                        }
                    }
                } else {
                    false
                }
            },
    ) {
        if (state.isLoading || !state.playerReady || state.streamUrl == null) {
            LoadingIndicator("Loading...")
        } else {
            // Render appropriate surface based on backend
            when (playerController.backend) {
                PlayerBackend.MPV -> {
                    MpvSurfaceView(
                        playerController = playerController,
                        url = state.streamUrl!!,
                        startPositionMs = state.startPositionMs,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                PlayerBackend.EXOPLAYER -> {
                    ExoPlayerSurfaceView(
                        playerController = playerController,
                        url = state.streamUrl!!,
                        startPositionMs = state.startPositionMs,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Buffering indicator
            if (playbackState.isBuffering) {
                LoadingIndicator("Buffering...")
            }

            // Error display
            playbackState.error?.let { error ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Error: $error",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Controls overlay
            if (state.showControls && !state.showAutoPlayCountdown) {
                PlayerControlsOverlay(
                    item = state.item,
                    playbackState = playbackState,
                    onPlayPause = { playerController.togglePause() },
                )
            }

            // Auto-play countdown overlay
            if (state.showAutoPlayCountdown && state.nextQueueItem != null) {
                AutoPlayCountdown(
                    nextItem = state.nextQueueItem!!,
                    countdownSeconds = state.countdownSecondsRemaining,
                    jellyfinClient = jellyfinClient,
                    onPlayNow = { viewModel.playNextNow() },
                    onCancel = { viewModel.cancelQueue() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun MpvSurfaceView(
    playerController: PlayerController,
    url: String,
    startPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    var surfaceReady by remember { mutableStateOf(false) }
    val playbackState by playerController.state.collectAsState()

    // Start playback when surface is ready
    LaunchedEffect(surfaceReady, url) {
        if (surfaceReady) {
            playerController.play(url, startPositionMs)
        }
    }

    // Use BoxWithConstraints to get container size and calculate proper aspect ratio
    val density = LocalDensity.current

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
            // Video is wider than container - fit width, letterbox top/bottom
            containerWidth to containerWidth / videoAspect
        } else {
            // Video is taller than container - fit height, pillarbox left/right
            containerHeight * videoAspect to containerHeight
        }

        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            playerController.attachSurface(holder.surface)
                            surfaceReady = true
                        }
                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int,
                        ) {}
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
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
private fun ExoPlayerSurfaceView(
    playerController: PlayerController,
    url: String,
    startPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    // Start playback
    LaunchedEffect(url) {
        playerController.play(url, startPositionMs)
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = playerController.exoPlayer
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view ->
            view.player = playerController.exoPlayer
        },
        modifier = modifier,
    )
}

@Composable
private fun LoadingIndicator(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun PlayerControlsOverlay(
    item: JellyfinItem?,
    playbackState: dev.jausc.myflix.core.player.PlaybackState,
    onPlayPause: () -> Unit,
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val playerType = playbackState.playerType

    // Determine colors based on content type
    val isDV = videoQuality.contains("Dolby Vision")
    val isHDR = videoQuality.contains("HDR")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        // Top bar - title and info badges
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
            }

            // Quality and player badges row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Player type badge
                PlayerBadge(
                    text = playerType,
                    backgroundColor = when (playerType) {
                        "MPV" -> Color(0xFF9C27B0) // Purple for MPV
                        "ExoPlayer" -> Color(0xFF2196F3) // Blue for ExoPlayer
                        else -> Color.Gray
                    },
                )

                // Video quality badges
                if (videoQuality.isNotEmpty()) {
                    // Resolution badge
                    val resolution = when {
                        videoQuality.contains("4K") -> "4K"
                        videoQuality.contains("1080p") -> "1080p"
                        videoQuality.contains("720p") -> "720p"
                        else -> null
                    }
                    resolution?.let {
                        PlayerBadge(
                            text = it,
                            backgroundColor = Color.White.copy(alpha = 0.2f),
                            textColor = Color.White,
                        )
                    }

                    // HDR type badge
                    when {
                        isDV -> PlayerBadge(
                            text = "Dolby Vision",
                            backgroundColor = Color(0xFFE50914), // DV red
                            textColor = Color.White,
                        )
                        isHDR -> PlayerBadge(
                            text = "HDR",
                            backgroundColor = Color(0xFFFFD700), // Gold
                            textColor = Color.Black,
                        )
                    }
                }
            }
        }

        // Center play/pause
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = ClickableSurfaceDefaults.shape(
                    shape = MaterialTheme.shapes.extraLarge,
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                ),
                onClick = onPlayPause,
            ) {
                Text(
                    text = if (playbackState.isPlaying && !playbackState.isPaused) "⏸" else "▶",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }

        // Bottom progress
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        ) {
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.White.copy(alpha = 0.3f), MaterialTheme.shapes.small),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(playbackState.progress)
                        .background(TvColors.BluePrimary, MaterialTheme.shapes.small),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = PlayerUtils.formatTime(playbackState.position),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Text(
                    text = PlayerUtils.formatTime(playbackState.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }

            // Hints
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "◀ -10s  |  ▶ +10s  |  ▲ +1min  |  ▼ -1min  |  OK Play/Pause",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun PlayerBadge(text: String, backgroundColor: Color, textColor: Color = Color.White) {
    Box(
        modifier = Modifier
            .background(backgroundColor, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
        )
    }
}
