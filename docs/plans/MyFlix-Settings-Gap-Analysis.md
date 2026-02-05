# MyFlix Settings & Convenience Feature Gap Analysis

**Date:** February 4, 2026  
**Analysis Scope:** Settings, UI conveniences, and minor features from Wholphin, Moonfin, VoidTV, and Elefin

---

## Current MyFlix Settings (Already Implemented)

### Servers
- ✅ Multi-server support with switching
- ✅ Add/remove servers

### Home Screen
- ✅ Upcoming Episodes (Season Premieres)
- ✅ Genre Rows (with selection)
- ✅ Pinned Collections
- ✅ Universes (franchises)
- ✅ Discover (Seerr) nav toggle
- ✅ Suggestions
- ✅ Recent Seerr Requests

### Display
- ✅ Hide Watched from Recently Added

### Playback
- ✅ MPV/ExoPlayer selection
- ✅ Skip Intro (Off/Ask/Auto)
- ✅ Skip Credits (Off/Ask/Auto)
- ✅ Preferred Audio Language
- ✅ Preferred Subtitle Language
- ✅ Max Streaming Bitrate
- ✅ Skip Forward/Backward durations
- ✅ Refresh Rate Matching (Off/Seamless/Always)
- ✅ Audio Passthrough
- ✅ Resolution Matching
- ✅ Audio Night Mode (DRC)
- ✅ Stereo Downmix
- ✅ Audio Delay
- ✅ Subtitle Delay
- ✅ External Subtitles
- ✅ **Subtitle Styling (partial)** - Font size, color, background opacity already in player menu with persistence

### Device
- ✅ HDR capabilities display

### About
- ✅ Version info
- ✅ Update checker

---

## Missing Features (Prioritized)

### 🔴 HIGH PRIORITY - Actual Feature Work

#### 1. Auto-Play Next Episode
**Found in:** Wholphin, Moonfin  
**Type:** NEW FEATURE  
**Effort:** 2-3 days

MyFlix has Next Up data but doesn't auto-play when an episode ends.

**What's needed:**
- Next Up overlay with countdown timer (shows when episode ends)
- Auto-advance to next episode after countdown
- Cancel button to stop auto-play

**Settings to add (simple toggles):**
- `autoPlayNextEnabled` - On/Off
- `autoPlayNextDelaySeconds` - 5-30 seconds

---

#### 2. Subtitle Styling - Dedicated Settings Screen
**Found in:** Wholphin, Moonfin  
**Type:** EXPAND EXISTING  
**Effort:** 1 day

MyFlix already has subtitle font size, color, and background opacity in the player menu. This just needs a dedicated settings screen to configure defaults without being in playback.

**Additional options to add:**
- Edge style (None / Outline / Shadow)
- Edge color
- Font bold toggle

---

#### 3. Pass-Out Protection / Still Watching
**Found in:** Wholphin, Moonfin  
**Type:** NEW FEATURE  
**Effort:** 1 day

Prompt "Still watching?" after continuous playback.

**Implementation:**
- Track continuous playback time in PlayerViewModel
- Show dialog after threshold (e.g., 2 hours)
- Pause if no response within 60 seconds

**Settings:** Single slider (0-3 hours, 0=disabled)

---

#### 4. Debug Info Overlay
**Found in:** Wholphin, Moonfin  
**Type:** NEW FEATURE  
**Effort:** 0.5 days

Show codec/bitrate/resolution/fps during playback for troubleshooting.

**Implementation:**
- Gather stats from ExoPlayer/MPV
- Overlay composable in player screen
- Toggle via long-press or menu

**Settings:** Simple toggle

---

#### 5. Theme Music on Browse
**Found in:** Wholphin, Moonfin, VoidTV  
**Type:** NEW FEATURE  
**Effort:** 2-3 days (complex)

Play show/movie theme music when browsing detail screens.

**Implementation:**
- Fetch theme music URL from Jellyfin (`ThemeSongs` field)
- Lightweight MediaPlayer for background audio
- Fade in/out on navigation
- Volume control

**Settings:** Off / Low / Medium / High volume

---

### 🟡 MEDIUM PRIORITY - Simple Settings (toggles/preferences only)

These are straightforward preference additions with minimal code changes:

| Setting | Description | Effort |
|---------|-------------|--------|
| **Controller Timeout** | How long player controls stay visible (3-15s) | 30 min |
| **Skip Back on Resume** | Rewind X seconds when resuming playback (0-10s) | 30 min |
| **Default Zoom Mode** | Fit/Fill/Crop preference (player already has zoom, need default) | 30 min |
| **Merge Continue + Next Up** | Single row toggle for home screen | 30 min |
| **Rewatch Next Up** | Include fully-watched series in Next Up API call | 15 min |
| **Remember Selected Tab** | Return to last nav item on app launch | 30 min |

---

### 🟢 LOW PRIORITY - Minor Toggles

These are very simple on/off preferences:

| Setting | Description | Effort |
|---------|-------------|--------|
| **Watched Indicator** | Always / On Focus / Never - when to show checkmark | 15 min |
| **Series Thumbnails** | Use episode screenshots instead of posters in rows | 15 min |
| **One-Click Pause** | Single click pauses vs shows controls | 15 min |
| **Confirm Exit** | "Are you sure?" dialog when exiting app | 15 min |
| **Clock Display** | Always / In Player / Never | 30 min |
| **Direct Play ASS** | Allow or burn-in ASS subtitles | 15 min |
| **Direct Play PGS** | Allow or burn-in PGS subtitles | 15 min |
| **Max Items Per Row** | 5-50 items per home row | 15 min |
| **Clear Image Cache** | Button to clear Coil cache | 15 min |

---

### 🔵 OPTIONAL - Advanced/Niche

| Feature | Description | Effort |
|---------|-------------|--------|
| **Screensaver Settings** | Age rating filter, dimming, mode selection for Dream Service | 1 day |
| **MPV Advanced** | Hardware decoding toggle, gpu-next, custom mpv.conf | 1 day |
| **Backdrop Blur Options** | Blur amount for detail screen backgrounds | 0.5 days |
| **OSS Licenses** | View open source licenses | 0.5 days |

---

## Recommended Implementation Order

### Phase 1 - High Value Features (4-5 days)
1. **Auto-Play Next Episode** ⭐ - Most requested feature for binge-watching
2. **Subtitle Settings Screen** - Expose existing functionality to settings
3. **Pass-Out Protection** - Useful for TV viewers who fall asleep

### Phase 2 - Quick Wins (1-2 days)
Add all the simple toggles from the Medium Priority list:
- Controller Timeout
- Skip Back on Resume  
- Default Zoom Mode
- Merge Continue + Next Up
- Rewatch Next Up
- Remember Selected Tab

### Phase 3 - Polish (1 day)
Add the minor Low Priority toggles as time permits.

### Phase 4 - Optional
- Theme Music (complex, consider skipping)
- Screensaver Settings
- MPV Advanced Settings

---

## Summary

| Category | Items | Effort |
|----------|-------|--------|
| 🔴 New Features | 5 | 6-8 days |
| 🟡 Simple Settings | 6 | 2-3 hours |
| 🟢 Minor Toggles | 9 | 2-3 hours |
| 🔵 Optional/Advanced | 4 | 2-3 days |

**Total if doing everything:** ~10-14 days  
**Recommended minimum:** Phase 1 + 2 = ~5-6 days

**Key Insight:** Most "missing settings" from competitors are just simple preference toggles that take 15-30 minutes each. The real feature work is Auto-Play Next Episode and Theme Music.
