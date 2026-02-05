# MyFlix Feature Test Plan

**Version:** 2.0
**Date:** 2026-02-05
**Scope:** All features added since commit b6e3582

---

## Overview

This document provides a comprehensive test plan for 15 features implemented in MyFlix, organized by test execution method:

1. **Automated (Emulator)** - Scripts/UI tests runnable on emulator without human verification
2. **Automated (Real Device)** - Scripts runnable on real Android TV without human verification
3. **Manual (Emulator)** - Requires human verification, can use emulator
4. **Manual (Real Device)** - Requires human verification AND real hardware capabilities

---

## Category 1: Automated Tests (Emulator)

These tests can be executed via ADB commands, UI Automator, or Espresso without human verification. Emulator is sufficient.

### Image Caching

| ID | Test Case | Automation Method | Expected Result |
|----|-----------|-------------------|-----------------|
| IC-01 | First load caching | Clear data, launch app, check logcat for cache misses then hits | Log shows network fetch then cache storage |
| IC-02 | Cached image retrieval | Navigate away/back, check logcat | Log shows cache hits only |
| IC-03 | Cache persistence | Kill app, relaunch, check logcat | Log shows cache hits on restart |

**Verification:** `adb logcat | grep -i "cache\|coil"`

### Preference Persistence

| ID | Test Case | Automation Method | Expected Result |
|----|-----------|-------------------|-----------------|
| NM-03 | Night mode persistence | Enable via UI, kill app, check SharedPrefs | Preference file contains `night_mode_enabled=true` |
| AD-03 | Audio delay range limits | Set values via UI, verify bounds | Value clamped to ±2000ms (or configured max) |
| AD-04 | Audio delay reset | Set delay, reset, check preference | Preference returns to 0 |
| AD-05 | Audio delay persistence | Set delay, kill app, relaunch | Preference value persists |
| TH-04 | Theme persistence | Set theme, kill app, check preference | Theme preference persists |
| SB-04 | Subtitle delay reset | Set delay, reset, verify | Value returns to 0 |

**Verification:** `adb shell cat /data/data/dev.jausc.myflix.tv/shared_prefs/*.xml`

### Error Handling

| ID | Test Case | Automation Method | Expected Result |
|----|-----------|-------------------|-----------------|
| ES-06 | Invalid subtitle file | Attempt to load non-subtitle file | Error message displayed, no crash |
| TA-04 | No trailer available | Navigate to content without trailer | No errors, graceful handling |
| OS-04 | Subtitle search no results | Search for nonexistent content | "No subtitles found" message |
| OS-05 | Server plugin missing | Search with plugin disabled | Graceful error message |

**Verification:** UI state assertions, no crash in logcat

### Build & Code Verification

| ID | Test Case | Automation Method | Expected Result |
|----|-----------|-------------------|-----------------|
| BUILD-01 | Project compiles | `./gradlew assembleDebug` | Build succeeds |
| BUILD-02 | Unit tests pass | `./gradlew test` | All tests pass |
| BUILD-03 | Lint check | `./gradlew lint` | No critical errors |

---

## Category 2: Automated Tests (Real Android TV Device)

These tests require real device capabilities but can be scripted without human verification.

### TV Channels Deep Links

| ID | Test Case | Automation Method | Expected Result |
|----|-----------|-------------------|-----------------|
| TC-03 | Channel click navigation | `adb shell am start -d "myflix://item/{id}"` | App opens to correct content detail screen |

**Verification:** Check foreground activity matches expected screen

### SyncPlay Basic Operations

| ID | Test Case | Automation Method | Expected Result |
|----|-----------|-------------------|-----------------|
| SP-01 | Create group | Navigate to SyncPlay, create group via UI automation | Group created, WebSocket message sent |
| SP-07 | Leave group | Join group, leave via UI automation | Left group, playback independent |

**Verification:** Check logcat for WebSocket messages, verify UI state

