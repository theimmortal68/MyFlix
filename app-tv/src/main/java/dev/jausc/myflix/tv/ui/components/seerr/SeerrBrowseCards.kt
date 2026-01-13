@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.seerr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.GenreBackdropColors
import dev.jausc.myflix.core.seerr.PopularNetworks
import dev.jausc.myflix.core.seerr.PopularStudios
import dev.jausc.myflix.core.seerr.SeerrGenre
import dev.jausc.myflix.core.seerr.SeerrNetwork
import dev.jausc.myflix.core.seerr.SeerrStudio
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Card for displaying a studio logo with duotone filter.
 * Clicking navigates to studio discover page.
 */
@Composable
fun SeerrStudioCard(
    studio: SeerrStudio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoUrl = studio.logoPath?.let { PopularStudios.getLogoUrl(it) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .aspectRatio(16f / 9f),
        colors = CardDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.SurfaceLight,
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = studio.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = studio.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

/**
 * Card for displaying a network logo with duotone filter.
 * Clicking navigates to network discover page.
 */
@Composable
fun SeerrNetworkCard(
    network: SeerrNetwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoUrl = network.logoPath?.let { PopularNetworks.getLogoUrl(it) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .aspectRatio(16f / 9f),
        colors = CardDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.SurfaceLight,
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = network.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = network.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

/**
 * Card for displaying a genre with backdrop image using duotone filter.
 * Clicking navigates to genre discover page.
 */
@Composable
fun SeerrGenreCard(
    genre: SeerrGenre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Get the first backdrop from the genre's backdrops list
    val backdropPath = genre.backdrops?.firstOrNull()
    val backdropUrl = backdropPath?.let { GenreBackdropColors.getBackdropUrl(it, genre.id) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(200.dp)
            .aspectRatio(16f / 9f),
        colors = CardDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.SurfaceLight,
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        ) {
            // Background image with duotone filter
            if (backdropUrl != null) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Fallback gradient based on genre color
                val (dark, light) = GenreBackdropColors.getColorPair(genre.id)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(android.graphics.Color.parseColor("#$dark")),
                                    Color(android.graphics.Color.parseColor("#$light")),
                                ),
                            ),
                        ),
                )
            }

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                            ),
                        ),
                    ),
            )

            // Genre name
            Text(
                text = genre.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
    }
}

/**
 * Horizontal row of studio cards.
 */
@Composable
fun SeerrStudiosRow(
    studios: List<SeerrStudio>,
    onStudioClick: (SeerrStudio) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Studios",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(start = 48.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(studios, key = { it.id }) { studio ->
                SeerrStudioCard(
                    studio = studio,
                    onClick = { onStudioClick(studio) },
                )
            }
        }
    }
}

/**
 * Horizontal row of network cards.
 */
@Composable
fun SeerrNetworksRow(
    networks: List<SeerrNetwork>,
    onNetworkClick: (SeerrNetwork) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Networks",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(start = 48.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(networks, key = { it.id }) { network ->
                SeerrNetworkCard(
                    network = network,
                    onClick = { onNetworkClick(network) },
                )
            }
        }
    }
}

/**
 * Horizontal row of genre cards with backdrops.
 */
@Composable
fun SeerrGenresRow(
    genres: List<SeerrGenre>,
    onGenreClick: (SeerrGenre) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Genres",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextPrimary,
            modifier = Modifier.padding(start = 48.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(genres, key = { it.id }) { genre ->
                SeerrGenreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre) },
                )
            }
        }
    }
}
