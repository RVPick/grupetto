package com.spop.poverlay.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.spop.poverlay.R

/**
 * Peloton's original palette: Woodsmoke black, Pumice gray and Cardinal red.
 * Numbers are white like Peloton's in-ride metrics, with red reserved for Output
 * and highlights.
 */
object PeloColors {
    val Woodsmoke = Color(0xFF101113)
    val Pumice = Color(0xFFCED0CF)
    /** Solid fills (buttons, bars). */
    val Cardinal = Color(0xFFC41F2F)
    /** Red text and lines on black; plain Cardinal is too dark for thin strokes. */
    val CardinalBright = Color(0xFFE8323F)

    val OverlayBackground = Woodsmoke
    val Surface = Color(0xFF1C1E21)
    val Divider = Color(0xFF2C2F33)

    val Text = Color.White
    val TextMuted = Color(0xFF8E9296)
}

object PeloFonts {
    /** Barlow Condensed, for numbers. */
    val Numbers = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )

    /** IBM Plex Sans (variable font), for labels. */
    @OptIn(ExperimentalTextApi::class)
    val Body = FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold).map { weight ->
            Font(
                R.font.ibm_plex_sans,
                weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )
}
