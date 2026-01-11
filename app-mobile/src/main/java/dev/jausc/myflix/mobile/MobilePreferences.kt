@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
)

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
        private const val KEY_SHOW_SEASON_PREMIERES = "show_season_premieres"
        private const val KEY_SHOW_GENRE_ROWS = "show_genre_rows"
        private const val KEY_ENABLED_GENRES = "enabled_genres"
        private const val KEY_SHOW_COLLECTIONS = "show_collections"
        private const val KEY_SHOW_SUGGESTIONS = "show_suggestions"
        private const val KEY_SEERR_ENABLED = "seerr_enabled"
        private const val KEY_SEERR_URL = "seerr_url"
        private const val KEY_SEERR_AUTO_DETECTED = "seerr_auto_detected"
        private const val KEY_SEERR_API_KEY = "seerr_api_key"
        private const val KEY_SEERR_SESSION_COOKIE = "seerr_session_cookie"
        private const val KEY_PINNED_COLLECTIONS = "pinned_collections"

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

    // Home Screen Row Preferences
    private val _showSeasonPremieres = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SEASON_PREMIERES, true))
    val showSeasonPremieres: StateFlow<Boolean> = _showSeasonPremieres.asStateFlow()

    fun setShowSeasonPremieres(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SEASON_PREMIERES, show).apply()
        _showSeasonPremieres.value = show
    }

    private val _showGenreRows = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GENRE_ROWS, false))
    val showGenreRows: StateFlow<Boolean> = _showGenreRows.asStateFlow()

    fun setShowGenreRows(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GENRE_ROWS, show).apply()
        _showGenreRows.value = show
    }

    // Enabled genres - ordered list for display order
    private val _enabledGenres = MutableStateFlow(loadEnabledGenres())
    val enabledGenres: StateFlow<List<String>> = _enabledGenres.asStateFlow()

    private fun loadEnabledGenres(): List<String> {
        val stored = prefs.getString(KEY_ENABLED_GENRES, null) ?: return emptyList()
        return if (stored.isBlank()) emptyList() else stored.split(",")
    }

    fun setEnabledGenres(genres: List<String>) {
        val stored = genres.joinToString(",")
        prefs.edit().putString(KEY_ENABLED_GENRES, stored).apply()
        _enabledGenres.value = genres
    }

    private val _showCollections = MutableStateFlow(prefs.getBoolean(KEY_SHOW_COLLECTIONS, true))
    val showCollections: StateFlow<Boolean> = _showCollections.asStateFlow()

    fun setShowCollections(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_COLLECTIONS, show).apply()
        _showCollections.value = show
    }

    // Pinned collections - ordered list for display order
    private val _pinnedCollections = MutableStateFlow(loadPinnedCollections())
    val pinnedCollections: StateFlow<List<String>> = _pinnedCollections.asStateFlow()

    private fun loadPinnedCollections(): List<String> {
        val stored = prefs.getString(KEY_PINNED_COLLECTIONS, null) ?: return emptyList()
        return if (stored.isBlank()) emptyList() else stored.split(",")
    }

    fun setPinnedCollections(collectionIds: List<String>) {
        val stored = collectionIds.joinToString(",")
        prefs.edit().putString(KEY_PINNED_COLLECTIONS, stored).apply()
        _pinnedCollections.value = collectionIds
    }

    private val _showSuggestions = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SUGGESTIONS, true))
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    fun setShowSuggestions(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SUGGESTIONS, show).apply()
        _showSuggestions.value = show
    }

    // Seerr Integration Preferences
    private val _seerrEnabled = MutableStateFlow(prefs.getBoolean(KEY_SEERR_ENABLED, false))
    val seerrEnabled: StateFlow<Boolean> = _seerrEnabled.asStateFlow()

    private val _seerrUrl = MutableStateFlow(prefs.getString(KEY_SEERR_URL, null))
    val seerrUrl: StateFlow<String?> = _seerrUrl.asStateFlow()

    private val _seerrAutoDetected = MutableStateFlow(prefs.getBoolean(KEY_SEERR_AUTO_DETECTED, false))
    val seerrAutoDetected: StateFlow<Boolean> = _seerrAutoDetected.asStateFlow()

    private val _seerrApiKey = MutableStateFlow(prefs.getString(KEY_SEERR_API_KEY, null))
    val seerrApiKey: StateFlow<String?> = _seerrApiKey.asStateFlow()

    private val _seerrSessionCookie = MutableStateFlow(prefs.getString(KEY_SEERR_SESSION_COOKIE, null))
    val seerrSessionCookie: StateFlow<String?> = _seerrSessionCookie.asStateFlow()

    fun setSeerrEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEERR_ENABLED, enabled).apply()
        _seerrEnabled.value = enabled
    }

    fun setSeerrUrl(url: String?) {
        prefs.edit().putString(KEY_SEERR_URL, url).apply()
        _seerrUrl.value = url
    }

    fun setSeerrAutoDetected(autoDetected: Boolean) {
        prefs.edit().putBoolean(KEY_SEERR_AUTO_DETECTED, autoDetected).apply()
        _seerrAutoDetected.value = autoDetected
    }

    fun setSeerrApiKey(apiKey: String?) {
        if (apiKey != null) {
            prefs.edit().putString(KEY_SEERR_API_KEY, apiKey).apply()
        } else {
            prefs.edit().remove(KEY_SEERR_API_KEY).apply()
        }
        _seerrApiKey.value = apiKey
    }

    fun setSeerrSessionCookie(cookie: String?) {
        if (cookie != null) {
            prefs.edit().putString(KEY_SEERR_SESSION_COOKIE, cookie).apply()
        } else {
            prefs.edit().remove(KEY_SEERR_SESSION_COOKIE).apply()
        }
        _seerrSessionCookie.value = cookie
    }

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
}
