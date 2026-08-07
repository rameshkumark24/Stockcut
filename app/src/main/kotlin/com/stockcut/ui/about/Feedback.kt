package com.stockcut.ui.about

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import java.net.URLEncoder

/**
 * Opening the feedback form.
 *
 * 🔴 The app links to the GitHub Pages REDIRECT, never to the Google Form
 * directly (docs/09 §9.5). With no server, that one layer of indirection is the
 * only thing here that can be changed without shipping an app update and waiting
 * days for store review — if the form is flooded or restructured, the redirect
 * is edited and it is live in a minute.
 *
 * 🔴 Opened with Intent.ACTION_VIEW in the user's own browser, never a WebView
 * (CLAUDE.md). That is not a style preference: it means the app process never
 * transmits anything. The browser does, after the user has seen the diagnostics
 * on screen and had the chance to delete them. docs/02 §12 calls that a stronger
 * position than an in-app POST, and it is why WebView is banned rather than
 * merely discouraged.
 */
object Feedback {

    /** The redirect, not the form. See the note above. */
    const val REDIRECT_URL = "https://rameshkumark24.github.io/Stockcut/feedback.html"

    /** Entry ID of the form's Diagnostics field, from its pre-filled link. */
    private const val DIAGNOSTICS_ENTRY = "entry.326955045"

    /** Max opens per 24 h before the mailto is offered instead (docs/09 §9.3). */
    const val SOFT_COOLDOWN_OPENS = 3

    /**
     * The form URL with diagnostics pre-filled.
     *
     * `usp=pp_url` is what tells Forms to treat the entry parameters as
     * pre-filled answers rather than ignoring them.
     *
     * Returns a String rather than a Uri so it is testable on the JVM —
     * Uri.parse is an Android API and returns null under a plain unit test, so a
     * Uri-returning version could only be checked on a device. The encoding is
     * the part worth testing: the diagnostics line is full of "|" and spaces,
     * and unencoded it truncates the query string.
     */
    fun formUrl(diagnostics: String): String {
        val encoded = URLEncoder.encode(diagnostics, "UTF-8")
        return "$REDIRECT_URL?usp=pp_url&$DIAGNOSTICS_ENTRY=$encoded"
    }

    /**
     * The form needs a network, and this app is used in workshops with no signal,
     * so this WILL happen (docs/09 §5.2).
     */
    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Queued by the mail app and sent when signal returns. */
    fun mailtoIntent(diagnostics: String): Intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "StockCut feedback")
            putExtra(Intent.EXTRA_TEXT, "\n\n---\n$diagnostics")
        }

    fun browserIntent(diagnostics: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(formUrl(diagnostics)))
}
