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

import android.app.Activity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import dev.jausc.myflix.core.player.TrickplayProvider
import dev.jausc.myflix.tv.ui.util.SubsetTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ClosedCaptionOff
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jausc.myflix.core.viewmodel.PlayerMediaInfo
import dev.jausc.myflix.core.viewmodel.PlayerUiState
import dev.jausc.myflix.core.viewmodel.PlayerViewModel
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import androidx.tv.material3.Border
import dev.jausc.myflix.core.common.ui.ActionColors
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.JellyfinChapter
import dev.jausc.myflix.core.common.model.MediaSegmentType
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.audioLabel
import dev.jausc.myflix.core.common.model.subtitleLabel
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.network.websocket.PlaystateCommandType
import dev.jausc.myflix.core.network.websocket.WebSocketEvent
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlayerBackend
import kotlinx.coroutines.flow.SharedFlow
import dev.jausc.myflix.core.player.PlayerConstants
import dev.jausc.myflix.core.player.PlayerDisplayMode
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.core.player.SubtitleStyle
import dev.jausc.myflix.core.player.SubtitleFontSize
import dev.jausc.myflix.core.player.SubtitleColor
import dev.jausc.myflix.tv.ui.components.DialogItem
import dev.jausc.myflix.tv.ui.components.DialogItemEntry
import dev.jausc.myflix.tv.ui.components.DialogParams
import dev.jausc.myflix.tv.ui.components.DialogPopup
import dev.jausc.myflix.tv.ui.components.DialogSectionHeader
import dev.jausc.myflix.tv.ui.components.AutoPlayCountdown
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.common.preferences.PlaybackOptions
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
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

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
                            // Seek to beginning or previous if at start
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
                                playerController.seekRelative(-skipBackwardMs)
                                viewModel.showControls()
                                true
                            }
                            Key.DirectionRight -> {
                                playerController.seekRelative(skipForwardMs)
                                viewModel.showControls()
                                true
                            }
                            Key.DirectionUp, Key.DirectionDown -> {
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

            val activeSegment = state.activeSegment
            val currentSkipMode = when (activeSegment?.type) {
                MediaSegmentType.Intro -> skipIntroMode
                MediaSegmentType.Outro -> skipCreditsMode
                else -> "OFF"
            }

            // Controls overlay
            if (state.showControls && !state.showAutoPlayCountdown) {
                TvPlayerControlsOverlay(
                    item = state.item,
                    playbackState = playbackState,
                    onPlayPause = { playerController.togglePause() },
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
                        // Also save as preference for future playback
                        appPreferences.setMaxStreamingBitrate(bitrate)
                        viewModel.updatePlaybackOptions(
                            audioStreamIndex = selectedAudioIndex,
                            subtitleStreamIndex = selectedSubtitleIndex,
                            maxBitrateMbps = bitrate,
                            startPositionMs = playbackState.position,
                        )
                    },
                    onUserInteraction = { viewModel.resetControlsHideTimer() },
                    playPauseFocusRequester = playPauseFocusRequester,
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
                !state.showControls && !state.showAutoPlayCountdown
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
                        .padding(end = 48.dp, bottom = 48.dp),
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
    playPauseFocusRequester: FocusRequester,
    trickplayProvider: TrickplayProvider? = null,
    jellyfinClient: JellyfinClient? = null,
    itemId: String? = null,
    skipLabel: String? = null,
    onSkipSegment: (() -> Unit)? = null,
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val playerType = playbackState.playerType
    val isDV = videoQuality.contains("Dolby Vision")
    val isHDR = videoQuality.contains("HDR")
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var showChaptersRow by remember { mutableStateOf(false) }
    val settingsRowFocusRequester = remember { FocusRequester() }
    val chaptersRowFocusRequester = remember { FocusRequester() }

    // Request focus on chapters row after it becomes visible
    LaunchedEffect(showChaptersRow) {
        if (showChaptersRow) {
            chaptersRowFocusRequester.requestFocus()
        }
    }

    // Seeking state for interactive progress bar
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }
    val seekBarFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Reset seek position when current position changes significantly (not during seek)
    LaunchedEffect(playbackState.position) {
        if (!isSeeking) {
            seekPosition = playbackState.position
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        backgroundColor = Color(0xFF000000),
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
        val skipBackwardLabel = (skipBackwardMs / 1000).toInt()
        val skipForwardLabel = (skipForwardMs / 1000).toInt()
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvControlButton(label = "-${skipBackwardLabel}s") {
                onUserInteraction()
                onSeekRelative(-skipBackwardMs)
            }
            TvControlButton(
                label = if (playbackState.isPlaying && !playbackState.isPaused) "Pause" else "Play",
                isPrimary = true,
                                focusRequester = playPauseFocusRequester,
            ) {
                onUserInteraction()
                onPlayPause()
            }
            TvControlButton(label = "+${skipForwardLabel}s") {
                onUserInteraction()
                onSeekRelative(skipForwardMs)
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
        ) {
            // Interactive seek bar with trickplay preview
            InteractiveSeekBar(
                currentPosition = playbackState.position,
                duration = playbackState.duration,
                isSeeking = isSeeking,
                seekPosition = seekPosition,
                trickplayProvider = trickplayProvider,
                jellyfinClient = jellyfinClient,
                itemId = itemId,
                focusRequester = seekBarFocusRequester,
                                onSeekStart = {
                    isSeeking = true
                    seekPosition = playbackState.position
                    onUserInteraction()
                },
                onSeekChange = { newPosition ->
                    seekPosition = newPosition.coerceIn(0L, playbackState.duration)
                    onUserInteraction()
                },
                onSeekConfirm = {
                    isSeeking = false
                    onSeekTo(seekPosition)
                    onUserInteraction()
                },
                onSeekCancel = {
                    isSeeking = false
                    seekPosition = playbackState.position
                    onUserInteraction()
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isSeeking) PlayerUtils.formatTime(seekPosition) else PlayerUtils.formatTime(playbackState.position),
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        down = if (showChaptersRow) chaptersRowFocusRequester else FocusRequester.Cancel
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left group: Audio, Subtitles, Sub Style
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TvActionButton(
                        label = "Audio",
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                                focusRequester = settingsRowFocusRequester,
                        onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                    ) {
                        onUserInteraction()
                        val items = if (audioStreams.isEmpty()) {
                            listOf(
                                DialogItem(
                                    text = "No audio tracks available",
                                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                    iconTint = ActionColors.Audio,
                                    enabled = false,
                                    onClick = {},
                                ),
                            )
                        } else {
                            audioStreams.map {
                                DialogItem(
                                    text = it.audioLabel(),
                                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                    iconTint = ActionColors.Audio,
                                    onClick = { onAudioSelected(it.index) },
                                )
                            }
                        }
                        dialogParams = DialogParams(title = "Audio Tracks", items = items)
                    }
                    TvActionButton(
                        label = "Subtitles",
                        icon = Icons.Outlined.ClosedCaption,
                                                onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                    ) {
                        onUserInteraction()
                        val items = if (subtitleStreams.isEmpty()) {
                            listOf(
                                DialogItem(
                                    text = "No subtitles available",
                                    icon = Icons.Outlined.ClosedCaptionOff,
                                    iconTint = ActionColors.Subtitles,
                                    enabled = false,
                                    onClick = {},
                                ),
                            )
                        } else {
                            val entries = mutableListOf<DialogItemEntry>(
                                DialogItem(
                                    text = "Off",
                                    icon = Icons.Outlined.ClosedCaptionOff,
                                    iconTint = ActionColors.Subtitles,
                                    onClick = { onSubtitleSelected(PlayerConstants.TRACK_DISABLED) },
                                ),
                            )
                            entries.addAll(
                                subtitleStreams.map {
                                    DialogItem(
                                        text = it.subtitleLabel(),
                                        icon = Icons.Outlined.ClosedCaption,
                                        iconTint = ActionColors.Subtitles,
                                        onClick = { onSubtitleSelected(it.index) },
                                    )
                                },
                            )
                            entries
                        }
                        dialogParams = DialogParams(title = "Subtitles", items = items)
                    }
                    TvActionButton(
                        label = "Sub Style",
                        icon = Icons.Outlined.FormatSize,
                                                onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                    ) {
                        onUserInteraction()
                        val items = mutableListOf<DialogItemEntry>()

                        // Font Size section
                        items.add(DialogSectionHeader("Font Size"))
                        items.addAll(
                            SubtitleFontSize.entries.map { size ->
                                DialogItem(
                                    text = size.label,
                                    icon = Icons.Outlined.FormatSize,
                                    iconTint = if (subtitleStyle.fontSize == size) ActionColors.Subtitles else Color.Gray,
                                    onClick = {
                                        onUserInteraction()
                                        onSubtitleStyleChanged(subtitleStyle.copy(fontSize = size))
                                    },
                                )
                            },
                        )

                        // Font Color section
                        items.add(DialogSectionHeader("Font Color"))
                        items.addAll(
                            SubtitleColor.entries.map { color ->
                                DialogItem(
                                    text = color.label,
                                    icon = Icons.Outlined.ClosedCaption,
                                    iconTint = Color(color.argb),
                                    onClick = {
                                        onUserInteraction()
                                        onSubtitleStyleChanged(subtitleStyle.copy(fontColor = color))
                                    },
                                )
                            },
                        )

                        // Background Opacity section
                        items.add(DialogSectionHeader("Background"))
                        val opacities = listOf(0, 25, 50, 75, 100)
                        items.addAll(
                            opacities.map { opacity ->
                                DialogItem(
                                    text = "$opacity%",
                                    icon = Icons.Outlined.FormatSize,
                                    iconTint = if (subtitleStyle.backgroundOpacity == opacity) ActionColors.Subtitles else Color.Gray,
                                    onClick = {
                                        onUserInteraction()
                                        onSubtitleStyleChanged(subtitleStyle.copy(backgroundOpacity = opacity))
                                    },
                                )
                            },
                        )

                        dialogParams = DialogParams(title = "Subtitle Style", items = items)
                    }
                }

                // Right group: Speed, Display, Quality (+ Skip if available)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (skipLabel != null && onSkipSegment != null) {
                        TvActionButton(
                            label = skipLabel,
                            icon = Icons.Default.FastForward,
                                                        onDownPressed = {
                                if (!showChaptersRow) {
                                    showChaptersRow = true
                                    true
                                } else {
                                    false
                                }
                            },
                        ) {
                            onUserInteraction()
                            onSkipSegment()
                        }
                    }
                    TvActionButton(
                        label = "Speed",
                        icon = Icons.Outlined.Speed,
                                                onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                    ) {
                        onUserInteraction()
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        dialogParams = DialogParams(
                            title = "Playback Speed",
                            items = speeds.map { value ->
                                DialogItem(
                                    text = "x${"%.2f".format(value)}",
                                    icon = Icons.Outlined.Speed,
                                    iconTint = ActionColors.Play,
                                    onClick = {
                                        onUserInteraction()
                                        onSpeedChanged(value)
                                    },
                                )
                            },
                        )
                    }
                    TvActionButton(
                        label = "Display",
                        icon = Icons.Outlined.AspectRatio,
                                                onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                    ) {
                        onUserInteraction()
                        dialogParams = DialogParams(
                            title = "Display Mode",
                            items = PlayerDisplayMode.values().map { mode ->
                                DialogItem(
                                    text = mode.label,
                                    icon = Icons.Outlined.AspectRatio,
                                    iconTint = ActionColors.GoTo,
                                    onClick = {
                                        onUserInteraction()
                                        onDisplayModeChanged(mode)
                                    },
                                )
                            },
                        )
                    }
                    TvActionButton(
                        label = "Quality",
                        icon = Icons.Outlined.HighQuality,
                                                onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                    ) {
                        onUserInteraction()
                        dialogParams = DialogParams(
                            title = "Streaming Quality",
                            items = PlaybackOptions.BITRATE_OPTIONS.map { (bitrate, label) ->
                                val isSelected = bitrate == currentBitrate
                                DialogItem(
                                    text = if (isSelected) "$label ✓" else label,
                                    icon = Icons.Outlined.HighQuality,
                                    iconTint = if (isSelected) ActionColors.Play else ActionColors.Audio,
                                    onClick = {
                                        onUserInteraction()
                                        onBitrateChanged(bitrate)
                                    },
                                )
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showChaptersRow) {
                ChapterThumbRow(
                    chapters = item?.chapters.orEmpty(),
                    trickplayProvider = trickplayProvider,
                    jellyfinClient = jellyfinClient,
                    itemId = itemId,
                    upFocusRequester = settingsRowFocusRequester,
                    rowFocusRequester = chaptersRowFocusRequester,
                    onChapterSelected = { chapterMs ->
                        onUserInteraction()
                        onSeekTo(chapterMs)
                    },
                )
            }

        }
    }

    dialogParams?.let { params ->
        DialogPopup(
            params = params,
            onDismissRequest = { dialogParams = null },
        )
    }
}

@Composable
private fun TvControlButton(
    label: String,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isPrimary) Color.White else Color.Black.copy(alpha = 0.6f)
    val focusedColor = if (isPrimary) Color.White else Color.White.copy(alpha = 0.2f)
    val textColor = if (isPrimary) Color.Black else Color.White
    Surface(
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = focusedColor,
        ),
        onClick = onClick,
        modifier = Modifier
            .height(36.dp)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = if (isPrimary) 20.dp else 12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
            )
        }
    }
}

