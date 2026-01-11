@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.tv.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Holds extracted colors from an image's palette.
 * All colors have fallbacks to the app's default theme colors.
 */
data class DynamicColors(
    val dominantColor: Color = TvColors.Background,
    val vibrantColor: Color = TvColors.BluePrimary,
    val mutedColor: Color = TvColors.Surface,
    val darkVibrantColor: Color = TvColors.BlueDark,
    val darkMutedColor: Color = TvColors.Background,
    val lightVibrantColor: Color = TvColors.BlueAccent,
    val lightMutedColor: Color = TvColors.SurfaceLight,
    val isExtracted: Boolean = false,
) {
    companion object {
        val Default = DynamicColors()
    }

    /**
     * Returns a darkened version of the dominant color suitable for backgrounds.
     * Ensures readability by maintaining a minimum darkness level.
     */
    val backgroundTint: Color
        get() = if (isExtracted) {
            darkenColor(dominantColor, 0.7f)
        } else {
            TvColors.Background
        }

    /**
     * Returns an accent color derived from the palette for buttons/highlights.
     */
    val accentColor: Color
        get() = if (isExtracted) {
            vibrantColor.takeIf { it != TvColors.BluePrimary } ?: lightVibrantColor
        } else {
            TvColors.BluePrimary
        }

    /**
     * Returns a surface color that's lighter than the background tint.
     */
    val surfaceTint: Color
        get() = if (isExtracted) {
            darkenColor(mutedColor, 0.5f)
        } else {
            TvColors.Surface
        }
}

/**
 * Colors optimized for gradient backgrounds, extracted from backdrop images.
 *
 * Layout of gradients:
 * ```
 * ┌────────────────────────────────────┐
 * │ Secondary            Tertiary      │
 * │ (cool tones)         (vibrant)     │
 * │                                    │
 * │                                    │
 * │ Base Color           Primary       │
 * │ (dark anchor)        (dark rich)   │
 * └────────────────────────────────────┘
 * ```
 */
data class GradientColors(
    /** Bottom-Right: darkVibrant -> darkMuted (deep anchor color) */
    val primary: Color = Color.Unspecified,
    /** Top-Left: Smart selection preferring cool tones (accent color) */
    val secondary: Color = Color.Unspecified,
    /** Top-Right: vibrant -> lightVibrant (under backdrop image) */
    val tertiary: Color = Color.Unspecified,
    /** Whether colors were successfully extracted */
    val isExtracted: Boolean = false,
) {
    companion object {
        val Default = GradientColors()
    }

    /** True if any color was successfully extracted */
    val hasColors: Boolean
        get() = primary != Color.Unspecified ||
            secondary != Color.Unspecified ||
            tertiary != Color.Unspecified
}

// Cache for extracted gradient colors to avoid re-extracting for the same images
private val gradientColorCache = LruCache<String, GradientColors>(50)

/**
 * Darkens a color by the specified factor (0.0 = black, 1.0 = original).
 */
private fun darkenColor(color: Color, factor: Float): Color {
    return Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = color.alpha,
    )
}

/**
 * Extracts colors from an image URL using Android's Palette API.
 *
 * @param imageUrl URL of the image to extract colors from
 * @return DynamicColors with extracted palette or defaults if extraction fails
 */
suspend fun extractColorsFromUrl(context: Context, imageUrl: String?): DynamicColors {
    if (imageUrl.isNullOrBlank()) {
        return DynamicColors.Default
    }

    return withContext(Dispatchers.IO) {
        try {
            val loader = context.imageLoader // Use Coil singleton
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()

            val result = loader.execute(request)

            if (result is SuccessResult) {
                // Coil 3: Convert Image -> Drawable -> Bitmap
                val drawable = result.image.asDrawable(context.resources)
                val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
                extractColorsFromBitmap(bitmap)
            } else {
                DynamicColors.Default
            }
        } catch (_: Exception) {
            DynamicColors.Default
        }
    }
}

/**
 * Extracts colors from a Bitmap using Android's Palette API.
 */
private fun extractColorsFromBitmap(bitmap: Bitmap): DynamicColors {
    val palette = Palette.from(bitmap)
        .maximumColorCount(16)
        .generate()

    return DynamicColors(
        dominantColor = palette.getDominantColor(TvColors.Background.toArgb()).toComposeColor(),
        vibrantColor = palette.getVibrantColor(TvColors.BluePrimary.toArgb()).toComposeColor(),
        mutedColor = palette.getMutedColor(TvColors.Surface.toArgb()).toComposeColor(),
        darkVibrantColor = palette.getDarkVibrantColor(TvColors.BlueDark.toArgb()).toComposeColor(),
        darkMutedColor = palette.getDarkMutedColor(TvColors.Background.toArgb()).toComposeColor(),
        lightVibrantColor = palette.getLightVibrantColor(TvColors.BlueAccent.toArgb()).toComposeColor(),
        lightMutedColor = palette.getLightMutedColor(TvColors.SurfaceLight.toArgb()).toComposeColor(),
        isExtracted = true,
    )
}

