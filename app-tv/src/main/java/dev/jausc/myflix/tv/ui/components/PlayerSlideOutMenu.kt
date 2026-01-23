@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/**
 * Netflix-style slide-out menu item.
 */
data class SlideOutMenuItem(
    val text: String,
    val icon: ImageVector? = null,
    val iconTint: Color = Color.White,
    val selected: Boolean = false,
    val enabled: Boolean = true,
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
 * Netflix-style slide-out menu for player overlay.
 * Compact design with small text and minimal padding.
 * Slides in from the right side.
 */
@Composable
fun PlayerSlideOutMenu(
    visible: Boolean,
    title: String,
    items: List<SlideOutMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Focus first item when menu appears
    LaunchedEffect(visible) {
        if (visible) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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

        // Slide-out panel from right
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(200)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(150)) { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 260.dp)
                    .padding(end = 24.dp, top = 48.dp, bottom = 120.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE6181818),
                                Color(0xF2141414),
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
                    items(items, key = { it.text }) { item ->
                        val itemFocusRequester = if (items.indexOf(item) == 0) focusRequester else remember { FocusRequester() }
                        SlideOutMenuItemRow(
                            item = item,
                            onDismiss = onDismiss,
                            modifier = Modifier.focusRequester(itemFocusRequester),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Netflix-style slide-out menu with sections.
 */
@Composable
fun PlayerSlideOutMenuSectioned(
    visible: Boolean,
    title: String,
    sections: List<SlideOutMenuSection>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var isFirstItem = true

    LaunchedEffect(visible) {
        if (visible) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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

        // Slide-out panel
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(200)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(150)) { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 260.dp)
                    .padding(end = 24.dp, top = 48.dp, bottom = 120.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE6181818),
                                Color(0xF2141414),
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
                    sections.forEach { section ->
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

                        items(section.items, key = { "${section.title}_${it.text}" }) { item ->
                            val itemFocusRequester = if (isFirstItem) {
                                isFirstItem = false
                                focusRequester
                            } else {
                                remember { FocusRequester() }
                            }
                            SlideOutMenuItemRow(
                                item = item,
                                onDismiss = onDismiss,
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
private fun SlideOutMenuItemRow(
    item: SlideOutMenuItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = {
            item.onClick()
            onDismiss()
        },
        enabled = item.enabled,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (item.selected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
            disabledContainerColor = Color.Transparent,
        ),
        modifier = modifier.padding(horizontal = 4.dp),
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
            )
        }
    }
}
