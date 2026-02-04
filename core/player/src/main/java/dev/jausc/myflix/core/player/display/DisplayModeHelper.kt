package dev.jausc.myflix.core.player.display

import android.os.Build
import android.util.Log
import android.view.Display
import androidx.annotation.RequiresApi
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Utility object for display mode selection and refresh rate matching.
 *
 * Handles finding optimal display modes for video playback, including:
 * - NTSC frame rate conversion (23.976 -> 24, 29.97 -> 30, etc.)
 * - Integer multiple preference (120Hz for 24fps content)
 * - Seamless switching detection (API 31+)
 */
object DisplayModeHelper {
    private const val TAG = "DisplayModeHelper"

    /** Tolerance for NTSC rate matching (1.001 factor) */
    private const val NTSC_TOLERANCE = 0.05f

    /** Tolerance for integer multiple checking */
    private const val MULTIPLE_TOLERANCE = 0.1f

    /**
     * Standard refresh rates commonly supported by displays.
     */
    private val STANDARD_REFRESH_RATES = listOf(24f, 25f, 30f, 48f, 50f, 60f, 120f)

    /**
     * Finds the optimal display mode for the given video frame rate.
     *
     * Selection priority:
     * 1. Integer multiples of video FPS (e.g., 120Hz for 24fps)
     * 2. Exact match to target refresh rate
     * 3. Higher refresh rate that's a multiple
     *
     * @param display The display to query for available modes
     * @param videoFps The video's frame rate (e.g., 23.976, 24.0, 29.97)
     * @param allowResolutionChange If false, only considers modes matching current resolution
     * @return The optimal display mode, or null if no suitable mode found
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun findOptimalMode(
        display: Display,
        videoFps: Float,
        allowResolutionChange: Boolean,
    ): Display.Mode? {
        val currentMode = display.mode
        val supportedModes = display.supportedModes

        if (supportedModes.isEmpty()) {
            Log.w(TAG, "No supported modes available")
            return null
        }

        val targetHz = getOptimalRefreshRate(videoFps)
        Log.d(TAG, "Finding optimal mode for ${videoFps}fps (target: ${targetHz}Hz)")

        // Filter modes by resolution if needed
        val candidateModes = if (allowResolutionChange) {
            supportedModes.toList()
        } else {
            supportedModes.filter { mode ->
                mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
            }
        }

        if (candidateModes.isEmpty()) {
            Log.w(TAG, "No candidate modes after filtering")
            return null
        }

        // Score and sort modes
        val scoredModes = candidateModes.map { mode ->
            val score = calculateModeScore(mode, videoFps, targetHz)
            Log.v(TAG, "Mode ${mode.physicalWidth}x${mode.physicalHeight}@${mode.refreshRate}Hz -> score: $score")
            mode to score
        }.sortedByDescending { it.second }

        val bestMode = scoredModes.firstOrNull()?.first

        if (bestMode != null) {
            Log.d(
                TAG,
                "Selected mode: ${bestMode.physicalWidth}x${bestMode.physicalHeight}@${bestMode.refreshRate}Hz",
            )
        }

        return bestMode
    }

    /**
     * Maps a video frame rate to the optimal target refresh rate.
     *
     * Handles NTSC rates by rounding to the nearest standard rate:
     * - 23.976 fps -> 24 Hz
     * - 29.97 fps -> 30 Hz
     * - 59.94 fps -> 60 Hz
     *
     * @param videoFps The video's actual frame rate
     * @return The target display refresh rate
     */
    fun getOptimalRefreshRate(videoFps: Float): Float {
        // Handle edge cases
        if (videoFps <= 0f) return 60f

        // Check for NTSC rates (multiply by 1.001 to get standard rate)
        val ntscAdjusted = videoFps * 1.001f

        // Find the closest standard rate
        val closestStandard = STANDARD_REFRESH_RATES.minByOrNull { abs(it - ntscAdjusted) }
            ?: return videoFps.roundToInt().toFloat()

        // If the adjusted rate is close to a standard rate, use it
        return if (abs(closestStandard - ntscAdjusted) <= NTSC_TOLERANCE) {
            closestStandard
        } else {
            // Fall back to rounding to nearest integer
            videoFps.roundToInt().toFloat()
        }
    }

