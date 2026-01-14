package dev.jausc.myflix.core.network

/**
 * Centralized cache key definitions for JellyfinClient.
 *
 * All cache keys and TTL values are defined here to:
 * - Prevent typos and inconsistencies
 * - Make cache invalidation patterns clear
 * - Document what data is cached and for how long
 */
object CacheKeys {

    // ==================== Cache TTL Values ====================

    /**
     * Time-to-live values for different data types.
     * Values are in milliseconds.
     */
    object Ttl {
        /** Libraries rarely change - 5 minutes */
        const val LIBRARIES = 300_000L

        /** Item details moderate volatility - 2 minutes */
        const val ITEM_DETAILS = 120_000L

        /** Resume/continue watching changes frequently - 30 seconds */
        const val RESUME = 30_000L

        /** Next up episodes - 1 minute */
        const val NEXT_UP = 60_000L

        /** Latest additions - 1 minute */
        const val LATEST = 60_000L

        /** Default fallback - 1 minute */
        const val DEFAULT = 60_000L

        /** DNS cache for network resilience - 5 minutes */
        const val DNS = 300_000L
    }

    // ==================== Cache Key Builders ====================

    /** User's library list */
    const val LIBRARIES = "libraries"

    /** Library items with pagination */
    fun library(libraryId: String, limit: Int, startIndex: Int, sortBy: String): String =
        "library:$libraryId:$limit:$startIndex:$sortBy"

    /** Single item details */
    fun item(itemId: String): String = "item:$itemId"

    /** Continue watching / resume items */
    fun resume(limit: Int): String = "resume:$limit"

    /** Next up episodes */
    fun nextUp(limit: Int, enableRewatching: Boolean): String =
        "nextup:$limit:$enableRewatching"

    /** Next up episodes for a specific series */
    fun nextUpSeries(seriesId: String, limit: Int, enableRewatching: Boolean): String =
        "nextup:$seriesId:$limit:$enableRewatching"

    /** Latest items from any library */
    fun latest(libraryId: String, limit: Int): String = "latest:$libraryId:$limit"

    /** Latest movies */
    fun latestMovies(libraryId: String?, limit: Int): String =
        "latestMovies:${libraryId ?: "all"}:$limit"

    /** Latest TV series */
    fun latestSeries(libraryId: String?, limit: Int): String =
        "latestSeries:${libraryId ?: "all"}:$limit"

    /** Latest episodes */
    fun latestEpisodes(libraryId: String?, limit: Int): String =
        "latestEpisodes:${libraryId ?: "all"}:$limit"

    /** Upcoming episodes (season premieres) */
    fun upcoming(libraryId: String?, limit: Int): String =
        "upcoming:${libraryId ?: "all"}:$limit"

    /** Available genres */
    fun genres(libraryId: String?): String = "genres:${libraryId ?: "all"}"

    /** Items in a specific genre */
    fun genre(genreName: String, libraryId: String?, limit: Int): String =
        "genre:$genreName:${libraryId ?: "all"}:$limit"

    /** All collections */
    fun collections(limit: Int): String = "collections:$limit"

    /** Items in a specific collection */
    fun collection(collectionId: String, limit: Int): String =
        "collection:$collectionId:$limit"

    /** Special features (extras) for an item */
    fun specialFeatures(itemId: String, limit: Int): String =
        "specialFeatures:$itemId:$limit"

    /** Ancestors for an item */
    fun ancestors(itemId: String): String = "ancestors:$itemId"

    /** Suggested items */
    fun suggestions(limit: Int): String = "suggestions:$limit"

    /** Seasons for a series */
    fun seasons(seriesId: String): String = "seasons:$seriesId"

    /** Episodes for a season */
    fun episodes(seriesId: String, seasonId: String): String =
        "episodes:$seriesId:$seasonId"

    /** Similar items */
    fun similar(itemId: String, limit: Int): String = "similar:$itemId:$limit"

    /** Items featuring a person */
    fun personItems(personId: String, limit: Int): String =
        "personItems:$personId:$limit"

    /** Favorite items */
    fun favorites(limit: Int, includeItemTypes: String?): String =
        "favorites:$limit:${includeItemTypes ?: "all"}"

    // ==================== Invalidation Patterns ====================

    /**
     * Patterns for cache invalidation.
     * Used with [JellyfinClient.invalidateCache] to clear related entries.
     */
    object Patterns {
        /** All resume/continue watching entries */
        const val RESUME = "resume"

        /** All next up entries */
        const val NEXT_UP = "nextup"

        /** All favorite entries */
        const val FAVORITES = "favorites"

        /** Item prefix for clearing item-related cache */
        const val ITEM = "item:"

        /** All latest content */
        const val LATEST = "latest"

        /** All genre content */
        const val GENRE = "genre"

        /** All collection content */
        const val COLLECTION = "collection"
    }
}