@Composable
private fun TvActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    focusRequester: FocusRequester? = null,
    onDownPressed: (() -> Boolean)? = null,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    // Animation for the glow effect
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "glowAlpha",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "iconScale",
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    onDownPressed?.invoke() == true
                } else if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Subtle glow effect
        Box(
            modifier = Modifier
                .size(26.dp)
                .alpha(glowAlpha)
                .blur(4.dp)
                .clip(CircleShape)
                .background(Color.White),
        )

        // Icon
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .size(20.dp)
                .scale(iconScale),
            tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ChapterThumbRow(
    chapters: List<JellyfinChapter>,
    trickplayProvider: TrickplayProvider?,
    jellyfinClient: JellyfinClient?,
    itemId: String?,
    upFocusRequester: FocusRequester,
    rowFocusRequester: FocusRequester,
    onChapterSelected: (Long) -> Unit,
) {
    if (chapters.isEmpty()) {
        Text(
            text = "No chapters available",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        return
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        itemsIndexed(chapters) { index, chapter ->
            val startMs = chapter.startPositionTicks?.let { PlayerUtils.ticksToMs(it) } ?: 0L
            val isFirst = index == 0
            ChapterThumbCard(
                chapter = chapter,
                chapterIndex = index,
                startMs = startMs,
                trickplayProvider = trickplayProvider,
                jellyfinClient = jellyfinClient,
                itemId = itemId,
                modifier = Modifier
                    .then(if (isFirst) Modifier.focusRequester(rowFocusRequester) else Modifier)
                    .focusProperties { up = upFocusRequester },
                onClick = { onChapterSelected(startMs) },
            )
        }
    }
}

@Composable
private fun ChapterThumbCard(
    chapter: JellyfinChapter,
    chapterIndex: Int,
    startMs: Long,
    trickplayProvider: TrickplayProvider?,
    jellyfinClient: JellyfinClient?,
    itemId: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val hasChapterImage = jellyfinClient != null && itemId != null
    val hasPreview = trickplayProvider != null && jellyfinClient != null && itemId != null
    val label = PlayerUtils.formatTime(startMs)

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.4f),
            focusedContainerColor = Color.Black.copy(alpha = 0.6f),
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        modifier = modifier
            .width(160.dp)
            .height(90.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasChapterImage) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(jellyfinClient!!.getChapterImageUrl(itemId!!, chapterIndex, maxWidth = 360))
                        .build(),
                    contentDescription = chapter.name ?: "Chapter",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (hasPreview) {
                val provider = trickplayProvider!!
                val tileIndex = provider.getTileImageIndex(startMs)
                val (offsetX, offsetY) = provider.getTileOffset(startMs)
                val thumbnailWidth = provider.thumbnailWidth
                val thumbnailHeight = provider.thumbnailHeight
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(
                            jellyfinClient!!.getTrickplayTileUrl(
                                itemId = itemId!!,
                                width = thumbnailWidth,
                                tileIndex = tileIndex,
                                mediaSourceId = provider.getMediaSourceId(),
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
                    contentDescription = chapter.name ?: "Chapter",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(topEnd = 8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }

            chapter.name?.takeIf { it.isNotBlank() }?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerBadge(text: String, backgroundColor: Color, textColor: Color = Color.White) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = textColor,
        )
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
    val focusRequester = remember { FocusRequester() }

    // Request focus when button appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.8f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        onClick = onSkip,
        modifier = modifier
            .focusRequester(focusRequester),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Text(
                text = "▶▶",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

/**
 * Interactive seek bar with trickplay preview support.
 * Handles D-pad navigation for seeking with optional thumbnail preview.
 */
@Composable
private fun InteractiveSeekBar(
    currentPosition: Long,
    duration: Long,
    isSeeking: Boolean,
    seekPosition: Long,
    trickplayProvider: TrickplayProvider?,
    jellyfinClient: JellyfinClient?,
    itemId: String?,
    focusRequester: FocusRequester,
    onSeekStart: () -> Unit,
    onSeekChange: (Long) -> Unit,
    onSeekConfirm: () -> Unit,
    onSeekCancel: () -> Unit,
) {
    val seekStepMs = 10_000L // 10 second steps for D-pad
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isFocused && trickplayProvider != null) 150.dp else 4.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Seek preview thumbnail (shown when progress bar is focused and trickplay available)
        if (isFocused && trickplayProvider != null && jellyfinClient != null && itemId != null) {
            val tileIndex = trickplayProvider.getTileImageIndex(seekPosition)
            val (offsetX, offsetY) = trickplayProvider.getTileOffset(seekPosition)
            val thumbnailWidth = trickplayProvider.thumbnailWidth
            val thumbnailHeight = trickplayProvider.thumbnailHeight

            // Calculate position for the preview (centered above seek position)
            val progress = if (duration > 0) seekPosition.toFloat() / duration.toFloat() else 0f

            SeekPreview(
                tileUrl = jellyfinClient.getTrickplayTileUrl(
                    itemId = itemId,
                    width = thumbnailWidth,
                    tileIndex = tileIndex,
                    mediaSourceId = trickplayProvider.getMediaSourceId(),
                ),
                offsetX = offsetX,
                offsetY = offsetY,
                thumbnailWidth = thumbnailWidth,
                thumbnailHeight = thumbnailHeight,
                timeLabel = PlayerUtils.formatTime(seekPosition),
                progress = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        } else if (isFocused) {
            // Time-only preview when trickplay not available
            TimeOnlyPreview(
                timeLabel = PlayerUtils.formatTime(seekPosition),
                progress = if (duration > 0) seekPosition.toFloat() / duration.toFloat() else 0f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused || isSeeking) 6.dp else 4.dp)
                .align(Alignment.BottomCenter)
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(alpha = 0.3f))
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    if (focusState.isFocused && !wasFocused) {
                        onSeekStart()
                    }
                }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && isFocused) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                onSeekChange(seekPosition - seekStepMs)
                                true
                            }
                            Key.DirectionRight -> {
                                onSeekChange(seekPosition + seekStepMs)
                                true
                            }
                            Key.DirectionCenter, Key.Enter -> {
                                onSeekConfirm()
                                true
                            }
                            Key.Back -> {
                                onSeekCancel()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
        ) {
            // Current position fill
            val displayProgress = if (isSeeking) {
                if (duration > 0) seekPosition.toFloat() / duration.toFloat() else 0f
            } else {
                if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                    .background(
                        Color.White.copy(alpha = if (isSeeking) 0.9f else 1f),
                        MaterialTheme.shapes.small,
                    ),
            )

            // Seek indicator when seeking
            if (isSeeking && duration > 0) {
                val seekProgress = (seekPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(seekProgress)
                        .padding(end = 2.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 16.dp)
                            .background(Color.White, RoundedCornerShape(2.dp)),
                    )
                }
            }

            // Focus indicator
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.White, MaterialTheme.shapes.small),
                )
            }
        }
    }
}

