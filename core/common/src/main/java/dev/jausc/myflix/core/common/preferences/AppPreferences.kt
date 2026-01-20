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

    private val _preferredAudioLanguage: MutableStateFlow<String?> by lazy {
        MutableStateFlow(prefs.getString(PreferenceKeys.Prefs.PREFERRED_AUDIO_LANGUAGE, PreferenceKeys.Defaults.PREFERRED_AUDIO_LANGUAGE))
    }
    val preferredAudioLanguage: StateFlow<String?> by lazy { _preferredAudioLanguage.asStateFlow() }

    private val _preferredSubtitleLanguage: MutableStateFlow<String?> by lazy {
        MutableStateFlow(prefs.getString(PreferenceKeys.Prefs.PREFERRED_SUBTITLE_LANGUAGE, PreferenceKeys.Defaults.PREFERRED_SUBTITLE_LANGUAGE))
    }
    val preferredSubtitleLanguage: StateFlow<String?> by lazy { _preferredSubtitleLanguage.asStateFlow() }

    private val _maxStreamingBitrate: MutableStateFlow<Int> by lazy {
        MutableStateFlow(prefs.getInt(PreferenceKeys.Prefs.MAX_STREAMING_BITRATE, PreferenceKeys.Defaults.MAX_STREAMING_BITRATE))
    }
    /** Max streaming bitrate in Mbps. 0 = unlimited (direct play preferred). */
    val maxStreamingBitrate: StateFlow<Int> by lazy { _maxStreamingBitrate.asStateFlow() }

    private val _skipDurationSeconds: MutableStateFlow<Int> by lazy {
        MutableStateFlow(prefs.getInt(PreferenceKeys.Prefs.SKIP_DURATION_SECONDS, PreferenceKeys.Defaults.SKIP_DURATION_SECONDS))
    }
    /** Skip forward/backward duration in seconds. */
    val skipDurationSeconds: StateFlow<Int> by lazy { _skipDurationSeconds.asStateFlow() }

    private val _playerDisplayMode: MutableStateFlow<String> by lazy {
        MutableStateFlow(
            prefs.getString(PreferenceKeys.Prefs.PLAYER_DISPLAY_MODE, PreferenceKeys.Defaults.PLAYER_DISPLAY_MODE)
                ?: PreferenceKeys.Defaults.PLAYER_DISPLAY_MODE
        )
    }
    val playerDisplayMode: StateFlow<String> by lazy { _playerDisplayMode.asStateFlow() }

    // Media Segment Preferences (skip intro/credits)
    // Values: "OFF" (disabled), "ASK" (show button), "AUTO" (skip automatically)
    private val _skipIntroMode: MutableStateFlow<String> by lazy {
        MutableStateFlow(
            prefs.getString(PreferenceKeys.Prefs.SKIP_INTRO_MODE, PreferenceKeys.Defaults.SKIP_INTRO_MODE)
                ?: PreferenceKeys.Defaults.SKIP_INTRO_MODE
        )
    }
    /** Skip intro mode: OFF (disabled), ASK (show button), AUTO (skip automatically). */
    val skipIntroMode: StateFlow<String> by lazy { _skipIntroMode.asStateFlow() }

    private val _skipCreditsMode: MutableStateFlow<String> by lazy {
        MutableStateFlow(
            prefs.getString(PreferenceKeys.Prefs.SKIP_CREDITS_MODE, PreferenceKeys.Defaults.SKIP_CREDITS_MODE)
                ?: PreferenceKeys.Defaults.SKIP_CREDITS_MODE
        )
    }
    /** Skip credits mode: OFF (disabled), ASK (show button), AUTO (skip automatically). */
    val skipCreditsMode: StateFlow<String> by lazy { _skipCreditsMode.asStateFlow() }

    // Subtitle Styling Preferences
    private val _subtitleFontSize: MutableStateFlow<String> by lazy {
        MutableStateFlow(prefs.getString(PreferenceKeys.Prefs.SUBTITLE_FONT_SIZE, PreferenceKeys.Defaults.SUBTITLE_FONT_SIZE) ?: PreferenceKeys.Defaults.SUBTITLE_FONT_SIZE)
    }
    val subtitleFontSize: StateFlow<String> by lazy { _subtitleFontSize.asStateFlow() }

    private val _subtitleFontColor: MutableStateFlow<String> by lazy {
        MutableStateFlow(prefs.getString(PreferenceKeys.Prefs.SUBTITLE_FONT_COLOR, PreferenceKeys.Defaults.SUBTITLE_FONT_COLOR) ?: PreferenceKeys.Defaults.SUBTITLE_FONT_COLOR)
    }
    val subtitleFontColor: StateFlow<String> by lazy { _subtitleFontColor.asStateFlow() }

    private val _subtitleBackgroundOpacity: MutableStateFlow<Int> by lazy {
        MutableStateFlow(prefs.getInt(PreferenceKeys.Prefs.SUBTITLE_BACKGROUND_OPACITY, PreferenceKeys.Defaults.SUBTITLE_BACKGROUND_OPACITY))
    }
    val subtitleBackgroundOpacity: StateFlow<Int> by lazy { _subtitleBackgroundOpacity.asStateFlow() }

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

    private val _universesEnabled: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.UNIVERSES_ENABLED, PreferenceKeys.Defaults.UNIVERSES_ENABLED))
    }
    val universesEnabled: StateFlow<Boolean> by lazy { _universesEnabled.asStateFlow() }

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

    // First-run tip preferences
    private val _hasSeenNavBarTip: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.Prefs.HAS_SEEN_NAV_BAR_TIP, PreferenceKeys.Defaults.HAS_SEEN_NAV_BAR_TIP))
    }
    val hasSeenNavBarTip: StateFlow<Boolean> by lazy { _hasSeenNavBarTip.asStateFlow() }

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

    /**
     * Set the preferred audio language (ISO 639-2/B code like "eng", "jpn", "spa").
     * When set, the player will automatically select audio tracks matching this language.
     * Set to null to use Jellyfin's default audio track.
     */
    fun setPreferredAudioLanguage(language: String?) {
        if (language != null) {
            prefs.edit().putString(PreferenceKeys.Prefs.PREFERRED_AUDIO_LANGUAGE, language).apply()
        } else {
            prefs.edit().remove(PreferenceKeys.Prefs.PREFERRED_AUDIO_LANGUAGE).apply()
        }
        _preferredAudioLanguage.value = language
    }

    /**
     * Set the preferred subtitle language (ISO 639-2/B code like "eng", "jpn", "spa").
     * When set, the player will automatically select subtitle tracks matching this language.
     * Set to null to use Jellyfin's default subtitle track.
     */
    fun setPreferredSubtitleLanguage(language: String?) {
        if (language != null) {
            prefs.edit().putString(PreferenceKeys.Prefs.PREFERRED_SUBTITLE_LANGUAGE, language).apply()
        } else {
            prefs.edit().remove(PreferenceKeys.Prefs.PREFERRED_SUBTITLE_LANGUAGE).apply()
        }
        _preferredSubtitleLanguage.value = language
    }

    /**
     * Set the max streaming bitrate in Mbps.
     * Set to 0 for unlimited (prefer direct play).
     */
    fun setMaxStreamingBitrate(bitrateMbps: Int) {
        prefs.edit().putInt(PreferenceKeys.Prefs.MAX_STREAMING_BITRATE, bitrateMbps).apply()
        _maxStreamingBitrate.value = bitrateMbps
    }

    /**
     * Set the skip forward/backward duration in seconds.
     */
    fun setSkipDurationSeconds(seconds: Int) {
        prefs.edit().putInt(PreferenceKeys.Prefs.SKIP_DURATION_SECONDS, seconds).apply()
        _skipDurationSeconds.value = seconds
    }

    /**
     * Set the player display mode (video scaling).
     * @param mode PlayerDisplayMode enum name (FIT, FILL, ZOOM, STRETCH)
     */
    fun setPlayerDisplayMode(mode: String) {
        prefs.edit().putString(PreferenceKeys.Prefs.PLAYER_DISPLAY_MODE, mode).apply()
        _playerDisplayMode.value = mode
    }

    // Media segment setters

    /**
     * Set the skip intro behavior.
     * @param mode "OFF" (disabled), "ASK" (show button), "AUTO" (skip automatically)
     */
    fun setSkipIntroMode(mode: String) {
        prefs.edit().putString(PreferenceKeys.Prefs.SKIP_INTRO_MODE, mode).apply()
        _skipIntroMode.value = mode
    }

    /**
     * Set the skip credits behavior.
     * @param mode "OFF" (disabled), "ASK" (show button), "AUTO" (skip automatically)
     */
    fun setSkipCreditsMode(mode: String) {
        prefs.edit().putString(PreferenceKeys.Prefs.SKIP_CREDITS_MODE, mode).apply()
        _skipCreditsMode.value = mode
    }

    // Subtitle styling setters

    /**
     * Set the subtitle font size.
     * @param size SubtitleFontSize enum name (SMALL, MEDIUM, LARGE, EXTRA_LARGE)
     */
    fun setSubtitleFontSize(size: String) {
        prefs.edit().putString(PreferenceKeys.Prefs.SUBTITLE_FONT_SIZE, size).apply()
        _subtitleFontSize.value = size
    }

    /**
     * Set the subtitle font color.
     * @param color SubtitleColor enum name (WHITE, YELLOW, GREEN, CYAN, BLUE, MAGENTA)
     */
    fun setSubtitleFontColor(color: String) {
        prefs.edit().putString(PreferenceKeys.Prefs.SUBTITLE_FONT_COLOR, color).apply()
        _subtitleFontColor.value = color
    }

    /**
     * Set the subtitle background opacity (0-100).
     */
    fun setSubtitleBackgroundOpacity(opacity: Int) {
        val clampedOpacity = opacity.coerceIn(0, 100)
        prefs.edit().putInt(PreferenceKeys.Prefs.SUBTITLE_BACKGROUND_OPACITY, clampedOpacity).apply()
        _subtitleBackgroundOpacity.value = clampedOpacity
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

    /**
     * Set whether Universe Collections feature is enabled.
     * When enabled: shows Universes nav item, filters universe-tagged collections from Collections.
     */
    fun setUniversesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.UNIVERSES_ENABLED, enabled).apply()
        _universesEnabled.value = enabled
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

    // First-run tip setters

    /**
     * Set whether the user has seen the nav bar tip.
     */
    fun setHasSeenNavBarTip(seen: Boolean) {
        prefs.edit().putBoolean(PreferenceKeys.Prefs.HAS_SEEN_NAV_BAR_TIP, seen).apply()
        _hasSeenNavBarTip.value = seen
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
