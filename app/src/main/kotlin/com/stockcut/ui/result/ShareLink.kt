package com.stockcut.ui.result

/**
 * The credit and link that travel with every shared cut plan.
 *
 * Sharing is the app's one word-of-mouth channel: a plan goes to an apprentice,
 * a colleague or a supplier, and those are exactly the people StockCut is for.
 * The footer on the image and the PDF names the app; the link, sent as the
 * message text, lets the person receiving it install in one tap.
 *
 * 🔴 The production applicationId, NOT `context.packageName`. Debug builds carry
 * a `.debug` suffix, and a link to `com.measure.stockcut.debug` is a dead
 * listing.
 *
 * The referrer is a constant. It says "this install came from a shared image"
 * or "from a shared PDF", and nothing else: no install ID, no job contents,
 * nothing about the person who shared it (CLAUDE.md rule 11). Play Console reads
 * it into the acquisition report's UTM breakdown; nothing in the app reads it.
 */
object ShareLink {

    const val APP_ID = "com.measure.stockcut"

    /** Drawn at the foot of the shared image and of every PDF page. */
    const val FOOTER = "Made with StockCut · free on Google Play"

    enum class Medium(val utm: String) { IMAGE("image"), PDF("pdf") }

    /**
     * The whole referrer is ONE query parameter, percent-encoded. Written as
     * plain `&utm_source=...` the UTM tags become top-level parameters of the
     * Play URL, and Play drops them instead of passing them to the report.
     */
    fun url(medium: Medium): String =
        "https://play.google.com/store/apps/details?id=$APP_ID" +
            "&referrer=utm_source%3Dshare" +
            "%26utm_medium%3D${medium.utm}" +
            "%26utm_campaign%3Dshared_plan"

    /** The message text. WhatsApp shows it as the image caption; email uses it as the body. */
    fun message(medium: Medium): String =
        "Cut plan made with StockCut, a free cut list app: ${url(medium)}"
}
