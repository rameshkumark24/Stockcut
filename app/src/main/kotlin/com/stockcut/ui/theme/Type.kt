package com.stockcut.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale from docs/04-uiux-brief.md §3.
 *
 * 🔴 NON-NEGOTIABLE: every measurement renders in TABULAR figures, so digits
 * align vertically in a list. A column of right-aligned lengths that does not
 * line up looks broken to someone who reads numbers for a living. Roboto ships
 * the "tnum" OpenType feature, which gives every digit an identical advance
 * width; that is what the fontFeatureSettings below turns on.
 *
 * Sizes are in sp, never dp, so the system font scale applies. Users here are
 * frequently 45+ and run large text; docs/06 §6 requires every screen to
 * survive the scale at maximum, which is why no row in this app has a fixed
 * height.
 */

private const val TABULAR_FIGURES = "tnum"

/**
 * The measurement style — used for EVERY length, everywhere in the app.
 *
 * Kept as a named style rather than a Material Typography slot because Material
 * has no slot meaning "a length in the user's chosen unit", and because using
 * the wrong style for a measurement is precisely the mistake this exists to
 * prevent.
 */
val MeasurementTextStyle = TextStyle(
    fontFeatureSettings = TABULAR_FIGURES,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
)

/** The big number on the result screen: waste % and bar count. */
val DisplayNumberTextStyle = TextStyle(
    fontFeatureSettings = TABULAR_FIGURES,
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
)

/**
 * Material's slots, mapped onto the brief's scale:
 *   headline 22sp SemiBold · title 16sp Medium · body 15sp Regular
 *   label 13sp Medium · caption 12sp Regular
 */
val StockCutTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
)
