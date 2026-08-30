package com.stockcut.ui.components

import com.stockcut.units.UnitSystem

private const val SUBSCRIPT_DIGITS = "₀₁₂₃₄₅₆₇₈₉"

private fun subscript(value: Int): String =
    value.toString().map { SUBSCRIPT_DIGITS[it - '0'] }.joinToString("")

/**
 * The label on a unit chip.
 *
 * 🔴 [denominator] is a parameter because the fractional label is NOT fixed.
 *
 * This was hardcoded to "inch ¹⁄₁₆" while `SUPPORTED_DENOMINATORS` is
 * 8 / 16 / 32 / 64, so selecting 1/64 rendered a unit chip reading "inch ¹⁄₁₆"
 * directly above a selected fraction chip reading "1/64" — the app
 * contradicting itself about the denominator, on screen, in a tool whose entire
 * claim is that its numbers can be trusted. It only reads as harmless because
 * the default happens to be 16.
 *
 * It also lived twice, once in the editor and once in settings, which is
 * precisely how both copies came to carry the same defect: a fix to one screen
 * would have left the other wrong. One home now, and only the denominator
 * varies.
 */
internal fun unitLabel(system: UnitSystem, denominator: Int): String = when (system) {
    UnitSystem.MM -> "mm"
    UnitSystem.CM -> "cm"
    UnitSystem.M -> "m"
    UnitSystem.INCH_DECIMAL -> "inch"
    UnitSystem.INCH_FRACTIONAL -> "inch ¹⁄${subscript(denominator)}"
}
