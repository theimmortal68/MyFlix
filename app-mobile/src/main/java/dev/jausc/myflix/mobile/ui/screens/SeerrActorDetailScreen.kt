@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.ui.SeerrActorDetailScreenState
import dev.jausc.myflix.core.common.ui.SeerrPersonLoader
import dev.jausc.myflix.core.common.ui.rememberSeerrActorDetailScreenState
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrPerson
import dev.jausc.myflix.core.seerr.SeerrPersonCastCredit

/**
 * Mobile Seerr actor/person detail screen.
 *
 * Features:
 * - Actor photo and biography
 * - Birth info and also known as names
 * - Grid of movies/shows they appeared in
 */
@Composable
fun SeerrActorDetailScreen(
    personId: Int,
    seerrClient: SeerrClient,
    onBack: () -> Unit,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    val loader = remember(seerrClient) { SeerrPersonLoader.from(seerrClient) }
    val state = rememberSeerrActorDetailScreenState(personId, loader)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = state.person?.name ?: "Actor Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            }

            state.error != null && state.person == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.error ?: "Failed to load person",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            state.person != null -> {
                MobileActorDetailContent(
                    state = state,
                    seerrClient = seerrClient,
                    onMediaClick = onMediaClick,
                )
            }
        }
    }
}

@Composable
private fun MobileActorDetailContent(
    state: SeerrActorDetailScreenState,
    seerrClient: SeerrClient,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    val currentPerson = state.person ?: return
    val castCredits = state.sortedCastCredits

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 32.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header section (full width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            MobilePersonHeader(
                state = state,
                person = currentPerson,
                seerrClient = seerrClient,
            )
        }

        // Appearances title
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Appearances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
        }

        // Movie/Show grid
        items(
            castCredits,
            key = { "${it.mediaType}_${it.id}_${it.character}" },
        ) { credit ->
            MobilePersonMediaCard(
                credit = credit,
                seerrClient = seerrClient,
                onClick = {
                    val mediaType = credit.mediaType ?: "movie"
                    onMediaClick(mediaType, credit.id)
                },
            )
        }
    }
}

@Composable
private fun MobilePersonHeader(state: SeerrActorDetailScreenState, person: SeerrPerson, seerrClient: SeerrClient,) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Profile photo
            AsyncImage(
                model = seerrClient.getProfileUrl(person.profilePath, "h632"),
                contentDescription = person.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            // Info column
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Name
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Birth info
                state.getBirthInfo(separator = "\n")?.let { birthInfo ->
                    Text(
                        text = birthInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Also known as
                person.formattedAlsoKnownAs?.let { alsoKnownAs ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alsoKnownAs,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Biography (separate row for full width)
        person.biography?.takeIf { it.isNotEmpty() }?.let { bio ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MobilePersonMediaCard(credit: SeerrPersonCastCredit, seerrClient: SeerrClient, onClick: () -> Unit,) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box {
            AsyncImage(
                model = seerrClient.getPosterUrl(credit.posterPath),
                contentDescription = credit.displayTitle,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            // Character name badge at bottom
            credit.character?.let { character ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = character,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
