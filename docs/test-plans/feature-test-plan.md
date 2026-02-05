# MyFlix Feature Test Plan

**Version:** 1.0
**Date:** 2026-02-04
**Scope:** All features added since commit b6e3582

---

## Overview

This document provides a comprehensive test plan for 15 features implemented in MyFlix. Each feature includes preconditions, test cases, expected results, and verification steps.

---

## 1. Image Caching Optimization

**Location:** Settings > Advanced (or automatic)

### Preconditions
- App installed with sufficient storage
- Library with multiple items with artwork

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| IC-01 | First load caching | 1. Clear app data 2. Open home screen 3. Scroll through library | Images load from network, then are cached |
| IC-02 | Cached image retrieval | 1. Navigate away from home 2. Return to home | Images load instantly from cache |
| IC-03 | Cache persistence | 1. Close app completely 2. Reopen app | Previously cached images load instantly |
| IC-04 | Large library performance | 1. Scroll rapidly through 500+ items | No OOM errors, smooth scrolling |

### Verification
- Check logcat for cache hit/miss messages
- Monitor memory usage during scrolling

---

## 2. Audio Night Mode (Dynamic Range Compression)

**Location:** Settings > Audio > Night Mode

### Preconditions
- Content with dynamic audio range (action movies recommended)
- External speaker or headphones for clear audio comparison

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| NM-01 | Enable night mode | 1. Go to Settings > Audio 2. Enable Night Mode 3. Play content with explosions/quiet dialogue | Loud sounds compressed, quiet sounds boosted |
| NM-02 | Disable night mode | 1. Disable Night Mode 2. Play same content | Full dynamic range restored |
| NM-03 | Persistence | 1. Enable Night Mode 2. Close app 3. Reopen and play content | Night Mode remains enabled |
| NM-04 | Quick toggle during playback | 1. During playback, toggle night mode | Audio changes immediately without interruption |

### Verification
- A/B comparison on same scene
- Loud scenes should be noticeably quieter with night mode

---

## 3. Audio Delay Adjustment

**Location:** Player > Options > Audio Delay (or Settings > Audio > Audio Delay)

### Preconditions
- Content where lip sync issues are visible
- Display/soundbar setup that introduces audio latency

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| AD-01 | Positive delay | 1. Open audio delay setting 2. Set +200ms 3. Observe lip sync | Audio delayed by 200ms |
| AD-02 | Negative delay | 1. Set -200ms | Audio advanced by 200ms |
| AD-03 | Range limits | 1. Try to set beyond max/min | Value clamped to valid range |
| AD-04 | Reset to zero | 1. Adjust delay 2. Reset to 0ms | Audio returns to original sync |
| AD-05 | Per-playback vs global | 1. Adjust during playback 2. Start new content | Verify if delay persists or resets per user preference |

### Verification
- Use content with visible dialogue for lip sync check
- Delay should be noticeable in 100ms increments

---

## 4. Stereo Downmix

**Location:** Settings > Audio > Stereo Downmix

### Preconditions
- Content with 5.1/7.1 surround audio
- Stereo output device (TV speakers, headphones)

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| SD-01 | Enable downmix | 1. Enable Stereo Downmix 2. Play 5.1 content on stereo device | All channels mixed to stereo, no missing audio |
| SD-02 | Center channel audibility | 1. Play dialogue-heavy scene | Dialogue (center channel) clearly audible |
| SD-03 | Surround effects | 1. Play scene with surround effects | Surround effects mixed into stereo field |
| SD-04 | Disable downmix | 1. Disable setting 2. Connect to surround system | Full surround output when available |

### Verification
- Compare same scene with setting on/off
- Ensure no audio channels are lost

---

## 5. Per-Codec Audio Passthrough

**Location:** Settings > Audio > Passthrough

