package dev.jausc.myflix.tv.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Standard button height for all TV buttons.
 */
private val StandardButtonHeight = 20.dp

/**
 * Standard icon size inside buttons.
 */
private val StandardIconSize = 14.dp

/**
 * Standard loading indicator size inside buttons.
 */
private val LoadingIndicatorSize = 14.dp

/**
 * Standardized text-only button matching the nav bar style.
 *
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param isLoading Show loading indicator instead of text
 * @param isPrimary Use blue (primary) container color instead of grey
 * @param containerColor Override container color (for semantic colors like green Save)
 */
@Composable
fun TvTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isPrimary: Boolean = false,
    containerColor: Color? = null,
) {
    val defaultContainerColor = if (isPrimary) {
        TvColors.BluePrimary
    } else {
        TvColors.SurfaceElevated.copy(alpha = 0.8f)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(StandardButtonHeight),
        enabled = enabled && !isLoading,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(scale = 1f, focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = containerColor ?: defaultContainerColor,
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
            disabledContainerColor = (containerColor ?: defaultContainerColor).copy(alpha = 0.5f),
            disabledContentColor = TvColors.TextSecondary,
        ),
    ) {
        if (isLoading) {
            TvLoadingIndicator(
                modifier = Modifier.size(LoadingIndicatorSize),
                color = TvColors.TextPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Standardized icon-only button matching the nav bar style.
 *
 * @param icon Icon to display
 * @param contentDescription Accessibility description
 * @param onClick Click handler
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(StandardButtonHeight),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(scale = 1f, focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f),
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
            disabledContainerColor = TvColors.SurfaceElevated.copy(alpha = 0.4f),
            disabledContentColor = TvColors.TextSecondary,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(StandardIconSize),
        )
    }
}

/**
 * Standardized icon + text button matching the nav bar style.
 *
 * @param icon Icon to display before text
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param isLoading Show loading indicator instead of icon
 * @param containerColor Override container color (for semantic colors like green Play, purple Request)
 */
@Composable
fun TvIconTextButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color? = null,
) {
    val defaultContainerColor = TvColors.SurfaceElevated.copy(alpha = 0.8f)

    Button(
        onClick = onClick,
        modifier = modifier.height(StandardButtonHeight),
        enabled = enabled && !isLoading,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        scale = ButtonDefaults.scale(scale = 1f, focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = containerColor ?: defaultContainerColor,
            contentColor = TvColors.TextPrimary,
            focusedContainerColor = TvColors.BluePrimary,
            focusedContentColor = Color.White,
            disabledContainerColor = (containerColor ?: defaultContainerColor).copy(alpha = 0.5f),
            disabledContentColor = TvColors.TextSecondary,
        ),
    ) {
        if (isLoading) {
            TvLoadingIndicator(
                modifier = Modifier.size(LoadingIndicatorSize),
                color = TvColors.TextPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(StandardIconSize),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
