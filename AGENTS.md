# MyFlix Seerr Parity Plan

Goal: Reach feature parity with the Seerr mobile reference app under `references/` without importing any reference code. This plan targets both `app-mobile` and `app-tv` where applicable.

## Scope Summary
- Dynamic Discover (server-configured sliders)
- Search (movie/tv/person)
- Requests list and management
- Rich details (recommendations, similar, ratings, crew, collections)
- Advanced request flow (season selection, 4K, quotas, overrides)
- Optional: Plex/local auth flows

## Phase 0: Baseline + Models
- Add Seerr discover settings endpoint support (`/api/v1/settings/discover`) and models.
- Confirm Seerr endpoints in `core/seerr/SeerrClient.kt` cover:
  - Discover sliders
  - Recommendations + similar
  - Watchlist (get/add/remove)
  - Requests (list, detail, approve/decline/cancel)
  - User quota
- Add missing data models to `core/seerr` (discover slider, filters, request/season details).

## Phase 1: Navigation + Shell Screens
- Add routes for:
  - Discover: movies, tv, trending, watchlist
  - Search: combined (movie/tv/person)
  - Requests list
  - Collection detail
- Add placeholder screens wired to `SeerrClient` calls.
- Ensure shared nav helpers in `core/common` for route encoding/decoding.

## Phase 2: Discover Parity
- Implement dynamic discover screen based on `/api/v1/settings/discover`:
  - recently added, recent requests, watchlist, trending, popular, upcoming
  - genre/network/studio sliders
  - custom TMDB keyword/genre sliders
- Add pagination and pull-to-refresh behavior.
- TV version: focus-safe rows + grid screens for "see all".

## Phase 3: Search Parity
- Implement search with paging:
  - combined results with media type chips
  - separate "see all" screens (optional)
- Add person results -> actor detail navigation.

## Phase 4: Requests Parity
- Implement requests list:
  - filter/sort (pending/approved/available/etc.)
  - paging (take/skip)
  - admin actions (approve/decline/cancel) if permissions allow
- Optional: request detail screen.

## Phase 5: Detail Parity
- Movie/TV detail enhancements:
  - recommendations + similar rows
  - crew + full cast
  - ratings (IMDB/RT/TMDB) if available
  - external links/watchlist toggle
- Collection details:
  - list parts with request actions
- Keep UI parity for both mobile + TV, with TV focus patterns.

## Phase 6: Advanced Request Flow
- Request modal for:
  - 4K toggle
  - quota display + enforcement
  - advanced requester options (server/user overrides)
  - TV season selection with partial request support

## Phase 7: Auth Extensions (Optional)
- Plex OAuth login flow.
- Local Jellyseerr login (username/password).
- Persist tokens safely (avoid plaintext credentials).

## Phase 8: QA + Validation
- Add tests for SeerrClient endpoints where practical.
- Add UI sanity checks for major screens.
- Validate flows on both mobile and TV.

## Milestones (Recommended Order)
1. Phase 0 + 1 (client models + routes)
2. Phase 2 (Discover)
3. Phase 3 (Search)
4. Phase 4 (Requests)
5. Phase 5 (Details)
6. Phase 6 (Advanced requests)
7. Phase 7 (Auth extensions)
8. Phase 8 (QA)
