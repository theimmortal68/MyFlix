@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configuration for TvTabRow focus behavior.
 */
data class TvTabRowFocusConfig(
    /** FocusRequester for navigating up from tabs */
    val upFocusRequester: FocusRequester? = null,
    /** FocusRequester for left edge (NavRail sentinel) */
    val leftEdgeFocusRequester: FocusRequester? = null,
    /** Whether to cancel left navigation on first tab when no left edge target */
    val cancelLeftOnFirstTab: Boolean = false,
)

/**
 * Reusable TV-style tab row with animated halo focus effect and debounced selection.
 *
 * Features:
 * - Blue halo glow effect on focused tabs
 * - Debounced tab selection (150ms) to prevent rapid switching
 * - Underline indicator for selected tab
 * - Focus restoration support via [getTabFocusRequester] and [onTabFocused]
 *
 * @param T The enum type for tabs
 * @param tabs List of available tabs to display
 * @param selectedTab Currently selected tab
 * @param onTabSelected Callback when a tab is selected (via click or focus debounce)
 * @param tabLabel Function to get display label for each tab
 * @param getTabFocusRequester Function to get/create FocusRequester for each tab
 * @param onTabFocused Callback when a tab receives focus (for tracking last focused tab)
 * @param focusConfig Configuration for focus navigation behavior
 * @param modifier Modifier for the row
 */
@Composable
fun <T : Enum<T>> TvTabRow(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    tabLabel: (T) -> String,
    getTabFocusRequester: (T) -> FocusRequester,
    onTabFocused: (T, FocusRequester) -> Unit,
    focusConfig: TvTabRowFocusConfig = TvTabRowFocusConfig(),
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var tabChangeJob by remember { mutableStateOf<Job?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTab == tab
            var isFocused by remember { mutableStateOf(false) }
            val isFirstTab = index == 0
            val tabRequester = getTabFocusRequester(tab)

            // Animated halo effect for focus indication
            val haloAlpha by animateFloatAsState(
                targetValue = if (isFocused) 0.6f else 0f,
                animationSpec = tween(durationMillis = 150),
                label = "tabHaloAlpha",
            )

            Column(
                modifier = Modifier
                    .focusRequester(tabRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onTabFocused(tab, tabRequester)
                            // Cancel any pending tab change
                            tabChangeJob?.cancel()
                            // Start new debounced change
                            tabChangeJob = coroutineScope.launch {
                                delay(150) // 150ms debounce
                                onTabSelected(tab)
                            }
                        }
                    }
                    .focusProperties {
                        focusConfig.upFocusRequester?.let { up = it }
                        left = when {
                            isFirstTab && focusConfig.leftEdgeFocusRequester != null ->
                                focusConfig.leftEdgeFocusRequester
                            isFirstTab && focusConfig.cancelLeftOnFirstTab ->
                                FocusRequester.Cancel
                            else -> FocusRequester.Default
                        }
                    }
                    .focusable()
                    .selectable(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Text with halo glow behind it when focused
                Box(contentAlignment = Alignment.Center) {
                    // Halo glow (blurred background)
                    if (haloAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .blur(12.dp)
                                .background(
                                    TvColors.BluePrimary.copy(alpha = haloAlpha),
                                    RoundedCornerShape(16.dp),
                                ),
                        )
                    }
                    Text(
                        text = tabLabel(tab),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isFocused -> Color.White
                            isSelected -> Color.White
                            else -> TvColors.TextSecondary
                        },
                        fontWeight = if (isSelected || isFocused) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Underline indicator - shows on both selected and focused
                Box(
                    modifier = Modifier
                        .width(if (isSelected || isFocused) 40.dp else 0.dp)
                        .height(2.dp)
                        .background(
                            when {
                                isFocused -> TvColors.BluePrimary
                                isSelected -> TvColors.BluePrimary.copy(alpha = 0.7f)
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(1.dp),
                        ),
                )
            }
        }
    }
}
