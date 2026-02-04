# SyncPlay Implementation Plan

> **For Claude:** Use subagent-driven development to implement this plan task-by-task.

**Goal:** Implement Jellyfin SyncPlay feature for synchronized playback across multiple clients with full queue support.

**Architecture:** Central `SyncPlayManager` state machine coordinates with `TimeSyncManager` for clock sync. WebSocket events drive state changes; `PlayerViewModel` intercepts playback commands when in SyncPlay mode. TV-first implementation with reusable core components.

**Tech Stack:** Ktor HTTP client, OkHttp WebSocket, kotlinx.serialization, Kotlin Coroutines/Flow, Jetpack Compose for TV

---

## Phase 1: Core Infrastructure

### Task 1: Create SyncPlay Data Models

**Files:**
- Create: `core/network/src/main/java/dev/jausc/myflix/core/network/syncplay/SyncPlayModels.kt`

**Step 1: Create the SyncPlayModels.kt file**

```kotlin
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
```

**Step 2: Verify file compiles**

Run: `./gradlew :core:network:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL

---

### Task 2: Create TimeSyncManager

**Files:**
- Create: `core/network/src/main/java/dev/jausc/myflix/core/network/syncplay/TimeSyncManager.kt`

**Step 1: Create TimeSyncManager.kt with NTP-style time sync**

```kotlin
package dev.jausc.myflix.core.network.syncplay

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages time synchronization between client and Jellyfin server.
 * Uses NTP-style algorithm selecting measurement with minimum delay for accuracy.
 *
 * Modes:
 * - Greedy: First 3 measurements at 1-second intervals
 * - Low-profile: After initial sync, measure every 60 seconds
 */
