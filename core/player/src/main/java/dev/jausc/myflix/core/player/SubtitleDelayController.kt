package dev.jausc.myflix.core.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller for delaying subtitle cue display in ExoPlayer.
 *
 * ExoPlayer doesn't have built-in subtitle delay support, so this class
 * intercepts cues via Player.Listener.onCues() and delays their display
 * using a Handler.
 *
 * **Thread Safety:** This class must be used from the main thread only,
 * as it manipulates Handler callbacks and UI state.
 *
 * Usage:
 * ```kotlin
 * val controller = SubtitleDelayController()
 *
 * // In ExoPlayer setup
 * player.addListener(object : Player.Listener {
 *     override fun onCues(cueGroup: CueGroup) {
 *         controller.onCues(cueGroup)
 *     }
 * })
 *
 * // In UI layer
 * val cues by controller.cues.collectAsState()
 * SubtitleView.setCues(cues)
 *
 * // To change delay
 * controller.setDelayMs(500) // +0.5s delay
 * ```
 */
class SubtitleDelayController(
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private var delayMs: Long = 0L
    private var pendingToken: Int = 0

    private val _cues = MutableStateFlow<List<Cue>>(emptyList())

    /**
     * Current cues to display, delayed according to [delayMs].
     * Observe this in the UI layer to update SubtitleView.
     */
    val cues: StateFlow<List<Cue>> = _cues.asStateFlow()

    /**
     * Set the subtitle delay.
     *
     * @param ms Delay in milliseconds.
     *           Positive = delay subtitles (show later)
     *           Negative = advance subtitles (show earlier, best effort)
     *           Range: -10000 to +10000
     */
    fun setDelayMs(ms: Long) {
        delayMs = ms.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        // Clear any pending delayed posts when delay changes
        handler.removeCallbacksAndMessages(TOKEN)
        Log.d(TAG, "Subtitle delay set to ${delayMs}ms")
    }

    /**
     * Get the current subtitle delay in milliseconds.
     */
    fun getDelayMs(): Long = delayMs

    /**
     * Process incoming cues from ExoPlayer.
     * Call this from Player.Listener.onCues().
     *
     * @param cueGroup The cue group from ExoPlayer
     */
    fun onCues(cueGroup: CueGroup) {
        val token = ++pendingToken
        handler.removeCallbacksAndMessages(TOKEN)

        if (delayMs <= 0L) {
            // Negative or zero delay: show immediately
            // Note: True "early" display would require knowing future cues,
            // which ExoPlayer doesn't provide. Best effort is immediate display.
            _cues.value = cueGroup.cues
        } else {
            // Positive delay: hide current cues and schedule delayed display
            _cues.value = emptyList()
            handler.postDelayed(
                {
                    if (token == pendingToken) {
                        _cues.value = cueGroup.cues
                    }
                },
                TOKEN,
                delayMs,
            )
        }
    }

    /**
     * Clear all cues and pending callbacks.
     * Call this when playback stops or player is released.
     */
    fun release() {
        handler.removeCallbacksAndMessages(TOKEN)
        _cues.value = emptyList()
        pendingToken = 0
        Log.d(TAG, "SubtitleDelayController released")
    }

    companion object {
        private const val TAG = "SubtitleDelayController"
        private const val MIN_DELAY_MS = -10_000L
        private const val MAX_DELAY_MS = 10_000L
        private val TOKEN = Any()
    }
}
