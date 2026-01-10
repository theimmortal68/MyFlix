# MyFlix

[![Build and Test](https://github.com/theimmortal68/MyFlix/actions/workflows/test.yml/badge.svg)](https://github.com/theimmortal68/MyFlix/actions/workflows/test.yml)

A modular Jellyfin client for Android with separate TV and Mobile apps.

## Features

### TV App
- Netflix-style home screen with hero section and content rows
- D-pad navigation with focus management
- Dynamic backdrop with color extraction
- Continue Watching, Next Up, and Recently Added rows
- Detail screen with season/episode browsing
- Video player with ExoPlayer (Dolby Vision) and MPV (HDR10/SDR)
- Quick Connect and QR code authentication
- Server discovery via UDP broadcast
- Playback progress reporting to Jellyfin

### Mobile App
- Responsive home screen with auto-rotating hero carousel
- Dropdown navigation menu
- Adaptive layout for phones, foldables, and tablets
- Progress indicators on continue watching cards
- Episode badges on wide cards
- Touch-optimized media cards
- PlaybackService for background/foreground playback
- Playback progress reporting to Jellyfin

### Shared Features
- Jellyfin API client with caching
- Automatic server discovery
- Resume playback from last position
- Real-time playback state sync

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 21
- Android SDK 36 (compile/target)
- Min SDK: 25 (Android 7.1)

## Getting Started

```bash
# Clone the repository
git clone https://github.com/theimmortal68/MyFlix.git
cd MyFlix

# Build TV app
./gradlew :app-tv:assembleDebug

# Build Mobile app
./gradlew :app-mobile:assembleDebug

# Install to device
adb install app-tv/build/outputs/apk/debug/app-tv-debug.apk
adb install app-mobile/build/outputs/apk/debug/app-mobile-debug.apk

# Run tests
./gradlew test
```

## Architecture

```
MyFlix/
├── app-tv/          # Android TV app (Compose TV Material)
├── app-mobile/      # Phone/tablet app (Material3)
├── core/
│   ├── common/      # Shared models, utilities
│   ├── network/     # Ktor-based Jellyfin API client
│   ├── data/        # DataStore preferences
│   └── player/      # ExoPlayer + MPV abstraction
└── ui/
    ├── common/      # Shared theme, colors
    ├── tv/          # TV-specific components
    └── mobile/      # Mobile-specific components
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.3 |
| UI (TV) | Jetpack Compose TV Material 3 |
| UI (Mobile) | Jetpack Compose Material 3 |
| Networking | Ktor 3.3 (OkHttp engine) |
| Serialization | kotlinx.serialization |
| Image Loading | Coil 3.3 |
| Video Player | Media3 ExoPlayer 1.9 + libmpv |
| Navigation | Jetpack Navigation Compose |
| Build | Gradle 8.13, AGP 8.13 |

## Documentation

For detailed development documentation, architecture decisions, and API patterns, see [CLAUDE.md](.claude/CLAUDE.md).

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

## License

TBD
