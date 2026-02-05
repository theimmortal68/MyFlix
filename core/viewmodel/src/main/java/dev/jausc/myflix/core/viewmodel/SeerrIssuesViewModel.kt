package dev.jausc.myflix.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.seerr.SeerrIssue
import dev.jausc.myflix.core.seerr.SeerrIssueComment
import dev.jausc.myflix.core.seerr.SeerrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for Seerr issues screen.
 */
data class SeerrIssuesUiState(
    val isLoading: Boolean = true,
    val issues: List<SeerrIssue> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val selectedIssue: SeerrIssue? = null,
    val isLoadingIssueDetail: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val isAddingComment: Boolean = false,
    val isResolvingIssue: Boolean = false,
    val isDeletingIssue: Boolean = false,
    val actionError: String? = null,
    val actionSuccess: String? = null,
)

/**
 * ViewModel for Seerr issues management.
 * Handles loading issues, viewing issue details, adding comments,
 * resolving issues, and deleting issues.
 */
class SeerrIssuesViewModel(
    private val seerrRepository: SeerrRepository,
) : ViewModel() {

    /**
     * Factory for creating SeerrIssuesViewModel with manual dependency injection.
     */
    class Factory(
        private val seerrRepository: SeerrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeerrIssuesViewModel(seerrRepository) as T
    }

    private val _uiState = MutableStateFlow(SeerrIssuesUiState())
    val uiState: StateFlow<SeerrIssuesUiState> = _uiState.asStateFlow()

    /** Current authenticated user */
    val currentUser = seerrRepository.currentUser

    companion object {
        private const val TAG = "SeerrIssuesViewModel"
        private const val PAGE_SIZE = 20
    }

    init {
        loadIssues()
    }

    /**
     * Load list of issues.
     */
    fun loadIssues() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            seerrRepository.getIssues(take = PAGE_SIZE, skip = 0)
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            issues = result.results,
                            currentPage = 0,
                            hasMore = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load issues: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load issues",
                        )
                    }
                }
        }
    }

    /**
     * Load more issues (pagination).
     */
    fun loadMoreIssues() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMore) {
            return
        }

        val nextPage = state.currentPage + 1
        val skip = nextPage * PAGE_SIZE

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            seerrRepository.getIssues(take = PAGE_SIZE, skip = skip)
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            issues = it.issues + result.results,
                            currentPage = nextPage,
                            hasMore = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load more issues: ${error.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    /**
     * Get detailed information for a specific issue.
     *
     * @param issueId ID of the issue to load
     */
    fun getIssue(issueId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingIssueDetail = true, actionError = null) }

            seerrRepository.getIssue(issueId)
                .onSuccess { issue ->
                    _uiState.update {
                        it.copy(
                            isLoadingIssueDetail = false,
                            selectedIssue = issue,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load issue detail: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoadingIssueDetail = false,
                            actionError = error.message ?: "Failed to load issue details",
                        )
                    }
                }
        }
    }

    /**
     * Add a comment to an issue.
     *
     * @param issueId ID of the issue to comment on
     * @param message Comment message
     */
    fun addComment(issueId: Int, message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingComment = true, actionError = null) }

            seerrRepository.addIssueComment(issueId, message)
                .onSuccess { comment ->
                    _uiState.update { state ->
                        // Update the selected issue with the new comment
                        val updatedSelectedIssue = state.selectedIssue?.let { issue ->
                            if (issue.id == issueId) {
                                issue.copy(
                                    comments = (issue.comments ?: emptyList()) + comment
                                )
                            } else {
                                issue
                            }
                        }

                        state.copy(
                            isAddingComment = false,
                            selectedIssue = updatedSelectedIssue,
                            actionSuccess = "Comment added",
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to add comment: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isAddingComment = false,
                            actionError = error.message ?: "Failed to add comment",
                        )
                    }
                }
        }
    }

    /**
     * Resolve an issue.
     *
     * @param issueId ID of the issue to resolve
     */
    fun resolveIssue(issueId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingIssue = true, actionError = null) }

            seerrRepository.resolveIssue(issueId)
                .onSuccess { resolvedIssue ->
                    _uiState.update { state ->
                        state.copy(
                            isResolvingIssue = false,
                            actionSuccess = "Issue resolved",
                            issues = state.issues.map { issue ->
                                if (issue.id == issueId) resolvedIssue else issue
                            },
                            selectedIssue = if (state.selectedIssue?.id == issueId) {
                                resolvedIssue
                            } else {
                                state.selectedIssue
                            },
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to resolve issue: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isResolvingIssue = false,
                            actionError = error.message ?: "Failed to resolve issue",
                        )
                    }
                }
        }
    }

    /**
     * Delete an issue.
     *
     * @param issueId ID of the issue to delete
     */
    fun deleteIssue(issueId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingIssue = true, actionError = null) }

            seerrRepository.deleteIssue(issueId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isDeletingIssue = false,
                            actionSuccess = "Issue deleted",
                            issues = state.issues.filter { issue -> issue.id != issueId },
                            selectedIssue = if (state.selectedIssue?.id == issueId) {
                                null
                            } else {
                                state.selectedIssue
                            },
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to delete issue: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isDeletingIssue = false,
                            actionError = error.message ?: "Failed to delete issue",
                        )
                    }
                }
        }
    }

    /**
     * Refresh issues list.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            seerrRepository.getIssues(take = PAGE_SIZE, skip = 0)
                .onSuccess { result ->
                    val hasMore = result.results.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            issues = result.results,
                            currentPage = 0,
                            hasMore = hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to refresh issues: ${error.message}")
                    _uiState.update { it.copy(isRefreshing = false) }
                }
        }
    }

    /**
     * Clear selected issue.
     */
    fun clearSelectedIssue() {
        _uiState.update { it.copy(selectedIssue = null) }
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
