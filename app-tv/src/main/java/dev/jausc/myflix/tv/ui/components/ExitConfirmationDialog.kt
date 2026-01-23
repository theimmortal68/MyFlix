@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Netflix-style exit confirmation dialog shown when user presses Back on the home screen.
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
    val exitFocusRequester = remember { FocusRequester() }

    // Focus cancel button by default (safer option)
    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    TvCenteredPopup(
        visible = true,
        onDismiss = onCancel,
        minWidth = 320.dp,
        maxWidth = 380.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Exit MyFlix?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Are you sure you want to exit the app?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Cancel button - focused by default
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(cancelFocusRequester)
                        .focusProperties {
                            left = FocusRequester.Cancel
                            right = exitFocusRequester
                        },
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White,
                        focusedContainerColor = TvColors.BluePrimary,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                // Exit button
                Button(
                    onClick = onConfirmExit,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(exitFocusRequester)
                        .focusProperties {
                            left = cancelFocusRequester
                            right = FocusRequester.Cancel
                        },
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.Error.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        focusedContainerColor = TvColors.Error,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Exit",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
