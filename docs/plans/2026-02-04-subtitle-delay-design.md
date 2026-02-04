# Subtitle Delay Adjustment Design

## Overview

Add subtitle delay adjustment (-10s to +10s in 100ms increments) with per-media persistence.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  UI Layer (PlayerSlideOutMenu)                                  │
│  - Slider + nudge buttons (±0.5s)                               │
│  - Displays current delay: "+0.3s"                              │
│  - Only visible when subtitles are active                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  PlayerController                                               │
│  - Routes to current backend                                    │
│  - Persists to SubtitleDelayRepository                          │
│  - Exposes: subtitleDelayMs: StateFlow<Long>                    │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────┐           ┌─────────────────────┐
│  ExoPlayerWrapper   │           │  MpvPlayer          │
│  - SubtitleDelay-   │           │  - MPVLib.set-      │
│    Controller       │           │    PropertyDouble   │
│  - Intercepts cues  │           │    ("sub-delay",    │
│  - Handler delay    │           │     seconds)        │
└─────────────────────┘           └─────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  SubtitleDelayRepository                                        │
│  - SharedPreferences                                            │
│  - Key: itemId (String)                                         │
│  - Value: delayMs (Long)                                        │
│  - Auto-loads on playback start                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Backend Implementations

### MPV
Simple property set:
```kotlin
fun setSubtitleDelayMs(delayMs: Long) {
    MPVLib.setPropertyDouble("sub-delay", delayMs / 1000.0)
}
```

### ExoPlayer
Cue interception via `SubtitleDelayController`:
- Implements `Player.Listener.onCues()`
- Positive delay: Uses Handler.postDelayed()
- Negative/zero delay: Shows immediately (best effort)
- Exposes cues via StateFlow for UI layer

## Persistence

Per-media storage using SharedPreferences:
- Key: Jellyfin item ID
- Value: delay in milliseconds
- Zero delay removes entry (keeps storage clean)
- Auto-loads saved delay when media starts

## UI Controls

Subtitle delay control in PlayerSlideOutMenu:
- **Visibility**: Only when subtitles are active
- **Slider**: -10.0s to +10.0s, 100ms steps
- **Nudge buttons**: -0.5s and +0.5s for quick adjustment
- **Reset button**: Returns to 0
- **Display**: Shows current value with sign (e.g., "+0.3s")

## Files

### New Files
| File | Purpose |
|------|---------|
| `core/player/.../SubtitleDelayController.kt` | ExoPlayer cue interception & delay |
| `core/player/.../SubtitleDelayRepository.kt` | Per-media delay persistence |

### Modified Files
| File | Changes |
|------|---------|
| `core/player/.../UnifiedPlayer.kt` | Add `setSubtitleDelayMs(ms: Long)` |
| `core/player/.../ExoPlayerWrapper.kt` | Own controller, implement onCues(), expose cues StateFlow |
| `core/player/.../MpvPlayer.kt` | Add `setSubtitleDelayMs()` using `sub-delay` property |
| `core/player/.../PlayerController.kt` | Add repository, expose StateFlow, wire up on media start |
| `app-tv/.../PlayerSlideOutMenu.kt` | Add SubtitleDelayControl |
| `app-tv/.../PlayerScreen.kt` | Pass delay state and callbacks to menu |

## Edge Cases

- **Backend switch mid-playback**: Reapply current delay to new backend
- **Subtitles off**: Control hidden, delay still stored for when enabled
- **Seeking**: Handler cancellation prevents stale cue posts
- **Negative delays**: Show immediately (true early requires full subtitle parsing)

## Consensus

Codex recommended this approach. Gemini was unavailable (rate limited).
