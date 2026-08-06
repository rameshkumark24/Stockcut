package com.stockcut.ui.about

import com.stockcut.data.entitlement.Tier
import com.stockcut.units.UnitSystem

/**
 * The diagnostics string shown in the feedback form and the support email.
 *
 * Format from docs/09 §4.1 — one compact line, not six fields, because the user
 * has to read it and decide about it:
 *
 *   v1.0.3 (12) | Android 13 | SM-A515F | mm 1/16 | free | 47 optimizes
 *
 * 🔴 CLAUDE.md rule 11 — NEVER add any of these:
 *   advertising ID · install ID · location · project contents · anything the
 *   user cannot see on screen before it is sent.
 *
 * Each field earns its place: app version is the first thing you would ask;
 * Android version and device model reproduce OS- and model-specific bugs; the
 * unit mode matters because fractional-inch handling is the predicted failure
 * mode for the US market; tier tells you whether a paying customer is unhappy;
 * and the optimize count separates first-run confusion from a power user
 * hitting a real limit.
 *
 * Pure Kotlin with primitive parameters, so the rule above is pinned by a JVM
 * test rather than trusted.
 */
fun buildDiagnostics(
    appVersionName: String,
    appVersionCode: Int,
    androidRelease: String,
    deviceModel: String,
    unitSystem: UnitSystem,
    denominator: Int,
    tier: Tier,
    optimizeCount: Int,
): String {
    val unit = when (unitSystem) {
        UnitSystem.MM -> "mm"
        UnitSystem.CM -> "cm"
        UnitSystem.M -> "m"
        UnitSystem.INCH_DECIMAL -> "inch"
        UnitSystem.INCH_FRACTIONAL -> "inch 1/$denominator"
    }
    val plan = if (tier == Tier.PAID) "paid" else "free"
    val optimizes = if (optimizeCount == 1) "1 optimize" else "$optimizeCount optimizes"

    return "v$appVersionName ($appVersionCode) | Android $androidRelease | " +
        "$deviceModel | $unit | $plan | $optimizes"
}
