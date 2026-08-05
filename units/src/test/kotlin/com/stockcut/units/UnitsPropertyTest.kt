package com.stockcut.units

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * Property tests over generated input.
 *
 * A tiny seeded harness rather than a property-testing library: the dependency
 * surface stays at zero (TRD §9), and a failure prints the exact seed and value
 * needed to reproduce it, which is what actually matters when a test goes red.
 */
class UnitsPropertyTest {

    private val cases = 2_000

    @Test
    fun `format then parse returns the original value, for every unit system`() {
        for (system in UnitSystem.entries) {
            val denominators =
                if (system == UnitSystem.INCH_FRACTIONAL) SUPPORTED_DENOMINATORS else listOf(16)
            for (denominator in denominators) {
                val seed = 20260804L + system.ordinal * 31L + denominator
                val rng = Random(seed)
                repeat(cases) { i ->
                    val raw = rng.nextLong(1L, MAX_LENGTH_U)
                    val value = snap(raw, system, denominator).coerceAtLeast(gridU(system, denominator))
                    val text = format(value, system, denominator)
                    val back = parse(text, system)
                    assertIs<ParseResult.Ok>(
                        back,
                        "seed=$seed case=$i system=$system denom=$denominator " +
                            "value=$value formatted='$text' did not parse: $back",
                    )
                    assertEquals(
                        value,
                        back.valueU,
                        "seed=$seed case=$i system=$system denom=$denominator " +
                            "value=$value formatted='$text' parsed back as ${back.valueU}",
                    )
                }
            }
        }
    }

    @Test
    fun `parse never throws, on arbitrary junk`() {
        val alphabet = "0123456789./' \"-+eE,abcXY\t".toCharArray()
        val rng = Random(99L)
        repeat(20_000) {
            val len = rng.nextInt(0, 14)
            val s = String(CharArray(len) { alphabet[rng.nextInt(alphabet.size)] })
            for (system in UnitSystem.entries) {
                // Must return a result, not raise. An exception here fails the test.
                parse(s, system)
            }
        }
    }

    @Test
    fun `snap is idempotent and stays on the grid`() {
        for (system in UnitSystem.entries) {
            val denominators =
                if (system == UnitSystem.INCH_FRACTIONAL) SUPPORTED_DENOMINATORS else listOf(16)
            for (denominator in denominators) {
                val g = gridU(system, denominator)
                val rng = Random(7L + system.ordinal)
                repeat(cases) {
                    val raw = rng.nextLong(0L, MAX_LENGTH_U)
                    val once = snap(raw, system, denominator)
                    assertEquals(0L, once % g, "snapped value $once is not on the $g grid")
                    assertEquals(once, snap(once, system, denominator), "snap is not idempotent")
                }
            }
        }
    }

    @Test
    fun `addition is exact where floating point is not`() {
        // Sum 1/64 sixty-four times; exactly one inch.
        val sixtyFourth = (parse("1/64", UnitSystem.INCH_FRACTIONAL) as ParseResult.Ok).valueU
        assertEquals(U_PER_INCH, (1..64).sumOf { sixtyFourth })

        // 0.1 mm summed ten times: exact in Long units.
        val tenthMm = (parse("0.1", UnitSystem.MM) as ParseResult.Ok).valueU
        assertEquals(U_PER_MM, (1..10).sumOf { tenthMm })

        // The same sum in Double does NOT come back to 1.0, because 0.1 has no
        // exact binary representation. This is the bug the Long design removes,
        // and on a 40-part cut list the drift compounds into a plan that will
        // not physically cut.
        val drifted = (1..10).fold(0.0) { acc, _ -> acc + 0.1 }
        assertNotEquals(1.0, drifted, "expected Double to drift; if this passes, the comment is wrong")
    }
}
