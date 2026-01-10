package dev.jausc.myflix.mobile

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mobile app preferences manager.
 * Uses SharedPreferences for persistence and StateFlow for reactive updates.
 */
class MobilePreferences private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "myflix_mobile_prefs"
        private const val KEY_HIDE_WATCHED = "hide_watched_from_recent"
        private const val KEY_USE_MPV_PLAYER = "use_mpv_player"

        @Volatile
        private var INSTANCE: MobilePreferences? = null

        fun getInstance(context: Context): MobilePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MobilePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Hide watched items from Recently Added rows
    private val _hideWatchedFromRecent = MutableStateFlow(prefs.getBoolean(KEY_HIDE_WATCHED, false))
    val hideWatchedFromRecent: StateFlow<Boolean> = _hideWatchedFromRecent.asStateFlow()

    fun setHideWatchedFromRecent(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_WATCHED, hide).apply()
        _hideWatchedFromRecent.value = hide
    }

    // Use MPV player instead of ExoPlayer
    private val _useMpvPlayer = MutableStateFlow(prefs.getBoolean(KEY_USE_MPV_PLAYER, false))
    val useMpvPlayer: StateFlow<Boolean> = _useMpvPlayer.asStateFlow()

    fun setUseMpvPlayer(use: Boolean) {
        prefs.edit().putBoolean(KEY_USE_MPV_PLAYER, use).apply()
        _useMpvPlayer.value = use
    }
}
