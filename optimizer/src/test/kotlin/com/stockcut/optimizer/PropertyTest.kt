package com.stockcut.optimizer

import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The tests that actually guarantee this app is correct.
 *
 * A seeded generator rather than a property-testing library: zero extra
 * dependencies (TRD §9), and every failure prints the seed and the exact request
 * needed to reproduce it — which is what matters when one goes red.
 */
class PropertyTest {

    private val cases = 1_500

    private fun generate(rng: Random): OptimizeRequest {
        val stockCount = rng.nextInt(1, 4)
        val stock = (1..stockCount).map { i ->
            StockSpec(
                id = i.toLong(),
                lengthU = rng.nextLong(500L, 12_000L),
                quantity = if (rng.nextInt(3) == 0) rng.nextInt(1, 6) else StockSpec.UNLIMITED,
            )
        }
        val partCount = rng.nextInt(1, 12)
        val parts = (1..partCount).map { i ->
            PartSpec(
                id = i.toLong(),
                lengthU = rng.nextLong(50L, 4_000L),
                quantity = rng.nextInt(1, 8),
            )
        }
        return OptimizeRequest(
            stock = stock,
            parts = parts,
            kerfU = if (rng.nextInt(4) == 0) 0L else rng.nextLong(1L, 10L),
            trimU = if (rng.nextInt(4) == 0) rng.nextLong(0L, 60L) else 0L,
        )
    }

    /**
     * THE invariant. Every bar must balance:
     *   sum(parts) + cutCount*kerf + offcut + trim == stockLength
     * This single test catches nearly every real arithmetic bug.
     */
    @Test
    fun `every bar in every plan balances exactly`() {
        val seed = 20260804L
        val rng = Random(seed)
        repeat(cases) { i ->
            val request = generate(rng)
            val plan = optimize(request).planOrNull() ?: return@repeat
            for ((index, bar) in plan.bars.withIndex()) {
                val sum = bar.parts.sumOf { it.lengthU } +
                    bar.cutCount * request.kerfU +
                    bar.offcutU +
                    bar.trimU
                if (sum != bar.stockLengthU) {
                    fail(
                        "seed=$seed case=$i bar=$index does not balance: " +
                            "$sum != ${bar.stockLengthU}\nrequest=$request",
                    )
                }
            }
        }
    }

    /**
     * Conservation. Every requested part appears exactly once across the plan and
     * the unplaced list. Guards the worst possible bug: a plan that silently omits
     * a piece, which the user only discovers after cutting everything.
     */
    @Test
    fun `no part is ever lost or duplicated`() {
        val seed = 424242L
        val rng = Random(seed)
        repeat(cases) { i ->
            val request = generate(rng)
            val result = optimize(request)
            val requested = request.parts.sumOf { it.quantity }

            val (placed, unplaced) = when (result) {
                is OptimizeResult.Success ->
                    result.plan.bars.sumOf { it.parts.size } to 0
                is OptimizeResult.Shortfall ->
                    result.plan.bars.sumOf { it.parts.size } to result.unplacedParts.sumOf { it.quantity }
                else -> return@repeat
            }

            if (placed + unplaced != requested) {
                fail(
                    "seed=$seed case=$i lost parts: requested=$requested " +
                        "placed=$placed unplaced=$unplaced\nrequest=$request",
                )
            }

            // Per-part-id conservation, not just the total.
            val placedById = HashMap<Long, Int>()
            result.planOrNull()?.bars?.forEach { bar ->
                bar.parts.forEach { placedById.merge(it.partId, 1, Int::plus) }
            }
            if (result is OptimizeResult.Shortfall) {
                result.unplacedParts.forEach { placedById.merge(it.id, it.quantity, Int::plus) }
            }
            for (spec in request.parts) {
                assertEquals(
                    spec.quantity,
                    placedById[spec.id] ?: 0,
                    "seed=$seed case=$i part ${spec.id} count mismatch\nrequest=$request",
                )
            }
        }
    }

