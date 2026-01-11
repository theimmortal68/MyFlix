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

    // Home Screen Row Preferences
    private val _showSeasonPremieres = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_SEASON_PREMIERES, true)
    )
    val showSeasonPremieres: StateFlow<Boolean> = _showSeasonPremieres.asStateFlow()

    private val _showGenreRows = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_GENRE_ROWS, false)
    )
    val showGenreRows: StateFlow<Boolean> = _showGenreRows.asStateFlow()

    private val _enabledGenres = MutableStateFlow(
        loadEnabledGenres()
    )
    val enabledGenres: StateFlow<List<String>> = _enabledGenres.asStateFlow()

    private val _showCollections = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_COLLECTIONS, true)
    )
    val showCollections: StateFlow<Boolean> = _showCollections.asStateFlow()

    private val _pinnedCollections = MutableStateFlow(
        loadPinnedCollections()
    )
    val pinnedCollections: StateFlow<List<String>> = _pinnedCollections.asStateFlow()

    private val _showSuggestions = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_SUGGESTIONS, true)
    )
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    // Seerr Integration Preferences
    private val _seerrEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SEERR_ENABLED, false)
    )
    val seerrEnabled: StateFlow<Boolean> = _seerrEnabled.asStateFlow()

    private val _seerrUrl = MutableStateFlow(
        prefs.getString(KEY_SEERR_URL, null)
    )
    val seerrUrl: StateFlow<String?> = _seerrUrl.asStateFlow()

    private val _seerrAutoDetected = MutableStateFlow(
        prefs.getBoolean(KEY_SEERR_AUTO_DETECTED, false)
    )
    val seerrAutoDetected: StateFlow<Boolean> = _seerrAutoDetected.asStateFlow()

    private val _seerrApiKey = MutableStateFlow(
        prefs.getString(KEY_SEERR_API_KEY, null)
    )
    val seerrApiKey: StateFlow<String?> = _seerrApiKey.asStateFlow()

    private val _seerrSessionCookie = MutableStateFlow(
        prefs.getString(KEY_SEERR_SESSION_COOKIE, null)
    )
    val seerrSessionCookie: StateFlow<String?> = _seerrSessionCookie.asStateFlow()

    private fun loadEnabledGenres(): List<String> {
        val stored = prefs.getString(KEY_ENABLED_GENRES, null) ?: return emptyList()
        return if (stored.isBlank()) emptyList() else stored.split(",")
    }

    private fun loadPinnedCollections(): List<String> {
        val stored = prefs.getString(KEY_PINNED_COLLECTIONS, null) ?: return emptyList()
        return if (stored.isBlank()) emptyList() else stored.split(",")
    }

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

    /**
     * Set whether to show upcoming episodes (Season Premieres) row
     */
    fun setShowSeasonPremieres(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SEASON_PREMIERES, show).apply()
        _showSeasonPremieres.value = show
    }

    /**
     * Set whether to show genre-based content rows
     */
    fun setShowGenreRows(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GENRE_ROWS, show).apply()
        _showGenreRows.value = show
    }

    /**
     * Set which genres to display as rows on the home screen (ordered)
     */
    fun setEnabledGenres(genres: List<String>) {
        val stored = genres.joinToString(",")
        prefs.edit().putString(KEY_ENABLED_GENRES, stored).apply()
        _enabledGenres.value = genres
    }

    /**
     * Set whether to show collections row
     */
    fun setShowCollections(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_COLLECTIONS, show).apply()
        _showCollections.value = show
    }

    /**
     * Set which collections to pin on the home screen (ordered)
     */
    fun setPinnedCollections(collectionIds: List<String>) {
        val stored = collectionIds.joinToString(",")
        prefs.edit().putString(KEY_PINNED_COLLECTIONS, stored).apply()
        _pinnedCollections.value = collectionIds
    }

    /**
     * Set whether to show suggestions row
     */
    fun setShowSuggestions(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SUGGESTIONS, show).apply()
        _showSuggestions.value = show
    }

    /**
     * Set whether Seerr integration is enabled
     */
    fun setSeerrEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEERR_ENABLED, enabled).apply()
        _seerrEnabled.value = enabled
    }

    /**
     * Set the Seerr server URL
     */
    fun setSeerrUrl(url: String?) {
        prefs.edit().putString(KEY_SEERR_URL, url).apply()
        _seerrUrl.value = url
    }

    /**
     * Set whether Seerr was auto-detected (vs manually configured)
     */
    fun setSeerrAutoDetected(autoDetected: Boolean) {
        prefs.edit().putBoolean(KEY_SEERR_AUTO_DETECTED, autoDetected).apply()
        _seerrAutoDetected.value = autoDetected
    }

    /**
     * Set the Seerr API key for persistent authentication
     */
    fun setSeerrApiKey(apiKey: String?) {
        if (apiKey != null) {
            prefs.edit().putString(KEY_SEERR_API_KEY, apiKey).apply()
        } else {
            prefs.edit().remove(KEY_SEERR_API_KEY).apply()
        }
        _seerrApiKey.value = apiKey
    }

    /**
     * Set the Seerr session cookie for persistent authentication
     */
    fun setSeerrSessionCookie(cookie: String?) {
        if (cookie != null) {
            prefs.edit().putString(KEY_SEERR_SESSION_COOKIE, cookie).apply()
        } else {
            prefs.edit().remove(KEY_SEERR_SESSION_COOKIE).apply()
        }
        _seerrSessionCookie.value = cookie
    }

    /**
     * Clear all Seerr configuration
     */
    fun clearSeerrConfig() {
        prefs.edit()
            .remove(KEY_SEERR_ENABLED)
            .remove(KEY_SEERR_URL)
            .remove(KEY_SEERR_AUTO_DETECTED)
            .remove(KEY_SEERR_API_KEY)
            .remove(KEY_SEERR_SESSION_COOKIE)
            .apply()
        _seerrEnabled.value = false
        _seerrUrl.value = null
        _seerrAutoDetected.value = false
        _seerrApiKey.value = null
        _seerrSessionCookie.value = null
    }

    companion object {
        private const val PREFS_NAME = "myflix_tv_prefs"
        private const val KEY_HIDE_WATCHED_FROM_RECENT = "hide_watched_from_recent"
        private const val KEY_USE_MPV_PLAYER = "use_mpv_player"
        private const val KEY_SHOW_SEASON_PREMIERES = "show_season_premieres"
        private const val KEY_SHOW_GENRE_ROWS = "show_genre_rows"
        private const val KEY_ENABLED_GENRES = "enabled_genres"
        private const val KEY_SHOW_COLLECTIONS = "show_collections"
        private const val KEY_PINNED_COLLECTIONS = "pinned_collections"
        private const val KEY_SHOW_SUGGESTIONS = "show_suggestions"
        private const val KEY_SEERR_ENABLED = "seerr_enabled"
        private const val KEY_SEERR_URL = "seerr_url"
        private const val KEY_SEERR_AUTO_DETECTED = "seerr_auto_detected"
        private const val KEY_SEERR_API_KEY = "seerr_api_key"
        private const val KEY_SEERR_SESSION_COOKIE = "seerr_session_cookie"

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
