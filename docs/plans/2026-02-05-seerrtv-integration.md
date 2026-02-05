# SeerrTV Integration - Complete Rewrite Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the current MyFlix Seerr implementation with all SeerrTV features and UI, backed by a clean API client written from scratch using the Jellyseerr OpenAPI spec.

**Architecture:**
- **Backend**: Write `SeerrClient.kt` from scratch following `JellyfinClient.kt` patterns exactly. Use `references/seerr/seerr-api.yml` (OpenAPI spec) as the authoritative API reference. Direct Ktor calls, no SDK abstractions.
- **UI/Features**: Adapt all SeerrTV screens and features to MyFlix design patterns (TvColors, focus halo, exit focus restoration, CardSizes, DialogPopup).

**Tech Stack:** Jetpack Compose for TV, Ktor HTTP Client, Kotlinx Serialization, Hilt DI, Kotlin Coroutines/Flow, Coil3 image loading

---

## Implementation Status

**Last Updated:** 2026-02-05

| Phase | Description | Status | Notes |
|-------|-------------|--------|-------|
| **Phase 1** | Core API Client Rewrite | ✅ **DONE** | Models, SeerrClient, Repository, DI |
| **Phase 2** | ViewModels | ✅ **DONE** | Home, Detail, Search, Requests, Issues, Setup |
| **Phase 3** | TV App Screens (New UI) | ❌ **NOT STARTED** | Old UI still in use |
| **Phase 4** | Navigation Integration | ⚠️ **PARTIAL** | Routes exist, NavRail works, but uses old screens |
| **Phase 5** | Testing & Polish | ⚠️ **PARTIAL** | Unit tests exist, manual testing incomplete |

### Task-Level Status

#### Phase 1: Core API Client Rewrite
- [x] Task 1.1: Write Data Models from OpenAPI Spec
- [x] Task 1.2: Write SeerrClient from OpenAPI Spec
- [x] Task 1.3: Create Seerr Repository Layer
- [x] Task 1.4: Update Hilt DI Module

#### Phase 2: ViewModels
- [x] Task 2.1: Create SeerrHomeViewModel
- [x] Task 2.2: Create SeerrDetailViewModel
- [x] Task 2.3: Create Additional ViewModels (Search, Requests, Issues, Setup)

#### Phase 3: TV App Screens (MyFlix UI/UX) — NOT STARTED
- [ ] Task 3.1: Create SeerrHomeScreen (new UI with Hero section)
- [ ] Task 3.2: Create SeerrDetailScreen (new UI)
- [ ] Task 3.3: Create SeerrSearchScreen (new UI)
- [ ] Task 3.4: Create SeerrRequestsScreen (new UI)
- [ ] Task 3.5: Create SeerrSetupScreen (new UI)
- [ ] Task 3.6: Create Shared Seerr Components

#### Phase 4: Navigation Integration
- [x] Task 4.1: Update Navigation Graph (routes exist)
- [x] Task 4.2: Add Seerr to NavRail (Discover item works)

#### Phase 5: Testing & Polish
- [x] Task 5.1: Add Unit Tests (SeerrRepositoryTest, SeerrDiscoverHelperTest)
- [ ] Task 5.2: Integration Testing (checklist incomplete)
- [ ] Task 5.3: Performance Optimization

### Blocking Issues

1. **Phase 3 not started**: The `references/seerrtv/` directory with UI reference material does not exist
2. **Old UI still active**: Current screens in `app-tv/ui/screens/Seerr*.kt` are the pre-rewrite versions
3. **No Hero section**: Plan specifies `SeerrHeroSection` component but it was never created

---

## Project Ethos

**Use MyFlix's existing architecture throughout:**

| Layer | Pattern | Reference |
|-------|---------|-----------|
| **API Client** | Direct Ktor, `Result<T>`, `runCatching` | `JellyfinClient.kt` |
| **State Management** | `StateFlow`, `MutableStateFlow` | Existing ViewModels |
| **DI** | Hilt `@HiltViewModel`, `@Inject` | Existing modules |
| **Image Loading** | Coil `AsyncImage` | Existing composables |
| **UI** | Jetpack Compose for TV | `app-tv/ui/` |
| **Models** | `@Serializable` with `@SerialName` | `JellyfinModels.kt` |

**DO:**
- Write API client from scratch using OpenAPI spec (`seerr-api.yml`)
- Follow `JellyfinClient.kt` patterns exactly (direct Ktor, `Result<T>`, `runCatching`)
- Use `@SerialName` annotations matching JSON fields from OpenAPI schemas
- Match existing ViewModel patterns (StateFlow, viewModelScope)
- Use Coil's `AsyncImage` for all image loading
- Only implement endpoints MyFlix actually needs
- Adapt SeerrTV UI to MyFlix design patterns

**DON'T:**
- Copy/port SeerrTV's `SeerrApiService.kt` code
- Use any SDK abstractions or third-party wrappers
- Introduce different state management patterns
- Include features MyFlix doesn't need (Plex auth, auto-updates)

**Reference Sources:**

| Source | Use For |
|--------|---------|
| `references/seerr/seerr-api.yml` | API endpoint paths, parameters, response schemas |
| `references/seerr/server/routes/*.ts` | Understanding API behavior and edge cases |
| `references/seerrtv/` | UI screens, components, feature interactions |
| `core/network/JellyfinClient.kt` | Backend code patterns to match |
| `app-tv/ui/` | UI/UX patterns to follow |

---

## Required Code Patterns

### SeerrClient Pattern (must match JellyfinClient)

```kotlin
class SeerrClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@SeerrClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest { contentType(ContentType.Application.Json) }
    }

    var serverUrl: String? = null
    var accessToken: String? = null  // Session cookie or API key
    var userId: String? = null

    private val baseUrl: String get() = serverUrl?.let { "$it/api/v1" } ?: error("Server URL not set")

    // API methods follow this exact pattern:
    suspend fun getDiscoverMovies(page: Int = 1): Result<SeerrDiscoverResponse> = runCatching {
        httpClient.get("$baseUrl/discover/movies") {
            header("Cookie", "connect.sid=$accessToken")
            parameter("page", page)
        }.body()
    }

    suspend fun createRequest(
        mediaType: String,
        mediaId: Int,
        is4k: Boolean = false,
    ): Result<SeerrRequest> = runCatching {
        httpClient.post("$baseUrl/request") {
            header("Cookie", "connect.sid=$accessToken")
            setBody(mapOf(
                "mediaType" to mediaType,
                "mediaId" to mediaId,
                "is4k" to is4k,
            ))
        }.body()
    }
}
```

### Model Pattern (must use @SerialName for JSON field names)

```kotlin
@Serializable
data class SeerrMedia(
    @SerialName("id") val id: Int,
    @SerialName("mediaType") val mediaType: String,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,  // TV shows use 'name'
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("mediaInfo") val mediaInfo: SeerrMediaInfo? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
}
```

### ViewModel Pattern (must match existing ViewModels)

```kotlin
@HiltViewModel
class SeerrHomeViewModel @Inject constructor(
    private val seerrClient: SeerrClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeerrHomeUiState())
    val uiState: StateFlow<SeerrHomeUiState> = _uiState.asStateFlow()

    fun loadDiscoverRows() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            seerrClient.getDiscoverMovies()
                .onSuccess { response ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        trendingMovies = response.results,
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message,
                    ) }
                }
        }
    }
}

data class SeerrHomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val trendingMovies: List<SeerrMedia> = emptyList(),
)
```

### Composable Pattern (Coil + MyFlix components)

```kotlin
@Composable
fun SeerrMediaCard(
    media: SeerrMedia,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    // Use Coil AsyncImage
    AsyncImage(
        model = SeerrClient.posterUrl(media.posterPath),
        contentDescription = media.displayTitle,
        modifier = Modifier
            .width(CardSizes.PortraitWidth)
            .aspectRatio(2f / 3f)
            .onFocusChanged { isFocused = it.isFocused },
        contentScale = ContentScale.Crop,
    )
}
```

> **IMPORTANT**: The code samples in Phase 1 tasks below are reference material showing
> data structures. During implementation, rewrite all code to match MyFlix patterns
> shown above. Do not copy the code verbatim.

---

## Phase 1: Core API Client Rewrite

### Task 1.1: Write Data Models from OpenAPI Spec

**Files:**
- Replace: `core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrModels.kt`

**Context:**
Write models from `references/seerr/seerr-api.yml` OpenAPI schemas. Reference SeerrTV for field usage patterns but derive structure from the API spec.

**Key models needed:**
- `SeerrMedia`, `SeerrMediaInfo` - Discovery and browse results
- `SeerrMovieDetails`, `SeerrTvDetails` - Detail screens
- `SeerrRequest`, `SeerrRequestResponse` - Request management
- `SeerrIssue`, `SeerrIssueComment` - Issue tracking
- `SeerrUser`, `SeerrQuota` - User and quota info
- `SeerrCredits`, `SeerrCastMember`, `SeerrCrewMember` - Cast/crew
- `SeerrRatings` - External ratings (IMDB, RT, etc.)
- `SeerrDiscoverResponse`, `SeerrSearchResponse` - API responses

**Step 1: Read OpenAPI schemas**

Review `references/seerr/seerr-api.yml` components/schemas section for field names and types.

**Step 2: Create models with @SerialName annotations**

All field names must use `@SerialName` matching the JSON field names from the API:

