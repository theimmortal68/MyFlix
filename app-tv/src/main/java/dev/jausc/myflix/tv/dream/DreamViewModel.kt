package dev.jausc.myflix.tv.dream

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.data.ServerManager
import dev.jausc.myflix.core.network.JellyfinClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val TAG = "DreamViewModel"
private const val ROTATION_INTERVAL_MS = 25_000L
private const val CLOCK_UPDATE_INTERVAL_MS = 1_000L
private const val ITEMS_TO_FETCH = 50
private const val MIN_ITEMS_FOR_ROTATION = 5

/**
 * ViewModel for the Dream Service screensaver.
 *
 * Manages the rotation of library content with preloaded images
 * and provides a clock that updates every second.
 */
class DreamViewModel(
    private val context: Context,
    private val jellyfinClient: JellyfinClient,
) : ViewModel() {
    private val imageLoader = ImageLoader.Builder(context).build()
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    private val _content = MutableStateFlow<DreamContent>(DreamContent.Logo())
    val content: StateFlow<DreamContent> = _content.asStateFlow()

    private val _currentTime = MutableStateFlow(formatCurrentTime())
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private var rotationJob: Job? = null
    private var clockJob: Job? = null

    private var items: List<JellyfinItem> = emptyList()
    private var currentIndex = 0

    init {
        startClock()
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            if (!jellyfinClient.isAuthenticated) {
                Log.d(TAG, "Not authenticated, showing logo")
                _content.value = DreamContent.Logo("Not signed in")
                return@launch
            }

            try {
                // First show logo while loading
                _content.value = DreamContent.Logo()

                // Get all libraries
                val libraries = jellyfinClient.getLibraries().getOrNull() ?: emptyList()
                Log.d(TAG, "Found ${libraries.size} libraries")

                if (libraries.isEmpty()) {
                    _content.value = DreamContent.Logo("No libraries found")
                    return@launch
                }

                // Fetch random items from all libraries
                val allItems = mutableListOf<JellyfinItem>()
                for (library in libraries) {
                    val collectionType = library.collectionType
                    // Only fetch from video libraries (movies, tvshows)
                    if (collectionType in listOf("movies", "tvshows", null)) {
                        jellyfinClient.getLibraryItemsFiltered(
                            libraryId = library.id,
                            limit = ITEMS_TO_FETCH / libraries.size.coerceAtLeast(1),
                            sortBy = "Random",
                            includeItemTypes = listOf("Movie", "Series"),
                        ).onSuccess { response ->
                            allItems.addAll(response.items.filter { it.backdropImageTags?.isNotEmpty() == true })
                        }.onFailure { e ->
                            Log.w(TAG, "Failed to fetch items from ${library.name}: ${e.message}")
                        }
                    }
                }

                items = allItems.shuffled()
                Log.d(TAG, "Fetched ${items.size} items with backdrops")

                if (items.size < MIN_ITEMS_FOR_ROTATION) {
                    _content.value = DreamContent.Logo("Not enough content")
                    return@launch
                }

                // Start rotation
                startRotation()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading content", e)
                _content.value = DreamContent.Error(e.message ?: "Failed to load content")
            }
        }
    }

    private fun startRotation() {
        rotationJob?.cancel()
        rotationJob = viewModelScope.launch {
            while (isActive && items.isNotEmpty()) {
                val item = items[currentIndex]
                loadAndShowItem(item)

                currentIndex = (currentIndex + 1) % items.size
                delay(ROTATION_INTERVAL_MS)
            }
        }
    }

    private suspend fun loadAndShowItem(item: JellyfinItem) {
        try {
            // Load backdrop
            val backdropUrl = jellyfinClient.getBackdropUrl(
                item.id,
                item.backdropImageTags?.firstOrNull(),
            )
            val backdropBitmap = loadBitmap(backdropUrl) ?: return

            // Try to load logo (optional)
            val logoTag = item.imageTags?.logo
            val logoBitmap = logoTag?.let {
                val logoUrl = jellyfinClient.getLogoUrl(item.id, it)
                loadBitmap(logoUrl)
            }

            // Extract metadata
            val year = item.productionYear
            val rating = item.communityRating
            val genres = item.genres ?: emptyList()

            _content.value = DreamContent.LibraryShowcase(
                backdrop = backdropBitmap,
                logo = logoBitmap,
                title = item.name,
                year = year,
                rating = rating,
                genres = genres.take(3),
                itemType = item.type,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load item ${item.name}", e)
        }
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Need software bitmap for rendering
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.image.asDrawable(context.resources)
                drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image: $url", e)
            null
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                _currentTime.value = formatCurrentTime()
                delay(CLOCK_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun formatCurrentTime(): String = LocalTime.now().format(timeFormatter)

    override fun onCleared() {
        super.onCleared()
        rotationJob?.cancel()
        clockJob?.cancel()
    }

    /**
     * Factory for creating DreamViewModel with dependencies.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val serverManager = ServerManager.getInstance(context)
            val activeServer = serverManager.getActiveServer()

            val jellyfinClient = JellyfinClient(context).apply {
                if (activeServer != null) {
                    configure(
                        activeServer.serverUrl,
                        activeServer.accessToken,
                        activeServer.userId,
                        deviceId,
                    )
                }
            }

            return DreamViewModel(context, jellyfinClient) as T
        }
    }
}
