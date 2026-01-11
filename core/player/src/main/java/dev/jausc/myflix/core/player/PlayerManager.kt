package dev.jausc.myflix.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class PlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null

    fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
        return player!!
    }

    fun play(url: String, startPosition: Long = 0) {
        getPlayer().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            if (startPosition > 0) {
                seekTo(startPosition)
            }
            playWhenReady = true
        }
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun release() {
        player?.release()
        player = null
    }

    val isPlaying: Boolean get() = player?.isPlaying == true
    val currentPosition: Long get() = player?.currentPosition ?: 0
    val duration: Long get() = player?.duration ?: 0
}
