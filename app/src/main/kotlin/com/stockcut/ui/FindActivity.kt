package com.stockcut.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * The Activity behind a composition Context, unwrapping any wrappers.
 *
 * 🔴 `LocalContext.current as? Activity` is NOT reliable, and it fails SILENTLY.
 *
 * The composition Context is frequently a [ContextWrapper] rather than the
 * Activity itself — a themed wrapper, a dialog or popup host, a preview host, a
 * test host. A safe cast against one of those yields null, and every call site
 * in this app used that null to mean "skip", so the feature simply did not
 * happen: no crash, no log, no user-visible error.
 *
 * That mattered most on the UMP "Ad privacy settings" button, which is a
 * consent control the app is obliged to keep reachable (CLAUDE.md: users in
 * regions granting ongoing consent options "have the right to change their
 * mind"). A consent button that is enabled, tappable, and does nothing is a
 * compliance problem rather than a cosmetic one. The interstitial gate and the
 * in-app review prompt had the same cast, where the cost is quieter — unshown
 * ads earn nothing, and neither reports a failure.
 *
 * Walking `baseContext` finds the Activity through any depth of wrapping, and
 * still returns null honestly when there genuinely is no Activity.
 */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
