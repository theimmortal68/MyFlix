package com.myflix.app.domain.usecase

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for fetching featured media items for the hero section.
 * Retrieves a mix of recently added, resume watching, and popular items.
 */
class GetFeaturedMediaUseCase @Inject constructor(
    private val apiClient: ApiClient
) {
    /**
     * Fetch featured items for the hero section.
     * 
     * Priority:
     * 1. Items currently being watched (resume)
     * 2. Recently added items with high ratings
     * 3. Popular items from the library
     * 
     * @param userId The current user's ID
     * @param limit Maximum number of items to return (default 10)
     * @return List of featured items for the hero
     */
    suspend fun invoke(userId: UUID, limit: Int = 10): Result<List<BaseItemDto>> {
        return try {
            val featuredItems = mutableListOf<BaseItemDto>()
            
            // 1. Get resume watching items (Continue Watching)
            val resumeItems = getResumeItems(userId, limit = 3)
            featuredItems.addAll(resumeItems)
            
            // 2. Get recently added items with backdrops
            val recentItems = getRecentlyAddedWithBackdrops(userId, limit = 5)
            featuredItems.addAll(recentItems.filter { item -> 
                featuredItems.none { it.id == item.id } 
            })
            
            // 3. If we still need more, get highly rated items
            if (featuredItems.size < limit) {
                val popularItems = getHighlyRatedItems(userId, limit = 5)
                featuredItems.addAll(popularItems.filter { item ->
                    featuredItems.none { it.id == item.id }
                })
            }
            
            // 4. Get next up episodes if we have room
            if (featuredItems.size < limit) {
                val nextUpItems = getNextUpEpisodes(userId, limit = 3)
                featuredItems.addAll(nextUpItems.filter { item ->
                    featuredItems.none { it.id == item.id }
                })
            }
            
            Result.success(featuredItems.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get items the user is currently watching (has progress).
     */
    private suspend fun getResumeItems(userId: UUID, limit: Int): List<BaseItemDto> {
        return try {
            val response = apiClient.itemsApi.getResumeItems(
                userId = userId,
                limit = limit,
                fields = listOf(
                    ItemFields.OVERVIEW,
                    ItemFields.GENRES,
                    ItemFields.MEDIA_SOURCES
                ),
                enableImages = true,
                enableUserData = true
            )
            response.content.items?.filterHasBackdrop() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get recently added movies and series that have backdrop images.
     */
    private suspend fun getRecentlyAddedWithBackdrops(userId: UUID, limit: Int): List<BaseItemDto> {
        return try {
            val response = apiClient.itemsApi.getItems(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                sortBy = listOf(ItemSortBy.DATE_CREATED),
                sortOrder = listOf(SortOrder.DESCENDING),
                limit = limit * 2, // Fetch extra to filter for backdrops
                recursive = true,
                fields = listOf(
                    ItemFields.OVERVIEW,
                    ItemFields.GENRES,
                    ItemFields.MEDIA_SOURCES
                ),
                enableImages = true,
                enableUserData = true,
                imageTypeLimit = 1
            )
            response.content.items?.filterHasBackdrop()?.take(limit) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get highly rated items from the library.
     */
    private suspend fun getHighlyRatedItems(userId: UUID, limit: Int): List<BaseItemDto> {
        return try {
            val response = apiClient.itemsApi.getItems(
                userId = userId,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                sortBy = listOf(ItemSortBy.COMMUNITY_RATING),
                sortOrder = listOf(SortOrder.DESCENDING),
                limit = limit * 2,
                recursive = true,
                minCommunityRating = 7.0, // Only items rated 7+
                fields = listOf(
                    ItemFields.OVERVIEW,
                    ItemFields.GENRES,
                    ItemFields.MEDIA_SOURCES
                ),
                enableImages = true,
                enableUserData = true,
                imageTypeLimit = 1
            )
            response.content.items?.filterHasBackdrop()?.take(limit) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get next up episodes for series the user is watching.
     */
    private suspend fun getNextUpEpisodes(userId: UUID, limit: Int): List<BaseItemDto> {
        return try {
            val response = apiClient.tvShowsApi.getNextUp(
                userId = userId,
                limit = limit,
                fields = listOf(
                    ItemFields.OVERVIEW,
                    ItemFields.MEDIA_SOURCES
                ),
                enableImages = true,
                enableUserData = true
            )
            // Next Up items always have series backdrop available
            response.content.items ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Filter items to only those with backdrop images.
     */
    private fun List<BaseItemDto>.filterHasBackdrop(): List<BaseItemDto> {
        return filter { item ->
            !item.backdropImageTags.isNullOrEmpty() ||
            // Episodes and seasons can use series backdrop
            (item.type == BaseItemKind.EPISODE && item.seriesId != null) ||
            (item.type == BaseItemKind.SEASON && item.seriesId != null)
        }
    }
}
