package dev.jausc.myflix.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.core.common.model.isEpisode
import dev.jausc.myflix.core.network.JellyfinClient

/**
 * Accent colors for different content row types.
 */
object MobileRowColors {
    val ContinueWatching = Color(0xFFEF4444) // Red
    val NextUp = Color(0xFFF59E0B) // Amber
    val RecentlyAdded = Color(0xFF3B82F6) // Blue
    val Movies = Color(0xFF8B5CF6) // Purple
    val Shows = Color(0xFF10B981) // Emerald
    val Default = Color(0xFF6B7280) // Gray
    val Premieres = Color(0xFF60A5FA) // Light Blue
    val Collections = Color(0xFF34D399) // Teal
    val Suggestions = Color(0xFFF472B6) // Pink

    // Genre row colors for variety
    val genreColors = listOf(
        Color(0xFF9C27B0), // Purple
        Color(0xFFE91E63), // Pink
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF9800), // Orange
        Color(0xFF4CAF50), // Green
        Color(0xFF673AB7), // Deep Purple
    )

    // Pinned collection row colors
    val pinnedCollectionColors = listOf(
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF97316), // Orange
        Color(0xFF84CC16), // Lime
        Color(0xFFA855F7), // Purple
        Color(0xFF14B8A6), // Teal
    )
}

/**
 * Content row with title and horizontal scrolling media cards.
 * Responsive sizing based on screen width.
 */
@Composable
fun MobileContentRow(
    title: String,
    items: List<JellyfinItem>,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: ((JellyfinItem) -> Unit)? = null,
    accentColor: Color = MobileRowColors.Default,
    isWideCard: Boolean = false,
    showLabels: Boolean = true,
    isUpcomingEpisodes: Boolean = false
) {
    if (items.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenSizeClass = getScreenSizeClass(configuration.screenWidthDp)
    val horizontalPadding = getHorizontalPadding(screenSizeClass)
    val cardSpacing = getCardSpacing(screenSizeClass)

    Column(
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        // Row header with accent bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Horizontal scrolling cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing)
        ) {
            items(items, key = { it.id }) { item ->
                if (isWideCard) {
                    val imageUrl = when {
                        item.isEpisode -> {
                            jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary, maxWidth = 500)
                        }
                        !item.backdropImageTags.isNullOrEmpty() -> {
                            jellyfinClient.getBackdropUrl(item.id, item.backdropImageTags?.firstOrNull(), maxWidth = 500)
                        }
                        else -> {
                            jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary, maxWidth = 500)
                        }
                    }
                    MobileWideMediaCard(
                        item = item,
                        imageUrl = imageUrl,
                        onClick = { onItemClick(item.id) },
                        onLongClick = onItemLongClick?.let { { it(item) } },
                        showLabel = showLabels,
                        screenSizeClass = screenSizeClass
                    )
                } else {
                    // For portrait cards: use series poster for episodes
                    val imageUrl = if (item.isEpisode && item.seriesId != null) {
                        jellyfinClient.getPrimaryImageUrl(item.seriesId!!, null)
                    } else {
                        jellyfinClient.getPrimaryImageUrl(item.id, item.imageTags?.primary)
                    }
                    MobileMediaCard(
                        item = item,
                        imageUrl = imageUrl,
                        onClick = { onItemClick(item.id) },
                        onLongClick = onItemLongClick?.let { { it(item) } },
                        showLabel = showLabels,
                        screenSizeClass = screenSizeClass,
                        isUpcomingEpisode = isUpcomingEpisodes
                    )
                }
            }
        }
    }
}

/**
 * Data class for building content rows.
 */
data class MobileRowData(
    val key: String,
    val title: String,
    val items: List<JellyfinItem>,
    val isWideCard: Boolean = false,
    val accentColor: Color = MobileRowColors.Default,
    val isUpcomingEpisodes: Boolean = false
)
