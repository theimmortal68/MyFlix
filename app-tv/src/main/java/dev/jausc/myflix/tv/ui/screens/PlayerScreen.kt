@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
    "UnusedPrivateMember",
)

package dev.jausc.myflix.tv.ui.screens

import android.app.Activity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ClosedCaptionOff
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import dev.jausc.myflix.core.player.audio.DelayAudioProcessor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import androidx.tv.material3.Border
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import dev.jausc.myflix.core.common.model.JellyfinChapter
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.MediaSegmentType
import dev.jausc.myflix.core.common.model.MediaStream
import dev.jausc.myflix.core.common.model.audioLabel
import dev.jausc.myflix.core.common.model.subtitleLabel
import dev.jausc.myflix.core.common.model.audioCodecLabel
import dev.jausc.myflix.core.common.model.videoCodecLabel
import dev.jausc.myflix.core.common.model.videoQualityLabel
import dev.jausc.myflix.core.common.model.videoRangeLabel
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.common.preferences.PlaybackOptions
import dev.jausc.myflix.core.common.ui.ActionColors
import dev.jausc.myflix.core.common.ui.util.SubsetTransformation
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.network.websocket.GeneralCommandType
import dev.jausc.myflix.core.network.websocket.PlaystateCommandType
import dev.jausc.myflix.core.network.websocket.WebSocketEvent
import dev.jausc.myflix.core.player.MediaInfo
import dev.jausc.myflix.core.player.PlayerBackend
import dev.jausc.myflix.core.player.PlayerConstants
import dev.jausc.myflix.core.player.AudioPassthroughMode
import dev.jausc.myflix.core.player.PlayerController
import dev.jausc.myflix.core.player.PlayerDisplayMode
import dev.jausc.myflix.core.player.PlayerUtils
import dev.jausc.myflix.core.player.SubtitleColor
import dev.jausc.myflix.core.player.SubtitleFontSize
import dev.jausc.myflix.core.player.SubtitleStyle
import dev.jausc.myflix.core.player.TrickplayProvider
import dev.jausc.myflix.core.viewmodel.PlayerViewModel
import dev.jausc.myflix.tv.ui.components.AutoPlayCountdown
import dev.jausc.myflix.tv.ui.components.MenuAnchor
import dev.jausc.myflix.tv.ui.components.MenuAnchorAlignment
import dev.jausc.myflix.tv.ui.components.MenuAnchorPlacement
import dev.jausc.myflix.tv.ui.components.PlayerSlideOutMenu
import dev.jausc.myflix.tv.ui.components.PlayerSlideOutMenuSectioned
import dev.jausc.myflix.tv.ui.components.SlideOutMenuItem
import dev.jausc.myflix.tv.ui.components.SlideOutMenuSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import androidx.compose.ui.geometry.Size as ComposeSize

/**
 * Types of slide-out menus in the player overlay.
 */
