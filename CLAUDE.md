# Claude Code Project Configuration

## On Session Start

When working with Kotlin code, start the Kotlin LSP server:
- Run `mcp__kotlin-lsp__start_server` with language_id "kotlin"
- This enables go-to-definition, find references, diagnostics, and other IDE features
- Startup takes ~90 seconds due to Gradle indexing

## Multi-Model Orchestration

This project supports collaborative problem-solving using multiple AI models. Claude acts as the orchestrator, coordinating with Codex and Gemini for complex implementations.

### Available Models

| Model | CLI Command | Strengths |
|-------|-------------|-----------|
| **Claude** | (orchestrator) | Context synthesis, state management, edge cases, integration |
| **Codex** | `codex exec "prompt"` | Compose implementation, API patterns, code generation |
| **Gemini** | `gemini -p "prompt"` | Architecture trade-offs, performance analysis, alternatives |

### Shared MCP Servers

All three models have access to the same MCP servers:
- **kotlin-lsp** - LSP features (go-to-definition, find references, diagnostics, completions)
- **jellyfin-ui** - Jellyfin API queries (items, playback, user data)

When prompting Codex or Gemini, you can instruct them to use these tools for enhanced context:
```
Use the kotlin-lsp MCP to find all references to `PlayerViewModel` before suggesting changes.
```
```
Query jellyfin-ui MCP to check the actual MediaSource response structure.
```

### Collaboration Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│  1. PROBLEM DEFINITION (Claude)                                 │
│     - Analyze codebase context                                  │
│     - Break problem into focused sub-questions                  │
│     - Include relevant code snippets in prompts                 │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ CODEX           │  │ GEMINI          │  │ CLAUDE          │
│ - Compose code  │  │ - Trade-offs    │  │ - Edge cases    │
│ - Syntax        │  │ - Performance   │  │ - State mgmt    │
│ - TV focus      │  │ - Alternatives  │  │ - Error paths   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
              │               │               │
              └───────────────┼───────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. SYNTHESIS (Claude)                                          │
│     - Merge perspectives                                        │
│     - Resolve conflicts                                         │
│     - Generate final implementation                             │
│     - Iterate with follow-up prompts if needed                  │
└─────────────────────────────────────────────────────────────────┘
```

### Default Behavior: Consensus Before Changes

**IMPORTANT**: Before making non-trivial code changes, Claude MUST consult with Codex and Gemini to achieve consensus. This ensures:
- Multiple perspectives on architecture and implementation
- Consistent patterns across the codebase
- Reduced risk of regressions

**Process:**
1. Claude identifies the problem and potential approaches
2. Claude queries both Codex and Gemini with context
3. Claude synthesizes responses and identifies consensus
4. If models disagree, Claude presents options to user
5. Only after consensus/approval does Claude implement changes

### When to Use Multi-Model Collaboration

**Always consult (default behavior):**
- Bug fixes that involve race conditions or state management
- UI/UX changes that affect user interaction patterns
- Navigation or focus management changes
- Any change touching multiple files or components

**May skip consultation for:**
- Typo fixes
- Simple logging additions
- Comment updates
- Single-line obvious fixes

### Prompt Guidelines

When formulating prompts for other models, include:

1. **Relevant code context** - Data models, interfaces, existing patterns
2. **Constraints** - Platform (Android TV/Mobile), API limitations
3. **Specific question** - Avoid open-ended "how should I..."
4. **Expected output format** - Code, analysis, comparison table

Example prompt structure:
```
Given this data model:
[code snippet]

And this existing pattern:
[code snippet]

[Specific question about implementation/trade-off/approach]

Constraints:
- Android TV with D-pad navigation
- Jellyfin API backend
- Jetpack Compose UI
```

### Execution Examples

**Parallel execution for independent questions:**
```bash
# Claude orchestrates these simultaneously
codex exec "Write a Compose BitrateSelectionSheet with TV focus handling given: [MediaSource model]"
gemini -p "Compare client-side vs server-side bitrate limiting for streaming apps - latency, bandwidth, UX trade-offs"
```

**Sequential execution for dependent questions:**
```bash
# First: architecture decision
gemini -p "Should watched state sync use optimistic updates or wait for server confirmation?"
# Then: implementation based on decision
codex exec "Implement optimistic watched state sync with WorkManager retry given: [UserData model]"
```

---

## Project Structure

### TV App (`app-tv/`)
- Jetpack Compose for TV with explicit focus management
- NavRail with activation model (Menu key / FocusSentinel)
- Per-screen exit focus restoration via `LocalExitFocusState`

### Mobile App (`app-mobile/`)
- Standard Jetpack Compose
- Bottom sheet patterns for media info
- Material 3 theming

### Core (`core/`)
- `JellyfinClient` - API communication
- `JellyfinModels.kt` - Data models (JellyfinItem, MediaSource, MediaStream)
- Shared ViewModels and repositories

---

## Building and Installing

### Build TV App (from WSL)
```bash
/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -Command "\$env:JAVA_HOME = 'C:\Users\jausc\AppData\Local\Programs\Android Studio\jbr'; cd 'C:\Users\jausc\StudioProjects\MyFlix'; .\gradlew.bat :app-tv:assembleDebug"
```

### Install on Test Devices
```bash
# Shield TV
adb -s 192.168.1.136:5555 install -r "/mnt/c/Users/jausc/StudioProjects/MyFlix/app-tv/build/outputs/apk/debug/app-tv-universal-debug.apk"

