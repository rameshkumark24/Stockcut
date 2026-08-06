package com.stockcut.ui.result

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.stockcut.optimizer.Plan
import com.stockcut.units.UnitSystem
import java.io.File
import java.io.FileOutputStream

/**
 * PDF export — "print it and pin it to the wall" (US-12).
 *
 * Platform [PdfDocument], not a third-party library. TRD §2 chose it precisely
 * so there is no extra dependency and no licence question, and it costs nothing
 * here because [PlanRenderer] already draws to a Canvas.
 *
 * 🔴 Paginates by whole bar groups, never mid-bar. A cut plan sliced across a
 * page break so that half a bar is on page 1 and half on page 2 is how somebody
 * mis-reads a length and cuts wrong. If a group does not fit in the remaining
 * space, it starts the next page.
 */
object PlanPdf {

    /** A4 at 72 dpi, in points. The primary markets are UK/AU (A4) and US/CA. */
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val DIR = "shared"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * @return a share Intent for the generated PDF.
     *
     * The caller is responsible for checking the entitlement first — this
     * function does not know about tiers, and gating belongs where the user
     * can be shown why.
     */
    fun shareIntent(
        context: Context,
        plan: Plan,
        jobName: String,
        kerfU: Long,
        unitSystem: UnitSystem,
        denominator: Int,
    ): Intent {
        val dir = File(context.cacheDir, DIR)
        dir.mkdirs()
        val file = File(dir, "${safeName(jobName)}.pdf")

        val document = PdfDocument()
        try {
            writePages(document, plan, jobName, kerfU, unitSystem, denominator)
            FileOutputStream(file).use(document::writeTo)
        } finally {
            // close() releases the native pages whether or not writing threw.
            document.close()
        }

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file,
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, jobName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Export cut plan") }
    }

    private fun writePages(
        document: PdfDocument,
        plan: Plan,
        jobName: String,
        kerfU: Long,
        unitSystem: UnitSystem,
        denominator: Int,
    ) {
        // The renderer draws at a fixed pixel width; scale that onto the page so
        // the drawing code never has to know what paper it is going onto.
        val scale = PAGE_WIDTH.toFloat() / PlanRenderer.WIDTH
        val usableHeight = PAGE_HEIGHT / scale - PlanRenderer.MARGIN

        val groups = plan.groupIdenticalBars()
        var index = 0
        var pageNumber = 1

        while (index < groups.size) {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
            )
            val canvas = page.canvas
            canvas.scale(scale, scale)

            // Title and summary on the first page only; repeating them would push
            // bars onto extra pages for no benefit.
            var y = if (pageNumber == 1) {
                PlanRenderer.drawHeader(canvas, plan, jobName, unitSystem, denominator)
            } else {
                PlanRenderer.MARGIN
            }

            while (index < groups.size && y + PlanRenderer.GROUP_HEIGHT <= usableHeight) {
                y = PlanRenderer.drawGroup(
                    canvas, groups[index], y, kerfU, unitSystem, denominator,
                )
                index++
            }

            document.finishPage(page)
            pageNumber++

            // Guard against a group taller than a whole page, which would loop
            // forever writing empty pages. Not reachable with current sizes, but
            // an infinite loop in an export is worse than a slightly cramped page.
            if (pageNumber > 200) break
        }

        // A plan with no bars still needs one page, or writeTo throws.
        if (document.pages.isEmpty()) {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create(),
            )
            page.canvas.scale(scale, scale)
            PlanRenderer.drawHeader(page.canvas, plan, jobName, unitSystem, denominator)
            document.finishPage(page)
        }
    }

    private fun safeName(jobName: String): String {
        val cleaned = jobName.trim()
            .map { char -> if (char.isLetterOrDigit() || char == '_') char else '-' }
            .joinToString("")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(40)
        return cleaned.ifBlank { "cut-plan" }
    }
}
