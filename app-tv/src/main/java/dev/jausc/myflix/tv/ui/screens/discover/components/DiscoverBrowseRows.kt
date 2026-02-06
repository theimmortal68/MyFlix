@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens.discover.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.core.seerr.SeerrNetwork
import dev.jausc.myflix.core.seerr.SeerrStudio
import dev.jausc.myflix.tv.ui.components.seerr.SeerrGenreCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrNetworkCard
import dev.jausc.myflix.tv.ui.components.seerr.SeerrStudioCard
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * BringIntoViewSpec that re-enables horizontal scrolling for browse row LazyRows.
 * The parent LazyColumn uses noOpBringIntoViewSpec which blocks all auto-scrolling;
 * this overrides it so cards scroll into view when focused.
 */
@OptIn(ExperimentalFoundationApi::class)
private val browseRowBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float {
        return when {
            offset < 0 -> offset
            offset + size > containerSize -> offset + size - containerSize
            else -> 0f
        }
    }
}

/**
 * Genre browse row for the Discover screen.
 * Shows genre cards with backdrop images and duotone filters.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverGenreBrowseRow(
    title: String,
    genres: List<SeerrGenre>,
    onGenreClick: (SeerrGenre) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF8B5CF6),
    onItemFocused: ((name: String, id: Int) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseRowTitle(title = title, accentColor = accentColor)

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides browseRowBringIntoViewSpec,
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(genres, key = { it.id }) { genre ->
                    SeerrGenreCard(
                        genre = genre,
                        onClick = { onGenreClick(genre) },
                        modifier = Modifier.onFocusChanged { state ->
                            if (state.hasFocus) onItemFocused?.invoke(genre.name, genre.id)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Studio browse row for the Discover screen.
 * Shows studio logo cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverStudioBrowseRow(
    title: String,
    studios: List<SeerrStudio>,
    onStudioClick: (SeerrStudio) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFBBF24),
    onItemFocused: ((name: String, id: Int) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseRowTitle(title = title, accentColor = accentColor)

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides browseRowBringIntoViewSpec,
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(studios, key = { it.id }) { studio ->
                    SeerrStudioCard(
                        studio = studio,
                        onClick = { onStudioClick(studio) },
                        modifier = Modifier.onFocusChanged { state ->
                            if (state.hasFocus) onItemFocused?.invoke(studio.name, studio.id)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Network browse row for the Discover screen.
 * Shows network logo cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverNetworkBrowseRow(
    title: String,
    networks: List<SeerrNetwork>,
    onNetworkClick: (SeerrNetwork) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF34D399),
    onItemFocused: ((name: String, id: Int) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseRowTitle(title = title, accentColor = accentColor)

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides browseRowBringIntoViewSpec,
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(networks, key = { it.id }) { network ->
                    SeerrNetworkCard(
                        network = network,
                        onClick = { onNetworkClick(network) },
                        modifier = Modifier.onFocusChanged { state ->
                            if (state.hasFocus) onItemFocused?.invoke(network.name, network.id)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Title with accent bar, matching DiscoverCarousel's CategoryTitle.
 */
@Composable
private fun BrowseRowTitle(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(accentColor, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TvColors.TextPrimary,
        )
    }
}
