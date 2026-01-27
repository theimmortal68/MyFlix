package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Dimension constants for the slide-out navigation rail.
 */
object SlideOutNavRailDimensions {
    val CollapsedWidth = 48.dp
    val ExpandedWidth = 200.dp
    val IconSize = 16.dp
    val ItemHeight = 32.dp // Match old NavRailItem size
    val ItemSpacing = 8.dp // Vertical gap between nav items
    val ItemHorizontalPadding = 4.dp // Reduced - was 8dp
    val IconContainerSize = 32.dp
    val HaloOuterSize = 28.dp
    val HaloInnerSize = 22.dp
    val SelectionIndicatorSize = 24.dp
    val LabelSpacing = 8.dp
    val VerticalPadding = 0.dp // Removed - was causing bottom offset
    val SettingsBottomPadding = 0.dp
    val SettingsSpaceReservation = 40.dp // Reduced to match smaller items
    val ItemCornerRadius = 8.dp
    val IconStartPadding = 0.dp // Removed - was adding extra left padding
    val HaloOuterBlur = 6.dp
    val HaloInnerBlur = 3.dp
}

/**
 * Animation constants for the slide-out navigation rail.
 */
object SlideOutNavRailAnimations {
    const val ExpandDurationMs = 300
    const val LabelFadeDelayMs = 100
    const val HaloDurationMs = 200
    const val IconScaleDurationMs = 150
    const val FocusedBackgroundAlpha = 0.0f // Removed to let halo effect shine
    const val ExpandedBackgroundAlpha = 0.88f // Slightly more transparent
    const val ExpandedBackgroundMidAlpha = 0.65f // Mid-point for gradual fade
    const val ExpandedBackgroundEdgeAlpha = 0.3f // Gradual fade to edge
    const val HaloOuterAlphaMultiplier = 0.7f // Stronger outer halo
    const val HaloInnerAlphaMultiplier = 0.5f // Stronger inner halo
    const val SelectionIndicatorAlpha = 0.2f
    const val SelectedUnfocusedAlpha = 0.9f
    const val FocusedHaloAlpha = 0.8f // More visible halo
    const val FocusedIconScale = 1.15f
    const val UnfocusedHaloScale = 0.5f
}

/**
 * Background color for the expanded navigation rail.
 */
private val NavRailBackgroundColor = Color.Black

/**
 * Builds the list of visible main navigation items based on feature flags.
 */
private fun buildMainNavItems(showUniverses: Boolean, showDiscover: Boolean): List<NavItem> {
    return buildList {
        add(NavItem.SEARCH)
        add(NavItem.HOME)
        add(NavItem.SHOWS)
        add(NavItem.MOVIES)
        add(NavItem.COLLECTIONS)
        if (showUniverses) add(NavItem.UNIVERSES)
        if (showDiscover) add(NavItem.DISCOVER)
    }
}

/**
 * Handles key events for the navigation rail container.
 */
private fun handleNavRailKeyEvent(
    event: KeyEvent,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCollapseAndFocusContent: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    return when (event.key) {
        Key.Back -> {
            if (isExpanded) {
                onCollapseAndFocusContent()
                true
            } else {
                false
            }
        }
        Key.Menu -> {
            onExpandedChange(!isExpanded)
            true
        }
        else -> {
            false
        }
    }
}

/**
 * Handles key events for individual navigation items.
 */
private fun handleNavItemKeyEvent(event: KeyEvent, onClick: () -> Unit, onRightPressed: () -> Unit): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    return when (event.key) {
        Key.Enter, Key.DirectionCenter -> {
            onClick()
            true
        }
        Key.DirectionRight -> {
            // Call callback to collapse, but return false to let focus system handle navigation
            onRightPressed()
            false
        }
        else -> {
            false
        }
    }
}

/**
 * Determines the icon tint color based on focus and selection state.
 */
private fun getIconTint(item: NavItem, isFocused: Boolean, isSelected: Boolean): Color {
    return when {
        isFocused -> item.color
        isSelected -> item.color.copy(alpha = SlideOutNavRailAnimations.SelectedUnfocusedAlpha)
        else -> TvColors.TextSecondary
    }
}

/**
 * Netflix-style slide-out navigation rail.
 *
 * - Collapsed: Shows only icons, fully transparent background
 * - Expanded: Shows icons + labels, semi-transparent gradient background
 * - Expands when any item receives focus
 * - Collapses when user selects an item, presses Right, or presses Back
 *
 * @param selectedItem Currently selected/highlighted navigation item
 * @param onItemSelected Callback when a nav item is selected (Enter pressed)
 * @param showUniverses Whether to show the Universes nav item
 * @param showDiscover Whether to show the Discover nav item
 * @param isExpanded External control for expansion state
 * @param onExpandedChange Callback when expansion state should change
 * @param onCollapseAndFocusContent Callback to collapse rail and return focus to content
 * @param modifier Modifier for the rail
 */
