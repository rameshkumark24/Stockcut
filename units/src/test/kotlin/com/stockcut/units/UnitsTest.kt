package com.stockcut.units

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExactnessTest {

    private fun ok(input: String, system: UnitSystem): Long {
        val r = parse(input, system)
        assertIs<ParseResult.Ok>(r, "expected $input to parse in $system, got $r")
        return r.valueU
    }

    @Test
    fun `the exactness table from the TRD holds`() {
        assertEquals(320L, ok("1", UnitSystem.MM))
        assertEquals(32L, ok("0.1", UnitSystem.MM))
        assertEquals(8_128L, ok("1", UnitSystem.INCH_DECIMAL))
        assertEquals(4_064L, ok("1/2", UnitSystem.INCH_FRACTIONAL))
        assertEquals(508L, ok("1/16", UnitSystem.INCH_FRACTIONAL))
        assertEquals(254L, ok("1/32", UnitSystem.INCH_FRACTIONAL))
        assertEquals(127L, ok("1/64", UnitSystem.INCH_FRACTIONAL))
    }

    @Test
    fun `three quarters plus one quarter is exactly one inch`() {
        val threeQuarters = ok("3/4", UnitSystem.INCH_FRACTIONAL)
        val oneQuarter = ok("1/4", UnitSystem.INCH_FRACTIONAL)
        assertEquals(U_PER_INCH, threeQuarters + oneQuarter)
        // The whole point of integer units: this is exact, not approximately equal.
        assertEquals("1\"", format(threeQuarters + oneQuarter, UnitSystem.INCH_FRACTIONAL))
    }

    @Test
    fun `a tenth of a millimetre summed ten times is exactly one millimetre`() {
        val tenth = ok("0.1", UnitSystem.MM)
        assertEquals(U_PER_MM, (1..10).sumOf { tenth })
    }

    @Test
    fun `metric and imperial agree on one inch`() {
        assertEquals(ok("25.4", UnitSystem.MM), ok("1", UnitSystem.INCH_DECIMAL))
    }

    @Test
    fun `feet and inches decompose correctly`() {
        val expected = 8 * U_PER_FOOT + 3 * U_PER_INCH + 4_064L
        assertEquals(expected, ok("8' 3 1/2\"", UnitSystem.INCH_FRACTIONAL))
        assertEquals(expected, ok("8'3 1/2", UnitSystem.INCH_FRACTIONAL))
    }

    @Test
    fun `unit suffixes are tolerated`() {
        assertEquals(1_200L * U_PER_MM, ok("1200 mm", UnitSystem.MM))
        assertEquals(1_200L * U_PER_MM, ok("1200mm", UnitSystem.MM))
        assertEquals(1_200L * U_PER_MM, ok("1,200", UnitSystem.MM))
        assertEquals(47L * U_PER_INCH, ok("47\"", UnitSystem.INCH_DECIMAL))
    }
}

class FormattingTest {

    @Test
    fun `fractions are reduced to lowest terms`() {
        // 8/16 must render as 1/2, never as 8/16.
        assertEquals("1/2\"", format(4_064L, UnitSystem.INCH_FRACTIONAL, 16))
        assertEquals("1/4\"", format(2_032L, UnitSystem.INCH_FRACTIONAL, 16))
        assertEquals("3/4\"", format(6_096L, UnitSystem.INCH_FRACTIONAL, 16))
    }

    @Test
    fun `whole numbers omit the fraction entirely`() {
        assertEquals("7\"", format(7L * U_PER_INCH, UnitSystem.INCH_FRACTIONAL, 16))
        assertEquals("11\"", format(11L * U_PER_INCH, UnitSystem.INCH_FRACTIONAL, 16))
        assertEquals("1200", format(1_200L * U_PER_MM, UnitSystem.MM))
    }

    @Test
    fun `inches roll up into feet once past twelve`() {
        // 47" is 3' 11" and must display that way — a joiner reading a cut list
        // wants feet and inches, not 47 inches to count out on a tape.
        assertEquals("3' 11\"", format(47L * U_PER_INCH, UnitSystem.INCH_FRACTIONAL, 16))
        assertEquals("1'", format(12L * U_PER_INCH, UnitSystem.INCH_FRACTIONAL, 16))
    }

    @Test
    fun `feet are shown when the value reaches twelve inches`() {
        assertEquals("1'", format(U_PER_FOOT, UnitSystem.INCH_FRACTIONAL, 16))
        assertEquals("8' 3 1/2\"", format(8 * U_PER_FOOT + 3 * U_PER_INCH + 4_064L, UnitSystem.INCH_FRACTIONAL, 16))
        assertEquals("11 1/2\"", format(11 * U_PER_INCH + 4_064L, UnitSystem.INCH_FRACTIONAL, 16))
    }

    @Test
    fun `metric trims trailing zeros`() {
        assertEquals("1200.5", format(1_200L * U_PER_MM + 160L, UnitSystem.MM))
        assertEquals("6", format(6L * U_PER_M, UnitSystem.M))
    }

    @Test
    fun `one sixty-fourth inch renders exactly in decimal inches`() {
        assertEquals("0.015625", format(127L, UnitSystem.INCH_DECIMAL))
    }
}

class RejectionTest {

    private fun err(input: String, system: UnitSystem = UnitSystem.MM) {
        assertIs<ParseResult.Error>(parse(input, system), "expected '$input' to be rejected in $system")
    }

    @Test
    fun `bad input is rejected with a message, never an exception`() {
        err("")
        err("   ")
        err("abc")
        err("-5")
        err("0")
        err("12.3.4")
        err("1/0", UnitSystem.INCH_FRACTIONAL)
        err("1//2", UnitSystem.INCH_FRACTIONAL)
        err("1 5/", UnitSystem.INCH_FRACTIONAL)
        err("\"", UnitSystem.INCH_FRACTIONAL)
        err("1 2 3", UnitSystem.INCH_FRACTIONAL)
        err("1/2 3", UnitSystem.INCH_FRACTIONAL) // fraction before whole
    }

    @Test
    fun `absurd lengths are rejected rather than silently overflowing`() {
        err("999999999999")
        err("200", UnitSystem.M) // 200 m > 100 m cap
    }

    @Test
    fun `error messages are written for a person`() {
        val r = parse("abc", UnitSystem.INCH_FRACTIONAL)
        assertIs<ParseResult.Error>(r)
        assertTrue(r.message.isNotBlank())
        assertTrue(r.message.first().isUpperCase() || r.message.startsWith("Try"))
    }
}
