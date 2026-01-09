# MyFlix - Multi-Platform Jellyfin Client

A modular Jellyfin client for Android with separate TV and Mobile apps, built from scratch using Clean Architecture and modern Android development patterns.

> **Goal:** Achieve 100% feature parity with MyFlix-Wholphin (the legacy fork-based client) while using a cleaner, more maintainable architecture.

## Project Overview

**Package:** `dev.jausc.myflix`  
**Min SDK:** 31 (Android 12)  
**Target SDK:** 35  
**Compile SDK:** 35  
**Language:** Kotlin  
**Java:** 21

## Multi-Module Architecture

```
MyFlix/
├── app-tv/                    # Android TV app (Compose TV Material, D-pad navigation)
│   └── dev.jausc.myflix.tv/
│       ├── MainActivity.kt
│       ├── MyFlixApp.kt
│       ├── TvPreferences.kt
│       └── ui/
│           ├── screens/       # HomeScreen, DetailScreen, PlayerScreen, etc.
│           ├── components/    # MediaCard, HeroSection, TopNavigationBar, etc.
│           ├── theme/         # Colors, Theme
│           └── util/          # DynamicColorExtractor
│
├── app-mobile/                # Phone/tablet app (Material3)
│   └── dev.jausc.myflix.mobile/
│       ├── MainActivity.kt
│       ├── service/           # PlaybackService
│       └── ui/
│           ├── screens/
│           ├── components/
│           └── theme/
│
├── core/
│   ├── common/                # Shared models, utilities, fonts
│   │   └── dev.jausc.myflix.core.common/
│   │       ├── model/         # JellyfinModels.kt
│   │       └── ui/            # Fonts.kt, MyFlixLogo.kt
│   │
│   ├── network/               # Ktor-based Jellyfin API client
│   │   └── dev.jausc.myflix.core.network/
│   │       └── JellyfinClient.kt
│   │
│   ├── data/                  # App state management
│   │   └── dev.jausc.myflix.core.data/
│   │       └── AppState.kt
│   │
│   └── player/                # Video player abstraction (ExoPlayer + MPV)
│       └── dev.jausc.myflix.core.player/
│           ├── PlayerController.kt
│           ├── PlayerManager.kt
│           ├── UnifiedPlayer.kt
│           ├── ExoPlayerWrapper.kt
│           └── MpvPlayer.kt
│
└── ui/
    ├── common/                # Shared theme, colors
    ├── tv/                    # TV-specific components (placeholder)
    └── mobile/                # Mobile-specific components (placeholder)
```

## Key Technologies

| Component | Technology |
|-----------|------------|
| UI (TV) | Jetpack Compose TV Material 3 |
| UI (Mobile) | Jetpack Compose Material 3 |
| Networking | Ktor (OkHttp engine) + Kotlinx Serialization |
| Image Loading | Coil 3 (Compose-native) |
| Video Players | Media3 ExoPlayer + MPV |
| Navigation | Jetpack Navigation Compose |
| Preferences | DataStore |
| QR Codes | ZXing |
| Color Extraction | AndroidX Palette |

## Currently Implemented

### TV App (app-tv)
- [x] Home screen with hero section and content rows
- [x] Dynamic backdrop with color extraction
- [x] Top navigation bar (Home, Movies, Shows, Search, Settings)
- [x] Continue Watching row
- [x] Next Up row
- [x] Latest Movies/Shows/Episodes rows
- [x] Media cards (portrait + wide/landscape)
- [x] Hero section with featured items carousel
- [x] Detail screen
- [x] Player screen
- [x] Library screen
- [x] Login screen
- [x] Server discovery (UDP broadcast)
- [x] Quick Connect authentication
- [x] QR code login display
- [x] Preferences screen
- [x] D-pad navigation with focus management
- [x] Background polling for content updates

### Mobile App (app-mobile)
- [x] Home screen
- [x] Detail screen
- [x] Player screen with PlaybackService
- [x] Library screen
- [x] Search screen
- [x] Login screen
- [x] Splash screen

