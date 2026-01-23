@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlin.math.max

/**
 * Netflix-style slide-out menu item.
 */
data class SlideOutMenuItem(
    val text: String,
    val icon: ImageVector? = null,
    val iconTint: Color = Color.White,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val dismissOnClick: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Section header for slide-out menu.
 */
data class SlideOutMenuSection(
    val title: String,
    val items: List<SlideOutMenuItem>,
)

/**
 * Horizontal alignment for the popup menu relative to the anchor point.
 */
enum class MenuAnchorAlignment {
    BottomStart,
    BottomEnd,
}

enum class MenuAnchorPlacement {
    Above,
    Below,
}

/**
 * Anchor position for menu placement.
 * Represents the bottom edge of the button that triggered the menu.
 */
data class MenuAnchor(
    val x: Dp,
    val y: Dp,
    val alignment: MenuAnchorAlignment = MenuAnchorAlignment.BottomEnd,
    val placement: MenuAnchorPlacement = MenuAnchorPlacement.Above,
)

/**
 * Netflix-style popup menu for player overlay.
 * Compact design with small text and minimal padding.
 * Appears above the anchor point with a scale animation from the anchor.
 */
@Composable
fun PlayerSlideOutMenu(
    visible: Boolean,
    title: String,
    items: List<SlideOutMenuItem>,
    onDismiss: () -> Unit,
    anchor: MenuAnchor? = null,
    firstItemFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        BackHandler { onDismiss() }
    }
    val itemFocusRequesters = remember(items.size, firstItemFocusRequester) {
        List(items.size) { index ->
            if (index == 0 && firstItemFocusRequester != null) {
                firstItemFocusRequester
            } else {
                FocusRequester()
            }
        }
    }
    val listState = rememberLazyListState()
    var focusedIndex by remember { mutableStateOf(0) }

    // Focus first item when menu appears
    LaunchedEffect(visible) {
        if (visible && itemFocusRequesters.isNotEmpty()) {
            itemFocusRequesters.first().requestFocus()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val menuWidth = 200.dp
        val menuHeight = 300.dp // Approximate max height
        var measuredWidthPx by remember { mutableStateOf<Float?>(null) }
        var measuredHeightPx by remember { mutableStateOf<Float?>(null) }
        val menuWidthPx = with(density) { measuredWidthPx ?: menuWidth.toPx() }
        val menuHeightPx = with(density) { measuredHeightPx ?: menuHeight.toPx() }

        // Calculate menu position based on anchor
        val offsetX = if (anchor != null) {
            with(density) {
                val anchorPx = anchor.x.toPx()
                val maxWidthPx = maxWidth.toPx()
                val rawX = when (anchor.alignment) {
                    MenuAnchorAlignment.BottomStart -> anchorPx
                    MenuAnchorAlignment.BottomEnd -> anchorPx - menuWidthPx
                }
                val maxX = max(16f, maxWidthPx - menuWidthPx - 16f)
                val targetX = rawX.coerceIn(16f, maxX)
                targetX.toInt()
            }
        } else {
            with(density) { (maxWidth - menuWidth - 24.dp).toPx().toInt() }
        }

        val offsetY = if (anchor != null) {
            with(density) {
                val anchorPx = anchor.y.toPx()
                val targetY = if (anchor.placement == MenuAnchorPlacement.Below) {
                    anchorPx + 12.dp.toPx()
                } else {
                    (anchorPx - menuHeightPx).coerceAtLeast(48f)
                }
                targetY.toInt()
            }
        } else {
            with(density) { 48.dp.toPx().toInt() }
        }

        // Transform origin for scale animation (bottom center of menu, pointing to anchor)
        val transformOrigin = if (anchor != null) {
            when (anchor.alignment) {
                MenuAnchorAlignment.BottomStart -> {
                    if (anchor.placement == MenuAnchorPlacement.Below) TransformOrigin(0f, 0f) else TransformOrigin(0f, 1f)
                }
                MenuAnchorAlignment.BottomEnd -> {
                    if (anchor.placement == MenuAnchorPlacement.Below) TransformOrigin(1f, 0f) else TransformOrigin(1f, 1f)
                }
            }
        } else {
            TransformOrigin(1f, 0.5f) // Scale from right center (fallback)
        }

        // Dismiss scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            )
        }

        // Popup menu panel
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = tween(200),
                initialScale = 0.8f,
                transformOrigin = transformOrigin,
            ) + fadeIn(tween(200)),
            exit = scaleOut(
                animationSpec = tween(150),
                targetScale = 0.8f,
                transformOrigin = transformOrigin,
            ) + fadeOut(tween(150)),
            modifier = Modifier.offset { IntOffset(offsetX, offsetY) },
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 260.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionUp -> {
                                if (focusedIndex > 0) {
                                    itemFocusRequesters[focusedIndex - 1].requestFocus()
                                }
                                true
                            }
                            Key.DirectionDown -> {
                                if (focusedIndex < items.lastIndex) {
                                    itemFocusRequesters[focusedIndex + 1].requestFocus()
                                }
                                true
                            }
                            else -> { false }
                        }
                    }
                    .onGloballyPositioned { coordinates ->
                        measuredWidthPx = coordinates.size.width.toFloat()
                        measuredHeightPx = coordinates.size.height.toFloat()
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF2141414),
                                Color(0xE6181818),
                            ),
                        ),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(vertical = 8.dp),
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )

                // Items
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(items, key = { _, item -> item.text }) { index, item ->
                        SlideOutMenuItemRow(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == items.lastIndex,
                            onDismiss = onDismiss,
                            leftFocusRequester = leftFocusRequester,
                            rightFocusRequester = rightFocusRequester,
                            onFocused = { focusedIndex = index },
                            modifier = Modifier.focusRequester(itemFocusRequesters[index]),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Netflix-style popup menu with sections.
 * Appears above the anchor point with a scale animation.
 */
@Composable
fun PlayerSlideOutMenuSectioned(
    visible: Boolean,
    title: String,
    sections: List<SlideOutMenuSection>,
    onDismiss: () -> Unit,
    anchor: MenuAnchor? = null,
    onItemAnchorChanged: ((SlideOutMenuItem, MenuAnchor) -> Unit)? = null,
    firstItemFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        BackHandler { onDismiss() }
    }
    val totalItems = remember(sections) { sections.sumOf { it.items.size } }
    val itemFocusRequesters = remember(totalItems, firstItemFocusRequester) {
        List(totalItems) { index ->
            if (index == 0 && firstItemFocusRequester != null) {
                firstItemFocusRequester
            } else {
                FocusRequester()
            }
        }
    }
    var focusedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(visible) {
        if (visible && itemFocusRequesters.isNotEmpty()) {
            itemFocusRequesters.first().requestFocus()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val menuWidth = 220.dp
        val menuHeight = 400.dp // Sectioned menus are taller
        var measuredWidthPx by remember { mutableStateOf<Float?>(null) }
        var measuredHeightPx by remember { mutableStateOf<Float?>(null) }
        val menuWidthPx = with(density) { measuredWidthPx ?: menuWidth.toPx() }
        val menuHeightPx = with(density) { measuredHeightPx ?: menuHeight.toPx() }

        // Calculate menu position based on anchor
        val offsetX = if (anchor != null) {
            with(density) {
                val anchorPx = anchor.x.toPx()
                val maxWidthPx = maxWidth.toPx()
                val rawX = when (anchor.alignment) {
                    MenuAnchorAlignment.BottomStart -> anchorPx
                    MenuAnchorAlignment.BottomEnd -> anchorPx - menuWidthPx
                }
                val maxX = max(16f, maxWidthPx - menuWidthPx - 16f)
                val targetX = rawX.coerceIn(16f, maxX)
                targetX.toInt()
            }
        } else {
            with(density) { (maxWidth - menuWidth - 24.dp).toPx().toInt() }
        }

        val offsetY = if (anchor != null) {
            with(density) {
                val anchorPx = anchor.y.toPx()
                val targetY = if (anchor.placement == MenuAnchorPlacement.Below) {
                    anchorPx + 12.dp.toPx()
                } else {
                    (anchorPx - menuHeightPx).coerceAtLeast(48f)
                }
                targetY.toInt()
            }
        } else {
            with(density) { 48.dp.toPx().toInt() }
        }

        val transformOrigin = if (anchor != null) {
            when (anchor.alignment) {
                MenuAnchorAlignment.BottomStart -> {
                    if (anchor.placement == MenuAnchorPlacement.Below) TransformOrigin(0f, 0f) else TransformOrigin(0f, 1f)
                }
                MenuAnchorAlignment.BottomEnd -> {
                    if (anchor.placement == MenuAnchorPlacement.Below) TransformOrigin(1f, 0f) else TransformOrigin(1f, 1f)
                }
            }
        } else {
            TransformOrigin(1f, 0.5f)
        }

        // Dismiss scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            )
        }

        // Popup menu panel
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = tween(200),
                initialScale = 0.8f,
                transformOrigin = transformOrigin,
            ) + fadeIn(tween(200)),
            exit = scaleOut(
                animationSpec = tween(150),
                targetScale = 0.8f,
                transformOrigin = transformOrigin,
            ) + fadeOut(tween(150)),
            modifier = Modifier.offset { IntOffset(offsetX, offsetY) },
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 280.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionUp -> {
                                if (focusedIndex > 0) {
                                    itemFocusRequesters[focusedIndex - 1].requestFocus()
                                }
                                true
                            }
                            Key.DirectionDown -> {
                                if (focusedIndex < totalItems - 1) {
                                    itemFocusRequesters[focusedIndex + 1].requestFocus()
                                }
                                true
                            }
                            else -> { false }
                        }
                    }
                    .onGloballyPositioned { coordinates ->
                        measuredWidthPx = coordinates.size.width.toFloat()
                        measuredHeightPx = coordinates.size.height.toFloat()
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF2141414),
                                Color(0xE6181818),
                            ),
                        ),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(vertical = 8.dp),
            ) {
                // Main title
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    var startIndex = 0
                    sections.forEach { section ->
                        // Capture startIndex for this section scope
                        val sectionStartIndex = startIndex

                        // Section header
                        item(key = "header_${section.title}") {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 10.dp,
                                    bottom = 4.dp,
                                ),
                            )
                        }

                        itemsIndexed(section.items, key = { _, item -> "${section.title}_${item.text}" }) { index, item ->
                            val globalIndex = sectionStartIndex + index

                            val itemModifier = if (onItemAnchorChanged != null) {
                                Modifier.onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInRoot()
                                    val size = coordinates.size
                                    with(density) {
                                        onItemAnchorChanged(
                                            item,
                                            MenuAnchor(
                                                x = (position.x + size.width).toDp(),
                                                y = position.y.toDp(),
                                                alignment = MenuAnchorAlignment.BottomStart,
                                                placement = MenuAnchorPlacement.Below,
                                            ),
                                        )
                                    }
                                }
                            } else {
                                Modifier
                            }
                            SlideOutMenuItemRow(
                                item = item,
                                isFirst = globalIndex == 0,
                                isLast = globalIndex == totalItems - 1,
                                onDismiss = onDismiss,
                                leftFocusRequester = leftFocusRequester,
                                rightFocusRequester = rightFocusRequester,
                                onFocused = { focusedIndex = globalIndex },
                                modifier = itemModifier.focusRequester(itemFocusRequesters[globalIndex]),
                            )
                        }
                        startIndex += section.items.size
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideOutMenuItemRow(
    item: SlideOutMenuItem,
    isFirst: Boolean,
    isLast: Boolean,
    onDismiss: () -> Unit,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onFocused: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = {
            item.onClick()
            if (item.dismissOnClick) {
                onDismiss()
            }
        },
        enabled = item.enabled,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (item.selected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
            disabledContainerColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onFocused?.invoke()
                }
            }
            .focusProperties {
                left = leftFocusRequester ?: FocusRequester.Cancel
                right = rightFocusRequester ?: FocusRequester.Cancel
                if (isFirst) {
                    up = FocusRequester.Cancel
                }
                if (isLast) {
                    down = FocusRequester.Cancel
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (leftFocusRequester != null) {
                            leftFocusRequester.requestFocus()
                            true
                        } else {
                            true
                        }
                    }
                    Key.DirectionRight -> {
                        if (rightFocusRequester != null) {
                            rightFocusRequester.requestFocus()
                            true
                        } else {
                            true
                        }
                    }
                    Key.DirectionUp -> { false }
                    Key.DirectionDown -> { false }
                    else -> { false }
                }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            item.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (item.enabled) {
                        if (item.selected) item.iconTint else item.iconTint.copy(alpha = 0.7f)
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (item.enabled) {
                    if (item.selected) Color.White else Color.White.copy(alpha = 0.9f)
                } else {
                    Color.White.copy(alpha = 0.4f)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
