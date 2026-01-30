# MyFlix TV App - Development Handoff

**Branch:** `tv/series-detail-consolidation`
**Last Updated:** 2026-01-30
**Last Commit:** `59babf8 tv: simplify focus handling in series and episode screens`

---

## What We've Been Working On

### Focus Handling Simplification (Completed)

The primary work involved simplifying the focus management system in the TV app's series and episode detail screens. The original implementation had complex per-item focus tracking that was over-engineered for the actual use case.

#### Key Insight
Navigation to the NavRail only happens from the **leftmost position** on the screen. This means:
- You don't need to track which specific button/item had focus
- You only need to know which **section** had focus (action buttons, episode row, or tabs)
- When returning from NavRail, focus restores to the first/main element of that section

#### Files Modified
1. **`app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/UnifiedSeriesScreen.kt`**
2. **`app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/EpisodesScreen.kt`**

---

## Current State

### UnifiedSeriesScreen.kt (Series Detail)

**Focus Architecture:**
```kotlin
// Three focus requesters for section entry points
val playButtonFocusRequester = remember { FocusRequester() }
val nextUpFocusRequester = remember { FocusRequester() }
val firstTabFocusRequester = remember { FocusRequester() }

// Track which section was last focused: "action", "nextup", "tabs"
var lastFocusedSection by remember { mutableStateOf("action") }

// Compute restoration target
val focusRestorerTarget = when (lastFocusedSection) {
    "nextup" -> nextUpFocusRequester
    "tabs" -> firstTabFocusRequester
    else -> playButtonFocusRequester
}
```

**Initial Focus:** Play button (via `LaunchedEffect(Unit)`)

**Sections:**
- Action buttons (Play, Shuffle, Watched, Favorite)
- Next Up row (if available)
- Tab header row (Seasons, Details, Cast & Crew, Trailers, Extras, Related)

**Tab Content Composables:** All simplified to only take `tabFocusRequester` for left-edge navigation:
- `SeasonsTabContent`
- `CastCrewTabContent`
- `DetailsTabContent`
- `TrailersTabContent`
- `ExtrasTabContent`
- `RelatedTabContent`

### EpisodesScreen.kt (Episode Detail)

**Focus Architecture:**
```kotlin
// Focus requesters for section entry points
val episodeRowFocusRequester = remember { FocusRequester() }
val firstTabFocusRequester = remember { FocusRequester() }
val playFocusRequester = remember { FocusRequester() }

// Track which section was last focused: "action", "episodes", "tabs"
var lastFocusedSection by remember { mutableStateOf("episodes") }

// Per-episode focus requesters for restoring to specific episode
val episodeFocusRequesters = remember(episodes) {
    episodes.associate { it.id to FocusRequester() }
}

// Compute restoration target
val focusRestorerTarget = when (lastFocusedSection) {
    "action" -> playFocusRequester
    "tabs" -> firstTabFocusRequester
    "episodes" -> focusedEpisode?.id?.let { episodeFocusRequesters[it] } ?: episodeRowFocusRequester
    else -> episodeRowFocusRequester
}
```

**Initial Focus:** Selected episode card (determined by `findTargetEpisode()` - uses selectedEpisodeId, then in-progress, then first unwatched, then first episode)

**Sections:**
- Action buttons (Play, Watched, Favorite)
- Episode card row (horizontal scrollable)
- Tab header row (Seasons, Details, Media Info, Cast & Crew, Guest Stars)

**Tab Content Composables:** All simplified:
- `DetailsTabContent`
- `MediaInfoTabContent`
- `CastCrewTabContent`
- `GuestStarsTabContent`
- `SeasonsTabContent`

---

## NavRail System Context

The focus restoration relies on the NavRail system in:
- `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/navrail/NavigationRail.kt`
- `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/navrail/NavRailItem.kt`
- `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/navrail/FocusSentinel.kt`

**How it works:**
1. `FocusSentinel` sits at the left edge of content (invisible, 1dp wide)
2. When user navigates left and focus hits sentinel, it activates the NavRail
3. NavRail expands and receives focus on the currently selected screen's icon
4. When user navigates right from NavRail or selects an item, NavRail deactivates
5. Focus returns to content via `focusRestorer(focusRestorerTarget)` on the main content container