### Core Modules
- [x] JellyfinClient with caching (variable TTL)
- [x] Server discovery via UDP
- [x] Quick Connect flow
- [x] Playback reporting (start/progress/stop)
- [x] Image URL builders (WebP format)
- [x] Player abstraction layer
- [x] ExoPlayer wrapper
- [x] MPV player stub (ready for libmpv)

## Feature Parity Checklist (from MyFlix-Wholphin)

### Playback - Video
- [x] Dual player backend architecture (ExoPlayer + MPV)
- [ ] Video zoom/aspect ratio (Fit/Crop/Fill/Stretch cycle)
- [ ] HDR detection and handling
- [ ] Dolby Vision support
- [ ] Buffer mode selection (Low/Medium/High)
- [ ] External subtitles during direct play
- [ ] Subtitle styling customization
- [x] Resume playback position
- [ ] Background video (audio continues when backgrounded)

### Playback - Audio
- [ ] Audio Night Mode (dynamic range compression)
- [ ] Audio Delay Offset (configurable sync delay)
- [ ] Stereo Downmix (7.1/5.1 → stereo)
- [ ] DTS/TrueHD runtime capability detection
- [ ] DTS capability toggle in settings
- [ ] DTS-only audio delay mode
- [ ] Audio track selection and memory

### Playback - Advanced
- [ ] Media Segments integration (skip intro/outro/credits)
- [ ] Per-segment-type skip settings
- [ ] Theme music during browsing
- [ ] Remember audio/subtitle preferences per series
- [ ] Multi-version/multi-source support
- [ ] Collection Binge Mode (auto-queue next item)

### Home Screen
- [ ] User-selectable collection rows
- [ ] Collection picker settings UI
- [ ] Genre rows with randomization
- [ ] Genre picker settings UI
- [ ] Section visibility toggles
- [x] Continue Watching row
- [x] Next Up row
- [x] Latest additions rows

### Universe Collections Integration
- [ ] Detect universe-collection tagged BoxSets
- [ ] Query items by universe-collection:{slug} tag
- [ ] Collection visibility settings
- [ ] Conditional tabs based on collection tags

### Search & Discovery
- [ ] Voice search (SpeechRecognizer)
- [x] Text search (basic)
- [ ] Search suggestions
- [ ] Filter by type/genre/year

### UI & Branding
- [x] Purple gradient theme
- [x] Material Icons (extended)
- [x] TV focus indicators
- [x] Dynamic color extraction from backdrops
- [ ] Loading states and skeletons
- [x] Error handling UI

### Server & Authentication
- [x] Server discovery (UDP)
- [x] Manual server entry
- [x] QR code login display
- [ ] Multiple server support
- [x] Quick Connect support

### Integration Features
- [ ] Jellyseerr integration
- [ ] OTA updates from GitHub releases
- [ ] Dream Service (screensaver)
- [ ] Photo player/slideshow

## Jellyfin API Patterns

The client uses Ktor with custom endpoints (not jellyfin-sdk-kotlin):

```kotlin
// API client is passed to screens
@Composable
fun HomeScreen(
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit
)

// Get latest movies
jellyfinClient.getLatestMovies(libraryId, limit = 12)

// Get continue watching
jellyfinClient.getContinueWatching(limit = 12)

// Get stream URL for playback
jellyfinClient.getStreamUrl(itemId)

// Report playback progress
jellyfinClient.reportPlaybackProgress(itemId, positionTicks, isPaused)
```

### Caching
Variable TTL based on data volatility:
- Libraries: 5 min
- Item details: 2 min
- Resume/Continue Watching: 30 sec
- Next Up/Latest: 1 min

## Common Commands

```bash
# Build TV app
./gradlew :app-tv:assembleDebug
adb install app-tv/build/outputs/apk/debug/app-tv-debug.apk

# Build Mobile app
./gradlew :app-mobile:assembleDebug
adb install app-mobile/build/outputs/apk/debug/app-mobile-debug.apk

# View logs
adb logcat -s MyFlix:V ExoPlayer:V MPV:V JellyfinClient:D

# Clean build
./gradlew clean
```

