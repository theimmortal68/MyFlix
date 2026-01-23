@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * TV-friendly dropdown menu that anchors to its parent.
 * Uses Material3 DropdownMenu with TV focus styling.
 */
@Composable
fun TvDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable () -> Unit,
) {
    val menuShape = MaterialTheme.shapes.medium
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .heightIn(max = 380.dp)
            .clip(menuShape),
        offset = offset,
        containerColor = TvColors.Surface,
        content = { content() },
    )
}

/**
 * Dropdown menu item with TV-friendly focus styling.
 * Changes background color when focused for D-pad navigation visibility.
 */
@Composable
fun TvDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    onLeftPressed: (() -> Unit)? = null,
    onRightPressed: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        focused -> TvColors.BluePrimary
        isSelected -> TvColors.BluePrimary.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val contentColor = when {
        focused -> Color.White
        isSelected -> TvColors.BluePrimary
        else -> TvColors.TextPrimary
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        DropdownMenuItem(
            text = {
                Text(
                    text = text,
                    color = contentColor,
                    style = textStyle,
                )
            },
            onClick = onClick,
            modifier = modifier
                .background(backgroundColor)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        if (onLeftPressed != null) {
                            onLeftPressed()
                            true
                        } else {
                            false
                        }
                    } else if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                        if (onRightPressed != null) {
                            onRightPressed()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
                .focusProperties {
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                },
            enabled = enabled,
            leadingIcon = leadingIcon?.let { icon ->
                {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        icon()
                    }
                }
            },
            trailingIcon = trailingIcon?.let { icon ->
                {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        icon()
                    }
                }
            },
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            colors = MenuDefaults.itemColors(
                textColor = contentColor,
                leadingIconColor = contentColor,
                trailingIconColor = contentColor,
            ),
        )
    }
}

/**
 * Dropdown menu item with a checkmark for selected state.
 */
@Composable
fun TvDropdownMenuItemWithCheck(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    onLeftPressed: (() -> Unit)? = null,
    onRightPressed: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    TvDropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        isSelected = isSelected,
        textStyle = textStyle,
        onLeftPressed = onLeftPressed,
        onRightPressed = onRightPressed,
        interactionSource = interactionSource,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
    )
}
