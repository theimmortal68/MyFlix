package dev.jausc.myflix.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.jausc.myflix.core.common.ui.MyFlixFonts
import dev.jausc.myflix.core.common.ui.theme.ThemeColorSchemes
import dev.jausc.myflix.core.common.ui.theme.ThemeColors
import dev.jausc.myflix.core.common.ui.theme.ThemePreset

/**
 * CompositionLocal for accessing the current theme colors directly.
 * This provides access to MyFlix-specific colors not mapped to Material Theme.
 */
val LocalMyFlixColors = staticCompositionLocalOf { ThemeColorSchemes.Default }

/**
 * Create a Mobile Material color scheme from our theme colors.
 */
private fun createColorScheme(colors: ThemeColors) = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.textPrimary,
    primaryContainer = colors.primaryVariant,
    secondary = colors.primaryLight,
    onSecondary = colors.textPrimary,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surfaceLight,
    onSurfaceVariant = colors.textSecondary,
    error = colors.error,
    onError = colors.textPrimary,
)

private val MyFlixTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MyFlixFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/**
 * MyFlix Mobile theme with support for theme presets.
 *
 * @param themePreset The theme preset to use (DEFAULT, OLED_DARK, HIGH_CONTRAST)
 * @param content The composable content to theme
 */
@Composable
fun MyFlixMobileTheme(
    themePreset: ThemePreset = ThemePreset.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colors = ThemeColorSchemes.forPreset(themePreset)
    val colorScheme = createColorScheme(colors)

    CompositionLocalProvider(LocalMyFlixColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MyFlixTypography,
            content = content,
        )
    }
}
