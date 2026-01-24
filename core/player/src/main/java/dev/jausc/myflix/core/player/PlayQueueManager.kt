package dev.jausc.myflix.core.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a single item in the play queue.
 *
 * @param itemId Jellyfin item ID
 * @param title Display title (episode name or movie title)
 * @param episodeInfo Optional episode info in "S1 E3" format
 * @param thumbnailItemId Item ID to use for thumbnail (may differ from itemId for episodes)
 */
data class QueueItem(
    val itemId: String,
    val title: String,
    val episodeInfo: String? = null,
    val thumbnailItemId: String? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
)

/**
 * Current state of the play queue.
 *
 * @param items All items in the queue
 * @param currentIndex Index of currently playing item
 * @param source Where this queue originated from
 */
data class PlayQueueState(
    val items: List<QueueItem> = emptyList(),
    val currentIndex: Int = 0,
    val source: QueueSource = QueueSource.SINGLE,
) {
    /** Currently playing item, or null if queue is empty */
    val currentItem: QueueItem?
        get() = items.getOrNull(currentIndex)

    /** Next item in queue, or null if at end */
    val nextItem: QueueItem?
        get() = items.getOrNull(currentIndex + 1)

    /** Whether there are more items after current */
    val hasNext: Boolean
        get() = currentIndex < items.lastIndex

    /** Whether the queue is empty */
    val isEmpty: Boolean
        get() = items.isEmpty()

    /** Whether we're in queue playback mode (more than single item) */
    val isQueueMode: Boolean
        get() = items.size > 1

    /** Remaining items count after current */
    val remainingCount: Int
        get() = (items.size - currentIndex - 1).coerceAtLeast(0)
}

/**
 * Source of the queue - helps with UI messaging.
 */
enum class QueueSource {
    /** Normal single-item playback, no queue */
    SINGLE,

    /** Playing from a specific episode onwards */
    EPISODE_PLAY_ALL,

    /** Playing all episodes in a season */
    SEASON_PLAY_ALL,

    /** Playing all items in a collection/BoxSet */
    COLLECTION,
}

/**
 * Singleton manager for the play queue.
 * Maintains queue state across player lifecycle.
 *
 * Usage:
 * 1. Before navigating to player, call setQueue() with items
 * 2. Player checks hasNext after video ends
 * 3. Call advanceToNext() to move to next item
 * 4. Call clear() when exiting player or canceling queue
 */
object PlayQueueManager {
    private val _state = MutableStateFlow(PlayQueueState())
    val state: StateFlow<PlayQueueState> = _state.asStateFlow()

    /**
     * Set up a new play queue.
     *
     * @param items List of items to play
     * @param source Where this queue came from
     * @param startIndex Index to start from (default 0)
     */
    fun setQueue(items: List<QueueItem>, source: QueueSource, startIndex: Int = 0) {
        _state.value = PlayQueueState(
            items = items,
            currentIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            source = source,
        )
    }

    /**
     * Set up queue for single item playback (no queue).
     */
    fun setSingleItem(
        itemId: String,
        title: String,
        episodeInfo: String? = null,
        thumbnailItemId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
    ) {
        _state.value = PlayQueueState(
            items = listOf(
                QueueItem(
                    itemId = itemId,
                    title = title,
                    episodeInfo = episodeInfo,
                    thumbnailItemId = thumbnailItemId,
                    audioStreamIndex = audioStreamIndex,
                    subtitleStreamIndex = subtitleStreamIndex,
                ),
            ),
            currentIndex = 0,
            source = QueueSource.SINGLE,
        )
    }

    /**
     * Advance to the next item in the queue.
     *
     * @return The next QueueItem, or null if at end of queue
     */
    fun advanceToNext(): QueueItem? {
        val current = _state.value
        if (!current.hasNext) return null

        val newIndex = current.currentIndex + 1
        _state.value = current.copy(currentIndex = newIndex)
        return _state.value.currentItem
    }

    /**
     * Get the current item ID, if any.
     */
    fun getCurrentItemId(): String? = _state.value.currentItem?.itemId

    /**
     * Get the current queue item, if any.
     */
    fun getCurrentItem(): QueueItem? = _state.value.currentItem

    /**
     * Get the next item without advancing.
     */
    fun peekNext(): QueueItem? = _state.value.nextItem

    /**
     * Check if there's a next item.
     */
    fun hasNext(): Boolean = _state.value.hasNext

    /**
     * Check if we're in queue mode (more than one item).
     */
    fun isQueueMode(): Boolean = _state.value.isQueueMode

    /**
     * Add an item to the end of the current queue.
     * If no queue exists, creates a new queue with this item.
     *
     * @param item The item to add to the queue
     */
    fun addItem(item: QueueItem) {
        val current = _state.value
        if (current.isEmpty) {
            // No queue yet - create single item queue
            _state.value = PlayQueueState(
                items = listOf(item),
                currentIndex = 0,
                source = QueueSource.SINGLE,
            )
        } else {
            // Add to existing queue
            _state.value = current.copy(
                items = current.items + item,
            )
        }
    }

    /**
     * Clear the queue and reset to empty state.
     */
    fun clear() {
        _state.value = PlayQueueState()
    }
}
