package com.stockcut.optimizer

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The waste-percentage regression gate (docs/06 §2.6).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHAT THIS IS FOR
 *
 * The optimizer is a heuristic, so "correct" and "good" are different questions.
 * The property tests answer correct: every plan balances, no part is lost. This
 * file answers good: a change that makes any known case pack WORSE fails the
 * build.
 *
 * That matters because packing quality is the product. A refactor that keeps
 * every invariant intact while quietly using one more bar per job costs a
 * tradesman real steel, and nothing else in the suite would notice.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * HOW TO CHANGE A BASELINE
 *
 * Improving the optimizer is expected to make these numbers go DOWN, and a lower
 * number passes. Only lower them here once the improvement is real.
 *
 * 🔴 Never raise a baseline to make a build green. That is the exact failure
 * this file exists to catch: it converts a silent regression into a deliberate,
 * reviewable act.
 */
class WasteBaselineTest {

    /**
     * @param wastePercent the worst acceptable figure. Recorded from the
     *   optimizer's actual output at the time the case was added.
     * @param bars the worst acceptable bar count — the number a user pays for.
     */
    private data class Baseline(
        val name: String,
        val request: OptimizeRequest,
        val wastePercent: Double,
        val bars: Int,
    )

    private fun stock(id: Long, len: Long, qty: Int = StockSpec.UNLIMITED) = StockSpec(id, len, qty)
    private fun part(id: Long, len: Long, qty: Int) = PartSpec(id, len, qty)

    /**
     * The oracle set from docs/06 §2.5, plus the shapes real jobs take.
     * Lengths are plain millimetres for readability; the optimizer is
     * unit-agnostic so the arithmetic is identical in internal units.
     */
    private val baselines = listOf(
        Baseline(
            "O-01 four exact pieces, no kerf",
            OptimizeRequest(listOf(stock(1, 6000)), listOf(part(1, 1500, 4)), kerfU = 0),
            wastePercent = 0.0, bars = 1,
        ),
        Baseline(
            "O-02 same cut with a 3mm blade",
            OptimizeRequest(listOf(stock(1, 6000)), listOf(part(1, 1500, 4)), kerfU = 3),
            wastePercent = 50.1, bars = 2,
        ),
        Baseline(
            "O-06 two stock sizes, short job",
            OptimizeRequest(
                listOf(stock(1, 6000), stock(2, 3000)),
                listOf(part(1, 2900, 2)),
                kerfU = 3,
            ),
            wastePercent = 3.4, bars = 2,
        ),
        Baseline(
            "O-08 a hundred identical parts, no kerf",
            OptimizeRequest(listOf(stock(1, 1000)), listOf(part(1, 100, 100)), kerfU = 0),
            wastePercent = 0.0, bars = 10,
        ),
        Baseline(
            "O-09 imperial job in internal units",
            OptimizeRequest(
                listOf(stock(1, 8L * 12L * 8128L)),
                listOf(part(1, 18L * 8128L, 6)),
                kerfU = 8128L / 8L,
            ),
            // 43.75% is high and is NOT a bug: six 18" pieces do not pack into
            // 96" bars — five fit the first, one rattles around the second.
            // A baseline records what the optimizer DOES, not what we wish it
            // did. Writing an aspirational number here would fail the build for
            // everyone until someone "fixed" it by raising it.
            wastePercent = 43.75, bars = 2,
        ),
        Baseline(
            "the seeded example: gate frame",
            OptimizeRequest(
                listOf(stock(1, 6000)),
                listOf(part(1, 1800, 2), part(2, 1200, 2), part(3, 900, 2), part(4, 850, 3)),
                kerfU = 3,
            ),
            wastePercent = 13.75, bars = 2,
        ),
        Baseline(
            "mixed lengths, one stock size — the commonest real shape",
            OptimizeRequest(
                listOf(stock(1, 6000)),
                listOf(
                    part(1, 2400, 3), part(2, 1850, 4), part(3, 1200, 6),
                    part(4, 950, 5), part(5, 600, 8),
                ),
                kerfU = 3,
            ),
            wastePercent = 12.92, bars = 7,
        ),
        Baseline(
            "many small parts from long stock",
            OptimizeRequest(
                listOf(stock(1, 12000)),
                listOf(part(1, 340, 60), part(2, 275, 40)),
                kerfU = 3,
            ),
            wastePercent = 12.78, bars = 3,
        ),
        Baseline(
            "awkward lengths that resist packing",
            OptimizeRequest(
                listOf(stock(1, 6000)),
                listOf(part(1, 3100, 4), part(2, 2700, 3)),
                kerfU = 3,
            ),
            wastePercent = 30.0, bars = 6,
        ),
        Baseline(
            "trim eats into every bar",
            OptimizeRequest(
                listOf(stock(1, 6000)),
                listOf(part(1, 1400, 8)),
                kerfU = 3,
                trimU = 50,
            ),
            wastePercent = 9.0, bars = 3,
        ),
    )

    @Test
    fun `no case packs worse than its recorded baseline`() {
        val regressions = mutableListOf<String>()

        for (baseline in baselines) {
            val plan = assertIs<OptimizeResult.Success>(
                optimize(baseline.request),
                "${baseline.name} stopped producing a plan at all",
            ).plan

            if (plan.wastePercent > baseline.wastePercent + 1e-9) {
                regressions += "${baseline.name}: waste ${"%.2f".format(plan.wastePercent)}% " +
                    "exceeds baseline ${baseline.wastePercent}%"
            }
            if (plan.bars.size > baseline.bars) {
                regressions += "${baseline.name}: used ${plan.bars.size} bars, " +
                    "baseline is ${baseline.bars} — that is steel the user has to buy"
            }
        }

        if (regressions.isNotEmpty()) {
            fail(
                "Packing quality regressed:\n" + regressions.joinToString("\n") { "  - $it" } +
                    "\n\nIf the change is a genuine improvement elsewhere, lower the " +
                    "affected baseline deliberately. Never raise one to go green.",
            )
        }
    }

    @Test
    fun `every baseline still satisfies the kerf invariant`() {
        // Belt and braces: a "better" waste figure achieved by producing a plan
        // that does not physically cut would be worse than any regression.
        for (baseline in baselines) {
            val plan = assertIs<OptimizeResult.Success>(optimize(baseline.request)).plan
            for (bar in plan.bars) {
                val sum = bar.parts.sumOf { it.lengthU } +
                    bar.cutCount * baseline.request.kerfU + bar.offcutU + bar.trimU
                assertTrue(
                    sum == bar.stockLengthU,
                    "${baseline.name}: a bar does not balance ($sum vs ${bar.stockLengthU})",
                )
            }
        }
    }
}