@Composable
fun SlideOutNavigationRail(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    showUniverses: Boolean = false,
    showDiscover: Boolean = false,
    isExpanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    onCollapseAndFocusContent: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mainItems = remember(showUniverses, showDiscover) {
        buildMainNavItems(showUniverses, showDiscover)
    }

    val mainFocusRequesters = remember(mainItems.size) {
        List(mainItems.size) { FocusRequester() }
    }
    val settingsFocusRequester = remember { FocusRequester() }

    var railHasFocus by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) {
            SlideOutNavRailDimensions.ExpandedWidth
        } else {
            SlideOutNavRailDimensions.CollapsedWidth
        },
        animationSpec = tween(durationMillis = SlideOutNavRailAnimations.ExpandDurationMs),
        label = "railWidth",
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isExpanded) SlideOutNavRailAnimations.ExpandedBackgroundAlpha else 0f,
        animationSpec = tween(durationMillis = SlideOutNavRailAnimations.ExpandDurationMs),
        label = "backgroundAlpha",
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = SlideOutNavRailAnimations.ExpandDurationMs,
            delayMillis = if (isExpanded) SlideOutNavRailAnimations.LabelFadeDelayMs else 0,
        ),
        label = "labelAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .background(createNavRailGradient(backgroundAlpha))
            .padding(vertical = SlideOutNavRailDimensions.VerticalPadding)
            .focusGroup()
            .onFocusChanged { focusState ->
                val hadFocus = railHasFocus
                railHasFocus = focusState.hasFocus
                if (!hadFocus && railHasFocus) {
                    onExpandedChange(true)
                }
            }
            .onKeyEvent { event ->
                handleNavRailKeyEvent(event, isExpanded, onExpandedChange, onCollapseAndFocusContent)
            },
    ) {
        NavRailMainItems(
            mainItems = mainItems,
            selectedItem = selectedItem,
            isExpanded = isExpanded,
            labelAlpha = labelAlpha,
            mainFocusRequesters = mainFocusRequesters,
            settingsFocusRequester = settingsFocusRequester,
            onItemSelected = onItemSelected,
            onExpandedChange = onExpandedChange,
            onCollapseAndFocusContent = onCollapseAndFocusContent,
        )

        NavRailSettingsItem(
            selectedItem = selectedItem,
            isExpanded = isExpanded,
            labelAlpha = labelAlpha,
            settingsFocusRequester = settingsFocusRequester,
            lastMainFocusRequester = mainFocusRequesters.lastOrNull(),
            onItemSelected = onItemSelected,
            onExpandedChange = onExpandedChange,
            onCollapseAndFocusContent = onCollapseAndFocusContent,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * Creates the gradient brush for the navigation rail background.
 * Uses 4 color stops for a gradual fade from opaque to transparent.
 */
private fun createNavRailGradient(backgroundAlpha: Float): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            NavRailBackgroundColor.copy(alpha = backgroundAlpha),
            NavRailBackgroundColor.copy(
                alpha = backgroundAlpha * SlideOutNavRailAnimations.ExpandedBackgroundMidAlpha
            ),
            NavRailBackgroundColor.copy(
                alpha = backgroundAlpha * SlideOutNavRailAnimations.ExpandedBackgroundEdgeAlpha
            ),
            Color.Transparent,
        ),
    )
}

/**
 * Renders the main navigation items (vertically centered).
 */
@Composable
private fun NavRailMainItems(
    mainItems: List<NavItem>,
    selectedItem: NavItem,
    isExpanded: Boolean,
    labelAlpha: Float,
    mainFocusRequesters: List<FocusRequester>,
    settingsFocusRequester: FocusRequester,
    onItemSelected: (NavItem) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onCollapseAndFocusContent: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(bottom = SlideOutNavRailDimensions.SettingsSpaceReservation),
        verticalArrangement = Arrangement.spacedBy(
            SlideOutNavRailDimensions.ItemSpacing,
            Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.Start,
    ) {
        mainItems.forEachIndexed { index, item ->
            SlideOutNavItem(
                item = item,
                isSelected = selectedItem == item,
                isExpanded = isExpanded,
                labelAlpha = labelAlpha,
                onClick = {
                    onItemSelected(item)
                    onExpandedChange(false)
                },
                onRightPressed = onCollapseAndFocusContent,
                modifier = Modifier.focusRequester(mainFocusRequesters[index]),
                upFocusRequester = mainFocusRequesters.getOrNull(index - 1) ?: FocusRequester.Cancel,
                downFocusRequester = mainFocusRequesters.getOrNull(index + 1) ?: settingsFocusRequester,
            )
        }
    }
}

/**
 * Renders the Settings item pinned to the bottom.
 */
