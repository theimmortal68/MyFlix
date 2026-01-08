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
        Font(R.font.bebas_neue_regular, FontWeight.Normal)
    )
    
    /**
     * Anton - used for headers, section titles, branding moments
     * Bold, impactful display font
     */
    val Anton = FontFamily(
        Font(R.font.anton_regular, FontWeight.Normal)
    )
    
    /**
     * Logo font alias
     */
    val Logo = BebasNeue
    
    /**
     * Header font alias
     */
    val Header = Anton
}
