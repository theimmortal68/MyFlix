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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import dev.jausc.myflix.core.common.ui.ActionColors
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.audioLabel
import dev.jausc.myflix.core.common.model.subtitleLabel
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlayerBackend
import dev.jausc.myflix.core.player.PlayerConstants
import dev.jausc.myflix.core.player.PlayerDisplayMode
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogItemEntry
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.MediaInfoDialog
import dev.jausc.myflix.tv.ui.components.AutoPlayCountdown
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerScreen(
    itemId: String,
    startPositionMs: Long? = null,
    jellyfinClient: JellyfinClient,
    useMpvPlayer: Boolean = false,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    // ViewModel with manual DI
    val viewModel: PlayerViewModel = viewModel(
        key = itemId,
        factory = PlayerViewModel.Factory(itemId, jellyfinClient, startPositionMs),
    )

    // Collect UI state from ViewModel
    val state by viewModel.uiState.collectAsState()

    // Player controller from core module - pass MPV preference
    val playerController = remember { PlayerController(context, useMpv = useMpvPlayer) }

    // Collect player state
    val playbackState by playerController.state.collectAsState()
    var displayMode by remember { mutableStateOf(PlayerDisplayMode.FIT) }
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
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.showControls) {
        if (state.showControls) {
            delay(100)
            playPauseFocusRequester.requestFocus()
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

    // Detect video end and trigger auto-play countdown
    LaunchedEffect(playbackState.isEnded) {
        if (playbackState.isEnded && !state.showAutoPlayCountdown) {
            viewModel.onVideoEnded()
        }
    }

    // Cleanup player resources (ViewModel handles playback stop reporting)
    DisposableEffect(Unit) {
        onDispose {
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
                    if (state.showControls) {
                        when (event.key) {
                            Key.Back -> {
                                if (state.showAutoPlayCountdown) {
                                    viewModel.cancelQueue()
                                } else {
                                    viewModel.hideControls()
                                }
                                true
                            }
                            else -> false
                        }
                    } else {
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter -> {
                                viewModel.showControls()
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
                    }
                } else {
                    false
                }
            },
    ) {
        if (state.isLoading || !state.playerReady || effectiveStreamUrl == null) {
            LoadingIndicator("Loading...")
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
                TvPlayerControlsOverlay(
                    item = state.item,
                    playbackState = playbackState,
                    onPlayPause = { playerController.togglePause() },
                    audioStreams = state.audioStreams,
                    subtitleStreams = state.subtitleStreams,
                    selectedAudioIndex = selectedAudioIndex,
                    selectedSubtitleIndex = selectedSubtitleIndex,
                    onAudioSelected = { index ->
                        currentStartPositionMs = playbackState.position
                        viewModel.setAudioStreamIndex(index)
                    },
                    onSubtitleSelected = { index ->
                        currentStartPositionMs = playbackState.position
                        viewModel.setSubtitleStreamIndex(index)
                    },
                    onPlayNext = if (state.isQueueMode) viewModel::playNextNow else null,
                    onSeekRelative = { offset -> playerController.seekRelative(offset) },
                    onSeekTo = { position -> playerController.seekTo(position) },
                    onSpeedChanged = { speed -> playerController.setSpeed(speed) },
                    displayMode = displayMode,
                    onDisplayModeChanged = { displayMode = it },
                    onUserInteraction = { viewModel.resetControlsHideTimer() },
                    playPauseFocusRequester = playPauseFocusRequester,
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
private fun TvPlayerControlsOverlay(
    item: JellyfinItem?,
    playbackState: dev.jausc.myflix.core.player.PlaybackState,
    onPlayPause: () -> Unit,
    audioStreams: List<MediaStream>,
    subtitleStreams: List<MediaStream>,
    selectedAudioIndex: Int?,
    selectedSubtitleIndex: Int?,
    onAudioSelected: (Int?) -> Unit,
    onSubtitleSelected: (Int?) -> Unit,
    onPlayNext: (() -> Unit)?,
    onSeekRelative: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    displayMode: PlayerDisplayMode,
    onDisplayModeChanged: (PlayerDisplayMode) -> Unit,
    onUserInteraction: () -> Unit,
    playPauseFocusRequester: FocusRequester,
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val playerType = playbackState.playerType
    val isDV = videoQuality.contains("Dolby Vision")
    val isHDR = videoQuality.contains("HDR")
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var showMediaInfo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = item?.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            val subtitleText = buildString {
                item?.seriesName?.let { append(it) }
                if (item?.parentIndexNumber != null && item.indexNumber != null) {
                    if (isNotEmpty()) append(" • ")
                    append("S${item.parentIndexNumber}E${item.indexNumber}")
                }
            }
            if (subtitleText.isNotBlank()) {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerBadge(
                    text = playerType,
                    backgroundColor = when (playerType) {
                        "MPV" -> Color(0xFF9C27B0)
                        "ExoPlayer" -> Color(0xFF2196F3)
                        else -> Color.Gray
                    },
                )
                if (videoQuality.isNotBlank()) {
                    PlayerBadge(
                        text = videoQuality,
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                    )
                }
                when {
                    isDV -> PlayerBadge(
                        text = "Dolby Vision",
                        backgroundColor = Color(0xFFE50914),
                    )
                    isHDR -> PlayerBadge(
                        text = "HDR",
                        backgroundColor = Color(0xFFFFD700),
                        textColor = Color.Black,
                    )
                }
            }
        }

        // Center controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvControlButton(label = "-10s") {
                onUserInteraction()
                onSeekRelative(-PlayerConstants.SEEK_STEP_MS)
            }
            TvControlButton(
                label = if (playbackState.isPlaying && !playbackState.isPaused) "Pause" else "Play",
                focusRequester = playPauseFocusRequester,
            ) {
                onUserInteraction()
                onPlayPause()
            }
            TvControlButton(label = "+10s") {
                onUserInteraction()
                onSeekRelative(PlayerConstants.SEEK_STEP_MS)
            }
            if (onPlayNext != null) {
                TvControlButton(label = "Next") {
                    onUserInteraction()
                    onPlayNext()
                }
            }
        }

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        ) {
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
                    text = "x${"%.2f".format(playbackState.speed)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = PlayerUtils.formatTime(playbackState.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TvActionButton("Audio") {
                    onUserInteraction()
                    val items = if (audioStreams.isEmpty()) {
                        listOf(
                            DialogItem(
                                text = "No audio tracks available",
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                iconTint = ActionColors.Audio,
                                enabled = false,
                                onClick = {},
                            ),
                        )
                    } else {
                        audioStreams.map {
                            DialogItem(
                                text = it.audioLabel(),
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                iconTint = ActionColors.Audio,
                                onClick = { onAudioSelected(it.index) },
                            )
                        }
                    }
                    dialogParams = DialogParams(title = "Audio Tracks", items = items)
                }
                TvActionButton("Subtitles") {
                    onUserInteraction()
                    val items = if (subtitleStreams.isEmpty()) {
                        listOf(
                            DialogItem(
                                text = "No subtitles available",
                                icon = Icons.Default.ClosedCaptionOff,
                                iconTint = ActionColors.Subtitles,
                                enabled = false,
                                onClick = {},
                            ),
                        )
                    } else {
                        val entries = mutableListOf<DialogItemEntry>(
                            DialogItem(
                                text = "Off",
                                icon = Icons.Default.ClosedCaptionOff,
                                iconTint = ActionColors.Subtitles,
                                onClick = { onSubtitleSelected(PlayerConstants.TRACK_DISABLED) },
                            ),
                        )
                        entries.addAll(
                            subtitleStreams.map {
                                DialogItem(
                                    text = it.subtitleLabel(),
                                    icon = Icons.Default.ClosedCaption,
                                    iconTint = ActionColors.Subtitles,
                                    onClick = { onSubtitleSelected(it.index) },
                                )
                            },
                        )
                        entries
                    }
                    dialogParams = DialogParams(title = "Subtitles", items = items)
                }
                TvActionButton("Speed") {
                    onUserInteraction()
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    dialogParams = DialogParams(
                        title = "Playback Speed",
                        items = speeds.map { value ->
                            DialogItem(
                                text = "x${"%.2f".format(value)}",
                                icon = Icons.Default.Speed,
                                iconTint = ActionColors.Play,
                                onClick = {
                                    onUserInteraction()
                                    onSpeedChanged(value)
                                },
                            )
                        },
                    )
                }
                TvActionButton("Display") {
                    onUserInteraction()
                    dialogParams = DialogParams(
                        title = "Display Mode",
                        items = PlayerDisplayMode.values().map { mode ->
                            DialogItem(
                                text = mode.label,
                                icon = Icons.Default.AspectRatio,
                                iconTint = ActionColors.GoTo,
                                onClick = {
                                    onUserInteraction()
                                    onDisplayModeChanged(mode)
                                },
                            )
                        },
                    )
                }
                TvActionButton("Chapters") {
                    onUserInteraction()
                    val chapters = item?.chapters.orEmpty()
                    val items = if (chapters.isEmpty()) {
                        listOf(
                            DialogItem(
                                text = "No chapters available",
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                iconTint = ActionColors.Season,
                                enabled = false,
                                onClick = {},
                            ),
                        )
                    } else {
                        chapters.map { chapter ->
                            val startMs = chapter.startPositionTicks?.let { PlayerUtils.ticksToMs(it) } ?: 0L
                            DialogItem(
                                text = "${chapter.name ?: "Chapter"} • ${PlayerUtils.formatTime(startMs)}",
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                iconTint = ActionColors.Season,
                                onClick = {
                                    onUserInteraction()
                                    onSeekTo(startMs)
                                },
                            )
                        }
                    }
                    dialogParams = DialogParams(title = "Chapters", items = items)
                }
                TvActionButton("Info") {
                    onUserInteraction()
                    showMediaInfo = true
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "◀ -10s  |  ▶ +10s  |  ▲ +1min  |  ▼ -1min  |  OK Play/Pause",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    dialogParams?.let { params ->
        DialogPopup(
            params = params,
            onDismissRequest = { dialogParams = null },
        )
    }

    if (showMediaInfo && item != null) {
        MediaInfoDialog(
            item = item,
            onDismiss = { showMediaInfo = false },
        )
    }
}

@Composable
private fun TvControlButton(
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.7f),
            focusedContainerColor = TvColors.Surface,
        ),
        onClick = onClick,
        modifier = Modifier
            .size(width = 120.dp, height = 56.dp)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}

@Composable
private fun TvActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        colors = ButtonDefaults.colors(containerColor = TvColors.Surface),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
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