### Refresh Rate API Calls

| ID | Test Case | Automation Method | Expected Result |
|----|-----------|-------------------|-----------------|
| RR-API-01 | Mode detection | Query available modes via Display API | Returns supported refresh rates |
| RR-API-02 | Mode switch request | Request mode change, check API response | API call succeeds (actual switch needs visual) |

**Verification:** Logcat for Display API calls

---

## Category 3: Manual Tests (Emulator)

These require human visual verification but emulator display/audio is sufficient.

### Theme Visual Verification

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| TH-01 | Default theme | Select Default theme, inspect UI | Standard dark theme colors |
| TH-02 | OLED Dark theme | Select OLED Dark, inspect backgrounds | Pure black (#000000) backgrounds |
| TH-03 | High Contrast theme | Select High Contrast | Enhanced contrast, clear text |
| TH-05 | All screens themed | Navigate app with each theme | Consistent theming everywhere |
| TH-06 | Player UI theming | Enter player with different themes | Player controls match theme |

### Subtitle Display

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| ES-01 | Load SRT file | Add external .srt file | Subtitles display correctly |
| ES-02 | Load ASS file | Add external .ass file | Styled subtitles with formatting |
| ES-03 | Load VTT file | Add external .vtt file | WebVTT subtitles display |
| ES-04 | Encoding handling | Load non-UTF8 subtitle | Special characters display correctly |
| ES-05 | Switch between tracks | Toggle embedded/external | Both tracks work |
| SB-01 | Positive subtitle delay | Set +500ms delay | Subtitles appear later (time with dialogue) |
| SB-02 | Negative subtitle delay | Set -500ms delay | Subtitles appear earlier |
| SB-03 | Real-time adjustment | Adjust during playback | Change applies immediately |
| SB-05 | Styled subtitle delay | Apply delay to ASS subs | Styling preserved |

### Trailer Playback (Visual)

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| TA-01 | Autoplay on focus | Navigate to movie with trailer, wait | Trailer begins playing inline |
| TA-02 | Muted playback | Observe autoplay | Trailer plays muted |
| TA-05 | Trailer stops on navigate | Navigate away during trailer | Trailer stops, no background audio |

### OpenSubtitles UI

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| OS-01 | Search subtitles | Open search, view results | Subtitles listed with metadata |
| OS-02 | Language filter | Filter by language | Only matching languages shown |
| OS-03 | Download subtitle | Select and download | Subtitle applies to playback |

### Memory & Performance

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| IC-04 | Large library scroll | Scroll rapidly through 500+ items | No OOM, smooth scrolling |

**Verification:** `adb shell dumpsys meminfo dev.jausc.myflix.tv`

---

## Category 4: Manual Tests (Real Android TV Device Required)

These require actual hardware capabilities that emulators cannot provide.

### Audio Quality Verification

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| NM-01 | Enable night mode | Enable, play dynamic content | Loud sounds compressed, quiet boosted | Requires real speakers/headphones |
| NM-02 | Disable night mode | Disable, play same content | Full dynamic range restored | Audio comparison |
| NM-04 | Toggle during playback | Toggle while playing | Audio changes immediately | Real-time audio verification |
| AD-01 | Positive audio delay | Set +200ms | Audio delayed (lip sync) | Requires visual+audio sync check |
| AD-02 | Negative audio delay | Set -200ms | Audio advanced | Lip sync verification |
| SD-01 | Enable stereo downmix | Play 5.1 on stereo device | All channels mixed, no missing audio | Requires surround content + stereo output |
| SD-02 | Center channel audibility | Play dialogue scene | Dialogue clearly audible | Audio quality verification |
| SD-03 | Surround effects | Play surround scene | Effects mixed to stereo | Audio quality verification |
| SD-04 | Disable downmix | Disable, use surround system | Full surround output | Requires surround system |

### Audio Passthrough (Requires AV Receiver)

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| PT-01 | AC3 passthrough | Enable, play AC3 content | Receiver shows "Dolby Digital" | Receiver display verification |
| PT-02 | EAC3 passthrough | Enable, play EAC3 content | Receiver shows "Dolby Digital Plus" | Receiver display verification |
| PT-03 | DTS passthrough | Enable, play DTS content | Receiver shows "DTS" | Receiver display verification |
| PT-04 | TrueHD passthrough | Enable, play TrueHD content | Receiver shows "Dolby TrueHD" | Receiver display verification |
| PT-05 | DTS-HD passthrough | Enable, play DTS-HD content | Receiver shows "DTS-HD MA" | Receiver display verification |
| PT-06 | Selective disable | Disable codec, play format | Audio decoded locally | Receiver shows PCM |
| PT-07 | Unsupported fallback | Enable unsupported, play | Falls back gracefully | No audio dropout |

### HDR/Dolby Vision (Requires HDR Display)

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| HF-01 | Enable HDR fallback | Enable, play DV content | Plays as HDR10 | HDR display required |
| HF-02 | Disable HDR fallback | Disable, play DV content | Plays as Dolby Vision | DV-capable display |
| HF-03 | Non-DV content | Enable fallback, play HDR10 | Plays normally | HDR verification |
| HF-04 | SDR content | Play SDR with setting on | No impact | Display verification |

### Refresh Rate (Requires Multi-Rate Display)

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| RR-01 | OFF mode | Set OFF, play 24fps | Display stays at default | Display rate verification |
| RR-02 | SEAMLESS mode | Set SEAMLESS, play 24fps | Switches without black screen | Display behavior |
| RR-03 | ALWAYS mode | Set ALWAYS, play 24fps | Switches to 24Hz | Display rate verification |
| RR-04 | 60fps content | Play 60fps with SEAMLESS | Display at 60Hz/120Hz | Display rate verification |
| RR-05 | Restore on exit | Exit playback | Returns to original rate | Display behavior |
| RR-06 | Integer multiple | Play 24fps on 120Hz | Prefers 120Hz | Judder observation |

### TV Home Screen Integration (Android TV Only)

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| TC-01 | Continue Watching channel | Watch partial content, go to home | Row appears on home screen | Real TV launcher |
| TC-02 | Next Up channel | Complete episode, check home | Next episode in Watch Next | Real TV launcher |
| TC-04 | Channel update | Watch more, check channel | Progress updated | Home screen refresh |
| TC-05 | Remove from channel | Mark watched, check channel | Item removed | Home screen sync |

### Dream Service (Android TV Only)

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| DS-01 | Now playing display | Play, pause, wait for dream | Currently playing info shown | Dream service |
| DS-02 | Artwork display | Trigger dream during playback | Poster/backdrop visible | Dream rendering |
| DS-03 | No playback state | Clear history, trigger dream | Graceful fallback | Dream service |
| DS-04 | Dream dismissal | Press button during dream | Returns to app correctly | Dream exit handling |

### Trailer Audio

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| TA-03 | Unmute on interaction | Click trailer | Audio unmutes | Audio verification |

### SyncPlay Multi-Device (Requires 2+ Devices)

| ID | Test Case | Steps | Expected Result | Why Real Device |
|----|-----------|-------|-----------------|-----------------|
| SP-02 | Join group | Device 2 joins existing group | Syncs position | Multi-device |
| SP-03 | Synchronized play | Host presses play | All start within 100ms | Side-by-side verification |
| SP-04 | Synchronized pause | Any member pauses | All pause together | Side-by-side verification |
| SP-05 | Synchronized seek | Any member seeks | All seek together | Side-by-side verification |
| SP-06 | Member list display | Open overlay | All members listed | Multi-device state |
| SP-08 | Group disbanded | Creator leaves | All notified, group ends | Multi-device messaging |
| SP-09 | Drift correction | Play extended period | Stay within 500ms | Long playback test |
| SP-10 | Buffering handling | One device buffers | Group waits | Network variation |
| SP-11 | Late joiner | Join during playback | Syncs to position | Mid-playback join |
| SP-12 | Network interruption | Disconnect briefly | Reconnects, resyncs | Network resilience |

---

## Test Summary by Category

| Category | Test Count | Automation Level |
|----------|------------|------------------|
| Automated (Emulator) | 18 | Full - CI/CD capable |
| Automated (Real Device) | 5 | Full - requires device farm |
| Manual (Emulator) | 22 | Human verification, emulator OK |
| Manual (Real Device) | 42 | Human verification, real hardware |
| **Total** | **87** | |

---

## Test Environment

### Emulator Configuration
- Android TV emulator (API 30+)
- 1080p resolution
- 2GB RAM minimum

### Real Device Requirements

| Capability | Required For | Recommended Device |
|------------|--------------|-------------------|
| Android TV 8.0+ | TV Channels, Dream | Any Android TV |
| HDR10/DV display | HDR tests | NVIDIA Shield + HDR TV |
| Multi-refresh display | Refresh rate tests | TV with 24/60/120Hz |
| AV Receiver (eARC) | Passthrough tests | Any Dolby/DTS receiver |
| Surround speakers | Downmix tests | 5.1+ system |
| Second device | SyncPlay tests | Any Android TV/mobile |

---

## Regression Checklist

After testing new features, verify core functionality:

- [ ] App launches successfully
- [ ] Library loads and displays
- [ ] Search works
- [ ] Basic playback (play/pause/seek)
- [ ] Episode navigation
- [ ] Settings save and persist
- [ ] Resume playback works
- [ ] Navigation (D-pad for TV)
- [ ] Focus management (TV)

---

## Bug Reporting Template

```
**Feature:** [Feature name]
**Test Case ID:** [e.g., SP-03]
**Category:** [Automated/Manual] [Emulator/Real Device]
**Device:** [Model, Android version]
**Steps to Reproduce:**
1.
2.
3.

**Expected:** [What should happen]
**Actual:** [What happened]
**Logs:** [Attach logcat if applicable]
**Screenshot/Video:** [If applicable]
```

---

## Sign-off

| Feature | Automated | Manual | Tester | Date | Status |
|---------|-----------|--------|--------|------|--------|
| Image Caching | ✅ | | Claude | 2026-02-05 | PASS |
| Audio Night Mode | ✅ | ✓ | Claude | 2026-02-05 | PASS (automated) |
| Audio Delay | ⚠️ | ✓ | Claude | 2026-02-05 | PARTIAL (AD-03) |
| Stereo Downmix | ✅ | ✓ | Claude | 2026-02-05 | PASS (automated) |
| Audio Passthrough | | ✓ | | | |
| HDR Fallback | | ✓ | | | |
| TV Channels | Partial | ✓ | | | |
| Refresh Rate | Partial | ✓ | | | |
| Subtitle Delay | ✅ | ✓ | Claude | 2026-02-05 | PASS (automated) |
| External Subtitles | ⚠️ | ✓ | Claude | 2026-02-05 | PARTIAL (ES-06) |
| Dream Service | | ✓ | | | |
| Trailer Autoplay | ✅ | ✓ | Claude | 2026-02-05 | PASS (automated) |
| OpenSubtitles | ✅ | ✓ | Claude | 2026-02-05 | PASS (automated) |
| Multiple Themes | ✅ | ✓ | Claude | 2026-02-05 | PASS (automated) |
| SyncPlay | Partial | ✓ | | | |

**Category 1 Results (2026-02-05):** 14 PASS, 2 PARTIAL - See `cat1-test-results-2026-02-05.md`
**Partial Tests:** See `tests-to-revisit.md` for AD-03 and ES-06 details

**Overall Status:** [ ] Ready for Release / [x] Needs Work (Manual tests pending)
