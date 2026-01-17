@file:Suppress("MagicNumber")

package dev.jausc.myflix.mobile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.viewmodel.PersonDetailViewModel
import dev.jausc.myflix.core.network.JellyfinClient
import dev.jausc.myflix.mobile.ui.components.MobileMediaCard
import dev.jausc.myflix.mobile.ui.components.detail.ItemRow
import dev.jausc.myflix.mobile.ui.components.detail.OverviewText

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

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.person == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = state.error ?: "Person not found")
                }
            }

            else -> {
                val person = state.person!!

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "header") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            AsyncImage(
                                model = jellyfinClient.getPersonImageUrl(
                                    person.id,
                                    person.imageTags?.primary,
                                    maxWidth = 300,
                                ),
                                contentDescription = person.name,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(MaterialTheme.shapes.large),
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )

                                person.overview?.let { overview ->
                                    OverviewText(
                                        overview = overview,
                                        maxLines = 6,
                                        onClick = {},
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
                                cardContent = { _, item, onClick, onLongClick ->
                                    if (item != null) {
                                        MobileMediaCard(
                                            item = item,
                                            imageUrl = jellyfinClient.getPrimaryImageUrl(
                                                item.id,
                                                item.imageTags?.primary,
                                            ),
                                            onClick = onClick,
                                            onLongClick = onLongClick,
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
    }
}
