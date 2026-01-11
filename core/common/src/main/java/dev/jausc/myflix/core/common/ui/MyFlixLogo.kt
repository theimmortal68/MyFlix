@file:Suppress("UnusedParameter")

package dev.jausc.myflix.core.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jausc.myflix.core.common.R

/**
 * MyFlix logo image.
 * Displays the marquee-style logo with glowing letters.
 *
 * @param height Height of the logo (width scales proportionally). If null, uses FillWidth scaling.
 * @param tint Optional color tint (null for original colors)
 */
@Composable
fun MyFlixLogo(modifier: Modifier = Modifier, height: Dp? = 80.dp, tint: Color? = null) {
    Image(
        painter = painterResource(R.drawable.myflix_logo),
        contentDescription = "MyFlix",
        modifier = if (height != null) modifier.height(height) else modifier,
        contentScale = if (height != null) ContentScale.FillHeight else ContentScale.FillWidth,
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
}

/**
 * Overload for backwards compatibility with fontSize parameter.
 * Converts fontSize to approximate height.
 */
@Composable
fun MyFlixLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    color: Color = Color(0xFF2563EB),
    letterSpacing: TextUnit = 2.sp,
    emphasisScale: Float = 1.35f,
) {
    // Convert sp to dp (approximate) - logo height is roughly fontSize
    val heightDp = (fontSize.value * 0.8f).dp
    MyFlixLogo(
        modifier = modifier,
        height = heightDp,
        tint = null, // Use original logo colors
    )
}
