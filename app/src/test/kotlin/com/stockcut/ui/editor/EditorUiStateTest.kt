package com.stockcut.ui.editor

import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.entitlement.Gate
import com.stockcut.data.entitlement.Limits
import com.stockcut.data.entitlement.PaywallTrigger
import com.stockcut.data.entitlement.Tier
import com.stockcut.data.model.PartEntry
import com.stockcut.data.model.Project
import com.stockcut.data.model.StockEntry
import com.stockcut.units.UnitSystem
import com.stockcut.units.U_PER_MM
import com.stockcut.units.format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The editor's rules, on the JVM.
 *
 * The two that matter are test-plan item 03 (changing units must not mutate a
 * stored value) and the entitlement boundary (limits block adding, never
 * editing). Both are the kind of thing a later refactor breaks silently.
 */
class EditorUiStateTest {

    private fun project(
        unitSystem: UnitSystem = UnitSystem.MM,
        denominator: Int = 16,
    ) = Project(
        id = 1,
        name = "Gate",
        unitSystem = unitSystem,
        fractionDenominator = denominator,
        kerfU = 3 * U_PER_MM,
        trimU = 0,
        isExample = false,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun state(
        tier: Tier = Tier.FREE,
        unitSystem: UnitSystem = UnitSystem.MM,
        parts: List<PartEntry> = emptyList(),
        stock: List<StockEntry> = emptyList(),
    ) = EditorUiState(
        project = project(unitSystem),
        parts = parts,
        stock = stock,
        tier = tier,
    )

    private fun part(lengthMm: Long, qty: Int, id: Long = 1) =
        PartEntry(id, lengthMm * U_PER_MM, qty, null, 0)

    // ── Test plan item 03 ────────────────────────────────────────────────────

    @Test
    fun `changing units re-formats every row without touching a stored length`() {
        val parts = listOf(part(1_200, 2, id = 1), part(600, 1, id = 2))
        val metric = state(unitSystem = UnitSystem.MM, parts = parts)

        assertEquals("1200", format(parts[0].lengthU, metric.unitSystem, metric.denominator))

        // The same rows, read through a project set to fractional inches.
        val imperial = state(unitSystem = UnitSystem.INCH_FRACTIONAL, parts = parts)
        assertEquals("3' 11 1/4\"", format(parts[0].lengthU, imperial.unitSystem, imperial.denominator))

        // The underlying values are the same objects, unchanged.
        assertEquals(1_200 * U_PER_MM, imperial.parts[0].lengthU)
        assertEquals(600 * U_PER_MM, imperial.parts[1].lengthU)
    }

    @Test
    fun `a value survives a full round trip through every unit system`() {
        val original = part(1_200, 1)
        for (system in UnitSystem.entries) {
            val s = state(unitSystem = system, parts = listOf(original))
            assertEquals(
                original.lengthU,
                s.parts.single().lengthU,
                "$system mutated the stored length",
            )
        }
    }

    // ── Counters ─────────────────────────────────────────────────────────────

    @Test
    fun `the parts counter counts pieces, not rows`() {
        val s = state(parts = listOf(part(100, 7, id = 1), part(200, 5, id = 2)))
        assertEquals(12, s.totalPieces)
    }

    @Test
    fun `🔴 the counter advertises no limit while there is none to enforce`() {
        // "12 / 20 pieces" in a build with no paywall would stop a user at 20 on
        // a job the app would have cut without complaint — the label inventing a
        // ceiling that the gate does not apply.
        val free = state(parts = listOf(part(100, 7, id = 1), part(200, 5, id = 2)))
        assertEquals("12 pieces", free.partsCountLabel)

        val paid = state(tier = Tier.PAID, parts = listOf(part(100, 7)))
        assertEquals("7 pieces", paid.partsCountLabel)
    }

    // ── Optimize ─────────────────────────────────────────────────────────────

    @Test
    fun `optimize needs both sides of the problem`() {
        assertFalse(state().canOptimize, "no parts and no stock")
        assertFalse(state(parts = listOf(part(100, 1))).canOptimize, "no stock")
        assertFalse(
            state(stock = listOf(StockEntry(1, 6_000 * U_PER_MM, -1, null, 0))).canOptimize,
            "no parts",
        )
        assertTrue(
            state(
                parts = listOf(part(100, 1)),
                stock = listOf(StockEntry(1, 6_000 * U_PER_MM, -1, null, 0)),
            ).canOptimize,
        )
    }

    // ── Entitlement boundary ─────────────────────────────────────────────────

    @Test
    fun `nothing the editor can do raises a paywall in this build`() {
        assertIs<Gate.Allowed>(Entitlement.canAddParts(Tier.FREE, currentTotalQuantity = 20))
        assertIs<Gate.Allowed>(
            Entitlement.canAddParts(Tier.FREE, currentTotalQuantity = 18, adding = 5),
        )
        assertIs<Gate.Allowed>(Entitlement.canAddStock(Tier.FREE, currentStockCount = 5))
    }

    @Test
    fun `the free limit blocks the twenty-first piece and names its trigger`() {
        // Dormant rule — see PaywallRulesTest for why these stay covered.
        assertIs<Gate.Allowed>(
            Entitlement.canAddParts(Tier.FREE, 19, paywallEnabled = true),
        )
        val gate = Entitlement.canAddParts(Tier.FREE, 20, paywallEnabled = true)
        assertEquals(PaywallTrigger.PARTS, assertIs<Gate.NeedsUpgrade>(gate).trigger)
    }

    @Test
    fun `a quantity that would jump the limit is caught, not just a single piece`() {
        // Adding 5 to an existing 18 must be blocked, even though 18 is under 20.
        val gate = Entitlement.canAddParts(Tier.FREE, 18, adding = 5, paywallEnabled = true)
        assertIs<Gate.NeedsUpgrade>(gate)
    }

    @Test
    fun `the thousand-piece cap is a hard limit, not a paywall, in either build`() {
        // Money cannot lift this one, so it must never be dressed as an offer —
        // and it must keep applying now that there is no offer to make at all.
        for (paywall in listOf(true, false)) {
            val gate = Entitlement.canAddParts(
                Tier.PAID,
                currentTotalQuantity = Limits.MAX_PARTS_PER_PROJECT,
                paywallEnabled = paywall,
            )
            val hard = assertIs<Gate.HardLimit>(gate, "paywallEnabled=$paywall")
            assertTrue(hard.message.contains("${Limits.MAX_PARTS_PER_PROJECT}"))
        }
        assertIs<Gate.HardLimit>(Entitlement.canAddParts(Tier.FREE, 1_500))
    }

    @Test
    fun `the free stock limit is five`() {
        assertIs<Gate.Allowed>(Entitlement.canAddStock(Tier.FREE, 4, paywallEnabled = true))
        val gate = Entitlement.canAddStock(Tier.FREE, 5, paywallEnabled = true)
        assertEquals(PaywallTrigger.STOCK, assertIs<Gate.NeedsUpgrade>(gate).trigger)
    }

    @Test
    fun `editing existing rows is never gated, at any tier or count`() {
        // Never lock someone out of data they already entered (docs/02 §6).
        assertIs<Gate.Allowed>(Entitlement.canEditExisting(Tier.FREE))
        assertIs<Gate.Allowed>(Entitlement.canEditExisting(Tier.PAID))
    }

    @Test
    fun `optimizing is never gated - correctness is not for sale`() {
        assertIs<Gate.Allowed>(Entitlement.canOptimize(Tier.FREE))
        assertIs<Gate.Allowed>(Entitlement.canOptimize(Tier.PAID))
    }
}

/**
 * Ad frequency. These exist because the cost of getting it wrong is an
 * uninstall, and an uninstalled app earns nothing at all.
 */
class AdCadenceTest {

