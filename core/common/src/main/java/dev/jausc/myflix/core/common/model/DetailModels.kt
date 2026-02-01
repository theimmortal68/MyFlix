package dev.jausc.myflix.core.common.model

/**
 * Represents a single detail info item (label-value pair).
 * Used in detail screens to display metadata like Director, Writer, Studio, etc.
 */
data class DetailInfoItem(
    val label: String,
    val value: String,
)

/**
 * Represents an external link item (e.g., IMDb, TMDb, Trakt).
 * Used in detail screens to display links to external services.
 */
data class ExternalLinkItem(
    val label: String,
    val url: String,
)

/**
 * Content type for TMDB URL generation.
 */
enum class TmdbContentType(val pathSegment: String) {
    MOVIE("movie"),
    TV("tv"),
}

/**
 * Builds a list of external links for a media item.
 * Includes any existing external URLs from the item, plus adds IMDb and TMDB
 * links if the item has those IDs but no existing links.
 *
 * @param item The Jellyfin item to build links for
 * @param tmdbContentType The type of content for TMDB URLs (movie or tv)
 * @return List of external link items
 */
fun buildExternalLinks(item: JellyfinItem, tmdbContentType: TmdbContentType): List<ExternalLinkItem> {
    val links = mutableListOf<ExternalLinkItem>()

    // Add any existing external URLs from the item
    item.externalUrls?.forEach { url ->
        val label = url.name?.trim().orEmpty()
        val link = url.url?.trim().orEmpty()
        if (label.isNotEmpty() && link.isNotEmpty()) {
            links.add(ExternalLinkItem(label, link))
        }
    }

    // Add IMDb link if not already present
    item.imdbId?.let { imdbId ->
        val hasImdb = links.any { it.label.equals("imdb", ignoreCase = true) }
        if (!hasImdb) {
            links.add(ExternalLinkItem("IMDb", "https://www.imdb.com/title/$imdbId"))
        }
    }

    // Add TMDB link if not already present
    item.tmdbId?.let { tmdbId ->
        val hasTmdb = links.any { it.label.equals("tmdb", ignoreCase = true) }
        if (!hasTmdb) {
            links.add(ExternalLinkItem("TMDB", "https://www.themoviedb.org/${tmdbContentType.pathSegment}/$tmdbId"))
        }
    }

    return links
}