```kotlin
package dev.jausc.myflix.core.seerr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================================
// MEDIA MODELS
// ============================================================================

@Serializable
data class SeerrMedia(
    val id: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String? = null,
    val name: String? = null, // TV shows use 'name'
    val originalTitle: String? = null,
    val originalName: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val popularity: Double? = null,
    val originalLanguage: String? = null,
    val genreIds: List<Int>? = null,
    val adult: Boolean? = null,
    val video: Boolean? = null,
    val mediaInfo: SeerrMediaInfo? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayReleaseDate: String? get() = releaseDate ?: firstAirDate
}

@Serializable
data class SeerrMediaInfo(
    val id: Int? = null,
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val imdbId: String? = null,
    val status: Int? = null, // 1=Unknown, 2=Pending, 3=Processing, 4=Partial, 5=Available
    val status4k: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val mediaAddedAt: String? = null,
    val serviceId: Int? = null,
    val serviceId4k: Int? = null,
    val externalServiceId: Int? = null,
    val externalServiceId4k: Int? = null,
    val externalServiceSlug: String? = null,
    val externalServiceSlug4k: String? = null,
    val ratingKey: String? = null, // Plex
    val ratingKey4k: String? = null,
    val jellyfinMediaId: String? = null,
    val jellyfinMediaId4k: String? = null,
    val iOSPlexUrl: String? = null,
    val iOSPlexUrl4k: String? = null,
    val plexUrl: String? = null,
    val plexUrl4k: String? = null,
    val downloadStatus: List<SeerrDownloadStatus>? = null,
    val downloadStatus4k: List<SeerrDownloadStatus>? = null,
    val requests: List<SeerrRequest>? = null,
    val issues: List<SeerrIssue>? = null,
)

@Serializable
data class SeerrDownloadStatus(
    val externalId: Int? = null,
    val estimatedCompletionTime: String? = null,
    val mediaType: String? = null,
    val size: Long? = null,
    val sizeLeft: Long? = null,
    val status: String? = null,
    val timeLeft: String? = null,
    val title: String? = null,
)

// ============================================================================
// DETAILED MEDIA MODELS
// ============================================================================

@Serializable
data class SeerrMovieDetails(
    val id: Int,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val popularity: Double? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val status: String? = null,
    val tagline: String? = null,
    val homepage: String? = null,
    val imdbId: String? = null,
    val originalLanguage: String? = null,
    val adult: Boolean? = null,
    val video: Boolean? = null,
    val genres: List<SeerrGenre>? = null,
    val productionCompanies: List<SeerrProductionCompany>? = null,
    val productionCountries: List<SeerrProductionCountry>? = null,
    val spokenLanguages: List<SeerrSpokenLanguage>? = null,
    val credits: SeerrCredits? = null,
    val externalIds: SeerrExternalIds? = null,
    val mediaInfo: SeerrMediaInfo? = null,
    val collection: SeerrCollectionInfo? = null,
    val relatedVideos: List<SeerrRelatedVideo>? = null,
    val watchProviders: List<SeerrWatchProvider>? = null,
)

@Serializable
data class SeerrTvDetails(
    val id: Int,
    val name: String,
    val originalName: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val firstAirDate: String? = null,
    val lastAirDate: String? = null,
    val episodeRunTime: List<Int>? = null,
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val popularity: Double? = null,
    val status: String? = null,
    val tagline: String? = null,
    val type: String? = null,
    val homepage: String? = null,
    val originalLanguage: String? = null,
    val inProduction: Boolean? = null,
    val numberOfEpisodes: Int? = null,
    val numberOfSeasons: Int? = null,
    val genres: List<SeerrGenre>? = null,
    val networks: List<SeerrNetwork>? = null,
    val productionCompanies: List<SeerrProductionCompany>? = null,
    val productionCountries: List<SeerrProductionCountry>? = null,
    val spokenLanguages: List<SeerrSpokenLanguage>? = null,
    val seasons: List<SeerrSeason>? = null,
    val credits: SeerrCredits? = null,
    val externalIds: SeerrExternalIds? = null,
    val mediaInfo: SeerrMediaInfo? = null,
    val contentRatings: SeerrContentRatings? = null,
    val relatedVideos: List<SeerrRelatedVideo>? = null,
    val watchProviders: List<SeerrWatchProvider>? = null,
    val keywords: List<SeerrKeyword>? = null,
)

@Serializable
data class SeerrSeason(
    val id: Int,
    val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val airDate: String? = null,
    val episodeCount: Int? = null,
    val episodes: List<SeerrEpisode>? = null,
)

@Serializable
data class SeerrEpisode(
    val id: Int,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val stillPath: String? = null,
    val airDate: String? = null,
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val runtime: Int? = null,
)

// ============================================================================
// CREDITS MODELS
// ============================================================================

@Serializable
data class SeerrCredits(
    val cast: List<SeerrCastMember>? = null,
    val crew: List<SeerrCrewMember>? = null,
)

@Serializable
data class SeerrCastMember(
    val id: Int,
    val name: String,
    val character: String? = null,
    val profilePath: String? = null,
    val order: Int? = null,
    val creditId: String? = null,
)

@Serializable
data class SeerrCrewMember(
    val id: Int,
    val name: String,
    val job: String? = null,
    val department: String? = null,
    val profilePath: String? = null,
    val creditId: String? = null,
)

@Serializable
data class SeerrPerson(
    val id: Int,
    val name: String,
    val biography: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    val placeOfBirth: String? = null,
    val profilePath: String? = null,
    val knownForDepartment: String? = null,
    val alsoKnownAs: List<String>? = null,
    val gender: Int? = null,
    val popularity: Double? = null,
    val imdbId: String? = null,
    val homepage: String? = null,
    val combinedCredits: SeerrCombinedCredits? = null,
    val externalIds: SeerrExternalIds? = null,
)

@Serializable
data class SeerrCombinedCredits(
    val cast: List<SeerrPersonCastCredit>? = null,
    val crew: List<SeerrPersonCrewCredit>? = null,
)

@Serializable
data class SeerrPersonCastCredit(
    val id: Int,
    val mediaType: String,
    val title: String? = null,
    val name: String? = null,
    val character: String? = null,
    val posterPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Double? = null,
    val episodeCount: Int? = null,
)

@Serializable
data class SeerrPersonCrewCredit(
    val id: Int,
    val mediaType: String,
    val title: String? = null,
    val name: String? = null,
    val job: String? = null,
    val department: String? = null,
    val posterPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Double? = null,
)

// ============================================================================
// REQUEST MODELS
// ============================================================================

@Serializable
data class SeerrRequest(
    val id: Int,
    val status: Int, // 1=Pending, 2=Approved, 3=Declined
    val type: String, // "movie" or "tv"
    val is4k: Boolean = false,
    val serverId: Int? = null,
    val profileId: Int? = null,
    val rootFolder: String? = null,
    val languageProfileId: Int? = null,
    val tags: List<Int>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val requestedBy: SeerrUser? = null,
    val modifiedBy: SeerrUser? = null,
    val media: SeerrRequestMedia? = null,
    val seasons: List<SeerrRequestSeason>? = null,
)

@Serializable
data class SeerrRequestMedia(
    val id: Int,
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val imdbId: String? = null,
    val status: Int? = null,
    val status4k: Int? = null,
    val mediaType: String? = null,
)

@Serializable
data class SeerrRequestSeason(
    val id: Int,
    val seasonNumber: Int,
    val status: Int,
)

@Serializable
data class SeerrRequestResponse(
    val pageInfo: SeerrPageInfo? = null,
    val results: List<SeerrRequest>,
)

@Serializable
data class SeerrPageInfo(
    val page: Int? = null,
    val pages: Int? = null,
    val results: Int? = null,
)

// ============================================================================
// ISSUE MODELS
// ============================================================================

@Serializable
data class SeerrIssue(
    val id: Int,
    val issueType: Int, // 1=Video, 2=Audio, 3=Subtitle, 4=Other
    val status: Int, // 1=Open, 2=Resolved
    val problemSeason: Int? = null,
    val problemEpisode: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val createdBy: SeerrUser? = null,
    val media: SeerrIssueMedia? = null,
    val comments: List<SeerrIssueComment>? = null,
)

@Serializable
data class SeerrIssueMedia(
    val id: Int,
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val mediaType: String? = null,
    val status: Int? = null,
)

@Serializable
data class SeerrIssueComment(
    val id: Int,
    val message: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val user: SeerrUser? = null,
)

@Serializable
data class SeerrIssueResponse(
    val pageInfo: SeerrPageInfo? = null,
    val results: List<SeerrIssue>,
)

// ============================================================================
// USER MODELS
// ============================================================================

@Serializable
data class SeerrUser(
    val id: Int,
    val email: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatar: String? = null,
    val plexToken: String? = null,
    val plexId: Int? = null,
    val jellyfinUserId: String? = null,
    val jellyfinUsername: String? = null,
    val permissions: Int? = null,
    val userType: Int? = null, // 1=Plex, 2=Local, 3=Jellyfin, 4=Emby
    val createdAt: String? = null,
    val requestCount: Int? = null,
    val movieQuotaLimit: Int? = null,
    val movieQuotaDays: Int? = null,
    val tvQuotaLimit: Int? = null,
    val tvQuotaDays: Int? = null,
    val settings: SeerrUserSettings? = null,
)

@Serializable
data class SeerrUserSettings(
    val locale: String? = null,
    val region: String? = null,
    val originalLanguage: String? = null,
    val watchlistSyncMovies: Boolean? = null,
    val watchlistSyncTv: Boolean? = null,
)

@Serializable
data class SeerrQuota(
    val movie: SeerrQuotaInfo? = null,
    val tv: SeerrQuotaInfo? = null,
)

@Serializable
data class SeerrQuotaInfo(
    val limit: Int? = null,
    val days: Int? = null,
    val used: Int? = null,
    val remaining: Int? = null,
    val restricted: Boolean? = null,
)

// ============================================================================
// DISCOVER/SEARCH MODELS
// ============================================================================

@Serializable
data class SeerrDiscoverResponse(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<SeerrMedia>,
)

@Serializable
data class SeerrSearchResponse(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<SeerrSearchResult>,
)

@Serializable
data class SeerrSearchResult(
    val id: Int,
    val mediaType: String, // "movie", "tv", "person"
    val title: String? = null,
    val name: String? = null,
    val posterPath: String? = null,
    val profilePath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val overview: String? = null,
    val voteAverage: Double? = null,
    val popularity: Double? = null,
    val knownFor: List<SeerrMedia>? = null, // For person results
    val mediaInfo: SeerrMediaInfo? = null,
)

// ============================================================================
// RATINGS MODELS
// ============================================================================

@Serializable
data class SeerrRatings(
    val criticsRating: String? = null, // "Rotten" or "Fresh"
    val criticsScore: Int? = null,
    val audienceRating: String? = null, // "Spilled" or "Upright"
    val audienceScore: Int? = null,
    val imdbRating: Double? = null,
    val tmdbRating: Double? = null,
    val metacriticRating: Int? = null,
)

// ============================================================================
// SERVICE MODELS (Radarr/Sonarr)
// ============================================================================

@Serializable
data class SeerrRadarrSettings(
    val id: Int,
    val name: String,
    val hostname: String,
    val port: Int,
    val apiKey: String? = null,
    val useSsl: Boolean = false,
    val baseUrl: String? = null,
    val activeProfileId: Int? = null,
    val activeProfileName: String? = null,
    val activeDirectory: String? = null,
    val is4k: Boolean = false,
    val minimumAvailability: String? = null,
    val tags: List<Int>? = null,
    val isDefault: Boolean = false,
    val syncEnabled: Boolean = false,
    val preventSearch: Boolean = false,
)

@Serializable
data class SeerrSonarrSettings(
    val id: Int,
    val name: String,
    val hostname: String,
    val port: Int,
    val apiKey: String? = null,
    val useSsl: Boolean = false,
    val baseUrl: String? = null,
    val activeProfileId: Int? = null,
    val activeProfileName: String? = null,
    val activeDirectory: String? = null,
    val activeLanguageProfileId: Int? = null,
    val activeAnimeProfileId: Int? = null,
    val activeAnimeDirectory: String? = null,
    val activeAnimeLanguageProfileId: Int? = null,
    val is4k: Boolean = false,
    val enableSeasonFolders: Boolean = true,
    val tags: List<Int>? = null,
    val isDefault: Boolean = false,
    val syncEnabled: Boolean = false,
    val preventSearch: Boolean = false,
)

@Serializable
data class SeerrQualityProfile(
    val id: Int,
    val name: String,
)

@Serializable
data class SeerrRootFolder(
    val id: Int,
    val path: String,
    val freeSpace: Long? = null,
)

@Serializable
data class SeerrLanguageProfile(
    val id: Int,
    val name: String,
)

@Serializable
data class SeerrTag(
    val id: Int,
    val label: String,
)

// ============================================================================
// SUPPORTING MODELS
// ============================================================================

@Serializable
data class SeerrGenre(
    val id: Int,
    val name: String,
)

@Serializable
data class SeerrNetwork(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
    val originCountry: String? = null,
)

@Serializable
data class SeerrProductionCompany(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
    val originCountry: String? = null,
)

@Serializable
data class SeerrProductionCountry(
    val iso31661: String? = null,
    val name: String? = null,
)

@Serializable
data class SeerrSpokenLanguage(
    val englishName: String? = null,
    val iso6391: String? = null,
    val name: String? = null,
)

@Serializable
data class SeerrExternalIds(
    val imdbId: String? = null,
    val freebaseMid: String? = null,
    val freebaseId: String? = null,
    val tvdbId: Int? = null,
    val tvrageId: Int? = null,
    val facebookId: String? = null,
    val instagramId: String? = null,
    val twitterId: String? = null,
)

@Serializable
data class SeerrCollectionInfo(
    val id: Int,
    val name: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val overview: String? = null,
)

@Serializable
data class SeerrCollectionDetails(
    val id: Int,
    val name: String,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val parts: List<SeerrMedia>? = null,
)

@Serializable
data class SeerrRelatedVideo(
    val url: String,
    val key: String? = null,
    val name: String? = null,
    val size: Int? = null,
    val type: String? = null, // "Trailer", "Teaser", "Clip", etc.
    val site: String? = null, // "YouTube"
)

@Serializable
data class SeerrWatchProvider(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
)

@Serializable
data class SeerrContentRatings(
    val results: List<SeerrContentRating>? = null,
)

@Serializable
data class SeerrContentRating(
    val iso31661: String? = null,
    val rating: String? = null,
)

@Serializable
data class SeerrKeyword(
    val id: Int,
    val name: String,
)

// ============================================================================
// SLIDER/DISCOVER CONFIG MODELS
// ============================================================================

@Serializable
data class SeerrDiscoverSlider(
    val id: Int,
    val type: Int, // Slider type enum
    val title: String? = null,
    val isBuiltIn: Boolean? = null,
    val enabled: Boolean? = null,
    val data: String? = null, // JSON string for genre/keyword/company/network ID
)

@Serializable
data class SeerrDiscoverSliderResponse(
    val sliders: List<SeerrDiscoverSlider>,
)

// ============================================================================
// AUTHENTICATION MODELS
// ============================================================================

@Serializable
data class SeerrAuthResponse(
    val id: Int? = null,
    val email: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatar: String? = null,
    val permissions: Int? = null,
    val userType: Int? = null,
    val plexToken: String? = null,
    val jellyfinAuthToken: String? = null,
)

@Serializable
data class SeerrJellyfinLoginRequest(
    val username: String,
    val password: String,
    val hostname: String? = null,
)

@Serializable
data class SeerrLocalLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class SeerrServerInfo(
    val version: String? = null,
    val applicationTitle: String? = null,
    val applicationUrl: String? = null,
    val localLogin: Boolean? = null,
    val movie4kEnabled: Boolean? = null,
    val series4kEnabled: Boolean? = null,
    val region: String? = null,
    val originalLanguage: String? = null,
    val partialRequestsEnabled: Boolean? = null,
    val cacheImages: Boolean? = null,
    val vapidPublic: String? = null,
    val enablePushRegistration: Boolean? = null,
    val locale: String? = null,
    val emailEnabled: Boolean? = null,
    val newPlexLogin: Boolean? = null,
)

// ============================================================================
// ENUMS & CONSTANTS
// ============================================================================

object SeerrMediaStatus {
    const val UNKNOWN = 1
    const val PENDING = 2
    const val PROCESSING = 3
    const val PARTIALLY_AVAILABLE = 4
    const val AVAILABLE = 5
}

object SeerrRequestStatus {
    const val PENDING = 1
    const val APPROVED = 2
    const val DECLINED = 3
}

object SeerrIssueType {
    const val VIDEO = 1
    const val AUDIO = 2
    const val SUBTITLE = 3
    const val OTHER = 4
}

object SeerrIssueStatus {
    const val OPEN = 1
    const val RESOLVED = 2
}

object SeerrUserType {
    const val PLEX = 1
    const val LOCAL = 2
    const val JELLYFIN = 3
    const val EMBY = 4
}

object SeerrSliderType {
    const val TRENDING_MOVIES = 1
    const val POPULAR_MOVIES = 2
    const val UPCOMING_MOVIES = 3
    const val TRENDING_TV = 4
    const val POPULAR_TV = 5
    const val UPCOMING_TV = 6
    const val MOVIE_GENRES = 7
    const val TV_GENRES = 8
    const val MOVIE_STUDIOS = 9
    const val TV_NETWORKS = 10
    const val RECENTLY_ADDED = 11
    const val STREAMING_NOW_MOVIES = 12
    const val STREAMING_NOW_TV = 13
    const val MOVIE_KEYWORDS = 14
    const val TV_KEYWORDS = 15
    const val CUSTOM_MOVIE = 16
    const val CUSTOM_TV = 17
    const val WATCHLIST = 18
}
```

