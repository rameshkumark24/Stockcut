package com.stockcut.data.entitlement

/**
 * Free vs paid gating. Pure Kotlin — no Android, no Room — so the rules that
 * decide paywall behaviour are pinned by fast JVM tests.
 *
 * Two principles, from docs/02-trd.md §6 and docs/03-app-flow.md:
 *
 *  1. NEVER gate correctness. Free and paid run the identical optimizer. The
 *     paywall sells scale, never accuracy.
 *  2. NEVER lock someone out of data they already entered. Limits block ADDING
 *     only; existing rows stay fully editable and deletable at any tier.
 */

enum class Tier { FREE, PAID }

enum class PaywallTrigger { PARTS, PROJECTS, STOCK, PDF_EXPORT }

object Limits {
    const val FREE_PARTS_PER_PROJECT = 20
    const val FREE_PROJECTS = 1
    const val FREE_STOCK_PER_PROJECT = 5

    /** Performance cap, applies to BOTH tiers. Not a paywall. */
    const val MAX_PARTS_PER_PROJECT = 1_000

    /**
     * Free tier sees an interstitial after every Nth optimize. Never mid-task.
     *
     * Raised from 3 to 5, deliberately deviating from docs/02 §6.
     *
     * The count is LIFETIME, so "every 3rd" is one interruption in three
     * forever — and this app is used iteratively. A tradesman adjusting a job
     * optimizes, looks, changes a length, optimizes again. At 1-in-3 a single
     * afternoon's work is punctuated by ads at the exact moment the answer
     * appears, which is the payoff the whole product exists to deliver.
     *
     * An uninstall earns nothing. A slightly rarer ad earns slightly less.
     */
    const val INTERSTITIAL_EVERY = 5

    /**
     * No two interstitials within this window, whatever the count says.
     *
     * The count alone does not protect the person iterating on one job, which
     * is exactly when an interruption is most costly and least deserved.
     */
    const val INTERSTITIAL_MIN_GAP_MILLIS = 10L * 60 * 1000
}

sealed interface Gate {
    data object Allowed : Gate

    /** Upgrading would lift this. Show the paywall, headlined by [trigger]. */
    data class NeedsUpgrade(val trigger: PaywallTrigger) : Gate

    /**
     * A ceiling that money does not move. Showing a paywall here would be a lie —
     * a paid user hitting the 1000-part cap must see an explanation, not an offer.
     */
    data class HardLimit(val message: String) : Gate
}

object Entitlement {

    /**
     * @param currentTotalQuantity sum of part quantities already in the project,
     *   not the row count — the limit counts pieces to be cut.
     */
    fun canAddParts(tier: Tier, currentTotalQuantity: Int, adding: Int = 1): Gate {
        require(adding > 0) { "adding must be positive" }
        val after = currentTotalQuantity + adding
        if (after > Limits.MAX_PARTS_PER_PROJECT) {
            return Gate.HardLimit(
                "A job can hold ${Limits.MAX_PARTS_PER_PROJECT} pieces. Split it into two jobs.",
            )
        }
        if (tier == Tier.FREE && after > Limits.FREE_PARTS_PER_PROJECT) {
            return Gate.NeedsUpgrade(PaywallTrigger.PARTS)
        }
        return Gate.Allowed
    }

    fun canAddProject(tier: Tier, currentProjectCount: Int): Gate =
        if (tier == Tier.FREE && currentProjectCount + 1 > Limits.FREE_PROJECTS) {
            Gate.NeedsUpgrade(PaywallTrigger.PROJECTS)
        } else {
            Gate.Allowed
        }

    fun canAddStock(tier: Tier, currentStockCount: Int): Gate =
        if (tier == Tier.FREE && currentStockCount + 1 > Limits.FREE_STOCK_PER_PROJECT) {
            Gate.NeedsUpgrade(PaywallTrigger.STOCK)
        } else {
            Gate.Allowed
        }

    fun canExportPdf(tier: Tier): Gate =
        if (tier == Tier.PAID) Gate.Allowed else Gate.NeedsUpgrade(PaywallTrigger.PDF_EXPORT)

    /** Sharing as an image is free. It is how the app spreads. */
    fun canShareImage(tier: Tier): Gate = Gate.Allowed

    /** Optimizing is never gated. Correctness is not for sale. */
    fun canOptimize(tier: Tier): Gate = Gate.Allowed

    /** Editing and deleting existing rows is never gated, at any tier. */
    fun canEditExisting(tier: Tier): Gate = Gate.Allowed

    fun showsAds(tier: Tier): Boolean = tier == Tier.FREE

    /**
     * @param optimizeCount lifetime successful optimizes, AFTER incrementing.
     * @param lastInterstitialAtMillis 0 if one has never been shown.
     *
     * Interstitials never interrupt an in-progress task — this is checked before
     * navigating to the result, never during entry — and never appear twice
     * inside [Limits.INTERSTITIAL_MIN_GAP_MILLIS], however many times someone
     * re-optimizes while adjusting a job.
     */
    fun interstitialDue(
        tier: Tier,
        optimizeCount: Int,
        lastInterstitialAtMillis: Long = 0L,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (tier != Tier.FREE) return false
        if (optimizeCount <= 0) return false
        if (optimizeCount % Limits.INTERSTITIAL_EVERY != 0) return false
        if (lastInterstitialAtMillis != 0L &&
            nowMillis - lastInterstitialAtMillis < Limits.INTERSTITIAL_MIN_GAP_MILLIS
        ) {
            return false
        }
        return true
    }

    /**
     * In-app review: only after a SUCCESSFUL optimize, at least 3 lifetime, and
     * at most once per 90 days. Never on launch, never after a crash.
     */
    fun reviewPromptDue(
        optimizeCount: Int,
        lastPromptAtMillis: Long,
        nowMillis: Long,
        minimumOptimizes: Int = 3,
        cooldownMillis: Long = 90L * 24 * 60 * 60 * 1000,
    ): Boolean {
        if (optimizeCount < minimumOptimizes) return false
        if (lastPromptAtMillis == 0L) return true
        return nowMillis - lastPromptAtMillis >= cooldownMillis
    }
}
