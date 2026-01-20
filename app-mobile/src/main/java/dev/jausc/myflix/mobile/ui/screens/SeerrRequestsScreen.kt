@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.mobile.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrRequest
import dev.jausc.myflix.core.seerr.SeerrRequestStatus
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header row with back button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Scope dropdown (Mine/All)
            if (canViewAllRequests != false) {
                Box {
                    AssistChip(
                        onClick = { showScopeDropdown = true },
                        label = { Text(requestScope.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )

                    DropdownMenu(
                        expanded = showScopeDropdown,
                        onDismissRequest = { showScopeDropdown = false },
                    ) {
                        SeerrRequestScope.entries.forEach { scopeItem ->
                            DropdownMenuItem(
                                text = { Text(scopeItem.label) },
                                onClick = {
                                    requestScope = scopeItem
                                    showScopeDropdown = false
                                },
                            )
                        }
                    }
                }
            }

            // Filter dropdown
            Box {
                AssistChip(
                    onClick = { showFilterDropdown = true },
                    label = { Text(selectedFilter.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.FilterAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = if (selectedFilter != SeerrRequestFilter.ALL) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    } else {
                        AssistChipDefaults.assistChipColors()
                    },
                )

                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false },
                ) {
                    SeerrRequestFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label) },
                            onClick = {
                                selectedFilter = filter
                                showFilterDropdown = false
                            },
                        )
                    }
                }
            }

            // Sort dropdown
            Box {
                AssistChip(
                    onClick = { showSortDropdown = true },
                    label = { Text(selectedSort.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )

                DropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = { showSortDropdown = false },
                ) {
                    SeerrRequestSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label) },
                            onClick = {
                                selectedSort = sort
                                showSortDropdown = false
                            },
                        )
                    }
                }
            }
        }

        if (actionMessage != null) {
            Text(
                text = actionMessage ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Failed to load requests",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(requests, key = { _, request -> request.id }) { _, request ->
                        val mediaType = request.media?.mediaType ?: "unknown"
                        val tmdbId = request.media?.tmdbId
                        val cacheKey = "$mediaType:$tmdbId"
                        val mediaTitle = mediaTitleCache[cacheKey]

                        CompactRequestCard(
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
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact request card showing title, badges, and actions.
 */
@Composable
private fun CompactRequestCard(
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
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = request.statusText
    val mediaType = request.media?.mediaType ?: "unknown"
    val availabilityColor = when (request.media?.status) {
        SeerrMediaStatus.AVAILABLE -> Color(0xFF22C55E)
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Color(0xFFFBBF24)
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> Color(0xFF60A5FA)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val availabilityText = SeerrMediaStatus.toDisplayString(request.media?.status)
    val canApprove = showAdminActions && request.isPendingApproval
    val canDecline = showAdminActions && request.isPendingApproval
    val canCancel = request.status != SeerrRequestStatus.DECLINED

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Title row with badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Title (or loading placeholder)
                Text(
                    text = mediaTitle ?: "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (mediaTitle != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

                Spacer(modifier = Modifier.weight(1f))

                // Requester
                request.requestedBy?.name?.let { requester ->
                    Text(
                        text = requester,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Action buttons
            if (canApprove || canDecline || canCancel) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canApprove) {
                        OutlinedButton(
                            onClick = onApprove,
                            enabled = !isUpdating,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text("Approve", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (canDecline) {
                        OutlinedButton(
                            onClick = onDecline,
                            enabled = !isUpdating,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text("Decline", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (canCancel) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !isUpdating,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelSmall)
                        }
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
