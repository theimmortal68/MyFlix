# MPV Player Integration

This module supports MPV as an optional video player backend. MPV provides superior codec support and video processing compared to ExoPlayer.

## Current Status

- **ExoPlayer**: Works out of the box (default)
- **MPV**: Requires native libraries (see below)

## Enabling MPV

To enable MPV playback, you need to provide the native libraries.

### Option 1: Extract from mpv-android APK (Easiest)

1. Download the latest mpv-android APK from:
   - [GitHub Releases](https://github.com/mpv-android/mpv-android/releases)
   - [F-Droid](https://f-droid.org/packages/is.xyz.mpv/)

2. Extract the APK (it's a ZIP file):
   ```bash
   unzip mpv-android-*.apk -d mpv-extracted
   ```

3. Copy the native libraries:
   ```bash
   mkdir -p core/player/src/main/jniLibs
   cp -r mpv-extracted/lib/* core/player/src/main/jniLibs/
   ```

   You should have:
   ```
   core/player/src/main/jniLibs/
   ├── arm64-v8a/
   │   └── libmpv.so
   ├── armeabi-v7a/
   │   └── libmpv.so
   └── x86_64/
       └── libmpv.so
   ```

### Option 2: Build from Source

1. Clone mpv-android:
   ```bash
   git clone https://github.com/mpv-android/mpv-android
   ```

2. Follow their build instructions in `buildscripts/README.md`

3. Copy the resulting `.so` files to `core/player/src/main/jniLibs/`

## Architecture

```
core/player/
├── src/main/
│   ├── java/dev/jausc/myflix/core/player/
│   │   ├── UnifiedPlayer.kt      # Player interface
│   │   ├── PlaybackState.kt      # Shared state
│   │   ├── MpvPlayer.kt          # MPV implementation
│   │   ├── ExoPlayerWrapper.kt   # ExoPlayer implementation
│   │   ├── PlayerController.kt   # Auto-selects backend
│   │   └── mpv/
│   │       └── MPVLib.kt         # JNI wrapper
│   ├── jni/                      # Native C++ code
│   │   ├── CMakeLists.txt
│   │   ├── main.cpp
│   │   ├── event.cpp
│   │   ├── property.cpp
│   │   ├── render.cpp
│   │   └── mpv/client.h          # MPV API header
│   └── jniLibs/                  # Place .so files here
│       ├── arm64-v8a/
│       ├── armeabi-v7a/
│       └── x86_64/
```

## How It Works

1. `PlayerController` checks if MPV libraries are available at runtime
2. If available, uses `MpvPlayer`; otherwise falls back to `ExoPlayerWrapper`
3. Both implement `UnifiedPlayer` interface for consistent API
4. UI code doesn't need to know which backend is active

## MPV vs ExoPlayer

| Feature | MPV | ExoPlayer |
|---------|-----|-----------|
| Codec support | Excellent (via FFmpeg) | Good |
| HDR | Yes | Limited |
| Audio filters | Yes (DRC, normalize) | Limited |
| Subtitle rendering | libass (high quality) | Basic |
| Hardware decode | Yes | Yes |
| Native library required | Yes | No |
| APK size impact | +15-20MB | None |

## Troubleshooting

### "MPV libraries not found"
- Ensure `.so` files are in the correct `jniLibs/{abi}/` folders
- Check that the ABI matches your device (arm64-v8a for most modern phones)

### Build errors about missing headers
- The JNI code requires libmpv headers at compile time
- Headers are included in `src/main/jni/mpv/`

### Crashes on startup
- Make sure libmpv.so is built for the correct Android API level
- Check logcat for native crashes
