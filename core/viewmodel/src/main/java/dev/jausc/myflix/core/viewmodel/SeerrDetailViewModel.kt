package dev.jausc.myflix.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.seerr.SeerrIssue
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaInfo
import dev.jausc.myflix.core.seerr.SeerrRatingResponse
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrRequest
import dev.jausc.myflix.core.seerr.SeerrRottenTomatoesRating
import dev.jausc.myflix.core.seerr.SeerrSeason
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for Seerr detail screens (movie/TV details).
 */
data class SeerrDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val movieDetails: SeerrMedia? = null,
    val tvDetails: SeerrMedia? = null,
    val mediaInfo: SeerrMediaInfo? = null,
    val ratings: SeerrRatingResponse? = null,
    val tvRatings: SeerrRottenTomatoesRating? = null,
    val similar: List<SeerrMedia> = emptyList(),
    val recommendations: List<SeerrMedia> = emptyList(),
    val loadedSeasons: Map<Int, SeerrSeason> = emptyMap(),
    val isRequesting: Boolean = false,
    val lastRequest: SeerrRequest? = null,
    val requestError: String? = null,
    val isReportingIssue: Boolean = false,
    val lastIssue: SeerrIssue? = null,
    val issueError: String? = null,
) {
    /** Combined display title from either movie or TV details */
    val displayTitle: String?
        get() = movieDetails?.displayTitle ?: tvDetails?.displayTitle

    /** Whether the media is available */
    val isAvailable: Boolean
        get() = mediaInfo?.isAvailable == true

    /** Whether the media has a pending request */
    val isPending: Boolean
        get() = mediaInfo?.isPending == true
}

/**
 * ViewModel for Seerr detail screens (movie/TV details).
 * Handles loading media details, ratings, similar/recommendations, and request/issue actions.
 */
