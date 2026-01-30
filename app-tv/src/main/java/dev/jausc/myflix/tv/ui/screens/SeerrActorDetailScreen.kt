@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.common.ui.SeerrActorDetailScreenState
import dev.jausc.myflix.core.common.ui.SeerrPersonLoader
import dev.jausc.myflix.core.common.ui.rememberSeerrActorDetailScreenState
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrPerson
import dev.jausc.myflix.core.seerr.SeerrPersonCastCredit
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberExitFocusRegistry

/**
 * Seerr actor/person detail screen for TV.
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

    val backButtonFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }

    // NavRail exit focus registration
    val updateExitFocus = rememberExitFocusRegistry(firstCardFocusRequester)

    // Request focus on first card when content loads
    LaunchedEffect(state.person) {
        if (state.person != null) {
            kotlinx.coroutines.delay(100)
            try {
                firstCardFocusRequester.requestFocus()
            } catch (_: Exception) {
                try {
                    backButtonFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            state.error != null && state.person == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.error ?: "Failed to load person",
                        color = TvColors.Error,
                    )
                }
            }

            state.person != null -> {
                TvActorDetailContent(
                    state = state,
                    seerrClient = seerrClient,
                    onBack = onBack,
                    onMediaClick = onMediaClick,
                    backButtonFocusRequester = backButtonFocusRequester,
                    firstCardFocusRequester = firstCardFocusRequester,
                    updateExitFocus = updateExitFocus,
                )
            }
        }
    }
}

@Composable
private fun TvActorDetailContent(
    state: SeerrActorDetailScreenState,
    seerrClient: SeerrClient,
    onBack: () -> Unit,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    backButtonFocusRequester: FocusRequester,
    firstCardFocusRequester: FocusRequester,
    updateExitFocus: (FocusRequester) -> Unit,
) {
    val currentPerson = state.person ?: return
    val castCredits = state.sortedCastCredits

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 48.dp,
            end = 48.dp,
            top = 24.dp,
            bottom = 48.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header section (full width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            PersonHeader(
                state = state,
                person = currentPerson,
                seerrClient = seerrClient,
                onBack = onBack,
                backButtonFocusRequester = backButtonFocusRequester,
            )
        }

        // Appearances title
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Appearances",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
        }

        // Movie/Show grid
        items(
            castCredits,
            key = { "${it.mediaType}_${it.id}_${it.character}" },
        ) { credit ->
            val isFirst = castCredits.indexOf(credit) == 0
            PersonMediaCard(
                credit = credit,
                seerrClient = seerrClient,
                onClick = {
                    val mediaType = credit.mediaType ?: "movie"
                    onMediaClick(mediaType, credit.id)
                },
                modifier = (if (isFirst) {
                    Modifier.focusRequester(firstCardFocusRequester)
                } else {
                    Modifier
                }).onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        updateExitFocus(firstCardFocusRequester)
                    }
                },
            )
        }
    }
}

@Composable
private fun PersonHeader(
    state: SeerrActorDetailScreenState,
    person: SeerrPerson,
    seerrClient: SeerrClient,
    onBack: () -> Unit,
    backButtonFocusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Back button and photo column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .height(24.dp)
                    .focusRequester(backButtonFocusRequester),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                scale = ButtonDefaults.scale(focusedScale = 1f),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
                    contentColor = TvColors.TextPrimary,
                    focusedContainerColor = TvColors.BluePrimary,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(14.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile photo
            AsyncImage(
                model = seerrClient.getProfileUrl(person.profilePath, "h632"),
                contentDescription = person.name,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }

        // Info column
        Column(
            modifier = Modifier.weight(1f),
        ) {
            // Name
            Text(
                text = person.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Birth info
            state.getBirthInfo(separator = " | ")?.let { birthInfo ->
                Text(
                    text = birthInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                )
            }

            // Also known as
            person.formattedAlsoKnownAs?.let { alsoKnownAs ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alsoKnownAs,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextSecondary,
                )
            }

            // Biography
            person.biography?.takeIf { it.isNotEmpty() }?.let { bio ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PersonMediaCard(
    credit: SeerrPersonCastCredit,
    seerrClient: SeerrClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(120.dp)
            .aspectRatio(2f / 3f),
        shape = ClickableSurfaceDefaults.shape(
            shape = MaterialTheme.shapes.medium,
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
        ),
    ) {
        Box {
            AsyncImage(
                model = seerrClient.getPosterUrl(credit.posterPath),
                contentDescription = credit.displayTitle,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = character,
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
