package dev.jausc.myflix.tv

import android.content.Context
import dev.jausc.myflix.core.common.preferences.AppPreferences

/**
 * TV app preferences manager.
 * Extends [AppPreferences] with TV-specific preferences name.
 */
class TvPreferences private constructor(context: Context) : AppPreferences(context) {

    override val preferencesName: String = PREFS_NAME

    companion object {
        private const val PREFS_NAME = "myflix_tv_prefs"

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
