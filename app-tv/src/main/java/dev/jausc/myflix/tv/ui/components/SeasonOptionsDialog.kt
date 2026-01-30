@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Dialog for season options when long-pressing a season poster.
 * Shows options to mark the season as watched or unwatched.
 *
 * @param season The season item to show options for
 * @param onMarkWatched Callback when user selects "Mark as Watched"
 * @param onMarkUnwatched Callback when user selects "Mark as Unwatched"
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun SeasonOptionsDialog(
    season: JellyfinItem,
    onMarkWatched: () -> Unit,
    onMarkUnwatched: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWatched = season.userData?.played == true
    val primaryFocusRequester = remember { FocusRequester() }

    // Focus the primary action button
    LaunchedEffect(Unit) {
        primaryFocusRequester.requestFocus()
    }

    TvCenteredPopup(
        visible = true,
        onDismiss = onDismiss,
        minWidth = 300.dp,
        maxWidth = 350.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = season.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Show appropriate option based on current state
            if (isWatched) {
                // Season is watched - show option to mark as unwatched
                Button(
                    onClick = {
                        onMarkUnwatched()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(primaryFocusRequester)
                        .focusProperties {
                            up = FocusRequester.Cancel
                            down = FocusRequester.Cancel
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                        },
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = "Mark as Unwatched",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            } else {
                // Season is not watched - show option to mark as watched
                Button(
                    onClick = {
                        onMarkWatched()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(primaryFocusRequester)
                        .focusProperties {
                            up = FocusRequester.Cancel
                            down = FocusRequester.Cancel
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                        },
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = "Mark as Watched",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
