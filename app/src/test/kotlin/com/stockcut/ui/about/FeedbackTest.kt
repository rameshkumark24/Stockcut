package com.stockcut.ui.about

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The feedback URL. These are cheap tests guarding decisions that are expensive
 * to reverse once the URL has shipped inside an APK.
 */
class FeedbackTest {

    @Test
    fun `🔴 the app points at the redirect, never at the Google Form`() {
        // docs/09 §9.5: the redirect is the only kill switch this app has without
        // a server. A docs.google.com URL here would mean a flooded or
        // restructured form could only be fixed by an app update and store
        // review — which is exactly what the redirect exists to avoid.
        assertFalse(
            Feedback.REDIRECT_URL.contains("docs.google.com"),
            "the app is linking straight at the form: ${Feedback.REDIRECT_URL}",
        )
        assertTrue(Feedback.REDIRECT_URL.startsWith("https://"), "must be https")
    }

    @Test
    fun `the diagnostics are pre-filled as a form entry parameter`() {
        val url = Feedback.formUrl("v1.0.0 (1) | Android 13 | SM-A515F | mm | free | 3 optimizes")

        assertTrue(url.contains("usp=pp_url"), "Forms ignores entries without usp=pp_url")
        assertTrue(url.contains("entry.326955045="), "the diagnostics entry ID is missing")
    }

    @Test
    fun `the diagnostics are URL-encoded, so the pipes and spaces survive`() {
        // The line is full of "|" and " ". Unencoded, the query string breaks and
        // the report arrives with the diagnostics truncated or empty.
        val url = Feedback.formUrl("v1.0.0 (1) | Android 13 | mm | free")

        assertFalse(url.contains(" "), "a raw space reached the URL: $url")
        assertFalse(url.contains("|"), "a raw pipe reached the URL: $url")
        assertTrue(url.contains("%7C") || url.contains("%7c"), "pipes were not encoded: $url")
    }

    @Test
    fun `the redirect keeps its query string, which is the whole point of it`() {
        val url = Feedback.formUrl("test")
        assertTrue(
            url.startsWith(Feedback.REDIRECT_URL + "?"),
            "parameters must hang off the redirect, not be lost: $url",
        )
    }

    @Test
    fun `the soft cooldown is a nudge, not a lockout`() {
        // docs/09 §9.3: three real reports in one session is a GOOD outcome. The
        // cooldown exists to stop accidental double-submits, and offering the
        // mailto on the 4th must never become a hard block.
        assertTrue(Feedback.SOFT_COOLDOWN_OPENS >= 3)
    }
}
