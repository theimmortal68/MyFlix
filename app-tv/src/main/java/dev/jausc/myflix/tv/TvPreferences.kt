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
    
    /**
     * Set whether to hide watched items from Recently Added rows
     */
    fun setHideWatchedFromRecent(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_WATCHED_FROM_RECENT, hide).apply()
        _hideWatchedFromRecent.value = hide
    }
    
    companion object {
        private const val PREFS_NAME = "myflix_tv_prefs"
        private const val KEY_HIDE_WATCHED_FROM_RECENT = "hide_watched_from_recent"
        
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
