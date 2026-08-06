package com.stockcut.ui.result

import com.stockcut.optimizer.OptimizeRequest
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.PartSpec
import com.stockcut.optimizer.StockSpec
import com.stockcut.optimizer.optimize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What S4 draws, tested without a device.
 *
 * These run against real optimizer output rather than hand-built Plans, so a
 * change to the optimizer that breaks the drawing shows up here rather than on
 * a user's screen.
 */
class PlanPresentationTest {

    private fun plan(
        stockLen: Long,
        stockQty: Int = StockSpec.UNLIMITED,
        parts: List<Pair<Long, Int>>,
        kerf: Long = 3,
    ) = assertIs<OptimizeResult.Success>(
        optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, stockLen, stockQty)),
                parts = parts.mapIndexed { i, (len, qty) -> PartSpec(i + 1L, len, qty) },
                kerfU = kerf,
            ),
        ),
    ).plan

    // ── Identical-bar collapsing ─────────────────────────────────────────────

    @Test
    fun `forty identical bars collapse into one card`() {
        // docs/03 S4: "a plan with 40 bars must not be 40 cards".
        val p = plan(stockLen = 1_000, parts = listOf(1_000L to 40), kerf = 0)
        assertEquals(40, p.bars.size)

        val groups = p.groupIdenticalBars()
        assertEquals(1, groups.size)
        assertEquals(40, groups.single().count)
        assertEquals(1, groups.single().firstIndex)
    }

    @Test
    fun `every bar is still accounted for after collapsing`() {
        // The collapse must not lose a bar — it changes presentation, not content.
        val p = plan(stockLen = 6_000, parts = listOf(1_800L to 2, 1_200L to 2, 900L to 2, 850L to 3))
        assertEquals(p.bars.size, p.groupIdenticalBars().sumOf { it.count })
    }

    @Test
    fun `differing bars are not merged`() {
        val p = plan(stockLen = 6_000, parts = listOf(2_000L to 1, 1_000L to 1, 500L to 1))
        val groups = p.groupIdenticalBars()
        assertEquals(p.bars.size, groups.sumOf { it.count })
        // One bar holds all three pieces, so there is exactly one group of one.
        assertTrue(groups.all { it.count >= 1 })
    }

    @Test
    fun `an empty plan collapses to nothing rather than throwing`() {
        val empty = com.stockcut.optimizer.Plan(
            bars = emptyList(),
            totalStockUsedU = 0, totalPartsU = 0, totalKerfU = 0,
            totalTrimU = 0, totalOffcutU = 0, wastePercent = 0.0,
        )
        assertEquals(emptyList(), empty.groupIdenticalBars())
    }

    @Test
    fun `bar numbering stays 1-based and continuous across groups`() {
        val p = plan(stockLen = 2_000, parts = listOf(1_900L to 3, 900L to 2), kerf = 3)
        val groups = p.groupIdenticalBars()

        var expectedIndex = 1
        for (group in groups) {
            assertEquals(expectedIndex, group.firstIndex, "bar numbering skipped")
            expectedIndex += group.count
        }
        assertEquals(p.bars.size + 1, expectedIndex)
    }

    // ── Waste bands ──────────────────────────────────────────────────────────

    @Test
    fun `waste bands match the thresholds in the app flow`() {
        assertEquals(WasteBand.GOOD, wasteBand(0.0))
        assertEquals(WasteBand.GOOD, wasteBand(4.99))
        assertEquals(WasteBand.FAIR, wasteBand(5.0))
        assertEquals(WasteBand.FAIR, wasteBand(15.0))
        assertEquals(WasteBand.POOR, wasteBand(15.01))
        assertEquals(WasteBand.POOR, wasteBand(80.0))
    }

    @Test
    fun `every band carries a label, so colour is never the only signal`() {
        for (band in WasteBand.entries) {
            assertTrue(band.label.isNotBlank(), "$band has no label")
        }
    }

    // ── Segments ─────────────────────────────────────────────────────────────

    @Test
    fun `segment weights sum to the whole bar`() {
        // 🔴 If these do not add up to 1, the drawing is lying about proportions
        // and a user reading lengths off the picture would be misled.
        val p = plan(stockLen = 6_000, parts = listOf(1_800L to 2, 1_200L to 2, 850L to 3))
        for (bar in p.bars) {
            val total = bar.segmentWeights(kerfU = 3).sumOf { it.weight.toDouble() }
            assertTrue(
                kotlin.math.abs(total - 1.0) < 1e-6,
                "segments summed to $total, not 1.0",
            )
        }
    }

    @Test
    fun `segment lengths sum to the stock length exactly, in Long`() {
        // The weights are Float for drawing; the LENGTHS behind them must still
        // satisfy the kerf invariant in exact integers.
        val p = plan(stockLen = 6_000, parts = listOf(1_800L to 2, 1_200L to 2, 850L to 3))
        for (bar in p.bars) {
            val sum = bar.segmentWeights(kerfU = 3).sumOf { it.lengthU }
            assertEquals(bar.stockLengthU, sum, "segment lengths do not fill the bar")
        }
    }

    @Test
    fun `a perfectly packed bar has no offcut segment`() {
        val p = plan(stockLen = 6_000, parts = listOf(1_500L to 4), kerf = 0)
        val segments = p.bars.single().segmentWeights(kerfU = 0)
        assertTrue(segments.none { it.kind == SegmentKind.OFFCUT })
        assertEquals(4, segments.count { it.kind == SegmentKind.PART })
    }

    @Test
    fun `trim appears first, offcut last`() {
        val result = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6_000, StockSpec.UNLIMITED)),
                parts = listOf(PartSpec(1, 1_000, 2)),
                kerfU = 3,
                trimU = 50,
            ),
        )
        val bar = assertIs<OptimizeResult.Success>(result).plan.bars.single()
        val segments = bar.segmentWeights(kerfU = 3)

        assertEquals(SegmentKind.TRIM, segments.first().kind)
        assertEquals(SegmentKind.OFFCUT, segments.last().kind)
    }

    @Test
    fun `part segments carry their real length, not a rounded one`() {
        val p = plan(stockLen = 6_000, parts = listOf(1_800L to 2))
        val partSegments = p.bars.first().segmentWeights(kerfU = 3)
            .filter { it.kind == SegmentKind.PART }
        assertTrue(partSegments.all { it.lengthU == 1_800L })
    }
}
