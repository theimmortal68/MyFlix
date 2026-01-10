package dev.jausc.myflix.mobile.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isDolbyVision
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.common.model.videoStream
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlaybackState
import dev.jausc.myflix.core.player.PlayerBackend
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
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
    
    // Player controller from core module
    val playerController = remember { PlayerController(context) }
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
            // DV content + DV device → ExoPlayer, everything else → MPV
            playerReady = playerController.initializeForMedia(mediaInfo)
            
            android.util.Log.d("PlayerScreen", 
                "Item: ${loadedItem.name}, DV: ${loadedItem.isDolbyVision}, Backend: ${playerController.backend}")
        }
        isLoading = false
    }
    
    // Report playback start when playback begins
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying && !playbackStarted) {
            playbackStarted = true
            val positionTicks = playbackState.position * 10_000 // ms to ticks
            jellyfinClient.reportPlaybackStart(itemId, positionTicks = positionTicks)
            android.util.Log.d("PlayerScreen", "Reported playback start at ${playbackState.position}ms")
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
                    android.util.Log.d("PlayerScreen", "Reported progress at ${playbackState.position}ms")
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
            delay(4000)
            showControls = false
        }
    }
    
    // Cleanup - report playback stopped
    DisposableEffect(Unit) {
        onDispose {
            // Report stopped with final position
            scope.launch {
                val positionTicks = playerController.state.value.position * 10_000
                jellyfinClient.reportPlaybackStopped(itemId, positionTicks)
                android.util.Log.d("PlayerScreen", "Reported playback stopped at ${playerController.state.value.position}ms")
            }
            playerController.stop()
            playerController.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        when {
            isLoading || !playerReady || streamUrl == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
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
                    MobilePlayerControls(
                        item = item,
                        playbackState = playbackState,
                        playerController = playerController,
                        onBack = onBack
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
private fun MobilePlayerControls(
    item: JellyfinItem?,
    playbackState: PlaybackState,
    playerController: PlayerController,
    onBack: () -> Unit
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val isDV = videoQuality.contains("Dolby Vision")
    val isHDR = videoQuality.contains("HDR")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                // Quality and player badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Player type badge
                    PlayerBadge(
                        text = playbackState.playerType,
                        backgroundColor = when (playbackState.playerType) {
                            "MPV" -> Color(0xFF9C27B0) // Purple
                            "ExoPlayer" -> Color(0xFF2196F3) // Blue
                            else -> Color.Gray
                        }
                    )
                    
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
                            backgroundColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                    
                    // HDR type badge
                    when {
                        isDV -> PlayerBadge(
                            text = "Dolby Vision",
                            backgroundColor = Color(0xFFE50914), // DV red
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

        // Center controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rewind 10s
            IconButton(
                onClick = { playerController.seekRelative(-10_000) },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.FastRewind,
                    contentDescription = "Rewind 10s",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // Play/Pause
            IconButton(
                onClick = { playerController.togglePause() },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    if (playbackState.isPlaying && !playbackState.isPaused) 
                        Icons.Default.Pause 
                    else 
                        Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // Forward 10s
            IconButton(
                onClick = { playerController.seekRelative(10_000) },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.FastForward,
                    contentDescription = "Forward 10s",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Bottom progress
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Slider(
                value = playbackState.progress,
                onValueChange = { 
                    playerController.seekTo((it * playbackState.duration).toLong()) 
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = PlayerUtils.formatTime(playbackState.position),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = PlayerUtils.formatTime(playbackState.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlayerBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color = Color.White
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
