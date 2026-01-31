# Episode Screen Enhancement Plan

## Overview

This plan outlines potential enhancements to the episode screen using available Jellyfin API endpoints and existing data models. Features are organized by category with UX impact, complexity, and platform considerations.

---

## Current State

### TV App (`EpisodesScreen.kt`)
- Hero section with Ken Burns animated backdrop
- Episode card row with progress bars, watched/favorite indicators
- Tabs: Seasons, Details, Media Info, Cast & Crew, Guest Stars
- Action buttons: Play/Resume, Mark Watched, Add Favorite
- Episode metadata: S#E#, air date, runtime, official rating, community rating

### Mobile App (`EpisodeDetailScreen.kt`)
- Header with series name, episode title, metadata
- Play buttons with resume support
- Chapters row (already implemented)
- Details section, Guest Stars, Crew
- Similar Items row

---

## Proposed Enhancements

### Category A: Skip Intro/Outro Integration

**API:** `/MediaSegments/{itemId}` - Returns segment markers (Intro, Outro, Recap, Preview, Commercial)

| Feature | Description | UX Impact | Complexity | Platform |
|---------|-------------|-----------|------------|----------|
| Segment Timeline Markers | Show intro/recap/outro segments as colored bars on episode card progress indicators | Medium | Low | Both |
| Pre-play Segment Info | Display "Contains 90s intro, 30s recap" badge in episode details | Low | Low | Both |
| Skip Button Integration | Show "Skip Intro" button during playback (player integration required) | High | High | Both |

**Implementation Notes:**
- MediaSegment model already exists in `JellyfinModels.kt`
- Player integration needed for skip button - separate scope
- Timeline markers can be added to existing progress bars

---

### Category B: Next Episode Experience

**API:** `/Shows/NextUp` - Returns next episode to watch for a series

| Feature | Description | UX Impact | Complexity | Platform |
|---------|-------------|-----------|------------|----------|
| "Up Next" Card | Show small preview card of next episode in hero section | Medium | Medium | TV |
| Post-Episode Transition | After playback ends, show "Next in 10s" with episode preview | High | High | Both |
| Quick Episode Jump | Button to jump directly to next unwatched episode | Medium | Low | Both |

**Implementation Notes:**
- Already have `findTargetEpisode()` logic that finds in-progress/unwatched episodes
- Post-play experience is player scope, but pre-play "Next" indicator is episode screen scope
- TV focus management consideration: "Up Next" card should be focusable

---

### Category C: Theme Media Integration

**APIs:**
- `/Items/{itemId}/ThemeSongs` - Theme music
- `/Items/{itemId}/ThemeVideos` - Opening sequence videos

| Feature | Description | UX Impact | Complexity | Platform |
|---------|-------------|-----------|------------|----------|
| "Play Theme" Button | Add action button to play series theme song | Low | Medium | Both |
| Theme Video Tile | Show "Watch Opening" option if theme video exists | Low | Medium | Both |
| Ambient Theme Music | Auto-play theme music on screen entry (user preference) | Low | High | TV |

**Implementation Notes:**
- Model `AllThemeMediaResult` already exists
- Requires audio playback integration
- User setting needed to avoid annoyance
- **Recommendation:** Low priority - niche feature

---

### Category D: Enhanced Metadata Display

**Available Data:** `providerIds`, `externalUrls`, `tags`, `genres`, `studios`, `productionLocations`

| Feature | Description | UX Impact | Complexity | Platform |
|---------|-------------|-----------|------------|----------|
| IMDB/TMDB Badges | Show ratings badges from external providers | Medium | Low | Both |
| External Links Section | Clickable links to IMDB, TMDB, TVDb pages | Medium | Low | Mobile |
| Tags/Genres as Chips | Display tags and genres as clickable chips for filtering | Medium | Medium | Both |
| Studio Attribution | Show production company logos or names | Low | Low | Both |

**Implementation Notes:**
- `providerIds` contains IMDB, TMDB IDs - can construct URLs
- TV: External links open in system browser (less useful)
- Mobile: Deep links to IMDB app if installed
- Tags/genres can integrate with existing search/filter infrastructure

---

### Category E: Discovery Features

**APIs:**
- `/Items/{itemId}/Similar` - Similar content
- `/Shows/Upcoming` - Upcoming episodes

| Feature | Description | UX Impact | Complexity | Platform |
|---------|-------------|-----------|------------|----------|
| Similar Episodes Row | "If you liked this" carousel showing similar episodes | Medium | Medium | Both |
| Upcoming Episodes | Show upcoming air dates for ongoing series | Medium | Medium | Both |
| Studio Exploration | Tap studio to see other shows from same studio | Low | Medium | Both |

