## Platform Parity Rules

**CRITICAL: All feature requests are automatically implemented on BOTH platforms.**

When the user says "Add [feature]..." or "Implement [feature]..." or any feature request:

1. **Always implement for BOTH TV and Mobile in the same response**
   - Do NOT implement one platform and wait for instructions
   - Do NOT ask "which platform?" - the answer is BOTH
   - Deliver complete implementation for app-tv/ AND app-mobile/

2. **Implementation Order:**
   ```
   Step 1: Extract shared logic to core/common/ (or appropriate core module)
   Step 2: Implement TV UI in app-tv/
   Step 3: Implement Mobile UI in app-mobile/
   Step 4: Deliver as single zip with all changes
   ```

3. **Where Code Lives:**
   | Code Type | Location |
   |-----------|----------|
   | API calls, data models | `core/network/`, `core/common/model/` |
   | Business logic, utilities | `core/common/` |
   | Playback logic | `core/player/` |
   | Shared UI logic (not composables) | `core/common/` |
   | TV-specific UI | `app-tv/ui/` |
   | Mobile-specific UI | `app-mobile/ui/` |

4. **Example Response Structure:**
   ```
   User: "Add a favorites filter to the library screen"
   
   Claude delivers ONE zip containing:
   - core/common/FilterUtils.kt (shared filtering logic)
   - app-tv/ui/screens/LibraryScreen.kt (TV implementation)
   - app-mobile/ui/screens/LibraryScreen.kt (Mobile implementation)
   ```

5. **Only Platform-Specific When Explicitly Requested:**
   - "Add TV-only feature..." → TV only is acceptable
   - "Fix this mobile bug..." → Mobile only is acceptable
   - Everything else → BOTH platforms

## Code Quality Rules

**NEVER suppress or disable Detekt/lint rules. ALWAYS fix the underlying issue.**

Exceptions allowed:
- `@Suppress("DEPRECATION")` for deprecated APIs required by minSdk 25 compatibility
- `@Suppress("UnusedParameter")` for interface compliance where parameter is required but unused

When Detekt flags something:
1. Explain WHY the rule exists
2. Fix the code to comply with the rule
3. Only suppress if genuinely unavoidable AND document why

### Common Fixes (NOT suppressions)

| Rule | Proper Fix |
|------|------------|
| `TooManyFunctions` | Extract to separate files/classes by responsibility |
| `LongMethod` | Break into smaller named functions |
| `LongParameterList` | Use data class or builder pattern |
| `ComplexCondition` | Extract to named boolean variables |
| `MagicNumber` | Create named constants in companion object |
| `TooGenericExceptionCaught` | Catch specific exception types |
| `UnusedPrivateMember` | Delete it |
| `MaxLineLength` | Break line, extract variables |
| `ReturnCount` | Use early returns or `when` expression |
| `NestedBlockDepth` | Extract inner logic to functions |
| `StringLiteralDuplication` | Extract to constants |

### Refactoring Patterns

**TooManyFunctions in a Screen:**
```kotlin
// BEFORE: HomeScreen.kt with 20+ functions
// AFTER: Split by responsibility
HomeScreen.kt          // Main composable, state, LaunchedEffects
HomeScreenRows.kt      // Row composables (ItemRow, GenreRow, etc.)
HomeScreenDialogs.kt   // Dialog builders and handlers
HomeScreenState.kt     // State classes, actions, helper functions
```

**LongParameterList:**
```kotlin
// BEFORE: 10+ parameters
fun HomeScreen(client: JellyfinClient, hideWatched: Boolean, showGenres: Boolean, ...)

// AFTER: Grouped into data classes
data class HomeScreenConfig(
    val hideWatched: Boolean,
    val showGenres: Boolean,
    val enabledGenres: List<String>,
    val showCollections: Boolean
)

data class HomeScreenCallbacks(
    val onItemClick: (String) -> Unit,
    val onPlayClick: (String) -> Unit,
    val onNavigateToDetail: (String) -> Unit
)

fun HomeScreen(
    client: JellyfinClient,
    config: HomeScreenConfig,
    callbacks: HomeScreenCallbacks
)
```

**StringLiteralDuplication:**
```kotlin
// BEFORE: Repeated strings
jellyfinClient.getItems(fields = "Overview,ImageTags,BackdropImageTags")
jellyfinClient.getLatest(fields = "Overview,ImageTags,BackdropImageTags")

// AFTER: Constants
private object Fields {
    const val CARD = "Overview,ImageTags,BackdropImageTags,UserData"
    const val DETAIL = "Overview,ImageTags,BackdropImageTags,UserData,MediaSources,Genres"
}

jellyfinClient.getItems(fields = Fields.CARD)
```

## Custom Skills

Claude Code has access to custom skills in `.claude/skills/`:
- `myflix-architecture` - Project structure, module layout, state patterns
- `kotlin-compose-patterns` - Compose best practices, focus management, card sizes
- `android-tv-development` - D-pad navigation, hero sections, focus restoration
- `jellyfin-api` - JellyfinClient usage, caching, image URLs

## Custom Commands

- `/build-tv` - Build TV app debug APK
- `/build-mobile` - Build mobile app debug APK  
- `/cleanup` - Clean code and generate commit message
- `/zip-update` - Package changes as zip file
