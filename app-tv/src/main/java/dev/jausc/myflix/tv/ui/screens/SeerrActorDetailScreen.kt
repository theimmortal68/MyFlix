@file:Suppress(
    "LongMethod",
    "CognitiveComplexMethod",
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import dev.jausc.myflix.core.seerr.SeerrClient
import dev.jausc.myflix.core.seerr.SeerrPerson
import dev.jausc.myflix.core.seerr.SeerrPersonCastCredit
import dev.jausc.myflix.tv.ui.components.TvLoadingIndicator
import dev.jausc.myflix.tv.ui.theme.TvColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var isLoading by remember { mutableStateOf(true) }
    var person by remember { mutableStateOf<SeerrPerson?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val backButtonFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }

    // Load person details
    LaunchedEffect(personId) {
        isLoading = true
        errorMessage = null

        seerrClient.getPerson(personId)
            .onSuccess { loadedPerson ->
                // If combinedCredits is null, fetch them separately
                if (loadedPerson.combinedCredits == null) {
                    seerrClient.getPersonCombinedCredits(personId)
                        .onSuccess { credits ->
                            person = loadedPerson.copy(combinedCredits = credits)
                        }
                        .onFailure {
                            // Still show person even if credits fail
                            person = loadedPerson
                        }
                } else {
                    person = loadedPerson
                }
            }
            .onFailure { errorMessage = it.message }

        isLoading = false
    }

    // Request focus on first card when content loads
    LaunchedEffect(person) {
        if (person != null) {
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
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TvLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            errorMessage != null && person == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Failed to load person",
                        color = TvColors.Error,
                    )
                }
            }

            person != null -> {
                val currentPerson = person!!
                val castCredits = currentPerson.combinedCredits?.cast
                    ?.sortedByDescending { it.voteAverage ?: 0.0 }
                    ?: emptyList()

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
                            modifier = if (isFirst) {
                                Modifier.focusRequester(firstCardFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonHeader(
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
                modifier = Modifier.focusRequester(backButtonFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.Surface,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TvColors.TextPrimary,
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
            val birthInfo = buildList {
                person.birthday?.let { birthday ->
                    val formattedDate = try {
                        val date = LocalDate.parse(birthday)
                        "Born ${date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))}"
                    } catch (_: Exception) {
                        "Born $birthday"
                    }
                    add(formattedDate)
                }
                person.placeOfBirth?.let { add(it) }
            }.joinToString(" | ")

            if (birthInfo.isNotEmpty()) {
                Text(
                    text = birthInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                )
            }

            // Also known as
            person.alsoKnownAs?.takeIf { it.isNotEmpty() }?.let { names ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Also known as: ${names.take(3).joinToString(", ")}",
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