**Step 3: Commit models**

```bash
git add core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrModels.kt
git commit -m "feat(seerr): port comprehensive data models from SeerrTV

Includes: MediaInfo, Ratings, Issues, Service configs, Credits,
Sliders, Authentication, and all supporting models.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

### Task 1.2: Write SeerrClient from OpenAPI Spec

**Files:**
- Replace: `core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrClient.kt`
- Create: `core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrConfig.kt`

**Context:**
Write API client from scratch using:
- `references/seerr/seerr-api.yml` for endpoint paths and schemas
- `core/network/JellyfinClient.kt` for code patterns

**API Endpoints to implement (from OpenAPI spec):**
- Auth: `/auth/jellyfin`, `/auth/local`, `/auth/me`, `/auth/logout`
- Discover: `/discover/movies`, `/discover/tv`, `/discover/trending`, `/discover/movies/upcoming`
- Search: `/search`
- Details: `/movie/{id}`, `/tv/{id}`, `/movie/{id}/ratings`, `/tv/{id}/ratings`
- Requests: `GET/POST /request`, `/request/{id}`, `/request/{id}/approve`, `/request/{id}/decline`
- Issues: `GET/POST /issue`, `/issue/{id}`, `/issue/{id}/comment`
- Service: `/service/radarr`, `/service/sonarr`
- Settings: `/settings/public`, `/settings/discover`

**Step 1: Create SeerrConfig.kt**

```kotlin
package dev.jausc.myflix.core.seerr

import kotlinx.serialization.Serializable