**Implementation Notes:**
- Mobile already has `similarItems` in DetailUiState
- TV could add a "Similar" tab or integrate into existing tabs
- Upcoming episodes useful for currently-airing series

---

### Category F: Trickplay/Seek Preview

**API:** `/Videos/{itemId}/Trickplay/{width}/tiles.m3u8` - Seek preview thumbnails

| Feature | Description | UX Impact | Complexity | Platform |
|---------|-------------|-----------|------------|----------|
| Seek Preview Thumbnails | Show thumbnail preview while seeking in player | High | High | Both |
| Episode Preview Scrubber | Mini timeline with preview images in episode card | Low | High | TV |

**Implementation Notes:**
- `TrickplayInfo` model already exists
- Primary benefit is during playback (player scope)
- Pre-play preview scrubber is complex for limited value

---

### Category G: Chapter Enhancements

**Available Data:** `chapters: List<JellyfinChapter>`

| Feature | Description | UX Impact | Complexity | Platform |
|---------|-------------|-----------|------------|----------|
| Chapter Jump List | Clickable chapter list with timestamps and thumbnails | Medium | Low | Both |
| Chapter Markers on Progress | Visual markers on progress bar for chapter boundaries | Low | Low | Both |

**Implementation Notes:**
- Mobile already has `ChaptersRow` component
- TV could add chapters to Details tab or as separate tab
- Chapter images available via `/Items/{itemId}/Images/Chapter/{index}`

---

## Priority Recommendations

### Tier 1: Quick Wins (Low Effort, Medium-High Value)

1. **IMDB/TMDB Rating Badges** - Use existing `providerIds` to show external ratings
2. **External Links Section** (Mobile) - Clickable links to IMDB/TMDB pages
3. **Chapter List for TV** - Port mobile's ChaptersRow to TV Details tab
4. **Tags/Genres as Chips** - Make metadata interactive for filtering

### Tier 2: Medium Effort, High Value

5. **Segment Timeline Markers** - Visual intro/recap/outro markers on progress
6. **"Up Next" Indicator** - Small card showing next unwatched episode
7. **Similar Episodes Tab** (TV) - New tab or section for discovery

### Tier 3: High Effort, High Value (Player Integration)

8. **Skip Intro/Outro Buttons** - Requires player UI integration
9. **Post-Play "Next Episode" Countdown** - Player end-of-playback handling
10. **Seek Preview Thumbnails** - Player seek bar integration

### Not Recommended

- **Auto-play Theme Music** - High annoyance potential, niche appeal
- **Pre-play Preview Scrubber** - Complex implementation, limited value

---

## API Endpoints Summary

| Endpoint | Purpose | Currently Used |
|----------|---------|----------------|
| `/Shows/{seriesId}/Episodes` | Get episodes | Yes |
| `/Items/{itemId}` | Get item details | Yes |
| `/MediaSegments/{itemId}` | Skip segment markers | No |
| `/Items/{itemId}/ThemeSongs` | Theme music | No |
| `/Items/{itemId}/ThemeVideos` | Theme videos | No |
| `/Shows/NextUp` | Next episode | Partial |
| `/Shows/Upcoming` | Upcoming episodes | No |
| `/Items/{itemId}/Similar` | Similar content | Mobile only |
| `/Videos/{itemId}/Trickplay/{width}/{index}.jpg` | Seek thumbnails | No (model exists) |
| `/Items/{itemId}/Images/Chapter/{index}` | Chapter images | Yes (mobile) |

---

## Implementation Approach

### Phase 1: Metadata Enhancements
- Add IMDB/TMDB badges to episode hero
- Add external links section (mobile)
- Port chapters to TV

### Phase 2: Discovery Features
- Implement Similar tab for TV
- Add "Up Next" indicator
- Integrate segment markers visualization

### Phase 3: Player Integration (Separate Scope)
- Skip intro/outro buttons
- Post-play next episode flow
- Trickplay seek previews

---

## Platform-Specific Considerations

### Android TV
- D-pad navigation: All new features must be focusable
- Focus restoration: New tabs/sections need proper focus management
- Limited text input: Avoid search-heavy features
- 10-foot UI: Badges and icons must be visible from distance

### Mobile
- Touch interaction: Swipe gestures, tap targets
- Deep linking: Can open external apps (IMDB, etc.)
- Screen space: Bottom sheets for additional info
- Scrolling: Long-form content is acceptable

---

*Generated by multi-model collaboration: Claude (orchestration) + Codex (feature analysis)*