    @Test
    fun `a paid user never sees an interstitial`() {
        // Dormant rule. In THIS build nobody can pay, so everybody sees ads —
        // that case is asserted in EntitlementTest.
        assertFalse(Entitlement.interstitialDue(Tier.PAID, 5, paywallEnabled = true))
        assertFalse(Entitlement.interstitialDue(Tier.PAID, 50, paywallEnabled = true))
    }

    @Test
    fun `the free tier sees one every fifth optimize, not every third`() {
        // Raised from 3 deliberately — see the note on Limits.INTERSTITIAL_EVERY.
        for (count in 1..4) {
            assertFalse(
                Entitlement.interstitialDue(Tier.FREE, count),
                "interrupted at optimize $count",
            )
        }
        assertTrue(Entitlement.interstitialDue(Tier.FREE, 5))
    }

    @Test
    fun `🔴 iterating on one job does not get ad after ad`() {
        // The scenario that gets an app deleted: someone adjusting a job hits
        // Optimize repeatedly, and the count alone would interrupt them again
        // as soon as it came round.
        val justShown = 1_000_000L
        assertFalse(
            Entitlement.interstitialDue(
                tier = Tier.FREE,
                optimizeCount = 10,
                lastInterstitialAtMillis = justShown,
                nowMillis = justShown + 60_000, // one minute later
            ),
            "showed two interstitials a minute apart",
        )
    }

    @Test
    fun `after the gap has passed, the cadence resumes`() {
        val earlier = 1_000_000L
        assertTrue(
            Entitlement.interstitialDue(
                tier = Tier.FREE,
                optimizeCount = 10,
                lastInterstitialAtMillis = earlier,
                nowMillis = earlier + Limits.INTERSTITIAL_MIN_GAP_MILLIS + 1,
            ),
        )
    }

    @Test
    fun `a first-time user is not interrupted before they have used anything`() {
        assertFalse(Entitlement.interstitialDue(Tier.FREE, optimizeCount = 0))
    }
}
