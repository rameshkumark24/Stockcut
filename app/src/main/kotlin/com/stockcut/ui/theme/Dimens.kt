package com.stockcut.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and touch targets from docs/04-uiux-brief.md §4 and §5.
 *
 * The spacing scale is 4 · 8 · 12 · 16 · 24 · 32 · 48 dp and NOTHING ELSE.
 * Named constants rather than raw numbers so a stray 13.dp is visible in review.
 *
 * Touch targets are deliberately larger than Material's defaults. The rationale
 * from the brief is worth keeping in front of you: gloves, cold hands, and a
 * phone held one-handed while the other hand holds a tape measure.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Screen horizontal padding. */
    val screenHorizontal = lg

    /** Between cards. */
    val betweenCards = md

    /** Inside a card. */
    val cardInner = lg

    /** Between sections. */
    val section = xl
}

object TouchTarget {
    /** Never smaller than this, anywhere. */
    val minimum = 48.dp

    /** Primary buttons are taller than Material's default 40 dp. */
    val primaryButtonHeight = 56.dp

    /** List rows grow with font scale; this is the floor, not a fixed height. */
    val listRowMinHeight = 64.dp

    /** Number entry — the same height as a primary button. */
    val fieldHeight = 56.dp

    val iconButton = 48.dp
    val iconSize = 24.dp
}
