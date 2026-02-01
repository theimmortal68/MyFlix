# Jellyfin MCP for MyFlix UI

A focused Jellyfin API specification tailored for MyFlix TV/Mobile UI development. Reduces the full ~2MB API spec to ~100KB while covering all UI-relevant endpoints.

## What's Included

### Home Screen Endpoints
| Endpoint | Use Case |
|----------|----------|
| `GET /UserItems/Resume` | Continue Watching row |
| `GET /Shows/NextUp` | Next Up row |
| `GET /Items/Latest` | Latest Movies/Shows rows |
| `GET /Items` + genreIds | Genre rows |
| `GET /Items` + tags | Universe Collection rows |
| `GET /Movies/Recommendations` | Personalized recommendations |
| `GET /Shows/Upcoming` | Upcoming episodes |

### Detail Screen Endpoints
| Endpoint | Use Case |
|----------|----------|
| `GET /Items/{itemId}` | Movie/Series/Episode details |
| `GET /Shows/{seriesId}/Seasons` | Season list |
| `GET /Shows/{seriesId}/Episodes` | Episode list |
| `GET /Items/{itemId}/Similar` | "More Like This" row |
| `GET /Items/{itemId}/ThemeMedia` | Theme music |
| `GET /Persons/{name}` | Cast/crew details |

### Browse & Search Endpoints
| Endpoint | Use Case |
|----------|----------|
| `GET /Items` | Library browsing with filters |
| `GET /UserViews` | Library navigation |
| `GET /Genres` | Genre list |
| `GET /Persons` | Cast/crew browsing |
| `GET /Search/Hints` | Search functionality |

### Playback Endpoints
| Endpoint | Use Case |
|----------|----------|
| `GET/POST /Items/{itemId}/PlaybackInfo` | Get playback URLs |
| `GET /MediaSegments/{itemId}` | Skip intro/credits markers |
| `GET /Items/{itemId}/Intros` | Pre-roll videos |
| `POST /Sessions/Playing` | Report playback start |
| `POST /Sessions/Playing/Progress` | Report progress (every 10s) |
| `POST /Sessions/Playing/Stopped` | Report playback stop |

### User Data Endpoints
| Endpoint | Use Case |
|----------|----------|
| `GET/POST /UserItems/{itemId}/UserData` | Get/update user data |
| `POST/DELETE /UserPlayedItems/{itemId}` | Mark played/unplayed |
| `POST/DELETE /UserFavoriteItems/{itemId}` | Toggle favorite |
| `GET /Users/Me` | Current user info |
| `GET /Users/Public` | Login screen users |
| `POST /Users/AuthenticateByName` | Login |

### Image Endpoints
| Endpoint | Use Case |
|----------|----------|
| `GET /Items/{itemId}/Images/{imageType}` | Get item images |

## Setup

### 1. Install the MCP Server

```bash
npm install -g @anthropic/openapi-mcp-server
```

### 2. Place the Spec File

Copy `jellyfin-myflix-ui.json` to a known location:

```bash
mkdir -p ~/.mcp-servers/jellyfin
cp jellyfin-myflix-ui.json ~/.mcp-servers/jellyfin/
```

### 3. Get Your Jellyfin API Key

**Option A: Dashboard API Key**
1. Go to Jellyfin Dashboard → API Keys
2. Create new key with description "MyFlix MCP"
3. Copy the token

**Option B: User Token (for user-specific data)**
```bash
curl -X POST "http://your-server:8096/Users/AuthenticateByName" \
  -H "Content-Type: application/json" \
  -H "X-Emby-Authorization: MediaBrowser Client=\"MyFlix\", Device=\"MCP\", DeviceId=\"myflix-mcp\", Version=\"1.0\"" \
  -d '{"Username": "your-user", "Pw": "your-password"}'
```

### 4. Configure Claude Code

Add to your project's `.mcp.json` or `~/.claude/mcp.json`:

```json
{
  "mcpServers": {
    "jellyfin-ui": {
      "command": "npx",
      "args": [
        "-y",
        "@anthropic/openapi-mcp-server",
        "~/.mcp-servers/jellyfin/jellyfin-myflix-ui.json"
      ],
      "env": {
        "API_BASE_URL": "http://your-jellyfin-server:8096",
        "API_KEY": "your-api-key-here"
      }
    }
  }
}
```

## Quick Reference

### Common Query Patterns

**Continue Watching:**
```
GET /UserItems/Resume?userId={userId}&limit=20&includeItemTypes=Movie,Episode
```

**Next Up:**
```
GET /Shows/NextUp?userId={userId}&limit=20&enableResumable=true
```

**Latest Movies:**
```
GET /Items/Latest?userId={userId}&includeItemTypes=Movie&limit=20
```

**Latest TV Shows:**
```
GET /Items/Latest?userId={userId}&includeItemTypes=Series&limit=20
```

**Genre Row (e.g., Action Movies):**
```
GET /Items?userId={userId}&includeItemTypes=Movie&genreIds={actionGenreId}&sortBy=SortName&recursive=true&limit=20
```

**Universe Collection Items (e.g., MCU):**
```
GET /Items?userId={userId}&includeItemTypes=Movie,Series&tags=universe-collection:mcu&sortBy=PremiereDate&recursive=true
```

**All Universe Collections:**
```
GET /Items?userId={userId}&includeItemTypes=BoxSet&tags=universe-collection&recursive=true
```

**Search:**
```
GET /Search/Hints?userId={userId}&searchTerm=batman&limit=20&includeItemTypes=Movie,Series,Person
```

### Recommended Fields Parameter

For home screen rows (minimal data):
```
fields=PrimaryImageAspectRatio
```

For detail screens (full data):
```
fields=Overview,People,Genres,Studios,Tags,ProviderIds,MediaStreams,Chapters,RemoteTrailers
```

For playback:
```
fields=MediaSources,MediaStreams,Chapters
```

### Image URL Pattern

```
{serverUrl}/Items/{itemId}/Images/{imageType}?maxWidth={width}&quality=90&tag={imageTag}
```

Image types: `Primary`, `Backdrop`, `Thumb`, `Logo`, `Banner`, `Art`

### Key Schema Notes

**BaseItemDto.Type values:**
- `Movie` - Movie
- `Series` - TV Show
- `Season` - TV Season
- `Episode` - TV Episode
- `BoxSet` - Collection
- `Person` - Actor/Director
- `Genre` - Genre
- `Studio` - Production Studio
- `MusicAlbum`, `MusicArtist`, `Audio` - Music

**Time values:**
- All time values are in "ticks" (10,000 ticks = 1 millisecond)
- `RunTimeTicks / 10_000_000` = seconds
- `PlaybackPositionTicks / 10_000_000` = resume position in seconds

**User Data (UserItemDataDto):**
- `Played` - Has been watched
- `IsFavorite` - Is in favorites
- `PlaybackPositionTicks` - Resume position
- `PlayCount` - Watch count
- `PlayedPercentage` - Percent complete

## Comparison: Full API vs UI Subset

| Aspect | Full API | UI Subset |
|--------|----------|-----------|
| File Size | ~2MB | ~100KB |
| Endpoints | 200+ | 30 |
| Schemas | 400+ | 35 |
| Context Tokens | ~500K | ~25K |
| Coverage | Everything | UI content display |

## Files

- `jellyfin-myflix-ui.json` - The filtered OpenAPI spec
- `mcp-config-example.json` - Example MCP configuration
- `README.md` - This file

## Usage in Claude Code

Once configured, you can ask Claude Code:

```
> What does the Continue Watching API response look like?
> Show me the data structure for a series with seasons and episodes
> What fields are available on a Movie item?
> How do I filter items by genre?
> What's the difference between /Items/Latest and /UserItems/Resume?
```

This helps validate UI assumptions against real API responses without loading the entire 2MB spec.
