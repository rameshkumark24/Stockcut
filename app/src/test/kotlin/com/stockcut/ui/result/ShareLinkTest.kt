package com.stockcut.ui.result

import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The link that goes out with every shared plan. Nobody sees it break: a wrong
 * one still opens a page, just not ours, or opens ours and loses the referrer.
 */
class ShareLinkTest {

    private fun params(url: String): Map<String, String> =
        url.substringAfter('?').split('&').associate {
            it.substringBefore('=') to it.substringAfter('=')
        }

    @Test
    fun `links to the production listing, never the debug build`() {
        for (medium in ShareLink.Medium.entries) {
            val id = params(ShareLink.url(medium))["id"]
            assertEquals("com.measure.stockcut", id)
        }
    }

    @Test
    fun `the referrer is one encoded parameter, so Play passes it to the report`() {
        val url = ShareLink.url(ShareLink.Medium.IMAGE)
        // Exactly two top-level parameters. A raw &utm_source would be a third,
        // and Play would drop it.
        assertEquals(setOf("id", "referrer"), params(url).keys)

        val referrer = URLDecoder.decode(params(url).getValue("referrer"), "UTF-8")
        assertEquals("utm_source=share&utm_medium=image&utm_campaign=shared_plan", referrer)
    }

    @Test
    fun `image and PDF shares are counted separately`() {
        assertNotEquals(
            ShareLink.url(ShareLink.Medium.IMAGE),
            ShareLink.url(ShareLink.Medium.PDF),
        )
        assertTrue("utm_medium%3Dpdf" in ShareLink.url(ShareLink.Medium.PDF))
    }

    @Test
    fun `the message carries the link and follows the copy rules`() {
        for (medium in ShareLink.Medium.entries) {
            val message = ShareLink.message(medium)
            assertTrue(ShareLink.url(medium) in message)
            // docs/04 §1: no exclamation marks in product copy.
            assertFalse('!' in message)
        }
        assertFalse('!' in ShareLink.FOOTER)
        assertTrue("StockCut" in ShareLink.FOOTER, "the footer must name the app")
    }
}
