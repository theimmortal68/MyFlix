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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dev.jausc.myflix.core.seerr.SeerrRepository
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
    seerrRepository: SeerrRepository,
    onBack: () -> Unit,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
) {
    val loader = remember(seerrRepository) { SeerrPersonLoader.from(seerrRepository) }
    val state = rememberSeerrActorDetailScreenState(personId, loader)

    val backButtonFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }

    // NavRail exit focus registration
    val updateExitFocus = rememberExitFocusRegistry(firstCardFocusRequester)

    // Request focus on back button when content loads (top of screen)
    LaunchedEffect(state.person) {
        if (state.person != null) {
            kotlinx.coroutines.delay(100)
            try {
                backButtonFocusRequester.requestFocus()
            } catch (_: Exception) {
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
                    seerrRepository = seerrRepository,
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
    seerrRepository: SeerrRepository,
    onBack: () -> Unit,
    onMediaClick: (mediaType: String, tmdbId: Int) -> Unit,
    backButtonFocusRequester: FocusRequester,
    firstCardFocusRequester: FocusRequester,
    updateExitFocus: (FocusRequester) -> Unit,
) {
    val currentPerson = state.person ?: return
    val castCredits = state.sortedCastCredits
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 16.dp),
    ) {
        // Header section
        PersonHeader(
            state = state,
            person = currentPerson,
            seerrRepository = seerrRepository,
            onBack = onBack,
            backButtonFocusRequester = backButtonFocusRequester,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Appearances title - matching Home screen row header style
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(TvColors.BluePrimary, RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Appearances",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Appearances row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(
                castCredits,
                key = { _, credit -> "${credit.mediaType}_${credit.id}_${credit.character}" },
            ) { index, credit ->
                PersonMediaCard(
                    credit = credit,
                    seerrRepository = seerrRepository,
                    onClick = {
                        val mediaType = credit.mediaType ?: "movie"
                        onMediaClick(mediaType, credit.id)
                    },
                    modifier = (if (index == 0) {
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
}

@Composable
private fun PersonHeader(
    state: SeerrActorDetailScreenState,
    person: SeerrPerson,
    seerrRepository: SeerrRepository,
    onBack: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Profile photo - fills the height of the info column
            AsyncImage(
                model = seerrRepository.getProfileUrl(person.profilePath, "h632"),
                contentDescription = person.name,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillHeight,
            )

        // Info column
        Column(
            modifier = Modifier.weight(1f).padding(top = 36.dp, bottom = 6.dp),
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

            // Biography (auto-scrolls if too long)
            person.biography?.takeIf { it.isNotEmpty() }?.let { bio ->
                Spacer(modifier = Modifier.height(12.dp))
                AutoScrollingText(text = bio)
            }
        }
        }
    }
}

@Composable
private fun PersonMediaCard(
    credit: SeerrPersonCastCredit,
    seerrRepository: SeerrRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(110.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
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
                focusedScale = 1.05f,
            ),
        ) {
            AsyncImage(
                model = seerrRepository.getPosterUrl(credit.posterPath),
                contentDescription = credit.displayTitle,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
        }

        // Title below poster
        Text(
            text = credit.displayTitle,
            style = MaterialTheme.typography.labelSmall,
            color = TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )

        // Character name below title
        credit.character?.let { character ->
            Text(
                text = character,
                style = MaterialTheme.typography.labelSmall,
                color = TvColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 130,
    scrollDuration: Int = 20000,
) {
    val scrollState = rememberScrollState()
    var needsScroll by remember { mutableStateOf(false) }
    var textHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val maxHeightPx = with(density) { maxHeight.dp.toPx() }

    LaunchedEffect(needsScroll) {
        if (needsScroll && scrollState.maxValue > 0) {
            while (true) {
                kotlinx.coroutines.delay(3000)
                scrollState.animateScrollTo(
                    scrollState.maxValue,
                    animationSpec = tween(durationMillis = scrollDuration, easing = LinearEasing),
                )
                kotlinx.coroutines.delay(3000)
                scrollState.scrollTo(0)
            }
        }
    }

    val dynamicHeight = with(density) {
        if (textHeight > 0 && textHeight <= maxHeightPx.toInt()) {
            textHeight.toDp()
        } else {
            maxHeight.dp
        }
    }

    Box(
        modifier = modifier
            .height(dynamicHeight)
            .clipToBounds(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.TextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier
                .verticalScroll(scrollState)
                .onSizeChanged { size ->
                    textHeight = size.height
                    needsScroll = size.height > maxHeightPx
                },
        )
    }
}