### Preconditions
- AV receiver or soundbar supporting various codecs
- Content with different audio codecs (AAC, AC3, EAC3, DTS, TrueHD, DTS-HD)

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| PT-01 | AC3 passthrough | 1. Enable AC3 passthrough 2. Play AC3 content | Receiver shows "Dolby Digital" |
| PT-02 | EAC3 passthrough | 1. Enable EAC3 passthrough 2. Play EAC3 content | Receiver shows "Dolby Digital Plus" |
| PT-03 | DTS passthrough | 1. Enable DTS passthrough 2. Play DTS content | Receiver shows "DTS" |
| PT-04 | TrueHD passthrough | 1. Enable TrueHD passthrough 2. Play TrueHD content | Receiver shows "Dolby TrueHD" |
| PT-05 | DTS-HD passthrough | 1. Enable DTS-HD passthrough 2. Play DTS-HD content | Receiver shows "DTS-HD MA" |
| PT-06 | Selective disable | 1. Disable specific codec 2. Play that format | Audio decoded locally, not passed through |
| PT-07 | Unsupported codec fallback | 1. Enable passthrough for unsupported codec 2. Play content | Falls back to decode gracefully |

### Verification
- Check receiver display for codec indicator
- Ensure no audio dropouts or artifacts

---

## 6. HDR Fallback for Buggy Dolby Vision

**Location:** Settings > Video > HDR Fallback

### Preconditions
- Device with known Dolby Vision playback issues
- Content with Dolby Vision + HDR10 fallback track

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| HF-01 | Enable HDR fallback | 1. Enable HDR Fallback 2. Play Dolby Vision content | Content plays as HDR10 instead |
| HF-02 | Disable HDR fallback | 1. Disable HDR Fallback 2. Play same content | Content plays as Dolby Vision |
| HF-03 | Non-DV content | 1. Enable HDR Fallback 2. Play HDR10 content | No change, plays normally |
| HF-04 | SDR content | 1. Play SDR content with setting enabled | No impact on SDR playback |

### Verification
- Check display info overlay for active HDR format
- Verify no green tint or color issues on problematic devices

---

## 7. TV Channels / Watch Next Integration

**Location:** Android TV home screen channels

### Preconditions
- Android TV device (8.0+)
- Content in Continue Watching / Next Up

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| TC-01 | Continue Watching channel | 1. Watch partial content 2. Go to Android TV home | "Continue Watching" row appears |
| TC-02 | Next Up channel | 1. Watch TV episode to completion 2. Check home | Next episode appears in Watch Next |
| TC-03 | Channel click navigation | 1. Click item in TV channel | App opens to correct content |
| TC-04 | Channel update on progress | 1. Watch more of an item 2. Check channel | Progress updated in channel |
| TC-05 | Remove from channel | 1. Mark item as watched 2. Check channel | Item removed from Continue Watching |

### Verification
- Check Android TV home screen for MyFlix channels
- Verify deep links work correctly

---

## 8. Refresh Rate Management

**Location:** Settings > Video > Refresh Rate Mode

### Preconditions
- TV supporting multiple refresh rates (24Hz, 50Hz, 60Hz, 120Hz)
- Content at various frame rates (24fps, 25fps, 30fps, 60fps)

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| RR-01 | OFF mode | 1. Set to OFF 2. Play 24fps content | Display stays at system default |
| RR-02 | SEAMLESS mode | 1. Set to SEAMLESS 2. Play 24fps content | Display switches without black screen if possible |
| RR-03 | ALWAYS mode | 1. Set to ALWAYS 2. Play 24fps content | Display switches to 24Hz (may black screen) |
| RR-04 | 60fps content | 1. Play 60fps content with SEAMLESS | Display at 60Hz or 120Hz |
| RR-05 | Restore on exit | 1. Start playback (rate changes) 2. Exit playback | Display returns to original rate |
| RR-06 | Integer multiple preference | 1. Play 24fps on 120Hz display | Prefers 120Hz over 24Hz for smoother motion |

### Verification
- Use display info to check current refresh rate
- Observe for judder on 24fps content

---

## 9. Subtitle Delay Adjustment

**Location:** Player > Options > Subtitle Delay

