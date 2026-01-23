@file:Suppress("MagicNumber")

package dev.jausc.myflix.core.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simple background music player for theme songs on detail screens.
 * Uses Android MediaPlayer for audio-only playback with fade in/out effects.
 */
class ThemeMusicPlayer(
    private val scope: CoroutineScope,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null
    private var fadeJob: Job? = null
    private var isEnabled: Boolean = true
    private var volume: Float = 0.5f

    companion object {
        private const val TAG = "ThemeMusicPlayer"
        private const val FADE_DURATION_MS = 1000L
        private const val FADE_STEPS = 20
    }

    /**
     * Play a theme song from the given URL.
     * If a theme is already playing, it will fade out first.
     *
     * @param url The audio stream URL
     * @param looping Whether to loop the theme song
     */
    fun play(url: String, looping: Boolean = true) {
        if (!isEnabled) {
            Log.d(TAG, "Theme music disabled, not playing")
            return
        }

        if (currentUrl == url && mediaPlayer?.isPlaying == true) {
            Log.d(TAG, "Already playing this theme")
            return
        }

        scope.launch(Dispatchers.Main) {
            // Fade out existing music first
            fadeOut()

            try {
                release()

                Log.d(TAG, "Playing theme from: $url")
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build(),
                    )
                    setDataSource(url)
                    isLooping = looping
                    setOnPreparedListener {
                        currentUrl = url
                        fadeIn()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                        false
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing theme music", e)
                release()
            }
        }
    }

    /**
     * Stop the theme song with fade out.
     */
    fun stop() {
        scope.launch(Dispatchers.Main) {
            fadeOut()
            release()
        }
    }

    /**
     * Pause the theme song (e.g., when navigating to player).
     */
    fun pause() {
        scope.launch(Dispatchers.Main) {
            fadeOut()
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing theme music", e)
            }
        }
    }

    /**
     * Resume the theme song after pause.
     */
    fun resume() {
        if (!isEnabled) return

        scope.launch(Dispatchers.Main) {
            try {
                if (mediaPlayer?.isPlaying == false && currentUrl != null) {
                    mediaPlayer?.start()
                    fadeIn()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming theme music", e)
            }
        }
    }

    /**
     * Set whether theme music is enabled.
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            stop()
        }
    }

    /**
     * Set the volume for theme music.
     *
     * @param volume Volume level from 0.0 to 1.0
     */
    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(this.volume, this.volume)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    /**
     * Check if theme music is currently playing.
     */
    val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }

    private fun fadeIn() {
        fadeJob?.cancel()
        fadeJob = scope.launch(Dispatchers.Main) {
            try {
                mediaPlayer?.let { player ->
                    player.setVolume(0f, 0f)
                    player.start()

                    val stepDelay = FADE_DURATION_MS / FADE_STEPS
                    val volumeStep = volume / FADE_STEPS

                    for (step in 0..FADE_STEPS) {
                        val currentVolume = volumeStep * step
                        player.setVolume(currentVolume, currentVolume)
                        delay(stepDelay)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during fade in", e)
            }
        }
    }

    private suspend fun fadeOut() {
        fadeJob?.cancel()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val stepDelay = FADE_DURATION_MS / FADE_STEPS
                    val currentVolume = volume

                    for (step in FADE_STEPS downTo 0) {
                        val newVolume = currentVolume / FADE_STEPS * step
                        player.setVolume(newVolume, newVolume)
                        delay(stepDelay)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during fade out", e)
        }
    }

    private fun release() {
        fadeJob?.cancel()
        fadeJob = null
        currentUrl = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player", e)
        }
        mediaPlayer = null
    }
}
