package dev.jausc.myflix.tv

import android.content.Context
import dev.jausc.myflix.core.common.preferences.AppPreferences
import dev.jausc.myflix.core.common.preferences.PreferenceKeys

/**
 * TV app preferences manager.
 * Extends [AppPreferences] with TV-specific preferences name.
 */
class TvPreferences private constructor(context: Context) : AppPreferences(context) {

    override val preferencesName: String = PreferenceKeys.Prefs.TV_PREFS_NAME

    companion object {
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
