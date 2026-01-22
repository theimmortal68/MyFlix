@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jausc.myflix.core.common.model.JellyfinItem
import dev.jausc.myflix.tv.R

/**
 * Studio name to drawable resource mapping.
 * Maps Jellyfin studio names to local drawable resources.
 */
private val studioLogoResources: Map<String, Int> = mapOf(
    // Major Studios
    "Warner Bros. Pictures" to R.drawable.studio_warner_bros_pictures,
    "Warner Bros." to R.drawable.studio_warner_bros_pictures,
    "Universal Pictures" to R.drawable.studio_universal_pictures,
    "Universal" to R.drawable.studio_universal_pictures,
    "Paramount Pictures" to R.drawable.studio_paramount_pictures,
    "Paramount" to R.drawable.studio_paramount_pictures,
    "Columbia Pictures" to R.drawable.studio_columbia_pictures,
    "Columbia" to R.drawable.studio_columbia_pictures,
    "20th Century Fox" to R.drawable.studio_20th_century_studios,
    "20th Century Studios" to R.drawable.studio_20th_century_studios,
    "New Line Cinema" to R.drawable.studio_new_line_cinema,
    "Walt Disney Pictures" to R.drawable.studio_walt_disney_pictures,
    "Disney" to R.drawable.studio_walt_disney_pictures,
    "Metro-Goldwyn-Mayer" to R.drawable.studio_metro_goldwyn_mayer,
    "MGM" to R.drawable.studio_metro_goldwyn_mayer,
    "Lionsgate" to R.drawable.studio_lionsgate,
    "Lionsgate Films" to R.drawable.studio_lionsgate,
    "TSG Entertainment" to R.drawable.studio_tsg_entertainment,
    "Miramax" to R.drawable.studio_miramax,
    "Miramax Films" to R.drawable.studio_miramax,

    // DreamWorks
    "DreamWorks Pictures" to R.drawable.studio_dreamworks_pictures,
    "DreamWorks" to R.drawable.studio_dreamworks_pictures,
    "DreamWorks Animation" to R.drawable.studio_dreamworks_studios,
    "DreamWorks Studios" to R.drawable.studio_dreamworks_studios,

    // Animation
    "Warner Bros. Animation" to R.drawable.studio_warner_animation_group,
    "Warner Animation Group" to R.drawable.studio_warner_animation_group,
    "Pixar" to R.drawable.studio_pixar,
    "Pixar Animation Studios" to R.drawable.studio_pixar,
    "Walt Disney Animation Studios" to R.drawable.studio_walt_disney_animation_studios,
    "Walt Disney Feature Animation" to R.drawable.studio_walt_disney_animation_studios,
    "Disney Television Animation" to R.drawable.studio_disney_television_animation,
    "DisneyToon Studios" to R.drawable.studio_disneytoon_studios,
    "Blue Sky Studios" to R.drawable.studio_blue_sky_studios,
    "Sony Pictures Animation" to R.drawable.studio_sony_pictures_animation,
    "Illumination" to R.drawable.studio_illumination_entertainment,
    "Illumination Entertainment" to R.drawable.studio_illumination_entertainment,
    "Laika Entertainment" to R.drawable.studio_laika_entertainment,
    "Laika" to R.drawable.studio_laika_entertainment,
    "Aardman" to R.drawable.studio_aardman,
    "Aardman Animations" to R.drawable.studio_aardman,
    "Cartoon Saloon" to R.drawable.studio_cartoon_saloon,
    "Nickelodeon Animation Studio" to R.drawable.studio_nickelodeon_animation_studio,

    // Other Major
    "Amblin Entertainment" to R.drawable.studio_amblin_entertainment,
    "Village Roadshow Pictures" to R.drawable.studio_village_roadshow_pictures,
    "Regency Enterprises" to R.drawable.studio_regency_pictures,
    "New Regency Pictures" to R.drawable.studio_regency_pictures,
    "Regency Pictures" to R.drawable.studio_regency_pictures,
    "Touchstone Pictures" to R.drawable.studio_touchstone_pictures,
    "Netflix" to R.drawable.studio_netflix,
    "Marvel Studios" to R.drawable.studio_marvel_studios,
    "Marvel Entertainment" to R.drawable.studio_marvel_studios,
    "Blumhouse Productions" to R.drawable.studio_blumhouse_productions,
    "Blumhouse" to R.drawable.studio_blumhouse_productions,
    "DC Entertainment" to R.drawable.studio_dc_comics,
    "DC Films" to R.drawable.studio_dc_comics,
    "DC" to R.drawable.studio_dc_comics,
    "DC Comics" to R.drawable.studio_dc_comics,
    "Original Film" to R.drawable.studio_original_film,
    "Legendary Pictures" to R.drawable.studio_legendary_pictures,
    "Legendary Entertainment" to R.drawable.studio_legendary_pictures,
    "Summit Entertainment" to R.drawable.studio_summit_entertainment,
    "Scott Free Productions" to R.drawable.studio_scott_free_productions,
    "Dimension Films" to R.drawable.studio_dimension_films,
    "TriStar Pictures" to R.drawable.studio_tristar_pictures,
    "United Artists" to R.drawable.studio_united_artists,
    "Ingenious Media" to R.drawable.studio_ingenious_media,
    "Working Title Films" to R.drawable.studio_working_title_films,
    "Skydance Media" to R.drawable.studio_skydance,
    "Skydance" to R.drawable.studio_skydance,
    "Walt Disney Productions" to R.drawable.studio_walt_disney_productions,
    "Screen Gems" to R.drawable.studio_screen_gems,
    "RatPac Entertainment" to R.drawable.studio_ratpac_entertainment,
    "Davis Entertainment" to R.drawable.studio_davis_entertainment,
    "FilmNation Entertainment" to R.drawable.studio_filmnation_entertainment,
    "Jerry Bruckheimer Films" to R.drawable.studio_jerry_bruckheimer_films,
    "HBO" to R.drawable.studio_hbo,
    "HBO Films" to R.drawable.studio_hbo,
    "StudioCanal" to R.drawable.studio_studiocanal,
    "Thunder Road" to R.drawable.studio_thunder_road,
    "Thunder Road Films" to R.drawable.studio_thunder_road,
    "EON Productions" to R.drawable.studio_eon_productions,
    "Eon Productions" to R.drawable.studio_eon_productions,
    "Castle Rock Entertainment" to R.drawable.studio_castle_rock_entertainment,
    "STXfilms" to R.drawable.studio_stx_entertainment,
    "STX Entertainment" to R.drawable.studio_stx_entertainment,
    "Studio Babelsberg" to R.drawable.studio_studio_babelsberg,
    "Millennium Media" to R.drawable.studio_millennium_films,
    "Millennium Films" to R.drawable.studio_millennium_films,
    "Lucasfilm Ltd." to R.drawable.studio_lucasfilm_ltd,
    "Lucasfilm" to R.drawable.studio_lucasfilm_ltd,
    "Focus Features" to R.drawable.studio_focus_features,
    "Atlas Entertainment" to R.drawable.studio_atlas_entertainment,
    "Heyday Films" to R.drawable.studio_heyday_films,
    "Fox Searchlight Pictures" to R.drawable.studio_searchlight_pictures,
    "Searchlight Pictures" to R.drawable.studio_searchlight_pictures,
    "Plan B Entertainment" to R.drawable.studio_plan_b_entertainment,
    "Film4 Productions" to R.drawable.studio_film4_productions,
    "Film4" to R.drawable.studio_film4_productions,
    "The Kennedy/Marshall Company" to R.drawable.studio_the_kennedy_marshall_company,
    "Kennedy/Marshall Company" to R.drawable.studio_the_kennedy_marshall_company,
    "1492 Pictures" to R.drawable.studio_1492_pictures,
    "Happy Madison Productions" to R.drawable.studio_happy_madison_productions,
    "Chernin Entertainment" to R.drawable.studio_chernin_entertainment,
    "Constantin Film" to R.drawable.studio_constantin_film,
    "Participant" to R.drawable.studio_participant,
    "Participant Media" to R.drawable.studio_participant,
    "Spyglass Entertainment" to R.drawable.studio_spyglass_entertainment,
    "Orion Pictures" to R.drawable.studio_orion_pictures,
    "A24" to R.drawable.studio_a24,
    "Annapurna Pictures" to R.drawable.studio_annapurna_pictures,
    "Bad Robot" to R.drawable.studio_bad_robot,
    "Bad Robot Productions" to R.drawable.studio_bad_robot,
    "Syncopy" to R.drawable.studio_syncopy,
    "Gracie Films" to R.drawable.studio_gracie_films,
    "20th Century Fox Animation" to R.drawable.studio_20th_century_animation,
    "20th Century Animation" to R.drawable.studio_20th_century_animation,
    "20th Century Fox Television" to R.drawable.studio_20th_century_fox_television,
    "Entertainment One" to R.drawable.studio_entertainment_one,
    "eOne" to R.drawable.studio_entertainment_one,
    "Gaumont" to R.drawable.studio_gaumont,
    "Canal+" to R.drawable.studio_canalplus,
    "Carnival Films" to R.drawable.studio_carnival_films,
    "Bad Wolf" to R.drawable.studio_bad_wolf,

    // Anime Studios
    "Studio Ghibli" to R.drawable.studio_studio_ghibli,
    "Ghibli" to R.drawable.studio_studio_ghibli,
    "TOHO" to R.drawable.studio_toho,
    "Toho" to R.drawable.studio_toho,
    "Bones" to R.drawable.studio_bones,
    "Madhouse" to R.drawable.studio_madhouse,
    "Production I.G" to R.drawable.studio_production_i_g,
    "Sunrise" to R.drawable.studio_sunrise,
    "ufotable" to R.drawable.studio_ufotable,
    "MAPPA" to R.drawable.studio_mappa,
    "Wit Studio" to R.drawable.studio_wit_studio,
    "CloverWorks" to R.drawable.studio_cloverworks,
    "Kyoto Animation" to R.drawable.studio_kyoto_animation,
    "KyoAni" to R.drawable.studio_kyoto_animation,
    "Trigger" to R.drawable.studio_trigger,
    "Studio Trigger" to R.drawable.studio_trigger,

    // TV Studios
    "Amazon Studios" to R.drawable.studio_amazon_studios,
    "BBC Studios" to R.drawable.studio_bbc_studios,
    "CBS Studios" to R.drawable.studio_cbs_studios,
    "CBS Television Studios" to R.drawable.studio_cbs_studios,
    "AMC Studios" to R.drawable.studio_amc_studios,
    "Walt Disney Studios" to R.drawable.studio_walt_disney_pictures,
)

/**
 * Get the drawable resource ID for a studio name.
 * Returns null if no matching logo is found.
 */
fun getStudioLogoResource(studioName: String): Int? {
    return studioLogoResources[studioName]
}

/**
 * Row of studio logos for movie/series detail screens.
 * Displays logos for studios that have matching images.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioLogosRow(
    item: JellyfinItem,
    modifier: Modifier = Modifier,
    logoHeight: Dp = 20.dp,
    maxLogos: Int = 4,
) {
    val studios = item.studios.orEmpty()

    // Get logos for studios that have images
    val studioLogos = remember(studios) {
        studios
            .mapNotNull { studio ->
                val name = studio.name ?: return@mapNotNull null
                getStudioLogoResource(name)?.let { resId -> name to resId }
            }
            .take(maxLogos)
    }

    if (studioLogos.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        studioLogos.forEach { (name, resId) ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = name,
                modifier = Modifier.height(logoHeight),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