    /**
     * Checks if a display refresh rate is an integer multiple of the video frame rate.
     *
     * Integer multiples provide judder-free playback because each video frame
     * can be displayed for exactly the same number of refresh cycles.
     *
     * Examples:
     * - 120Hz is 5x multiple of 24fps (displays each frame 5 times)
     * - 60Hz is 2.5x of 24fps (NOT an integer multiple - causes judder)
     * - 48Hz is 2x multiple of 24fps
     *
     * @param modeHz The display's refresh rate
     * @param videoFps The video's frame rate
     * @return True if modeHz is an integer multiple of videoFps
     */
    fun isIntegerMultiple(modeHz: Float, videoFps: Float): Boolean {
        if (videoFps <= 0f || modeHz <= 0f) return false

        // Account for NTSC rates
        val normalizedFps = getOptimalRefreshRate(videoFps)

        val ratio = modeHz / normalizedFps
        val roundedRatio = ratio.roundToInt()

        // Check if the ratio is close to an integer
        if (roundedRatio <= 0) return false

        val difference = abs(ratio - roundedRatio)
        return difference <= MULTIPLE_TOLERANCE
    }

    /**
     * Checks if switching between two display modes can be done seamlessly.
     *
     * A seamless switch avoids the black screen flicker that normally occurs
     * when changing display modes. This is only available on API 31+ devices
     * that support the feature.
     *
     * @param current The current display mode
     * @param target The target display mode to switch to
     * @return True if the switch can be done without visible disruption
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun isSeamlessSwitch(current: Display.Mode, target: Display.Mode): Boolean {
        // Must be same resolution for seamless switch
        if (current.physicalWidth != target.physicalWidth ||
            current.physicalHeight != target.physicalHeight
        ) {
            return false
        }

        // Check if target rate is in alternative refresh rates
        val alternativeRates = current.alternativeRefreshRates
        if (alternativeRates.isEmpty()) {
            Log.d(TAG, "No alternative refresh rates available for seamless switching")
            return false
        }

        // Check if target refresh rate is in the alternatives
        val targetRefresh = target.refreshRate
        val isSeamless = alternativeRates.any { rate ->
            abs(rate - targetRefresh) < 0.5f
        }

        Log.d(
            TAG,
            "Seamless switch check: ${current.refreshRate}Hz -> ${targetRefresh}Hz = $isSeamless " +
                "(alternatives: ${alternativeRates.joinToString()})",
        )

        return isSeamless
    }

    /**
     * Calculates a score for how well a display mode matches the video content.
     *
     * Higher scores are better. The scoring favors:
     * 1. Integer multiples of the video FPS (huge bonus)
     * 2. Exact or near-exact refresh rate matches
     * 3. Higher resolution (if resolution changes are allowed)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun calculateModeScore(
        mode: Display.Mode,
        videoFps: Float,
        targetHz: Float,
    ): Int {
        var score = 0
        val modeHz = mode.refreshRate

        // Big bonus for integer multiples (judder-free playback)
        if (isIntegerMultiple(modeHz, videoFps)) {
            score += 1000

            // Prefer lower multiples (24Hz > 48Hz > 120Hz for 24fps)
            // But 120Hz is still very good for smooth UI during playback
            val multiplier = (modeHz / getOptimalRefreshRate(videoFps)).roundToInt()
            when (multiplier) {
                1 -> score += 100 // Exact match
                2 -> score += 90 // 2x (e.g., 48Hz for 24fps)
                5 -> score += 80 // 5x (e.g., 120Hz for 24fps)
                4 -> score += 70 // 4x (e.g., 120Hz for 30fps)
                3 -> score += 60 // 3x
                else -> score += 50 // Other multiples
            }
        }

        // Bonus for being close to target Hz
        val hzDifference = abs(modeHz - targetHz)
        if (hzDifference < 1f) {
            score += 200 // Near-exact match
        } else if (hzDifference < 5f) {
            score += 100
        }

        // Small bonus for higher refresh rates (smoother UI)
        score += (modeHz / 10).toInt()

        // Resolution bonus (prefer higher resolution if allowed)
        val pixels = mode.physicalWidth.toLong() * mode.physicalHeight
        score += (pixels / 10000).toInt().coerceAtMost(500)

        return score
    }

    /**
     * Gets a human-readable description of a display mode.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getModeDescription(mode: Display.Mode): String {
        return "${mode.physicalWidth}x${mode.physicalHeight} @ ${mode.refreshRate}Hz"
    }

    /**
     * Logs all available display modes for debugging.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun logAvailableModes(display: Display) {
        Log.d(TAG, "Available display modes:")
        display.supportedModes.forEach { mode ->
            val current = if (mode.modeId == display.mode.modeId) " (current)" else ""
            Log.d(TAG, "  - ${getModeDescription(mode)}$current")
        }
    }
}
