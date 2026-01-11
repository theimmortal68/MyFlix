package dev.jausc.myflix.mobile

import android.content.Context
import dev.jausc.myflix.core.common.preferences.AppPreferences

/**
 * Mobile app preferences manager.
 * Extends [AppPreferences] with mobile-specific preferences name.
 */
class MobilePreferences private constructor(context: Context) : AppPreferences(context) {

    override val preferencesName: String = PREFS_NAME

    companion object {
        private const val PREFS_NAME = "myflix_mobile_prefs"

        @Volatile
        private var INSTANCE: MobilePreferences? = null

        fun getInstance(context: Context): MobilePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MobilePreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