class SeerrDetailViewModel(
    private val seerrRepository: SeerrRepository,
    private val tmdbId: Int,
    private val mediaType: String,
) : ViewModel() {

    /**
     * Factory for creating SeerrDetailViewModel with manual dependency injection.
     */
    class Factory(
        private val seerrRepository: SeerrRepository,
        private val tmdbId: Int,
        private val mediaType: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeerrDetailViewModel(seerrRepository, tmdbId, mediaType) as T
    }

    private val _uiState = MutableStateFlow(SeerrDetailUiState())
    val uiState: StateFlow<SeerrDetailUiState> = _uiState.asStateFlow()

    /** Expose Radarr servers for request dialogs */
    val radarrServers = seerrRepository.radarrServers

    /** Expose Sonarr servers for request dialogs */
    val sonarrServers = seerrRepository.sonarrServers

    init {
        if (tmdbId > 0) {
            loadDetails()
        }
    }

    // ========================================================================
    // Loading Methods
    // ========================================================================

    /**
     * Main entry point for loading details.
     * Routes to loadMovieDetails() or loadTvDetails() based on mediaType.
     */
    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (mediaType) {
                "movie" -> loadMovieDetails()
                "tv" -> loadTvDetails()
                else -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Unknown media type: $mediaType")
                    }
                }
            }
        }
    }

    /**
     * Load movie details, then ratings/similar/recommendations in parallel.
     */
    private suspend fun loadMovieDetails() {
        seerrRepository.getMovie(tmdbId)
            .onSuccess { movie ->
                _uiState.update {
                    it.copy(
                        movieDetails = movie,
                        mediaInfo = movie.mediaInfo,
                        isLoading = false,
                    )
                }

                // Load supplementary data in parallel
                viewModelScope.launch {
                    val ratingsDeferred = async { loadMovieRatings() }
                    val similarDeferred = async { loadSimilarMovies() }
                    val recommendationsDeferred = async { loadMovieRecommendations() }

                    ratingsDeferred.await()
                    similarDeferred.await()
                    recommendationsDeferred.await()
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load movie details",
                    )
                }
            }
    }

    /**
     * Load TV show details, then ratings/similar/recommendations in parallel.
     */
    private suspend fun loadTvDetails() {
        seerrRepository.getTVShow(tmdbId)
            .onSuccess { tvShow ->
                _uiState.update {
                    it.copy(
                        tvDetails = tvShow,
                        mediaInfo = tvShow.mediaInfo,
                        isLoading = false,
                    )
                }

                // Load supplementary data in parallel
                viewModelScope.launch {
                    val ratingsDeferred = async { loadTvRatings() }
                    val similarDeferred = async { loadSimilarTv() }
                    val recommendationsDeferred = async { loadTvRecommendations() }

                    ratingsDeferred.await()
                    similarDeferred.await()
                    recommendationsDeferred.await()
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load TV show details",
                    )
                }
            }
    }

    /**
     * Load a specific season for TV shows.
     */
    fun loadSeason(seasonNumber: Int) {
        // Skip if already loaded
        if (_uiState.value.loadedSeasons.containsKey(seasonNumber)) {
            return
        }

        viewModelScope.launch {
            seerrRepository.getTVSeason(tmdbId, seasonNumber)
                .onSuccess { season ->
                    _uiState.update { state ->
                        state.copy(
                            loadedSeasons = state.loadedSeasons + (seasonNumber to season),
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(
                        "SeerrDetailViewModel",
                        "Failed to load season $seasonNumber: ${error.message}",
                    )
                }
        }
    }

    /**
     * Load movie ratings (Rotten Tomatoes + IMDB).
     */
    private suspend fun loadMovieRatings() {
        seerrRepository.getMovieRatings(tmdbId)
            .onSuccess { ratings ->
                _uiState.update { it.copy(ratings = ratings) }
            }
            .onFailure { error ->
                Log.w(
                    "SeerrDetailViewModel",
                    "Failed to load movie ratings: ${error.message}",
                )
            }
    }

    /**
     * Load TV show ratings (Rotten Tomatoes).
     */
    private suspend fun loadTvRatings() {
        seerrRepository.getTVRatings(tmdbId)
            .onSuccess { ratings ->
                _uiState.update { it.copy(tvRatings = ratings) }
            }
            .onFailure { error ->
                Log.w(
                    "SeerrDetailViewModel",
                    "Failed to load TV ratings: ${error.message}",
                )
            }
    }

    /**
     * Load similar movies.
     */
    private suspend fun loadSimilarMovies() {
        seerrRepository.getSimilarMovies(tmdbId)
            .onSuccess { result ->
                _uiState.update { it.copy(similar = result.results) }
            }
            .onFailure { error ->
                Log.w(
                    "SeerrDetailViewModel",
                    "Failed to load similar movies: ${error.message}",
                )
            }
    }

    /**
     * Load similar TV shows.
     */
    private suspend fun loadSimilarTv() {
        seerrRepository.getSimilarTV(tmdbId)
            .onSuccess { result ->
                _uiState.update { it.copy(similar = result.results) }
            }
            .onFailure { error ->
                Log.w(
                    "SeerrDetailViewModel",
                    "Failed to load similar TV shows: ${error.message}",
                )
            }
    }

    /**
     * Load movie recommendations.
     */
    private suspend fun loadMovieRecommendations() {
        seerrRepository.getMovieRecommendations(tmdbId)
            .onSuccess { result ->
                _uiState.update { it.copy(recommendations = result.results) }
            }
            .onFailure { error ->
                Log.w(
                    "SeerrDetailViewModel",
                    "Failed to load movie recommendations: ${error.message}",
                )
            }
    }

    /**
     * Load TV show recommendations.
     */
    private suspend fun loadTvRecommendations() {
        seerrRepository.getTVRecommendations(tmdbId)
            .onSuccess { result ->
                _uiState.update { it.copy(recommendations = result.results) }
            }
            .onFailure { error ->
                Log.w(
                    "SeerrDetailViewModel",
                    "Failed to load TV recommendations: ${error.message}",
                )
            }
    }

    // ========================================================================
    // Request Methods
    // ========================================================================

    /**
     * Request a movie.
     *
     * @param is4k Whether to request 4K version
     */
    fun requestMovie(is4k: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true, requestError = null) }

            seerrRepository.requestMovie(tmdbId, is4k)
                .onSuccess { request ->
                    _uiState.update {
                        it.copy(
                            isRequesting = false,
                            lastRequest = request,
                        )
                    }
                    // Refresh details to get updated mediaInfo
                    loadDetails()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRequesting = false,
                            requestError = error.message ?: "Failed to request movie",
                        )
                    }
                }
        }
    }

    /**
     * Request a TV show.
     *
     * @param is4k Whether to request 4K version
     * @param seasons Optional list of specific season numbers to request
     */
    fun requestTv(is4k: Boolean = false, seasons: List<Int>? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true, requestError = null) }

            seerrRepository.requestTVShow(tmdbId, seasons, is4k)
                .onSuccess { request ->
                    _uiState.update {
                        it.copy(
                            isRequesting = false,
                            lastRequest = request,
                        )
                    }
                    // Refresh details to get updated mediaInfo
                    loadDetails()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRequesting = false,
                            requestError = error.message ?: "Failed to request TV show",
                        )
                    }
                }
        }
    }

    /**
     * Clear request error state.
     */
    fun clearRequestError() {
        _uiState.update { it.copy(requestError = null) }
    }

    // ========================================================================
    // Issue Methods
    // ========================================================================

    /**
     * Report an issue with the media.
     *
     * @param issueType Issue type (1=Video, 2=Audio, 3=Subtitle, 4=Other)
     * @param message Description of the issue
     * @param problemSeason Season number for TV episode issues
     * @param problemEpisode Episode number for TV episode issues
     */
    fun reportIssue(
        issueType: Int,
        message: String,
        problemSeason: Int? = null,
        problemEpisode: Int? = null,
    ) {
        val mediaId = _uiState.value.mediaInfo?.id
        if (mediaId == null) {
            _uiState.update {
                it.copy(issueError = "Cannot report issue: media not found in library")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isReportingIssue = true, issueError = null) }

            seerrRepository.createIssue(
                mediaId = mediaId,
                issueType = issueType,
                message = message,
                problemSeason = problemSeason,
                problemEpisode = problemEpisode,
            )
                .onSuccess { issue ->
                    _uiState.update {
                        it.copy(
                            isReportingIssue = false,
                            lastIssue = issue,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isReportingIssue = false,
                            issueError = error.message ?: "Failed to report issue",
                        )
                    }
                }
        }
    }

    /**
     * Clear issue error state.
     */
    fun clearIssueError() {
        _uiState.update { it.copy(issueError = null) }
    }
}
