package dev.jausc.myflix.tv.ui.components.navrail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.components.NavItem
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Individual navigation rail item with icon, optional label, and focus effects.
 *
 * @param item The navigation item data
 * @param isSelected Whether this item represents the current screen
 * @param isExpanded Whether the rail is expanded (shows labels)
 * @param isActive Whether the rail is active (item can receive focus)
 * @param labelAlpha Alpha for the label animation
 * @param onClick Called when item is selected (Enter pressed)
 * @param onExitRight Called when user presses Right to exit rail
 * @param modifier Modifier for the item
 * @param focusRequester FocusRequester for this item
 * @param upFocusRequester Target for Up navigation
 * @param downFocusRequester Target for Down navigation
 * @param rightFocusRequester Target for Right navigation (exit rail)
 */
@Composable
fun NavRailItem(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    isActive: Boolean,
    labelAlpha: Float,
    onClick: () -> Unit,
    onExitRight: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    upFocusRequester: FocusRequester = FocusRequester.Cancel,
    downFocusRequester: FocusRequester = FocusRequester.Cancel,
    rightFocusRequester: FocusRequester = FocusRequester.Default,
) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) NavRailAlpha.FocusedHalo else 0f,
        animationSpec = tween(durationMillis = NavRailAnimations.HaloDurationMs),
        label = "haloAlpha",
    )

    val haloScale by animateFloatAsState(
        targetValue = if (isFocused) 1f else NavRailScale.UnfocusedHalo,
        animationSpec = tween(durationMillis = NavRailAnimations.HaloDurationMs),
        label = "haloScale",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) NavRailScale.FocusedIcon else 1f,
        animationSpec = tween(durationMillis = NavRailAnimations.IconScaleDurationMs),
        label = "iconScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NavRailDimensions.ItemHeight)
            .padding(horizontal = NavRailDimensions.ItemHorizontalPadding)
            .clip(RoundedCornerShape(NavRailDimensions.ItemCornerRadius))
            .background(Color.Transparent)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusProperties {
                up = upFocusRequester
                down = downFocusRequester
                left = FocusRequester.Cancel
                // Right uses explicit requester if registered, else Default for natural traversal
                right = rightFocusRequester
                canFocus = isActive
            }
            .focusable()
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Enter, Key.DirectionCenter -> {
                        if (event.type == KeyEventType.KeyDown) {
                            onClick()
                        }
                        // Consume both KeyDown and KeyUp to prevent event leaking to other
                        // components after focus shifts during navigation
                        true
                    }
                    Key.DirectionRight -> {
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        // Collapse/deactivate NavRail first
                        onExitRight()
                        // If screen registered explicit focus target, use it
                        // Otherwise let focusProperties handle with Default traversal
                        if (rightFocusRequester != FocusRequester.Default) {
                            try {
                                rightFocusRequester.requestFocus()
                            } catch (_: IllegalStateException) {
                                // Ignore if not attached
                            }
                            true // Consume event - we handled focus explicitly
                        } else {
                            false // Let focusProperties handle with Default traversal
                        }
                    }
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        NavRailItemIcon(
            item = item,
            isSelected = isSelected,
            isFocused = isFocused,
            haloAlpha = haloAlpha,
            haloScale = haloScale,
            iconScale = iconScale,
        )

        if (isExpanded) {
            NavRailItemLabel(
                item = item,
                isSelected = isSelected,
                isFocused = isFocused,
                labelAlpha = labelAlpha,
            )
        }
    }
}

@Composable
private fun NavRailItemIcon(
    item: NavItem,
    isSelected: Boolean,
    isFocused: Boolean,
    haloAlpha: Float,
    haloScale: Float,
    iconScale: Float,
) {
    Box(
        modifier = Modifier.size(NavRailDimensions.IconContainerSize),
        contentAlignment = Alignment.Center,
    ) {
        // Outer halo glow
        Box(
            modifier = Modifier
                .size(NavRailDimensions.HaloOuterSize)
                .scale(haloScale)
                .alpha(haloAlpha * NavRailAlpha.HaloOuter)
                .blur(NavRailDimensions.HaloOuterBlur)
                .clip(CircleShape)
                .background(item.color),
        )

        // Inner halo glow
        Box(
            modifier = Modifier
                .size(NavRailDimensions.HaloInnerSize)
                .scale(haloScale)
                .alpha(haloAlpha * NavRailAlpha.HaloInner)
                .blur(NavRailDimensions.HaloInnerBlur)
                .clip(CircleShape)
                .background(item.color),
        )

        // Selection indicator (when selected but not focused)
        if (isSelected && !isFocused) {
            Box(
                modifier = Modifier
                    .size(NavRailDimensions.SelectionIndicatorSize)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = NavRailAlpha.SelectionIndicator)),
            )
        }

        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(NavRailDimensions.IconSize)
                .scale(iconScale),
            tint = getIconTint(item, isFocused, isSelected),
        )
    }
}

@Composable
private fun NavRailItemLabel(
    item: NavItem,
    isSelected: Boolean,
    isFocused: Boolean,
    labelAlpha: Float,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(NavRailDimensions.LabelSpacing))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
            color = getIconTint(item, isFocused, isSelected).copy(alpha = labelAlpha),
            modifier = Modifier.alpha(labelAlpha),
        )
    }
}

private fun getIconTint(item: NavItem, isFocused: Boolean, isSelected: Boolean): Color {
    return when {
        isFocused -> item.color
        isSelected -> item.color.copy(alpha = NavRailAlpha.SelectedUnfocused)
        else -> TvColors.TextSecondary
    }
}