## Key Files for Common Changes

| Change | Files to Modify |
|--------|-----------------|
| Add API endpoint | `core/network/JellyfinClient.kt` |
| Add data model | `core/common/model/JellyfinModels.kt` |
| Add TV screen | `app-tv/ui/screens/`, update navigation |
| Add Mobile screen | `app-mobile/ui/screens/`, update navigation |
| Add TV component | `app-tv/ui/components/` |
| Add player feature | `core/player/` |
| Modify home rows | `app-tv/ui/screens/HomeScreen.kt` |
| Change theme | `app-tv/ui/theme/` or `app-mobile/ui/theme/` |

## Navigation Routes

Both apps use Jetpack Navigation Compose with string-based routes. Navigation is defined in each app's `MainActivity.kt`.

### TV App (`app-tv/MainActivity.kt`)

| Route | Arguments | Screen | Description |
|-------|-----------|--------|-------------|
| `splash` | — | `SplashScreen` | Start destination, animated logo |
| `login` | — | `LoginScreen` | Server discovery, Quick Connect, QR code |
| `home` | — | `HomeScreen` | Main hub with content rows, hero section |
| `settings` | — | `PreferencesScreen` | App settings and preferences |
| `library/{libraryId}/{libraryName}` | `libraryId: String`, `libraryName: String` | `LibraryScreen` | Browse library contents |
| `detail/{itemId}` | `itemId: String` | `DetailScreen` | Item details, episodes, play button |
| `player/{itemId}` | `itemId: String` | `PlayerScreen` | Video playback |

**Flow:** `splash` → `login` (if needed) → `home` → `detail` → `player`

### Mobile App (`app-mobile/MainActivity.kt`)

| Route | Arguments | Screen | Description |
|-------|-----------|--------|-------------|
| `splash` | — | `SplashScreen` | Start destination, animated logo |
| `login` | — | `LoginScreen` | Server discovery, Quick Connect |
| `home` | — | `HomeScreen` | Main hub with bottom nav |
| `search` | — | `SearchScreen` | Text search with results |
| `library/{libraryId}/{libraryName}` | `libraryId: String`, `libraryName: String` | `LibraryScreen` | Browse library contents |
| `detail/{itemId}` | `itemId: String` | `DetailScreen` | Item details, episodes, play button |
| `player/{itemId}` | `itemId: String` | `PlayerScreen` | Video playback with PlaybackService |

**Flow:** `splash` → `login` (if needed) → `home` → `detail` → `player`

### Adding a New Screen

1. Create screen composable in `ui/screens/`
2. Add route to `NavHost` in `MainActivity.kt`:
   ```kotlin
   composable(
       route = "myscreen/{param}",
       arguments = listOf(navArgument("param") { type = NavType.StringType })
   ) { backStackEntry ->
       val param = backStackEntry.arguments?.getString("param") ?: return@composable
       MyScreen(param = param, onBack = { navController.popBackStack() })
   }
   ```
3. Navigate from other screens: `navController.navigate("myscreen/$paramValue")`

## Dependency Injection

The project uses **manual dependency injection** without a DI framework (no Hilt, Koin, or Dagger). Dependencies are created at the app level and passed down through composable parameters.

### Architecture

```
MainActivity.kt
    └── remember { JellyfinClient() }           // Created once
    └── remember { AppState(context, client) }  // Created once, wraps client
    └── remember { TvPreferences.getInstance() } // Singleton
            │
            ▼
        NavHost
            │
            ├── HomeScreen(jellyfinClient, ...)
            ├── DetailScreen(jellyfinClient, ...)
            ├── PlayerScreen(jellyfinClient, ...)
            └── LoginScreen(appState, jellyfinClient, ...)
```

### Core Dependencies

| Class | Scope | Creation | Description |
|-------|-------|----------|-------------|
| `JellyfinClient` | App | `remember { }` | API client, caching, auth state |
| `AppState` | App | `remember { }` | Persistent auth, wraps JellyfinClient |
| `TvPreferences` | Singleton | `getInstance(context)` | TV app settings (SharedPreferences) |

