# Tests to Revisit

This document tracks tests that were only partially completed and require additional runtime verification in the future.

---

## AD-03: Audio Delay Range Limits

**Status:** PARTIAL
**Category:** Preference Persistence (Automated/Emulator)
**Date Tested:** 2026-02-05

### What Was Verified
- `DelayAudioProcessor` class exists in `core/player`
- Audio delay is applied via ExoPlayer's audio processing pipeline
- `setAudioDelayMs()` method accepts delay values in milliseconds

### What Needs Runtime Verification
- **Range clamping behavior**: Verify that values outside ±2000ms (or configured max) are clamped
- **UI enforcement**: Verify that the slider/input in settings enforces the range limits
- **Edge cases**: Test boundary values (exactly ±2000ms, values at ±2001ms)

### How to Test
1. Launch app on emulator or device
2. Navigate to Settings > Audio > Audio Delay
3. Attempt to set values outside the allowed range:
   - Try setting +3000ms (should clamp to +2000ms)
   - Try setting -3000ms (should clamp to -2000ms)
4. Verify the actual applied delay matches the clamped value

### Files to Check
- `core/player/src/main/java/dev/jausc/myflix/core/player/audio/DelayAudioProcessor.kt`
- Player settings UI component (audio delay slider)

### Priority
Low - This is a safety check for edge cases. Normal usage stays within the valid range.

---

## ES-06: Invalid Subtitle File Error Handling

**Status:** PARTIAL
**Category:** Error Handling (Automated/Emulator)
**Date Tested:** 2026-02-05

### What Was Verified
- **Load errors** (network failures, timeouts): Handled by `LoadErrorHandlingPolicy` with up to 3 retries before giving up
- **Parse errors** (malformed content): ExoPlayer's default behavior logs a warning and continues playback without subtitles
- **No crash**: The player architecture allows video playback to continue even if subtitle loading fails

### What Needs Runtime Verification
- **Actual malformed subtitle injection**: Test with a corrupted .srt/.vtt file
- **User feedback**: Verify user receives appropriate feedback when subtitles fail to load
- **Graceful degradation**: Confirm video playback continues normally

### How to Test
1. Create test subtitle files:
   ```
   # malformed.srt (invalid timestamps)
   1
   INVALID_TIMESTAMP
   This is malformed subtitle text

   # empty.srt (empty file)
   [empty file]

   # binary.srt (binary data instead of text)
   [random binary bytes]
   ```

2. Host test files on a local server or use Jellyfin's external subtitle feature

3. Test scenarios:
   - Load video and select malformed external subtitle
   - Verify video continues playing
   - Check logcat for error messages: `adb logcat | grep -i "subtitle\|parse\|error"`
   - Verify no ANR or crash occurs

### Files to Check
- `core/player/src/main/java/dev/jausc/myflix/core/player/ExoPlayerWrapper.kt` (lines 947-978: LoadErrorHandlingPolicy)
- Subtitle loading UI component

### Priority
Medium - Users may encounter malformed subtitles from external sources (OpenSubtitles downloads, etc.).

### Notes
ExoPlayer's default behavior for subtitle parsing errors is to log a warning and continue playback without displaying subtitles. This is acceptable graceful degradation, but we should verify the user experience (no confusing error dialogs, etc.).

---

## Future Test Sessions

When revisiting these tests:

1. **Environment needed**: Android TV emulator (Sony_85_TV) or real device
2. **App version**: Ensure testing latest debug build
3. **Test data**: Prepare malformed subtitle files before testing
4. **Logging**: Enable verbose logging for subtitle and audio components

### Commands for Test Setup

```bash
# Launch emulator
emulator -avd Sony_85_TV &

# Wait for boot
adb wait-for-device && adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'

# Install latest APK
adb install -r app-tv/build/outputs/apk/debug/app-tv-universal-debug.apk

# Monitor relevant logs
adb logcat | grep -E "(DelayAudioProcessor|subtitle|Subtitle|ExoPlayer.*error)"
```