private sealed class PlayerMenuType {
    data object Audio : PlayerMenuType()
    data object Subtitles : PlayerMenuType()
    data object SubtitleStyle : PlayerMenuType()
    data object Speed : PlayerMenuType()
    data object DisplayMode : PlayerMenuType()
    data object Quality : PlayerMenuType()
}

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

    // Get audio passthrough mode preference
    val audioPassthroughModeName by appPreferences.audioPassthroughMode.collectAsState()
    val audioPassthroughMode = remember(audioPassthroughModeName) {
        try {
            AudioPassthroughMode.valueOf(audioPassthroughModeName)
        } catch (e: IllegalArgumentException) {
            AudioPassthroughMode.OFF
        }
    }

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

    // Player controller from core module - pass MPV preference and audio passthrough mode
    val playerController = remember(audioPassthroughMode) {
        PlayerController(
            context = context,
            useMpv = useMpvPlayer,
            audioPassthroughMode = audioPassthroughMode,
        )
    }

    // Collect player state
    val playbackState by playerController.state.collectAsState()
    var displayMode by remember(displayModeName) {
        mutableStateOf(
            try {
                PlayerDisplayMode.valueOf(displayModeName)
            } catch (
                e: IllegalArgumentException
            ) {
                PlayerDisplayMode.FIT
            }
        )
    }

    // Current bitrate - can be changed during playback (0 = no limit / direct play)
    var currentBitrate by remember { mutableIntStateOf(maxStreamingBitrate) }
    var didFallbackToMpv by remember { mutableStateOf(false) }

    // Audio delay adjustment state
    var audioDelayMs by remember { mutableLongStateOf(0L) }

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
                        PlaystateCommandType.PlayPause -> {
                            playerController.togglePause()
                        }
                        PlaystateCommandType.Pause -> {
                            playerController.pause()
                        }
                        PlaystateCommandType.Unpause -> {
                            playerController.resume()
                        }
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
                        PlaystateCommandType.Rewind -> {
                            playerController.seekRelative(-skipBackwardMs)
                        }
                        PlaystateCommandType.FastForward -> {
                            playerController.seekRelative(skipForwardMs)
                        }
                        PlaystateCommandType.NextTrack -> {
                            viewModel.playNextNow()
                        }
                        PlaystateCommandType.PreviousTrack -> {
                            // Seek to beginning or previous if at start
                            if (playerController.state.value.position > 5000) {
                                playerController.seekTo(0)
                            }
                        }
                    }
                }
                is WebSocketEvent.GeneralCommand -> {
                    android.util.Log.d("PlayerScreen", "Remote general command: ${event.name}")
                    when (event.name) {
                        GeneralCommandType.SetAudioStreamIndex -> {
                            event.arguments["Index"]?.toIntOrNull()?.let { index ->
                                viewModel.setAudioStreamIndex(index)
                                viewModel.updatePlaybackOptions(
                                    audioStreamIndex = index,
                                    subtitleStreamIndex = state.selectedSubtitleStreamIndex,
                                    maxBitrateMbps = currentBitrate,
                                    startPositionMs = playbackState.position,
                                )
                            }
                        }
                        GeneralCommandType.SetSubtitleStreamIndex -> {
                            val index = event.arguments["Index"]?.toIntOrNull()
                            // Index of -1 means disable subtitles
                            val subtitleIndex = if (index == -1) null else index
                            viewModel.setSubtitleStreamIndex(subtitleIndex)
                            viewModel.updatePlaybackOptions(
                                audioStreamIndex = state.selectedAudioStreamIndex,
                                subtitleStreamIndex = subtitleIndex,
                                maxBitrateMbps = currentBitrate,
                                startPositionMs = playbackState.position,
                            )
                        }
                        GeneralCommandType.SetMaxStreamingBitrate -> {
                            event.arguments["MaxBitrate"]?.toLongOrNull()?.let { bitrate ->
                                // Server sends bitrate in bits/sec, we use Mbps
                                val bitrateMbps = (bitrate / 1_000_000).toInt()
                                currentBitrate = bitrateMbps
                                viewModel.updatePlaybackOptions(
                                    audioStreamIndex = state.selectedAudioStreamIndex,
                                    subtitleStreamIndex = state.selectedSubtitleStreamIndex,
                                    maxBitrateMbps = bitrateMbps,
                                    startPositionMs = playbackState.position,
                                )
                            }
                        }
                        GeneralCommandType.ToggleOsd -> {
                            viewModel.toggleControls()
                        }
                        GeneralCommandType.ToggleMute -> {
                            // Note: Volume is typically handled by TV system, but we can track state
                            android.util.Log.d("PlayerScreen", "ToggleMute requested (handled by system)")
                        }
                        else -> {
                            // Other general commands handled by MainActivity
                        }
                    }
                }
                else -> {
                    // Other events handled by MainActivity
                }
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
                        if (event.key == Key.Back) {
                            if (state.showAutoPlayCountdown) {
                                viewModel.cancelQueue()
                            } else {
                                viewModel.hideControls()
                            }
                            true
                        } else {
                            false
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
                        android.util.Log.d("PlayerScreen", "onDisplayModeChanged: $mode (was $displayMode)")
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
                    audioDelayMs = audioDelayMs,
                    onAudioDelayChanged = { delayMs ->
                        audioDelayMs = delayMs
                        playerController.setAudioDelayMs(delayMs)
                    },
                    onUserInteraction = { viewModel.resetControlsHideTimer() },
                    onMenuOpenChanged = { isOpen ->
                        if (isOpen) {
                            viewModel.cancelControlsHideTimer()
                        } else {
                            viewModel.resetControlsHideTimer()
                        }
                    },
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
                    transcodeReasons = state.transcodeReasons,
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
            PlayerDisplayMode.FIT -> {
                if (videoAspect > containerAspect) {
                    containerWidth to containerWidth / videoAspect
                } else {
                    containerHeight * videoAspect to containerHeight
                }
            }
            PlayerDisplayMode.FILL -> {
                if (videoAspect > containerAspect) {
                    containerHeight * videoAspect to containerHeight
                } else {
                    containerWidth to containerWidth / videoAspect
                }
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
            PlayerDisplayMode.STRETCH -> {
                containerWidth to containerHeight
            }
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

    // Keep reference to PlayerView for direct updates
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    // Update display mode when it changes - use view scaling for hardcoded letterboxing
    LaunchedEffect(displayMode) {
        android.util.Log.d("PlayerScreen", "LaunchedEffect(displayMode): $displayMode, playerView=$playerView")
        playerView?.let { view ->
            // For hardcoded letterboxing, we need to scale the view to zoom/crop
            // Scale factors: FIT=1.0, FILL=1.33 (for 2.35:1), ZOOM=1.5, STRETCH=different X/Y
            when (displayMode) {
                PlayerDisplayMode.FIT -> {
                    view.scaleX = 1.0f
                    view.scaleY = 1.0f
                }
                PlayerDisplayMode.FILL -> {
                    // Scale to crop typical 2.35:1 letterboxing (scale ~1.33)
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
            android.util.Log.d("PlayerScreen", "Set scale: scaleX=${view.scaleX}, scaleY=${view.scaleY}")
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
    audioDelayMs: Long,
    onAudioDelayChanged: (Long) -> Unit,
    onUserInteraction: () -> Unit,
    onMenuOpenChanged: (Boolean) -> Unit,
    playPauseFocusRequester: FocusRequester,
    trickplayProvider: TrickplayProvider? = null,
    jellyfinClient: JellyfinClient? = null,
    itemId: String? = null,
    skipLabel: String? = null,
    onSkipSegment: (() -> Unit)? = null,
    transcodeReasons: List<String>? = null,
) {
    val videoQuality = item?.videoQualityLabel ?: ""
    val videoCodec = item?.videoCodecLabel ?: ""
    val videoRange = item?.videoRangeLabel ?: ""
    val audioCodec = item?.audioCodecLabel ?: ""
    val playerType = playbackState.playerType
    // Slide-out menu state
    var activeMenu by remember { mutableStateOf<PlayerMenuType?>(null) }
    var menuAnchor by remember { mutableStateOf<MenuAnchor?>(null) }

    // Notify parent when menu opens/closes to control hide timer
    LaunchedEffect(activeMenu) {
        onMenuOpenChanged(activeMenu != null)
    }
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
            .background(Color.Black.copy(alpha = 0.2f))
            .onPreviewKeyEvent { event ->
                // Reset hide timer on any key press while navigating the overlay
                if (event.type == KeyEventType.KeyDown) {
                    onUserInteraction()
                }
                false // Don't consume the event, let it propagate
            },
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
                // Player type badge
                PlayerBadge(
                    text = playerType,
                    backgroundColor = when (playerType) {
                        "MPV" -> Color(0xFF9C27B0)
                        "ExoPlayer" -> Color(0xFF2196F3)
                        else -> Color.Gray
                    },
                )
                // Resolution badge (e.g., "4K", "1080p")
                if (videoQuality.isNotBlank()) {
                    PlayerBadge(
                        text = videoQuality,
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                    )
                }
                // Video codec badge (e.g., "HEVC", "AV1")
                if (videoCodec.isNotBlank()) {
                    PlayerBadge(
                        text = videoCodec,
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                    )
                }
                // Dynamic range badge (e.g., "HDR10", "Dolby Vision")
                if (videoRange.isNotBlank()) {
                    PlayerBadge(
                        text = videoRange,
                        backgroundColor = when {
                            videoRange.contains("Dolby Vision") -> Color(0xFF000000)
                            videoRange.contains("HDR") -> Color(0xFFFFD700)
                            videoRange == "HLG" -> Color(0xFF4CAF50)
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                        textColor = when {
                            videoRange.contains("HDR") -> Color.Black
                            else -> Color.White
                        },
                    )
                }
                // Audio codec badge (e.g., "TrueHD Atmos", "DTS-HD MA 7.1")
                if (audioCodec.isNotBlank()) {
                    PlayerBadge(
                        text = audioCodec,
                        backgroundColor = when {
                            audioCodec.contains("Atmos") -> Color(0xFF1E88E5)
                            audioCodec.contains("DTS") -> Color(0xFFFF5722)
                            audioCodec.contains("TrueHD") -> Color(0xFF9C27B0)
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                    )
                }
            }
            // Show transcode reasons if transcoding is happening
            if (!transcodeReasons.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Transcoding: ${formatTranscodeReasons(transcodeReasons)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB74D), // Amber/orange color to indicate transcoding
                )
            }
        }

        // Playback state for controls
        val chapters = item?.chapters.orEmpty()
        val currentPositionMs = playbackState.position

        // Find previous and next chapter positions
        val previousChapterMs = chapters
            .mapNotNull { it.startPositionTicks?.let { ticks -> PlayerUtils.ticksToMs(ticks) } }
            .filter { it < currentPositionMs - 3000 } // Must be at least 3s before current position
            .maxOrNull()
        val nextChapterMs = chapters
            .mapNotNull { it.startPositionTicks?.let { ticks -> PlayerUtils.ticksToMs(ticks) } }
            .filter { it > currentPositionMs + 1000 } // Must be at least 1s after current position
            .minOrNull()

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
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TvActionButton(
                        label = "Audio",
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        focusRequester = settingsRowFocusRequester,
                        anchorAlignment = MenuAnchorAlignment.BottomStart,
                        onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                        onClickWithPosition = { anchor ->
                            onUserInteraction()
                            menuAnchor = anchor
                            activeMenu = PlayerMenuType.Audio
                        },
                    )
                    TvActionButton(
                        label = "Subtitles",
                        icon = Icons.Outlined.ClosedCaption,
                        anchorAlignment = MenuAnchorAlignment.BottomStart,
                        onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                        onClickWithPosition = { anchor ->
                            onUserInteraction()
                            menuAnchor = anchor
                            activeMenu = PlayerMenuType.Subtitles
                        },
                    )
                    TvActionButton(
                        label = "Sub Style",
                        icon = Icons.Outlined.FormatSize,
                        anchorAlignment = MenuAnchorAlignment.BottomStart,
                        onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                        onClickWithPosition = { anchor ->
                            onUserInteraction()
                            menuAnchor = anchor
                            activeMenu = PlayerMenuType.SubtitleStyle
                        },
                    )
                }

                // Center group: Playback controls (play/pause, seek, chapters)
                Row(
                    modifier = Modifier
                        .focusGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                if (!showChaptersRow) {
                                    showChaptersRow = true
                                    true // Consume - LaunchedEffect will request focus
                                } else {
                                    false // Let focus system navigate
                                }
                            } else {
                                false
                            }
                        }
                        .focusProperties {
                            down = if (showChaptersRow) chaptersRowFocusRequester else FocusRequester.Cancel
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Previous chapter button
                    if (chapters.isNotEmpty()) {
                        TvPlaybackButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Chapter",
                            enabled = previousChapterMs != null,
                        ) {
                            onUserInteraction()
                            previousChapterMs?.let { onSeekTo(it) }
                        }
                    }

                    TvPlaybackButton(
                        icon = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                    ) {
                        onUserInteraction()
                        onSeekRelative(-skipBackwardMs)
                    }

                    TvPlaybackButton(
                        icon = if (playbackState.isPlaying && !playbackState.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying && !playbackState.isPaused) "Pause" else "Play",
                        isPrimary = true,
                        focusRequester = playPauseFocusRequester,
                    ) {
                        onUserInteraction()
                        onPlayPause()
                    }

                    TvPlaybackButton(
                        icon = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                    ) {
                        onUserInteraction()
                        onSeekRelative(skipForwardMs)
                    }

                    // Next chapter button
                    if (chapters.isNotEmpty()) {
                        TvPlaybackButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Next Chapter",
                            enabled = nextChapterMs != null,
                        ) {
                            onUserInteraction()
                            nextChapterMs?.let { onSeekTo(it) }
                        }
                    }

                    if (onPlayNext != null) {
                        TvPlaybackButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Next Episode",
                        ) {
                            onUserInteraction()
                            onPlayNext()
                        }
                    }
                }

                // Right group: Speed, Display, Quality (+ Skip if available)
                Row(
                    modifier = Modifier.focusGroup(),
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
                        anchorAlignment = MenuAnchorAlignment.BottomEnd,
                        onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                        onClickWithPosition = { anchor ->
                            onUserInteraction()
                            menuAnchor = anchor
                            activeMenu = PlayerMenuType.Speed
                        },
                    )
                    TvActionButton(
                        label = "Display",
                        icon = Icons.Outlined.AspectRatio,
                        anchorAlignment = MenuAnchorAlignment.BottomEnd,
                        onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                        onClickWithPosition = { anchor ->
                            onUserInteraction()
                            menuAnchor = anchor
                            activeMenu = PlayerMenuType.DisplayMode
                        },
                    )
                    TvActionButton(
                        label = "Quality",
                        icon = Icons.Outlined.HighQuality,
                        anchorAlignment = MenuAnchorAlignment.BottomEnd,
                        onDownPressed = {
                            if (!showChaptersRow) {
                                showChaptersRow = true
                                true
                            } else {
                                false
                            }
                        },
                        onClickWithPosition = { anchor ->
                            onUserInteraction()
                            menuAnchor = anchor
                            activeMenu = PlayerMenuType.Quality
                        },
                    )
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
                    onNavigateUp = { showChaptersRow = false },
                )
            }
        }
    }

    // Audio settings popup menu (track selection + audio sync)
    PlayerSlideOutMenuSectioned(
        visible = activeMenu == PlayerMenuType.Audio,
        title = "Audio",
        sections = listOfNotNull(
            // Audio Track section
            SlideOutMenuSection(
                title = "Audio Track",
                items = if (audioStreams.isEmpty()) {
                    listOf(
                        SlideOutMenuItem(
                            text = "No audio tracks",
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                            iconTint = ActionColors.Audio,
                            enabled = false,
                            onClick = {},
                        ),
                    )
                } else {
                    audioStreams.map { stream ->
                        val isSelected = stream.index == selectedAudioIndex
                        SlideOutMenuItem(
                            text = stream.audioLabel(),
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                            iconTint = ActionColors.Audio,
                            selected = isSelected,
                            onClick = {
                                onUserInteraction()
                                onAudioSelected(stream.index)
                            },
                        )
                    }
                },
            ),
            // Audio Sync section
            SlideOutMenuSection(
                title = "Audio Sync",
                items = listOf(
                    SlideOutMenuItem(
                        text = if (audioDelayMs == 0L) "No delay" else "${audioDelayMs}ms",
                        icon = Icons.Outlined.Timer,
                        iconTint = ActionColors.Audio,
                        selected = audioDelayMs == 0L,
                        dismissOnClick = false,
                        onClick = {
                            onUserInteraction()
                            onAudioDelayChanged(0L)
                        },
                    ),
                    SlideOutMenuItem(
                        text = "Delay +${DelayAudioProcessor.DELAY_INCREMENT_MS}ms",
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        iconTint = ActionColors.Audio,
                        enabled = audioDelayMs < DelayAudioProcessor.MAX_DELAY_MS,
                        dismissOnClick = false,
                        onClick = {
                            onUserInteraction()
                            onAudioDelayChanged(
                                (audioDelayMs + DelayAudioProcessor.DELAY_INCREMENT_MS)
                                    .coerceAtMost(DelayAudioProcessor.MAX_DELAY_MS),
                            )
                        },
                    ),
                    SlideOutMenuItem(
                        text = "Delay -${DelayAudioProcessor.DELAY_INCREMENT_MS}ms",
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        iconTint = ActionColors.Audio,
                        enabled = audioDelayMs > DelayAudioProcessor.MIN_DELAY_MS,
                        dismissOnClick = false,
                        onClick = {
                            onUserInteraction()
                            onAudioDelayChanged(
                                (audioDelayMs - DelayAudioProcessor.DELAY_INCREMENT_MS)
                                    .coerceAtLeast(DelayAudioProcessor.MIN_DELAY_MS),
                            )
                        },
                    ),
                ),
            ),
        ),
        onDismiss = { activeMenu = null },
        anchor = menuAnchor,
    )

    // Subtitles popup menu
    PlayerSlideOutMenu(
        visible = activeMenu == PlayerMenuType.Subtitles,
        title = "Subtitles",
        items = if (subtitleStreams.isEmpty()) {
            listOf(
                SlideOutMenuItem(
                    text = "No subtitles available",
                    icon = Icons.Outlined.ClosedCaptionOff,
                    iconTint = ActionColors.Subtitles,
                    enabled = false,
                    onClick = {},
                ),
            )
        } else {
            listOf(
                SlideOutMenuItem(
                    text = "Off",
                    icon = Icons.Outlined.ClosedCaptionOff,
                    iconTint = ActionColors.Subtitles,
                    selected = selectedSubtitleIndex == PlayerConstants.TRACK_DISABLED,
                    onClick = {
                        onUserInteraction()
                        onSubtitleSelected(PlayerConstants.TRACK_DISABLED)
                    },
                ),
            ) + subtitleStreams.map { stream ->
                val isSelected = stream.index == selectedSubtitleIndex
                SlideOutMenuItem(
                    text = stream.subtitleLabel(),
                    icon = Icons.Outlined.ClosedCaption,
                    iconTint = ActionColors.Subtitles,
                    selected = isSelected,
                    onClick = {
                        onUserInteraction()
                        onSubtitleSelected(stream.index)
                    },
                )
            }
        },
        onDismiss = { activeMenu = null },
        anchor = menuAnchor,
    )

    // Subtitle style popup menu (sectioned)
    PlayerSlideOutMenuSectioned(
        visible = activeMenu == PlayerMenuType.SubtitleStyle,
        title = "Subtitle Style",
        sections = listOf(
            SlideOutMenuSection(
                title = "Font Size",
                items = SubtitleFontSize.entries.map { size ->
                    SlideOutMenuItem(
                        text = size.label,
                        icon = Icons.Outlined.FormatSize,
                        iconTint = ActionColors.Subtitles,
                        selected = subtitleStyle.fontSize == size,
                        onClick = {
                            onUserInteraction()
                            onSubtitleStyleChanged(subtitleStyle.copy(fontSize = size))
                        },
                    )
                },
            ),
            SlideOutMenuSection(
                title = "Font Color",
                items = SubtitleColor.entries.map { color ->
                    SlideOutMenuItem(
                        text = color.label,
                        icon = Icons.Outlined.ClosedCaption,
                        iconTint = Color(color.argb),
                        selected = subtitleStyle.fontColor == color,
                        onClick = {
                            onUserInteraction()
                            onSubtitleStyleChanged(subtitleStyle.copy(fontColor = color))
                        },
                    )
                },
            ),
            SlideOutMenuSection(
                title = "Background",
                items = listOf(0, 25, 50, 75, 100).map { opacity ->
                    SlideOutMenuItem(
                        text = "$opacity%",
                        icon = Icons.Outlined.FormatSize,
                        iconTint = ActionColors.Subtitles,
                        selected = subtitleStyle.backgroundOpacity == opacity,
                        onClick = {
                            onUserInteraction()
                            onSubtitleStyleChanged(subtitleStyle.copy(backgroundOpacity = opacity))
                        },
                    )
                },
            ),
        ),
        onDismiss = { activeMenu = null },
        anchor = menuAnchor,
    )

    // Playback speed popup menu
    PlayerSlideOutMenu(
        visible = activeMenu == PlayerMenuType.Speed,
        title = "Playback Speed",
        items = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).map { speed ->
            SlideOutMenuItem(
                text = "x${"%.2f".format(speed)}",
                icon = Icons.Outlined.Speed,
                iconTint = ActionColors.Play,
                selected = playbackState.speed == speed,
                onClick = {
                    onUserInteraction()
                    onSpeedChanged(speed)
                },
            )
        },
        onDismiss = { activeMenu = null },
        anchor = menuAnchor,
    )

    // Display mode popup menu
    PlayerSlideOutMenu(
        visible = activeMenu == PlayerMenuType.DisplayMode,
        title = "Display Mode",
        items = PlayerDisplayMode.entries.map { mode ->
            SlideOutMenuItem(
                text = mode.label,
                icon = Icons.Outlined.AspectRatio,
                iconTint = ActionColors.GoTo,
                selected = displayMode == mode,
                onClick = {
                    onUserInteraction()
                    onDisplayModeChanged(mode)
                },
            )
        },
        onDismiss = { activeMenu = null },
        anchor = menuAnchor,
    )

    // Streaming quality popup menu
    PlayerSlideOutMenu(
        visible = activeMenu == PlayerMenuType.Quality,
        title = "Streaming Quality",
        items = PlaybackOptions.BITRATE_OPTIONS.map { (bitrate, label) ->
            SlideOutMenuItem(
                text = label,
                icon = Icons.Outlined.HighQuality,
                iconTint = ActionColors.Audio,
                selected = bitrate == currentBitrate,
                onClick = {
                    onUserInteraction()
                    onBitrateChanged(bitrate)
                },
            )
        },
        onDismiss = { activeMenu = null },
        anchor = menuAnchor,
    )
}

