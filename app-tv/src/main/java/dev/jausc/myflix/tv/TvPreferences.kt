package dev.jausc.myflix.tv

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple preferences manager for TV app settings.
 * Uses SharedPreferences for persistence.
 */
class TvPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    // State flows for reactive updates
    private val _hideWatchedFromRecent = MutableStateFlow(
        prefs.getBoolean(KEY_HIDE_WATCHED_FROM_RECENT, false)
    )
    val hideWatchedFromRecent: StateFlow<Boolean> = _hideWatchedFromRecent.asStateFlow()

    private val _useMpvPlayer = MutableStateFlow(
        prefs.getBoolean(KEY_USE_MPV_PLAYER, false)
    )
    val useMpvPlayer: StateFlow<Boolean> = _useMpvPlayer.asStateFlow()

    /**
     * Set whether to hide watched items from Recently Added rows
     */
    fun setHideWatchedFromRecent(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_WATCHED_FROM_RECENT, hide).apply()
        _hideWatchedFromRecent.value = hide
    }

    /**
     * Set whether to use MPV player instead of ExoPlayer.
     * MPV offers better codec support but may have compatibility issues.
     * ExoPlayer is the default and recommended for most users.
     */
    fun setUseMpvPlayer(useMpv: Boolean) {
        prefs.edit().putBoolean(KEY_USE_MPV_PLAYER, useMpv).apply()
        _useMpvPlayer.value = useMpv
    }

    companion object {
        private const val PREFS_NAME = "myflix_tv_prefs"
        private const val KEY_HIDE_WATCHED_FROM_RECENT = "hide_watched_from_recent"
        private const val KEY_USE_MPV_PLAYER = "use_mpv_player"
        
        @Volatile
        private var INSTANCE: TvPreferences? = null
        
        fun getInstance(context: Context): TvPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TvPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
