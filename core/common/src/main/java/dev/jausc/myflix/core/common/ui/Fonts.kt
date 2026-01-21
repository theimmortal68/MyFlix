package dev.jausc.myflix.core.common.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.jausc.myflix.core.common.R

/**
 * Shared fonts for MyFlix branding
 */
object MyFlixFonts {
    /**
     * Bebas Neue - used for the MyFlix logo
     * Netflix-style tall condensed font
     */
    val BebasNeue = FontFamily(
        Font(R.font.bebas_neue_regular, FontWeight.Normal),
    )

    /**
     * Inter - clean, modern sans-serif optimized for screen readability
     * Designed specifically for computer screens with excellent legibility
     */
    val Inter = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

    /**
     * Logo font alias
     */
    val Logo = BebasNeue

    /**
     * Body/UI font alias
     */
    val Body = Inter
}
