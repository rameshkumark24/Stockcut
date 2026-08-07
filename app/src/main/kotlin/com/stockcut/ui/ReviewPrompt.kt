package com.stockcut.ui

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.stockcut.data.entitlement.Entitlement
import com.stockcut.data.settings.SettingsStore

/**
 * The in-app review prompt.
 *
 * The rules were written and tested long before this file existed — see
 * Entitlement.reviewPromptDue: after a SUCCESSFUL optimize, at least 3 lifetime,
 * at most once per 90 days. All this adds is the Play API call.
 *
 * 🔴 Never on launch, and never after a crash. Asking someone to rate an app
 * that just failed them is how you manufacture a one-star review.
 *
 * Play itself also rate-limits and may show nothing at all. That is expected:
 * the API deliberately gives no success signal, so this records the attempt
 * rather than the outcome. Trying to detect whether it appeared, and retrying if
 * not, is exactly the nagging the quota exists to prevent.
 */
object ReviewPrompt {

    suspend fun maybeAsk(
        activity: Activity,
        settings: SettingsStore,
        optimizeCount: Int,
        lastPromptAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        if (!Entitlement.reviewPromptDue(optimizeCount, lastPromptAtMillis, nowMillis)) return

        // Recorded up front. If the request fails we still wait 90 days rather
        // than retrying on the next optimize.
        settings.recordReviewPrompt(nowMillis)

        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
