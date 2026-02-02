# Collections Library Screen Refresh

**Date:** 2026-02-01
**Branch:** `feature/collections-refresh`
**Scope:** `CollectionsLibraryScreen` only (CollectionDetailScreen unchanged)

## Goal

Update `CollectionsLibraryScreen` to match `LibraryScreen` design patterns for a unified browsing experience.

## Changes Overview

| Feature | Current | After |
|---------|---------|-------|
| Background | DynamicBackground (gradient) | Black background |
| Filter Bar | None | LibraryFilterBar |
| Filter Menu | None | LibraryFilterMenu (genres, year, rating, watched, favorites) |
| Sort Menu | None | LibrarySortMenu (name, date added, rating) |
| View Mode | Poster only (7 cols) | Toggle: Poster (7 cols) / Thumbnail (4 cols) |
| Long-press Menu | None | Go to, Mark watched, Toggle favorite |
| Alphabet Nav | AlphabetScrollBar | Keep as secondary navigation |

## Architecture

### Option Selected: Extend CollectionsViewModel

Rather than generalizing `LibraryViewModel`, we'll extend `CollectionsViewModel` to support the same filter/sort state pattern. This keeps concerns separate (collections vs library items) while achieving UI consistency.

**Rationale:**
- Collections (BoxSets) have different semantics than library items
- Avoids complicating LibraryViewModel with collection-specific logic
- CollectionsViewModel already has pagination and prefetch working well

### Filter State

Add `CollectionFilterState` mirroring `LibraryFilterState`:

```kotlin
data class CollectionFilterState(
    val viewMode: LibraryViewMode = LibraryViewMode.POSTER,
    val sortBy: String = "SortName",
    val sortOrder: String = "Ascending",
    val watchedFilter: WatchedFilter = WatchedFilter.ALL,
    val ratingFilter: Float? = null,
    val yearRange: IntRange? = null,
    val favoritesOnly: Boolean = false,
    val selectedGenres: Set<String> = emptySet(),
    val selectedParentalRatings: Set<String> = emptySet(),
)
```

## Implementation Tasks

### 1. API Layer (`JellyfinClient.kt`)

Extend `getCollectionsFiltered` to support all filter parameters:

```kotlin
suspend fun getCollectionsFiltered(
    limit: Int = 100,
    startIndex: Int = 0,
    sortBy: String? = null,
    sortOrder: String? = null,
    nameStartsWith: String? = null,
    excludeUniverseCollections: Boolean = false,
    // New filter parameters:
    genres: List<String>? = null,
    isPlayed: Boolean? = null,
    isFavorite: Boolean? = null,
    minCommunityRating: Float? = null,
    years: String? = null,
    officialRatings: List<String>? = null,
): Result<ItemsResponse>
```

Add method to fetch available genres for BoxSets:
```kotlin
suspend fun getCollectionGenres(): Result<List<GenreItem>>
```

### 2. ViewModel (`CollectionsViewModel.kt`)

- Add `CollectionFilterState` to `CollectionsUiState`
- Add filter/sort methods matching LibraryViewModel:
  - `setViewMode(mode: LibraryViewMode)`
  - `updateSort(sortBy: String, sortOrder: String)`
  - `applyFilters(...)`
  - `toggleGenre(genre: String)`
  - `setPlayed(itemId: String, played: Boolean)`
  - `setFavorite(itemId: String, favorite: Boolean)`
- Add `availableGenres` and `availableParentalRatings` to state
- Load genres on init

### 3. Screen (`CollectionsLibraryScreen.kt`)

- Remove `DynamicBackground` and `rememberGradientColors`
- Add black background (`TvColors.Background`)
- Add `LibraryFilterBar` with:
  - Title: "Collections"
  - Count display
  - Shuffle button (picks random collection)
  - View mode toggle
  - Filter menu button
  - Sort menu button
- Add `LibraryFilterMenu` and `LibrarySortMenu` (outside focusGroup)
- Add `DialogPopup` for long-press menu
- Update grid to use dynamic columns based on viewMode
- Add `onLongClick` to `MediaCard` for each collection
- Keep `AlphabetScrollBar` on right side

### 4. Long-press Menu Actions

For collections, the menu should include:
- **Go to Collection** - Navigate to CollectionDetailScreen
- **Mark Watched** / **Mark Unwatched** - Toggle all items in collection
- **Add to Favorites** / **Remove from Favorites** - Toggle collection favorite

### 5. Focus Management

- Filter bar gets `focusProperties { down = firstItemFocusRequester }`
- Grid first row gets `focusProperties { up = filterBarFirstButtonFocusRequester }`
- Keep existing AlphabetScrollBar focus integration
- DialogPopup inside focusGroup for proper focus trapping

## File Changes

| File | Change |
|------|--------|
| `core/network/.../JellyfinClient.kt` | Extend `getCollectionsFiltered`, add `getCollectionGenres` |
| `app-tv/.../CollectionsViewModel.kt` | Add filter state, filter methods, genre loading |
| `app-tv/.../CollectionsLibraryScreen.kt` | Replace background, add filter bar, menus, long-press |

## Testing Checklist

- [ ] Black background displays correctly
- [ ] Filter bar shows title, count, all buttons
- [ ] View mode toggle switches between poster (7 cols) and thumbnail (4 cols)
- [ ] Sort menu changes sort order (verify API calls)
- [ ] Filter menu filters work (genres, year, watched, favorites, parental)
- [ ] Long-press opens dialog with correct actions
- [ ] Mark watched/favorite updates UI optimistically
- [ ] AlphabetScrollBar still works alongside filter bar
- [ ] Focus navigation: filter bar <-> grid <-> alphabet bar
- [ ] NavRail exit focus restoration works
- [ ] Pagination still loads more items on scroll

## Out of Scope

- CollectionDetailScreen changes (future work)
- Universe collections special handling
- Search functionality
