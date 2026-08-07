package com.stockcut.data

import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.entitlement.Gate
import com.stockcut.data.entitlement.Limits
import com.stockcut.data.entitlement.PaywallTrigger
import com.stockcut.data.entitlement.Tier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EntitlementTest {

    // ── The two principles ────────────────────────────────────────────────────

    @Test
    fun `correctness is never gated`() {
        // Free and paid run the identical optimizer. If this test ever needs
        // changing, the business model has gone wrong, not the code.
        assertIs<Gate.Allowed>(Entitlement.canOptimize(Tier.FREE))
        assertIs<Gate.Allowed>(Entitlement.canOptimize(Tier.PAID))
    }

    @Test
    fun `existing data stays editable at the limit`() {
        // A free user with 20 parts can still edit and delete them. Limits block
        // ADDING only — never lock someone out of work they already entered.
        assertIs<Gate.Allowed>(Entitlement.canEditExisting(Tier.FREE))
        assertIs<Gate.Allowed>(Entitlement.canEditExisting(Tier.PAID))
    }

    @Test
    fun `sharing as an image is free, because it is how the app spreads`() {
        assertIs<Gate.Allowed>(Entitlement.canShareImage(Tier.FREE))
    }

    // ── Part limits ───────────────────────────────────────────────────────────

    @Test
    fun `free tier allows exactly twenty pieces`() {
        assertIs<Gate.Allowed>(Entitlement.canAddParts(Tier.FREE, currentTotalQuantity = 19))
        val blocked = Entitlement.canAddParts(Tier.FREE, currentTotalQuantity = 20)
        assertEquals(PaywallTrigger.PARTS, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
    }

    @Test
    fun `the limit counts pieces, not rows`() {
        // One row of quantity 20 is already at the limit; a 21st piece is blocked.
        assertIs<Gate.NeedsUpgrade>(Entitlement.canAddParts(Tier.FREE, 20, adding = 1))
        // Adding a row of 5 when 18 are present would reach 23 — blocked.
        assertIs<Gate.NeedsUpgrade>(Entitlement.canAddParts(Tier.FREE, 18, adding = 5))
        // Adding 2 when 18 are present reaches exactly 20 — allowed.
        assertIs<Gate.Allowed>(Entitlement.canAddParts(Tier.FREE, 18, adding = 2))
    }

    @Test
    fun `paid tier is limited only by the performance cap`() {
        assertIs<Gate.Allowed>(Entitlement.canAddParts(Tier.PAID, 999))
        assertIs<Gate.HardLimit>(Entitlement.canAddParts(Tier.PAID, 1_000))
    }

    @Test
    fun `the performance cap is never dressed up as a paywall`() {
        // Offering an upgrade to a user who already paid would be a lie.
        val gate = Entitlement.canAddParts(Tier.PAID, Limits.MAX_PARTS_PER_PROJECT)
        val hard = assertIs<Gate.HardLimit>(gate)
        assertTrue(hard.message.isNotBlank())
        assertFalse(hard.message.contains("unlock", ignoreCase = true))
        assertFalse(hard.message.contains("upgrade", ignoreCase = true))
    }

    @Test
    fun `the hard cap outranks the paywall for a free user too`() {
        // A free user cannot reach 1000 legitimately, but if state is ever wrong,
        // the honest message wins over the sales one.
        assertIs<Gate.HardLimit>(Entitlement.canAddParts(Tier.FREE, 1_000))
    }

    // ── Project and stock limits ──────────────────────────────────────────────

    @Test
    fun `free tier saves one job`() {
        assertIs<Gate.Allowed>(Entitlement.canAddProject(Tier.FREE, currentProjectCount = 0))
        val blocked = Entitlement.canAddProject(Tier.FREE, currentProjectCount = 1)
        assertEquals(PaywallTrigger.PROJECTS, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
        assertIs<Gate.Allowed>(Entitlement.canAddProject(Tier.PAID, currentProjectCount = 500))
    }

    @Test
    fun `free tier allows five stock entries`() {
        assertIs<Gate.Allowed>(Entitlement.canAddStock(Tier.FREE, 4))
        val blocked = Entitlement.canAddStock(Tier.FREE, 5)
        assertEquals(PaywallTrigger.STOCK, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
    }

    @Test
    fun `pdf export is paid, and names itself as the trigger`() {
        val blocked = Entitlement.canExportPdf(Tier.FREE)
        assertEquals(PaywallTrigger.PDF_EXPORT, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
        assertIs<Gate.Allowed>(Entitlement.canExportPdf(Tier.PAID))
    }

    @Test
    fun `every paywall trigger is reachable, so every headline gets used`() {
        val reached = setOf(
            (Entitlement.canAddParts(Tier.FREE, 20) as Gate.NeedsUpgrade).trigger,
            (Entitlement.canAddProject(Tier.FREE, 1) as Gate.NeedsUpgrade).trigger,
            (Entitlement.canAddStock(Tier.FREE, 5) as Gate.NeedsUpgrade).trigger,
            (Entitlement.canExportPdf(Tier.FREE) as Gate.NeedsUpgrade).trigger,
        )
        assertEquals(PaywallTrigger.entries.toSet(), reached)
    }

    // ── Ads ───────────────────────────────────────────────────────────────────

    @Test
    fun `paying removes ads immediately`() {
        assertTrue(Entitlement.showsAds(Tier.FREE))
        assertFalse(Entitlement.showsAds(Tier.PAID))
        assertFalse(Entitlement.interstitialDue(Tier.PAID, 3))
        assertFalse(Entitlement.interstitialDue(Tier.PAID, 99))
    }

    @Test
    fun `interstitial fires on every fifth optimize, never on the first`() {
        // Was every third. Raised deliberately — the count is lifetime, this app
        // is used iteratively, and 1-in-3 interruptions at the exact moment the
        // answer appears is how a tool gets uninstalled. See the note on
        // Limits.INTERSTITIAL_EVERY.
        assertFalse(Entitlement.interstitialDue(Tier.FREE, 0))
        for (count in 1..4) assertFalse(Entitlement.interstitialDue(Tier.FREE, count))
        assertTrue(Entitlement.interstitialDue(Tier.FREE, 5))
        assertFalse(Entitlement.interstitialDue(Tier.FREE, 6))
        assertTrue(Entitlement.interstitialDue(Tier.FREE, 10))
    }

    @Test
    fun `🔴 two interstitials never land inside the minimum gap`() {
        // Protects the person adjusting one job, who re-optimizes repeatedly.
        val shown = 5_000_000L
        assertFalse(
            Entitlement.interstitialDue(
                tier = Tier.FREE,
                optimizeCount = 10,
                lastInterstitialAtMillis = shown,
                nowMillis = shown + 60_000,
            ),
        )
        assertTrue(
            Entitlement.interstitialDue(
                tier = Tier.FREE,
                optimizeCount = 10,
                lastInterstitialAtMillis = shown,
                nowMillis = shown + Limits.INTERSTITIAL_MIN_GAP_MILLIS + 1,
            ),
        )
    }

    // ── Review prompt ─────────────────────────────────────────────────────────

    @Test
    fun `review prompt waits for three optimizes and never fires on launch`() {
        assertFalse(Entitlement.reviewPromptDue(0, 0L, NOW))
        assertFalse(Entitlement.reviewPromptDue(2, 0L, NOW))
        assertTrue(Entitlement.reviewPromptDue(3, 0L, NOW))
    }

    @Test
    fun `review prompt respects a ninety day cooldown`() {
        val ninetyDays = 90L * 24 * 60 * 60 * 1000
        assertFalse(Entitlement.reviewPromptDue(50, NOW - ninetyDays + 1, NOW))
        assertTrue(Entitlement.reviewPromptDue(50, NOW - ninetyDays, NOW))
    }

    private companion object {
        const val NOW = 1_785_000_000_000L
    }
}
