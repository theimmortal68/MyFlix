@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrRequest
import dev.jausc.myflix.core.seerr.SeerrRequestStatus
import dev.jausc.myflix.tv.ui.components.TvDropdownMenu
import dev.jausc.myflix.tv.ui.components.TvDropdownMenuItemWithCheck
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val REQUEST_PAGE_SIZE = 20

@Composable
fun SeerrRequestsScreen(
    seerrClient: SeerrClient,
    onBack: () -> Unit,
    onNavigateToDetail: (tmdbId: Int, mediaType: String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var requestScope by remember { mutableStateOf(SeerrRequestScope.MINE) }
    var canViewAllRequests by remember { mutableStateOf<Boolean?>(null) }
    var selectedFilter by remember { mutableStateOf(SeerrRequestFilter.ALL) }
    var selectedSort by remember { mutableStateOf(SeerrRequestSort.ADDED) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var requests by remember { mutableStateOf<List<SeerrRequest>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var updatingRequestId by remember { mutableStateOf<Int?>(null) }

    // Cache for media titles (tmdbId -> title)
    val mediaTitleCache = remember { mutableStateMapOf<String, String>() }

    // Dropdown states
    var showFilterDropdown by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var showScopeDropdown by remember { mutableStateOf(false) }

    suspend fun loadRequests(pageToLoad: Int, append: Boolean) {
        if (append) {
            isLoadingMore = true
        } else {
            isLoading = true
        }
        errorMessage = null
        actionMessage = null

        val result = if (requestScope == SeerrRequestScope.ALL) {
            seerrClient.getAllRequests(
                page = pageToLoad,
                pageSize = REQUEST_PAGE_SIZE,
                filter = selectedFilter.filterValue,
                sort = selectedSort.sortValue,
            )
        } else {
            seerrClient.getMyRequests(
                page = pageToLoad,
                pageSize = REQUEST_PAGE_SIZE,
                filter = selectedFilter.filterValue,
                sort = selectedSort.sortValue,
            )
        }

        result
            .onSuccess { response ->
                if (requestScope == SeerrRequestScope.ALL) {
                    canViewAllRequests = true
                }
                requests = if (append) {
                    (requests + response.results).distinctBy { it.id }
                } else {
                    response.results
                }
                page = response.pageInfo.page
                totalPages = response.pageInfo.pages
            }
            .onFailure { error ->
                if (requestScope == SeerrRequestScope.ALL) {
                    canViewAllRequests = false
                    requestScope = SeerrRequestScope.MINE
                }
                errorMessage = error.message ?: "Failed to load requests"
            }

        if (append) {
            isLoadingMore = false
        } else {
            isLoading = false
        }
    }

    // Fetch media title for a request
    suspend fun fetchMediaTitle(request: SeerrRequest) {
        val mediaType = request.media?.mediaType ?: return
        val tmdbId = request.media?.tmdbId ?: return
        val cacheKey = "$mediaType:$tmdbId"

        if (mediaTitleCache.containsKey(cacheKey)) return

        val result = when (mediaType) {
            "movie" -> seerrClient.getMovie(tmdbId)
            "tv" -> seerrClient.getTVShow(tmdbId)
            else -> return
        }

        result.onSuccess { media ->
            mediaTitleCache[cacheKey] = media.displayTitle
        }
    }

    fun handleApprove(request: SeerrRequest) {
        scope.launch {
            updatingRequestId = request.id
            actionMessage = null
            seerrClient.approveRequest(request.id)
                .onSuccess {
                    actionMessage = "Request approved"
                    loadRequests(pageToLoad = 1, append = false)
                }
                .onFailure { actionMessage = it.message ?: "Failed to approve request" }
            updatingRequestId = null
        }
    }

    fun handleDecline(request: SeerrRequest) {
        scope.launch {
            updatingRequestId = request.id
            actionMessage = null
            seerrClient.declineRequest(request.id)
                .onSuccess {
                    actionMessage = "Request declined"
                    loadRequests(pageToLoad = 1, append = false)
                }
                .onFailure { actionMessage = it.message ?: "Failed to decline request" }
            updatingRequestId = null
        }
    }

    fun handleCancel(request: SeerrRequest) {
        scope.launch {
            updatingRequestId = request.id
            actionMessage = null

            // First cancel the request
            seerrClient.cancelRequest(request.id)
                .onSuccess {
                    // Then delete the media to remove from Sonarr/Radarr
                    val mediaId = request.media?.id
                    if (mediaId != null) {
                        seerrClient.deleteMedia(mediaId)
                            .onSuccess {
                                actionMessage = "Request canceled and media removed"
                            }
                            .onFailure {
                                // Request was canceled but media deletion failed
                                actionMessage = "Request canceled (media removal failed)"
                            }
                    } else {
                        actionMessage = "Request canceled"
                    }
                    loadRequests(pageToLoad = 1, append = false)
                }
                .onFailure { actionMessage = it.message ?: "Failed to cancel request" }
            updatingRequestId = null
        }
    }

    LaunchedEffect(requestScope, selectedFilter, selectedSort) {
        loadRequests(pageToLoad = 1, append = false)
    }

    // Fetch titles for visible requests
    LaunchedEffect(requests) {
        requests.forEach { request ->
            scope.launch {
                fetchMediaTitle(request)
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .collectLatest { lastVisibleIndex ->
                val shouldLoadMore = lastVisibleIndex >= requests.lastIndex - 3
                val hasMore = page < totalPages
                if (shouldLoadMore && hasMore && !isLoading && !isLoadingMore) {
                    scope.launch { loadRequests(pageToLoad = page + 1, append = true) }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        // Header row with back button, title, and filter/sort dropdowns
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.height(20.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                scale = ButtonDefaults.scale(focusedScale = 1f),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                    contentColor = TvColors.TextPrimary,
                    focusedContainerColor = TvColors.BluePrimary,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Requests",
                style = MaterialTheme.typography.headlineMedium,
                color = TvColors.TextPrimary,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Scope dropdown (Mine/All)
            if (canViewAllRequests != false) {
                Box {
                    Button(
                        onClick = { showScopeDropdown = true },
                        modifier = Modifier.height(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        scale = ButtonDefaults.scale(focusedScale = 1f),
                        colors = ButtonDefaults.colors(
                            containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                            contentColor = TvColors.TextPrimary,
                            focusedContainerColor = TvColors.BluePrimary,
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = requestScope.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    TvDropdownMenu(
                        expanded = showScopeDropdown,
                        onDismissRequest = { showScopeDropdown = false },
                        offset = DpOffset(0.dp, 4.dp),
                    ) {
                        SeerrRequestScope.entries.forEach { scopeItem ->
                            TvDropdownMenuItemWithCheck(
                                text = scopeItem.label,
                                isSelected = requestScope == scopeItem,
                                onClick = {
                                    requestScope = scopeItem
                                    showScopeDropdown = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Filter dropdown
            Box {
                Button(
                    onClick = { showFilterDropdown = true },
                    modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    scale = ButtonDefaults.scale(focusedScale = 1f),
                    colors = ButtonDefaults.colors(
                        containerColor = if (selectedFilter != SeerrRequestFilter.ALL) {
                            TvColors.BluePrimary.copy(alpha = 0.3f)
                        } else {
                            TvColors.SurfaceElevated.copy(alpha = 0.8f)
                        },
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedFilter.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                TvDropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false },
                    offset = DpOffset(0.dp, 4.dp),
                ) {
                    SeerrRequestFilter.entries.forEach { filter ->
                        TvDropdownMenuItemWithCheck(
                            text = filter.label,
                            isSelected = selectedFilter == filter,
                            onClick = {
                                selectedFilter = filter
                                showFilterDropdown = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sort dropdown
            Box {
                Button(
                    onClick = { showSortDropdown = true },
                    modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    scale = ButtonDefaults.scale(focusedScale = 1f),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                        contentColor = TvColors.TextPrimary,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedSort.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                TvDropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = { showSortDropdown = false },
                    offset = DpOffset(0.dp, 4.dp),
                ) {
                    SeerrRequestSort.entries.forEach { sort ->
                        TvDropdownMenuItemWithCheck(
                            text = sort.label,
                            isSelected = selectedSort == sort,
                            onClick = {
                                selectedSort = sort
                                showSortDropdown = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (actionMessage != null) {
            Text(
                text = actionMessage ?: "",
                color = TvColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Failed to load requests",
                        color = TvColors.Error,
                    )
                }
            }
            requests.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No requests found",
                        color = TvColors.TextSecondary,
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(requests, key = { _, request -> request.id }) { _, request ->
                        val mediaType = request.media?.mediaType ?: "unknown"
                        val tmdbId = request.media?.tmdbId
                        val cacheKey = "$mediaType:$tmdbId"
                        val mediaTitle = mediaTitleCache[cacheKey]

                        CompactRequestRow(
                            request = request,
                            mediaTitle = mediaTitle,
                            showAdminActions = requestScope == SeerrRequestScope.ALL,
                            isUpdating = updatingRequestId == request.id,
                            onClick = {
                                if (tmdbId != null && mediaType != "unknown") {
                                    onNavigateToDetail(tmdbId, mediaType)
                                }
                            },
                            onApprove = { handleApprove(request) },
                            onDecline = { handleDecline(request) },
                            onCancel = { handleCancel(request) },
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                TvLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact single/two-line request row.
 * Shows: Title | Type Badge | Status Badge | Requester | Actions
 */
@Composable
private fun CompactRequestRow(
    request: SeerrRequest,
    mediaTitle: String?,
    showAdminActions: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
) {
    val statusColor = when (request.status) {
        SeerrRequestStatus.PENDING_APPROVAL -> Color(0xFFFBBF24) // Yellow
        SeerrRequestStatus.APPROVED -> Color(0xFF22C55E) // Green
        SeerrRequestStatus.DECLINED -> Color(0xFFEF4444) // Red
        else -> TvColors.TextSecondary
    }
    val statusText = request.statusText
    val mediaType = request.media?.mediaType ?: "unknown"
    val availabilityColor = when (request.media?.status) {
        SeerrMediaStatus.AVAILABLE -> Color(0xFF22C55E)
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Color(0xFFFBBF24)
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> Color(0xFF60A5FA)
        else -> TvColors.TextSecondary
    }
    val availabilityText = SeerrMediaStatus.toDisplayString(request.media?.status)
    val canApprove = showAdminActions && request.isPendingApproval
    val canDecline = showAdminActions && request.isPendingApproval
    val canCancel = request.status != SeerrRequestStatus.DECLINED

    // Use Column to separate the clickable info row from the focusable action buttons
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Clickable info row - navigates to detail
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.FocusedSurface,
            ),
            scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Title (or loading placeholder)
                Text(
                    text = mediaTitle ?: "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (mediaTitle != null) TvColors.TextPrimary else TvColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Media type badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (mediaType == "movie") Color(0xFF8B5CF6) else Color(0xFF60A5FA),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (mediaType == "movie") "Movie" else "TV",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }

                // Request status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                    )
                }

                // Availability badge
                Box(
                    modifier = Modifier
                        .background(
                            color = availabilityColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = availabilityText,
                        style = MaterialTheme.typography.labelSmall,
                        color = availabilityColor,
                    )
                }

                // Requester
                request.requestedBy?.name?.let { requester ->
                    Text(
                        text = requester,
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.TextSecondary,
                        maxLines = 1,
                    )
                }
            }
        }

        // Action buttons - separate from Surface so they can receive D-pad focus
        if (canApprove || canDecline || canCancel) {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (canApprove) {
                    Button(
                        onClick = onApprove,
                        enabled = !isUpdating,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        scale = ButtonDefaults.scale(focusedScale = 1.05f),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF22C55E).copy(alpha = 0.2f),
                            contentColor = Color(0xFF22C55E),
                            focusedContainerColor = Color(0xFF22C55E),
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Text("Approve", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (canDecline) {
                    Button(
                        onClick = onDecline,
                        enabled = !isUpdating,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        scale = ButtonDefaults.scale(focusedScale = 1.05f),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                            contentColor = Color(0xFFEF4444),
                            focusedContainerColor = Color(0xFFEF4444),
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Text("Decline", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (canCancel) {
                    Button(
                        onClick = onCancel,
                        enabled = !isUpdating,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        scale = ButtonDefaults.scale(focusedScale = 1.05f),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                            contentColor = Color(0xFFEF4444),
                            focusedContainerColor = Color(0xFFEF4444),
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private enum class SeerrRequestScope(val label: String) {
    MINE("Mine"),
    ALL("All"),
}

private enum class SeerrRequestFilter(val label: String, val filterValue: String) {
    ALL("All", "all"),
    PENDING("Pending", "pending"),
    APPROVED("Approved", "approved"),
    AVAILABLE("Available", "available"),
    DECLINED("Declined", "declined"),
}

private enum class SeerrRequestSort(val label: String, val sortValue: String) {
    ADDED("Added", "added"),
    MODIFIED("Modified", "modified"),
}