@Serializable
data class SeerrConfig(
    val protocol: String = "https",
    val hostname: String = "",
    val port: Int? = null,
    val apiKey: String = "",
    val authType: AuthType = AuthType.API_KEY,
    val username: String = "",
    val password: String = "",
    val cloudflareEnabled: Boolean = false,
    val cfClientId: String = "",
    val cfClientSecret: String = "",
    val bypassSsl: Boolean = false,
) {
    enum class AuthType {
        API_KEY,
        LOCAL,
        JELLYFIN,
        PLEX_PIN,
    }

    val baseUrl: String
        get() = buildString {
            append(protocol)
            append("://")
            append(hostname)
            if (port != null) {
                append(":")
                append(port)
            }
        }

    val isConfigured: Boolean
        get() = hostname.isNotBlank() && (
            apiKey.isNotBlank() ||
            (authType == AuthType.LOCAL && username.isNotBlank()) ||
            (authType == AuthType.JELLYFIN && username.isNotBlank())
        )
}
```

**Step 2: Create comprehensive SeerrClient.kt**

This will be a large file (~1500 lines). Key sections:

```kotlin
package dev.jausc.myflix.core.seerr

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SeerrClient(
    private val configProvider: () -> SeerrConfig,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private var httpClient: HttpClient? = null
    private var sessionCookie: String? = null

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<SeerrUser?>(null)
    val currentUser: StateFlow<SeerrUser?> = _currentUser.asStateFlow()

    private val _serverInfo = MutableStateFlow<SeerrServerInfo?>(null)
    val serverInfo: StateFlow<SeerrServerInfo?> = _serverInfo.asStateFlow()

    private val config: SeerrConfig get() = configProvider()

    // ========================================================================
    // CLIENT INITIALIZATION
    // ========================================================================

    private fun getOrCreateClient(): HttpClient {
        val currentClient = httpClient
        if (currentClient != null) return currentClient

        val newClient = HttpClient(OkHttp) {
            engine {
                preconfigured = if (config.bypassSsl) {
                    createUnsafeOkHttpClient()
                } else {
                    OkHttpClient.Builder()
                        .followRedirects(true)
                        .build()
                }
                config {
                    // Rate limiting for TV devices
                    dispatcher.maxRequests = 8
                    dispatcher.maxRequestsPerHost = 4
                }
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }

            defaultRequest {
                url(config.baseUrl)
                contentType(ContentType.Application.Json)

                // API Key auth
                if (config.apiKey.isNotBlank()) {
                    header("X-Api-Key", config.apiKey)
                }

                // Session cookie
                sessionCookie?.let {
                    header("Cookie", it)
                }

                // Cloudflare Access
                if (config.cloudflareEnabled) {
                    header("CF-Access-Client-Id", config.cfClientId)
                    header("CF-Access-Client-Secret", config.cfClientSecret)
                }
            }
        }

        httpClient = newClient
        return newClient
    }

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(true)
            .build()
    }

    fun resetClient() {
        httpClient?.close()
        httpClient = null
        sessionCookie = null
        _isAuthenticated.value = false
        _currentUser.value = null
    }

    // ========================================================================
    // AUTHENTICATION
    // ========================================================================

    suspend fun testConnection(): Result<SeerrServerInfo> = runCatching {
        val client = getOrCreateClient()
        val response: SeerrServerInfo = client.get("/api/v1/settings/public").body()
        _serverInfo.value = response
        response
    }

    suspend fun loginWithJellyfin(username: String, password: String): Result<SeerrUser> = runCatching {
        val client = getOrCreateClient()
        val response = client.post("/api/v1/auth/jellyfin") {
            setBody(SeerrJellyfinLoginRequest(username, password))
        }

        // Extract session cookie
        response.headers.getAll("Set-Cookie")?.forEach { cookie ->
            if (cookie.contains("connect.sid")) {
                sessionCookie = cookie.substringBefore(";")
            }
        }

        val user: SeerrAuthResponse = response.body()
        val seerrUser = SeerrUser(
            id = user.id ?: 0,
            email = user.email,
            username = user.username,
            displayName = user.displayName,
            avatar = user.avatar,
            permissions = user.permissions,
            userType = user.userType,
        )

        _currentUser.value = seerrUser
        _isAuthenticated.value = true
        seerrUser
    }

    suspend fun loginWithLocal(email: String, password: String): Result<SeerrUser> = runCatching {
        val client = getOrCreateClient()
        val response = client.post("/api/v1/auth/local") {
            setBody(SeerrLocalLoginRequest(email, password))
        }

        response.headers.getAll("Set-Cookie")?.forEach { cookie ->
            if (cookie.contains("connect.sid")) {
                sessionCookie = cookie.substringBefore(";")
            }
        }

        val user: SeerrAuthResponse = response.body()
        val seerrUser = SeerrUser(
            id = user.id ?: 0,
            email = user.email,
            username = user.username,
            displayName = user.displayName,
            avatar = user.avatar,
            permissions = user.permissions,
            userType = user.userType,
        )

        _currentUser.value = seerrUser
        _isAuthenticated.value = true
        seerrUser
    }

    suspend fun getCurrentUser(): Result<SeerrUser> = runCatching {
        val client = getOrCreateClient()
        val user: SeerrUser = client.get("/api/v1/auth/me").body()
        _currentUser.value = user
        _isAuthenticated.value = true
        user
    }

    suspend fun getQuota(): Result<SeerrQuota> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/auth/me") {
            parameter("quota", true)
        }.body()
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/auth/logout")
        sessionCookie = null
        _isAuthenticated.value = false
        _currentUser.value = null
    }

    // ========================================================================
    // DISCOVER
    // ========================================================================

    suspend fun getDiscoverMovies(page: Int = 1): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/movies") {
            parameter("page", page)
        }.body()
    }

    suspend fun getDiscoverTv(page: Int = 1): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/tv") {
            parameter("page", page)
        }.body()
    }

    suspend fun getTrendingMovies(page: Int = 1): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/trending") {
            parameter("page", page)
        }.body()
    }

    suspend fun getUpcomingMovies(page: Int = 1): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/movies/upcoming") {
            parameter("page", page)
        }.body()
    }

    suspend fun getUpcomingTv(page: Int = 1): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/tv/upcoming") {
            parameter("page", page)
        }.body()
    }

    suspend fun getDiscoverByGenre(
        mediaType: String,
        genreId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/$mediaType/genre/$genreId") {
            parameter("page", page)
        }.body()
    }

    suspend fun getDiscoverByStudio(
        studioId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/movies/studio/$studioId") {
            parameter("page", page)
        }.body()
    }

    suspend fun getDiscoverByNetwork(
        networkId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/tv/network/$networkId") {
            parameter("page", page)
        }.body()
    }

    suspend fun getDiscoverByKeyword(
        mediaType: String,
        keywordId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/discover/$mediaType") {
            parameter("keywords", keywordId)
            parameter("page", page)
        }.body()
    }

    suspend fun getDiscoverSliders(): Result<List<SeerrDiscoverSlider>> = runCatching {
        val client = getOrCreateClient()
        val response: SeerrDiscoverSliderResponse = client.get("/api/v1/settings/discover").body()
        response.sliders.filter { it.enabled == true }
    }

    // ========================================================================
    // SEARCH
    // ========================================================================

    suspend fun search(
        query: String,
        page: Int = 1,
    ): Result<SeerrSearchResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/search") {
            parameter("query", query)
            parameter("page", page)
        }.body()
    }

    // ========================================================================
    // MEDIA DETAILS
    // ========================================================================

    suspend fun getMovieDetails(tmdbId: Int): Result<SeerrMovieDetails> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/movie/$tmdbId").body()
    }

    suspend fun getTvDetails(tmdbId: Int): Result<SeerrTvDetails> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/tv/$tmdbId").body()
    }

    suspend fun getSeasonDetails(
        tvId: Int,
        seasonNumber: Int,
    ): Result<SeerrSeason> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/tv/$tvId/season/$seasonNumber").body()
    }

    suspend fun getCollectionDetails(collectionId: Int): Result<SeerrCollectionDetails> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/collection/$collectionId").body()
    }

    suspend fun getPersonDetails(personId: Int): Result<SeerrPerson> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/person/$personId").body()
    }

    suspend fun getMovieRatings(tmdbId: Int): Result<SeerrRatings> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/movie/$tmdbId/ratings").body()
    }

    suspend fun getTvRatings(tmdbId: Int): Result<SeerrRatings> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/tv/$tmdbId/ratings").body()
    }

    suspend fun getSimilarMovies(
        tmdbId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/movie/$tmdbId/similar") {
            parameter("page", page)
        }.body()
    }

    suspend fun getSimilarTv(
        tmdbId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/tv/$tmdbId/similar") {
            parameter("page", page)
        }.body()
    }

    suspend fun getMovieRecommendations(
        tmdbId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/movie/$tmdbId/recommendations") {
            parameter("page", page)
        }.body()
    }

    suspend fun getTvRecommendations(
        tmdbId: Int,
        page: Int = 1,
    ): Result<SeerrDiscoverResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/tv/$tmdbId/recommendations") {
            parameter("page", page)
        }.body()
    }

    // ========================================================================
    // REQUESTS
    // ========================================================================

    suspend fun createMovieRequest(
        tmdbId: Int,
        is4k: Boolean = false,
        serverId: Int? = null,
        profileId: Int? = null,
        rootFolder: String? = null,
        tags: List<Int>? = null,
    ): Result<SeerrRequest> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/request") {
            setBody(mapOf(
                "mediaType" to "movie",
                "mediaId" to tmdbId,
                "is4k" to is4k,
                "serverId" to serverId,
                "profileId" to profileId,
                "rootFolder" to rootFolder,
                "tags" to tags,
            ).filterValues { it != null })
        }.body()
    }

    suspend fun createTvRequest(
        tmdbId: Int,
        is4k: Boolean = false,
        seasons: List<Int>? = null, // null = all seasons
        serverId: Int? = null,
        profileId: Int? = null,
        rootFolder: String? = null,
        languageProfileId: Int? = null,
        tags: List<Int>? = null,
    ): Result<SeerrRequest> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/request") {
            setBody(buildMap {
                put("mediaType", "tv")
                put("mediaId", tmdbId)
                put("is4k", is4k)
                if (seasons != null) {
                    put("seasons", seasons)
                }
                serverId?.let { put("serverId", it) }
                profileId?.let { put("profileId", it) }
                rootFolder?.let { put("rootFolder", it) }
                languageProfileId?.let { put("languageProfileId", it) }
                tags?.let { put("tags", it) }
            })
        }.body()
    }

    suspend fun getRequests(
        page: Int = 1,
        pageSize: Int = 20,
        filter: String? = null, // "all", "pending", "approved", "declined", "processing", "available"
        sort: String? = null, // "added", "modified"
        requestedBy: Int? = null,
    ): Result<SeerrRequestResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/request") {
            parameter("take", pageSize)
            parameter("skip", (page - 1) * pageSize)
            filter?.let { parameter("filter", it) }
            sort?.let { parameter("sort", it) }
            requestedBy?.let { parameter("requestedBy", it) }
        }.body()
    }

    suspend fun getRequest(requestId: Int): Result<SeerrRequest> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/request/$requestId").body()
    }

    suspend fun approveRequest(requestId: Int): Result<SeerrRequest> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/request/$requestId/approve").body()
    }

    suspend fun declineRequest(requestId: Int): Result<Unit> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/request/$requestId/decline")
    }

    suspend fun deleteRequest(requestId: Int): Result<Unit> = runCatching {
        val client = getOrCreateClient()
        client.delete("/api/v1/request/$requestId")
    }

    suspend fun retryRequest(requestId: Int): Result<SeerrRequest> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/request/$requestId/retry").body()
    }

    // ========================================================================
    // ISSUES
    // ========================================================================

    suspend fun getIssues(
        page: Int = 1,
        pageSize: Int = 20,
        filter: String? = null, // "all", "open", "resolved"
        sort: String? = null,
    ): Result<SeerrIssueResponse> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/issue") {
            parameter("take", pageSize)
            parameter("skip", (page - 1) * pageSize)
            filter?.let { parameter("filter", it) }
            sort?.let { parameter("sort", it) }
        }.body()
    }

    suspend fun createIssue(
        mediaType: String,
        mediaId: Int,
        issueType: Int, // 1=Video, 2=Audio, 3=Subtitle, 4=Other
        message: String,
        problemSeason: Int? = null,
        problemEpisode: Int? = null,
    ): Result<SeerrIssue> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/issue") {
            setBody(buildMap {
                put("mediaType", mediaType)
                put("mediaId", mediaId)
                put("issueType", issueType)
                put("message", message)
                problemSeason?.let { put("problemSeason", it) }
                problemEpisode?.let { put("problemEpisode", it) }
            })
        }.body()
    }

    suspend fun getIssue(issueId: Int): Result<SeerrIssue> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/issue/$issueId").body()
    }

    suspend fun addIssueComment(
        issueId: Int,
        message: String,
    ): Result<SeerrIssueComment> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/issue/$issueId/comment") {
            setBody(mapOf("message" to message))
        }.body()
    }

    suspend fun resolveIssue(issueId: Int): Result<SeerrIssue> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/issue/$issueId/resolve").body()
    }

    suspend fun reopenIssue(issueId: Int): Result<SeerrIssue> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/issue/$issueId/reopen").body()
    }

    suspend fun deleteIssue(issueId: Int): Result<Unit> = runCatching {
        val client = getOrCreateClient()
        client.delete("/api/v1/issue/$issueId")
    }

    // ========================================================================
    // GENRES & METADATA
    // ========================================================================

    suspend fun getMovieGenres(): Result<List<SeerrGenre>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/genres/movie").body()
    }

    suspend fun getTvGenres(): Result<List<SeerrGenre>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/genres/tv").body()
    }

    // ========================================================================
    // SERVICE CONFIGURATION (Radarr/Sonarr)
    // ========================================================================

    suspend fun getRadarrServers(): Result<List<SeerrRadarrSettings>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/service/radarr").body()
    }

    suspend fun getSonarrServers(): Result<List<SeerrSonarrSettings>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/service/sonarr").body()
    }

    suspend fun getRadarrProfiles(serverId: Int): Result<List<SeerrQualityProfile>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/service/radarr/$serverId/profiles").body()
    }

    suspend fun getSonarrProfiles(serverId: Int): Result<List<SeerrQualityProfile>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/service/sonarr/$serverId/profiles").body()
    }

    suspend fun getRadarrRootFolders(serverId: Int): Result<List<SeerrRootFolder>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/service/radarr/$serverId").body()
    }

    suspend fun getSonarrRootFolders(serverId: Int): Result<List<SeerrRootFolder>> = runCatching {
        val client = getOrCreateClient()
        client.get("/api/v1/service/sonarr/$serverId").body()
    }

    // ========================================================================
    // BLACKLIST
    // ========================================================================

    suspend fun addToBlacklist(
        mediaType: String,
        tmdbId: Int,
    ): Result<Unit> = runCatching {
        val client = getOrCreateClient()
        client.post("/api/v1/blacklist") {
            setBody(mapOf(
                "mediaType" to mediaType,
                "tmdbId" to tmdbId,
            ))
        }
    }

    suspend fun removeFromBlacklist(blacklistId: Int): Result<Unit> = runCatching {
        val client = getOrCreateClient()
        client.delete("/api/v1/blacklist/$blacklistId")
    }

    // ========================================================================
    // IMAGE URLS
    // ========================================================================

    companion object {
        private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p"

        fun posterUrl(path: String?, size: String = "w500"): String? {
            return path?.let { "$TMDB_IMAGE_BASE/$size$it" }
        }

        fun backdropUrl(path: String?, size: String = "w1280"): String? {
            return path?.let { "$TMDB_IMAGE_BASE/$size$it" }
        }

        fun profileUrl(path: String?, size: String = "w185"): String? {
            return path?.let { "$TMDB_IMAGE_BASE/$size$it" }
        }

        fun stillUrl(path: String?, size: String = "w300"): String? {
            return path?.let { "$TMDB_IMAGE_BASE/$size$it" }
        }

        fun logoUrl(path: String?, size: String = "w154"): String? {
            return path?.let { "$TMDB_IMAGE_BASE/$size$it" }
        }
    }
}
```

**Step 3: Commit API client**

```bash
git add core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrClient.kt
git add core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrConfig.kt
git commit -m "feat(seerr): port comprehensive API client from SeerrTV

