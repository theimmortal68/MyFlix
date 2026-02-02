# Collection Details Screen UI Refresh Plan

## Executive Summary

Modernize CollectionDetailScreen to match the refreshed MovieDetailScreen, UnifiedSeriesScreen, and EpisodesScreen patterns while preserving collection-specific UX strengths.

---

## Current State Analysis

### Current CollectionDetailScreen
- **Backdrop**: Static `DetailBackdropLayer` (no animation)
- **Layout**: 50% hero / 50% content split
- **Items Display**: 7-column `LazyVerticalGrid`
- **Hero Pattern**: Master-detail (focused item updates backdrop/hero)
- **Actions**: 2 buttons only (Shuffle, Favorite)
- **Tabs**: None

### Target Patterns (from refreshed screens)
- Ken Burns animated backdrop
- Tab-based content organization
- Hero with metadata row + tabs at bottom
- Horizontal scrolling rows with compact cards
- `ExpandablePlayButtons` for actions
- Tab focus restoration pattern

---

## Consensus Recommendations (Codex + Gemini)

### 1. Ken Burns Animated Backdrop
**Consensus: YES** - Both models strongly recommend adopting `KenBurnsBackdrop`

- Use existing `KenBurnsBackdrop` component with `KenBurnsFadePreset.MOVIE` or new `COLLECTION` preset
- Position: 85% width × 90% height, top-right aligned
- On **Items tab**: Backdrop syncs with focused item (master-detail preserved)
- On **Details/Similar tabs**: Backdrop locked to collection's own image

### 2. Tab Structure
**Consensus: Items, Details, Similar**

| Tab | Content | Hero Behavior |
|-----|---------|---------------|
| **Items** (default) | Collection items in rows | Master-detail: focused item updates hero |
| **Details** | Collection overview, metadata, tags | Static: collection info only |
| **Similar** | Related collections | Static: collection info only |

### 3. Items Display Strategy
**Decision: Hybrid approach based on collection size**