/**
 * Converts Compose Color to ARGB int for Palette API.
 */
private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
}

/**
 * Converts ARGB int to Compose Color.
 */
private fun Int.toComposeColor(): Color = Color(this)

/**
 * Composable that extracts dynamic colors from an image URL.
 * Returns a state that updates when the image changes.
 *
 * Usage:
 * ```
 * val dynamicColors by rememberDynamicColors(backdropUrl)
 * // Use dynamicColors.backgroundTint, dynamicColors.accentColor, etc.
 * ```
 */
@Composable
fun rememberDynamicColors(imageUrl: String?): DynamicColors {
    val context = LocalContext.current
    var colors by remember { mutableStateOf(DynamicColors.Default) }

    LaunchedEffect(imageUrl) {
        colors = if (imageUrl != null) {
            extractColorsFromUrl(context, imageUrl)
        } else {
            DynamicColors.Default
        }
    }

    return colors
}

/**
 * Composable that extracts gradient-optimized colors from an image URL.
 * Returns colors suitable for creating animated gradient backgrounds.
 *
 * Usage:
 * ```
 * val gradientColors = rememberGradientColors(backdropUrl)
 * DynamicBackground(gradientColors = gradientColors)
 * ```
 */
@Composable
fun rememberGradientColors(imageUrl: String?): GradientColors {
    val context = LocalContext.current
    var colors by remember { mutableStateOf(GradientColors.Default) }

    LaunchedEffect(imageUrl) {
        colors = if (imageUrl != null) {
            extractGradientColorsFromUrl(context, imageUrl)
        } else {
            GradientColors.Default
        }
    }

    return colors
}

/**
 * Extracts gradient-optimized colors from an image URL.
 * Uses caching to avoid re-extracting for the same images.
 */
private suspend fun extractGradientColorsFromUrl(context: Context, imageUrl: String?): GradientColors {
    if (imageUrl.isNullOrBlank()) {
        return GradientColors.Default
    }

    // Check cache first
    gradientColorCache.get(imageUrl)?.let { return it }

    return withContext(Dispatchers.IO) {
        try {
            val loader = context.imageLoader // Use Coil singleton
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()

            val result = loader.execute(request)

            if (result is SuccessResult) {
                // Coil 3: Convert Image -> Drawable -> Bitmap
                val drawable = result.image.asDrawable(context.resources)
                val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)

                extractGradientColorsFromBitmap(bitmap).also {
                    // Cache the result
                    gradientColorCache.put(imageUrl, it)
                }
            } else {
                GradientColors.Default
            }
        } catch (_: Exception) {
            GradientColors.Default
        }
    }
}

/**
 * Extracts gradient-optimized colors from a bitmap.
 * Returns full opacity colors - alpha is applied when rendering.
 *
 * - Primary (Bottom-Right): darkVibrant -> darkMuted (deep, rich colors)
 * - Secondary (Top-Left): Smart selection preferring cool tones (blue/purple/green)
 * - Tertiary (Top-Right): vibrant -> lightVibrant (bright accent)
 */
private fun extractGradientColorsFromBitmap(bitmap: Bitmap): GradientColors {
    val palette = Palette.from(bitmap)
        .maximumColorCount(16)
        .generate()

    val vibrant = palette.vibrantSwatch
    val darkVibrant = palette.darkVibrantSwatch
    val lightVibrant = palette.lightVibrantSwatch
    val muted = palette.mutedSwatch
    val darkMuted = palette.darkMutedSwatch

    // Primary (Bottom-Right): Deep anchor color - full opacity
    val primaryColor = (darkVibrant ?: darkMuted)?.let {
        Color(it.rgb)
    } ?: Color.Unspecified

    // Secondary (Top-Left): Smart selection preferring cool tones
    val secondarySwatch = when {
        vibrant != null && isCoolColor(vibrant) -> vibrant
        muted != null && isCoolColor(muted) -> muted
        vibrant != null -> vibrant
        muted != null -> muted
        else -> null
    }
    val secondaryColor = secondarySwatch?.let {
        Color(it.rgb)
    } ?: Color.Unspecified

    // Tertiary (Top-Right): Bright accent color
    val tertiaryColor = (vibrant ?: lightVibrant)?.let {
        Color(it.rgb)
    } ?: Color.Unspecified

    return GradientColors(
        primary = primaryColor,
        secondary = secondaryColor,
        tertiary = tertiaryColor,
        isExtracted = true,
    )
}

/**
 * Determines if a color swatch is "cool" (blue/purple/green) vs "warm" (red/orange/yellow).
 */
private fun isCoolColor(swatch: Palette.Swatch): Boolean {
    val rgb = swatch.rgb
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    return b > r && b + g > r * 1.5f
}