Features: Multi-auth, Cloudflare support, SSL bypass, rate limiting,
Issues, Service configs, Blacklist, all discover/search endpoints.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

### Task 1.3: Create Seerr Repository Layer

**Files:**
- Create: `core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrRepository.kt`

**Context:**
Repository pattern provides caching, error handling, and clean interface for ViewModels.

**Step 1: Create repository**

```kotlin
package dev.jausc.myflix.core.seerr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeerrRepository @Inject constructor(
    private val client: SeerrClient,
) {
    // Cached data
    private val _movieGenres = MutableStateFlow<List<SeerrGenre>>(emptyList())
    val movieGenres: StateFlow<List<SeerrGenre>> = _movieGenres.asStateFlow()

    private val _tvGenres = MutableStateFlow<List<SeerrGenre>>(emptyList())
    val tvGenres: StateFlow<List<SeerrGenre>> = _tvGenres.asStateFlow()

    private val _radarrServers = MutableStateFlow<List<SeerrRadarrSettings>>(emptyList())
    val radarrServers: StateFlow<List<SeerrRadarrSettings>> = _radarrServers.asStateFlow()

    private val _sonarrServers = MutableStateFlow<List<SeerrSonarrSettings>>(emptyList())
    val sonarrServers: StateFlow<List<SeerrSonarrSettings>> = _sonarrServers.asStateFlow()

    private val _discoverSliders = MutableStateFlow<List<SeerrDiscoverSlider>>(emptyList())
    val discoverSliders: StateFlow<List<SeerrDiscoverSlider>> = _discoverSliders.asStateFlow()

    // Delegated auth state
    val isAuthenticated: StateFlow<Boolean> = client.isAuthenticated
    val currentUser: StateFlow<SeerrUser?> = client.currentUser
    val serverInfo: StateFlow<SeerrServerInfo?> = client.serverInfo

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    suspend fun initialize(): Result<Unit> = runCatching {
        // Test connection and load initial data
        client.testConnection().getOrThrow()

        // Try to get current user (validates auth)
        client.getCurrentUser().onSuccess { user ->
            // Load genres and service configs if authenticated
            loadGenres()
            loadServiceConfigs()
            loadDiscoverSliders()
        }
    }

    private suspend fun loadGenres() {
        client.getMovieGenres().onSuccess { _movieGenres.value = it }
        client.getTvGenres().onSuccess { _tvGenres.value = it }
    }

    private suspend fun loadServiceConfigs() {
        client.getRadarrServers().onSuccess { _radarrServers.value = it }
        client.getSonarrServers().onSuccess { _sonarrServers.value = it }
    }

    private suspend fun loadDiscoverSliders() {
        client.getDiscoverSliders().onSuccess { _discoverSliders.value = it }
    }

    // ========================================================================
    // AUTHENTICATION
    // ========================================================================

    suspend fun loginWithJellyfin(username: String, password: String): Result<SeerrUser> {
        val result = client.loginWithJellyfin(username, password)
        if (result.isSuccess) {
            loadGenres()
            loadServiceConfigs()
            loadDiscoverSliders()
        }
        return result
    }

    suspend fun loginWithLocal(email: String, password: String): Result<SeerrUser> {
        val result = client.loginWithLocal(email, password)
        if (result.isSuccess) {
            loadGenres()
            loadServiceConfigs()
            loadDiscoverSliders()
        }
        return result
    }

    suspend fun logout(): Result<Unit> {
        val result = client.logout()
        _movieGenres.value = emptyList()
        _tvGenres.value = emptyList()
        _radarrServers.value = emptyList()
        _sonarrServers.value = emptyList()
        _discoverSliders.value = emptyList()
        return result
    }

    suspend fun getQuota(): Result<SeerrQuota> = client.getQuota()

    // ========================================================================
    // DISCOVER
    // ========================================================================

    suspend fun getDiscoverMovies(page: Int = 1) = client.getDiscoverMovies(page)
    suspend fun getDiscoverTv(page: Int = 1) = client.getDiscoverTv(page)
    suspend fun getTrendingMovies(page: Int = 1) = client.getTrendingMovies(page)
    suspend fun getUpcomingMovies(page: Int = 1) = client.getUpcomingMovies(page)
    suspend fun getUpcomingTv(page: Int = 1) = client.getUpcomingTv(page)

    suspend fun getDiscoverByGenre(
        mediaType: String,
        genreId: Int,
        page: Int = 1,
    ) = client.getDiscoverByGenre(mediaType, genreId, page)

    suspend fun getDiscoverByStudio(studioId: Int, page: Int = 1) =
        client.getDiscoverByStudio(studioId, page)

    suspend fun getDiscoverByNetwork(networkId: Int, page: Int = 1) =
        client.getDiscoverByNetwork(networkId, page)

    suspend fun getDiscoverByKeyword(mediaType: String, keywordId: Int, page: Int = 1) =
        client.getDiscoverByKeyword(mediaType, keywordId, page)

    // ========================================================================
    // SEARCH
    // ========================================================================

    suspend fun search(query: String, page: Int = 1) = client.search(query, page)

    // ========================================================================
    // MEDIA DETAILS
    // ========================================================================

    suspend fun getMovieDetails(tmdbId: Int) = client.getMovieDetails(tmdbId)
    suspend fun getTvDetails(tmdbId: Int) = client.getTvDetails(tmdbId)
    suspend fun getSeasonDetails(tvId: Int, seasonNumber: Int) =
        client.getSeasonDetails(tvId, seasonNumber)
    suspend fun getCollectionDetails(collectionId: Int) = client.getCollectionDetails(collectionId)
    suspend fun getPersonDetails(personId: Int) = client.getPersonDetails(personId)

    suspend fun getMovieRatings(tmdbId: Int) = client.getMovieRatings(tmdbId)
    suspend fun getTvRatings(tmdbId: Int) = client.getTvRatings(tmdbId)

    suspend fun getSimilarMovies(tmdbId: Int, page: Int = 1) =
        client.getSimilarMovies(tmdbId, page)
    suspend fun getSimilarTv(tmdbId: Int, page: Int = 1) =
        client.getSimilarTv(tmdbId, page)

    suspend fun getMovieRecommendations(tmdbId: Int, page: Int = 1) =
        client.getMovieRecommendations(tmdbId, page)
    suspend fun getTvRecommendations(tmdbId: Int, page: Int = 1) =
        client.getTvRecommendations(tmdbId, page)

    // ========================================================================
    // REQUESTS
    // ========================================================================

    suspend fun createMovieRequest(
        tmdbId: Int,
        is4k: Boolean = false,
        serverId: Int? = null,
        profileId: Int? = null,
        rootFolder: String? = null,
        tags: List<Int>? = null,
    ) = client.createMovieRequest(tmdbId, is4k, serverId, profileId, rootFolder, tags)

    suspend fun createTvRequest(
        tmdbId: Int,
        is4k: Boolean = false,
        seasons: List<Int>? = null,
        serverId: Int? = null,
        profileId: Int? = null,
        rootFolder: String? = null,
        languageProfileId: Int? = null,
        tags: List<Int>? = null,
    ) = client.createTvRequest(tmdbId, is4k, seasons, serverId, profileId, rootFolder, languageProfileId, tags)

    suspend fun getRequests(
        page: Int = 1,
        pageSize: Int = 20,
        filter: String? = null,
        sort: String? = null,
        requestedBy: Int? = null,
    ) = client.getRequests(page, pageSize, filter, sort, requestedBy)

    suspend fun getRequest(requestId: Int) = client.getRequest(requestId)
    suspend fun approveRequest(requestId: Int) = client.approveRequest(requestId)
    suspend fun declineRequest(requestId: Int) = client.declineRequest(requestId)
    suspend fun deleteRequest(requestId: Int) = client.deleteRequest(requestId)
    suspend fun retryRequest(requestId: Int) = client.retryRequest(requestId)

    // ========================================================================
    // ISSUES
    // ========================================================================

    suspend fun getIssues(
        page: Int = 1,
        pageSize: Int = 20,
        filter: String? = null,
        sort: String? = null,
    ) = client.getIssues(page, pageSize, filter, sort)

    suspend fun createIssue(
        mediaType: String,
        mediaId: Int,
        issueType: Int,
        message: String,
        problemSeason: Int? = null,
        problemEpisode: Int? = null,
    ) = client.createIssue(mediaType, mediaId, issueType, message, problemSeason, problemEpisode)

    suspend fun getIssue(issueId: Int) = client.getIssue(issueId)
    suspend fun addIssueComment(issueId: Int, message: String) =
        client.addIssueComment(issueId, message)
    suspend fun resolveIssue(issueId: Int) = client.resolveIssue(issueId)
    suspend fun reopenIssue(issueId: Int) = client.reopenIssue(issueId)
    suspend fun deleteIssue(issueId: Int) = client.deleteIssue(issueId)

    // ========================================================================
    // SERVICE CONFIGS
    // ========================================================================

    suspend fun getRadarrProfiles(serverId: Int) = client.getRadarrProfiles(serverId)
    suspend fun getSonarrProfiles(serverId: Int) = client.getSonarrProfiles(serverId)
    suspend fun getRadarrRootFolders(serverId: Int) = client.getRadarrRootFolders(serverId)
    suspend fun getSonarrRootFolders(serverId: Int) = client.getSonarrRootFolders(serverId)

    // ========================================================================
    // BLACKLIST
    // ========================================================================

    suspend fun addToBlacklist(mediaType: String, tmdbId: Int) =
        client.addToBlacklist(mediaType, tmdbId)
    suspend fun removeFromBlacklist(blacklistId: Int) =
        client.removeFromBlacklist(blacklistId)
}
```

