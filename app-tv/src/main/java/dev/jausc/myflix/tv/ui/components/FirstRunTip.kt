@file:Suppress(
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * First-run tip component that informs new users about the auto-hiding navigation bar.
 *
 * Displays as a modal overlay with a scrim that blocks interaction with content below.
 * The tip card appears near the top of the screen (below the nav bar).
 *
 * @param visible Whether the tip is currently visible
 * @param onDismiss Callback when the user dismisses the tip (presses "Got it" or OK/Enter)
 * @param modifier Modifier for the component
 */
@Composable
fun FirstRunTip(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonFocusRequester = remember { FocusRequester() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        // Request focus after animation completes
        LaunchedEffect(Unit) {
            // Small delay to ensure composable is fully laid out
            kotlinx.coroutines.delay(100)
            buttonFocusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Tip card - positioned below where the nav bar would be
            Surface(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .widthIn(max = 400.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = TvColors.SurfaceElevated,
                ),
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header with icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = TvColors.BluePrimary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = "Tip",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TvColors.TextPrimary,
                        )
                    }

                    // Tip message
                    Text(
                        text = "Press UP to show the navigation bar at any time.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Got it button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .focusRequester(buttonFocusRequester)
                            .height(36.dp)
                            .onPreviewKeyEvent { event ->
                                // Block back button while tip is showing
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.Back || event.key == Key.Escape)
                                ) {
                                    true // Consume the event
                                } else {
                                    false
                                }
                            },
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = TvColors.BluePrimary,
                            contentColor = Color.White,
                            focusedContainerColor = TvColors.BluePrimary,
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "Got it",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