@Composable
private fun TvIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Surface(
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.6f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
        ),
        onClick = { if (enabled) onClick() },
        modifier = Modifier.size(36.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Round playback control button (play/pause, seek, chapter skip).
 */
@Composable
private fun TvPlaybackButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val alpha = if (enabled) 1f else 0.4f

    // Animation for the glow effect
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "glowAlpha",
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "buttonScale",
    )

    val buttonSize = if (isPrimary) 40.dp else 36.dp
    val iconSize = if (isPrimary) 24.dp else 20.dp
    val backgroundColor = if (isPrimary) Color.White else Color.Black.copy(alpha = 0.6f)
    val iconColor = if (isPrimary) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(buttonSize)
            .scale(buttonScale)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    if (enabled) onClick()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Glow effect
        if (!isPrimary) {
            Box(
                modifier = Modifier
                    .size(buttonSize - 8.dp)
                    .alpha(glowAlpha)
                    .blur(6.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }

        // Button background
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor.copy(alpha = alpha),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun TvActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    focusRequester: FocusRequester? = null,
    anchorAlignment: MenuAnchorAlignment = MenuAnchorAlignment.BottomEnd,
    onDownPressed: (() -> Boolean)? = null,
    onClickWithPosition: ((MenuAnchor) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var buttonPosition by remember { mutableStateOf<MenuAnchor?>(null) }

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
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                with(density) {
                    val anchorX = if (anchorAlignment == MenuAnchorAlignment.BottomStart) {
                        position.x.toDp()
                    } else {
                        (position.x + size.width).toDp()
                    }
                    buttonPosition = MenuAnchor(
                        x = anchorX,
                        y = position.y.toDp(),
                        alignment = anchorAlignment,
                        placement = MenuAnchorPlacement.Above,
                    )
                }
            }
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                        onDownPressed?.invoke() == true
                    }
                    // Handle click on KeyUp to avoid double-dispatch on real TV hardware
                    // (KeyDown can cause the event to propagate to newly focused menu items)
                    event.type == KeyEventType.KeyUp &&
                        (event.key == Key.Enter || event.key == Key.DirectionCenter) -> {
                        buttonPosition?.let { onClickWithPosition?.invoke(it) }
                        onClick?.invoke()
                        true
                    }
                    else -> false
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
    onNavigateUp: () -> Unit,
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
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                            onNavigateUp()
                        }
                        false // Don't consume - let focus system handle navigation
                    }
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
            // Use inline null checks for smart casting
            if (jellyfinClient != null && itemId != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(jellyfinClient.getChapterImageUrl(itemId, chapterIndex, maxWidth = 360))
                        .build(),
                    contentDescription = chapter.name ?: "Chapter",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (trickplayProvider != null && jellyfinClient != null && itemId != null) {
                val tileIndex = trickplayProvider.getTileImageIndex(startMs)
                val (offsetX, offsetY) = trickplayProvider.getTileOffset(startMs)
                val thumbnailWidth = trickplayProvider.thumbnailWidth
                val thumbnailHeight = trickplayProvider.thumbnailHeight
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
 * Format transcode reasons into a human-readable string.
 * Jellyfin returns machine-readable reason codes that we convert to user-friendly text.
 */
private fun formatTranscodeReasons(reasons: List<String>): String {
    return reasons.map { reason ->
        when (reason) {
            "ContainerNotSupported" -> "Container"
            "VideoCodecNotSupported" -> "Video codec"
            "AudioCodecNotSupported" -> "Audio codec"
            "ContainerBitrateExceedsLimit" -> "Bitrate limit"
            "VideoBitrateNotSupported" -> "Video bitrate"
            "AudioBitrateNotSupported" -> "Audio bitrate"
            "VideoProfileNotSupported" -> "Video profile"
            "VideoLevelNotSupported" -> "Video level"
            "RefFramesNotSupported" -> "Ref frames"
            "AudioChannelsNotSupported" -> "Audio channels"
            "SecondaryAudioNotSupported" -> "Secondary audio"
            "SubtitleCodecNotSupported" -> "Subtitle codec"
            "DirectPlayError" -> "Direct play error"
            "VideoRangeTypeNotSupported" -> "HDR type"
            "AudioProfileNotSupported" -> "Audio profile"
            "AudioSampleRateNotSupported" -> "Sample rate"
            "AudioBitDepthNotSupported" -> "Audio bit depth"
            "UnknownVideoStreamInfo" -> "Unknown video"
            "UnknownAudioStreamInfo" -> "Unknown audio"
            else -> reason.replace(Regex("([A-Z])"), " $1").trim().lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }.joinToString(", ")
}

/**
 * Skip segment button for intro/outro skipping.
 * Appears in the bottom-right corner when a skippable segment is detected.
 */
@Composable
private fun SkipSegmentButton(label: String, onSkip: () -> Unit, modifier: Modifier = Modifier,) {
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
                tileWidth = trickplayProvider.tileWidth,
                tileHeight = trickplayProvider.tileHeight,
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
                            else -> {
                                false
                            }
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
/**
 * Seek preview thumbnail with time label.
 * Uses Canvas with translate/scale to extract the correct tile from the grid.
 */
@Composable
private fun SeekPreview(
    tileUrl: String,
    offsetX: Int,
    offsetY: Int,
    thumbnailWidth: Int,
    thumbnailHeight: Int,
    tileWidth: Int,
    tileHeight: Int,
    timeLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Calculate display dimensions maintaining aspect ratio
    val displayHeight = 90.dp
    val displayWidth = displayHeight * (thumbnailWidth.toFloat() / thumbnailHeight)

    // Calculate scale factor to fit thumbnail into display size
    val scale = with(LocalDensity.current) { displayWidth.toPx() / thumbnailWidth }

    // Load full tile image at original size
    val imageRequest = remember(tileUrl) {
        ImageRequest.Builder(context)
            .data(tileUrl)
            .size(coil3.size.Size.ORIGINAL)
            .build()
    }
    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        contentScale = ContentScale.None,
    )

    // Calculate tile position within grid
    val tileX = offsetX / thumbnailWidth
    val tileY = offsetY / thumbnailHeight

    // Calculate horizontal offset to position preview at seek location
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val previewWidthPx = with(LocalDensity.current) { displayWidth.toPx() }

        // Calculate offset: center preview at progress position, but clamp to screen edges
        val targetX = containerWidth * progress - previewWidthPx / 2
        val clampedX = targetX.coerceIn(0f, containerWidth - previewWidthPx)
        val offsetDp = with(LocalDensity.current) { clampedX.toDp() }

        Column(
            modifier = Modifier
                .offset(x = offsetDp)
                .width(displayWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Thumbnail using Canvas
            Box(
                modifier = Modifier
                    .width(displayWidth)
                    .height(displayHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .width(displayWidth)
                        .height(displayHeight)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    with(painter) {
                        // Scale and translate to the correct position in the tile grid
                        scale(scale, scale, pivot = Offset.Zero) {
                            translate(
                                left = -tileX.toFloat() * thumbnailWidth,
                                top = -tileY.toFloat() * thumbnailHeight,
                            ) {
                                draw(
                                    size = ComposeSize(
                                        width = thumbnailWidth * tileWidth.toFloat(),
                                        height = thumbnailHeight * tileHeight.toFloat(),
                                    ),
                                )
                            }
                        }
                    }
                }
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
private fun TimeOnlyPreview(timeLabel: String, progress: Float, modifier: Modifier = Modifier,) {
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