class TimeSyncManager(
    private val syncTimeProvider: suspend () -> UtcTimeResponse,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val measurements = mutableListOf<TimeSyncMeasurement>()
    private val maxMeasurements = 8

    private var _timeOffset: Long = 0L
    private var _roundTripTime: Long = 0L
    private var _measurementCount: Int = 0

    private val _serverTimeOffset = MutableStateFlow(0L)
    val serverTimeOffset: StateFlow<Long> = _serverTimeOffset.asStateFlow()

    private val _averagePing = MutableStateFlow(0L)
    val averagePing: StateFlow<Long> = _averagePing.asStateFlow()

    val timeOffset: Long get() = _timeOffset
    val roundTripTime: Long get() = _roundTripTime
    val measurementCount: Int get() = _measurementCount
    val isGreedyMode: Boolean get() = _measurementCount < GREEDY_PING_COUNT

    private var syncJob: Job? = null
    private var isSyncing = false

    private data class TimeSyncMeasurement(
        val offset: Long,
        val roundTripTime: Long,
        val delay: Long,
        val timestamp: Long = System.currentTimeMillis(),
    )

    companion object {
        private const val TAG = "TimeSyncManager"
        private const val GREEDY_INTERVAL_MS = 1000L
        private const val LOW_PROFILE_INTERVAL_MS = 60_000L
        private const val GREEDY_PING_COUNT = 3
        private const val MAX_RTT_MS = 5000L
    }

    /**
     * Start periodic time synchronization.
     * Initial greedy mode for quick sync, then low-profile maintenance.
     */
    fun startSync() {
        if (isSyncing) return
        isSyncing = true
        _measurementCount = 0

        syncJob = scope.launch {
            while (isActive && isSyncing) {
                performSyncMeasurement()
                _measurementCount++

                val interval = if (_measurementCount < GREEDY_PING_COUNT) {
                    GREEDY_INTERVAL_MS
                } else {
                    LOW_PROFILE_INTERVAL_MS
                }
                delay(interval)
            }
        }
        Log.d(TAG, "Started time synchronization")
    }

    /**
     * Stop periodic time synchronization.
     */
    fun stopSync() {
        isSyncing = false
        syncJob?.cancel()
        syncJob = null
        measurements.clear()
        _measurementCount = 0
        Log.d(TAG, "Stopped time synchronization")
    }

    /**
     * Perform a single NTP-style time sync measurement.
     *
     * Algorithm:
     * t0 = client send time
     * t1 = server receive time
     * t2 = server send time
     * t3 = client receive time
     *
     * offset = ((t1 - t0) + (t2 - t3)) / 2
     * roundTrip = (t3 - t0) - (t2 - t1)
     */
    private suspend fun performSyncMeasurement() {
        try {
            val t0 = System.currentTimeMillis()

            val response = withContext(Dispatchers.IO) {
                syncTimeProvider()
            }

            val t3 = System.currentTimeMillis()

            val t1 = parseIsoTimestamp(response.requestReceptionTime)
            val t2 = parseIsoTimestamp(response.responseTransmissionTime)

            val offset = ((t1 - t0) + (t2 - t3)) / 2
            val rtt = (t3 - t0) - (t2 - t1)
            val networkDelay = (t3 - t0) / 2

            if (rtt > MAX_RTT_MS || rtt < 0) {
                Log.w(TAG, "Discarding measurement with RTT=${rtt}ms")
                return
            }

            synchronized(measurements) {
                measurements.add(TimeSyncMeasurement(offset, rtt, networkDelay))

                while (measurements.size > maxMeasurements) {
                    measurements.removeAt(0)
                }

                if (measurements.isNotEmpty()) {
                    val best = measurements.minByOrNull { it.delay }!!
                    _timeOffset = best.offset
                    _roundTripTime = best.roundTripTime
                    _serverTimeOffset.value = _timeOffset
                    _averagePing.value = _roundTripTime
                }
            }

            Log.v(TAG, "offset=${_timeOffset}ms, RTT=${_roundTripTime}ms, measurements=${measurements.size}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync time: ${e.message}")
        }
    }

    /**
     * Force an immediate sync measurement.
     */
    suspend fun syncNow() {
        performSyncMeasurement()
    }

    /**
     * Convert server time to local time.
     */
    fun toLocalTime(serverMs: Long): Long = serverMs - _timeOffset

    /**
     * Convert local time to server time.
     */
    fun toServerTime(localMs: Long): Long = localMs + _timeOffset

    /**
     * Get current server time based on local clock and offset.
     */
    fun getServerTimeNow(): Long = System.currentTimeMillis() + _timeOffset

    /**
     * Parse ISO 8601 timestamp to milliseconds.
     */
    private fun parseIsoTimestamp(iso: String): Long {
        // Format: "2026-02-04T12:34:56.1234567Z"
        return try {
            val instant = java.time.Instant.parse(iso)
            instant.toEpochMilli()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $iso")
            System.currentTimeMillis()
        }
    }
}
```

**Step 2: Verify file compiles**

Run: `./gradlew :core:network:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL

---

### Task 3: Add WebSocket SyncPlay Events

**Files:**
- Modify: `core/network/src/main/java/dev/jausc/myflix/core/network/websocket/WebSocketModels.kt`

**Step 1: Add SyncPlay event types to WebSocketEvent sealed class**

Add after the existing `UserDataChanged` event:

```kotlin
// SyncPlay Events
data class SyncPlayCommand(
    val command: SyncPlayCommandType,
    val positionTicks: Long,
    val whenUtc: String,
    val playlistItemId: String? = null,
) : WebSocketEvent()

data class SyncPlayGroupJoined(
    val groupId: String,
    val groupName: String,
    val state: String,
    val participants: List<String>,
) : WebSocketEvent()

data object SyncPlayGroupLeft : WebSocketEvent()

data class SyncPlayGroupStateUpdate(
    val state: String,
    val reason: String? = null,
    val positionTicks: Long = 0,
    val whenUtc: String? = null,
) : WebSocketEvent()

data class SyncPlayPlayQueueUpdate(
    val playlistItemIds: List<String>,
    val startPositionTicks: Long,
    val playingItemIndex: Int,
    val reason: String,
) : WebSocketEvent()

data class SyncPlayUserJoined(
    val userId: String,
    val userName: String,
) : WebSocketEvent()

data class SyncPlayUserLeft(
    val userId: String,
    val userName: String,
) : WebSocketEvent()
```

**Step 2: Add SyncPlayCommandType enum**

Add near the other enums:

```kotlin
enum class SyncPlayCommandType {
    Unpause, Pause, Seek, Stop
}
```

