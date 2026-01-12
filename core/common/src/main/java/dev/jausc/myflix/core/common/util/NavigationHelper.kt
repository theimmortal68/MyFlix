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
     */
    fun buildLibraryRoute(libraryId: String, libraryName: String): String =
        "library/$libraryId/${encodeNavArg(libraryName)}"
}
