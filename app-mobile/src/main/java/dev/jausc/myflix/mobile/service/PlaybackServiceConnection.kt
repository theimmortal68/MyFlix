@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

package dev.jausc.myflix.mobile.service

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages connection to PlaybackService and provides Player access.
 * Use this in your UI to control playback.
 */
class PlaybackServiceConnection(private val context: Context) {
    companion object {
        private const val TAG = "PlaybackServiceConn"
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player.asStateFlow()

    /**
     * Connect to the PlaybackService
     */
    fun connect() {
        if (controllerFuture != null) {
            Log.d(TAG, "Already connecting/connected")
            return
        }

        Log.d(TAG, "Connecting to PlaybackService")

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                _player.value = mediaController
                _isConnected.value = true
                Log.d(TAG, "Connected to PlaybackService")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to PlaybackService", e)
                _isConnected.value = false
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Disconnect from the PlaybackService
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from PlaybackService")
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        _player.value = null
        _isConnected.value = false
    }

    /**
     * Get the current MediaController
     */
    fun getController(): MediaController? = mediaController

    /**
     * Play a URL with metadata
     */
    fun play(
        url: String,
        title: String,
        subtitle: String? = null,
        artworkUrl: String? = null,
        startPositionMs: Long = 0,
    ) {
        Log.d(TAG, "play() called - url: $url, startPos: $startPositionMs")
        Log.d(TAG, "mediaController: $mediaController, isConnected: ${_isConnected.value}")

        if (mediaController == null) {
            Log.e(TAG, "mediaController is null, cannot play")
            return
        }

        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
            .build()

        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()

        mediaController?.apply {
            Log.d(TAG, "Setting media item and starting playback")
            setMediaItem(mediaItem)
            seekTo(startPositionMs)
            prepare()
            play()
            Log.d(TAG, "Playback started")
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    /**
     * Seek relative to current position
     */
    fun seekRelative(offsetMs: Long) {
        mediaController?.let { controller ->
            val newPosition = (controller.currentPosition + offsetMs).coerceAtLeast(0)
            controller.seekTo(newPosition)
        }
    }

    /**
     * Stop playback
     */
    fun stop() {
        mediaController?.stop()
    }
}
