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

import android.app.Activity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.util.Rational
import androidx.media3.ui.AspectRatioFrameLayout
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HighQuality
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import dev.jausc.myflix.core.viewmodel.PlayerViewModel
import androidx.media3.ui.PlayerView
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaSegmentType
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.audioLabel
import dev.jausc.myflix.core.common.model.subtitleLabel
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.network.websocket.PlaystateCommandType
import dev.jausc.myflix.core.network.websocket.WebSocketEvent
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlaybackState
import dev.jausc.myflix.core.player.PlayerBackend
import dev.jausc.myflix.core.player.PlayerConstants
import dev.jausc.myflix.core.player.PlayerDisplayMode
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.core.player.TrickplayProvider
import dev.jausc.myflix.core.player.SubtitleStyle
import dev.jausc.myflix.core.player.SubtitleFontSize
import dev.jausc.myflix.core.player.SubtitleColor
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.common.preferences.PlaybackOptions
import dev.jausc.myflix.mobile.ui.components.AutoPlayCountdown
import dev.jausc.myflix.core.common.ui.util.SubsetTransformation
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerScreen(
    itemId: String,
    startPositionMs: Long? = null,
    jellyfinClient: JellyfinClient,
    appPreferences: AppPreferences,
    useMpvPlayer: Boolean = false,
    webSocketEvents: SharedFlow<WebSocketEvent>? = null,
    onBack: () -> Unit,
    onPlayerActiveChange: (Boolean, Rational?) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current

    // PiP Mode detection
    var isInPipMode by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val activity = context.findActivity() as? androidx.activity.ComponentActivity
        val consumer = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
        }

        // Initial check
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            isInPipMode = activity?.isInPictureInPictureMode == true
        }

        activity?.addOnPictureInPictureModeChangedListener(consumer)

        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(consumer)
        }
    }

    // Get preferred audio and subtitle language from preferences
    val preferredAudioLanguage by appPreferences.preferredAudioLanguage.collectAsState()
    val preferredSubtitleLanguage by appPreferences.preferredSubtitleLanguage.collectAsState()
    val maxStreamingBitrate by appPreferences.maxStreamingBitrate.collectAsState()
    val skipForwardSeconds by appPreferences.skipForwardSeconds.collectAsState()
    val skipBackwardSeconds by appPreferences.skipBackwardSeconds.collectAsState()

    // Get skip mode preferences (OFF, ASK, AUTO)
    val skipIntroMode by appPreferences.skipIntroMode.collectAsState()
    val skipCreditsMode by appPreferences.skipCreditsMode.collectAsState()

    // Calculate skip durations in milliseconds
    val skipForwardMs = remember(skipForwardSeconds) { skipForwardSeconds * 1000L }
    val skipBackwardMs = remember(skipBackwardSeconds) { skipBackwardSeconds * 1000L }

    // Get display mode preference
    val displayModeName by appPreferences.playerDisplayMode.collectAsState()

    // Get refresh rate mode preference
    val refreshRateMode by appPreferences.refreshRateMode.collectAsState()

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
            appPreferences = appPreferences,
            preferredAudioLanguage = preferredAudioLanguage,
            preferredSubtitleLanguage = preferredSubtitleLanguage,
            maxStreamingBitrateMbps = maxStreamingBitrate,
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

    // Current bitrate - can be changed during playback (0 = no limit / direct play)
    var currentBitrate by remember { mutableIntStateOf(maxStreamingBitrate) }
    var didFallbackToMpv by remember { mutableStateOf(false) }

    // Update active state and aspect ratio for PiP
    LaunchedEffect(playbackState.videoWidth, playbackState.videoHeight) {
        if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
            val ratio = Rational(playbackState.videoWidth, playbackState.videoHeight)
            onPlayerActiveChange(true, ratio)
        } else {
            onPlayerActiveChange(true, Rational(16, 9))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onPlayerActiveChange(false, null)
        }
    }

    val selectedAudioIndex = state.selectedAudioStreamIndex
    val selectedSubtitleIndex = state.selectedSubtitleStreamIndex
    // Use the stream URL from ViewModel (which uses PlaybackInfo API with transcoding support)
    val effectiveStreamUrl = state.streamUrl

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

    // Cleanup player resources and report playback stopped
    DisposableEffect(Unit) {
        onDispose {
            // Report playback stopped BEFORE releasing player
            viewModel.stopPlayback(playerController.state.value.position)
            playerController.stop()
            playerController.release()
            // Reset refresh rate when leaving player
            (context as? Activity)?.let { activity ->
                PlayerController.resetRefreshRate(activity)
            }
        }
    }

    // Apply refresh rate when playback starts or mode changes
    LaunchedEffect(playbackState.isPlaying, refreshRateMode, playbackState.videoWidth) {
        if (playbackState.isPlaying && playbackState.videoWidth > 0) {
            (context as? Activity)?.let { activity ->
                // Use video frame rate if available, otherwise estimate from common frame rates
                val videoFps = if (playbackState.videoHeight > 0) {
                    // Most content is 24fps (movies) or 30fps (TV), default to 24 for cinematic content
                    24f
                } else {
                    0f
                }
                PlayerController.applyRefreshRate(activity, refreshRateMode, videoFps)
            }
        }
    }

    // Apply subtitle style when it changes or player becomes ready
    LaunchedEffect(subtitleStyle, playerController.isInitialized) {
        if (playerController.isInitialized) {
            playerController.setSubtitleStyle(subtitleStyle)
        }
    }

    // Fallback to MPV if ExoPlayer fails to decode
    LaunchedEffect(playbackState.error, effectiveStreamUrl) {
        if (!didFallbackToMpv &&
            playbackState.error != null &&
            effectiveStreamUrl != null &&
            playerController.backend == PlayerBackend.EXOPLAYER
        ) {
            val fallbackPosition = playbackState.position
            if (playerController.switchToMpvForError(playbackState.error)) {
                didFallbackToMpv = true
                currentStartPositionMs = fallbackPosition
            }
        }
    }

    // Handle WebSocket remote control commands
    LaunchedEffect(webSocketEvents) {
        webSocketEvents?.collect { event ->
            when (event) {
                is WebSocketEvent.PlaystateCommand -> {
                    android.util.Log.d("PlayerScreen", "Remote command: ${event.command}")
                    when (event.command) {
                        PlaystateCommandType.PlayPause -> playerController.togglePause()
                        PlaystateCommandType.Pause -> playerController.pause()
                        PlaystateCommandType.Unpause -> playerController.resume()
                        PlaystateCommandType.Stop -> {
                            viewModel.stopPlayback(playerController.state.value.position)
                            onBack()
                        }
                        PlaystateCommandType.Seek -> {
                            event.seekPositionTicks?.let { ticks ->
                                val positionMs = PlayerUtils.ticksToMs(ticks)
                                playerController.seekTo(positionMs)
                            }
                        }
                        PlaystateCommandType.Rewind -> playerController.seekRelative(-skipBackwardMs)
                        PlaystateCommandType.FastForward -> playerController.seekRelative(skipForwardMs)
                        PlaystateCommandType.NextTrack -> viewModel.playNextNow()
                        PlaystateCommandType.PreviousTrack -> {
                            if (playerController.state.value.position > 5000) {
                                playerController.seekTo(0)
                            }
                        }
                    }
                }
                else -> Unit // Other events handled by MainActivity
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!isInPipMode) {
                    viewModel.toggleControls()
                }
            },
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

            val activeSegment = state.activeSegment
            val currentSkipMode = when (activeSegment?.type) {
                MediaSegmentType.Intro -> skipIntroMode
                MediaSegmentType.Outro -> skipCreditsMode
                else -> "OFF"
            }

            // Controls overlay
            if (state.showControls && !isInPipMode) {
                MobilePlayerControls(
                    item = state.item,
                    playbackState = playbackState,
                    playerController = playerController,
                    skipForwardMs = skipForwardMs,
                    skipBackwardMs = skipBackwardMs,
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
                        viewModel.updatePlaybackOptions(
                            audioStreamIndex = index,
                            subtitleStreamIndex = selectedSubtitleIndex,
                            maxBitrateMbps = currentBitrate,
                            startPositionMs = playbackState.position,
                        )
                    },
                    onSubtitleSelected = { index ->
                        currentStartPositionMs = playbackState.position
                        viewModel.setSubtitleStreamIndex(index)
                        viewModel.updatePlaybackOptions(
                            audioStreamIndex = selectedAudioIndex,
                            subtitleStreamIndex = index,
                            maxBitrateMbps = currentBitrate,
                            startPositionMs = playbackState.position,
                        )
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
                    currentBitrate = currentBitrate,
                    onBitrateChanged = { bitrate ->
                        currentStartPositionMs = playbackState.position
                        currentBitrate = bitrate
                        appPreferences.setMaxStreamingBitrate(bitrate)
                        viewModel.updatePlaybackOptions(
                            audioStreamIndex = selectedAudioIndex,
                            subtitleStreamIndex = selectedSubtitleIndex,
                            maxBitrateMbps = bitrate,
                            startPositionMs = playbackState.position,
                        )
                    },
                    onUserInteraction = { viewModel.resetControlsHideTimer() },
                    onBack = onBack,
                    trickplayProvider = state.trickplayProvider,
                    jellyfinClient = jellyfinClient,
                    itemId = state.item?.id,
                    skipLabel = if (activeSegment != null && currentSkipMode == "ASK") {
                        viewModel.getSkipButtonLabel()
                    } else {
                        null
                    },
                    onSkipSegment = if (activeSegment != null && currentSkipMode == "ASK") {
                        {
                            viewModel.getSkipTargetMs()?.let { targetMs ->
                                playerController.seekTo(targetMs)
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            // Auto-play countdown overlay
            val nextQueueItem = state.nextQueueItem
            if (state.showAutoPlayCountdown && nextQueueItem != null && !isInPipMode) {
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

            // Auto-skip: automatically skip when entering a segment and mode is AUTO
            LaunchedEffect(activeSegment?.id, currentSkipMode) {
                if (activeSegment != null && currentSkipMode == "AUTO") {
                    viewModel.getSkipTargetMs()?.let { targetMs ->
                        playerController.seekTo(targetMs)
                    }
                }
            }

            // Show skip button only when mode is ASK
            if (activeSegment != null && currentSkipMode == "ASK" &&
                !state.showControls && !state.showAutoPlayCountdown && !isInPipMode
            ) {
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

    // Keep reference to PlayerView for direct updates
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    // Update display mode when it changes - use view scaling for hardcoded letterboxing
    LaunchedEffect(displayMode) {
        playerView?.let { view ->
            // For hardcoded letterboxing, we need to scale the view to zoom/crop
            when (displayMode) {
                PlayerDisplayMode.FIT -> {
                    view.scaleX = 1.0f
                    view.scaleY = 1.0f
                }
                PlayerDisplayMode.FILL -> {
                    // Scale to crop typical 2.35:1 letterboxing
                    view.scaleX = 1.33f
                    view.scaleY = 1.33f
                }
                PlayerDisplayMode.ZOOM -> {
                    // Zoom in more
                    view.scaleX = 1.5f
                    view.scaleY = 1.5f
                }
                PlayerDisplayMode.STRETCH -> {
                    // Stretch to fill - scale Y more to remove letterboxing without scaling X
                    view.scaleX = 1.0f
                    view.scaleY = 1.33f
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = playerController.exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                playerView = this
            }
        },
        update = { view ->
            view.player = playerController.exoPlayer
            playerView = view
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
    skipForwardMs: Long,
    skipBackwardMs: Long,
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
    currentBitrate: Int,
    onBitrateChanged: (Int) -> Unit,
    onUserInteraction: () -> Unit,
    onBack: () -> Unit,
    trickplayProvider: TrickplayProvider? = null,
    jellyfinClient: JellyfinClient? = null,
    itemId: String? = null,
    skipLabel: String? = null,
    onSkipSegment: (() -> Unit)? = null,
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val isDV = videoQuality.contains("Dolby Vision")
    val isHDR = videoQuality.contains("HDR")
    val showNext = onPlayNext != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeSheet by remember { mutableStateOf<PlayerSheet?>(null) }
    val speed = playbackState.speed
    val accentColor = Color(0xFF2563EB)
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(playbackState.position) }
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
            .background(Color.Black.copy(alpha = 0.15f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                    ),
                ),
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart),
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
                            backgroundColor = Color.White.copy(alpha = 0.16f),
                        )
                    }
                    when {
                        isDV -> PlayerPill(
                            text = "Dolby Vision",
                            backgroundColor = accentColor,
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
        val skipBackwardLabel = (skipBackwardMs / 1000).toInt()
        val skipForwardLabel = (skipForwardMs / 1000).toInt()
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onUserInteraction()
                    onSeekRelative(-skipBackwardMs)
                },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Default.FastRewind,
                    contentDescription = "Rewind ${skipBackwardLabel}s",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            Surface(
                shape = CircleShape,
                color = accentColor,
                modifier = Modifier.size(82.dp),
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
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
            IconButton(
                onClick = {
                    onUserInteraction()
                    onSeekRelative(skipForwardMs)
                },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Default.FastForward,
                    contentDescription = "Forward ${skipForwardLabel}s",
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
            LaunchedEffect(playbackState.position) {
                if (!isScrubbing) {
                    scrubPositionMs = playbackState.position
                }
            }

            if (skipLabel != null && onSkipSegment != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    SkipActionPill(
                        label = skipLabel,
                        accentColor = accentColor,
                        onClick = {
                            onUserInteraction()
                            onSkipSegment()
                        },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isScrubbing) {
                if (trickplayProvider != null && jellyfinClient != null && itemId != null) {
                    TrickplayPreview(
                        trickplayProvider = trickplayProvider,
                        jellyfinClient = jellyfinClient,
                        itemId = itemId,
                        positionMs = scrubPositionMs,
                        durationMs = playbackState.duration,
                    )
                } else {
                    TimeOnlyPreview(
                        timeLabel = PlayerUtils.formatTime(scrubPositionMs),
                        progress = if (playbackState.duration > 0) {
                            scrubPositionMs.toFloat() / playbackState.duration.toFloat()
                        } else {
                            0f
                        },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Slider(
                value = if (isScrubbing && playbackState.duration > 0) {
                    (scrubPositionMs.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    playbackState.progress
                },
                onValueChange = {
                    onUserInteraction()
                    isScrubbing = true
                    scrubPositionMs = (it * playbackState.duration).toLong()
                    onSeekTo(scrubPositionMs)
                },
                onValueChangeFinished = {
                    isScrubbing = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
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
                horizontalArrangement = Arrangement.SpaceAround,
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
                    label = "Quality",
                    icon = Icons.Default.HighQuality,
                    onClick = {
                        onUserInteraction()
                        activeSheet = PlayerSheet.QUALITY
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
            containerColor = Color(0xFF141414),
        ) {
            SheetHandle()
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
                currentBitrate = currentBitrate,
                onBitrateSelected = {
                    onUserInteraction()
                    onBitrateChanged(it)
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
    QUALITY,
    CHAPTERS,
    INFO,
}

@Composable
private fun PlayerPill(text: String, backgroundColor: Color, textColor: Color = Color.White) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PlayerActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .padding(vertical = 4.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
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
    currentBitrate: Int,
    onBitrateSelected: (Int) -> Unit,
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
        PlayerSheet.QUALITY -> {
            SheetHeader(title = "Streaming Quality")
            LazyColumn {
                items(PlaybackOptions.BITRATE_OPTIONS) { (bitrate, label) ->
                    SheetRow(
                        title = label,
                        selected = currentBitrate == bitrate,
                        onClick = { onBitrateSelected(bitrate) },
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
                    item?.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                        SheetInfoRow("Description", overview)
                    }
                }
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
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
            )
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

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun SkipActionPill(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = accentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Skip",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TrickplayPreview(
    trickplayProvider: TrickplayProvider,
    jellyfinClient: JellyfinClient,
    itemId: String,
    positionMs: Long,
    durationMs: Long,
) {
    val context = LocalContext.current
    val tileIndex = trickplayProvider.getTileImageIndex(positionMs)
    val (offsetX, offsetY) = trickplayProvider.getTileOffset(positionMs)
    val thumbnailWidth = trickplayProvider.thumbnailWidth
    val thumbnailHeight = trickplayProvider.thumbnailHeight
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val previewWidthDp = 140.dp
        val previewWidthPx = with(LocalDensity.current) { previewWidthDp.toPx() }
        val targetX = containerWidth * progress - previewWidthPx / 2
        val clampedX = targetX.coerceIn(0f, containerWidth - previewWidthPx)
        val offsetDp = with(LocalDensity.current) { clampedX.toDp() }

        Column(
            modifier = Modifier
                .offset(x = offsetDp)
                .width(previewWidthDp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(previewWidthDp)
                    .height(78.dp)
                    .background(Color.Black, RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(
                            jellyfinClient.getTrickplayTileUrl(
                                itemId = itemId,
                                width = thumbnailWidth,
                                tileIndex = tileIndex,
                                mediaSourceId = trickplayProvider.getMediaSourceId(),
                            ),
                        )
                        .transformations(
                            SubsetTransformation(
                                x = offsetX,
                                y = offsetY,
                                cropWidth = thumbnailWidth,
                                cropHeight = thumbnailHeight,
                            ),
                        )
                        .build(),
                    contentDescription = "Seek preview",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = PlayerUtils.formatTime(positionMs),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun TimeOnlyPreview(
    timeLabel: String,
    progress: Float,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val previewWidthDp = 72.dp
        val previewWidthPx = with(LocalDensity.current) { previewWidthDp.toPx() }
        val targetX = containerWidth * progress - previewWidthPx / 2
        val clampedX = targetX.coerceIn(0f, containerWidth - previewWidthPx)
        val offsetDp = with(LocalDensity.current) { clampedX.toDp() }

        Box(
            modifier = Modifier
                .offset(x = offsetDp)
                .width(previewWidthDp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
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

private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

/** Bitrate options for streaming quality selection (Mbps, label) */
