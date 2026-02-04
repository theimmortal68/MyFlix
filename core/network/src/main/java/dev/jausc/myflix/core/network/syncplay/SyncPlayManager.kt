package dev.jausc.myflix.core.network.syncplay

import android.util.Log
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.network.websocket.SyncPlayCommandType
import dev.jausc.myflix.core.network.websocket.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Interface for controlling the media player from SyncPlay.
 * Implemented by PlayerViewModel or similar.
 */
interface SyncPlayPlayerController {
    /** Get current playback position in ticks */
    fun getCurrentPositionTicks(): Long

    /** Check if player is currently playing */
    fun isPlaying(): Boolean

    /** Set playback speed (1.0 = normal, 0.95 = slow down, 1.05 = speed up) */
    fun setPlaybackSpeed(speed: Float)

    /** Seek to position in ticks */
    fun seekTo(positionTicks: Long)

    /** Pause playback */
    fun pause()

    /** Resume playback */
    fun play()
}

/**
 * Central state machine for SyncPlay feature.
 *
 * Coordinates group management, playback commands, and queue operations.
 * Holds the current SyncPlay state and sends commands to server via JellyfinClient.
 *
 * Event handling and drift correction will be added in subsequent tasks.
 */
class SyncPlayManager(
    private val jellyfinClient: JellyfinClient,
    private val timeSyncManager: TimeSyncManager,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "SyncPlayManager"

        // Drift correction constants
        private const val DRIFT_CHECK_INTERVAL_MS = 1000L
        private const val SPEED_CORRECTION_THRESHOLD_MS = 200L // Use speed adjustment above this
        private const val SKIP_CORRECTION_THRESHOLD_MS = 2000L // Use seek above this
        private const val CORRECTION_COOLDOWN_MS = 3000L
        private const val SPEED_SLOW = 0.95f
        private const val SPEED_FAST = 1.05f
        private const val SPEED_NORMAL = 1.0f

        // Command deduplication
        private const val COMMAND_DEDUP_WINDOW_MS = 500L
    }

    // Player controller for drift correction
    private var playerController: SyncPlayPlayerController? = null
    private var driftCheckJob: Job? = null
    private var lastCorrectionTime: Long = 0L

    // Command deduplication
    private val processedCommands = mutableSetOf<String>()
    private var lastCommandClearTime = 0L

    // State management
    private val _state = MutableStateFlow(SyncPlayState())
    val state: StateFlow<SyncPlayState> = _state.asStateFlow()

    private val _availableGroups = MutableStateFlow<List<SyncPlayGroup>>(emptyList())
    val availableGroups: StateFlow<List<SyncPlayGroup>> = _availableGroups.asStateFlow()

    // ==================== Group Management ====================

    /**
     * Fetch available SyncPlay groups from server.
     */
    suspend fun refreshGroups(): Result<Unit> {
        Log.d(TAG, "refreshGroups: Fetching available groups")
        return try {
            val result = jellyfinClient.syncPlayGetGroups()
            result.fold(
                onSuccess = { groups ->
                    _availableGroups.value = groups
                    Log.d(TAG, "refreshGroups: Found ${groups.size} groups")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "refreshGroups: Failed to fetch groups", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "refreshGroups: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new SyncPlay group and automatically join it.
     */
    suspend fun createGroup(name: String): Result<Unit> {
        Log.d(TAG, "createGroup: Creating group '$name'")
        return try {
            val result = jellyfinClient.syncPlayCreateGroup(name)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "createGroup: Successfully created group '$name'")
                    // Server will send GroupJoined event via WebSocket
                    // State will be updated in onGroupJoined
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "createGroup: Failed to create group '$name'", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "createGroup: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Join an existing SyncPlay group.
     */
    suspend fun joinGroup(groupId: String): Result<Unit> {
        Log.d(TAG, "joinGroup: Joining group $groupId")
        return try {
            val result = jellyfinClient.syncPlayJoinGroup(groupId)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "joinGroup: Successfully joined group $groupId")
                    // Server will send GroupJoined event via WebSocket
                    // State will be updated in onGroupJoined
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "joinGroup: Failed to join group $groupId", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "joinGroup: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Leave the current SyncPlay group.
     */
    suspend fun leaveGroup(): Result<Unit> {
        val currentGroupId = _state.value.groupId
        Log.d(TAG, "leaveGroup: Leaving group $currentGroupId")

        if (currentGroupId == null) {
            Log.w(TAG, "leaveGroup: Not in a group")
            return Result.success(Unit)
        }

        return try {
            val result = jellyfinClient.syncPlayLeaveGroup()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "leaveGroup: Successfully left group")
                    onGroupLeft()
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "leaveGroup: Failed to leave group", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "leaveGroup: Exception", e)
            Result.failure(e)
        }
    }

    // ==================== Playback Control ====================

    /**
     * Request the group to start/resume playback.
     */
    suspend fun requestPlay(): Result<Unit> {
        Log.d(TAG, "requestPlay: Requesting group playback")
        return try {
            val result = jellyfinClient.syncPlayPlay()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "requestPlay: Play request sent")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "requestPlay: Failed to request play", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "requestPlay: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Request the group to pause playback.
     */
    suspend fun requestPause(): Result<Unit> {
        Log.d(TAG, "requestPause: Requesting group pause")
        return try {
            val result = jellyfinClient.syncPlayPause()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "requestPause: Pause request sent")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "requestPause: Failed to request pause", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "requestPause: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Request the group to seek to a position.
     */
    suspend fun requestSeek(positionTicks: Long): Result<Unit> {
        Log.d(TAG, "requestSeek: Requesting seek to $positionTicks ticks")
        return try {
            val result = jellyfinClient.syncPlaySeek(positionTicks)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "requestSeek: Seek request sent")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "requestSeek: Failed to request seek", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "requestSeek: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Request the group to stop playback.
     */
    suspend fun requestStop(): Result<Unit> {
        Log.d(TAG, "requestStop: Requesting group stop")
        return try {
            val result = jellyfinClient.syncPlayStop()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "requestStop: Stop request sent")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "requestStop: Failed to request stop", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "requestStop: Exception", e)
            Result.failure(e)
        }
    }

    // ==================== Queue Management ====================

    /**
     * Set the group's play queue.
     */
    suspend fun setPlayQueue(itemIds: List<String>, startIndex: Int = 0): Result<Unit> {
        Log.d(TAG, "setPlayQueue: Setting queue with ${itemIds.size} items, startIndex=$startIndex")
        return try {
            val result = jellyfinClient.syncPlaySetQueue(itemIds, startIndex)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "setPlayQueue: Queue set successfully")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "setPlayQueue: Failed to set queue", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "setPlayQueue: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Add items to the group's play queue.
     */
    suspend fun addToQueue(itemIds: List<String>): Result<Unit> {
        Log.d(TAG, "addToQueue: Adding ${itemIds.size} items to queue")
        return try {
            val result = jellyfinClient.syncPlayQueueAdd(itemIds)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "addToQueue: Items added to queue")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "addToQueue: Failed to add items", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "addToQueue: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Request to play the next item in queue.
     */
    suspend fun nextItem(): Result<Unit> {
        Log.d(TAG, "nextItem: Requesting next item")
        return try {
            val result = jellyfinClient.syncPlayQueueNext()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "nextItem: Next item request sent")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "nextItem: Failed to request next", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "nextItem: Exception", e)
            Result.failure(e)
        }
    }

    /**
     * Request to play the previous item in queue.
     */
    suspend fun previousItem(): Result<Unit> {
        Log.d(TAG, "previousItem: Requesting previous item")
        return try {
            val result = jellyfinClient.syncPlayQueuePrevious()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "previousItem: Previous item request sent")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "previousItem: Failed to request previous", error)
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "previousItem: Exception", e)
            Result.failure(e)
        }
    }

    // ==================== Internal State Updates ====================

    /**
     * Called when successfully joined a group (from event handlers).
     */
    internal fun onGroupJoined(
        groupId: String,
        groupName: String,
        state: String,
        participants: List<String>,
    ) {
        Log.d(TAG, "onGroupJoined: groupId=$groupId, name=$groupName, state=$state, participants=${participants.size}")

        // Map server state string to GroupState enum
        val groupState = try {
            GroupState.valueOf(state.uppercase())
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "onGroupJoined: Unknown state '$state', defaulting to IDLE")
            GroupState.IDLE
        }

        // Start time synchronization when joining a group
        timeSyncManager.startSync()

        _state.update { current ->
            current.copy(
                enabled = true,
                groupId = groupId,
                groupName = groupName,
                groupState = groupState,
                members = participants.map { userId ->
                    GroupMember(userId = userId, userName = userId)
                },
            )
        }

        // Start drift correction if player controller is available
        if (playerController != null) {
            startDriftCheck()
        }
    }

    /**
     * Called when left a group (from event handlers or explicit leave).
     */
    internal fun onGroupLeft() {
        Log.d(TAG, "onGroupLeft: Clearing SyncPlay state")

        // Stop drift correction before clearing state
        stopDriftCheck()

        // Stop time synchronization when leaving a group
        timeSyncManager.stopSync()

        _state.value = SyncPlayState()
    }

    // ==================== Drift Correction ====================

    /**
     * Set the player controller for drift correction.
     * Call with null to disconnect the player.
     */
    fun setPlayerController(controller: SyncPlayPlayerController?) {
        playerController = controller
        if (controller != null && _state.value.enabled) {
            startDriftCheck()
        } else {
            stopDriftCheck()
        }
    }

    private fun startDriftCheck() {
        stopDriftCheck()
        driftCheckJob = scope.launch {
            while (isActive) {
                delay(DRIFT_CHECK_INTERVAL_MS)
                checkAndCorrectDrift()
            }
        }
        Log.d(TAG, "startDriftCheck: Started drift correction loop")
    }

    private fun stopDriftCheck() {
        driftCheckJob?.cancel()
        driftCheckJob = null
        // Reset playback speed when stopping
        playerController?.setPlaybackSpeed(SPEED_NORMAL)
        Log.d(TAG, "stopDriftCheck: Stopped drift correction loop")
    }

    private fun checkAndCorrectDrift() {
        val controller = playerController ?: return
        val state = _state.value

        // Only check during active playback
        if (state.groupState != GroupState.PLAYING || !controller.isPlaying()) {
            // Reset speed if we're not in playing state
            controller.setPlaybackSpeed(SPEED_NORMAL)
            return
        }

        // Calculate expected position based on last sync time
        val expectedPositionMs = calculateExpectedPositionMs()
        val actualPositionMs = SyncPlayUtils.ticksToMs(controller.getCurrentPositionTicks())
        val driftMs = actualPositionMs - expectedPositionMs

        // Check cooldown
        val now = System.currentTimeMillis()
        if (now - lastCorrectionTime < CORRECTION_COOLDOWN_MS) {
            return
        }

        when {
            kotlin.math.abs(driftMs) < SPEED_CORRECTION_THRESHOLD_MS -> {
                // Within tolerance, ensure normal speed
                controller.setPlaybackSpeed(SPEED_NORMAL)
            }
            kotlin.math.abs(driftMs) < SKIP_CORRECTION_THRESHOLD_MS -> {
                // Small drift - use speed correction
                val newSpeed = if (driftMs > 0) SPEED_SLOW else SPEED_FAST
                controller.setPlaybackSpeed(newSpeed)
                lastCorrectionTime = now
                Log.d(TAG, "checkAndCorrectDrift: Speed correction, drift=${driftMs}ms, speed=$newSpeed")
            }
            else -> {
                // Large drift - seek to expected position
                val expectedTicks = SyncPlayUtils.msToTicks(expectedPositionMs)
                controller.seekTo(expectedTicks)
                controller.setPlaybackSpeed(SPEED_NORMAL)
                lastCorrectionTime = now
                Log.d(TAG, "checkAndCorrectDrift: Seek correction, drift=${driftMs}ms, seeking to $expectedTicks")
            }
        }
    }

    private fun calculateExpectedPositionMs(): Long {
        val state = _state.value
        val lastSyncMs = state.lastSyncTime
        val lastPositionMs = SyncPlayUtils.ticksToMs(state.lastSyncPositionTicks)

        if (lastSyncMs == 0L) {
            return 0L
        }

        // Calculate elapsed time since last sync, accounting for server time offset
        val serverTimeNow = timeSyncManager.getServerTimeNow()
        val elapsedMs = serverTimeNow - lastSyncMs

        return lastPositionMs + elapsedMs
    }

    // ==================== WebSocket Event Handling ====================

    /**
     * Process a SyncPlay WebSocket event.
     * Called by the component that collects WebSocket events.
     */
    fun onSyncPlayEvent(event: WebSocketEvent) {
        when (event) {
            is WebSocketEvent.SyncPlayCommand -> handleSyncPlayCommand(event)
            is WebSocketEvent.SyncPlayGroupJoined -> handleGroupJoined(event)
            is WebSocketEvent.SyncPlayGroupLeft -> handleGroupLeft()
            is WebSocketEvent.SyncPlayGroupStateUpdate -> handleGroupStateUpdate(event)
            is WebSocketEvent.SyncPlayPlayQueueUpdate -> handlePlayQueueUpdate(event)
            is WebSocketEvent.SyncPlayUserJoined -> handleUserJoined(event)
            is WebSocketEvent.SyncPlayUserLeft -> handleUserLeft(event)
            else -> {} // Not a SyncPlay event
        }
    }

    private fun handleSyncPlayCommand(event: WebSocketEvent.SyncPlayCommand) {
        // Deduplication check
        val commandKey = "${event.command}_${event.positionTicks}_${event.whenUtc}"
        if (!shouldProcessCommand(commandKey)) {
            Log.d(TAG, "handleSyncPlayCommand: Duplicate command ignored: ${event.command}")
            return
        }

        Log.d(TAG, "handleSyncPlayCommand: ${event.command} at ${event.positionTicks} ticks, when=${event.whenUtc}")

        // Schedule command execution at the specified server time
        scope.launch {
            scheduleCommandExecution(event)
        }
    }

    private fun shouldProcessCommand(commandKey: String): Boolean {
        val now = System.currentTimeMillis()

        // Clear old commands periodically
        if (now - lastCommandClearTime > COMMAND_DEDUP_WINDOW_MS * 2) {
            processedCommands.clear()
            lastCommandClearTime = now
        }

        return processedCommands.add(commandKey)
    }

    private suspend fun scheduleCommandExecution(event: WebSocketEvent.SyncPlayCommand) {
        val controller = playerController ?: return

        // Parse the whenUtc timestamp and convert to local time
        val serverExecuteTime = parseServerTime(event.whenUtc)
        val localExecuteTime = timeSyncManager.toLocalTime(serverExecuteTime)
        val now = System.currentTimeMillis()
        val delayMs = localExecuteTime - now

        // If delay is positive, wait. If negative/small, execute immediately
        if (delayMs > 10) {
            delay(delayMs)
        }

        // Update state with new sync point
        _state.update {
            it.copy(
                lastSyncTime = serverExecuteTime,
                lastSyncPositionTicks = event.positionTicks,
            )
        }

        // Execute the command
        when (event.command) {
            SyncPlayCommandType.Unpause -> {
                controller.seekTo(event.positionTicks)
                controller.play()
                _state.update { it.copy(groupState = GroupState.PLAYING) }
            }
            SyncPlayCommandType.Pause -> {
                controller.pause()
                controller.seekTo(event.positionTicks)
                _state.update { it.copy(groupState = GroupState.PAUSED) }
            }
            SyncPlayCommandType.Seek -> {
                controller.seekTo(event.positionTicks)
            }
            SyncPlayCommandType.Stop -> {
                controller.pause()
                _state.update { it.copy(groupState = GroupState.IDLE) }
            }
        }

        Log.d(TAG, "scheduleCommandExecution: Executed ${event.command}")
    }

    private fun parseServerTime(whenUtc: String): Long {
        return try {
            java.time.Instant.parse(whenUtc).toEpochMilli()
        } catch (e: Exception) {
            Log.w(TAG, "parseServerTime: Failed to parse '$whenUtc', using current time")
            System.currentTimeMillis()
        }
    }

    private fun handleGroupJoined(event: WebSocketEvent.SyncPlayGroupJoined) {
        onGroupJoined(event.groupId, event.groupName, event.state, event.participants)
    }

    private fun handleGroupLeft() {
        onGroupLeft()
    }

    private fun handleGroupStateUpdate(event: WebSocketEvent.SyncPlayGroupStateUpdate) {
        Log.d(TAG, "handleGroupStateUpdate: state=${event.state}, reason=${event.reason}")

        val groupState = try {
            GroupState.valueOf(event.state.uppercase())
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "handleGroupStateUpdate: Unknown state '${event.state}'")
            return
        }

        _state.update { current ->
            current.copy(
                groupState = groupState,
                lastSyncTime = event.whenUtc?.let { parseServerTime(it) } ?: current.lastSyncTime,
                lastSyncPositionTicks = if (event.positionTicks > 0) event.positionTicks else current.lastSyncPositionTicks,
            )
        }
    }

    private fun handlePlayQueueUpdate(event: WebSocketEvent.SyncPlayPlayQueueUpdate) {
        Log.d(TAG, "handlePlayQueueUpdate: ${event.playlistItemIds.size} items, index=${event.playingItemIndex}, reason=${event.reason}")

        _state.update { current ->
            current.copy(
                queue = event.playlistItemIds.map { itemId ->
                    QueueItem(itemId = itemId, name = itemId) // Name will be resolved later by UI
                },
                currentQueueIndex = event.playingItemIndex,
            )
        }
    }

    private fun handleUserJoined(event: WebSocketEvent.SyncPlayUserJoined) {
        Log.d(TAG, "handleUserJoined: ${event.userName} (${event.userId})")

        _state.update { current ->
            val newMember = GroupMember(userId = event.userId, userName = event.userName)
            if (current.members.none { it.userId == event.userId }) {
                current.copy(members = current.members + newMember)
            } else {
                current
            }
        }
    }

    private fun handleUserLeft(event: WebSocketEvent.SyncPlayUserLeft) {
        Log.d(TAG, "handleUserLeft: ${event.userName} (${event.userId})")

        _state.update { current ->
            current.copy(members = current.members.filter { it.userId != event.userId })
        }
    }

    // ==================== Buffering/Ready State Reporting ====================

    /**
     * Report that the player is buffering.
     */
    suspend fun reportBuffering(positionTicks: Long, playlistItemId: String) {
        if (!_state.value.enabled) return

        Log.d(TAG, "reportBuffering: position=$positionTicks")
        _state.update { it.copy(groupState = GroupState.BUFFERING) }

        try {
            jellyfinClient.syncPlayBuffering(
                positionTicks = positionTicks,
                isPlaying = playerController?.isPlaying() ?: false,
                playlistItemId = playlistItemId,
            )
        } catch (e: Exception) {
            Log.e(TAG, "reportBuffering: Failed", e)
        }
    }

    /**
     * Report that the player is ready (done buffering).
     */
    suspend fun reportReady(positionTicks: Long, playlistItemId: String) {
        if (!_state.value.enabled) return

        Log.d(TAG, "reportReady: position=$positionTicks")

        try {
            jellyfinClient.syncPlayReady(
                positionTicks = positionTicks,
                isPlaying = playerController?.isPlaying() ?: false,
                playlistItemId = playlistItemId,
            )
        } catch (e: Exception) {
            Log.e(TAG, "reportReady: Failed", e)
        }
    }
}
