package com.stockcut.ui.result

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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PDF export. Instrumented, because PdfDocument is a platform API.
 *
 * The entitlement gate lives in the ViewModel and is covered separately; this
 * checks the thing that only a device can answer — that a real, openable PDF
 * comes out, and that a long plan paginates instead of being truncated or
 * looping forever.
 */
@RunWith(AndroidJUnit4::class)
class PlanPdfTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun planWith(bars: Int): Plan {
        // Each 1000 part fills its own 1000 bar exactly, so bar count is exact.
        val result = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 1_000, StockSpec.UNLIMITED)),
                parts = listOf(PartSpec(1, 1_000, bars)),
                kerfU = 0,
            ),
        )
        return assertIs<OptimizeResult.Success>(result).plan
    }

    private fun export(plan: Plan, jobName: String = "Gate"): File {
        PlanPdf.shareIntent(
            context = context,
            plan = plan,
            jobName = jobName,
            kerfU = 0,
            unitSystem = UnitSystem.MM,
            denominator = 16,
        )
        val safe = jobName.trim()
            .map { if (it.isLetterOrDigit() || it == '_') it else '-' }
            .joinToString("")
            .replace(Regex("-+"), "-")
            .trim('-')
        return File(File(context.cacheDir, "shared"), "$safe.pdf")
    }

    private fun pageCount(file: File): Int =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { it.pageCount }
        }

    @Test
    fun aPlanExportsAsARealPdf() {
        val file = export(planWith(3))

        assertTrue(file.exists(), "no PDF was written")
        assertTrue(file.length() > 0, "the PDF is empty")
        // %PDF magic bytes — proof it is a PDF, not just a file we named .pdf.
        val header = file.inputStream().use { input ->
            ByteArray(4).also { input.read(it) }
        }
        assertEquals("%PDF", String(header))
    }

    @Test
    fun aShortPlanIsOnePage() {
        assertEquals(1, pageCount(export(planWith(3))))
    }

    @Test
    fun aLongPlanPaginatesInsteadOfBeingTruncated() {
        // 40 identical bars collapse to ONE group, so the page count stays 1 —
        // which is the point of collapsing. Distinct bars are what force pages.
        val distinct = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6_000, StockSpec.UNLIMITED)),
                // 30 different lengths, so no two bars are identical.
                parts = (1..30).map { PartSpec(it.toLong(), 5_000L + it * 20, 1) },
                kerfU = 3,
            ),
        )
        val plan = assertIs<OptimizeResult.Success>(distinct).plan
        val file = export(plan, "Long job")

        assertTrue(plan.groupIdenticalBars().size >= 10, "test needs many distinct bars")
        assertTrue(
            pageCount(file) > 1,
            "a ${plan.bars.size}-bar plan fitted on one page — it is being truncated",
        )
    }

    @Test
    fun identicalBarsCollapseSoTheyDoNotBloatThePdf() {
        // 40 identical bars must not become 40 pages of the same picture.
        val file = export(planWith(40), "Repetitive")
        assertEquals(1, pageCount(file))
    }

    @Test
    fun aJobNameFullOfAwkwardCharactersStillProducesAFile() {
        // Job names are free text: emoji, slashes, and path traversal all reach
        // this code. None of them may escape the cache directory.
        val file = export(planWith(2), "../../etc/passwd 🔩")
        assertTrue(file.exists(), "expected ${file.absolutePath}")
        assertEquals("shared", file.parentFile?.name, "the file escaped cache/shared")
    }
}
