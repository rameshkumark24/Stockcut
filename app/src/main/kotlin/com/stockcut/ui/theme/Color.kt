package com.stockcut.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens, verbatim from docs/04-uiux-brief.md §2.
 *
 * Industrial and high-contrast, deliberately not the generic blue every
 * competitor uses. Every text/background pair is >= 4.5:1 (WCAG AA) because
 * this app is read in direct sunlight.
 *
 * 🔴 Colour is never the only signal (brief §2). Waste % is colour-coded AND
 * labelled; cut-plan segments differ in colour AND carry a text label. A
 * colour-blind user must be able to read the same plan.
 */

// Light
val PrimaryLight = Color(0xFFE8590C)
val OnPrimaryLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFF8F9FA)
val SurfaceContainerLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF14181C)
val OnSurfaceVariantLight = Color(0xFF5A636B)
val OutlineLight = Color(0xFFC9CFD4)

// Dark
val PrimaryDark = Color(0xFFFF8A3D)
val OnPrimaryDark = Color(0xFF1A0E06)
val SurfaceDark = Color(0xFF14181C)
val SurfaceContainerDark = Color(0xFF1E2429)
val OnSurfaceDark = Color(0xFFE3E6E8)
val OnSurfaceVariantDark = Color(0xFFA8B0B8)
val OutlineDark = Color(0xFF3A424A)

/**
 * Cut-plan and status colours.
 *
 * These are NOT part of Material's colour scheme — Material has no slot for
 * "the offcut segment of a steel bar". They are carried on [StockCutColors]
 * and reached through [LocalStockCutColors] so a composable cannot accidentally
 * grab a light-mode value while running in dark mode.
 */
val CutSegmentLight = Color(0xFF2D7DD2)
val CutSegmentAltLight = Color(0xFF1E5B9A)
val OffcutLight = Color(0xFF9AA3AB)
val SuccessLight = Color(0xFF2A9D3F)
val WarningLight = Color(0xFFD48806)
val ErrorLight = Color(0xFFC0392B)

val CutSegmentDark = Color(0xFF4A9BE8)
val CutSegmentAltDark = Color(0xFF3579B5)
val OffcutDark = Color(0xFF5F6870)
val SuccessDark = Color(0xFF4FBF63)
val WarningDark = Color(0xFFF0A92E)
val ErrorDark = Color(0xFFF0685A)