---

### Task 4: Add WebSocket SyncPlay Event Parsing

**Files:**
- Modify: `core/network/src/main/java/dev/jausc/myflix/core/network/websocket/WebSocketEventParser.kt`

**Step 1: Add SyncPlay message type parsing in the when block**

Add these cases in the `parse()` function's `when (messageType)` block:

```kotlin
"SyncPlayCommand" -> parseSyncPlayCommand(data)
"SyncPlayGroupJoined" -> parseSyncPlayGroupJoined(data)
"SyncPlayGroupLeft" -> WebSocketEvent.SyncPlayGroupLeft
"SyncPlayGroupUpdate" -> parseSyncPlayGroupUpdate(data)
"SyncPlayPlayQueueUpdate" -> parseSyncPlayPlayQueueUpdate(data)
"SyncPlayUserJoined" -> parseSyncPlayUserJoined(data)
"SyncPlayUserLeft" -> parseSyncPlayUserLeft(data)
```

**Step 2: Add the parsing helper functions**

```kotlin
private fun parseSyncPlayCommand(data: JsonObject?): WebSocketEvent? {
    data ?: return null
    val commandStr = data["Command"]?.jsonPrimitive?.contentOrNull ?: return null
    val command = try {
        SyncPlayCommandType.valueOf(commandStr)
    } catch (e: Exception) {
        Log.w(TAG, "Unknown SyncPlay command: $commandStr")
        return null
    }
    return WebSocketEvent.SyncPlayCommand(
        command = command,
        positionTicks = data["PositionTicks"]?.jsonPrimitive?.longOrNull ?: 0L,
        whenUtc = data["When"]?.jsonPrimitive?.contentOrNull ?: "",
        playlistItemId = data["PlaylistItemId"]?.jsonPrimitive?.contentOrNull,
    )
}

private fun parseSyncPlayGroupJoined(data: JsonObject?): WebSocketEvent? {
    data ?: return null
    return WebSocketEvent.SyncPlayGroupJoined(
        groupId = data["GroupId"]?.jsonPrimitive?.contentOrNull ?: return null,
        groupName = data["GroupName"]?.jsonPrimitive?.contentOrNull ?: "",
        state = data["State"]?.jsonPrimitive?.contentOrNull ?: "Idle",
        participants = data["Participants"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.contentOrNull
        } ?: emptyList(),
    )
}

private fun parseSyncPlayGroupUpdate(data: JsonObject?): WebSocketEvent? {
    data ?: return null
    return WebSocketEvent.SyncPlayGroupStateUpdate(
        state = data["State"]?.jsonPrimitive?.contentOrNull ?: "Idle",
        reason = data["Reason"]?.jsonPrimitive?.contentOrNull,
        positionTicks = data["PositionTicks"]?.jsonPrimitive?.longOrNull ?: 0L,
        whenUtc = data["When"]?.jsonPrimitive?.contentOrNull,
    )
}

private fun parseSyncPlayPlayQueueUpdate(data: JsonObject?): WebSocketEvent? {
    data ?: return null
    val playlist = data["Playlist"]?.jsonArray?.mapNotNull { item ->
        item.jsonObject["ItemId"]?.jsonPrimitive?.contentOrNull
    } ?: emptyList()
    return WebSocketEvent.SyncPlayPlayQueueUpdate(
        playlistItemIds = playlist,
        startPositionTicks = data["StartPositionTicks"]?.jsonPrimitive?.longOrNull ?: 0L,
        playingItemIndex = data["PlayingItemIndex"]?.jsonPrimitive?.intOrNull ?: 0,
        reason = data["Reason"]?.jsonPrimitive?.contentOrNull ?: "",
    )
}

private fun parseSyncPlayUserJoined(data: JsonObject?): WebSocketEvent? {
    data ?: return null
    return WebSocketEvent.SyncPlayUserJoined(
        userId = data["UserId"]?.jsonPrimitive?.contentOrNull ?: return null,
        userName = data["UserName"]?.jsonPrimitive?.contentOrNull ?: "",
    )
}

private fun parseSyncPlayUserLeft(data: JsonObject?): WebSocketEvent? {
    data ?: return null
    return WebSocketEvent.SyncPlayUserLeft(
        userId = data["UserId"]?.jsonPrimitive?.contentOrNull ?: return null,
        userName = data["UserName"]?.jsonPrimitive?.contentOrNull ?: "",
    )
}
```

