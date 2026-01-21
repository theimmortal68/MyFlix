# MyFlix

Multi-platform Jellyfin client for Android with dedicated TV and Mobile apps, built with Kotlin and Jetpack Compose.

## Modules

- `app-tv` - Android TV app (Compose TV Material, D-pad navigation)
- `app-mobile` - Phone/tablet app (Material3)
- `core/common` - Shared models, UI utilities, fonts
- `core/network` - Ktor-based Jellyfin API client
- `core/data` - Auth and app state persistence
- `core/player` - Player abstraction (ExoPlayer + MPV)
- `core/seerr` - Jellyseerr/Overseerr client
- `ui/common`, `ui/tv`, `ui/mobile` - Shared UI placeholders

## Requirements

- Android Studio Ladybug or newer
- JDK 21
- Android SDK 36

## Build

```bash
# TV app
./gradlew :app-tv:assembleDebug

# Mobile app
./gradlew :app-mobile:assembleDebug
```

## Install (device/emulator)

```bash
adb install app-tv/build/outputs/apk/debug/app-tv-debug.apk
adb install app-mobile/build/outputs/apk/debug/app-mobile-debug.apk
```

## Test

```bash
./gradlew test
./gradlew :core:network:test
```

## Key Files

- Jellyfin API client: `core/network/src/main/java/dev/jausc/myflix/core/network/JellyfinClient.kt`
- App state: `core/data/src/main/java/dev/jausc/myflix/core/data/AppState.kt`
- Player abstraction: `core/player/src/main/java/dev/jausc/myflix/core/player/UnifiedPlayer.kt`
- TV entrypoint: `app-tv/src/main/java/dev/jausc/myflix/tv/MainActivity.kt`
- Mobile entrypoint: `app-mobile/src/main/java/dev/jausc/myflix/mobile/MainActivity.kt`

## Notes

- Manual dependency injection (no Hilt/Koin/Dagger).
- Compose state is managed in screens; shared state uses `StateFlow`.
- Detekt is enforced; fix rules rather than suppressing.

## Feature Checklist

### TV App
- [x] Home screen with hero section and content rows
- [x] Dynamic backdrop with color extraction
- [x] Top navigation bar (Home/Movies/Shows/Search/Discover/Settings)
- [x] Continue Watching row
- [x] Next Up row
- [x] Latest Movies/Shows/Episodes rows
- [x] Season Premieres row
- [x] Genre rows with randomization
- [x] Collection rows with pinning
- [x] Media cards (portrait + wide)
- [x] Long-press context menus on media cards
- [x] Detail screen
- [x] Player screen
- [x] Library screen
- [x] Search screen
- [x] Login screen
- [x] Server discovery (UDP)
- [x] Quick Connect authentication
- [x] QR code login display
- [x] Preferences screen with home customization
- [x] D-pad navigation and focus management
- [x] Background polling for content updates
- [x] Jellyseerr integration (setup, browse, request)
- [x] Jellyseerr advanced filters (sort, rating, year range)
- [x] Recent Requests row (optional, settings toggle)

### Mobile App
- [x] Home screen with responsive hero section
- [x] Dropdown navigation menu
- [x] Auto-rotating hero carousel
- [x] Responsive layout (phones, foldables, tablets)
- [x] Progress bars on Continue Watching cards
- [x] Episode badges on wide cards
- [x] Season Premieres, Genre, and Collection rows
- [x] Detail screen
- [x] Player screen with PlaybackService
- [x] Library screen
- [x] Search screen
- [x] Settings screen with home customization
- [x] Login screen
- [x] Splash screen
- [x] Jellyseerr integration (setup, browse, request)
- [x] Jellyseerr advanced filters (sort, rating, year range)
- [x] Recent Requests row (optional, settings toggle)

### Playback - Video
- [x] Dual player backend architecture (ExoPlayer + MPV)
- [x] Video zoom/aspect ratio (fit/crop/fill/stretch cycle)
- [x] HDR detection and handling
- [x] Dolby Vision support
- [x] Max bitrate/quality selection
- [x] Direct play preference
- [x] Subtitle styling customization
- [x] Resume playback position
- [ ] Refresh rate switching
- [ ] HDR fallback option

### Playback - Audio
- [x] Audio track selection
- [x] Audio language memory (remembered between episodes)
- [ ] Audio Night Mode (dynamic range compression)
- [ ] Audio passthrough (direct bitstream)
- [ ] Audio delay adjustment

### Playback - Advanced
- [x] Media Segments integration (skip intro/credits)
- [x] Configurable skip modes (Off/Ask/Auto) per segment type
- [x] Chapter selection in player
- [x] Configurable skip forward/back duration
- [x] Auto-play next episode with countdown
- [x] Theme music on detail screens (with fade effects)
- [x] Trailers row on detail screens (categorized extras)
- [ ] Trick play thumbnails during seek
- [ ] Configurable auto-play delay

### Home Screen
- [x] User-selectable collection rows
- [x] Collection picker settings UI
- [x] Genre rows with randomization
- [x] Genre picker settings UI
- [x] Section visibility toggles
- [x] Season Premieres row
- [x] Continue Watching row
- [x] Next Up row
- [x] Latest additions rows
- [x] Recent Requests row (Jellyseerr, optional)

### Search and Discovery
- [x] Text search
- [x] Voice search (TV)
- [x] Seerr discover filters (sort, rating, year range)
- [ ] Search history
- [ ] Search suggestions

### Server and Authentication
- [x] Server discovery (UDP)
- [x] Manual server entry
- [x] QR code login display
- [x] Multiple server support
- [x] Quick Connect support
- [x] Server switching UI

### Integrations
- [x] Jellyseerr integration (browse, request, admin)
- [x] OTA updates from GitHub releases
- [ ] Dream Service (screensaver)
- [ ] Android TV channels/recommendations

## Roadmap

### Near-term (Polish)
- Trick play thumbnails during seek
- Search history
- Theme/appearance options
- Cache management UI

### Mid-term (Enhanced Experience)
- Audio passthrough option
- Night mode (audio compression)
- Configurable auto-play delay

### Long-term (Platform Integration)
- Dream Service (screensaver)
- Android TV channels/recommendations
- Live TV support
- Refresh rate switching
