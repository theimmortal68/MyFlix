@file:Suppress(
    "MagicNumber",
)

package dev.jausc.myflix.tv.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
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
 * A section header in the dialog list.
 * Displays bold text to separate groups of items.
 */
data class DialogSectionHeader(val text: String) : DialogItemEntry

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
    val onClick: () -> Unit,
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
    val fromLongClick: Boolean = false,
)

/**
 * Netflix-style popup menu for context menus and actions.
 * Displays a list of actions the user can perform on an item.
 *
 * Features:
 * - Compact dark gradient styling
 * - Scale-in animation from center
 * - 1-second delay after long-press to prevent accidental selection
 * - D-pad navigation support
 */
@Composable
fun DialogPopup(
    params: DialogParams,
    onDismissRequest: () -> Unit,
    anchor: PopupAnchor? = null,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Anti-propagation: 1-second delay after long-press dialog appears
    var waiting by remember { mutableStateOf(params.fromLongClick) }

    if (params.fromLongClick) {
        LaunchedEffect(Unit) {
            waiting = true
            delay(1000)
            waiting = false
        }
    }

    // Focus first item when popup appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TvPopupContainer(
        visible = true,
        onDismiss = onDismissRequest,
        anchor = anchor,
        minWidth = 220.dp,
        maxWidth = 320.dp,
        maxHeight = 400.dp,
        modifier = modifier.onKeyEvent { event ->
            // Detect key release to immediately re-enable items
            val code = event.nativeKeyEvent.keyCode
            if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                code in setOf(
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                )
            ) {
                waiting = false
            }
            false
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Title
            Text(
                text = params.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Items list
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(params.items) { item ->
                    when (item) {
                        is DialogItemDivider -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                            )
                        }
                        is DialogSectionHeader -> {
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        is DialogItem -> {
                            val itemFocusRequester = if (params.items.filterIsInstance<DialogItem>().firstOrNull() == item) {
                                focusRequester
                            } else {
                                remember { FocusRequester() }
                            }
                            DialogMenuItem(
                                item = item,
                                enabled = !waiting && item.enabled,
                                onClick = {
                                    item.onClick()
                                    onDismissRequest()
                                },
                                modifier = Modifier.focusRequester(itemFocusRequester),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogMenuItem(
    item: DialogItem,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
            disabledContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            item.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) item.iconTint else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = if (enabled) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.4f),
            )
        }
    }
}