---

### Task 5: Add JellyfinClient SyncPlay API Methods

**Files:**
- Modify: `core/network/src/main/java/dev/jausc/myflix/core/network/JellyfinClient.kt`

**Step 1: Add SyncPlay imports at top of file**

```kotlin
import dev.jausc.myflix.core.network.syncplay.SyncPlayGroup
import dev.jausc.myflix.core.network.syncplay.UtcTimeResponse
```

**Step 2: Add SyncPlay API methods**

Add these methods in the JellyfinClient class (near the end, before the companion object):

```kotlin
// ==================== SyncPlay API ====================

/**
 * Get available SyncPlay groups.
 */
suspend fun syncPlayGetGroups(): Result<List<SyncPlayGroup>> = runCatching {
    Log.d("JellyfinClient", "syncPlayGetGroups: GET $baseUrl/SyncPlay/List")
    val response = httpClient.get("$baseUrl/SyncPlay/List") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to get SyncPlay groups: ${response.status}")
    }
    response.body<List<SyncPlayGroup>>()
}

/**
 * Create a new SyncPlay group.
 */
suspend fun syncPlayCreateGroup(groupName: String): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayCreateGroup: POST $baseUrl/SyncPlay/New")
    val response = httpClient.post("$baseUrl/SyncPlay/New") {
        header("Authorization", authHeader())
        setBody(mapOf("GroupName" to groupName))
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to create SyncPlay group: ${response.status}")
    }
}

/**
 * Join an existing SyncPlay group.
 */
suspend fun syncPlayJoinGroup(groupId: String): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayJoinGroup: POST $baseUrl/SyncPlay/Join")
    val response = httpClient.post("$baseUrl/SyncPlay/Join") {
        header("Authorization", authHeader())
        setBody(mapOf("GroupId" to groupId))
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to join SyncPlay group: ${response.status}")
    }
}

/**
 * Leave the current SyncPlay group.
 */
suspend fun syncPlayLeaveGroup(): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayLeaveGroup: POST $baseUrl/SyncPlay/Leave")
    val response = httpClient.post("$baseUrl/SyncPlay/Leave") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to leave SyncPlay group: ${response.status}")
    }
}

/**
 * Request group to start/resume playback.
 */
suspend fun syncPlayPlay(): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayPlay: POST $baseUrl/SyncPlay/Unpause")
    val response = httpClient.post("$baseUrl/SyncPlay/Unpause") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to request SyncPlay play: ${response.status}")
    }
}

/**
 * Request group to pause playback.
 */
suspend fun syncPlayPause(): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayPause: POST $baseUrl/SyncPlay/Pause")
    val response = httpClient.post("$baseUrl/SyncPlay/Pause") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to request SyncPlay pause: ${response.status}")
    }
}

/**
 * Request group to seek to position.
 */
suspend fun syncPlaySeek(positionTicks: Long): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlaySeek: POST $baseUrl/SyncPlay/Seek position=$positionTicks")
    val response = httpClient.post("$baseUrl/SyncPlay/Seek") {
        header("Authorization", authHeader())
        setBody(mapOf("PositionTicks" to positionTicks))
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to request SyncPlay seek: ${response.status}")
    }
}

/**
 * Request group to stop playback.
 */
suspend fun syncPlayStop(): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayStop: POST $baseUrl/SyncPlay/Stop")
    val response = httpClient.post("$baseUrl/SyncPlay/Stop") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to request SyncPlay stop: ${response.status}")
    }
}

/**
 * Set the group's play queue.
 */
suspend fun syncPlaySetQueue(
    itemIds: List<String>,
    startIndex: Int = 0,
    startPositionTicks: Long = 0,
): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlaySetQueue: POST $baseUrl/SyncPlay/SetNewQueue items=${itemIds.size}")
    val response = httpClient.post("$baseUrl/SyncPlay/SetNewQueue") {
        header("Authorization", authHeader())
        setBody(mapOf(
            "PlayingQueue" to itemIds,
            "PlayingItemPosition" to startIndex,
            "StartPositionTicks" to startPositionTicks,
        ))
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to set SyncPlay queue: ${response.status}")
    }
}

/**
 * Add items to the group's play queue.
 */
suspend fun syncPlayQueueAdd(itemIds: List<String>): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayQueueAdd: POST $baseUrl/SyncPlay/Queue items=${itemIds.size}")
    val response = httpClient.post("$baseUrl/SyncPlay/Queue") {
        header("Authorization", authHeader())
        setBody(mapOf("ItemIds" to itemIds))
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to add to SyncPlay queue: ${response.status}")
    }
}

/**
 * Request group to play next item in queue.
 */
suspend fun syncPlayQueueNext(): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayQueueNext: POST $baseUrl/SyncPlay/NextItem")
    val response = httpClient.post("$baseUrl/SyncPlay/NextItem") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to request SyncPlay next: ${response.status}")
    }
}

/**
 * Request group to play previous item in queue.
 */
suspend fun syncPlayQueuePrevious(): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayQueuePrevious: POST $baseUrl/SyncPlay/PreviousItem")
    val response = httpClient.post("$baseUrl/SyncPlay/PreviousItem") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to request SyncPlay previous: ${response.status}")
    }
}

/**
 * Report buffering state to group.
 */
suspend fun syncPlayBuffering(
    positionTicks: Long,
    isPlaying: Boolean,
    playlistItemId: String,
): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayBuffering: POST $baseUrl/SyncPlay/Buffering")
    val serverTime = java.time.Instant.now().toString()
    val response = httpClient.post("$baseUrl/SyncPlay/Buffering") {
        header("Authorization", authHeader())
        setBody(mapOf(
            "When" to serverTime,
            "PositionTicks" to positionTicks,
            "IsPlaying" to isPlaying,
            "PlaylistItemId" to playlistItemId,
        ))
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to report SyncPlay buffering: ${response.status}")
    }
}

/**
 * Report ready state to group.
 */
suspend fun syncPlayReady(
    positionTicks: Long,
    isPlaying: Boolean,
    playlistItemId: String,
): Result<Unit> = runCatching {
    Log.d("JellyfinClient", "syncPlayReady: POST $baseUrl/SyncPlay/Ready")
    val serverTime = java.time.Instant.now().toString()
    val response = httpClient.post("$baseUrl/SyncPlay/Ready") {
        header("Authorization", authHeader())
        setBody(mapOf(
            "When" to serverTime,
            "PositionTicks" to positionTicks,
            "IsPlaying" to isPlaying,
            "PlaylistItemId" to playlistItemId,
        ))
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to report SyncPlay ready: ${response.status}")
    }
}

/**
 * Send ping to server for latency tracking.
 */
suspend fun syncPlayPing(ping: Long): Result<Unit> = runCatching {
    val response = httpClient.post("$baseUrl/SyncPlay/Ping") {
        header("Authorization", authHeader())
        setBody(mapOf("Ping" to ping))
    }
    // Ping failures are non-critical, don't throw
}

/**
 * Get UTC time from server for time synchronization.
 */
suspend fun getUtcTime(): Result<UtcTimeResponse> = runCatching {
    val response = httpClient.get("$baseUrl/GetUtcTime") {
        header("Authorization", authHeader())
    }
    if (!response.status.isSuccess()) {
        throw Exception("Failed to get UTC time: ${response.status}")
    }
    response.body<UtcTimeResponse>()
}
```

