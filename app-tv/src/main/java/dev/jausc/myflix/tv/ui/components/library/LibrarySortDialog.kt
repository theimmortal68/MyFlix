@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Dialog for selecting library sort option and order.
 */
@Composable
fun LibrarySortDialog(
    currentSortBy: LibrarySortOption,
    currentSortOrder: SortOrder,
    onSortSelected: (LibrarySortOption, SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(TvColors.Surface, MaterialTheme.shapes.medium)
                .padding(16.dp),
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Sort options
            LibrarySortOption.entries.forEachIndexed { index, option ->
                val isSelected = option == currentSortBy
                Surface(
                    onClick = {
                        onSortSelected(option, currentSortOrder)
                    },
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                    shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) {
                            TvColors.BluePrimary.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        },
                        focusedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TvColors.BluePrimary else TvColors.TextPrimary,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TvColors.BluePrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort order section
            Text(
                text = "Order",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SortOrder.entries.forEach { order ->
                val isSelected = order == currentSortOrder
                Surface(
                    onClick = {
                        onSortSelected(currentSortBy, order)
                    },
                    shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) {
                            TvColors.BluePrimary.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        },
                        focusedContainerColor = TvColors.BluePrimary.copy(alpha = 0.3f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = order.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TvColors.BluePrimary else TvColors.TextPrimary,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TvColors.BluePrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.colors(
                    containerColor = TvColors.SurfaceElevated,
                    contentColor = TvColors.TextPrimary,
                ),
            ) {
                Text("Close")
            }
        }
    }
}
