package dev.jausc.myflix.core.network.syncplay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SyncPlay group states matching Jellyfin API.
 */
enum class GroupState {
    IDLE,
    WAITING,
    PAUSED,
    PLAYING,
    BUFFERING,
}

/**
 * Current SyncPlay state for the client.
 */
data class SyncPlayState(
    val enabled: Boolean = false,
    val groupId: String? = null,
    val groupName: String = "",
    val groupState: GroupState = GroupState.IDLE,
    val members: List<GroupMember> = emptyList(),
    val isHost: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val currentQueueIndex: Int = 0,
    val lastSyncTime: Long = 0L,
    val localTimeOffset: Long = 0L,
)

/**
 * A member in a SyncPlay group.
 */
data class GroupMember(
    val userId: String,
    val userName: String,
    val isHost: Boolean = false,
)

/**
 * An item in the SyncPlay queue.
 */
data class QueueItem(
    val itemId: String,
    val name: String,
    val runtimeTicks: Long = 0L,
)

/**
 * Information about an available SyncPlay group.
 */
@Serializable
data class SyncPlayGroup(
    @SerialName("GroupId") val groupId: String,
    @SerialName("GroupName") val groupName: String,
    @SerialName("State") val state: String,
    @SerialName("Participants") val participants: List<String> = emptyList(),
    @SerialName("LastUpdatedAt") val lastUpdatedAt: String? = null,
)

/**
 * Response from time sync ping.
 */
@Serializable
data class UtcTimeResponse(
    @SerialName("RequestReceptionTime") val requestReceptionTime: String,
    @SerialName("ResponseTransmissionTime") val responseTransmissionTime: String,
)

/**
 * Commands that can be sent to the group.
 */
enum class SyncPlayCommandType {
    UNPAUSE,
    PAUSE,
    SEEK,
    STOP,
}

/**
 * Utility functions for SyncPlay time conversions.
 */
object SyncPlayUtils {
    const val TICKS_PER_MS = 10_000L

    fun ticksToMs(ticks: Long): Long = ticks / TICKS_PER_MS
    fun msToTicks(ms: Long): Long = ms * TICKS_PER_MS
}
