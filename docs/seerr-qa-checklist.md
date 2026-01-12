## Seerr QA Checklist (Phase 8)

Use this list to sanity-check mobile and TV flows after integrating phases 0-7.

### Setup + Auth
- Connect to Seerr by URL (manual).
- Jellyfin login (auto + manual).
- Local login (manual).
- Verify Seerr token persistence across app relaunch.

### Discover
- Home screen populates dynamic rows from server discover settings.
- Each row opens the "see all" grid/list.
- Watchlist add/remove reflects in watchlist row.

### Search
- Combined search returns movies, TV, and people.
- Filter chips work (All/Movie/TV/People).
- Paging loads additional results at end of list/grid.
- Person result navigates to actor detail screen.

### Detail
- Availability status shows (available/pending/partial).
- Request button works for movie + TV.
- Season selection changes request payload for TV.
- 4K toggle changes request payload.
- Quota text displays and blocks request when exhausted.
- Watchlist toggle adds/removes item.
- Recommendations + similar rows navigate to details.
- External links open TMDb/IMDb.

### Requests
- Mine vs All scopes work.
- Filter + sort chips change results.
- Paging loads additional results.
- Admin actions: approve/decline/cancel.

### Collection
- Parts list shows status.
- Request from collection part updates status on refresh.

### TV Focus
- D-pad navigation works for chips/buttons and rows.
- Back button reachable from first focus.
