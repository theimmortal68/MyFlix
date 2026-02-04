package dev.jausc.myflix.tv.dream

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.dream.components.ZoomBox
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.util.Locale
import java.util.concurrent.TimeUnit
import dev.jausc.myflix.core.common.R as CommonR

private const val CROSSFADE_DURATION_MS = 1000

/**
 * Main composable for the Dream Service screensaver.
 *
 * Displays rotating media backdrops with Ken Burns zoom effect,
 * metadata overlay, and a clock.
 */
@Composable
fun DreamScreen(viewModel: DreamViewModel) {
    val content by viewModel.content.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        AnimatedContent(
            targetState = content,
            transitionSpec = {
                fadeIn(animationSpec = tween(CROSSFADE_DURATION_MS)) togetherWith
                    fadeOut(animationSpec = tween(CROSSFADE_DURATION_MS))
            },
            label = "dreamContent",
        ) { currentContent ->
            when (currentContent) {
                is DreamContent.Logo -> LogoScreen(currentContent.message)
                is DreamContent.LibraryShowcase -> ShowcaseScreen(currentContent)
                is DreamContent.NowPlaying -> NowPlayingScreen(currentContent)
                is DreamContent.Error -> ErrorScreen(currentContent.message)
            }
        }

        // Clock in top-right corner
        Text(
            text = currentTime,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
            ),
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp),
        )

        // MyFlix branding in bottom-right corner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
        ) {
            Image(
                painter = painterResource(id = CommonR.drawable.myflix_logo),
                contentDescription = "MyFlix",
                modifier = Modifier.height(24.dp),
            )
        }
    }
}

@Composable
private fun LogoScreen(message: String?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(id = CommonR.drawable.myflix_logo),
                contentDescription = "MyFlix",
                modifier = Modifier.height(80.dp),
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ShowcaseScreen(content: DreamContent.LibraryShowcase) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop with Ken Burns zoom effect
        ZoomBox(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = content.backdrop.asImageBitmap(),
                contentDescription = content.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Vignette overlay for cinematic effect
        VignetteOverlay()

        // Bottom gradient for text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f),
                        ),
                    ),
                ),
        )

        // Metadata overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(48.dp)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Logo or title
            if (content.logo != null) {
                Image(
                    bitmap = content.logo.asImageBitmap(),
                    contentDescription = content.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(80.dp)
                        .widthIn(max = 400.dp),
                )
            } else {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            }

            // Metadata row (year, rating, genres)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Year
                content.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }

                // Rating with star
                content.rating?.let { rating ->
                    StarRating(rating)
                }

                // Genres
                if (content.genres.isNotEmpty()) {
                    Text(
                        text = content.genres.joinToString(" \u2022 "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Now Playing screen - shows what's currently playing.
 * Displays poster, title, subtitle, progress bar, and time remaining.
 */
@Composable
private fun NowPlayingScreen(content: DreamContent.NowPlaying) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background - use backdrop if available, otherwise dark background
        if (content.backdropBitmap != null) {
            ZoomBox(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = content.backdropBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            VignetteOverlay()
        }

        // Dark overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
        )

        // Now Playing content
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(48.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Poster thumbnail
            if (content.posterBitmap != null) {
                Image(
                    bitmap = content.posterBitmap.asImageBitmap(),
                    contentDescription = content.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 120.dp, height = 180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }

            // Title, subtitle, and progress
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // "Now Playing" label with play/pause indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (content.isPaused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (content.isPaused) "Paused" else "Playing",
                        tint = TvColors.BluePrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (content.isPaused) "Paused" else "Now Playing",
                        style = MaterialTheme.typography.labelLarge,
                        color = TvColors.BluePrimary,
                    )
                }

                // Title
                Text(
                    text = content.displayTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    maxLines = 2,
                )

                // Subtitle (series name for episodes, year for movies)
                content.displaySubtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom progress bar (TV Material3 doesn't have LinearProgressIndicator)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(content.progress)
                            .fillMaxHeight()
                            .background(TvColors.BluePrimary),
                    )
                }

                // Time info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(content.positionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                    )
                    Text(
                        text = "-${formatDuration(content.remainingMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Format milliseconds to "H:MM:SS" or "MM:SS" format.
 */
private fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

@Composable
private fun StarRating(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFFFFD700), // Gold color
        )
        Text(
            text = String.format(Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun VignetteOverlay() {
    // Create a vignette effect by layering radial gradients
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.3f),
                    ),
                    radius = 1500f,
                ),
            ),
    )
}

@Composable
private fun ErrorScreen(message: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(id = CommonR.drawable.myflix_logo),
                contentDescription = "MyFlix",
                modifier = Modifier.height(60.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TvColors.Error,
            )
        }
    }
}
