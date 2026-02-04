@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

private const val TRAILER_START_DELAY_MS = 10000L
private const val CROSSFADE_DURATION_MS = 800

/**
 * State for trailer backdrop behavior.
 */
data class TrailerBackdropState(
    /** The stream URL for the trailer (null if no trailer available) */
    val trailerUrl: String? = null,
    /** Whether trailers are enabled in settings */
    val trailersEnabled: Boolean = true,
    /** Whether to show the unmute button */
    val showUnmuteButton: Boolean = true,
)

/**
 * Backdrop that can show either a static Ken Burns animated image or an inline trailer video.
 *
 * Behavior:
 * 1. Initially shows Ken Burns animated backdrop
 * 2. After 3 seconds, if trailer URL is available and trailers are enabled, fades to trailer
 * 3. Trailer plays muted by default with unmute button in corner
 * 4. Falls back to static backdrop if trailer fails or is unavailable
 *
 * @param backdropUrl The URL for the static backdrop image
 * @param state The trailer state containing URL and settings
 * @param modifier Modifier for positioning
 * @param fadePreset The Ken Burns fade preset to use
 * @param unmuteFocusRequester Optional focus requester for the unmute button
 */
@Composable
fun TrailerBackdrop(
    backdropUrl: String?,
    state: TrailerBackdropState,
    modifier: Modifier = Modifier,
    fadePreset: KenBurnsFadePreset = KenBurnsFadePreset.MOVIE,
    unmuteFocusRequester: FocusRequester? = null,
) {
    // Track whether we should start playing the trailer
    var shouldPlayTrailer by remember { mutableStateOf(false) }

    // Track whether trailer is actually playing (after first frame rendered)
    var isTrailerPlaying by remember { mutableStateOf(false) }

    // Track muted state
    var isMuted by remember { mutableStateOf(true) }

    // Track if trailer failed
    var trailerFailed by remember { mutableStateOf(false) }

    // Determine if trailer should be shown
    val canShowTrailer = state.trailerUrl != null &&
        state.trailersEnabled &&
        !trailerFailed

    // Start playing trailer after delay
    LaunchedEffect(state.trailerUrl, state.trailersEnabled) {
        if (canShowTrailer) {
            delay(TRAILER_START_DELAY_MS)
            shouldPlayTrailer = true
        } else {
            shouldPlayTrailer = false
            isTrailerPlaying = false
        }
    }

    // Reset state when trailer URL changes
    LaunchedEffect(state.trailerUrl) {
        trailerFailed = false
        isTrailerPlaying = false
        shouldPlayTrailer = false
        isMuted = true
    }

    // Animated alpha for crossfade
    val trailerAlpha by animateFloatAsState(
        targetValue = if (isTrailerPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = CROSSFADE_DURATION_MS),
        label = "trailerAlpha",
    )

    val backdropAlpha = 1f - trailerAlpha

    Box(modifier = modifier.fillMaxSize()) {
        // Ken Burns backdrop (always present, fades out when trailer plays)
        if (backdropAlpha > 0f) {
            KenBurnsBackdrop(
                imageUrl = backdropUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdropAlpha },
                fadePreset = fadePreset,
            )
        }

        // Inline trailer player (fades in when playing) - full size matching backdrop
        // SurfaceView bypasses Compose drawing, so we use gradient overlays instead of DstIn masking
        if (canShowTrailer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = trailerAlpha },
            ) {
                // Video player layer
                InlineTrailerPlayer(
                    state = InlineTrailerState(
                        streamUrl = state.trailerUrl,
                        shouldPlay = shouldPlayTrailer,
                        isMuted = isMuted,
                    ),
                    alpha = 1f,
                    modifier = Modifier.fillMaxSize(),
                    onPlaybackStarted = {
                        isTrailerPlaying = true
                    },
                    onPlaybackEnded = {
                        isTrailerPlaying = false
                        shouldPlayTrailer = false
                    },
                    onError = {
                        trailerFailed = true
                        isTrailerPlaying = false
                        shouldPlayTrailer = false
                    },
                )

                // Left edge fade overlay (gradient from background to transparent)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to TvColors.Background,
                                    0.12f to TvColors.Background.copy(alpha = 0.5f),
                                    0.28f to Color.Transparent,
                                    1.0f to Color.Transparent,
                                ),
                            ),
                        ),
                )

                // Bottom edge fade overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    1.0f to TvColors.Background,
                                ),
                            ),
                        ),
                )
            }
        }

        // Unmute button (bottom-right corner, only when trailer is playing)
        if (state.showUnmuteButton && isTrailerPlaying) {
            UnmuteButton(
                isMuted = isMuted,
                onToggleMute = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .then(
                        if (unmuteFocusRequester != null) {
                            Modifier.focusRequester(unmuteFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

/**
 * Unmute/mute toggle button for trailer audio.
 */
@Composable
private fun UnmuteButton(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onToggleMute,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f)),
    ) {
        Icon(
            imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            contentDescription = if (isMuted) "Unmute" else "Mute",
            tint = TvColors.TextPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}
