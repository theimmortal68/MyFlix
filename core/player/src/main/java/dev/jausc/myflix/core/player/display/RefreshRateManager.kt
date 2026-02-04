package dev.jausc.myflix.core.player.display

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * Refresh rate switching mode for video playback.
 *
 * Controls how the display refresh rate is adjusted to match video content
 * for judder-free playback.
 */
enum class RefreshRateSwitchMode {
    /**
     * No refresh rate switching. Uses the system default refresh rate.
     * Safest option but may result in judder for 24fps content on 60Hz displays.
     */
    OFF,

    /**
     * Only switch refresh rate if the resolution stays the same.
     * Uses Surface.setFrameRate() on API 30+ for smooth transitions.
     * Will not cause black screen or HDMI handshake issues.
     */
    SEAMLESS,

    /**
     * Switch refresh rate even if resolution differs.
     * May cause black screen during HDMI handshake on some displays.
     * Provides the best frame rate matching but with potential visual disruption.
     */
    ALWAYS
}

/**
 * Manages display refresh rate switching during video playback.
 *
 * This class handles:
 * - Finding optimal display modes for video content
 * - Applying refresh rate changes based on the selected mode
 * - Preserving and restoring the original display mode
 * - Using seamless frame rate hints on API 30+ devices
 *
 * **Thread Safety:** This class is not thread-safe. All methods should be called
 * from the main (UI) thread, which is the standard pattern for Android UI operations.
 * The window attribute modifications require main thread execution.
 *
 * Usage:
 * ```kotlin
 * val manager = RefreshRateManager(context)
 *
 * // When playback starts
 * manager.applyRefreshRate(activity, mode, videoFps, surface)
 *
 * // When playback stops
 * manager.restoreOriginalMode(activity)
 * ```
 *
 * @param context Application or activity context for accessing display info
 */
class RefreshRateManager(private val context: Context) {
    companion object {
        private const val TAG = "RefreshRateManager"
    }

    /**
     * The original display mode ID before any refresh rate changes were applied.
     * Used to restore the display to its original state when playback ends.
     */
    private var originalModeId: Int? = null