### Patterns Used

**1. Compose `remember` for app-scoped instances:**
```kotlin
@Composable
fun MyFlixTvApp() {
    val jellyfinClient = remember { JellyfinClient() }
    val appState = remember { AppState(context, jellyfinClient) }
    // Pass to screens...
}
```

**2. Constructor injection for classes:**
```kotlin
class AppState(
    private val context: Context,
    val jellyfinClient: JellyfinClient  // Injected via constructor
)
```

**3. Double-checked locking singleton (TvPreferences):**
```kotlin
companion object {
    @Volatile
    private var INSTANCE: TvPreferences? = null

    fun getInstance(context: Context): TvPreferences {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: TvPreferences(context.applicationContext).also { INSTANCE = it }
        }
    }
}
```

### Adding a New Dependency

1. **For app-scoped dependencies** - Create in `MainActivity.kt` with `remember { }`:
   ```kotlin
   val myService = remember { MyService(jellyfinClient) }
   ```

2. **For singletons with context** - Use the `getInstance` pattern (see `TvPreferences.kt`)

3. **Pass to screens** - Add as composable parameter:
   ```kotlin
   composable("myscreen") {
       MyScreen(myService = myService)
   }
   ```

### Why No DI Framework?

- Simple dependency graph (few core services)
- Compose's `remember` handles scoping naturally
- Avoids annotation processing build overhead
- Easy to understand and debug

## Testing Strategy

> **Current state:** No tests implemented yet. This section documents the recommended approach.

### Recommended Frameworks

| Type | Framework | Purpose |
|------|-----------|---------|
| Unit tests | JUnit 5 + MockK | Business logic, ViewModels, utilities |
| Flow testing | Turbine | Testing Kotlin Flows (API responses, state) |
| API client | Ktor MockEngine | Mock HTTP responses for JellyfinClient |
| Compose UI | Compose UI Testing | Screen and component tests |
| Integration | AndroidX Test | End-to-end flows |

### Test Directory Structure

```
MyFlix/
├── app-tv/
│   └── src/
│       ├── test/java/                    # Unit tests
│       │   └── dev/jausc/myflix/tv/
│       │       └── ui/screens/           # Screen logic tests
│       └── androidTest/java/             # Instrumented tests
│           └── dev/jausc/myflix/tv/
│               └── ui/                   # Compose UI tests
│
├── app-mobile/
│   └── src/
│       ├── test/java/                    # Unit tests
│       └── androidTest/java/             # Instrumented tests
│
├── core/
│   ├── network/
│   │   └── src/test/java/                # JellyfinClient tests
│   │       └── dev/jausc/myflix/core/network/
│   │           └── JellyfinClientTest.kt
│   │
│   ├── data/
│   │   └── src/test/java/                # AppState tests
│   │
│   └── player/
│       └── src/test/java/                # Player logic tests
```

### What to Test

**core:network (JellyfinClient)**
- API response parsing (use MockEngine)
- Cache TTL behavior
- Error handling (network failures, 401s)
- URL building (image URLs, stream URLs)

```kotlin
// Example: Testing with Ktor MockEngine
@Test
fun `getLatestMovies parses response correctly`() = runTest {
    val mockEngine = MockEngine { request ->
        respond(
            content = """{"Items": [{"Id": "123", "Name": "Test Movie"}]}""",
            headers = headersOf("Content-Type", "application/json")
        )
    }
    val client = JellyfinClient(mockEngine)
    // ...
}
```

**core:data (AppState)**
- Login/logout state persistence
- Initialization from DataStore
- Device ID generation

**app-tv / app-mobile (Screens)**
- Navigation triggers
- Loading/error state rendering
- User interactions (clicks, focus changes)

```kotlin
// Example: Compose UI test
@Test
fun homeScreen_displaysLoadingState() {
    composeTestRule.setContent {
        HomeScreen(
            jellyfinClient = mockClient,
            onItemClick = {}
        )
    }
    composeTestRule.onNodeWithTag("loading").assertIsDisplayed()
}
```

### Test Dependencies to Add

