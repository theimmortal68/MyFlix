# SyncPlay Feature Design

**Date:** February 4, 2026
**Status:** Approved
**Scope:** Full Jellyfin SyncPlay feature parity (TV-first)

---

## Overview

SyncPlay enables synchronized playback across multiple Jellyfin clients. Users can create or join groups, and all group members see the same content at the same position. The server acts as the source of truth for timing.

**Goals:**
- Create and join SyncPlay groups from TV app
- Synchronized playback with drift correction
- Full queue support (shared playlist)
- Standard group management (create, join, leave, see members)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      PlayerViewModel                         │
│                  (existing - orchestrates)                   │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     SyncPlayManager                          │
│         (new - central SyncPlay state machine)               │
│  • Group state (IDLE, WAITING, PAUSED, PLAYING, BUFFERING)  │
│  • Member list & permissions                                 │
│  • Queue management                                          │
│  • Command deduplication (500ms window)                      │
└────────────┬─────────────────────────────┬──────────────────┘
             │                             │
┌────────────▼────────────┐   ┌───────────▼───────────────────┐
│    TimeSyncManager      │   │      JellyfinClient           │
│  (new - clock sync)     │   │  (extended - SyncPlay API)    │
│  • Server time offset   │   │  • Create/join/leave groups   │
│  • Drift calculation    │   │  • Queue operations           │
│  • Ping tracking        │   │  • Playback commands          │
└─────────────────────────┘   └───────────────────────────────┘
                                           │
                              ┌────────────▼──────────────────┐
                              │     JellyfinWebSocket         │
                              │   (extended - SyncPlay msgs)  │
                              │  • GroupUpdate events         │
                              │  • PlaybackRequest events     │
                              │  • QueueUpdate events         │
                              └───────────────────────────────┘
```

---

## State Machine

### Group States

```
    ┌──────────────────────────────────────────────────────┐
    │                                                      │
    ▼                                                      │
┌───────┐  joinGroup()  ┌─────────┐  play()  ┌─────────┐  │
│ IDLE  │──────────────▶│ WAITING │─────────▶│ PLAYING │──┤
└───────┘               └─────────┘          └─────────┘  │
    ▲                        │                    │       │
    │                        │ pause()            │       │
    │  leaveGroup()          ▼                    │       │
    │                   ┌─────────┐               │       │
    └───────────────────│ PAUSED  │◀──────────────┘       │
                        └─────────┘                       │
                             │                            │
                             │ buffering detected         │
                             ▼                            │
                        ┌───────────┐  buffer complete    │
                        │ BUFFERING │─────────────────────┘
                        └───────────┘
```

### Core State Model

```kotlin
data class SyncPlayState(
    val groupId: String?,
    val groupName: String,
    val groupState: GroupState,  // IDLE, WAITING, PAUSED, PLAYING, BUFFERING
    val members: List<GroupMember>,
    val isHost: Boolean,
    val queue: List<QueueItem>,
    val currentQueueIndex: Int,
    val lastSyncTime: Long,      // Server timestamp of last sync
    val localTimeOffset: Long,   // Difference from server clock
)

data class GroupMember(
    val userId: String,
    val userName: String,
    val isHost: Boolean,
)

data class QueueItem(
    val itemId: String,
    val name: String,
    val runtime: Long,
)
```

---

## Time Synchronization

### TimeSyncManager

```kotlin
class TimeSyncManager {
    val serverTimeOffset: StateFlow<Long>  // serverTime - localTime
    val averagePing: StateFlow<Long>       // Network latency estimate

    fun startSync()   // Begin 5-second ping cycle
    fun stopSync()

    fun toServerTime(localMs: Long): Long
    fun toLocalTime(serverMs: Long): Long
}
```

### Drift Calculation

```
Expected Position = ServerPositionAtSyncTime + (localNow - syncTime) + serverTimeOffset
Actual Position   = player.currentPositionMs
Drift             = Expected - Actual
```

### Correction Thresholds

| Drift Amount | Action |
|--------------|--------|
| < 200ms | No correction (acceptable) |
| 200ms - 2s | Speed correction (0.95x or 1.05x) |
| > 2s | Skip correction (seek to expected position) |

### Speed Correction Behavior
- Behind by 200ms-2s → speed up to 1.05x
- Ahead by 200ms-2s → slow down to 0.95x
- Check every 1 second
- Return to 1.0x once drift < 100ms
- Gradual ramp to avoid jarring audio

---

## API Surface

### JellyfinClient Methods

```kotlin
// Group Management
suspend fun syncPlayGetGroups(): Result<List<SyncPlayGroup>>
suspend fun syncPlayCreateGroup(groupName: String): Result<Unit>
suspend fun syncPlayJoinGroup(groupId: String): Result<Unit>
suspend fun syncPlayLeaveGroup(): Result<Unit>

// Playback Commands (sent to group)
suspend fun syncPlayPlay(): Result<Unit>
suspend fun syncPlayPause(): Result<Unit>
suspend fun syncPlaySeek(positionTicks: Long): Result<Unit>
suspend fun syncPlayStop(): Result<Unit>

// Queue Management
suspend fun syncPlaySetQueue(itemIds: List<String>, startIndex: Int): Result<Unit>
suspend fun syncPlayQueueAdd(itemIds: List<String>): Result<Unit>
suspend fun syncPlayQueueNext(): Result<Unit>
suspend fun syncPlayQueuePrevious(): Result<Unit>

