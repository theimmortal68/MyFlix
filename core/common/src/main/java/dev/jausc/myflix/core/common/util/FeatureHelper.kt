package dev.jausc.myflix.core.common.util

import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.youtube.VideoInfo
import java.time.Instant
import java.util.Locale

/**
 * Represents a categorized section of special features/extras.
 */
data class FeatureSection(
    val title: String,
    val items: List<JellyfinItem>,
)

/**
 * Unified representation of an extra that can be either local (from Jellyfin library)
 * or remote (from TMDB via YouTube).
 */
sealed class ExtraItem {
    abstract val name: String
    abstract val type: String

    /**
     * Local extra from Jellyfin library (already downloaded).
     */
    data class Local(
        val item: JellyfinItem,
        override val name: String = item.name,
        override val type: String = item.extraType ?: "Extra",
    ) : ExtraItem()

    /**
     * Remote extra from TMDB (streamed via YouTube).
     */
    data class Remote(
        val videoInfo: VideoInfo,
        override val name: String = videoInfo.name,
        override val type: String = videoInfo.type,
    ) : ExtraItem() {
        val key: String get() = videoInfo.key
        val isOfficial: Boolean get() = videoInfo.official
    }
}

/**
 * Build a unified list of extras from local features and remote TMDB videos.
 * Local extras take priority (shown first), remote extras fill in additional content.
 *
 * @param localFeatures Local special features from Jellyfin
 * @param remoteVideos Remote videos from TMDB
 * @param excludeTrailers Whether to exclude trailers (since they're shown separately)
 * @return Combined list of extras
 */
fun buildUnifiedExtras(
    localFeatures: List<JellyfinItem>,
    remoteVideos: List<VideoInfo>,
    excludeTrailers: Boolean = true,
): List<ExtraItem> {
    val extras = mutableListOf<ExtraItem>()

    // Add local extras first (they have priority)
    localFeatures
        .filter { item ->
            if (excludeTrailers) {
                !item.name.contains("trailer", ignoreCase = true)
            } else true
        }
        .forEach { item ->
            extras.add(ExtraItem.Local(item))
        }

    // Add remote extras that don't duplicate local ones
    val localNames = localFeatures.map { it.name.lowercase() }.toSet()
    remoteVideos
        .filter { video ->
            // Exclude trailers if requested
            val isTrailer = video.type.equals("Trailer", ignoreCase = true) ||
                video.type.equals("Teaser", ignoreCase = true)
            if (excludeTrailers && isTrailer) return@filter false

            // Don't add duplicates (by similar name)
            val nameLower = video.name.lowercase()
            !localNames.any { localName ->
                nameLower.contains(localName) || localName.contains(nameLower)
            }
        }
        .forEach { video ->
            extras.add(ExtraItem.Remote(video))
        }

    return extras
}

private data class FeatureCategory(
    val title: String,
    val keywords: List<String>,
)

/**
 * Build categorized sections from a list of special features.
 * Groups items into categories like Trailers, Teasers, Featurettes, etc.
 *
 * @param features List of special feature items
 * @param excludedIds Set of item IDs to exclude (e.g., already-displayed trailer)
 * @return List of categorized feature sections
 */
fun buildFeatureSections(features: List<JellyfinItem>, excludedIds: Set<String>,): List<FeatureSection> {
    if (features.isEmpty()) return emptyList()

    val categories = listOf(
        FeatureCategory("Trailers", listOf("trailer")),
        FeatureCategory("Teasers", listOf("teaser")),
        FeatureCategory("Featurettes", listOf("featurette")),
        FeatureCategory("Clips", listOf("clip")),
        FeatureCategory("Behind the Scenes", listOf("behind", "bts", "making of")),
        FeatureCategory("Bloopers", listOf("blooper", "gag")),
    )

    val extras = mutableListOf<JellyfinItem>()
    val grouped = categories.associate { it.title to mutableListOf<JellyfinItem>() }

    features
        .filter { it.id !in excludedIds }
        .forEach { item ->
            val name = item.name.lowercase(Locale.US)
            val category = categories.firstOrNull { candidate ->
                candidate.keywords.any { keyword -> name.contains(keyword) }
            }
            if (category == null) {
                extras.add(item)
            } else {
                grouped.getValue(category.title).add(item)
            }
        }

    val sections = mutableListOf<FeatureSection>()
    categories.forEach { category ->
        val items = grouped.getValue(category.title)
        if (items.isNotEmpty()) {
            sections.add(FeatureSection(category.title, items))
        }
    }
    if (extras.isNotEmpty()) {
        sections.add(FeatureSection("Extras", extras))
    }

    return sections
}

/**
 * Check if an item is a trailer based on its name.
 */
fun isTrailerFeature(item: JellyfinItem): Boolean = item.name.contains("trailer", ignoreCase = true)

/**
 * Find the newest trailer from a list of special features.
 * Prefers trailers with premiere dates, falls back to the last trailer in the list.
 *
 * @param features List of special feature items
 * @return The newest trailer, or null if no trailers found
 */
fun findNewestTrailer(features: List<JellyfinItem>): JellyfinItem? {
    val trailers = features.filter { isTrailerFeature(it) }
    if (trailers.isEmpty()) return null

    val datedTrailers = trailers.mapNotNull { trailer ->
        trailer.premiereDate?.let { date ->
            val epoch = runCatching { Instant.parse(date).toEpochMilli() }.getOrNull()
            epoch?.let { trailer to it }
        }
    }

    return if (datedTrailers.isNotEmpty()) {
        datedTrailers.maxByOrNull { it.second }?.first
    } else {
        trailers.lastOrNull()
    }
}

/**
 * Extract YouTube video key from various YouTube URL formats.
 * Supports youtube.com/watch?v=, youtu.be/, and youtube.com/embed/ formats.
 *
 * @param url YouTube URL to parse
 * @return Video key/ID, or null if not a valid YouTube URL
 */
fun extractYouTubeVideoKey(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val patterns = listOf(
        Regex("v=([A-Za-z0-9_-]{6,})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{6,})"),
        Regex("youtube\\.com/embed/([A-Za-z0-9_-]{6,})"),
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(url)?.groupValues?.getOrNull(1)
    }
}