**Step 2: Commit repository**

```bash
git add core/seerr/src/main/java/dev/jausc/myflix/core/seerr/SeerrRepository.kt
git commit -m "feat(seerr): add repository layer with caching

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

### Task 1.4: Update Hilt DI Module

**Files:**
- Modify: `core/seerr/src/main/java/dev/jausc/myflix/core/seerr/di/SeerrModule.kt`

**Step 1: Update DI module**

```kotlin
package dev.jausc.myflix.core.seerr.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrConfig
import dev.jausc.myflix.core.seerr.SeerrRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SeerrModule {

    @Provides
    @Singleton
    fun provideSeerrConfigStore(
        @ApplicationContext context: Context,
    ): SeerrConfigStore {
        return SeerrConfigStore(context)
    }

    @Provides
    @Singleton
    fun provideSeerrClient(
        configStore: SeerrConfigStore,
    ): SeerrClient {
        return SeerrClient(configProvider = { configStore.config })
    }

    @Provides
    @Singleton
    fun provideSeerrRepository(
        client: SeerrClient,
    ): SeerrRepository {
        return SeerrRepository(client)
    }
}

class SeerrConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("seerr_config", Context.MODE_PRIVATE)

    var config: SeerrConfig
        get() = SeerrConfig(
            protocol = prefs.getString("protocol", "https") ?: "https",
            hostname = prefs.getString("hostname", "") ?: "",
            port = prefs.getInt("port", -1).takeIf { it != -1 },
            apiKey = prefs.getString("apiKey", "") ?: "",
            authType = SeerrConfig.AuthType.entries.getOrElse(
                prefs.getInt("authType", 0)
            ) { SeerrConfig.AuthType.API_KEY },
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            cloudflareEnabled = prefs.getBoolean("cloudflareEnabled", false),
            cfClientId = prefs.getString("cfClientId", "") ?: "",
            cfClientSecret = prefs.getString("cfClientSecret", "") ?: "",
            bypassSsl = prefs.getBoolean("bypassSsl", false),
        )
        set(value) {
            prefs.edit().apply {
                putString("protocol", value.protocol)
                putString("hostname", value.hostname)
                putInt("port", value.port ?: -1)
                putString("apiKey", value.apiKey)
                putInt("authType", value.authType.ordinal)
                putString("username", value.username)
                putString("password", value.password)
                putBoolean("cloudflareEnabled", value.cloudflareEnabled)
                putString("cfClientId", value.cfClientId)
                putString("cfClientSecret", value.cfClientSecret)
                putBoolean("bypassSsl", value.bypassSsl)
                apply()
            }
        }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
```

**Step 2: Commit DI module**

```bash
git add core/seerr/src/main/java/dev/jausc/myflix/core/seerr/di/SeerrModule.kt
git commit -m "feat(seerr): update Hilt DI module with new client and repository

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Phase 2: ViewModels

### Task 2.1: Create SeerrHomeViewModel

