package dev.jausc.myflix.core.common.preferences

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Centralized preference key definitions for all persistence layers.
 *
 * This file contains keys for:
 * - DataStore (authentication/credentials in AppState)
 * - SharedPreferences (UI settings in AppPreferences)
 *
 * Centralizing keys prevents:
 * - Typos and inconsistencies
 * - Accidental key collisions
 * - Scattered magic strings throughout the codebase
 */
object PreferenceKeys {

    // ==================== DataStore Keys (AppState) ====================

    /**
     * Keys for DataStore-based authentication storage.
     * Used in [dev.jausc.myflix.core.data.AppState].
     */
    object DataStore {
        /** DataStore file name */
        const val STORE_NAME = "myflix_prefs"

        /** Jellyfin server URL */
        val SERVER_URL = stringPreferencesKey("server_url")

        /** Authentication access token */
        val ACCESS_TOKEN = stringPreferencesKey("access_token")

        /** Authenticated user ID */
        val USER_ID = stringPreferencesKey("user_id")

        /** Unique device identifier */
        val DEVICE_ID = stringPreferencesKey("device_id")

        /** Stored username (for Seerr integration) */
        val USERNAME = stringPreferencesKey("username")

        /** Stored password (for Seerr integration) */
        val PASSWORD = stringPreferencesKey("password")
    }

    // ==================== SharedPreferences Keys (AppPreferences) ====================

    /**
     * Keys for SharedPreferences-based UI settings storage.
     * Used in [AppPreferences] and platform-specific subclasses.
     */
    object Prefs {
        /** TV app preferences file name */
        const val TV_PREFS_NAME = "myflix_tv_prefs"

        /** Mobile app preferences file name */
        const val MOBILE_PREFS_NAME = "myflix_mobile_prefs"

        // Playback preferences
        /** Hide watched items from Recently Added rows */
        const val HIDE_WATCHED_FROM_RECENT = "hide_watched_from_recent"

        /** Use MPV player instead of ExoPlayer */
        const val USE_MPV_PLAYER = "use_mpv_player"

        // Home screen row preferences
        /** Show upcoming episodes (Season Premieres) row */
        const val SHOW_SEASON_PREMIERES = "show_season_premieres"

        /** Show genre-based content rows */
        const val SHOW_GENRE_ROWS = "show_genre_rows"

        /** Comma-separated list of enabled genres */
        const val ENABLED_GENRES = "enabled_genres"

        /** Show collections row */
        const val SHOW_COLLECTIONS = "show_collections"

        /** Comma-separated list of pinned collection IDs */
        const val PINNED_COLLECTIONS = "pinned_collections"

        /** Show suggestions row */
        const val SHOW_SUGGESTIONS = "show_suggestions"

        // Seerr integration preferences
        /** Whether Seerr integration is enabled */
        const val SEERR_ENABLED = "seerr_enabled"

        /** Seerr server URL */
        const val SEERR_URL = "seerr_url"

        /** Whether Seerr was auto-detected */
        const val SEERR_AUTO_DETECTED = "seerr_auto_detected"

        /** Seerr API key for persistent authentication */
        const val SEERR_API_KEY = "seerr_api_key"

        /** Seerr session cookie for persistent authentication */
        const val SEERR_SESSION_COOKIE = "seerr_session_cookie"
    }

    // ==================== Default Values ====================

    /**
     * Default values for preferences.
     */
    object Defaults {
        const val HIDE_WATCHED_FROM_RECENT = false
        const val USE_MPV_PLAYER = false
        const val SHOW_SEASON_PREMIERES = true
        const val SHOW_GENRE_ROWS = false
        const val SHOW_COLLECTIONS = true
        const val SHOW_SUGGESTIONS = true
        const val SEERR_ENABLED = false
        const val SEERR_AUTO_DETECTED = false
    }
}
