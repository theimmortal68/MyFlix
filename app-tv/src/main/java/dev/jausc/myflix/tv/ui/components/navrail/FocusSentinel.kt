package dev.jausc.myflix.tv.ui.components.navrail

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Invisible focusable element that sits at the left edge of content.
 *
 * When focus lands here (user navigated left from content), it:
 * 1. Calls onActivate to activate the navigation rail
 * 2. Transfers focus to the rail after a brief delay
 *
 * This prevents the rail from stealing focus during screen loads while
 * still allowing natural left-edge navigation into the rail.
 *
 * @param onActivate Called when focus lands on sentinel (activates rail)
 * @param railFocusRequester FocusRequester to transfer focus to rail
 * @param modifier Modifier for the sentinel
 */
/**
 * @param isEnabled When false, sentinel cannot receive focus (rail is already active)
 * @param onActivate Called when focus lands on sentinel (activates rail)
 * @param railFocusRequester FocusRequester to transfer focus to rail
 * @param modifier Modifier for the sentinel
 */
/**
 * @param isEnabled When false, sentinel cannot receive focus (rail is already active)
 * @param onActivate Called when focus lands on sentinel (activates rail)
 * @param railFocusRequester FocusRequester to transfer focus to rail
 * @param sentinelFocusRequester FocusRequester for the sentinel itself, allowing content to
 *                              explicitly navigate left to it
 * @param modifier Modifier for the sentinel
 */
@Composable
fun FocusSentinel(
    isEnabled: Boolean,
    onActivate: () -> Unit,
    railFocusRequester: FocusRequester,
    sentinelFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    // Track if we're in the process of transferring focus to prevent canFocus from
    // becoming false during the transfer (which could cause Compose to move focus elsewhere)
    var isTransferring by remember { mutableStateOf(false) }

    // Transfer focus to rail when sentinel gains focus
    // This effect survives even if isEnabled changes, ensuring focus transfer completes
    LaunchedEffect(isFocused) {
        if (isFocused) {
            isTransferring = true
            onActivate()
            // Small delay to ensure rail is activated and focusable
            delay(NavRailAnimations.FocusTransferDelayMs)
            try {
                railFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // Focus requester not attached yet, ignore
            }
            isTransferring = false
        }
    }

    Box(
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight()
            .focusRequester(sentinelFocusRequester)
            .focusProperties {
                // Block left navigation - nothing to the left of sentinel
                left = FocusRequester.Cancel
                // Keep focusable during transfer, otherwise only when enabled
                canFocus = isEnabled || isTransferring
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable(),
    )
}
