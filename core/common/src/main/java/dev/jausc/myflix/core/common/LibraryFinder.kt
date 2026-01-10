package dev.jausc.myflix.core.common

import dev.jausc.myflix.core.common.model.JellyfinItem

/**
 * Utility for finding specific library types from a list of Jellyfin libraries.
 *
 * Prefers matching by collectionType, falls back to name-based matching.
 */
object LibraryFinder {

    /**
     * Find the movies library from a list of libraries.
     *
     * @param libraries List of library items from Jellyfin
     * @return The movies library if found, null otherwise
     */
    fun findMoviesLibrary(libraries: List<JellyfinItem>): JellyfinItem? {
        return libraries.find {
            it.collectionType == "movies"
        } ?: libraries.find {
            it.name.contains("movie", ignoreCase = true) ||
            it.name.contains("film", ignoreCase = true)
        }
    }

    /**
     * Find the TV shows library from a list of libraries.
     *
     * @param libraries List of library items from Jellyfin
     * @return The TV shows library if found, null otherwise
     */
    fun findShowsLibrary(libraries: List<JellyfinItem>): JellyfinItem? {
        return libraries.find {
            it.collectionType == "tvshows"
        } ?: libraries.find {
            it.name.contains("show", ignoreCase = true) ||
            it.name.contains("series", ignoreCase = true) ||
            it.name.equals("tv", ignoreCase = true)
        }
    }
}