**Files:**
- Replace: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrHomeViewModel.kt`

**Step 1: Create comprehensive home ViewModel**

```kotlin
package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jausc.myflix.core.seerr.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeerrHomeViewModel @Inject constructor(
    private val repository: SeerrRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeerrHomeUiState())
    val uiState: StateFlow<SeerrHomeUiState> = _uiState.asStateFlow()

    val isAuthenticated = repository.isAuthenticated
    val currentUser = repository.currentUser
    val discoverSliders = repository.discoverSliders
    val movieGenres = repository.movieGenres
    val tvGenres = repository.tvGenres

    init {
        viewModelScope.launch {
            repository.discoverSliders.collect { sliders ->
                if (sliders.isNotEmpty()) {
                    loadDiscoverRows(sliders)
                }
            }
        }
    }

    fun initialize() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.initialize()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
        }
    }

    private fun loadDiscoverRows(sliders: List<SeerrDiscoverSlider>) {
        viewModelScope.launch {
            val rows = mutableListOf<SeerrDiscoverRow>()

            for (slider in sliders) {
                val row = loadSliderContent(slider)
                if (row != null && row.items.isNotEmpty()) {
                    rows.add(row)
                }
            }

            _uiState.update { it.copy(discoverRows = rows) }
        }
    }

    private suspend fun loadSliderContent(slider: SeerrDiscoverSlider): SeerrDiscoverRow? {
        val result = when (slider.type) {
            SeerrSliderType.TRENDING_MOVIES -> {
                repository.getTrendingMovies().map {
                    SeerrDiscoverRow(
                        id = "trending_movies",
                        title = slider.title ?: "Trending Movies",
                        type = RowType.TRENDING_MOVIES,
                        items = it.results,
                    )
                }
            }
            SeerrSliderType.POPULAR_MOVIES -> {
                repository.getDiscoverMovies().map {
                    SeerrDiscoverRow(
                        id = "popular_movies",
                        title = slider.title ?: "Popular Movies",
                        type = RowType.POPULAR_MOVIES,
                        items = it.results,
                    )
                }
            }
            SeerrSliderType.UPCOMING_MOVIES -> {
                repository.getUpcomingMovies().map {
                    SeerrDiscoverRow(
                        id = "upcoming_movies",
                        title = slider.title ?: "Upcoming Movies",
                        type = RowType.UPCOMING_MOVIES,
                        items = it.results,
                    )
                }
            }
            SeerrSliderType.TRENDING_TV -> {
                repository.getDiscoverTv().map {
                    SeerrDiscoverRow(
                        id = "trending_tv",
                        title = slider.title ?: "Trending TV",
                        type = RowType.TRENDING_TV,
                        items = it.results,
                    )
                }
            }
            SeerrSliderType.POPULAR_TV -> {
                repository.getDiscoverTv().map {
                    SeerrDiscoverRow(
                        id = "popular_tv",
                        title = slider.title ?: "Popular TV",
                        type = RowType.POPULAR_TV,
                        items = it.results,
                    )
                }
            }
            SeerrSliderType.UPCOMING_TV -> {
                repository.getUpcomingTv().map {
                    SeerrDiscoverRow(
                        id = "upcoming_tv",
                        title = slider.title ?: "Upcoming TV",
                        type = RowType.UPCOMING_TV,
                        items = it.results,
                    )
                }
            }
            SeerrSliderType.MOVIE_GENRES -> {
                // Genres are handled separately as navigation items
                return null
            }
            SeerrSliderType.TV_GENRES -> {
                return null
            }
            else -> return null
        }

        return result.getOrNull()
    }

    fun loadMoreForRow(rowId: String, page: Int) {
        viewModelScope.launch {
            val currentRows = _uiState.value.discoverRows.toMutableList()
            val rowIndex = currentRows.indexOfFirst { it.id == rowId }
            if (rowIndex == -1) return@launch

            val row = currentRows[rowIndex]
            val result = when (row.type) {
                RowType.TRENDING_MOVIES -> repository.getTrendingMovies(page)
                RowType.POPULAR_MOVIES -> repository.getDiscoverMovies(page)
                RowType.UPCOMING_MOVIES -> repository.getUpcomingMovies(page)
                RowType.TRENDING_TV,
                RowType.POPULAR_TV -> repository.getDiscoverTv(page)
                RowType.UPCOMING_TV -> repository.getUpcomingTv(page)
                else -> return@launch
            }

            result.onSuccess { response ->
                val updatedItems = row.items + response.results
                currentRows[rowIndex] = row.copy(
                    items = updatedItems,
                    currentPage = page,
                    hasMore = page < response.totalPages,
                )
                _uiState.update { it.copy(discoverRows = currentRows) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            repository.initialize()
                .onSuccess {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
        }
    }
}

data class SeerrHomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val discoverRows: List<SeerrDiscoverRow> = emptyList(),
)

data class SeerrDiscoverRow(
    val id: String,
    val title: String,
    val type: RowType,
    val items: List<SeerrMedia>,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
)

enum class RowType {
    TRENDING_MOVIES,
    POPULAR_MOVIES,
    UPCOMING_MOVIES,
    TRENDING_TV,
    POPULAR_TV,
    UPCOMING_TV,
    GENRE_MOVIES,
    GENRE_TV,
    STUDIO,
    NETWORK,
    KEYWORD,
}
```

**Step 2: Commit ViewModel**

```bash
git add core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrHomeViewModel.kt
git commit -m "feat(seerr): add comprehensive SeerrHomeViewModel

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

### Task 2.2: Create SeerrDetailViewModel

**Files:**
- Create: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrDetailViewModel.kt`

**Step 1: Create detail ViewModel**

```kotlin
package dev.jausc.myflix.core.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jausc.myflix.core.seerr.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeerrDetailViewModel @Inject constructor(
    private val repository: SeerrRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tmdbId: Int = savedStateHandle["tmdbId"] ?: 0
    private val mediaType: String = savedStateHandle["mediaType"] ?: "movie"

    private val _uiState = MutableStateFlow(SeerrDetailUiState())
    val uiState: StateFlow<SeerrDetailUiState> = _uiState.asStateFlow()

    val radarrServers = repository.radarrServers
    val sonarrServers = repository.sonarrServers

    init {
        if (tmdbId > 0) {
            loadDetails()
        }
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (mediaType == "movie") {
                loadMovieDetails()
            } else {
                loadTvDetails()
            }
        }
    }

    private suspend fun loadMovieDetails() {
        repository.getMovieDetails(tmdbId)
            .onSuccess { movie ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        movieDetails = movie,
                        mediaInfo = movie.mediaInfo,
                    )
                }

                // Load ratings in parallel
                loadMovieRatings()
                loadSimilarMovies()
                loadMovieRecommendations()
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.message)
                }
            }
    }

    private suspend fun loadTvDetails() {
        repository.getTvDetails(tmdbId)
            .onSuccess { tv ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tvDetails = tv,
                        mediaInfo = tv.mediaInfo,
                    )
                }

                loadTvRatings()
                loadSimilarTv()
                loadTvRecommendations()
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.message)
                }
            }
    }

    private fun loadMovieRatings() {
        viewModelScope.launch {
            repository.getMovieRatings(tmdbId)
                .onSuccess { ratings ->
                    _uiState.update { it.copy(ratings = ratings) }
                }
        }
    }

    private fun loadTvRatings() {
        viewModelScope.launch {
            repository.getTvRatings(tmdbId)
                .onSuccess { ratings ->
                    _uiState.update { it.copy(ratings = ratings) }
                }
        }
    }

    private fun loadSimilarMovies() {
        viewModelScope.launch {
            repository.getSimilarMovies(tmdbId)
                .onSuccess { response ->
                    _uiState.update { it.copy(similar = response.results) }
                }
        }
    }

    private fun loadSimilarTv() {
        viewModelScope.launch {
            repository.getSimilarTv(tmdbId)
                .onSuccess { response ->
                    _uiState.update { it.copy(similar = response.results) }
                }
        }
    }

    private fun loadMovieRecommendations() {
        viewModelScope.launch {
            repository.getMovieRecommendations(tmdbId)
                .onSuccess { response ->
                    _uiState.update { it.copy(recommendations = response.results) }
                }
        }
    }

    private fun loadTvRecommendations() {
        viewModelScope.launch {
            repository.getTvRecommendations(tmdbId)
                .onSuccess { response ->
                    _uiState.update { it.copy(recommendations = response.results) }
                }
        }
    }

    fun loadSeason(seasonNumber: Int) {
        viewModelScope.launch {
            repository.getSeasonDetails(tmdbId, seasonNumber)
                .onSuccess { season ->
                    val currentSeasons = _uiState.value.loadedSeasons.toMutableMap()
                    currentSeasons[seasonNumber] = season
                    _uiState.update { it.copy(loadedSeasons = currentSeasons) }
                }
        }
    }

    // ========================================================================
    // REQUEST ACTIONS
    // ========================================================================

    fun requestMovie(
        is4k: Boolean = false,
        serverId: Int? = null,
        profileId: Int? = null,
        rootFolder: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true) }

            repository.createMovieRequest(tmdbId, is4k, serverId, profileId, rootFolder)
                .onSuccess { request ->
                    // Reload details to get updated mediaInfo
                    loadDetails()
                    _uiState.update { it.copy(isRequesting = false, lastRequest = request) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isRequesting = false, requestError = error.message)
                    }
                }
        }
    }

    fun requestTv(
        is4k: Boolean = false,
        seasons: List<Int>? = null,
        serverId: Int? = null,
        profileId: Int? = null,
        rootFolder: String? = null,
        languageProfileId: Int? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true) }

            repository.createTvRequest(tmdbId, is4k, seasons, serverId, profileId, rootFolder, languageProfileId)
                .onSuccess { request ->
                    loadDetails()
                    _uiState.update { it.copy(isRequesting = false, lastRequest = request) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isRequesting = false, requestError = error.message)
                    }
                }
        }
    }

    fun clearRequestError() {
        _uiState.update { it.copy(requestError = null) }
    }

    // ========================================================================
    // ISSUE ACTIONS
    // ========================================================================

    fun reportIssue(
        issueType: Int,
        message: String,
        problemSeason: Int? = null,
        problemEpisode: Int? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReportingIssue = true) }

            repository.createIssue(mediaType, tmdbId, issueType, message, problemSeason, problemEpisode)
                .onSuccess { issue ->
                    loadDetails()
                    _uiState.update {
                        it.copy(isReportingIssue = false, lastIssue = issue)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isReportingIssue = false, issueError = error.message)
                    }
                }
        }
    }

    fun clearIssueError() {
        _uiState.update { it.copy(issueError = null) }
    }
}

data class SeerrDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val movieDetails: SeerrMovieDetails? = null,
    val tvDetails: SeerrTvDetails? = null,
    val mediaInfo: SeerrMediaInfo? = null,
    val ratings: SeerrRatings? = null,
    val similar: List<SeerrMedia> = emptyList(),
    val recommendations: List<SeerrMedia> = emptyList(),
    val loadedSeasons: Map<Int, SeerrSeason> = emptyMap(),
    val isRequesting: Boolean = false,
    val lastRequest: SeerrRequest? = null,
    val requestError: String? = null,
    val isReportingIssue: Boolean = false,
    val lastIssue: SeerrIssue? = null,
    val issueError: String? = null,
)
```

**Step 2: Commit detail ViewModel**

```bash
git add core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrDetailViewModel.kt
git commit -m "feat(seerr): add SeerrDetailViewModel with request and issue actions

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

### Task 2.3: Create Additional ViewModels

**Files:**
- Create: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrSearchViewModel.kt`
- Create: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrRequestsViewModel.kt`
- Create: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrIssuesViewModel.kt`
- Create: `core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/SeerrSetupViewModel.kt`

These follow similar patterns to above. Each will be ~100-200 lines handling their specific domain concerns.

**Commit:**

```bash
git add core/viewmodel/src/main/java/dev/jausc/myflix/core/viewmodel/Seerr*.kt
git commit -m "feat(seerr): add Search, Requests, Issues, and Setup ViewModels

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Phase 3: TV App Screens (MyFlix UI/UX)

This phase adapts SeerrTV screens to MyFlix design patterns:
- TvColors instead of SeerrTV colors
- Focus halo effect (blur-based glow)
- NavRail integration with exit focus restoration
- DialogPopup pattern for context menus
- Tab focus restoration pattern
- CardSizes dimensions (110dp portrait, 210dp landscape)

### Task 3.1: Create SeerrHomeScreen

