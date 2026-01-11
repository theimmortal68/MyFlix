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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.jausc.myflix.mobile.MainActivity

/**
 * Background playback service using Media3 MediaSessionService.
 * Enables audio playback when app is in background or screen is off.
 *
 * Features:
 * - Foreground service with media notification
 * - Lock screen controls
 * - Bluetooth/headphone controls
 * - Audio focus management
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    companion object {
        private const val TAG = "PlaybackService"
        private const val NOTIFICATION_CHANNEL_ID = "myflix_playback_channel"

        @Suppress("UnusedPrivateProperty")
        private const val NOTIFICATION_ID = 1001

        // Keys for media item extras
        const val EXTRA_ITEM_ID = "itemId"
        const val EXTRA_SERVER_URL = "serverUrl"
    }

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PlaybackService created")

        createNotificationChannel()
        initializePlayer()
        initializeMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MyFlix Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows playback controls for background audio"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                // handleAudioFocus=
                true,
            )
            .setHandleAudioBecomingNoisy(true) // Pause when headphones disconnected
            .setWakeMode(C.WAKE_MODE_NETWORK) // Keep CPU/WiFi awake during playback
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val stateName = when (playbackState) {
                            Player.STATE_IDLE -> "IDLE"
                            Player.STATE_BUFFERING -> "BUFFERING"
                            Player.STATE_READY -> "READY"
                            Player.STATE_ENDED -> "ENDED"
                            else -> "UNKNOWN($playbackState)"
                        }
                        Log.d(TAG, "Playback state changed: $stateName")
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "Playback ended")
                            }
                            Player.STATE_IDLE -> {
                                // Could stop service here if desired
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d(TAG, "isPlaying changed: $isPlaying")
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}", error)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        Log.d(TAG, "Media item transition: ${mediaItem?.mediaMetadata?.title}, reason=$reason")
                    }
                })
            }
    }

    private fun initializeMediaSession() {
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            // Stop service if not playing anything
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "PlaybackService destroyed")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    /**
     * Play media from URL with metadata
     */
    fun playMedia(
        url: String,
        title: String,
        subtitle: String? = null,
        artworkUrl: String? = null,
        startPositionMs: Long = 0,
        itemId: String? = null,
    ) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
            .build()

        val extras = Bundle().apply {
            itemId?.let { putString(EXTRA_ITEM_ID, it) }
        }

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .setTag(extras)
            .build()

        player?.apply {
            setMediaItem(mediaItem)
            seekTo(startPositionMs)
            prepare()
            play()
        }
    }

    /**
     * Get the current player for UI binding
     */
    fun getPlayer(): Player? = player

    /**
     * MediaSession callback for handling media button events
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Return empty - we don't support resumption from notification after app killed
            return com.google.common.util.concurrent.Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    emptyList(),
                    0,
                    0,
                ),
            )
        }
    }
}
