@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

/**
 * Dialog for season options when long-pressing a season poster.
 * Shows option to mark the season as watched or unwatched.
 * Styled to match LibrarySortDialog.
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
    val focusRequester = remember { FocusRequester() }

    // Prevent immediate clicks from long press event propagation
    var isClickEnabled by remember { mutableStateOf(false) }

    // Delay focus and enable clicks after a short delay to prevent
    // the long press event from being interpreted as a click
    LaunchedEffect(Unit) {
        delay(200) // Wait for long press event to complete
        focusRequester.requestFocus()
        isClickEnabled = true
    }

    TvCenteredPopup(
        visible = true,
        onDismiss = onDismiss,
        minWidth = 280.dp,
        maxWidth = 320.dp,
        modifier = modifier,
    ) {
        Column {
            Text(
                text = season.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Show appropriate option based on current state
            if (isWatched) {
                // Season is watched - show option to mark as unwatched
                Surface(
                    onClick = {
                        if (isClickEnabled) {
                            onMarkUnwatched()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White.copy(alpha = 0.9f),
                        )
                        Text(
                            text = "Mark as Unwatched",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            } else {
                // Season is not watched - show option to mark as watched
                Surface(
                    onClick = {
                        if (isClickEnabled) {
                            onMarkWatched()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White.copy(alpha = 0.9f),
                        )
                        Text(
                            text = "Mark as Watched",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }
        }
    }
}
