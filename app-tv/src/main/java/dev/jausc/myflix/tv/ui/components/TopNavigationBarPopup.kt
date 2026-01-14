@file:Suppress(
    "LongMethod",
    "MagicNumber",
    "WildcardImport",
    "NoWildcardImports",
    "LabeledExpression",
    "ModifierMissing",
    "ParameterNaming",
    "ComposableParamOrder",
)

package dev.jausc.myflix.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.delay

/**
 * Netflix-style top navigation bar that appears as a popup overlay.
 * - Visible on page load, auto-hides after 5 seconds
 * - Reappears when user navigates up from content
 * - Hides on item selection or down navigation
 */
@Composable
fun TopNavigationBarPopup(
    visible: Boolean,
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    homeButtonFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    // Track if nav bar has been hidden once (for auto-focus behavior)
    var hasBeenHiddenOnce by remember { mutableStateOf(false) }

    // Focus Home button when popup reappears after being hidden
    LaunchedEffect(visible) {
        if (visible && hasBeenHiddenOnce) {
            try {
                homeButtonFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
        if (!visible) {
            hasBeenHiddenOnce = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.6f),
                            0.4f to Color.Black.copy(alpha = 0.5f),
                            0.7f to Color.Black.copy(alpha = 0.3f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 24.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown, Key.Back, Key.Escape -> {
                                onDismiss()
                                true
                            }
                            else -> {
                                false
                            }
                        }
                    } else {
                        false
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Settings
            NavIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                isSelected = selectedItem == NavItem.SETTINGS,
                onClick = {
                    onItemSelected(NavItem.SETTINGS)
                    onDismiss()
                },
            )

            // Main nav items: Home, TV Shows, Movies, Collections, Universes, Discover
            listOf(
                NavItem.HOME,
                NavItem.SHOWS,
                NavItem.MOVIES,
                NavItem.COLLECTIONS,
                NavItem.UNIVERSES,
                NavItem.DISCOVER,
            ).forEach { item ->
                NavTabButton(
                    item = item,
                    isSelected = selectedItem == item,
                    onClick = {
                        onItemSelected(item)
                        onDismiss()
                    },
                    focusRequester = if (item == NavItem.HOME) homeButtonFocusRequester else null,
                )
            }

            // Search
            NavIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                isSelected = selectedItem == NavItem.SEARCH,
                onClick = {
                    onItemSelected(NavItem.SEARCH)
                    onDismiss()
                },
            )
        }
    }
}

/**
 * State holder for TopNavigationBarPopup.
 * Handles visibility and auto-hide timer.
 */
class NavBarPopupState {
    var isVisible by mutableStateOf(true)
        private set

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }
}

/**
 * Remember and create a [NavBarPopupState] with appropriate auto-hide behavior.
 *
 * @param hasSeenNavBarTip Whether the user has dismissed the first-run tip.
 *   - `false` (first run): Nav bar stays visible (no auto-hide) while tip is shown.
 *   - `true` (subsequent launches): Nav bar auto-hides after 2 seconds.
 */
@Composable
fun rememberNavBarPopupState(hasSeenNavBarTip: Boolean = true): NavBarPopupState {
    val state = remember { NavBarPopupState() }

    // Auto-hide behavior depends on whether user has seen the tip
    LaunchedEffect(hasSeenNavBarTip) {
        if (hasSeenNavBarTip) {
            // Subsequent launches: auto-hide after 2 seconds
            delay(2000)
            state.hide()
        }
        // First run (hasSeenNavBarTip = false): don't auto-hide, tip will trigger hide
    }

    return state
}

@Composable
private fun NavIconButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(20.dp),
        contentPadding = PaddingValues(0.dp),
        scale = ButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
        ),
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) {
                TvColors.BluePrimary.copy(alpha = 0.3f)
            } else {
                Color.Transparent
            },
            contentColor = if (isSelected) {
                TvColors.BluePrimary
            } else {
                TvColors.TextSecondary
            },
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun NavTabButton(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(20.dp)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
        ),
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) {
                TvColors.BluePrimary.copy(alpha = 0.3f)
            } else {
                Color.Transparent
            },
            contentColor = if (isSelected) {
                TvColors.BluePrimary
            } else {
                TvColors.TextSecondary
            },
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
        ),
    ) {
        Text(
            text = when (item) {
                NavItem.HOME -> "Home"
                NavItem.MOVIES -> "Movies"
                NavItem.SHOWS -> "TV Shows"
                NavItem.SEARCH -> "Search"
                NavItem.DISCOVER -> "Discover"
                NavItem.SETTINGS -> "Settings"
                NavItem.COLLECTIONS -> "Collections"
                NavItem.UNIVERSES -> "Universes"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
