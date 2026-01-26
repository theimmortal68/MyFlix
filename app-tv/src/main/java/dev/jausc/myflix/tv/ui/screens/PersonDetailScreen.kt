@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.core.viewmodel.PersonDetailViewModel
import dev.jausc.myflix.tv.ui.components.DynamicBackground
import dev.jausc.myflix.tv.ui.components.MediaCard
import dev.jausc.myflix.tv.ui.components.detail.ItemRow
import dev.jausc.myflix.tv.ui.components.detail.OverviewText
import dev.jausc.myflix.tv.ui.theme.TvColors
import dev.jausc.myflix.tv.ui.util.rememberGradientColors

@Composable
fun PersonDetailScreen(
    personId: String,
    jellyfinClient: JellyfinClient,
    onItemClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val viewModel: PersonDetailViewModel = viewModel(
        key = personId,
        factory = PersonDetailViewModel.Factory(personId, jellyfinClient),
    )
    val state by viewModel.uiState.collectAsState()

    // Get person image URL for dynamic background
    val personImageUrl = remember(state.person?.id, state.person?.imageTags?.primary) {
        state.person?.let { person ->
            jellyfinClient.getPersonImageUrl(person.id, person.imageTags?.primary, maxWidth = 400)
        }
    }
    val gradientColors = rememberGradientColors(personImageUrl)

    // Layered UI: DynamicBackground → NavigationRail + Content
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Dynamic gradient background (covers full screen including nav rail)
        DynamicBackground(
            gradientColors = gradientColors,
            modifier = Modifier.fillMaxSize(),
        )

            // Content area
            Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Loading...",
                            color = TvColors.TextPrimary,
                        )
                    }
                }

                state.person == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error ?: "Person not found",
                            color = TvColors.TextPrimary,
                        )
                    }
                }

                else -> {
                    val person = state.person!!

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 10.dp, top = 16.dp, end = 32.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                    item(key = "header") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AsyncImage(
                                model = jellyfinClient.getPersonImageUrl(
                                    person.id,
                                    person.imageTags?.primary,
                                    maxWidth = 400,
                                ),
                                contentDescription = person.name,
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(MaterialTheme.shapes.large),
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = TvColors.TextPrimary,
                                )

                                person.overview?.let { overview ->
                                    OverviewText(
                                        overview = overview,
                                        maxLines = 6,
                                        onClick = {},
                                        showFocusBackground = false,
                                    )
                                }
                            }
                        }
                    }

                    if (state.credits.isNotEmpty()) {
                        item(key = "known_for") {
                            ItemRow(
                                title = "Known For",
                                items = state.credits,
                                onItemClick = { _, item -> onItemClick(item.id) },
                                onItemLongClick = { _, _ ->
                                    // TODO: Show item context menu
                                },
                                cardContent = { _, item, cardModifier, onClick, onLongClick ->
                                    if (item != null) {
                                        MediaCard(
                                            item = item,
                                            imageUrl = jellyfinClient.getPrimaryImageUrl(
                                                item.id,
                                                item.imageTags?.primary,
                                            ),
                                            onClick = onClick,
                                            onLongClick = onLongClick,
                                            modifier = cardModifier,
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
            } // End content Box
    } // End outer Box
}
