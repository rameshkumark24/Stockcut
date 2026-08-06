package com.stockcut.ui.components

import com.stockcut.units.U_PER_FOOT
import com.stockcut.units.U_PER_INCH
import com.stockcut.units.U_PER_MM
import com.stockcut.units.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MeasurementField is the component docs/04 §6 calls the critical one, and
 * fractional-inch entry is the predicted failure mode for the US market
 * (docs/09 §4.1). It is tested accordingly, on the JVM, in milliseconds.
 */
class MeasurementFieldStateTest {

    private fun field(
        system: UnitSystem = UnitSystem.MM,
        denominator: Int = 16,
    ) = MeasurementFieldState(unitSystem = system, denominator = denominator)

    // ── Entry ────────────────────────────────────────────────────────────────

    @Test
    fun `a plain metric length commits and reformats`() {
        val f = field()
        f.onTextChange("1200")
        assertTrue(f.commit())
        assertEquals(1_200L * U_PER_MM, f.valueU)
        assertEquals("1200", f.text)
        assertNull(f.error)
    }

    @Test
    fun `test plan item 4 - one and five sixteenths displays correctly after blur`() {
        val f = field(UnitSystem.INCH_FRACTIONAL)
        f.onTextChange("1 5/16")
        assertTrue(f.commit())
        assertEquals("1 5/16\"", f.text)
        assertEquals(U_PER_INCH + 5L * (U_PER_INCH / 16), f.valueU)
    }

    @Test
    fun `a bare fraction is accepted, as a carpenter would type it`() {
        val f = field(UnitSystem.INCH_FRACTIONAL)
        f.onTextChange("3/4")
        assertTrue(f.commit())
        assertEquals("3/4\"", f.text)
    }

    @Test
    fun `feet and inches round-trip into canonical form`() {
        val f = field(UnitSystem.INCH_FRACTIONAL)
        f.onTextChange("8'3.5")
        assertTrue(f.commit())
        // The user typed a decimal; the field answers in the fractions they read.
        assertEquals("8' 3 1/2\"", f.text)
        assertEquals(8 * U_PER_FOOT + 3 * U_PER_INCH + U_PER_INCH / 2, f.valueU)
    }

    // ── Errors ───────────────────────────────────────────────────────────────

    @Test
    fun `typing does not raise an error mid-way through a valid entry`() {
        // "1 5/16" passes through "1", "1 ", "1 5", "1 5/" — none may show an
        // error, or the field nags at someone who is typing it correctly.
        val f = field(UnitSystem.INCH_FRACTIONAL)
        for (partial in listOf("1", "1 ", "1 5", "1 5/")) {
            f.onTextChange(partial)
            assertNull(f.error, "showed an error while typing '$partial'")
        }
        f.onTextChange("1 5/16")
        assertTrue(f.commit())
    }

    @Test
    fun `an error appears on blur and clears as soon as the user edits`() {
        val f = field()
        f.onTextChange("abc")
        assertFalse(f.commit())
        assertNotNull(f.error)

        f.onTextChange("ab")
        assertNull(f.error, "the message must go the moment they start fixing it")
    }

    @Test
    fun `zero and negative are rejected with a message written for a person`() {
        val f = field()
        f.onTextChange("0")
        assertFalse(f.commit())
        assertEquals("Length must be more than 0.", f.error)

        f.onTextChange("-5")
        assertFalse(f.commit())
        assertNotNull(f.error)
    }

    @Test
    fun `an empty field reports itself rather than committing a null length`() {
        val f = field()
        f.onTextChange("   ")
        assertFalse(f.commit())
        assertNull(f.valueU)
        assertNotNull(f.error)
    }

    // ── Rule 2: units re-format, they never mutate ───────────────────────────

    @Test
    fun `test plan item 03 - changing units re-formats but never changes the stored value`() {
        val f = field(UnitSystem.MM)
        f.onTextChange("1200")
        f.commit()
        val stored = f.valueU

        f.setUnits(UnitSystem.INCH_FRACTIONAL, 16)
        assertEquals(stored, f.valueU, "switching units moved the stored value")
        assertEquals("3' 11 1/4\"", f.text)

        f.setUnits(UnitSystem.M)
        assertEquals(stored, f.valueU)
        assertEquals("1.2", f.text)

        f.setUnits(UnitSystem.MM)
        assertEquals(stored, f.valueU)
        assertEquals("1200", f.text)
    }

    @Test
    fun `changing the fraction denominator does not move the value either`() {
        val f = field(UnitSystem.INCH_FRACTIONAL, 16)
        f.onTextChange("1 1/2")
        f.commit()
        val stored = f.valueU

        f.setUnits(UnitSystem.INCH_FRACTIONAL, 64)
        assertEquals(stored, f.valueU)
        assertEquals("1 1/2\"", f.text, "1/2 reduces to 1/2 at any denominator")
    }

    // ── Rule 3: what is shown is what is stored ──────────────────────────────

    @Test
    fun `the committed value is snapped, so the display never lies about storage`() {
        // 1200.55 mm cannot be shown at one decimal place. If it were stored
        // unsnapped, the box would read 1200.6 while 1200.55 was in the database
        // — and every calculation would use a number the user never saw.
        val f = field(UnitSystem.MM)
        f.onTextChange("1200.55")
        assertTrue(f.commit())

        val shown = f.text
        val stored = f.valueU
        assertNotNull(stored)

        // Re-parsing exactly what is on screen must give exactly what is stored.
        val reread = MeasurementFieldState(unitSystem = UnitSystem.MM)
        reread.onTextChange(shown)
        reread.commit()
        assertEquals(stored, reread.valueU, "the field displays a different value than it stores")
    }

    @Test
    fun `a value loaded from storage renders without a parse`() {
        val f = field(UnitSystem.INCH_FRACTIONAL)
        f.setValueU(8 * U_PER_FOOT)
        assertEquals("8'", f.text)
        assertEquals(8 * U_PER_FOOT, f.valueU)
        assertNull(f.error)
    }

    @Test
    fun `clearing an optional field leaves no error behind`() {
        val f = field()
        f.onTextChange("50")
        f.commit()
        f.clear()
        assertNull(f.valueU)
        assertEquals("", f.text)
        assertNull(f.error)
    }

    @Test
    fun `commit is idempotent`() {
        // The field commits on blur, and again when Optimize reads it. Committing
        // an already-canonical value must not drift it.
        val f = field(UnitSystem.INCH_FRACTIONAL)
        f.onTextChange("1 5/16")
        f.commit()
        val once = f.valueU
        val text = f.text

        repeat(5) { f.commit() }
        assertEquals(once, f.valueU)
        assertEquals(text, f.text)
    }
}
