# MyFlix Feature Parity Plan

**Date:** February 4, 2026  
**Scope:** Movies & TV Shows playback only (no Live TV, no Music)

---

## Implementation Status

| # | Feature | Status |
|---|---------|--------|
| 1 | [Image Caching Optimization](#1-image-caching-optimization) | ✅ Done |
| 2 | [Audio Night Mode (DRC)](#2-audio-night-mode-drc) | ✅ Done |
| 3 | [Audio Delay Adjustment](#3-audio-delay-adjustment) | ✅ Done |
| 4 | [Stereo Downmix](#4-stereo-downmix) | ✅ Done |
| 5 | [Audio Passthrough](#5-audio-passthrough) | ✅ Done |
| 6 | [HDR Fallback Option](#6-hdr-fallback-option) | ✅ Done |
| 7 | [TV Channels / WatchNext](#7-tv-channels--watchnext) | ✅ Done |
| 8 | [Refresh Rate Switching](#8-refresh-rate-switching) | ✅ Done |
| 9 | [Subtitle Delay Adjustment](#9-subtitle-delay-adjustment) | ✅ Done |
| 10 | [External Subtitle Support](#10-external-subtitle-support) | ✅ Done |
| 11 | [Complete Dream Service](#11-complete-dream-service) | ✅ Done |
| 12 | [Inline Trailer Autoplay](#12-inline-trailer-autoplay) | ✅ Done |
| 13 | [OpenSubtitles Search](#13-opensubtitles-search) | ✅ Done |
| 14 | [Multiple Themes](#14-multiple-themes) | ⬜ Todo |
| 15 | [SyncPlay](#15-syncplay) | ⬜ Todo |

**Progress: 13/15 complete (87%)**

---

## ✅ Completed Features

### 1. Image Caching Optimization
**Commit:** `aeb4544` - Feb 3, 2026

- `ImageConstants.kt` - 25% memory cache, 250MB disk cache, 1920x1080 max backdrop
- `MyFlixImageLoader.kt` - Aggressive caching, ignores server headers
- TV: no crossfade, Mobile: crossfade enabled

### 2. Audio Night Mode (DRC)
**Commit:** `0a41637` - Feb 3, 2026

- `NightModeAudioProcessor.kt` - Dynamic range compression
- Threshold: -24dB, Ratio: 4:1, Makeup gain: 6dB
- Custom AudioSink in ExoPlayerWrapper
- TV and Mobile settings toggles

### 3. Audio Delay Adjustment
**Commit:** `bf9b81e` - Feb 3, 2026

- `DelayAudioProcessor.kt` - Circular buffer for delays
- Range: -500ms to +500ms in 10ms increments
- TV: Audio menu with Audio Track and Audio Sync sections
- Mobile: Audio sheet with delay controls

### 4. Stereo Downmix
**Commit:** `0815af1` - Feb 3, 2026

- `StereoDownmixProcessor.kt` - ITU-R BS.775-1 coefficients
- Supports 5.1, 7.1, quad, and 3.0 layouts
- TV Settings > Playback toggle

### 5. Audio Passthrough
**Commit:** `90d5762` - Feb 3, 2026

- `AudioCapabilityDetector.kt` - Device capability detection
- `PassthroughHelper.kt` - Custom AudioCapabilities builder
- Per-codec toggles: DTS, Dolby TrueHD, E-AC3, AC3

### 6. HDR Fallback Option
**Commit:** `ba24967` - Feb 3, 2026

- JellyfinClient: CodecProfile with VideoRangeType NotEquals condition
- Excludes DOVI, DOVIWithHDR10, DOVIWithHDR10Plus variants
- PreferencesScreen: Device section toggle

### 7. TV Channels / WatchNext
**Commit:** `28b615f` (merge) - Feb 3, 2026

- `WatchNextManager.kt` - Updates Play Next row on playback stop
- `TvChannelManager.kt` - Custom preview channel (Continue Watching + Next Up)
- `ChannelSyncWorker.kt` - WorkManager periodic sync (30 min)
- Deep links: `myflix://play/{itemId}?startPositionMs={position}`

### 8. Refresh Rate Switching
**Commits:** `bef4d0c` → `a98a5a6` - Feb 3, 2026

- `DisplayModeHelper.kt` - Optimal refresh rate selection with scoring
- `RefreshRateManager.kt` - Playback refresh rate control
- Modes: OFF / SEAMLESS (API 30+) / ALWAYS
- NTSC rate mapping (23.976 → 24Hz, 29.97 → 30Hz)
- TV and Mobile PlayerScreen integration

### 9. Subtitle Delay Adjustment
**Commit:** `629cdbc` - Feb 4, 2026

- `SubtitleDelayRepository.kt` - Per-media persistence (SharedPreferences)
- `SubtitleDelayController.kt` - ExoPlayer cue interception with Handler delay
- MPV: Uses native `sub-delay` property
- Range: -10s to +10s with ±500ms nudge buttons
- TV UI component visible when subtitles active

### 10. External Subtitle Support
**Commit:** `dd780cc` - Feb 4, 2026

- MediaStream model: `isExternal`, `deliveryMethod`, `deliveryUrl`
- Helper extensions: `isExternalSubtitle`, `subtitleMimeType`, `trackId()`
- ExoPlayer: `MediaItem.SubtitleConfiguration` with soft restart
- MPV: `sub-add` command for dynamic loading
- TV PlayerScreen auto-detects and loads external subtitles

### 11. Complete Dream Service
**Commit:** `21f16d5` - Feb 4, 2026

- `PlaybackStateRepository.kt` - Global playback state tracking singleton
- `DreamContent.kt` - NowPlaying variant with poster, progress, time info
- `DreamViewModel.kt` - Combined flows (NowPlaying > LibraryShowcase > Logo)
- `DreamScreen.kt` - NowPlayingScreen with play/pause indicator and progress bar
- PlayerViewModel integration for playback start/progress/stop reporting

### 12. Inline Trailer Autoplay
**Commit:** `85da00e` - Feb 4, 2026

- `InlineTrailerPlayer.kt` - ExoPlayer-based inline trailer with lifecycle management
- `TrailerBackdrop.kt` - Ken Burns backdrop with trailer crossfade overlay
- 3-second delay before trailer starts, muted autoplay with unmute button
- `trailerAutoplayEnabled` preference with toggle in Settings > Display
- MovieDetailScreen and UnifiedSeriesScreen integration

### 13. OpenSubtitles Search
**Commit:** Pending - Feb 4, 2026

- `JellyfinClient.kt` - Added `searchRemoteSubtitles()` and `downloadRemoteSubtitle()` API methods
- `RemoteSubtitleInfo` data class for subtitle search results
- `SubtitleSearchDialog.kt` - TV-focused dialog with language input and results list
- `PlayerScreen.kt` - "Search Subtitles..." option in subtitle menu
- Shows provider, language, download count, community rating, hearing impaired indicator
- Requires Jellyfin OpenSubtitles plugin installed on server

---

## ⬜ Remaining Features

### 14. Multiple Themes

**Status:** Not implemented  
**Effort:** 3-4 days

| Action | Path |
|--------|------|
| Add | `core/common/src/main/java/dev/jausc/myflix/core/common/ui/theme/ThemePresets.kt` |
| Modify | `app-tv/src/main/java/dev/jausc/myflix/tv/ui/theme/Theme.kt` |
| Modify | `app-mobile/src/main/java/dev/jausc/myflix/mobile/ui/theme/Theme.kt` |

**References:** DUNE theme, Wholphin theme

**Notes:** Default, OLED Dark (#000000), High Contrast presets

---

### 15. SyncPlay

**Status:** Not implemented  
**Effort:** 2-3 weeks

| Action | Path |
|--------|------|
| Add | `core/network/src/main/java/dev/jausc/myflix/core/network/syncplay/SyncPlayManager.kt` |
| Add | `core/network/src/main/java/dev/jausc/myflix/core/network/syncplay/TimeSyncManager.kt` |
| Add | `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/SyncPlayDialog.kt` |

**References:** Moonfin SyncPlayManager, TimeSyncManager, SyncPlayDialog

**Notes:** WebSocket sync, time drift compensation - complex feature, defer if needed

---

## Summary

| Status | Count | Features |
|--------|-------|----------|
| ✅ Done | 13 | Image Caching, Night Mode, Audio Delay, Stereo Downmix, Passthrough, HDR Fallback, TV Channels, Refresh Rate, Subtitle Delay, External Subs, Dream Service, Trailer Autoplay, OpenSubtitles |
| ⬜ Todo | 2 | Themes, SyncPlay |

**Recommended next:** Multiple Themes (3-4 days) - Default, OLED Dark, High Contrast presets