---

### Task 6: Build and Verify Phase 1

**Step 1: Build the project**

Run the Windows build command:
```bash
/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -Command "$env:JAVA_HOME = 'C:\Users\jausc\AppData\Local\Programs\Android Studio\jbr'; cd 'C:\Users\jausc\StudioProjects\MyFlix'; .\gradlew.bat :core:network:compileDebugKotlin"
```

Expected: BUILD SUCCESSFUL

**Step 2: Verify no compilation errors**

If errors occur, fix them before proceeding.

**Step 3: Commit Phase 1**

```bash
git add core/network/src/main/java/dev/jausc/myflix/core/network/syncplay/
git add core/network/src/main/java/dev/jausc/myflix/core/network/websocket/WebSocketModels.kt
git add core/network/src/main/java/dev/jausc/myflix/core/network/websocket/WebSocketEventParser.kt
git add core/network/src/main/java/dev/jausc/myflix/core/network/JellyfinClient.kt
git commit -m "$(cat <<'EOF'
feat(syncplay): add Phase 1 core infrastructure

- Add SyncPlayModels.kt with state, member, queue models
- Add TimeSyncManager.kt with NTP-style clock synchronization
- Add SyncPlay WebSocket events to WebSocketModels.kt
- Add SyncPlay event parsing to WebSocketEventParser.kt
- Add SyncPlay API methods to JellyfinClient.kt

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## Phase 2: SyncPlayManager (Tasks 7-10)

### Task 7: Create SyncPlayManager State Machine

**Files:**
- Create: `core/network/src/main/java/dev/jausc/myflix/core/network/syncplay/SyncPlayManager.kt`

This is the central state machine that:
- Manages group state (IDLE, WAITING, PAUSED, PLAYING, BUFFERING)
- Handles WebSocket events
- Coordinates with TimeSyncManager for drift correction
- Deduplicates commands (500ms window)

**Full implementation in separate task due to size (~400 lines)**

---

### Task 8: Implement SyncPlayManager Core Methods

Create the file with:
- Constructor with JellyfinClient and TimeSyncManager dependencies
- StateFlow for SyncPlayState
- Group management: createGroup, joinGroup, leaveGroup, refreshGroups
- Playback requests: requestPlay, requestPause, requestSeek, requestStop
- Queue management: setPlayQueue, addToQueue, nextItem, previousItem

---

### Task 9: Implement SyncPlayManager Drift Correction

Add to SyncPlayManager:
- `checkAndCorrectDrift()` - Called every 1 second during playback
- Speed correction for 200ms-2s drift (0.95x or 1.05x)
- Skip correction for >2s drift (seek to expected position)
- Cooldown tracking to avoid correction oscillation

---

### Task 10: Implement SyncPlayManager Event Handling

Add to SyncPlayManager:
- `onSyncPlayEvent(event: WebSocketEvent)` - Route WebSocket events
- Command scheduling with server time conversion
- Duplicate command detection (500ms window)
- Buffering/ready state reporting

---

## Phase 3: TV UI (Tasks 11-14)

### Task 11: Create SyncPlayDialog

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/syncplay/SyncPlayDialog.kt`

