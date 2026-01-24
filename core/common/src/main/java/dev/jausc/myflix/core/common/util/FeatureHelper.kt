package dev.jausc.myflix.core.common.util

import dev.jausc.myflix.core.common.model.JellyfinItem
import java.time.Instant
import java.util.Locale

/**
 * Represents a categorized section of special features/extras.
 */
data class FeatureSection(
    val title: String,
    val items: List<JellyfinItem>,
)

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