```kotlin
// In build.gradle.kts (root or module)
dependencies {
    // Unit testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")

    // Ktor MockEngine for API tests
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")

    // Compose UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // AndroidX Test
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

### Running Tests

```bash
# Unit tests (all modules)
./gradlew test

# Unit tests (specific module)
./gradlew :core:network:test

# Instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Specific module instrumented tests
./gradlew :app-tv:connectedAndroidTest
```

### Testing Priorities

When adding tests, prioritize in this order:

1. **JellyfinClient** - Core API client, most critical
2. **AppState** - Auth persistence, affects all screens
3. **Player logic** - Playback reporting, position tracking
4. **Screen navigation** - Ensure routes work correctly
5. **UI components** - Visual regression, accessibility

## Build Variants

### Build Types

| Type | Minify | Shrink Resources | Debuggable | Use Case |
|------|--------|------------------|------------|----------|
| `debug` | No | No | Yes | Development, testing |
| `release` | Yes (TV) / No (Mobile) | Yes (TV) | No | Production builds |

### Module Variants

**app-tv**
- Release builds have R8 minification and resource shrinking enabled
- ABI splits generate separate APKs per architecture:
  - `arm64-v8a` - Modern 64-bit ARM (most TV devices)
  - `armeabi-v7a` - Legacy 32-bit ARM
  - `universal` - All architectures (larger file size)

**app-mobile**
- Minification disabled (set `isMinifyEnabled = true` when ready)
- No ABI splits (single universal APK)

### APK Output Locations

```
app-tv/build/outputs/apk/
├── debug/
│   └── app-tv-debug.apk
└── release/
    ├── app-tv-arm64-v8a-release.apk
    ├── app-tv-armeabi-v7a-release.apk
    └── app-tv-universal-release.apk

app-mobile/build/outputs/apk/
├── debug/
│   └── app-mobile-debug.apk
└── release/
    └── app-mobile-release.apk
```

### Build Commands

```bash
# Debug builds (fast, for development)
./gradlew :app-tv:assembleDebug
./gradlew :app-mobile:assembleDebug

# Release builds (optimized)
./gradlew :app-tv:assembleRelease
./gradlew :app-mobile:assembleRelease

# Install debug directly to device
./gradlew :app-tv:installDebug
./gradlew :app-mobile:installDebug

# Build Android App Bundle (for Play Store)
./gradlew :app-tv:bundleRelease
./gradlew :app-mobile:bundleRelease
```

### ProGuard/R8 Configuration

ProGuard rules in `app-*/proguard-rules.pro` preserve:

| Library | Reason |
|---------|--------|
| Ktor | HTTP client reflection |
| Kotlinx Serialization | JSON serialization |
| Coil | Image loading |
| Media3/ExoPlayer | Video playback |
| MPV | Native video player |
| ZXing | QR code generation |

### Signing

> **Not configured yet.** Release builds use debug signing.

### Future: Product Flavors

No product flavors defined. Example if needed for different environments:

```kotlin
android {
    flavorDimensions += "environment"
    productFlavors {
        create("dev") { applicationIdSuffix = ".dev" }
        create("prod") { }
    }
}
```

## Error Handling Patterns

### API Layer (JellyfinClient)

All API calls return `Result<T>` using Kotlin's Result type with `runCatching`:

```kotlin
suspend fun getLibraries(): Result<List<JellyfinItem>> {
    return runCatching {
        httpClient.get("$baseUrl/Users/$userId/Views") { ... }.body()
    }
}
```

### Screen Layer

Screens handle results with `onSuccess`/`onFailure`:

```kotlin
// Pattern 1: Direct handling
jellyfinClient.getLatestMovies(libraryId).onSuccess { items ->
    recentMovies = items
}

// Pattern 2: Error callback propagation
jellyfinClient.connectToServer(address)
    .onSuccess { server -> onServerConnected(server) }
    .onFailure { onError(it.message ?: "Connection failed") }