/**
 * Seek preview thumbnail with time label.
 */
@Composable
private fun SeekPreview(
    tileUrl: String,
    offsetX: Int,
    offsetY: Int,
    thumbnailWidth: Int,
    thumbnailHeight: Int,
    timeLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Calculate horizontal offset to position preview at seek location
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val previewWidthDp = 160.dp
        val previewWidthPx = with(LocalDensity.current) { previewWidthDp.toPx() }

        // Calculate offset: center preview at progress position, but clamp to screen edges
        val targetX = containerWidth * progress - previewWidthPx / 2
        val clampedX = targetX.coerceIn(0f, containerWidth - previewWidthPx)
        val offsetDp = with(LocalDensity.current) { clampedX.toDp() }

        Column(
            modifier = Modifier
                .offset(x = offsetDp)
                .width(previewWidthDp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(previewWidthDp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(tileUrl)
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
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time label
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Time-only preview when trickplay is not available.
 */
@Composable
private fun TimeOnlyPreview(
    timeLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val previewWidthDp = 80.dp
        val previewWidthPx = with(LocalDensity.current) { previewWidthDp.toPx() }

        val targetX = containerWidth * progress - previewWidthPx / 2
        val clampedX = targetX.coerceIn(0f, containerWidth - previewWidthPx)
        val offsetDp = with(LocalDensity.current) { clampedX.toDp() }

        Box(
            modifier = Modifier
                .offset(x = offsetDp)
                .width(previewWidthDp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.9f))
                .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}
