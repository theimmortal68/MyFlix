package dev.jausc.myflix.core.common

import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isEpisode

/**
 * Shared logic for building hero section featured content.
 * Used by both TV and mobile apps to ensure consistent content selection.
 */
object HeroContentBuilder {
    
    /**
     * Configuration for hero content selection.
     */
    data class Config(
        val maxContinueWatching: Int = 3,
        val maxNextUp: Int = 2,
        val maxRecentMovies: Int = 3,
        val maxRecentShows: Int = 2,
        val maxTotalItems: Int = 10
    )
    
    /**
     * Default configuration for hero content.
     */
    val defaultConfig = Config()
    
    /**
     * Mobile-optimized configuration with fewer items.
     */
    val mobileConfig = Config(
        maxContinueWatching = 3,
        maxNextUp = 2,
        maxRecentMovies = 2,
        maxRecentShows = 1,
        maxTotalItems = 8
    )
    
    /**
     * Build the list of featured items for the hero section.
     * 
     * Priority order:
     * 1. Continue watching items with backdrops
     * 2. Next up episodes
     * 3. Recent movies with backdrops
     * 4. Recent shows with backdrops
     * 
     * @param continueWatching List of items the user is currently watching
     * @param nextUp List of next episodes to watch
     * @param recentMovies List of recently added movies
     * @param recentShows List of recently added TV shows
     * @param config Configuration for content limits
     * @return List of featured items for the hero section
     */
    fun buildFeaturedItems(
        continueWatching: List<JellyfinItem>,
        nextUp: List<JellyfinItem>,
        recentMovies: List<JellyfinItem>,
        recentShows: List<JellyfinItem>,
        config: Config = defaultConfig
    ): List<JellyfinItem> {
        val featured = mutableListOf<JellyfinItem>()
        
        // Add items with backdrops from continue watching
        // Episodes with series IDs can use the series backdrop
        featured.addAll(
            continueWatching.filter { item ->
                hasBackdrop(item)
            }.take(config.maxContinueWatching)
        )
        
        // Add next up items (avoiding duplicates)
        featured.addAll(
            nextUp.filter { item -> 
                featured.none { it.id == item.id }
            }.take(config.maxNextUp)
        )
        
        // Add recent movies with backdrops (avoiding duplicates)
        featured.addAll(
            recentMovies.filter { item ->
                !item.backdropImageTags.isNullOrEmpty() &&
                featured.none { it.id == item.id }
            }.take(config.maxRecentMovies)
        )
        
        // Add recent shows with backdrops (avoiding duplicates)
        featured.addAll(
            recentShows.filter { item ->
                !item.backdropImageTags.isNullOrEmpty() &&
                featured.none { it.id == item.id }
            }.take(config.maxRecentShows)
        )
        
        return featured.take(config.maxTotalItems)
    }
    
    /**
     * Check if an item has a backdrop image available.
     * Episodes can use their series backdrop.
     */
    fun hasBackdrop(item: JellyfinItem): Boolean {
        return !item.backdropImageTags.isNullOrEmpty() || 
               (item.isEpisode && item.seriesId != null)
    }
}
