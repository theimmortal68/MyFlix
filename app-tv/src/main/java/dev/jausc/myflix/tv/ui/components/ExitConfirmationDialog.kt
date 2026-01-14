@file:Suppress(
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Exit confirmation dialog shown when user presses Back on the home screen.
 *
 * This is a modal dialog that captures all input until dismissed.
 *
 * @param onConfirmExit Callback when user confirms exit
 * @param onCancel Callback when user cancels (stays in app)
 * @param modifier Modifier for the component
 */
@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cancelFocusRequester = remember { FocusRequester() }
    val scrimFocusRequester = remember { FocusRequester() }

    // Handle Back button to cancel
    BackHandler {
        onCancel()
    }

    // Focus cancel button by default (safer option)
    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    // Modal scrim that blocks all events from reaching content behind
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .focusRequester(scrimFocusRequester)
            .focusable(true)
            .onPreviewKeyEvent { event ->
                // Block all navigation keys from reaching content behind the dialog
                // Only allow Left/Right for button navigation and Enter for selection
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionRight,
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter,
                    -> false // Let these through to buttons
                    else -> true // Block everything else
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 400.dp),
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(
                containerColor = TvColors.SurfaceElevated,
            ),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Exit MyFlix?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TvColors.TextPrimary,
                )

                Text(
                    text = "Are you sure you want to exit the app?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextSecondary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Cancel button - focused by default
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .focusRequester(cancelFocusRequester)
                            .height(40.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = TvColors.SurfaceElevated,
                            contentColor = TvColors.TextPrimary,
                            focusedContainerColor = TvColors.BluePrimary,
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    // Exit button
                    Button(
                        onClick = onConfirmExit,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = TvColors.Error.copy(alpha = 0.8f),
                            contentColor = Color.White,
                            focusedContainerColor = TvColors.Error,
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "Exit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
