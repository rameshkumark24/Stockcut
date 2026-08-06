package com.stockcut.ui.about

import com.stockcut.data.entitlement.Tier
import com.stockcut.units.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The diagnostics string is the one thing this app sends about a user, so what
 * it does NOT contain is the part worth testing.
 *
 * docs/09 §6: with no contact field, the form adds no data-safety category that
 * Crashlytics does not already force. That claim only stays true if this string
 * stays this small — which is why an inaccurate declaration is a suspension risk
 * and why CLAUDE.md rule 11 says the Play declarations change in the same PR as
 * this function.
 */
class DiagnosticsTest {

    private fun diagnostics(
        unitSystem: UnitSystem = UnitSystem.MM,
        denominator: Int = 16,
        tier: Tier = Tier.FREE,
        optimizeCount: Int = 47,
    ) = buildDiagnostics(
        appVersionName = "1.0.3",
        appVersionCode = 12,
        androidRelease = "13",
        deviceModel = "SM-A515F",
        unitSystem = unitSystem,
        denominator = denominator,
        tier = tier,
        optimizeCount = optimizeCount,
    )

    @Test
    fun `matches the format in the feedback channel doc`() {
        assertEquals(
            "v1.0.3 (12) | Android 13 | SM-A515F | mm | free | 47 optimizes",
            diagnostics(),
        )
    }

    @Test
    fun `fractional inch reports its denominator, because that is the predicted failure mode`() {
        val line = diagnostics(unitSystem = UnitSystem.INCH_FRACTIONAL, denominator = 32)
        assertTrue(line.contains("inch 1/32"), line)
    }

    @Test
    fun `a paying customer is identifiable as paying, so their report can be prioritised`() {
        assertTrue(diagnostics(tier = Tier.PAID).contains("| paid |"))
        assertTrue(diagnostics(tier = Tier.FREE).contains("| free |"))
    }

    @Test
    fun `one optimize is singular`() {
        assertTrue(diagnostics(optimizeCount = 1).contains("1 optimize |") ||
            diagnostics(optimizeCount = 1).endsWith("1 optimize"))
        assertTrue(diagnostics(optimizeCount = 2).endsWith("2 optimizes"))
    }

    @Test
    fun `it is one short line, because the user has to read it before sending`() {
        // Six fields on one line, per docs/09 §4.1 — "easier for the user to read
        // and decide about, and easier for you to scan in a spreadsheet".
        val line = diagnostics()
        assertFalse(line.contains("\n"), "diagnostics must be a single line")
        assertEquals(6, line.split(" | ").size)
        assertTrue(line.length < 120, "too long to read on a phone: ${line.length} chars")
    }

    @Test
    fun `🔴 it carries nothing that identifies a person`() {
        // The function takes only these parameters, so there is physically
        // nowhere for an advertising ID, install ID, location or project
        // contents to come from. This test states the intent so that adding such
        // a parameter is a deliberate act that breaks a named rule, not a quiet
        // one-line change.
        val line = diagnostics(unitSystem = UnitSystem.INCH_FRACTIONAL, tier = Tier.PAID)

        for (forbidden in listOf("lat", "lon", "gps", "uuid", "advertis", "install-id", "androidid")) {
            assertFalse(
                line.lowercase().contains(forbidden),
                "diagnostics leaked something matching '$forbidden': $line",
            )
        }
        // No project data: no job name, no part length, no stock length.
        assertFalse(line.contains("gate"), "a project name reached the diagnostics")
        assertFalse(Regex("\\b\\d{3,}\\b").containsMatchIn(line.substringAfter("SM-A515F")),
            "a suspiciously length-like number reached the diagnostics: $line")
    }
}
