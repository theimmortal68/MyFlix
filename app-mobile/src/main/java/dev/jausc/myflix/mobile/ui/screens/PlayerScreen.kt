@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.mobile.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jausc.myflix.core.viewmodel.PlayerMediaInfo
import dev.jausc.myflix.core.viewmodel.PlayerUiState
import dev.jausc.myflix.core.viewmodel.PlayerViewModel
import androidx.media3.ui.PlayerView
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.audioLabel
import dev.jausc.myflix.core.common.model.subtitleLabel
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlaybackState
import dev.jausc.myflix.core.player.PlayerBackend
import dev.jausc.myflix.core.player.PlayerConstants
import dev.jausc.myflix.core.player.PlayerDisplayMode
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.core.player.SubtitleStyle
import dev.jausc.myflix.core.player.SubtitleFontSize
import dev.jausc.myflix.core.player.SubtitleColor
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.mobile.ui.components.AutoPlayCountdown
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerScreen(
    itemId: String,
    startPositionMs: Long? = null,
    jellyfinClient: JellyfinClient,
    appPreferences: AppPreferences,
    useMpvPlayer: Boolean = false,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // Get preferred audio and subtitle language from preferences
    val preferredAudioLanguage by appPreferences.preferredAudioLanguage.collectAsState()
    val preferredSubtitleLanguage by appPreferences.preferredSubtitleLanguage.collectAsState()

    // Get display mode preference
    val displayModeName by appPreferences.playerDisplayMode.collectAsState()

    // Get subtitle styling preferences
    val subtitleFontSizeName by appPreferences.subtitleFontSize.collectAsState()
    val subtitleFontColorName by appPreferences.subtitleFontColor.collectAsState()
    val subtitleBackgroundOpacity by appPreferences.subtitleBackgroundOpacity.collectAsState()
    val subtitleStyle = remember(subtitleFontSizeName, subtitleFontColorName, subtitleBackgroundOpacity) {
        SubtitleStyle(
            fontSize = SubtitleFontSize.fromName(subtitleFontSizeName),
            fontColor = SubtitleColor.fromName(subtitleFontColorName),
            backgroundOpacity = subtitleBackgroundOpacity,
        )
    }

    // ViewModel with manual DI
    val viewModel: PlayerViewModel = viewModel(
        key = itemId,
        factory = PlayerViewModel.Factory(
            itemId = itemId,
            jellyfinClient = jellyfinClient,
            preferredAudioLanguage = preferredAudioLanguage,
            preferredSubtitleLanguage = preferredSubtitleLanguage,
            startPositionMs = startPositionMs,
        ),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Player controller from core module - pass MPV preference
    val playerController = remember { PlayerController(context, useMpv = useMpvPlayer) }

    // Collect player state
    val playbackState by playerController.state.collectAsState()
    var displayMode by remember(displayModeName) {
        mutableStateOf(
            try { PlayerDisplayMode.valueOf(displayModeName) } catch (e: IllegalArgumentException) { PlayerDisplayMode.FIT }
        )
    }

    val selectedAudioIndex = state.selectedAudioStreamIndex
    val selectedSubtitleIndex = state.selectedSubtitleStreamIndex
    val effectiveStreamUrl = remember(state.streamUrl, state.item?.id, selectedAudioIndex, selectedSubtitleIndex) {
        val item = state.item
        if (state.streamUrl == null || item == null) {
            null
        } else {
            jellyfinClient.getStreamUrl(item.id, selectedAudioIndex, selectedSubtitleIndex)
        }
    }

    var currentStartPositionMs by remember(state.item?.id) { mutableLongStateOf(state.startPositionMs) }

    LaunchedEffect(state.item?.id, state.startPositionMs) {
        currentStartPositionMs = state.startPositionMs
    }

    // Initialize player when item is loaded
    LaunchedEffect(state.item, effectiveStreamUrl) {
        val mediaInfo = state.mediaInfo
        if (mediaInfo != null && effectiveStreamUrl != null) {
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
        viewModel.checkVideoCompletion(playbackState.position, playbackState.duration)
    }

    // Detect video ended for queue auto-play
    LaunchedEffect(playbackState.isEnded) {
        if (playbackState.isEnded) {
            viewModel.onVideoEnded()
        }
    }

    // Update active segment based on playback position
    LaunchedEffect(playbackState.isPlaying, state.mediaSegments) {
        if (playbackState.isPlaying && state.mediaSegments.isNotEmpty()) {
            while (isActive) {
                viewModel.updateActiveSegment(playerController.state.value.position)
                delay(500) // Check every 500ms
            }
        }
    }

    // Cleanup player resources (ViewModel handles playback stop reporting)
    DisposableEffect(Unit) {
        onDispose {
            playerController.stop()
            playerController.release()
        }
    }

    // Apply subtitle style when it changes or player becomes ready
    LaunchedEffect(subtitleStyle, playerController.isInitialized) {
        if (playerController.isInitialized) {
            playerController.setSubtitleStyle(subtitleStyle)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.toggleControls() },
    ) {
        if (state.isLoading || !state.playerReady || effectiveStreamUrl == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            // Render appropriate surface based on backend
            when (playerController.backend) {
                PlayerBackend.MPV -> {
                    MpvSurfaceView(
                        playerController = playerController,
                        url = effectiveStreamUrl,
                        startPositionMs = currentStartPositionMs,
                        displayMode = displayMode,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                PlayerBackend.EXOPLAYER -> {
                    ExoPlayerSurfaceView(
                        playerController = playerController,
                        url = effectiveStreamUrl,
                        startPositionMs = currentStartPositionMs,
                        displayMode = displayMode,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Buffering indicator
            if (playbackState.isBuffering) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
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
            if (state.showControls) {
                MobilePlayerControls(
                    item = state.item,
                    playbackState = playbackState,
                    playerController = playerController,
                    audioStreams = state.audioStreams,
                    subtitleStreams = state.subtitleStreams,
                    selectedAudioIndex = selectedAudioIndex,
                    selectedSubtitleIndex = selectedSubtitleIndex,
                    onAudioSelected = { index ->
                        currentStartPositionMs = playbackState.position
                        viewModel.setAudioStreamIndex(index)
                        // Save the selected audio language as preference
                        val selectedStream = state.audioStreams.find { it.index == index }
                        selectedStream?.language?.let { language ->
                            appPreferences.setPreferredAudioLanguage(language)
                        }
                    },
                    onSubtitleSelected = { index ->
                        currentStartPositionMs = playbackState.position
                        viewModel.setSubtitleStreamIndex(index)
                    },
                    subtitleStyle = subtitleStyle,
                    onSubtitleStyleChanged = { newStyle ->
                        appPreferences.setSubtitleFontSize(newStyle.fontSize.name)
                        appPreferences.setSubtitleFontColor(newStyle.fontColor.name)
                        appPreferences.setSubtitleBackgroundOpacity(newStyle.backgroundOpacity)
                    },
                    onPlayNext = if (state.isQueueMode) viewModel::playNextNow else null,
                    onSeekRelative = { offset -> playerController.seekRelative(offset) },
                    onSeekTo = { position -> playerController.seekTo(position) },
                    onSpeedChanged = { speed -> playerController.setSpeed(speed) },
                    displayMode = displayMode,
                    onDisplayModeChanged = { mode ->
                        displayMode = mode
                        appPreferences.setPlayerDisplayMode(mode.name)
                    },
                    onUserInteraction = { viewModel.resetControlsHideTimer() },
                    onBack = onBack,
                )
            }

            // Auto-play countdown overlay
            val nextQueueItem = state.nextQueueItem
            if (state.showAutoPlayCountdown && nextQueueItem != null) {
                AutoPlayCountdown(
                    nextItem = nextQueueItem,
                    countdownSeconds = state.countdownSecondsRemaining,
                    jellyfinClient = jellyfinClient,
                    onPlayNow = { viewModel.playNextNow() },
                    onCancel = {
                        viewModel.cancelQueue()
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Skip segment button (intro/outro)
            if (state.activeSegment != null && !state.showControls && !state.showAutoPlayCountdown) {
                SkipSegmentButton(
                    label = viewModel.getSkipButtonLabel(),
                    onSkip = {
                        viewModel.getSkipTargetMs()?.let { targetMs ->
                            playerController.seekTo(targetMs)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 24.dp)
                        .navigationBarsPadding(),
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
    displayMode: PlayerDisplayMode,
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
        val (surfaceWidth, surfaceHeight) = when (displayMode) {
            PlayerDisplayMode.FIT -> if (videoAspect > containerAspect) {
                containerWidth to containerWidth / videoAspect
            } else {
                containerHeight * videoAspect to containerHeight
            }
            PlayerDisplayMode.FILL -> if (videoAspect > containerAspect) {
                containerHeight * videoAspect to containerHeight
            } else {
                containerWidth to containerWidth / videoAspect
            }
            PlayerDisplayMode.ZOOM -> {
                val zoomFactor = 1.1f
                val base = if (videoAspect > containerAspect) {
                    containerHeight * videoAspect to containerHeight
                } else {
                    containerWidth to containerWidth / videoAspect
                }
                base.first * zoomFactor to base.second * zoomFactor
            }
            PlayerDisplayMode.STRETCH -> containerWidth to containerHeight
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
                            height: Int
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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ExoPlayerSurfaceView(
    playerController: PlayerController,
    url: String,
    startPositionMs: Long,
    displayMode: PlayerDisplayMode,
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
                resizeMode = when (displayMode) {
                    PlayerDisplayMode.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    PlayerDisplayMode.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    PlayerDisplayMode.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    PlayerDisplayMode.STRETCH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view ->
            view.player = playerController.exoPlayer
            view.resizeMode = when (displayMode) {
                PlayerDisplayMode.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                PlayerDisplayMode.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                PlayerDisplayMode.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                PlayerDisplayMode.STRETCH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobilePlayerControls(
    item: JellyfinItem?,
    playbackState: PlaybackState,
    playerController: PlayerController,
    audioStreams: List<MediaStream>,
    subtitleStreams: List<MediaStream>,
    selectedAudioIndex: Int?,
    selectedSubtitleIndex: Int?,
    onAudioSelected: (Int?) -> Unit,
    onSubtitleSelected: (Int?) -> Unit,
    subtitleStyle: SubtitleStyle,
    onSubtitleStyleChanged: (SubtitleStyle) -> Unit,
    onPlayNext: (() -> Unit)?,
    onSeekRelative: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    displayMode: PlayerDisplayMode,
    onDisplayModeChanged: (PlayerDisplayMode) -> Unit,
    onUserInteraction: () -> Unit,
    onBack: () -> Unit,
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val isDV = videoQuality.contains("Dolby Vision")
    val isHDR = videoQuality.contains("HDR")
    val showNext = onPlayNext != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeSheet by remember { mutableStateOf<PlayerSheet?>(null) }
    val speed = playbackState.speed
    val subtitleText = buildString {
        item?.seriesName?.let { append(it) }
        if (item?.parentIndexNumber != null && item.indexNumber != null) {
            if (isNotEmpty()) append(" • ")
            append("S${item.parentIndexNumber}E${item.indexNumber}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                onUserInteraction()
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                if (subtitleText.isNotBlank()) {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    PlayerPill(
                        text = playbackState.playerType,
                        backgroundColor = when (playbackState.playerType) {
                            "MPV" -> Color(0xFF9C27B0)
                            "ExoPlayer" -> Color(0xFF2196F3)
                            else -> Color.Gray
                        },
                    )
                    if (videoQuality.isNotBlank()) {
                        PlayerPill(
                            text = videoQuality,
                            backgroundColor = Color.White.copy(alpha = 0.15f),
                        )
                    }
                    when {
                        isDV -> PlayerPill(
                            text = "Dolby Vision",
                            backgroundColor = Color(0xFFE50914),
                        )
                        isHDR -> PlayerPill(
                            text = "HDR",
                            backgroundColor = Color(0xFFFFD700),
                            textColor = Color.Black,
                        )
                    }
                }
            }
        }

        // Center controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onUserInteraction()
                    onSeekRelative(-PlayerConstants.SEEK_STEP_MS)
                },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Default.FastRewind,
                    contentDescription = "Rewind 10s",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(78.dp),
            ) {
                IconButton(
                    onClick = {
                        onUserInteraction()
                        playerController.togglePause()
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        if (playbackState.isPlaying && !playbackState.isPaused) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            IconButton(
                onClick = {
                    onUserInteraction()
                    onSeekRelative(PlayerConstants.SEEK_STEP_MS)
                },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Default.FastForward,
                    contentDescription = "Forward 10s",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            if (showNext) {
                IconButton(
                    onClick = {
                        onUserInteraction()
                        onPlayNext()
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding(),
        ) {
            Slider(
                value = playbackState.progress,
                onValueChange = {
                    onUserInteraction()
                    onSeekTo((it * playbackState.duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = PlayerUtils.formatTime(playbackState.position),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
                Text(
                    text = "x${"%.2f".format(speed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = PlayerUtils.formatTime(playbackState.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PlayerActionButton(
                    label = "Audio",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.AUDIO
                    },
                )
                PlayerActionButton(
                    label = "Subtitles",
                    icon = Icons.Default.ClosedCaption,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.SUBTITLES
                    },
                )
                PlayerActionButton(
                    label = "Sub Style",
                    icon = Icons.Default.FormatSize,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.SUB_STYLE
                    },
                )
                PlayerActionButton(
                    label = "Speed",
                    icon = Icons.Default.Speed,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.SPEED
                    },
                )
                PlayerActionButton(
                    label = "Display",
                    icon = Icons.Default.AspectRatio,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.DISPLAY
                    },
                )
                PlayerActionButton(
                    label = "Chapters",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.CHAPTERS
                    },
                )
                PlayerActionButton(
                    label = "Info",
                    icon = Icons.Default.Info,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.INFO
                    },
                )
            }
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = Color(0xFF101318),
        ) {
            MobilePlayerSheetContent(
                sheet = sheet,
                item = item,
                audioStreams = audioStreams,
                subtitleStreams = subtitleStreams,
                selectedAudioIndex = selectedAudioIndex,
                selectedSubtitleIndex = selectedSubtitleIndex,
                subtitleStyle = subtitleStyle,
                speed = speed,
                displayMode = displayMode,
                onAudioSelected = {
                    onUserInteraction()
                    onAudioSelected(it)
                    activeSheet = null
                },
                onSubtitleSelected = {
                    onUserInteraction()
                    onSubtitleSelected(it)
                    activeSheet = null
                },
                onSubtitleStyleChanged = {
                    onUserInteraction()
                    onSubtitleStyleChanged(it)
                },
                onSpeedSelected = {
                    onUserInteraction()
                    onSpeedChanged(it)
                    activeSheet = null
                },
                onDisplayModeSelected = {
                    onUserInteraction()
                    onDisplayModeChanged(it)
                    activeSheet = null
                },
                onChapterSelected = { chapterMs ->
                    onUserInteraction()
                    onSeekTo(chapterMs)
                    activeSheet = null
                },
            )
        }
    }
}

private enum class PlayerSheet {
    AUDIO,
    SUBTITLES,
    SUB_STYLE,
    SPEED,
    DISPLAY,
    CHAPTERS,
    INFO,
}

@Composable
private fun PlayerPill(text: String, backgroundColor: Color, textColor: Color = Color.White) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PlayerActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = Color.White)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun MobilePlayerSheetContent(
    sheet: PlayerSheet,
    item: JellyfinItem?,
    audioStreams: List<MediaStream>,
    subtitleStreams: List<MediaStream>,
    selectedAudioIndex: Int?,
    selectedSubtitleIndex: Int?,
    subtitleStyle: SubtitleStyle,
    speed: Float,
    displayMode: PlayerDisplayMode,
    onAudioSelected: (Int?) -> Unit,
    onSubtitleSelected: (Int?) -> Unit,
    onSubtitleStyleChanged: (SubtitleStyle) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onDisplayModeSelected: (PlayerDisplayMode) -> Unit,
    onChapterSelected: (Long) -> Unit,
) {
    when (sheet) {
        PlayerSheet.AUDIO -> {
            SheetHeader(title = "Audio Tracks")
            if (audioStreams.isEmpty()) {
                SheetEmptyState(message = "No audio tracks available.")
            } else {
                LazyColumn {
                    items(audioStreams, key = { it.index }) { stream ->
                        SheetRow(
                            title = stream.audioLabel(),
                            selected = selectedAudioIndex == stream.index,
                            onClick = { onAudioSelected(stream.index) },
                        )
                    }
                }
            }
        }
        PlayerSheet.SUBTITLES -> {
            SheetHeader(title = "Subtitles")
            if (subtitleStreams.isEmpty()) {
                SheetEmptyState(message = "No subtitle tracks available.")
            } else {
                LazyColumn {
                    item {
                        SheetRow(
                            title = "Off",
                            selected = selectedSubtitleIndex == PlayerConstants.TRACK_DISABLED,
                            onClick = { onSubtitleSelected(PlayerConstants.TRACK_DISABLED) },
                        )
                    }
                    items(subtitleStreams, key = { it.index }) { stream ->
                        SheetRow(
                            title = stream.subtitleLabel(),
                            selected = selectedSubtitleIndex == stream.index,
                            onClick = { onSubtitleSelected(stream.index) },
                        )
                    }
                }
            }
        }
        PlayerSheet.SUB_STYLE -> {
            SheetHeader(title = "Subtitle Style")
            LazyColumn {
                // Font Size section
                item { SheetSectionHeader("Font Size") }
                items(SubtitleFontSize.entries) { size ->
                    SheetRow(
                        title = size.label,
                        selected = subtitleStyle.fontSize == size,
                        onClick = { onSubtitleStyleChanged(subtitleStyle.copy(fontSize = size)) },
                    )
                }
                // Font Color section
                item { SheetSectionHeader("Font Color") }
                items(SubtitleColor.entries) { color ->
                    SheetRow(
                        title = color.label,
                        selected = subtitleStyle.fontColor == color,
                        onClick = { onSubtitleStyleChanged(subtitleStyle.copy(fontColor = color)) },
                    )
                }
                // Background Opacity section
                item { SheetSectionHeader("Background") }
                val opacities = listOf(0, 25, 50, 75, 100)
                items(opacities) { opacity ->
                    SheetRow(
                        title = "$opacity%",
                        selected = subtitleStyle.backgroundOpacity == opacity,
                        onClick = { onSubtitleStyleChanged(subtitleStyle.copy(backgroundOpacity = opacity)) },
                    )
                }
            }
        }
        PlayerSheet.SPEED -> {
            SheetHeader(title = "Playback Speed")
            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            LazyColumn {
                items(speeds) { value ->
                    SheetRow(
                        title = "x${"%.2f".format(value)}",
                        selected = speed == value,
                        onClick = { onSpeedSelected(value) },
                    )
                }
            }
        }
        PlayerSheet.DISPLAY -> {
            SheetHeader(title = "Display Mode")
            LazyColumn {
                items(PlayerDisplayMode.values()) { mode ->
                    SheetRow(
                        title = mode.label,
                        selected = displayMode == mode,
                        onClick = { onDisplayModeSelected(mode) },
                    )
                }
            }
        }
        PlayerSheet.CHAPTERS -> {
            SheetHeader(title = "Chapters")
            val chapters = item?.chapters.orEmpty()
            if (chapters.isEmpty()) {
                SheetEmptyState(message = "No chapters available.")
            } else {
                LazyColumn {
                    items(chapters) { chapter ->
                        val startMs = chapter.startPositionTicks?.let { PlayerUtils.ticksToMs(it) } ?: 0L
                        SheetRow(
                            title = chapter.name ?: "Chapter",
                            subtitle = PlayerUtils.formatTime(startMs),
                            selected = false,
                            onClick = { onChapterSelected(startMs) },
                        )
                    }
                }
            }
        }
        PlayerSheet.INFO -> {
            SheetHeader(title = "Media Info")
            val mediaSource = item?.mediaSources?.firstOrNull()
            val streams = mediaSource?.mediaStreams.orEmpty()
            LazyColumn {
                item {
                    SheetInfoRow("Container", mediaSource?.container?.uppercase() ?: "Unknown")
                    streams.firstOrNull { it.type == "Video" }?.let { video ->
                        SheetInfoRow(
                            "Video",
                            listOfNotNull(
                                video.codec?.uppercase(),
                                video.width?.let { w -> video.height?.let { h -> "${w}x$h" } },
                                video.videoRangeType,
                            ).joinToString(" • "),
                        )
                    }
                }
                items(streams.filter { it.type == "Audio" }) { audio ->
                    SheetInfoRow("Audio", audio.audioLabel())
                }
                items(streams.filter { it.type == "Subtitle" }) { subtitle ->
                    SheetInfoRow("Subtitle", subtitle.subtitleLabel())
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun SheetSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(top = 8.dp),
    )
}

@Composable
private fun SheetRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .then(
                if (selected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let {
                Text(text = it, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (selected) {
            Text(text = "Selected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SheetInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SheetEmptyState(message: String) {
    Text(
        text = message,
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

/**
 * Skip segment button for intro/outro skipping.
 * Appears in the bottom-right corner when a skippable segment is detected.
 */
@Composable
private fun SkipSegmentButton(
    label: String,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onSkip,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Skip",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
