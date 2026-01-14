package dev.jausc.myflix.core.common.preferences

import android.content.Context
import android.content.SharedPreferences
import dev.jausc.myflix.core.common.model.LibraryFilterState
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.LibraryViewMode
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.core.common.model.WatchedFilter
import dev.jausc.myflix.core.common.model.YearRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstract base class for app preferences.
 * Provides shared preference management with reactive StateFlow updates.
 *
 * Subclasses must provide the preferences name via [preferencesName].
 */
abstract class AppPreferences(context: Context) {

    /**
     * The name used for SharedPreferences storage.
     * Each platform should use a unique name (e.g., "myflix_tv_prefs", "myflix_mobile_prefs").
     */
    protected abstract val preferencesName: String

    protected val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    // Playback Preferences
    private val _hideWatchedFromRecent: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.HIDE_WATCHED_FROM_RECENT, PreferenceKeys.Defaults.HIDE_WATCHED_FROM_RECENT))
    }
    val hideWatchedFromRecent: StateFlow<Boolean> by lazy { _hideWatchedFromRecent.asStateFlow() }

    private val _useMpvPlayer: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.USE_MPV_PLAYER, PreferenceKeys.Defaults.USE_MPV_PLAYER))
    }
    val useMpvPlayer: StateFlow<Boolean> by lazy { _useMpvPlayer.asStateFlow() }

    private val _useTrailerFallback: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.USE_TRAILER_FALLBACK, PreferenceKeys.Defaults.USE_TRAILER_FALLBACK))
    }
    val useTrailerFallback: StateFlow<Boolean> by lazy { _useTrailerFallback.asStateFlow() }

    // Home Screen Row Preferences
    private val _showSeasonPremieres: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.SHOW_SEASON_PREMIERES, PreferenceKeys.Defaults.SHOW_SEASON_PREMIERES))
    }
    val showSeasonPremieres: StateFlow<Boolean> by lazy { _showSeasonPremieres.asStateFlow() }

    private val _showGenreRows: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.SHOW_GENRE_ROWS, PreferenceKeys.Defaults.SHOW_GENRE_ROWS))
    }
    val showGenreRows: StateFlow<Boolean> by lazy { _showGenreRows.asStateFlow() }

    private val _enabledGenres: MutableStateFlow<List<String>> by lazy {
        MutableStateFlow(loadStringList(PreferenceKeys.Prefs.ENABLED_GENRES))
    }
    val enabledGenres: StateFlow<List<String>> by lazy { _enabledGenres.asStateFlow() }

    private val _showCollections: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.SHOW_COLLECTIONS, PreferenceKeys.Defaults.SHOW_COLLECTIONS))
    }
    val showCollections: StateFlow<Boolean> by lazy { _showCollections.asStateFlow() }

    private val _pinnedCollections: MutableStateFlow<List<String>> by lazy {
        MutableStateFlow(loadStringList(PreferenceKeys.Prefs.PINNED_COLLECTIONS))
    }
    val pinnedCollections: StateFlow<List<String>> by lazy { _pinnedCollections.asStateFlow() }

    private val _showSuggestions: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.SHOW_SUGGESTIONS, PreferenceKeys.Defaults.SHOW_SUGGESTIONS))
    }
    val showSuggestions: StateFlow<Boolean> by lazy { _showSuggestions.asStateFlow() }

    private val _showSeerrRecentRequests: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.SHOW_SEERR_RECENT_REQUESTS, PreferenceKeys.Defaults.SHOW_SEERR_RECENT_REQUESTS))
    }
    val showSeerrRecentRequests: StateFlow<Boolean> by lazy { _showSeerrRecentRequests.asStateFlow() }

    // Seerr Integration Preferences
    private val _seerrEnabled: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.SEERR_ENABLED, PreferenceKeys.Defaults.SEERR_ENABLED))
    }
    val seerrEnabled: StateFlow<Boolean> by lazy { _seerrEnabled.asStateFlow() }

    private val _seerrUrl: MutableStateFlow<String?> by lazy {
        MutableStateFlow(prefs.getString(PreferenceKeys.Prefs.SEERR_URL, null))
    }
    val seerrUrl: StateFlow<String?> by lazy { _seerrUrl.asStateFlow() }

    private val _seerrAutoDetected: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.SEERR_AUTO_DETECTED, PreferenceKeys.Defaults.SEERR_AUTO_DETECTED))
    }
    val seerrAutoDetected: StateFlow<Boolean> by lazy { _seerrAutoDetected.asStateFlow() }

    private val _seerrApiKey: MutableStateFlow<String?> by lazy {
        MutableStateFlow(prefs.getString(PreferenceKeys.Prefs.SEERR_API_KEY, null))
    }
    val seerrApiKey: StateFlow<String?> by lazy { _seerrApiKey.asStateFlow() }

    private val _seerrSessionCookie: MutableStateFlow<String?> by lazy {
        MutableStateFlow(prefs.getString(PreferenceKeys.Prefs.SEERR_SESSION_COOKIE, null))
    }
    val seerrSessionCookie: StateFlow<String?> by lazy { _seerrSessionCookie.asStateFlow() }

    // Helper methods
    private fun loadStringList(key: String): List<String> {
        val stored = prefs.getString(key, null) ?: return emptyList()
        return if (stored.isBlank()) emptyList() else stored.split(",")
    }

    private fun saveStringList(key: String, list: List<String>) {
        prefs.edit().putString(key, list.joinToString(",")).apply()
    }

    // Playback setters

    /**
     * Set whether to hide watched items from Recently Added rows.
     */
    fun setHideWatchedFromRecent(hide: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.HIDE_WATCHED_FROM_RECENT, hide).apply()
        _hideWatchedFromRecent.value = hide
    }

    /**
     * Set whether to use MPV player instead of ExoPlayer.
     * MPV offers better codec support but may have compatibility issues.
     */
    fun setUseMpvPlayer(useMpv: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.USE_MPV_PLAYER, useMpv).apply()
        _useMpvPlayer.value = useMpv
    }

    /**
     * Set whether to use the WebView fallback for Seerr trailers.
     */
    fun setUseTrailerFallback(useFallback: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.USE_TRAILER_FALLBACK, useFallback).apply()
        _useTrailerFallback.value = useFallback
    }

    // Home screen row setters

    /**
     * Set whether to show upcoming episodes (Season Premieres) row.
     */
    fun setShowSeasonPremieres(show: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.SHOW_SEASON_PREMIERES, show).apply()
        _showSeasonPremieres.value = show
    }

    /**
     * Set whether to show genre-based content rows.
     */
    fun setShowGenreRows(show: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.SHOW_GENRE_ROWS, show).apply()
        _showGenreRows.value = show
    }

    /**
     * Set which genres to display as rows on the home screen (ordered).
     */
    fun setEnabledGenres(genres: List<String>) {
        saveStringList(PreferenceKeys.Prefs.ENABLED_GENRES, genres)
        _enabledGenres.value = genres
    }

    /**
     * Set whether to show collections row.
     */
    fun setShowCollections(show: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.SHOW_COLLECTIONS, show).apply()
        _showCollections.value = show
    }

    /**
     * Set which collections to pin on the home screen (ordered).
     */
    fun setPinnedCollections(collectionIds: List<String>) {
        saveStringList(PreferenceKeys.Prefs.PINNED_COLLECTIONS, collectionIds)
        _pinnedCollections.value = collectionIds
    }

    /**
     * Set whether to show suggestions row.
     */
    fun setShowSuggestions(show: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.SHOW_SUGGESTIONS, show).apply()
        _showSuggestions.value = show
    }

    /**
     * Set whether to show recent requests row on Seerr home (Discover).
     */
    fun setShowSeerrRecentRequests(show: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.SHOW_SEERR_RECENT_REQUESTS, show).apply()
        _showSeerrRecentRequests.value = show
    }

    // Seerr setters

    /**
     * Set whether Seerr integration is enabled.
     */
    fun setSeerrEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.SEERR_ENABLED, enabled).apply()
        _seerrEnabled.value = enabled
    }

    /**
     * Set the Seerr server URL.
     */
    fun setSeerrUrl(url: String?) {
        prefs.edit().putString(PreferenceKeys.Prefs.SEERR_URL, url).apply()
        _seerrUrl.value = url
    }

    /**
     * Set whether Seerr was auto-detected (vs manually configured).
     */
    fun setSeerrAutoDetected(autoDetected: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.SEERR_AUTO_DETECTED, autoDetected).apply()
        _seerrAutoDetected.value = autoDetected
    }

    /**
     * Set the Seerr API key for persistent authentication.
     */
    fun setSeerrApiKey(apiKey: String?) {
        if (apiKey != null) {
            prefs.edit().putString(PreferenceKeys.Prefs.SEERR_API_KEY, apiKey).apply()
        } else {
            prefs.edit().remove(PreferenceKeys.Prefs.SEERR_API_KEY).apply()
        }
        _seerrApiKey.value = apiKey
    }

    /**
     * Set the Seerr session cookie for persistent authentication.
     */
    fun setSeerrSessionCookie(cookie: String?) {
        if (cookie != null) {
            prefs.edit().putString(PreferenceKeys.Prefs.SEERR_SESSION_COOKIE, cookie).apply()
        } else {
            prefs.edit().remove(PreferenceKeys.Prefs.SEERR_SESSION_COOKIE).apply()
        }
        _seerrSessionCookie.value = cookie
    }

    /**
     * Clear all Seerr configuration.
     */
    fun clearSeerrConfig() {
        prefs.edit()
            .remove(PreferenceKeys.Prefs.SEERR_ENABLED)
            .remove(PreferenceKeys.Prefs.SEERR_URL)
            .remove(PreferenceKeys.Prefs.SEERR_AUTO_DETECTED)
            .remove(PreferenceKeys.Prefs.SEERR_API_KEY)
            .remove(PreferenceKeys.Prefs.SEERR_SESSION_COOKIE)
            .apply()
        _seerrEnabled.value = false
        _seerrUrl.value = null
        _seerrAutoDetected.value = false
        _seerrApiKey.value = null
        _seerrSessionCookie.value = null
    }

    // ==================== Library Filter Preferences ====================

    /**
     * Get the saved filter state for a specific library.
     * Returns default state if no preferences are saved.
     */
    fun getLibraryFilterState(libraryId: String): LibraryFilterState {
        val sortByValue = prefs.getString(
            PreferenceKeys.Prefs.LIBRARY_SORT_BY_PREFIX + libraryId,
            LibrarySortOption.TITLE.jellyfinValue,
        ) ?: LibrarySortOption.TITLE.jellyfinValue

        val sortOrderValue = prefs.getString(
            PreferenceKeys.Prefs.LIBRARY_SORT_ORDER_PREFIX + libraryId,
            SortOrder.ASCENDING.jellyfinValue,
        ) ?: SortOrder.ASCENDING.jellyfinValue

        val viewModeValue = prefs.getString(
            PreferenceKeys.Prefs.LIBRARY_VIEW_MODE_PREFIX + libraryId,
            LibraryViewMode.POSTER.name,
        ) ?: LibraryViewMode.POSTER.name

        val watchedFilterValue = prefs.getString(
            PreferenceKeys.Prefs.LIBRARY_WATCHED_FILTER_PREFIX + libraryId,
            WatchedFilter.ALL.name,
        ) ?: WatchedFilter.ALL.name

        val genresString = prefs.getString(
            PreferenceKeys.Prefs.LIBRARY_GENRES_PREFIX + libraryId,
            null,
        )
        val selectedGenres = if (genresString.isNullOrBlank()) {
            emptySet()
        } else {
            genresString.split(",").toSet()
        }

        val yearFrom = prefs.getInt(
            PreferenceKeys.Prefs.LIBRARY_YEAR_FROM_PREFIX + libraryId,
            -1,
        ).takeIf { it > 0 }

        val yearTo = prefs.getInt(
            PreferenceKeys.Prefs.LIBRARY_YEAR_TO_PREFIX + libraryId,
            -1,
        ).takeIf { it > 0 }

        val ratingFilter = prefs.getFloat(
            PreferenceKeys.Prefs.LIBRARY_RATING_PREFIX + libraryId,
            -1f,
        ).takeIf { it > 0 }

        val parentalRatingsString = prefs.getString(
            PreferenceKeys.Prefs.LIBRARY_PARENTAL_RATINGS_PREFIX + libraryId,
            null,
        )
        val selectedParentalRatings = if (parentalRatingsString.isNullOrBlank()) {
            emptySet()
        } else {
            parentalRatingsString.split(",").toSet()
        }

        return LibraryFilterState(
            sortBy = LibrarySortOption.fromJellyfinValue(sortByValue),
            sortOrder = SortOrder.fromJellyfinValue(sortOrderValue),
            viewMode = LibraryViewMode.fromString(viewModeValue),
            watchedFilter = WatchedFilter.fromString(watchedFilterValue),
            selectedGenres = selectedGenres,
            selectedParentalRatings = selectedParentalRatings,
            yearRange = YearRange(from = yearFrom, to = yearTo),
            ratingFilter = ratingFilter,
        )
    }

    /**
     * Save the filter state for a specific library.
     */
    fun setLibraryFilterState(libraryId: String, state: LibraryFilterState) {
        prefs.edit()
            .putString(
                PreferenceKeys.Prefs.LIBRARY_SORT_BY_PREFIX + libraryId,
                state.sortBy.jellyfinValue,
            )
            .putString(
                PreferenceKeys.Prefs.LIBRARY_SORT_ORDER_PREFIX + libraryId,
                state.sortOrder.jellyfinValue,
            )
            .putString(
                PreferenceKeys.Prefs.LIBRARY_VIEW_MODE_PREFIX + libraryId,
                state.viewMode.name,
            )
            .putString(
                PreferenceKeys.Prefs.LIBRARY_WATCHED_FILTER_PREFIX + libraryId,
                state.watchedFilter.name,
            )
            .putString(
                PreferenceKeys.Prefs.LIBRARY_GENRES_PREFIX + libraryId,
                state.selectedGenres.joinToString(","),
            )
            .putString(
                PreferenceKeys.Prefs.LIBRARY_PARENTAL_RATINGS_PREFIX + libraryId,
                state.selectedParentalRatings.joinToString(","),
            )
            .apply()

        // Save year range (use -1 as "not set" sentinel)
        prefs.edit()
            .putInt(
                PreferenceKeys.Prefs.LIBRARY_YEAR_FROM_PREFIX + libraryId,
                state.yearRange.from ?: -1,
            )
            .putInt(
                PreferenceKeys.Prefs.LIBRARY_YEAR_TO_PREFIX + libraryId,
                state.yearRange.to ?: -1,
            )
            .apply()

        // Save rating filter (use -1 as "not set" sentinel)
        prefs.edit()
            .putFloat(
                PreferenceKeys.Prefs.LIBRARY_RATING_PREFIX + libraryId,
                state.ratingFilter ?: -1f,
            )
            .apply()
    }

    /**
     * Clear all filter preferences for a specific library, resetting to defaults.
     */
    fun clearLibraryFilterState(libraryId: String) {
        prefs.edit()
            .remove(PreferenceKeys.Prefs.LIBRARY_SORT_BY_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_SORT_ORDER_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_VIEW_MODE_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_WATCHED_FILTER_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_GENRES_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_YEAR_FROM_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_YEAR_TO_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_RATING_PREFIX + libraryId)
            .remove(PreferenceKeys.Prefs.LIBRARY_PARENTAL_RATINGS_PREFIX + libraryId)
            .apply()
    }
}