### Preconditions
- Content with external or embedded subtitles
- Content where subtitle timing is off

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| SB-01 | Positive delay | 1. Set subtitle delay +500ms | Subtitles appear 500ms later |
| SB-02 | Negative delay | 1. Set subtitle delay -500ms | Subtitles appear 500ms earlier |
| SB-03 | Real-time adjustment | 1. Adjust delay during playback | Change applies immediately |
| SB-04 | Reset delay | 1. Reset to 0ms | Subtitles return to original timing |
| SB-05 | Delay with styled subtitles | 1. Apply delay to ASS/SSA subs | Styling preserved, timing adjusted |

### Verification
- Compare subtitle appearance to dialogue
- Check with known out-of-sync content

---

## 10. External Subtitle Support

**Location:** Player > Options > Subtitles > Add External

### Preconditions
- External subtitle file (.srt, .ass, .ssa, .vtt) accessible
- Content to apply subtitles to

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| ES-01 | Load SRT file | 1. Select Add External 2. Choose .srt file | Subtitles load and display |
| ES-02 | Load ASS file | 1. Choose .ass file | Styled subtitles with formatting |
| ES-03 | Load VTT file | 1. Choose .vtt file | WebVTT subtitles display |
| ES-04 | Encoding handling | 1. Load non-UTF8 subtitle file | Characters display correctly |
| ES-05 | Switch between tracks | 1. Load external sub 2. Switch to embedded 3. Switch back | Both tracks work correctly |
| ES-06 | Invalid file handling | 1. Try to load non-subtitle file | Error message, graceful failure |

### Verification
- Verify text displays correctly
- Check special characters and formatting

---

## 11. Dream Service Now Playing

**Location:** Automatic during device screensaver/dream

### Preconditions
- Android TV device with Dream/Screensaver enabled
- Recent playback history

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| DS-01 | Now playing display | 1. Play content 2. Pause and wait for dream | Currently playing info shown in dream |
| DS-02 | Artwork display | 1. Trigger dream during playback | Poster/backdrop artwork visible |
| DS-03 | No playback state | 1. Clear recent playback 2. Trigger dream | Graceful fallback (clock or generic) |
| DS-04 | Dream dismissal | 1. During dream, press button | Returns to app correctly |

### Verification
- Wait for dream to activate
- Check displayed metadata accuracy

---

## 12. Inline Trailer Autoplay

**Location:** Detail screens with trailers

### Preconditions
- Content with associated trailers (movies with YouTube trailers)
- Network connection for trailer loading

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| TA-01 | Autoplay on focus | 1. Navigate to movie with trailer 2. Wait on detail screen | Trailer begins playing inline |
| TA-02 | Muted playback | 1. Observe autoplay trailer | Trailer plays muted by default |
| TA-03 | Unmute on interaction | 1. Click/select trailer | Audio unmutes, fullscreen option |
| TA-04 | No trailer available | 1. View content without trailer | No autoplay, no errors |
| TA-05 | Trailer stops on navigate | 1. During trailer, navigate away | Trailer stops, no background audio |

### Verification
- Check detail screens for various content
- Verify no unwanted audio

---

## 13. OpenSubtitles Search

**Location:** Player > Options > Search Subtitles (requires Jellyfin plugin)

### Preconditions
- OpenSubtitles plugin configured in Jellyfin server
- Content without embedded subtitles

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| OS-01 | Search subtitles | 1. Open subtitle search 2. View results | Available subtitles listed |
| OS-02 | Language filter | 1. Filter by language | Only matching languages shown |
| OS-03 | Download subtitle | 1. Select subtitle 2. Confirm download | Subtitle downloads and applies |
| OS-04 | Search no results | 1. Search for obscure content | "No subtitles found" message |
| OS-05 | Server plugin missing | 1. Search with plugin disabled | Graceful error message |

### Verification
- Check Jellyfin server logs for API calls
- Verify downloaded subtitle displays correctly

---

## 14. Multiple Theme Presets

