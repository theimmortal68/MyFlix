package dev.jausc.myflix.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.jausc.myflix.core.common.util.DateFormatter
import dev.jausc.myflix.core.seerr.SeerrPerson
import dev.jausc.myflix.core.seerr.SeerrPersonCastCredit
import dev.jausc.myflix.core.seerr.SeerrPersonCredits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for SeerrActorDetailScreen.
 * Manages person details and loading state across TV and mobile platforms.
 */
@Stable
class SeerrActorDetailScreenState(
    val personId: Int,
    private val loader: SeerrPersonLoader,
    private val scope: CoroutineScope,
) {
    var person by mutableStateOf<SeerrPerson?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /**
     * Cast credits sorted by rating (highest first).
     */
    val sortedCastCredits: List<SeerrPersonCastCredit>
        get() = person?.combinedCredits?.sortedCast ?: emptyList()

    /**
     * Formatted birthday string (e.g., "Born November 14, 1985").
     */
    val formattedBirthday: String?
        get() = DateFormatter.formatBirthday(person?.birthday)

    /**
     * Birth info for display (birthday and/or place of birth).
     * Uses newline separator for multi-line display, pipe for single-line.
     */
    fun getBirthInfo(separator: String = "\n"): String? {
        val parts = buildList {
            formattedBirthday?.let { add(it) }
            person?.placeOfBirth?.let { add(it) }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(separator)
    }

    /**
     * Load person details and combined credits.
     */
    internal fun loadPerson() {
        scope.launch {
            isLoading = true
            error = null

            loader.loadPerson(personId)
                .onSuccess { loadedPerson ->
                    // If combinedCredits is null, fetch them separately
                    if (loadedPerson.combinedCredits == null) {
                        loader.loadCombinedCredits(personId)
                            .onSuccess { credits ->
                                person = loadedPerson.copy(combinedCredits = credits)
                            }
                            .onFailure {
                                // Still show person even if credits fail
                                person = loadedPerson
                            }
                    } else {
                        person = loadedPerson
                    }
                }
                .onFailure { error = it.message }

            isLoading = false
        }
    }
}

/**
 * Loader interface for fetching person data.
 * Abstracts the SeerrClient for testing and flexibility.
 */
interface SeerrPersonLoader {
    suspend fun loadPerson(personId: Int): Result<SeerrPerson>
    suspend fun loadCombinedCredits(personId: Int): Result<SeerrPersonCredits>

    companion object {
        /**
         * Create a loader that delegates to the given SeerrClient.
         */
        fun from(client: dev.jausc.myflix.core.seerr.SeerrClient): SeerrPersonLoader = object : SeerrPersonLoader {
                override suspend fun loadPerson(personId: Int) = client.getPerson(personId)
                override suspend fun loadCombinedCredits(personId: Int) = client.getPersonCombinedCredits(personId)
            }

        /**
         * Create a loader that delegates to the given SeerrRepository.
         */
        fun from(repository: dev.jausc.myflix.core.seerr.SeerrRepository): SeerrPersonLoader = object : SeerrPersonLoader {
                override suspend fun loadPerson(personId: Int) = repository.getPerson(personId)
                override suspend fun loadCombinedCredits(personId: Int) = repository.getPersonCombinedCredits(personId)
            }
    }
}

/**
 * Creates and remembers a [SeerrActorDetailScreenState].
 *
 * @param personId The TMDb ID of the person to display
 * @param loader Loader to fetch person details and credits
 * @return A [SeerrActorDetailScreenState] for managing actor detail screen UI state
 */
@Composable
fun rememberSeerrActorDetailScreenState(personId: Int, loader: SeerrPersonLoader,): SeerrActorDetailScreenState {
    val scope = rememberCoroutineScope()
    val state = remember(personId) {
        SeerrActorDetailScreenState(personId, loader, scope)
    }

    LaunchedEffect(personId) {
        state.loadPerson()
    }

    return state
}
