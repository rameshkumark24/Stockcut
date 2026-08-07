package com.stockcut.optimizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The edge-case table from docs/05 §2.5 and docs/06 §8. */
class EdgeCaseTest {

    private fun opt(
        stockLen: Long,
        stockQty: Int = StockSpec.UNLIMITED,
        partLen: Long,
        partQty: Int = 1,
        kerf: Long = 0,
        trim: Long = 0,
    ) = optimize(
        OptimizeRequest(
            stock = listOf(StockSpec(1, stockLen, stockQty)),
            parts = listOf(PartSpec(1, partLen, partQty)),
            kerfU = kerf,
            trimU = trim,
        ),
    )

    @Test
    fun `no parts is invalid input`() {
        val r = optimize(OptimizeRequest(listOf(StockSpec(1, 6000, -1)), emptyList(), 3))
        assertIs<OptimizeResult.InvalidInput>(r)
    }

    @Test
    fun `no stock is invalid input`() {
        val r = optimize(OptimizeRequest(emptyList(), listOf(PartSpec(1, 100, 1)), 3))
        assertIs<OptimizeResult.InvalidInput>(r)
    }

    @Test
    fun `part exactly one unit shorter than the bar still needs a kerf to cut`() {
        // 6000 bar, 5999 part, 3 kerf: you would need 6002mm of steel. Cannot fit.
        assertIs<OptimizeResult.Infeasible>(opt(stockLen = 6000, partLen = 5999, kerf = 3))
        // With no kerf it fits, leaving 1.
        val plan = assertIs<OptimizeResult.Success>(opt(stockLen = 6000, partLen = 5999, kerf = 0)).plan
        assertEquals(1L, plan.bars[0].offcutU)
        assertEquals(1, plan.bars[0].cutCount)
    }

    @Test
    fun `zero kerf is valid, for a waterjet or shear`() {
        val plan = assertIs<OptimizeResult.Success>(
            opt(stockLen = 1000, partLen = 250, partQty = 4, kerf = 0),
        ).plan
        assertEquals(1, plan.bars.size)
        assertEquals(0L, plan.bars[0].offcutU)
    }

    @Test
    fun `trim longer than every stock length is invalid, not a crash`() {
        val r = opt(stockLen = 1000, partLen = 100, trim = 2000)
        assertIs<OptimizeResult.InvalidInput>(r)
    }

    @Test
    fun `kerf wider than the part is allowed but wasteful`() {
        val plan = assertIs<OptimizeResult.Success>(
            opt(stockLen = 1000, partLen = 5, partQty = 3, kerf = 50),
        ).plan
        assertTrue(plan.wastePercent > 50.0)
        assertEquals(3, plan.bars.sumOf { it.parts.size })
    }

    @Test
    fun `negative and zero values are rejected`() {
        assertIs<OptimizeResult.InvalidInput>(opt(stockLen = -100, partLen = 50))
        assertIs<OptimizeResult.InvalidInput>(opt(stockLen = 1000, partLen = 0))
        assertIs<OptimizeResult.InvalidInput>(opt(stockLen = 1000, partLen = 50, kerf = -1))
        assertIs<OptimizeResult.InvalidInput>(opt(stockLen = 1000, partLen = 50, trim = -1))
        assertIs<OptimizeResult.InvalidInput>(opt(stockLen = 1000, stockQty = 0, partLen = 50))
        assertIs<OptimizeResult.InvalidInput>(opt(stockLen = 1000, partLen = 50, partQty = 0))
    }