| Collection Size | Display Mode | Rationale |
|-----------------|--------------|-----------|
| **Small (≤15 items)** | Horizontal `LazyRow` | Fits nicely, consistent with other screens |
| **Large (>15 items)** | Vertical Grid (7 columns) | Better for browsing many items (e.g., MCU's 47 films) |

**Implementation Notes:**
- Grid preserves server sort order (Timeline Order for Universe Collections)
- No Phase grouping - single sorted grid
- Compact poster cards used in both modes (83×125dp)

**Alternative Styles (documented for quick switching):**
```kotlin
// Configuration constant in CollectionDetailScreen.kt
private const val LARGE_COLLECTION_THRESHOLD = 15

// Alternative A: All horizontal rows (change threshold to Int.MAX_VALUE)
private const val LARGE_COLLECTION_THRESHOLD = Int.MAX_VALUE

// Alternative B: All grid (change threshold to 0)
private const val LARGE_COLLECTION_THRESHOLD = 0

// Alternative C: Higher threshold (e.g., 30 items)
private const val LARGE_COLLECTION_THRESHOLD = 30
```

### 4. Hero Section
**Consensus: Modernize with metadata row**

```
┌─────────────────────────────────────────────────────────────┐
│ Collection Title                                             │
│ 24 items • Action, Sci-Fi                                   │
│ [Overview text - 3 lines max, auto-scroll if longer]        │
│                                                             │
│ [▶ Shuffle] [♡ Favorite]                                    │
│                                                             │
│ ─────────────────────────────────────────────────────────── │
│ [Items]  [Details]  [Similar]                               │
└─────────────────────────────────────────────────────────────┘
```

- Title + item count + genres
- Overview (3 lines, expandable)
- Action buttons using `ExpandablePlayButtons` pattern
- Tab row anchored at bottom of hero

### 5. Compact Poster Cards
**Consensus: YES** - Match other refreshed screens

- Size: 83dp × 125dp (2:3 aspect ratio)
- Single-line title below
- Reuse `CompactPosterCard` from MovieDetailScreen
- Focus border with blue highlight

### 6. Focus Management
**Consensus: Use established patterns**

- Stable `tabFocusRequesters` map with enum keys
- `lastFocusedTab` tracking for UP navigation
- Tab content `focusProperties { up = lastFocusedTab }`
- `rememberExitFocusRegistry` for NavRail integration

---

## Implementation Phases

### Phase 1: Foundation (Core Layout)
1. Add `CollectionTab` enum: `Items`, `Details`, `Similar`
2. Replace `DetailBackdropLayer` with `KenBurnsBackdrop`
3. Restructure layout: Hero → Spacer → Tabs → Content
4. Implement tab switching with `TvTabRow`
5. Add tab focus restoration pattern

### Phase 2: Items Tab
1. Implement hybrid display: `LazyRow` for ≤15 items, `LazyVerticalGrid` for >15 items
2. Use `CompactPosterCard` in both modes (83×125dp)
3. Preserve master-detail pattern (focused item → hero update)
4. Grid preserves server sort order (Timeline Order for Universe Collections)
5. Add `LARGE_COLLECTION_THRESHOLD` constant for easy style switching

### Phase 3: Details Tab
1. Create `CollectionDetailsTabContent` composable
2. Display: Overview, item count, genres, tags, year range
3. Static hero (no master-detail updates)
4. Match `MovieDetailsTabContent` layout pattern

### Phase 4: Similar Tab
1. Fetch similar collections via Jellyfin API
2. Display as horizontal row of collection cards
3. Static hero (collection info only)
4. On click → navigate to selected collection

### Phase 5: Polish
1. Add `ExpandablePlayButtons` (Shuffle primary, Favorite secondary)
2. Tune grid column count for optimal card size (currently 7 columns)
3. Test with various collection sizes (5, 15, 20, 47, 100+ items)
4. Verify threshold switching between row and grid modes
5. Ensure consistent focus behavior in both display modes

---

## Component Reuse

| Component | Source | Usage |
|-----------|--------|-------|
| `KenBurnsBackdrop` | Existing | Animated backdrop |
| `TvTabRow` | Existing | Tab navigation |
| `CompactPosterCard` | MovieDetailScreen | Item cards |
| `ExpandablePlayButtons` | Existing | Action buttons |
| `DynamicBackground` | Existing | Gradient background |

---

## API Requirements

### Existing Endpoints (sufficient)
- `getItem(collectionId)` - Collection metadata
- `getCollectionItems(collectionId, sortBy = null)` - Items in collection (respects server Display Order)
- `getSimilarItems(collectionId)` - Similar collections

### Notes
- `sortBy = null` preserves server-configured sort order (Timeline Order for Universe Collections)
- Genre aggregation computed client-side from item metadata if needed
- No additional API endpoints required for this refresh

---

## Focus Flow Diagram

```
                    ┌──────────────┐
                    │   NavRail    │
                    └──────┬───────┘
                           │ DOWN
                    ┌──────▼───────┐
                    │ Shuffle Btn  │ ◄── Initial focus
                    └──────┬───────┘
                           │ DOWN
                    ┌──────▼───────┐
                    │   Tab Row    │ ◄── Items | Details | Similar
                    └──────┬───────┘
                           │ DOWN
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼──────┐ ┌───▼───┐ ┌──────▼──────┐
       │ Items Row   │ │Details│ │ Similar Row │
       │ [Card][Card]│ │Content│ │ [Coll][Coll]│
       └─────────────┘ └───────┘ └─────────────┘
              │
              │ UP (restores to last focused tab)
              ▼
       ┌─────────────┐
       │   Tab Row   │
       └─────────────┘
```

---

## Decisions Made

1. **Large Collection Handling**: ✅ Hybrid approach
   - ≤15 items: Horizontal `LazyRow`
   - >15 items: Vertical Grid (7 columns)
   - Threshold configurable via `LARGE_COLLECTION_THRESHOLD` constant

2. **Universe Collection Display**: ✅ Single sorted grid
   - No Phase grouping - server sort order preserved (Timeline Order)
   - Grid displays items in server-defined order
   - Future: Phase grouping can be added later if tags are implemented

3. **Similar Collections**: ✅ Use Jellyfin API
   - Rely on `getSimilarItems` endpoint for now
   - Alternative matching method to be implemented app-wide later

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Large collections slow to render | LazyVerticalGrid with virtualization (already efficient) |
| Focus loss on tab switch | Stable FocusRequester map, focusRestorer |
| Master-detail flicker | Debounce focus changes (150ms) |
| No similar collections returned | Show "No similar collections" message or hide tab |
| Grid vs Row inconsistency | Clear threshold (15 items), documented alternatives |

---

## Success Criteria

- [ ] Ken Burns backdrop animates smoothly
- [ ] Tab switching works with D-pad (LEFT/RIGHT on tabs, UP from content)
- [ ] Master-detail updates hero on Items tab only
- [ ] Small collections (≤15) display in horizontal row
- [ ] Large collections (>15) display in 7-column grid
- [ ] Grid preserves server sort order (Timeline Order for Universe Collections)
- [ ] Collections with 100+ items remain performant
- [ ] Focus restoration works when navigating UP from content
- [ ] Visual consistency with MovieDetailScreen achieved
- [ ] `LARGE_COLLECTION_THRESHOLD` allows easy style switching
