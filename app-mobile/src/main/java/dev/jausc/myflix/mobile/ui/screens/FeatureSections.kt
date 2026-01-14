package dev.jausc.myflix.mobile.ui.screens

import dev.jausc.myflix.core.common.model.JellyfinItem
import java.util.Locale

data class FeatureSection(
    val title: String,
    val items: List<JellyfinItem>,
)

private data class FeatureCategory(
    val title: String,
    val keywords: List<String>,
)

fun buildFeatureSections(
    features: List<JellyfinItem>,
    excludedIds: Set<String>,
): List<FeatureSection> {
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

fun isTrailerFeature(item: JellyfinItem): Boolean =
    item.name.contains("trailer", ignoreCase = true)

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
