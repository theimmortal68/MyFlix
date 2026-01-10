# MyFlix

[![Build and Test](https://github.com/theimmortal68/MyFlix/actions/workflows/test.yml/badge.svg)](https://github.com/theimmortal68/MyFlix/actions/workflows/test.yml)

A modular Jellyfin client for Android with separate TV and Mobile apps.

## Features

### TV App
- Netflix-style home screen with hero section and content rows
- D-pad navigation with focus management
- Dynamic backdrop with color extraction
- Continue Watching, Next Up, and Latest content rows
- Detail screen with season/episode browsing
- Video player with ExoPlayer and MPV support
- Quick Connect and QR code authentication
- Server discovery via UDP broadcast

### Mobile App
- Responsive home screen with swipeable hero carousel
- Dropdown navigation menu
- Adaptive layout for phones, foldables, and tablets
- Progress indicators on continue watching cards
- Touch-optimized media cards
- PlaybackService for background playback

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 21
- Android SDK 35
- Min SDK: 31 (Android 12)

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
| UI (TV) | Jetpack Compose TV Material 3 |
| UI (Mobile) | Jetpack Compose Material 3 |
| Networking | Ktor (OkHttp engine) |
| Serialization | kotlinx.serialization |
| Image Loading | Coil 3 |
| Video Player | Media3 ExoPlayer + MPV |
| Navigation | Jetpack Navigation Compose |

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
