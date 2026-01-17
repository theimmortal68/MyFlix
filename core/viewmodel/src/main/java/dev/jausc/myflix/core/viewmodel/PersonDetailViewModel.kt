package dev.jausc.myflix.core.viewmodel

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
 * UI state for person detail screens.
 */
data class PersonDetailUiState(
    val person: JellyfinItem? = null,
    val credits: List<JellyfinItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Shared ViewModel for person detail screens.
 * Loads person information and their credits (movies/shows they appear in).
 */
class PersonDetailViewModel(
    private val personId: String,
    private val jellyfinClient: JellyfinClient,
) : ViewModel() {

    class Factory(
        private val personId: String,
        private val jellyfinClient: JellyfinClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PersonDetailViewModel(personId, jellyfinClient) as T
    }

    private val _uiState = MutableStateFlow(PersonDetailUiState())
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    init {
        loadPerson()
    }

    private fun loadPerson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            jellyfinClient.getItem(personId)
                .onSuccess { person ->
                    _uiState.update { it.copy(person = person) }
                    loadCredits()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load person: ${e.message ?: "Unknown error"}",
                        )
                    }
                }
        }
    }

    private fun loadCredits() {
        viewModelScope.launch {
            jellyfinClient.getItemsByPerson(personId, limit = 30)
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            credits = items.filter { it.id != personId },
                            isLoading = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
}