    /**
     * Applies the optimal refresh rate for video playback.
     *
     * This method finds and applies the best display mode for the given video
     * frame rate, respecting the user's chosen switching mode.
     *
     * @param activity The activity whose window will have its display mode changed
     * @param mode The refresh rate switching mode (OFF, SEAMLESS, or ALWAYS)
     * @param videoFps The video's frame rate (e.g., 23.976, 24.0, 29.97, 60.0)
     * @param surface Optional surface for API 30+ frame rate hints (improves seamless switching)
     */
    fun applyRefreshRate(
        activity: Activity,
        mode: RefreshRateSwitchMode,
        videoFps: Float,
        surface: Surface? = null,
    ) {
        // Early exit if switching is disabled
        if (mode == RefreshRateSwitchMode.OFF) {
            Log.d(TAG, "Refresh rate switching is disabled")
            return
        }

        // API 23+ required for display mode switching
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "Display mode switching requires API 23+")
            return
        }

        val display = getDisplay() ?: run {
            Log.w(TAG, "Unable to get display")
            return
        }

        val currentMode = display.mode

        // Save original mode for later restoration (only once per playback session)
        if (originalModeId == null) {
            originalModeId = currentMode.modeId
            Log.d(TAG, "Saved original mode: ${DisplayModeHelper.getModeDescription(currentMode)}")
        }

        // Find the optimal display mode for this video
        val allowResolutionChange = mode == RefreshRateSwitchMode.ALWAYS
        val targetMode = DisplayModeHelper.findOptimalMode(display, videoFps, allowResolutionChange)

        if (targetMode == null) {
            Log.w(TAG, "No suitable display mode found for ${videoFps}fps")
            return
        }

        // Already using the optimal mode
        if (targetMode.modeId == currentMode.modeId) {
            Log.d(TAG, "Already using optimal mode: ${DisplayModeHelper.getModeDescription(targetMode)}")
            // Still set frame rate hint on surface for smoothness
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && surface != null) {
                setFrameRateHint(surface, videoFps)
            }
            return
        }

        Log.d(
            TAG,
            "Switching from ${DisplayModeHelper.getModeDescription(currentMode)} " +
                "to ${DisplayModeHelper.getModeDescription(targetMode)} for ${videoFps}fps",
        )

        when (mode) {
            RefreshRateSwitchMode.SEAMLESS -> {
                applySeamlessMode(activity, currentMode, targetMode, videoFps, surface)
            }

            RefreshRateSwitchMode.ALWAYS -> {
                applyAlwaysMode(activity, targetMode)
            }

            RefreshRateSwitchMode.OFF -> {
                // Already handled above
            }
        }
    }

    /**
     * Restores the display to its original mode.
     *
     * Should be called when playback ends or is paused to return the display
     * to its pre-playback state.
     *
     * @param activity The activity whose window will have its display mode restored
     */
    fun restoreOriginalMode(activity: Activity) {
        val modeId = originalModeId
        if (modeId == null) {
            Log.d(TAG, "No original mode to restore")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        Log.d(TAG, "Restoring original display mode (modeId: $modeId)")

        setPreferredDisplayMode(activity, modeId)
        originalModeId = null
    }

    /**
     * Applies seamless refresh rate switching.
     *
     * For API 30+, uses Surface.setFrameRate() which provides the smoothest
     * transition. For older APIs, only applies the mode if the display
     * reports seamless switching is available.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun applySeamlessMode(
        activity: Activity,
        currentMode: android.view.Display.Mode,
        targetMode: android.view.Display.Mode,
        videoFps: Float,
        surface: Surface?,
    ) {
        // API 30+: Use Surface.setFrameRate for best seamless behavior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && surface != null) {
            setFrameRateHint(surface, videoFps)
        }

        // API 31+: Check if seamless switching is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (DisplayModeHelper.isSeamlessSwitch(currentMode, targetMode)) {
                Log.d(TAG, "Seamless switch available, applying mode change")
                setPreferredDisplayMode(activity, targetMode.modeId)
            } else {
                Log.d(TAG, "Seamless switch not available, skipping mode change")
            }
        } else {
            // Pre-S: Only switch if same resolution (best effort seamless)
            if (currentMode.physicalWidth == targetMode.physicalWidth &&
                currentMode.physicalHeight == targetMode.physicalHeight
            ) {
                Log.d(TAG, "Same resolution, attempting mode switch (pre-S)")
                setPreferredDisplayMode(activity, targetMode.modeId)
            } else {
                Log.d(TAG, "Resolution differs, skipping mode change in seamless mode")
            }
        }
    }

    /**
     * Applies refresh rate switching regardless of seamless availability.
     *
     * This may cause a brief black screen during HDMI handshake on some displays,
     * but provides the best frame rate matching.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun applyAlwaysMode(activity: Activity, targetMode: android.view.Display.Mode) {
        Log.d(TAG, "Applying mode change (ALWAYS mode)")
        setPreferredDisplayMode(activity, targetMode.modeId)
    }

    /**
     * Sets a frame rate hint on the surface for smoother playback.
     *
     * This tells the display system the expected frame rate, allowing it to
     * optimize refresh timing even if the actual mode doesn't change.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun setFrameRateHint(surface: Surface, videoFps: Float) {
        try {
            val targetHz = DisplayModeHelper.getOptimalRefreshRate(videoFps)
            surface.setFrameRate(
                targetHz,
                Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
            )
            Log.d(TAG, "Set surface frame rate hint: ${targetHz}Hz")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set surface frame rate", e)
        }
    }

    /**
     * Sets the preferred display mode on the activity window.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setPreferredDisplayMode(activity: Activity, modeId: Int) {
        activity.runOnUiThread {
            try {
                val params = activity.window.attributes
                params.preferredDisplayModeId = modeId
                activity.window.attributes = params
                Log.d(TAG, "Set preferred display mode: $modeId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set display mode", e)
            }
        }
    }

    /**
     * Gets the display for the current context.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getDisplay(): android.view.Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
    }
}
