package com.stockcut.ui.result

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.stockcut.optimizer.Plan
import com.stockcut.units.UnitSystem
import com.stockcut.units.format

/**
 * Draws a cut plan onto a [Canvas].
 *
 * Deliberately NOT a screenshot of the S4 composable. Two reasons:
 *
 *  1. A screenshot captures only what is on screen. A 40-bar plan would be
 *     cropped, and a cut list missing bars is the same class of failure as a
 *     plan missing parts.
 *  2. The same drawing has to become the PDF export (checklist Phase 5). One
 *     renderer with two outputs — a PNG for WhatsApp and a PDF page — means the
 *     two can never disagree about what the plan says.
 *
 * 🔴 Everything here must survive being printed in black and white (docs/00
 * gap audit §B12). So every segment is outlined and labelled; the fills are a
 * convenience, never the information.
 */
object PlanRenderer {

    const val WIDTH = 1080
    const val MARGIN = 48f
    private const val BAR_HEIGHT = 84f
    private const val GAP = 28f

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 32f
    }
    private val barLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 28f
    }
    private val segmentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val partFill = Color.rgb(0x2D, 0x7D, 0xD2)
    private val partFillAlt = Color.rgb(0x1E, 0x5B, 0x9A)
    private val offcutFill = Color.rgb(0x9A, 0xA3, 0xAB)

    /**
     * Where the first bar heading sits, and how far each group advances.
     *
     * Both are used by measureHeight AND by render, so the measured height and
     * the drawn height cannot drift apart. When they did, the shared PNG carried
     * a band of dead whitespace — harmless, but it is the artifact a tradesman
     * sends to someone else, so it should not look unfinished.
     */
    const val HEADER_HEIGHT = MARGIN + 44f + 48f + GAP + 20f
    const val GROUP_HEIGHT = 16f + BAR_HEIGHT + 34f + GAP + 10f

    /** Exact height needed for [plan] at the fixed render width. */
    fun measureHeight(plan: Plan): Int {
        val groups = plan.groupIdenticalBars().size
        // The last group needs no trailing gap, only the bottom margin.
        val trailing = GAP + 10f
        return (HEADER_HEIGHT + groups * GROUP_HEIGHT - trailing + MARGIN).toInt()
    }

    fun render(
        plan: Plan,
        jobName: String,
        kerfU: Long,
        unitSystem: UnitSystem,
        denominator: Int,
    ): Bitmap {
        val height = measureHeight(plan)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // White, not transparent: a transparent PNG in a dark-mode chat app
        // renders black text on black.
        canvas.drawColor(Color.WHITE)

        var y = drawHeader(canvas, plan, jobName, unitSystem, denominator)
        for (group in plan.groupIdenticalBars()) {
            y = drawGroup(canvas, group, y, kerfU, unitSystem, denominator)
        }
        return bitmap
    }

    /**
     * Title and summary. Returns the y the first bar starts at, which is exactly
     * [HEADER_HEIGHT].
     *
     * Public so the PDF exporter can draw it on page 1 and skip it on later
     * pages, without duplicating the layout.
     */
    fun drawHeader(
        canvas: Canvas,
        plan: Plan,
        jobName: String,
        unitSystem: UnitSystem,
        denominator: Int,
    ): Float {
        var y = MARGIN + 44f
        canvas.drawText(jobName, MARGIN, y, titlePaint)
        y += 48f
        canvas.drawText(summaryLine(plan, unitSystem, denominator), MARGIN, y, summaryPaint)
        return y + GAP + 20f
    }

    private fun summaryLine(plan: Plan, unitSystem: UnitSystem, denominator: Int): String {
        val bars = if (plan.bars.size == 1) "1 bar" else "${plan.bars.size} bars"
        val waste = "%.1f".format(plan.wastePercent)
        val offcut = format(plan.totalOffcutU, unitSystem, denominator)
        return "$bars · $waste% waste · $offcut offcut total"
    }

    fun drawGroup(
        canvas: Canvas,
        group: BarGroup,
        top: Float,
        kerfU: Long,
        unitSystem: UnitSystem,
        denominator: Int,
    ): Float {
        var y = top

        val heading = if (group.count == 1) {
            "Bar ${group.firstIndex}"
        } else {
            "Bars ${group.firstIndex}–${group.firstIndex + group.count - 1}  ×${group.count} identical"
        }
        canvas.drawText(heading, MARGIN, y, barLabelPaint)
        y += 16f

        val barWidth = WIDTH - 2 * MARGIN
        var x = MARGIN
        var partIndex = 0

        for (segment in group.bar.segmentWeights(kerfU)) {
            val segWidth = barWidth * segment.weight
            // A kerf is a fraction of a percent of the bar; it is drawn as part
            // of the outline rather than as its own visible block.
            if (segment.kind == SegmentKind.KERF) {
                x += segWidth
                continue
            }

            val rect = Rect(
                x.toInt(),
                y.toInt(),
                (x + segWidth).toInt(),
                (y + BAR_HEIGHT).toInt(),
            )
            fillPaint.color = when (segment.kind) {
                SegmentKind.PART -> if (partIndex % 2 == 0) partFill else partFillAlt
                SegmentKind.OFFCUT -> offcutFill
                else -> Color.LTGRAY
            }
            if (segment.kind == SegmentKind.PART) partIndex++

            canvas.drawRect(rect, fillPaint)
            // The outline is what makes this readable in black and white — every
            // cut is a visible boundary even with no colour at all.
            canvas.drawRect(rect, strokePaint)

            val label = format(segment.lengthU, unitSystem, denominator)
            segmentTextPaint.color =
                if (segment.kind == SegmentKind.OFFCUT) Color.BLACK else Color.WHITE
            // Same rule as on screen: a clipped number reads as a different
            // number, so a label that does not fit is omitted. The text line
            // below lists every length regardless.
            if (segmentTextPaint.measureText(label) < segWidth - 8f) {
                canvas.drawText(
                    label,
                    x + segWidth / 2f,
                    y + BAR_HEIGHT / 2f + 9f,
                    segmentTextPaint,
                )
            }
            x += segWidth
        }

        y += BAR_HEIGHT + 34f
        val detail = group.bar.parts.joinToString(" · ") {
            format(it.lengthU, unitSystem, denominator)
        } + "  →  offcut " + format(group.bar.offcutU, unitSystem, denominator)
        canvas.drawText(detail, MARGIN, y, detailPaint)

        return y + GAP + 10f
    }
}
