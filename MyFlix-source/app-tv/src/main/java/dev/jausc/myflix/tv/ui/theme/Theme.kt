package dev.jausc.myflix.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = TvColors.BluePrimary,
    onPrimary = TvColors.TextPrimary,
    secondary = TvColors.BlueLight,
    onSecondary = TvColors.TextPrimary,
    background = TvColors.Background,
    onBackground = TvColors.TextPrimary,
    surface = TvColors.Surface,
    onSurface = TvColors.TextPrimary,
    surfaceVariant = TvColors.SurfaceLight,
    onSurfaceVariant = TvColors.TextSecondary,
    error = TvColors.Error,
    onError = TvColors.TextPrimary
)

@Composable
fun MyFlixTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