Dialog for creating/joining SyncPlay groups with TV focus management.

---

### Task 12: Create SyncPlayStatusBar

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/syncplay/SyncPlayStatusBar.kt`

Small bar at top of player showing group name, member count, sync status.

---

### Task 13: Create SyncPlayGroupOverlay

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/syncplay/SyncPlayGroupOverlay.kt`

Full-screen overlay showing member list and actions (Add to Queue, Leave Group).

---

### Task 14: Integrate SyncPlay into PlayerViewModel and PlayerScreen

**Files:**
- Modify: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/PlayerViewModel.kt`
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/PlayerScreen.kt`
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/PlayerSlideOutMenu.kt`

---

## Summary

| Task | File | Action | Purpose |
|------|------|--------|---------|
| 1 | SyncPlayModels.kt | Create | Data classes for state, members, queue |
| 2 | TimeSyncManager.kt | Create | NTP-style clock synchronization |
| 3 | WebSocketModels.kt | Modify | Add SyncPlay event types |
| 4 | WebSocketEventParser.kt | Modify | Parse SyncPlay WebSocket messages |
| 5 | JellyfinClient.kt | Modify | Add SyncPlay API methods |
| 6 | Build verification | Verify | Ensure Phase 1 compiles |
| 7-10 | SyncPlayManager.kt | Create | Central state machine |
| 11 | SyncPlayDialog.kt | Create | Group selection UI |
| 12 | SyncPlayStatusBar.kt | Create | In-player status bar |
| 13 | SyncPlayGroupOverlay.kt | Create | Member list overlay |
| 14 | PlayerViewModel/Screen | Modify | Integration |