**Location:** Settings > Appearance > Theme

### Preconditions
- App installed

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| TH-01 | Default theme | 1. Select Default theme | Standard dark theme applied |
| TH-02 | OLED Dark theme | 1. Select OLED Dark | Pure black backgrounds |
| TH-03 | High Contrast theme | 1. Select High Contrast | Enhanced contrast, accessibility |
| TH-04 | Theme persistence | 1. Set theme 2. Close/reopen app | Theme remains selected |
| TH-05 | All screens themed | 1. Navigate through app with each theme | Consistent theming everywhere |
| TH-06 | Player UI theming | 1. Enter player with different themes | Player controls match theme |

### Verification
- Visual inspection of all major screens
- Check OLED Dark has true #000000 blacks

---

## 15. SyncPlay Synchronized Playback

**Location:** Player > SyncPlay button (or Settings > SyncPlay)

### Preconditions
- Multiple devices with MyFlix installed
- Same Jellyfin server connection
- Same content available on all devices

### Test Cases

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| SP-01 | Create group | 1. Open SyncPlay 2. Create new group | Group created, creator is member |
| SP-02 | Join group | 1. On device 2, open SyncPlay 2. Select existing group | Joins group, syncs position |
| SP-03 | Synchronized play | 1. Host presses play | All devices start within 100ms |
| SP-04 | Synchronized pause | 1. Any member pauses | All devices pause together |
| SP-05 | Synchronized seek | 1. Any member seeks | All devices seek to same position |
| SP-06 | Member list display | 1. Open SyncPlay overlay | All group members listed |
| SP-07 | Leave group | 1. Member leaves group | Removed from group, playback independent |
| SP-08 | Group disbanded | 1. Creator leaves/disbands | All members notified, group ends |
| SP-09 | Drift correction | 1. Play for extended period | Positions stay within 500ms |
| SP-10 | Buffering handling | 1. One device buffers | Group waits for buffering device |
| SP-11 | Late joiner | 1. Join during playback | Syncs to current position |
| SP-12 | Network interruption | 1. Briefly disconnect device | Reconnects and resyncs |

### Verification
- Use two devices side-by-side
- Check in-player sync status indicator
- Verify member list updates

---

## Test Environment

### Recommended Devices

| Device Type | Model | Purpose |
|-------------|-------|---------|
| Android TV | NVIDIA Shield | Primary TV testing |
| Android TV | Chromecast with Google TV | Secondary TV testing |
| Tablet | Any Android tablet | Mobile/tablet testing |
| AV Receiver | Any with HDMI eARC | Passthrough testing |

### Test Content Matrix

| Content Type | Example | Features Tested |
|--------------|---------|-----------------|
| 4K HDR Movie | Any 4K Blu-ray rip | HDR fallback, refresh rate |
| 1080p Movie | Standard movie | General playback |
| TV Episode | Any series | Next Up, Watch Next |
| 24fps Film | Classic film | Refresh rate matching |
| 60fps Content | Sports/gaming | High refresh content |
| 5.1 Audio | Action movie | Passthrough, downmix |
| Dolby Atmos | Recent blockbuster | Audio passthrough |
| External Subs | User-provided .srt | External subtitle |
| No Subs | Obscure content | OpenSubtitles search |

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

When filing bugs, include:

```
**Feature:** [Feature name]
**Test Case ID:** [e.g., SP-03]
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

| Feature | Tester | Date | Status |
|---------|--------|------|--------|
| Image Caching | | | |
| Audio Night Mode | | | |
| Audio Delay | | | |
| Stereo Downmix | | | |
| Audio Passthrough | | | |
| HDR Fallback | | | |
| TV Channels | | | |
| Refresh Rate | | | |
| Subtitle Delay | | | |
| External Subtitles | | | |
| Dream Service | | | |
| Trailer Autoplay | | | |
| OpenSubtitles | | | |
| Multiple Themes | | | |
| SyncPlay | | | |

**Overall Status:** [ ] Ready for Release / [ ] Needs Work