// Pattern 3: Mapping common errors to user-friendly messages
.onFailure { e ->
    onError(when {
        e.message?.contains("401") == true -> "Invalid username or password"
        e.message?.contains("timeout", true) == true -> "Connection timed out"
        else -> e.message ?: "Login failed"
    })
}
```

### Error UI Components

**Error Snackbar (LoginScreen pattern):**
```kotlin
// State
var errorMessage by remember { mutableStateOf<String?>(null) }

// Auto-dismiss after 5 seconds
LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
        delay(5000)
        errorMessage = null
    }
}

// UI - positioned at bottom center
errorMessage?.let { error ->
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(32.dp)
            .background(TvColors.Error.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(error, color = Color.White)
    }
}
```

**Loading Indicator:**
```kotlin
TvLoadingIndicator(
    modifier = Modifier.size(48.dp),
    color = TvColors.BluePrimary,  // Optional, defaults to BluePrimary
    strokeWidth = 3.dp             // Optional
)
```

### Error Flow Architecture

```
JellyfinClient                    Screen                         UI
     │                              │                             │
     │  Result.success(data)        │                             │
     ├─────────────────────────────>│  .onSuccess { }             │
     │                              ├────────────────────────────>│ Update state
     │                              │                             │
     │  Result.failure(exception)   │                             │
     ├─────────────────────────────>│  .onFailure { }             │
     │                              │  onError(message)           │
     │                              ├────────────────────────────>│ Show snackbar
     │                              │                             │
```

### Theme Colors

| Color | Value | Use Case |
|-------|-------|----------|
| `TvColors.Error` | `#EF4444` (Red) | Error backgrounds, alerts |
| `TvColors.Success` | `#22C55E` (Green) | Success states |
| `TvColors.TextPrimary` | `#F8FAFC` (White) | Text on error/success backgrounds |

### Current Gaps

> **Note:** Some screens silently ignore errors (e.g., HomeScreen shows empty rows on failure). Consider adding:
> - Retry buttons for failed API calls
> - Empty state with error message
> - Global error boundary/handler

### Adding Error Handling to a Screen

```kotlin
@Composable
fun MyScreen(
    jellyfinClient: JellyfinClient,
    onError: (String) -> Unit  // Propagate to parent, or handle locally
) {
    var items by remember { mutableStateOf<List<Item>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        jellyfinClient.getItems()
            .onSuccess { items = it }
            .onFailure { errorMessage = it.message }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> TvLoadingIndicator(...)
            errorMessage != null -> ErrorState(errorMessage, onRetry = { ... })
            items.isEmpty() -> EmptyState()
            else -> ContentList(items)
        }
    }
}
```

## State Management

The project uses **pure Compose state** without ViewModels. State is managed directly in composables using `remember` + `mutableStateOf` for local state, and `StateFlow` for shared/observable state in core modules.

### State Patterns

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| `remember { mutableStateOf() }` | Screens, Components | Local UI state |
| `StateFlow` + `collectAsState()` | Core modules → Screens | Observable shared state |
| `LaunchedEffect` | Screens | Side effects, data loading |
| `rememberCoroutineScope()` | Event handlers | User-triggered async operations |

### Screen State (Local)

Screens manage their own state with `remember`:

```kotlin
@Composable
fun DetailScreen(
    itemId: String,
    jellyfinClient: JellyfinClient,
    onPlayClick: () -> Unit
) {
    // Local state
    var item by remember { mutableStateOf<JellyfinItem?>(null) }
    var seasons by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<JellyfinItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load data on first composition or when itemId changes
    LaunchedEffect(itemId) {
        jellyfinClient.getItem(itemId).onSuccess { item = it }
        isLoading = false
    }

    // React to selection changes
    LaunchedEffect(selectedSeason) {
        selectedSeason?.let { season ->
            jellyfinClient.getEpisodes(item!!.id, season.id).onSuccess { ... }
        }
    }
}
```

### Shared State (StateFlow)

Core modules expose `StateFlow` for observable state:

```kotlin
// In core module (e.g., PlayerController)
class PlayerController {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun play(url: String) {
        _state.value = _state.value.copy(isPlaying = true)
    }
}

// In Composable
@Composable
fun PlayerScreen(playerController: PlayerController) {
    val playbackState by playerController.state.collectAsState()

    Text("Position: ${playbackState.position}")
    Text("Duration: ${playbackState.duration}")
}
```

### Key State Classes

**PlaybackState** (`core/player/UnifiedPlayer.kt`):
```kotlin
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val position: Long = 0L,      // milliseconds
    val duration: Long = 0L,      // milliseconds
    val speed: Float = 1.0f,
    val error: String? = null,
    val playerType: String = "Unknown"
) {
    val progress: Float  // 0.0 to 1.0
    val remainingTime: Long
}
```

**TvPreferences** (`app-tv/TvPreferences.kt`):
```kotlin
class TvPreferences(context: Context) {
    private val _hideWatchedFromRecent = MutableStateFlow(false)
    val hideWatchedFromRecent: StateFlow<Boolean> = _hideWatchedFromRecent.asStateFlow()

    fun setHideWatchedFromRecent(hide: Boolean) {
        prefs.edit().putBoolean(KEY, hide).apply()
        _hideWatchedFromRecent.value = hide
    }
}

// Usage in MainActivity
val hideWatched by tvPreferences.hideWatchedFromRecent.collectAsState()
HomeScreen(hideWatchedFromRecent = hideWatched, ...)
```

### State Flow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      MainActivity                            │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐    │
│  │JellyfinClient│ │  AppState   │  │  TvPreferences   │    │
│  │  (passed)   │  │  (passed)   │  │ StateFlow→collect│    │
│  └─────────────┘  └─────────────┘  └──────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                        Screen                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  var items by remember { mutableStateOf(...) }       │   │
│  │  var isLoading by remember { mutableStateOf(true) }  │   │
│  │  val prefs by preferences.flow.collectAsState()      │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│         LaunchedEffect    │   User Events                   │
│              │            │        │                         │
│              ▼            ▼        ▼                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              API Call / State Update                 │   │
│  │  jellyfinClient.getX().onSuccess { items = it }     │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Common State Patterns

**Loading + Content + Error:**
```kotlin
var data by remember { mutableStateOf<Data?>(null) }
var isLoading by remember { mutableStateOf(true) }
var error by remember { mutableStateOf<String?>(null) }

LaunchedEffect(Unit) {
    api.getData()
        .onSuccess { data = it }
        .onFailure { error = it.message }
    isLoading = false
}
```

**Refresh trigger:**
```kotlin
var refreshTrigger by remember { mutableStateOf(0) }

LaunchedEffect(refreshTrigger) {
    loadContent()
}

// To refresh:
Button(onClick = { refreshTrigger++ }) { Text("Refresh") }
```

**Derived state:**
```kotlin
val contentReady = !isLoading && items.isNotEmpty()

LaunchedEffect(contentReady) {
    if (contentReady) focusRequester.requestFocus()
}
```

### Why No ViewModels?

- Simpler mental model - state lives where it's used
- No extra abstraction layer for straightforward screens
- `remember` survives recomposition; navigation handles screen lifecycle
- Core modules with StateFlow provide shared state where needed
- Easy to extract to ViewModel later if complexity grows

## Development Preferences

- **Code delivery:** Provide changes as zip files with unique names that preserve directory structure for direct extraction into git repo
- **Architecture:** Multi-module with shared core, platform-specific UI
- **State:** Compose state hoisting, remember + mutableStateOf patterns
- **Networking:** Ktor with Result<T> return types

## Design Principles

1. **Multi-platform** - Shared core logic, platform-specific UI
2. **Pure Compose** - No Leanback library, Compose TV Material only
3. **Ktor networking** - Custom API client, not SDK dependency
4. **Result types** - All API calls return Result<T>
5. **Variable caching** - TTL based on data volatility
6. **Focus management** - Proper D-pad navigation on TV

## Related Projects

| Project | Purpose |
|---------|---------|
| MyFlix-Wholphin | Legacy fork-based client (production) |
| Universe Collections | Jellyfin plugin for collection tagging |
| UMTK | Unraid Media Toolkit |
