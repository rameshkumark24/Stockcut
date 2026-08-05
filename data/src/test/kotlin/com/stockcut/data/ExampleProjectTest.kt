package com.stockcut.data

import com.stockcut.data.entitlement.Limits
import com.stockcut.data.seed.ExampleProject
import com.stockcut.optimizer.OptimizeRequest
import com.stockcut.optimizer.OptimizeResult
import com.stockcut.optimizer.PartSpec
import com.stockcut.optimizer.StockSpec
import com.stockcut.optimizer.optimize
import com.stockcut.units.U_PER_MM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The example job is the first thing a new user sees. If it produces a bad plan,
 * or no plan, the app has failed before the user has typed anything.
 */
class ExampleProjectTest {

    private fun request(): OptimizeRequest = OptimizeRequest(
        stock = ExampleProject.stock.mapIndexed { i, s ->
            StockSpec(i + 1L, s.lengthU, s.quantity, s.label)
        },
        parts = ExampleProject.parts.mapIndexed { i, p ->
            PartSpec(i + 1L, p.lengthU, p.quantity, p.label)
        },
        kerfU = ExampleProject.kerfU,
    )

    @Test
    fun `the example job produces a working plan`() {
        val plan = assertIs<OptimizeResult.Success>(optimize(request())).plan
        assertEquals(ExampleProject.totalPieces, plan.bars.sumOf { it.parts.size })
        assertTrue(plan.bars.isNotEmpty())
    }

    @Test
    fun `the example fits inside the free tier, so it always opens`() {
        // If the seed ever grew past 20 pieces, a brand-new user would tap the
        // example and be shown a paywall. That would be an appalling first run.
        assertTrue(
            ExampleProject.totalPieces <= Limits.FREE_PARTS_PER_PROJECT,
            "example has ${ExampleProject.totalPieces} pieces, free tier allows ${Limits.FREE_PARTS_PER_PROJECT}",
        )
        assertTrue(ExampleProject.stock.size <= Limits.FREE_STOCK_PER_PROJECT)
    }

    @Test
    fun `the example shows real offcut, not a suspiciously perfect fit`() {
        val plan = assertIs<OptimizeResult.Success>(optimize(request())).plan
        // A demo that lands on 0% waste teaches nothing and looks fake.
        assertTrue(plan.totalOffcutU > 0, "the example should show visible offcut")
        assertTrue(plan.wastePercent > 1.0, "waste was ${plan.wastePercent}%")
    }

    /**
     * Waste baseline. An improvement to the optimizer makes this pass; a
     * regression makes it fail. This is the regression gate from
     * docs/06-test-plan.md §2.6, pinned to a job a person can actually picture.
     */
    @Test
    fun `example waste does not regress`() {
        val plan = assertIs<OptimizeResult.Success>(optimize(request())).plan
        val baseline = 13.75
        assertTrue(
            plan.wastePercent <= baseline + 1e-9,
            "waste regressed to ${plan.wastePercent}%, baseline is $baseline%",
        )
    }

    @Test
    fun `the plan balances, in the same units the app stores`() {
        val plan = assertIs<OptimizeResult.Success>(optimize(request())).plan
        for (bar in plan.bars) {
            val sum = bar.parts.sumOf { it.lengthU } +
                bar.cutCount * ExampleProject.kerfU +
                bar.offcutU +
                bar.trimU
            assertEquals(bar.stockLengthU, sum)
        }
        assertEquals(
            plan.totalStockUsedU,
            plan.totalPartsU + plan.totalKerfU + plan.totalTrimU + plan.totalOffcutU,
        )
    }

    @Test
    fun `seed lengths convert to internal units exactly`() {
        assertEquals(6_000L * U_PER_MM, ExampleProject.stock[0].lengthU)
        assertEquals(1_800L * U_PER_MM, ExampleProject.parts[0].lengthU)
        assertEquals(3L * U_PER_MM, ExampleProject.kerfU)
    }
}
