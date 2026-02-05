package dev.jausc.myflix.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Filter options for requests list.
 */
enum class RequestFilter(val apiValue: String) {
    ALL("all"),
    PENDING("pending"),
    APPROVED("approved"),
    AVAILABLE("available"),
    PROCESSING("processing"),
}

/**
 * UI state for Seerr requests screen.
 */
data class SeerrRequestsUiState(
    val isLoading: Boolean = true,
    val myRequests: List<SeerrRequest> = emptyList(),
    val allRequests: List<SeerrRequest> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val filter: RequestFilter = RequestFilter.ALL,
    val currentPage: Int = 1,
    val hasMoreMyRequests: Boolean = false,
    val hasMoreAllRequests: Boolean = false,
    val isApproving: Boolean = false,
    val isDeclining: Boolean = false,
    val isCancelling: Boolean = false,
    val actionError: String? = null,
    val actionSuccess: String? = null,
)

/**
 * ViewModel for Seerr requests management.
 * Handles loading user's requests and all requests (for admins),
 * as well as request actions (approve, decline, cancel).
 */
class SeerrRequestsViewModel(
    private val seerrRepository: SeerrRepository,
) : ViewModel() {

    /**
     * Factory for creating SeerrRequestsViewModel with manual dependency injection.
     */
    class Factory(
        private val seerrRepository: SeerrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeerrRequestsViewModel(seerrRepository) as T
    }

    private val _uiState = MutableStateFlow(SeerrRequestsUiState())
    val uiState: StateFlow<SeerrRequestsUiState> = _uiState.asStateFlow()

    /** Current authenticated user (for checking admin permissions) */
    val currentUser = seerrRepository.currentUser

    companion object {
        private const val TAG = "SeerrRequestsViewModel"
        private const val PAGE_SIZE = 20
    }

    init {
        loadMyRequests()
    }

    /**
     * Load current user's requests.
     */
    fun loadMyRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filter = _uiState.value.filter
            seerrRepository.getMyRequests(
                page = 1,
                pageSize = PAGE_SIZE,
                filter = filter.apiValue,
            )
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            myRequests = result.results,
                            currentPage = 1,
                            hasMoreMyRequests = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load my requests: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load requests",
                        )
                    }
                }
        }
    }

    /**
     * Load more of current user's requests (pagination).
     */
    fun loadMoreMyRequests() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMoreMyRequests) {
            return
        }

        val nextPage = state.currentPage + 1

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            seerrRepository.getMyRequests(
                page = nextPage,
                pageSize = PAGE_SIZE,
                filter = state.filter.apiValue,
            )
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            myRequests = it.myRequests + result.results,
                            currentPage = nextPage,
                            hasMoreMyRequests = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load more my requests: ${error.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    /**
     * Load all requests (admin only).
     */
    fun loadAllRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filter = _uiState.value.filter
            seerrRepository.getAllRequests(
                page = 1,
                pageSize = PAGE_SIZE,
                filter = filter.apiValue,
            )
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allRequests = result.results,
                            currentPage = 1,
                            hasMoreAllRequests = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load all requests: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load requests",
                        )
                    }
                }
        }
    }

    /**
     * Load more of all requests (admin, pagination).
     */
    fun loadMoreAllRequests() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMoreAllRequests) {
            return
        }

        val nextPage = state.currentPage + 1

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            seerrRepository.getAllRequests(
                page = nextPage,
                pageSize = PAGE_SIZE,
                filter = state.filter.apiValue,
            )
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allRequests = it.allRequests + result.results,
                            currentPage = nextPage,
                            hasMoreAllRequests = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load more all requests: ${error.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    /**
     * Cancel a request.
     *
     * @param requestId ID of the request to cancel
     */
    fun cancelRequest(requestId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, actionError = null) }

            seerrRepository.cancelRequest(requestId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            actionSuccess = "Request cancelled",
                            myRequests = it.myRequests.filter { req -> req.id != requestId },
                            allRequests = it.allRequests.filter { req -> req.id != requestId },
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to cancel request: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            actionError = error.message ?: "Failed to cancel request",
                        )
                    }
                }
        }
    }

    /**
     * Approve a request (admin only).
     *
     * @param requestId ID of the request to approve
     */
    fun approveRequest(requestId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApproving = true, actionError = null) }

            seerrRepository.approveRequest(requestId)
                .onSuccess { updatedRequest ->
                    _uiState.update {
                        it.copy(
                            isApproving = false,
                            actionSuccess = "Request approved",
                            myRequests = it.myRequests.map { req ->
                                if (req.id == requestId) updatedRequest else req
                            },
                            allRequests = it.allRequests.map { req ->
                                if (req.id == requestId) updatedRequest else req
                            },
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to approve request: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isApproving = false,
                            actionError = error.message ?: "Failed to approve request",
                        )
                    }
                }
        }
    }

    /**
     * Decline a request (admin only).
     *
     * @param requestId ID of the request to decline
     */
    fun declineRequest(requestId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeclining = true, actionError = null) }

            seerrRepository.declineRequest(requestId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeclining = false,
                            actionSuccess = "Request declined",
                            myRequests = it.myRequests.filter { req -> req.id != requestId },
                            allRequests = it.allRequests.filter { req -> req.id != requestId },
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to decline request: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isDeclining = false,
                            actionError = error.message ?: "Failed to decline request",
                        )
                    }
                }
        }
    }

    /**
     * Set the filter for requests list.
     *
     * @param filter New filter to apply
     */
    fun setFilter(filter: RequestFilter) {
        if (_uiState.value.filter == filter) return

        _uiState.update { it.copy(filter = filter) }
        loadMyRequests()
    }

    /**
     * Refresh requests lists.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val filter = _uiState.value.filter
            seerrRepository.getMyRequests(
                page = 1,
                pageSize = PAGE_SIZE,
                filter = filter.apiValue,
            )
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            myRequests = result.results,
                            currentPage = 1,
                            hasMoreMyRequests = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to refresh my requests: ${error.message}")
                    _uiState.update { it.copy(isRefreshing = false) }
                }
        }
    }

    /**
     * Clear action error message.
     */
    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    /**
     * Clear action success message.
     */
    fun clearActionSuccess() {
        _uiState.update { it.copy(actionSuccess = null) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