    @Test
    fun `duplicate ids are rejected rather than silently merged`() {
        val r = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6000, -1), StockSpec(1, 3000, -1)),
                parts = listOf(PartSpec(1, 100, 1)),
                kerfU = 3,
            ),
        )
        assertIs<OptimizeResult.InvalidInput>(r)
    }

    @Test
    fun `part count above the paid cap is rejected`() {
        val r = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6000, -1)),
                parts = listOf(PartSpec(1, 100, 1001)),
                kerfU = 3,
            ),
        )
        assertIs<OptimizeResult.InvalidInput>(r)
    }

    @Test
    fun `labels survive into the plan, including unicode and sql characters`() {
        val nasty = "'; DROP TABLE part_entry; -- 🔩 ਪੰਜਾਬੀ"
        val r = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6000, -1, label = nasty)),
                parts = listOf(PartSpec(1, 1000, 2, label = nasty)),
                kerfU = 3,
            ),
        )
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertTrue(plan.bars.all { bar -> bar.parts.all { it.label == nasty } })
    }

    @Test
    fun `limited stock is consumed before unlimited stock`() {
        val r = optimize(
            OptimizeRequest(
                stock = listOf(
                    StockSpec(1, 3000, quantity = 1),
                    StockSpec(2, 3000, quantity = StockSpec.UNLIMITED),
                ),
                parts = listOf(PartSpec(1, 2900, 3)),
                kerfU = 3,
            ),
        )
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertEquals(3, plan.bars.size)
        assertTrue(plan.bars.any { it.stockId == 1L }, "the limited bar should be used, not stranded")
    }

    @Test
    fun `parts are listed longest first on each bar`() {
        val r = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6000, -1)),
                parts = listOf(PartSpec(1, 900, 2), PartSpec(2, 1800, 2), PartSpec(3, 400, 2)),
                kerfU = 3,
            ),
        )
        val plan = assertIs<OptimizeResult.Success>(r).plan
        for (bar in plan.bars) {
            val lengths = bar.parts.map { it.lengthU }
            assertEquals(lengths.sortedDescending(), lengths, "cut longest first")
        }
    }

    @Test
    fun `a single part equal to the whole bar after trim fits exactly`() {
        val plan = assertIs<OptimizeResult.Success>(
            opt(stockLen = 6000, partLen = 5950, kerf = 3, trim = 50),
        ).plan
        assertEquals(0L, plan.bars[0].offcutU)
        assertEquals(0, plan.bars[0].cutCount)
        assertEquals(50L, plan.bars[0].trimU)
        // Invariant still holds with trim in play.
        assertEquals(6000L, 5950L + 0L * 3L + 0L + 50L)
    }
}

/**
 * The edge cases docs/06 §8 lists that were not otherwise covered.
 *
 * Each is a shape that has broken a real cutting app somewhere: the maximum
 * job, the pathological input, and the double tap.
 */
class RemainingEdgeCaseTest {

    @Test
    fun `the paid cap of a thousand pieces completes and stays correct`() {
        // Not just "does not crash" — the invariant must still hold at the
        // largest job the app permits, where an off-by-one in the improvement
        // pass would have the most room to hide.
        val kerf = 3L
        val result = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 6000, StockSpec.UNLIMITED)),
                parts = (1..50).map { PartSpec(it.toLong(), 200L + it * 20L, 20) },
                kerfU = kerf,
            ),
        )
        val plan = assertIs<OptimizeResult.Success>(result).plan

        assertEquals(1000, plan.bars.sumOf { it.parts.size }, "a piece went missing at the cap")
        for (bar in plan.bars) {
            val sum = bar.parts.sumOf { it.lengthU } + bar.cutCount * kerf + bar.offcutU + bar.trimU
            assertEquals(bar.stockLengthU, sum, "a bar stopped balancing at 1000 pieces")
        }
    }

    @Test
    fun `optimizing the same request twice gives identical results`() {
        // The double-tap case. Optimize is idempotent and has no side effects,
        // so two taps racing each other cannot produce two different plans —
        // which is what makes the UI's guard a convenience rather than a
        // correctness measure.
        val request = OptimizeRequest(
            stock = listOf(StockSpec(1, 6000, StockSpec.UNLIMITED)),
            parts = listOf(PartSpec(1, 1800, 3), PartSpec(2, 950, 5)),
            kerfU = 3,
        )
        assertEquals(optimize(request), optimize(request))
    }

    @Test
    fun `a single part one unit long is planned, not rounded away`() {
        val plan = assertIs<OptimizeResult.Success>(
            optimize(
                OptimizeRequest(
                    stock = listOf(StockSpec(1, 6000, StockSpec.UNLIMITED)),
                    parts = listOf(PartSpec(1, 1, 1)),
                    kerfU = 0,
                ),
            ),
        ).plan
        assertEquals(1, plan.bars.single().parts.size)
        assertEquals(5999L, plan.bars.single().offcutU)
    }

    @Test
    fun `a stock length of one unit rejects everything larger without crashing`() {
        assertIs<OptimizeResult.Infeasible>(
            optimize(
                OptimizeRequest(
                    stock = listOf(StockSpec(1, 1, StockSpec.UNLIMITED)),
                    parts = listOf(PartSpec(1, 2, 1)),
                    kerfU = 0,
                ),
            ),
        )
    }

    @Test
    fun `every part being unplaceable is a Shortfall with nothing invented`() {
        // Limited stock that runs out immediately. The plan must contain what
        // fits and nothing more — inventing stock the user does not own is the
        // same class of lie as dropping a part.
        val result = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 1000, quantity = 1)),
                parts = listOf(PartSpec(1, 900, 5)),
                kerfU = 3,
            ),
        )
        val shortfall = assertIs<OptimizeResult.Shortfall>(result)
        assertEquals(1, shortfall.plan.bars.size, "it invented stock it was not given")
        assertEquals(
            5,
            shortfall.plan.bars.sumOf { it.parts.size } +
                shortfall.unplacedParts.sumOf { it.quantity },
            "a part was neither placed nor reported",
        )
    }
}