**Files:**
- Replace: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/seerr/SeerrHomeScreen.kt`

**Context:**
Adapts SeerrTV's MainScreen to MyFlix patterns:
- Hero section with featured content
- Horizontal carousels for discover rows
- Genre/Studio/Network navigation rows
- Focus management with LocalExitFocusState

**Step 1: Create SeerrHomeScreen**

```kotlin
package dev.jausc.myflix.tv.ui.screens.seerr

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.viewmodel.SeerrDiscoverRow
import dev.jausc.myflix.core.viewmodel.SeerrHomeViewModel
import dev.jausc.myflix.tv.ui.components.*
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.LocalExitFocusState
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry

@Composable
fun SeerrHomeScreen(
    onNavigateToDetail: (tmdbId: Int, mediaType: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBrowse: (mediaType: String, genreId: Int?, title: String) -> Unit,
    viewModel: SeerrHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryFocusRequester = remember { FocusRequester() }
    val updateExitFocus = rememberExitFocusRegistry(primaryFocusRequester)

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        when {
            uiState.isLoading -> {
                TvLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            uiState.error != null -> {
                SeerrErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.initialize() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                SeerrHomeContent(
                    discoverRows = uiState.discoverRows,
                    primaryFocusRequester = primaryFocusRequester,
                    updateExitFocus = updateExitFocus,
                    onNavigateToDetail = onNavigateToDetail,
                    onLoadMore = { rowId, page -> viewModel.loadMoreForRow(rowId, page) },
                )
            }
        }
    }
}

@Composable
private fun SeerrHomeContent(
    discoverRows: List<SeerrDiscoverRow>,
    primaryFocusRequester: FocusRequester,
    updateExitFocus: (FocusRequester) -> Unit,
    onNavigateToDetail: (tmdbId: Int, mediaType: String) -> Unit,
    onLoadMore: (rowId: String, page: Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Featured hero section (first row items)
        if (discoverRows.isNotEmpty() && discoverRows.first().items.isNotEmpty()) {
            item {
                SeerrHeroSection(
                    items = discoverRows.first().items.take(5),
                    onItemClick = { media ->
                        onNavigateToDetail(media.id, media.mediaType)
                    },
                )
            }
        }

        // Discover rows
        items(
            items = discoverRows,
            key = { it.id },
        ) { row ->
            SeerrMediaRow(
                title = row.title,
                items = row.items,
                primaryFocusRequester = if (row == discoverRows.firstOrNull()) {
                    primaryFocusRequester
                } else null,
                updateExitFocus = updateExitFocus,
                onItemClick = { media ->
                    onNavigateToDetail(media.id, media.mediaType)
                },
                onEndReached = {
                    if (row.hasMore) {
                        onLoadMore(row.id, row.currentPage + 1)
                    }
                },
            )
        }
    }
}

@Composable
private fun SeerrMediaRow(
    title: String,
    items: List<SeerrMedia>,
    primaryFocusRequester: FocusRequester?,
    updateExitFocus: (FocusRequester) -> Unit,
    onItemClick: (SeerrMedia) -> Unit,
    onEndReached: () -> Unit,
) {
    val rowState = rememberLazyListState()

    // Load more when near end
    LaunchedEffect(rowState.firstVisibleItemIndex) {
        val lastVisibleIndex = rowState.firstVisibleItemIndex + rowState.layoutInfo.visibleItemsInfo.size
        if (lastVisibleIndex >= items.size - 3) {
            onEndReached()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Row title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 48.dp),
        )

        // Media cards row
        LazyRow(
            state = rowState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = items,
                key = { "${it.mediaType}_${it.id}" },
            ) { media ->
                val focusRequester = remember { FocusRequester() }
                val isFirst = items.indexOf(media) == 0

                SeerrMediaCard(
                    media = media,
                    modifier = Modifier
                        .focusRequester(
                            if (isFirst && primaryFocusRequester != null) {
                                primaryFocusRequester
                            } else {
                                focusRequester
                            }
                        )
                        .onFocusChanged { state ->
                            if (state.hasFocus) {
                                updateExitFocus(
                                    if (isFirst && primaryFocusRequester != null) {
                                        primaryFocusRequester
                                    } else {
                                        focusRequester
                                    }
                                )
                            }
                        },
                    onClick = { onItemClick(media) },
                )
            }
        }
    }
}

@Composable
fun SeerrMediaCard(
    media: SeerrMedia,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "haloAlpha",
    )

    Box(modifier = modifier) {
        // Halo glow layer
        if (haloAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(12.dp)
                    .background(
                        TvColors.BluePrimary.copy(alpha = haloAlpha),
                        RoundedCornerShape(12.dp),
                    ),
            )
        }

        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(CardSizes.PortraitWidth)
                .aspectRatio(2f / 3f)
                .onFocusChanged { isFocused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(12.dp),
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.Surface,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvColors.BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                ),
            ),
        ) {
            Box {
                // Poster image
                AsyncImage(
                    model = SeerrClient.posterUrl(media.posterPath),
                    contentDescription = media.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Status badge
                media.mediaInfo?.let { info ->
                    SeerrStatusBadge(
                        status = info.status ?: 1,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                }
            }
        }
    }

    // Title below card
    if (isFocused) {
        Text(
            text = media.displayTitle,
            style = MaterialTheme.typography.labelMedium,
            color = TvColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier
                .width(CardSizes.PortraitWidth)
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun SeerrStatusBadge(
    status: Int,
    modifier: Modifier = Modifier,
) {
    val (color, text) = when (status) {
        SeerrMediaStatus.AVAILABLE -> TvColors.Success to "Available"
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> Color(0xFFF59E0B) to "Partial"
        SeerrMediaStatus.PENDING -> TvColors.BlueAccent to "Pending"
        SeerrMediaStatus.PROCESSING -> TvColors.BlueAccent to "Processing"
        else -> return // Don't show badge for unknown status
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SeerrErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Unable to load content",
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextSecondary,
        )
        TvTextButton(
            text = "Retry",
            onClick = onRetry,
            isPrimary = true,
        )
    }
}

private object CardSizes {
    val PortraitWidth = 110.dp
    val LandscapeWidth = 210.dp
}
```

**Step 2: Commit SeerrHomeScreen**

```bash
git add app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/seerr/SeerrHomeScreen.kt
git commit -m "feat(seerr-tv): add SeerrHomeScreen with MyFlix UI patterns

- Focus halo effect
- Exit focus restoration
- TvColors theming
- Standard card sizes

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

### Task 3.2: Create SeerrDetailScreen

**Files:**
- Replace: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/seerr/SeerrDetailScreen.kt`

**Context:**
Full detail screen with:
- Hero backdrop
- Media info (title, rating, overview)
- Cast/crew row
- Request modal with season selection
- Issue reporting modal
- Similar/recommendations rows

This will be ~600 lines following MyFlix patterns.

---

### Task 3.3: Create SeerrSearchScreen

**Files:**
- Replace: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/seerr/SeerrSearchScreen.kt`

---

### Task 3.4: Create SeerrRequestsScreen

**Files:**
- Replace: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/seerr/SeerrRequestsScreen.kt`

---

### Task 3.5: Create SeerrSetupScreen

**Files:**
- Replace: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/seerr/SeerrSetupScreen.kt`

---

### Task 3.6: Create Shared Seerr Components

**Files:**
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/seerr/SeerrHeroSection.kt`
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/seerr/SeerrRequestModal.kt`
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/seerr/SeerrIssueModal.kt`
- Create: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/seerr/SeerrFilterBar.kt`

---

## Phase 4: Navigation Integration

### Task 4.1: Update Navigation Graph

**Files:**
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/navigation/TvNavGraph.kt`

Add Seerr routes:
- `seerr/home`
- `seerr/detail/{tmdbId}/{mediaType}`
- `seerr/search`
- `seerr/requests`
- `seerr/setup`
- `seerr/browse/{mediaType}?genreId={genreId}&title={title}`
- `seerr/person/{personId}`
- `seerr/collection/{collectionId}`

---

### Task 4.2: Add Seerr to NavRail

**Files:**
- Modify: `app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/navrail/NavigationRail.kt`

Add "Discover" nav item that navigates to Seerr home.

---

## Phase 5: Testing & Polish

### Task 5.1: Add Unit Tests

**Files:**
- Create: `core/seerr/src/test/java/dev/jausc/myflix/core/seerr/SeerrClientTest.kt`
- Create: `core/seerr/src/test/java/dev/jausc/myflix/core/seerr/SeerrRepositoryTest.kt`

---

### Task 5.2: Integration Testing

Manual testing checklist:
- [ ] Setup flow with Jellyfin auth
- [ ] Setup flow with local auth
- [ ] Home screen loads discover rows
- [ ] Search returns results
- [ ] Detail screen shows movie info
- [ ] Detail screen shows TV info with seasons
- [ ] Request movie (regular and 4K)
- [ ] Request TV show (all seasons, specific seasons)
- [ ] View requests list
- [ ] Approve/decline requests (if admin)
- [ ] Report issue
- [ ] Browse by genre
- [ ] Focus navigation works correctly
- [ ] Back navigation restores focus

---

### Task 5.3: Performance Optimization

- Add image preloading for visible cards
- Implement request debouncing for search
- Add skeleton loading states
- Cache discover rows across navigation

---

## Summary

This plan provides a complete rewrite of the Seerr integration:

**Phase 1: Core (~4 tasks)**
- Data models with all SeerrTV fields
- Comprehensive API client with multi-auth
- Repository layer with caching
- Hilt DI setup

**Phase 2: ViewModels (~3 tasks)**
- Home, Detail, Search, Requests, Issues, Setup

**Phase 3: TV Screens (~6 tasks)**
- All screens adapted to MyFlix UI/UX
- Focus halo effects
- NavRail integration
- Standard card sizes and colors

**Phase 4: Navigation (~2 tasks)**
- Route definitions
- NavRail integration

**Phase 5: Testing (~3 tasks)**
- Unit tests
- Integration testing
- Performance optimization

Total: ~18 tasks across 5 phases
