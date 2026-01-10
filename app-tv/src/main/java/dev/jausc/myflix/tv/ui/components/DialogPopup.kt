package dev.jausc.myflix.tv.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

/**
 * Sealed interface for dialog item entries.
 * Allows both regular items and dividers in the dialog list.
 */
sealed interface DialogItemEntry

/**
 * A divider in the dialog list.
 */
object DialogItemDivider : DialogItemEntry

/**
 * A single item in a long-press context dialog.
 *
 * @param text The display text for the item
 * @param icon Optional icon to show on the left
 * @param iconTint Color tint for the icon
 * @param enabled Whether the item can be selected
 * @param onClick Action to perform when selected
 */
data class DialogItem(
    val text: String,
    val icon: ImageVector? = null,
    val iconTint: Color = Color.White,
    val enabled: Boolean = true,
    val onClick: () -> Unit
) : DialogItemEntry

/**
 * Parameters for displaying a dialog popup.
 *
 * @param title The title shown at the top of the dialog
 * @param items List of dialog items to display
 * @param fromLongClick If true, applies a 1-second delay before enabling items
 *                      to prevent accidental selection from the long-press release
 */
data class DialogParams(
    val title: String,
    val items: List<DialogItemEntry>,
    val fromLongClick: Boolean = false
)

/**
 * Modal dialog popup for long-press context menus.
 * Displays a list of actions the user can perform on an item.
 *
 * Features:
 * - 1-second delay after long-press to prevent accidental selection
 * - Immediate re-enable when button is released
 * - D-pad navigation support
 * - Semi-transparent backdrop
 */
@Composable
fun DialogPopup(
    params: DialogParams,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        DialogPopupContent(
            title = params.title,
            items = params.items,
            waitToLoad = params.fromLongClick,
            onDismissRequest = onDismissRequest,
            modifier = modifier
        )
    }
}

@Composable
private fun DialogPopupContent(
    title: String,
    items: List<DialogItemEntry>,
    waitToLoad: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Anti-propagation: 1-second delay after long-press dialog appears
    // This prevents the long-press release from selecting the first item
    var waiting by remember { mutableStateOf(waitToLoad) }

    if (waitToLoad) {
        LaunchedEffect(Unit) {
            waiting = true
            delay(1000)
            waiting = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            // Detect key release to immediately re-enable items
            .onKeyEvent { event ->
                val code = event.nativeKeyEvent.keyCode
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                    code in setOf(
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER
                    )
                ) {
                    waiting = false
                }
                false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = modifier
                .widthIn(min = 300.dp, max = 400.dp)
                .background(
                    TvColors.Surface,
                    RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Items list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items) { item ->
                    when (item) {
                        is DialogItemDivider -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(TvColors.TextSecondary.copy(alpha = 0.2f))
                                    .padding(vertical = 0.5.dp)
                            )
                        }
                        is DialogItem -> {
                            DialogListItem(
                                item = item,
                                enabled = !waiting && item.enabled,
                                onClick = {
                                    item.onClick()
                                    onDismissRequest()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogListItem(
    item: DialogItem,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        selected = false,
        onClick = onClick,
        enabled = enabled,
        headlineContent = {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) TvColors.TextPrimary else TvColors.TextSecondary
            )
        },
        leadingContent = item.icon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) item.iconTint else TvColors.TextSecondary
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvColors.FocusedSurface,
            selectedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        shape = ListItemDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