// Time Sync
suspend fun syncPlayPing(): Result<SyncPlayPingResponse>
```

### WebSocket Events

```kotlin
sealed interface SyncPlayEvent {
    data class GroupJoined(val groupId: String, val members: List<GroupMember>) : SyncPlayEvent
    data class GroupLeft(val reason: String?) : SyncPlayEvent
    data class GroupStateUpdate(val state: GroupState, val positionTicks: Long, val whenUtc: String) : SyncPlayEvent
    data class MembersUpdate(val members: List<GroupMember>) : SyncPlayEvent
    data class QueueUpdate(val queue: List<QueueItem>, val currentIndex: Int) : SyncPlayEvent
    data class PlayRequest(val positionTicks: Long, val whenUtc: String) : SyncPlayEvent
    data class PauseRequest(val positionTicks: Long, val whenUtc: String) : SyncPlayEvent
    data class SeekRequest(val positionTicks: Long) : SyncPlayEvent
}
```

---

## TV UI Components

### Entry Points

1. **Player Menu** - "SyncPlay" option in player slide-out menu
2. **Detail Screen** - "Watch Together" button on movie/episode detail

### SyncPlayDialog (Group Selection)

```
┌─────────────────────────────────────────┐
│  🎬 SyncPlay                            │
├─────────────────────────────────────────┤
│                                         │
│  ○ Create New Group                     │
│                                         │
│  ─── Available Groups ───               │
│                                         │
│  ● Movie Night (3 members)        [Join]│
│  ○ Living Room Sync (2 members)   [Join]│
│                                         │
│                            [Cancel]     │
└─────────────────────────────────────────┘
```

### SyncPlayStatusBar (In-Player)

```
┌──────────────────────────────────────────────────────────┐
│ 🔗 Movie Night • 3 viewers • In Sync                     │
└──────────────────────────────────────────────────────────┘
```

- Small bar at top of player (auto-hides with controls)
- Shows: group name, member count, sync status

### SyncPlayGroupOverlay (Member List)

```
┌─────────────────────────────────────────┐
│  Movie Night                            │
├─────────────────────────────────────────┤
│  👤 You (Host)                          │
│  👤 Sarah's TV                          │
│  👤 Kitchen Shield                      │
├─────────────────────────────────────────┤
│  [Add to Queue]  [Leave Group]          │
└─────────────────────────────────────────┘
```

---

## PlayerViewModel Integration

### Modified Behavior

```kotlin
class PlayerViewModel {
    private val syncPlayManager: SyncPlayManager

    val isInSyncPlay: StateFlow<Boolean>
    val syncPlayState: StateFlow<SyncPlayState?>

    // Intercept controls when in SyncPlay
    fun play() {
        if (isInSyncPlay.value) {
            syncPlayManager.requestPlay()  // Send to group
        } else {
            playerController.play()  // Direct control
        }
    }

    // Handle commands from SyncPlayManager
    fun onSyncPlayCommand(command: SyncPlayCommand) {
        when (command) {
            is SyncPlayCommand.Play -> playerController.play()
            is SyncPlayCommand.Pause -> playerController.pause()
            is SyncPlayCommand.Seek -> playerController.seekTo(command.positionMs)
            is SyncPlayCommand.SetSpeed -> playerController.setPlaybackSpeed(command.speed)
            is SyncPlayCommand.LoadItem -> loadMedia(command.itemId, command.startPositionMs)
        }
    }
}
```

### Buffering Coordination

- Local player buffering → notify SyncPlayManager → server pauses group
- All members ready → server resumes group

---

## Error Handling

| Error | Action |
|-------|--------|
| WebSocket disconnected | Show "Reconnecting..." toast, auto-rejoin on reconnect |
| Group disbanded | Show "Group ended" toast, continue local playback |
| Kicked from group | Show "Removed from group" toast, continue local playback |
| Sync drift > 10s | Show "Having trouble syncing" warning, offer to leave |

---

## File Structure

### New Files

```
core/network/src/main/java/dev/jausc/myflix/core/network/syncplay/
├── SyncPlayManager.kt          # Central state machine (~400 lines)
├── SyncPlayModels.kt           # Data classes (~100 lines)
└── TimeSyncManager.kt          # Clock synchronization (~150 lines)

app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/
├── SyncPlayDialog.kt           # Group list/create/join (~300 lines)
├── SyncPlayStatusBar.kt        # In-player status (~80 lines)
└── SyncPlayGroupOverlay.kt     # Member list overlay (~200 lines)
```

### Modified Files

- `JellyfinClient.kt` - Add SyncPlay API methods
- `WebSocketModels.kt` - Add SyncPlay events
- `JellyfinWebSocket.kt` - Parse SyncPlay events
- `PlayerViewModel.kt` - SyncPlay command interception
- `PlayerScreen.kt` (TV) - Add SyncPlay UI components
- `PlayerSlideOutMenu.kt` - Add "SyncPlay" menu option

---

## Implementation Phases

### Phase 1: Core Infrastructure (Week 1)
- SyncPlayModels.kt, TimeSyncManager.kt
- JellyfinClient SyncPlay API methods
- WebSocket SyncPlay event parsing

### Phase 2: SyncPlayManager (Week 1-2)
- State machine implementation
- Drift detection and correction
- Command deduplication

### Phase 3: TV UI (Week 2)
- SyncPlayDialog, StatusBar, GroupOverlay
- PlayerViewModel integration
- PlayerScreen integration

### Phase 4: Testing & Polish (Week 2-3)
- Multi-device testing
- Edge cases (reconnection, buffering)
- Mobile adaptation (if time permits)

---

## References

- Moonfin SyncPlayManager: `/references/Moonfin/app/src/main/java/org/jellyfin/androidtv/data/syncplay/SyncPlayManager.kt`
- Jellyfin SyncPlay API: https://api.jellyfin.org/#tag/SyncPlay
