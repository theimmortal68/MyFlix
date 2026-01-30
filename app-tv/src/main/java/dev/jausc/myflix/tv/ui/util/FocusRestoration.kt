package dev.jausc.myflix.tv.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester

/**
 * CompositionLocal holding the current screen's exit focus requester.
 * NavRail reads this to know where to send focus when exiting right.
 */
val LocalExitFocusState = compositionLocalOf<MutableState<FocusRequester?>> {
    mutableStateOf(null)
}

/**
 * Registers a screen's focus target with the global NavRail exit focus system.
 *
 * @param primaryRequester The default/fallback focus target for this screen.
 *                         Registered immediately so NavRail exit works even before user focuses anything.
 * @return A lambda to call whenever focus changes to update the exit target.
 */
@Composable
fun rememberExitFocusRegistry(
    primaryRequester: FocusRequester,
): (FocusRequester) -> Unit {
    val exitFocusState = LocalExitFocusState.current
    DisposableEffect(primaryRequester) {
        // Register immediately so exit-right works even before any focus changes
        exitFocusState.value = primaryRequester
        onDispose { exitFocusState.value = null }
    }
    return { requester -> exitFocusState.value = requester }
}
