# MyFlix - Multi-Platform Jellyfin Client

A modular Android app for Jellyfin with separate TV and Mobile builds.

## Project Structure

```
MyFlix/
├── app-tv/                    # Android TV app (Compose TV, D-pad navigation)
├── app-mobile/                # Phone/tablet app (Material3)
├── core/
│   ├── common/                # Shared models, utilities
│   ├── network/               # Ktor-based Jellyfin API client
│   ├── data/                  # DataStore preferences, AppState
│   └── player/                # Player interface (MPV/ExoPlayer)
└── ui/
    ├── common/                # Shared theme, colors
    ├── tv/                    # TV-specific components
    └── mobile/                # Mobile-specific components
```

## Building

### TV App
```bash
./gradlew :app-tv:assembleDebug
adb install app-tv/build/outputs/apk/debug/app-tv-debug.apk
```

### Mobile App
```bash
./gradlew :app-mobile:assembleDebug
adb install app-mobile/build/outputs/apk/debug/app-mobile-debug.apk
```

## Key Features

- **Shared Core**: Business logic and API client shared between platforms
- **Platform-Optimized UI**: TV uses Compose TV Material, Mobile uses Material3
- **MPV Player**: Ready for libmpv integration (DV support on Homatics Box R 4K)
- **ExoPlayer**: Mobile fallback with broader device compatibility
- **In-Memory Cache**: Fast response times with smart cache invalidation

## Next Steps

1. Add libmpv native library to core:player for TV
2. Implement remaining screens (Detail, Library, Player)
3. Add search functionality
4. Implement playback progress reporting
5. Add settings screen
