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

        // Legacy credential keys - for migration to encrypted storage only.
        // New code should NOT use these; credentials are now in SecureCredentialStore.

        /** @deprecated Migrate to SecureCredentialStore, then remove */
        val USERNAME_LEGACY = stringPreferencesKey("username")

        /** @deprecated Migrate to SecureCredentialStore, then remove */
        val PASSWORD_LEGACY = stringPreferencesKey("password")
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

        /** Use WebView fallback for Seerr trailers */
        const val USE_TRAILER_FALLBACK = "use_trailer_fallback"

        /** Preferred audio language (ISO 639-2/B code like "eng", "jpn", "spa") */
        const val PREFERRED_AUDIO_LANGUAGE = "preferred_audio_language"

        /** Preferred subtitle language (ISO 639-2/B code like "eng", "jpn", "spa") */
        const val PREFERRED_SUBTITLE_LANGUAGE = "preferred_subtitle_language"

        /** Max streaming bitrate in Mbps (0 = unlimited/direct play) */
        const val MAX_STREAMING_BITRATE = "max_streaming_bitrate"

        /** Skip forward duration in seconds */
        const val SKIP_FORWARD_SECONDS = "skip_forward_seconds"

        /** Skip backward duration in seconds */
        const val SKIP_BACKWARD_SECONDS = "skip_backward_seconds"

        /** Player display mode (FIT, FILL, ZOOM, STRETCH) */
        const val PLAYER_DISPLAY_MODE = "player_display_mode"

        /** Refresh rate switching mode (OFF, AUTO, 60, 120) */
        const val REFRESH_RATE_MODE = "refresh_rate_mode"

        // Media segment preferences (skip intro/credits)
        /** Skip intro behavior: OFF, ASK, AUTO */
        const val SKIP_INTRO_MODE = "skip_intro_mode"

        /** Skip credits behavior: OFF, ASK, AUTO */
        const val SKIP_CREDITS_MODE = "skip_credits_mode"

        // Subtitle styling preferences
        /** Subtitle font size (SMALL, MEDIUM, LARGE, EXTRA_LARGE) */
        const val SUBTITLE_FONT_SIZE = "subtitle_font_size"

        /** Subtitle font color (WHITE, YELLOW, GREEN, CYAN, BLUE, MAGENTA) */
        const val SUBTITLE_FONT_COLOR = "subtitle_font_color"

        /** Subtitle background opacity (0-100) */
        const val SUBTITLE_BACKGROUND_OPACITY = "subtitle_background_opacity"

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

        /** Show recent requests row on Seerr home (Discover) */
        const val SHOW_SEERR_RECENT_REQUESTS = "show_seerr_recent_requests"

        /** Enable Universe Collections feature (separate screen for universe-tagged collections) */
        const val UNIVERSES_ENABLED = "universes_enabled"

        /** Show Discover (Seerr) in navigation bar */
        const val SHOW_DISCOVER_NAV = "show_discover_nav"

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

        // Library filter preferences (per-library, use with libraryId suffix)
        /** Prefix for library sort option (append libraryId) */
        const val LIBRARY_SORT_BY_PREFIX = "library_sort_by_"

        /** Prefix for library sort order (append libraryId) */
        const val LIBRARY_SORT_ORDER_PREFIX = "library_sort_order_"

        /** Prefix for library view mode (append libraryId) */
        const val LIBRARY_VIEW_MODE_PREFIX = "library_view_mode_"

        /** Prefix for library watched filter (append libraryId) */
        const val LIBRARY_WATCHED_FILTER_PREFIX = "library_watched_filter_"

        /** Prefix for library selected genres (append libraryId) */
        const val LIBRARY_GENRES_PREFIX = "library_genres_"

        /** Prefix for library year range from (append libraryId) */
        const val LIBRARY_YEAR_FROM_PREFIX = "library_year_from_"

        /** Prefix for library year range to (append libraryId) */
        const val LIBRARY_YEAR_TO_PREFIX = "library_year_to_"

        /** Prefix for library rating filter (append libraryId) */
        const val LIBRARY_RATING_PREFIX = "library_rating_"

        /** Prefix for library parental ratings (append libraryId) */
        const val LIBRARY_PARENTAL_RATINGS_PREFIX = "library_parental_ratings_"

        // First-run tips
        /** Whether user has seen the nav bar tip */
        const val HAS_SEEN_NAV_BAR_TIP = "has_seen_nav_bar_tip"

        // Active playback session tracking (for crash recovery)
        /** Item ID of active playback session (empty if none) */
        const val ACTIVE_PLAYBACK_ITEM_ID = "active_playback_item_id"

        /** Position in ticks of active playback session */
        const val ACTIVE_PLAYBACK_POSITION_TICKS = "active_playback_position_ticks"

        /** Media source ID of active playback session */
        const val ACTIVE_PLAYBACK_MEDIA_SOURCE_ID = "active_playback_media_source_id"
    }

    // ==================== Default Values ====================

    /**
     * Default values for preferences.
     */
    object Defaults {
        const val HIDE_WATCHED_FROM_RECENT = false
        const val USE_MPV_PLAYER = false

        // Default to true until PoToken implementation is verified on real device
        // WebView fallback works but doesn't support remote control (seek/pause)
        const val USE_TRAILER_FALLBACK = true

        /** null means use Jellyfin server's default audio track */
        val PREFERRED_AUDIO_LANGUAGE: String? = null

        /** null means use Jellyfin server's default subtitle track */
        val PREFERRED_SUBTITLE_LANGUAGE: String? = null

        /** 0 = unlimited/direct play, otherwise value in Mbps */
        const val MAX_STREAMING_BITRATE = 0

        /** Default skip forward duration: 10 seconds */
        const val SKIP_FORWARD_SECONDS = 10

        /** Default skip backward duration: 10 seconds */
        const val SKIP_BACKWARD_SECONDS = 10

        /** Default to FIT (letterbox/pillarbox) */
        const val PLAYER_DISPLAY_MODE = "FIT"

        /** Default refresh rate mode: OFF (no switching) */
        const val REFRESH_RATE_MODE = "OFF"

        // Media segment defaults (OFF, ASK, AUTO)
        const val SKIP_INTRO_MODE = "ASK"
        const val SKIP_CREDITS_MODE = "ASK"

        // Subtitle styling defaults
        const val SUBTITLE_FONT_SIZE = "MEDIUM"
        const val SUBTITLE_FONT_COLOR = "WHITE"
        const val SUBTITLE_BACKGROUND_OPACITY = 75
        const val SHOW_SEASON_PREMIERES = true
        const val SHOW_GENRE_ROWS = false
        const val SHOW_COLLECTIONS = true
        const val SHOW_SUGGESTIONS = true
        const val SHOW_SEERR_RECENT_REQUESTS = false
        const val UNIVERSES_ENABLED = false
        const val SHOW_DISCOVER_NAV = false
        const val SEERR_ENABLED = false
        const val SEERR_AUTO_DETECTED = false
        const val HAS_SEEN_NAV_BAR_TIP = false
    }
}
