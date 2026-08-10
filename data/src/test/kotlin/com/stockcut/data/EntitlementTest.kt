package com.stockcut.data

import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.entitlement.Gate
import com.stockcut.data.entitlement.Limits
import com.stockcut.data.entitlement.Monetization
import com.stockcut.data.entitlement.PaywallTrigger
import com.stockcut.data.entitlement.Tier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What this build actually does — the paywall is OFF and StockCut is free.
 *
 * The dormant paywall rules are covered separately in [PaywallRulesTest]; the two
 * files exist because they answer different questions. This one answers "is the
 * app free right now", which is a shipping decision, and it must fail loudly if
 * someone re-enables the paywall without meaning to.
 */
class EntitlementTest {

    @Test
    fun `🔴 this build sells nothing`() {
        // The guard on everything below. If this fails, the app has started
        // charging users — deliberately or otherwise — and every other
        // expectation in this file is void.
        assertFalse(
            Monetization.PAYWALL_ENABLED,
            "The paywall was switched on. That is a product decision, not a " +
                "refactor: read the note on Monetization, and do not enable it " +
                "without the grandfathering rule that protects existing users.",
        )
    }

    // ── The two principles, which hold at any tier and in any build ───────────

    @Test
    fun `correctness is never gated`() {
        assertIs<Gate.Allowed>(Entitlement.canOptimize(Tier.FREE))
        assertIs<Gate.Allowed>(Entitlement.canOptimize(Tier.PAID))
    }

    @Test
    fun `existing data stays editable at the limit`() {
        assertIs<Gate.Allowed>(Entitlement.canEditExisting(Tier.FREE))
        assertIs<Gate.Allowed>(Entitlement.canEditExisting(Tier.PAID))
    }

    @Test
    fun `sharing as an image is free, because it is how the app spreads`() {
        assertIs<Gate.Allowed>(Entitlement.canShareImage(Tier.FREE))
    }

    // ── Nothing is gated ─────────────────────────────────────────────────────

    @Test
    fun `no number of parts raises a paywall`() {
        for (count in listOf(0, 19, 20, 21, 100, 999)) {
            assertIs<Gate.Allowed>(
                Entitlement.canAddParts(Tier.FREE, currentTotalQuantity = count),
                "$count pieces was gated in a build with nothing for sale",
            )
        }
    }

    @Test
    fun `jobs, stock and PDF export are all free`() {
        assertIs<Gate.Allowed>(Entitlement.canAddProject(Tier.FREE, currentProjectCount = 50))
        assertIs<Gate.Allowed>(Entitlement.canAddStock(Tier.FREE, currentStockCount = 50))
        assertIs<Gate.Allowed>(Entitlement.canExportPdf(Tier.FREE))
    }

    // ── The hard cap is NOT a paywall and must survive ────────────────────────

    @Test
    fun `the thousand-piece cap still applies with the paywall off`() {
        // It protects the optimizer, not the revenue, so removing the paywall
        // must not remove it. Easy thing to lose while lifting "limits".
        assertIs<Gate.Allowed>(Entitlement.canAddParts(Tier.FREE, 999))
        assertIs<Gate.HardLimit>(Entitlement.canAddParts(Tier.FREE, 1_000))
    }

    @Test
    fun `the performance cap is never dressed up as a paywall`() {
        val gate = Entitlement.canAddParts(Tier.FREE, Limits.MAX_PARTS_PER_PROJECT)
        val hard = assertIs<Gate.HardLimit>(gate)
        assertTrue(hard.message.isNotBlank())
        assertFalse(hard.message.contains("unlock", ignoreCase = true))
        assertFalse(hard.message.contains("upgrade", ignoreCase = true))
    }

    // ── Ads ──────────────────────────────────────────────────────────────────

    @Test
    fun `🔴 everyone sees ads, because ads are the only income`() {
        // Including anyone carrying a stale isUnlocked from a pre-launch test
        // build. There is no way to buy ads away, so nobody is exempt — a tier
        // check that quietly exempted someone would mean unmonetised traffic
        // forever, and nothing in the UI would ever show it.
        assertTrue(Entitlement.showsAds(Tier.FREE))
        assertTrue(Entitlement.showsAds(Tier.PAID))
        assertTrue(Entitlement.interstitialDue(Tier.PAID, 5))
    }

    @Test
    fun `interstitial fires on every fifth optimize, never on the first`() {
        assertFalse(Entitlement.interstitialDue(Tier.FREE, 0))
        for (count in 1..4) assertFalse(Entitlement.interstitialDue(Tier.FREE, count))
        assertTrue(Entitlement.interstitialDue(Tier.FREE, 5))
        assertFalse(Entitlement.interstitialDue(Tier.FREE, 6))
        assertTrue(Entitlement.interstitialDue(Tier.FREE, 10))
    }