@Composable
private fun NavRailSettingsItem(
    selectedItem: NavItem,
    isExpanded: Boolean,
    labelAlpha: Float,
    settingsFocusRequester: FocusRequester,
    lastMainFocusRequester: FocusRequester?,
    onItemSelected: (NavItem) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onCollapseAndFocusContent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(bottom = SlideOutNavRailDimensions.SettingsBottomPadding),
    ) {
        SlideOutNavItem(
            item = NavItem.SETTINGS,
            isSelected = selectedItem == NavItem.SETTINGS,
            isExpanded = isExpanded,
            labelAlpha = labelAlpha,
            onClick = {
                onItemSelected(NavItem.SETTINGS)
                onExpandedChange(false)
            },
            onRightPressed = onCollapseAndFocusContent,
            modifier = Modifier.focusRequester(settingsFocusRequester),
            upFocusRequester = lastMainFocusRequester ?: FocusRequester.Cancel,
            downFocusRequester = FocusRequester.Cancel,
        )
    }
}

/**
 * Individual navigation item with icon and optional label.
 */
@Composable
private fun SlideOutNavItem(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    labelAlpha: Float,
    onClick: () -> Unit,
    onRightPressed: () -> Unit,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester = FocusRequester.Cancel,
    downFocusRequester: FocusRequester = FocusRequester.Cancel,
) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) SlideOutNavRailAnimations.FocusedHaloAlpha else 0f,
        animationSpec = tween(durationMillis = SlideOutNavRailAnimations.HaloDurationMs),
        label = "haloAlpha",
    )

    val haloScale by animateFloatAsState(
        targetValue = if (isFocused) 1f else SlideOutNavRailAnimations.UnfocusedHaloScale,
        animationSpec = tween(durationMillis = SlideOutNavRailAnimations.HaloDurationMs),
        label = "haloScale",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) SlideOutNavRailAnimations.FocusedIconScale else 1f,
        animationSpec = tween(durationMillis = SlideOutNavRailAnimations.IconScaleDurationMs),
        label = "iconScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SlideOutNavRailDimensions.ItemHeight)
            .padding(horizontal = SlideOutNavRailDimensions.ItemHorizontalPadding)
            .clip(RoundedCornerShape(SlideOutNavRailDimensions.ItemCornerRadius))
            .background(
                if (isFocused) {
                    Color.White.copy(alpha = SlideOutNavRailAnimations.FocusedBackgroundAlpha)
                } else {
                    Color.Transparent
                },
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusProperties {
                up = upFocusRequester
                down = downFocusRequester
                left = FocusRequester.Cancel
                // right is not set - let the system handle D-pad Right naturally
            }
            .focusable()
            .onKeyEvent { event -> handleNavItemKeyEvent(event, onClick, onRightPressed) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        NavItemIcon(
            item = item,
            isSelected = isSelected,
            isFocused = isFocused,
            haloAlpha = haloAlpha,
            haloScale = haloScale,
            iconScale = iconScale,
        )

        if (isExpanded) {
            NavItemLabel(
                item = item,
                isSelected = isSelected,
                isFocused = isFocused,
                labelAlpha = labelAlpha,
            )
        }
    }
}

/**
 * Icon with halo effect for a navigation item.
 */
@Composable
private fun NavItemIcon(
    item: NavItem,
    isSelected: Boolean,
    isFocused: Boolean,
    haloAlpha: Float,
    haloScale: Float,
    iconScale: Float,
) {
    Box(
        modifier = Modifier
            .size(SlideOutNavRailDimensions.IconContainerSize)
            .padding(start = SlideOutNavRailDimensions.IconStartPadding),
        contentAlignment = Alignment.Center,
    ) {
        // Outer halo
        Box(
            modifier = Modifier
                .size(SlideOutNavRailDimensions.HaloOuterSize)
                .scale(haloScale)
                .alpha(haloAlpha * SlideOutNavRailAnimations.HaloOuterAlphaMultiplier)
                .blur(SlideOutNavRailDimensions.HaloOuterBlur)
                .clip(CircleShape)
                .background(item.color),
        )

        // Inner glow
        Box(
            modifier = Modifier
                .size(SlideOutNavRailDimensions.HaloInnerSize)
                .scale(haloScale)
                .alpha(haloAlpha * SlideOutNavRailAnimations.HaloInnerAlphaMultiplier)
                .blur(SlideOutNavRailDimensions.HaloInnerBlur)
                .clip(CircleShape)
                .background(item.color),
        )

        // Selection indicator
        if (isSelected && !isFocused) {
            Box(
                modifier = Modifier
                    .size(SlideOutNavRailDimensions.SelectionIndicatorSize)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = SlideOutNavRailAnimations.SelectionIndicatorAlpha)),
            )
        }

        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(SlideOutNavRailDimensions.IconSize)
                .scale(iconScale),
            tint = getIconTint(item, isFocused, isSelected),
        )
    }
}

/**
 * Label text for a navigation item.
 */
@Composable
private fun NavItemLabel(
    item: NavItem,
    isSelected: Boolean,
    isFocused: Boolean,
    labelAlpha: Float,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(SlideOutNavRailDimensions.LabelSpacing))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
            color = getIconTint(item, isFocused, isSelected).copy(alpha = labelAlpha),
            modifier = Modifier.alpha(labelAlpha),
        )
    }
}
