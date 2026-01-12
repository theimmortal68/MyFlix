package dev.jausc.myflix.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the library screen.
 */
data class LibraryUiState(
    val items: List<JellyfinItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty() && error == null
}

/**
 * ViewModel for the mobile library screen.
 * Manages library items loading.
 */
class LibraryViewModel(
    private val libraryId: String,
    private val jellyfinClient: JellyfinClient,
) : ViewModel() {

    /**
     * Factory for creating LibraryViewModel with manual dependency injection.
     */
    class Factory(
        private val libraryId: String,
        private val jellyfinClient: JellyfinClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(libraryId, jellyfinClient) as T
        }
    }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibraryItems()
    }

    /**
     * Load library items.
     */
    private fun loadLibraryItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            jellyfinClient.getLibraryItems(libraryId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            items = result.items,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Failed to load library",
                            isLoading = false,
                        )
                    }
                }
        }
    }

    /**
     * Refresh library items.
     */
    fun refresh() {
        loadLibraryItems()
    }
}
