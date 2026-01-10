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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isDolbyVision
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.common.model.videoStream
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlayerBackend
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    useMpvPlayer: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var item by remember { mutableStateOf<JellyfinItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var startPosition by remember { mutableLongStateOf(0L) }
    var playbackStarted by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // Player controller from core module - pass MPV preference
    val playerController = remember { PlayerController(context, useMpv = useMpvPlayer) }
    var playerReady by remember { mutableStateOf(false) }
    
    // Collect player state
    val playbackState by playerController.state.collectAsState()
    
    // Load item info first, then initialize player with DV-aware selection
    LaunchedEffect(itemId) {
        jellyfinClient.getItem(itemId).onSuccess { loadedItem ->
            item = loadedItem
            streamUrl = jellyfinClient.getStreamUrl(itemId)
            startPosition = loadedItem.userData?.playbackPositionTicks?.let { 
                it / 10_000  // Convert ticks to milliseconds
            } ?: 0L
            
            // Create MediaInfo for DV-aware player selection
            val videoStream = loadedItem.videoStream
            val mediaInfo = MediaInfo(
                title = loadedItem.name,
                videoCodec = videoStream?.codec,
                videoProfile = videoStream?.profile,
                videoRangeType = videoStream?.videoRangeType,
                width = videoStream?.width ?: 0,
                height = videoStream?.height ?: 0,
                bitrate = videoStream?.bitRate ?: 0
            )
            
            // Initialize player with content-aware backend selection
            // DV content → ExoPlayer, everything else → MPV
            playerReady = playerController.initializeForMedia(mediaInfo)
        }
        isLoading = false
        focusRequester.requestFocus()
    }
    
    // Report playback start when playback begins
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying && !playbackStarted) {
            playbackStarted = true
            val positionTicks = playbackState.position * 10_000 // ms to ticks
            jellyfinClient.reportPlaybackStart(itemId, positionTicks = positionTicks)
        }
    }
    
    // Report progress periodically (every 10 seconds while playing)
    LaunchedEffect(playbackStarted) {
        if (playbackStarted) {
            while (isActive) {
                delay(10_000) // Report every 10 seconds
                if (playbackState.isPlaying && !playbackState.isPaused) {
                    val positionTicks = playbackState.position * 10_000
                    jellyfinClient.reportPlaybackProgress(
                        itemId = itemId,
                        positionTicks = positionTicks,
                        isPaused = false
                    )
                }
            }
        }
    }
    
    // Report pause/unpause
    LaunchedEffect(playbackState.isPaused) {
        if (playbackStarted) {
            val positionTicks = playbackState.position * 10_000
            jellyfinClient.reportPlaybackProgress(
                itemId = itemId,
                positionTicks = positionTicks,
                isPaused = playbackState.isPaused
            )
        }
    }
    
    // Auto-hide controls
    LaunchedEffect(showControls, playbackState.isPlaying) {
        if (showControls && playbackState.isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    // Detect video completion (95% watched = mark as played)
    LaunchedEffect(playbackState.position, playbackState.duration) {
        if (playbackState.duration > 0 && playbackStarted) {
            val watchedPercent = playbackState.position.toFloat() / playbackState.duration.toFloat()
            if (watchedPercent >= 0.95f) {
                // Mark as played when 95% watched
                jellyfinClient.setPlayed(itemId, true)
            }
        }
    }

    // Cleanup - report playback stopped (use NonCancellable to ensure it completes)
    DisposableEffect(Unit) {
        onDispose {
            // Report stopped with final position - must complete even if scope is cancelled
            scope.launch {
                withContext(NonCancellable) {
                    val positionTicks = playerController.state.value.position * 10_000
                    jellyfinClient.reportPlaybackStopped(itemId, positionTicks)
                }
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
                            if (showControls) {
                                playerController.togglePause()
                            } else {
                                showControls = true
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            playerController.seekRelative(-10_000)
                            showControls = true
                            true
                        }
                        Key.DirectionRight -> {
                            playerController.seekRelative(10_000)
                            showControls = true
                            true
                        }
                        Key.DirectionUp -> {
                            playerController.seekRelative(60_000)
                            showControls = true
                            true
                        }
                        Key.DirectionDown -> {
                            playerController.seekRelative(-60_000)
                            showControls = true
                            true
                        }
                        Key.Back -> {
                            onBack()
                            true
                        }
                        else -> {
                            showControls = true
                            false
                        }
                    }
                } else false
            }
    ) {
        when {
            isLoading || !playerReady || streamUrl == null -> {
                LoadingIndicator("Loading...")
            }
            else -> {
                // Render appropriate surface based on backend
                when (playerController.backend) {
                    PlayerBackend.MPV -> {
                        MpvSurfaceView(
                            playerController = playerController,
                            url = streamUrl!!,
                            startPositionMs = startPosition,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    PlayerBackend.EXOPLAYER -> {
                        ExoPlayerSurfaceView(
                            playerController = playerController,
                            url = streamUrl!!,
                            startPositionMs = startPosition,
                            modifier = Modifier.fillMaxSize()
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
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: $error",
                            color = Color.Red,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                // Controls overlay
                if (showControls) {
                    PlayerControlsOverlay(
                        item = item,
                        playbackState = playbackState,
                        onPlayPause = { playerController.togglePause() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MpvSurfaceView(
    playerController: PlayerController,
    url: String,
    startPositionMs: Long,
    modifier: Modifier = Modifier
) {
    var surfaceReady by remember { mutableStateOf(false) }
    
    // Start playback when surface is ready
    LaunchedEffect(surfaceReady, url) {
        if (surfaceReady) {
            playerController.play(url, startPositionMs)
        }
    }
    
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        playerController.attachSurface(holder.surface)
                        surfaceReady = true
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        surfaceReady = false
                        playerController.detachSurface()
                    }
                })
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ExoPlayerSurfaceView(
    playerController: PlayerController,
    url: String,
    startPositionMs: Long,
    modifier: Modifier = Modifier
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
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            view.player = playerController.exoPlayer
        },
        modifier = modifier
    )
}

@Composable
private fun LoadingIndicator(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PlayerControlsOverlay(
    item: JellyfinItem?,
    playbackState: dev.jausc.myflix.core.player.PlaybackState,
    onPlayPause: () -> Unit
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val playerType = playbackState.playerType
    
    // Determine colors based on content type
    val isDV = videoQuality.contains("Dolby Vision")
    val isHDR = videoQuality.contains("HDR")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        // Top bar - title and info badges
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Quality and player badges row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player type badge
                PlayerBadge(
                    text = playerType,
                    backgroundColor = when (playerType) {
                        "MPV" -> Color(0xFF9C27B0) // Purple for MPV
                        "ExoPlayer" -> Color(0xFF2196F3) // Blue for ExoPlayer
                        else -> Color.Gray
                    }
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
                            textColor = Color.White
                        )
                    }
                    
                    // HDR type badge
                    when {
                        isDV -> PlayerBadge(
                            text = "Dolby Vision",
                            backgroundColor = Color(0xFFE50914), // DV red
                            textColor = Color.White
                        )
                        isHDR -> PlayerBadge(
                            text = "HDR",
                            backgroundColor = Color(0xFFFFD700), // Gold
                            textColor = Color.Black
                        )
                    }
                }
            }
        }

        // Center play/pause
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = ClickableSurfaceDefaults.shape(
                    shape = MaterialTheme.shapes.extraLarge
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                onClick = onPlayPause
            ) {
                Text(
                    text = if (playbackState.isPlaying && !playbackState.isPaused) "⏸" else "▶",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        // Bottom progress
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.White.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(playbackState.progress)
                        .background(TvColors.BluePrimary, MaterialTheme.shapes.small)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = PlayerUtils.formatTime(playbackState.position),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = PlayerUtils.formatTime(playbackState.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            
            // Hints
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "◀ -10s  |  ▶ +10s  |  ▲ +1min  |  ▼ -1min  |  OK Play/Pause",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PlayerBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