    /** A negative offcut means an over-packed bar — a plan that cannot be cut. */
    @Test
    fun `offcut is never negative and parts never exceed the bar`() {
        val seed = 777L
        val rng = Random(seed)
        repeat(cases) { i ->
            val request = generate(rng)
            val plan = optimize(request).planOrNull() ?: return@repeat
            for (bar in plan.bars) {
                assertTrue(
                    bar.offcutU >= 0L,
                    "seed=$seed case=$i negative offcut ${bar.offcutU}\nrequest=$request",
                )
                assertTrue(
                    bar.parts.sumOf { it.lengthU } <= bar.stockLengthU,
                    "seed=$seed case=$i parts exceed bar\nrequest=$request",
                )
                assertTrue(bar.cutCount >= 0, "seed=$seed case=$i negative cut count")
                assertTrue(bar.parts.isNotEmpty(), "seed=$seed case=$i empty bar in plan")
            }
        }
    }

    /** Identical input must give byte-identical output. Catches stray Random or set ordering. */
    @Test
    fun `optimize is deterministic`() {
        val rng = Random(31337L)
        repeat(300) {
            val request = generate(rng)
            val first = optimize(request)
            repeat(3) {
                assertEquals(first, optimize(request), "non-deterministic for request=$request")
            }
        }
    }

    /** Every requested part must fit, or be reported. Nothing is silently ignored. */
    @Test
    fun `infeasible parts are always reported, never dropped`() {
        val r = optimize(
            OptimizeRequest(
                stock = listOf(StockSpec(1, 2000, StockSpec.UNLIMITED)),
                parts = listOf(
                    PartSpec(1, 500, 2),
                    PartSpec(2, 5000, 1), // impossible
                    PartSpec(3, 9000, 1), // impossible
                ),
                kerfU = 3,
            ),
        )
        val infeasible = assertIs<OptimizeResult.Infeasible>(r)
        assertEquals(listOf(5000L, 9000L), infeasible.impossibleParts.map { it.lengthU }.sorted())
    }

    @Test
    fun `optimize never throws, on adversarial input`() {
        val rng = Random(1234L)
        repeat(5_000) {
            val request = OptimizeRequest(
                stock = (0..rng.nextInt(0, 4)).map {
                    StockSpec(it.toLong(), rng.nextLong(-100L, 8_000L), rng.nextInt(-3, 5))
                },
                parts = (0..rng.nextInt(0, 6)).map {
                    PartSpec(it.toLong(), rng.nextLong(-100L, 9_000L), rng.nextInt(-3, 6))
                },
                kerfU = rng.nextLong(-5L, 20L),
                trimU = rng.nextLong(-5L, 200L),
            )
            // Must return a result, never raise.
            optimize(request)
        }
    }
}

class BenchmarkTest {

    private fun run(partCount: Int): Long {
        val request = OptimizeRequest(
            stock = listOf(StockSpec(1, 6000, StockSpec.UNLIMITED)),
            parts = (1..partCount).map { PartSpec(it.toLong(), 100L + (it * 37L) % 1800L, 1) },
            kerfU = 3,
        )
        var result: OptimizeResult? = null
        val millis = measureTimeMillis { result = optimize(request) }
        assertIs<OptimizeResult.Success>(result)
        return millis
    }

    @Test
    fun `20 parts, the free tier limit, is effectively instant`() {
        run(20) // warm up
        val millis = run(20)
        assertTrue(millis < 50, "20 parts took ${millis}ms, budget 50ms")
    }

    @Test
    fun `200 parts stays well inside the 2 second on-device budget`() {
        run(200)
        val millis = run(200)
        // CI budget is 500ms as a proxy for <2s on a low-end phone (NFR-1).
        assertTrue(millis < 500, "200 parts took ${millis}ms, budget 500ms")
    }

    @Test
    fun `1000 parts, the paid cap, completes within budget`() {
        run(1_000)
        val millis = run(1_000)
        assertTrue(millis < 10_000, "1000 parts took ${millis}ms, budget 10s")
    }
}
