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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    fun refreshRequests() {
        scope.launch { loadRequests(pageToLoad = 1, append = false) }
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
            seerrClient.cancelRequest(request.id)
                .onSuccess {
                    actionMessage = "Request canceled"
                    loadRequests(pageToLoad = 1, append = false)
                }
                .onFailure { actionMessage = it.message ?: "Failed to cancel request" }
            updatingRequestId = null
        }
    }

    LaunchedEffect(requestScope, selectedFilter, selectedSort) {
        loadRequests(pageToLoad = 1, append = false)
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

        if (canViewAllRequests != false) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SeerrRequestScope.entries) { scopeItem ->
                    FilterChip(
                        selected = requestScope == scopeItem,
                        onClick = { requestScope = scopeItem },
                        label = { Text(scopeItem.label) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SeerrRequestFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SeerrRequestSort.entries) { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { selectedSort = sort },
                    label = { Text(sort.label) },
                )
            }
        }

        if (actionMessage != null) {
            Text(
                text = actionMessage ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(requests, key = { _, request -> request.id }) { _, request ->
                        SeerrRequestCard(
                            request = request,
                            showAdminActions = requestScope == SeerrRequestScope.ALL,
                            isUpdating = updatingRequestId == request.id,
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

@Composable
private fun SeerrRequestCard(
    request: SeerrRequest,
    showAdminActions: Boolean,
    isUpdating: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
) {
    val statusText = SeerrRequestStatus.toDisplayString(request.status)
    val mediaType = request.media?.mediaType ?: "unknown"
    val mediaId = request.media?.tmdbId ?: request.media?.id
    val mediaStatusText = SeerrMediaStatus.toDisplayString(request.media?.status)
    val canApprove = showAdminActions && request.isPendingApproval
    val canDecline = showAdminActions && request.isPendingApproval
    val canCancel = request.status != SeerrRequestStatus.DECLINED

    Card(
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                text = "Request #${request.id}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$mediaType • ${mediaId ?: "unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            request.requestedBy?.name?.let { requester ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Requested by $requester",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Availability: $mediaStatusText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (canApprove || canDecline || canCancel) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canApprove) {
                        OutlinedButton(onClick = onApprove, enabled = !isUpdating) {
                            Text("Approve")
                        }
                    }
                    if (canDecline) {
                        OutlinedButton(onClick = onDecline, enabled = !isUpdating) {
                            Text("Decline")
                        }
                    }
                    if (canCancel) {
                        OutlinedButton(onClick = onCancel, enabled = !isUpdating) {
                            Text("Cancel")
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