# Secondary device
adb -s 192.168.1.200:5555 install -r "/mnt/c/Users/jausc/StudioProjects/MyFlix/app-tv/build/outputs/apk/debug/app-tv-universal-debug.apk"
```

### Build + Install in One Step
```bash
/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -Command "\$env:JAVA_HOME = 'C:\Users\jausc\AppData\Local\Programs\Android Studio\jbr'; cd 'C:\Users\jausc\StudioProjects\MyFlix'; .\gradlew.bat :app-tv:assembleDebug" && adb -s 192.168.1.136:5555 install -r "/mnt/c/Users/jausc/StudioProjects/MyFlix/app-tv/build/outputs/apk/debug/app-tv-universal-debug.apk"
```

---

## Key Patterns

### TV Focus Management
```kotlin
// Register screen's focus target for NavRail exit
val updateExitFocus = rememberExitFocusRegistry(primaryFocusRequester)

// Update when focus changes within screen
Modifier.onFocusChanged { if (it.hasFocus) updateExitFocus(thisFocusRequester) }
```

### Tab Focus Restoration
When navigating from tab content back up to tab headers, focus should return to the last focused tab, not always the first tab.

```kotlin
// 1. Create stable map of FocusRequesters (not inside loop)
val tabFocusRequesters = remember { mutableStateMapOf<TabType, FocusRequester>() }
fun getTabFocusRequester(tab: TabType): FocusRequester =
    tabFocusRequesters.getOrPut(tab) { FocusRequester() }

// 2. Track last focused tab
var lastFocusedTab by remember { mutableStateOf(TabType.First) }

// 3. On each tab, update tracking when focused
Tab(
    modifier = Modifier
        .focusRequester(getTabFocusRequester(tab))
        .onFocusChanged { if (it.isFocused) lastFocusedTab = tab }
)

// 4. In content area, point UP to last focused tab
Box(
    modifier = Modifier.focusProperties {
        up = tabFocusRequesters[lastFocusedTab] ?: FocusRequester.Default
    }
) { /* Tab content */ }
```

### Animated Focus Halo Effect
Visual feedback for focused elements using blur-based glow.

```kotlin
var isFocused by remember { mutableStateOf(false) }

val haloAlpha by animateFloatAsState(
    targetValue = if (isFocused) 0.6f else 0f,
    animationSpec = tween(durationMillis = 150),
    label = "haloAlpha",
)

Box {
    // Halo glow layer (behind content)
    if (haloAlpha > 0f) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(12.dp)
                .background(TvColors.BluePrimary.copy(alpha = haloAlpha), RoundedCornerShape(16.dp)),
        )
    }
    // Actual content
    Surface(
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
        // ...
    ) { /* content */ }
}
```

### Menu Positioning with Container Offset
When menus are inside padded containers (e.g., NavRail), anchor coordinates need adjustment.

```kotlin
var containerOffsetX by remember { mutableStateOf(0f) }
var containerOffsetY by remember { mutableStateOf(0f) }

BoxWithConstraints(
    modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            containerOffsetX = position.x
            containerOffsetY = position.y
        },
) {
    // Adjust anchor position by container offset
    val adjustedAnchorX = anchor.x.toPx() - containerOffsetX
    val adjustedAnchorY = anchor.y.toPx() - containerOffsetY
}
```

### Media Stream Detection
```kotlin
// Check for HDR/Dolby Vision
item.isDolbyVision  // Extension property
item.isHdr          // Extension property
item.is4K           // Extension property

// Get primary video stream
item.videoStream    // First video MediaStream
```

---

## Recent Feature Work

### Episode Screen Enhancements (January 2026)

**Tab System Improvements:**
- Added Chapters tab to episode screen showing chapter thumbnails with timestamps
- Implemented tab focus restoration - navigating up from content returns to last focused tab
- Added animated blue halo effect on focused tabs for better visual feedback
- Removed padding between tab row and content for tighter layout

**Chapter Display:**
- Thumbnail size: 199×112dp (16:9 aspect ratio, matching season poster height of 112dp)
- Chapter name displayed below thumbnail (not overlayed)
- Focus border only on thumbnail, not text
- Clicking chapter seeks to that timestamp in playback

**Episode Options Menu:**
- Fixed menu positioning when inside NavRail padded container
- Implemented cascading submenu pattern with activeSubmenu state
- LEFT key closes submenu, returns to parent menu
- Added "Go to Series" action in Season Actions submenu

**Key Files Modified:**
- `EpisodesScreen.kt` - Tab system, chapters display, options dialog
- `UnifiedSeriesScreen.kt` - Tab focus restoration, halo effect
- `PlayerSlideOutMenu.kt` - Container offset tracking, cascading submenus
- `MainActivity.kt` - Navigation callback for "Go to Series"
- `JellyfinClient.kt` - Added "Chapters" to episode list API fields

---

## References

- `references/episode-detail-enhancement-plan.md` - Panel discussion on episode screen features
- `docs/` - Additional documentation
