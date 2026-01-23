@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.LibrarySortOption
import dev.jausc.myflix.core.common.model.SortOrder
import dev.jausc.myflix.tv.ui.components.TvCenteredPopup
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

    TvCenteredPopup(
        visible = true,
        onDismiss = onDismiss,
        minWidth = 280.dp,
        maxWidth = 350.dp,
    ) {
        Column {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sort options
            LibrarySortOption.entries.forEachIndexed { index, option ->
                val isSelected = option == currentSortBy
                Surface(
                    onClick = {
                        onSortSelected(option, currentSortOrder)
                    },
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) {
                            Color.White.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        },
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TvColors.BluePrimary else Color.White.copy(alpha = 0.9f),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Sort order section
            Text(
                text = "Order",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SortOrder.entries.forEach { order ->
                val isSelected = order == currentSortOrder
                Surface(
                    onClick = {
                        onSortSelected(currentSortBy, order)
                    },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) {
                            Color.White.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        },
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = order.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TvColors.BluePrimary else Color.White.copy(alpha = 0.9f),
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
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White,
                    focusedContainerColor = TvColors.BluePrimary,
                    focusedContentColor = Color.White,
                ),
            ) {
                Text("Close")
            }
        }
    }
}