    @Test
    fun `🔴 two interstitials never land inside the minimum gap`() {
        // Protects the person adjusting one job, who re-optimizes repeatedly.
        // More important now, not less: with no purchase to escape to, an
        // over-served ad has no remedy but uninstalling.
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

    // ── Review prompt — independent of monetisation ──────────────────────────

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

/**
 * The paywall rules, exercised with `paywallEnabled = true`.
 *
 * 🔴 These do NOT describe the shipping app. They keep the dormant paywall
 * honest so that switching it back on is a one-line change to code that has been
 * tested every single build in the meantime, rather than a rediscovery of
 * whatever rotted while it sat unused.
 *
 * Every assertion here was passing before the paywall was switched off; the only
 * change is the explicit flag.
 */
class PaywallRulesTest {

    @Test
    fun `free tier allows exactly twenty pieces`() {
        assertIs<Gate.Allowed>(Entitlement.canAddParts(Tier.FREE, 19, paywallEnabled = true))
        val blocked = Entitlement.canAddParts(Tier.FREE, 20, paywallEnabled = true)
        assertEquals(PaywallTrigger.PARTS, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
    }

    @Test
    fun `the limit counts pieces, not rows`() {
        assertIs<Gate.NeedsUpgrade>(
            Entitlement.canAddParts(Tier.FREE, 20, adding = 1, paywallEnabled = true),
        )
        // Adding a row of 5 when 18 are present would reach 23 — blocked.
        assertIs<Gate.NeedsUpgrade>(
            Entitlement.canAddParts(Tier.FREE, 18, adding = 5, paywallEnabled = true),
        )
        // Adding 2 when 18 are present reaches exactly 20 — allowed.
        assertIs<Gate.Allowed>(
            Entitlement.canAddParts(Tier.FREE, 18, adding = 2, paywallEnabled = true),
        )
    }

    @Test
    fun `paid tier is limited only by the performance cap`() {
        assertIs<Gate.Allowed>(Entitlement.canAddParts(Tier.PAID, 999, paywallEnabled = true))
        assertIs<Gate.HardLimit>(Entitlement.canAddParts(Tier.PAID, 1_000, paywallEnabled = true))
    }

    @Test
    fun `the hard cap outranks the paywall for a free user too`() {
        assertIs<Gate.HardLimit>(Entitlement.canAddParts(Tier.FREE, 1_000, paywallEnabled = true))
    }

    @Test
    fun `free tier saves one job`() {
        assertIs<Gate.Allowed>(Entitlement.canAddProject(Tier.FREE, 0, paywallEnabled = true))
        val blocked = Entitlement.canAddProject(Tier.FREE, 1, paywallEnabled = true)
        assertEquals(PaywallTrigger.PROJECTS, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
        assertIs<Gate.Allowed>(Entitlement.canAddProject(Tier.PAID, 500, paywallEnabled = true))
    }

    @Test
    fun `free tier allows five stock entries`() {
        assertIs<Gate.Allowed>(Entitlement.canAddStock(Tier.FREE, 4, paywallEnabled = true))
        val blocked = Entitlement.canAddStock(Tier.FREE, 5, paywallEnabled = true)
        assertEquals(PaywallTrigger.STOCK, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
    }

    @Test
    fun `pdf export is paid, and names itself as the trigger`() {
        val blocked = Entitlement.canExportPdf(Tier.FREE, paywallEnabled = true)
        assertEquals(PaywallTrigger.PDF_EXPORT, assertIs<Gate.NeedsUpgrade>(blocked).trigger)
        assertIs<Gate.Allowed>(Entitlement.canExportPdf(Tier.PAID, paywallEnabled = true))
    }

    @Test
    fun `every paywall trigger is reachable, so every headline gets used`() {
        val reached = setOf(
            (Entitlement.canAddParts(Tier.FREE, 20, paywallEnabled = true) as Gate.NeedsUpgrade)
                .trigger,
            (Entitlement.canAddProject(Tier.FREE, 1, paywallEnabled = true) as Gate.NeedsUpgrade)
                .trigger,
            (Entitlement.canAddStock(Tier.FREE, 5, paywallEnabled = true) as Gate.NeedsUpgrade)
                .trigger,
            (Entitlement.canExportPdf(Tier.FREE, paywallEnabled = true) as Gate.NeedsUpgrade)
                .trigger,
        )
        assertEquals(PaywallTrigger.entries.toSet(), reached)
    }

    @Test
    fun `paying removes ads immediately`() {
        assertTrue(Entitlement.showsAds(Tier.FREE, paywallEnabled = true))
        assertFalse(Entitlement.showsAds(Tier.PAID, paywallEnabled = true))
        assertFalse(Entitlement.interstitialDue(Tier.PAID, 3, paywallEnabled = true))
        assertFalse(Entitlement.interstitialDue(Tier.PAID, 99, paywallEnabled = true))
    }
}
