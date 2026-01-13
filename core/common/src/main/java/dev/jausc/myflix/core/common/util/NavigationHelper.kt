package dev.jausc.myflix.core.common.util

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Helper object for navigation-related utilities.
 * Provides URL encoding/decoding for navigation arguments to handle
 * special characters in library names, item titles, etc.
 */
object NavigationHelper {

    private const val CHARSET = "UTF-8"

    /**
     * Encodes a string for safe use in navigation route arguments.
     * Handles spaces, punctuation, and other special characters.
     *
     * Example: "Sci-Fi Movies" -> "Sci-Fi+Movies"
     */
    fun encodeNavArg(value: String): String =
        URLEncoder.encode(value, CHARSET)

    /**
     * Decodes a navigation route argument back to its original string.
     *
     * Example: "Sci-Fi+Movies" -> "Sci-Fi Movies"
     */
    fun decodeNavArg(value: String): String =
        URLDecoder.decode(value, CHARSET)

    /**
     * Builds a library route with properly encoded arguments.
     * @param collectionType The library type (e.g., "movies", "tvshows", null for unknown)
     */
    fun buildLibraryRoute(libraryId: String, libraryName: String, collectionType: String? = null): String =
        "library/$libraryId/${encodeNavArg(libraryName)}/${encodeNavArg(collectionType ?: "")}"

    /**
     * Base routes for Seerr navigation.
     */
    const val SEERR_SEARCH_ROUTE = "seerr/search"
    const val SEERR_REQUESTS_ROUTE = "seerr/requests"

    /**
     * Builds a Seerr discover route for a specific category.
     */
    fun buildSeerrDiscoverRoute(category: String): String =
        "seerr/discover/${encodeNavArg(category)}"

    /**
     * Builds a Seerr collection detail route.
     */
    fun buildSeerrCollectionRoute(collectionId: Int): String =
        "seerr/collection/$collectionId"
}
