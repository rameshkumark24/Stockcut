package com.stockcut.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.stockcut.BuildConfig
import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.entitlement.Tier

/**
 * Ads. Free tier only, and never in the way of the work.
 *
 * 🔴 Test ad unit IDs are the DEFAULT (see app/build.gradle.kts). Clicking a
 * live ad in your own app terminates the AdMob account permanently and forfeits
 * earnings — CLAUDE.md rule 8 — so real IDs require an explicit build flag and
 * cannot be reached by accident.
 *
 * Placement rules, from docs/03 S3 and docs/02 §6:
 *  - Interstitial after every Nth optimize — see Limits.INTERSTITIAL_EVERY,
 *    which is 5, plus a 10-minute minimum gap. This comment said "every 3rd"
 *    long after the constant was raised to 5; the number lives in one place
 *    now and this refers to it rather than restating it.
 *  - Only between finishing entry and seeing the plan — NEVER mid-task.
 *  - Nothing at all when ads are switched off for a user.
 *  - A banner that fails to load collapses to zero height. No reserved grey box.
 */
class AdsManager(private val context: Context) {

    private var initialised = false
    private var interstitial: InterstitialAd? = null

    val bannerUnitId: String get() = BuildConfig.ADMOB_BANNER_ID
    private val interstitialUnitId: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID

    /**
     * 🔴 Call ONLY after [ConsentManager] has resolved. Initialising the SDK
     * before consent is the violation this ordering exists to prevent.
     */
    fun initialise(onReady: () -> Unit = {}) {
        if (initialised) {
            onReady()
            return
        }
        initialised = true
        MobileAds.initialize(context) { onReady() }
    }

    /** Preloads, so the ad is ready when the moment arrives rather than after it. */
    fun preloadInterstitial(tier: Tier) {
        if (!Entitlement.showsAds(tier) || !initialised || interstitial != null) return

        InterstitialAd.load(
            context,
            interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Nothing to do. An ad that will not load is not an error the
                    // user should ever learn about.
                    interstitial = null
                }
            },
        )
    }

    /**
     * Shows the interstitial if one is due and one is loaded.
     *
     * @param optimizeCount lifetime successful optimizes, AFTER incrementing.
     * @param onFinished ALWAYS invoked — whether the ad showed, failed, or was
     *   never due. The caller navigates from here, so a missing callback would
     *   strand the user on the editor after a successful optimize.
     */
    fun maybeShowInterstitial(
        activity: Activity,
        tier: Tier,
        optimizeCount: Int,
        lastInterstitialAtMillis: Long,
        onShown: () -> Unit,
        onFinished: () -> Unit,
    ) {
        val due = Entitlement.interstitialDue(tier, optimizeCount, lastInterstitialAtMillis)
        val ad = interstitial

        if (!due || ad == null) {
            onFinished()
            // Get the next one ready for whenever the next one is due.
            preloadInterstitial(tier)
            return
        }

        onShown()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadInterstitial(tier)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                onFinished()
            }
        }
        ad.show(activity)
    }
}
