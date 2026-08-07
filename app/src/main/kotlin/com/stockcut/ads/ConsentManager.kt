package com.stockcut.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.stockcut.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UMP consent. Mandatory for EU/UK traffic, and this app deliberately targets
 * US/UK/CA/AU (docs/00-phase-0 §3), so EU/UK users will arrive.
 *
 * 🔴 The consent form must be resolved BEFORE any ad request. Requesting ads
 * first and asking afterwards is the violation, and it is easy to do by accident
 * because ads still appear to work.
 *
 * The form must also stay reachable afterwards — a user has the right to change
 * their mind, which is why [showPrivacyOptions] exists and Settings offers it.
 */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val _canRequestAds = MutableStateFlow(false)

    /** False until consent has been gathered or is not required. Gate ads on this. */
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /** True only for users whose region gives them ongoing options (EU/UK). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Call once, early, from the Activity.
     *
     * @param onResolved invoked when it is safe to initialise the ads SDK —
     *   whether consent was granted, refused, or never required.
     */
    fun gather(activity: Activity, onResolved: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    // Testing the EU flow needs an EEA IP or this override. The
                    // real thing must still be verified against an EU IP before
                    // release (docs/06 §5) — a debug override proves the form
                    // renders, not that geo-targeting works.
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            .build(),
                    )
                }
            }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // Fires whether the form was shown, dismissed, or not needed.
                    publish(onResolved)
                }
            },
            {
                // Consent lookup failed — no network, or UMP is unreachable.
                // canRequestAds stays false, so no ad is requested. The app is
                // fully usable regardless; ads are not load-bearing.
                publish(onResolved)
            },
        )
    }

    private fun publish(onResolved: () -> Unit) {
        _canRequestAds.value = consentInformation.canRequestAds()
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        onResolved()
    }

    /** Re-entry point from Settings, so consent can be changed later. */
    fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            _canRequestAds.value = consentInformation.canRequestAds()
            onDone(error?.message)
        }
    }
}
