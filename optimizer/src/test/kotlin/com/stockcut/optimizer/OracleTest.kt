package com.stockcut.optimizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Hand-verified cases from docs/06-test-plan.md §2.5.
 *
 * Lengths here are plain millimetres for readability — the optimizer is
 * unit-agnostic, so the arithmetic is identical in internal units.
 *
 * O-10 is reserved for the first real job from a working tradesman. When a user
 * reports a wrong plan, it is added here permanently.
 */
class OracleTest {

    private fun req(
        stock: List<StockSpec>,
        parts: List<PartSpec>,
        kerf: Long,
        trim: Long = 0,
    ) = OptimizeRequest(stock, parts, kerf, trim)

    private fun stock(id: Long, len: Long, qty: Int = StockSpec.UNLIMITED) = StockSpec(id, len, qty)
    private fun part(id: Long, len: Long, qty: Int) = PartSpec(id, len, qty)

    @Test
    fun `O-01 four exact pieces, no kerf, fills one bar with no waste`() {
        val r = optimize(req(listOf(stock(1, 6000)), listOf(part(1, 1500, 4)), kerf = 0))
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertEquals(1, plan.bars.size)
        assertEquals(0L, plan.bars[0].offcutU)
        assertEquals(3, plan.bars[0].cutCount, "the fourth piece is the bar end — no fourth cut")
        assertEquals(0.0, plan.wastePercent)
    }

    @Test
    fun `O-02 the same cut with a 3mm blade no longer fits one bar`() {
        val r = optimize(req(listOf(stock(1, 6000)), listOf(part(1, 1500, 4)), kerf = 3))
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertEquals(2, plan.bars.size, "4x1500 plus kerf exceeds 6000")
        assertEquals(4, plan.bars.sumOf { it.parts.size })
    }

    @Test
    fun `O-03 a part the exact length of the bar needs no cut at all`() {
        val r = optimize(req(listOf(stock(1, 6000)), listOf(part(1, 6000, 1)), kerf = 3))
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertEquals(1, plan.bars.size)
        assertEquals(0L, plan.bars[0].offcutU)
        assertEquals(0, plan.bars[0].cutCount)
    }

    @Test
    fun `O-04 a part longer than any stock is infeasible, not silently dropped`() {
        val r = optimize(req(listOf(stock(1, 2000)), listOf(part(1, 2400, 1)), kerf = 3))
        val infeasible = assertIs<OptimizeResult.Infeasible>(r)
        assertEquals(1, infeasible.impossibleParts.size)
        assertEquals(2400L, infeasible.impossibleParts[0].lengthU)
        assertEquals(2000L, infeasible.longestUsableU)
    }

    @Test
    fun `O-05a two 6000 bars hold exactly ten 1000mm parts with a 3mm blade`() {
        // 5 per bar: 5*1000 + 5 kerfs = 5015, leaving 985. A 6th would need 6015.
        val r = optimize(req(listOf(stock(1, 6000, qty = 2)), listOf(part(1, 1000, 10)), kerf = 3))
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertEquals(2, plan.bars.size)
        assertTrue(plan.bars.all { it.parts.size == 5 })
    }

    @Test
    fun `O-05b limited stock that runs out reports a shortfall and what to buy`() {
        // Same stock, 14 parts: 10 fit, 4 do not, and one more bar would clear them.
        val r = optimize(req(listOf(stock(1, 6000, qty = 2)), listOf(part(1, 1000, 14)), kerf = 3))
        val shortfall = assertIs<OptimizeResult.Shortfall>(r)
        assertEquals(2, shortfall.plan.bars.size, "it must not invent stock it was not given")
        val placed = shortfall.plan.bars.sumOf { it.parts.size }
        val unplacedCount = shortfall.unplacedParts.sumOf { it.quantity }
        assertEquals(10, placed)
        assertEquals(4, unplacedCount)
        assertEquals(14, placed + unplacedCount, "every part is either placed or reported")
        assertEquals(mapOf(1L to 1), shortfall.additionalStockNeeded)
    }

    @Test
    fun `O-06 with two stock sizes it does not waste a long bar on a short job`() {
        val r = optimize(
            req(listOf(stock(1, 6000), stock(2, 3000)), listOf(part(1, 2900, 2)), kerf = 3),
        )
        val plan = assertIs<OptimizeResult.Success>(r).plan
        // Either answer uses 6000mm of steel; what must not happen is buying more.
        assertEquals(6000L, plan.totalStockUsedU)
        assertEquals(2, plan.bars.sumOf { it.parts.size })
    }

    @Test
    fun `O-07 trim is taken off the usable length before anything else`() {
        val r = optimize(req(listOf(stock(1, 6000)), listOf(part(1, 5960, 1)), kerf = 3, trim = 50))
        assertIs<OptimizeResult.Infeasible>(r, "usable is 5950, so 5960 cannot fit")
    }

    @Test
    fun `O-08 a hundred identical parts pack exactly with no kerf`() {
        val r = optimize(req(listOf(stock(1, 1000)), listOf(part(1, 100, 100)), kerf = 0))
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertEquals(10, plan.bars.size)
        assertEquals(0L, plan.totalOffcutU)
        assertEquals(0.0, plan.wastePercent)
    }

    @Test
    fun `O-09 imperial job in internal units round-trips through the optimizer`() {
        // 8 ft stock, six 18-inch pieces, 1/8" blade. Internal units: 1" = 8128.
        val eightFeet = 8L * 12L * 8128L
        val eighteenInches = 18L * 8128L
        val eighthInch = 8128L / 8L
        val r = optimize(
            req(listOf(stock(1, eightFeet)), listOf(part(1, eighteenInches, 6)), kerf = eighthInch),
        )
        val plan = assertIs<OptimizeResult.Success>(r).plan
        assertEquals(6, plan.bars.sumOf { it.parts.size })
        // 5 pieces per 96" bar: 5*18 = 90", plus 5 kerfs = 90.625", leaving 5.375".
        assertEquals(2, plan.bars.size)
    }

    @Test
    fun `waste percent is reported against stock actually consumed`() {
        val r = optimize(req(listOf(stock(1, 1000)), listOf(part(1, 400, 2)), kerf = 0))
        val plan = assertIs<OptimizeResult.Success>(r).plan
        // 800 used of 1000 -> 200 offcut -> 20% waste.
        assertEquals(20.0, plan.wastePercent)
    }
}