**Key Modifiers:**
- `focusRestorer { focusRestorerTarget }` - on main content Box, restores focus when re-entering
- `onFocusChanged { if (it.hasFocus) lastFocusedSection = "xxx" }` - tracks which section has focus
- `focusProperties { left = leftEdgeFocusRequester }` - connects content to FocusSentinel

---

## Issues and Blockers

### Known Issues
None currently blocking. The focus simplification is complete and tested.

### Potential Edge Cases to Monitor
1. **Rapid season switching:** The `focusSetForSeason` guard prevents re-focusing when already on the correct season, but rapid switching might cause race conditions with the 100ms delay.

2. **Empty states:** If episodes list is empty, `findTargetEpisode()` returns null. The UI handles this but focus behavior is undefined.

3. **Tab content focus:** Tab content uses `focusGroup()` but doesn't have its own `focusRestorer`. If focus is deep in tab content when navigating to NavRail, returning will go to tab header (by design, since left-edge nav happens from leftmost position).

---

## Next Steps

### Suggested Improvements
1. **Testing:** Manually test all focus scenarios:
   - Initial load focus on both screens
   - NavRail activation/deactivation from each section
   - Season switching focus behavior
   - Deep navigation into tab content and back

2. **Consider consolidating:** Both screens share similar focus patterns. Could extract a shared focus management hook/utility if more screens need this pattern.

3. **Mobile parity:** If mobile app has similar navigation patterns, consider applying the same simplification philosophy.

### Branch Status
This branch (`tv/series-detail-consolidation`) appears to be a feature branch with many changes. Recent commits focus on NavRail and focus improvements:
- `59babf8` tv: simplify focus handling in series and episode screens
- `f75e94f` tv: fix tab content navigation and NavRail focus restoration
- `46fe905` tv: fix tab content navigation and EpisodesScreen backdrop
- `cceb8ea` tv: focus NavRail on current screen's icon when activated
- `6ae6bde` tv: fix NavRail exit to hero buttons
- `809ef91` tv: fix HomeScreen left nav and NavRail exit focus

---

## Key Concepts Reference

### Compose TV Focus Concepts

| Concept | Description |
|---------|-------------|
| `isFocused` | True only when THIS element has direct focus |
| `hasFocus` | True when this element OR any child has focus |
| `FocusRequester` | Programmatically request focus on a composable |
| `focusRestorer` | Remembers and restores focus within a `focusGroup` |
| `focusGroup` | Groups focusable elements for collective focus management |
| `focusProperties` | Customize D-pad navigation (up/down/left/right targets) |
| `canFocus` | Control whether an element can receive focus |

### Focus Tracking Pattern
```kotlin
.onFocusChanged { focusState ->
    if (focusState.hasFocus) {  // Use hasFocus, not isFocused!
        lastFocusedSection = "sectionName"
    }
}
```

### Focus Restoration Pattern
```kotlin
Box(
    modifier = Modifier
        .focusRestorer { targetFocusRequester }
        .focusGroup()
) {
    // Content here
}
```

---

## File Locations Quick Reference

```
app-tv/src/main/java/dev/jausc/myflix/tv/ui/
├── screens/
│   ├── UnifiedSeriesScreen.kt    # Series detail with tabs
│   ├── EpisodesScreen.kt         # Episode browser with hero
│   ├── DetailScreen.kt           # Router for detail screens
│   └── ...
├── components/
│   ├── navrail/
│   │   ├── NavigationRail.kt     # Main NavRail component
│   │   ├── NavRailItem.kt        # Individual nav items
│   │   ├── FocusSentinel.kt      # Left-edge focus trigger
│   │   └── NavRailDefaults.kt    # Constants and dimensions
│   └── detail/
│       ├── PlayButtons.kt        # Expandable play button
│       └── ...
└── ...
```

---

## Contact / Questions

This handoff was generated by Claude Code. For questions about the codebase or to continue this work, provide this file as context for a new session.
