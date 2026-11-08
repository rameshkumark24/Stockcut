package com.stockcut.ui.result

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stockcut.optimizer.OptimizeRequest
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.PartSpec
import com.stockcut.optimizer.Plan
import com.stockcut.optimizer.StockSpec
import com.stockcut.optimizer.optimize
import com.stockcut.units.UnitSystem
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "Made with StockCut" credit on shared plans.
 *
 * Instrumented because both outputs are platform drawing. The failure this
 * guards against is silent: if measureHeight and render ever disagree, the
 * footer is drawn below the bitmap's last row and simply is not there.
 */
@RunWith(AndroidJUnit4::class)
class PlanFooterTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun plan(distinctBars: Int): Plan {
        val result = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6_000, StockSpec.UNLIMITED)),
                // Lengths over half the bar, so every part gets its own bar and
                // no two bars are identical.
                parts = (1..distinctBars).map { PartSpec(it.toLong(), 5_000L + it * 20, 1) },
                kerfU = 3,
            ),
        )
        return assertIs<OptimizeResult.Success>(result).plan
    }

    /** Any pixel that is drawn and is not white. Works for opaque and transparent backgrounds. */
    private fun hasInk(bitmap: Bitmap, fromRow: Int, toRow: Int): Boolean {
        for (y in fromRow.coerceAtLeast(0) until toRow.coerceAtMost(bitmap.height)) {
            for (x in 0 until bitmap.width) {
                val p = bitmap.getPixel(x, y)
                if (Color.alpha(p) > 0 &&
                    (Color.red(p) < 240 || Color.green(p) < 240 || Color.blue(p) < 240)
                ) {
                    return true
                }
            }
        }
        return false
    }

    @Test
    fun theSharedImageEndsWithTheFooterAndItIsNotClipped() {
        val p = plan(4)
        val bitmap = PlanRenderer.render(p, "Gate", 3, UnitSystem.MM, 16)

        assertEquals(PlanRenderer.measureHeight(p), bitmap.height)

        val margin = PlanRenderer.MARGIN.toInt()
        val footerBaseline = bitmap.height - margin
        // The footer text sits just above the bottom margin...
        assertTrue(
            hasInk(bitmap, footerBaseline - 20, footerBaseline),
            "no footer text above the bottom margin; it was drawn off the image",
        )
        // ...and nothing sits in the margin itself, beyond descenders.
        assertFalse(
            hasInk(bitmap, footerBaseline + 12, bitmap.height),
            "something is drawn in the bottom margin; measure and render disagree",
        )
    }

    @Test
    fun everyPdfPageCarriesTheFooter() {
        PlanPdf.shareIntent(context, plan(30), "Long job", 3, UnitSystem.MM, 16)
        val file = File(File(context.cacheDir, "shared"), "Long-job.pdf")

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                assertTrue(renderer.pageCount > 1, "the test needs a multi-page plan")
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width, page.height, Bitmap.Config.ARGB_8888,
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val scale = page.width.toFloat() / PlanRenderer.WIDTH
                        val baseline = (page.height - PlanRenderer.MARGIN * scale).toInt()
                        assertTrue(
                            hasInk(bitmap, baseline - (20 * scale).toInt() - 1, baseline + 1),
                            "page ${i + 1} has no footer",
                        )
                    }
                }
            }
        }
    }
}
