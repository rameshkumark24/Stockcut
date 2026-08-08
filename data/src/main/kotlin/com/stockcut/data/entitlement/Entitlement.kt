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

/**
 * The single switch that decides whether this app sells anything.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CURRENTLY OFF. StockCut v1 is completely free and earns only from AdMob.
 *
 * WHY, so nobody "fixes" this back:
 *
 * v1 launches on a friend's Play Console account, because a developer account
 * costs $25 and the owner does not have it yet. Play Billing pays the ACCOUNT
 * HOLDER — there is no way to route in-app purchase money to anyone else. So
 * shipping the unlock would put this app's main revenue line in someone else's
 * bank account and on someone else's tax record, recoverable only by a promise.
 *
 * Turning the paywall off removes that problem entirely rather than managing it.
 * AdMob is unaffected: ad unit IDs are compiled into the APK and AdMob pays
 * whoever owns the AdMob account, whoever published the app.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * TURNING IT BACK ON (planned, once the app is transferred to the owner's own
 * account — see docs/15-free-launch-and-paywall-plan.md)
 *
 * Flipping this to `true` restores every limit for everyone, including people
 * who have used the app unlimited for months. Do not flip it alone. Users who
 * installed before the cutoff must be grandfathered — the hook for that is
 * [com.stockcut.data.settings.Settings.firstRunAt], which v1 records precisely
 * so this stays possible.
 *
 * 🔴 That hook only works because it ships in v1. It cannot be added later:
 * there is no way to find out, in six months, when someone installed if the app
 * never wrote it down.
 */
object Monetization {
    const val PAYWALL_ENABLED = false
}

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

    /*
     * Every gate takes `paywallEnabled` as a defaulted parameter rather than
     * reading Monetization directly.
     *
     * That is not indirection for its own sake. PAYWALL_ENABLED is a compile-time
     * const, so a test cannot flip it — and without this parameter the paywall
     * rules would be completely untestable for as long as they are switched off.
     * They would then be re-enabled in six months having not been exercised once
     * in the interim, which is exactly how dormant code comes back broken.
     *
     * Production call sites pass nothing and get the shipped behaviour.
     */

    /**
     * @param currentTotalQuantity sum of part quantities already in the project,
     *   not the row count — the limit counts pieces to be cut.
     */
    fun canAddParts(
        tier: Tier,
        currentTotalQuantity: Int,
        adding: Int = 1,
        paywallEnabled: Boolean = Monetization.PAYWALL_ENABLED,
    ): Gate {
        require(adding > 0) { "adding must be positive" }
        val after = currentTotalQuantity + adding
        // The hard cap is checked FIRST and is not part of the paywall — it
        // protects the optimizer, and it applies just as much to a free build.
        if (after > Limits.MAX_PARTS_PER_PROJECT) {
            return Gate.HardLimit(
                "A job can hold ${Limits.MAX_PARTS_PER_PROJECT} pieces. Split it into two jobs.",
            )
        }
        if (!paywallEnabled) return Gate.Allowed
        if (tier == Tier.FREE && after > Limits.FREE_PARTS_PER_PROJECT) {
            return Gate.NeedsUpgrade(PaywallTrigger.PARTS)
        }
        return Gate.Allowed
    }

    fun canAddProject(
        tier: Tier,
        currentProjectCount: Int,
        paywallEnabled: Boolean = Monetization.PAYWALL_ENABLED,
    ): Gate =
        if (paywallEnabled && tier == Tier.FREE &&
            currentProjectCount + 1 > Limits.FREE_PROJECTS
        ) {
            Gate.NeedsUpgrade(PaywallTrigger.PROJECTS)
        } else {
            Gate.Allowed
        }

    fun canAddStock(
        tier: Tier,
        currentStockCount: Int,
        paywallEnabled: Boolean = Monetization.PAYWALL_ENABLED,
    ): Gate =
        if (paywallEnabled && tier == Tier.FREE &&
            currentStockCount + 1 > Limits.FREE_STOCK_PER_PROJECT
        ) {
            Gate.NeedsUpgrade(PaywallTrigger.STOCK)
        } else {
            Gate.Allowed
        }

    fun canExportPdf(
        tier: Tier,
        paywallEnabled: Boolean = Monetization.PAYWALL_ENABLED,
    ): Gate =
        if (!paywallEnabled || tier == Tier.PAID) Gate.Allowed
        else Gate.NeedsUpgrade(PaywallTrigger.PDF_EXPORT)

    /** Sharing as an image is free. It is how the app spreads. */
    fun canShareImage(tier: Tier): Gate = Gate.Allowed

    /** Optimizing is never gated. Correctness is not for sale. */
    fun canOptimize(tier: Tier): Gate = Gate.Allowed

    /** Editing and deleting existing rows is never gated, at any tier. */
    fun canEditExisting(tier: Tier): Gate = Gate.Allowed

    /**
     * With no paywall there is no way to buy ads away, so everyone sees them —
     * they are the only thing paying for the app.
     *
     * This deliberately ignores [tier]. A stale `isUnlocked` left in DataStore by
     * a pre-launch test build must not silently turn a user into unpaid,
     * unmonetised traffic forever.
     */
    fun showsAds(
        tier: Tier,
        paywallEnabled: Boolean = Monetization.PAYWALL_ENABLED,
    ): Boolean = if (!paywallEnabled) true else tier == Tier.FREE

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
        paywallEnabled: Boolean = Monetization.PAYWALL_ENABLED,
    ): Boolean {
        // Routed through showsAds so there is ONE answer to "does this person
        // see ads", rather than two rules that can disagree.
        if (!showsAds(tier, paywallEnabled)) return false
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
