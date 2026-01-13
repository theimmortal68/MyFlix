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
     * Anton - used for headers, section titles, branding moments
     * Bold, impactful display font
     */
    val Anton = FontFamily(
        Font(R.font.anton_regular, FontWeight.Normal),
    )

    /**
     * Plus Jakarta Sans - modern, clean sans-serif for body text
     * Popular choice for streaming apps
     */
    val PlusJakartaSans = FontFamily(
        Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    )

    /**
     * Outfit - modern geometric sans-serif with distinctive character
     * Clean, contemporary look perfect for media apps
     */
    val Outfit = FontFamily(
        Font(R.font.outfit_regular, FontWeight.Normal),
        Font(R.font.outfit_medium, FontWeight.Medium),
        Font(R.font.outfit_semibold, FontWeight.SemiBold),
        Font(R.font.outfit_bold, FontWeight.Bold),
    )

    /**
     * Montserrat - geometric, elegant, Netflix-like feel
     * Popular for streaming and media applications
     */
    val Montserrat = FontFamily(
        Font(R.font.montserrat_regular, FontWeight.Normal),
        Font(R.font.montserrat_medium, FontWeight.Medium),
        Font(R.font.montserrat_semibold, FontWeight.SemiBold),
        Font(R.font.montserrat_bold, FontWeight.Bold),
    )

    /**
     * Logo font alias
     */
    val Logo = BebasNeue

    /**
     * Header font alias
     */
    val Header = Anton

    /**
     * Body/UI font alias
     */
    val Body = Montserrat
}
